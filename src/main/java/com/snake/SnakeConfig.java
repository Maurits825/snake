package com.snake;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Range;

@ConfigGroup(SnakeConfig.GROUP)
public interface SnakeConfig extends Config
{
	String GROUP = "snakeConfig";

	@ConfigItem(
		keyName = "gameSize",
		name = "Game size",
		description = "The size of the game.",
		position = 0
	)
	@Range(min = 1, max = 30)
	default int gameSize()
	{
		return 2;
	}

	@ConfigItem(
		keyName = "allowRun",
		name = "Allow run",
		description = "Allow running.",
		position = 1
	)
	default boolean allowRun()
	{
		return false;
	}

	@ConfigItem(
		keyName = "gridTheme",
		name = "Grid theme",
		description = "The theme of the grid.",
		position = 2
	)
	default SnakeGridTheme gridTheme()
	{
		return SnakeGridTheme.ORIGINAL;
	}

	@ConfigSection(
		name = "Multiplayer options",
		description = "Refer to the readme via support link for more info.",
		position = 3
	)
	String multiplayerOptionsSection = "multiplayerOptionsSection";

	@ConfigItem(
		keyName = "enableMultiplayer",
		name = "Enable multiplayer",
		description = "Enable multiplayer mode.",
		position = 0,
		section = multiplayerOptionsSection
	)
	default boolean enableMultiplayer()
	{
		return false;
	}

	@ConfigItem(
		keyName = "seed",
		name = "Seed",
		description = "The seed used to randomize spawn locations.",
		position = 1,
		section = multiplayerOptionsSection
	)
	default int seed()
	{
		return 1;
	}

	@ConfigItem(
		keyName = "isSameFoodSpawn",
		name = "Same food spawn",
		description = "If enabled all players have the same food spawn location, otherwise it is per player.",
		position = 2,
		section = multiplayerOptionsSection
	)
	default boolean isSameFoodSpawn()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showAllFood",
		name = "Show all food",
		description = "Show all food spawns if same food spawn is enabled.",
		position = 3,
		section = multiplayerOptionsSection
	)
	default boolean showAllFood()
	{
		return false;
	}

	@ConfigItem(
		keyName = "playerNames",
		name = "Player names",
		description = "RSN of all the players participating, as comma separated list.",
		position = 4,
		section = multiplayerOptionsSection
	)
	default String playerNames()
	{
		return "";
	}

	@ConfigItem(
		keyName = "addPlayerMenuEntry",
		name = "Show add player menu",
		description = "Show add player menu on players.",
		position = 5,
		section = multiplayerOptionsSection
	)
	default boolean addPlayerMenuEntry()
	{
		return false;
	}
}
