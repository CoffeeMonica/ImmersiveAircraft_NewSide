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
    // General
    // ========================
    @BooleanConfigEntry(true)
    public boolean enableDropsForNonPlayer = true;

    @BooleanConfigEntry(true)
    public boolean enableCrashExplosion;

    @BooleanConfigEntry(true)
    public boolean enableCrashBlockDestruction = true;

    @BooleanConfigEntry(true)
    public boolean enableCrashFire = true;

    @FloatConfigEntry(3.0F)
    public float crashExplosionRadius;

    @FloatConfigEntry(15.0f)
    public float crashDamage;

    @BooleanConfigEntry(true)
    public boolean preventKillThroughCrash;

    @IntegerConfigEntry(0)
    public int healthBarRow;

    @IntegerConfigEntry(20)
    public int damagePerHealthPoint;

    @BooleanConfigEntry(true)
    public boolean separateCamera = true;

    @BooleanConfigEntry(true)
    public boolean useThirdPersonByDefault = true;

    @BooleanConfigEntry(true)
    public boolean enableTrails = true;

    @FloatConfigEntry(320.0f)
    public float renderDistance;

    @FloatConfigEntry(30.0f)
    public float fuelConsumption;

    // Engine acceleration multiplier (1.0 = normal, 2.0 = twice as fast spin-up, 0.5 = twice as slow)
    // When the target power is lower than or equal to the current power, spin-down happens 3x faster.
    @FloatConfigEntry(0.2f)
    public float engineAccelerationMultiplier;

    @FloatConfigEntry(3.0f)
    public float windClearWeather;

    @FloatConfigEntry(10.0f)
    public float windRainWeather;

    @FloatConfigEntry(20.0f)
    public float windThunderWeather;

    @FloatConfigEntry(0.025f)
    public float repairSpeed;

    @FloatConfigEntry(2.0f)
    public float repairExhaustion;

    @BooleanConfigEntry(true)
    public boolean collisionDamage;

    @FloatConfigEntry(40.0f)
    public float collisionDamageMultiplier;

    @BooleanConfigEntry(false)
    public boolean burnFuelInCreative;

    @BooleanConfigEntry(true)
    public boolean acceptVanillaFuel;

    @BooleanConfigEntry(true)
    public boolean useCustomKeybindSystem;

    @BooleanConfigEntry(true)
    public boolean showHotbarEngineGauge;

    @BooleanConfigEntry(true)
    public boolean weaponsAreDestructive;

    @BooleanConfigEntry(true)
    public boolean dropAircraft;

    @BooleanConfigEntry(true)
    public boolean dropInventory;

    @BooleanConfigEntry(false)
    public boolean dropUpgrades;

    @IntegerConfigEntry(0)
    public int regenerateHealthEveryNTicks;

    @BooleanConfigEntry(false)
    public boolean requireShiftForRepair;

    @BooleanConfigEntry(true)
    public boolean hideVehicleWhileScoping;

    // ========================
    // Flight
    // ========================
    @FloatConfigEntry(1.0f)
    public float globalEngineSpeedMultiplier;

    // Engine power multiplier (1.0 = normal, 2.0 = twice as powerful, 0.5 = half as powerful)
    @FloatConfigEntry(1.0f)
    public float durabilityMultiplier;

    // Gravity multiplier (1.0 = normal gravity, 0.5 = half gravity, 2.0 = double gravity)
    @FloatConfigEntry(1.0f)
    public float gravityMultiplier;

    // Engine drag when engines are off (1.0 = normal drag)
    @FloatConfigEntry(1.0f)
    public float engineOffDrag;

    // ========================
    // Weapons
    // ========================
    // The entity to spawn when triggering the bomb bay
    // The item also needs to be valid ammunition (e.g., set to 100)
    public Map<String, String> bombBayEntity = Map.of(
            "minecraft:egg", "minecraft:chicken"
    );

    @FloatConfigEntry(4.0f)
    public float rotaryCannonDamage;

    @FloatConfigEntry(5.0f)
    public float heavyCrossBowVelocity;

    @FloatConfigEntry(0.3f)
    public float heavyCrossBowInaccuracy;

    @FloatConfigEntry(0.1875f)
    public float heavyCrossBowCooldown;

    @FloatConfigEntry(3.5f)
    public float multiHeavyCrossBowVelocity;

    @FloatConfigEntry(1.0f)
    public float multiHeavyCrossBowInaccuracy;

    @FloatConfigEntry(0.75f)
    public float multiHeavyCrossBowCooldown;

    @IntegerConfigEntry(8)
    public int multiHeavyCrossBowBulletCount;

    // Количество стрел для расхода за выстрел (по умолчанию 2, чтобы тратить в 4 раза меньше чем стреляем)
    @IntegerConfigEntry(2)
    public int multiHeavyCrossBowAmmoPerShot;

    // Sculk Heavy Crossbow settings
    // Radius of the sonic beam (damage area)
    @FloatConfigEntry(2.0f)
    public float sculkHeavyCrossBowRadius;

    // Maximum range of the sonic beam
    @FloatConfigEntry(320.0f)
    public float sculkHeavyCrossBowRange;

    // Cooldown between shots (in seconds, 1.0f = 1 second)
    @FloatConfigEntry(1.0f)
    public float sculkHeavyCrossBowCooldown;

    // Sonic beam damage per hit
    @FloatConfigEntry(10.0f)
    public float sculkHeavyCrossBowDamage;

    // Multi Heavy Crossbow spread settings
    // Velocity spread multiplier (1.0 = 75%-125% of base velocity)
    @FloatConfigEntry(0.25f)
    public float heavyCrossBowVelocitySpread;

    // Directional spread for arrows (in radians, 0.0 = no spread)
    @FloatConfigEntry(0.5f)
    public float multiHeavyCrossBowSpread;

    // ========================
    // Ammunition
    // ========================
    public Map<String, Integer> fuelList = Map.of(
            "minecraft:blaze_powder", 1200
    );

    public Map<String, Boolean> validDimensions = Map.of(
            "minecraft:overworld", true,
            "minecraft:the_nether", true,
            "minecraft:the_end", true
    );

    // rotaryCannonAmmunition - items that act as ammo for the rotary cannon, 1 consumed per shot
    public Set<String> rotaryCannonAmmunition = Set.of(
            "minecraft:copper_nugget"
    );

    // arrowAmmunition - items that act as arrows, 1 consumed per shot
    public Set<String> arrowAmmunition = Set.of(
            "minecraft:arrow",
            "minecraft:tipped_arrow",
            "minecraft:spectral_arrow"
    );

    // tnt - 1 per shot with spentAmmoItems system  
    public Set<String> bombBayAmmunition = Set.of(
            "minecraft:tnt"
    );

    // Bomb Bay cooldown in seconds
    @FloatConfigEntry(1.5f)
    public float bombBayCooldown;
}