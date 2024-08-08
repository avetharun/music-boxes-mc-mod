package com.feintha.musicboxes.block;

import com.feintha.musicboxes.Music_boxes;
import com.feintha.musicboxes.block.entity.MusicBoxBlockEntity;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIntArray;
import net.minecraft.network.packet.s2c.play.StopSoundS2CPacket;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.server.command.StopSoundCommand;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.IntProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Nameable;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.*;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Objects;

@SuppressWarnings("deprecation")
public class MusicBoxBlock extends BlockWithEntity implements Oxidizable{
    public final int ticksBetweenNotes;
    public final static BooleanProperty PLAYING = BooleanProperty.of("playing");
    public final static BooleanProperty MANUAL_PLAYING = BooleanProperty.of("manualy_playing");
    final static BooleanProperty WAXED = BooleanProperty.of("waxed");
    public MusicBoxBlock(Settings settings, int ticksBetweenNotes) {
        super(settings);
        this.ticksBetweenNotes = ticksBetweenNotes;
        this.setDefaultState((BlockState)this.getDefaultState().with(PLAYING, false).with(MANUAL_PLAYING, false).with(WAXED, false).with(Properties.OPEN, false));
    }

    @Override
    public void onBreak(World world, BlockPos pos, BlockState state, PlayerEntity player) {
        if (world.getBlockEntity(pos) instanceof MusicBoxBlockEntity mbe) {
            if (mbe.hasCustomSong()) {
                if (world instanceof ServerWorld sw) {
                    StopSoundS2CPacket stopSoundS2CPacket = new StopSoundS2CPacket(MusicBoxBlockEntity.NAME_TO_MUSIC.get(mbe.getCustomSongName()).getLeft(), SoundCategory.RECORDS);
                    sw.getServer().getPlayerManager().sendToAround(null, pos.getX(), pos.getY(), pos.getZ(), 64, world.getRegistryKey(), stopSoundS2CPacket);
                }
            }

            if (!world.isClient && !mbe.PublishedNotes.isEmpty()) {
                mbe.setSongName(null);
                ItemStack newStack = Music_boxes.DRUM_SPINDLE.getDefaultStack();
                NbtCompound compound = new NbtCompound();
                compound.putIntArray("notes", mbe.PublishedNotes);
                compound.putInt("length", mbe.songLength);
                newStack.setSubNbt("song", compound);
                ItemEntity ie = new ItemEntity(world, pos.getX() + 0.5f, pos.getY() + 0.5f, pos.getZ() + 0.5f, newStack);
                ie.addVelocity((world.random.nextBetween(0, 10)-5) /90f, (world.random.nextBetween(0, 10)-5) /90f, (world.random.nextBetween(0, 10)-5) /90f);
                world.spawnEntity(ie);
                mbe.PublishedNotes.clear();
                mbe.NotesRuntime.clear();
                mbe.sync();
            }
        }
        super.onBreak(world, pos, state, player);
    }

    public boolean isPlaying(BlockState state) {
        return state.get(PLAYING) || state.get(MANUAL_PLAYING);
    }
    public boolean hasValidNoteBlockBelow(World world, BlockPos pos) {
        return world.getBlockState(pos.down()).isOf(Blocks.NOTE_BLOCK);
    }

    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }

    @Nullable
    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        return super.getPlacementState(ctx).with(Properties.HORIZONTAL_FACING, ctx.getHorizontalPlayerFacing().getOpposite());
    }

    @Override
    public void onPlaced(World world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack itemStack) {
        super.onPlaced(world, pos, state, placer, itemStack);

        ((MusicBoxBlockEntity) Objects.requireNonNull(world.getBlockEntity(pos), "Missing blocks entity")).setCustomName(itemStack.getName());
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (player.getStackInHand(hand).isOf(Items.HONEYCOMB) &&!state.get(WAXED)) {
            world.setBlockState(pos, state.with(WAXED, true));
            if (!player.isCreative()) player.getStackInHand(hand).decrement(1);
            world.playSound(null, pos.getX(), pos.getY(), pos.getZ(), SoundEvents.ITEM_HONEYCOMB_WAX_ON, SoundCategory.BLOCKS, 1, 1);
            return ActionResult.CONSUME;
        } else if (player.getStackInHand(hand).isIn(ItemTags.AXES)) {
            if (world.getBlockState(pos).get(WAXED)) {
                world.setBlockState(pos, state.with(WAXED, false));
                player.getStackInHand(hand).damage(1, player, playerEntity -> playerEntity.sendToolBreakStatus(hand));
                world.playSound(null, pos.getX(), pos.getY(), pos.getZ(), SoundEvents.ITEM_AXE_WAX_OFF, SoundCategory.BLOCKS, 1, 1);
                return ActionResult.CONSUME;
            } else {
                if (world.getBlockEntity(pos) instanceof MusicBoxBlockEntity mbe) {
                    if (getDegradationLevel() != OxidationLevel.UNAFFECTED) {
                        world.playSound(null, pos.getX(), pos.getY(), pos.getZ(), SoundEvents.ITEM_AXE_SCRAPE, SoundCategory.BLOCKS, 1, 1);
                        Objects.requireNonNull(world.getBlockEntity(pos), "BlockEntity not created!").readNbt(mbe.writeNbtAlt(new NbtCompound()));

                        player.getStackInHand(hand).damage(1, player, playerEntity -> playerEntity.sendToolBreakStatus(hand));
                        world.setBlockState(pos, getScrapedVersion(getDegradationLevel(), state));
                        mbe.skipReadNbtPass = true;
                        world.addBlockEntity(mbe);
                    }
                }
                return ActionResult.CONSUME;
            }
        }else if (player.getStackInHand(hand).isOf(Music_boxes.MUSIC_BOX_KEY)) {
            world.setBlockState(pos, state.cycle(Properties.OPEN));
            if (!state.get(Properties.OPEN)) {
                world.playSound(null, pos.getX()+0.5f, pos.getY(), pos.getZ()+0.5f, SoundEvents.BLOCK_BARREL_OPEN, SoundCategory.BLOCKS, 0.2f, 2);
            } else world.playSound(null, pos.getX()+0.5f, pos.getY(), pos.getZ()+0.5f, SoundEvents.BLOCK_BARREL_CLOSE, SoundCategory.BLOCKS, 0.2f, 1.2f);
            return ActionResult.SUCCESS;
        }
        else if (player.getStackInHand(hand).isOf(Music_boxes.DRUM_SPINDLE) && !state.get(WAXED)) {
            if (world.getBlockEntity(pos) instanceof MusicBoxBlockEntity mbe) {
                ItemStack stack = player.getStackInHand(hand);
                if (stack.getOrCreateNbt().contains("song") && stack.getOrCreateNbt().getCompound("song").contains("notes") && stack.getOrCreateNbt().getCompound("song").getIntArray("notes").length > 0) {
                    mbe.PublishedNotes.clear();
                    if (stack.hasNbt() && stack.getOrCreateNbt().contains("song")) {
                        for (int note : Objects.requireNonNull(stack.getSubNbt("song"), "Missing compound 'song'!").getIntArray("notes")) {
                            mbe.PublishedNotes.add(note);
                        }
                    }
                    if (stack.getOrCreateSubNbt("song").contains("length")) { mbe.songLength = stack.getOrCreateSubNbt("song").getInt("length"); }
                    if (stack.hasCustomName()) {
                        mbe.setSongName(stack.getName());
                    }
                    if (!player.isCreative()) stack.decrement(1);
                    mbe.sync();
                }
            }
            return ActionResult.CONSUME;
        }
        else if (!player.isSneaking()){
            world.setBlockState(pos, state.cycle(MANUAL_PLAYING));
            if (world.getBlockEntity(pos) instanceof MusicBoxBlockEntity mbe) {
                if (mbe.hasCustomSong()) {
                    if ((world.getBlockState(pos).get(PLAYING) || world.getBlockState(pos).get(MANUAL_PLAYING)) && mbe.ticksUntilNextNote > 0) {
                        mbe.ticksUntilNextNote = 0;
                    }
                    if (world instanceof ServerWorld sw) {
                        StopSoundS2CPacket stopSoundS2CPacket = new StopSoundS2CPacket(MusicBoxBlockEntity.NAME_TO_MUSIC.get(mbe.getCustomSongName()).getLeft(), SoundCategory.RECORDS);
                        sw.getServer().getPlayerManager().sendToAround(null, pos.getX(), pos.getY(), pos.getZ(), 64, world.getRegistryKey(), stopSoundS2CPacket);
                    }
                }
            }
            return ActionResult.CONSUME;
        } else if (player.isSneaking() && player.getStackInHand(hand).isEmpty()&& !state.get(WAXED)) {
            if (world.getBlockEntity(pos) instanceof MusicBoxBlockEntity mbe && !world.isClient && !mbe.PublishedNotes.isEmpty()) {
                mbe.setSongName(null);
                ItemStack newStack = Music_boxes.DRUM_SPINDLE.getDefaultStack();
                NbtCompound compound = new NbtCompound();
                compound.putIntArray("notes", mbe.PublishedNotes);
                compound.putInt("length", mbe.songLength);
                newStack.setSubNbt("song", compound);
                ItemEntity ie = new ItemEntity(world, pos.getX() + 0.5f, pos.getY() + 0.5f, pos.getZ() + 0.5f, newStack);
                ie.addVelocity((world.random.nextBetween(0, 10)-5) /90f, (world.random.nextBetween(0, 10)-5) /90f, (world.random.nextBetween(0, 10)-5) /90f);
                world.spawnEntity(ie);
                mbe.PublishedNotes.clear();
                mbe.NotesRuntime.clear();
                mbe.sync();
            }
        }
        return super.onUse(state, world, pos, player, hand, hit);
    }
    public void neighborUpdate(BlockState state, World world, BlockPos pos, Block sourceBlock, BlockPos sourcePos, boolean notify) {
        if (!world.isClient) {
            boolean bl = (Boolean)state.get(PLAYING);
            if (world.getBlockEntity(pos) instanceof MusicBoxBlockEntity mbe) {
                if (mbe.hasCustomSong()) {
                    world.emitGameEvent(GameEvent.JUKEBOX_STOP_PLAY, pos, GameEvent.Emitter.of(state));
                }
                if (bl != world.isReceivingRedstonePower(pos)) {
                    mbe.ticksUntilNextNote = 0;
                    if (bl) {
                        world.scheduleBlockTick(pos, this, 4);
                    } else {
                        world.setBlockState(pos, (BlockState)state.cycle(PLAYING), 2);
                    }
                }
            }

        }
    }

    @Override
    public boolean hasComparatorOutput(BlockState state) {
        return state.get(PLAYING) || state.get(MANUAL_PLAYING);
    }

    @Override
    public int getComparatorOutput(BlockState state, World world, BlockPos pos) {
//        if (world.getBlockEntity(pos) instanceof MusicBoxBlockEntity mbe && ) {
//            if (state.get(Properties.OPEN)) {
//                if (mbe.NotesRuntime.isEmpty()) {
//                    return 0;
//                }
//                int i1 = mbe.NotesRuntime.peekFirst();
//                int i2 = 0;
//                for (int i = 0; i < 32; i++) {
//                    if ((i1 >> i & 1) > 0) {
//                        i2 = i;
//                    }
//                }
//                if (i2 == 0) {
//                    return 0;
//                }
//                return MathHelper.lerp(((float) i2 / 32), 0, 15);
//            } else {
//            }
//        }
        return isPlaying(state) ? 15 / (getDegradationLevel().ordinal() + 1) : 0;
    }
    public void scheduledTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        if ((Boolean)state.get(PLAYING) && !world.isReceivingRedstonePower(pos)) {
            world.setBlockState(pos, (BlockState)state.cycle(PLAYING), 2);
        }
        if (world.getBlockEntity(pos) instanceof MusicBoxBlockEntity mbe) {
            if (mbe.hasCustomSong()) {
                if ((world.getBlockState(pos).get(PLAYING))) {
                    mbe.ticksUntilNextNote = (int) (20 * MusicBoxBlockEntity.NAME_TO_MUSIC.get(mbe.getCustomSongName()).getRight()) + 1;
                    world.playSound((PlayerEntity) null, (double) pos.getX() + 0.5, (double) pos.getY() + 0.5, (double) pos.getZ() + 0.5, SoundEvent.of(MusicBoxBlockEntity.NAME_TO_MUSIC.get(mbe.getCustomSongName()).getLeft()), SoundCategory.RECORDS, 1F, 1, world.random.nextLong());
                } else {
                    StopSoundS2CPacket stopSoundS2CPacket = new StopSoundS2CPacket(MusicBoxBlockEntity.NAME_TO_MUSIC.get(mbe.getCustomSongName()).getLeft(), SoundCategory.BLOCKS);
                    world.getServer().getPlayerManager().sendToAround(null, pos.getX(), pos.getY(), pos.getZ(), 64, world.getRegistryKey(), stopSoundS2CPacket);
                }
            }
        }
        world.updateComparators(pos, state.getBlock());
    }

    @Nullable @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) { return MusicBoxBlockEntity::tick; }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        super.appendProperties(builder.add(PLAYING, MANUAL_PLAYING, WAXED, Properties.HORIZONTAL_FACING, Properties.OPEN));
        // PURE
        // EXPOSED
        // WEATHERED
        // OXIDIZED
    }
    @Override
    public void randomTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        if (state.get(WAXED)) {return;}
        if (world.getBlockEntity(pos) instanceof MusicBoxBlockEntity mbe) {
            // Either every tick, with a 1/3 chance or every tick while open, a 1/2 chance
            if (world.getRandom().nextBetween(0, 3) == 2 || (state.get(Properties.OPEN) && world.getRandom().nextBoolean())) {
                if (getDegradationLevel() != OxidationLevel.OXIDIZED) { world.setBlockState(pos, getDegradedVersion(getDegradationLevel(), state)); }
                mbe.skipReadNbtPass = true;
                world.addBlockEntity(mbe);
            }
        }
        super.randomTick(state, world, pos, random);
    }


    private static final VoxelShape NORTH_OPEN;
    private static final VoxelShape SOUTH_OPEN;
    private static final VoxelShape WEST_OPEN;
    private static final VoxelShape EAST_OPEN;

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        if (state.get(Properties.OPEN)) {
            return switch (state.get(Properties.HORIZONTAL_FACING)) {
                case NORTH -> NORTH_OPEN;
                case SOUTH -> SOUTH_OPEN;
                case WEST -> WEST_OPEN;
                case EAST -> EAST_OPEN;
                default -> throw new IllegalStateException("Unexpected value: " + state.get(Properties.HORIZONTAL_FACING));
            };
        }
        else {
            if (state.get(Properties.HORIZONTAL_FACING).getAxis() == Direction.Axis.X) {
                return VoxelShapes.cuboid(0.3125000000000001, 0, 0.25, 0.6875000000000001, 0.25, 0.75);
            }
            if (state.get(Properties.HORIZONTAL_FACING).getAxis() == Direction.Axis.Z) {
                return VoxelShapes.cuboid(0.2500000000000001, 0, 0.3125, 0.7500000000000001, 0.25, 0.6875);
            }
        }
        return VoxelShapes.fullCube();
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new MusicBoxBlockEntity(pos,state);
    }


    static {
        NORTH_OPEN = VoxelShapes.union(
                VoxelShapes.cuboid(0.2500000000000001, 0, 0.3125, 0.7500000000000001, 0.1875, 0.375),
                VoxelShapes.cuboid(0.2500000000000001, 0.1875, 0.6875, 0.7500000000000001, 0.5625, 0.75),
                VoxelShapes.cuboid(0.2500000000000001, 0, 0.625, 0.7500000000000001, 0.1875, 0.6875),
                VoxelShapes.cuboid(0.2500000000000001, 0, 0.375, 0.3125000000000001, 0.1875, 0.625),
                VoxelShapes.cuboid(0.6875000000000001, 0, 0.375, 0.7500000000000001, 0.1875, 0.625),
                VoxelShapes.cuboid(0.3125000000000001, 0, 0.375, 0.6875000000000001, 0.125, 0.625)
        );
        SOUTH_OPEN = VoxelShapes.union(
                VoxelShapes.cuboid(0.2500000000000001, 0, 0.625, 0.7500000000000001, 0.1875, 0.6875),
                VoxelShapes.cuboid(0.2500000000000001, 0.1875, 0.25, 0.7500000000000001, 0.5625, 0.3125),
                VoxelShapes.cuboid(0.2500000000000001, 0, 0.3125, 0.7500000000000001, 0.1875, 0.375),
                VoxelShapes.cuboid(0.6875000000000001, 0, 0.375, 0.7500000000000001, 0.1875, 0.625),
                VoxelShapes.cuboid(0.2500000000000001, 0, 0.375, 0.3125000000000001, 0.1875, 0.625),
                VoxelShapes.cuboid(0.3125000000000001, 0, 0.375, 0.6875000000000001, 0.125, 0.625)
        );
        WEST_OPEN = VoxelShapes.union(
                VoxelShapes.cuboid(0.3125000000000001, 0, 0.25, 0.3750000000000001, 0.1875, 0.75),
                VoxelShapes.cuboid(0.6875000000000001, 0.1875, 0.25, 0.7500000000000001, 0.5625, 0.75),
                VoxelShapes.cuboid(0.6250000000000001, 0, 0.25, 0.6875000000000001, 0.1875, 0.75),
                VoxelShapes.cuboid(0.3750000000000001, 0, 0.6875, 0.6250000000000001, 0.1875, 0.75),
                VoxelShapes.cuboid(0.3750000000000001, 0, 0.25, 0.6250000000000001, 0.1875, 0.3125),
                VoxelShapes.cuboid(0.3750000000000001, 0, 0.3125, 0.6250000000000001, 0.125, 0.6875)
        );
        EAST_OPEN = VoxelShapes.union(
                VoxelShapes.cuboid(0.6250000000000001, 0, 0.25, 0.6875000000000001, 0.1875, 0.75),
                VoxelShapes.cuboid(0.2500000000000001, 0.1875, 0.25, 0.3125000000000001, 0.5625, 0.75),
                VoxelShapes.cuboid(0.3125000000000001, 0, 0.25, 0.3750000000000001, 0.1875, 0.75),
                VoxelShapes.cuboid(0.3750000000000001, 0, 0.25, 0.6250000000000001, 0.1875, 0.3125),
                VoxelShapes.cuboid(0.3750000000000001, 0, 0.6875, 0.6250000000000001, 0.1875, 0.75),
                VoxelShapes.cuboid(0.3750000000000001, 0, 0.3125, 0.6250000000000001, 0.125, 0.6875)
        );
    }

    public OxidationLevel getDegradationLevel() {
        if (this == Music_boxes.MUSIC_BOX_BLOCK) {return OxidationLevel.UNAFFECTED;}
        if (this == Music_boxes.EXPOSED_MUSIC_BOX_BLOCK) {return OxidationLevel.EXPOSED;}
        if (this == Music_boxes.WEATHERED_MUSIC_BOX_BLOCK) {return OxidationLevel.WEATHERED;}
        if (this == Music_boxes.OXIDIZED_MUSIC_BOX_BLOCK) {return OxidationLevel.OXIDIZED;}
        return OxidationLevel.UNAFFECTED;
    }
    public BlockState getDegradedVersion(OxidationLevel current, BlockState currentBlockState) {
        return switch (current) {
            case UNAFFECTED -> Music_boxes.EXPOSED_MUSIC_BOX_BLOCK.getStateWithProperties(currentBlockState);
            case EXPOSED -> Music_boxes.WEATHERED_MUSIC_BOX_BLOCK.getStateWithProperties(currentBlockState);
            case WEATHERED, OXIDIZED -> Music_boxes.OXIDIZED_MUSIC_BOX_BLOCK.getStateWithProperties(currentBlockState);
        };
    }
    public BlockState getScrapedVersion(OxidationLevel current, BlockState currentBlockState) {
        return switch (current) {
            case UNAFFECTED, EXPOSED -> Music_boxes.MUSIC_BOX_BLOCK.getStateWithProperties(currentBlockState);
            case WEATHERED -> Music_boxes.EXPOSED_MUSIC_BOX_BLOCK.getStateWithProperties(currentBlockState);
            case OXIDIZED -> Music_boxes.WEATHERED_MUSIC_BOX_BLOCK.getStateWithProperties(currentBlockState);
        };
    }
}
