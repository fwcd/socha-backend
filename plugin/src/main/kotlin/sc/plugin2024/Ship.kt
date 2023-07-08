package sc.plugin2024

import com.thoughtworks.xstream.annotations.XStreamAsAttribute
import com.thoughtworks.xstream.annotations.XStreamOmitField
import sc.api.plugins.*
import kotlin.math.abs

/**
 * This class represents a Ship in the game.
 *
 * @property points The current points of the ship.
 * @property position The current coordinate of the ship.
 * @property direction The direction the ship is facing.
 * @property speed The current speed of the ship.
 * @property coal The current amount of coal units of the ship.
 * @property tile The tile index where the ship is located.
 * @property passengers The number of passengers collected by the player.
 * @property movement This field is relevant only for the server.
 * @property freeTurns This field is relevant only for the server.
 * @property freeAcc This field is relevant only for the GUI.
 */
data class Ship(override val id: Int, @XStreamAsAttribute override val position: Coordinates, override val team: ITeam): IPiece {
    
    /**
     * Aktuelle Punktzahl des Spielers abhängig vom Fortschritt auf dem Spielfeld
     * und der Anzahl der eingesammelten Passagiere
     */
    @XStreamAsAttribute
    val points = 0
    
    /** Richtung, in die das Schiff ausgerichtet ist. */
    @XStreamAsAttribute
    var direction: HexDirection = HexDirection.RIGHT
    
    /**
     * aktuelle Geschwindigkeit des Schiffes des Spielers
     */
    @XStreamAsAttribute
    var speed = 0
    
    /**
     * aktuelle Anzahl der Kohleeinheiten des Schiffes des Spielers
     */
    @XStreamAsAttribute
    var coal = 0
    
    /**
     * Spielsegment, auf dem sich das Schiff des Spielers befindet
     */
    @XStreamAsAttribute
    val tile = 0
    
    /**
     * Anzahl der vom Spieler eingesammelten Passagiere
     */
    @XStreamAsAttribute
    val passengers = 0
    
    /**
     * Nur fuer den Server relevant
     */
    @XStreamOmitField
    var movement = 0
    
    /**
     * Nur fuer den Server relevant
     */
    @XStreamOmitField
    var freeTurns = 0
    
    /**
     * Nur fuer die Gui relevant
     */
    @XStreamOmitField
    var freeAcc = 0
    
}