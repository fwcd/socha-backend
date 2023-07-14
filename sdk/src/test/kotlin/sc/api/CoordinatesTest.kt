package sc.api

import io.kotest.core.datatest.forAll
import io.kotest.core.script.test
import io.kotest.core.spec.style.FunSpec
import io.kotest.core.spec.style.WordSpec
import io.kotest.matchers.*
import io.kotest.matchers.types.shouldNotBeSameInstanceAs
import sc.api.plugins.Coordinates
import sc.api.plugins.CubeCoordinates
import sc.api.plugins.CubeDirection
import sc.api.plugins.HexDirection

class CoordinatesTest: FunSpec({
    context("Coordinates") {
        val position = Coordinates(3, 2)
        test("is equal to same position") {
            position shouldBe position
            position shouldBe Coordinates(3, 2)
            position shouldNotBeSameInstanceAs Coordinates(3, 2)
        }
        test("not be equal to other positions") {
            position shouldNotBe Coordinates(2, 3)
            position shouldNotBe Coordinates(-3, 2)
            position shouldNotBe Coordinates(-3, -2)
            position shouldNotBe Coordinates(3, -2)
        }
    }
    context("CubeCoordinates") {
        val position = CubeCoordinates(3, 2)
        context("is equal to same position") {
            forAll(CubeCoordinates(3, 2), position.rotatedBy(0), position.rotatedBy(-12)) {
                position shouldBe it
                position shouldNotBeSameInstanceAs it
            }
        }
        test("not equal to other positions") {
            position shouldNotBe CubeCoordinates(2, 3)
            position shouldNotBe CubeCoordinates(-3, 2)
            position shouldNotBe CubeCoordinates(-3, -2)
            position shouldNotBe CubeCoordinates(3, -2)
        }
        test("produce correct rotation") {
            CubeCoordinates(1, 0).rotatedBy(2) shouldBe CubeCoordinates(-1, 1)
            position.rotatedBy(1) shouldBe CubeCoordinates(-2, 5)
            position.rotatedBy(2) shouldBe position.rotatedBy(-4)
            position.rotatedBy(3) shouldBe position.rotatedBy(-3)
            position.rotatedBy(3) shouldBe position.unaryMinus()
        }
    }
    context("HexDirections") {
        context("produce correct opposites") {
            forAll(
                    HexDirection.LEFT to HexDirection.RIGHT,
                    HexDirection.UP_LEFT to HexDirection.DOWN_RIGHT,
                    HexDirection.DOWN_LEFT to HexDirection.UP_RIGHT,
            ) {
                it.first shouldNotBe it.second
                it.first.opposite() shouldBe it.second
                it.second.opposite() shouldBe it.first
                it.first.rotatedBy(3) shouldBe it.second
                it.first.rotatedBy(-3) shouldBe it.second
                it.second.rotatedBy(-3) shouldBe it.first
            }
        }
        test("simple rotation") {
            HexDirection.RIGHT.rotatedBy(1) shouldBe HexDirection.RIGHT.rotatedBy(-5)
            HexDirection.RIGHT.rotatedBy(1) shouldBe HexDirection.DOWN_RIGHT
            HexDirection.RIGHT.turnCountTo(HexDirection.DOWN_RIGHT) shouldBe 1
        }
    }
    context("CubeDirections") {
        context("produce correct opposites") {
            forAll(
                    CubeDirection.LEFT to CubeDirection.RIGHT,
                    CubeDirection.UP_LEFT to CubeDirection.DOWN_RIGHT,
                    CubeDirection.DOWN_LEFT to CubeDirection.UP_RIGHT,
            ) {
                it.first shouldNotBe it.second
                it.first.opposite() shouldBe it.second
                it.second.opposite() shouldBe it.first
                it.first.rotatedBy(3) shouldBe it.second
                it.first.rotatedBy(-3) shouldBe it.second
                it.second.rotatedBy(-3) shouldBe it.first
                
                it.first.vector.rotatedBy(3) shouldBe it.second.vector
                it.second.vector shouldBe it.first.vector.unaryMinus()
            }
        }
        test("simple rotation") {
            CubeDirection.RIGHT.rotatedBy(1) shouldBe CubeDirection.RIGHT.rotatedBy(-5)
            CubeDirection.RIGHT.rotatedBy(1) shouldBe CubeDirection.DOWN_RIGHT
            CubeDirection.RIGHT.turnCountTo(CubeDirection.DOWN_RIGHT) shouldBe 1
        }
    }
})