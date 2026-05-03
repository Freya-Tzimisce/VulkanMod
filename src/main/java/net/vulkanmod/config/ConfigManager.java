package net.vulkanmod.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class ConfigManager {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static Path CONFIG_PATH;
	private static Config CONFIG;

	public static void save(Config config) {

		if (Files.notExists(CONFIG_PATH)) {
			try {
				Files.createFile(CONFIG_PATH);
			} catch (IOException e) {
				throw new RuntimeException(e.getMessage());
			}
		}

		try {
			Files.writeString(CONFIG_PATH, GSON.toJson(config));
		} catch (IOException e) {
			throw new RuntimeException(e.getMessage());
		}
	}

	public static void init(Path path) {
		var config = load(path);

		if (config == null) {
			config = new Config();
			save(config);
		}

		CONFIG = config;
	}

	@Nullable
	public static Config load(Path path) {
		Config config;
		CONFIG_PATH = path;

		if (Files.exists(path)) {
			try (var bufferedReader = Files.newBufferedReader(path)) {
				config = GSON.fromJson(bufferedReader, Config.class);
			} catch (IOException e) {
				throw new RuntimeException(e.getMessage());
			}
		} else {
			config = null;
		}

		return config;
	}

	public static Config getConfig() {
		return CONFIG;
	}
}
