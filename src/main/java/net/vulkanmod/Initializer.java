package net.vulkanmod;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.renderer.v1.Renderer;
import net.fabricmc.loader.api.FabricLoader;
import net.vulkanmod.config.Config;
import net.vulkanmod.config.Platform;
import net.vulkanmod.config.video.VideoModeManager;
import net.vulkanmod.render.chunk.build.frapi.VulkanModRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;

public class Initializer implements ClientModInitializer {
	private static final Logger LOGGER = LoggerFactory.getLogger("VulkanMod");

	private static String VERSION;
	public static Config CONFIG;

	@Override
	public void onInitializeClient() {

		VERSION = FabricLoader.getInstance()
				.getModContainer("vulkanmod")
				.orElseThrow()
				.getMetadata()
				.getVersion()
				.getFriendlyString();

		LOGGER.info("== VulkanMod ==");

		Platform.init();
		VideoModeManager.init();

		var configPath = FabricLoader.getInstance()
				.getConfigDir()
				.resolve("vulkanmod_settings.json");

		CONFIG = loadConfig(configPath);

		Renderer.register(VulkanModRenderer.INSTANCE);
	}

	private static Config loadConfig(Path path) {
		Config config = Config.load(path);

		if (config == null) {
			config = new Config();
			config.write();
		}

		return config;
	}

	public static Logger getLogger() {
		return LOGGER;
	}

	public static String getVersion() {
		return VERSION;
	}
}
