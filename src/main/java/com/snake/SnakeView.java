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

	private final Map<SnakePlayer, List<RuneLiteObject>> snakePlayerTrails = new HashMap<>();
	private final List<RuneLiteObject> walls = new ArrayList<>();


	private final int trailModelId = 29311;
	private final int wallModelId = 32693;

	@Inject
	public SnakeView(Client client)
	{
		this.client = client;
	}

	public void clearAll()
	{
		clearWalls();
		clearSnakeTrails();
	}

	//TODO what should be the input...
	//but it means that we have to check if exsits in map, create object if not, create obj if not enough and so on
	//is not alive check if exists, if so then setActive false and clear map
	public void drawSnakeTrails(List<SnakePlayer> snakePlayers)
	{
		//TODO check here or just not call it?
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
					snakeObjectTrail.add(spawnNewSnakeTrailObject());
				}

				int index = 0;
				for (WorldPoint point : snakePointTrail)
				{
					RuneLiteObject obj = snakeObjectTrail.get(index);
					LocalPoint lp = LocalPoint.fromWorld(client, point);
					obj.setLocation(lp, client.getPlane());
					obj.setActive(true);
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

	public void drawWalls(WorldPoint playerWorldPosition, int gameSize)
	{
		clearWalls();

		WorldPoint wallStartPoint = SnakeUtils.getWallStartPoint(playerWorldPosition, gameSize);

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

	private RuneLiteObject spawnNewSnakeTrailObject()
	{
		RuneLiteObject obj = client.createRuneLiteObject();
		ModelData trailModel = client.loadModelData(trailModelId).cloneColors();
		//TODO color for each player?
		trailModel.recolor(trailModel.getFaceColors()[0],
			JagexColor.rgbToHSL(new Color(32, 139, 25).getRGB(), 1.0d));
		trailModel.recolor(trailModel.getFaceColors()[1],
			JagexColor.rgbToHSL(new Color(59, 148, 74).getRGB(), 1.0d));

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
}
