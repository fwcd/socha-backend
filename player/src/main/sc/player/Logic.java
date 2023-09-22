package sc.player;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.thoughtworks.xstream.XStream;
import sc.api.plugins.IGameState;
import sc.api.plugins.IMove;
import sc.api.plugins.TwoPlayerGameState;
import sc.shared.GameResult;
import sc.networking.XStreamProvider;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileAttribute;
import java.util.List;

/**
 * Das Herz des Clients:
 * Eine simple Logik, die zufaellige gueltige Zuege macht.
 * <p>
 * Ausserdem werden zum Spielverlauf Konsolenausgaben gemacht.
 */
public class Logic implements IGameHandler {
  private static final Logger log = LoggerFactory.getLogger(Logic.class);

  /** Aktueller Spielstatus. */
  private TwoPlayerGameState<IMove> gameState;

  private XStream xStream = XStreamProvider.loadPluginXStream();

  /** In dieser Methode habt ihr 2 Sekunden (berechnet etwas Puffer ein) Zeit,
   * um euren nächsten Zug zu planen. */
  @Override
  public IMove calculateMove() {
    long startTime = System.currentTimeMillis();
    log.info("Es wurde ein Zug von {} angefordert.", gameState.getCurrentTeam());

    List<IMove> possibleMoves = gameState.getSensibleMoves();
    
    // Hier intelligente Strategie zur Auswahl des Zuges einfügen
    IMove move = possibleMoves.get((int) (Math.random() * possibleMoves.size()));

    log.info("Sende {} nach {}ms.", move, System.currentTimeMillis() - startTime);
    return move;
  }

  /** Ein neuer Spielstatus ist verfügbar, d.h. ein Zug wurde erfolgreich ausgeführt. */
  @Override
  public void onUpdate(IGameState gameState) {
    this.gameState = (TwoPlayerGameState<IMove>) gameState;

    // DEBUG
    Path dirPath = Paths.get("game-dump");
    Path path = dirPath.resolve("" + gameState.getTurn());
    try {
      Files.createDirectories(dirPath);
      Path statePath = dirPath.resolve(String.format("%02d.state.xml", gameState.getTurn()));
      Files.writeString(statePath, xStream.toXML(gameState));

      Path movesPath = dirPath.resolve(String.format("%02d.moves.xml", gameState.getTurn()));
      Files.writeString(movesPath, xStream.toXML(this.gameState.getSensibleMoves()));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    log.info("Zug: {} Dran: {}", gameState.getTurn(), gameState.getCurrentTeam());
  }

  /** Wird aufgerufen, wenn das Spiel beendet ist. */
  public void onGameOver(GameResult data) {
    log.info("Das Spiel ist beendet, Ergebnis: {}", data);
  }

  /** Wird aufgerufen, wenn der Server einen Fehler meldet.
   * Bedeutet auch den Abbruch des Spiels. */
  @Override
  public void onError(String error) {
    log.warn("Fehler: {}", error);
  }
}
