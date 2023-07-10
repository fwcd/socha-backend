package sc.plugin2024

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import sc.api.plugins.IMove
import sc.api.plugins.Team
import sc.framework.plugins.AbstractGame
import sc.shared.MoveMistake
import sc.plugin2024.util.WinReason
import sc.plugin2024.util.GamePlugin
import sc.shared.InvalidMoveException
import sc.shared.WinCondition

fun <T> Collection<T>.maxByNoEqual(selector: (T) -> Int): T? =
        fold(Int.MIN_VALUE to (null as T?)) { acc, pos ->
            val value = selector(pos)
            when {
                value > acc.first -> value to pos
                value == acc.first -> value to null
                else -> acc
            }
        }.second

class Game(override val currentState: GameState = GameState()): AbstractGame(GamePlugin.PLUGIN_ID) {
    companion object {
        val logger: Logger = LoggerFactory.getLogger(Game::class.java)
    }
    
    val isGameOver: Boolean
        get() = currentState.isOver
    
    override fun onRoundBasedAction(move: IMove) {
        if(move !is Move)
            throw InvalidMoveException(MoveMistake.INVALID_FORMAT)
        
        AbstractGame.logger.debug("Performing {}", move)
        currentState.performMove(move)
        AbstractGame.logger.debug("Current State: ${currentState.longString()}")
    }
    
    /**
     * Checks whether and why the game is over.
     *
     * @return null if any player can still move, otherwise a WinCondition with the winner and reason.
     */
    override fun checkWinCondition(): WinCondition? {
        if(!isGameOver) return null
        val currentShip: Ship = currentState.currentTeam.pieces.first() as Ship
        val otherShip: Ship = currentState.otherTeam.pieces.first() as Ship
        
        return when {
            // victory by points
            currentShip.points > otherShip.points -> WinCondition(currentState.currentTeam, WinReason.DIFFERING_SCORES)
            currentShip.points < otherShip.points -> WinCondition(currentState.otherTeam, WinReason.DIFFERING_SCORES)
            // victory by passengers
            currentShip.passengers > otherShip.passengers -> WinCondition(currentState.currentTeam, WinReason.DIFFERING_PASSENGERS)
            currentShip.passengers < otherShip.passengers -> WinCondition(currentState.otherTeam, WinReason.DIFFERING_PASSENGERS)
            else -> WinCondition(null, WinReason.EQUAL_PASSENGERS)
        }
    }
    
    override fun toString(): String =
            "Game(${
                when {
                    isGameOver -> "OVER, "
                    isPaused -> "PAUSED, "
                    else -> ""
                }
            }players=$players, gameState=$currentState)"
    
}