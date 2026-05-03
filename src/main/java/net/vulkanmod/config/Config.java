package net.vulkanmod.config;

import com.mojang.blaze3d.platform.VideoMode;
import net.vulkanmod.config.video.VideoModeManager;

public class Config {
    public VideoMode videoMode = VideoModeManager.getFirstAvailable().getVideoMode();
    public int windowMode = 0;

    public int advCulling = 2;
    public boolean indirectDraw = true;

    public boolean uniqueOpaqueLayer = true;
    public boolean entityCulling = true;
    public int device = -1;

    public int ambientOcclusion = 1;
    public int frameQueueSize = 2;
    public int builderThreads = 0;

    public boolean backFaceCulling = true;
}
