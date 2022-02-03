package com.snake;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("snake")
public interface SnakeConfig extends Config
{
	String GROUP = "snakeConfig";

	@ConfigItem(
		keyName = "gameSize",
		name = "Game size",
		description = "The size of the snake game."
	)
	default Integer gameSize()
	{
		return 5;
	}
}
