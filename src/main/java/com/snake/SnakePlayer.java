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
	private Player player;
	@Getter
	private String playerName;
	@Getter
	private Color color;

	@Getter
	private Queue<WorldPoint> snakeTrail = new ArrayDeque<>();

	public SnakePlayer(Player player)
	{
		this.player = player;

		previousLocation = player.getWorldLocation();
		isAlive = true;
		playerName = player.getName();

		for (int i = 0; i < INITIAL_TRAIL_SIZE; i++)
		{
			snakeTrail.add(previousLocation);
		}
	}

	public int getScore()
	{
		return snakeTrail.size() - INITIAL_TRAIL_SIZE;
	}

	public void updateSnakeTrail()
	{
		WorldPoint point = snakeTrail.poll();
		snakeTrail.add(player.getWorldLocation());
	}
}
