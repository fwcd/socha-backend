package sc.plugin2023

import io.kotest.core.spec.style.FunSpec
import io.kotest.inspectors.forAll
import io.kotest.matchers.*
import io.kotest.matchers.collections.*
import io.kotest.matchers.ints.*
import io.kotest.matchers.maps.shouldHaveSize
import io.kotest.matchers.nulls.*
import io.kotest.matchers.string.*
import sc.api.plugins.Coordinates
import sc.api.plugins.Team
import sc.helpers.shouldSerializeTo
import sc.helpers.testXStream
import sc.plugin2023.util.PluginConstants

class BoardTest: FunSpec({
    context("Board generation") {
        val generatedBoard = Board()
        test("works properly") {
            generatedBoard shouldHaveSize PluginConstants.BOARD_SIZE * PluginConstants.BOARD_SIZE
            generatedBoard.forAll {
                it.penguin.shouldBeNull()
                it.fish shouldBeInRange 1..4
            }
            
            generatedBoard.getPenguins() shouldHaveSize 0
            generatedBoard[1 y 1] = Team.ONE
            generatedBoard.getPenguins() shouldHaveSize 1
            
            arrayOf(-1 y 1, -2 y 0, -1 y 3, -1 y 0).forAll {
                generatedBoard.getOrNull(it).shouldBeNull()
            }
            (0 until PluginConstants.BOARD_SIZE).map { (it * 2) y 2 }.forAll {
                val field = generatedBoard[it]
                field.fish shouldBeInRange 1..4
                generatedBoard[it.x, it.y] shouldBe field
            }
        }
        test("clones well") {
            val board = makeBoard(0 y 0 to 1)
            board.getPenguins() shouldHaveSize 1
            val clone = board.clone()
            board[1 y 1] = Team.ONE
            board.getPenguins() shouldHaveSize 2
            clone.getPenguins() shouldHaveSize 1
            clone shouldBe makeBoard(0 y 0 to 1)
        }
    }
    context("Board generates Moves") {
        val board = makeBoard(0 y 0 to 0)
        test("many possible moves") {
            // right, right down
            board.possibleMovesFrom(0 y 0) shouldHaveSize 2 * (PluginConstants.BOARD_SIZE - 1)
        }
        test("restricted moves") {
            board[1 y 1] = Team.ONE
            board.possibleMovesFrom(0 y 0) shouldHaveSize PluginConstants.BOARD_SIZE - 1
        }
    }
    context("Board calculates diffs") {
        // TODO
        //val board = makeBoard(0 y 0 to "r", 2 y 0 to "r")
        //test("empty for itself") {
        //    board.diff(board).shouldBeEmpty()
        //    board.diff(board.clone()).shouldBeEmpty()
        //    board.clone().diff(board).shouldBeEmpty()
        //}
        //test("one moved and one unmoved piece") {
        //    val move = Move(0 y 0, 2 y 1)
        //    val newBoard = board.clone()
        //    newBoard.movePiece(move)
        //    board.diff(newBoard) shouldContainExactly listOf(move)
        //}
        //test("both pieces moved") {
        //    val newBoard = makeBoard(2 y 1 to "r", 1 y 2 to "r")
        //    board.diff(newBoard) shouldHaveSize 2
        //}
        //test("one piece vanished") {
        //    val newBoard = makeBoard(2 y 0 to "r")
        //    val move = board.diff(newBoard).single()
        //    move.from shouldBe (0 y 0)
        //    move.to.isValid.shouldBeFalse()
        //}
    }
    context("XML Serialization") {
        test("empty Board") {
            Board(emptyList()) shouldSerializeTo """
              <board/>
            """.trimIndent()
        }
        test("random Board length") {
            testXStream.toXML(Board()) shouldHaveLineCount 82
        }
        test("Board with content") {
            testXStream.toXML(makeBoard(0 y 0 to 1)) shouldContainOnlyOnce "<field>TWO</field>"
        }
    }
})

infix fun Int.y(other: Int) = Coordinates(this, other)

fun makeBoard(vararg list: Pair<Coordinates, Int>) =
        Board(List(PluginConstants.BOARD_SIZE) { MutableList(PluginConstants.BOARD_SIZE) { Field(1) } }).apply {
            list.forEach { set(it.first, Team.values().getOrNull(it.second)) }
        }