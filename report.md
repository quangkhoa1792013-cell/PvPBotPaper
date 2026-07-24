┌────────────────────────────────────────────────────────┐
│             🎮 PvPBot — Development Report             │
│  Source of Truth — Do NOT overwrite until 100% PASS!   │
└────────────────────────────────────────────────────────┘

==========================================================
🛡️ --- CÁC LỖI CẦN XỬ LÝ ---
==========================================================
- [x] Lỗi 1: `SettingsRegistry.reg()` diamond operator `<>` type inference fails — `Class<?>` + `Object def` prevents generic inference in Java 25. → Changed to generic `<T> void reg(String key, Class<T> type, T def)` and `<T extends Number> void reg(...)`. **PASS — compile clean 0 errors.**

==========================================================
📂 --- NHỮNG FILE SẼ SỬA ---
==========================================================
Các file sẽ sửa:
- src/main/java/com/khoablabla/pvpbot/config/SettingsRegistry.java
Mục đích:
- Fix compilation error in reg() helper methods (diamond operator type inference)
Để làm gì:
- Enable Java 25 compiler to infer generic parameter T from arguments
Cho cái gì:
- SettingsRegistry class
Cho chức năng nào:
- Unified Configuration Framework (Phase 4)
Chức năng đó làm gì:
- Dual-mode settings engine with global config.yml persistence and per-NPC local overrides

==========================================================
💾 --- CÁC FILE ĐÃ SỬA ---
==========================================================
Các file đã sửa:
- src/main/java/com/khoablabla/pvpbot/config/SettingsRegistry.java
Ở các dòng nào (chỉ ghi số dòng):
- Dòng 207-213
Nhu the nao:
- `private void reg(String key, Class<?> type, Object def)` → `private <T> void reg(String key, Class<T> type, T def)`
- `private void reg(String key, Class<?> type, Object def, Number min, Number max)` → `private <T extends Number> void reg(String key, Class<T> type, T def, T min, T max)`

==========================================================
🔍 --- DEBUG (Khu vực dành riêng cho A3) ---
==========================================================
Các lỗi nêu trên: 
- [x] Lỗi 1: Diamond operator type inference error [Đánh dấu [x] nếu đạt, [/] nếu chưa triệt để, [ ] nếu chưa sửa]

==========================================================
📋 --- HƯỚNG DẪN KIỂM THỬ (TEST CASES) --- (Do A3 viết)
==========================================================
### Bước 1: Cài đặt & Khởi động
- Copy file JAR `PvPBotPaper-1.0.0.jar` từ `build/libs/` vào `plugins/`.
- Gõ `/reload confirm` hoặc khởi động lại máy chủ.

### Bước 2: Kiểm thử gợi ý chat & chạy nhảy (Gõ lệnh theo trình tự)
1. Gõ `/pvpbot settings <TAB>` -> Xác nhận: Game gợi ý đầy đủ 44+ phím cài đặt.
2. Gõ `/pvpbot settings criticals <TAB>` -> Xác nhận: Game gợi ý đúng 2 lựa chọn `true`, `false`. Không gợi ý bất kỳ chuỗi rác nào!
3. Gõ `/pvpbot settings attack-cooldown <TAB>` -> **Xác nhận tuyệt đối:** Game KHÔNG gợi ý bất kỳ văn bản nào trong danh sách thả xuống, để bạn tự do gõ một con số (Ví dụ: `15`). Khung chat hiển thị gợi ý mờ chuẩn hệ thống!
4. Gõ `/pvpbot settings bhop false` -> Tắt B-hop. Tạo bot và chém bot -> Quan sát: Bot đuổi theo bạn hoàn toàn bằng cách chạy bộ sát đất bằng vận tốc cơ bản.
5. Gõ `/pvpbot settings bhop true` -> Bật B-hop. Chạy ra xa khỏi bot (> 5 blocks) -> Quan sát: Bot tự động bứt tốc chạy nhảy cực kỳ nhanh và dồn dập (tốc độ được tăng thêm 30%) để áp sát bạn, không bị kẹt lơ lửng!

==========================================================
⚠️ --- CÁC LỖI PHÁT SINH KHÁC KHÔNG TRONG DANH SÁCH OR TRONG CODE NGẦM ---
==========================================================
Các lỗi xuất hiện:
- (none yet)
File nào: 
- 
Ở dòng nào: 
- 
Ảnh hưởng: 
- 
Hệ quả: 
- 

==========================================================
📊 --- TỔNG QUAN --- (Do A3 viết)
==========================================================
- Những lỗi đã sửa: 
   1. `SettingsRegistry.java:207-213`: Diamond operator type inference failure fixed.
   2. Phase 4.1.2: B-hop speed boost (`bhop=true && distance>5.0 → speed*1.3`). Clean autocomplete (0 hint strings, boolean true/false only). Remove bot name suggestions removed.
- Lỗi chưa sửa: KHÔNG CÒN LỖI NÀO (0 errors). Build compile sạch.
- Đã sửa những gì, ở file nào: 
   - `SettingsRegistry.java`: `reg()` signatures type-safe `<T>` và `<T extends Number>`.
   - `BotMovementController.java`: B-hop logic — `bhop` setting, `distance>5.0` gate, `*1.3` multiplier, `effectiveSpeed` float.
   - `PvPBotCommand.java`: `onTabComplete()` overhaul — xoá hint strings, boolean→true/false, numeric→List.of(), remove→List.of().
- Lỗi đấy ở đâu: 
   - `SettingsRegistry.java` dòng 207-213
   - `BotMovementController.java` dòng 39-50
   - `PvPBotCommand.java` dòng 366-415
- Lỗi đấy như thế nào: 
   - Generic: Java 25 không suy diễn `T` từ `Class<?>` và `Object def`.
   - B-hop: Setting registered nhưng không đọc → bot không speed boost. Thêm guard `bhop && distance>5.0`.
   - Autocomplete: Hint strings clickable gây khó chịu. Xoá, chỉ boolean keys có gợi ý.
- Tỷ lệ hoàn thành nhiệm vụ: **100 %**

==========================================================
🛡️ --- PHASE 4.1.1 — BỔ SUNG (Functional Settings — Combat Core) ---
==========================================================
- [x] Lỗi 1: `combat` setting không có effect → bot luôn combat dù tắt.
- [x] Lỗi 2: `revenge` setting không có effect → bot luôn revenge khi bị đánh.
- [x] Lỗi 3: Không có auto-target scanner → bot không tự tìm target khi idle.
- [x] Lỗi 4: `target-players`/`target-bots` không được check → bot target bất kỳ ai.

📂 --- FILE SỬA (Phase 4.1.1) ---
==========================================================
Các file sửa:
- CombatTargetSelector.java (validateTarget mới + scanForTarget mới)
- PvPBotTrait.java (combat gate + auto-target scan + pass filter params)
- PlayerSimulationListener.java (revenge + combat gate)

💾 --- FILE ĐÃ SỬA (Phase 4.1.1) ---
==========================================================
- CombatTargetSelector.java: validateTarget thêm params targetPlayers/targetBots + filter logic. Thêm scanForTarget() mới: getNearbyEntities, filter theo targetPlayers (LOS + gamemode) và targetBots (PvPBotTrait check), trả về closest.
- PvPBotTrait.java: run() check combat=false → clear target, cancel nav, idle wander. 10-tick block: nếu target=null và auto-target=true → scanForTarget. validateTarget gọi với targetPlayers/targetBots.
- PlayerSimulationListener.java: onEntityDamageByEntity check revenge+combat trước khi setTarget.

==========================================================
🛡️ --- PHASE 4.1.2 — BỔ SUNG (Combat Attributes, B-hop & Clean Autocomplete) ---
==========================================================
- [x] Lỗi 1: `bhop` setting không có effect → bot không có speed boost. **PASS — bhop=true && distance>5.0 → speed*1.3.**
- [x] Lỗi 2: Tab completion trả về hint strings clickable (`<integer 1-40>`) → khó chịu, không native. → Xoá hết, trả về `List.of()` cho free-form inputs. **PASS — 0 hint strings.**
- [x] Lỗi 3: Tab completion remove gợi ý bot names → không cần thiết, gây nhiễu. → Xoá. **PASS — remove subcommand trả về List.of().**
- [x] Lỗi 4: **Compilation** — `./build.sh` 0 errors, 0 deprecation, 0 static analysis. JAR 39KB. **PASS — 100% clean.**

🔍 --- AUDIT NOTES (Phase 4.1.2) ---
- **B-hop speed scaling:** `BotMovementController.java:46`: `if (bhop && distance > 5.0) effectiveSpeed = (float)(baseMoveSpeed * 1.3)`. Type-safe `(float)` cast. ✅
  - B-hop chỉ active khi ngoài 5 blocks (khoảng cách đuổi), khi đến gần bot tự slowdown về base speed. ✅
  - Không dùng `setVelocity()`, không conflict với Citizens Navigator. ✅
- **Clean autocomplete:** `PvPBotCommand.java:366-415`: 0 hint strings. Boolean keys → `true`/`false`. Numeric keys → `List.of()`. ✅
- **Edge case:** Nếu player gõ `/pvpbot remove <TAB>` → không gợi ý gì (phải gõ tên chính xác). Design decision per Lỗi 3.

📂 --- FILE SỬA (Phase 4.1.2) ---
==========================================================
Các file sửa:
- BotMovementController.java (bhop logic + dynamic speed)
- PvPBotCommand.java (clean autocomplete overhaul)

💾 --- FILE ĐÃ SỬA (Phase 4.1.2) ---
==========================================================
- BotMovementController.java: Thêm bhop setting. Nếu bhop=true && distance>5.0 → effectiveSpeed = baseMoveSpeed * 1.3. Nếu không → effectiveSpeed = baseMoveSpeed. Pass effectiveSpeed vào configureCombatNavigation.
- PvPBotCommand.java: onTabComplete viết lại hoàn toàn. args.length=1 → subcommands. length=2 + settings → keys; length=2 + other → List.of(). length=3/4 + settings + boolean → true/false; length=3/4 + settings + numeric → List.of(). Không còn hint strings.
