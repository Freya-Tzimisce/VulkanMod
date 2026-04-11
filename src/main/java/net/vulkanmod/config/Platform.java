package net.vulkanmod.config;

import net.vulkanmod.Initializer;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import oshi.SystemInfo;

import static org.lwjgl.glfw.GLFW.GLFW_ANY_PLATFORM;
import static org.lwjgl.glfw.GLFW.GLFW_PLATFORM;
import static org.lwjgl.glfw.GLFW.GLFW_PLATFORM_COCOA;
import static org.lwjgl.glfw.GLFW.GLFW_PLATFORM_WAYLAND;
import static org.lwjgl.glfw.GLFW.GLFW_PLATFORM_WIN32;
import static org.lwjgl.glfw.GLFW.GLFW_PLATFORM_X11;

public abstract class Platform {
	private static final Logger LOGGER = Initializer.getLogger();
    private static final int GLFW_CURRENT_PLATFORM = getCurrentPlatform();

    public static void init() {
        GLFW.glfwInitHint(GLFW_PLATFORM, GLFW_CURRENT_PLATFORM);
        LOGGER.info("Selecting Platform: {}", getPlatfromString(GLFW_CURRENT_PLATFORM));
        LOGGER.info("GLFW: {}", GLFW.glfwGetVersionString());
        GLFW.glfwInit();
    }

    // Actually detect the currently active Display Server (if both Wayland and X11 are present on the system and/or GLFW is compiled to support both)
    private static int getCurrentWindowManager() {
        // Return Null platform if not on Unix-like (i.e. no X11 or Wayland)
		String desktopSession = System.getenv("DESKTOP_SESSION");
        String xdgSessionType = System.getenv("XDG_SESSION_TYPE");
        if ("wayland".equals(desktopSession) || "wayland".equals(xdgSessionType)) {
			return GLFW_PLATFORM_WAYLAND; // Wayland
		} else if ("x11".equals(desktopSession) || "x11".equals(xdgSessionType)) {
			return GLFW_PLATFORM_X11; // X11
		} else {
			return GLFW_ANY_PLATFORM; // Either unknown Platform or Display Server
		}
    }

    private static int getCurrentPlatform() {
		var currentPlatform = SystemInfo.getCurrentPlatform();
        return switch (currentPlatform) {
	        case MACOS -> GLFW_PLATFORM_COCOA;
			case WINDOWS -> GLFW_PLATFORM_WIN32;
	        case LINUX, SOLARIS, FREEBSD, OPENBSD, AIX, GNU, KFREEBSD, NETBSD -> getCurrentWindowManager();
			case WINDOWSCE, ANDROID -> GLFW_ANY_PLATFORM;
			case UNKNOWN -> getCurrentWindowManager(); // Unknown platform
        };
    }

    private static String getPlatfromString(int platfrom) {
        return switch (platfrom) {
            case GLFW_ANY_PLATFORM -> "UNKNOWN";
            case GLFW_PLATFORM_WIN32 -> "WIN32";
            case GLFW_PLATFORM_COCOA -> "MACOS";
            case GLFW_PLATFORM_WAYLAND -> "WAYLAND";
            case GLFW_PLATFORM_X11 -> "X11";
            default -> throw new IllegalStateException("Unexpected value: " + platfrom);
        };
    }

    public static int getGlfwCurrentPlatform() {
        return GLFW_CURRENT_PLATFORM;
    }

    //Allows platform specific checks to be handled
	public static boolean isOthers() {
		return GLFW_CURRENT_PLATFORM == GLFW_ANY_PLATFORM;
	}

	public static boolean isWindows() {
        return GLFW_CURRENT_PLATFORM == GLFW_PLATFORM_WIN32;
    }

    public static boolean isMacOS() {
        return GLFW_CURRENT_PLATFORM == GLFW_PLATFORM_COCOA;
    }

    public static boolean isWayLand() {
        return GLFW_CURRENT_PLATFORM == GLFW_PLATFORM_WAYLAND;
    }

    public static boolean isX11() {
        return GLFW_CURRENT_PLATFORM == GLFW_PLATFORM_X11;
    }
}
