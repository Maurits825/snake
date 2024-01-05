package com.snake;

import java.awt.Color;
import java.util.ArrayDeque;
import java.util.Queue;
import lombok.Getter;
import lombok.Setter;
import net.runelite.api.Player;
import net.runelite.api.coords.WorldPoint;

public class SnakePlayer
{
	private static final int INITIAL_TRAIL_SIZE = 2;

	private WorldPoint previousLocation;

	@Getter
	@Setter
	private boolean isAlive;
	@Getter
	@Setter
	private boolean isReady;

	@Getter
	private final Player player;
	@Getter
	private final String playerName;
	@Getter
	private final Color color;
	@Getter
	private final boolean isActivePlayer;

	@Getter
	private Queue<WorldPoint> snakeTrail = new ArrayDeque<>();

	public SnakePlayer(Player player, Color color, boolean isActivePlayer)
	{
		this.player = player;
		this.color = color;
		this.isActivePlayer = isActivePlayer;

		previousLocation = player.getWorldLocation();
		isAlive = true;
		playerName = player.getName();
		isReady = false;

		snakeTrail.add(previousLocation);
	}

	public int getScore()
	{
		return Math.max(0, snakeTrail.size() - INITIAL_TRAIL_SIZE);
	}

	public void fillInitialSnakeTrail()
	{
		for (int i = 0; i < INITIAL_TRAIL_SIZE - 1; i++)
		{
			snakeTrail.add(previousLocation);
		}
	}

	public void updateSnakeTrail()
	{
		WorldPoint point = snakeTrail.poll();
		snakeTrail.add(player.getWorldLocation());
	}
}
