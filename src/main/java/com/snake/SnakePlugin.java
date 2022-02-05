package com.snake;

import com.google.inject.Provides;

import javax.inject.Inject;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameTick;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.events.OverlayMenuClicked;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.ui.overlay.OverlayMenuEntry;

import java.awt.Color;
import java.util.*;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@PluginDescriptor(
        name = "Snake"
)
public class SnakePlugin extends Plugin {
    @Inject
    private Client client;

    @Inject
    private SnakeConfig config;

    @Inject
    private ClientThread clientThread;

    @Inject
    private OverlayManager overlayManager;

    @Inject
    private SnakeOverlay overlay;

    enum State {
        INIT,
        WAITING_TO_START,
        PLAYING,
        GAME_OVER,
        RUN_ON,
    }

    @Getter
    private State currentState;

    private WorldPoint playerWorldPosition;
    private WorldPoint previousPlayerWorldPosition;
    private LocalPoint playerLocalPosition;

    private Queue<RuneLiteObject> snakeTrail = new ArrayDeque<>();
    private int initialTrailSize = 2;

    private List<RuneLiteObject> walls = new ArrayList<>();
    private WorldPoint wallStartPoint;
    private int gameSize;

    private RuneLiteObject foodObject;

    @Override
    protected void startUp() throws Exception {
        overlayManager.add(overlay);
        currentState = State.INIT;
    }

    @Override
    protected void shutDown() throws Exception {
        overlayManager.remove(overlay);
        clientThread.invokeLater(() ->
        {
            resetGame();
            return true;
        });
    }

    public Integer getScore() {
        return snakeTrail.size() - initialTrailSize;
    }

    @Subscribe
    public void onGameTick(GameTick tick) {
        previousPlayerWorldPosition = playerWorldPosition;
        playerWorldPosition = client.getLocalPlayer().getWorldLocation();
        playerLocalPosition = LocalPoint.fromWorld(client, playerWorldPosition);

        switch (currentState) {
            case INIT:
                initializeGame();
                break;
            case WAITING_TO_START:
                if (checkPlayerRunning()) {
                    currentState = State.RUN_ON;
                } else if (!previousPlayerWorldPosition.equals(playerWorldPosition)) {
                    gameLoop();
                    currentState = State.PLAYING;
                }
                break;
            case PLAYING:
                gameLoop();
                break;
            case GAME_OVER:
            case RUN_ON:
                break;
        }
    }

    private void initializeGame() {
        for (int i = 0; i < initialTrailSize; i++) {
            snakeTrail.add(spawnNewSnakeTrailObject());
        }

        updateGameSize();

        drawWalls();

        createFoodObj();
        reSpawnFood();

        currentState = State.WAITING_TO_START;
    }

    private void gameLoop() {
        if (checkPlayerRunning()) {
            currentState = State.RUN_ON;
        } else if (checkInvalidMovement()) {
            Player localPlayer = client.getLocalPlayer();
            localPlayer.setAnimation(2925);
            localPlayer.setAnimationFrame(0);
            currentState = State.GAME_OVER;
        } else if (playerLocalPosition.equals(foodObject.getLocation())) {
            snakeTrail.add(spawnNewSnakeTrailObject());
            reSpawnFood();
        } else {
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

        currentState = State.INIT;
    }

    private boolean checkInvalidMovement() {
        boolean inGameBoundary =
                playerWorldPosition.getX() > wallStartPoint.getX() &&
                        playerWorldPosition.getX() <= (wallStartPoint.getX() + gameSize) &&
                        playerWorldPosition.getY() < wallStartPoint.getY() &&
                        playerWorldPosition.getY() >= (wallStartPoint.getY() - gameSize);

        if (!inGameBoundary) {
            return true;
        }

        for (RuneLiteObject trailObj : snakeTrail) {
            if (playerLocalPosition.equals(trailObj.getLocation())) {
                return true;
            }
        }

        return false;
    }

    private void clearSnakeTrail() {
        for (RuneLiteObject obj : snakeTrail) {
            obj.setActive(false);
        }
        snakeTrail.clear();
    }

    private RuneLiteObject spawnNewSnakeTrailObject() {
        RuneLiteObject obj = client.createRuneLiteObject();
        ModelData trailModel = client.loadModelData(29311).cloneColors();
        trailModel.recolor(trailModel.getFaceColors()[0],
                JagexColor.rgbToHSL(new Color(38, 212, 64).getRGB(), 0.6d));
        trailModel.recolor(trailModel.getFaceColors()[1],
                JagexColor.rgbToHSL(new Color(173, 214, 179).getRGB(), 1.0d));

        obj.setModel(trailModel.light());
        obj.setLocation(playerLocalPosition, client.getPlane());

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

        int offset = (int) Math.ceil(gameSize / 2.0f);
        wallStartPoint = playerWorldPosition
                .dx(-offset)
                .dy(offset);

        for (int x = 0; x < gameSize + 2; x++) {
            walls.add(spawnWallObject(wallStartPoint.dx(x)));
        }

        for (int x = 0; x < gameSize + 2; x++) {
            walls.add(spawnWallObject(wallStartPoint.dx(x).dy(-offset * 2)));
        }

        for (int y = 0; y < gameSize; y++) {
            walls.add(spawnWallObject(wallStartPoint.dy(-y - 1)));
        }

        for (int y = 0; y < gameSize; y++) {
            walls.add(spawnWallObject(wallStartPoint.dy(-y - 1).dx(offset * 2)));
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
            int x = ThreadLocalRandom.current().nextInt(0, gameSize);
            int y = ThreadLocalRandom.current().nextInt(0, gameSize);
            randomPoint = wallStartPoint.dx(x + 1).dy(-(y + 1));
        } while (randomPoint.equals(playerWorldPosition));

        return randomPoint;
    }

    private boolean checkPlayerRunning() {
        return previousPlayerWorldPosition != null && playerLocalPosition != null &&
                previousPlayerWorldPosition.distanceTo(playerWorldPosition) > 1;
    }

    private void updateGameSize() {
        gameSize = 1 + 2 * config.gameSize();
    }

    @Subscribe
    public void onOverlayMenuClicked(OverlayMenuClicked overlayMenuClicked) {
        OverlayMenuEntry overlayMenuEntry = overlayMenuClicked.getEntry();
        if (overlayMenuEntry.getMenuAction() == MenuAction.RUNELITE_OVERLAY
                && overlayMenuClicked.getEntry().getOption().equals("Start")
                && overlayMenuClicked.getOverlay() == overlay)
        {
            resetGame();
        }
    }

    @Subscribe
    public void onConfigChanged(ConfigChanged configChanged) {
        if (configChanged.getGroup().equals("snakeConfig")) {
            updateGameSize();
            clientThread.invokeLater(() ->
            {
                resetGame();
                return true;
            });
        }
    }

    @Provides
    SnakeConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(SnakeConfig.class);
    }
}
