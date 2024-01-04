package com.snake;

import net.runelite.api.coords.WorldPoint;

public class SnakeUtils
{
	public static WorldPoint getWallStartPoint(WorldPoint playerWorldPosition, int gameSize)
	{
		int offset = (int) Math.ceil(gameSize / 2.0f);
		return playerWorldPosition.dx(-offset).dy(offset);
	}
}
