package net.vulkanmod.config.option;

import com.mojang.blaze3d.platform.VideoMode;
import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.AttackIndicatorStatus;
import net.minecraft.client.CloudStatus;
import net.minecraft.client.GraphicsStatus;
import net.minecraft.client.InactivityFpsLimit;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.PrioritizeChunkUpdates;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ParticleStatus;
import net.vulkanmod.config.ConfigManager;
import net.vulkanmod.gui.OptionBlock;
import net.vulkanmod.config.video.WindowMode;
import net.vulkanmod.render.chunk.WorldRenderer;
import net.vulkanmod.render.chunk.build.light.LightMode;
import net.vulkanmod.render.vertex.TerrainRenderType;
import net.vulkanmod.vulkan.Renderer;
import net.vulkanmod.vulkan.device.DeviceManager;

import java.util.Optional;
import java.util.stream.IntStream;

public abstract class VOptions {
	private static final Minecraft minecraft = Minecraft.getInstance();
	private static final Options options = minecraft.options;
	private static final Window window = minecraft.getWindow();
	public static boolean fullscreenDirty = false;

	public static OptionBlock[] getVideoOpts() {
//		var videoMode = ConfigManager.getConfig().videoMode;
//		var videoModeSet = VideoModeManager.getFromVideoMode(videoMode);

//		VideoModeManager.selectedVideoMode = videoMode;
//		var refreshRates = videoModeSet.getRefreshRates();

//		CyclingOption<Integer> RefreshRate = (CyclingOption<Integer>) new CyclingOption<>(
//				Component.translatable("vulkanmod.options.refreshRate"),
//				refreshRates.toArray(new Integer[0]),
//				(value) -> {
//					VideoModeManager.selectedVideoMode.refreshRate = value;
//					VideoModeManager.applySelectedVideoMode();

//					if (options.fullscreen().get()) {
//						fullscreenDirty = true;
//					}
//				},
//				() -> VideoModeManager.selectedVideoMode.getRefreshRate())
//				.setTranslator(refreshRate -> Component.nullToEmpty(refreshRate.toString()));

		var monitor = window.findBestMonitor();
		int modeCount = monitor != null ? monitor.getModeCount() : 0;

		Integer[] values = new Integer[modeCount + 1];
		values[0] = -1;
		for (int i = 0; i < modeCount; i++) {
			values[i + 1] = i;
		}

		CyclingOption<Integer> resolutionOption = (CyclingOption<Integer>) new CyclingOption<>(
				Component.translatable("options.fullscreen.resolution"),
				values,
				(value) -> {
					if (monitor != null) {
						window.setPreferredFullscreenVideoMode(
								value == -1 ? Optional.empty() : Optional.of(monitor.getMode(value))
						);
					}
					if (options.fullscreen().get()) {
						fullscreenDirty = true;
					}
				},
				() -> {
					if (monitor == null) return -1;
					return window.getPreferredFullscreenVideoMode().map(monitor::getVideoModeIndex).orElse(-1);
				})
				.setTranslator(value -> {
					if (monitor == null) {
						return Component.translatable("options.fullscreen.unavailable");
					} else if (value == -1) {
						return Component.translatable("options.fullscreen.current");
					} else {
						VideoMode videoMode = monitor.getMode(value);
						return Component.translatable(
								"options.fullscreen.entry",
								videoMode.getWidth(),
								videoMode.getHeight(),
								videoMode.getRefreshRate(),
								videoMode.getRedBits() + videoMode.getGreenBits() + videoMode.getBlueBits()
						);
					}
				});

//		Option<VideoModeSet> resolutionOption = new CyclingOption<>(
//				Component.translatable("options.fullscreen.resolution"),
//				VideoModeManager.getVideoResolutions(),
//				(value) -> {
//					VideoModeManager.selectedVideoMode = value.getVideoMode(RefreshRate.getNewValue());
//					VideoModeManager.applySelectedVideoMode();

//					if (options.fullscreen().get()) {
//						fullscreenDirty = true;
//					}
//				},
//				() -> {
//					var selectedVideoMode = VideoModeManager.selectedVideoMode;
//					var selectedVideoModeSet = VideoModeManager.getFromVideoMode(selectedVideoMode);
//
//					return selectedVideoModeSet;
//				})
//				.setTranslator(resolution -> Component.nullToEmpty(resolution.toString()));

//		resolutionOption.setOnChange(() -> {
//			var newVideoMode = resolutionOption.getNewValue();
//			var newRefreshRates = newVideoMode.getRefreshRates().toArray(new Integer[0]);

//			RefreshRate.setValues(newRefreshRates);
//			RefreshRate.setNewValue(newRefreshRates[newRefreshRates.length - 1]);
//		});

		return new OptionBlock[]{
				new OptionBlock("", new Option<?>[]{
						resolutionOption,
//						RefreshRate,
						new CyclingOption<>(Component.translatable("vulkanmod.options.windowMode"),
								WindowMode.values(),
								value -> {
									boolean exclusiveFullscreen = value == WindowMode.EXCLUSIVE_FULLSCREEN;
									options.fullscreen().set(exclusiveFullscreen);
									ConfigManager.getConfig().windowMode = value.mode;
									fullscreenDirty = true;
								},
								() -> WindowMode.fromValue(ConfigManager.getConfig().windowMode))
								.setTranslator(value -> Component.translatable(WindowMode.getComponentName(value))),
						new RangeOption(Component.translatable("options.framerateLimit"),
								10, 260, 10,
								value -> Component.nullToEmpty(value == 260
										? Component.translatable("options.framerateLimit.max").getString()
										: String.valueOf(value)),
								value -> {
									options.framerateLimit().set(value);
									minecraft.getFramerateLimitTracker().setFramerateLimit(value);
								},
								() -> options.framerateLimit().get()),
						new SwitchOption(Component.translatable("options.vsync"),
								value -> {
									options.enableVsync().set(value);
									window.updateVsync(value);
								},
								() -> options.enableVsync().get()),
						new CyclingOption<>(Component.translatable("options.inactivityFpsLimit"),
								InactivityFpsLimit.values(),
								value -> options.inactivityFpsLimit().set(value),
								() -> options.inactivityFpsLimit().get())
								.setTranslator(inactivityFpsLimit -> Component.translatable(inactivityFpsLimit.getKey()))
				}),
				new OptionBlock("", new Option<?>[]{
						new RangeOption(Component.translatable("options.guiScale"),
								0, window.calculateScale(0, minecraft.isEnforceUnicode()), 1,
								value -> Component.translatable((value == 0)
										? "options.guiScale.auto"
										: String.valueOf(value)),
								value -> {
									options.guiScale().set(value);
									minecraft.resizeDisplay();
								},
								() -> (options.guiScale().get())),
						new RangeOption(Component.translatable("options.gamma"),
								0, 100, 1,
								value -> Component.translatable(switch (value) {
									case 0 -> "options.gamma.min";
									case 50 -> "options.gamma.default";
									case 100 -> "options.gamma.max";
									default -> String.valueOf(value);
								}),
								value -> options.gamma().set(value * 0.01),
								() -> (int) (options.gamma().get() * 100.0)),
				}),
				new OptionBlock("", new Option<?>[]{
						new SwitchOption(Component.translatable("options.viewBobbing"),
								(value) -> options.bobView().set(value),
								() -> options.bobView().get()),
						new CyclingOption<>(Component.translatable("options.attackIndicator"),
								AttackIndicatorStatus.values(),
								value -> options.attackIndicator().set(value),
								() -> options.attackIndicator().get())
								.setTranslator(value -> Component.translatable(value.getKey())),
						new SwitchOption(Component.translatable("options.autosaveIndicator"),
								value -> options.showAutosaveIndicator().set(value),
								() -> options.showAutosaveIndicator().get()),
				})
		};
	}

	public static OptionBlock[] getGraphicsOpts() {
		return new OptionBlock[]{
				new OptionBlock("", new Option<?>[]{
						new RangeOption(Component.translatable("options.renderDistance"),
								2, 32, 1,
								(value) -> options.renderDistance().set(value),
								() -> options.renderDistance().get()),
						new RangeOption(Component.translatable("options.simulationDistance"),
								5, 32, 1,
								(value) -> options.simulationDistance().set(value),
								() -> options.simulationDistance().get()),
						new CyclingOption<>(Component.translatable("options.prioritizeChunkUpdates"),
								PrioritizeChunkUpdates.values(),
								value -> options.prioritizeChunkUpdates().set(value),
								() -> options.prioritizeChunkUpdates().get())
								.setTranslator(value -> Component.translatable(value.getKey())),
				}),
				new OptionBlock("", new Option<?>[]{
						new CyclingOption<>(Component.translatable("options.graphics"),
								new GraphicsStatus[]{GraphicsStatus.FAST, GraphicsStatus.FANCY},
								value -> options.graphicsMode().set(value),
								() -> options.graphicsMode().get())
								.setTranslator(graphicsMode -> Component.translatable(graphicsMode.getKey())),
						new CyclingOption<>(Component.translatable("options.particles"),
								new ParticleStatus[]{ParticleStatus.MINIMAL, ParticleStatus.DECREASED, ParticleStatus.ALL},
								value -> options.particles().set(value),
								() -> options.particles().get())
								.setTranslator(particlesMode -> Component.translatable(particlesMode.getKey())),
						new CyclingOption<>(Component.translatable("options.renderClouds"),
								CloudStatus.values(),
								value -> options.cloudStatus().set(value),
								() -> options.cloudStatus().get())
								.setTranslator(value -> Component.translatable(value.getKey())),
						new CyclingOption<>(Component.translatable("options.ao"),
								new Integer[]{LightMode.FLAT, LightMode.SMOOTH, LightMode.SUB_BLOCK},
								(value) -> {
									if (value > LightMode.FLAT) {
										options.ambientOcclusion().set(true);
									} else {
										options.ambientOcclusion().set(false);
									}

									ConfigManager.getConfig().ambientOcclusion = value;

									minecraft.levelRenderer.allChanged();
								},
								() -> ConfigManager.getConfig().ambientOcclusion)
								.setTranslator(value -> Component.translatable(switch (value) {
									case LightMode.FLAT -> "options.off";
									case LightMode.SMOOTH -> "options.on";
									case LightMode.SUB_BLOCK -> "vulkanmod.options.ao.subBlock";
									default -> "vulkanmod.options.unknown";
								}))
								.setTooltip(Component.translatable("vulkanmod.options.ao.subBlock.tooltip")),
						new RangeOption(Component.translatable("options.biomeBlendRadius"),
								0, 7, 1,
								value -> {
									int v = value * 2 + 1;
									return Component.nullToEmpty("%d x %d".formatted(v, v));
								},
								(value) -> {
									options.biomeBlendRadius().set(value);
									minecraft.levelRenderer.allChanged();
								},
								() -> options.biomeBlendRadius().get()),
				}),
				new OptionBlock("", new Option<?>[]{
						new SwitchOption(Component.translatable("options.entityShadows"),
								value -> options.entityShadows().set(value),
								() -> options.entityShadows().get()),
						new RangeOption(Component.translatable("options.entityDistanceScaling"),
								50, 500, 25,
								value -> options.entityDistanceScaling().set(value * 0.01),
								() -> options.entityDistanceScaling().get().intValue() * 100),
						new CyclingOption<>(Component.translatable("options.mipmapLevels"),
								new Integer[]{0, 1, 2, 3, 4},
								value -> {
									options.mipmapLevels().set(value);
									minecraft.updateMaxMipLevel(value);
									minecraft.delayTextureReload();
								},
								() -> options.mipmapLevels().get())
								.setTranslator(value -> Component.nullToEmpty(value.toString()))
				})
		};
	}

	public static OptionBlock[] getOptimizationOpts() {
		return new OptionBlock[]{
				new OptionBlock("", new Option[]{
						new CyclingOption<>(Component.translatable("vulkanmod.options.advCulling"),
								new Integer[]{1, 2, 3, 10},
								value -> ConfigManager.getConfig().advCulling = value,
								() -> ConfigManager.getConfig().advCulling)
								.setTranslator(value -> Component.translatable(switch (value) {
									case 1 -> "vulkanmod.options.advCulling.aggressive";
									case 2 -> "vulkanmod.options.advCulling.normal";
									case 3 -> "vulkanmod.options.advCulling.conservative";
									case 10 -> "options.off";
									default -> "vulkanmod.options.unknown";
								}))
								.setTooltip(Component.translatable("vulkanmod.options.advCulling.tooltip")),
						new SwitchOption(Component.translatable("vulkanmod.options.entityCulling"),
								value -> ConfigManager.getConfig().entityCulling = value,
								() -> ConfigManager.getConfig().entityCulling)
								.setTooltip(Component.translatable("vulkanmod.options.entityCulling.tooltip")),
						new SwitchOption(Component.translatable("vulkanmod.options.uniqueOpaqueLayer"),
								value -> {
									ConfigManager.getConfig().uniqueOpaqueLayer = value;
									TerrainRenderType.updateMapping();
									minecraft.levelRenderer.allChanged();
								},
								() -> ConfigManager.getConfig().uniqueOpaqueLayer)
								.setTooltip(Component.translatable("vulkanmod.options.uniqueOpaqueLayer.tooltip")),
						new SwitchOption(Component.translatable("vulkanmod.options.backfaceCulling"),
								value -> {
									ConfigManager.getConfig().backFaceCulling = value;
									Minecraft.getInstance().levelRenderer.allChanged();
								},
								() -> ConfigManager.getConfig().backFaceCulling)
								.setTooltip(Component.translatable("vulkanmod.options.backfaceCulling.tooltip")),
						new SwitchOption(Component.translatable("vulkanmod.options.indirectDraw"),
								value -> ConfigManager.getConfig().indirectDraw = value,
								() -> ConfigManager.getConfig().indirectDraw)
								.setTooltip(Component.translatable("vulkanmod.options.indirectDraw.tooltip")),
				})
		};

	}

	public static OptionBlock[] getOtherOpts() {
		return new OptionBlock[]{
				new OptionBlock("", new Option[]{
						new RangeOption(Component.translatable("vulkanmod.options.builderThreads"),
								0, (Runtime.getRuntime().availableProcessors() - 1), 1,
								value -> {
									ConfigManager.getConfig().builderThreads = value;
									WorldRenderer.getInstance().getTaskDispatcher().createThreads(value);
								},
								() -> ConfigManager.getConfig().builderThreads)
								.setTranslator(value -> {
							if (value == 0) {
								return Component.translatable("vulkanmod.options.builderThreads.auto");
							} else {
								return Component.nullToEmpty(String.valueOf(value));
							}
						}),
						new RangeOption(Component.translatable("vulkanmod.options.frameQueue"),
								2, 5, 1,
								value -> {
									ConfigManager.getConfig().frameQueueSize = value;
									Renderer.scheduleSwapChainUpdate();
								},
								() -> ConfigManager.getConfig().frameQueueSize)
								.setTooltip(Component.translatable("vulkanmod.options.frameQueue.tooltip")),
						new CyclingOption<>(Component.translatable("vulkanmod.options.deviceSelector"),
								IntStream.range(-1, DeviceManager.suitableDevices.size()).boxed().toArray(Integer[]::new),
								value -> ConfigManager.getConfig().device = value,
								() -> ConfigManager.getConfig().device)
								.setTranslator(value -> Component.translatable((value == -1)
										? "vulkanmod.options.deviceSelector.auto"
										: DeviceManager.suitableDevices.get(value).deviceName)
								)
								.setTooltip(Component.nullToEmpty("%s: %s".formatted(
										Component.translatable("vulkanmod.options.deviceSelector.tooltip").getString(),
										DeviceManager.device.deviceName
								))
						)
				})
		};

	}
}