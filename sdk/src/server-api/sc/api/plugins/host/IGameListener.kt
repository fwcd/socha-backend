package sc.api.plugins.host

import sc.api.plugins.IGameState
import sc.framework.plugins.Player
import sc.shared.PlayerScore

interface IGameListener {
    fun onGameOver(results: Map<Player, PlayerScore>)
    fun onStateChanged(data: IGameState, observersOnly: Boolean)
}