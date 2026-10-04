package wootrevived.woot.upgrades.filter.buttons;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;
import wootrevived.woot.util.render.WootButton;
import wootrevived.woot.util.render.WootContainerScreen;

public class WootFilterGPButton extends WootButton {
    protected final OnPress onPress;
    protected Type type;
    public boolean disabled;

    public WootFilterGPButton(int x, int y, boolean disabled, Type type, OnPress onPress) {
        super(x, y, 14, 14);
        this.onPress = onPress;
        this.disabled = disabled;
        this.type = type;
    }

    @Override
    protected void renderWidget(@NotNull GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        if(active){
            gui.blit(WootContainerScreen.GUI, getX(), getY(), type.u, type.v, getWidth(), getHeight());
            if(disabled)
                gui.fill(getX() + 1, getY() + 1, getX() + getWidth() - 1, getY() + getHeight() - 1, 0x80000000);
            if(isHovered()) {
                if(!disabled) gui.fill(getX() + 1, getY() + 1, getX() + getWidth() - 1, getY() + getHeight() - 1, 0x80FFFFFF);
                gui.renderTooltip(WootContainerScreen.getFont(), type.getComponent(), mouseX, mouseY);
            }
        }
    }

    @Override
    public void onPress() {
        if(!disabled) onPress.onPress(this);
    }

    @OnlyIn(Dist.CLIENT)
    public interface OnPress {
        void onPress(WootFilterGPButton button);
    }

    public enum Type {
        NEW(185, 177),
        EDIT(185, 192),
        DELETE(200, 192),
        CONFIRM(215, 162),
        CANCEL(230, 162);

        public final int u;
        public final int v;

        Type(int u, int v){
            this.u = u;
            this.v = v;
        }

        public Component getComponent(){
            if(this == NEW) return Component.translatable("gui.woot_revived.filter.button.add");
            else if(this == EDIT) return Component.translatable("gui.woot_revived.filter.button.edit");
            else if(this == DELETE) return Component.translatable("gui.woot_revived.filter.button.remove");
            else if(this == CONFIRM) return Component.translatable("gui.woot_revived.filter.button.confirm");
            else if(this == CANCEL) return Component.translatable("gui.woot_revived.filter.button.cancel");
            return Component.empty();
        }
    }
}
