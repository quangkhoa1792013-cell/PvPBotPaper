┌────────────────────────────────────────────────────────┐
│             🎮 PvPBot — Development Report             │
│  Source of Truth — Do NOT overwrite until 100% PASS!   │
└────────────────────────────────────────────────────────┘

==========================================================
🛡️ --- CÁC LỖI CẦN XỬ LÝ ---
==========================================================
- [ ] Lỗi 1: Project skeleton chưa tồn tại — Cần tạo plugin.yml, main class PvPBot.java, và trait PvPBotTrait.java từ đầu.
- [ ] Lỗi 2: Plugin chưa kiểm tra sự tồn tại của Citizens khi enable — Cần self-disable nếu Citizens vắng mặt.
- [ ] Lỗi 3: Trait chưa có lifecycle hooks (onAttach, onSpawn, onDespawn, run) — Cần implement với log đầy đủ.
- [ ] Lỗi 4: Trait chưa thiết lập vulnerability (setProtected(false), DAMAGE_BY_PLAYER=true) cho combat NPC.
- [ ] Lỗi 5: run() thiếu tick throttle — Cần throttle mỗi 100 ticks để tránh CPU spike.
- [ ] Lỗi 6: build.gradle dùng sai repository Citizens (old: repo.citizensnpcs.co + ivy CI) và sai GAV coordinates (old: citizens:2.0.43-b4216) — Cần sửa thành maven.citizensnpcs.co/repo và net.citizensnpcs:citizens-main:2.0.35-SNAPSHOT với exclude group/module.

==========================================================
📂 --- NHỮNG FILE SẼ SỬA ---
==========================================================
Các file sẽ sửa:
- src/main/resources/plugin.yml (tạo mới)
- src/main/java/com/khoablabla/pvpbot/PvPBot.java (tạo mới)
- src/main/java/com/khoablabla/pvpbot/traits/PvPBotTrait.java (tạo mới)
- build.gradle (sửa — repository + dependency coordinates + exclude)

Mục đích:
- Thiết lập project skeleton cho PvPBot plugin trên nền Paper 1.21.11 với Citizens hard dependency; cập nhật Maven repository và GAV coordinates theo hướng dẫn mới từ A1.

Để làm gì:
- Định nghĩa entry point, khai báo dependencies, đăng ký custom Citizens trait; trỏ đúng repository và dependency cho Citizens.

Cho cái gì:
- Plugin yml, main vessel class, trait class, Gradle build script.

Cho chức năng nào:
- Khởi tạo plugin, kiểm tra dependency, đăng ký trait, lifecycle logging, tick throttle; build hệ thống với dependency chính xác.

Chức năng đó làm gì:
- PvPBot.java: Kiểm tra Citizens khi enable, đăng ký trait, log ProtocolLib status; disable plugin nếu thiếu Citizens.
- PvPBotTrait.java: Lifecycle hooks theo dõi attach/spawn/despawn; unprotected NPC; tick throttle mỗi 100 ticks log health.
- build.gradle: Repository https://maven.citizensnpcs.co/repo; dependency net.citizensnpcs:citizens-main:2.0.35-SNAPSHOT với exclude group/module all.

==========================================================
💾 --- CÁC FILE ĐÃ SỬA ---
==========================================================
Các file đã sửa:
- src/main/resources/plugin.yml
- src/main/java/com/khoablabla/pvpbot/PvPBot.java
- src/main/java/com/khoablabla/pvpbot/traits/PvPBotTrait.java
- build.gradle

Ở các dòng nào (chỉ ghi số dòng):
- plugin.yml: 1-7
- PvPBot.java: 1-36
- PvPBotTrait.java: 1-61
- build.gradle: 18 (repo), 23-25 (dependency)

Nhu the nao:
- plugin.yml: Khai báo name=PvPBot, main class, depend=[Citizens], softdepend=[ProtocolLib].
- PvPBot.java: Kiểm tra isPluginEnabled("Citizens"), self-disable nếu thiếu; registerTrait với TraitInfo.create; log ProtocolLib detection.
- PvPBotTrait.java: extends Trait; constructor super("pvpbot"); onAttach kiểm tra entity instanceof Player; onSpawn setProtected(false) + DAMAGE_BY_PLAYER=true; onDespawn log; run() throttle 100 ticks log health.
- build.gradle: dòng 18 — sửa repository thành https://maven.citizensnpcs.co/repo; dòng 23-25 — dependency đổi thành net.citizensnpcs:citizens-main:2.0.35-SNAPSHOT với exclude group: '*', module: '*'.
