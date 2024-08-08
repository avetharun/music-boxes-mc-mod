package com.feintha.musicboxes.client.renderer.screen;

import com.feintha.musicboxes.Music_boxes;
import com.feintha.musicboxes.screen.SpindleScreenHandler;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.block.NoteBlock;
import net.minecraft.block.entity.DropperBlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.StonecutterScreen;
import net.minecraft.client.input.KeyCodes;
import net.minecraft.client.input.KeyboardInput;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.client.sound.SoundSystem;
import net.minecraft.client.toast.SystemToast;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.s2c.play.ScreenHandlerPropertyUpdateS2CPacket;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

import java.util.ArrayList;

public class SpindleScreen extends HandledScreen<SpindleScreenHandler> {
    public static Identifier BACKGROUND = new Identifier("music_boxes:textures/gui/spindle.png");
    public SpindleScreen(SpindleScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
    }

    @Override
    public Text getTitle() {
        assert MinecraftClient.getInstance().player != null;
        return MinecraftClient.getInstance().player.getMainHandStack().getName();
    }

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        titleY = -99999;
        int x = (width - backgroundWidth) / 2 - 28;
        int y = (height - backgroundHeight) / 2 - 32;
        context.drawTexture(BACKGROUND, x, y, 0, 0, 228, 222);
        int bgx = (width - backgroundWidth) / 2 - 28;
        int bgy = (height - backgroundHeight) / 2 - 32;
        boolean hasDrawnFlagOnThisPage = false;
        int song_len = handler.delegate.get(SpindleScreenHandler.INFO_DELEGATE_SONG_LENGTH);
        for (int bX = 0; bX < 11; bX++) {
            int offsetX = scroll_x;
            String idx = String.valueOf(bX+offsetX);
            context.drawText(MinecraftClient.getInstance().textRenderer, Text.literal(idx), bgx + (18 - (MinecraftClient.getInstance().textRenderer.getWidth(idx)/2)) + (bX * 18), bgy + 8, 0x0f0f0f, false);
            if (offsetX + bX == song_len) {
                context.drawTexture(BACKGROUND, 8 + bgx + 16 + (bX * 18), bgy + 8, 243, 1, 8, 6);
                hasDrawnFlagOnThisPage = true;
            }
            for (int bY = 0; bY < 10; bY++) {
                int offsetY = scroll_y;
                if (((handler.delegate.get(bX + offsetX) >> bY + offsetY) & 1) > 0) {
                    context.drawTexture(BACKGROUND, 8 + bgx + (bX * 18), 18 +bgy + (bY * 18), 230, 18, 16, 16);
                } else {
                    context.drawTexture(BACKGROUND, 8 + bgx + (bX * 18), 18 +bgy + (bY * 18), 230, 36, 16, 16);
                }
                if (offsetX + bX > song_len) {
                    int _x = 8 + bgx + (bX * 18);
                    int _y = 18 +bgy + (bY * 18);
                    context.fill(_x, _y, _x+16, _y + 16, 0x40ff0000);
                }
            }
        }
        if (!hasDrawnFlagOnThisPage) {
            if (scroll_x > song_len) {
                context.drawTexture(BACKGROUND, bgx + 217, bgy + 8, 243, 7, 5, 8);
                // Left arrow
            } else {
                context.drawTexture(BACKGROUND, bgx + 217, bgy + 8, 248, 7, 5, 8);
                // Right arrow
            }
            context.drawTexture(BACKGROUND, bgx + 223, bgy + 8, 243, 1, 8, 6);
        }
//        System.out.println(focusedSlot);
    }

    @Override
    protected void drawMouseoverTooltip(DrawContext context, int x, int y) {
        super.drawMouseoverTooltip(context, x, y);
        if (focusedSlot == null) return;
        context.drawTooltip(MinecraftClient.getInstance().textRenderer, Text.of(INT_TO_NOTE_MAP.get(getHoveredSlotColumn() + scroll_y)), x, y);
    }

    int getHoveredSlotRow() {
        if (focusedSlot == null) {return -999;}
        int rows = 10;
        return (focusedSlot.id / (rows));
    }
    int getHoveredSlotColumn() {
        if (focusedSlot == null) {return -999;}
        int rows = 10;
        return focusedSlot.id % rows;
    }
    boolean horizontal_scroll = false;
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 340) {
            horizontal_scroll = true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 340) {
            horizontal_scroll = false;
        }
        return super.keyReleased(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        if (amount > 0) {
            if (horizontal_scroll) {
                scroll_x = MathHelper.clamp(scroll_x-1, 0, 53);
//                handler.delegate.set(SpindleScreenHandler.INFO_DELEGATE_SCROLL_X, MathHelper.clamp(handler.delegate.get(SpindleScreenHandler.INFO_DELEGATE_SCROLL_X)-1, 0, 53));
            } else
                scroll_y = MathHelper.clamp(scroll_y-1, 0, 21);
//                handler.delegate.set(SpindleScreenHandler.INFO_DELEGATE_SCROLL_Y, MathHelper.clamp(handler.delegate.get(SpindleScreenHandler.INFO_DELEGATE_SCROLL_Y)-1, 0, 21));
        } else {
            if (horizontal_scroll) {
                scroll_x = MathHelper.clamp(scroll_x+1, 0, 53);
//                handler.delegate.set(SpindleScreenHandler.INFO_DELEGATE_SCROLL_X, MathHelper.clamp(handler.delegate.get(SpindleScreenHandler.INFO_DELEGATE_SCROLL_X)+1, 0, 53));
            } else
                scroll_y = MathHelper.clamp(scroll_y+1, 0, 21);
//                handler.delegate.set(SpindleScreenHandler.INFO_DELEGATE_SCROLL_Y, MathHelper.clamp(handler.delegate.get(SpindleScreenHandler.INFO_DELEGATE_SCROLL_Y)+1, 0, 21));
        }
        return super.mouseScrolled(mouseX, mouseY, amount);
    }

    int scrollAmountX = 0;
    int scrollAmountY = 0;
    int columnsScrollableX = 21;
    int columnsScrollableY = 22;
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        int bgx = (width - backgroundWidth) / 2 - 28;
        int bgy = (height - backgroundHeight) / 2 - 32;
        int ofX = MathHelper.lerp((float) (scroll_x + 1) / 54f, 16, 161);
        int ofY = MathHelper.lerp((float) (scroll_y + 1) / 22f, -8, 161);
        context.drawTexture(BACKGROUND, bgx + 8 + ofX, bgy + 198, 1, 221, 17, 14);
        context.drawTexture(BACKGROUND, bgx + 207, bgy + 18 + ofY, 229, 0, 14, 17);
        drawMouseoverTooltip(context, mouseX, mouseY);
    }

    @Override
    protected boolean isClickOutsideBounds(double mouseX, double mouseY, int left, int top, int button) {
        return false;
    }

    static final ArrayList<String> INT_TO_NOTE_MAP = new ArrayList<>();
    int scroll_x, scroll_y = 0;
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (focusedSlot != null) {
            var slotIndex = focusedSlot.id;
            int columns = 11;
            int rows = 10;
            int rowIndex = getHoveredSlotRow();
            int colIndex = getHoveredSlotColumn();
            int x = rowIndex + scroll_x;
            int y = colIndex + scroll_y;
            if (slotIndex < SpindleScreenHandler.DISCARD_ALL_NOTES) {
                int note_existing = handler.delegate.get(x);
                int song_len = handler.delegate.get(SpindleScreenHandler.INFO_DELEGATE_SONG_LENGTH);
                if (button == 0) {
                    note_existing = SpindleScreenHandler.setBit(note_existing, y, !SpindleScreenHandler.getBit(note_existing, y));

                    assert MinecraftClient.getInstance().player != null;
                    MinecraftClient.getInstance().getSoundManager().play(PositionedSoundInstance.master(Music_boxes.MUSIC_BOX_CHIME, NoteBlock.getNotePitch(y-3), 1));
                }
                if (button == 1) song_len = x;
                PacketByteBuf buf = PacketByteBufs.create();
                buf.writeInt(x).writeInt(note_existing);
                buf.writeInt(song_len);
                ClientPlayNetworking.send(Music_boxes.SET_NOTE_PACKET_ID, buf);
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

//
//    @Override
//    public void close() {
//        super.close();
//        MinecraftClient.getInstance().getToastManager().add(new SystemToast(SystemToast.Type.PERIODIC_NOTIFICATION, Text.of("Spindle Modified"), Text.of("Spindle modified, add it to a music box to play!")));
//    }

    @Override
    protected void init() {
        super.init();
        titleY = -26;
        titleX = -22;
        playerInventoryTitleY = -99999;
    }
    static {

        String[] baseNotes = {"F#", "G", "G#", "A", "A#", "B", "C", "D", "E"};
        int totalPitches = 32;

        for (int i = 0; i < totalPitches; i++) {
            int octave = i / baseNotes.length; // Calculate the octave
            int noteIndex = i % baseNotes.length; // Calculate the note index
            String octaveStr = switch (octave) {
                case 1 -> "¹";
                case 2 -> "²";
                case 3 -> "³";
                default -> "";
            };
            String noteValue = baseNotes[noteIndex] + octaveStr;
            INT_TO_NOTE_MAP.add(noteValue);
        }
    }
}
