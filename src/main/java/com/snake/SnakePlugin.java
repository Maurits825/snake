package com.snake;

import com.google.inject.Provides;

import javax.inject.Inject;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.JagexColor;
import net.runelite.api.MenuAction;
import net.runelite.api.Model;
import net.runelite.api.ModelData;
import net.runelite.api.Player;
import net.runelite.api.RuneLiteObject;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameTick;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.events.OverlayMenuClicked;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.ui.overlay.OverlayMenuEntry;

import java.awt.Color;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ThreadLocalRandom;
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
	private SnakeOverlay overlay;

	@Inject
	private SnakeController snakeController;

	@Inject
	private SnakeView snakeView;

	enum State
	{
		INIT,
		WAITING_TO_START,
		PLAYING,
		GAME_OVER,
		RUN_ON,
	}

	@Getter
	private State currentState;

	private WorldPoint playerWorldPosition;
	private WorldPoint previousPlayerWorldPosition;
	private LocalPoint playerLocalPosition;

	private Queue<RuneLiteObject> snakeTrail = new ArrayDeque<>();
	private int initialTrailSize = 2;

	private WorldPoint wallStartPoint;
	private int gameSize;

	private RuneLiteObject foodObject;

	private int foodModelId = 2317;
	private int trailModelId = 29311;

	@Override
	protected void startUp() throws Exception
	{
		overlayManager.add(overlay);
		currentState = State.INIT;
	}

	@Override
	protected void shutDown() throws Exception
	{
		overlayManager.remove(overlay);
		clientThread.invokeLater(() ->
		{
			resetGame();
			return true;
		});
	}

	public Integer getScore()
	{
		return snakeTrail.size() - initialTrailSize;
	}

	@Subscribe
	public void onGameTick(GameTick tick)
	{
		snakeController.tick();
		snakeView.drawSnakeTrails(snakeController.getSnakePlayers());
	}

	private void oldOnGameTick()
	{
		previousPlayerWorldPosition = playerWorldPosition;
		playerWorldPosition = client.getLocalPlayer().getWorldLocation();
		playerLocalPosition = LocalPoint.fromWorld(client, playerWorldPosition);

		switch (currentState)
		{
			case INIT:
				initializeGame();
				break;
			case WAITING_TO_START:
				if (checkPlayerRunning())
				{
					currentState = State.RUN_ON;
				}
				else if (!previousPlayerWorldPosition.equals(playerWorldPosition))
				{
//					gameLoop();
					currentState = State.PLAYING;
				}
				break;
			case PLAYING:
//				gameLoop();
				break;
			case GAME_OVER:
			case RUN_ON:
				break;
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

//	private void gameLoop()
//	{
//		if (checkPlayerRunning())
//		{
//			currentState = State.RUN_ON;
//		}
//		else if (checkInvalidMovement())
//		{
//			Player localPlayer = client.getLocalPlayer();
//			localPlayer.setAnimation(2925);
//			localPlayer.setAnimationFrame(0);
//			localPlayer.setOverheadCycle(150);
//			localPlayer.setOverheadText("Game Over!");
//			currentState = State.GAME_OVER;
//		}
//		else if (playerLocalPosition.equals(foodObject.getLocation()))
//		{
//			snakeTrail.add(spawnNewSnakeTrailObject());
//			foodObject.setActive(false);
//			respawnFood();
//		}
//		else
//		{
//			//remove last and add it to the front trail
//			RuneLiteObject snakeTrailObj = snakeTrail.poll();
//			snakeTrailObj.setLocation(playerLocalPosition, client.getPlane());
//			snakeTrail.add(snakeTrailObj);
//		}
//	}

	private void createFoodObj()
	{
		foodObject = client.createRuneLiteObject();

		ModelData foodModel = client.loadModelData(foodModelId)
			.cloneVertices()
			.translate(0, 200, 0)
			.cloneColors();
		foodModel.recolor(foodModel.getFaceColors()[0],
			JagexColor.rgbToHSL(new Color(186, 16, 225).getRGB(), 1.0d));
		foodObject.setModel(foodModel.light());

		foodObject.setAnimation(client.loadAnimation(502));
		foodObject.setShouldLoop(true);
	}

	private void respawnFood()
	{
		LocalPoint lp = LocalPoint.fromWorld(client, getRandomPointInGrid());
		foodObject.setLocation(lp, client.getPlane());
		foodObject.setActive(true);
	}

	private WorldPoint getRandomPointInGrid()
	{
		WorldPoint randomPoint;
		do
		{
			int x = ThreadLocalRandom.current().nextInt(0, gameSize);
			int y = ThreadLocalRandom.current().nextInt(0, gameSize);
			randomPoint = wallStartPoint.dx(x + 1).dy(-(y + 1));
		} while (randomPoint.equals(playerWorldPosition));

		return randomPoint;
	}

	private boolean checkPlayerRunning()
	{
		return previousPlayerWorldPosition != null && playerLocalPosition != null &&
			previousPlayerWorldPosition.distanceTo(playerWorldPosition) > 1;
	}

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
		}
	}

	@Provides
	SnakeConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(SnakeConfig.class);
	}
}
