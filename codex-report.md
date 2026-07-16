# Codex Deep Scan Report - PvPBotPaper

## Re-scan sau khi code đã sửa - 2026-07-16

Kết luận ngắn: **chưa đạt 100%**.

Trạng thái xác minh mới nhất:

- `./gradlew compileJava`: PASS.
- `./gradlew test`: PASS nhưng vẫn `NO-SOURCE`, nghĩa là chưa có test tự động thật.
- `./gradlew build`: PASS.
- Không còn lỗi cú pháp/blocking compile tại thời điểm quét.

Các lỗi đã được cải thiện so với báo cáo trước:

1. `BotManager.reloadRegistry()` đã nạp `BotSettings` cho NPC reload từ Citizens nếu trait chưa có settings.
   - File: `src/main/java/com/pvpbot/bot/BotManager.java`
   - Dòng: 99-113.
   - Trạng thái: **đã sửa phần lõi**.

2. CI đã đổi từ JDK 21 sang JDK 25 để khớp Gradle toolchain.
   - File: `.github/workflows/build.yml`
   - Dòng: 14-18.
   - File: `.github/workflows/release.yml`
   - Dòng: 42-46.
   - Trạng thái: **đã sửa**.

3. `/pvpbot remove` đã kiểm tra kết quả xóa thay vì luôn báo thành công.
   - File: `src/main/java/com/pvpbot/bot/BotManager.java`
   - Dòng: 63-75.
   - File: `src/main/java/com/pvpbot/command/PvPBotCommand.java`
   - Dòng: 75-89.
   - Trạng thái: **đã sửa**.

4. `/pvpbot settings <key> <value>` cho global setting đã có nhánh nhận diện key.
   - File: `src/main/java/com/pvpbot/command/PvPBotCommand.java`
   - Dòng: 108-151.
   - Trạng thái: **sửa một phần**, còn lỗi case-sensitive ở mục dưới.

Các lỗi còn lại, cần sửa trước khi gọi là 100%:

### A. `onInventoryClick` đang cancel mọi inventory click trên server

- Mức độ: CRITICAL runtime/UX.
- File: `src/main/java/com/pvpbot/gui/SettingsGUI.java`
- Dòng: 207-214.

Mô tả:
`e.setCancelled(true)` đang chạy ngay đầu handler, trước khi kiểm tra người chơi có đang mở PvPBot GUI hay không. Vì listener nhận mọi `InventoryClickEvent`, hành vi này có thể hủy click inventory của cả người chơi không mở GUI PvPBot.

Ảnh hưởng:
- Người chơi có thể không click/di chuyển item trong inventory bình thường.
- Đây là lỗi server-wide, không chỉ trong GUI PvPBot.

Cách sửa:
- Chỉ `setCancelled(true)` sau khi xác nhận `openGUIs.get(player.getUniqueId()) != null`.
- Ví dụ logic đúng:
  - kiểm tra `clickedInventory != null`
  - kiểm tra `whoClicked instanceof Player`
  - lấy `OpenGUI gui`
  - nếu `gui == null` thì return
  - sau đó mới cancel event.

### B. Sửa `onInventoryDrag` hiện tại không có tác dụng

- Mức độ: HIGH.
- File: `src/main/java/com/pvpbot/gui/SettingsGUI.java`
- Dòng: 226-230.
- Liên quan:
  - `src/main/java/com/pvpbot/gui/SettingsGUI.java`: 108, 137.

Mô tả:
Inventory được tạo bằng `Bukkit.createInventory(null, ...)`, tức holder là `null`. Nhưng handler lại kiểm tra:
`event.getView().getTopInventory().getHolder() instanceof SettingsGUI`.
Điều kiện này gần như luôn false vì holder không phải `SettingsGUI`.

Ảnh hưởng:
- Drag item trong PvPBot GUI có thể không bị chặn như mong muốn.
- Sửa cũ từ “cancel toàn server” đã chuyển thành “không cancel GUI PvPBot”.

Cách sửa:
- Dùng `openGUIs.containsKey(event.getWhoClicked().getUniqueId())`.
- Nếu muốn chính xác hơn, kiểm tra raw slots drag có chạm top inventory hay không:
  - nếu player không có trong `openGUIs` thì return
  - nếu `event.getRawSlots()` có slot `< event.getView().getTopInventory().getSize()` thì cancel.

### C. `SettingsGUI.getSettings()` vẫn có thể trả null trong trường hợp trait null-settings ngoài luồng reload

- Mức độ: MEDIUM.
- File: `src/main/java/com/pvpbot/gui/SettingsGUI.java`
- Dòng: 160-167.

Mô tả:
`getSettings()` trả thẳng `trait.getSettings()` nếu trait tồn tại. Hiện `spawnBot()` và `reloadRegistry()` đã set settings, nên đường chính đã ổn hơn. Nhưng nếu trait được attach bằng Citizens/plugin khác hoặc có trạng thái bất thường, method vẫn có thể trả null.

Ảnh hưởng:
- `createSettingItem()` dòng 170-172 có thể NPE khi gọi `settings.getValue(key)`.

Cách sửa:
- Nếu `trait.getSettings() == null`, tạo/load settings hoặc fallback `plugin.getDefaultSettings()`.

### D. Command settings vẫn lỗi với key viết hoa/thường không chuẩn

- Mức độ: LOW-MEDIUM.
- File: `src/main/java/com/pvpbot/command/PvPBotCommand.java`
- Dòng: 108-125.

Mô tả:
Code kiểm tra `key.equalsIgnoreCase(second)` nhưng sau đó vẫn dùng `String key = second`. `BotSettings.getType(key)` là case-sensitive. Ví dụ `/pvpbot settings DEBUG true` được nhận là setting key, nhưng `getType("DEBUG")` trả null.

Ảnh hưởng:
- Command thủ công có hành vi mâu thuẫn.

Cách sửa:
- Khi match key, lưu canonical key từ `SETTING_KEYS`, ví dụ `matchedKey = key`, rồi dùng `matchedKey` cho `getType/getValue/setValue`.

### E. Boolean parser vẫn biến input sai thành `false`

- Mức độ: MEDIUM.
- File: `src/main/java/com/pvpbot/command/PvPBotCommand.java`
- Dòng: 131-135, 190-194.

Mô tả:
Input boolean chỉ coi `true`, `1`, `yes` là true. Các giá trị sai như `abc`, `maybe`, `2` bị set thành false mà không báo lỗi.

Ảnh hưởng:
- Admin gõ nhầm có thể tắt setting ngoài ý muốn.

Cách sửa:
- Chỉ chấp nhận rõ `true/false`, `yes/no`, `1/0`, `on/off`.
- Giá trị khác phải báo lỗi và không lưu.

### F. Tab-complete global settings vẫn chưa đúng

- Mức độ: LOW.
- File: `src/main/java/com/pvpbot/command/PvPBotCommand.java`
- Dòng: 276-299.

Mô tả:
Với `/pvpbot settings debug <value>`, `args[1]` đã là key. Nhưng ở `args.length == 3`, code vẫn suggest setting key cho `args[2]`, thay vì suggest value boolean.

Ảnh hưởng:
- UX command chưa hoàn chỉnh.

### G. `getCommand("pvpbot")` vẫn chưa null-safe

- Mức độ: MEDIUM.
- File: `src/main/java/com/pvpbot/PvPBotPlugin.java`
- Dòng: 35-37.

Mô tả:
`getCommand("pvpbot")` vẫn được gọi trực tiếp 2 lần. Nếu `plugin.yml` lỗi hoặc command đổi tên, plugin sẽ NPE trong `onEnable()`.

### H. `plugin.yml` usage vẫn thiếu `settings|gui`

- Mức độ: LOW.
- File: `src/main/resources/plugin.yml`
- Dòng: 11.

Mô tả:
Usage vẫn là `/ <command> <spawn|remove|removeall> [name]`, chưa khớp command thật.

### I. `.gradle/` vẫn đang bị track trong git

- Mức độ: HIGH repo hygiene.
- Số file `.gradle` đang tracked: 24.
- File: `.gitignore`
- Dòng: 1-13.

Mô tả:
`.gitignore` chưa ignore `.gradle/`, và các file cache/lock Gradle vẫn đang nằm trong index. Sau mỗi build, worktree tiếp tục dirty.

Cách sửa:
- Thêm `.gradle/` vào `.gitignore`.
- Sau khi xác nhận, chạy `git rm -r --cached .gradle`.

### J. Các rủi ro còn giữ nguyên

- `build.sh` vẫn hardcode `JAVA_HOME=/usr/lib/jvm/java-25-openjdk`.
  - File: `build.sh`
  - Dòng: 24-26.
- `push.sh` vẫn dùng `git add .`.
  - File: `push.sh`
  - Dòng: 14-15.
- Release workflow và `release.sh` vẫn sửa version bằng regex rộng `sed -i "s/version:.*/.../g"`.
  - File: `release.sh`
  - Dòng: 90-91.
  - File: `.github/workflows/release.yml`
  - Dòng: 57-58.
- `PvPBotTrait` vẫn log mỗi 100 tick và lấy `ServerPlayer nmsPlayer` nhưng không dùng.
  - File: `src/main/java/com/pvpbot/npc/PvPBotTrait.java`
  - Dòng: 31-38.
- Nhiều setting combat/ranged/shield/totem vẫn chỉ là config/GUI, trait chưa implement AI PvP thật.
  - File: `src/main/java/com/pvpbot/npc/PvPBotTrait.java`
  - Dòng: 24-46.

Tỷ lệ đánh giá sau re-scan:

- Build/compile: **100% pass**.
- Test tự động: **chưa đạt**, vì `NO-SOURCE`.
- Các lỗi critical cũ: **đã sửa một phần**, nhưng phát sinh/chưa sửa lỗi GUI critical.
- Tổng thể dự án: **khoảng 80-85%**, chưa thể xác nhận 100%.

Ưu tiên sửa tiếp:

1. Sửa `SettingsGUI.onInventoryClick()` để không cancel toàn server.
2. Sửa `SettingsGUI.onInventoryDrag()` bằng `openGUIs` và raw slot check.
3. Thêm fallback null settings trong `SettingsGUI.getSettings()` và `PvPBotCommand`.
4. Chuẩn hóa canonical setting key và boolean parser.
5. Bỏ `.gradle/` khỏi git.

---

Thời điểm quét: 2026-07-16  
Phạm vi: toàn bộ repository `/home/khoablabla/PvPBotPaper`, gồm Java source, resources, Gradle, GitHub Actions, shell scripts, Python script, trạng thái git và artifact build.

## 1. Kết quả kiểm tra tự động

- `./gradlew compileJava`: PASS.
- `./gradlew test`: PASS nhưng `NO-SOURCE`, nghĩa là dự án hiện không có test source.
- `./gradlew build`: PASS, tạo được:
  - `build/libs/PvPBotPaper-1.0.0.jar`
  - `build/libs/PvPBotPaper-1.0.0-reobf.jar`
- Gradle problem report ghi 2 advice compiler:
  - Some input files use or override a deprecated API.
  - Recompile with `-Xlint:deprecation` for details.

Kết luận build: không có lỗi cú pháp Java tại thời điểm quét. Các lỗi dưới đây chủ yếu là lỗi logic/runtime/CI/release/maintenance.

## 2. Lỗi nghiêm trọng và rủi ro runtime

### 2.1. NPC reload từ Citizens có thể không có `BotSettings`

- Mức độ: HIGH.
- File: `src/main/java/com/pvpbot/bot/BotManager.java`
- Dòng: 99-108.
- Liên quan thêm:
  - `src/main/java/com/pvpbot/npc/PvPBotTrait.java`: 12, 20-22, 48-53.
  - `src/main/java/com/pvpbot/gui/SettingsGUI.java`: 160-165, 170-172, 273-291.
  - `src/main/java/com/pvpbot/command/PvPBotCommand.java`: 165-171, 189-203.

Mô tả:
`reloadRegistry()` chỉ đưa NPC có `PvPBotTrait` vào `activeNPCs`, nhưng không đảm bảo trait đó có `settings`. `PvPBotTrait.onAttach()` đang để trống, còn `settings` chỉ được inject khi spawn mới qua `BotManager.spawnBot()` dòng 37-41. Sau restart/server reload, trait được Citizens nạp lại có thể tồn tại nhưng `settings == null`.

Ảnh hưởng:
- `/pvpbot settings <bot>` có thể ném `NullPointerException` tại `botSettings.getValue(key)` nếu trait settings null.
- `/pvpbot gui <bot>` có thể ném `NullPointerException` tại `settings.getValue(key)`.
- Bot reload từ Citizens không giữ cấu hình riêng, và cũng không fallback về default settings.

Khuyến nghị:
- Trong `reloadRegistry()`, lấy trait và nếu `trait.getSettings() == null` thì tạo `BotSettings`, `loadFromConfig(plugin.getConfig())`, rồi `trait.setSettings(settings)`.
- Trong `SettingsGUI.getSettings()` và `PvPBotCommand.handleSettings()`, nếu trait settings null thì fallback an toàn về `plugin.getDefaultSettings()` hoặc tự khởi tạo.

### 2.2. `getCommand("pvpbot")` không kiểm tra null

- Mức độ: MEDIUM.
- File: `src/main/java/com/pvpbot/PvPBotPlugin.java`
- Dòng: 36-37.

Mô tả:
`getCommand("pvpbot").setExecutor(...)` và `.setTabCompleter(...)` giả định command luôn tồn tại trong `plugin.yml`. Hiện tại `plugin.yml` có command, nên build/runtime bình thường. Nhưng nếu YAML lỗi, resource filtering lỗi, hoặc command đổi tên, plugin sẽ crash trong `onEnable()` bằng `NullPointerException`.

Ảnh hưởng:
- Plugin fail enable hoàn toàn.
- Log lỗi khó đọc hơn vì NPE thay vì message rõ ràng.

Khuyến nghị:
- Gán `PluginCommand pvpbotCommand = getCommand("pvpbot")`.
- Nếu null, log severe và disable plugin hoặc throw `IllegalStateException` có thông điệp rõ.

### 2.3. Không kiểm tra trạng thái Citizens trước khi gọi API

- Mức độ: MEDIUM.
- File: `src/main/java/com/pvpbot/PvPBotPlugin.java`
- Dòng: 33, 41.
- File: `src/main/java/com/pvpbot/bot/BotManager.java`
- Dòng: 34, 101.

Mô tả:
`plugin.yml` khai báo `depend: Citizens`, nên Bukkit/Paper sẽ cố đảm bảo Citizens có trước. Tuy vậy code vẫn gọi trực tiếp `CitizensAPI.getTraitFactory()` và `CitizensAPI.getNPCRegistry()` mà không có guard. Nếu Citizens sai version, load lỗi một phần, hoặc API trả trạng thái chưa sẵn sàng, plugin có thể fail enable.

Ảnh hưởng:
- Plugin không bật được.
- Khó phân biệt lỗi cấu hình dependency với lỗi code.

Khuyến nghị:
- Kiểm tra `getServer().getPluginManager().isPluginEnabled("Citizens")` trước khi register trait/reload registry.
- Log lỗi rõ version/plugin dependency nếu thiếu.

### 2.4. `BotManager.spawnBot()` không kiểm tra `loc.getWorld()`

- Mức độ: LOW-MEDIUM.
- File: `src/main/java/com/pvpbot/bot/BotManager.java`
- Dòng: 26, 45, 57.

Mô tả:
Command spawn truyền `player.getLocation()` nên world thường không null. Nhưng API public `spawnBot(Location loc, String name)` không tự validate. Dòng 57 gọi `loc.getWorld().getName()` có thể NPE nếu có code khác gọi với `Location` không world.

Ảnh hưởng:
- Crash ở callsite mở rộng hoặc test/plugin khác.

Khuyến nghị:
- Check `loc == null || loc.getWorld() == null` và trả lỗi/log rõ.

### 2.5. `npc.spawn(loc)` không kiểm tra kết quả

- Mức độ: MEDIUM.
- File: `src/main/java/com/pvpbot/bot/BotManager.java`
- Dòng: 45-60.

Mô tả:
Citizens `npc.spawn(loc)` có thể fail trong một số tình huống, nhưng code vẫn `activeNPCs.put(...)`, log "Spawned bot" và command báo thành công.

Ảnh hưởng:
- Người dùng thấy bot đã spawn dù thực tế không xuất hiện.
- `activeNPCs` có NPC không spawned, làm `/remove`, `/gui`, tab completion thiếu tin cậy.

Khuyến nghị:
- Kiểm tra boolean result nếu API trả boolean trong version đang dùng, hoặc kiểm tra `npc.isSpawned()` ngay sau spawn.
- Nếu spawn fail thì destroy/cleanup và trả null hoặc ném exception có message rõ.

## 3. Lỗi logic command và cấu hình

### 3.1. Boolean parser biến mọi input lạ thành `false`

- Mức độ: MEDIUM.
- File: `src/main/java/com/pvpbot/command/PvPBotCommand.java`
- Dòng: 131-135, 190-194.
- File: `src/main/java/com/pvpbot/bot/BotSettings.java`
- Dòng: 247-250.

Mô tả:
Với setting boolean, code chỉ coi `true`, `1`, `yes` là true. Mọi giá trị khác, kể cả `abc`, `maybe`, `2`, sẽ thành false mà không báo lỗi.

Ảnh hưởng:
- Admin gõ nhầm sẽ vô tình tắt setting.
- Debug khó vì command vẫn báo set thành công.

Khuyến nghị:
- Chỉ chấp nhận `true/false`, `yes/no`, `1/0`, `on/off`.
- Nếu input không thuộc danh sách hợp lệ, báo `Invalid boolean value`.

### 3.2. Tên setting không được chuẩn hóa case

- Mức độ: LOW-MEDIUM.
- File: `src/main/java/com/pvpbot/command/PvPBotCommand.java`
- Dòng: 108-125, 181-184.

Mô tả:
Phần kiểm tra global dùng `equalsIgnoreCase(second)`, nhưng sau đó vẫn dùng `String key = second`. `BotSettings.getType(key)` dùng map case-sensitive. Nếu admin gõ `/pvpbot settings DEBUG true`, `isKey == true` nhưng `getType("DEBUG") == null`, dẫn tới "Unknown setting key".

Ảnh hưởng:
- Tab-complete dùng lowercase nên ít gặp, nhưng command thủ công bị hành vi mâu thuẫn.

Khuyến nghị:
- Khi match key, lưu canonical key từ `SETTING_KEYS` rồi dùng canonical key cho `getType/getValue/setValue`.

### 3.3. Tab complete cho `/pvpbot settings <key> <value>` chưa đúng nhánh global

- Mức độ: LOW.
- File: `src/main/java/com/pvpbot/command/PvPBotCommand.java`
- Dòng: 276-299.

Mô tả:
Khi args length 3, code luôn suggest setting keys ở `args[2]`. Nhưng với cú pháp global `/pvpbot settings debug <value>`, `args[1]` đã là key, `args[2]` nên suggest value boolean/number, không phải danh sách key.

Ảnh hưởng:
- UX command kém, dễ gõ sai.

Khuyến nghị:
- Nếu `args[1]` là setting key, ở args length 3 suggest value phù hợp theo type.
- Nếu `args[1]` là bot name, ở args length 3 mới suggest setting keys.

### 3.4. `plugin.yml` usage chưa cập nhật đủ subcommand

- Mức độ: LOW.
- File: `src/main/resources/plugin.yml`
- Dòng: 11.

Mô tả:
Usage chỉ ghi `/pvpbot <spawn|remove|removeall> [name]`, trong khi command hiện có `settings` và `gui`.

Ảnh hưởng:
- Người dùng nhận hướng dẫn thiếu khi command trả usage.

Khuyến nghị:
- Cập nhật usage: `/pvpbot <spawn|remove|removeall|settings|gui> [args]`.

## 4. GUI và event handling

### 4.1. `onInventoryClick` cancel toàn bộ click của người có GUI đang mở, kể cả click ngoài top inventory

- Mức độ: LOW-MEDIUM.
- File: `src/main/java/com/pvpbot/gui/SettingsGUI.java`
- Dòng: 207-214.

Mô tả:
`e.setCancelled(true)` chạy trước khi kiểm tra click thuộc top inventory. Khi người chơi đang có GUI trong `openGUIs`, click ở bottom inventory cũng bị cancel.

Ảnh hưởng:
- Ngăn item theft tốt, nhưng có thể gây UX khó chịu vì mọi thao tác inventory cá nhân bị khóa khi GUI mở.

Khuyến nghị:
- Nếu muốn chặn tuyệt đối thì giữ nguyên.
- Nếu muốn UX tốt hơn, chỉ cancel top inventory và các thao tác shift/hotbar liên quan đến top inventory.

### 4.2. `onInventoryDrag` cancel mọi drag trên toàn server nếu event listener nhận được

- Mức độ: MEDIUM.
- File: `src/main/java/com/pvpbot/gui/SettingsGUI.java`
- Dòng: 226-229.

Mô tả:
`onInventoryDrag()` gọi `e.setCancelled(true)` mà không kiểm tra người chơi có đang mở PvPBot GUI hay không.

Ảnh hưởng:
- Có thể chặn drag item trong mọi inventory của mọi người chơi trên server, kể cả không liên quan PvPBot.

Khuyến nghị:
- Thêm guard:
  - `if (!openGUIs.containsKey(e.getWhoClicked().getUniqueId())) return;`
  - Chỉ cancel khi drag chạm top inventory của GUI PvPBot.

### 4.3. `openGUIs` dùng `HashMap` thường

- Mức độ: LOW.
- File: `src/main/java/com/pvpbot/gui/SettingsGUI.java`
- Dòng: 27.

Mô tả:
Paper inventory events chạy main thread nên hiện ổn. Nhưng nếu sau này mở GUI từ async task, `HashMap` không thread-safe.

Ảnh hưởng:
- Rủi ro thấp hiện tại, nhưng dễ lỗi nếu mở rộng async.

Khuyến nghị:
- Giữ toàn bộ GUI operation trên main thread, hoặc dùng `ConcurrentHashMap` nếu có async access.

### 4.4. GUI reload toàn bộ submenu sau mỗi click

- Mức độ: LOW.
- File: `src/main/java/com/pvpbot/gui/SettingsGUI.java`
- Dòng: 294-297.

Mô tả:
Sau mỗi thay đổi setting, code schedule mở lại submenu. Cách này đơn giản nhưng có thể gây flicker và spam inventory open packets khi admin click nhanh.

Ảnh hưởng:
- Không phải lỗi chức năng, nhưng ảnh hưởng trải nghiệm và hiệu năng nhẹ.

Khuyến nghị:
- Cập nhật item tại slot hiện tại thay vì mở lại toàn bộ inventory.

## 5. BotSettings và cấu hình

### 5.1. Quan hệ ranged min/optimal/max không được validate

- Mức độ: MEDIUM.
- File: `src/main/java/com/pvpbot/bot/BotSettings.java`
- Dòng: 274-276, 424-426.

Mô tả:
Từng giá trị được clamp 1.0-50.0, nhưng không đảm bảo `ranged-min-range <= ranged-optimal-range <= ranged-max-range`.

Ảnh hưởng:
- Khi logic combat/ranged được triển khai, bot có thể chọn khoảng cách sai hoặc oscillation nếu min > optimal/max.

Khuyến nghị:
- Validate quan hệ sau khi load/set.
- Khi set một giá trị, điều chỉnh các giá trị còn lại hoặc từ chối input không hợp lệ.

### 5.2. `move-speed` cho phép 0.0

- Mức độ: LOW-MEDIUM.
- File: `src/main/java/com/pvpbot/bot/BotSettings.java`
- Dòng: 256, 406.

Mô tả:
`moveSpeed` clamp từ 0.0 đến 10.0. Nếu bot logic dùng trực tiếp, 0.0 có thể làm bot đứng yên dù các setting combat/target bật.

Ảnh hưởng:
- Bot trông như lỗi AI.

Khuyến nghị:
- Nếu 0.0 là intentional "no movement", ghi rõ trong GUI/lore.
- Nếu không, min nên là giá trị dương nhỏ như 0.05 hoặc 0.1.

### 5.3. `SETTING_TYPES` init dư thừa và dễ lệch khi thêm setting

- Mức độ: LOW.
- File: `src/main/java/com/pvpbot/bot/BotSettings.java`
- Dòng: 72-121.

Mô tả:
Map được fill null cho toàn bộ key rồi override từng key. Hiện tất cả 44 key đã có type, nhưng pattern này dễ bỏ sót khi thêm key.

Ảnh hưởng:
- Nếu thêm setting mới mà quên type, GUI/command có thể xử lý như numeric unknown hoặc báo lỗi muộn.

Khuyến nghị:
- Dùng enum/record `SettingDefinition(key, type, default, min, max)` làm nguồn sự thật duy nhất.

## 6. Trait/NMS và performance

### 6.1. `ServerPlayer nmsPlayer` lấy ra nhưng không dùng

- Mức độ: LOW.
- File: `src/main/java/com/pvpbot/npc/PvPBotTrait.java`
- Dòng: 31.

Mô tả:
`craftPlayer.getHandle()` được gọi nhưng biến `nmsPlayer` không được dùng.

Ảnh hưởng:
- Không gây lỗi build, nhưng là code chết và có thể trigger cảnh báo/lệ thuộc NMS không cần thiết.

Khuyến nghị:
- Xóa dòng này nếu chưa dùng.
- Nếu chuẩn bị dùng NMS combat, thêm TODO rõ hoặc implement thật.

### 6.2. Trait log mỗi 100 tick cho mọi bot khi debug không bật

- Mức độ: LOW-MEDIUM.
- File: `src/main/java/com/pvpbot/npc/PvPBotTrait.java`
- Dòng: 34-38.

Mô tả:
Mỗi PvPBotTrait log `"PvPBot Trait ticking..."` cứ 100 tick, không phụ thuộc setting debug.

Ảnh hưởng:
- Nhiều bot sẽ spam console.
- Không đồng nhất với `PvPBotPlugin.broadcastDebug()` đang có debug gate.

Khuyến nghị:
- Chỉ log khi `PvPBotPlugin.getInstance().getDefaultSettings().isDebug()` true.
- Hoặc bỏ log tick trong production.

### 6.3. AI/combat settings phần lớn chưa được dùng

- Mức độ: MEDIUM.
- File: `src/main/java/com/pvpbot/npc/PvPBotTrait.java`
- Dòng: 24-46.
- File: `src/main/java/com/pvpbot/bot/BotSettings.java`
- Dòng: 57-70 và getters/setters tương ứng.

Mô tả:
Nhiều setting như combat, auto-target, criticals, ranged, shield, auto-eat, totem... đã có config/GUI/command nhưng trait hiện chỉ tick/log, chưa có logic AI tương ứng.

Ảnh hưởng:
- Người dùng có thể nghĩ setting đã điều khiển bot thật, nhưng thực tế chưa có tác dụng.
- Rủi ro kỳ vọng sai khi release.

Khuyến nghị:
- Nếu đây là skeleton, ghi rõ trong README/report.
- Nếu mục tiêu là plugin PvP thực, cần implement behavior hoặc ẩn các setting chưa hoạt động.

## 7. Build, CI và release

### 7.1. CI dùng JDK 21 nhưng Gradle yêu cầu toolchain Java 25

- Mức độ: HIGH cho CI.
- File: `build.gradle`
- Dòng: 10-15, 44-47.
- File: `.github/workflows/build.yml`
- Dòng: 14-18.
- File: `.github/workflows/release.yml`
- Dòng: 42-46.

Mô tả:
`build.gradle` yêu cầu `JavaLanguageVersion.of(25)`, nhưng GitHub Actions setup JDK 21. Nếu runner không có Java 25 và Gradle không được cấu hình toolchain download repository, CI có thể fail tìm toolchain.

Ảnh hưởng:
- Local build pass vì máy hiện có Java/Gradle cache phù hợp.
- CI/release có nguy cơ fail dù source code đúng.

Khuyến nghị:
- Hoặc đổi workflow sang `java-version: '25'`.
- Hoặc đổi Gradle toolchain về 21 nếu project thật sự chỉ target Java 21 (`options.release.set(21)`).

### 7.2. `build.sh` hardcode `JAVA_HOME=/usr/lib/jvm/java-25-openjdk`

- Mức độ: MEDIUM.
- File: `build.sh`
- Dòng: 24-26.

Mô tả:
Script build ép `JAVA_HOME` vào path cụ thể chỉ đúng trên một số máy.

Ảnh hưởng:
- Máy khác/CI có thể fail dù Gradle wrapper hoạt động.

Khuyến nghị:
- Không hardcode `JAVA_HOME`, hoặc kiểm tra path tồn tại trước khi export.
- Dựa vào Gradle toolchain thay vì shell env.

### 7.3. `.gradle/` đang bị track trong git

- Mức độ: HIGH cho repo hygiene.
- File: `.gitignore`
- Dòng: 1-13.
- Git tracked files: nhiều file dưới `.gradle/8.10`, `.gradle/9.0.0`, `.gradle/caches/...`.

Mô tả:
`.gitignore` chưa ignore `.gradle/`, và git đang track cache/lock/binary của Gradle. `git status` hiện có nhiều modified file trong `.gradle`.

Ảnh hưởng:
- Repo phình to vì cache/binary.
- Dễ conflict giữa máy khác nhau.
- Build local làm dirty worktree liên tục.

Khuyến nghị:
- Thêm `.gradle/` vào `.gitignore`.
- Remove khỏi index bằng `git rm -r --cached .gradle` sau khi xác nhận.

### 7.4. `build.sh` cleanup xóa report build của chính Gradle

- Mức độ: LOW-MEDIUM.
- File: `build.sh`
- Dòng: 160-171.

Mô tả:
Script xóa `build/reports`, trong khi Gradle tạo report lỗi/cảnh báo ở `build/reports/problems/problems-report.html`.

Ảnh hưởng:
- Khi build fail hoặc có warning, script có thể làm mất dữ liệu chẩn đoán sau build thành công.

Khuyến nghị:
- Không xóa `build/reports`.
- Nếu muốn cleanup artifact nặng, chỉ xóa thư mục tạm không chứa report.

### 7.5. `push.sh` stage toàn bộ repo bằng `git add .`

- Mức độ: HIGH.
- File: `push.sh`
- Dòng: 14-15.

Mô tả:
Script push tự động stage mọi file trong repo, bao gồm cache `.gradle`, session json, report tạm, file nhạy cảm nếu có.

Ảnh hưởng:
- Dễ commit nhầm file không liên quan hoặc dữ liệu cá nhân/session.

Khuyến nghị:
- Không dùng `git add .` trong script publish.
- Chỉ add path cần thiết hoặc yêu cầu review `git status` trước commit.

### 7.6. `release.sh` sửa trực tiếp `plugin.yml` bằng regex rộng

- Mức độ: MEDIUM.
- File: `release.sh`
- Dòng: 90-96.
- File: `.github/workflows/release.yml`
- Dòng: 54-64.

Mô tả:
Regex `s/version:.*/.../g` thay mọi dòng có `version:` trong `plugin.yml`. Hiện file chỉ có một dòng version nên ổn, nhưng nếu sau này có field khác chứa `version:`, script sẽ sửa nhầm. Ngoài ra local release commit trực tiếp thay `plugin.yml`, trong khi build.gradle đã có resource expansion `${version}`.

Ảnh hưởng:
- Có thể làm lệch source `plugin.yml` khỏi template `${version}`.
- Release local và CI có thể hành xử khác nhau.

Khuyến nghị:
- Giữ `plugin.yml` là template `version: ${version}`.
- Release nên update `gradle.properties` version hoặc dùng `-Pversion=...`, không sed resource template.

## 8. Security và dữ liệu nhạy cảm

### 8.1. Session IDs được hardcode và session json được track

- Mức độ: HIGH nếu ID/session có giá trị thật.
- File: `opencode-session.sh`
- Dòng: 3-8, 21-27, 35-46.
- File tracked: `ses-debug.json`, `ses-coding.json`.

Mô tả:
Script chứa session IDs cụ thể và repo đang track các file session JSON.

Ảnh hưởng:
- Có thể lộ metadata hoặc dữ liệu phiên làm việc nếu repo public/shared.
- `push.sh` có thể commit cập nhật session ngoài ý muốn.

Khuyến nghị:
- Đưa `ses-*.json` vào `.gitignore` nếu không cần version control.
- Đọc IDs từ env var hoặc file local ignored.

### 8.2. `sign.py` tin vào chuỗi `"100%"` trong `report.md`

- Mức độ: MEDIUM.
- File: `sign.py`
- Dòng: 46-55.

Mô tả:
Script phê duyệt chỉ kiểm tra content có `"100%"` hoặc một chuỗi tiếng Việt cụ thể. Bất kỳ report nào có `"100%"` cũng pass điều kiện.

Ảnh hưởng:
- Cơ chế ký duyệt không có giá trị bảo đảm kỹ thuật.
- Dễ tạo false positive.

Khuyến nghị:
- Nếu cần sign-off thật, kiểm tra kết quả build/test machine-readable, ví dụ file JSON do CI tạo.
- Không dựa vào text tự do trong markdown.

## 9. Tài liệu và báo cáo cũ

### 9.1. `report.md` chứa nhận định cũ không còn đúng

- Mức độ: MEDIUM.
- File: `report.md`
- Các đoạn trong diff hiện tại nói còn lỗi duplicate `@Override` và build failed.

Mô tả:
`report.md` đang ghi `PvPBotCommand.java:241-242` có duplicate `@Override` gây compile failure. Mã hiện tại tại dòng 241-242 chỉ có một `@Override` trước `onTabComplete`, và `./gradlew build` PASS.

Ảnh hưởng:
- Người đọc report cũ có thể hiểu sai trạng thái dự án.
- `sign.py` lại dựa trên `report.md`, nên càng dễ ký duyệt sai.

Khuyến nghị:
- Cập nhật hoặc archive `report.md`.
- Dùng `codex-report.md` hiện tại làm báo cáo mới sau scan.

## 10. Trạng thái git đáng chú ý

- Worktree đang dirty trước khi tôi ghi báo cáo:
  - Modified: nhiều file `.gradle/...`
  - Modified: `report.md`
  - Modified: `src/main/java/com/pvpbot/PvPBotPlugin.java`
  - Modified: `src/main/java/com/pvpbot/bot/BotManager.java`
  - Modified: `src/main/java/com/pvpbot/command/PvPBotCommand.java`
  - Untracked: `codex-report.md`
- Tôi không revert các thay đổi sẵn có.
- Việc chạy Gradle có thể tiếp tục làm `.gradle` dirty vì cache đang bị track.

## 11. Ưu tiên sửa đề xuất

1. Sửa null settings sau Citizens reload:
   - `BotManager.reloadRegistry()`
   - `SettingsGUI.getSettings()`
   - `PvPBotCommand.handleSettings()`
2. Sửa `onInventoryDrag()` để chỉ cancel khi người chơi đang mở PvPBot GUI.
3. Đồng bộ Java version giữa `build.gradle`, `build.sh`, GitHub Actions.
4. Bỏ `.gradle/` khỏi git và thêm vào `.gitignore`.
5. Sửa boolean parser và canonical key cho command settings.
6. Cập nhật `plugin.yml` usage và `report.md`.
7. Quyết định rõ các setting AI nào đã hoạt động; ẩn hoặc implement phần chưa có logic.

## 12. Kết luận

Dự án hiện build được và không có lỗi cú pháp Java. Lỗi đáng chú ý nhất là các rủi ro runtime sau restart Citizens, GUI drag đang cancel quá rộng, CI/release lệch Java version, và repo hygiene do `.gradle` bị track. Ngoài ra plugin đang có nhiều setting PvP/AI nhưng trait hiện chưa thực thi combat logic tương ứng, nên đây là rủi ro chức năng lớn nếu mục tiêu là bot PvP hoàn chỉnh.
