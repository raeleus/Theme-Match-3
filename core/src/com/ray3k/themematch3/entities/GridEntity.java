/*
 * The MIT License
 *
 * Copyright 2018 Raymond Buckley.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.ray3k.themematch3.entities;

import com.badlogic.gdx.Input.Buttons;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Action;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Container;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ActorGestureListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.NinePatchDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Scaling;
import com.esotericsoftware.spine.SkeletonData;
import com.ray3k.themematch3.Core;
import com.ray3k.themematch3.Entity;
import com.ray3k.themematch3.SpineDrawable;
import com.ray3k.themematch3.SpineDrawable.SpineDrawableTemplate;
import com.ray3k.themematch3.states.GameState;

public class GridEntity extends Entity {
    private static final float GRID_WIDTH = 64.0f;
    private static final float GRID_HEIGHT = 64.0f;
    private static final int COLUMNS = 8;
    private static final int ROWS = 8;
    private NinePatchDrawable drawable;
    private NinePatchDrawable drawableWarning;
    private Container selectedContainer;
    private static final Vector2 temp = new Vector2();
    private int difficulty;
    private float spawnFrequency;
    private float spawnCounter;
    private float spawnCount;
    private static final float SPAWN_DELAY = .5f;
    private static final float INITIAL_SPAWN_DELAY = .1f;
    private int level;
    private int piecesToNextLevel;
    
    @Override
    public void create() {
        level = 0;
        increaseLevel();
        
        drawable = new NinePatchDrawable(GameState.spineAtlas.createPatch("grid"));
        drawableWarning = new NinePatchDrawable(GameState.spineAtlas.createPatch("grid-warning"));
        
        Table root = GameState.spineStage.getRoot().findActor("root");
        
        Table table = new Table();
        table.setName("gridTable");
        table.setBackground(drawable);
        root.add(table);
        
        Array<Container> containers = new Array<Container>();
        for (int y = 0; y < ROWS; y++) {
            for (int x = 0; x < COLUMNS; x++) {
                final Container container = new Container();
                container.setBackground(drawable);
                containers.add(container);
                table.add(container).size(GRID_WIDTH, GRID_HEIGHT);
                
                container.setTouchable(Touchable.enabled);
                container.addListener(new ClickListener(Buttons.LEFT) {
                    @Override
                    public void clicked(InputEvent event, float x, float y) {
                        if (selectedContainer == null) {
                            if (container.getActor() != null) {
                                selectedContainer = container;
                                SpineDrawableTemplate template = new SpineDrawableTemplate();
                                template.minWidth = 64;
                                template.minHeight = 64;
                                SpineDrawable spineDrawable = new SpineDrawable(Core.assetManager.get(Core.DATA_PATH + "/spine/target.json", SkeletonData.class), Core.skeletonRenderer, template);
                                spineDrawable.getAnimationState().setAnimation(0, "animation", true);
                                spineDrawable.getAnimationState().getCurrent(0).setLoop(true);
                                Image image = new Image(spineDrawable);
                                image.setName("target");
                                Vector2 point = new Vector2();
                                point = container.localToStageCoordinates(point);
                                image.setPosition(point.x, point.y);
                                image.setTouchable(Touchable.disabled);
                                GameState.spineStage.addActor(image);
                            }
                        } else {
                            Movement movement = adjacentToSelected(container);
                            if (movement != Movement.NONE) {
                                swap(selectedContainer, container);
                            }
                            
                            GameState.spineStage.getRoot().findActor("target").remove();
                            selectedContainer = null;
                        }
                    }
                    
                });
                
                container.addListener(new ActorGestureListener() {
                    @Override
                    public void fling(InputEvent event, float velocityX,
                            float velocityY, int button) {
                        
                        if (selectedContainer != null) {
                            GameState.spineStage.getRoot().findActor("target").remove();
                            selectedContainer = null;
                        }
                        
                        if (Math.abs(velocityX) > Math.abs(velocityY)) {
                            if (velocityX > 0) {
                                swap(container, getAdjacent(container, Movement.RIGHT));
                            } else {
                                swap(container, getAdjacent(container, Movement.LEFT));
                            }
                        } else {
                            if (velocityY > 0) {
                                swap(container, getAdjacent(container, Movement.UP));
                            } else {
                                swap(container, getAdjacent(container, Movement.DOWN));
                            }
                        }
                    }
                    
                });
            }
            table.row();
        }
        
        initialSpawn(15);
    }
    
    private static enum Movement {
        LEFT, RIGHT, UP, DOWN, NONE
    }
    
    private Movement adjacentToSelected(Container container) {
        if (selectedContainer.equals(container)) return Movement.NONE;
        
        Table table = GameState.spineStage.getRoot().findActor("gridTable");
        int selectedIndex = table.getCells().indexOf(table.getCell(selectedContainer), false);
        int targetIndex = table.getCells().indexOf(table.getCell(container), false);
        
        if (selectedIndex % COLUMNS == targetIndex % COLUMNS) {
            if (selectedIndex / COLUMNS == targetIndex / COLUMNS + 1) return Movement.UP;
            else if (selectedIndex / COLUMNS == targetIndex / COLUMNS - 1) return Movement.DOWN;
            else return Movement.NONE;
        } else if (selectedIndex / COLUMNS == targetIndex / COLUMNS) {
            if (selectedIndex == targetIndex + 1) return Movement.LEFT;
            else if (selectedIndex == targetIndex - 1) return Movement.RIGHT;
            else return Movement.NONE;
        } else return Movement.NONE;
    }
    
    private Container getAdjacent(Container container, Movement movement) {
        Container returnValue = null;
        
        Table table = GameState.spineStage.getRoot().findActor("gridTable");
        int startIndex = table.getCells().indexOf(table.getCell(container), false);
        
        if (null != movement) switch (movement) {
            case RIGHT:
                int targetIndex = startIndex + 1;
                if (targetIndex < table.getCells().size && startIndex / COLUMNS == targetIndex / COLUMNS) {
                    returnValue = (Container) table.getCells().get(targetIndex).getActor();
                }   break;
            case LEFT:
                targetIndex = startIndex - 1;
                if (targetIndex >= 0 && startIndex / COLUMNS == targetIndex / COLUMNS) {
                    returnValue = (Container) table.getCells().get(targetIndex).getActor();
                }   break;
            case UP:
                targetIndex = startIndex - COLUMNS;
                if (targetIndex >= 0) {
                    returnValue = (Container) table.getCells().get(targetIndex).getActor();
                }   break;
            case DOWN:
                targetIndex = startIndex + COLUMNS;
                if (targetIndex < table.getCells().size) {
                    returnValue = (Container) table.getCells().get(targetIndex).getActor();
                }   break;
            default:
                break;
        }
        
        return returnValue;
    }
    
    private void swap(final Container start, final Container destination) {
        Actor actor = start.getActor();
        Actor other = null;
        if (destination != null) {
            other = destination.getActor();
        }
        
        if (actor != null) {
            GameState.inst().playSound("swap");
            
            final Table table = (Table)GameState.spineStage.getRoot().findActor("gridTable");
            
            temp.set(start.getPadLeft(), start.getPadBottom());
            start.localToStageCoordinates(temp);
            float startX = temp.x;
            float startY = temp.y;
            
            
            start.setActor(destination.getActor());
            destination.setActor(actor);
            table.pack();
            
            temp.set(destination.getPadLeft(), destination.getPadBottom());
            destination.localToStageCoordinates(temp);
            float endX = temp.x;
            float endY = temp.y;
            
            actor.setPosition(startX - endX + start.getPadLeft(), startY - endY + start.getPadBottom());
            actor.clearActions();
            actor.addAction(Actions.sequence(Actions.moveTo(destination.getPadLeft(), destination.getPadBottom(), .15f, Interpolation.smooth), new Action() {
                @Override
                public boolean act(float delta) {
                    checkForMatch();
                    dropAll();
                    return true;
                }
            }));
            
            if (other != null) {
                other.setPosition(endX - startX + start.getPadLeft(), endY - startY + start.getPadBottom());
                other.clearActions();
                other.addAction(Actions.moveTo(start.getPadLeft(), start.getPadBottom(), .15f, Interpolation.smooth));
            }
            
            updateWarningContainers();
        }
    }
    
    private boolean isColumnFree(int columnIndex) {
        return getFreeContainer(columnIndex) != null;
    }
    
    private Container getFreeContainer(int columnIndex) {
        Container returnValue = null;
        Table table = (Table) GameState.spineStage.getRoot().findActor("gridTable");
        
        for (int i = columnIndex % COLUMNS; i < table.getCells().size; i += COLUMNS) {
            Container container = (Container) table.getCells().get(i).getActor();
            if (container.getActor() == null) {
                returnValue = container;
            } else {
                break;
            }
        }
        
        return returnValue;
    }
    
    private void initialSpawn(int numberOfSpawn) {
        for (int i = 0; i < numberOfSpawn; i++) {
            GameState.spineStage.addAction(Actions.delay(i * INITIAL_SPAWN_DELAY, new Action() {
                @Override
                public boolean act(float delta) {
                    int columnIndex;

                    do {
                        columnIndex = MathUtils.random(COLUMNS - 1);
                    } while (!isColumnFree(columnIndex));
                    
                    int item = MathUtils.random(difficulty);
                    addPiece(columnIndex, GameState.textureNames.get(item), GameState.colors.get(item));
                    return true;
                }
            }));
        }
    }
    
    private void addRandomPiece() {
        int columnIndex = MathUtils.random(COLUMNS - 1);
        
        if (isColumnFree(columnIndex)) {
            int item = MathUtils.random(difficulty);
            addPiece(columnIndex, GameState.textureNames.get(item), GameState.colors.get(item));
            
            if (!isColumnFree(columnIndex)) {
                GameState.inst().playSound("warning");
            }
        } else {
            GameState.entityManager.addEntity(new GameOverTimerEntity(3.0f));
            removeAll();
            GameState.inst().playSound("lose");
            GameState.spineStage.getRoot().clearActions();
            spawnCounter = 100.0f;
            spawnCount = 0;
        }
        
        updateWarningContainers();
    }
    
    private void addRandomPieceSafe() {
        int columnIndex;
        
        do {
            columnIndex = MathUtils.random(COLUMNS - 1);
        } while (!isColumnFree(columnIndex));
        
        int item = MathUtils.random(difficulty);
        addPiece(columnIndex, GameState.textureNames.get(item), GameState.colors.get(item));

        if (!isColumnFree(columnIndex)) {
            GameState.inst().playSound("warning");
        }
        
        updateWarningContainers();
    }
    
    private boolean addPiece(int columnIndex, String name, String color) {
        GameState.inst().playSound("drop");
        
        Container container = getFreeContainer(columnIndex);
        if (container != null) {
            SpineDrawableTemplate template = new SpineDrawableTemplate();
            template.minWidth = 60;
            template.minHeight = 60;

            SpineDrawable spineDrawable = new SpineDrawable(Core.assetManager.get(Core.DATA_PATH + "/spine/" + name + ".json", SkeletonData.class), Core.skeletonRenderer, template);
            spineDrawable.getSkeleton().setSkin(color);
            Image image = new Image(spineDrawable);
            image.setUserObject(name + color);
            container.setActor(image);
            Table table = (Table) GameState.spineStage.getRoot().findActor("gridTable");
            table.pack();
            image.setPosition(container.getPadLeft(), container.getPadBottom() + GameState.GAME_HEIGHT);
            image.addAction(Actions.sequence(Actions.moveTo(container.getPadLeft(), container.getPadBottom(), 1.0f, Interpolation.bounceOut), new Action() {
                @Override
                public boolean act(float delta) {
                    checkForMatch();
                    dropAll();
                    return true;
                }
            }));
            return true;
        } else {
            return false;
        }
    }
    
    private void checkForMatch() {
        Array<Container> totalMatchingContainers = new Array<Container>();
        
        Table table = (Table) GameState.spineStage.getRoot().findActor("gridTable");
        
        //rows
        Array<Container> matchingContainers = new Array<Container>();
        int row = 0;
        for (int i = 0; i < table.getCells().size; i++) {
            if (i / COLUMNS > row) {
                row = i / COLUMNS;
                
                if (matchingContainers.size >= 3) {
                    totalMatchingContainers.addAll(matchingContainers);
                }
                matchingContainers.clear();
            }
            
            Container container = (Container) table.getCells().get(i).getActor();
            
            if (matchingContainers.size == 0 || container.getActor() == null || !container.getActor().getUserObject().equals(matchingContainers.first().getActor().getUserObject())) {
                if (matchingContainers.size >= 3) {
                    totalMatchingContainers.addAll(matchingContainers);
                }
                matchingContainers.clear();
            }
            
            if (container.getActor() != null) {
                matchingContainers.add(container);
            }
        }
        
        if (matchingContainers.size >= 3) {
            totalMatchingContainers.addAll(matchingContainers);
        }
        
        //columns
        matchingContainers.clear();
        for (int column = 0; column < COLUMNS; column++) {
            for (int i = column; i < table.getCells().size; i += COLUMNS) {
                
                Container container = (Container) table.getCells().get(i).getActor();

                if (matchingContainers.size == 0 || container.getActor() == null || !container.getActor().getUserObject().equals(matchingContainers.first().getActor().getUserObject())) {
                    if (matchingContainers.size >= 3) {
                        totalMatchingContainers.addAll(matchingContainers);
                    }
                    matchingContainers.clear();
                }

                if (container.getActor() != null) {
                    matchingContainers.add(container);
                }
            }
            
            if (matchingContainers.size >= 3) {
                score(matchingContainers.size);
                remove(matchingContainers);
            }
            
            matchingContainers.clear();
        }
        
        //tally score and remove from field
        if (totalMatchingContainers.size >= 3) {
            score(totalMatchingContainers.size);
            remove(totalMatchingContainers);
        }
    }
    
    private void score(int matchSize) {
        switch (matchSize) {
            case 3:
                GameState.inst().addScore(1);
                GameState.inst().playSound("match");
                break;
            case 4:
                GameState.inst().addScore(5);
                GameState.inst().playSound("bonus");
                break;
            case 5:
                GameState.inst().addScore(10);
                GameState.inst().playSound("bonus");
                break;
            case 6:
                GameState.inst().addScore(25);
                GameState.inst().playSound("bonus");
                break;
            default:
                GameState.inst().addScore(50);
                GameState.inst().playSound("bonus");
                break;
        }
        
        piecesToNextLevel -= matchSize;
        if (piecesToNextLevel <= 0) increaseLevel();
    }
    
    private void removeAll() {
        Table table = GameState.spineStage.getRoot().findActor("gridTable");
        for (int i = 0; i < table.getCells().size; i++) {
            remove((Container) table.getCells().get(i).getActor());
        }
    }
    
    private void remove(Array<Container> containers) {
        for (Container container : containers) {
            remove(container);
        }
    }
    
    private void remove(final Container container) {
        Actor actor = container.getActor();
        if (actor != null) {
            temp.set(container.getPadLeft(), container.getPadRight());
            container.localToStageCoordinates(temp);
            final float tempX = temp.x;
            final float tempY = temp.y;

            GameState.spineStage.addActor(actor);
            actor.setPosition(tempX, tempY);

            actor.clearActions();
            actor.addAction(Actions.sequence(Actions.moveTo(tempX, tempY - GameState.GAME_HEIGHT, .5f, Interpolation.circleIn), Actions.removeActor()));
        }
    }
    
    private void dropAll() {
        final Table table = GameState.spineStage.getRoot().findActor("gridTable");
        boolean didDrop = false;
        for (int i = table.getCells().size - 1; i >= 0; i--) {
            if (drop((Container) table.getCells().get(i).getActor())) {
                didDrop = true;
            }
        }
        if (didDrop) {
            GameState.spineStage.addAction(Actions.sequence(Actions.delay(.75f), new Action() {
                @Override
                public boolean act(float delta) {
                    checkForMatch();
                    dropAll();
                    return true;
                }
            }));
        } else {
            checkForMatch();
        }
    }
    
    private boolean drop(Container container) {
        boolean didDrop = false;
        Actor actor = container.getActor();
        if (actor != null) {
            Table table = GameState.spineStage.getRoot().findActor("gridTable");
            int index = table.getCells().indexOf(table.getCell(container), false);
            int column = index % COLUMNS;
            
            for (int i = column + COLUMNS * (ROWS - 1); i > index; i -= COLUMNS) {
                Container targetContainer = (Container) table.getCells().get(i).getActor();
                
                if (targetContainer.getActor() == null) {
                    
                    temp.set(container.getPadLeft(), container.getPadRight());
                    container.localToStageCoordinates(temp);
                    float startX = temp.x;
                    float startY = temp.y;
                    
                    targetContainer.setActor(actor);
                    table.pack();
                    temp.set(targetContainer.getPadLeft(), targetContainer.getPadBottom());
                    targetContainer.localToStageCoordinates(temp);
                    float endX = temp.x;
                    float endY = temp.y;
                    
                    actor.setPosition(startX - endX + container.getPadLeft(), startY - endY + container.getPadBottom());
                    
                    actor.clearActions();
                    actor.addAction(Actions.moveTo(targetContainer.getPadLeft(), targetContainer.getPadBottom(), .5f, Interpolation.bounceOut));
                    didDrop = true;
                    break;
                }
            }
        }
        
        return didDrop;
    }
    
    private void updateWarningContainers() {
        Table table = GameState.spineStage.getRoot().findActor("gridTable");
        for (int i = 0; i < COLUMNS; i++) {
            if (isColumnFree(i)) {
                for (int j = i; j < table.getCells().size; j += COLUMNS) {
                    ((Container) table.getCells().get(j).getActor()).setBackground(drawable);
                }
            } else {
                for (int j = i; j < table.getCells().size; j += COLUMNS) {
                    ((Container) table.getCells().get(j).getActor()).setBackground(drawableWarning);
                }
            }
        }
    }
    
    private void increaseLevel() {
        level++;
        
        if (level > 1) {
            GameState.inst().playSound("victory");

            Image image = new Image(GameState.spineAtlas.findRegion("levelup"));
            image.setScaling(Scaling.none);
            image.setPosition(GameState.GAME_WIDTH / 2.0f, GameState.GAME_HEIGHT / 2.0f, Align.center);
            image.setTouchable(Touchable.disabled);
            GameState.spineStage.addActor(image);
            image.addAction(Actions.sequence(Actions.fadeOut(1.5f), Actions.removeActor()));
        }
        
        switch (level) {
            case 1:
                difficulty = 3;
                spawnFrequency = 5.0f;
                spawnCount = 3;
                piecesToNextLevel = 15;
                break;
            case 2:
                difficulty = 3;
                spawnFrequency = 4.0f;
                spawnCount = 4;
                piecesToNextLevel = 20;
                break;
            case 3:
                difficulty = 4;
                spawnFrequency = 4.0f;
                spawnCount = 4;
                piecesToNextLevel = 25;
                break;
            case 4:
                difficulty = 4;
                spawnFrequency = 4.0f;
                spawnCount = 5;
                piecesToNextLevel = 30;
                break;
            case 5:
                difficulty = 4;
                spawnFrequency = 3.5f;
                spawnCount = 5;
                piecesToNextLevel = 35;
                break;
            case 6:
                difficulty = 5;
                spawnFrequency = 3.5f;
                spawnCount = 5;
                piecesToNextLevel = 40;
                break;
            case 7:
                difficulty = 5;
                spawnFrequency = 3.5f;
                spawnCount = 6;
                piecesToNextLevel = 45;
                break;
            case 8:
                difficulty = 6;
                spawnFrequency = 3.5f;
                spawnCount = 6;
                piecesToNextLevel = 50;
                break;
            case 9:
                difficulty = 6;
                spawnFrequency = 3.0f;
                spawnCount = 6;
                piecesToNextLevel = 60;
                break;
        }
        
        spawnCounter = spawnFrequency;
        GameState.inst().levelLabel.setText("Level\n" + level);
    }

    @Override
    public void act(float delta) {
        spawnCounter -= delta;
        if (spawnCounter < 0) {
            spawnCounter = spawnFrequency;
            
            addRandomPiece();
            
            for (int i = 1; i < spawnCount; i++) {
                GameState.spineStage.addAction(Actions.delay(i * SPAWN_DELAY, new Action() {
                    @Override
                    public boolean act(float delta) {
                        addRandomPieceSafe();
                        return true;
                    }
                }));
            }
        }
    }

    @Override
    public void actEnd(float delta) {
    }

    @Override
    public void draw(SpriteBatch spriteBatch, float delta) {
    }

    @Override
    public void destroy() {
    }

    @Override
    public void collision(Entity other) {
    }
}
