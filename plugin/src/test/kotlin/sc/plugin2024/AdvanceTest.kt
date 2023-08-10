package sc.plugin2024

import com.thoughtworks.xstream.XStream
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.datatest.forAll
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.*
import sc.api.plugins.CubeCoordinates
import sc.api.plugins.CubeDirection
import sc.helpers.shouldSerializeTo
import sc.plugin2024.actions.Acceleration
import sc.plugin2024.actions.Advance
import sc.plugin2024.actions.Turn
import sc.plugin2024.exceptions.AdvanceException
import sc.shared.InvalidMoveException

class AdvanceTest: FunSpec({
    test("serializes nicely") {
        val xStream = XStream().apply {
            processAnnotations(Advance::class.java)
            XStream.setupDefaultSecurity(this)
            allowTypesByWildcard(arrayOf("sc.plugin2024.actions.*"))
        }
        
        val serialized = xStream.toXML(Advance(5))
        
        serialized shouldBe """<advance distance="5"/>"""
        Advance(5) shouldSerializeTo """<advance distance="5"/>"""
    }
    
    context("perform") {
        val gameState = GameState()
        val shipONE = gameState.currentShip
        
        test("advance by 2") {
            shipONE.speed = 2
            shipONE.movement = 2
            
            Advance(2).perform(gameState, shipONE)
            
            shipONE.position shouldBe CubeCoordinates(1, -1)
            shipONE.direction shouldBe CubeDirection.RIGHT
            shipONE.movement shouldBe 0
            shipONE.speed shouldBe 2
        }
        
        context("current") {
            test("across") {
                shouldThrow<InvalidMoveException> {
                    gameState.performMoveDirectly(Move(Turn(CubeDirection.DOWN_RIGHT), Advance(1)))
                }.mistake shouldBe AdvanceException.NO_MOVEMENT_POINTS
                forAll<Int>((1..3).toList()) {
                    gameState.performMoveDirectly(Move(Acceleration(it), Turn(CubeDirection.DOWN_RIGHT), Advance(it)))
                    shipONE.position shouldBe CubeCoordinates(0, it - 1)
                }
                shouldThrow<InvalidMoveException> {
                    gameState.performMoveDirectly(Move(Acceleration(4), Turn(CubeDirection.DOWN_RIGHT), Advance(4)))
                }.mistake shouldBe AdvanceException.FIELD_IS_BLOCKED
            }
            test("within") {
                shipONE.position = CubeCoordinates.ORIGIN
                shouldThrow<InvalidMoveException> {
                    gameState.performMoveDirectly(Move(Advance(1)))
                }.mistake shouldBe AdvanceException.NO_MOVEMENT_POINTS
                forAll<Int>((1..2).toList()) {
                    gameState.performMoveDirectly(Move(Acceleration(it), Advance(it + 1)))
                    shipONE.position shouldBe CubeCoordinates(it, 0)
                }
            }
            test("double crossing") {
                shouldThrow<InvalidMoveException> {
                    gameState.performMove(Move(Acceleration(4), Turn(CubeDirection.DOWN_RIGHT), Advance(2), Turn(CubeDirection.UP_RIGHT), Advance(2)))
                }.mistake shouldBe AdvanceException.NO_MOVEMENT_POINTS
                gameState.performMoveDirectly(Move(Acceleration(5), Turn(CubeDirection.DOWN_RIGHT), Advance(2), Turn(CubeDirection.UP_RIGHT), Advance(2)))
                shipONE.position shouldBe CubeCoordinates(1, -1)
            }
        }
        
        test("no movement points") {
            shouldThrow<InvalidMoveException> {
                gameState.performMoveDirectly(Move(Advance(3)))
            }.mistake shouldBe AdvanceException.NO_MOVEMENT_POINTS
        }
        
        test("invalid distance") {
            shipONE.speed = 2
            shipONE.movement = 6
            
            shouldThrow<InvalidMoveException> {
                Advance(-2).perform(gameState, shipONE)
            }.mistake shouldBe AdvanceException.INVALID_DISTANCE
            shouldThrow<InvalidMoveException> {
                Advance(0).perform(gameState, shipONE)
            }.mistake shouldBe AdvanceException.INVALID_DISTANCE
            shouldThrow<InvalidMoveException> {
                Advance(7).perform(gameState, shipONE)
            }.mistake shouldBe AdvanceException.INVALID_DISTANCE
        }
        
        test("field does not exists") {
            shipONE.speed = 2
            shipONE.movement = 2
            shipONE.direction = CubeDirection.LEFT
            
            val invalidAdvanceLessThanMinusOne = Advance(2)
            
            shouldThrow<InvalidMoveException> {
                invalidAdvanceLessThanMinusOne.perform(gameState, shipONE)
            }.mistake shouldBe AdvanceException.FIELD_IS_BLOCKED
        }
        
        test("field is blocked") {
            val blockedFieldCoordinate = gameState.board
                    .findNearestFieldTypes(CubeCoordinates.ORIGIN, Field.BLOCKED::class).first()
            
            var takenDirection: CubeDirection? = null
            
            shipONE.position = CubeDirection.values().map { it.vector }.firstOrNull { direction ->
                gameState.board[blockedFieldCoordinate + direction] != null
                        .also { takenDirection = CubeDirection.values().firstOrNull { it.vector == direction } }
            }?.let { blockedFieldCoordinate + it } ?: run {
                throw IllegalStateException("No valid direction found.")
            }
            
            shipONE.direction = takenDirection?.opposite()
                                ?: throw IllegalStateException("No valid opposite direction found.")
            
            val invalidAdvanceLessThanMinusOne = Advance(1)
            
            shouldThrow<InvalidMoveException> {
                invalidAdvanceLessThanMinusOne.perform(gameState, shipONE)
            }.mistake shouldBe AdvanceException.FIELD_IS_BLOCKED
        }
        
        test("backwards move is not possible") {
            shipONE.direction = CubeDirection.DOWN_RIGHT
            shouldThrow<InvalidMoveException> {
                Advance(-1).perform(gameState, shipONE)
            }.mistake shouldBe AdvanceException.INVALID_DISTANCE
        }
        
        xtest("only one move allowed on sandbank") {
            val sandbankCoordinate = gameState.board
                    .findNearestFieldTypes(CubeCoordinates.ORIGIN, Field.SANDBANK::class).first()
            
            shipONE.position = sandbankCoordinate
            
            shipONE.direction = CubeDirection.values().firstOrNull { direction ->
                val dest = gameState.board[sandbankCoordinate + direction.vector]
                dest != null && dest.isEmpty
            } ?: throw IllegalStateException("No valid direction found.")
            
            shouldThrow<InvalidMoveException> {
                Advance(2).perform(gameState, shipONE)
            }.mistake shouldBe AdvanceException.NO_MOVEMENT_POINTS
        }
        
        context("on opponent") {
            shipONE.position = CubeCoordinates(-1, 0)
            shipONE.direction = CubeDirection.DOWN_LEFT
            
            test("insufficient movement") {
                shouldThrow<InvalidMoveException> {
                    Advance(1).perform(gameState, shipONE)
                }.mistake shouldBe AdvanceException.INSUFFICIENT_PUSH
            }
            
            shipONE.movement = 2
            test("allowed") {
                Advance(1).perform(gameState, shipONE)
            }
            
            test("ship already in target") {
                shouldThrow<InvalidMoveException> {
                    Advance(2).perform(gameState, shipONE)
                }.mistake shouldBe AdvanceException.SHIP_ALREADY_IN_TARGET
            }
        }
    }
})