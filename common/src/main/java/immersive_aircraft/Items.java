package immersive_aircraft;

import immersive_aircraft.cobalt.registration.Registration;
import immersive_aircraft.entity.*;
import immersive_aircraft.entity.misc.WeaponMount;
import immersive_aircraft.item.AircraftItem;
import immersive_aircraft.item.DescriptionItem;
import immersive_aircraft.item.DyeableAircraftItem;
import immersive_aircraft.item.UavItem;
import immersive_aircraft.item.WeaponItem;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.LinkedList;
import java.util.List;
import java.util.function.Supplier;

public interface Items {
    List<Supplier<Item>> items = new LinkedList<>();

    Supplier<Item> HULL = register("hull", (name) -> new DescriptionItem(baseProps(name).stacksTo(8)) {});
    Supplier<Item> ENGINE = register("engine", (name) -> new DescriptionItem(baseProps(name).stacksTo(8)) {});
    Supplier<Item> SAIL = register("sail", (name) -> new DescriptionItem(baseProps(name).stacksTo(8)) {});
    Supplier<Item> PROPELLER = register("propeller", (name) -> new DescriptionItem(baseProps(name).stacksTo(8)) {});
    Supplier<Item> BOILER = register("boiler", (name) -> new DescriptionItem(baseProps(name).stacksTo(8)) {});

    Supplier<Item> AIRSHIP = register("airship", (name) -> new DyeableAircraftItem(baseProps(name).stacksTo(1), world -> new AirshipEntity(Entities.AIRSHIP.get(), world)));
    Supplier<Item> CARGO_AIRSHIP = register("cargo_airship", (name) -> new DyeableAircraftItem(baseProps(name).stacksTo(1), world -> new CargoAirshipEntity(Entities.CARGO_AIRSHIP.get(), world)));
    Supplier<Item> WARSHIP = register("warship", (name) -> new DyeableAircraftItem(baseProps(name).stacksTo(1), world -> new WarshipEntity(Entities.WARSHIP.get(), world)));
    Supplier<Item> BIPLANE = register("biplane", (name) -> new AircraftItem(baseProps(name).stacksTo(1), world -> new BiplaneEntity(Entities.BIPLANE.get(), world)));
    Supplier<Item> GYRODYNE = register("gyrodyne", (name) -> new AircraftItem(baseProps(name).stacksTo(1), world -> new GyrodyneEntity(Entities.GYRODYNE.get(), world)));
    Supplier<Item> QUADROCOPTER = register("quadrocopter", (name) -> new AircraftItem(baseProps(name).stacksTo(1), world -> new QuadrocopterEntity(Entities.QUADROCOPTER.get(), world)));
    Supplier<Item> BAMBOO_HOPPER = register("bamboo_hopper", (name) -> new AircraftItem(baseProps(name).stacksTo(1), world -> new BambooHopperEntity(Entities.BAMBOO_HOPPER.get(), world)));

    Supplier<Item> UAV = register("uav", (name) -> new UavItem(baseProps(name).stacksTo(16)));
    Supplier<Item> IMPROVED_UAV = register("improved_uav", (name) -> new UavItem(baseProps(name).stacksTo(16)));

    Supplier<Item> ROTARY_CANNON = register("rotary_cannon", (name) -> new WeaponItem(baseProps(name).stacksTo(1), WeaponMount.Type.ROTATING));
    Supplier<Item> REINFORCED_ROTARY_CANNON = register("reinforced_rotary_cannon", (name) -> new WeaponItem(baseProps(name).stacksTo(1), WeaponMount.Type.ROTATING));
    Supplier<Item> HEAVY_CROSSBOW = register("heavy_crossbow", (name) -> new WeaponItem(baseProps(name).stacksTo(1), WeaponMount.Type.FRONT));
    Supplier<Item> MULTI_HEAVY_CROSSBOW = register("multi_heavy_crossbow", (name) -> new WeaponItem(baseProps(name).stacksTo(1), WeaponMount.Type.FRONT));
    Supplier<Item> SCULK_HEAVY_CROSSBOW = register("sculk_heavy_crossbow", (name) -> new WeaponItem(baseProps(name).stacksTo(1), WeaponMount.Type.FRONT));
    Supplier<Item> TELESCOPE = register("telescope", (name) -> new WeaponItem(baseProps(name).stacksTo(1), WeaponMount.Type.ROTATING));
    Supplier<Item> BOMB_BAY = register("bomb_bay", (name) -> new WeaponItem(baseProps(name).stacksTo(1), WeaponMount.Type.DROP));

    Supplier<Item> ENHANCED_PROPELLER = register("enhanced_propeller", (name) -> new DescriptionItem(baseProps(name).stacksTo(8)) {});
    Supplier<Item> ECO_ENGINE = register("eco_engine", (name) -> new DescriptionItem(baseProps(name).stacksTo(8)) {});
    Supplier<Item> NETHER_ENGINE = register("nether_engine", (name) -> new DescriptionItem(baseProps(name).stacksTo(8)) {});
    Supplier<Item> ENGINEER_ENGINE = register("engineer_engine", (name) -> new DescriptionItem(baseProps(name).stacksTo(8)) {});
    Supplier<Item> STEEL_BOILER = register("steel_boiler", (name) -> new DescriptionItem(baseProps(name).stacksTo(8)) {});
    Supplier<Item> INDUSTRIAL_GEARS = register("industrial_gears", (name) -> new DescriptionItem(baseProps(name).stacksTo(8)) {});
    Supplier<Item> STURDY_PIPES = register("sturdy_pipes", (name) -> new DescriptionItem(baseProps(name).stacksTo(8)) {});
    Supplier<Item> GYROSCOPE = register("gyroscope", (name) -> new DescriptionItem(baseProps(name).stacksTo(8)) {});
    Supplier<Item> GYROSCOPE_HUD = register("gyroscope_hud", (name) -> new DescriptionItem(baseProps(name).stacksTo(8)) {});
    Supplier<Item> GYROSCOPE_DIALS = register("gyroscope_dials", (name) -> new DescriptionItem(baseProps(name).stacksTo(8)) {});
    Supplier<Item> HULL_REINFORCEMENT = register("hull_reinforcement", (name) -> new DescriptionItem(baseProps(name).stacksTo(8)) {});
    Supplier<Item> IMPROVED_LANDING_GEAR = register("improved_landing_gear", (name) -> new DescriptionItem(baseProps(name).stacksTo(8)) {});
    Supplier<Item> TNT_BUNDLE = register("tnt_bundle", (name) -> new DescriptionItem(baseProps(name).stacksTo(8)) {});
    Supplier<Item> AVIATION_FUEL = register("aviation_fuel", (name) -> new DescriptionItem(baseProps(name).stacksTo(64)) {});

    Supplier<Item> RADAR = register("radar", (name) -> new DescriptionItem(baseProps(name).stacksTo(4)) {});
    Supplier<Item> MUFFLER = register("muffler", (name) -> new DescriptionItem(baseProps(name).stacksTo(4)) {});
    Supplier<Item> CAMOUFLAGE_NET = register("camouflage_net", (name) -> new DescriptionItem(baseProps(name).stacksTo(4)) {});

    static Supplier<Item> register(String name, java.util.function.Function<String, Item> factory) {
        Identifier id = Main.locate(name);
        Supplier<Item> register = Registration.register(BuiltInRegistries.ITEM, id, () -> factory.apply(name));
        items.add(register);
        return register;
    }

    /**
     * No-op hook. Calling it forces the static field initializers above to run,
     * which register all items.
     */
    static void bootstrap() {
    }

    static Item.Properties baseProps(String name) {
        return new Item.Properties().setId(ResourceKey.create(Registries.ITEM, Main.locate(name)));
    }

    static List<ItemStack> getSortedItems() {
        return items.stream().map(i -> i.get().getDefaultInstance()).toList();
    }
}
