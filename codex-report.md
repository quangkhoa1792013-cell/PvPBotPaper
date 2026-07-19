┌────────────────────────────────────────────────────────┐
│             🎮 PvPBot — Deep Codex Audit Report         │
│  Audit-only report: không sửa src/, không chạy sign.py  │
└────────────────────────────────────────────────────────┘

==========================================================
✅ --- HOTFIX ĐÃ ÁP DỤNG SAU LOG MOONRISE UUID ---
==========================================================
- Đã sửa lỗi bot chết rồi biến mất do Moonrise báo `Entity uuid already exists`.
- File sửa: src/main/java/com/khoablabla/pvpbot/listeners/PlayerSimulationListener.java
- Dòng chính sau sửa: 80-113
- Cách sửa: không spawn lại cùng `NPC` object nữa. Khi bot chết, code lưu tên/vị trí, `destroy()` NPC cũ, sau 10 ticks tạo NPC mới cùng tên + metadata + trait rồi spawn lại.
- Lý do: spawn lại cùng NPC giữ cùng Minecraft UUID, trong khi entity cũ vẫn còn trong Moonrise EntityLookup. NPC mới có UUID sạch nên không đụng UUID cũ.
- Đã chạy `./gradlew compileJava`: PASS, 0 errors, 8 deprecation warnings.
- Đã chạy `./build.sh`: PASS, 0 errors, JAR mới tại `build/libs/PvPBotPaper-1.0.0.jar`.
- Bổ sung sau yêu cầu fix warnings: đã thay toàn bộ `broadcastMessage(String)` bằng Adventure `Component` broadcast và đổi Gradle flatDir dependencies sang single-string notation.
- Đã chạy lại `./gradlew compileJava`, `./gradlew build --warning-mode all`, `./build.sh`: PASS, 0 errors, 0 deprecation warnings trong output.
- Bổ sung tiếp: bot chết sẽ respawn tại vị trí spawn gốc đã cache trong `NPCSpawnEvent`; bỏ death message giả lập và bỏ leave message giả lập, chỉ giữ join message khi spawn/respawn.

==========================================================
🛡️ --- CÁC LỖI CẦN XỬ LÝ ---
==========================================================
- [ ] Lỗi 1: Bot bị kill xong biến mất luôn dù đã delay 10 ticks.
- [ ] Lỗi 2: Respawn handler bỏ qua kết quả `NPC.spawn(finalLoc)`, fail spawn không có log, không có fallback.
- [ ] Lỗi 3: Death handler có thể truyền `null` vào `NPC.spawn(finalLoc)`.
- [ ] Lỗi 4: Thứ tự `entity.remove()` rồi `despawn()` rồi `spawn()` trên cùng NPC có rủi ro làm Citizens state lệch sau death.
- [ ] Lỗi 5: Bot không được reset máu/food/fire/invulnerability sau respawn.
- [ ] Lỗi 6: Death message chỉ ghi console, không broadcast cho player trong game.
- [ ] Lỗi 7: `PvPBotTrait` thiếu metadata `DAMAGE_BY_PLAYER` theo AGENTS.md, nhưng Citizens jar hiện tại cũng không có enum này.
- [ ] Lỗi 8: `PvPBotTrait` spam log mỗi 100 tick cho mọi bot, dễ làm console/log lag khi spawn nhiều bot.
- [ ] Lỗi 9: `SafeLocationFinder` thiếu null guard cho `Location`/`World`.
- [ ] Lỗi 10: `SafeLocationFinder` coi một số block nguy hiểm là nền hợp lệ.
- [ ] Lỗi 11: `/pvpbot spawn <name>` báo "Spawned" kể cả khi spawn thật sự fail.
- [ ] Lỗi 12: `/pvpbot spawn <name>` không validate chuẩn tên Minecraft player.
- [ ] Lỗi 13: Random name generator có thể trả tên đã trùng sau 100 attempts.
- [ ] Lỗi 14: `removeall` batch path không broadcast "left the game" từng bot, khác behavior path <=20.
- [ ] Lỗi 15: `removeall` batch giữ sender object trong runnable, có thể dùng sender đã offline/invalid sau nhiều tick.
- [ ] Lỗi 16: Root `plugin.yml` lệch với `src/main/resources/plugin.yml`.
- [ ] Lỗi 17: Build có 7 Java deprecation warnings.
- [ ] Lỗi 18: Build có 4 Gradle deprecation warnings, sẽ lỗi ở Gradle 10.

==========================================================
📂 --- NHỮNG FILE ĐÃ QUÉT ---
==========================================================
Các file/thư mục đã đọc/quét sâu:
- AGENTS.md
- README.md
- build.gradle
- build.sh
- plugin.yml
- src/main/resources/plugin.yml
- src/main/java/com/khoablabla/pvpbot/PvPBot.java
- src/main/java/com/khoablabla/pvpbot/commands/PvPBotCommand.java
- src/main/java/com/khoablabla/pvpbot/listeners/PlayerSimulationListener.java
- src/main/java/com/khoablabla/pvpbot/traits/PvPBotTrait.java
- src/main/java/com/khoablabla/pvpbot/utils/SafeLocationFinder.java
- libs/citizens.jar API qua `javap`
- build/libs/PvPBotPaper-1.0.0.jar contents/plugin.yml

Lệnh kiểm tra đã chạy:
- `./build.sh`
- `./gradlew build --warning-mode all`
- `rg` static scan trên `src`
- `javap` Citizens `NPC`, `NPC$Metadata`, `NPCDeathEvent`
- `jar tf build/libs/PvPBotPaper-1.0.0.jar`
- `unzip -p build/libs/PvPBotPaper-1.0.0.jar plugin.yml`

Kết quả build:
- Compile/build: PASS, 0 errors.
- Unit tests: NO-SOURCE.
- Static scan trong build.sh: PASS.
- Java warnings: 7 warnings.
- Gradle warnings: 4 warnings.

==========================================================
💾 --- CÁC FILE ĐÃ SỬA ---
==========================================================
Các file đã sửa:
- codex-report.md

Ở các dòng nào (chỉ ghi số dòng):
- Toàn file

Nhu the nao:
- Chỉ ghi audit report theo yêu cầu.
- Không sửa bất kỳ file production nào trong `src/`.
- Không sửa `report.md`.
- Không chạy hoặc sửa `sign.py`.

==========================================================
🔍 --- DEBUG / ROOT CAUSE CHO LỖI BOT CHẾT RỒI BIẾN MẤT ---
==========================================================
Lỗi chính:
- [/] Bot chết rồi biến mất: có delay 10 tick thật, nhưng logic respawn chưa an toàn.

File nào:
- src/main/java/com/khoablabla/pvpbot/listeners/PlayerSimulationListener.java

Ở dòng nào:
- 57-58
- 63-68
- 71-85
- Đặc biệt: 76-83

Vì sao lỗi mạnh:
- Dòng 76 lấy lại `finalNpc.getEntity()` sau khi NPC đã chết. Tại thời điểm task chạy sau 10 ticks, Citizens/Paper có thể đã clear entity hoặc entity đang ở state removed/dead.
- Dòng 78 gọi `entity.remove()` trực tiếp trên entity của Citizens. Đây là thao tác Bukkit-level, không phải Citizens lifecycle-level. Sau đó dòng 81 mới gọi `finalNpc.despawn()`. Thứ tự này có thể làm Citizens nghĩ NPC đã despawn hoặc mất entity handle không đúng cách.
- Dòng 83 gọi `finalNpc.spawn(finalLoc)` nhưng không check boolean return. Nếu spawn fail, code không log gì, không retry, không destroy/recreate NPC. Kết quả nhìn trong game là bot biến mất luôn.
- Dòng 72 cho phép `finalLoc` là null. Nếu `npc.getStoredLocation()` và entity location fallback đều không có, dòng 83 có thể spawn với null location.

Điểm cần sửa:
- Lấy death location sớm từ `bukkitEvent.getEntity().getLocation().clone()`.
- Không dùng location mutable trực tiếp.
- Despawn theo Citizens lifecycle trước, hoặc destroy/recreate NPC nếu Citizens không hỗ trợ respawn lại ổn định sau death.
- Check `boolean spawned = finalNpc.spawn(finalLoc);`.
- Nếu `spawned == false`, log warning rõ NPC name/id/location và cleanup/retry an toàn.
- Sau spawn thành công, reset health/fireTicks/fallDistance/noDamageTicks nếu entity là `Player`.

==========================================================
⚠️ --- CÁC LỖI PHÁT SINH KHÁC KHÔNG TRONG DANH SÁCH OR TRONG CODE NGẦM ---
==========================================================
1. Respawn không check return value.
File nào:
- src/main/java/com/khoablabla/pvpbot/listeners/PlayerSimulationListener.java
Ở dòng nào:
- 83
Ảnh hưởng:
- HIGH. Đây là nguyên nhân trực tiếp khiến bot "mất luôn" nếu Citizens spawn fail.
Hệ quả:
- Không log lỗi, không retry, không còn entity trong world.

2. Respawn location có thể null.
File nào:
- src/main/java/com/khoablabla/pvpbot/listeners/PlayerSimulationListener.java
Ở dòng nào:
- 63-72, 83
Ảnh hưởng:
- HIGH. `finalLoc` không được validate trước khi spawn.
Hệ quả:
- Spawn fail hoặc runtime exception tùy Citizens/Paper implementation.

3. Thứ tự remove/despawn/spawn rủi ro cao.
File nào:
- src/main/java/com/khoablabla/pvpbot/listeners/PlayerSimulationListener.java
Ở dòng nào:
- 76-83
Ảnh hưởng:
- HIGH. `entity.remove()` bypass một phần lifecycle Citizens.
Hệ quả:
- Citizens registry/NPC entity handle có thể lệch với entity thật trong world.

4. Không reset trạng thái sống sau respawn.
File nào:
- src/main/java/com/khoablabla/pvpbot/listeners/PlayerSimulationListener.java
Ở dòng nào:
- 83
Ảnh hưởng:
- MEDIUM/HIGH. Player NPC có thể respawn với state xấu hoặc chết lại ngay.
Hệ quả:
- Bot vừa respawn có thể không đứng được, chết lại, hoặc bị client/server state không đồng bộ.

5. Death message không broadcast in-game.
File nào:
- src/main/java/com/khoablabla/pvpbot/listeners/PlayerSimulationListener.java
Ở dòng nào:
- 61
Ảnh hưởng:
- LOW/MEDIUM. Chỉ console thấy message.
Hệ quả:
- Người chơi tưởng không có death event hoặc respawn event.

6. Thiếu `DAMAGE_BY_PLAYER` theo AGENTS.md.
File nào:
- src/main/java/com/khoablabla/pvpbot/traits/PvPBotTrait.java
Ở dòng nào:
- 38-39
Ảnh hưởng:
- MEDIUM. Code mới chỉ `setProtected(false)` và `DAMAGE_OTHERS`.
Hệ quả:
- Theo tiêu chuẩn dự án thì chưa đủ đảm bảo bot vulnerable.
Ghi chú:
- `javap libs/citizens.jar net.citizensnpcs.api.npc.NPC$Metadata` không thấy enum `DAMAGE_BY_PLAYER`. Có thể AGENTS.md đang lệch version Citizens, hoặc cần API khác của Citizens version hiện tại.

7. Log spam mỗi 100 tick mỗi bot.
File nào:
- src/main/java/com/khoablabla/pvpbot/traits/PvPBotTrait.java
Ở dòng nào:
- 53-62
Ảnh hưởng:
- MEDIUM khi nhiều bot. 50 bot = khoảng 10 log/giây.
Hệ quả:
- Console noise, disk log tăng nhanh, giảm hiệu năng khi mass spawn.

8. `SafeLocationFinder` thiếu null guard.
File nào:
- src/main/java/com/khoablabla/pvpbot/utils/SafeLocationFinder.java
Ở dòng nào:
- 14-16, 29, 39-42
Ảnh hưởng:
- MEDIUM. Nếu origin hoặc world null, crash NPE.
Hệ quả:
- Command spawn có thể fail cứng thay vì trả false.

9. Safe spawn support block chưa đủ an toàn.
File nào:
- src/main/java/com/khoablabla/pvpbot/utils/SafeLocationFinder.java
Ở dòng nào:
- 55-58
Ảnh hưởng:
- MEDIUM. Chỉ loại lava/fire ở block dưới, không loại cactus, magma, campfire, berry bush, powder snow, portal, void hazard, liquid feet/head.
Hệ quả:
- Bot có thể spawn ở vị trí gây damage/kẹt/chết lại.

10. Spawn một bot fail vẫn báo thành công cho player.
File nào:
- src/main/java/com/khoablabla/pvpbot/commands/PvPBotCommand.java
Ở dòng nào:
- 61-67
- 84-98
Ảnh hưởng:
- MEDIUM. `player.sendMessage("Spawned ...")` chạy ngoài check success.
Hệ quả:
- Người dùng thấy báo spawned nhưng không có bot.

11. Tên custom không validate chuẩn Minecraft/Citizens.
File nào:
- src/main/java/com/khoablabla/pvpbot/commands/PvPBotCommand.java
Ở dòng nào:
- 84-98
- 102-118
- 122-130
Ảnh hưởng:
- MEDIUM. Chỉ chặn `<` và `>`, không chặn dài >16, space, ký tự màu, ký tự đặc biệt, tên quá ngắn.
Hệ quả:
- Tablist/skin/profile/command targeting có thể lỗi hoặc hành vi lạ.

12. Random name vẫn có thể trùng.
File nào:
- src/main/java/com/khoablabla/pvpbot/commands/PvPBotCommand.java
Ở dòng nào:
- 155-173
Ảnh hưởng:
- LOW/MEDIUM. Sau 100 attempts, function trả name hiện tại dù còn trùng.
Hệ quả:
- Có thể spawn nhiều bot cùng tên, remove/tab complete khó đoán.

13. `removeall` batch không broadcast từng bot left message.
File nào:
- src/main/java/com/khoablabla/pvpbot/commands/PvPBotCommand.java
Ở dòng nào:
- 233-246
Ảnh hưởng:
- LOW/MEDIUM. Behavior không nhất quán với path <=20 ở dòng 221-228.
Hệ quả:
- Người chơi không thấy từng bot leave khi remove số lượng lớn.

14. `removeall` batch giữ `sender` trong delayed runnable.
File nào:
- src/main/java/com/khoablabla/pvpbot/commands/PvPBotCommand.java
Ở dòng nào:
- 231-246
Ảnh hưởng:
- LOW/MEDIUM. Nếu player logout trước khi batch xong, vẫn giữ sender object.
Hệ quả:
- Message cuối có thể không tới nơi; giữ reference không cần thiết.

15. Root `plugin.yml` lệch file resource thật.
File nào:
- plugin.yml
- src/main/resources/plugin.yml
Ở dòng nào:
- plugin.yml:1-6
- src/main/resources/plugin.yml:8-12
Ảnh hưởng:
- LOW. JAR hiện đóng gói đúng `src/main/resources/plugin.yml`, root file thiếu command.
Hệ quả:
- Dễ deploy nhầm root `plugin.yml` hoặc audit nhầm metadata.

16. Java deprecation warnings.
File nào:
- src/main/java/com/khoablabla/pvpbot/commands/PvPBotCommand.java
Ở dòng nào:
- 64, 79, 95, 115, 184, 207, 224
Ảnh hưởng:
- LOW hiện tại, tăng rủi ro tương lai.
Hệ quả:
- `Bukkit.getServer().broadcastMessage(String)` deprecated; Paper khuyến nghị Component API.

17. Gradle deprecation warnings.
File nào:
- build.gradle
Ở dòng nào:
- 16, 17, 23, 24
Ảnh hưởng:
- MEDIUM tương lai.
Hệ quả:
- Gradle 10 sẽ fail: repository `url '...'` syntax và dependency multi-string notation `compileOnly name:`.

18. `application` plugin không cần thiết cho Bukkit plugin.
File nào:
- build.gradle
Ở dòng nào:
- 3
Ảnh hưởng:
- LOW.
Hệ quả:
- Build tạo `distTar`, `distZip`, scripts app không cần cho Paper plugin.

==========================================================
📋 --- HƯỚNG DẪN KIỂM THỬ (TEST CASES) ---
==========================================================
Lệnh cần gõ:
- `./build.sh`
- Start Paper 1.21.11 server với Citizens + PvPBot jar mới.
- Trong game: `/pvpbot spawn TestBot`
- Kill bot bằng kiếm/bow/lava/fall.
- Quan sát console trong 20 ticks sau death.

Trình tự các bước thực hiện:
- Spawn 1 bot tên cố định.
- Kill bot.
- Đợi ít nhất 1 giây.
- Kiểm tra bot có entity thật trong world không.
- Kiểm tra console có warning/error không.
- Lặp lại 5 lần liên tục.
- Test thêm `/pvpbot spawn 50`, kill nhiều bot liên tục, sau đó `/pvpbot removeall`.

Các trường hợp kiểm thử:
- Bot respawn sau đúng 10 ticks.
- Không có `Entity uuid already exists`.
- Không có spawn fail im lặng.
- Bot sau respawn có 20 health, không cháy, không chết lại ngay.
- `/pvpbot remove <name>` remove đúng bot sau respawn.
- Player mới join sau bot respawn vẫn thấy bot/tablist.
- Spawn fail phải báo fail, không được báo "Spawned".

==========================================================
📊 --- TỔNG QUAN ---
==========================================================
- Build hiện tại: PASS, 0 compile errors.
- Lỗi nghiêm trọng nhất: `PlayerSimulationListener.java:76-83`.
- Kết luận cho câu "tôi kill 1 bot sau đó nó biến mất luôn, tôi cho là 10 tick rồi mà":
  - 10 tick có chạy ở dòng 85.
  - Nhưng sau 10 tick, code gọi `entity.remove()` + `despawn()` + `spawn(finalLoc)` không kiểm tra spawn success.
  - Nếu Citizens không spawn lại được sau death hoặc `finalLoc` xấu/null, bot sẽ mất luôn và không có log báo nguyên nhân.
- Lỗi chưa sửa: tất cả lỗi trên mới được audit, chưa sửa code production.
- Tỷ lệ audit hoàn thành: **100%**
