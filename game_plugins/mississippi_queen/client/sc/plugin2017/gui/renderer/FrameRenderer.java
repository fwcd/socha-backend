/**
 *
 */
package sc.plugin2017.gui.renderer;

import java.awt.Image;
import java.awt.event.MouseEvent;
import java.util.LinkedHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import processing.core.PApplet;
import sc.plugin2017.Action;
import sc.plugin2017.EPlayerId;
import sc.plugin2017.GameState;
import sc.plugin2017.Move;
import sc.plugin2017.PlayerColor;
import sc.plugin2017.gui.renderer.primitives.Background;
import sc.plugin2017.gui.renderer.primitives.BoardFrame;
import sc.plugin2017.gui.renderer.primitives.GameEndedDialog;
import sc.plugin2017.gui.renderer.primitives.GuiBoard;
import sc.plugin2017.gui.renderer.primitives.GuiConstants;
import sc.plugin2017.gui.renderer.primitives.GuiTile;
import sc.plugin2017.gui.renderer.primitives.HexField;
import sc.plugin2017.gui.renderer.primitives.ProgressBar;
import sc.plugin2017.gui.renderer.primitives.SideBar;
import sc.plugin2017.util.InvalidMoveException;

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
  private GameState backUp;
  public Move currentMove;
  public boolean humanPlayer;
  private boolean humanPlayerMaxTurn;
  public int maxTurn;
  private EPlayerId id;

  private boolean initialized = false;

  public GuiBoard guiBoard;

  private Background background;

  private ProgressBar progressBar;
  private SideBar sideBar;
  private BoardFrame boardFrame;

  public LinkedHashMap<HexField, Action> stepPossible;

  public FrameRenderer(int frameWidth, int frameHeight) {
    super();

    this.width = frameWidth;
    this.height = frameHeight;
    this.humanPlayer = false;
    this.humanPlayerMaxTurn = false;
    this.id = EPlayerId.OBSERVER;

    RenderConfiguration.loadSettings();

    background = new Background(this);
    guiBoard = new GuiBoard(this);
    progressBar = new ProgressBar(this);
    sideBar = new SideBar(this);
    boardFrame = new BoardFrame(this);
    stepPossible = new LinkedHashMap<HexField, Action>();

  }

  @Override
  public void setup() {
    super.setup();
    logger.debug("Dimension when creating board: (" + width + ","
        + height + ")");
    maxTurn = -1;
    // choosing renderer from options - using P2D as default (currently it seems
    // that only the java renderer works).
    //
    // NOTE that setting the size needs to be the first action of the setup
    // method (as stated in the processing reference).
    if (RenderConfiguration.optionRenderer.equals("JAVA2D")) {
      logger.debug("Using Java2D as Renderer");
      size(width, height, JAVA2D);
    } else if (RenderConfiguration.optionRenderer.equals("P3D")) {
      logger.debug("Using P3D as Renderer");
      size(width, height, P3D);
    } else {
      logger.debug("Using P2D as Renderer");
      size(width, height, P2D);
    }
    smooth(RenderConfiguration.optionAntiAliasing); // Anti Aliasing

    GuiConstants.generateFonts(this);

    guiBoard.setup();

    initialized = true;
  }

  @Override
  public void draw() {
    if (!initialized) {
      return;
    }
    background.draw();
    guiBoard.draw();
    progressBar.draw();
    sideBar.draw();
    boardFrame.draw();
    if (currentGameState != null && currentGameState.gameEnded()) {
      GameEndedDialog.draw(this);
    }
    text(String.format("Mouse position: %d,%d", mouseX, mouseY), 20, 60);
  }

  public void updateGameState(GameState gameState) {
    int lastTurn = -1;
    if (currentGameState != null) {
      lastTurn = currentGameState.getTurn();
    }
    currentGameState = gameState;
    currentMove = new Move();
    // needed for simulation of actions
    currentGameState.getRedPlayer().setMovement(currentGameState.getRedPlayer().getSpeed());
    currentGameState.getBluePlayer().setMovement(currentGameState.getBluePlayer().getSpeed());
    currentGameState.getCurrentPlayer().setFreeTurns(currentGameState.isFreeTurn() ? 2 : 1);
    currentGameState.getCurrentPlayer().setFreeAcc(1);
    // make backup of gameState
    try {
      backUp = currentGameState.clone();
    } catch (CloneNotSupportedException e) {
      // TODO Auto-generated catch block
      System.out.println("Clone of Backup failed");
      e.printStackTrace();
    }

    if (gameState != null && gameState.getBoard() != null) {
      logger.debug("updating gui board gamestate");
      updateView(currentGameState);
    } else {
      logger.error("got gamestate without board");
    }
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
    redraw();
  }

  public void requestMove(int maxTurn, EPlayerId id) {
    int turn = currentGameState.getTurn();
    this.id = id;
    if (turn % 2 == 1) {
      if (id == EPlayerId.PLAYER_ONE) {
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

  private void updateView(GameState gameState) {
    if (gameState != null && gameState.getBoard() != null) {
      gameState.getRedPlayer().setPoints(gameState.getPointsForPlayer(PlayerColor.RED));
      gameState.getBluePlayer().setPoints(gameState.getPointsForPlayer(PlayerColor.BLUE));
      guiBoard.update(gameState.getVisibleBoard(), gameState.getRedPlayer(),
          gameState.getBluePlayer(), gameState.getCurrentPlayerColor());
    }
    redraw();
  }

  @Override
  public void mouseMoved(MouseEvent e) {
    super.mouseMoved(e);
    redraw();
  }

  @Override
  public void mouseDragged(MouseEvent e) {
    super.mouseDragged(e);
    redraw();
  }

  @Override
  public void mouseClicked(MouseEvent e) {
    super.mouseClicked(e);
    if(isHumanPlayer() && maxTurn == currentGameState.getTurn()) {
      // first the gui buttons
      /*
      if(currentGameState.getCurrentPlayer()
        .getField( currentGameState.getBoard()).getType() != FieldType.SANDBANK) {
        if(guiBoard.left.isClicked()) {
          Turn turn = new Turn(1);
          currentMove.actions.add(turn);
          try {
            turn.perform(currentGameState, currentGameState.getCurrentPlayer());
          } catch (InvalidMoveException e1) {
            System.out.println("Failed to perform move of user, please report if this happens");
            e1.printStackTrace();
          }
        }
        if(guiBoard.right.isClicked()) {
          Turn turn = new Turn(-1);
          currentMove.actions.add(turn);
          try {
            turn.perform(currentGameState, currentGameState.getCurrentPlayer());
          } catch (InvalidMoveException e1) {
            System.out.println("Failed to perform move of user, please report if this happens");
            e1.printStackTrace();
          }
        }
        if(currentGameState.getCurrentPlayer().getSpeed() != 1) {
          if(guiBoard.speedDown.isClicked()) {
            Acceleration acc = new Acceleration(-1);
            currentMove.actions.add(acc);
            try {
              acc.perform(currentGameState, currentGameState.getCurrentPlayer());
            } catch (InvalidMoveException e1) {
              System.out.println("Failed to perform move of user, please report if this happens");
              e1.printStackTrace();
            }
          }
        }
        if(currentGameState.getCurrentPlayer().getSpeed()  != 6) {
          if(guiBoard.speedUp.isClicked()) {
            Acceleration acc = new Acceleration(1);
            currentMove.actions.add(acc);
            try {
              acc.perform(currentGameState, currentGameState.getCurrentPlayer());
            } catch (InvalidMoveException e1) {
              System.out.println("Failed to perform move of user, please report if this happens");
              e1.printStackTrace();
            }
          }
        }
      }
      if(guiBoard.send.isClicked()) {
        sendMove();
      }
      if(guiBoard.cancel.isClicked()) {
        try {
          currentGameState = backUp.clone();
        } catch (CloneNotSupportedException e1) {
          System.out.println("Clone of backup failed");
          e1.printStackTrace();
        }
        updateGameState(currentGameState);
        redraw();
        return;
      }
      */
      // then field clicks
      HexField clicked = getFieldCoordinates(mouseX, mouseY);
      Action action = stepPossible.get(clicked);
      if(action != null) {
        try {
          action.perform(currentGameState, currentGameState.getCurrentPlayer());
        } catch (InvalidMoveException e1) {
          System.out.println("Failed to perform move of user, please report if this happens");
          e1.printStackTrace();
        }
        currentMove.actions.add(action);
        logger.debug(
            String.format("current player position is now %d, %d",
                currentGameState.getCurrentPlayer().getField(currentGameState.getBoard()).getX(),
                currentGameState.getCurrentPlayer().getField(currentGameState.getBoard()).getY()
            )
        );
        updateView(currentGameState);
      }
    }
    updateView(currentGameState);
    redraw();
  }

  private void sendMove() {
    Move move = new Move();
    for (Action action : currentMove.actions) {
      move.actions.add(action);
    }
    move.setOrderInActions();
    RenderFacade.getInstance().sendMove(move);
  }

  private HexField getFieldCoordinates(int x, int y) {
    HexField coordinates;

    for (GuiTile tile : guiBoard.getTiles()) {
      coordinates = tile.getFieldCoordinates(x,y);
      if(coordinates != null) {
        return coordinates;
      }
    }
    return null;
  }

  /**
   * Is called automatically (by RenderFacade) when window dimensions have
   * changed.
   */
  @Override
  public void resize(int width, int height) {
    logger.debug(String.format("updating dimension: %d,%d", width, height));
    super.resize(width, height); // this is actually needed to propagate size, otherwise you will get exceptions that dimensions need to be >=0

    background.resize(width, height);
    guiBoard.resize();

    logger.debug(String.format("dimension of PApplet: %d,%d", this.width, this.height));
    // progressBar, sideBar and boardFrame have no own dimensions, they access
    // parents dimensions directly and don't need to be resized.
  }

  @Override
  public void keyPressed() {
    super.keyPressed();
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
  }
}
