package fr.missilemod.block;

import fr.missilemod.explosion.ImpactProfiles;
import fr.missilemod.explosion.ImpactScheduler;
import fr.missilemod.explosion.ImpactSystem;
import fr.missilemod.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.common.Tags;

/**
 * Mine terrestre (2.4).
 * Antipersonnel : n'importe quelle entite qui marche dessus -> "clic" puis explosion 0,5 s apres.
 * Antichar : entite lourde (golem, ravageur, vehicule avec passager...) ou joueur accroupi dessus pendant 3 s.
 * Desamorcable avec une cisaille (rendue) ; cassee a la main, elle explose.
 */
public class LandMineBlock extends Block {

    public static final BooleanProperty TRIGGERED = BooleanProperty.create("triggered");
    public static final IntegerProperty PRESSURE = IntegerProperty.create("pressure", 0, 6);
    /** Sous-munition non explosee : ne se recupere pas a la cisaille. */
    public static final BooleanProperty FROM_BOMBLET = BooleanProperty.create("from_bomblet");

    private static final VoxelShape SHAPE_AP = Block.box(4.0D, 0.0D, 4.0D, 12.0D, 2.0D, 12.0D);
    private static final VoxelShape SHAPE_AT = Block.box(2.0D, 0.0D, 2.0D, 14.0D, 3.0D, 14.0D);
    private static final int FUSE_TICKS = 10;
    private static final int PRESSURE_CHECK_TICKS = 10;

    private final boolean antiTank;

    public LandMineBlock(boolean antiTank, Properties properties) {
        super(properties);
        this.antiTank = antiTank;
        registerDefaultState(this.stateDefinition.any()
                .setValue(TRIGGERED, false).setValue(PRESSURE, 0).setValue(FROM_BOMBLET, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(TRIGGERED, PRESSURE, FROM_BOMBLET);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return this.antiTank ? SHAPE_AT : SHAPE_AP;
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return Block.canSupportCenter(level, pos.below(), Direction.UP);
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighbor, LevelAccessor level,
                                  BlockPos pos, BlockPos neighborPos) {
        return direction == Direction.DOWN && !state.canSurvive(level, pos)
                ? Blocks.AIR.defaultBlockState()
                : super.updateShape(state, direction, neighbor, level, pos, neighborPos);
    }

    // ---------------------------------------------------------------- declenchement

    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (level.isClientSide || state.getValue(TRIGGERED) || !isTriggerCandidate(entity)) {
            return;
        }
        AABB plate = new AABB(pos).setMaxY(pos.getY() + 0.25D);
        if (!entity.getBoundingBox().intersects(plate)) {
            return;
        }
        if (!this.antiTank) {
            trigger(level, pos, state);
            return;
        }
        if (isHeavy(entity)) {
            trigger(level, pos, state);
        } else if (entity instanceof Player player && player.isCrouching()
                && level.getGameTime() % PRESSURE_CHECK_TICKS == 0) {
            int pressure = state.getValue(PRESSURE) + 1;
            if (pressure >= 6) {
                trigger(level, pos, state);
            } else {
                level.setBlock(pos, state.setValue(PRESSURE, pressure), 3);
                level.playSound(null, pos, SoundEvents.TRIPWIRE_CLICK_OFF, SoundSource.BLOCKS, 0.3F, 0.5F + pressure * 0.1F);
                level.scheduleTick(pos, this, PRESSURE_CHECK_TICKS + 5);
            }
        }
    }

    private static boolean isTriggerCandidate(Entity entity) {
        return !(entity instanceof ItemEntity) && !(entity instanceof ExperienceOrb) && !(entity instanceof Projectile)
                && !entity.isSpectator() && !(entity instanceof Player player && player.isCreative());
    }

    private static boolean isHeavy(Entity entity) {
        return entity.isVehicle() || entity.getBbWidth() * entity.getBbWidth() * entity.getBbHeight() >= 4.0F;
    }

    private void trigger(Level level, BlockPos pos, BlockState state) {
        level.setBlock(pos, state.setValue(TRIGGERED, true), 3);
        level.playSound(null, pos, ModSounds.MINE_CLICK.get(), SoundSource.BLOCKS, 1.0F, this.antiTank ? 0.7F : 1.0F);
        level.scheduleTick(pos, this, FUSE_TICKS);
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (state.getValue(TRIGGERED)) {
            explode(level, pos);
            return;
        }
        if (state.getValue(PRESSURE) > 0) {
            // Plus personne d'accroupi dessus : la pression retombe.
            boolean stillPressed = !level.getEntitiesOfClass(Player.class, new AABB(pos).setMaxY(pos.getY() + 0.5D),
                    Player::isCrouching).isEmpty();
            if (!stillPressed) {
                level.setBlock(pos, state.setValue(PRESSURE, 0), 3);
            } else {
                level.scheduleTick(pos, this, PRESSURE_CHECK_TICKS + 5);
            }
        }
    }

    private void explode(ServerLevel level, BlockPos pos) {
        level.removeBlock(pos, false);
        ImpactSystem.detonate(level, Vec3.atBottomCenterOf(pos).add(0.0D, 0.2D, 0.0D), null, null,
                this.antiTank ? ImpactProfiles.MINE_AT : ImpactProfiles.MINE_AP);
    }

    // ---------------------------------------------------------------- desamorcage, casse, explosions voisines

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
                                 BlockHitResult hit) {
        ItemStack held = player.getItemInHand(hand);
        if (!held.is(Tags.Items.SHEARS)) {
            return InteractionResult.PASS;
        }
        if (state.getValue(TRIGGERED)) {
            return InteractionResult.FAIL; // trop tard !
        }
        if (!level.isClientSide) {
            level.removeBlock(pos, false);
            if (!state.getValue(FROM_BOMBLET)) {
                Block.popResource(level, pos, new ItemStack(this));
            }
            level.playSound(null, pos, SoundEvents.SHEEP_SHEAR, SoundSource.BLOCKS, 1.0F, 1.4F);
            held.hurtAndBreak(1, player, p -> p.broadcastBreakEvent(hand));
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        super.playerWillDestroy(level, pos, state, player);
        if (level instanceof ServerLevel serverLevel && !player.isCreative()) {
            // Casser une mine sans la desamorcer la fait exploser.
            ImpactScheduler.schedule(serverLevel, 2, () -> ImpactSystem.detonate(serverLevel,
                    Vec3.atBottomCenterOf(pos).add(0.0D, 0.2D, 0.0D), null, player,
                    this.antiTank ? ImpactProfiles.MINE_AT : ImpactProfiles.MINE_AP));
        }
    }

    @Override
    public void wasExploded(Level level, BlockPos pos, Explosion explosion) {
        if (level instanceof ServerLevel serverLevel) {
            // Explosion par sympathie, legerement decalee.
            ImpactScheduler.schedule(serverLevel, 3 + serverLevel.random.nextInt(6), () -> ImpactSystem.detonate(serverLevel,
                    Vec3.atBottomCenterOf(pos).add(0.0D, 0.2D, 0.0D), null, null,
                    this.antiTank ? ImpactProfiles.MINE_AT : ImpactProfiles.MINE_AP));
        }
    }

    @Override
    public boolean dropFromExplosion(Explosion explosion) {
        return false;
    }
}
