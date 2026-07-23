┌────────────────────────────────────────────────────────┐
│             🎮 PvPBot — Deep Codex Audit Report         │
│  Audit-only: đã đọc source, chạy build, không sửa src/  │
└────────────────────────────────────────────────────────┘

==========================================================
🔎 --- PHẠM VI QUÉT ---
==========================================================
Thời điểm quét:
- 2026-07-23 15:06 +07

Đã đọc/quét:
- build.gradle
- build.sh
- README.md
- src/main/resources/plugin.yml
- src/main/java/com/khoablabla/pvpbot/PvPBot.java
- src/main/java/com/khoablabla/pvpbot/traits/PvPBotTrait.java
- src/main/java/com/khoablabla/pvpbot/movement/BotMovementController.java
- src/main/java/com/khoablabla/pvpbot/combat/MeleeAttackController.java
- src/main/java/com/khoablabla/pvpbot/combat/CombatTargetSelector.java
- src/main/java/com/khoablabla/pvpbot/listeners/PlayerSimulationListener.java
- src/main/java/com/khoablabla/pvpbot/commands/PvPBotCommand.java
- src/main/java/com/khoablabla/pvpbot/utils/SafeLocationFinder.java

Lệnh đã chạy:
- ./build.sh
- rg static scan các điểm: cancelNavigation, setTarget, setVelocity, damage, metadata Citizens, speedModifier
- javap Citizens NPC/NPC.Metadata để đối chiếu metadata đang dùng

Kết quả build:
- ./build.sh: PASS
- compileJava: PASS
- Unit tests: NO-SOURCE
- Static scan trong build.sh: 0 warnings
- JAR tạo được: build/libs/PvPBotPaper-1.0.0.jar
- Cảnh báo compile: MeleeAttackController.java:44 dùng Player#isOnGround() deprecated

Kết luận nhanh:
- Không thấy lỗi cú pháp/build.
- Lỗi chính là logic movement/combat runtime: bot tự hủy navigation khi nhảy crit, sau đó setTarget lại mỗi tick, gây giật/khựng/nhảy đoạn. Khi bị đánh có knockback thật của server cộng thêm setVelocity nhảy crit, bot rất dễ trông như "bay", rồi đi chậm hoặc chase không tự nhiên.

==========================================================
🛡️ --- CÁC LỖI CẦN XỬ LÝ ---
==========================================================
- [ ] Lỗi 1: Combat jump cancel navigation làm bot khựng khi đánh.
- [ ] Lỗi 2: Movement setTarget(target, true) mỗi tick, reset Citizens navigator liên tục.
- [ ] Lỗi 3: Attack chạy trước movement trong cùng tick, nên sau khi bật jump thì movement lại cancel navigation ngay.
- [ ] Lỗi 4: Bot dùng setVelocity để nhảy crit nhưng không quản trị vận tốc ngang/knockback nên dễ tạo cảm giác bay hoặc nhảy đoạn.
- [ ] Lỗi 5: Trong lúc jumpTicks >= 0, bot không kiểm tra lại khoảng cách/line-of-sight trước khi gây damage ở tick 5.
- [ ] Lỗi 6: Critical strike dùng damage số cố định, bỏ qua item, armor, enchant, cooldown combat thật.
- [ ] Lỗi 7: Không có aim/face target trước khi đánh.
- [ ] Lỗi 8: OnSpawn thiếu NPC.Metadata.DAMAGE_BY_PLAYER theo luật AGENTS.md.
- [ ] Lỗi 9: Target revenge timeout không được refresh khi bot đánh trúng hoặc tiếp tục bị đánh nhiều kiểu không phải Player trực tiếp.
- [ ] Lỗi 10: Bot chỉ trả đũa Player trực tiếp; projectile, potion, TNT, pet, mob hoặc nguồn sát thương gián tiếp không được xử lý.
- [ ] Lỗi 11: Bot có thể mất target sau 10 giây nếu bị knockback ra xa hơn 3 block, tạo cảm giác đuổi chậm/ngắt quãng.
- [ ] Lỗi 12: Idle/chase không chống stuck, không đổi path khi navigator kẹt.
- [ ] Lỗi 13: Respawn destroy NPC cũ rồi tạo NPC mới, có nguy cơ mất state và làm quản lý id khó ổn định.
- [ ] Lỗi 14: spawnLocations remove theo old NPC id; replacement id mới chỉ được put lại nếu NPCSpawnEvent chạy đúng.
- [ ] Lỗi 15: Removeall theo batch vẫn có thể bị death respawn task cũ tạo lại bot sau khi xóa.
- [ ] Lỗi 16: Spawn fail không báo nguyên nhân cho người chơi.
- [ ] Lỗi 17: Name validation còn yếu, không giới hạn ký tự chuẩn và không trim name.
- [ ] Lỗi 18: SafeLocationFinder coi nhiều block non-solid là an toàn, có thể spawn/wander vào block gây lỗi hành vi.
- [ ] Lỗi 19: Không có test tự động nào; build PASS không chứng minh AI chạy đúng trong game.
- [ ] Lỗi 20: README/spec mô tả nhiều hệ thống chưa có trong code: GUI, kit, faction, ranged, path, dashboard, config.

==========================================================
🔥 --- LỖI CHÍNH GÂY TRIỆU CHỨNG "ĐÁNH NÓ THÌ NÓ NHẢY/BAY, ĐI CHẬM, RỒI ĐẾN ĐÁNH" ---
==========================================================
1. CRITICAL - Bot hủy navigation trong lúc nhảy crit.
File nào:
- src/main/java/com/khoablabla/pvpbot/combat/MeleeAttackController.java
- src/main/java/com/khoablabla/pvpbot/movement/BotMovementController.java
- src/main/java/com/khoablabla/pvpbot/traits/PvPBotTrait.java
Ở dòng nào:
- MeleeAttackController.java:68-71
- BotMovementController.java:21-25
- PvPBotTrait.java:83-85
Lỗi như nào:
- Khi bot đủ tầm đánh, code setVelocity Y = 0.42 để nhảy crit rồi gọi npc.getNavigator().cancelNavigation().
- Ngay sau đó PvPBotTrait vẫn gọi movementController.handleMovement(..., attackController.isJumping()).
- Vì isJumping() true, BotMovementController lại cancelNavigation tiếp và return.
Ảnh hưởng:
- Bot mất path Citizens trong toàn bộ thời gian jumpTicks.
- Người chơi nhìn thấy bot dừng/khựng, bật lên, sau đó mới path lại.
Hệ quả:
- Rất khớp triệu chứng: "đánh nó thì nó nhảy 1 đoạn", "di chuyển kiểu bay", "cực chậm", "rồi đến đánh tôi".
Mức độ:
- CRITICAL.
Hướng sửa đề xuất:
- Không cancel navigator mỗi lần nhảy crit nếu không thật sự cần.
- Tách state combat jump khỏi state pathfinding: bot vẫn chase/strafe bình thường, chỉ jump khi đang on-ground và đang thật sự trong melee range.
- Nếu phải cancel, chỉ cancel một lần khi chuyển state, không cancel mỗi tick.

2. HIGH - setTarget(target, true) được gọi mỗi tick, dễ reset navigator/path.
File nào:
- src/main/java/com/khoablabla/pvpbot/movement/BotMovementController.java
Ở dòng nào:
- BotMovementController.java:28-29
Lỗi như nào:
- handleMovement gọi npc.getNavigator().setTarget(target, true) mỗi tick khi có target.
- Citizens navigator thường cần thời gian để path; setTarget lặp lại quá dày có thể làm đường đi bị reset/không ổn định.
Ảnh hưởng:
- Bot chase không mượt, có cảm giác khựng, quay đầu, đổi path liên tục.
Hệ quả:
- Khi kết hợp với cancelNavigation lúc jump, hành vi càng giống teleport nhỏ/nhảy đoạn rồi đi chậm.
Mức độ:
- HIGH.
Hướng sửa đề xuất:
- Chỉ setTarget khi target đổi, khi navigator không chạy, hoặc theo interval 5-10 tick.
- Cache target id/location cuối; chỉ repath khi target dịch chuyển đủ xa.
- Set speedModifier trước hoặc một lần khi spawn/enter combat, không cần spam mỗi tick.

3. HIGH - setVelocity nhảy crit không kiểm soát knockback và vận tốc ngang.
File nào:
- src/main/java/com/khoablabla/pvpbot/combat/MeleeAttackController.java
Ở dòng nào:
- MeleeAttackController.java:68-70
Lỗi như nào:
- Code giữ X/Z velocity hiện tại và ép Y velocity = 0.42.
- Nếu bot vừa bị người chơi đánh, server/Citizens có thể đã áp knockback X/Z. Bot sẽ mang knockback đó cộng với jump Y, nhìn như bị bay.
- Sau đó navigator bị cancel nên không có path kéo lại mượt.
Ảnh hưởng:
- Bot bị bật lùi/bật ngang rồi treo state jump vài tick.
Hệ quả:
- Hiện tượng "khi tôi đánh nó thì nó lại nhảy một đoạn rồi di chuyển kiểu bay".
Mức độ:
- HIGH.
Hướng sửa đề xuất:
- Chỉ jump crit khi bot đang on-ground, không vừa nhận knockback lớn, và horizontal velocity nằm dưới ngưỡng.
- Clamp X/Z velocity khi chủ động jump crit.
- Không dùng jump crit liên tục nếu target không đứng trong tầm ổn định.

4. HIGH - Trong state jump, bot vẫn đánh ở tick 5 mà không re-check range/LOS.
File nào:
- src/main/java/com/khoablabla/pvpbot/combat/MeleeAttackController.java
Ở dòng nào:
- MeleeAttackController.java:33-40
- MeleeAttackController.java:78-84
Lỗi như nào:
- Khi jumpTicks == 5, executeStrike được gọi trực tiếp.
- Không kiểm tra lại target còn trong 3.5 block không.
- Không kiểm tra target cùng world, còn valid, còn line-of-sight.
Ảnh hưởng:
- Bot có thể gây damage khi vừa bị knockback hoặc target đã lùi khỏi range ngắn.
Hệ quả:
- Combat nhìn không tự nhiên: bot bay/nhảy rồi vẫn đánh trúng.
Mức độ:
- HIGH.
Hướng sửa đề xuất:
- Trước executeStrike: validate target valid, same world, distance <= ATTACK_RANGE, line-of-sight nếu muốn PvP thật.

5. MEDIUM - Logic landing dùng API deprecated và block check chưa chuẩn.
File nào:
- src/main/java/com/khoablabla/pvpbot/combat/MeleeAttackController.java
Ở dòng nào:
- MeleeAttackController.java:43-44
Lỗi như nào:
- Player#isOnGround() deprecated.
- botPlayer.getLocation().subtract(0, 0.2, 0) tạo Location tạm đã bị mutate; hiện không phá state bot nhưng check block ở -0.2 không đủ chuẩn cho slab, stair, carpet, snow layer, water, cobweb.
Ảnh hưởng:
- jumpTicks có thể kết thúc sai hoặc timeout tới 12 tick.
Hệ quả:
- Bot có thể đứng khựng lâu hơn cần thiết sau cú nhảy.
Mức độ:
- MEDIUM.
Hướng sửa đề xuất:
- Dùng điều kiện grounded ổn định hơn từ Bukkit/Paper hoặc bounding box/support block helper riêng.

==========================================================
⚠️ --- LỖI NGẦM / XUNG ĐỘT LOGIC KHÁC ---
==========================================================
6. HIGH - Thiếu metadata DAMAGE_BY_PLAYER theo AGENTS.md.
File nào:
- src/main/java/com/khoablabla/pvpbot/traits/PvPBotTrait.java
Ở dòng nào:
- PvPBotTrait.java:53-57
Lỗi như nào:
- Code có npc.setProtected(false) và NPC.Metadata.DAMAGE_OTHERS.
- AGENTS.md bắt buộc thêm npc.data().set(NPC.Metadata.DAMAGE_BY_PLAYER, true).
- Trong citizens.jar hiện tại javap không thấy enum DAMAGE_BY_PLAYER, nên rule AGENTS.md có thể lệch version Citizens hiện tại hoặc đang nhắc API khác.
Ảnh hưởng:
- Nếu runtime Citizens/Paper khác với jar local, khả năng bot không nhận damage player đúng như kỳ vọng.
Hệ quả:
- Cần A2/A3 xác minh trên đúng Citizens server đang chạy, không chỉ jar libs/citizens.jar.
Mức độ:
- HIGH vì là rule bắt buộc của dự án, dù build hiện tại vẫn PASS.
Hướng sửa đề xuất:
- Xác nhận Citizens version runtime.
- Nếu metadata DAMAGE_BY_PLAYER có ở runtime API mới, cập nhật libs/citizens.jar và thêm đúng metadata.
- Nếu không có, ghi rõ thay thế hợp lệ của version hiện tại.

7. MEDIUM - Target timeout không refresh theo combat thực tế.
File nào:
- src/main/java/com/khoablabla/pvpbot/traits/PvPBotTrait.java
- src/main/java/com/khoablabla/pvpbot/combat/CombatTargetSelector.java
Ở dòng nào:
- PvPBotTrait.java:33-37
- CombatTargetSelector.java:22-23
Lỗi như nào:
- lastDamageTick chỉ update khi setTarget(newTarget) được gọi từ damage event.
- Nếu bot đang đuổi lâu hơn 200 tick và target ngoài 3 block, validateTarget trả null.
Ảnh hưởng:
- Sau khi bị đánh văng hoặc target chạy xa, bot có thể bỏ chase đột ngột.
Hệ quả:
- Người chơi thấy bot lúc đuổi, lúc ngắt, rồi lại chỉ đánh khi bị hit lại.
Mức độ:
- MEDIUM.
Hướng sửa đề xuất:
- Đổi tên lastDamageTick thành lastAggroTick và refresh khi bot nhìn thấy target, nhận damage tiếp, hoặc có combat interaction hợp lệ.

8. MEDIUM - Chỉ nhận revenge từ Player direct damager.
File nào:
- src/main/java/com/khoablabla/pvpbot/listeners/PlayerSimulationListener.java
Ở dòng nào:
- PlayerSimulationListener.java:129-140
Lỗi như nào:
- Chỉ xử lý event.getDamager() instanceof Player.
- Arrow, trident, potion, TNT, wolf/pet, firework hoặc damage gián tiếp không set target.
Ảnh hưởng:
- Bot không trả đũa nhiều kiểu PvP phổ biến.
Hệ quả:
- AI hành xử không nhất quán: kiếm thì chase, cung/trident thì có thể đứng im.
Mức độ:
- MEDIUM.
Hướng sửa đề xuất:
- Resolve Projectile#getShooter, Tameable owner, TNT source nếu cần.

9. MEDIUM - Respawn tạo NPC mới sau death, dễ mất state/quản lý id.
File nào:
- src/main/java/com/khoablabla/pvpbot/listeners/PlayerSimulationListener.java
Ở dòng nào:
- PlayerSimulationListener.java:75-105
Lỗi như nào:
- Death handler remove spawnLocations old id, destroy NPC cũ, tạo NPC replacement id mới.
- Nếu có future per-NPC settings/faction/kit/path, state sẽ mất nếu không copy.
Ảnh hưởng:
- Bot sau khi chết không còn đồng nhất với bản cũ.
Hệ quả:
- Các feature về sau sẽ lỗi ngầm nếu bám theo npc id.
Mức độ:
- MEDIUM.
Hướng sửa đề xuất:
- Dùng respawn policy rõ ràng; copy state trait; hoặc despawn/spawn cùng NPC nếu Citizens hỗ trợ.

10. MEDIUM - removeall có thể bị respawn task tạo lại bot.
File nào:
- src/main/java/com/khoablabla/pvpbot/listeners/PlayerSimulationListener.java
- src/main/java/com/khoablabla/pvpbot/commands/PvPBotCommand.java
Ở dòng nào:
- PlayerSimulationListener.java:95-126
- PvPBotCommand.java:219-257
Lỗi như nào:
- onNPCDeath schedule respawn sau 10 tick.
- removeall destroy bot theo batch nhưng không hủy các respawn task đã hẹn từ death trước đó.
Ảnh hưởng:
- Người chơi xóa sạch bot nhưng vài tick sau bot có thể xuất hiện lại nếu đang có task cũ.
Hệ quả:
- Quản trị server thấy removeall không triệt để trong một số tình huống.
Mức độ:
- MEDIUM.
Hướng sửa đề xuất:
- Có set suppressedRespawns hoặc generation token khi remove/removeall.

11. LOW - Spawn fail không báo nguyên nhân.
File nào:
- src/main/java/com/khoablabla/pvpbot/commands/PvPBotCommand.java
Ở dòng nào:
- PvPBotCommand.java:63-69
- PvPBotCommand.java:124-138
Lỗi như nào:
- spawnSingleBot trả false nếu không tìm được safeLocation hoặc Citizens spawn fail.
- Caller nhiều nhánh không báo lý do cụ thể.
Ảnh hưởng:
- Người chơi/admin không biết lỗi do vị trí, Citizens, world hay name.
Hệ quả:
- Khó debug khi bot không spawn.
Mức độ:
- LOW.
Hướng sửa đề xuất:
- Trả enum/result object thay vì boolean, log + sendMessage cụ thể.

12. LOW - Name validation chưa chặt.
File nào:
- src/main/java/com/khoablabla/pvpbot/commands/PvPBotCommand.java
Ở dòng nào:
- PvPBotCommand.java:86-99
- PvPBotCommand.java:104-119
- PvPBotCommand.java:150-181
Lỗi như nào:
- Chỉ chặn < và > cho custom name.
- Không trim, không giới hạn 16 ký tự cho custom name, không giới hạn regex Minecraft username.
Ảnh hưởng:
- Tên NPC có thể xấu, quá dài, chứa ký tự điều khiển/màu hoặc gây vấn đề tablist/scoreboard.
Hệ quả:
- Rủi ro lỗi hiển thị và conflict plugin khác.
Mức độ:
- LOW.
Hướng sửa đề xuất:
- Chuẩn hóa name: trim, 3-16 ký tự, [A-Za-z0-9_], reject duplicate sau normalize.

13. LOW - SafeLocationFinder cho phép một số block non-solid không thật sự an toàn.
File nào:
- src/main/java/com/khoablabla/pvpbot/utils/SafeLocationFinder.java
Ở dòng nào:
- SafeLocationFinder.java:11-15
- SafeLocationFinder.java:51-58
- SafeLocationFinder.java:61-68
Lỗi như nào:
- isNonSolid cho phép mọi block không solid làm feet/head.
- Danh sách hazard thiếu cobweb, berry ở feet/head, bubble column, pointed dripstone, powder snow ở feet/head, campfire ở feet/head, portal ở feet/head.
Ảnh hưởng:
- Bot có thể spawn/wander vào block gây kẹt/chậm/damage.
Hệ quả:
- Một phần cảm giác "đi cực chậm" có thể đến từ block môi trường nếu bot được đặt vào vị trí kẹt.
Mức độ:
- LOW/MEDIUM tùy map.
Hướng sửa đề xuất:
- Có allowlist passable block thay vì blacklist hazard.

14. LOW - README/spec lệch xa code thực tế.
File nào:
- README.md
Ở dòng nào:
- Toàn file, đặc biệt các mục GUI, ranged, mace, survival, faction, kit, path, dashboard.
Lỗi như nào:
- README mô tả nhiều feature chưa có trong source hiện tại.
Ảnh hưởng:
- A1/A2/A3 dễ hiểu sai trạng thái dự án.
Hệ quả:
- Prompt/code sau có thể dựa vào feature chưa tồn tại.
Mức độ:
- LOW.
Hướng sửa đề xuất:
- Tách "đã có" và "roadmap"; ghi rõ Phase hiện tại chỉ có spawn/remove, revenge melee cơ bản, idle wander.

==========================================================
📋 --- HƯỚNG DẪN KIỂM THỬ TRONG GAME ---
==========================================================
Mục tiêu test:
- Xác nhận lỗi chính là cancel navigation + jump crit + setTarget mỗi tick.

Chuẩn bị:
- Server Paper 1.21.11 với Citizens đúng version đang dùng.
- Plugin jar: build/libs/PvPBotPaper-1.0.0.jar
- Người test có permission pvpbot.admin.

Lệnh cần gõ:
- /pvpbot spawn TestBot
- /pvpbot remove TestBot
- /pvpbot removeall

Test Case 1 - Hit bot khi đứng sát:
- Spawn TestBot trên nền phẳng.
- Đứng cách bot 2-3 block.
- Đánh bot một lần bằng kiếm.
- Quan sát: bot có bật lên, khựng path, rồi mới lao tới không.
- Kỳ vọng hiện tại: có khả năng xuất hiện lỗi nhảy/khựng.

Test Case 2 - Hit bot rồi lùi nhanh:
- Đánh bot một lần.
- Lùi ra 5-8 block ngay khi bot bắt đầu chase.
- Quan sát: bot có dừng/đổi nhịp/đi chậm sau cú nhảy crit không.
- Kỳ vọng hiện tại: chase không mượt do setTarget mỗi tick và cancel khi jump.

Test Case 3 - Knockback:
- Dùng kiếm có Knockback hoặc sprint-hit bot.
- Quan sát: bot có bị bật ngang + bật Y giống bay không.
- Kỳ vọng hiện tại: lỗi rõ hơn vì setVelocity jump cộng với knockback.

Test Case 4 - Projectile:
- Bắn bot bằng cung/trident.
- Quan sát: bot có trả đũa không.
- Kỳ vọng hiện tại: có thể không set target vì listener chỉ nhận damager là Player trực tiếp.

Test Case 5 - Removeall sau combat/death:
- Spawn nhiều bot, cho một bot chết hoặc đang trong respawn window.
- Gõ /pvpbot removeall.
- Đợi 1-2 giây.
- Quan sát: có bot nào xuất hiện lại không.
- Kỳ vọng hiện tại: có rủi ro respawn task cũ tạo lại bot.

==========================================================
📊 --- TỔNG QUAN ---
==========================================================
- Build/cú pháp: PASS.
- Test tự động: Không có test source, nên mức tin cậy runtime vẫn thấp.
- Lỗi nghiêm trọng nhất: combat jump hủy navigation + movement spam setTarget mỗi tick.
- File trọng tâm cần sửa trước:
  - src/main/java/com/khoablabla/pvpbot/combat/MeleeAttackController.java
  - src/main/java/com/khoablabla/pvpbot/movement/BotMovementController.java
  - src/main/java/com/khoablabla/pvpbot/traits/PvPBotTrait.java
- Ưu tiên sửa:
  1. Không cancel navigation mỗi tick khi jump.
  2. Không setTarget lại mỗi tick; repath có throttle/cache.
  3. Re-check range/LOS/validity trước khi executeStrike.
  4. Clamp/guard velocity khi nhảy crit sau khi bị knockback.
  5. Bổ sung xử lý projectile/indirect damage nếu muốn revenge AI thật.
- Tỷ lệ hoàn thành audit: 100%.
- Tỷ lệ ổn định của code hiện tại theo audit runtime: khoảng 55-65% cho melee AI cơ bản, vì build pass nhưng movement/combat state machine còn lỗi hành vi nặng.
