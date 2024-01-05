package com.snake;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.JagexColor;
import net.runelite.api.Model;
import net.runelite.api.ModelData;
import net.runelite.api.RuneLiteObject;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;

public class SnakeView
{
	private final Client client;

	private List<SnakePlayer> snakePlayers;
	private int gameSize;

	private final Map<SnakePlayer, List<RuneLiteObject>> snakePlayerTrails = new HashMap<>();
	private final List<RuneLiteObject> walls = new ArrayList<>();
	private RuneLiteObject foodObject;

	private final int trailModelId = 29311;
	private final int wallModelId = 32693;
	private int foodModelId = 2317;

	@Inject
	public SnakeView(Client client)
	{
		this.client = client;
	}

	public void initialize(List<SnakePlayer> snakePlayers, WorldPoint foodLocation, int gameSize)
	{
		this.snakePlayers = snakePlayers;
		this.gameSize = gameSize;

		drawWalls();
		foodObject = spawnFoodObject();
	}

	public void update(WorldPoint foodLocation)
	{
		updateFoodObject(foodLocation);
		updateSnakeTrails();
	}

	public void reset()
	{
		clearWalls();
		clearSnakeTrails();

		if (foodObject != null)
		{
			foodObject.setActive(false);
		}

		snakePlayers = null;
	}

	private void updateSnakeTrails()
	{
		if (snakePlayers == null)
		{
			return;
		}

		for (SnakePlayer snakePlayer : snakePlayers)
		{
			if (snakePlayer.isAlive())
			{
				if (!snakePlayerTrails.containsKey(snakePlayer))
				{
					snakePlayerTrails.put(snakePlayer, new ArrayList<>());
				}

				List<RuneLiteObject> snakeObjectTrail = snakePlayerTrails.get(snakePlayer);
				Queue<WorldPoint> snakePointTrail = snakePlayer.getSnakeTrail();
				for (int i = 0; i <= (snakePointTrail.size() - snakeObjectTrail.size()); i++)
				{
					snakeObjectTrail.add(spawnSnakeTrailObject(snakePlayer.getColor()));
				}

				int index = 0;
				for (WorldPoint point : snakePointTrail)
				{
					RuneLiteObject obj = snakeObjectTrail.get(index);
					LocalPoint lp = LocalPoint.fromWorld(client, point);
					obj.setLocation(lp, client.getPlane());
					if (!obj.isActive())
					{
						obj.setActive(true);
					}
					index++;
				}

			}
			else if (snakePlayerTrails.containsKey(snakePlayer))
			{
				for (RuneLiteObject rlObject : snakePlayerTrails.get(snakePlayer))
				{
					rlObject.setActive(false);
				}
				snakePlayerTrails.remove(snakePlayer);
			}
		}
	}

	private void drawWalls()
	{
		clearWalls();

		WorldPoint wallStartPoint = SnakeUtils.getWallStartPoint(client.getLocalPlayer().getWorldLocation(), gameSize);

		for (int x = 0; x < gameSize + 2; x++)
		{
			walls.add(spawnWallObject(wallStartPoint.dx(x)));
		}

		for (int x = 0; x < gameSize + 2; x++)
		{
			walls.add(spawnWallObject(wallStartPoint.dx(x).dy(-gameSize - 1)));
		}

		for (int y = 0; y < gameSize; y++)
		{
			walls.add(spawnWallObject(wallStartPoint.dy(-y - 1)));
		}

		for (int y = 0; y < gameSize; y++)
		{
			walls.add(spawnWallObject(wallStartPoint.dy(-y - 1).dx(gameSize + 1)));
		}
	}

	private void updateFoodObject(WorldPoint foodLocation)
	{
		if (foodLocation == null)
		{
			return;
		}

		if (!foodObject.isActive())
		{
			foodObject.setActive(true);
		}
		foodObject.setLocation(LocalPoint.fromWorld(client, foodLocation), client.getPlane());
	}

	private void clearWalls()
	{
		for (RuneLiteObject obj : walls)
		{
			obj.setActive(false);
		}
		walls.clear();
	}

	private void clearSnakeTrails()
	{
		snakePlayerTrails.forEach((snakePlayer, runeLiteObjects) -> {
			for (RuneLiteObject runeLiteObject : runeLiteObjects)
			{
				runeLiteObject.setActive(false);
			}
		});
		snakePlayerTrails.clear();
	}

	private RuneLiteObject spawnSnakeTrailObject(Color color)
	{
		RuneLiteObject obj = client.createRuneLiteObject();
		ModelData trailModel = client.loadModelData(trailModelId).cloneColors();

		trailModel.recolor(trailModel.getFaceColors()[0],
			JagexColor.rgbToHSL(color.getRGB(), 0.01d));
		trailModel.recolor(trailModel.getFaceColors()[1],
			JagexColor.rgbToHSL(color.getRGB(), 1.0d));

		obj.setModel(trailModel.light());

		return obj;
	}

	private RuneLiteObject spawnWallObject(WorldPoint point)
	{
		RuneLiteObject obj = client.createRuneLiteObject();

		Model wall = client.loadModel(wallModelId);
		obj.setModel(wall);
		LocalPoint lp = LocalPoint.fromWorld(client, point);
		obj.setLocation(lp, client.getPlane());
		obj.setActive(true);
		return obj;
	}

	private RuneLiteObject spawnFoodObject()
	{
		RuneLiteObject obj = client.createRuneLiteObject();

		ModelData foodModel = client.loadModelData(foodModelId)
			.cloneVertices()
			.translate(0, 200, 0)
			.cloneColors();
		foodModel.recolor(foodModel.getFaceColors()[0],
			JagexColor.rgbToHSL(new Color(186, 16, 225).getRGB(), 1.0d));
		obj.setModel(foodModel.light());

		obj.setAnimation(client.loadAnimation(502));
		obj.setShouldLoop(true);

		obj.setDrawFrontTilesFirst(true);//TODO TEST!!!

		return obj;
	}
}
