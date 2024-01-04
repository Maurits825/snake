package com.snake;

import net.runelite.client.config.*;

@ConfigGroup("snakeConfig")
public interface SnakeConfig extends Config
{
	@ConfigItem(
		keyName = "gameSize",
		name = "Game size",
		description = "The size of the snake game."
	)
	@Range(min = 1)
	default int gameSize()
	{
		return 2;
	}

	@ConfigItem(
		keyName = "playerNames",
		name = "Player name",
		description = "RSN of the players, as comma separated list."
	)
	default String playerNames()
	{
		return "";
	}
}
