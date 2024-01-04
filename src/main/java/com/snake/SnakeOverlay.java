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

class SnakeOverlay extends OverlayPanel
{

	private final Client client;
	private final SnakePlugin plugin;
	private final SnakeController snakeController;

	@Inject
	SpriteManager spriteManager;

	private long hitsplatStart;

	@Inject
	SnakeOverlay(SnakePlugin plugin, Client client, SnakeController snakeController)
	{
		super(plugin);
		this.client = client;
		this.plugin = plugin;
		this.snakeController = snakeController;

		setPosition(OverlayPosition.TOP_LEFT);
		getMenuEntries().add(new OverlayMenuEntry(RUNELITE_OVERLAY, "Start", "new game"));
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		SnakeController.State currentState = snakeController.getCurrentState();
		String status = getStatusText(currentState);

		panelComponent.getChildren().add(TitleComponent.builder()
			.text("Snake: " + status)
			.color(Color.GREEN)
			.build());

		if (currentState == SnakeController.State.PLAYING || currentState == SnakeController.State.GAME_OVER)
		{
			panelComponent.getChildren().add(LineComponent.builder()
				.left("Scores")
				.build());

			for (SnakePlayer snakePlayer : snakeController.getSnakePlayers())
			{
				panelComponent.getChildren().add(LineComponent.builder()
					.left(snakePlayer.getPlayerName())
					.right(snakePlayer.isAlive() ? String.valueOf(snakePlayer.getScore()) : "Dead!")
					.build());
			}
		}
		else
		{
			panelComponent.getChildren().add(LineComponent.builder()
				.left("shift right-click to start a new game")
				.build());
		}

		return super.render(graphics);
	}

	private String getStatusText(SnakeController.State state)
	{
		switch (state)
		{
			case IDLE:
				return "Idle";
			case PLAYING:
				return "Playing";
			case GAME_OVER:
				return "Game Over";
			default:
				return "-";
		}
	}

	private void renderHitsplat(Graphics2D graphics)
	{
		BufferedImage image = spriteManager.getSprite(SpriteID.HITSPLAT_GREEN_POISON, 0);
		Point playerLocation = Perspective.getCanvasImageLocation(
			client, client.getLocalPlayer().getLocalLocation(), image, 98);

		Point imageLocation = new Point(playerLocation.getX() - 5, playerLocation.getY() - 22);
		OverlayUtil.renderImageLocation(graphics, imageLocation, image);

		Point textLocation = new Point(playerLocation.getX() - 3, playerLocation.getY() - 3);
		OverlayUtil.renderTextLocation(graphics, textLocation, "99", Color.WHITE);
	}
}
