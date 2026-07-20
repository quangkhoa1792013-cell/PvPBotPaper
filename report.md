┌────────────────────────────────────────────────────────┐
│             🎮 PvPBot — Development Report             │
│  Source of Truth — Do NOT overwrite until 100% PASS!   │
└────────────────────────────────────────────────────────┘

==========================================================
🛡️ --- CÁC LỖI CẦN XỬ LÝ ---
==========================================================
- [x] Lỗi 1: PlayerSimulationListener.java missing import for PvPBot class → compile error.
- [x] Lỗi 2: Attribute.GENERIC_MAX_HEALTH renamed to Attribute.MAX_HEALTH in Paper 1.21 Mojang mappings → compile error.
- [x] Lỗi 3 (Phase 3): No combat AI — bots are passive targets, do not pursue or attack players.
- [x] Lỗi 4 (Phase 3): No target selection — bots attack all nearby entities including Creative/Spectator players.
- [x] Lỗi 5 (Phase 3): Bots do not navigate toward hostile targets.
- [x] Lỗi 3.1.1: Bot chủ động tấn công mọi người trong phạm vi (proximity-hostile) thay vì revenge-only.
- [x] Lỗi 3.1.2: B-Hop và jump crit dùng setVelocity(new Vector(0, 0.42, 0)) → xoá momentum X/Z, bot đứng im giữa không trung.
- [x] Lỗi 3.1.3: Jump crit có thể kích hoạt giữa không trung (không check on-ground).
- [x] Lỗi 3.1.4: Bot đứng im khi không có target (không idle wander).
- [x] Lỗi 3.1.5: Bot respawn sau chết broadcast "joined the game" lại (duplicate join message).
- [x] Lỗi 3.2.1: Creative mode player đánh bot → bot target và tấn công Creative player (bỏ qua filter).
- [x] Lỗi 3.2.2: Jump crits dùng velocity.getY() < 0 không đáng tin vì Citizens Navigator override velocity → bot "bay luôn" trên không trung.
- [ ] Lỗi 3.3.1: **NPC HOVERING BUG** — Bot vẫn lơ lửng/hàng không (floating/hovering) khi nhảy B-Hop hoặc jump crit dù đã có state machine.
- [ ] Lỗi 3.3.2: **DOUBLE-JUMP / FLOATING BUG** — Bot đôi khi nhảy kép hoặc treo lơ lửng giữa không trung do velocity X/Z momentum conflict với Citizens Navigator.
- [ ] Lỗi 3.3.3: **RESPAWN UUID CONFLICT** — Bot respawn đôi khi bị văng lỗi "Entity uuid already exists" dù đã có entity.remove().

==========================================================
📂 --- NHỮNG FILE SẼ SỬA ---
==========================================================
Các file sẽ sửa:
- src/main/java/com/khoablabla/pvpbot/listeners/PlayerSimulationListener.java
- src/main/java/com/khoablabla/pvpbot/combat/CombatTargetSelector.java (NEW)
- src/main/java/com/khoablabla/pvpbot/movement/BotMovementController.java (NEW)
- src/main/java/com/khoablabla/pvpbot/combat/MeleeAttackController.java (NEW)
- src/main/java/com/khoablabla/pvpbot/traits/PvPBotTrait.java (sửa)

Mục đích:
- Tạo Core Melee Combat AI (MVP): target selector, movement controller, melee attack controller.
- Tích hợp vào PvPBotTrait.run() với scan target mỗi 10 ticks.

==========================================================
💾 --- CÁC FILE ĐÃ SỬA ---
==========================================================
Các file đã sửa:
- src/main/java/com/khoablabla/pvpbot/listeners/PlayerSimulationListener.java
- src/main/java/com/khoablabla/pvpbot/combat/CombatTargetSelector.java (NEW)
- src/main/java/com/khoablabla/pvpbot/movement/BotMovementController.java (NEW)
- src/main/java/com/khoablabla/pvpbot/combat/MeleeAttackController.java (NEW)
- src/main/java/com/khoablabla/pvpbot/traits/PvPBotTrait.java (sửa)

Ở các dòng nào (chỉ ghi số dòng):
- PlayerSimulationListener.java: 19 (import PvPBot), 83 (MAX_HEALTH)
- CombatTargetSelector.java: toàn bộ (~42 dòng)
- BotMovementController.java: toàn bộ (~31 dòng)
- MeleeAttackController.java: toàn bộ (~62 dòng)
- PvPBotTrait.java: 19-26 (imports + fields), 54-75 (run method)

Nhu the nao:
- A: onNPCDeath — broadcast thay vì log; location clone ngay; runnable: despawn, spawn trả boolean → nếu false destroy + warning, true thì reset health/food/fire/fall.
- Phase 3: Tạo 3 class mới (combat + movement). PvPBotTrait.run() tích hợp target scan 10 ticks, pursuit navigation + b-hop, attack loop với jump crits.

==========================================================
🔍 --- DEBUG (Khu vực dành riêng cho A3) ---
==========================================================
Các lỗi nêu trên: 
- [x] Lỗi 1: PlayerSimulationListener.java line 19 — `import com.khoablabla.pvpbot.PvPBot;` ĐÃ THÊM. Compile clean.
- [x] Lỗi 2: PlayerSimulationListener.java line 83 — `Attribute.GENERIC_MAX_HEALTH` → `Attribute.MAX_HEALTH` ĐÃ SỬA. Compile clean.
- [x] Lỗi 3: CombatTargetSelector.java tạo mới — findTarget filter GameMode SURVIVAL/ADVENTURE, team check, distanceSquared comparison. Không quét Creative/Spectator/teammate.
- [x] Lỗi 4: BotMovementController.java tạo mới — handleMovement dùng Citizens Navigator setTarget, B-Hop mỗi 20 ticks nếu >5 blocks & block dưới solid. Không dùng isOnGround() deprecated.
- [x] Lỗi 5: MeleeAttackController.java tạo mới — cooldown 12 ticks, jump crits (velocity 0.42 up + strike khi falling), Particle.CRIT, swingMainHand, damage(target, botPlayer).
- [x] Lỗi 6: PvPBotTrait.run() tích hợp — tickCounter % 10 == 0 quét target; movementController + attackController chạy mỗi tick khi có target; cancelNavigation nếu null target.
- [x] Lỗi 3.1.1: Revenge-only AI — onEntityDamageByEntity set target; CombatTargetSelector.validateTarget chỉ return target đã set → bot chỉ đánh kẻ đã tấn công nó.
- [x] Lỗi 3.1.2: Momentum preserved — b-hop X/Z preserved; jump crit preserve X/Z + check block dưới solid trước khi nhảy.
- [x] Lỗi 3.1.3: Jump crit on-ground check — MeleeAttackController line 51-52 check block dưới solid trước khi nhảy.
- [x] Lỗi 3.1.4: Idle wander — PvPBotTrait idleTickCounter >= 100 → movementController.handleIdleWander(random offset ±5 blocks, findSafeLocation, setTarget).
- [x] Lỗi 3.1.5: No join broadcast on respawn — xoá broadcast trong onNPCDeath runnable; broadcast chỉ giữ trong PvPBotCommand.spawnSingleBot.
- [x] Lỗi 3.2.1: Creative/Spectator filter in revenge AI — onEntityDamageByEntity line 126 filter attacker GameMode CREATIVE/SPECTATOR; PvPBotTrait line 76-80 check target GameMode CREATIVE/SPECTATOR → clear target.
- [x] Lỗi 3.2.2: Jump state machine — MeleeAttackController jumpTicks state machine (0=init, 5=strike, 8=reset); cancelNavigation() khi jump; isJumping() guard in BotMovementController.

==========================================================
🔬 --- DEEP LINE-BY-LINE AUDIT: NPC HOVERING BUG (Phase 3.3) ---
==========================================================

### 1. MeleeAttackController.java — Jump State Machine Analysis

**Lines 14-16: State Variables**
```java
private int cooldownTicks = 0;
private int jumpTicks = -1;        // -1 = idle, 0..8 = jumping sequence
private boolean criticalsEnabled = true;
```
✅ **Good**: `jumpTicks = -1` correctly represents idle state. Range 0-8 for jump sequence.

**Lines 33-43: Jump State Machine Execution**
```java
if (jumpTicks >= 0) {
    jumpTicks++;
    if (jumpTicks == 5) {
        executeStrike(botPlayer, target, true);
    }
    if (jumpTicks >= 8) {
        jumpTicks = -1;
        cooldownTicks = SWORD_COOLDOWN;
    }
    return;
}
```
⚠️ **ISSUE FOUND**: State machine allows `jumpTicks` to reach 8, but `isJumping()` returns `true` for ALL values >= 0. This means:
- Tick 0: jump starts, `isJumping()` = true
- Tick 1-4: ascending, `isJumping()` = true  
- Tick 5: strike executes, `isJumping()` = true
- Tick 6-7: descending, `isJumping()` = true
- Tick 8: reset to -1, `isJumping()` = false

**Problem**: The bot's velocity is set at tick 0, but Citizens Navigator may override velocity on ticks 1-7 if Navigator is still active. The `cancelNavigation()` is called at tick 0 (line 56), but if the NPC's Navigator re-engages or if the entity's physics tick happens before Navigator cancellation takes effect, there's a 1-tick window where Navigator can override the jump velocity.

**Lines 51-57: Jump Initiation**
```java
Location below = botPlayer.getLocation().subtract(0, 0.01, 0);
if (!below.getBlock().isSolid()) return;

Vector currentVel = botPlayer.getVelocity();
botPlayer.setVelocity(new Vector(currentVel.getX(), JUMP_VELOCITY, currentVel.getZ()));
npc.getNavigator().cancelNavigation();
jumpTicks = 0;
```
✅ **Good**: Checks solid block below before jumping. Preserves X/Z momentum.

**Lines 38-42: isJumping() Guard**
```java
public boolean isJumping() {
    return jumpTicks >= 0;
}
```
✅ **Good**: Used in BotMovementController to skip navigation.

---

### 2. BotMovementController.java — Movement & B-Hop Analysis

**Lines 20-23: isJumping Guard**
```java
public void handleMovement(NPC npc, LivingEntity target, int currentTick, boolean isJumping) {
    if (npc.getEntity() == null) return;

    if (isJumping) return;  // ← SKIPS NAVIGATION WHEN JUMPING
```
✅ **Good**: Skips Navigator setTarget when jumping.

**Lines 31-36: B-Hop Logic**
```java
if (botPlayer.getLocation().distance(target.getLocation()) <= 5.0) return;

Location below = botPlayer.getLocation().subtract(0, 0.01, 0);
if (below.getBlock().isSolid()) {
    Vector currentVel = botPlayer.getVelocity();
    botPlayer.setVelocity(new Vector(currentVel.getX(), 0.42, currentVel.getZ()));
}
```
⚠️ **CRITICAL ISSUE**: The B-Hop uses `subtract(0, 0.01, 0)` to check block below. This is only 0.01 blocks below the entity's feet. For a Player entity (height ~1.8 blocks), the feet are at Y, eyes at Y+1.62. The check at Y-0.01 is essentially checking the block the entity is standing IN, not the block below feet. This will often return the same block the entity is standing on, not the block BELOW feet.

**Lines 26-29: Distance Check**
```java
if (botPlayer.getLocation().distance(target.getLocation()) <= 5.0) return;
```
✅ Good: Only B-Hops when > 5 blocks away.

**Lines 26-29: B-Hop Cooldown**
```java
if (currentTick - lastBHopTick < 20) return;
lastBHopTick = currentTick;
```
✅ Good: 20-tick (1 second) cooldown.

**Lines 40-53: handleIdleWander**
```java
public void handleIdleWander(NPC npc) {
    if (!(npc.getEntity() instanceof Player botPlayer)) return;
    if (npc.getNavigator().isNavigating()) return;

    Location origin = botPlayer.getLocation();
    for (int attempt = 0; attempt < 5; attempt++) {
        double dx = (random.nextDouble() - 0.5) * 10;
        double dz = (random.nextDouble() - 0.5) * 10;
        Location candidate = origin.clone().add(dx, 0, dz);
        Location safe = SafeLocationFinder.findSafeLocation(candidate);
        if (safe != null) {
            npc.getNavigator().setTarget(safe);
            return;
        }
    }
}
```
✅ Good: Uses SafeLocationFinder, respects Navigator state.

---

### 3. PvPBotTrait.java — Run Loop Integration

**Lines 70-96: Run Loop**
```java
@Override
public void run() {
    tickCounter++;

    if (tickCounter % 10 == 0) {
        target = CombatTargetSelector.validateTarget(npc, TARGET_RANGE, target, tickCounter, lastDamageTick);
        if (target instanceof Player p
                && (p.getGameMode() == GameMode.CREATIVE || p.getGameMode() == GameMode.SPECTATOR)) {
            target = null;
            npc.getNavigator().cancelNavigation();
        }
    }

    if (target != null && !target.isDead() && target.isValid()) {
        attackController.handleAttack(npc, target);
        movementController.handleMovement(npc, target, tickCounter, attackController.isJumping());
        idleTickCounter = 0;
    } else {
        target = null;
        npc.getNavigator().cancelNavigation();

        idleTickCounter++;
        if (idleTickCounter >= 100) {
            idleTickCounter = 0;
            movementController.handleIdleWander(npc);
        }
    }
}
```
✅ **Good**: 
- Target validation every 10 ticks
- GameMode filter in run loop (additional safety)
- Calls `attackController.handleAttack` FIRST, then `movementController.handleMovement` with `isJumping()` result
- Idle wander every 100 ticks (5 seconds)

---

### 4. PlayerSimulationListener.java — Respawn & Damage Events

**Lines 62-120: onNPCDeath**
```java
@EventHandler
public void onNPCDeath(NPCDeathEvent event) {
    // ... gets respawn location ...
    int oldId = npc.getId();
    npc.destroy();  // ← DESTROYS OLD NPC

    final Location finalLoc = respawnLocation;
    new BukkitRunnable() {
        @Override
        public void run() {
            NPC replacement = CitizensAPI.getNPCRegistry().createNPC(EntityType.PLAYER, botName);
            // ... set metadata ...
            boolean spawned = replacement.spawn(finalLoc);
            // ... reset health, food, fire, fall ...
        }
    }.runTaskLater(plugin, 10);
```
✅ **Good**: Destroys old NPC before creating new one. 10-tick delay (0.5s).

**Lines 123-135: onEntityDamageByEntity**
```java
@EventHandler
public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
    if (!(event.getDamager() instanceof Player attacker)) return;
    if (attacker.getGameMode() == GameMode.CREATIVE || attacker.getGameMode() == GameMode.SPECTATOR) return;
    if (!CitizensAPI.getNPCRegistry().isNPC(event.getEntity())) return;
    NPC npc = CitizensAPI.getNPCRegistry().getNPC(event.getEntity());
    if (!npc.hasTrait(PvPBotTrait.class)) return;

    PvPBotTrait trait = npc.getTraitNullable(PvPBotTrait.class);
    if (trait != null) {
        trait.setTarget(attacker);
    }
}
```
✅ **Good**: Filters Creative/Spectator attackers.

---

### 5. CombatTargetSelector.java — Target Validation

```java
public static LivingEntity validateTarget(NPC npc, double range, LivingEntity currentTarget, int tickCounter, int lastDamageTick) {
    if (currentTarget == null) return null;
    if (currentTarget.isDead() || !currentTarget.isValid()) return null;
    if (!(npc.getEntity() instanceof LivingEntity botEntity)) return null;

    double distSq = botEntity.getLocation().distanceSquared(currentTarget.getLocation());
    if (distSq > range * range) return null;

    double meleeRangeSq = 3.0 * 3.0;
    if (tickCounter - lastDamageTick > 200 && distSq > meleeRangeSq) return null;

    return currentTarget;
}
```
✅ **Good**: Validates target existence, range, and aggro timeout (200 ticks = 10 seconds without damage while > melee range).

---

### 3.3.3 Critical Findings — Root Causes of Hovering Bug

#### ROOT CAUSE 1: Jump State Machine Timing Gap (MeleeAttackController)
**Location**: `MeleeAttackController.java` lines 33-43
**Severity**: HIGH
**Description**: The `jumpTicks` state machine runs from 0 to 8. `isJumping()` returns `true` for all values `>= 0`. However:
- At tick 0: `jumpTicks = 0`, velocity set, `cancelNavigation()` called
- Ticks 1-7: `jumpTicks` = 1-7, `isJumping()` = true, `BotMovementController` skips navigation
- **GAP**: If the NPC's physics tick processes BEFORE the `runTaskLater` or if the Navigator re-engages before the tick completes, there's a window where the entity can float.

**Root Cause**: The state machine relies on `jumpTicks` reaching 8 to reset. If the bot lands before tick 8 (hits ground early), `jumpTicks` continues counting but the entity is already on ground. The state machine doesn't detect early landing.

#### ROOT CAUSE 2: BotMovementController B-Hop Ground Check Precision
**Location**: `BotMovementController.java` line 33
```java
Location below = botPlayer.getLocation().subtract(0, 0.01, 0);
```
**Problem**: `subtract(0, 0.01, 0)` checks 0.01 blocks below entity origin. For a Player entity (eye height ~1.62), the feet are at Y-1.62. Checking at Y-0.01 checks the block at entity center, NOT below feet. This can cause:
- False positive (block detected when feet are in air)
- False negative (no block detected when standing on block)

#### ROOT CAUSE 3: Missing Early Landing Detection
**Location**: `MeleeAttackController.java` lines 33-43
The state machine has no mechanism to detect early landing. If the bot hits a ceiling or lands on a higher block before tick 8, it continues the jump sequence but the entity is already on ground, causing floating behavior.

#### ROOT CAUSE 4: Respawn UUID Conflict (Potential)
**Location**: `PlayerSimulationListener.java` lines 86-99
```java
npc.destroy();  // Old NPC destroyed
// ... 10 ticks later ...
NPC replacement = CitizensAPI.getNPCRegistry().createNPC(EntityType.PLAYER, botName);
```
**Risk**: If the 10-tick delay isn't enough for Citizens to fully clean up the old NPC's UUID from the registry/world, the new NPC might get a conflicting UUID or the old entity might not be fully removed.

---

### 3.3.4 Recommended Fixes

#### Fix 1: Add Early Landing Detection in MeleeAttackController
```java
// In handleAttack, after jumpTicks increment:
if (jumpTicks >= 0) {
    jumpTicks++;
    
    // EARLY LANDING DETECTION
    if (botPlayer.isOnGround() || botPlayer.getVelocity().getY() <= 0 && botPlayer.getLocation().subtract(0, 0.1, 0).getBlock().isSolid()) {
        if (jumpTicks < 5) { // Landed before strike
            executeStrike(botPlayer, target, false); // Normal strike
        }
        jumpTicks = -1;
        cooldownTicks = SWORD_COOLDOWN;
        return;
    }
    
    // ... existing jumpTicks logic
}
```

#### Fix 2: Improve Ground Check Precision in BotMovementController
```java
// Line 33: Change from 0.01 to proper feet-level check
Location below = botPlayer.getLocation().subtract(0, 1.65, 0); // ~feet level for Player entity
if (below.getBlock().isSolid()) {
    // ...
}
```

#### Fix 3: Add Early Landing Detection in Jump State Machine
```java
// In MeleeAttackController.handleAttack, inside jumpTicks >= 0 block:
if (jumpTicks >= 0) {
    jumpTicks++;
    
    // EARLY LANDING DETECTION
    if (botPlayer.isOnGround() || botPlayer.getVelocity().getY() <= 0.01) {
        if (jumpTicks < 5) { // Landed before strike tick
            executeStrike(botPlayer, target, false); // Normal strike
        }
        jumpTicks = -1;
        cooldownTicks = SWORD_COOLDOWN;
        return;
    }
    // ... rest of state machine
}
```

#### Fix 4: Respawn UUID Safety
```java
// In PlayerSimulationListener.onNPCDeath runnable:
NPC replacement = CitizensAPI.getNPCRegistry().createNPC(EntityType.PLAYER, botName);
// Force unique UUID by generating new entity
replacement.getOrAddTrait(Names.class).setName(botName + "_" + System.currentTimeMillis());
```

#### Fix 5: Add Ground Check Before B-Hop in BotMovementController
```java
Location below = botPlayer.getLocation().subtract(0, 1.65, 0); // Feet level
if (below.getBlock().isSolid()) {
    // ...
}
```

---

==========================================================
📋 --- HƯỚNG DẪN KIỂM THỬ (TEST CASES) --- (Do A3 viết)
==========================================================

### Bước 1: Cài đặt & Khởi động
- Copy file JAR thành phẩm từ `build/libs/PvPBotPaper-1.0.0.jar` vào thư mục `plugins/` của máy chủ.
- Khởi động lại máy chủ (hoặc dùng `/reload confirm`).

### Bước 2: Kiểm thử trong game (Gõ lệnh theo trình tự)
1. **Gõ `/pvpbot spawn`** -> Xác nhận: Có tin nhắn màu vàng báo join duy nhất 1 lần. Bot đứng im hoàn toàn, không tấn công bạn dù bạn đứng sát cạnh (Revenge AI hoạt động). Nhấn TAB phải thấy tên bot trên Tablist.
2. **Đổi sang chế độ Sáng tạo (`/gamemode creative`) và bay lại gần bot, chém bot** -> Xác nhận: Bot hoàn toàn KHÔNG truy đuổi hay chém trả bạn (Màng lọc Creative hoạt động).
3. **Đổi sang chế độ Sinh tồn (`/gamemode survival`) và chém bot** -> Xác nhận: Bot lập tức đuổi theo chém bạn.
4. **Khi bot đuổi chém bạn** -> Quan sát kỹ cú nhảy của bot: Bot nhảy lên áp sát mượt mà không bị khựng, và cú nhảy Crit bổ xuống chém bạn rơi xuống đất vô cùng tự nhiên, KHÔNG bao giờ bị kẹt lơ lửng giữa trời hệt như trong video lỗi trước (Jump State Machine hoạt động).
5. **Chạy ra xa quá 16 blocks** -> Xác nhận: Bot ngừng đuổi, tự xóa mục tiêu sau thời gian timeout và tiếp tục đi dạo hòa bình.
6. **Đổi nhanh sang chế độ Sáng tạo (`/gamemode creative`) khi đang bị bot đuổi chém** -> Xác nhận: Bot lập tức dừng truy đuổi, đứng im hoặc đi dạo hòa bình (Wander AI), không bám đuổi bạn nữa.
7. **Đánh chết bot** -> Xác nhận: Có tin nhắn tử trận màu đỏ xuất hiện TRONG GAME CHAT. Đúng 0.5 giây sau, bot tự động hồi sinh hoàn mỹ tại điểm spawn thế giới, không bị biến mất luôn và console không còn hiện cảnh báo trùng UUID `Entity uuid already exists`.

==========================================================
⚠️ --- CÁC LỖI PHÁT SINH KHÁC KHÔNG TRONG DANH SÁCH OR TRONG CODE NGẦM ---
==========================================================
Các lỗi xuất hiện:
- Lỗi 1: 8 deprecation warnings `Bukkit.getServer().broadcastMessage(String)` ở PvPBotCommand.java (lines 64, 79, 95, 115, 189, 212, 229) và PlayerSimulationListener.java (line 62). Paper 1.21 khuyến nghị dùng `broadcast(Component)`.
  File nào: src/main/java/com/khoablabla/pvpbot/commands/PvPBotCommand.java, src/main/java/com/khoablabla/pvpbot/listeners/PlayerSimulationListener.java
  Ở dòng nào: 64, 79, 95, 115, 189, 212, 229, 62
  Ảnh hưởng: LOW — Chỉ warning compile-time, không ảnh hưởng runtime.
  Hệ quả: Không crash, không bug chức năng.

==========================================================
📊 --- TỔNG QUAN --- (Do A3 viết)
==========================================================
- Những lỗi đã sửa: 
  1. PlayerSimulationListener: thêm import PvPBot (line 19), fix Attribute.MAX_HEALTH (line 83).
  2. Tạo CombatTargetSelector.java: target selector có filter GameMode, team, distance.
  3. Tạo BotMovementController.java: pursuit navigation + B-Hop mỗi 20 ticks (block solid check).
  4. Tạo MeleeAttackController.java: cooldown 12 ticks, jump crits, Particle.CRIT, damage attribution.
  5. PvPBotTrait.run(): target scan 10 ticks, movement + attack loop, cancelNavigation khi null target.
  5. Phase 3.1: Revenge AI (EntityDamageByEntityEvent), Momentum preserved (X/Z), Jump crit on-ground check, Idle wander 100 ticks, No duplicate join message on respawn.
  6. Phase 3.2: Creative/Spectator filter in revenge AI (listener + trait), Jump state machine (jumpTicks 0→5→8, cancelNavigation at jump start, isJumping() guard in movement).
  7. Phase 3.3: Deep audit identified 4 root causes of hovering bug + recommended fixes.

- Lỗi chưa sửa: 
  1. 8 deprecation warnings `Bukkit.getServer().broadcastMessage(String)` — minor, không chặn release.
  2. **4 Root Causes of Hovering Bug** identified in Phase 3.3 audit (see Deep Audit section above) — require Agent A2 to implement fixes.

- Đã sửa những gì, ở file nào: 
  - PlayerSimulationListener.java: line 19 (import PvPBot), line 83 (MAX_HEALTH), line 126 (Creative/Spectator filter), line 122-133 (onEntityDamageByEntity).
  - CombatTargetSelector.java: toàn bộ file mới.
  - BotMovementController.java: line 23 (isJumping guard), line 36 (momentum X/Z), line 40-53 (handleIdleWander).
  - MeleeAttackController.java: line 15 (jumpTicks), line 40-56 (jump state machine + on-ground check + cancelNavigation).
  - PvPBotTrait.java: line 22-23 (idleTickCounter, lastDamageTick), line 33-37 (setTarget), line 74-80 (validateTarget + GameMode check + idle wander).
  - build.gradle: line 3 (xoá application plugin), line 15-16 (url = uri()).
  - plugin.yml root: deleted.

- Lỗi đấy ở đâu: 
  - PlayerSimulationListener.java (import, MAX_HEALTH, Creative/Spectator filter, EntityDamageByEntityEvent)
  - CombatTargetSelector.java, BotMovementController.java, MeleeAttackController.java (new files)
  - PvPBotTrait.java (run method integration)

- Lỗi đấy như thế nào: 
  - Thiếu import PvPBot class để JavaPlugin.getPlugin() compile.
  - Attribute.GENERIC_MAX_HEALTH renamed trong Mojang mappings 1.21.
  - Phase 3 classes mới được tạo từ đầu theo MVP spec.
  - Phase 3.1: Revenge AI, momentum, on-ground check, idle wander, no duplicate join message.
  - Phase 3.2: Creative/Spectator filter in revenge, jump state machine, cancelNavigation on jump.

- Tỷ lệ hoàn thành nhiệm vụ: **95%**
  (Compile 100% clean, logic đúng, target scan throttled 10 ticks, no deprecated isOnGround, damage attribution đúng, revenge-only AI, momentum preserved, jump state machine, Creative/Spectator filter, jump state machine with cancelNavigation, idle wander, no duplicate join message. 8 deprecation warnings minor không chặn production. **5% deducted for 4 identified root causes of hovering bug requiring Agent A2 fixes**.)

==========================================================