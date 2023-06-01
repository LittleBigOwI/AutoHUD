package mod.crend.autohud.mixin.gui;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import mod.crend.autohud.AutoHud;
import mod.crend.autohud.component.Component;
import mod.crend.autohud.component.Hud;
import mod.crend.autohud.render.AutoHudRenderer;
import mod.crend.autohud.render.CustomFramebufferRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.texture.StatusEffectSpriteManager;
import net.minecraft.entity.JumpingMount;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.scoreboard.ScoreboardObjective;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

@Mixin(value = InGameHud.class, priority = 800)
public class InGameHudMixin {

    @Inject(method="render", at=@At("HEAD"))
    private void autoHud$preRender(DrawContext context, float tickDelta, CallbackInfo ci) {
        AutoHudRenderer.inRender = true;
        AutoHudRenderer.tickDelta = tickDelta;
    }
    @Inject(method="render", at=@At("RETURN"))
    private void autoHud$postRender(DrawContext context, float tickDelta, CallbackInfo ci) {
        AutoHudRenderer.inRender = false;
    }


    // Hotbar
    @WrapOperation(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/hud/InGameHud;renderHotbar(FLnet/minecraft/client/gui/DrawContext;)V"
            )
    )
    private void autoHud$wrapHotbar(InGameHud instance, float tickDelta, DrawContext context, Operation<Void> original) {
        if (AutoHud.targetHotbar) {
            AutoHudRenderer.preInject(context, Component.Hotbar);
        }
        original.call(instance, tickDelta, context);
        if (AutoHud.targetHotbar) {
            AutoHudRenderer.postInject(context);
        }
    }
    @Inject(method = "renderHotbar", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/DrawContext;getMatrices()Lnet/minecraft/client/util/math/MatrixStack;", ordinal = 0))
    private void autoHud$preHotbar(float tickDelta, DrawContext context, CallbackInfo ci) {
        AutoHudRenderer.injectTransparency();
    }

    // Tooltip
    @WrapOperation(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/hud/InGameHud;renderHeldItemTooltip(Lnet/minecraft/client/gui/DrawContext;)V"
            )
    )
    private void autoHud$wrapTooltip(InGameHud instance, DrawContext context, Operation<Void> original) {
        if (AutoHud.targetHotbar) {
            AutoHudRenderer.preInject(context, Component.Tooltip);
        }
        original.call(instance, context);
        if (AutoHud.targetHotbar) {
            AutoHudRenderer.postInject(context);
        }
    }

    // Hotbar items
    @WrapOperation(
            method = "renderHotbar",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/hud/InGameHud;renderHotbarItem(Lnet/minecraft/client/gui/DrawContext;IIFLnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/item/ItemStack;I)V"
            )
    )
    private void autoHud$transparentHotbarItems(InGameHud instance, DrawContext context, int x, int y, float tickDelta, PlayerEntity player, ItemStack stack, int seed, Operation<Void> original) {
        if (AutoHud.targetHotbar && AutoHud.config.animationFade()) {
            // We need to reset the renderer because otherwise the first item gets drawn with double alpha
            AutoHudRenderer.postInjectFade();
            // Setup custom framebuffer
            CustomFramebufferRenderer.init();
        }

        // Have the original call draw onto the custom framebuffer
        original.call(instance, context, x, y, tickDelta, player, stack, seed);

        if (AutoHud.targetHotbar && AutoHud.config.animationFade()) {
            // Render the contents of the custom framebuffer as a texture with transparency onto the main framebuffer
            AutoHudRenderer.preInjectFade(context, Component.Hotbar, AutoHud.config.getHotbarItemsMaximumFade());
            CustomFramebufferRenderer.draw(context);
            AutoHudRenderer.postInjectFade(context);
        }
    }

    // Experience Bar
    @WrapOperation(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/hud/InGameHud;renderExperienceBar(Lnet/minecraft/client/gui/DrawContext;I)V"
            )
    )
    private void autoHud$wrapExperienceBar(InGameHud instance, DrawContext context, int x, Operation<Void> original) {
        if (AutoHud.targetExperienceBar) {
            AutoHudRenderer.preInject(context, Component.ExperienceBar);
        }
        original.call(instance, context, x);
        if (AutoHud.targetExperienceBar) {
            AutoHudRenderer.postInject(context);
        }
    }

    @ModifyArg(method = "renderExperienceBar", at=@At(value = "INVOKE", target = "Lnet/minecraft/client/gui/DrawContext;drawText(Lnet/minecraft/client/font/TextRenderer;Ljava/lang/String;IIIZ)I"), index = 4)
    private int autoHud$experienceText(int color) {
        if (AutoHudRenderer.inRender) {
            return AutoHudRenderer.modifyArgb(color);
        }
        return color;
    }


    // Status Bars
    @WrapOperation(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/hud/InGameHud;renderStatusBars(Lnet/minecraft/client/gui/DrawContext;)V"
            )
    )
    private void autoHud$wrapStatusBars(InGameHud instance, DrawContext context, Operation<Void> original) {
        if (AutoHud.targetStatusBars) {
            // Armor is the first rendered status bar in the vanilla renderer
            AutoHudRenderer.preInject(context, Component.Armor);
        }
        original.call(instance, context);
        if (AutoHud.targetStatusBars) {
            AutoHudRenderer.postInject(context);
        }
    }

    @Inject(method = "renderStatusBars", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/MinecraftClient;getProfiler()Lnet/minecraft/util/profiler/Profiler;", ordinal = 1))
    private void autoHud$postArmorBar(final DrawContext context, final CallbackInfo ci) {
        if (AutoHud.targetStatusBars) {
            AutoHudRenderer.postInject(context);
            AutoHudRenderer.preInject(context, Component.Health);
        }
    }

    @Inject(method = "renderStatusBars", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/MinecraftClient;getProfiler()Lnet/minecraft/util/profiler/Profiler;", ordinal = 2))
    private void autoHud$postHealthBar(final DrawContext context, final CallbackInfo ci) {
        if (AutoHud.targetStatusBars) {
            AutoHudRenderer.postInject(context);
            AutoHudRenderer.preInject(context, Component.Hunger);
        }
    }

    @Inject(method = "renderStatusBars", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/MinecraftClient;getProfiler()Lnet/minecraft/util/profiler/Profiler;", ordinal = 3))
    private void autoHud$postFoodBar(final DrawContext context, final CallbackInfo ci) {
        if (AutoHud.targetStatusBars) {
            AutoHudRenderer.postInject(context);
            AutoHudRenderer.preInject(context, Component.Air);
        }
    }

    // Mount Health
    @WrapOperation(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/hud/InGameHud;renderMountHealth(Lnet/minecraft/client/gui/DrawContext;)V"
            )
    )
    private void autoHud$wrapMountHealth(InGameHud instance, DrawContext context, Operation<Void> original) {
        if (AutoHud.targetStatusBars) {
            AutoHudRenderer.preInject(context, Component.MountHealth);
        }
        original.call(instance, context);
        if (AutoHud.targetStatusBars) {
            AutoHudRenderer.postInject(context);
        }
    }

    // Mount Jump Bar
    @WrapOperation(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/hud/InGameHud;renderMountJumpBar(Lnet/minecraft/entity/JumpingMount;Lnet/minecraft/client/gui/DrawContext;I)V"
            )
    )
    private void autoHud$wrapMountJumpBar(InGameHud instance, JumpingMount mount, DrawContext context, int x, Operation<Void> original) {
        if (AutoHud.targetStatusBars) {
            AutoHudRenderer.preInject(context, Component.MountJumpBar);
        }
        original.call(instance, mount, context, x);
        if (AutoHud.targetStatusBars) {
            AutoHudRenderer.postInject(context);
        }
    }

    // Scoreboard
    @WrapOperation(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/hud/InGameHud;renderScoreboardSidebar(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/scoreboard/ScoreboardObjective;)V"
            )
    )
    private void autoHud$wrapScoreboardSidebar(InGameHud instance, DrawContext context, ScoreboardObjective objective, Operation<Void> original) {
        if (AutoHud.targetScoreboard) {
            AutoHudRenderer.preInject(context, Component.Scoreboard);
        }
        original.call(instance, context, objective);
        if (AutoHud.targetScoreboard) {
            AutoHudRenderer.postInject(context);
        }
    }

    @ModifyArg(method = "renderScoreboardSidebar", at=@At(value = "INVOKE", target = "Lnet/minecraft/client/gui/DrawContext;drawText(Lnet/minecraft/client/font/TextRenderer;Ljava/lang/String;IIIZ)I"), index = 4)
    private int autoHud$scoreboardSidebarString(int color) {
        if (AutoHudRenderer.inRender) {
            return AutoHudRenderer.getArgb() | 0xFFFFFF;
        }
        return color;
    }
    @ModifyArg(method = "renderScoreboardSidebar", at=@At(value = "INVOKE", target = "Lnet/minecraft/client/gui/DrawContext;drawText(Lnet/minecraft/client/font/TextRenderer;Lnet/minecraft/text/Text;IIIZ)I"), index = 4)
    private int autoHud$scoreboardSidebarText(int color) {
        if (AutoHudRenderer.inRender) {
            return AutoHudRenderer.getArgb() | 0xFFFFFF;
        }
        return color;
    }
    @ModifyArg(method = "renderScoreboardSidebar", at=@At(value = "INVOKE", target = "Lnet/minecraft/client/gui/DrawContext;fill(IIIII)V"), index=4)
    private int autoHud$scoreboardSidebarFill(int color) {
        if (AutoHudRenderer.inRender) {
            return AutoHudRenderer.modifyArgb(color);
        }
        return color;
    }


    // Status Effects
    @Inject(method = "renderStatusEffectOverlay", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/effect/StatusEffectInstance;getEffectType()Lnet/minecraft/entity/effect/StatusEffect;", ordinal = 0), locals = LocalCapture.CAPTURE_FAILSOFT)
    private void autoHud$preEffect(DrawContext context, CallbackInfo ci, Collection<StatusEffectInstance> collection, int i, int j, StatusEffectSpriteManager statusEffectSpriteManager, List<Runnable> list, Iterator<StatusEffectInstance> var7, StatusEffectInstance statusEffectInstance) {
        if (AutoHud.targetStatusEffects && Hud.shouldShowIcon(statusEffectInstance)) {
            AutoHudRenderer.preInject(context, Component.get(statusEffectInstance.getEffectType()));
        }
    }
    @Inject(method = "renderStatusEffectOverlay", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/texture/StatusEffectSpriteManager;getSprite(Lnet/minecraft/entity/effect/StatusEffect;)Lnet/minecraft/client/texture/Sprite;"))
    private void autoHud$postEffect(DrawContext context, CallbackInfo ci) {
        if (AutoHud.targetStatusEffects) {
            AutoHudRenderer.postInject(context);
        }
    }
    @Inject(method = "method_18620", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/DrawContext;drawSprite(IIIIILnet/minecraft/client/texture/Sprite;)V"))
    private static void autoHud$preSprite(DrawContext context, float f, int i, int j, Sprite sprite, CallbackInfo ci) {
        if (AutoHud.targetStatusEffects) {
            Component component = Component.findBySprite(sprite);
            if (component != null) {
                AutoHudRenderer.preInject(context, component);
            } else {
                context.getMatrices().push();
            }
        }
    }
    @Inject(method = "method_18620", at = @At(value = "RETURN"))
    private static void autoHud$postSprite(DrawContext drawContext, float f, int i, int j, Sprite sprite, CallbackInfo ci) {
        if (AutoHud.targetStatusEffects) {
            AutoHudRenderer.postInject(drawContext);
        }
    }

    @Redirect(method = "renderStatusEffectOverlay", at = @At(value = "INVOKE", target="Lnet/minecraft/entity/effect/StatusEffectInstance;shouldShowIcon()Z"))
    private boolean autoHud$shouldShowIconProxy(StatusEffectInstance instance) {
        return Hud.shouldShowIcon(instance);
    }

    @Inject(method = "tick()V", at = @At(value = "TAIL"))
    private void autoHud$tickAutoHud(CallbackInfo ci) {
        Hud.tick();
    }

}
