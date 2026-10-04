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

public class WootNBTButton extends WootButton {
    protected final OnPress onPress;
    protected State state;
    public boolean disabled;

    public WootNBTButton(int x, int y, boolean disabled, State state, OnPress onPress) {
        super(x, y, 14, 14);
        this.onPress = onPress;
        this.disabled = disabled;
        this.state = state;
    }

    @Override
    protected void renderWidget(@NotNull GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        if(active){
            gui.blit(WootContainerScreen.GUI, getX(), getY(), state.u, state.v, getWidth(), getHeight());
            if(disabled)
                gui.fill(getX() + 1, getY() + 1, getX() + getWidth() - 1, getY() + getHeight() - 1, 0x80000000);
            if(isHovered()) {
                if(!disabled) gui.fill(getX() + 1, getY() + 1, getX() + getWidth() - 1, getY() + getHeight() - 1, 0x80FFFFFF);
                gui.renderTooltip(WootContainerScreen.getFont(), state.getComponent(), mouseX, mouseY);
            }
        }
    }

    @Override
    public void onPress() {
        if(!disabled) onPress.onPress(this);
    }

    public void setState(State state){
        this.state = state;
    }

    public State nextState(){
        state = state.getNext();
        return state;
    }

    @OnlyIn(Dist.CLIENT)
    public interface OnPress {
        void onPress(WootNBTButton button);
    }

    public enum State implements StringRepresentable {
        ONLY("only", 215, 207),
        EXACT("exact", 230, 207);

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
            if(this == ONLY) return EXACT;
            return ONLY;
        }

        public Component getComponent(){
            if(this == ONLY) return Component.translatable("gui.woot_revived.filter.nbt.only");
            return Component.translatable("gui.woot_revived.filter.nbt.exact");
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
