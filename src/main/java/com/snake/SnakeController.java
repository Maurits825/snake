package com.snake;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
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

	private int readyCount;
	@Getter
	private int readyTickCountdown;

	private Random generator;

	@Inject
	public SnakeController(Client client)
	{
		this.client = client;
	}

	public void initialize(int gameSize, List<String> playerNames)
	{
		this.wallStartPoint = SnakeUtils.getWallStartPoint(client.getLocalPlayer().getWorldLocation(), gameSize);
		this.gameSize = gameSize;

		reset();

		List<Player> players = client.getPlayers();
		String currentPlayer = client.getLocalPlayer().getName();

		int colorIndex = 0;
		for (String playerName : playerNames)
		{
			Player player = SnakeUtils.findPlayer(players, playerName);
			if (player != null)
			{
				boolean isActivePlayer = playerName.equals(currentPlayer);
				Color color = isActivePlayer ? Color.GREEN : PLAYER_COLORS.get(colorIndex);
				snakePlayers.add(new SnakePlayer(player, color, isActivePlayer));
			}

			colorIndex = (colorIndex + 1) % PLAYER_COLORS.size();
		}

		if (snakePlayers.size() == 1)
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
		foodLocation = null;
		this.currentState = State.IDLE;
	}

	public void tick()
	{
		switch (currentState)
		{
			case IDLE:
				break;
			case WAITING_TO_START:
				waiting();
				break;
			case READY:
				ready();
				break;
			case PLAYING:
				playing();
				break;
			case GAME_OVER:
				break;
		}
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

	private void waiting()
	{
		updateAllSnakeTrails();

		if (readyCount == snakePlayers.size())
		{
			readyTickCountdown = READY_COUNTDOWN_TICKS;
			currentState = State.READY;
		}
	}

	private void ready()
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

			generator = new Random(System.currentTimeMillis());
			respawnFood();
			currentState = State.PLAYING;
		}
	}

	private void playing()
	{
		for (SnakePlayer snakePlayer : snakePlayers)
		{
			if (snakePlayer.isAlive())
			{
				snakePlayer.setAlive(checkValidMovement(snakePlayer));
			}
		}

		updatePlayersOnFood();
		updateAllSnakeTrails();
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
		if (player == null || player.getName() == null)
		{
			return false;
		}

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

		//TODO check trail collision

		return true;
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
