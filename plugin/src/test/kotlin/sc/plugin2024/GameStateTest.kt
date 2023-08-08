package sc.plugin2024

import com.thoughtworks.xstream.XStream
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.*
import io.kotest.matchers.collections.*
import sc.api.plugins.Team
import sc.plugin2024.actions.Advance

class GameStateTest: FunSpec({
    val gameState = GameState()
    
    xtest("serializes nicely") {
        val xStream = XStream().apply {
            processAnnotations(GameState::class.java)
            processAnnotations(Segment::class.java)
            XStream.setupDefaultSecurity(this)
            allowTypesByWildcard(arrayOf("sc.plugin2024.*"))
        }
        
        val serialized = xStream.toXML(gameState)
        
        serialized shouldBe """<state turn="0">
    <board>
    </board>
    <lastMove>
    </lastMove>
    <ships>
        <ship>
        </ship>
        <ship>
        </ship>
    </ships>
</state>"""
    }
    
    test("currentTeam should be determined correctly") {
        gameState.startTeam shouldBe Team.ONE
        gameState.currentTeam shouldBe gameState.startTeam
        gameState.determineCurrentTeam() shouldBe gameState.currentTeam
        gameState.currentShip shouldBe gameState.ships.first()
        gameState.otherShip shouldBe gameState.ships.last()
        gameState.turn++
        gameState.currentTeam shouldBe gameState.startTeam.opponent()
        gameState.turn++
        gameState.currentTeam shouldBe gameState.startTeam
    }
    
    test("getPossiblePushs") {
        gameState.getPossiblePushs().shouldBeEmpty()
        gameState.currentShip.position = gameState.otherShip.position
        gameState.getPossiblePushs().shouldNotBeEmpty()
    }
    
    test("getPossibleTurns") {
        gameState.getPossibleTurns(0).shouldHaveSize(2)
        gameState.getPossibleTurns(1).shouldHaveSize(4)
    }
    
    context("getPossibleAdvances") {
        test("from starting position") {
            gameState.getPossibleAdvances() shouldHaveSingleElement Advance(1)
        }
        test("from sandbank")  {
            gameState.currentShip.position =
                    gameState.board.findNearestFieldTypes(gameState.currentShip.position, Field.SANDBANK::class).first()
            gameState.getPossibleAdvances() shouldContainAnyOf listOf(Advance(1), Advance(-1))
        }
    }
    
    test("getPossibleAccelerations") {
        gameState.getPossibleAccelerations(0).size shouldBe 1
        gameState.getPossibleAccelerations(1).size shouldBe 2
    }
    
    context("getPossibleActions") {
        test("from starting position") {
            gameState.getPossibleActions(0) shouldHaveSize 12
        }
        test("push") {
            gameState.currentShip.position = gameState.otherShip.position
            gameState.getPossibleActions(1) shouldHaveSize 4
        }
    }
    
    test("getSensibleMoves") {
        val sensibleMoves = gameState.getSensibleMoves()
        sensibleMoves shouldHaveSize 7
    }
})