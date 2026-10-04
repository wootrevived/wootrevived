package wootrevived.woot.upgrades.filter.buttons;

import com.mojang.serialization.Codec;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.StringRepresentable;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;
import wootrevived.woot.util.render.WootButton;
import wootrevived.woot.util.render.WootContainerScreen;

public class WootWBListButton extends WootButton {
    protected final OnPress onPress;
    protected final boolean alternative;
    protected State state;

    public WootWBListButton(int x, int y, boolean alternative, State state, OnPress onPress) {
        super(x, y, 14, 14);
        this.onPress = onPress;
        this.alternative = alternative;
        this.state = state;
    }

    @Override
    protected void renderWidget(@NotNull GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        if(active){
            gui.blit(WootContainerScreen.GUI, getX(), getY(), state.u, state.v, getWidth(), getHeight());
            if(isHovered()) {
                gui.fill(getX() + 1, getY() + 1, getX() + getWidth() - 1, getY() + getHeight() - 1, 0x80FFFFFF);
                gui.renderTooltip(WootContainerScreen.getFont(), state.getComponent(alternative), mouseX, mouseY);
            }
        }
    }

    public void setState(State state){
        this.state = state;
    }

    public State nextState(){
        state = state.getNext();
        return state;
    }

    @Override
    public void onPress() {
        onPress.onPress(this);
    }

    @OnlyIn(Dist.CLIENT)
    public interface OnPress {
        void onPress(WootWBListButton button);
    }

    public enum State implements StringRepresentable {
        WHITELIST("whitelist", 215, 192),
        BLACKLIST("blacklist", 230, 192);

        private static final Codec<State> CODEC = StringRepresentable.fromEnum(State::values);

        private final String name;
        public final int u;
        public final int v;

        State(String name, int u, int v){
            this.name = name;
            this.u = u;
            this.v = v;
        }

        public State getNext(){
            if(this == WHITELIST) return BLACKLIST;
            return WHITELIST;
        }

        public Component getComponent(boolean alternative){
            if(this == WHITELIST) return Component.translatable(alternative ? "gui.woot_revived.filter.mode.regular" : "gui.woot_revived.filter.mode.whitelist");
            return Component.translatable(alternative ? "gui.woot_revived.filter.mode.inverted" : "gui.woot_revived.filter.mode.blacklist");
        }

        @Override
        public @NotNull String getSerializedName() {
            return name;
        }

        public Codec<State> codec() {
            return CODEC;
        }
    }
}
