package sc.plugin2024

import com.thoughtworks.xstream.annotations.XStreamAlias
import sc.api.plugins.IBoard
import sc.plugin2024.util.PluginConstants as Constants
import java.util.*


@XStreamAlias(value = "board")
open class Board: IBoard {
    /**
     * Liste der Spielsegmente
     */
    var tiles: ArrayList<Tile>? = null
        private set

    /**
     * Erzeugt ein neues, initialisiertes Spielfeld
     */
    constructor() {
        init()
    }

    /**
     * Erzeugt ein neues Spielfeld anhand der gegebenen Segmente
     * @param tiles Spielsegmente des neuen Spielfelds
     */
    constructor(tiles: ArrayList<Tile>?) {
        this.tiles = tiles
    }

    /**
     * Erzeugt ein neues Spielfeld
     * @param init boolean der entscheidet, ob das Spielfeld initialisiert werden soll
     */
    constructor(init: Boolean) {
        if (init) init() else makeClearBoard()
    }

    /**
     * Nur fuer den Server relevant
     * initializes the board
     */
    private fun init() {
        tiles = ArrayList()
        val rnd = Random()
        val direction = arrayOfNulls<Direction>(Constants.NUMBER_OF_TILES)
        val startCoordinates = Array<IntArray>(Constants.NUMBER_OF_TILES) {
            IntArray(
                2
            )
        }
        val tilesWithPassengers = ArrayList<Int>() // holds all tiles numbers with a passenger field
        for (i in 0 until Constants.NUMBER_OF_PASSENGERS) {
            // They cannot be a passenger on the starting tile change to -2 for no passenger on last Tile
            var number: Int
            do {
                number = rnd.nextInt(Constants.NUMBER_OF_TILES - if (Constants.PASSENGER_ON_LAST_TILE) 1 else 2) + 1
            } while (tilesWithPassengers.contains(number))
            tilesWithPassengers.add(number)
        }
        direction[0] = Direction.RIGHT
        startCoordinates[0][0] = 0
        startCoordinates[0][1] = 0
        // generate directions of tiles
        for (i in 1 until Constants.NUMBER_OF_TILES) {
            val dir: Int = if (i == 1) {
                // The tile after the starting tile should always point in the same
                // direction. Otherwise, one player would have a disadvantage.
                0
            } else {
                if (direction[i - 1] === Direction.DOWN_LEFT) {
                    // last direction was down left, don't allow more turning to the right (to avoid circles)
                    rnd.nextInt(2) // 0 or 1 only straight or turning left
                } else if (direction[i - 1] === Direction.UP_LEFT) {
                    // last direction was up left, don't allow more turning to the left (to avoid circles)
                    rnd.nextInt(2) - 1 // 0 or -1 only straight or turning right
                } else {
                    rnd.nextInt(3) - 1 // -1, 0 or 1
                }
            }
            direction[i] = direction[i - 1]!!.getTurnedDirection(dir)
            startCoordinates[i][0] = getXCoordinateInDirection(startCoordinates[i - 1][0], direction[i])
            startCoordinates[i][1] = getYCoordinateInDirection(startCoordinates[i - 1][1], direction[i])
        }
        generateStartField()
        for (i in 1 until Constants.NUMBER_OF_TILES) {
            generateTile(
                i, tilesWithPassengers.contains(i),
                direction[i], startCoordinates[i][0], startCoordinates[i][1]
            )
        }
    }

    /**
     * Nur fuer den Server relevant. Gibt Koordiante 4 Felder in Richtung zurück
     * @param y y Koordinate
     * @param direction Richtung
     * @return y Koordinate des neuen Feldes
     */
    private fun getYCoordinateInDirection(y: Int, direction: Direction?): Int {
        when (direction) {
            Direction.RIGHT, Direction.LEFT -> return y
            Direction.UP_RIGHT, Direction.UP_LEFT -> return y - 4
            Direction.DOWN_LEFT, Direction.DOWN_RIGHT -> return y + 4
        }
        return 0
    }

    /**
     * Nur fuer den Server relevant. Gibt Koordiante 4 Felder in Richtung zurück
     * @param x x Koordinate
     * @param direction Richtung
     * @return x Koordinate des neuen Feldes
     */
    private fun getXCoordinateInDirection(x: Int, direction: Direction?): Int {
        when (direction) {
            Direction.RIGHT -> return x + 4
            Direction.LEFT -> return x - 4
            Direction.UP_RIGHT, Direction.DOWN_RIGHT -> return x + 2
            Direction.DOWN_LEFT, Direction.UP_LEFT -> return x - 2
        }
        return 0
    }

    /**
     * Nur fuer den Server relevant
     * generates tile
     * @param index index of Tile
     * @param hasPassenger has the Tile a passenger?
     * @param direction direction of tile
     * @param x x Coordinate of middle
     * @param y y Coordinate of middle
     */
    private fun generateTile(index: Int, hasPassenger: Boolean, direction: Direction?, x: Int, y: Int) {
        val rnd = Random()
        val blocked: Int =
            rnd.nextInt(Constants.MAX_ISLANDS - Constants.MIN_ISLANDS + 1) + Constants.MIN_ISLANDS // 2 to 3 blocked fields
        val special: Int =
            rnd.nextInt(Constants.MAX_SPECIAL - Constants.MIN_SPECIAL + 1) + Constants.MIN_SPECIAL // 1 oder 2 special fields
        val newTile = Tile(index, direction!!.value, x, y, if (hasPassenger) 1 else 0, blocked, special)
        tiles!!.add(newTile)
    }

    private fun generateStartField() {
        val start = Tile(0, 0, 0, 0, 0, 0, 0) // generate tile with middle at 0,0 in direction 0
        // with no other fields than WATER fields
        tiles!!.add(start)
    }

    /**
     * Nur fuer den Server relevant
     * creates a new clear board
     */
    private fun makeClearBoard() {
        tiles = ArrayList()
    }

    /**
     * Gibt ein Feld zurück
     * @param x x-Koordinate
     * @param y y-Koordinate
     * @return Feld an entsprechenden Koordinaten, gibt null zurück, sollte das Feld nicht (mehr) existieren
     */
    fun getField(x: Int, y: Int): Field? {
        for (tile in tiles!!) {
            if (tile.isVisible) {
                val field = tile.getField(x, y)
                if (field != null) {
                    return field
                }
            }
        }
        return null
    }

    /**
     * Gibt ein Feld zurück
     * @param x x-Koordinate
     * @param y y-Koordinate
     * @return Feld an entsprechenden Koordinaten, gibt null zurück, sollte das Feld nicht (mehr) existieren
     */
    fun alwaysGetField(x: Int, y: Int): Field? {
        for (tile in tiles!!) {
            val field = tile.getField(x, y)
            if (field != null) {
                return field
            }
        }
        return null
    }

    /**
     * Equals Methode fuer ein Spielbrett
     */
    override fun equals(other: Any?): Boolean {
        if (other is Board) {
            val tiles1 = other.tiles
            val tiles2 = tiles
            if (tiles1!!.size != tiles2!!.size) {
                return false
            }
            for (i in tiles1.indices) {
                if (!tiles1[i].equals(tiles2[i])) {
                    return false
                }
            }
            return true
        }
        return false
    }

    /**
     * Erzeugt eine Deepcopy des Spielbretts
     */
    fun clone(): Board {
        val clonedTiles = ArrayList<Tile>()
        for (tile in tiles!!) {
            val clonedTile = tile.clone()
            clonedTiles.add(clonedTile)
        }
        return Board(clonedTiles)
    }

    protected val visibleTiles: ArrayList<Tile>
        get() {
            val visibleTiles = ArrayList<Tile>()
            for (tile in tiles!!) {
                if (tile.isVisible) {
                    visibleTiles.add(tile)
                }
            }
            return visibleTiles
        }

    override fun toString(): String {
        var toString = "board:\n"
        for (tile in tiles!!) {
            toString += """
                
                $tile
                """.trimIndent()
        }
        return toString
    }

    override fun hashCode(): Int {
        return tiles?.hashCode() ?: 0
    }
}

