package com.snake;

import java.awt.Color;
import java.util.ArrayDeque;
import java.util.Queue;
import lombok.Getter;
import lombok.Setter;
import net.runelite.api.Player;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.util.ColorUtil;

public class SnakePlayer
{
	private static final int INITIAL_TRAIL_SIZE = 2;

	@Getter
	private WorldPoint currentLocation;
	private WorldPoint previousLocation;

	@Getter
	private boolean isAlive;
	@Getter
	@Setter
	private boolean isReady;

	private final Player player;
	@Getter
	private final String playerName;
	@Getter
	private final Color color;
	@Getter
	private final boolean isActivePlayer;

	@Setter
	private boolean shouldGrow;

	@Getter
	private final Queue<WorldPoint> snakeTrail = new ArrayDeque<>();
	@Getter
	@Setter
	private WorldPoint foodLocation;

	@Getter
	@Setter
	private int score;

	public SnakePlayer(Player player, Color color, boolean isActivePlayer)
	{
		this.player = player;
		this.color = color;
		this.isActivePlayer = isActivePlayer;
		this.score = INITIAL_TRAIL_SIZE;

		currentLocation = player.getWorldLocation();
		previousLocation = currentLocation;
		isAlive = true;
		playerName = player.getName();
		isReady = false;

		shouldGrow = false;

		snakeTrail.add(currentLocation);
		foodLocation = null;
	}

    public void setOverHeadText(String text)
	{
		setOverHeadText(text, 50);
	}

	public void setOverHeadText(String text, int duration)
	{
		player.setOverheadCycle(duration);
		player.setOverheadText(ColorUtil.wrapWithColorTag(text, color));
	}

	public void fillInitialSnakeTrail()
	{
		for (int i = 0; i < INITIAL_TRAIL_SIZE - 1; i++)
		{
			snakeTrail.add(player.getWorldLocation());
		}
	}

	public void increaseScore()
	{
		score++;
	}

	public void updateLocation()
	{
		previousLocation = currentLocation;
		currentLocation = player.getWorldLocation();
	}

	public void moveSnakeTrail()
	{
		if (shouldGrow)
		{
			snakeTrail.add(currentLocation);
			shouldGrow = false;
			increaseScore();
		}
		else
		{
			snakeTrail.poll();
			snakeTrail.add(currentLocation);
		}
	}

	public void setAlive(boolean isAlive)
	{
		this.isAlive = isAlive;
		if (!isAlive)
		{
			setOverHeadText("Game Over!");
			player.setAnimation(2925);
			player.setAnimationFrame(0);
			snakeTrail.clear();
		}
	}

	public boolean isRunning()
	{
		return previousLocation.distanceTo(currentLocation) > 1;
	}
}
