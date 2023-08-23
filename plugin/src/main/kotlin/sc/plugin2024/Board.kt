package sc.plugin2024

import com.thoughtworks.xstream.annotations.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import sc.api.plugins.*
import sc.plugin2024.util.BoardConverter
import kotlin.math.abs
import kotlin.reflect.KClass

/**
 * Erzeugt ein neues Spielfeld anhand der gegebenen Segmente
 * @param segments Spielsegmente des neuen Spielfelds
 */
@XStreamConverter(BoardConverter::class)
@XStreamAlias(value = "board")
data class Board(
        @XStreamImplicit
        val segments: Segments = generateBoard(),
        @XStreamOmitField
        internal var visibleSegments: Int = 2.coerceAtMost(segments.size),
        @XStreamAsAttribute
        var nextDirection: CubeDirection = segments.lastOrNull()?.direction ?: CubeDirection.RIGHT,
): IBoard {
    companion object {
        val logger: Logger = LoggerFactory.getLogger(this::class.java)
    }
    
    /** Size of the map. */
    val bounds
        get() = segments.fold(Pair(0 to 0, 0 to 0)) { acc, segment ->
            val center = segment.center
            val x = center.x / 2
            Pair(acc.first.first.coerceAtMost(x) to acc.first.second.coerceAtLeast(x),
                    acc.second.first.coerceAtMost(center.r) to acc.second.second.coerceAtLeast(center.r))
        }
    
    val rectangleSize: Coordinates
        get() = bounds.let { Coordinates(it.first.second - it.first.first + 5, it.second.second - it.second.first + 5) }
        
    override fun clone(): Board = copy(segments = this.segments.clone())
    
    internal fun getNextDirection() =
            segments[visibleSegments.coerceAtMost(segments.lastIndex)].direction
    
    internal fun revealSegment(segment: Int) {
        visibleSegments = segment.coerceIn(visibleSegments, segments.size)
        nextDirection = getNextDirection()
    }
    
    /** Iterate over each [Field] paired with its [CubeCoordinates]. */
    fun forEachField(handler: (CubeCoordinates, Field) -> Unit) =
            segments.forEach { it.forEachField { coordinates, field -> handler(coordinates, field) } }
    
    /**
     * Ruft das Feld an den angegebenen [CubeCoordinates] ab.
     *
     * @param coords Die [CubeCoordinates], die das abzurufende Feld angeben.
     * @return Das Feld an den angegebenen [CubeCoordinates], oder null, wenn kein Feld gefunden wird.
     */
    operator fun get(coords: CubeCoordinates) =
            segments.firstNotNullOfOrNull {
                if(coords.distanceTo(it.center) <= 3)
                    it[coords]
                else
                    null
            }
    
    fun doesFieldHaveCurrent(coords: CubeCoordinates): Boolean =
            segmentIndex(coords).let {
                if(it == -1)
                    return@let false
                val segment = segments[it]
                val nextDirection = segments.getOrNull(it + 1)?.direction ?: nextDirection
                arrayOf(segment.center + segment.direction.opposite().vector,
                        segment.center,
                        segment.center + nextDirection.vector,
                        segment.center + nextDirection.vector * 2
                ).contains(coords)
            }
    
    /**
     * Gibt das [Field] zurück, das an das angegebene Feld in der angegebenen Richtung angrenzt.
     *
     * @param direction die [HexDirection], in der das benachbarte Feld zu finden ist
     * @param coordinate die [Coordinates], für die das angrenzende Feld gefunden werden soll
     *
     * @return das angrenzende [Field], wenn es existiert, sonst null
     */
    fun getFieldInDirection(direction: CubeDirection, coordinate: CubeCoordinates): Field? =
            get(coordinate + direction.vector)
    
    /**
     * Gibt die [CubeCoordinates] für einen bestimmten Index innerhalb eines Segments zurück.
     *
     * @param segmentIndex Der Index des Segments.
     * @param xIndex Der x-Index innerhalb des Segments.
     * @param yIndex Der y-Index innerhalb des Segments.
     * @return Die [CubeCoordinates] für den angegebenen Index innerhalb des Segments.
     */
    fun getCoordinateByIndex(segmentIndex: Int, xIndex: Int, yIndex: Int): CubeCoordinates =
            segments[segmentIndex].let { segment ->
                segment.localToGlobal(Coordinates(xIndex, yIndex))
            }
    
    /**
     * Berechnet den Abstand zwischen zwei [Field]s in der Anzahl der [Segment].
     *
     * @param coordinate1 Das erste Feld, von dem aus die Entfernung berechnet wird.
     * @param coordinate2 Das zweite Feld, aus dem die Entfernung berechnet wird.
     * @return Der Abstand zwischen den angegebenen Feldern im Segment.
     */
    fun segmentDistance(coordinate1: CubeCoordinates, coordinate2: CubeCoordinates): Int =
            abs(segmentIndex(coordinate1) - segmentIndex(coordinate2))
    
    /**
     * Findet den [segments]-Index für die angegebene [CubeCoordinates].
     *
     * @param coordinate Die Koordinate, für die das [Segment] gefunden werden soll.
     * @return Der Index des Segments, das die Koordinate enthält, oder -1, falls nicht gefunden.
     */
    fun segmentIndex(coordinate: CubeCoordinates): Int =
            segments.indexOfFirst { segment ->
                segment[coordinate] != null
            }
    
    fun findSegment(coordinate: CubeCoordinates) =
            segmentIndex(coordinate).takeUnless { it == -1 }?.let { segments[it] }
    
    /**
     * Gibt eine Liste benachbarter [Field]s auf der Grundlage der angegebenen [CubeCoordinates] zurück.
     *
     * @param coords die [CubeCoordinates] des Mittelfeldes
     * @return eine Liste der benachbarten [Field]s
     */
    fun neighboringFields(coords: CubeCoordinates): List<Field?> =
            CubeDirection.values().map { direction ->
                getFieldInDirection(direction, coords)
            }
    
    fun effectiveSpeed(ship: Ship) =
            ship.speed.minus(if(doesFieldHaveCurrent(ship.position)) 1 else 0)
    
    /**
     * Methode zur Abholung eines Passagiers auf einem [Ship].
     * Berücksichtigt die effektive Schiffsgeschwindigkeit.
     *
     * @param ship Das [Ship], mit dem der Passagier abgeholt wird.
     * @return ob ein Passagier erfolgreich abgeholt wurde
     */
    fun pickupPassenger(ship: Ship): Boolean =
            if(effectiveSpeed(ship) < 2) {
                pickupPassenger(ship.position).let { field ->
                    if(field != null) {
                        field.passenger--
                        ship.passengers++
                        true
                    } else {
                        false
                    }
                }
            } else false
    
    /**
     * Check zur Abholung eines Passagiers mit einem Schiff auf den gegebenen Koordinaten.
     *
     * @return Feld mit abholbarem Passagier, wenn vorhanden
     */
    fun pickupPassenger(pos: CubeCoordinates): Field.PASSENGER? =
            CubeDirection.values().firstNotNullOfOrNull { direction ->
                getFieldInDirection(direction, pos).takeIf { field ->
                    field is Field.PASSENGER && field.passenger > 0 && field.direction == direction.opposite()
                } as Field.PASSENGER?
            }
    
    /**
     * Findet das nächstgelegene Feld des angegebenen [Field], ausgehend von den angegebenen [CubeCoordinates],
     * aber ohne [startCoordinates].
     *
     * @param startCoordinates Die Startkoordinaten.
     * @param field Der [Field], nach dem gesucht werden soll.
     * @return Eine Liste von [CubeCoordinates], die die nächstgelegenen Feldkoordinaten mit dem angegebenen [Field] darstellen.
     */
    fun findNearestFieldTypes(startCoordinates: CubeCoordinates, field: KClass<out Field>): List<CubeCoordinates> {
        val visitedCoordinates = mutableSetOf<CubeCoordinates>()
        val neighbourCoordinatesQueue = ArrayDeque<CubeCoordinates>()
        val nearestFieldCoordinates: MutableList<CubeCoordinates> = mutableListOf()
        
        /** Prüft das [Field] an den angegebenen [CubeCoordinates] und fügt es ggf. der [ArrayDeque] hinzu. */
        fun checkFieldAndAddToQueue(coordinates: CubeCoordinates) {
            val neighbourField = this[coordinates]
            if(neighbourField != null && !visitedCoordinates.contains(coordinates) && !neighbourCoordinatesQueue.contains(coordinates)) {
                neighbourCoordinatesQueue.add(coordinates)
            }
        }
        
        CubeDirection.values().forEach { direction ->
            checkFieldAndAddToQueue(startCoordinates + direction.vector)
        }
        
        while(neighbourCoordinatesQueue.isNotEmpty()) {
            val currentCoordinates = neighbourCoordinatesQueue.removeFirst()
            
            if(nearestFieldCoordinates.isNotEmpty() && startCoordinates.distanceTo(currentCoordinates) > startCoordinates.distanceTo(nearestFieldCoordinates.last()))
                break
            
            visitedCoordinates.add(currentCoordinates)
            
            val currentField = this[currentCoordinates]
            if(currentField != null && field.isInstance(currentField)) {
                nearestFieldCoordinates.add(currentCoordinates)
            } else {
                CubeDirection.values().forEach { direction ->
                    checkFieldAndAddToQueue(currentCoordinates + direction.vector)
                }
            }
        }
        
        return nearestFieldCoordinates
    }
    
    override fun toString() =
            segments.joinToString("\nBoard", prefix = "Board", postfix = "\nNext Segment towards $nextDirection")
}

