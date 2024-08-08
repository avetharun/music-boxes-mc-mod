package com.feintha.musicboxes.block.entity;

import com.feintha.musicboxes.Music_boxes;
import com.feintha.musicboxes.block.MusicBoxBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.NoteBlock;
import net.minecraft.block.ObserverBlock;
import net.minecraft.block.entity.*;
import net.minecraft.block.enums.Instrument;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.packet.s2c.play.StopSoundS2CPacket;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Nameable;
import net.minecraft.util.Pair;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class MusicBoxBlockEntity extends BlockEntity implements Nameable {
    public MusicBoxBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }
    public MusicBoxBlockEntity(BlockPos pos, BlockState state) {
        super(Music_boxes.MUSIC_BOX_BLOCK_ENTITY_TYPE, pos, state);
    }
    public int ticksUntilNextNote = 0;
    public int songNotesLeft = 64;
    public int songLength = 64;

    public void setCustomName(@Nullable Text customName) {
        this.customName = customName;
    }
    public void setSongName(@Nullable Text customName) {
        this.songName = customName;
    }

    public @NotNull Text getName() {
        return this.songName == null ? (this.customName != null ? this.customName : this.getContainerName()) : this.songName;
    }

    public Text getDisplayName() {
        return this.getName();
    }

    @Nullable
    public Text getCustomName() {
        return this.customName;
    }
    public final ArrayList<Integer> PublishedNotes = new ArrayList<>();
    public final ArrayDeque<Integer> NotesRuntime = new ArrayDeque<>();
    @Nullable
    private Text customName;
    @Nullable
    private Text songName;
    public boolean skipReadNbtPass = false;
    @Override
    public void readNbt(NbtCompound nbt) {
        if (skipReadNbtPass) { skipReadNbtPass = false; }
        super.readNbt(nbt);
        PublishedNotes.clear();
        NotesRuntime.clear();
        for (int note : nbt.getIntArray("Song")) {
            PublishedNotes.add(note);
        }
        NotesRuntime.addAll(PublishedNotes);
        if (nbt.contains("CustomName", 8)) {
            this.customName = Text.Serializer.fromJson(nbt.getString("CustomName"));
        }
        if (nbt.contains("SongName", 8)) {
            this.songName = Text.Serializer.fromJson(nbt.getString("SongName"));
        }
        if (nbt.contains("SongLength")) {
            this.songLength = nbt.getInt("SongLength");
        }
    }
    public NbtCompound writeNbtAlt(NbtCompound nbt) {
        this.writeNbt(nbt);
        return nbt;
    }
    @Override
    protected void writeNbt(NbtCompound nbt) {
        if (skipReadNbtPass) { skipReadNbtPass = false; }
        super.writeNbt(nbt);
        nbt.putIntArray("Song", PublishedNotes);
        if (this.customName != null) {
            nbt.putString("CustomName", Text.Serializer.toJson(this.customName));
        }
        if (this.songName != null) {
            nbt.putString("SongName", Text.Serializer.toJson(this.songName));
        }
        nbt.putInt("SongLength", songLength);
    }
    public void sync() {
        NotesRuntime.clear();;
        NotesRuntime.addAll(PublishedNotes);
        ticksUntilNextNote = ((MusicBoxBlock)getCachedState().getBlock()).ticksBetweenNotes;
    }

    public void playSoundFromNoteBlock(BlockState state, World world, BlockPos pos, int note, float volume) {
        Instrument instrument = (Instrument)state.get(NoteBlock.INSTRUMENT);
        float f = NoteBlock.getNotePitch(note) * 0.95f;
        world.playSound((PlayerEntity)null, (double)pos.getX() + 0.5, (double)pos.getY() + 0.5, (double)pos.getZ() + 0.5, instrument.getSound(), SoundCategory.RECORDS, volume, f, world.random.nextLong());
    }
    public boolean isCurrentNoteOf(int note_idx) {
        if (!NotesRuntime.isEmpty()) return NotesRuntime.peekFirst() << note_idx == 1;
        return false;
    }
    int fibLast = 1;
    int[] songDefault = new int[]{
            1 << 4,
            1 << 4 | 1 << 1,
            1 << 4 | 1 << 2,
            1 << 4 | 1 << 1,
            1 << 4 | 1 << 2,
            1 << 3 | 1 << 2,
            1 << 4 | 1 << 1,
            1 << 4 | 1 << 2,
            1 << 4 | 1 << 1,
            1 << 4 | 1 << 2,
            1 << 4 | 1 << 1,
            1 << 4 | 1 << 2,
    };
    public static final HashMap<String, Pair<Identifier, Float>> NAME_TO_MUSIC = new HashMap<>();
    static {
//        NAME_TO_MUSIC.put("lilium", new Pair<>(Music_boxes.LILIUM.getId(), 49f));
    }
    protected Text getContainerName() {
        return Text.translatable("container.music_box");
    }
    public boolean hasCustomSong() {
        if (customName == null || songName != null) {return false;}
        for (var key : NAME_TO_MUSIC.keySet()) {
            if (customName.toString().contains(key)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void setStackNbt(ItemStack stack) {
        super.setStackNbt(stack);
        if (customName != null) {
            NbtCompound display;
            if (stack.getOrCreateNbt().contains("display")) {
                display = stack.getOrCreateNbt().getCompound("display");
            } else {
                display = new NbtCompound();
            }
            display.putString("Name", customName.toString());
        }
    }

    public @Nullable String getCustomSongName() {
        if (hasCustomSong()) {
            for (var key : NAME_TO_MUSIC.keySet()) {
                assert customName != null;
                if (customName.toString().contains(key)) {
                    return key;
                }
            }
        }
        return null;
    }
    public void playNote() {
        if (getCachedState().getBlock() instanceof MusicBoxBlock mbb) {
            ticksUntilNextNote--;
            assert world != null;
            if ((world.getBlockState(pos).get(MusicBoxBlock.PLAYING) || world.getBlockState(pos).get(MusicBoxBlock.MANUAL_PLAYING)) && !NotesRuntime.isEmpty()) {
                int note = NotesRuntime.pop();
                if (note == 0) {
                    NotesRuntime.addLast(note);
                    return;
                }
                float volMod = 1;
                int notesPerMod = 0;
                for (int i = 0; i < 32; i++) {
                    if (((note >> i) & 1) > 0) {
                        notesPerMod++;
                    }
                }
                volMod-= notesPerMod / 32f;

                for (int i = 0; i < 32; i++) {
                    if ((note >> i & 1) > 0) {
                        if (mbb.hasValidNoteBlockBelow(Objects.requireNonNull(getWorld(), "Null world- Cannot play note"), getPos())) {
                            playSoundFromNoteBlock(getWorld().getBlockState(getPos().down()), getWorld(), getPos().down(), note - 3, volMod-0.25f);
                        } else {
                            if (world instanceof ServerWorld serverWorld) {
                                float f = NoteBlock.getNotePitch(i - 3);
                                serverWorld.playSound((PlayerEntity) null, (double) pos.getX() + 0.5, (double) pos.getY() + 0.5, (double) pos.getZ() + 0.5, Music_boxes.MUSIC_BOX_CHIME, SoundCategory.BLOCKS, volMod+1.25f, f, world.random.nextLong());
                            }
                        }
                        if (getCachedState().get(Properties.OPEN)) {
                            world.addParticle(ParticleTypes.NOTE, (double) pos.getX() + 0.5, (double) pos.getY() + .2, (double) pos.getZ() + 0.5, (double) note / 32.0, 0.0, 0.0);
                        }
                    }
                }
                NotesRuntime.addLast(note);
            }
        }
    }

    public static <T extends BlockEntity> void tick(World world, BlockPos pos, BlockState blockState, T t) {
        if (t instanceof MusicBoxBlockEntity mbe) {
            if (mbe.ticksUntilNextNote-- > 0 && ((world.getBlockState(pos).get(MusicBoxBlock.PLAYING) || world.getBlockState(pos).get(MusicBoxBlock.MANUAL_PLAYING)))) {
                if (world.random.nextBetween(0,100) > 95 && blockState.get(Properties.OPEN)) {
                    world.addParticle(ParticleTypes.NOTE, (double) pos.getX() + 0.5, (double) pos.getY() + .2, (double) pos.getZ() + 0.5, (double) world.random.nextBetween(0,10) / 32.0, 0.0, 0.0);
                }
            }
            else if ((Music_boxes.MUSIC_BOX_BLOCK.isPlaying(blockState)) && mbe.ticksUntilNextNote <=0 && mbe.hasCustomSong()){
                for (var key : NAME_TO_MUSIC.keySet()) {
                    if (mbe.customName.toString().contains(key) && world instanceof ServerWorld serverWorld) {
                        mbe.ticksUntilNextNote = (int) (20 * NAME_TO_MUSIC.get(key).getRight()) + 1;
                        serverWorld.playSound((PlayerEntity) null, (double) pos.getX() + 0.5, (double) pos.getY() + 0.5, (double) pos.getZ() + 0.5, SoundEvent.of(NAME_TO_MUSIC.get(key).getLeft()), SoundCategory.BLOCKS, 1F, 1, world.random.nextLong());
                        return;
                    }
                }
            }
            if (mbe.ticksUntilNextNote-- <= 0) {
                if (mbe.songNotesLeft <= 0 && !mbe.PublishedNotes.isEmpty()) {
                    mbe.NotesRuntime.clear();
                    mbe.songNotesLeft = mbe.songLength;
                    mbe.NotesRuntime.addAll(mbe.PublishedNotes);
                }
                mbe.playNote();
                mbe.songNotesLeft--;
                world.updateComparators(pos, blockState.getBlock());
                mbe.ticksUntilNextNote = ((MusicBoxBlock)blockState.getBlock()).ticksBetweenNotes;
            }
        }
    }
}
