package com.feintha.musicboxes.screen;

import com.feintha.musicboxes.Music_boxes;
import com.feintha.musicboxes.client.renderer.screen.SpindleScreen;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.minecraft.block.NoteBlock;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtIntArray;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.screen.ArrayPropertyDelegate;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class SpindleScreenHandler extends ScreenHandler {
    ItemStack stack;
    public PropertyDelegate delegate;
    public int getScrollX() {return delegate.get(INFO_DELEGATE_SCROLL_X);}
    public int getScrollY() {return delegate.get(INFO_DELEGATE_SCROLL_Y);}
    private static class UnmodifiableSlot extends Slot{
        public UnmodifiableSlot(Inventory inventory, int index, int x, int y) { super(inventory, index, x, y); }

        @Override
        public boolean canTakeItems(PlayerEntity playerEntity) { return false; }

        @Override
        public boolean canInsert(ItemStack stack) { return false; }

        @Override
        public boolean isEnabled() {
            return true;
        }
    }
    public final static int DISCARD_ALL_NOTES = 110;
    final static int SCROLL_H_FW = 111;
    final static int SCROLL_H_BK = 112;
    public SpindleScreenHandler(int syncId, ItemStack spindleItem, PropertyDelegate propertyDelegate) {
        super(Music_boxes.SPINDLE_SCREEN_HANDLER, syncId);
        this.stack = spindleItem;
        this.delegate = propertyDelegate;
        this.addProperties(this.delegate);
        SimpleInventory inv = new SimpleInventory(120);
        int[] ia = spindleItem.getOrCreateSubNbt("song").getIntArray("notes");
        int ln = spindleItem.getOrCreateSubNbt("song").getInt("length");
        this.delegate.set(INFO_DELEGATE_SONG_LENGTH, ln);
        /*for (int i = 0; i < 64; i++) {
            this.delegate.set(i, ia[i]);
        }*/
        for (int x = 0; x < 11; x++) {
            for (int y = 0; y < 10; y++) {
                this.addSlot(new UnmodifiableSlot(inv, x+y*10, (x*18) - 20, (y*18) - 14));
            }
        }
        this.addSlot(new UnmodifiableSlot(inv, DISCARD_ALL_NOTES, 11 * 18 - 20, 10 * 18 - 14));
        this.addSlot(new UnmodifiableSlot(inv, SCROLL_H_FW, 10 * 18 - 20, 10 * 18 - 14));
        this.addSlot(new UnmodifiableSlot(inv, SCROLL_H_BK,  -20, 10 * 18 - 14));

    }

    public SpindleScreenHandler(int syncId, PacketByteBuf buf) {
        this(syncId, buf.readItemStack(), new ArrayPropertyDelegate(72));
    }
    @Override
    public ItemStack quickMove(PlayerEntity player, int slot) {
        return null;
    }
    public static float getNotePitch(int note) {
        return (float)Math.pow(2.0, (double)(note - 32) / 32.0);
    }
    public static final int INFO_DELEGATE_SCROLL_X = 64;
    public static final int INFO_DELEGATE_SCROLL_Y = 64+1;
    public static final int INFO_DELEGATE_SONG_LENGTH = 64+2;
    @Override
    public void onSlotClick(int slotIndex, int button, SlotActionType actionType, PlayerEntity player) {
        super.onSlotClick(slotIndex, button, actionType, player);
        int rows = 10;

        int rowIndex = (slotIndex % (rows));
        int colIndex = (slotIndex / (rows));
//        System.out.println(colIndex + getScrollX());
        if (slotIndex >= 0 && slotIndex < DISCARD_ALL_NOTES) {
            if (button == 0) {
                if (player.getEntityWorld().isClient) {
                }
            } else if (button == 1){
                if (player.getEntityWorld().isClient) { player.playSound(SoundEvents.UI_TOAST_OUT, 1, 1); }
            }
        }
        if (button == 0 && slotIndex == DISCARD_ALL_NOTES) {
            if (player.getEntityWorld().isClient) { player.playSound(SoundEvents.UI_BUTTON_CLICK.value(), 1, 1); }
            for (int i = 0; i < 64; i++) { delegate.set(i, 0); }
            delegate.set(INFO_DELEGATE_SONG_LENGTH, 64);
            this.updateToClient();
        }
        if (button == 0 && (slotIndex == SCROLL_H_FW)) {
            delegate.set(INFO_DELEGATE_SCROLL_X, MathHelper.clamp(delegate.get(INFO_DELEGATE_SCROLL_X)+1, 0, 53));
        }
        if (button == 0 && (slotIndex == SCROLL_H_BK)) {
            delegate.set(INFO_DELEGATE_SCROLL_X, MathHelper.clamp(delegate.get(INFO_DELEGATE_SCROLL_X)-1, 0, 53));
        }
    }

    public static int setBit(int num, int index, boolean bitIsOne) {
        if (bitIsOne) {
            return num | (1 << index);
        } else {
            return num & ~(1 << index);
        }
    }
    public static boolean getBit(int num, int index) {
        return ((num >> index) & 1) == 1;
    }
    @Override
    public void onClosed(PlayerEntity player) {
        super.onClosed(player);
        if (player.getStackInHand(Hand.MAIN_HAND).isOf(Music_boxes.DRUM_SPINDLE)) { player.getStackInHand(Hand.MAIN_HAND).setSubNbt("song", stack.getSubNbt("song")); }
        else if (player.getStackInHand(Hand.OFF_HAND).isOf(Music_boxes.DRUM_SPINDLE)) { player.getStackInHand(Hand.OFF_HAND).setSubNbt("song", stack.getSubNbt("song")); }
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return player.getStackInHand(Hand.MAIN_HAND).isOf(Music_boxes.DRUM_SPINDLE) || player.getStackInHand(Hand.OFF_HAND).isOf(Music_boxes.DRUM_SPINDLE);
    }
}
