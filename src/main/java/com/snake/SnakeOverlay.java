package com.snake;

import static com.snake.SnakeController.READY_MESSAGE;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import javax.inject.Inject;
import static net.runelite.api.MenuAction.RUNELITE_OVERLAY;
import static net.runelite.api.MenuAction.RUNELITE_OVERLAY_CONFIG;
import static net.runelite.client.ui.overlay.OverlayManager.OPTION_CONFIGURE;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

class SnakeOverlay extends OverlayPanel
{

	private final SnakeController snakeController;

	@Inject
	SnakeOverlay(SnakePlugin plugin, SnakeController snakeController)
	{
		super(plugin);
		this.snakeController = snakeController;

		setPosition(OverlayPosition.TOP_LEFT);
		addMenuEntry(RUNELITE_OVERLAY, "Start", "new game");
		addMenuEntry(RUNELITE_OVERLAY_CONFIG, OPTION_CONFIGURE, "Snake");
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		SnakeController.State currentState = snakeController.getCurrentState();
		String status = getStatusText(currentState);

		panelComponent.getChildren().add(TitleComponent.builder()
			.text("Snake - " + status)
			.build());

		if (currentState == SnakeController.State.WAITING_TO_START ||
			currentState == SnakeController.State.READY ||
			currentState == SnakeController.State.PLAYING ||
			currentState == SnakeController.State.GAME_OVER)
		{
			buildScoreOverlay(currentState);
		}
		else
		{
			panelComponent.getChildren().add(LineComponent.builder()
				.left("shift right-click to start a new game")
				.build());
		}

		return super.render(graphics);
	}

	private void buildScoreOverlay(SnakeController.State currentState)
	{
		panelComponent.getChildren().add(LineComponent.builder()
			.left("Score")
			.build());

		for (SnakePlayer snakePlayer : snakeController.getSnakePlayers())
		{
			String rightText = (currentState == SnakeController.State.GAME_OVER ? "Win: " : "") + snakePlayer.getScore();
			if (currentState == SnakeController.State.WAITING_TO_START)
			{
				rightText = snakePlayer.isReady() ? "R" : "-";
			}
			else if (!snakePlayer.isAlive())
			{
				rightText = "Dead: " + snakePlayer.getScore();
			}

			panelComponent.getChildren().add(LineComponent.builder()
				.left(snakePlayer.getPlayerName())
				.leftColor(snakePlayer.isAlive() ? snakePlayer.getColor() : Color.DARK_GRAY)
				.right(rightText)
				.build());

			if (!snakePlayer.isReady() && snakePlayer.isActivePlayer())
			{
				panelComponent.getChildren().add(LineComponent.builder()
					.left("Type " + READY_MESSAGE + " in chat to ready!")
					.leftColor(Color.RED)
					.build());
			}
		}

		if (currentState == SnakeController.State.READY)
		{
			int tickCountDown = snakeController.getReadyTickCountdown();
			panelComponent.getChildren().add(LineComponent.builder()
				.left(tickCountDown == 0 ? "Go!" : "Starting in " + snakeController.getReadyTickCountdown())
				.build());
		}
	}

	private String getStatusText(SnakeController.State state)
	{
		switch (state)
		{
			case IDLE:
				return "Idle";
			case WAITING_TO_START:
				return "Waiting";
			case READY:
				return "Get Ready!";
			case PLAYING:
				return "Playing";
			case GAME_OVER:
				return "Game Over";
			default:
				return "-";
		}
	}
}
