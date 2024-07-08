package com.snake;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.TreeSet;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;

@Slf4j
@Singleton
public class SnakeController
{
	public static final String READY_MESSAGE = "r";

	public enum State
	{
		IDLE,
		WAITING_TO_START,
		READY,
		PLAYING,
		GAME_OVER,
	}

	private static final List<Color> PLAYER_COLORS = Arrays.asList(
		Color.BLUE, Color.YELLOW, Color.MAGENTA, Color.CYAN, Color.RED
	);
	private static final int READY_COUNTDOWN_TICKS = 5;
	private static final int MAX_RANDOM_POINT_TRIES = 100;

	private final Client client;

	@Getter
	private State currentState = State.IDLE;

	@Getter
	private List<SnakePlayer> snakePlayers;
	@Getter
	private boolean[][] walkableTiles;

	private WorldPoint wallStartPoint;
	private int gameSize;
	private boolean allowRun;
	private boolean isSameFoodSpawn;

	private int readyCount;
	@Getter
	private int readyTickCountdown;
	private int deadCount;
	private int gameOverDeadCount;

	private Random generator;

	@Inject
	public SnakeController(Client client)
	{
		this.client = client;
	}

	public void initialize(List<String> playerNames, int gameSize, boolean allowRun, boolean isMultiplayer, boolean isSameFoodSpawn, int seed)
	{
		this.wallStartPoint = SnakeUtils.getWallStartPoint(client.getLocalPlayer().getWorldLocation(), gameSize);
		this.gameSize = gameSize;
		this.allowRun = allowRun;
		this.isSameFoodSpawn = isSameFoodSpawn;
		generator = new Random(isMultiplayer ? seed : System.nanoTime());

		reset();

		List<Player> players = client.getPlayers();
		String currentPlayer = client.getLocalPlayer().getName();
		walkableTiles = getWalkableTiles(wallStartPoint.dx(1).dy(-1));

		int colorIndex = 0;
		TreeSet<String> uniquePlayerNames = new TreeSet<>(playerNames);
		for (String playerName : uniquePlayerNames)
		{
			Player player = SnakeUtils.findPlayer(players, playerName);
			if (player != null)
			{
				boolean isActivePlayer = playerName.equals(currentPlayer);
				Color color = PLAYER_COLORS.get(colorIndex);
				if (isActivePlayer)
				{
					color = Color.GREEN;
				}
				snakePlayers.add(new SnakePlayer(player, color, isActivePlayer));
				colorIndex = (colorIndex + 1) % PLAYER_COLORS.size();
			}
		}

		gameOverDeadCount = snakePlayers.size() - (isMultiplayer && snakePlayers.size() != 1 ? 1 : 0);

		if (!isMultiplayer)
		{
			snakePlayers.get(0).setReady(true);
			readyTickCountdown = READY_COUNTDOWN_TICKS;
			currentState = State.READY;
		}
		else
		{
			currentState = State.WAITING_TO_START;
		}
	}

	public void reset()
	{
		snakePlayers = new ArrayList<>();
		readyCount = 0;
		readyTickCountdown = 0;
		deadCount = 0;
		this.currentState = State.IDLE;
	}

	public void tick()
	{
		State nextState = currentState;
		switch (currentState)
		{
			case WAITING_TO_START:
				nextState = waiting();
				break;
			case READY:
				nextState = ready();
				break;
			case PLAYING:
				nextState = playing();
				break;
			case IDLE:
			case GAME_OVER:
				break;
		}
		currentState = nextState;
	}

	public void handleChatMessage(String playerName, String message)
	{
		if (currentState == State.WAITING_TO_START && message.equals(READY_MESSAGE))
		{
			for (SnakePlayer snakePlayer : snakePlayers)
			{
				if (snakePlayer.getPlayerName().equals(playerName) && !snakePlayer.isReady())
				{
					snakePlayer.setReady(true);
					readyCount++;
				}
			}
		}
	}

	private State waiting()
	{
		for (SnakePlayer snakePlayer : snakePlayers)
		{
			snakePlayer.updateLocation();
			snakePlayer.moveSnakeTrail();
		}

		if (readyCount == snakePlayers.size())
		{
			readyTickCountdown = READY_COUNTDOWN_TICKS;
			return State.READY;
		}
		return currentState;
	}

	private State ready()
	{
		for (SnakePlayer snakePlayer : snakePlayers)
		{
			snakePlayer.updateLocation();
			snakePlayer.moveSnakeTrail();
		}

		readyTickCountdown--;
		setAllOverheadText(String.valueOf(readyTickCountdown));
		if (readyTickCountdown == 0)
		{
			for (SnakePlayer snakePlayer : snakePlayers)
			{
				snakePlayer.fillInitialSnakeTrail();
			}
			setAllOverheadText("Go!");

			respawnAllFood();
			return State.PLAYING;
		}
		return currentState;
	}

	private State playing()
	{
		updateAllPlayers();

		if (deadCount >= gameOverDeadCount)
		{
			return State.GAME_OVER;
		}

		updatePlayersOnFood();
		updateAllSnakeTrails();

		return currentState;
	}

	private void updateAllPlayers()
	{
		for (SnakePlayer snakePlayer : snakePlayers)
		{
			if (snakePlayer.isAlive())
			{
				snakePlayer.updateLocation();
				boolean isAlive = checkValidMovement(snakePlayer);
				if (!isAlive)
				{
					snakePlayer.setAlive(false);
					deadCount++;
				}
			}
		}
	}

	private void updatePlayersOnFood()
	{
		List<SnakePlayer> onFoodPlayers = new ArrayList<>();
		for (SnakePlayer snakePlayer : snakePlayers)
		{
			if (snakePlayer.isAlive() && snakePlayer.getCurrentLocation().equals(snakePlayer.getFoodLocation()))
			{
				onFoodPlayers.add(snakePlayer);
			}
		}

		if (onFoodPlayers.size() >= 1)
		{
			if (isSameFoodSpawn)
			{
				int randomIndex = generator.nextInt(onFoodPlayers.size());
				SnakePlayer snakePlayerGrow = onFoodPlayers.get(randomIndex);
				snakePlayerGrow.setShouldGrow(true);
				snakePlayerGrow.increaseScore();
				snakePlayerGrow.setOverHeadText("+1");
				respawnAllFood();
			}
			else
			{
				for (SnakePlayer snakePlayerGrow : onFoodPlayers)
				{
					snakePlayerGrow.setShouldGrow(true);
					snakePlayerGrow.setOverHeadText("+1");
					snakePlayerGrow.increaseScore();
					snakePlayerGrow.setFoodLocation(getRandomPointInGrid());
				}
			}
		}
	}

	private void updateAllSnakeTrails()
	{
		for (SnakePlayer snakePlayer : snakePlayers)
		{
			if (snakePlayer.isAlive())
			{
				snakePlayer.moveSnakeTrail();
			}
		}
	}

	private boolean checkValidMovement(SnakePlayer snakePlayer)
	{
		WorldPoint playerWorldPosition = snakePlayer.getCurrentLocation();
		boolean inGameBoundary =
			playerWorldPosition.getX() > wallStartPoint.getX() &&
				playerWorldPosition.getX() <= (wallStartPoint.getX() + gameSize) &&
				playerWorldPosition.getY() < wallStartPoint.getY() &&
				playerWorldPosition.getY() >= (wallStartPoint.getY() - gameSize);

		if (!inGameBoundary)
		{
			return false;
		}

		if (!allowRun && snakePlayer.isRunning())
		{
			return false;
		}

		return !checkCollision(snakePlayer);
	}

	private boolean checkCollision(SnakePlayer sPlayer)
	{
		WorldPoint playerLocation = sPlayer.getCurrentLocation();
		for (SnakePlayer snakePlayer : snakePlayers)
		{
			for (WorldPoint trailPoint : snakePlayer.getSnakeTrail())
			{
				if (trailPoint.equals(playerLocation))
				{
					return true;
				}
			}
		}
		return false;
	}

	private void setAllOverheadText(String text)
	{
		for (SnakePlayer snakePlayer : snakePlayers)
		{
			snakePlayer.setOverHeadText(text);
		}
	}

	private void respawnAllFood()
	{
		if (isSameFoodSpawn)
		{
			WorldPoint foodLocation = getRandomPointInGrid();
			for (SnakePlayer snakePlayer : snakePlayers)
			{
				snakePlayer.setFoodLocation(foodLocation);
			}
		}
		else
		{
			for (SnakePlayer snakePlayer : snakePlayers)
			{
				snakePlayer.setFoodLocation(getRandomPointInGrid());
			}
		}
	}

	private WorldPoint getRandomPointInGrid()
	{
		WorldPoint randomPoint;
		int x;
		int y;
		int count = 0;
		do
		{
			x = generator.nextInt(gameSize);
			y = generator.nextInt(gameSize);
			randomPoint = wallStartPoint.dx(x + 1).dy(-(y + 1));
			count++;
		} while (count < MAX_RANDOM_POINT_TRIES && !isFoodSpawnValid(randomPoint, x, y));

		return randomPoint;
	}

	private boolean isFoodSpawnValid(WorldPoint point, int x, int y)
	{
		if (!walkableTiles[x][y])
		{
			return false;
		}

		for (SnakePlayer snakePlayer : snakePlayers)
		{
			if (point.equals(snakePlayer.getFoodLocation()))
			{
				return false;
			}
		}
		return true;
	}

	private boolean[][] getWalkableTiles(WorldPoint gridStart)
	{
		boolean[][] walkable = new boolean[gameSize][gameSize];

		int[][] flags = client.getCollisionMaps()[client.getPlane()].getFlags();
		LocalPoint gridStartInScene = SnakeUtils.getWorldPointLocationInScene(client, gridStart);
		for (int x = 0; x < gameSize; ++x)
		{
			for (int y = 0; y < gameSize; ++y)
			{
				int data = flags[x + gridStartInScene.getX()][gridStartInScene.getY() - y];
				walkable[x][y] = data == 0;
			}
		}
		return walkable;
	}
}
