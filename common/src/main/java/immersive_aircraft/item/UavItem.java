package immersive_aircraft.item;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * UAV item - placeholder. Cannot be placed or used, only crafted.
 */
public class UavItem extends DescriptionItem {
    public UavItem(Properties settings) {
        super(settings);
    }

    @Override
    public InteractionResult use(Level world, Player user, InteractionHand hand) {
        return InteractionResult.PASS;
    }
}