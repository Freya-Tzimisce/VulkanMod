package net.vulkanmod.config.gui.widget;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.vulkanmod.config.gui.GuiRenderer;
import net.vulkanmod.config.option.CyclingOption;
import net.vulkanmod.vulkan.VRenderSystem;
import net.vulkanmod.vulkan.util.ColorUtil;
import org.joml.Matrix4f;

public class CyclingOptionWidget extends OptionWidget<CyclingOption<?>> {
    private Button leftButton;
    private Button rightButton;

    private boolean focused;

    public CyclingOptionWidget(CyclingOption<?> option, int x, int y, int width, int height, Component name) {
        super(x, y, width, height, name);
        this.option = option;
        this.leftButton = new Button(this.controlX, 16, Button.Direction.LEFT);
        this.rightButton = new Button(this.controlX + this.controlWidth - 16, 16, Button.Direction.RIGHT);

        // updateDisplayedValue(option.getValueText());
    }

    @Override
    protected int getYImage(boolean hovered) {
        return 0;
    }

    @Override
    public void renderControls(double mouseX, double mouseY) {
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);

        this.renderBars();

        this.leftButton.setStatus(this.option.index() > 0);
        this.rightButton.setStatus(this.option.index() < this.option.getValues().length - 1);

        int color = this.active ? 0xFFFFFF : 0xA0A0A0;
        Font textRenderer = Minecraft.getInstance().font;
        int x = this.controlX + this.controlWidth / 2;
        int y = this.y + (this.height - 9) / 2;
        GuiRenderer.drawCenteredString(textRenderer, this.getDisplayedValue(), x, y, color);

        this.leftButton.renderButton(GuiRenderer.guiGraphics.pose(), mouseX, mouseY);
        this.rightButton.renderButton(GuiRenderer.guiGraphics.pose(), mouseX, mouseY);
    }

    public void renderBars() {
        int count = this.option.getValues().length;
        int current = this.option.index();

        int margin = 30;
        int padding = 4;

        int barWidth = (this.controlWidth - (2 * margin) - (padding * count)) / count;
        int color = ColorUtil.ARGB.pack(1.0f, 1.0f, 1.0f, 0.4f);
        int activeColor = ColorUtil.ARGB.pack(1.0f, 1.0f, 1.0f, 1.0f);

        if (barWidth > 0) {
            for (int i = 0; i < count; i++) {
                float x0 = this.controlX + margin + i * (barWidth + padding);
                float y0 = this.y + this.height - 5.0f;

                int c = i == current ? activeColor : color;
                GuiRenderer.fill(x0, y0, x0 + barWidth, y0 + 1.5f, c);
            }
        }
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        if (this.leftButton.isHovered(mouseX, mouseY)) {
            this.option.prevValue();
        } else if (this.rightButton.isHovered(mouseX, mouseY)) {
            this.option.nextValue();
        }
    }

    @Override
    public void onRelease(double mouseX, double mouseY) {
    }

    @Override
    protected void onDrag(double mouseX, double mouseY, double deltaX, double deltaY) {
    }

    @Override
    public void setFocused(boolean bl) {
        this.focused = bl;
    }

    @Override
    public boolean isFocused() {
        return this.focused;
    }

    class Button {
        int x;
        int width;
        boolean active;
        Direction direction;

        Button(int x, int width, Direction direction) {
            this.x = x;
            this.width = width;
            this.active = true;
            this.direction = direction;
        }

        boolean isHovered(double mouseX, double mouseY) {
            return mouseX >= x
                    && mouseX <= x + width
                    && mouseY >= y
                    && mouseY <= y + height;
        }

        void setStatus(boolean status) {
            this.active = status;
        }

        void renderButton(PoseStack matrices, double mouseX, double mouseY) {
            Tesselator tesselator = Tesselator.getInstance();
            BufferBuilder bufferBuilder = tesselator.begin(VertexFormat.Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_COLOR);

            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);

            float f = this.isHovered(mouseX, mouseY) && this.active ? 5.0f : 4.5f;

            Matrix4f matrix4f = matrices.last().pose();

            VRenderSystem.enableBlend();

            if (this.isHovered(mouseX, mouseY) && this.active) {
                RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
            } else if (this.active) {
                RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 0.8f);
            } else {
                RenderSystem.setShaderColor(0.3f, 0.3f, 0.3f, 0.8f);
            }

            int color = -1;
            float w = f - 1.0f;
            float yC = y + height * 0.5f;
            float xC = x + width * 0.5f;
            if (this.direction == Direction.LEFT) {
                bufferBuilder.addVertex(matrix4f, xC - w, yC, 0.0f).setColor(color);
                bufferBuilder.addVertex(matrix4f, xC + w, yC + f, 0.0f).setColor(color);
                bufferBuilder.addVertex(matrix4f, xC + w, yC - f, 0.0f).setColor(color);
            } else {
                bufferBuilder.addVertex(matrix4f, xC + w, yC, 0.0f).setColor(color);
                bufferBuilder.addVertex(matrix4f, xC - w, yC - f, 0.0f).setColor(color);
                bufferBuilder.addVertex(matrix4f, xC - w, yC + f, 0.0f).setColor(color);
            }

            RenderType.gui().draw(bufferBuilder.buildOrThrow());

            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        }

        enum Direction {
            LEFT,
            RIGHT
        }
    }
}