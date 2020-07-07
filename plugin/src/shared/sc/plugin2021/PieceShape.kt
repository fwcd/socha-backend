package sc.plugin2021

import sc.plugin2020.Piece
import sc.plugin2020.util.Constants
import kotlin.math.max
import kotlin.math.min

// data structure to represent one shape of piece of Blokus. There are 21 different kinds, see https://en.wikipedia.org/wiki/Blokus
// The shapes are represented as coordinate list of occupied fields, where the left upper corner of a shape is the origin (0,0), x-axis going to the right and y-axis going down
class PieceShape(coordinates: Set<Coordinates>) {
    val coordinates: Set<Coordinates> = align(coordinates)
    
    fun rotate(rotation: Rotation): PieceShape = when (rotation) {
        Rotation.NONE -> this
        Rotation.RIGHT -> align(turnRight())
        Rotation.MIRROR -> align(mirror())
        Rotation.LEFT -> align(turnLeft())
    }
    
    private fun mirror(): PieceShape {
        return PieceShape(coordinates.map {
            Coordinates(-it.x, -it.y)
        }.toSet())
    }
    
    private fun turnRight(): PieceShape {
        return PieceShape(coordinates.map {
            Coordinates(it.y, -it.x)
        }.toSet())
    }
    
    private fun turnLeft(): PieceShape {
        return PieceShape(coordinates.map {
            Coordinates(-it.y, it.x)
        }.toSet())
    }
    
    companion object {
        fun align(coordinates: Set<Coordinates>): Set<Coordinates> {
            var minX = Constants.BOARD_SIZE
            var minY = Constants.BOARD_SIZE
            coordinates.forEach {
                minX = min(it.x, minX)
                minY = min(it.y, minY)
            }
            return coordinates.map {
                Coordinates(it.x - minX, it.y - minY)
            }.toSet()
        }
    
        fun align(shape: PieceShape): PieceShape =
                PieceShape(align(shape.coordinates))
    
        val shapes = listOf(
                0 to PieceShape(setOf(Coordinates(0, 0))),
                1 to PieceShape(setOf(Coordinates(0, 0), Coordinates(1, 0))),
                2 to PieceShape(setOf(Coordinates(0, 0), Coordinates(1, 0), Coordinates(1, 1))),
                3 to PieceShape(setOf(Coordinates(0, 0), Coordinates(1, 0), Coordinates(2, 0))),
                4 to PieceShape(setOf(Coordinates(0, 0), Coordinates(1, 0), Coordinates(0, 1), Coordinates(1, 1))),
                5 to PieceShape(setOf(Coordinates(1, 0), Coordinates(0, 1), Coordinates(1, 1), Coordinates(2, 1))),
                6 to PieceShape(setOf(Coordinates(0, 0), Coordinates(1, 0), Coordinates(2, 0), Coordinates(3, 0))),
                7 to PieceShape(setOf(Coordinates(2, 0), Coordinates(0, 1), Coordinates(1, 1), Coordinates(2, 1))),
                8 to PieceShape(setOf(Coordinates(1, 0), Coordinates(2, 0), Coordinates(0, 1), Coordinates(1, 1))),
                9 to PieceShape(setOf(Coordinates(0, 0), Coordinates(0, 1), Coordinates(1, 1), Coordinates(2, 1), Coordinates(3, 1))),
                10 to PieceShape(setOf(Coordinates(1, 0), Coordinates(1, 1), Coordinates(0, 2), Coordinates(1, 2), Coordinates(2, 2))),
                11 to PieceShape(setOf(Coordinates(0, 0), Coordinates(0, 1), Coordinates(0, 2), Coordinates(1, 2), Coordinates(2, 2))),
                12 to PieceShape(setOf(Coordinates(1, 0), Coordinates(2, 0), Coordinates(3, 0), Coordinates(0, 1), Coordinates(1, 1))),
                13 to PieceShape(setOf(Coordinates(2, 0), Coordinates(0, 1), Coordinates(1, 1), Coordinates(2, 1), Coordinates(0, 2))),
                14 to PieceShape(setOf(Coordinates(0, 0), Coordinates(0, 1), Coordinates(0, 2), Coordinates(0, 3), Coordinates(0, 4))),
                15 to PieceShape(setOf(Coordinates(0, 0), Coordinates(0, 1), Coordinates(1, 1), Coordinates(0, 2), Coordinates(1, 2))),
                16 to PieceShape(setOf(Coordinates(1, 0), Coordinates(2, 0), Coordinates(0, 1), Coordinates(1, 1), Coordinates(0, 2))),
                17 to PieceShape(setOf(Coordinates(0, 0), Coordinates(1, 0), Coordinates(0, 1), Coordinates(0, 2), Coordinates(1, 2))),
                18 to PieceShape(setOf(Coordinates(1, 0), Coordinates(2, 0), Coordinates(0, 1), Coordinates(1, 1), Coordinates(1, 2))),
                19 to PieceShape(setOf(Coordinates(1, 0), Coordinates(0, 1), Coordinates(1, 1), Coordinates(2, 1), Coordinates(1, 2))),
                20 to PieceShape(setOf(Coordinates(1, 0), Coordinates(0, 1), Coordinates(1, 1), Coordinates(2, 1), Coordinates(3, 1)))
        )
    }
    
    override fun equals(other: Any?): Boolean {
        return other is PieceShape && other.coordinates.equals(coordinates) ||
                other is Set<*> && other == coordinates
    }
    
    override fun hashCode(): Int {
        return coordinates.hashCode()
    }
    
}
