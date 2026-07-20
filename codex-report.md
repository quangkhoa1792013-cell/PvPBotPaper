┌────────────────────────────────────────────────────────┐
│             🎮 PvPBot — Deep Codex Audit Report         │
│  Audit-only report: không sửa src/, không chạy sign.py  │
└────────────────────────────────────────────────────────┘

==========================================================
🔎 --- PHẠM VI QUÉT ---
==========================================================
Đã đọc/quét:
- AGENTS.md
- README.md
- build.gradle
- settings.gradle
- gradle.properties
- build.sh
- release.sh
- push.sh
- .github/workflows/build.yml
- .github/workflows/release.yml
- src/main/resources/plugin.yml
- src/main/java/com/khoablabla/pvpbot/PvPBot.java
- src/main/java/com/khoablabla/pvpbot/commands/PvPBotCommand.java
- src/main/java/com/khoablabla/pvpbot/listeners/PlayerSimulationListener.java
- src/main/java/com/khoablabla/pvpbot/traits/PvPBotTrait.java
- src/main/java/com/khoablabla/pvpbot/combat/CombatTargetSelector.java
- src/main/java/com/khoablabla/pvpbot/combat/MeleeAttackController.java
- src/main/java/com/khoablabla/pvpbot/movement/BotMovementController.java
- src/main/java/com/khoablabla/pvpbot/utils/SafeLocationFinder.java

Lệnh đã chạy:
- `./build.sh`
- `rg` static scan trên source/scripts
- `find src/test -type f -print`

Kết quả build:
- `./build.sh`: PASS
- Compile: PASS, 0 errors
- Unit tests: NO-SOURCE
- build.sh static scan: 0 warnings
- JAR tạo được: `build/libs/PvPBotPaper-1.0.0.jar`

==========================================================
🛡️ --- CÁC LỖI CẦN XỬ LÝ ---
==========================================================
- [ ] Lỗi 1: Bot tự động đánh người chơi chỉ vì đứng gần, không cần người chơi đánh trước.
- [ ] Lỗi 2: Bot không kiểm tra line-of-sight nên có thể chọn target sau tường/vật cản.
- [ ] Lỗi 3: Không có trạng thái revenge/aggro; bot không biết "ai đánh mình thì mới đánh lại".
- [ ] Lỗi 4: Khi không có target, bot cancel navigation rồi đứng im hoàn toàn, không idle/wander/guard.
- [ ] Lỗi 5: Bot có thể target mọi LivingEntity, gồm mob và PvPBot khác, vì không lọc Citizens NPC/bot.
- [ ] Lỗi 6: Movement gọi `setTarget(target, true)` mỗi tick, dễ làm navigator bị reset liên tục.
- [ ] Lỗi 7: B-hop dùng `setVelocity(new Vector(0, 0.42, 0))`, xóa vận tốc ngang và tạo cảm giác bot bị khựng/đóng băng.
- [ ] Lỗi 8: Critical jump dùng velocity thô trong attack loop, có thể làm bot bay/nhảy theo người chơi khi sát target.
- [ ] Lỗi 9: Attack controller chỉ đánh khi velocity Y âm; nếu trạng thái rơi không xảy ra đúng lúc, bot giữ `jumpTriggered` và không đánh.
- [ ] Lỗi 10: Attack dùng damage số cố định, không tính vũ khí, giáp, enchant, cooldown combat thật.
- [ ] Lỗi 11: Bot không xoay mặt/aim về target trước khi đánh.
- [ ] Lỗi 12: OnSpawn thiếu metadata `DAMAGE_BY_PLAYER` theo AGENTS.md.
- [ ] Lỗi 13: Bot chết được broadcast lại "joined the game", trái yêu cầu chỉ join một lần khi spawn.
- [ ] Lỗi 14: Death handler luôn tạo NPC thay thế sau khi chết, không có setting/policy "bot chết thì không join lại".
- [ ] Lỗi 15: Respawn dùng lại spawn location cũ rồi remove cache theo old NPC id; vòng đời id mới dễ khó quản lý khi bot chết nhiều lần.
- [ ] Lỗi 16: Spawn fail không báo lỗi cho người chơi trong nhiều nhánh command.
- [ ] Lỗi 17: Custom name validation quá yếu, có thể tạo tên dài/sai chuẩn Minecraft.
- [ ] Lỗi 18: SafeLocationFinder cho phép đứng trong một số block nguy hiểm non-solid.
- [ ] Lỗi 19: Project không có unit/integration test; build PASS không chứng minh hành vi trong game.
- [ ] Lỗi 20: Release workflow tìm `*-reobf.jar` nhưng Gradle build hiện không tạo file reobf.
- [ ] Lỗi 21: README/spec mô tả rất nhiều lệnh và hệ thống chưa tồn tại trong code hiện tại.

==========================================================
⚠️ --- CHI TIẾT LỖI THEO FILE / DÒNG ---
==========================================================
1. HIGH - Bot tự đánh khi người chơi đến gần, không cần bị khiêu khích.
File nào:
- src/main/java/com/khoablabla/pvpbot/traits/PvPBotTrait.java
- src/main/java/com/khoablabla/pvpbot/combat/CombatTargetSelector.java
Ở dòng nào:
- PvPBotTrait.java:64-70
- CombatTargetSelector.java:24-40
Ảnh hưởng:
- Bot cứ mỗi 10 tick tự quét entity gần nhất trong 16 block rồi chase/attack.
Hệ quả:
- Đúng triệu chứng "khi tôi đến gần nó đánh"; logic hiện tại là auto-hostile proximity AI, không phải revenge AI.

2. HIGH - Không có revenge/provocation state.
File nào:
- src/main/java/com/khoablabla/pvpbot/listeners/PlayerSimulationListener.java
- src/main/java/com/khoablabla/pvpbot/traits/PvPBotTrait.java
Ở dòng nào:
- PlayerSimulationListener.java:38-123
- PvPBotTrait.java:20-24, 61-75
Ảnh hưởng:
- Không listener nào bắt `EntityDamageByEntityEvent` để nhớ attacker.
- Trait chỉ có `target`, không có `lastDamager`, timeout aggro, hay owner/faction command state.
Hệ quả:
- Không thể làm hành vi "thằng nào đánh mình thì nhắm thằng đó đánh" bằng code hiện tại.

3. HIGH - Không kiểm tra tầm nhìn.
File nào:
- src/main/java/com/khoablabla/pvpbot/combat/CombatTargetSelector.java
Ở dòng nào:
- 24-39
Ảnh hưởng:
- Chỉ dựa vào nearby entities và distanceSquared.
Hệ quả:
- Bot có thể lock/chase target sau tường, dưới/sau block, hoặc nơi không nhìn thấy.

4. MEDIUM - Khi không có target bot đứng im/cancel navigation.
File nào:
- src/main/java/com/khoablabla/pvpbot/traits/PvPBotTrait.java
Ở dòng nào:
- 71-74
Ảnh hưởng:
- Không có idle/wander/path/guard state.
Hệ quả:
- Đúng triệu chứng "không có ai trong tầm mắt thì nó đứng im luôn, không biết gì luôn".

5. HIGH - Bot target cả mob và PvPBot khác.
File nào:
- src/main/java/com/khoablabla/pvpbot/combat/CombatTargetSelector.java
Ở dòng nào:
- 25-30
Ảnh hưởng:
- Dòng 27 trả `true` cho mọi non-player LivingEntity.
- Không dùng `CitizensAPI.getNPCRegistry().isNPC(e)` để loại NPC khác.
Hệ quả:
- PvPBot có thể đánh mob, armor stand living-like entity nếu có, hoặc đánh nhau với bot khác ngoài ý muốn.

6. MEDIUM/HIGH - Navigator bị set target lại mỗi tick.
File nào:
- src/main/java/com/khoablabla/pvpbot/movement/BotMovementController.java
Ở dòng nào:
- 15-19
Ảnh hưởng:
- `npc.getNavigator().setTarget(target, true)` được gọi mỗi tick khi có target.
Hệ quả:
- Citizens pathfinder có thể bị reset liên tục, gây giật, đứng khựng, hoặc khó xử lý stuck state.

7. HIGH - B-hop xóa vận tốc ngang.
File nào:
- src/main/java/com/khoablabla/pvpbot/movement/BotMovementController.java
Ở dòng nào:
- 20-29
Ảnh hưởng:
- `setVelocity(new Vector(0, 0.42, 0))` thay toàn bộ velocity bằng vector chỉ có Y.
Hệ quả:
- Bot đang chạy sẽ mất X/Z velocity, nhìn giống bị đóng băng rồi nhảy lên.

8. HIGH - Critical jump có thể làm bot bay/nhảy bất thường.
File nào:
- src/main/java/com/khoablabla/pvpbot/combat/MeleeAttackController.java
Ở dòng nào:
- 38-47
Ảnh hưởng:
- Khi trong 3 block và hết cooldown, bot tự set velocity Y = 0.42 để crit.
- Không check onGround, liquid, ladder, web, fall distance, jump cooldown vật lý, knockback, hay trạng thái đang navigation.
Hệ quả:
- Đúng triệu chứng "nó đánh rồi nó bay luôn", "tôi nhảy lên nó nhảy theo".

9. MEDIUM/HIGH - Attack có thể bị kẹt `jumpTriggered`.
File nào:
- src/main/java/com/khoablabla/pvpbot/combat/MeleeAttackController.java
Ở dòng nào:
- 14-16, 31-47
Ảnh hưởng:
- Sau khi jump, code chỉ đánh nếu `botPlayer.getVelocity().getY() < 0`.
- Nếu target vẫn trong range nhưng velocity Y không âm theo timing server/Citizens, bot không strike và `jumpTriggered` có thể giữ true.
Hệ quả:
- Bot có thể chỉ nhảy/treo nhịp đánh, gây cảm giác đơ.

10. MEDIUM - Damage cố định, bỏ qua combat thật.
File nào:
- src/main/java/com/khoablabla/pvpbot/combat/MeleeAttackController.java
Ở dòng nào:
- 18-22, 54-60
Ảnh hưởng:
- Damage luôn là 2.0 hoặc 4.0, không dựa vào item, attack cooldown, armor, enchant, potion, shield, axe.
Hệ quả:
- PvP không giống player thật; balance và anti-cheat/event behavior có thể lệch.

11. MEDIUM - Bot không aim/rotate về target trước khi đánh.
File nào:
- src/main/java/com/khoablabla/pvpbot/combat/MeleeAttackController.java
- src/main/java/com/khoablabla/pvpbot/movement/BotMovementController.java
Ở dòng nào:
- MeleeAttackController.java:24-61
- BotMovementController.java:15-30
Ảnh hưởng:
- Không có yaw/pitch/lookAt trước `swingMainHand()` và `damage()`.
Hệ quả:
- Bot có thể đánh trúng dù nhìn hướng khác, rất "ảo".

12. MEDIUM - Thiếu `DAMAGE_BY_PLAYER` theo luật dự án.
File nào:
- src/main/java/com/khoablabla/pvpbot/traits/PvPBotTrait.java
Ở dòng nào:
- 43-47
Ảnh hưởng:
- Code có `npc.setProtected(false)` và `DAMAGE_OTHERS`, nhưng thiếu `npc.data().set(NPC.Metadata.DAMAGE_BY_PLAYER, true);` như AGENTS.md bắt buộc.
Hệ quả:
- Tùy version Citizens, bot có thể không nhận damage từ player đúng như kỳ vọng hoặc lệch chuẩn dự án.

13. HIGH - Bot chết vẫn phát "joined the game".
File nào:
- src/main/java/com/khoablabla/pvpbot/listeners/PlayerSimulationListener.java
Ở dòng nào:
- 91-122, đặc biệt 120
Ảnh hưởng:
- Mỗi lần bot chết và replacement spawn thành công, server broadcast `botName + " joined the game"`.
Hệ quả:
- Trái yêu cầu "khi spawn bot thì 1 lần join game thôi, khi chết không join nữa".

14. HIGH - Death handler luôn respawn/rejoin bằng NPC mới.
File nào:
- src/main/java/com/khoablabla/pvpbot/listeners/PlayerSimulationListener.java
Ở dòng nào:
- 63-123
Ảnh hưởng:
- Không có setting `bot-leave-on-death`, không có policy "death = stop", không phân biệt kill thật với manual remove.
Hệ quả:
- Bot chết luôn được tạo replacement nếu có location hợp lệ.

15. MEDIUM - Cache spawn location theo NPC id dễ lệch vòng đời.
File nào:
- src/main/java/com/khoablabla/pvpbot/listeners/PlayerSimulationListener.java
Ở dòng nào:
- 32, 44, 71, 94-100
Ảnh hưởng:
- Old id bị remove ở dòng 71, replacement có id mới và phụ thuộc `NPCSpawnEvent` để cache lại.
Hệ quả:
- Nếu spawn event không chạy như kỳ vọng hoặc replacement spawn fail, thông tin respawn gốc mất.

16. MEDIUM - Spawn fail im lặng trong command.
File nào:
- src/main/java/com/khoablabla/pvpbot/commands/PvPBotCommand.java
Ở dòng nào:
- 63-69
- 72-85
- 96-99
- 115-120
- 124-137
Ảnh hưởng:
- `spawnSingleBot` trả false nhưng nhiều nhánh không báo rõ nguyên nhân cho player.
Hệ quả:
- Người dùng chỉ thấy không có bot, khó biết do unsafe location hay Citizens spawn fail.

17. MEDIUM - Custom name validation yếu.
File nào:
- src/main/java/com/khoablabla/pvpbot/commands/PvPBotCommand.java
Ở dòng nào:
- 87-95
- 104-114
Ảnh hưởng:
- Chỉ chặn `<` và `>`.
Hệ quả:
- Có thể nhập tên quá dài, có space/ký tự màu/ký tự đặc biệt, gây lỗi tablist/profile/remove name.

18. MEDIUM - Safe location chưa chặn đủ hazardous block ở feet/head.
File nào:
- src/main/java/com/khoablabla/pvpbot/utils/SafeLocationFinder.java
Ở dòng nào:
- 11-15
- 51-58
- 61-68
Ảnh hưởng:
- HAZARDOUS_BLOCKS chỉ check block bên dưới.
- Feet/head chỉ check non-solid và liquid, nên các block nguy hiểm non-solid có thể lọt.
Hệ quả:
- Bot có thể spawn trong fire/soul fire/berry bush/nether portal/powder snow tùy block state, rồi kẹt hoặc chết.

19. MEDIUM - Không có test source.
File nào:
- src/test
- build.gradle
Ở dòng nào:
- build.gradle:24, 32-34
Ảnh hưởng:
- Gradle khai báo JUnit nhưng `find src/test -type f -print` không có file nào.
Hệ quả:
- Build PASS không bắt được bug AI, respawn, movement, command behavior.

20. HIGH - Release workflow có thể fail vì jar reobf không tồn tại.
File nào:
- .github/workflows/release.yml
- build.gradle
Ở dòng nào:
- release.yml:36-40
- build.gradle:1-3, 42-56
Ảnh hưởng:
- Workflow copy `build/libs/*-reobf.jar`, nhưng project chỉ dùng plugin `java`, không có paperweight/reobf task.
- Build hiện tạo `build/libs/PvPBotPaper-1.0.0.jar`.
Hệ quả:
- GitHub release có thể fail tại bước rename jar.

21. MEDIUM - README/spec lệch xa code thật.
File nào:
- README.md
- src/main/java/com/khoablabla/pvpbot/commands/PvPBotCommand.java
Ở dòng nào:
- README.md:198-375
- PvPBotCommand.java:41-54, 274-300
Ảnh hưởng:
- README liệt kê settings GUI, factions, kits, paths, ranged, mace, dashboard, nhiều command.
- Code command hiện chỉ có `spawn`, `remove`, `removeall`.
Hệ quả:
- Người test sẽ tưởng có tính năng nhưng plugin chưa implement.

22. LOW/MEDIUM - `release.sh` tự sửa source trước khi release mà không sync Gradle version.
File nào:
- release.sh
- build.gradle
- gradle.properties
Ở dòng nào:
- release.sh:90-96
- build.gradle:5-6
- gradle.properties:1-2
Ảnh hưởng:
- Script chỉ sửa `src/main/resources/plugin.yml`, không sửa version trong Gradle.
Hệ quả:
- Tên jar vẫn theo version Gradle cũ, metadata source có thể lệch.

23. LOW - Root có class files/manifest rời ngoài build output.
File nào:
- net/citizensnpcs/api/ai/Navigator.class
- net/citizensnpcs/api/ai/NavigatorParameters.class
- META-INF/MANIFEST.MF
Ở dòng nào:
- Không áp dụng, là binary file ở root.
Ảnh hưởng:
- File binary rời nằm ngoài `build/` và `libs/`.
Hệ quả:
- Dễ nhầm là source/runtime artifact cần đóng gói, làm repo bẩn và audit khó hơn.

==========================================================
🎯 --- KẾT LUẬN THEO TRIỆU CHỨNG BẠN BÁO ---
==========================================================
Triệu chứng: "khi tôi đến gần nó đánh".
- Nguyên nhân chính: PvPBotTrait.java:64-70 + CombatTargetSelector.java:24-40.
- Code hiện là auto-target theo khoảng cách, không phải bị đánh mới trả đũa.

Triệu chứng: "bot bị đóng băng, đánh rồi bay, tôi nhảy lên nó nhảy theo".
- Nguyên nhân chính: BotMovementController.java:20-29 và MeleeAttackController.java:38-47.
- Cả movement và crit đều dùng `setVelocity` thô, thiếu check onGround/state.

Triệu chứng: "không có ai trong tầm mắt thì đứng im".
- Nguyên nhân chính: PvPBotTrait.java:71-74.
- Không có idle behavior.

Triệu chứng: "spawn bot thì join 1 lần thôi, chết không join nữa".
- Nguyên nhân chính: PlayerSimulationListener.java:91-122, đặc biệt 120.
- Death respawn đang broadcast joined game mỗi lần replacement spawn.

==========================================================
📋 --- TEST CASES ĐỀ XUẤT CHO A3 / HUMAN ---
==========================================================
Lệnh cần gõ:
- `./build.sh`
- Start Paper 1.21.11 với Citizens + PvPBot jar.
- `/pvpbot spawn TestBot`
- `/pvpbot spawn 5`
- `/pvpbot removeall`

Các case cần test trong game:
- Đứng gần bot nhưng không đánh: bot không được tự target/đánh nếu mục tiêu là revenge-only.
- Đánh bot một hit: bot phải lock đúng người vừa đánh trong thời gian aggro timeout.
- Đứng sau tường trong 16 block: bot không được target nếu không có line-of-sight.
- Nhảy trước mặt bot: bot không được spam nhảy/bay theo người chơi.
- Bot đánh cận chiến: không được khựng mất vận tốc ngang.
- Không có target: bot phải idle theo design mới, hoặc đứng guard có chủ ý, không cancel loạn navigator.
- Kill bot: không broadcast "joined the game" lần nữa nếu yêu cầu là chỉ join một lần khi spawn.
- Spawn fail ở vị trí không an toàn: player phải nhận message lỗi rõ.
- Release workflow: xác nhận artifact name đúng, không phụ thuộc `*-reobf.jar` nếu project chưa dùng paperweight.

==========================================================
📊 --- TỔNG QUAN ---
==========================================================
- Build hiện tại: PASS, 0 compile errors.
- Static scan của build.sh: PASS, 0 warnings.
- Unit tests: không có test nào.
- Số lỗi/cảnh báo audit thủ công: 23.
- Lỗi nghiêm trọng nhất liên quan gameplay: auto-target không revenge-only, velocity jump thô, respawn broadcast join.
- Không sửa production code trong `src/`.
- Không sửa `report.md`.
- Không chạy hoặc sửa `sign.py`.
- File đã ghi: `codex-report.md`.
- Tỷ lệ hoàn thành audit: **100%**
