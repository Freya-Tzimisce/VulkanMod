package net.vulkanmod.config.video;

import com.mojang.blaze3d.platform.VideoMode;
import net.vulkanmod.config.ConfigManager;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public abstract class VideoModeManager {
	private static VideoMode osVideoMode;
	private static VideoModeSet[] videoModeSets;

	@Deprecated(forRemoval = true)
	public static VideoMode selectedVideoMode;

	public static void init() {
		long monitor = GLFW.glfwGetPrimaryMonitor();
		osVideoMode = getCurrentVideoMode(monitor);
		videoModeSets = populateVideoResolutions(monitor);
	}

	@Deprecated(forRemoval = true)
	public static void applySelectedVideoMode() {
		ConfigManager.getConfig().videoMode = selectedVideoMode;
	}

	@Deprecated(forRemoval = true)
	public static VideoModeSet[] getVideoResolutions() {
		return videoModeSets;
	}

	public static VideoModeSet getFirstAvailable() {
		return videoModeSets[videoModeSets.length - 1];
	}

	public static VideoMode getOsVideoMode() {
		return osVideoMode;
	}

	public static VideoMode getCurrentVideoMode(long monitor){
		var vidMode = GLFW.glfwGetVideoMode(monitor);

		if (vidMode == null) {
			throw new NullPointerException("Unable to get current video mode");
		}

		return new VideoMode(vidMode.width(), vidMode.height(), vidMode.redBits(), vidMode.greenBits(), vidMode.blueBits(), vidMode.refreshRate());
	}

	public static VideoModeSet[] populateVideoResolutions(long monitor) {
		var buffer = GLFW.glfwGetVideoModes(monitor);

		List<VideoModeSet> videoModeSets = new ArrayList<>();

		int widthx = 0, heightx = 0, redBitsx = 0, greenBitsx = 0, blueBitsx = 0;
		VideoModeSet videoModeSet = null;

		for (int i = 0; i < buffer.limit(); i++) {
			buffer.position(i);
			int width = buffer.width();
			int height = buffer.height();
			int redBits = buffer.redBits();
			int greenBits = buffer.greenBits();
			int blueBits = buffer.blueBits();
			int refreshRate = buffer.refreshRate();

			if (widthx != width || heightx != height || redBitsx != redBits || greenBitsx != greenBits || blueBitsx != blueBits) {
				widthx = width;
				heightx = height;
				redBitsx = redBits;
				greenBitsx = greenBits;
				blueBitsx = blueBits;

				videoModeSet = new VideoModeSet(widthx, heightx, redBitsx, greenBitsx, blueBitsx);
				videoModeSets.add(videoModeSet);
			}

			videoModeSet.addRefreshRate(refreshRate);
		}

		VideoModeSet[] arr = new VideoModeSet[videoModeSets.size()];
		videoModeSets.toArray(arr);

		return arr;
	}

	public static VideoModeSet getFromVideoMode(VideoMode videoMode) {
		for (var set : videoModeSets) {
			if (set.width == videoMode.getWidth() && set.height == videoMode.getHeight()) {
				return set;
			}
		}

		return null;
	}
}
