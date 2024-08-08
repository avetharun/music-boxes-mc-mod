package com.feintha.musicboxes.client;

import com.feintha.musicboxes.Music_boxes;
import com.feintha.musicboxes.client.renderer.MusicBoxBlockEntityRenderer;
import com.feintha.musicboxes.client.renderer.screen.SpindleScreen;
import com.feintha.musicboxes.screen.SpindleScreenHandler;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry;
import net.fabricmc.fabric.api.screenhandler.v1.ScreenHandlerRegistry;
import net.minecraft.client.gui.screen.ingame.HandledScreens;
import net.minecraft.util.Identifier;

public class Music_boxesClient implements ClientModInitializer {
    @SuppressWarnings("deprecation")
    @Override
    public void onInitializeClient() {
        BlockEntityRendererRegistry.register(Music_boxes.MUSIC_BOX_BLOCK_ENTITY_TYPE, ctx -> new MusicBoxBlockEntityRenderer());
        HandledScreens.register(Music_boxes.SPINDLE_SCREEN_HANDLER, SpindleScreen::new);
    }
}
