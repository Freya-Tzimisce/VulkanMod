package net.vulkanmod.render.engine;

import com.google.common.collect.EvictingQueue;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.mojang.blaze3d.platform.DebugMemoryUntracker;
import com.mojang.blaze3d.platform.GLX;
import net.vulkanmod.util.LogUtil;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.opengl.ARBDebugOutput;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GLCapabilities;
import org.lwjgl.opengl.GLDebugMessageARBCallback;
import org.lwjgl.opengl.GLDebugMessageCallback;
import org.lwjgl.opengl.KHRDebug;
import org.slf4j.Logger;

import java.util.List;
import java.util.Queue;
import java.util.Set;

public class VkDebug {
	private static final Logger LOGGER = LogUtil.getLogger();
	private static final int CIRCULAR_LOG_SIZE = 10;
	private final Queue<LogEntry> MESSAGE_BUFFER = EvictingQueue.create(10);
	@Nullable
	private volatile VkDebug.LogEntry lastEntry;
	private static final List<Integer> DEBUG_LEVELS = ImmutableList.of(37190, 37191, 37192, 33387);
	private static final List<Integer> DEBUG_LEVELS_ARB = ImmutableList.of(37190, 37191, 37192);

	private static String printUnknownToken(int i) {
		return "Unknown (0x" + Integer.toHexString(i).toUpperCase() + ")";
	}

	public static String sourceToString(int i) {
		return switch (i) {
			case 33350 -> "API";
			case 33351 -> "WINDOW SYSTEM";
			case 33352 -> "SHADER COMPILER";
			case 33353 -> "THIRD PARTY";
			case 33354 -> "APPLICATION";
			case 33355 -> "OTHER";
			default -> printUnknownToken(i);
		};
	}

	public static String typeToString(int i) {
		return switch (i) {
			case VkConst.VK_DEBUG_UTILS_MESSAGE_TYPE_GENERAL_BIT_EXT -> "GENERAL";
			case VkConst.VK_DEBUG_UTILS_MESSAGE_TYPE_VALIDATION_BIT_EXT -> "VALIDATION";
			case VkConst.VK_DEBUG_UTILS_MESSAGE_TYPE_PERFORMANCE_BIT_EXT -> "PERFORMANCE";
			case VkConst.VK_DEBUG_UTILS_MESSAGE_TYPE_DEVICE_ADDRESS_BINDING_BIT_EXT -> "DEVICE ADDRESS BINDING";
			default -> printUnknownToken(i);
		};
	}

	public static String severityToString(int i) {
		return switch (i) {
			case VkConst.VK_DEBUG_UTILS_MESSAGE_SEVERITY_VERBOSE_BIT_EXT -> "VERBOSE";
			case VkConst.VK_DEBUG_UTILS_MESSAGE_SEVERITY_INFO_BIT_EXT -> "INFO";
			case VkConst.VK_DEBUG_UTILS_MESSAGE_SEVERITY_WARNING_BIT_EXT -> "WARNING";
			case VkConst.VK_DEBUG_UTILS_MESSAGE_SEVERITY_ERROR_BIT_EXT -> "ERROR";
			default -> printUnknownToken(i);
		};
	}

	private void printDebugLog(int i, int j, int k, int l, int m, long n, long o) {
		String string = GLDebugMessageCallback.getMessage(m, n);
		VkDebug.LogEntry logEntry;
		synchronized (this.MESSAGE_BUFFER) {
			logEntry = this.lastEntry;
			if (logEntry != null && logEntry.isSame(i, j, k, l, string)) {
				logEntry.count++;
			} else {
				logEntry = new VkDebug.LogEntry(i, j, k, l, string);
				this.MESSAGE_BUFFER.add(logEntry);
				this.lastEntry = logEntry;
			}
		}

		LOGGER.info("Vulkan debug message: {}", logEntry);
	}

	public List<String> getLastVulkanDebugMessages() {
		synchronized (this.MESSAGE_BUFFER) {
			List<String> list = Lists.<String>newArrayListWithCapacity(this.MESSAGE_BUFFER.size());

			for (VkDebug.LogEntry logEntry : this.MESSAGE_BUFFER) {
				list.add(logEntry + " x " + logEntry.count);
			}

			return list;
		}
	}

	@Nullable
	public static VkDebug enableDebugCallback(int i, boolean bl, Set<String> set) {
		if (i <= 0) {
			return null;
		} else {
			GLCapabilities gLCapabilities = GL.getCapabilities();
			if (gLCapabilities.GL_KHR_debug && VkDevice.USE_GL_KHR_debug) {
				VkDebug vkDebug = new VkDebug();
				set.add("GL_KHR_debug");
				GL11.glEnable(37600);
				if (bl) {
					GL11.glEnable(33346);
				}

				for (int j = 0; j < DEBUG_LEVELS.size(); j++) {
					boolean bl2 = j < i;
					KHRDebug.glDebugMessageControl(4352, 4352, (Integer)DEBUG_LEVELS.get(j), (int[])null, bl2);
				}

				KHRDebug.glDebugMessageCallback(GLX.make(GLDebugMessageCallback.create(vkDebug::printDebugLog), DebugMemoryUntracker::untrack), 0L);
				return vkDebug;
			} else if (gLCapabilities.GL_ARB_debug_output && VkDevice.USE_GL_ARB_debug_output) {
				VkDebug vkDebug = new VkDebug();
				set.add("GL_ARB_debug_output");
				if (bl) {
					GL11.glEnable(33346);
				}

				for (int j = 0; j < DEBUG_LEVELS_ARB.size(); j++) {
					boolean bl2 = j < i;
					ARBDebugOutput.glDebugMessageControlARB(4352, 4352, (Integer)DEBUG_LEVELS_ARB.get(j), (int[])null, bl2);
				}

				ARBDebugOutput.glDebugMessageCallbackARB(GLX.make(GLDebugMessageARBCallback.create(vkDebug::printDebugLog), DebugMemoryUntracker::untrack), 0L);
				return vkDebug;
			} else {
				return null;
			}
		}
	}

	static class LogEntry {
		private final int id;
		private final int source;
		private final int type;
		private final int severity;
		private final String message;
		int count = 1;

		LogEntry(int i, int j, int k, int l, String string) {
			this.id = k;
			this.source = i;
			this.type = j;
			this.severity = l;
			this.message = string;
		}

		boolean isSame(int i, int j, int k, int l, String string) {
			return j == this.type && i == this.source && k == this.id && l == this.severity && string.equals(this.message);
		}

		public String toString() {
			return "id="
					+ this.id
					+ ", source="
					+ VkDebug.sourceToString(this.source)
					+ ", type="
					+ VkDebug.typeToString(this.type)
					+ ", severity="
					+ VkDebug.severityToString(this.severity)
					+ ", message='"
					+ this.message
					+ "'";
		}
	}
}
