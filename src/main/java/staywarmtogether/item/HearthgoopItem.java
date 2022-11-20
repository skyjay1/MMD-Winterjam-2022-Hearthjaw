package staywarmtogether.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class HearthgoopItem extends BlockItem {

    private static final FoodProperties GOO_FOOD = new FoodProperties.Builder().alwaysEat().build();

    public HearthgoopItem(Block block, Properties properties) {
        super(block, properties.food(GOO_FOOD));
    }

    @Override
    public ItemStack finishUsingItem(ItemStack itemStack, Level level, LivingEntity livingEntity) {
        if(livingEntity.isFreezing()) {
            livingEntity.setTicksFrozen(0);
        } else {
            livingEntity.setSecondsOnFire(2);
        }
        return super.finishUsingItem(itemStack, level, livingEntity);
    }

    @Override
    public void appendHoverText(ItemStack itemStack, @Nullable Level level, List<Component> tooltips, TooltipFlag flag) {
        super.appendHoverText(itemStack, level, tooltips, flag);
        tooltips.add(Component.translatable(getDescriptionId() + ".tooltip").withStyle(ChatFormatting.GRAY));
    }
}
