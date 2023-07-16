package sc.plugin2024

import com.thoughtworks.xstream.annotations.XStreamAlias
import com.thoughtworks.xstream.annotations.XStreamAsAttribute
import com.thoughtworks.xstream.annotations.XStreamImplicit
import com.thoughtworks.xstream.annotations.XStreamOmitField
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import sc.api.plugins.*
import kotlin.math.abs

/**
 * Erzeugt ein neues Spielfeld anhand der gegebenen Segmente
 * @param segments Spielsegmente des neuen Spielfelds
 */
@XStreamAlias(value = "board")
data class Board(
        @XStreamImplicit
        val segments: Segments = generateBoard(),
        @XStreamOmitField
        internal var visibleSegments: Int = 2,
): IBoard {
    companion object {
        val logger: Logger = LoggerFactory.getLogger(this::class.java)
    }
    
    override fun clone(): Board = Board(this.segments.clone(), visibleSegments)
    
    // TODO direction of segment beyond visible one, set with visibleSegments
    @XStreamAsAttribute
    var nextDirection: CubeDirection? = null
    
    /**
     * Ruft das Feld an den angegebenen [CubeCoordinates] ab.
     *
     * @param coords Die [CubeCoordinates], die das abzurufende Feld angeben.
     * @return Das Feld an den angegebenen [CubeCoordinates], oder null, wenn kein Feld gefunden wird.
     */
    operator fun get(coords: CubeCoordinates) =
            segments.firstNotNullOfOrNull {
                val diff = coords - it.center
                logger.info("Locating {} in {}: {}, {}", coords, it, diff, diff.distanceTo(CubeCoordinates.ORIGIN))
                if(diff.distanceTo(CubeCoordinates.ORIGIN) <= 3)
                    it.segment[diff.rotatedBy(it.direction.turnCountTo(CubeDirection.RIGHT))]
                else
                    null
            }
    
    /**
     * Gibt den [FieldType] zurück, der an das angegebene Feld in der angegebenen Richtung angrenzt.
     *
     * @param direction die [HexDirection], in der das benachbarte Feld zu finden ist
     * @param coordinate die [Coordinates], für die das angrenzende Feld gefunden werden soll
     * @return das angrenzende [FieldType], wenn es existiert, sonst null
     */
    fun getFieldInDirection(direction: CubeDirection, coordinate: CubeCoordinates): FieldType? =
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
                CubeCoordinates(xIndex - 1, yIndex - 2)
                        .rotatedBy(-segment.direction.turnCountTo(CubeDirection.RIGHT)).plus(segment.center)
            }
    
    /**
     * Berechnet den Abstand zwischen zwei [FieldType]s in der Anzahl der [Segment].
     *
     * @param coordinate1 Das erste Feld, von dem aus die Entfernung berechnet wird.
     * @param coordinate2 Das zweite Feld, aus dem die Entfernung berechnet wird.
     * @return Der Abstand zwischen den angegebenen Feldern im Segment.
     * Wenn eines der Felder in keinem Segment gefunden wird, wird -1 zurückgegeben.
     */
    fun segmentDistance(coordinate1: CubeCoordinates, coordinate2: CubeCoordinates): Int? {
        return findSegment(coordinate1)?.let { index1 ->
            findSegment(coordinate2)?.let { index2 ->
                abs(index1 - index2)
            }
        }
    }
    
    /**
     * Findet den [segments]-Index für die angegebene [CubeCoordinates].
     *
     * @param coordinate Die Koordinate, für die das [Segment] gefunden werden soll.
     * @return Der Index des Segments, das die Koordinate enthält, oder -1, falls nicht gefunden.
     */
    private fun findSegment(coordinate: CubeCoordinates): Int? =
            segments.indexOfFirst { segment ->
                val diff = coordinate - segment.center
                diff.distanceTo(CubeCoordinates.ORIGIN) <= 3 &&
                segment.segment[diff.rotatedBy(segment.direction.turnCountTo(CubeDirection.RIGHT))] != null
            }.takeIf { it != -1 }
    
    /**
     * Gibt eine Liste benachbarter [FieldType]s auf der Grundlage der angegebenen [CubeCoordinates] zurück.
     *
     * @param coords die [CubeCoordinates] des Mittelfeldes
     * @return eine Liste der benachbarten [FieldType]s
     */
    fun neighboringFields(coords: CubeCoordinates): List<FieldType?> =
            CubeDirection.values().map { direction ->
                getFieldInDirection(direction, coords)
            }
    
    /**
     * Methode zur Abholung eines Passagiers auf einem [Ship].
     *
     * @param ship Das [Ship], mit dem der Passagier abgeholt wird.
     * @return `true`, wenn ein Passagier erfolgreich abgeholt wurde, sonst `false`.
     */
    fun pickupPassenger(ship: Ship): Boolean =
        neighboringFields(ship.position).any { field ->
            if(field is FieldType.PASSENGER && field.passenger > 0) {
                field.passenger--
                ship.passengers++
                true
            } else {
                false
            }
        }
    
    /**
     * Findet das [Ship], das dem Ziel im letzten [Segment] am nächsten ist.
     *
     * @param ship1 Das erste [Ship] zum Vergleich.
     * @param ship2 Das zweite [Ship] zum Vergleich.
     * @return Das [Ship], das dem Ziel im letzten [Segment] am nächsten ist, oder null,
     * wenn beide Schiffe den gleichen Abstand haben.
     */
    fun closestShipToGoal(ship1: Ship, ship2: Ship): Ship? =
            with(segments.last()) {
                // TODO this assumes that the segment is 5x4
                val mostFurthestPoint = center + CubeCoordinates(2 * direction.vector.q, 2 * direction.vector.r)
                val dist1 = ship1.position.distanceTo(mostFurthestPoint)
                val dist2 = ship2.position.distanceTo(mostFurthestPoint)
                if(dist1 < dist2) ship1 else if(dist1 > dist2) ship2 else null
            }
    
    /**
     * Findet das nächstgelegene Feld des angegebenen [FieldType], ausgehend von den angegebenen [CubeCoordinates],
     * aber ohne [startCoordinates].
     *
     * @param startCoordinates Die Startkoordinaten.
     * @param fieldType Der FieldType, nach dem gesucht werden soll.
     * @return Eine Liste von CubeCoordinates, die die nächstgelegenen Feldkoordinaten mit dem angegebenen FeldTyp darstellen.
     */
    fun findNearestFieldTypes(startCoordinates: CubeCoordinates, fieldType: FieldType): List<CubeCoordinates> {
        val visited = mutableSetOf(startCoordinates)
        var neighbours = CubeDirection.values().map { direction -> startCoordinates + direction.vector }
                .filterNot { visited.contains(it) || this[it] == null }
        
        val queue = ArrayDeque<Pair<CubeCoordinates, Int>>().apply {
            addAll(neighbours.map { it to 1 })
        }

        var minDist = Int.MAX_VALUE
        val minDistCoords = mutableListOf<CubeCoordinates>()

        while(queue.isNotEmpty()) {
            val (currentCoordinates, currDist) = queue.removeFirst()

            if(currentCoordinates in visited) {
                continue
            }

            val currentField = this[currentCoordinates]
            if(currentField == fieldType) {
                if(currDist < minDist) {
                    minDist = currDist
                    minDistCoords.clear()
                    minDistCoords.add(currentCoordinates)
                } else if(currDist == minDist) {
                    minDistCoords.add(currentCoordinates)
                }
            }

            neighbours = CubeDirection.values().map { direction -> currentCoordinates + direction.vector }
                    .filterNot { visited.contains(it) }

            queue.addAll(neighbours.map { it to currDist + 1 })
            visited.add(currentCoordinates)

            if(currDist > 1 && minDistCoords.isNotEmpty())
                break
        }

        return minDistCoords
    }
    
    /**
     * Druckt die Segmente in einem lesbaren Format.
     *
     * Diese Methode durchläuft jedes Segment des gegebenen Objekts und druckt dessen Inhalt in formatierter Form aus.
     */
    fun prettyPrint() {
        val stringBuilder = StringBuilder()
        for((index, segment) in this.segments.withIndex()) {
            stringBuilder.append("Segment ${index + 1}:\n")
            for(fieldTypeRow in segment.segment) {
                for(fieldType in fieldTypeRow) {
                    stringBuilder.append("| ${fieldType.javaClass.simpleName.firstOrNull() ?: '-'} ")
                }
                stringBuilder.append("|\n")
            }
            stringBuilder.append("\n")
        }
        print(stringBuilder.toString())
    }
    
}

