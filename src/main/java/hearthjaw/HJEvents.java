package hearthjaw;

import hearthjaw.entity.Bloomina;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.List;

public final class HJEvents {

    public static final class ForgeHandler {

        @SubscribeEvent
        public static void onEntityJoinLevel(final EntityJoinLevelEvent event) {
            // modify goals
            if(event.getEntity() instanceof PathfinderMob mob) {
                // illager and undead attack bloomina
                if(mob.getMobType() == MobType.ILLAGER || mob.getMobType() == MobType.UNDEAD) {
                    mob.targetSelector.addGoal(5, new NearestAttackableTargetGoal<>(mob, Bloomina.class, 10, true, true,
                            e -> (e instanceof Bloomina bloomina && !bloomina.isHiding())));
                }
            }
            // lightning strike
            if(event.getEntity() instanceof LightningBolt bolt) {
                // locate nearby bloominas
                AABB aabb = new AABB(bolt.blockPosition()).inflate(48.0D, 16.0D, 48.0D);
                List<Bloomina> list = bolt.level.getEntitiesOfClass(Bloomina.class, aabb);
                // scare each bloomina
                list.forEach(bloomina -> bloomina.scare());
            }

        }
    }

    public static final class ModHandler {

    }
}
