/**
 * 
 */
package sc.plugin2017.gui.renderer;

import java.awt.Image;
import java.awt.event.MouseEvent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import processing.core.PApplet;
import sc.plugin2017.gui.renderer.primitives.GameEndedDialog;
import sc.plugin2017.gui.renderer.primitives.GuiConstants;
import sc.plugin2017.gui.renderer.primitives.Background;
import sc.plugin2017.gui.renderer.primitives.BoardFrame;
import sc.plugin2017.gui.renderer.primitives.GuiPlayer;
import sc.plugin2017.gui.renderer.primitives.ProgressBar;
import sc.plugin2017.gui.renderer.primitives.SideBar;
import sc.plugin2017.gui.renderer.primitives.GuiBoard;
import sc.plugin2017.GameState;
import sc.plugin2017.Move;
import sc.plugin2017.util.Constants;
import sc.plugin2017.EPlayerId;

/**
 * @author soeren
 */

public class FrameRenderer extends PApplet {

  /**
   * 
   */
  private static final long serialVersionUID = 1L;
  private static final Logger logger = LoggerFactory
      .getLogger(FrameRenderer.class);

  public GameState currentGameState;
  private boolean humanPlayer;
  private boolean humanPlayerMaxTurn;
  private int maxTurn;
  private EPlayerId id;


  public GuiBoard guiBoard;
  
  private Background background;
  
  private ProgressBar progressBar;
  private SideBar sideBar;
  private BoardFrame boardFrame;
  
  public GuiPlayer red;
  public GuiPlayer blue;
  
  public FrameRenderer() {
    super();

    // logger.debug("calling frameRenderer.size()");
    this.humanPlayer = false;
    this.humanPlayerMaxTurn = false;
    this.id = EPlayerId.OBSERVER;

    RenderConfiguration.loadSettings();
    
    background = new Background(this);
    logger.debug("Dimension when creating board: (" + this.width + ","
        + this.height + ")");
    guiBoard = new GuiBoard(this);
    progressBar = new ProgressBar(this);
    sideBar = new SideBar(this);
    red = new GuiPlayer(this);
    blue = new GuiPlayer(this);


    boardFrame = new BoardFrame(this);
    // logger.debug("Constructor finished");

    // load Images
    //currently no images
  }

  public void setup() {
    maxTurn = -1;
    // choosing renderer from options - using P2D as default
    if (RenderConfiguration.optionRenderer.equals("JAVA2D")) {
      logger.debug("Using Java2D as Renderer");
      size(this.width, this.height, JAVA2D);
    } else if (RenderConfiguration.optionRenderer.equals("P3D")) {
      logger.debug("Using P3D as Renderer");
      size(this.width, this.height, P3D);
    } else {
      logger.debug("Using P2D as Renderer");
      size(this.width, this.height, P2D);
    }

    smooth(RenderConfiguration.optionAntiAliasing); // Anti Aliasing

    // initial draw
    GuiConstants.generateFonts(this);
    redraw();
    noLoop(); // prevent thread from starving everything else

  }

  public void draw() {
    background.draw();
    guiBoard.draw();
    progressBar.draw();
    // sideBar.draw(); TODO
    boardFrame.draw();
    red.draw();
    blue.draw();
    if (currentGameState != null && currentGameState.gameEnded()) {
      GameEndedDialog.draw(this);
    }
  }

  public void updateGameState(GameState gameState) {
    int lastTurn = -1;
    if (currentGameState != null) {
      lastTurn = currentGameState.getTurn();
    }
    currentGameState = gameState;
    if (gameState != null && gameState.getVisibleBoard() != null)
      guiBoard.update(gameState.getVisibleBoard());
    if ((currentGameState == null || lastTurn == currentGameState.getTurn() - 1)) {

      if (maxTurn == currentGameState.getTurn() - 1) {

        maxTurn++;
        humanPlayerMaxTurn = false;
      }
    }
    humanPlayer = false;
    if (currentGameState != null && maxTurn == currentGameState.getTurn()
        && humanPlayerMaxTurn) {
      humanPlayer = true;
    }
  }

  public void requestMove(int maxTurn, EPlayerId id) {
    int turn = currentGameState.getTurn();
    this.id = id;
    if (turn % 2 == 1) {
      // System.out.println("Blauer Spieler ist dran");
      if (id == EPlayerId.PLAYER_ONE) {
        // System.out.println("Spielerupdate");
        this.id = EPlayerId.PLAYER_TWO;
      }
    }
    // this.maxTurn = maxTurn;
    this.humanPlayer = true;
    humanPlayerMaxTurn = true;
  }

  public Image getImage() {
    // TODO return an Image of the current board
    return null;
  }

  public void mouseClicked(MouseEvent e) {
    
  }

  public void mousePressed(MouseEvent e) {
    
  }

  public void mouseReleased(MouseEvent e) {
    // TODO implemnet human player
  }

  private int[] getFieldCoordinates(int x, int y) {
    // TODO get edges and set coordinates according -> return field coordinates
    // TODO check from coordinates x, y(position) on which field(x,y) ()coordniates you are
    return null;
  }

  public void resize(int width, int height) {
    background.resize(width, height);
    guiBoard.resize(width, height);
    red.resize(width, height);
    blue.resize(width, height);
  }

  /*
   * Hack! wenn das Fenster resized wird, wird setBounds aufgerufen. hier
   * rufen wir resize auf, um die Komponenten auf die richtige Größe zu
   * bringen.
   */
  public void setBounds(int x, int y, int width, int height) {
    super.setBounds(x, y, width, height);
    this.resize(width, height);
  }

  public void keyPressed() {
    if (key == 'c' || key == 'C') {
      new RenderConfigurationDialog(FrameRenderer.this);
    }

  }

  public boolean isHumanPlayer() {
    return humanPlayer;
  }

  public EPlayerId getId() {
    return id;
  }

  public void killAll() {
noLoop();
    
    if(background != null) {
      background.kill();
    }
    if(guiBoard != null) {
      // TODO kill board
      guiBoard.kill();
    }
    if(progressBar != null) {
      progressBar.kill();
    }
    if(sideBar != null) {
      sideBar.kill();
    }
    if(boardFrame != null) {
      boardFrame.kill();
    }
    if(red != null) {
      red.kill();
    }
    if(blue != null) {
      blue.kill();
    }
  }
}
