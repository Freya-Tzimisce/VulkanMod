package net.vulkanmod.config.video;

import com.mojang.blaze3d.platform.VideoMode;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import java.util.List;

public class VideoModeSet {
	public final int width;
	public final int height;
	public final int redBits;
	public final int greenBits;
	public final int blueBits;
	List<Integer> refreshRates = new ObjectArrayList<>();

	public VideoModeSet(int width, int height, int redBits, int greenBits, int blueBits) {
		this.width = width;
		this.height = height;
		this.redBits = redBits;
		this.greenBits = greenBits;
		this.blueBits = blueBits;
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

	@Deprecated(forRemoval = true)
	public String toString() {
		return this.width + " x " + this.height;
	}

	@Override
	@Deprecated(forRemoval = true)
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}

		VideoModeSet that = (VideoModeSet) o;
		return width == that.width && height == that.height && redBits == that.redBits && greenBits == that.greenBits && blueBits == that.blueBits && refreshRates.equals(that.refreshRates);
	}

	@Deprecated(forRemoval = true)
	public VideoMode getVideoMode(int refresh) {
		int index = refreshRates.indexOf(refresh);

		if (index == -1) {
			index = 0;
		}

		return new VideoMode(this.width, this.height, this.redBits, this.greenBits, this.blueBits, this.refreshRates.get(index));
	}

	public VideoMode getVideoMode() {
		return new VideoMode(this.width, this.height, this.redBits, this.greenBits, this.blueBits, this.refreshRates.getLast());
	}
}
