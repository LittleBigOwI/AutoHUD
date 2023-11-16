package mod.crend.autohud.forge;

import com.llamalad7.mixinextras.MixinExtrasBootstrap;
import mod.crend.autohud.AutoHud;
import mod.crend.autohud.ModKeyBindings;
import mod.crend.autohud.api.AutoHudApi;
import mod.crend.autohud.config.ConfigHandler;
import mod.crend.autohud.render.ChatMessageIndicator;
import mod.crend.yaclx.forge.ConfigScreen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.InterModComms;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.InterModProcessEvent;

@Mod.EventBusSubscriber(modid = AutoHud.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class AutoHudModEvents {
	public static final String REGISTER_API = "register_api";

	@SubscribeEvent
	static void onClientSetup(FMLClientSetupEvent event) {
		MixinExtrasBootstrap.init();
		AutoHud.init();
		ConfigScreen.register(ConfigHandler.CONFIG_STORE);
		MinecraftForge.EVENT_BUS.register(new AutoHudGui());
	}

	@SubscribeEvent
	static void onInterModProcess(InterModProcessEvent event) {
		InterModComms.getMessages(AutoHud.MOD_ID, REGISTER_API::equals)
				.map(msg -> (AutoHudApi) msg.messageSupplier().get())
				.forEach(AutoHud::addApi);
	}

	@SubscribeEvent
	static void onKeyMappingsRegister(RegisterKeyMappingsEvent event) {
		ModKeyBindings.ALL.forEach(event::register);
	}

	@SubscribeEvent
	static void onRegisterOverlaysEvent(RegisterGuiOverlaysEvent event) {
		event.registerAboveAll("autohud_new_chat_message_indicator", (forgeGui, context, f, i, j) -> ChatMessageIndicator.render(context));
	}

}
