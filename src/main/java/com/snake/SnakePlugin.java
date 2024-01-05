package com.snake;

import com.google.inject.Provides;

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

import net.runelite.client.util.Text;

@Slf4j
@PluginDescriptor(
	name = "Snake"
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

	private static final String ADD_PLAYER_MENU = "Add snake player";

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
		snakeView.drawSnakeTrails(snakeController.getSnakePlayers());
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

		snakeController.initialize(getGameSize(), Text.fromCSV(config.playerNames()));
		snakeView.drawWalls(getGameSize());
	}

	private void resetGame()
	{
		snakeController.reset();
		snakeView.clearAll();
	}

//	private void respawnFood()
//	{
//		LocalPoint lp = LocalPoint.fromWorld(client, getRandomPointInGrid());
//		foodObject.setLocation(lp, client.getPlane());
//		foodObject.setActive(true);
//	}
//
//	private WorldPoint getRandomPointInGrid()
//	{
//		WorldPoint randomPoint;
//		do
//		{
//			int x = ThreadLocalRandom.current().nextInt(0, gameSize);
//			int y = ThreadLocalRandom.current().nextInt(0, gameSize);
//			randomPoint = wallStartPoint.dx(x + 1).dy(-(y + 1));
//		} while (randomPoint.equals(playerWorldPosition));
//
//		return randomPoint;
//	}
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
