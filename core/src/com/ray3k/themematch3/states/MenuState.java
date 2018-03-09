/*
 * The MIT License
 *
 * Copyright 2017 Raymond Buckley.
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
package com.ray3k.themematch3.states;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Dialog;
import com.badlogic.gdx.scenes.scene2d.ui.HorizontalGroup;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton.ImageButtonStyle;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.esotericsoftware.spine.SkeletonData;
import com.ray3k.themematch3.Core;
import com.ray3k.themematch3.SpineDrawable;
import com.ray3k.themematch3.SpineDrawable.SpineDrawableTemplate;
import com.ray3k.themematch3.State;

public class MenuState extends State {
    private Stage stage;
    private Skin skin;
    private Table root;

    public MenuState(Core core) {
        super(core);
    }
    
    @Override
    public void start() {
        skin = Core.assetManager.get(Core.DATA_PATH + "/ui/theme-match-3.json", Skin.class);
        stage = new Stage(new ScreenViewport());
        
        Gdx.input.setInputProcessor(stage);
        
        createMenu();
    }
    
    private void createMenu() {
        root = new Table();
        root.setFillParent(true);
        stage.addActor(root);
        
        Image logo = new Image(skin, "logo");
        root.add(logo).colspan(2).expand();
        
        root.defaults().space(30.0f).expand().top().minWidth(200.0f);
        root.row();
        TextButton textButtton = new TextButton("Play", skin);
        root.add(textButtton);
        
        textButtton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeListener.ChangeEvent event, Actor actor) {
                Core.assetManager.get(Core.DATA_PATH + "/sfx/drop.wav", Sound.class).play(1.0f);
                showPieceDialog();
            }
        });
        
        textButtton = new TextButton("Quit", skin);
        root.add(textButtton);
        
        textButtton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeListener.ChangeEvent event, Actor actor) {
                Core.assetManager.get(Core.DATA_PATH + "/sfx/match.wav", Sound.class).play(1.0f);
                Gdx.app.exit();
            }
        });
    }
    
    @Override
    public void draw(SpriteBatch spriteBatch, float delta) {
        Gdx.gl.glClearColor(0 / 255.0f, 0 / 255.0f, 0 / 255.0f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        stage.draw();
    }

    @Override
    public void act(float delta) {
        stage.act(delta);
    }

    @Override
    public void dispose() {
        
    }

    @Override
    public void stop() {
        stage.dispose();
    }
    
    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }
    
    private void showPieceDialog() {
        final Dialog dialog = new Dialog("", skin) {
            @Override
            protected void result(Object object) {
                if ((Boolean) object) {
                    Core.assetManager.get(Core.DATA_PATH + "/sfx/match.wav", Sound.class).play(1.0f);
                    Core.stateManager.loadState("game");
                }
            }
        };
        
        HorizontalGroup hgroup = new HorizontalGroup();
        hgroup.wrap();
        hgroup.space(25.0f);
        hgroup.wrapSpace(25.0f);
        hgroup.center();
        hgroup.rowAlign(Align.center);
        dialog.getContentTable().add(hgroup).grow();
        
        for (int i = 0; i < 7; i++) {
            final ImageButton imageButton = new ImageButton(new ImageButtonStyle(skin.get("default", ImageButtonStyle.class)));
            imageButton.setName("button" + i);
            imageButton.setUserObject(i);
            hgroup.addActor(imageButton);
            
            if (GameState.textureNames.get(i) != null && GameState.colors.get(i) != null) {
                SpineDrawableTemplate template = new SpineDrawableTemplate();
                template.minWidth = 60;
                template.minHeight = 60;
                SpineDrawable drawable = new SpineDrawable(Core.assetManager.get(Core.DATA_PATH + "/spine/" + GameState.textureNames.get(i) + ".json", SkeletonData.class), Core.skeletonRenderer, template);
                drawable.getSkeleton().setSkin(GameState.colors.get(i));
                imageButton.getStyle().imageUp = drawable;
            }
            
            imageButton.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeListener.ChangeEvent event,
                        Actor actor) {
                    Core.assetManager.get(Core.DATA_PATH + "/sfx/drop.wav", Sound.class).play(1.0f);
                    showTextureDialog((Integer) imageButton.getUserObject());
                    dialog.hide();
                }
            });
        }
        
        dialog.button("Play", true).button("Cancel", false);
        dialog.key(Keys.ENTER, true).key(Keys.ESCAPE, false);
        dialog.getButtonTable().getCells().first().getActor().setName("playButton");
        boolean disabled = false;
        for (String string : GameState.textureNames) {
            disabled = string == null;
            if (disabled) break;
        }
        if (!disabled) for (String string : GameState.colors) {
            disabled = string == null;
            if (disabled) break;
        }
        ((TextButton) dialog.findActor("playButton")).setDisabled(disabled);
        dialog.setFillParent(true);
        
        dialog.show(stage, null);
    }
    
    private void showTextureDialog(final int index) {
        final Dialog dialog = new Dialog("", skin);
        
        HorizontalGroup hgroup = new HorizontalGroup();
        hgroup.align(Align.center);
        hgroup.rowAlign(Align.center);
        hgroup.wrap();
        hgroup.space(25.0f);
        hgroup.wrapSpace(25.0f);
        dialog.getContentTable().add(hgroup).grow();
        
        String[] names = {"watermelon", "strawberry", "orange", "lemon", "jawbreaker", "heart", "gummy-bear", "grapes", "diamond", "cog", "chip", "cherries", "candy-corn", "candy-2", "candy-1", "banana", "apple"};
        for (String name : names) {
            final ImageButton imageButton = new ImageButton(new ImageButtonStyle(skin.get("default", ImageButtonStyle.class)));
            imageButton.setUserObject(name);
            hgroup.addActor(imageButton);
            
            SpineDrawableTemplate template = new SpineDrawableTemplate();
            template.minWidth = 60;
            template.minHeight = 60;
            SpineDrawable drawable = new SpineDrawable(Core.assetManager.get(Core.DATA_PATH + "/spine/" + name + ".json", SkeletonData.class), Core.skeletonRenderer, template);
            drawable.getSkeleton().setSkin("white");
            imageButton.getStyle().imageUp = drawable;
            
            imageButton.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeListener.ChangeEvent event,
                        Actor actor) {
                    dialog.hide();
                    Core.assetManager.get(Core.DATA_PATH + "/sfx/drop.wav", Sound.class).play(1.0f);
                    GameState.textureNames.set(index, (String) imageButton.getUserObject());
                    showColorDialog(index);
                }
            });
        }

        dialog.setFillParent(true);
        
        dialog.show(stage, null);
    }
    
    private void showColorDialog(final int index) {
        final Dialog dialog = new Dialog("", skin);
        
        Table table = new Table();
        ScrollPane scrollPane = new ScrollPane(table, skin);
        scrollPane.setFadeScrollBars(false);
        scrollPane.setFlickScroll(false);
        dialog.getContentTable().add(scrollPane).fillY().minWidth(300.0f);
        
        Array<String> names = new Array<String>(new String[] {"blue", "brown", "cyan", "dark-gray", "gold", "gray", "green", "magenta", "orange", "pink", "purple", "red", "white", "yellow"});
        
        String textureName = GameState.textureNames.get(index);
        
        for (int i = 0; i < GameState.textureNames.size; i++) {
            if (i != index) {
                String texName = GameState.textureNames.get(i);
                if (texName != null && texName.equals(textureName)) {
                    String color = GameState.colors.get(i);
                    if (color != null) {
                        names.removeValue(color, false);
                    }
                }
            }
        }
        
        for (String name : names) {
            final TextButton textButton = new TextButton(name, skin, name);
            textButton.setUserObject(name);
            table.add(textButton).growX();
            table.row();
            
            textButton.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeListener.ChangeEvent event,
                        Actor actor) {
                    dialog.hide();
                    Core.assetManager.get(Core.DATA_PATH + "/sfx/drop.wav", Sound.class).play(1.0f);
                    GameState.colors.set(index, (String) textButton.getUserObject());
                    showPieceDialog();
                }
            });
        }

        dialog.setFillParent(true);
        
        dialog.show(stage, null);
        stage.setScrollFocus(scrollPane);
    }
}