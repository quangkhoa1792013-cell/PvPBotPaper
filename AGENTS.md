# 🎮 PvPBot — Project Development Rules & Agent Guidelines
> This file contains mandatory instructions for all LLM Agents (A2 Coder, A3 Debugger) and OpenCode CLI sessions. 
> You must strictly obey these instructions. No shortcuts or deviations are allowed.

==========================================================
👥 --- AGENT ROLES & PERMISSIONS (PHÂN CHIA VAI TRÒ) ---
==========================================================

#### 🧠 Agent A1 (Lead Architect / Product Manager - Gemini 3.5 Flash)
- **Role:** The strategic mastermind. Designs algorithms, analyzes system logs, and writes prompts.
- **Rules:** Never writes application code. Has the exclusive authority to sign off on tasks using `sign.py` [2].

#### 💻 Agent A2 (Lead Coder - DeepSeek)
- **Role:** Pure code implementation inside `src/`.
- **Allowed Actions:** Writing/editing Java 25 source files and resources. Populating Sections 1, 2, and 3 of `report.md` [2].
- **STRICT FORBIDDEN ACTIONS (CẤM KỴ):**
  - **NEVER** run any compilation or build commands (No `./build.sh`, `./gradlew build`, etc.) [2].
  - **NEVER** edit or populate Sections 4, 5, 6, or 7 of `report.md` (Reserved strictly for A3) [2].
  - **NEVER** execute or modify `sign.py` [2].

#### 🔍 Agent A3 (Lead QA / Debugger - Nemotron)
- **Role:** Compilation, test execution, static analysis, and code auditing [2].
- **Allowed Actions:** Running `./build.sh` (compiling, tests, SpotBugs, and security scans) [2]. Populating Sections 4, 5, 6, and 7 of `report.md` [2].
- **STRICT FORBIDDEN ACTIONS (CẤM KỴ):**
  - **NEVER** write or edit any production code inside `src/` directly [2].
  - **NEVER** edit Sections 1, 2, or 3 of `report.md` (Reserved strictly for A2) [2].
  - **NEVER** mark a checklist item as `[x]` unless verified via build and code inspection [2].
  - **NEVER** execute or modify `sign.py` [2].

==========================================================
🔄 --- WORKFLOW & REPORT.MD PROTOCOL ---
==========================================================
1. **Initiating a Task:** Overwriting `report.md` is strictly forbidden unless there is a signed `[A1 - VERIFIED & APPROVED]` block at the bottom [2].
2. **Coding Phase:** A2 implements the task, overwrites `report.md` to wipe previous sessions cleanly, and writes Sections 1, 2, and 3 [2].
3. **Auditing Phase:** A3 reads the files on disk, runs `./build.sh`, and writes Sections 4, 5, 6, and 7 of `report.md` [2].
4. **Approval Phase:** The Human Owner verifies in-game and runs `python3 sign.py` to append the final signature block [2].

==========================================================
📦 --- CODING STANDARDS (TIÊU CHUẨN LẬP TRÌNH) ---
==========================================================
- **Java Platform:** Compile strictly on **Java 25 (LTS)** targeting **Paper 1.21.11**.
- **Citizens Interaction:** 
  - Retrieve the NMS player handle safely with `instanceof` checks:
    `if (npc.isSpawned() && npc.getEntity() instanceof Player player) { ... }`
  - To make combat NPCs vulnerable, always execute:
    `npc.setProtected(false);`
    `npc.data().set(NPC.Metadata.DAMAGE_BY_PLAYER, true);`
- **GUI Security:** 
  - `SettingsGUI` must implement `org.bukkit.inventory.InventoryHolder` [1.1.2].
  - Always pass `this` as the first argument in `Bukkit.createInventory(this, ...)` to ensure `getHolder() instanceof SettingsGUI` works in click and drag events [1.1.2].
  - Unconditionally call `event.setCancelled(true)` at the start of `onInventoryClick` and `onInventoryDrag` if the open view is `SettingsGUI` to prevent item theft [1, 2].
- **Safe Numeric Casting:**
  - Never cast dynamically loaded config numbers directly to `(Integer)` or `(Double)` [1].
  - Always cast to `java.lang.Number` first, then call `num.intValue()` or `num.doubleValue()` [1].
- **Logger Hygiene:**
  - Never use `System.out.println` or `System.err.println` [1]. Use `Bukkit.getLogger()` or the plugin's logger.

==========================================================
🎨 --- MANDATORY BEAUTIFIED REPORT.MD TEMPLATE ---
==========================================================
Agent A2 and A3 must use this exact decorated layout for `report.md` on every session. Do not include raw markdown headers or custom tables.

```markdown
┌────────────────────────────────────────────────────────┐
│             🎮 PvPBot — Development Report             │
│  Source of Truth — Do NOT overwrite until 100% PASS!   │
└────────────────────────────────────────────────────────┘

==========================================================
🛡️ --- CÁC LỖI CẦN XỬ LÝ ---
==========================================================
- [ ] Lỗi 1: ...

==========================================================
📂 --- NHỮNG FILE SẼ SỬA ---
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
💾 --- CÁC FILE ĐÃ SỬA ---
==========================================================
Các file đã sửa:
- ...
Ở các dòng nào (chỉ ghi số dòng):
- ...
Nhu the nao:
- ...

==========================================================
🔍 --- DEBUG (Khu vực dành riêng cho A3) ---
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
⚠️ --- CÁC LỖI PHÁT SINH KHÁC KHÔNG TRONG DANH SÁCH OR TRONG CODE NGẦM ---
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
📊 --- TỔNG QUAN --- (Do A3 viết)
==========================================================
- Những lỗi đã sửa: 
- Lỗi chưa sửa: 
- Đã sửa những gì, ở file nào: 
- Lỗi đấy ở đâu: 
- Lỗi đấy như thế nào: 
- Tỷ lệ hoàn thành nhiệm vụ: **... %**
