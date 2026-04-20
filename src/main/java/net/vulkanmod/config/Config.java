package net.vulkanmod.config;

import net.vulkanmod.config.video.VideoModeManager;
import net.vulkanmod.config.video.VideoModeSet;

public class Config {
    public VideoModeSet.VideoMode videoMode = VideoModeManager.getFirstAvailable().getVideoMode();
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
