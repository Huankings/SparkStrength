package annina.sparkstrength.client.screen.criminologist;

import annina.sparkstrength.client.ui.common.PlayerNameResolver;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public final class CriminologistScreen extends Screen {
    private static final int COLUMNS = 8;
    private static final int SPACING_X = 36;
    private static final int SPACING_Y = 42;
    private final UUID victimUuid;

    public CriminologistScreen(UUID victimUuid) {
        super(Text.translatable("screen.sparkstrength.criminologist.title"));
        this.victimUuid = victimUuid;
    }

    @Override
    protected void init() {
        super.init();
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player == null) {
            close();
            return;
        }

        GameWorldComponent gameComponent = GameWorldComponent.KEY.get(player.getWorld());
        List<UUID> targets = new ArrayList<>(gameComponent.getAllPlayers());
        targets.sort(Comparator.comparing(PlayerNameResolver::playerName, String.CASE_INSENSITIVE_ORDER));

        int visibleColumns = Math.min(COLUMNS, Math.max(1, targets.size()));
        int rows = (int) Math.ceil(targets.size() / (double) COLUMNS);
        int startX = (width / 2) - ((visibleColumns - 1) * SPACING_X / 2) - (CriminologistTargetWidget.WIDGET_SIZE / 2);
        int startY = (height / 2) - (rows * SPACING_Y / 2) + 16;
        for (int index = 0; index < targets.size(); index++) {
            UUID targetUuid = targets.get(index);
            int row = index / COLUMNS;
            int column = index % COLUMNS;
            addDrawableChild(new CriminologistTargetWidget(
                    startX + column * SPACING_X,
                    startY + row * SPACING_Y,
                    victimUuid,
                    targetUuid
            ));
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, width, height, 0xD8000000);
        context.fill(0, 0, width, 20, 0xFF3A1111);
        context.fill(0, height - 20, width, height, 0xFF3A1111);
        super.render(context, mouseX, mouseY, delta);

        TextRenderer renderer = MinecraftClient.getInstance().textRenderer;
        context.drawCenteredTextWithShadow(
                renderer,
                Text.translatable("screen.sparkstrength.criminologist.title"),
                width / 2,
                height / 2 - 90,
                0xFFFFFF
        );
        context.drawCenteredTextWithShadow(
                renderer,
                Text.translatable("screen.sparkstrength.criminologist.subtitle"),
                width / 2,
                height / 2 - 75,
                0xAAAAAA
        );
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void close() {
        if (client != null) {
            client.setScreen(null);
        }
    }
}
