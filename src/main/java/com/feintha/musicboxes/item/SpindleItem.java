package com.feintha.musicboxes.item;

import com.feintha.musicboxes.screen.SpindleScreenHandler;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtInt;
import net.minecraft.nbt.NbtIntArray;
import net.minecraft.nbt.NbtLongArray;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.screen.ArrayPropertyDelegate;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.state.property.IntProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class SpindleItem extends Item implements ExtendedScreenHandlerFactory {
    public SpindleItem(Settings settings) {
        super(settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        if (!world.isClient) { user.openHandledScreen(this); }
        return super.use(world, user, hand);
    }

    @Override
    public void writeScreenOpeningData(ServerPlayerEntity player, PacketByteBuf buf) {
        buf.writeItemStack(player.getActiveItem());
    }

    @Override
    public Text getDisplayName() {
        return getName();
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        super.appendTooltip(stack, world, tooltip, context);
    }

    @Nullable
    @Override
    public ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
        ItemStack ref = playerInventory.getStack(playerInventory.selectedSlot).copy();
        return new SpindleScreenHandler(syncId, ref, new PropertyDelegate(){
            @Override
            public int get(int index) {
                if (index < 64) {
                    NbtCompound a = ref.getOrCreateSubNbt("song");
                    var iA = new ArrayList<Integer>(Arrays.stream(a.getIntArray("notes")).boxed().toList());
                    if (iA.size() < 64) {
                        while (iA.size() < 64) {
                            iA.add(0);
                        }
                    }
                    return iA.get(index);
                }
                if (index == SpindleScreenHandler.INFO_DELEGATE_SONG_LENGTH) {
                    return ref.getOrCreateSubNbt("song").contains("length") ? ref.getOrCreateSubNbt("song").getInt("length") : 64;
                }
                return 0;
            }

            @Override
            public void set(int index, int value) {
                if (index < 64) {
                    NbtCompound a = ref.getOrCreateSubNbt("song");
                    var iA = new ArrayList<Integer>(Arrays.stream(a.getIntArray("notes")).boxed().toList());
                    if (iA.size() < 64) {
                        while (iA.size() < 64) {
                            iA.add(0);
                        }
                    }
                    iA.set(index, value);
                    a.putIntArray("notes", iA);
                    ref.setSubNbt("song", a);
                }
                if (index == SpindleScreenHandler.INFO_DELEGATE_SONG_LENGTH) {
                    NbtCompound a = ref.getOrCreateSubNbt("song");
                    a.putInt("length", value);
                }
            }
            @Override
            public int size() {
                return 72;
            }
        });
    }
}
