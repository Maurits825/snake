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

	public void growSnakeTrail()
	{
		snakeTrail.add(player.getWorldLocation());
	}

	public void moveSnakeTrail()
	{
		snakeTrail.poll();
		snakeTrail.add(player.getWorldLocation());
	}
}
