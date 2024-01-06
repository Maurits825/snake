package com.snake;

import com.google.inject.Provides;
import java.awt.Color;
import java.util.Collections;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Provider;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.MenuAction;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.events.OverlayMenuClicked;
import net.runelite.client.menus.MenuManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.ui.overlay.OverlayMenuEntry;
import net.runelite.client.util.ColorUtil;
import net.runelite.client.util.Text;

@Slf4j
@PluginDescriptor(
	name = "Snake",
	description = "Play snake in game!"
)
public class SnakePlugin extends Plugin
{
	@Inject
	private Client client;

	@Inject
	private SnakeConfig config;

	@Inject
	private ClientThread clientThread;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private Provider<MenuManager> menuManager;

	@Inject
	private ConfigManager configManager;

	@Inject
	private SnakeOverlay overlay;

	@Inject
	private SnakeController snakeController;

	@Inject
	private SnakeView snakeView;

	private static final String ADD_PLAYER_MENU = ColorUtil.wrapWithColorTag("Add snake player", Color.GREEN);

	@Override
	protected void startUp()
	{
		overlayManager.add(overlay);

		if (config.addPlayerMenuEntry() && client != null)
		{
			menuManager.get().addPlayerMenuItem(ADD_PLAYER_MENU);
		}
	}

	@Override
	protected void shutDown()
	{
		overlayManager.remove(overlay);
		clientThread.invokeLater(() ->
		{
			resetGame();
			return true;
		});

		if (client != null)
		{
			menuManager.get().removePlayerMenuItem(ADD_PLAYER_MENU);
		}
	}

	@Subscribe
	public void onGameTick(GameTick tick)
	{
		snakeController.tick();
		snakeView.update(snakeController.getFoodLocation());
	}

	@Subscribe
	public void onMenuOptionClicked(MenuOptionClicked event)
	{
		if (event.getMenuAction() == MenuAction.RUNELITE_PLAYER && event.getMenuOption().equals(ADD_PLAYER_MENU))
		{
			String playerName = event.getMenuEntry().getPlayer().getName();
			String newPlayerNames = config.playerNames() + "," + playerName;
			configManager.setConfiguration(SnakeConfig.GROUP, "playerNames", newPlayerNames);
		}
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged gameStateChanged)
	{
		if (gameStateChanged.getGameState() == GameState.LOGGED_IN)
		{
			resetGame();
		}
	}

	@Subscribe
	public void onChatMessage(ChatMessage event)
	{
		if (event.getType() == ChatMessageType.PUBLICCHAT)
		{
			String playerName = event.getName();
			String message = Text.sanitize(Text.removeTags(event.getMessage())).toLowerCase();
			snakeController.handleChatMessage(playerName, message);
		}
	}

	private void initializeGame()
	{
		resetGame();

		List<String> playerNames = config.enableMultiplayer() ?
			Text.fromCSV(config.playerNames()) : Collections.singletonList(client.getLocalPlayer().getName());
		snakeController.initialize(getGameSize(), playerNames);
		snakeView.initialize(snakeController.getSnakePlayers(), getGameSize(), config.gridTheme());
	}

	private void resetGame()
	{
		snakeController.reset();
		snakeView.reset();
	}


//
//	private boolean checkPlayerRunning()
//	{
//		return previousPlayerWorldPosition != null && playerLocalPosition != null &&
//			previousPlayerWorldPosition.distanceTo(playerWorldPosition) > 1;
//	}

	private int getGameSize()
	{
		return 1 + 2 * config.gameSize();
	}

	@Subscribe
	public void onOverlayMenuClicked(OverlayMenuClicked overlayMenuClicked)
	{
		OverlayMenuEntry overlayMenuEntry = overlayMenuClicked.getEntry();
		if (overlayMenuEntry.getMenuAction() == MenuAction.RUNELITE_OVERLAY
			&& overlayMenuClicked.getEntry().getOption().equals("Start")
			&& overlayMenuClicked.getOverlay() == overlay)
		{
			initializeGame();
		}
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged configChanged)
	{
		if (configChanged.getGroup().equals("snakeConfig"))
		{
			clientThread.invokeLater(() ->
			{
				resetGame();
				return true;
			});

			if (client != null)
			{
				menuManager.get().removePlayerMenuItem(ADD_PLAYER_MENU);

				if (config.addPlayerMenuEntry())
				{
					menuManager.get().addPlayerMenuItem(ADD_PLAYER_MENU);
				}
			}
		}
	}

	@Provides
	SnakeConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(SnakeConfig.class);
	}
}
