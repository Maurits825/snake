package com.snake;

import net.runelite.api.Client;
import net.runelite.api.Perspective;
import net.runelite.api.Point;
import net.runelite.api.SpriteID;
import net.runelite.client.game.SpriteManager;
import net.runelite.client.ui.overlay.OverlayMenuEntry;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayUtil;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

import javax.inject.Inject;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import static net.runelite.api.MenuAction.RUNELITE_OVERLAY;

class SnakeOverlay extends OverlayPanel {

    private Client client;
    private SnakePlugin plugin;
    private SnakeConfig config;

    @Inject
    SpriteManager spriteManager;

    private boolean hitsplatTimerStarted;
    private long hitsplatStart;

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
            case INIT:
                hitsplatStart = 0;
            case PLAYING:
                title = "Playing";
                color = Color.GREEN;
                break;
            case GAME_OVER:
                title = "Game Over";
                color = Color.RED;

                if (hitsplatStart == 0) {
                    hitsplatStart = System.currentTimeMillis();
                }
                else {
                    long timerDiff = System.currentTimeMillis() - hitsplatStart;
                    if (timerDiff <= 1000) {
                        renderHitsplat(graphics);
                    }
                }
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

    private void renderHitsplat(Graphics2D graphics) {
        BufferedImage image =  spriteManager.getSprite(SpriteID.HITSPLAT_GREEN_POISON, 0);
        Point playerLocation = Perspective.getCanvasImageLocation(
                client, client.getLocalPlayer().getLocalLocation(), image, 98);

        Point imageLocation = new Point(playerLocation.getX() - 5, playerLocation.getY() - 22);
        OverlayUtil.renderImageLocation(graphics, imageLocation, image);

        Point textLocation = new Point(playerLocation.getX() - 3, playerLocation.getY() - 3);
        OverlayUtil.renderTextLocation(graphics, textLocation, "99", Color.WHITE);
    }
}
