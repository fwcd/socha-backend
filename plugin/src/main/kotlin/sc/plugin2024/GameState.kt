package sc.plugin2024

import com.thoughtworks.xstream.annotations.XStreamAlias
import com.thoughtworks.xstream.annotations.XStreamAsAttribute
import com.thoughtworks.xstream.annotations.XStreamImplicit
import com.thoughtworks.xstream.annotations.XStreamOmitField
import sc.api.plugins.*
import sc.plugin2024.actions.Acceleration
import sc.plugin2024.util.PluginConstants
import sc.shared.InvalidMoveException


/**
 * Der aktuelle Spielstand.
 *
 * Er hält alle Informationen zur momentanen Runde,
 * mit deren Hilfe der nächste Zug berechnet werden kann.
 */
@XStreamAlias(value = "state")
data class GameState @JvmOverloads constructor(
    /** Das aktuelle Spielfeld. */
    override val board: Board = Board(),
    /** Die Anzahl an bereits getätigten Zügen. */
    @XStreamAsAttribute override var turn: Int = 0,
    /** Der zuletzt gespielte Zug. */
    override var lastMove: Move? = null,
) : TwoPlayerGameState<Move>(Team.ONE) {

    constructor(other: GameState) : this(other.board.clone(), other.turn, other.lastMove, other.fishes.clone())

    override val currentTeam: Team
        get() = currentTeamFromTurn().run { takeIf { !immovable(it) } ?: opponent() }

    /**
     * Der Index des am weitesten vom Start entfernten Segmentes, welches bisher aufgedeckt wurde. Wird nur intern verwendet.
     */
    @XStreamOmitField
    private val latestTileIndex = 0

    /**
     * Wurde der Spieler im LastMove abgedrängt. Falls ja ist eine weitere Drehaktion möglich
     */
    @XStreamAsAttribute
    private val freeTurn = false

    /**
     * Liste von Aktionen aus denen der Zug besteht. Die Reihenfolge, in der die
     * Aktionen ausgeführt werden, wird NICHT durch die Reihenfolge in der Liste
     * bestimmt, sondern durch die Werte im order-Attribut jedes Action objektes:
     * Die Aktionen werden nach dem order-Attribut aufsteigend sortiert
     * ausgeführt.
     */
    @XStreamImplicit
    var actions: List<Action>? = null

    override fun performMove(move: Move) {
        orderActions()
        var order = 0
        var onEnemy: Boolean

        if (getActions().isEmpty()) {
            throw InvalidMoveException("Der Zug enthält keine Aktionen")
        }
        for (action in actions) {
            onEnemy = player.getX() === state.getOtherPlayer().getX() && player.getY() === state.getOtherPlayer().getY()
            if (onEnemy && action.getClass() !== Push::class.java) {
                throw InvalidMoveException(
                    "Wenn du auf einem gegnerischen Schiff landest," + " muss darauf eine Abdrängaktion folgen."
                )
            }
            var lastAction: Action? = null
            if (order > 0) {
                lastAction = actions!![action.order - 1]
            }
            if (lastAction != null && lastAction.getClass() === Advance::class.java) {
                if ((lastAction as Advance).endsTurn) {
                    throw InvalidMoveException("Zug auf eine Sandbank muss letzte Aktion sein.")
                }
            }
            if (action.getClass() === Turn::class.java) {
                if (player.getField(state.getBoard()).getType() === FieldType.SANDBANK) {
                    throw InvalidMoveException("Du kannst nicht auf einer Sandbank drehen")
                }
                (action as Turn).perform(state, player) // count turns decreases
                // freeTurns and reduces coal if
                // necessary
            } else if (action.getClass() === Acceleration::class.java) {
                val acc: Acceleration = action as Acceleration
                if (acc.order !== 0) {
                    throw InvalidMoveException("Du kannst nur in der ersten Aktion beschleunigen.")
                }
                acc.perform(state, player) // coal is decreased in perform
            } else {
                action.perform(state, player) // Speed and movement is decreased here
            }
            ++order
        }
        // when stepping onto the opponents field, the opponent has to be pushed
        // away
        // when stepping onto the opponents field, the opponent has to be pushed
        // away
        if (player.getX() === state.getOtherPlayer().getX() && player.getY() === state.getOtherPlayer().getY()) {
            throw InvalidMoveException("Der Zug darf nicht auf dem Gegner enden.")
        }
        // pick up passenger
        // pick up passenger
        if (player.getSpeed() === 1 && player.canPickupPassenger(state.getBoard())) {
            state.removePassenger(player)
        }
        // otherplayer could possible pick up Passenger in enemy turn
        // otherplayer could possible pick up Passenger in enemy turn
        if (state.getOtherPlayer().getSpeed() === 1 && state.getOtherPlayer().canPickupPassenger(state.getBoard())) {
            state.removePassenger(state.getOtherPlayer())
        }
        if (player.getCoal() < 0) {
            throw InvalidMoveException("Nicht genug Kohle für den Zug vorhanden.")
        }
        if (player.getMovement() > 0) { // check whether movement points are left
            throw InvalidMoveException(("Es sind noch " + player.getMovement()).toString() + " Bewegungspunkte übrig.")
        }
        if (player.getMovement() < 0) { // check whether movement points are left
            throw InvalidMoveException(
                ("Es sind " + Math.abs(player.getMovement())).toString() + " Bewegungspunkte zuviel verbraucht worden."
            )
        }
    }

    val currentPieces
        get() = board.filterValues { it.penguin == currentTeam }

    val penguinsPlaced
        get() = currentPieces.size == PluginConstants.PENGUINS

    override fun getPossibleMoves(): List<Move> =
        if (penguinsPlaced) {
            currentPieces.flatMap { (pos, _) -> board.possibleMovesFrom(pos) }
        } else {
            board.filterValues { it.fish == 1 }.map { Move(null, it.key) }
        }

    fun canPlacePenguin(pos: Coordinates) = !penguinsPlaced && board[pos].fish == 1

    fun immovable(team: Team? = null) =
        board.getPenguins()
            .filter { team == null || it.second == team }
            .takeIf { it.size == PluginConstants.PENGUINS * (if (team == null) Team.values().size else 1) }
            ?.all { pair -> pair.first.hexNeighbors.all { board.getOrEmpty(it).fish == 0 } } ?: false

    override val isOver: Boolean
        get() = immovable()

    /** Berechne die Punkteanzahl für das gegebene Team. */
    override fun getPointsForTeam(team: ITeam): IntArray =
        intArrayOf(fishes[team.index])

    override fun clone() = GameState(this)

    override fun toString(): String =
        "GameState$turn - ${currentTeam.color} (Fische)"

    // Generated Stuff below

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GameState) return false

        if (board != other.board) return false
        if (turn != other.turn) return false
        if (lastMove != other.lastMove) return false
        if (!fishes.contentEquals(other.fishes)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = board.hashCode()
        result = 31 * result + turn
        result = 31 * result + (lastMove?.hashCode() ?: 0)
        result = 31 * result + fishes.contentHashCode()
        return result
    }

}
