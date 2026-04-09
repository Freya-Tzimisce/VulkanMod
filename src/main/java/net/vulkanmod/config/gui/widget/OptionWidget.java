package net.vulkanmod.config.gui.widget;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.narration.NarratableEntry.NarrationPriority;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.vulkanmod.config.gui.GuiRenderer;
import net.vulkanmod.config.option.Option;
import net.vulkanmod.vulkan.util.ColorUtil;

public abstract class OptionWidget<O extends Option<?>> extends VAbstractWidget implements NarratableEntry {

    public int controlX;
    public int controlWidth;
    private final Component name;
    protected Component displayedValue;

    protected boolean controlHovered;

    O option;

    public OptionWidget(int x, int y, int width, int height, Component name) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.name = name;
        this.displayedValue = Component.literal("N/A");

        this.controlWidth = Math.min((int)(width * 0.5f) - 8, 120);
        this.controlX = this.x + this.width - this.controlWidth - 8;
    }

    public void setOption(O option) {
        this.option = option;
    }

    @Override
    public void render(double mouseX, double mouseY) {
        if (this.visible) {
            this.updateDisplayedValue();

            this.controlHovered = mouseX >= this.controlX
                    && mouseY >= this.y
                    && mouseX < this.controlX + this.controlWidth
                    && mouseY < this.y + this.height;
            this.renderWidget(mouseX, mouseY);
        }
    }

    public void updateState() {
    }

    @Override
    public void renderWidget(double mouseX, double mouseY) {
        Minecraft minecraftClient = Minecraft.getInstance();

        int i = this.getYImage(this.isHovered());

        int xPadding = 0;
        int yPadding = 0;

        int color = ColorUtil.ARGB.pack(0.0f, 0.0f, 0.0f, 0.45f);
        GuiRenderer.fill(this.x - xPadding, this.y - yPadding, this.x + this.width + xPadding, this.y + this.height + yPadding, color);

        this.renderHovering(0, 0);

        color = this.active ? 0xFFFFFF : 0xA0A0A0;

        Font textRenderer = minecraftClient.font;
        GuiRenderer.drawString(textRenderer, this.getName().getVisualOrderText(), this.x + 8, this.y + (this.height - 8) / 2, color);

        this.renderControls(mouseX, mouseY);
    }

    protected int getYImage(boolean hovered) {
        int i = 1;
        if (!this.active) {
            i = 0;
        } else if (hovered) {
            i = 2;
        }
        return i;
    }

    public boolean isHovered() {
        return this.hovered || this.focused;
    }

    protected abstract void renderControls(double mouseX, double mouseY);

    @Override
    public abstract void onClick(double mouseX, double mouseY);

    @Override
    public abstract void onRelease(double mouseX, double mouseY);

    @Override
    protected abstract void onDrag(double mouseX, double mouseY, double deltaX, double deltaY);

    @Override
    protected boolean isValidClickButton(int button) {
        return button == 0;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (this.isValidClickButton(button)) {
            this.onDrag(mouseX, mouseY, deltaX, deltaY);
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!this.active || !this.visible) {
            return false;
        }

        if (this.isValidClickButton(button) && this.clicked(mouseX, mouseY)) {
            this.playDownSound(Minecraft.getInstance().getSoundManager());
            this.onClick(mouseX, mouseY);
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (this.isValidClickButton(button)) {
            this.onRelease(mouseX, mouseY);
            return true;
        }
        return false;
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        return this.active
                && this.visible
                && mouseX >= (double) this.x
                && mouseY >= (double) this.y
                && mouseX < (double)(this.x + this.width)
                && mouseY < (double)(this.y + this.height);
    }

    @Override
    public void setFocused(boolean bl) {
        this.focused = bl;
    }

    @Override
    public boolean isFocused() {
        return this.focused;
    }

    @Override
    protected boolean clicked(double mouseX, double mouseY) {
        return this.active
                && this.visible
                && mouseX >= (double) this.controlX
                && mouseY >= (double) this.y
                && mouseX < (double)(this.x + this.width)
                && mouseY < (double)(this.y + this.height);
    }

    public Component getName() {
        return this.name;
    }

    public Component getDisplayedValue() {
        return this.displayedValue;
    }

    protected void updateDisplayedValue() {
        this.displayedValue = this.option.getDisplayedValue();
    }

    @Override
    public Component getTooltip() {
        return this.option.getTooltip();
    }

    @Override
    public NarrationPriority narrationPriority() {
        if (this.focused) {
            return NarrationPriority.FOCUSED;
        }
        if (this.hovered) {
            return NarrationPriority.HOVERED;
        }
        return NarrationPriority.NONE;
    }

    @Override
    public final void updateNarration(NarrationElementOutput narrationElementOutput) {
    }

    @Override
    public void playDownSound(SoundManager soundManager) {
        soundManager.play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f));
    }
}