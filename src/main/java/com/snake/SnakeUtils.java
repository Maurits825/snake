package com.snake;

import java.util.List;
import net.runelite.api.Player;
import net.runelite.api.coords.WorldPoint;

public class SnakeUtils
{
	public static WorldPoint getWallStartPoint(WorldPoint playerWorldPosition, int gameSize)
	{
		int offset = (int) Math.ceil(gameSize / 2.0f);
		return playerWorldPosition.dx(-offset).dy(offset);
	}

	public static Player findPlayer(List<Player> players, String name)
	{
		for (Player player : players)
		{
			if (name.equals(player.getName()))
			{
				return player;
			}
		}
		return null;
	}
}
