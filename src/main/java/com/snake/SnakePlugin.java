package com.snake;

import com.google.inject.Provides;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameTick;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

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

	enum State {
		INIT,
		PLAYING,
		RESET,
	}

	private State currentState;

	private WorldPoint playerPosition;

	private Queue<RuneLiteObject> snakeTrail = new ArrayDeque<>();
	private Integer gridSize = 5;
	private List<RuneLiteObject> walls = new ArrayList<>();
	private WorldPoint wallStartPoint;

	private RuneLiteObject foodObject;

	@Override
	protected void startUp() throws Exception
	{
		currentState = State.INIT;
		//todo move to config later
		gridSize = 5;
	}

	@Override
	protected void shutDown() throws Exception
	{
		currentState = State.INIT;
		clientThread.invokeLater(() ->
		{
			resetGame();
			return true;
		});
	}

	@Subscribe
	public void onGameTick(GameTick tick) {
		playerPosition = client.getLocalPlayer().getWorldLocation();

		switch (currentState) {
			case INIT:
				clearSnakeTrail();
				//start with trail size 2
				snakeTrail.add(spawnNewSnakeTrailObject());
				snakeTrail.add(spawnNewSnakeTrailObject());

				drawWalls();

				createFoodObj();
				reSpawnFood();

				currentState = State.PLAYING;
				break;
			case PLAYING:
				GameLoop();
				break;
		}
	}

	private void GameLoop() {
		//check if food item consumed
		LocalPoint playerLocalPosition = LocalPoint.fromWorld(client, playerPosition);
		if (playerLocalPosition.equals(foodObject.getLocation())) {
			snakeTrail.add(spawnNewSnakeTrailObject());
			reSpawnFood();
		}
		//check collisions later
		else {
			//remove last and add it to the front trail
			RuneLiteObject snakeTrailObj = snakeTrail.poll();
			snakeTrailObj.setLocation(playerLocalPosition, client.getPlane());
			snakeTrail.add(snakeTrailObj);
		}
	}

	private void resetGame() {
		clearSnakeTrail();
		clearWalls();

		foodObject.setActive(false);
	}

	private void clearSnakeTrail() {
		for (RuneLiteObject obj : snakeTrail) {
			obj.setActive(false);
		}
		snakeTrail.clear();
	}

	private RuneLiteObject spawnNewSnakeTrailObject() {
		RuneLiteObject obj = client.createRuneLiteObject();
		Model trailModel = client.loadModel(35394); //todo make this a var?
		obj.setModel(trailModel);

		LocalPoint lp = LocalPoint.fromWorld(client, playerPosition);
		obj.setLocation(lp, client.getPlane());

		obj.setActive(true);
		return obj;
	}

	private void createFoodObj() {
		foodObject = client.createRuneLiteObject();

		//todo make this id a var?
		Model food = client.loadModel(26003);
		foodObject.setModel(food);
	}

	private void reSpawnFood() {
		LocalPoint lp = LocalPoint.fromWorld(client, getRandomPointInGrid());
		foodObject.setLocation(lp, client.getPlane());
		foodObject.setActive(true);
	}

	private void clearWalls() {
		for (RuneLiteObject obj : walls) {
			obj.setActive(false);
		}
		walls.clear();
	}

	private void drawWalls() {
		clearWalls();

		int offset = (int) Math.ceil(gridSize/2.0f);
		wallStartPoint = playerPosition
				.dx(-offset)
				.dy(offset);

		for (int x = 0; x < gridSize + 2; x++) {
			walls.add(spawnWallObject(wallStartPoint.dx(x)));
		}

		for (int x = 0; x < gridSize + 2; x++) {
			walls.add(spawnWallObject(wallStartPoint.dx(x).dy(-offset*2)));
		}

		for (int y = 0; y < gridSize; y++) {
			walls.add(spawnWallObject(wallStartPoint.dy(-y - 1)));
		}

		for (int y = 0; y < gridSize; y++) {
			walls.add(spawnWallObject(wallStartPoint.dy(-y - 1).dx(offset*2)));
		}
	}

	private RuneLiteObject spawnWallObject(WorldPoint point) {
		RuneLiteObject obj = client.createRuneLiteObject();

		//todo make this id a var?
		Model wall = client.loadModel(17822);
		obj.setModel(wall);
		LocalPoint lp = LocalPoint.fromWorld(client, point);
		obj.setLocation(lp, client.getPlane());
		obj.setActive(true);
		return obj;
	}

	private WorldPoint getRandomPointInGrid() {
		WorldPoint randomPoint;
		do {
			int x = ThreadLocalRandom.current().nextInt(0, gridSize);
			int y = ThreadLocalRandom.current().nextInt(0, gridSize);
			randomPoint = wallStartPoint.dx(x + 1).dy(-(y + 1));
		} while (randomPoint.equals(playerPosition));

		return randomPoint;
	}

	@Provides
	SnakeConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(SnakeConfig.class);
	}
}
