package sc.plugin2023

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.*
import io.kotest.matchers.nulls.*
import sc.api.plugins.Coordinates
import sc.api.plugins.Vector
import sc.helpers.shouldSerializeTo
import sc.protocol.room.RoomPacket

class MoveTest: FunSpec({
    val move = Move(Coordinates(0, 7), Coordinates(17, 5))
    context("Move manipulation") {
        test("reversal should not be equal") {
            move.reversed() shouldNotBe move
            move.reversed()!!.compareTo(move) shouldBe 0
        }
        test("double reversal should yield identity") {
            move.reversed()!!.reversed() shouldBe move
        }
        test("can't reverse penguin placement") {
            Move(null, Coordinates.ORIGIN).reversed().shouldBeNull()
        }
    }
    test("Move XML") {
        RoomPacket("hi", move) shouldSerializeTo """
                <room roomId="hi">
                  <data class="move">
                    <from x="0" y="7"/>
                    <to x="17" y="5"/>
                  </data>
                </room>
            """.trimIndent()
        Move.run(0 y 1, Vector(1, 1)) shouldSerializeTo """
                <move>
                  <from x="0" y="1"/>
                  <to x="1" y="2"/>
                </move>
            """.trimIndent()
    }
})