package staywarmtogether;

import staywarmtogether.block.BloominaBudBlock;
import staywarmtogether.block.BloomLanternBlock;
import staywarmtogether.block.HearthgoopBlock;
import staywarmtogether.block.MovingLightBlock;
import staywarmtogether.block.SnowBricksJellyBlock;
import staywarmtogether.entity.Bloomina;
import staywarmtogether.entity.Hearthjaw;
import staywarmtogether.entity.Rimeite;
import staywarmtogether.entity.RimeiteQueen;
import staywarmtogether.entity.SnowBrickProjectile;
import staywarmtogether.item.HearthgoopItem;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.Material;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.event.entity.SpawnPlacementRegisterEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

@SuppressWarnings("unused")
public final class SWTRegistry {

    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, SWTMain.MODID);
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, SWTMain.MODID);
    private static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, SWTMain.MODID);
    private static final DeferredRegister<SoundEvent> SOUND_EVENTS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, SWTMain.MODID);

    public static final CreativeModeTab HJ_TAB = new CreativeModeTab(SWTMain.MODID) {
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
        public static final RegistryObject<Block> BLOOM_LANTERN = BLOCKS.register("bloom_lantern", () ->
                new BloomLanternBlock(BlockBehaviour.Properties.of(Material.WOOD)
                        .strength(3.5F).sound(SoundType.LANTERN).lightLevel(b -> 15).noOcclusion()));
        public static final RegistryObject<Block> HEARTHGOOP = BLOCKS.register("hearthgoop", () ->
                new HearthgoopBlock(BlockBehaviour.Properties.of(Material.WEB)
                        .noCollission().lightLevel(b -> 15)
                        .sound(SoundType.HONEY_BLOCK)));
        public static final RegistryObject<Block> LIGHT = BLOCKS.register("light", () ->
                new MovingLightBlock(BlockBehaviour.Properties.of(Material.AIR)
                        .strength(-1F).noCollission().randomTicks()
                        .lightLevel(b -> b.getValue(MovingLightBlock.LEVEL))));
        public static final RegistryObject<Block> SNOW_BRICKS = BLOCKS.register("snow_bricks", () ->
                new Block(BlockBehaviour.Properties.of(Material.ICE_SOLID)
                        .requiresCorrectToolForDrops()
                        .strength(1.5F, 6.0F)));
        public static final RegistryObject<Block> SNOW_BRICKS_SLAB = BLOCKS.register("snow_bricks_slab", () ->
                new SlabBlock(BlockBehaviour.Properties.of(Material.ICE_SOLID)
                        .requiresCorrectToolForDrops()
                        .strength(1.5F, 6.0F)));
        public static final RegistryObject<Block> SNOW_BRICKS_JELLY = BLOCKS.register("snow_bricks_jelly", () ->
                new SnowBricksJellyBlock(BlockBehaviour.Properties.of(Material.ICE_SOLID)
                        .requiresCorrectToolForDrops()
                        .strength(1.5F, 6.0F)));
    }

    public static final class ItemReg {

        public static void register() {
            ITEMS.register(FMLJavaModLoadingContext.get().getModEventBus());
        }

        public static final RegistryObject<Item> BLOOMINA_BUD = ITEMS.register("bloomina_bud", () ->
                new BlockItem(BlockReg.BLOOMINA_BUD.get(), new Item.Properties().tab(HJ_TAB)));
        public static final RegistryObject<Item> BLOOMLIGHT = ITEMS.register("bloomlight", () ->
                new Item(new Item.Properties().tab(HJ_TAB)));
        public static final RegistryObject<Item> BLOOM_LANTERN = ITEMS.register("bloom_lantern", () ->
                new BlockItem(BlockReg.BLOOM_LANTERN.get(), new Item.Properties().tab(HJ_TAB)));
        public static final RegistryObject<Item> BLOOMLIGHT_ON_A_STICK = ITEMS.register("bloomlight_on_a_stick", () ->
                new Item(new Item.Properties().tab(HJ_TAB)));
        public static final RegistryObject<Item> HEARTHGOOP = ITEMS.register("hearthgoop", () ->
                new HearthgoopItem(BlockReg.HEARTHGOOP.get(), new Item.Properties().tab(HJ_TAB)));
        public static final RegistryObject<Item> RIMEITE_JELLY = ITEMS.register("rimeite_jelly", () ->
                new Item(new Item.Properties().tab(HJ_TAB).stacksTo(16)));
        public static final RegistryObject<Item> SNOW_BRICKS = ITEMS.register("snow_bricks", () ->
                new BlockItem(BlockReg.SNOW_BRICKS.get(), new Item.Properties().tab(HJ_TAB)));
        public static final RegistryObject<Item> SNOW_BRICKS_SLAB = ITEMS.register("snow_bricks_slab", () ->
                new BlockItem(BlockReg.SNOW_BRICKS_SLAB.get(), new Item.Properties().tab(HJ_TAB)));
        public static final RegistryObject<Item> SNOW_BRICKS_JELLY = ITEMS.register("snow_bricks_jelly", () ->
                new BlockItem(BlockReg.SNOW_BRICKS_JELLY.get(), new Item.Properties().tab(HJ_TAB)));
    }

    public static final class EntityReg {

        public static void register() {
            ENTITY_TYPES.register(FMLJavaModLoadingContext.get().getModEventBus());
            FMLJavaModLoadingContext.get().getModEventBus().addListener(EntityReg::onRegisterAttributes);
            FMLJavaModLoadingContext.get().getModEventBus().addListener(EntityReg::onRegisterSpawnPlacements);
        }

        private static void onRegisterAttributes(final EntityAttributeCreationEvent event) {
            event.put(BLOOMINA.get(), Bloomina.createAttributes().build());
            event.put(HEARTHJAW.get(), Hearthjaw.createAttributes().build());
            event.put(RIMEITE_QUEEN.get(), RimeiteQueen.createAttributes().build());
            event.put(RIMEITE.get(), Rimeite.createAttributes().build());
        }

        private static void onRegisterSpawnPlacements(final SpawnPlacementRegisterEvent event) {
            event.register(BLOOMINA.get(), SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Animal::checkAnimalSpawnRules, SpawnPlacementRegisterEvent.Operation.OR);
            event.register(HEARTHJAW.get(), SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Animal::checkAnimalSpawnRules, SpawnPlacementRegisterEvent.Operation.OR);
            event.register(RIMEITE_QUEEN.get(), SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, RimeiteQueen::checkRimeiteQueenSpawnRules, SpawnPlacementRegisterEvent.Operation.OR);
            event.register(RIMEITE.get(), SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Mob::checkMobSpawnRules, SpawnPlacementRegisterEvent.Operation.OR);
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

        public static final RegistryObject<EntityType<RimeiteQueen>> RIMEITE_QUEEN = ENTITY_TYPES.register("rimeite_queen", () ->
                EntityType.Builder.of(RimeiteQueen::new, MobCategory.CREATURE)
                        .sized(0.98F, 1.25F)
                        .build("rimeite_queen"));

        public static final RegistryObject<EntityType<Rimeite>> RIMEITE = ENTITY_TYPES.register("rimeite", () ->
                EntityType.Builder.of(Rimeite::new, MobCategory.CREATURE)
                        .sized(0.5F, 0.5F)
                        .build("rimeite"));

        public static final RegistryObject<EntityType<? extends SnowBrickProjectile>> SNOW_BRICK = ENTITY_TYPES.register("snow_brick", () ->
                EntityType.Builder.<SnowBrickProjectile>of(SnowBrickProjectile::new, MobCategory.MISC)
                        .sized(0.25F, 0.25F)
                        .noSave().noSummon()
                        .clientTrackingRange(4).updateInterval(5)
                        .build("snow_brick"));
    }

    public static final class SoundReg {

        public static void register() {
            SOUND_EVENTS.register(FMLJavaModLoadingContext.get().getModEventBus());
        }

        private static RegistryObject<SoundEvent> register(final String name) {
            return SOUND_EVENTS.register(name, () -> new SoundEvent(new ResourceLocation(SWTMain.MODID, name)));
        }

        public static final RegistryObject<SoundEvent> BLOOMINA_AMBIENT = register("entity.bloomina.ambient");
        public static final RegistryObject<SoundEvent> BLOOMINA_BURP = register("entity.bloomina.burp");
        public static final RegistryObject<SoundEvent> BLOOMINA_HURT = register("entity.bloomina.hurt");
        public static final RegistryObject<SoundEvent> BLOOMINA_SCARED = register("entity.bloomina.scared");
        public static final RegistryObject<SoundEvent> HEARTHJAW_AMBIENT = register("entity.hearthjaw.ambient");
        public static final RegistryObject<SoundEvent> HEARTHJAW_BREATHE = register("entity.hearthjaw.breathe");
        public static final RegistryObject<SoundEvent> HEARTHJAW_HURT = register("entity.hearthjaw.hurt");
    }
}
