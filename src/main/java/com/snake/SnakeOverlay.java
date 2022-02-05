package com.snake;

import net.runelite.api.Client;
import net.runelite.client.ui.overlay.OverlayMenuEntry;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

import javax.inject.Inject;
import java.awt.*;

import static net.runelite.api.MenuAction.RUNELITE_OVERLAY;

class SnakeOverlay extends OverlayPanel {

    private Client client;
    private SnakePlugin plugin;
    private SnakeConfig config;

    @Inject
    SnakeOverlay(SnakePlugin plugin, Client client, SnakeConfig config) {
        super(plugin);
        this.client = client;
        this.plugin = plugin;
        this.config = config;

        setPosition(OverlayPosition.TOP_LEFT);
        getMenuEntries().add(new OverlayMenuEntry(RUNELITE_OVERLAY, "Start", "new game"));
    }

    @Override
    public Dimension render(Graphics2D graphics) {
        String title;
        Color color;
        switch (plugin.getCurrentState()) {
            case PLAYING:
                title = "Playing";
                color = Color.GREEN;
                break;
            case GAME_OVER:
                title = "Game Over";
                color = Color.RED;
                break;
            case WAITING_TO_START:
                title = "Move to start";
                color = Color.YELLOW;
                break;
            case RUN_ON:
                title = "Running is not allowed";
                color = Color.RED;
                break;
            default:
                return super.render(graphics);
        }

        panelComponent.getChildren().add(TitleComponent.builder()
                .text(title)
                .color(color)
                .build());

        panelComponent.getChildren().add(LineComponent.builder()
                .left("Score:")
                .right(String.format("%d", plugin.getScore()))
                .build());

        return super.render(graphics);
    }
}
