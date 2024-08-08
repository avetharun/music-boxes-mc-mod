package com.feintha.musicboxes;

import com.feintha.musicboxes.block.MusicBoxBlock;
import com.feintha.musicboxes.block.entity.MusicBoxBlockEntity;
import com.feintha.musicboxes.item.SpindleItem;
import com.feintha.musicboxes.screen.SpindleScreenHandler;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.fabricmc.fabric.api.screenhandler.v1.FabricScreenHandlerFactory;
import net.fabricmc.fabric.impl.itemgroup.FabricItemGroupBuilderImpl;
import net.minecraft.block.Blocks;
import net.minecraft.block.Oxidizable;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.resource.featuretoggle.FeatureFlags;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.List;
import java.util.stream.Stream;

public class Music_boxes implements ModInitializer {



    public static ExtendedScreenHandlerType<SpindleScreenHandler> SPINDLE_SCREEN_HANDLER = Registry.register(Registries.SCREEN_HANDLER, new Identifier("music_boxes", "spindle"), new ExtendedScreenHandlerType<>((syncId, inventory, buf) -> new SpindleScreenHandler(syncId, buf)));
    public static MusicBoxBlock MUSIC_BOX_BLOCK = Registry.register(Registries.BLOCK, new Identifier("music_boxes", "music_box"), new MusicBoxBlock(FabricBlockSettings.create().strength(0.1f).nonOpaque().ticksRandomly(), 4));
    public static MusicBoxBlock EXPOSED_MUSIC_BOX_BLOCK = Registry.register(Registries.BLOCK, new Identifier("music_boxes", "exposed_music_box"), new MusicBoxBlock(FabricBlockSettings.create().strength(0.1f).nonOpaque().ticksRandomly(), 6));
    public static MusicBoxBlock WEATHERED_MUSIC_BOX_BLOCK = Registry.register(Registries.BLOCK, new Identifier("music_boxes", "weathered_music_box"), new MusicBoxBlock(FabricBlockSettings.create().strength(0.1f).nonOpaque().ticksRandomly(), 8));
    public static MusicBoxBlock OXIDIZED_MUSIC_BOX_BLOCK = Registry.register(Registries.BLOCK, new Identifier("music_boxes", "oxidized_music_box"), new MusicBoxBlock(FabricBlockSettings.create().strength(0.1f).nonOpaque().ticksRandomly(), 12));

    static{
    }

    public static final BlockItem MUSIC_BOX_ITEM = Registry.register(Registries.ITEM, new Identifier("music_boxes", "music_box"), new BlockItem(MUSIC_BOX_BLOCK, new Item.Settings().maxCount(1)));
    public static final BlockItem EXPOSED_MUSIC_BOX_ITEM = Registry.register(Registries.ITEM, new Identifier("music_boxes", "exposed_music_box"), new BlockItem(EXPOSED_MUSIC_BOX_BLOCK, new Item.Settings().maxCount(1)));
    public static final BlockItem WEATHERED_MUSIC_BOX_ITEM = Registry.register(Registries.ITEM, new Identifier("music_boxes", "weathered_music_box"), new BlockItem(WEATHERED_MUSIC_BOX_BLOCK, new Item.Settings().maxCount(1)));
    public static final BlockItem OXIDIZED_MUSIC_BOX_ITEM = Registry.register(Registries.ITEM, new Identifier("music_boxes", "oxidized_music_box"), new BlockItem(OXIDIZED_MUSIC_BOX_BLOCK, new Item.Settings().maxCount(1)));
    public static final Item DRUM_SPINDLE = Registry.register(Registries.ITEM, new Identifier("music_boxes", "drum_spindle"), new SpindleItem(new Item.Settings().maxCount(16)));
//    public static final Item SPRING_MOTOR = Registry.register(Registries.ITEM, new Identifier("music_boxes", "spring_motor"), new Item(new Item.Settings()));
    public static final Item MUSIC_BOX_KEY = Registry.register(Registries.ITEM, new Identifier("music_boxes", "music_box_key"), new Item(new Item.Settings()));
    public static BlockEntityType<MusicBoxBlockEntity> MUSIC_BOX_BLOCK_ENTITY_TYPE = Registry.register(Registries.BLOCK_ENTITY_TYPE, new Identifier("music_boxes", "music_box"), FabricBlockEntityTypeBuilder.create(MusicBoxBlockEntity::new, MUSIC_BOX_BLOCK, EXPOSED_MUSIC_BOX_BLOCK, WEATHERED_MUSIC_BOX_BLOCK, OXIDIZED_MUSIC_BOX_BLOCK).build());
    public static final SoundEvent MUSIC_BOX_CHIME = SoundEvent.of(new Identifier("music_boxes:music_boxes.block.music_box.chime"));
    public static final Identifier SET_NOTE_PACKET_ID = new Identifier("music_boxes", "set_note_to");
    @Override
    public void onInitialize() {
        ServerPlayNetworking.registerGlobalReceiver(SET_NOTE_PACKET_ID, (server, player, handler, buf, responseSender) -> {
            if (player.currentScreenHandler instanceof SpindleScreenHandler spindleScreenHandler) {
                int index = buf.readInt();
                if (index < SpindleScreenHandler.DISCARD_ALL_NOTES) {
                    spindleScreenHandler.setProperty(index, buf.readInt());
                }
                spindleScreenHandler.setProperty(SpindleScreenHandler.INFO_DELEGATE_SONG_LENGTH, buf.readInt());
            }
        });
    }

    static {
        Registry.register(Registries.ITEM_GROUP, new Identifier("music_boxes:music_boxes_item_group"), FabricItemGroup.builder().icon(MUSIC_BOX_ITEM::getDefaultStack).entries((displayContext, entries) -> entries.addAll(Stream.of(DRUM_SPINDLE, MUSIC_BOX_KEY, MUSIC_BOX_ITEM,  EXPOSED_MUSIC_BOX_ITEM, WEATHERED_MUSIC_BOX_ITEM, OXIDIZED_MUSIC_BOX_ITEM).map(Item::getDefaultStack).toList())).displayName(Text.translatable("item_group.music_boxes")).build());
    }
}
