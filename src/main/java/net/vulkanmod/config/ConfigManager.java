package net.vulkanmod.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;

public class ConfigManager {
	private static Path CONFIG_PATH;
	private static final Gson GSON = new GsonBuilder()
			.setPrettyPrinting()
			.excludeFieldsWithModifiers(Modifier.PRIVATE)
			.create();

	public static void save(Config config) {

		if (!Files.exists(CONFIG_PATH)) {
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

	public static Config init(Path path) {
		var config = load(path);

		if (config == null) {
			config = new Config();
			save(config);
		}

		return config;
	}

	public static Config load(Path path) {
		Config config;
		CONFIG_PATH = path;

		if (Files.exists(path)) {
			try (var fileReader = new FileReader(path.toFile())) {
				config = GSON.fromJson(fileReader, Config.class);
			} catch (IOException e) {
				throw new RuntimeException(e.getMessage());
			}
		} else {
			config = null;
		}

		return config;
	}
}
