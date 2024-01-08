package com.snake;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
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
	private boolean[][] walkableTiles;
	private SnakeGridTheme theme;
	private boolean isSameFoodSpawn;
	private boolean showAllFood;

	private final Map<SnakePlayer, List<RuneLiteObject>> snakePlayerTrails = new HashMap<>();
	private final List<RuneLiteObject> walls = new ArrayList<>();
	private final List<RuneLiteObject> tiles = new ArrayList<>();
	private final List<RuneLiteObject> foods = new ArrayList<>();

	private WorldPoint wallStartPoint;

	private static final int TRAIL_MODEL_ID = 29311;
	private static final int FOOD_MODEL_ID = 2317;

	private static final Color DEFAULT_FOOD_COLOR = new Color(186, 16, 225);

	@Inject
	public SnakeView(Client client)
	{
		this.client = client;
	}

	public void initialize(List<SnakePlayer> snakePlayers, int gameSize, SnakeGridTheme theme, boolean[][] walkableTiles, boolean isSameFoodSpawn, boolean showAllFood)
	{
		this.snakePlayers = snakePlayers;
		this.gameSize = gameSize;
		this.theme = theme;
		this.walkableTiles = walkableTiles;
		this.isSameFoodSpawn = isSameFoodSpawn;
		this.showAllFood = showAllFood;

		wallStartPoint = SnakeUtils.getWallStartPoint(client.getLocalPlayer().getWorldLocation(), gameSize);

		if (theme.getWallModelId() != -1)
		{
			spawnWalls();
		}
		if (theme.getTileModelId1() != -1 && theme.getTileModelId2() != -1)
		{
			spawnGridTiles();
		}

		spawnFoods();
	}

	public void update()
	{
		updateFoodObjects();
		updateSnakeTrails();
	}

	public void reset()
	{
		clearAll(Arrays.asList(tiles, walls, foods));
		clearSnakeTrails();

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

	private void updateFoodObjects()
	{
		if (snakePlayers == null)
		{
			return;
		}

		if (isSameFoodSpawn)
		{
			drawFoodAtLocation(foods.get(0), snakePlayers.get(0).getFoodLocation());
		}
		else
		{
			for (int i = 0; i < snakePlayers.size(); i++)
			{
				if (showAllFood || snakePlayers.get(i).isActivePlayer())
				{
					drawFoodAtLocation(foods.get(i), snakePlayers.get(i).getFoodLocation());
				}
			}
		}
	}

	private void drawFoodAtLocation(RuneLiteObject food, WorldPoint location)
	{
		if (location == null)
		{
			return;
		}

		food.setLocation(LocalPoint.fromWorld(client, location), client.getPlane());
		if (!food.isActive())
		{
			food.setActive(true);
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

	private void spawnFoods()
	{
		if (isSameFoodSpawn || !showAllFood || snakePlayers.size() == 1)
		{
			foods.add(spawnFoodObject(DEFAULT_FOOD_COLOR));
		}
		else
		{
			for (SnakePlayer snakePlayer : snakePlayers)
			{
				foods.add(spawnFoodObject(snakePlayer.getColor()));
			}
		}
	}

	private void clearAll(List<List<RuneLiteObject>> allObjectLists)
	{
		for (List<RuneLiteObject> objectList : allObjectLists)
		{
			for (RuneLiteObject obj : objectList)
			{
				obj.setActive(false);
			}
			objectList.clear();
		}
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

	private RuneLiteObject spawnFoodObject(Color color)
	{
		RuneLiteObject obj = client.createRuneLiteObject();

		ModelData foodModel = client.loadModelData(FOOD_MODEL_ID)
			.cloneVertices()
			.translate(0, 200, 0)
			.cloneColors();
		foodModel.recolor(foodModel.getFaceColors()[0],
			JagexColor.rgbToHSL(color.getRGB(), 1.0d));
		obj.setModel(foodModel.light());

		obj.setAnimation(client.loadAnimation(502));
		obj.setShouldLoop(true);

		obj.setDrawFrontTilesFirst(true);

		return obj;
	}
}
