package immersive_aircraft.config;

import immersive_aircraft.Main;
import immersive_aircraft.config.configEntries.BooleanConfigEntry;
import immersive_aircraft.config.configEntries.FloatConfigEntry;
import immersive_aircraft.config.configEntries.IntegerConfigEntry;

import java.util.Map;
import java.util.Set;

public final class Config extends JsonConfig {
    private static final Config INSTANCE = loadOrCreate(new Config("immersive_aircraft_new_side"), Config.class);

    public Config() {
        super("default");
    }

    public Config(String name) {
        super(name);
    }

    public static Config getInstance() {
        return INSTANCE;
    }

    @Override
    int getVersion() {
        return 3;
    }

    // ========================
    // World rules
    // ========================
    // Dimension whitelist for flying: dimension id -> allowed.
    // true  = aircraft work normally in that dimension.
    // false = trying to board a vehicle there is blocked ("invalid dimension" message).
    // Dimensions not listed at all are treated as allowed.
    public Map<String, Boolean> validDimensions = Map.of(
            "minecraft:overworld", true,
            "minecraft:the_nether", true,
            "minecraft:the_end", true
    );

    // Base wind strength in clear weather; it is added on top of the vehicle's own
    // speed contribution and then scaled by the vehicle's WIND stat.
    // Higher = bumpier clear-weather flights, lower = calmer skies.
    @FloatConfigEntry(4.0f)
    public float windClearWeather;

    // Wind strength added during storms. NOTE (kept exactly as in the original mod):
    // the code combines weather levels crosswise - THIS multiplier is applied to the
    // THUNDER level while windThunderWeather is applied to the RAIN level.
    // Higher = stronger storm wind.
    @FloatConfigEntry(8.0f)
    public float windRainWeather;

    // Storm wind strength (see the note on windRainWeather about the crosswise
    // application to rain/thunder levels). Higher = stronger storm wind.
    @FloatConfigEntry(16.0f)
    public float windThunderWeather;

    // Whether a vehicle destroyed by crash damage (not by a player) explodes.
    // true  = crash explosion using the radius/flags below; a TNT bundle upgrade
    //         forces the explosion even when this is false.
    // false = vehicles break apart silently when crashed.
    @BooleanConfigEntry(true)
    public boolean enableCrashExplosion;

    // Radius (in blocks) of the crash explosion. Larger = bigger, more destructive blast.
    // Only used when enableCrashExplosion is true (or a TNT bundle is installed).
    @FloatConfigEntry(2.0F)
    public float crashExplosionRadius;

    // Applies only to the crash explosion above.
    // true  = the crash explosion leaves fire around the impact site.
    // false = no fire from crash explosions (default).
    @BooleanConfigEntry(false)
    public boolean enableCrashFire = false;

    // Applies only to the crash explosion above.
    // true  = the crash explosion breaks blocks (MOB-power explosion).
    // false = the crash explosion is visual only, no block damage.
    @BooleanConfigEntry(true)
    public boolean enableCrashBlockDestruction = true;

    // Multiplier applied to the vehicle's collision damage to compute the damage
    // the PILOT takes when the vehicle is destroyed by a crash
    // (pilot damage = vehicle collision damage * crashDamage). Higher = far more lethal crashes.
    @FloatConfigEntry(15.0f)
    public float crashDamage;

    // Protects the pilot from being killed by their own crash.
    // true  = pilot crash damage is capped at (current health - 1), i.e. it can knock the
    //         pilot down but never kill. Ignored when a TNT bundle is installed.
    // false = crashes can kill the pilot.
    @BooleanConfigEntry(true)
    public boolean preventKillThroughCrash;

    // Whether vehicles take damage from flying into blocks at speed.
    // true  = hard collisions damage the vehicle (and the pilot via crashDamage when it is destroyed).
    // false = collisions never damage anything.
    @BooleanConfigEntry(true)
    public boolean collisionDamage;

    // Multiplier for all collision damage (both to the vehicle itself and to the pilot
    // through crashDamage). Higher = crashes hurt much more, lower = gentler landings.
    @FloatConfigEntry(40.0f)
    public float collisionDamageMultiplier;

    // Drop the aircraft itself as an item when it is destroyed.
    // true = the vehicle item pops out; false = the vehicle is simply lost.
    @BooleanConfigEntry(true)
    public boolean dropAircraft;

    // Drop cargo from INVENTORY-type slots when the aircraft is destroyed.
    // true = cargo spills onto the ground; false = cargo is destroyed with the vehicle.
    @BooleanConfigEntry(true)
    public boolean dropInventory;

    // Drop items from upgrade/weapon/engine slots when the aircraft is destroyed.
    // true = upgrades and weapons spill onto the ground; false = they are destroyed.
    @BooleanConfigEntry(false)
    public boolean dropUpgrades;

    // Master switch for loot drops when an aircraft is destroyed.
    // true  = destroyed aircraft drops its item and (per the flags above) its cargo,
    //         as long as the ENTITY_DROPS gamerule is also enabled.
    // false = destroyed aircraft never drops anything through this code path.
    @BooleanConfigEntry(true)
    public boolean enableDropsForNonPlayer = true;
    // ========================
    // Vehicle rules
    // ========================
    // Base fuel burn rate per tick at 100% throttle (scaled further by the vehicle's
    // FUEL stat and the current engine target). Higher = fuel drains faster, lower = slower.
    // 0 disables fuel consumption completely (engines run forever).
    @FloatConfigEntry(30.0f)
    public float fuelConsumption;

    // Fuel usage in creative mode.
    // true  = fuel is consumed even when the pilot is in creative mode.
    // false = engines run without consuming fuel for creative pilots.
    @BooleanConfigEntry(false)
    public boolean burnFuelInCreative;

    // Whether vanilla furnace fuels (coal, planks, etc.) work in vehicle boilers.
    // true  = anything that burns in a furnace is accepted (using its vanilla burn time).
    // false = only items from fuelList below are accepted.
    @BooleanConfigEntry(true)
    public boolean acceptVanillaFuel;

    // Custom boiler fuels: item id -> burn value granted per item (same unit as vanilla
    // furnace burn times: coal = 1600, coal block = 8000). Items listed here work even when
    // acceptVanillaFuel is false. Higher values = more flight time per item.
    // Aviation fuel is tuned to 16000 = exactly two coal blocks.
    public Map<String, Integer> fuelList = Map.of(
            "minecraft:blaze_powder", 1200,
            "immersive_aircraft:aviation_fuel", 16000
    );

    // Global multiplier for the ENGINE_SPEED stat of every vehicle.
    // 2.0 = all engines produce twice the thrust, 0.5 = half thrust, 1.0 = as defined in vehicle data.
    @FloatConfigEntry(1.0f)
    public float globalEngineSpeedMultiplier;

    // Engine spool-up speed multiplier. Calibrated so that 1.0 = a stock biplane
    // (engineReactionSpeed 20, no acceleration upgrades) reaches full power in
    // exactly 8 seconds (160 ticks). Higher = faster spin-up (2.0 = 4 s),
    // lower = slower (0.5 = 16 s). Vehicles scale by their own
    // getEngineReactionSpeed()/20. Acceleration upgrades divide this further.
    // Does NOT affect spool-down: see engineDecayTicks.
    @FloatConfigEntry(1.0f)
    public float engineAccelerationMultiplier;

    // Ticks for the engine to spin DOWN: real power falls linearly from 100% to
    // 0% in this many ticks (scaled by each vehicle's getEngineReactionSpeed()/20,
    // e.g. the warship with reaction 100 decays 5x slower). Acceleration upgrades
    // and engineAccelerationMultiplier do NOT affect spool-down.
    @IntegerConfigEntry(20)
    public int engineDecayTicks = 20;

    // Global gravity multiplier for all vehicles.
    // 0.5 = half gravity (floatier flight), 2.0 = double gravity (heavier feel), 1.0 = normal.
    @FloatConfigEntry(1.0f)
    public float gravityMultiplier;

    // Global multiplier for the DURABILITY stat of every vehicle.
    // 2.0 = all vehicles are twice as tough, 0.5 = twice as fragile, 1.0 = as defined in vehicle data.
    @FloatConfigEntry(1.0f)
    public float durabilityMultiplier;

    // Toughness divisor: incoming damage is divided by (durability * this value)
    // before being subtracted from vehicle health. Higher = vehicles take less damage;
    // lower = vehicles are more fragile. 20 means a durability-1.0 vehicle needs
    // 20 raw damage to lose 1 health point.
    @IntegerConfigEntry(20)
    public int damagePerHealthPoint;

    // Per-tick velocity multiplier applied to a biplane gliding with the engine turned off.
    // 1.0 = no extra drag (long glide), lower values = stronger drag (plane slows quickly);
    // values above 1.0 would make a gliding plane accelerate (not recommended).
    @FloatConfigEntry(1.0f)
    public float engineOffDrag;

    // Passive vehicle regeneration interval in ticks.
    // 0 = disabled. N > 0 = every N game ticks the vehicle restores 5% health (scaled by durability).
    @IntegerConfigEntry(0)
    public int regenerateHealthEveryNTicks;

    // Health restored per repair action (right-click on a damaged vehicle), on a 0..1 scale.
    // 0.025 = 2.5% of the vehicle's health per click. Higher = faster repairs.
    @FloatConfigEntry(0.025f)
    public float repairSpeed;

    // Food exhaustion added to the repairing player per repair action.
    // Higher = repairing makes you hungrier faster; 0 = repairs never drain hunger.
    @FloatConfigEntry(2.0f)
    public float repairExhaustion;

    // Repair interaction style.
    // true  = the player must sneak (shift) while right-clicking to repair.
    // false = a plain right-click on a damaged vehicle repairs it.
    @BooleanConfigEntry(false)
    public boolean requireShiftForRepair;

    // ========================
    // Client interface & visuals
    // ========================
    // Automatic camera perspective switching for vehicles.
    // true  = entering a vehicle switches to the configured perspective, leaving restores the previous one.
    // false = the camera perspective is never changed by this mod.
    @BooleanConfigEntry(true)
    public boolean separateCamera = true;

    // Used when separateCamera is true.
    // true  = entering a vehicle switches to third person (back view).
    // false = entering a vehicle switches to first person.
    @BooleanConfigEntry(true)
    public boolean useThirdPersonByDefault = true;

    // Maximum distance (in blocks) at which vehicles are rendered.
    // Larger = aircraft stay visible farther away (higher rendering cost);
    // smaller = aircraft pop out of view sooner.
    @FloatConfigEntry(320.0f)
    public float renderDistance;

    // Engine gauge drawn above the hotbar while piloting.
    // true  = show the gauge. false = hide it.
    @BooleanConfigEntry(true)
    public boolean showHotbarEngineGauge;

    // Vertical placement of the vehicle health bar above the hotbar, in rows of 10 pixels.
    // 0 = directly above the hotbar; increase to move the bar higher up the screen.
    @IntegerConfigEntry(0)
    public int healthBarRow;

    // While using the on-board telescope:
    // true  = the player's own vehicle model is hidden and the camera is not pulled back.
    // false = the vehicle stays visible and the camera zooms out as usual.
    @BooleanConfigEntry(true)
    public boolean hideVehicleWhileScoping;

    // Fabric only. Dedicated aircraft control keys instead of reusing vanilla movement keys.
    // true  = aircraft use their own rebindable keys (W/A/S/D/Space/Shift by default).
    // false = aircraft reuse whatever the vanilla movement keys are bound to.
    @BooleanConfigEntry(true)
    public boolean useCustomKeybindSystem;

    // Contrails/smoke trails behind aircraft.
    // true  = trails are recorded and rendered.
    // false = trails are disabled entirely (slightly better performance).
    @BooleanConfigEntry(true)
    public boolean enableTrails = true;

    // ========================
    // Weapons - common
    // ========================
    // Whether weapon explosions (tiny TNT dropped by the bomb bay) damage entities.
    // true  = tiny TNT deals manual radius damage (blocks stay intact).
    // false = tiny TNT explosions are purely visual.
    @BooleanConfigEntry(true)
    public boolean weaponsAreDestructive;

    // RESERVED / CURRENTLY UNUSED: this map is declared but never read anywhere in the code.
    // It was intended to map an ammunition item id to an entity id spawned by the bomb bay
    // (e.g. egg -> chicken). Editing it currently has no effect.
    public Map<String, String> bombBayEntity = Map.of(
            "minecraft:egg", "minecraft:chicken"
    );

    // ========================
    // Weapons - rotary cannons
    // ========================
    // Damage per rotary cannon bullet, in health points (2.0 = one heart).
    // Higher = more damaging shots.
    @FloatConfigEntry(4.0f)
    public float rotaryCannonDamage;

    // Muzzle velocity of rotary cannon bullets, in blocks per tick (3.0 = 60 blocks/second).
    // Higher = flatter trajectory, longer effective range.
    @FloatConfigEntry(3.0f)
    public float rotaryCannonVelocity;

    // Aim deviation of rotary cannon bullets (vanilla shoot() spread parameter).
    // Intentionally very small: 0.1 = nearly laser-straight bursts with a tiny wobble.
    // Higher = looser spread; 0 = perfectly straight shots.
    @FloatConfigEntry(0.3f)
    public float rotaryCannonInaccuracy;

    // Cooldown between rotary cannon shots, in seconds (client-side rate limit).
    // The default of 0.2s equals one shot every 4 ticks (~5 shots/sec). Lower = faster firing.
    @FloatConfigEntry(0.2f)
    public float rotaryCannonCooldown;

    // Items accepted as rotary cannon ammunition (item ids). 1 item is consumed per shot.
    public Set<String> rotaryCannonAmmunition = Set.of(
            "minecraft:copper_nugget"
    );

    // Damage per reinforced rotary cannon bullet, in health points (2.0 = one heart).
    // Higher = more damaging shots.
    @FloatConfigEntry(6.0f)
    public float reinforcedRotaryCannonDamage;

    // Muzzle velocity of reinforced rotary cannon bullets, in blocks per tick
    // (5.0 = 100 blocks/second, 3x the regular rotary cannon). Higher = longer range.
    @FloatConfigEntry(5.0f)
    public float reinforcedRotaryCannonVelocity;

    // Aim deviation of reinforced rotary cannon bullets (vanilla shoot() spread parameter).
    // Intentionally very small: 0.1 = nearly laser-straight bursts with a tiny wobble.
    @FloatConfigEntry(0.1f)
    public float reinforcedRotaryCannonInaccuracy;

    // Cooldown between reinforced rotary cannon shots, in seconds (client-side rate limit).
    // The default of 4/60s (~0.0667s) is 3x shorter than the rotary cannon's interval; because
    // cooldown is decremented once per tick, this lands on one shot every 2 ticks in practice.
    @FloatConfigEntry(0.1f)
    public float reinforcedRotaryCannonCooldown;

    // Items accepted as reinforced rotary cannon ammunition (item ids). 1 item per shot.
    public Set<String> reinforcedRotaryCannonAmmunition = Set.of(
            "minecraft:iron_nugget"
    );
    // ========================
    // Weapons - crossbows
    // ========================
    // Items accepted as ammunition for all crossbow-type weapons (item ids).
    // Heavy crossbows consume 1 per shot, the multi crossbow consumes
    // multiHeavyCrossBowAmmoPerShot per volley, the sculk crossbow consumes 1 per beam.
    public Set<String> arrowAmmunition = Set.of(
            "minecraft:arrow",
            "minecraft:tipped_arrow",
            "minecraft:spectral_arrow"
    );

    // Muzzle velocity of heavy crossbow arrows, in blocks per tick
    // (5.0 = 100 blocks/second). Higher = flatter trajectory and longer range.
    @FloatConfigEntry(5.0f)
    public float heavyCrossBowVelocity;

    // Aim deviation of heavy crossbow arrows (passed to the vanilla shoot() spread parameter).
    // Higher = more random spread / less accurate; 0 = perfectly straight shots.
    @FloatConfigEntry(0.3f)
    public float heavyCrossBowInaccuracy;

    // Cooldown between heavy crossbow shots, in seconds (client-side rate limit).
    // Lower = faster firing.
    @FloatConfigEntry(0.1875f)
    public float heavyCrossBowCooldown;

    // Random launch-speed variation for crossbow arrows.
    // 0.25 = each arrow flies at 75%..125% of the configured velocity; 0 = always exact speed.
    @FloatConfigEntry(0.25f)
    public float heavyCrossBowVelocitySpread;

    // Muzzle velocity of multi heavy crossbow arrows, in blocks per tick (same unit as heavyCrossBowVelocity).
    @FloatConfigEntry(3.5f)
    public float multiHeavyCrossBowVelocity;

    // Aim deviation of multi heavy crossbow arrows (vanilla shoot() spread parameter).
    // Higher = a wider fan of arrows.
    @FloatConfigEntry(0.5f)
    public float multiHeavyCrossBowInaccuracy;

    // Cooldown between multi heavy crossbow volleys, in seconds. Lower = faster firing.
    @FloatConfigEntry(1.0f)
    public float multiHeavyCrossBowCooldown;

    // Number of arrows actually FIRED per multi heavy crossbow volley.
    // Higher = more arrows in the air per trigger pull.
    @IntegerConfigEntry(8)
    public int multiHeavyCrossBowBulletCount;

    // Arrows CONSUMED from the inventory per multi heavy crossbow volley.
    // The volley always fires multiHeavyCrossBowBulletCount arrows regardless of this value,
    // so with the defaults 8 arrows are fired but only 2 are taken from storage.
    @IntegerConfigEntry(2)
    public int multiHeavyCrossBowAmmoPerShot;

    // Random directional deviation of multi heavy crossbow arrows, in RADIANS
    // (0.25 ≈ ±7° fan). Higher = wider fan, 0 = all arrows fly parallel.
    @FloatConfigEntry(0.25f)
    public float multiHeavyCrossBowSpread;

    // Radius (in blocks) of the sculk crossbow sonic beam - the damage area around the beam line.
    // Higher = easier to hit targets next to the beam; values near 0 make hits very hard to land.
    @FloatConfigEntry(2.0f)
    public float sculkHeavyCrossBowRadius;

    // Maximum length of the sonic beam in blocks. Larger = sniping across huge distances.
    @FloatConfigEntry(320.0f)
    public float sculkHeavyCrossBowRange;

    // Cooldown between sculk crossbow shots, in seconds. Lower = faster firing.
    @FloatConfigEntry(2.0f)
    public float sculkHeavyCrossBowCooldown;

    // Damage dealt by one sonic beam hit, in health points (applied once per target per shot).
    @FloatConfigEntry(15.0f)
    public float sculkHeavyCrossBowDamage;

    // ========================
    // Weapons - bomb bay
    // ========================
    // Fallback bomb bay ammunition (item ids): used only when neither a UAV nor TNT
    // is found in the vehicle inventory. 1 item per drop.
    public Set<String> bombBayAmmunition = Set.of(
            "minecraft:tnt"
    );

    // Bomb bay cooldown between drops, in seconds. Lower = faster bombing runs.
    @FloatConfigEntry(1.5f)
    public float bombBayCooldown;

    // Explosion power of the standard UAV (in blocks, vanilla TNT = 4).
    @FloatConfigEntry(10.0f)
    public float uavExplosionPower;

    // Explosion power of the improved UAV (in blocks).
    @FloatConfigEntry(15.0f)
    public float improvedUavExplosionPower;

    // ========================
    // Upgrades
    // ========================
    // Global CAP for the radar detection range: a target is detected within
    // min(its radarDetectability, this value). 0 disables the cap.
    @FloatConfigEntry(256.0f)
    public float radarRange;

    // Color of the radar-detected glowing outline, as RGB hex (16777215 = 0xFFFFFF,
    // white - like the spectral arrow outline).
    @IntegerConfigEntry(16777215)
    public int radarGlowColor;
}