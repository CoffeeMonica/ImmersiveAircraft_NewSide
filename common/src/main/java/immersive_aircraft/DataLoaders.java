package immersive_aircraft;

import immersive_aircraft.cobalt.registration.Registration;
import immersive_aircraft.data.UpgradeDataLoader;
import immersive_aircraft.data.VehicleDataLoader;
import immersive_aircraft.resources.BBModelLoader;

public class DataLoaders {
    /**
     * No-op hook. Calling it forces the static initializer below to run,
     * which registers the data and resource reload listeners.
     */
    public static void bootstrap() {
    }

    static {
        Registration.registerDataLoader("aircraft_upgrades", new UpgradeDataLoader());
        Registration.registerDataLoader("aircraft", new VehicleDataLoader());

        Registration.registerResourceLoader("objects_bbmodel", new BBModelLoader());
    }
}
