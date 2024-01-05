package com.snake;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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
		PLAYING,
		GAME_OVER,
	}

	private static final List<Color> PLAYER_COLORS = Arrays.asList(
		Color.BLUE, Color.YELLOW, Color.MAGENTA, Color.CYAN, Color.RED
	);

	private final Client client;

	@Getter
	private State currentState = State.IDLE;

	@Getter
	private List<SnakePlayer> snakePlayers;
	private int readyCount;

	private WorldPoint wallStartPoint;
	private int gameSize;

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

		this.currentState = State.WAITING_TO_START;
	}

	public void reset()
	{
		snakePlayers = new ArrayList<>();
		readyCount = 0;
		this.currentState = State.IDLE;
	}

	public void tick()
	{
		switch (currentState)
		{
			case IDLE:
				break;
			case WAITING_TO_START:
				if (readyCount == snakePlayers.size())
				{
					//TODO maybe have a countdown state or something here
					currentState = State.PLAYING;
				}
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

	private void playing()
	{
		//first set alive status
		for (SnakePlayer snakePlayer : snakePlayers)
		{
			if (snakePlayer.isAlive())
			{
				snakePlayer.setAlive(checkValidMovement(snakePlayer));
			}
		}

		//second update trail for alive players
		for (SnakePlayer snakePlayer : snakePlayers)
		{
			if (snakePlayer.isAlive())
			{
				snakePlayer.updateSnakeTrail();
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
}
