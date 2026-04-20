package net.vulkanmod.config.video;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import java.util.List;

public class VideoModeSet {
    public final int width;
    public final int height;
    public final int bitDepth;
    List<Integer> refreshRates = new ObjectArrayList<>();

    public static VideoModeSet getDummy() {
        var set = new VideoModeSet(-1, -1, -1);
        set.addRefreshRate(-1);
        return set;
    }

    public VideoModeSet(int width, int height, int bitDepth) {
        this.width = width;
        this.height = height;
        this.bitDepth = bitDepth;
    }

    public int getRefreshRate() {
        return this.refreshRates.getFirst();
    }

    public boolean hasRefreshRate(int refreshRate) {
        return this.refreshRates.contains(refreshRate);
    }

    public List<Integer> getRefreshRates() {
        return this.refreshRates;
    }

    void addRefreshRate(int refreshRate) {
        this.refreshRates.add(refreshRate);
    }

    public String toString() {
        return this.width + " x " + this.height;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        VideoModeSet that = (VideoModeSet) o;
        return width == that.width && height == that.height && bitDepth == that.bitDepth && refreshRates.equals(that.refreshRates);
    }

    public VideoMode getVideoMode(int refresh) {
        int index = refreshRates.indexOf(refresh);

        if (index == -1) {
            index = 0;
        }

        return new VideoMode(this.width, this.height, this.bitDepth, this.refreshRates.get(index));
    }

    public VideoMode getVideoMode() {
        int refreshRate = this.refreshRates.getLast();
        return new VideoMode(this.width, this.height, this.bitDepth, refreshRate);
    }

    public static final class VideoMode {
        public int width;
        public int height;
        public int bitDepth;
        public int refreshRate;

        public VideoMode(int width, int height, int bitDepth, int refreshRate) {
            this.width = width;
            this.height = height;
            this.bitDepth = bitDepth;
            this.refreshRate = refreshRate;
        }

        @Override
        public String toString() {
            return "VideoMode["
                    + "width=" + width + ", "
                    + "height=" + height + ", "
                    + "bitDepth=" + bitDepth + ", "
                    + "refreshRate=" + refreshRate + ']';
        }

    }

}
