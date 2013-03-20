/*
 ##################################################################
 #                     GNU BACKGAMMON MOBILE                      #
 ##################################################################
 #                                                                #
 #  Authors: Domenico Martella - Davide Saurino                   #
 #  E-mail: info@alcacoop.it                                      #
 #  Date:   19/12/2012                                            #
 #                                                                #
 ##################################################################
 #                                                                #
 #  Copyright (C) 2012   Alca Societa' Cooperativa                #
 #                                                                #
 #  This file is part of GNU BACKGAMMON MOBILE.                   #
 #  GNU BACKGAMMON MOBILE is free software: you can redistribute  # 
 #  it and/or modify it under the terms of the GNU General        #
 #  Public License as published by the Free Software Foundation,  #
 #  either version 3 of the License, or (at your option)          #
 #  any later version.                                            #
 #                                                                #
 #  GNU BACKGAMMON MOBILE is distributed in the hope that it      #
 #  will be useful, but WITHOUT ANY WARRANTY; without even the    #
 #  implied warranty of MERCHANTABILITY or FITNESS FOR A          #
 #  PARTICULAR PURPOSE.  See the GNU General Public License       #
 #  for more details.                                             #
 #                                                                #
 #  You should have received a copy of the GNU General            #
 #  Public License v3 along with this program.                    #
 #  If not, see <http://http://www.gnu.org/licenses/>             #
 #                                                                #
 ##################################################################
*/

package it.alcacoop.backgammon.layers;

import it.alcacoop.backgammon.GnuBackgammon;
import it.alcacoop.backgammon.fsm.BaseFSM.Events;
import it.alcacoop.backgammon.actions.MyActions;
import it.alcacoop.fibs.Player;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.TreeMap;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL10;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane.ScrollPaneStyle;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;


public class FibsScreen implements Screen {

  private Stage stage;
  private Group g;
  private Image bgImg;
  
  public String username = "";
  public String lastLogin;
  public String fibsRating;
  public TreeMap<String, Player> fibsPlayers;
   
  
  public Map<String, Player> fibsPlayers;
  
  private Label LUsername, LLastLogin;
  
  private Table playerTable;
  private ScrollPane container;
  private float timeout, height, width;
  private LabelStyle evenLs;
  private TextureRegion readyRegion, busyRegion, playingRegion;
  private Drawable evenbg;
  
  
  public FibsScreen(){
    fibsPlayers = Collections.synchronizedMap(new TreeMap<String, Player>());
    playerTable = new Table();
    evenbg = GnuBackgammon.skin.getDrawable("even");

    readyRegion = GnuBackgammon.atlas.findRegion("ready");
    busyRegion = GnuBackgammon.atlas.findRegion("busy");
    playingRegion = GnuBackgammon.atlas.findRegion("playing");
    
    timeout = 0;
    
    TextureRegion  bgRegion = GnuBackgammon.atlas.findRegion("bg");
    bgImg = new Image(bgRegion);
    
    stage = new Stage(Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), false);
    //VIEWPORT DIM = VIRTUAL RES (ON SELECTED TEXTURE BASIS)
    stage.setViewport(GnuBackgammon.Instance.resolution[0], GnuBackgammon.Instance.resolution[1], false);
    stage.addActor(bgImg);

    stage.addListener(new InputListener() {
      @Override
      public boolean keyDown(InputEvent event, int keycode) {
        if(Gdx.input.isKeyPressed(Keys.BACK)||Gdx.input.isKeyPressed(Keys.ESCAPE)) {
          GnuBackgammon.fsm.processEvent(Events.BUTTON_CLICKED, "BACK");
        }
        return super.keyDown(event, keycode);
      }
    });
    
    ClickListener cl = new ClickListener() {
      public void clicked(InputEvent event, float x, float y) {
        GnuBackgammon.fsm.processEvent(Events.BUTTON_CLICKED,((TextButton)event.getListenerActor()).getText().toString().toUpperCase());
      };
    };

    
    LUsername = new Label("", GnuBackgammon.skin);
    LLastLogin = new Label("", GnuBackgammon.skin);
    
    //TextButtonStyle ts = GnuBackgammon.skin.get("default", TextButtonStyle.class);
    evenLs = GnuBackgammon.skin.get("even", LabelStyle.class);
    
    width = stage.getWidth()*0.95f;
    height = stage.getHeight()*0.95f;
    ScrollPaneStyle sps = GnuBackgammon.skin.get("lists", ScrollPaneStyle.class);
    container = new ScrollPane(playerTable, sps);
    container.setFadeScrollBars(false);
    container.setForceOverscroll(false, false);
    container.setOverscroll(false, false);
        
    Table table = new Table();
    //table.debug();
    //Drawable d = GnuBackgammon.skin.getDrawable("default-window");
    //table.setBackground(d);
    table.setFillParent(true);
    table.debug();
    
    table.add(LUsername).expandX().left();
    table.add(LLastLogin).expandX().right();
    
    table.row();
    table.add().fill().expand().colspan(2);
    
    table.row();
    table.add(container).fill().left().colspan(2).height(height*0.7f).width(width*0.6f);
    
    table.row();
    table.add().fill().expand().colspan(2);

    TextButton back = new TextButton("BACK", GnuBackgammon.skin);
    back.addListener(cl);
    table.row();
    table.add(back).expand().colspan(2).height(height*0.11f);
    
    g = new Group();
    g.setWidth(width);
    g.setHeight(height);
    g.addActor(table);
    
    g.setX((stage.getWidth()-g.getWidth())/2);
    g.setY((stage.getHeight()-g.getHeight())/2);
    
    stage.addActor(g);
  }
  
  public Stage getStage() {
    return stage;
  }


  @Override
  public void render(float delta) {
    Gdx.gl.glClearColor(0, 0, 0, 0);
    Gdx.gl.glClear(GL10.GL_COLOR_BUFFER_BIT);
    stage.act(delta);
    stage.draw();
    timeout += delta;
    if (timeout>1) {
      refreshPlayerList();
      timeout = 0;
    }
    
    Table.drawDebug(stage);
  }


  @Override
  public void resize(int width, int height) {
    bgImg.setWidth(stage.getWidth());
    bgImg.setHeight(stage.getHeight());
  }

  
  @Override
  public void show() {
    bgImg.setWidth(stage.getWidth());
    bgImg.setHeight(stage.getHeight());
    Gdx.input.setInputProcessor(stage);
    Gdx.input.setCatchBackKey(true);
    g.setColor(1,1,1,0);
    
    LUsername.setText(username+ " ("+fibsRating+")");
    Date expiry = new Date(Long.parseLong(lastLogin)*1000);
    SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
    String formattedDate = formatter.format(expiry);
    LLastLogin.setText("Last login: "+formattedDate);
    
    g.addAction(MYActions.sequence(Actions.delay(0.1f),Actions.fadeIn(0.6f), Actions.run(new Runnable() {
      @Override
      public void run() {
        refreshPlayerList();
      }
    })));
  }
  
  public void playerChanged(Player p) {
    if (p==null) return;
    if (p.getName()==null) return;
    if (p.getName().equals("")) return;
    if (p.getName().toLowerCase().equals(username.toLowerCase())) return;
   
    String u = p.getName().toLowerCase();
    if (fibsPlayers.containsKey(u)) 
      GnuBackgammon.Instance.fibsPlayersPool.free(fibsPlayers.remove(u));
    fibsPlayers.put(u, p);
  }
  
  public void playerGone(String p) {
    String u = p.toLowerCase();
    if (u.equals(username.toLowerCase())) return;
    if (u.equals("")) return;
    if (fibsPlayers.containsKey(u)) {
      GnuBackgammon.Instance.fibsPlayersPool.free(fibsPlayers.remove(u));
    }
  }
  
  private void refreshPlayerList() {
	Gdx.graphics.setContinuousRendering(true);	
    System.out.println("REFRESH PLAYER LIST");
    //float width = stage.getWidth()*0.95f*0.6f;
    playerTable.remove();
    playerTable.reset();
    float twidth = width*0.6f;
    int n = 0;
    
    synchronized (fibsPlayers) {
      for(Map.Entry<String,Player> entry : fibsPlayers.entrySet()) {
        n++;
        Player value = entry.getValue();
        Image status;
        if (value.isPlaying())  status =new Image(playingRegion);
        else if (!value.isReady()) status = new Image(busyRegion);
        else status = new Image(readyRegion);

        Table t = new Table();
        if (n%2==0) t.setBackground(evenbg);
        t.add(status).fill();
        
        playerTable.row();
        
        if (n%2==0) {
          playerTable.add(new Label(" "+value.getName()+" ("+value.getRating()+")", evenLs)).left().width(twidth*0.7f).height(height*0.11f);
          playerTable.add(t).expandX().fillX().height(height*0.11f);
        } else {
          playerTable.add(new Label(" "+value.getName()+" ("+value.getRating()+")", GnuBackgammon.skin)).left().width(twidth*0.7f).height(height*0.11f);
          playerTable.add(t).height(height*0.11f);
        }
      }
      
      playerTable.row();
      playerTable.add().expand().fill().colspan(2);
    }
    container.setWidget(playerTable);
	Gdx.graphics.setContinuousRendering(false);	
  }

  @Override
  public void hide() {
  }

  @Override
  public void pause() {
  }

  @Override
  public void resume() {
    Gdx.graphics.requestRendering();
  }

  @Override
  public void dispose() {
  }
}
