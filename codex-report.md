┌────────────────────────────────────────────────────────┐
│             🎮 PvPBot — Deep Codex Audit Report         │
│  Audit + fix: đã sửa movement/combat và chạy build     │
└────────────────────────────────────────────────────────┘

==========================================================
🔎 --- PHẠM VI QUÉT ---
==========================================================
Thời điểm quét:
- 2026-07-24 14:39:46 +0700

Đã đọc/quét:
- AGENTS.md
- README.md
- build.gradle
- build.sh
- settings.gradle
- gradle.properties
- src/main/resources/plugin.yml
- src/main/java/com/khoablabla/pvpbot/PvPBot.java
- src/main/java/com/khoablabla/pvpbot/commands/PvPBotCommand.java
- src/main/java/com/khoablabla/pvpbot/listeners/PlayerSimulationListener.java
- src/main/java/com/khoablabla/pvpbot/traits/PvPBotTrait.java
- src/main/java/com/khoablabla/pvpbot/combat/CombatTargetSelector.java
- src/main/java/com/khoablabla/pvpbot/combat/MeleeAttackController.java
- src/main/java/com/khoablabla/pvpbot/movement/BotMovementController.java
- src/main/java/com/khoablabla/pvpbot/utils/SafeLocationFinder.java
- libs/citizens.jar API qua javap: Navigator, NavigatorParameters, NPC.Metadata

Lệnh đã chạy:
- ./build.sh
- rg static scan: System.out/err, unsafe casts, Citizens metadata, setTarget, cancelNavigation, distance, isOnGround
- javap -classpath libs/citizens.jar net.citizensnpcs.api.ai.Navigator
- javap -classpath libs/citizens.jar net.citizensnpcs.api.ai.NavigatorParameters
- javap -classpath libs/citizens.jar 'net.citizensnpcs.api.npc.NPC$Metadata'

Kết quả build:
- ./build.sh: PASS
- compileJava: PASS
- gradle build: PASS
- Unit tests: NO-SOURCE
- Static scan trong build.sh: 0 warnings
- JAR tạo được: build/libs/PvPBotPaper-1.0.0.jar
- Cảnh báo compile: Không còn warning deprecated sau fix.

Kết luận nhanh:
- Không thấy lỗi cú pháp Java làm fail build.
- Không có test tự động, nên build PASS chỉ chứng minh code biên dịch được, chưa chứng minh AI chase/combat đúng trong game.
- Lỗi bạn mô tả rất khớp với vùng logic movement/combat:
  - Bot dùng Citizens navigator bám entity nhưng không cấu hình distanceMargin/attackRange/pathDistanceMargin theo melee range 3.5 block.
  - Khi navigator còn "isNavigating" và vị trí target không đổi quá 1.5 block, code bỏ qua repath.
  - Khi vào tầm đánh, attack controller hủy navigation để nhảy crit.
  - Kết quả dễ thấy trong game: bot dừng ở khoảng xa hơn tầm đánh thật, không đánh; chỉ khi người chơi tiến vào 3-4 block thì attack logic chạy lại.

==========================================================
✅ --- FIX ĐÃ ÁP DỤNG NGÀY 2026-07-24 ---
==========================================================
Các file đã sửa:
- src/main/java/com/khoablabla/pvpbot/movement/BotMovementController.java
- src/main/java/com/khoablabla/pvpbot/combat/MeleeAttackController.java

Đã sửa gì:
- BotMovementController không dùng waypoint 24 block cho combat chase nữa; trong phạm vi 128 block bot bám entity target trực tiếp.
- Cấu hình Citizens NavigatorParameters mỗi tick combat: speedModifier, attackRange, distanceMargin, pathDistanceMargin, straightLineTargetingDistance, range, updatePathRate.
- Repath không còn chỉ dựa vào targetLoc; có thêm phát hiện target đổi, navigator ngừng, target di chuyển, và bot đứng kẹt ngoài melee range.
- Repath stuck/non-navigating bị giới hạn theo REPATH_INTERVAL để không spam Citizens navigator.
- MeleeAttackController không còn gọi cancelNavigation khi nhảy crit.
- Critical jump được clamp horizontal velocity để tránh cộng knockback thành bay/giật đoạn.
- Nếu không đủ điều kiện crit ổn định, bot đánh thường thay vì đứng im.
- Trước khi damage luôn kiểm tra target valid, same world, distance <= 3.5 và line-of-sight.
- Bot face target trước khi đánh/đang jump.
- Đã bỏ Player#isOnGround() deprecated, compile không còn warning này.

Kết quả xác minh:
- ./gradlew compileJava: PASS
- ./build.sh: PASS
- JAR mới: build/libs/PvPBotPaper-1.0.0.jar

==========================================================
🔥 --- LỖI CHÍNH GÂY TRIỆU CHỨNG "CÁCH 7 BLOCK THÌ DỪNG, GẦN 3-4 BLOCK MỚI ĐUỔI LẠI" ---
==========================================================
1. CRITICAL - Citizens navigator không được cấu hình khoảng dừng sát melee range.
File nào:
- src/main/java/com/khoablabla/pvpbot/movement/BotMovementController.java
- src/main/java/com/khoablabla/pvpbot/combat/MeleeAttackController.java
Ở dòng nào:
- BotMovementController.java:48-49
- BotMovementController.java:62
- MeleeAttackController.java:18
- MeleeAttackController.java:57-58
Lỗi như nào:
- Movement gọi npc.getNavigator().setTarget(target, true) khi distance <= 32 block.
- Attack thật chỉ chạy khi botPlayer.getLocation().distance(target.getLocation()) <= 3.5.
- Code không set navigator attackRange(), distanceMargin(), pathDistanceMargin(), straightLineTargetingDistance() hoặc updatePathRate() để Citizens luôn kéo bot vào đúng <= 3.5 block.
- Nếu Citizens mặc định coi target đã "đủ gần" ở khoảng lớn hơn 3.5 block, navigator có thể dừng trong khi MeleeAttackController vẫn return vì dist > ATTACK_RANGE.
Ảnh hưởng:
- Bot có thể đứng ở khoảng 6-8 block, không đi tiếp và không đánh.
Hệ quả:
- Khớp trực tiếp với mô tả: "cách 7 block là dừng không đánh nữa"; chỉ khi bạn tự đi lại gần 3-4 block thì dist <= 3.5, attack logic bắt đầu chạy.
Mức độ:
- CRITICAL.
Hướng sửa đề xuất:
- Khi vào combat, cấu hình NavigatorParameters nhất quán với melee:
  - attackRange khoảng 3.0-3.3.
  - distanceMargin/pathDistanceMargin nhỏ hơn ATTACK_RANGE, ví dụ 1.2-2.0 tùy Citizens.
  - updatePathRate thấp hơn hoặc tự throttle repath rõ ràng.
- Không phụ thuộc default Citizens range nếu custom melee range là 3.5.

2. CRITICAL - Throttle repath bỏ qua khi target đứng gần như yên, làm bot không tự sửa path nếu đã dừng sai khoảng.
File nào:
- src/main/java/com/khoablabla/pvpbot/movement/BotMovementController.java
Ở dòng nào:
- BotMovementController.java:21-23
- BotMovementController.java:40-43
- BotMovementController.java:45-49
Lỗi như nào:
- REPATH_INTERVAL = 5 tick.
- THROTTLE_DISTANCE_SQ = 2.25, nghĩa là nếu target chỉ dịch chuyển <= 1.5 block và navigator đang navigating thì handleMovement return luôn, không setTarget lại.
- Điều kiện chỉ nhìn vào targetLoc so với lastTargetLocation, không nhìn vào việc bot đang bị kẹt, đứng sai khoảng, path complete, path paused, hay distance còn lớn hơn ATTACK_RANGE.
Ảnh hưởng:
- Nếu bot đã bị Citizens dừng ở khoảng 7 block, target đứng yên hoặc nhích nhẹ thì code không repath.
Hệ quả:
- Bot chờ đến khi target tự vào gần 3-4 block hoặc target di chuyển đủ xa mới có cơ hội thay đổi hành vi.
Mức độ:
- CRITICAL.
Hướng sửa đề xuất:
- Throttle phải xét thêm khoảng cách bot-target và trạng thái stuck:
  - Nếu distance > ATTACK_RANGE + buffer mà bot đứng yên/navigator không tiến bộ, bắt buộc repath.
  - Nếu navigator không còn target/path hoặc target type đã complete, setTarget lại.
  - Lưu lastBotLocation/lastProgressTick để phát hiện đứng yên.

3. HIGH - Vùng 32-128 block dùng waypoint 24 block, có thể tạo chase ngắt quãng.
File nào:
- src/main/java/com/khoablabla/pvpbot/movement/BotMovementController.java
Ở dòng nào:
- BotMovementController.java:23-25
- BotMovementController.java:48-59
Lỗi như nào:
- Nếu distance > 32 và <= 128, bot không bám entity trực tiếp mà đặt waypoint cách bot 24 block theo hướng target.
- Sau khi tới waypoint, nếu target không dịch chuyển đủ 1.5 block và navigator vẫn báo navigating/hoặc vừa complete không được xử lý rõ, bot có thể có nhịp đứng lại trước khi tạo waypoint mới.
Ảnh hưởng:
- Chase đường dài không liên tục, nhìn như đi từng đoạn.
Hệ quả:
- Khi người chơi vừa chạy xa vừa quay lại, bot có thể khựng rồi chỉ chase tiếp khi điều kiện repath được kích hoạt.
Mức độ:
- HIGH.
Hướng sửa đề xuất:
- Với pursuit bình thường nên bám entity hoặc waypoint động có progress detection.
- Nếu dùng waypoint, phải repath khi bot gần waypoint, khi target đổi hướng, hoặc khi distance tới target vẫn còn lớn.

4. HIGH - Attack controller hủy navigation khi nhảy crit.
File nào:
- src/main/java/com/khoablabla/pvpbot/combat/MeleeAttackController.java
- src/main/java/com/khoablabla/pvpbot/traits/PvPBotTrait.java
Ở dòng nào:
- MeleeAttackController.java:62-84
- PvPBotTrait.java:84-87
Lỗi như nào:
- Khi dist <= 3.5 và cooldown xong, code setVelocity để nhảy crit rồi gọi npc.getNavigator().cancelNavigation().
- Trong cùng tick, PvPBotTrait vẫn gọi movementController.handleMovement sau handleAttack.
- Vì navigation vừa bị cancel, movement có thể setTarget lại ở tick sau, tạo nhịp cancel/setTarget lặp.
Ảnh hưởng:
- Bot khựng path khi chuẩn bị đánh.
Hệ quả:
- Khi người chơi đứng quanh mép range, bot có cảm giác dừng, nhảy, rồi mới đuổi/đánh lại.
Mức độ:
- HIGH.
Hướng sửa đề xuất:
- Không cancelNavigation cho cú nhảy crit trừ khi thật sự cần.
- Nếu cần cancel, chỉ cancel một lần khi chuyển state và không để movement spam setTarget ngay sau đó.
- Tách state "combat jump" khỏi state "pursuit".

5. HIGH - setVelocity nhảy crit cộng với knockback làm bot giống bay/giật đoạn.
File nào:
- src/main/java/com/khoablabla/pvpbot/combat/MeleeAttackController.java
Ở dòng nào:
- MeleeAttackController.java:19-20
- MeleeAttackController.java:66-83
Lỗi như nào:
- Code lấy currentVel rồi trộn với hướng target, sau đó ép Y velocity = 0.38.
- Nếu bot vừa bị người chơi đánh, server đã có knockback X/Z; cú nhảy crit giữ một phần velocity cũ và thêm velocity mới.
Ảnh hưởng:
- Bot bật ngang/bật lùi/bật lên, nhìn như bay hoặc di chuyển không bình thường.
Hệ quả:
- Khớp phần bạn mô tả "tôi đánh nó" rồi nó di chuyển lạ/khựng.
Mức độ:
- HIGH.
Hướng sửa đề xuất:
- Chỉ nhảy crit khi bot đang grounded ổn định và horizontal velocity dưới ngưỡng.
- Clamp X/Z velocity.
- Không nhảy crit nếu vừa nhận knockback mạnh hoặc target đang ngoài range ổn định.

==========================================================
🛡️ --- CÁC LỖI CẦN XỬ LÝ ---
==========================================================
- [ ] Lỗi 1: Navigator range/margin không khớp ATTACK_RANGE 3.5, gây dừng ở khoảng 6-8 block.
- [ ] Lỗi 2: Throttle repath chỉ dựa vào vị trí target, không dựa vào tiến độ bot/stuck/path complete.
- [ ] Lỗi 3: Waypoint pursuit 32-128 block tạo chase từng đoạn, dễ khựng.
- [ ] Lỗi 4: MeleeAttackController cancelNavigation khi nhảy crit, phá pursuit.
- [ ] Lỗi 5: setVelocity nhảy crit cộng knockback gây cảm giác bay/giật đoạn.
- [ ] Lỗi 6: Jump strike chỉ đánh ở tick 5, thiếu kiểm tra line-of-sight và grounded/velocity tốt.
- [ ] Lỗi 7: Damage dùng số cố định, bỏ qua item, armor, enchant, cooldown combat thật.
- [ ] Lỗi 8: Bot không face/aim target trước khi đánh.
- [ ] Lỗi 9: OnSpawn chưa cấu hình đầy đủ theo AGENTS.md; AGENTS nhắc DAMAGE_BY_PLAYER nhưng citizens.jar hiện tại không có enum này.
- [ ] Lỗi 10: Không có auto-target scan; bot chỉ trả đũa khi bị đánh.
- [ ] Lỗi 11: CombatTargetSelector nhận tham số tickCounter/lastDamageTick nhưng không dùng, code smell.
- [ ] Lỗi 12: SafeLocationFinder có thể coi nhiều block không an toàn là hợp lệ.
- [ ] Lỗi 13: Respawn tạo NPC mới, dễ mất state/id cho feature sau.
- [ ] Lỗi 14: removeall có rủi ro respawn task cũ tạo lại bot.
- [ ] Lỗi 15: Custom name validation yếu.
- [ ] Lỗi 16: spawn fail không báo nguyên nhân.
- [ ] Lỗi 17: build.sh báo 100% clean dù compile có warning và không có unit test.
- [ ] Lỗi 18: README/spec lệch xa code thực tế.

==========================================================
⚠️ --- LỖI NGẦM / XUNG ĐỘT LOGIC KHÁC ---
==========================================================
6. MEDIUM - Strike tick 5 có validate chưa đủ cho combat thật.
File nào:
- src/main/java/com/khoablabla/pvpbot/combat/MeleeAttackController.java
Ở dòng nào:
- MeleeAttackController.java:34-40
- MeleeAttackController.java:91-98
Lỗi như nào:
- Tick 5 có check same world và distance <= 3.5 trước khi executeStrike.
- Nhưng không check line-of-sight, target invulnerable/noDamageTicks, shield state, spectator đổi mode trong cùng tick, hoặc bot có đang thật sự nhìn về target không.
Ảnh hưởng:
- Bot có thể đánh không tự nhiên qua góc/địa hình mỏng tùy server collision và Citizens movement.
Hệ quả:
- PvP nhìn "ảo", nhất là khi bot vừa bật khỏi đường path.
Mức độ:
- MEDIUM.
Hướng sửa đề xuất:
- Validate target valid, same world, distance, line-of-sight, bot facing/aim threshold, cooldown hợp lệ trước damage.

7. MEDIUM - Damage cố định bỏ qua hệ thống combat Minecraft.
File nào:
- src/main/java/com/khoablabla/pvpbot/combat/MeleeAttackController.java
Ở dòng nào:
- MeleeAttackController.java:21-23
- MeleeAttackController.java:91-98
Lỗi như nào:
- NORMAL_DAMAGE = 2.0, CRITICAL_DAMAGE = 4.0.
- target.damage(...) gọi damage thẳng, không dựa vào sword/axe, enchant, potion, armor penetration, attack cooldown thật, shield break.
Ảnh hưởng:
- Bot không mô phỏng PvP thật.
Hệ quả:
- Dễ lệch balance và gây bug khi mở rộng auto-weapon/kit.
Mức độ:
- MEDIUM.
Hướng sửa đề xuất:
- Dùng Bukkit/Paper combat event hoặc tính damage theo item/equipment/settings rõ ràng.

8. MEDIUM - Bot không xoay mặt/aim target trước khi đánh.
File nào:
- src/main/java/com/khoablabla/pvpbot/combat/MeleeAttackController.java
- src/main/java/com/khoablabla/pvpbot/movement/BotMovementController.java
Ở dòng nào:
- MeleeAttackController.java:29-89
- BotMovementController.java:48-62
Lỗi như nào:
- Không gọi faceLocation hoặc ProtocolLib rotate packet.
- README nói có aim-speed/packet rotation, nhưng code chưa có.
Ảnh hưởng:
- Bot có thể đánh khi model chưa nhìn đúng hướng.
Hệ quả:
- Combat cảm giác không tự nhiên.
Mức độ:
- MEDIUM.
Hướng sửa đề xuất:
- Ít nhất gọi npc.faceLocation(target eye/body location) khi chase/attack.
- Về sau dùng ProtocolLib aim smoothing nếu cần.

9. HIGH - Xung đột AGENTS.md với Citizens API cục bộ về DAMAGE_BY_PLAYER.
File nào:
- AGENTS.md
- src/main/java/com/khoablabla/pvpbot/traits/PvPBotTrait.java
- libs/citizens.jar
Ở dòng nào:
- PvPBotTrait.java:55-58
Lỗi như nào:
- Code có npc.setProtected(false) và NPC.Metadata.DAMAGE_OTHERS.
- AGENTS.md bắt buộc npc.data().set(NPC.Metadata.DAMAGE_BY_PLAYER, true).
- javap trên libs/citizens.jar hiện tại không thấy NPC.Metadata.DAMAGE_BY_PLAYER; chỉ thấy DAMAGE_OTHERS và các metadata khác.
Ảnh hưởng:
- Nếu A2 thêm đúng theo AGENTS.md với jar hiện tại thì sẽ fail compile.
- Nếu server runtime dùng Citizens khác jar local thì hành vi damage có thể lệch giữa build và runtime.
Hệ quả:
- Đây là xung đột tiêu chuẩn/dependency cần A1 quyết định: update Citizens jar hay sửa rule theo API thật.
Mức độ:
- HIGH.
Hướng sửa đề xuất:
- Xác nhận Citizens version trên server thật.
- Đồng bộ libs/citizens.jar với runtime.
- Nếu API hiện tại không có DAMAGE_BY_PLAYER, ghi lại metadata thay thế hợp lệ trong AGENTS.md/report.

10. MEDIUM - Không có auto-target scan, bot chỉ revenge.
File nào:
- src/main/java/com/khoablabla/pvpbot/traits/PvPBotTrait.java
- src/main/java/com/khoablabla/pvpbot/listeners/PlayerSimulationListener.java
Ở dòng nào:
- PvPBotTrait.java:34-39
- PvPBotTrait.java:72-97
- PlayerSimulationListener.java:146-170
Lỗi như nào:
- target chỉ được set qua trait.setTarget(attacker) khi bot bị damage.
- Không có logic quét player gần đó để chủ động đánh.
Ảnh hưởng:
- Bot không đúng mục tiêu README "auto target search".
Hệ quả:
- Người chơi không đánh bot thì bot chỉ idle wander.
Mức độ:
- MEDIUM.
Hướng sửa đề xuất:
- Thêm TargetScanner riêng: lọc world, gamemode, range, line-of-sight/settings/faction, rồi set target.

11. LOW - CombatTargetSelector có tham số không dùng.
File nào:
- src/main/java/com/khoablabla/pvpbot/combat/CombatTargetSelector.java
- src/main/java/com/khoablabla/pvpbot/traits/PvPBotTrait.java
Ở dòng nào:
- CombatTargetSelector.java:14
- PvPBotTrait.java:24
- PvPBotTrait.java:76
Lỗi như nào:
- validateTarget nhận tickCounter và lastDamageTick nhưng không dùng.
- lastDamageTick chỉ set ở PvPBotTrait.java:37, không refresh theo thời gian combat.
Ảnh hưởng:
- Code gây hiểu nhầm là có aggro timeout hoặc revenge tracking, nhưng hiện không có.
Hệ quả:
- Sau này dễ sửa sai vì tưởng đã có state machine hoàn chỉnh.
Mức độ:
- LOW/MEDIUM.
Hướng sửa đề xuất:
- Xóa tham số thừa hoặc triển khai timeout/prolong aggro thật.

12. MEDIUM - SafeLocationFinder blacklist chưa đủ, có thể chọn vị trí kẹt/chậm/damage.
File nào:
- src/main/java/com/khoablabla/pvpbot/utils/SafeLocationFinder.java
Ở dòng nào:
- SafeLocationFinder.java:11-15
- SafeLocationFinder.java:46-69
Lỗi như nào:
- isNonSolid cho phép mọi block không solid cho feet/head.
- Hazard check chủ yếu kiểm tra block below, không kiểm tra đầy đủ feet/head với cobweb, powder snow, berry bush, portal, bubble column, pointed dripstone, campfire...
Ảnh hưởng:
- Bot có thể spawn/wander vào vị trí làm chậm hoặc gây damage.
Hệ quả:
- Có thể góp phần tạo cảm giác "đi không bình thường" trên map phức tạp.
Mức độ:
- MEDIUM tùy map.
Hướng sửa đề xuất:
- Dùng allowlist/denylist passable rõ hơn.
- Check feet/head/below đều không hazard.

13. MEDIUM - Respawn tạo NPC mới, rủi ro mất state/id.
File nào:
- src/main/java/com/khoablabla/pvpbot/listeners/PlayerSimulationListener.java
Ở dòng nào:
- PlayerSimulationListener.java:75-144
Lỗi như nào:
- onNPCDeath destroy NPC cũ, tạo replacement NPC mới với id mới.
- Chỉ copy name và trait cơ bản, không copy state combat/settings/faction/path/kit nếu sau này có.
Ảnh hưởng:
- Feature per-NPC về sau dễ mất dữ liệu sau khi bot chết.
Hệ quả:
- Debug khó vì ID thay đổi liên tục.
Mức độ:
- MEDIUM.
Hướng sửa đề xuất:
- Có RespawnManager copy state rõ ràng hoặc tái spawn cùng NPC nếu Citizens hỗ trợ.

14. MEDIUM - removeall có thể không triệt respawn task đã hẹn.
File nào:
- src/main/java/com/khoablabla/pvpbot/commands/PvPBotCommand.java
- src/main/java/com/khoablabla/pvpbot/listeners/PlayerSimulationListener.java
Ở dòng nào:
- PvPBotCommand.java:222-264
- PlayerSimulationListener.java:43-48
- PlayerSimulationListener.java:110-143
Lỗi như nào:
- removeall hủy respawnTasks theo id bot đang có trong registry.
- Nếu task cũ đã hẹn từ NPC chết trước đó và old id không còn trong registry, vòng removeall không biết để cancel.
Ảnh hưởng:
- Có tình huống /pvpbot removeall xong vẫn có bot respawn lại.
Hệ quả:
- Admin tưởng đã xóa sạch nhưng bot xuất hiện sau 10 tick.
Mức độ:
- MEDIUM.
Hướng sửa đề xuất:
- Khi removeall, cancel toàn bộ respawnTasks hoặc bật flag suppressRespawnUntil.

15. LOW - Spawn fail im lặng.
File nào:
- src/main/java/com/khoablabla/pvpbot/commands/PvPBotCommand.java
Ở dòng nào:
- PvPBotCommand.java:64-70
- PvPBotCommand.java:73-86
- PvPBotCommand.java:125-139
Lỗi như nào:
- spawnSingleBot trả false khi safeLocation null hoặc npc.spawn fail.
- Caller chỉ báo số lượng hoặc không báo chi tiết nguyên nhân.
Ảnh hưởng:
- Admin khó biết lỗi do vị trí không an toàn, Citizens spawn fail, hay world state.
Hệ quả:
- Debug production chậm.
Mức độ:
- LOW.
Hướng sửa đề xuất:
- Trả SpawnResult gồm status + message; log lý do cụ thể.

16. LOW - Custom name validation yếu.
File nào:
- src/main/java/com/khoablabla/pvpbot/commands/PvPBotCommand.java
Ở dòng nào:
- PvPBotCommand.java:87-100
- PvPBotCommand.java:105-121
- PvPBotCommand.java:151-182
Lỗi như nào:
- Custom name chỉ chặn < và >.
- Không trim, không giới hạn 3-16 ký tự, không giới hạn [A-Za-z0-9_].
Ảnh hưởng:
- Có thể tạo tên quá dài/ký tự lạ gây lỗi hiển thị tablist/nameplate/scoreboard.
Hệ quả:
- Xung đột plugin khác hoặc khó remove bằng command.
Mức độ:
- LOW.
Hướng sửa đề xuất:
- Validate custom name giống username Minecraft: trim, 3-16, regex [A-Za-z0-9_].

17. LOW - build.sh báo 100% clean dù có warning và không có unit test.
File nào:
- build.sh
Ở dòng nào:
- build.sh:79-86
- build.sh:151-159
- build.sh:181-188
Lỗi như nào:
- compileJava có warning deprecated ở MeleeAttackController.java:43.
- Unit tests là NO-SOURCE.
- build.sh vẫn in "Pipeline completed — 100% clean".
Ảnh hưởng:
- Báo cáo build dễ gây hiểu nhầm là runtime AI đã sạch.
Hệ quả:
- A3/A1 có thể đánh giá quá cao độ ổn định.
Mức độ:
- LOW/MEDIUM về quy trình.
Hướng sửa đề xuất:
- Bật -Werror nếu muốn chặn warning.
- Ghi rõ NO-SOURCE là thiếu test, không phải test pass.

18. LOW - README/spec lệch xa source hiện tại.
File nào:
- README.md
Ở dòng nào:
- README.md:1-72
- README.md:197-393
Lỗi như nào:
- README mô tả GUI settings, ranged AI, mace combo, survival, faction, kit, path, dashboard, SQLite/DuckDB.
- Source hiện tại chỉ có spawn/remove/removeall, trait revenge melee cơ bản, idle wander, respawn.
Ảnh hưởng:
- Agent hoặc người đọc dễ nghĩ feature đã có.
Hệ quả:
- Dễ sinh prompt/sửa code dựa trên module chưa tồn tại.
Mức độ:
- LOW.
Hướng sửa đề xuất:
- Tách "đã triển khai" và "roadmap".

==========================================================
📋 --- HƯỚNG DẪN KIỂM THỬ TRONG GAME ---
==========================================================
Mục tiêu test:
- Xác nhận lỗi dừng 6-8 block do Citizens navigator margin/throttle.
- Xác nhận lỗi khựng/bay do cancelNavigation + setVelocity khi nhảy crit.

Chuẩn bị:
- Server Paper 1.21.11.
- Citizens runtime phải cùng version với libs/citizens.jar hoặc ghi rõ version khác.
- Plugin jar: build/libs/PvPBotPaper-1.0.0.jar.
- Người test có permission pvpbot.admin.

Lệnh cần gõ:
- /pvpbot spawn TestBot
- /pvpbot remove TestBot
- /pvpbot removeall

Test Case 1 - Khoảng dừng 7 block:
- Spawn TestBot trên nền phẳng, không vật cản.
- Đánh bot một lần để set target.
- Chạy lùi và giữ khoảng cách khoảng 6-8 block.
- Quan sát bot có đứng lại, không tiến sát vào <= 3.5 block không.
- Kỳ vọng hiện tại: có khả năng bot dừng ở khoảng 6-8 block và không đánh.

Test Case 2 - Đến gần 3-4 block:
- Sau Test Case 1, đi bộ lại gần bot tới khoảng 3-4 block.
- Quan sát bot có bắt đầu nhảy/đánh lại không.
- Kỳ vọng hiện tại: attack chạy lại khi dist <= ATTACK_RANGE 3.5.

Test Case 3 - Target đứng yên:
- Đánh bot, chạy ra khoảng 7 block, sau đó đứng yên.
- Quan sát 5-10 giây.
- Kỳ vọng hiện tại: nếu throttle/path margin mắc lỗi, bot không tự repath vì targetLoc không đổi quá 1.5 block.

Test Case 4 - Knockback/sprint hit:
- Dùng sprint-hit hoặc kiếm Knockback đánh bot khi nó sắp vào melee range.
- Quan sát bot có bật ngang + nhảy Y, nhìn như bay/giật không.
- Kỳ vọng hiện tại: lỗi rõ hơn do setVelocity cộng với knockback.

Test Case 5 - Vùng chase xa 32-128 block:
- Đánh bot rồi chạy ra 40-60 block trên nền phẳng.
- Quan sát bot đuổi liên tục hay đi theo từng đoạn 24 block rồi khựng.
- Kỳ vọng hiện tại: có thể chase không mượt vì waypoint pursuit.

Test Case 6 - Projectile revenge:
- Bắn bot bằng cung/trident.
- Quan sát bot có set target và đuổi không.
- Kỳ vọng hiện tại: code đã resolve Projectile shooter là Player, nên trường hợp cung/trident cơ bản có set target; vẫn cần test potion/firework/nguồn phức tạp nếu dùng.

Test Case 7 - removeall sau respawn window:
- Spawn bot, giết bot để kích hoạt respawn sau 10 tick.
- Gõ /pvpbot removeall ngay trong khoảng chờ đó.
- Đợi 1-2 giây.
- Quan sát có bot nào xuất hiện lại không.
- Kỳ vọng hiện tại: có rủi ro task cũ tạo lại bot nếu old id không còn trong registry.

==========================================================
📊 --- TỔNG QUAN ---
==========================================================
- Build/cú pháp: PASS.
- Test tự động: Không có test source.
- Lỗi nghiêm trọng nhất: Navigator range/margin + throttle repath làm bot dừng ngoài ATTACK_RANGE 3.5.
- Lỗi combat đi kèm: cancelNavigation khi jump crit và setVelocity cộng knockback làm bot khựng/bay.
- File cần sửa trước:
  - src/main/java/com/khoablabla/pvpbot/movement/BotMovementController.java
  - src/main/java/com/khoablabla/pvpbot/combat/MeleeAttackController.java
  - src/main/java/com/khoablabla/pvpbot/traits/PvPBotTrait.java
- Ưu tiên sửa:
  1. Cấu hình Citizens NavigatorParameters cho melee range thật, không để default dừng ở khoảng 6-8 block.
  2. Đổi throttle repath để xét stuck/progress/distance, không chỉ xét targetLoc.
  3. Không cancel navigation mỗi lần nhảy crit; hoặc quản trị state jump/pursuit rõ ràng.
  4. Clamp velocity khi nhảy crit, tránh cộng knockback thành bay.
  5. Thêm face target và validate line-of-sight trước damage.
  6. Đồng bộ AGENTS.md với Citizens API thật về DAMAGE_BY_PLAYER.
- Tỷ lệ hoàn thành audit: 100%.
- Tỷ lệ ổn định runtime hiện tại theo audit: khoảng 55-65% cho melee AI cơ bản, vì build pass nhưng state machine chase/combat còn lỗi hành vi nặng.
