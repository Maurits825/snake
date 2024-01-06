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

	private final Client client;

	@Getter
	private State currentState = State.IDLE;

	@Getter
	private List<SnakePlayer> snakePlayers;
	@Getter
	private WorldPoint foodLocation;

	private WorldPoint wallStartPoint;
	private int gameSize;
	private boolean allowRun;
	private boolean isMultiplayer;

	private int readyCount;
	@Getter
	private int readyTickCountdown;
	private int deadCount;

	private Random generator;

	@Inject
	public SnakeController(Client client)
	{
		this.client = client;
	}

	public void initialize(List<String> playerNames, int gameSize, boolean allowRun, boolean isMultiplayer)
	{
		this.wallStartPoint = SnakeUtils.getWallStartPoint(client.getLocalPlayer().getWorldLocation(), gameSize);
		this.gameSize = gameSize;
		this.allowRun = allowRun;
		this.isMultiplayer = isMultiplayer;
		reset();

		List<Player> players = client.getPlayers();
		String currentPlayer = client.getLocalPlayer().getName();

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
		foodLocation = null;
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
				if (snakePlayer.getPlayerName().equals(playerName))
				{
					snakePlayer.setReady(true);
					readyCount++;
				}
			}
		}
	}

	private State waiting()
	{
		updateAllSnakeTrails();

		if (readyCount == snakePlayers.size())
		{
			readyTickCountdown = READY_COUNTDOWN_TICKS;
			return State.READY;
		}
		return currentState;
	}

	private State ready()
	{
		updateAllSnakeTrails();

		readyTickCountdown--;
		setAllOverheadText(String.valueOf(readyTickCountdown));
		if (readyTickCountdown == 0)
		{
			for (SnakePlayer snakePlayer : snakePlayers)
			{
				snakePlayer.fillInitialSnakeTrail();
			}
			setAllOverheadText("Go!");

			long seed = System.currentTimeMillis() / 5000;
			generator = new Random(seed);
			log.debug("Seed: " + seed);
			respawnFood();
			return State.PLAYING;
		}
		return currentState;
	}

	private State playing()
	{
		for (SnakePlayer snakePlayer : snakePlayers)
		{
			if (snakePlayer.isAlive())
			{
				boolean isAlive = checkValidMovement(snakePlayer);
				if (!isAlive)
				{
					snakePlayer.setAlive(false);
					deadCount++;
				}
			}
		}
		if (deadCount >= snakePlayers.size() - (isMultiplayer ? 1 : 0))
		{
			return State.GAME_OVER;
		}

		updatePlayersOnFood();
		updateAllSnakeTrails();

		return currentState;
	}

	private void updatePlayersOnFood()
	{
		List<SnakePlayer> onFoodPlayers = new ArrayList<>();
		for (SnakePlayer snakePlayer : snakePlayers)
		{
			if (snakePlayer.isAlive() && snakePlayer.getPlayer().getWorldLocation().equals(foodLocation))
			{
				onFoodPlayers.add(snakePlayer);
			}
		}

		if (onFoodPlayers.size() >= 1)
		{
			int randomIndex = generator.nextInt(onFoodPlayers.size());
			SnakePlayer snakePlayerGrow = onFoodPlayers.get(randomIndex);
			snakePlayerGrow.growSnakeTrail();
			snakePlayerGrow.setOverHeadText("+1");

			respawnFood();
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
		Player player = snakePlayer.getPlayer();

		WorldPoint playerWorldPosition = player.getWorldLocation();
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

		return true;//!checkCollision(snakePlayer); //TODO uncomment!!
	}

	private boolean checkCollision(SnakePlayer sPlayer)
	{
		WorldPoint playerLocation = sPlayer.getPlayer().getWorldLocation();
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

	private void respawnFood()
	{
		foodLocation = getRandomPointInGrid();
	}

	private WorldPoint getRandomPointInGrid()
	{
		WorldPoint randomPoint;
		do
		{
			int x = generator.nextInt(gameSize);
			int y = generator.nextInt(gameSize);
			randomPoint = wallStartPoint.dx(x + 1).dy(-(y + 1));
		} while (randomPoint.equals(foodLocation));

		return randomPoint;
	}
}
