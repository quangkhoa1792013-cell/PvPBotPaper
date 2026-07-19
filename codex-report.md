┌────────────────────────────────────────────────────────┐
│              PvPBot — Codex Deep Audit Report          │
│        Scope: /tp, /kick, autocomplete, entity lookup   │
└────────────────────────────────────────────────────────┘

==========================================================
KẾT LUẬN NGẮN GỌN
==========================================================
- Lỗi chính: code đang kỳ vọng Citizens NPC xuất hiện trong tablist thì vanilla `/tp <tên_bot>` và `/kick <tên_bot>` sẽ resolve được như player/entity thật. Kỳ vọng này sai.
- `REMOVE_FROM_TABLIST=false` và `REMOVE_FROM_PLAYERLIST=false` chỉ giúp client nhìn thấy tên trong player list/tablist packet. Nó không đảm bảo Paper/Brigadier/vanilla command resolver coi Citizens NPC là online player hoặc command entity target hợp lệ.
- Vì vậy hiện tượng game gợi ý tên bot nhưng khi chạy lệnh lại báo không có entity là hợp lý với code hiện tại.
- Không chạy build theo luật A2/AGENTS.md. Báo cáo này là static audit từ source, metadata jar và cấu hình.

==========================================================
LỖI CỰC MẠNH / BLOCKER
==========================================================

1. Vanilla `/tp` và `/kick` không được tích hợp với registry bot của plugin
- Mức độ: BLOCKER
- File: src/main/java/com/khoablabla/pvpbot/commands/PvPBotCommand.java
- Dòng: 117-127
- Code liên quan:
  - Dòng 121 tạo NPC bằng `CitizensAPI.getNPCRegistry().createNPC(EntityType.PLAYER, name)`.
  - Dòng 122-124 set metadata playerlist/tablist/nameplate.
  - Không có code nào đăng ký bot vào Bukkit online player list theo cách vanilla `/kick` dùng.
  - Không có command riêng `/pvpbot tp <name>` hoặc `/pvpbot remove <name>` để resolve qua `CitizensAPI.getNPCRegistry()`.
- Ảnh hưởng:
  - `/tp <tên_bot>` có thể autocomplete tên do client/player-list cache, nhưng khi execute thì Paper/Minecraft command parser không tìm thấy entity target.
  - `/kick <tên_bot>` càng chắc chắn lỗi vì `/kick` là lệnh xử lý online player thật, không phải Citizens NPC registry.
- Hệ quả:
  - Test case trong `report.md` đang kết luận sai: "Dùng `/tp <tên_bot>` để teleport đến bot" chưa được đảm bảo bởi implementation.
- Fix đúng:
  - Thêm command plugin-owned, ví dụ `/pvpbot tp <botName>` để tìm NPC bằng Citizens registry rồi teleport người chơi đến `npc.getEntity().getLocation()`.
  - Thêm `/pvpbot remove <botName>` hoặc `/pvpbot kick <botName>` để destroy NPC theo tên, không dựa vào vanilla `/kick`.
  - Nếu bắt buộc muốn dùng đúng chuỗi `/tp <botName>` hoặc `/kick <botName>`, phải intercept `PlayerCommandPreprocessEvent`/command map hoặc custom command override rất cẩn thận. Cách này rủi ro hơn vì đụng lệnh vanilla/server/admin plugin khác.

2. Spawn báo thành công dù Citizens spawn thật sự thất bại
- Mức độ: HIGH
- File: src/main/java/com/khoablabla/pvpbot/commands/PvPBotCommand.java
- Dòng: 126-127
- Lỗi:
  - `npc.spawn(safeLocation);`
  - `return true;`
- Ảnh hưởng:
  - Nếu Citizens spawn fail, plugin vẫn broadcast "joined the game" và vẫn báo "Spawned PvPBot".
  - Người chơi thấy tên/message nhưng entity không tồn tại trong world, sau đó `/tp` hoặc các thao tác target báo không có entity.
- Fix đúng:
  - Lưu kết quả `boolean spawned = npc.spawn(safeLocation);`
  - Nếu `spawned == false` thì destroy/cleanup NPC vừa tạo và trả false.
  - Sau spawn phải kiểm tra `npc.isSpawned()` và `npc.getEntity() != null`.

3. Không set metadata DAMAGE_BY_PLAYER theo luật AGENTS.md
- Mức độ: HIGH
- File: src/main/java/com/khoablabla/pvpbot/traits/PvPBotTrait.java
- Dòng: 37-39
- Lỗi:
  - Có `npc.setProtected(false);`
  - Có `npc.data().set(NPC.Metadata.DAMAGE_OTHERS, true);`
  - Thiếu `npc.data().set(NPC.Metadata.DAMAGE_BY_PLAYER, true);` theo AGENTS.md dòng 45-47.
- Ghi chú quan trọng:
  - Trong `libs/citizens.jar` hiện tại, `javap` trên `NPC$Metadata` không thấy enum `DAMAGE_BY_PLAYER`; chỉ thấy `DAMAGE_OTHERS`, `DEFAULT_PROTECTED`, v.v.
  - Nghĩa là instruction AGENTS.md có thể đang lệch với Citizens jar hiện tại, hoặc cần cơ chế khác của Citizens version này để cho phép player đánh NPC.
- Ảnh hưởng:
  - Bot có thể vẫn không nhận sát thương đúng như yêu cầu PvP.
  - Nếu A2 thêm y nguyên constant này với jar hiện tại, khả năng cao sẽ không compile.
- Fix đúng:
  - A3 cần xác minh Citizens API version thực tế.
  - Nếu không có `DAMAGE_BY_PLAYER`, dùng API đúng của Citizens version đang cài để cho NPC nhận damage, hoặc cập nhật Citizens dependency cho khớp tiêu chuẩn dự án.

4. `NPCDeathEvent` có nguy cơ NullPointerException khi entity đã null
- Mức độ: MEDIUM
- File: src/main/java/com/khoablabla/pvpbot/listeners/PlayerSimulationListener.java
- Dòng: 62-64
- Lỗi:
  - Nếu `npc.getStoredLocation()` trả null, code gọi `npc.getEntity().getLocation()`.
  - Trong death/despawn timing, `npc.getEntity()` có thể null.
- Ảnh hưởng:
  - Respawn loop có thể crash event handler, bot chết xong không respawn.
  - Console có thể warning/error không nằm trong bug list ban đầu.
- Fix đúng:
  - Guard `npc.getEntity() != null` trước khi lấy location.
  - Fallback sang death entity location từ `event.getEvent().getEntity().getLocation()`.

5. Root `plugin.yml` thiếu khai báo command, dễ gây nhầm artifact/deploy
- Mức độ: WARN
- File: plugin.yml
- Dòng: 1-6
- Lỗi:
  - Root `plugin.yml` chỉ có name/version/api/main/depend/softdepend, không có `commands:`.
  - `src/main/resources/plugin.yml` dòng 8-12 thì có khai báo command đúng.
- Kiểm tra artifact:
  - `build/libs/PvPBotPaper-1.0.0.jar` đang chứa `plugin.yml` đúng từ `src/main/resources`, có command.
- Ảnh hưởng:
  - Nếu ai copy nhầm root `plugin.yml`, hoặc build script/custom packaging khác lấy file root, `/pvpbot` sẽ không đăng ký.
  - Không phải nguyên nhân trực tiếp nếu server đang chạy jar hiện tại trong `build/libs`.
- Fix đúng:
  - Xóa root `plugin.yml` nếu không dùng, hoặc đồng bộ nội dung với `src/main/resources/plugin.yml`.

6. `report.md` hiện đang đánh dấu PASS cho behavior chưa được code bảo đảm
- Mức độ: WARN
- File: report.md
- Dòng: 45, 74-75, 107, 122
- Lỗi:
  - Báo cáo nói tablist fix làm `/tp <tab>` hoạt động.
  - Source không có implementation đảm bảo vanilla `/tp`/`/kick` resolve Citizens NPC.
- Ảnh hưởng:
  - QA/UAT bị sai hướng: thấy autocomplete là tưởng command execute sẽ pass.
- Fix đúng:
  - A3 cần cập nhật lại test case: phân biệt "autocomplete/tablist hiển thị tên" với "command execution resolve target".

7. `spawnSingleBot` không validate tên theo giới hạn Minecraft/Citizens trước khi tạo NPC
- Mức độ: WARN
- File: src/main/java/com/khoablabla/pvpbot/commands/PvPBotCommand.java
- Dòng: 80-92, 97-111, 117-127
- Lỗi:
  - Chỉ chặn `<` và `>`.
  - Không giới hạn ký tự, độ dài, khoảng trắng, ký tự màu, selector-like token.
- Ảnh hưởng:
  - Một số tên có thể hiện trong message/tab theo kiểu khác với tên command resolver cần.
  - Có thể làm autocomplete/lookup lệch, nhất là khi thêm command resolve theo tên sau này.
- Fix đúng:
  - Chuẩn hóa regex tên bot, ví dụ `[A-Za-z0-9_]{1,16}` nếu muốn giống player name.

==========================================================
NGUYÊN NHÂN GỐC CỦA BUG /tp VÀ /kick
==========================================================
- Code chỉ tạo Citizens NPC và set metadata hiển thị:
  - `REMOVE_FROM_PLAYERLIST=false`
  - `REMOVE_FROM_TABLIST=false`
  - `NAMEPLATE_VISIBLE=true`
- Các metadata trên là hiển thị/client list behavior, không phải contract cho vanilla command target resolution.
- Vanilla `/kick` target online server players thật.
- Vanilla `/tp <name>` dùng command target resolver của server, không dùng trực tiếp registry `CitizensAPI.getNPCRegistry()`.
- Plugin hiện không có bridge từ tên bot sang Citizens NPC cho các command này.

==========================================================
HƯỚNG SỬA KHUYẾN NGHỊ
==========================================================
Ưu tiên 1:
- Thêm `/pvpbot tp <botName>`.
- Tab-complete bot names cho subcommand `tp`.
- Resolve bằng:
  - lặp `CitizensAPI.getNPCRegistry()`
  - check `npc.hasTrait(PvPBotTrait.class)`
  - check `npc.isSpawned()`
  - check `npc.getEntity() != null`
  - so sánh `npc.getName().equalsIgnoreCase(botName)`
  - teleport player tới `npc.getEntity().getLocation()`

Ưu tiên 2:
- Thêm `/pvpbot remove <botName>` hoặc `/pvpbot kick <botName>`.
- Không dùng vanilla `/kick` cho Citizens bot.

Ưu tiên 3:
- Sửa `spawnSingleBot` để chỉ báo thành công khi `npc.spawn(...)` thật sự trả true.

Ưu tiên 4:
- Sửa respawn location null guard trong `PlayerSimulationListener`.

==========================================================
TRẠNG THÁI KIỂM TRA
==========================================================
- Đã đọc toàn bộ file source trong `src/main/java`.
- Đã đọc `src/main/resources/plugin.yml`, root `plugin.yml`, `build.gradle`, `README.md`, `AGENTS.md`.
- Đã kiểm tra jar hiện tại `build/libs/PvPBotPaper-1.0.0.jar` có `plugin.yml` command đúng.
- Đã kiểm tra `libs/citizens.jar` qua `javap` cho `NPC` và `NPC$Metadata`.
- Không chạy `./build.sh`, `./gradlew build`, `./gradlew test`, `./gradlew compileJava` theo luật dự án.

==========================================================
TÓM TẮT FILE / DÒNG CÓ LỖI
==========================================================
- src/main/java/com/khoablabla/pvpbot/commands/PvPBotCommand.java: 117-127
  - Không bridge vanilla command target với Citizens NPC.
  - Bỏ qua return value của `npc.spawn`.
- src/main/java/com/khoablabla/pvpbot/commands/PvPBotCommand.java: 192-200
  - TabCompleter chỉ cho `/pvpbot`, không có bot-name completion cho command quản trị bot theo tên.
- src/main/java/com/khoablabla/pvpbot/traits/PvPBotTrait.java: 37-39
  - Thiếu cơ chế rõ ràng cho player damage theo tiêu chuẩn AGENTS.md; Citizens jar hiện tại không có `DAMAGE_BY_PLAYER`.
- src/main/java/com/khoablabla/pvpbot/listeners/PlayerSimulationListener.java: 62-64
  - Có thể NPE khi entity null lúc xử lý death/respawn.
- plugin.yml: 1-6
  - Root metadata thiếu commands, WARN nếu deploy nhầm.
- report.md: 45, 74-75, 107, 122
  - Test/report kết luận quá mức so với code thật.
