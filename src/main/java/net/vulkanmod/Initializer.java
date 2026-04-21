package net.vulkanmod;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.renderer.v1.Renderer;
import net.fabricmc.loader.api.FabricLoader;
import net.vulkanmod.config.ConfigManager;
import net.vulkanmod.config.Platform;
import net.vulkanmod.config.video.VideoModeManager;
import net.vulkanmod.render.chunk.build.frapi.VulkanModRenderer;
import net.vulkanmod.util.LogUtil;
import org.slf4j.Logger;

public class Initializer implements ClientModInitializer {
	private static final Logger LOGGER = LogUtil.getLogger();

	private static final String MOD_ID = "vulkanmod";
	private static final String MOD_NAME = "VulkanMod";

	private static String VERSION;

	@Override
	public void onInitializeClient() {

		VERSION = FabricLoader.getInstance()
				.getModContainer(MOD_ID)
				.orElseThrow()
				.getMetadata()
				.getVersion()
				.getFriendlyString();

		LOGGER.info("== {} ==", MOD_NAME);

		Platform.init();
		VideoModeManager.init();

		var configPath = FabricLoader.getInstance()
				.getConfigDir()
				.resolve("vulkanmod_settings.json");

		ConfigManager.init(configPath);

		Renderer.register(VulkanModRenderer.INSTANCE);
	}

	public static String getVersion() {
		return VERSION;
	}
}
