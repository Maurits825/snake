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
	boolean[][] walkableTiles;
	private SnakeGridTheme theme;

	private final Map<SnakePlayer, List<RuneLiteObject>> snakePlayerTrails = new HashMap<>();
	private final List<RuneLiteObject> walls = new ArrayList<>();
	private final List<RuneLiteObject> tiles = new ArrayList<>();
	private RuneLiteObject foodObject;

	private WorldPoint wallStartPoint;

	private static final int TRAIL_MODEL_ID = 29311;
	private static final int FOOD_MODEL_ID = 2317;

	@Inject
	public SnakeView(Client client)
	{
		this.client = client;
	}

	public void initialize(List<SnakePlayer> snakePlayers, int gameSize, SnakeGridTheme theme, boolean[][] walkableTiles)
	{
		this.snakePlayers = snakePlayers;
		this.gameSize = gameSize;
		this.theme = theme;
		this.walkableTiles = walkableTiles;

		wallStartPoint = SnakeUtils.getWallStartPoint(client.getLocalPlayer().getWorldLocation(), gameSize);

		if (theme.getWallModelId() != -1)
		{
			spawnWalls();
		}
		if (theme.getTileModelId1() != -1 && theme.getTileModelId2() != -1)
		{
			spawnGridTiles();
		}

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
		clearGridTiles();
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

	private void spawnWalls()
	{
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
			walls.add(spawnWallObject(wallStartPoint.dx(gameSize + 1).dy(-y - 1)));
		}
	}

	private void spawnGridTiles()
	{
		int tileObjectId;
		for (int x = 0; x < gameSize; x++)
		{
			for (int y = 0; y < gameSize; y++)
			{
				if ((x + y) % 2 == 0)
				{
					tileObjectId = theme.getTileModelId1();
				}
				else
				{
					tileObjectId = theme.getTileModelId2();
				}

				if (walkableTiles[x][y])
				{
					tiles.add(spawnGridTileObject(wallStartPoint.dx(x + 1).dy(-(y + 1)), tileObjectId));
				}
			}
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

	private void clearGridTiles()
	{
		for (RuneLiteObject obj : tiles)
		{
			obj.setActive(false);
		}
		tiles.clear();
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
		ModelData trailModel = client.loadModelData(TRAIL_MODEL_ID).cloneColors();

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

		Model wall = client.loadModel(theme.getWallModelId());
		obj.setModel(wall);
		LocalPoint lp = LocalPoint.fromWorld(client, point);
		obj.setLocation(lp, client.getPlane());
		obj.setActive(true);
		return obj;
	}

	private RuneLiteObject spawnGridTileObject(WorldPoint point, int tileObjectId)
	{
		RuneLiteObject obj = client.createRuneLiteObject();

		Model tile = client.loadModel(tileObjectId);
		obj.setModel(tile);
		LocalPoint lp = LocalPoint.fromWorld(client, point);
		obj.setLocation(lp, client.getPlane());

		obj.setActive(true);
		return obj;
	}

	private RuneLiteObject spawnFoodObject()
	{
		RuneLiteObject obj = client.createRuneLiteObject();

		ModelData foodModel = client.loadModelData(FOOD_MODEL_ID)
			.cloneVertices()
			.translate(0, 200, 0)
			.cloneColors();
		foodModel.recolor(foodModel.getFaceColors()[0],
			JagexColor.rgbToHSL(new Color(186, 16, 225).getRGB(), 1.0d));
		obj.setModel(foodModel.light());

		obj.setAnimation(client.loadAnimation(502));
		obj.setShouldLoop(true);

		obj.setDrawFrontTilesFirst(true);

		return obj;
	}
}
