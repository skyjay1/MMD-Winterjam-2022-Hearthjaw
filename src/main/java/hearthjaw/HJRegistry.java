package hearthjaw;

import hearthjaw.block.BloominaBudBlock;
import hearthjaw.block.BloomlightLanternBlock;
import hearthjaw.block.HearthgoopBlock;
import hearthjaw.block.MovingLightBlock;
import hearthjaw.entity.Bloomina;
import hearthjaw.entity.Hearthjaw;
import hearthjaw.item.HearthgoopItem;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.Material;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

@SuppressWarnings("unused")
public final class HJRegistry {

    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, HJMain.MODID);
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, HJMain.MODID);
    private static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, HJMain.MODID);
    private static final DeferredRegister<SoundEvent> SOUND_EVENTS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, HJMain.MODID);

    public static final CreativeModeTab HJ_TAB = new CreativeModeTab(HJMain.MODID) {
        @Override
        public ItemStack makeIcon() {
            return new ItemStack(ItemReg.HEARTHGOOP.get());
        }
    };

    public static void register() {
        BlockReg.register();
        ItemReg.register();
        EntityReg.register();
        SoundReg.register();
    }

    public static final class BlockReg {

        public static void register() {
            BLOCKS.register(FMLJavaModLoadingContext.get().getModEventBus());
        }

        public static final RegistryObject<Block> BLOOMINA_BUD = BLOCKS.register("bloomina_bud", () ->
                new BloominaBudBlock(BlockBehaviour.Properties.of(Material.PLANT)
                        .noCollission().lightLevel(b -> 11).randomTicks()
                        .sound(SoundType.CROP)));
        public static final RegistryObject<Block> BLOOMLIGHT_LANTERN = BLOCKS.register("bloomlight_lantern", () ->
                new BloomlightLanternBlock(BlockBehaviour.Properties.of(Material.WOOD)
                        .strength(3.5F).sound(SoundType.LANTERN).lightLevel(b -> 15).noOcclusion()));
        public static final RegistryObject<Block> HEARTHGOOP = BLOCKS.register("hearthgoop", () ->
                new HearthgoopBlock(BlockBehaviour.Properties.of(Material.WEB)
                        .noCollission().lightLevel(b -> 15)
                        .sound(SoundType.HONEY_BLOCK)));
        public static final RegistryObject<Block> LIGHT = BLOCKS.register("light", () ->
                new MovingLightBlock(BlockBehaviour.Properties.of(Material.AIR)
                        .strength(-1F).noCollission().randomTicks()
                        .lightLevel(b -> b.getValue(MovingLightBlock.LEVEL))));
    }

    public static final class ItemReg {

        public static void register() {
            ITEMS.register(FMLJavaModLoadingContext.get().getModEventBus());
        }

        public static final RegistryObject<Item> BLOOMINA_BUD = ITEMS.register("bloomina_bud", () ->
                new BlockItem(BlockReg.BLOOMINA_BUD.get(), new Item.Properties().tab(HJ_TAB)));
        public static final RegistryObject<Item> BLOOMLIGHT = ITEMS.register("bloomlight", () ->
                new Item(new Item.Properties().tab(HJ_TAB)));
        public static final RegistryObject<Item> BLOOMINA_LANTERN = ITEMS.register("bloomlight_lantern", () ->
                new BlockItem(BlockReg.BLOOMLIGHT_LANTERN.get(), new Item.Properties().tab(HJ_TAB)));
        public static final RegistryObject<Item> BLOOMLIGHT_ROD = ITEMS.register("bloomlight_rod", () ->
                new Item(new Item.Properties().tab(HJ_TAB)));
        public static final RegistryObject<Item> HEARTHGOOP = ITEMS.register("hearthgoop", () ->
                new HearthgoopItem(BlockReg.HEARTHGOOP.get(), new Item.Properties().tab(HJ_TAB)));
    }

    public static final class EntityReg {

        public static void register() {
            ENTITY_TYPES.register(FMLJavaModLoadingContext.get().getModEventBus());
            FMLJavaModLoadingContext.get().getModEventBus().addListener(EntityReg::onRegisterAttributes);
        }

        private static void onRegisterAttributes(final EntityAttributeCreationEvent event) {
            event.put(BLOOMINA.get(), Bloomina.createAttributes().build());
            event.put(HEARTHJAW.get(), Hearthjaw.createAttributes().build());
        }

        public static final RegistryObject<EntityType<Bloomina>> BLOOMINA = ENTITY_TYPES.register("bloomina", () ->
                EntityType.Builder.of(Bloomina::new, MobCategory.CREATURE)
                        .sized(0.75F, 0.98F)
                        .build("bloomina"));

        public static final RegistryObject<EntityType<Hearthjaw>> HEARTHJAW = ENTITY_TYPES.register("hearthjaw", () ->
                EntityType.Builder.of(Hearthjaw::new, MobCategory.CREATURE)
                        .sized(1.39F, 1.125F)
                        .fireImmune()
                        .build("hearthjaw"));
    }

    public static final class SoundReg {

        public static void register() {
            SOUND_EVENTS.register(FMLJavaModLoadingContext.get().getModEventBus());
        }

        private static RegistryObject<SoundEvent> register(final String name) {
            return SOUND_EVENTS.register(name, () -> new SoundEvent(new ResourceLocation(HJMain.MODID, name)));
        }

        public static final RegistryObject<SoundEvent> BLOOMINA_AMBIENT = register("entity.bloomina.ambient");
        public static final RegistryObject<SoundEvent> BLOOMINA_BURP = register("entity.bloomina.burp");
        public static final RegistryObject<SoundEvent> HEARTHJAW_AMBIENT = register("entity.hearthjaw.ambient");
        public static final RegistryObject<SoundEvent> HEARTHJAW_BREATHE = register("entity.hearthjaw.breathe");
        public static final RegistryObject<SoundEvent> HEARTHJAW_HURT = register("entity.hearthjaw.hurt");
    }
}
