package hearthjaw;

import hearthjaw.entity.Bloomina;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public final class HJEvents {

    public static final class ForgeHandler {

        @SubscribeEvent
        public static void onEntityJoinLevel(final EntityJoinLevelEvent event) {
            if(event.getEntity() instanceof PathfinderMob mob) {
                if(mob.getMobType() == MobType.ILLAGER || mob.getMobType() == MobType.UNDEAD) {
                    mob.targetSelector.addGoal(5, new NearestAttackableTargetGoal<>(mob, Bloomina.class, 10, true, true,
                            e -> (e instanceof Bloomina bloomina && !bloomina.isHiding())));
                }
            }
        }
    }

    public static final class ModHandler {

    }
}
