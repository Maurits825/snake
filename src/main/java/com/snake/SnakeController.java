package com.snake;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.api.coords.WorldPoint;

@Singleton
@Slf4j
public class SnakeController
{
	private final Client client;

	enum State
	{
		IDLE,
		WAITING_TO_START,
		PLAYING,
		GAME_OVER,
	}

	@Getter
	private State currentState = State.IDLE;

	@Getter
	private List<SnakePlayer> snakePlayers;

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

		snakePlayers = new ArrayList<>();

		List<Player> players = client.getPlayers();
		String currentPlayer = client.getLocalPlayer().getName();

		int colorIndex = 0;
		for (String playerName : playerNames)
		{
			Player player = SnakeUtils.findPlayer(players, playerName);
			if (player != null)
			{
				Color color = playerName.equals(currentPlayer) ? Color.GREEN : SnakeUtils.PLAYER_COLORS.get(colorIndex);
				snakePlayers.add(new SnakePlayer(player, color));
			}

			colorIndex = (colorIndex + 1) % SnakeUtils.PLAYER_COLORS.size();
		}

		this.currentState = State.PLAYING; //TODO figure out later how to sync start for everyone
	}

	public void reset()
	{
		snakePlayers = new ArrayList<>();
		this.currentState = State.IDLE;
	}

	public void tick()
	{
		switch (currentState)
		{
			case IDLE:
				break;
			case WAITING_TO_START:
				break;
			case PLAYING:
				playing();
				break;
			case GAME_OVER:
				break;
		}
	}

	public void playing()
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

	//TODO this has to be on controller or model???
	private boolean checkValidMovement(SnakePlayer snakePlayer)
	{
		WorldPoint playerWorldPosition = snakePlayer.getPlayer().getWorldLocation();
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
