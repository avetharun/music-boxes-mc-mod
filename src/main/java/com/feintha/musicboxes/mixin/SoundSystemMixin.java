package com.feintha.musicboxes.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalFloatRef;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.SoundSystem;
import net.minecraft.server.command.PlaySoundCommand;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import sun.misc.Unsafe;

import java.lang.reflect.Field;

@Mixin(SoundSystem.class)
public abstract class SoundSystemMixin {
    @ModifyArg(method = "getAdjustedPitch", at= @At(value = "INVOKE", target = "Lnet/minecraft/util/math/MathHelper;clamp(FFF)F"), index = 2)
    float modifyPitchMixinMax(float value) {
        return 3f;
    }
    @ModifyArg(method = "getAdjustedPitch", at= @At(value = "INVOKE", target = "Lnet/minecraft/util/math/MathHelper;clamp(FFF)F"), index = 1)
    float modifyPitchMixinMin(float value) {
        return 0.1f;
    }
}
