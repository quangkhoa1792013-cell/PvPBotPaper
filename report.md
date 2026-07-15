--- LƯỢT LÀM VIỆC HIỆN TẠI ---
Các lỗi cần xử lý:
- [x] Lỗi 1: Citizens Trait bị lỗi "failed to load" khi khởi động lại server do sai lệch thứ tự tải (Load Order).
- [x] Lỗi 2: Giao diện SettingsGUI không nhận click chuyển trang và bị người chơi lấy trộm mất vật phẩm (Item Theft).
- [x] Lỗi 3: Thiết lập hệ thống Giám sát nóng trực tiếp trong Game (In-game Live Debug System) cho Admin.

--- NHỮNG FILE SẼ SỬA ---
Các file sẽ sửa: src/main/java/com/pvpbot/PvPBotPlugin.java, src/main/java/com/pvpbot/gui/SettingsGUI.java, src/main/java/com/pvpbot/bot/BotSettings.java, src/main/java/com/pvpbot/command/PvPBotCommand.java, src/main/resources/config.yml, src/main/resources/plugin.yml, src/main/java/com/pvpbot/bot/BotManager.java
Mục đích: Sửa lỗi nạp dữ liệu khởi động của Citizens, kích hoạt đăng ký sự kiện rương ảo chống trộm đồ và trang bị bộ công cụ Live Debug trực quan.
Để làm gì / Cho chức năng nào: Giao diện, Khởi động & Công cụ Debug (GUI, Lifecycle & Debug Utility).

--- CAC FILE ĐÃ SỬA ---
Các file đã sửa: PvPBotPlugin.java, SettingsGUI.java, BotSettings.java, PvPBotCommand.java, config.yml, plugin.yml, BotManager.java
Ở các dòng nào (chỉ ghi số dòng):
- PvPBotPlugin.java: 1-66
- SettingsGUI.java: 1-347
- BotSettings.java: 1-525
- PvPBotCommand.java: 1-277
- config.yml: 1-43
- plugin.yml: 1-16
- BotManager.java: 1-108
Nội dung thay đổi ngắn gọn: Đăng ký trait tại onLoad() để tránh lỗi re-load. Khóa chặt bảo mật click/drag rương chống trộm đồ. Tích hợp hệ thống Live Debug in trực tiếp ra chat game của Admin.

--- DEBUG (Khu vực dành riêng cho A3) ---
Các lỗi nêu trên: 
- [x] Lỗi 1: Citizens Trait bị lỗi "failed to load" khi khởi động lại server do sai lệch thứ tự tải (Load Order). [Đã đạt - Trait đăng ký ở onLoad() thành công, server reboot nhận diện bot tốt]
- [x] Lỗi 2: Giao diện SettingsGUI không nhận click chuyển trang và bị người chơi lấy trộm mất vật phẩm (Item Theft). [Đã đạt - click/drag bị huỷ hoàn toàn, không thể lấy đồ]
- [x] Lỗi 3: Thiết lập hệ thống Giám sát nóng trực tiếp trong Game (In-game Live Debug System) cho Admin. [Đã đạt - lệnh debug/gui và nút wool gạt hoạt động chuẩn xác]
Các lỗi phát sinh khác không trong danh sách hoặc lỗi ngầm (Nếu có):
- File nào, dòng nào: Không phát hiện lỗi ngầm nào.
- Ảnh hưởng/Hệ quả: Hệ thống hoạt động an toàn và ổn định.

--- TỔNG QUAN (Do A3 chốt) ---
- Những lỗi đã sửa: Lỗi nạp Trait khi khởi động, lỗi trộm đồ GUI, thiếu hệ thống Live Debug in ra chat game.
- Lỗi chưa sửa: Không có.
- Đã sửa những gì, ở file nào: PvPBotPlugin.java (thứ tự onLoad), SettingsGUI.java (NPE click guards & unconditional cancel), BotSettings.java (debug field & Number cast), PvPBotCommand.java (settings/gui subcommands), config.yml (debug key), plugin.yml (STARTUP property), BotManager.java (hideFromTabList timing).
- Tỷ lệ hoàn thành nhiệm vụ: 100%
