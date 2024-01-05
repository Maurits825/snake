package com.snake;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.Range;

@ConfigGroup(SnakeConfig.GROUP)
public interface SnakeConfig extends Config
{
	String GROUP = "snakeConfig";

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
		keyName = "enableMultiplayer",
		name = "Enable multiplayer",
		description = "Enable multiplayer mode.",
		position = 97
	)
	default boolean enableMultiplayer()
	{
		return false;
	}

	@ConfigItem(
		keyName = "playerNames",
		name = "Player names",
		description = "RSN of the players, as comma separated list.",
		position = 98
	)
	default String playerNames()
	{
		return "";
	}


	@ConfigItem(
		keyName = "addPlayerMenuEntry",
		name = "Show add player menu",
		description = "Show add player menu on players.",
		position = 99
	)
	default boolean addPlayerMenuEntry()
	{
		return false;
	}
}
