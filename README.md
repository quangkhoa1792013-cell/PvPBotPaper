🎮 BẢN ĐẶC TẢ KIẾN TRÚC & LỘ TRÌNH TỔNG THỂ DỰ ÁN PVPBOT
I. MỤC TIÊU TỔNG THỂ CỦA DỰ ÁN (PROJECT OBJECTIVES)
Tính năng cốt lõi: Xây dựng một plugin PvP Bot độc lập, chuyên nghiệp cho máy chủ Minecraft, mô phỏng hành vi chiến đấu cận chiến, bắn cung dự đoán tầm xa, ném Wind Charge nhảy cao đập Chùy (Mace combo), tự động mặc đồ/ăn táo vàng/bơm thuốc, lập lộ trình tuần tra (Paths), chia bang nhóm (Factions) và hiển thị Web Dashboard thời gian thực [1.1.1].
Khắc phục nhược điểm viết tay: Chuyển đổi hoàn toàn từ kiến trúc NMS thô sơ viết tay (dễ xung đột, đơ cứng khi bị plugin khác chặn) sang Mô hình lai Citizens2 (Quản lý thân xác thực thể) + ProtocolLib (Xử lý gói tin xoay camera mượt mà) + Trí tuệ AI chiến đấu NMS gốc [1.1.2].
Tương thích & Hiệu năng: Biên dịch hoàn toàn trên Java 25 (LTS) để chạy mượt mà trên Paper 1.21.11, chống giật lag khi mass-spawn tới 50 Bots [1.1.1, 1.1.2].
II. CÔNG NGHỆ & THƯ VIỆN SỬ DỤNG (THE TECH STACK)
Hạt nhân nền tảng: Paper API 1.21.11-R0.1-SNAPSHOT (sử dụng Mojang Mappings qua paperweight-userdev) [1.1.1, 1.1.2].
Citizens API (citizens-api:2.0.35-SNAPSHOT): Đóng vai trò làm khung xương quản lý thực thể NPC dạng PLAYER. Chịu trách nhiệm lo khâu kết nối mạng giả lập, đăng ký thực thể trong world, chống kẹt xác và chống crash [1.1.2].
ProtocolLib (ProtocolLib:5.5.0-SNAPSHOT): Thư viện can thiệp gói tin (Packets). Chịu trách nhiệm xử lý các gói tin xoay đầu (ClientboundRotateHeadPacket) một cách mượt mà theo chỉ số aim-speed [1.1.2].
SQLite JDBC (org.xerial:sqlite-jdbc): Cơ sở dữ liệu thời gian thực cục bộ (metrics.db) lưu giữ lịch sử hoạt động của Bot, tự động dọn dẹp các bản ghi cũ hơn 30 ngày [1.1.1].
Embedded Web Server (Thuần JDK HttpServer): Máy chủ web siêu nhẹ, phục vụ giao diện HTML/CSS/JS Dashboard tối sử dụng Chart.js (CDN), truyền số liệu thời gian thực qua Server-Sent Events (SSE) tại đầu mút /api/stream [1.1.1].
Bộ công cụ tự động hóa DevOps:
build.sh: Tự động Dọn dẹp -> Quét lỗi bảo mật & SpotBugs logic -> Biên dịch -> Dọn rác tạm thời (giữ lại file .jar thành phẩm) [1.1.1].
push.sh: Gõ nhanh lưu trạng thái và đẩy toàn bộ code lên GitHub [1.1.1].
release.sh: Nhập số phiên bản Semantic, kiểm tra trùng lặp thẻ Tag và tự động kích hoạt đóng gói tạo Release kèm file Jar sạch đẹp dạng PvPBot-<version>.jar trên GitHub [1.1.1].
sign.py: Lớp bảo vệ tự động của A1. Chỉ cho phép đóng dấu ký duyệt PASS khi A3 báo cáo tiến độ đạt 100% [1.1.3].
AGENTS.md: Tệp hiến pháp tối cao chứa toàn bộ phân vai, tiêu chuẩn code và biểu mẫu rương ảo trang trí chuẩn đẹp của dự án [1.1.3].
III. CẤU TRÚC ĐIỀU KHIỂN & VẬN HÀNH NGẦM (CONTROL STRUCTURE)
Để Bot di chuyển lắt léo và chiến đấu thông minh, chúng ta áp dụng mô hình liên kết bắc cầu:
code
Text
[Citizens NPC Entity (PLAYER)] 
      │ (onAttach)
      ▼
[PvPBotTrait.java (Ticking @ 20 TPS)]
      │ (Mỗi tick: Lấy ra ServerPlayer gốc)
      ▼
   nmsPlayer = ((CraftPlayer) player).getHandle();
      │
      ├─► [CombatTargetSelector.java] (Quét tìm mục tiêu, kiểm tra Faction)
      ├─► [BotMovementController.java] (B-Hop, Combat Strafe, Citizens Navigator)
      ├─► [SurvivalController.java] (Auto-Equip, Auto-Eat, Auto-Potion, Shield Block)
      └─► [MaceComboController.java] (Wind Charge Jump -> Mace Smash Combo)
Dữ liệu Cấu hình kép (Dual-Mode Settings): Tách biệt hai chế độ chỉnh sửa:
Mặc định toàn cầu (Global Defaults): Chỉnh sửa qua /pvpbot gui hoặc /pvpbot settings mà không chỉ định Bot. Thay đổi sẽ lưu đè vào config.yml trên ổ cứng [1.1.3].
Chỉnh nóng thời gian thực (Per-NPC Settings): Chỉnh sửa qua /pvpbot gui <tên_bot>. Thay đổi sẽ ghi trực tiếp vào RAM của Trait Bot đó, không làm ảnh hưởng đến file config gốc [1.1.3].
IV. SƠ ĐỒ NHÂN SỰ & QUY TRÌNH PHỐI HỢP (AGENTS WORKFLOW)
Hệ thống vận hành xoay vòng khép kín theo đúng tệp cấu hình tối cao AGENTS.md [1.1.3]:
Agent A1 (Kiến trúc sư trưởng - Tôi): Lắng nghe ý kiến của bạn, thiết kế thuật toán, soạn thảo Prompt cực dài chỉ đạo cấu trúc cho A2 và thực hiện ký duyệt PASS [1.1.3].
Agent A2 (Thợ code - DeepSeek - tab viet code): Chỉ tập trung viết code Java sạch sẽ vào thư mục src/ [1.1.3]. Code xong tự giác dọn sạch report.md cũ và điền thông tin vào các mục 1, 2, 3 (Tuyệt đối cấm tự tiện chạy build) [1.1.3].
Agent A3 (QA/Debugger - Nemotron - tab debug bot): Túc trực quét thư mục nguồn, gõ lệnh ./build.sh biên dịch kiểm thử Bytecode và SpotBugs [1.1.1, 1.1.3]. Tự động điền các checklist đạt [x] và chốt bảng TỔNG QUAN đạt bao nhiêu % tiến độ vào tệp report.md [1.1.3].
Chủ dự án (Bạn): Vào game chạy thử nghiệm thực tế dựa trên UAT Test Cases của A3 viết [1.1.3]. Khi thấy mọi thứ hoàn hảo, gõ lệnh python3 sign.py để A1 ký duyệt chính thức và đóng lại Phase [1.1.3].
V. LỘ TRÌNH PHÁT TRIỂN 6 PHASES CHI TIẾT (THE MASTER ROADMAP)
✅ Phase 1: Project Setup & Citizens Base Framework (ĐÃ HOÀN THÀNH)
Mục tiêu: Nạp thư viện Citizens + ProtocolLib trên Java 25 [1.1.1, 1.1.2]. Đăng ký Trait PvPBotTrait chuẩn xác lúc khởi động máy chủ [1.1.2].
Kết quả: Spawn được Bot đứng thẳng, chịu trọng lực, tự đi lại được bằng bộ điều hướng không bị ngắt kết nối mạng [1.1.1].
✅ Phase 2: Settings & Ultimate Settings GUI (ĐÃ HOÀN THÀNH)
Mục tiêu: Tích hợp cấu hình 44 thuộc tính, rương ảo 9 trang đẹp mắt, chống trộm đồ/dupe đồ tuyệt đối, chuẩn hóa chữ viết thường và tích hợp hệ thống Live Debug in trực tiếp ra chat game của Admin [1.1.1, 1.1.3].
Kết quả: Điều khiển mượt mà thông số của Bot nóng thời gian thực hoặc toàn cầu thông qua click chuột [1.1.3].
🔄 Phase 3: Melee Combat & Advanced Movement AI (Sắp tiến hành)
Mục tiêu:
Kích hoạt cơ chế nhận sát thương của Citizens bằng cách tắt thuộc tính bảo vệ mặc định [1.1.2].
Tự động quét mục tiêu trong tầm nhìn, lọc bỏ đồng đội cùng Factions nếu thân thiện tắt [1.1.1].
Lập trình nhảy đánh chí mạng (Crits), tự mặc giáp/vũ khí mạnh nhất, tự gạt rìu phá khiên đối thủ [1.1.1].
Lập trình di chuyển B-Hop tăng tốc và di chuyển lắt léo vòng tròn (Combat strafing) tránh né đòn đánh cận chiến [1.1.1].
Lập trình cỗ máy trạng thái nâng/hạ khiên thông minh chặn đòn cận chiến/tầm xa và tự hạ khiên [1.1.1].
🏹 Phase 4: Ranged AI, Arrow Prediction & Mace Combo (Tương lai)
Mục tiêu:
Lập trình ngắm bắn Cung/Nỏ thông minh dự báo đón đầu tọa độ di chuyển của đối thủ kết hợp bù góc rơi trọng lực (ArrowPrediction) [1.1.1].
Lập trình cơ chế giữ khoảng cách di chuyển lắt léo khi bắn cung (Ranged strafe & Ranged retreat) [1.1.1].
Lập trình combo đập Chùy từ trên cao: Nhìn xuống chân -> bắn Wind Charge tạo nổ bay cao -> đổi sang Mace -> khóa đầu đối thủ lúc rơi xuống -> đập Mace sát thương cực đại triệt tiêu sát thương rơi [1.1.1].
🛡️ Phase 5: Auto-Survival & Expanded Subsystems (Tương lai)
Mục tiêu:
Tích hợp bộ sinh tồn nâng cao: tự ăn táo vàng khi HP thấp, tự đặt Totem tay phụ khi hấp hối, tự ném các bình thuốc hồi máu/tăng sức mạnh/tốc độ/kháng lửa, ném XP sửa giáp mending, và tự đặt bẫy mạng nhện làm chậm đối thủ [1.1.1].
Tích hợp hệ thống Kit lưu trữ hòm đồ, Path tuần tra Waypoints nối hạt particles và phân phối bot đều dọc đường đi [1.1.1].
Tích hợp bang hội (Factions): lệnh nhóm tấn công, Friendly Fire toggle, dịch chuyển cả bang hội từ từ tránh lag [1.1.1].
📊 Phase 6: SQLite, Web Dashboard, Velocity & CI/CD (Tương lai)
Mục tiêu:
Thiết lập database SQLite lưu trữ lịch sử, nạp dữ liệu nhịp tim 5 giây gửi lên Web Server tích hợp thông qua cơ chế Server-Sent Events (SSE) hiển thị biểu đồ Chart.js trực quan [1.1.1].
Tích hợp chế độ chạy trên Velocity Proxy để thu thập dữ liệu tập trung [1.1.1].
Đồng bộ hóa 3 tệp tin script build tự động và quy trình CI/CD hoàn chỉnh trên GitHub [1.1.1].

VI. QUY CHẾ VẬN HÀNH & KỶ LUẬT THÉP CỦA CÁC AGENT
Hệ thống hoạt động dựa trên sự phân quyền tuyệt đối và chế độ kiểm soát chéo (Cross-check) nhằm triệt tiêu hoàn toàn lỗi ngầm trước khi đóng gói sản phẩm [2]:
1. Phân định ranh giới hai tệp tin hạt nhân (The Documents Boundary)
AGENTS.md (Hiến pháp tối cao - CHỈ ĐỌC):
Bản chất: Là tệp hướng dẫn hệ thống, tiêu chuẩn code và chứa khuôn mẫu trang trí báo cáo chuẩn đẹp của dự án [2, 3].
Kỷ luật: Nghiêm cấm tuyệt đối tất cả các Agent tự ý sửa đổi, ghi đè hay xóa bỏ tệp này [3]. Đây là tài sản độc quyền của Bạn và A1 [3].
report.md (Hồ sơ làm việc thực tế - GHI ĐÈ LIÊN TỤC):
Bản chất: Là tệp nháp để ghi nhận tiến độ, tệp này bắt buộc phải được ghi đè (Overwrite) sạch sẽ ở mỗi lượt làm việc mới để tránh quá tải bộ nhớ (Token Bloat) cho AI [2].
2. Chu kỳ vận hành 5 bước khép kín (The 5-Step Operational Loop)
Mọi Phase hay phiên sửa lỗi đều bắt buộc phải tuân thủ nghiêm ngặt trình tự 5 bước sau để đảm bảo chất lượng phần mềm tốt nhất [2, 3]:
code
Text
[BƯỚC 1: A1 Thiết kế] ──► [BƯỚC 2: A2 Viết Code] ──► [BƯỚC 3: A3 Thẩm Định & UAT]
                                                               │
  [BƯỚC 5: A1 Ký Duyệt] ◄── [BƯỚC 4: Bạn Nghiệm Thu] ◄─────────┘
🛠️ BƯỚC 1: Thiết kế giải thuật (Do A1 thực hiện)
Tôi (A1) thảo luận với bạn về tính năng mới hoặc các bug phát sinh [3].
Tôi viết một bản Prompt cực kỳ chi tiết, ép cấu trúc thuật toán chiến đấu cận chiến/di chuyển và gửi cho bạn để chuyển tiếp sang tab viet code (A2) [3].
💻 BƯỚC 2: Lập trình mã nguồn sạch (Do Agent A2 - DeepSeek thực hiện)
A2 đọc tệp AGENTS.md ở thư mục gốc để tự nạp luật chơi và tiêu chuẩn code [3].
A2 xóa sạch hoàn toàn tệp report.md cũ [2].
A2 truy cập mục MANDATORY BEAUTIFIED REPORT.MD TEMPLATE trong AGENTS.md, copy nguyên xi cái khung trang trí rương ảo lộng lẫy đó dán đè vào tệp report.md mới [2].
A2 tiến hành viết/sửa code Java trong thư mục src/ [1].
A2 điền đầy đủ thông tin vào Phần 1, Phần 2, và Phần 3 của tệp report.md mới tạo [2].
KỶ LUẬT THÉP: A2 tuyệt đối CẤM gõ các lệnh biên dịch/build (như ./build.sh hay compileJava) và CẤM viết vào các phần dưới của report.md [2]. Sau khi lưu code xong, A2 bắt buộc phải DỪNG LẠI NGAY để bàn giao [2]!
🔍 BƯỚC 3: Biên dịch & Kiểm thử tĩnh (Do Agent A3 - Nemotron thực hiện)
Bạn mở cửa sổ chat debug bot (A3) và gõ ngắn gọn lệnh: @report.md [2].
A3 đọc toàn bộ mã nguồn mới sửa đổi của A2, tự động gõ lệnh chạy ./build.sh để kiểm thử biên dịch và chạy SpotBugs kiểm tra lỗi logic chìm [2].
A3 tự động điền các đánh giá vào phần còn lại của tệp report.md [2]:
Đánh dấu đạt [x] cho các lỗi đã được sửa thật sự trong mục DEBUG [2].
Tự viết một bản UAT HƯỚNG DẪN KIỂM THỬ chi tiết từng bước, từng trường hợp cụ thể để bạn làm theo [2].
Bóc tách và ghi nhận các lỗi ngầm, lỗi phát sinh ngoài ý muốn (nếu phát hiện) vào mục LỖI PHÁT SINH [2].
Chốt số % tiến độ hoàn thành thực tế vào mục TỔNG QUAN (Nếu có lỗi ngầm, A3 kéo tụt tiến độ xuống 60-80% để cảnh cáo) [2].
KỶ LUẬT THÉP: A3 tuyệt đối CẤM sửa đổi code trong src/ và CẤM tự ý sinh chữ ký phê duyệt [2]. Ghi tệp xong, A3 bắt buộc phải DỪNG LẠI NGAY để báo cáo [2]!
🎮 BƯỚC 4: Nghiệm thu thực tế trong game (Do BẠN thực hiện)
Bạn mở tệp report.md lên, nhìn xuống phần HƯỚNG DẪN KIỂM THỬ (TEST CASES) do A3 vừa tự tay soạn thảo [2].
Bạn vào game và gõ đúng các lệnh, bấm đúng các nút, test đúng các trường hợp hệt như hướng dẫn của A3 để tự tay nghiệm thu thực tế [2].
✍️ BƯỚC 5: Tổng duyệt & Đóng dấu (Do Tôi - A1 & BẠN thực hiện)
Bạn mang tệp report.md hoàn chỉnh về gửi cho tôi (A1) duyệt [3].
Nếu mọi thứ đạt 100% hoàn hảo, tôi (A1) sẽ phê duyệt lệnh PASS và chỉ thị cho bạn chạy tệp ký tên [2, 3]:
code
Bash
python3 sign.py
Tệp script sẽ tự động quét, xác nhận tiến độ đạt 100% và đóng dấu chữ ký phê duyệt chính thức của A1 vào cuối tệp report.md của bạn, kết thúc một chu kỳ Phase hoàn mỹ [2]!
🎨 KHUÔN MẪU BÁO CÁO BẮT BUỘC (MANDATORY REPORT TEMPLATE)
(Cấu trúc này đã được nhúng chặt vào AGENTS.md để ép AI tự động sử dụng, không còn tình trạng viết lộn xộn nữa) [2]:
code
Markdown
┌────────────────────────────────────────────────────────┐
│             🎮 PvPBot — Development Report             │
│  Source of Truth — Do NOT overwrite until 100% PASS!   │
└────────────────────────────────────────────────────────┘

==========================================================
🛡️ --- CÁC LỖI CẦN XỬ LÝ --- (Do A1 giao - A2 sao chép)
==========================================================
- [ ] Lỗi 1: ...

==========================================================
📂 --- NHỮNG FILE SẼ SỬA --- (Do A2 khai báo)
==========================================================
Các file sẽ sửa:
- ...
Mục đích:
- ...
Để làm gì:
- ...
Cho cái gì:
- ...
Cho chức năng nào:
- ...
Chức năng đó làm gì:
- ...

==========================================================
💾 --- CÁC FILE ĐÃ SỬA --- (Do A2 khai báo)
==========================================================
Các file đã sửa:
- ...
Ở các dòng nào (chỉ ghi số dòng):
- ...
Nhu the nao:
- ...

==========================================================
🔍 --- DEBUG (Khu vực dành riêng cho A3) --- (Do A3 viết)
==========================================================
Các lỗi nêu trên: 
- [ ] Lỗi 1: ... [Đánh dấu [x] nếu đạt, [/] nếu chưa triệt để, [ ] nếu chưa sửa]

==========================================================
📋 --- HƯỚNG DẪN KIỂM THỬ (TEST CASES) --- (Do A3 viết)
==========================================================
Lệnh cần gõ:
- ...
Trình tự các bước thực hiện:
- ...
Các trường hợp kiểm thử (Test Cases):
- ...

==========================================================
⚠️ --- CÁC LỖI PHÁT SINH KHÁC KHÔNG TRONG DANH SÁCH OR TRONG CODE NGẦM --- (Do A3 phát hiện)
==========================================================
Các lỗi xuất hiện:
- ...
File nào: 
- ...
Ở dòng nào: 
- ...
Ảnh hưởng: 
- ...
Hệ quả: 
- ...

==========================================================
📊 --- TỔNG QUAN --- (Do A3 chốt)
==========================================================
- Những lỗi đã sửa: 
- Lỗi chưa sửa: 
- Đã sửa những gì, ở file nào: 
- Lỗi đấy ở đâu: 
- Lỗi đấy như thế nào: 
- Tỷ lệ hoàn thành nhiệm vụ: **... %**

Cau truc goc:
🎮 Main Features
🤖 Bot Management
Bot spawn with unique names (random name generation)
Mass spawn of up to 50 bots simultaneously
Automatic restoration of bots after a server restart
Synchronization of the bot list with the server
⚔️ Combat System
Advanced combat AI with various tactics
Critical hits when jumping
Many weapon types:
Melee: swords, axes
Ranged: bows, crossbows
Mace with jumping attacks (wind charges)
Crystal PVP (obsidian + crystals)
Anchor PVP (respawn anchor + glowstone)
Auto-potions healing, strength, speed, fire resistance
Shield mechanics - blocking, shield breaking, mace defense prediction
🛡️ Utilities and Survival
Auto-equip armor and weapons
Auto-totem offhand
Auto-shield on low HP
Auto-food
Auto-repair armor with Mending XP bottles
Cobweb use to slow enemies
🚶 Navigation and Movement
Bunny hop for faster travel
Adjustable speed
Wander mode when no target
Retreat on low HP
Smart navigation to targets
Combat strafing
Path following system
👥 Faction System
Faction creation for bots
Hostile relations - set factions as enemies for automatic combat
Member Management - add/remove bots and players
Faction-wide commands - attack, give items, follow paths
Friendly Fire toggle
🎒 Kit System
Creating kits from your inventory
Giving kits to bots and factions
Saving kits across restarts
🛤️ Path System
Create paths for bots to follow
Add waypoints to paths
Loop mode - back-and-forth or circular movement
Walk types - bhop, sprint, or walk
Visual indicators - particles showing path points and lines
Distribute bots evenly along a path
🎯 Realism
Miss Chance
Mistake Chance - attack wrong direction
Shield Break Chance
📊 Live Stats Dashboard
Real-time bot statistics — total spawns, kills, damage, active bots
Live graphs — server activity history with downsampling (500 points max)
Top names — most frequently spawned bot names
All-time records — max online servers and max active bots
Server-Sent Events — instant dashboard updates
History stored in DuckDB — efficient time-series storage, 30-day retention
Backend — standalone Velocity plugin with embedded HTTP server
Dashboard URL: http://pvpbotstats.survivalworld.win/
📋 Commands
Basic Commands
/pvpbot spawn [name]          - Spawn a bot (random name if not specified)
/pvpbot remove <name>         - Remove a bot
/pvpbot removeall             - Remove all bots
/pvpbot reload                - Reload all configurations
Bot Management
/pvpbot bot-management list              - List all active bots
/pvpbot bot-management inventory <name>  - Show bot's inventory
/pvpbot bot-management mass-spawn <count> - Spawn multiple bots (1-50)
/pvpbot bot-management attack <bot> <target> - Order bot to attack
/pvpbot bot-management stop-attack <bot>     - Stop bot from attacking
Settings
/pvpbot settings                          - Show all settings
/pvpbot settings auto-armor [true/false]  - Auto-equip armor
/pvpbot settings auto-weapon [true/false] - Auto-equip weapons
/pvpbot settings drop-armor [true/false]  - Drop worse armor
/pvpbot settings drop-weapon [true/false] - Drop worse weapons
/pvpbot settings drop-distance <1-10>     - Item pickup distance
/pvpbot settings interval <1-100>         - Check interval (ticks)
/pvpbot settings combat [true/false]      - Enable combat
/pvpbot settings revenge [true/false]     - Revenge mode
/pvpbot settings auto-target [true/false] - Auto target search
/pvpbot settings target-players [true/false] - Attack players
/pvpbot settings target-mobs [true/false]    - Attack mobs
/pvpbot settings target-bots [true/false]    - Attack other bots
/pvpbot settings criticals [true/false]      - Critical hits
/pvpbot settings ranged [true/false]         - Use bow/crossbow
/pvpbot settings mace [true/false]           - Use mace
/pvpbot settings attack-cooldown <1-40>      - Attack cooldown (ticks)
/pvpbot settings melee-range <2-6>           - Melee range
/pvpbot settings move-speed <0.1-2.0>        - Movement speed
/pvpbot settings view-distance <5-128>       - Target search range
/pvpbot settings retreat [true/false]        - Retreat when low HP
/pvpbot settings auto-totem [true/false]     - Auto-equip totem
/pvpbot settings totem-priority [true/false] - Totem over shield
/pvpbot settings auto-shield [true/false]    - Auto-use shield
/pvpbot settings shield-break [true/false]   - Break shields with axe
/pvpbot settings shield-break-chance <0-100> - Shield break chance (%)
/pvpbot settings shield-hold-ticks <10-200>  - Max shield hold ticks
/pvpbot settings shield-raise-ticks <2-40>   - Shield raise ticks
/pvpbot settings shield-mace [true/false]    - Raise shield vs mace
/pvpbot settings prefer-sword [true/false]   - Prefer sword over axe
/pvpbot settings auto-eat [true/false]       - Auto-eat food
/pvpbot settings auto-potion [true/false]    - Auto-use potions
/pvpbot settings auto-mend [true/false]      - Auto-repair armor
/pvpbot settings bhop [true/false]           - Bunny hop
/pvpbot settings idle [true/false]           - Wander without target
/pvpbot settings idle-radius <3-50>          - Wander radius
/pvpbot settings friendly-fire [true/false]  - Damage allies
/pvpbot settings miss-chance <0-100>         - Miss chance (%)
/pvpbot settings mistake-chance <0-100>      - Mistake chance (%)
/pvpbot settings aim-speed <3-45>            - Rotation speed
/pvpbot settings special-names [true/false]      - Use special names
/pvpbot settings bot-leave-on-death [true/false] - Remove on death
/pvpbot settings attack-invincible [true/false]  - Attack creative/spectator
/pvpbot settings safe-spawn [true/false]         - Random offset on spawn
/pvpbot settings clear-on-remove [true/false]    - Clear inventory on kill
/pvpbot settings shield-mace [true/false]        - Raise shield vs mace
/pvpbot settings view-distance <5-128>           - Target search range
/pvpbot settings max-mass-spawn <50-10000>       - Max bots per mass-spawn
/pvpbot settings profile-lagg-fix [true/false]   - Prevent lag on bot spawn
Ranged Combat Settings
/pvpbot settings ranged-min-range <3.0-20.0>     - Min bow distance
/pvpbot settings ranged-optimal-range <10.0-50.0> - Ideal bow distance
/pvpbot settings ranged-max-range <15.0-100.0>   - Max bow engagement range
/pvpbot settings bow-draw-ticks <5-100>           - Bow full draw time
/pvpbot settings arrow-prediction [true/false]    - Predictive bow aiming
/pvpbot settings ranged-strafe [true/false]       - Strafe while shooting bow
/pvpbot settings ranged-retreat [true/false]      - Retreat with bow when close
Critical & Misc Settings
/pvpbot settings crit-fall-ticks <1-10>          - Falling ticks for crit
/pvpbot settings shield-hold-ticks <10-200>      - Max shield hold ticks
/pvpbot settings shield-raise-ticks <2-40>       - Shield raise anticipation
/pvpbot settings aim-speed <3.0-90.0>            - Rotation speed
Faction Commands
/pvpbot faction list                          - List all factions
/pvpbot faction create <name>                 - Create a faction
/pvpbot faction delete <name>                 - Delete a faction
/pvpbot faction info <faction>                - Faction information
/pvpbot faction add <faction> <player>        - Add player/bot
/pvpbot faction remove <faction> <player>     - Remove player/bot
/pvpbot faction add-near <faction> <radius>   - Add nearby bots
/pvpbot faction add-all <faction>             - Add all bots
/pvpbot faction hostile <f1> <f2> [true/false] - Set hostile
/pvpbot faction attack <faction> <target>     - All bots attack
/pvpbot faction give <faction> <item>         - Give item to all
/pvpbot faction path start <faction> <path>   - Start path
/pvpbot faction path stop <faction>           - Stop path
/pvpbot faction tp <faction> <x y z|player>   - Teleport entire faction gradually
Faction Kit Commands
/pvpbot faction kit give-kit <faction> <kit>      - Give kit to all members
/pvpbot faction kit give-kit-random <faction> <kit1> <w1>% [<kit2> <w2>% ...] - Give random weighted kit
Kit Commands
/pvpbot kit create-kit <name>                         - Create a kit from your inventory
/pvpbot kit delete-kit <name>                         - Delete a kit
/pvpbot kit kits                                      - List all kits
/pvpbot kit give-kit <player> <kit>                   - Give kit to a player/bot
/pvpbot kit give-kit-near <kit> [radius]              - Give kit to nearby bots
/pvpbot kit give-kit-near-random <radius> <kit1> <w1>% [<kit2> <w2>% ...] - Give random weighted kit to bots within radius
Path Commands
/pvpbot path create <name>                    - Create a new path
/pvpbot path delete <name>                    - Delete a path
/pvpbot path add-point <name>                 - Add waypoint (current pos)
/pvpbot path remove-point <name> [index]      - Remove waypoint
/pvpbot path clear <name>                     - Remove all waypoints
/pvpbot path list                             - List all paths
/pvpbot path info <name>                      - Show path information
/pvpbot path loop <name> <true/false>         - Toggle loop mode
/pvpbot path walk-type <name> <type>          - Set walk type (bhop/sprint/walk)
/pvpbot path show <name> <true/false>         - Toggle visualization
/pvpbot path start <bot> <path>               - Make bot follow path
/pvpbot path stop <bot>                       - Stop bot from following
/pvpbot path distribute <path>                - Distribute bots evenly
/pvpbot path start-near <path> <radius>       - Start path for nearby bots
/pvpbot path stop-all <path>                  - Stop all bots on path
🖼️ Screenshots
Bots in battle 2026-02-04_22 48 36

Faction system изображение изображение

Crystal PVP in action изображение изображение

Mass bot spawn

image
Path system in action изображение изображение

🔗 Links
Modrinth: https://modrinth.com/mod/pvp-bot-fabric
GitHub: https://github.com/Stepan1411/pvp-bot-fabric
Issues: https://github.com/Stepan1411/pvp-bot-fabric/issues
Wiki: https://stepan1411.github.io/pvpbot-docs.html
Live Stats Dashboard: http://pvpbotstats.survivalworld.win/
Have a nice pvp! 🎮
