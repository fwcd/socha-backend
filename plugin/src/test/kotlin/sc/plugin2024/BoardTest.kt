package sc.plugin2024

import io.kotest.core.datatest.forAll
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.*
import io.kotest.matchers.collections.*
import io.kotest.matchers.ints.*
import io.kotest.matchers.nulls.*
import io.kotest.matchers.string.*
import sc.api.plugins.CubeCoordinates
import sc.api.plugins.CubeDirection
import sc.api.plugins.Team
import sc.helpers.shouldSerializeTo
import sc.helpers.testXStream
import sc.plugin2024.util.PluginConstants

class BoardTest: FunSpec({
    context("get field by CubeCoordinates") {
        test("arrayX works within first segment") {
            CubeCoordinates(0, 0, 0).arrayX shouldBe 0 // center
            CubeCoordinates(-3, 2, 1).arrayX shouldBe -1 // bottom left
            CubeCoordinates(0, 2, -2).arrayX shouldBe 2 // bottom right
            CubeCoordinates(1, -2, 1).arrayX shouldBe 1 // top
            val segment = generateSegment(false, arrayOf())
            segment[CubeCoordinates(0, 0)] shouldBe Field.WATER
        }
        val board = Board()
        test("delineates first segment") {
            board.getCoordinateByIndex(0, 0, 0) shouldBe CubeCoordinates(-1, -2)
            board.getCoordinateByIndex(0, 1, 2) shouldBe CubeCoordinates.ORIGIN
            board.getCoordinateByIndex(0, 2, 3) shouldBe CubeCoordinates(0, 1)
            board[CubeCoordinates(0, 0)] shouldBe Field.WATER
            CubeCoordinates(-1, -2).distanceTo(CubeCoordinates.ORIGIN) shouldBe 3
            board[CubeCoordinates(-1, -2)] shouldBe Field.WATER
            board[CubeCoordinates(-3, 2)] shouldBe Field.WATER
            board[CubeCoordinates(-2, -2)].shouldBeNull()
            board[CubeCoordinates(0, -3)].shouldBeNull()
        }
        test("end of second segment") {
            board.getCoordinateByIndex(1,
                    PluginConstants.SEGMENT_FIELDS_WIDTH - 1, 0) shouldBe CubeCoordinates(6, -2)
            board.getCoordinateByIndex(1,
                    PluginConstants.SEGMENT_FIELDS_WIDTH - 1,
                    PluginConstants.SEGMENT_FIELDS_HEIGHT - 1) shouldBe CubeCoordinates(4, 2)
            board[CubeCoordinates(6, -2)] shouldBe Field.WATER // top right
            board[CubeCoordinates(4, 2)] shouldBe Field.WATER // bottom right
        }
        test("start of second segment") {
            val coordinate = board.getCoordinateByIndex(1, 0, 2)
            coordinate shouldBe CubeCoordinates(3, 0)
            val center = board.segments[1].center
            (coordinate + CubeDirection.RIGHT.vector) shouldBe center
            board.getCoordinateByIndex(1, 1, 2) shouldBe center
        }
    }
    
    val board = Board()
    test("segmentIndex") {
        board.segmentIndex(CubeCoordinates.ORIGIN) shouldBe 0
        board.segmentIndex(CubeCoordinates(4, 0, -4)) shouldBe 1
        board.segmentIndex(CubeCoordinates(0, -3, 3)) shouldBe -1
        
    }
    
    test("segmentDistance") {
        board.segmentDistance(CubeCoordinates.ORIGIN, CubeCoordinates(0, 2)) shouldBe 0
        board.segmentDistance(CubeCoordinates.ORIGIN, CubeCoordinates(1, 2)) shouldBe 1
        board.segmentDistance(CubeCoordinates(-1, -2), CubeCoordinates(3, 2)) shouldBe 1
    }
    
    test("find nearest field type") {
        val startCoordinates = CubeCoordinates(0, 0, 0)
        board.findNearestFieldTypes(startCoordinates, Field.WATER::class) shouldContain CubeCoordinates(1, 0, -1)

        val dynamicField = board.segments.last().center + board.segments.last().direction.vector
        val result = board.findNearestFieldTypes(board.segments.last().center, board[dynamicField]!!::class)
        result shouldContain dynamicField
        
        board.findNearestFieldTypes(startCoordinates, Field.PASSENGER::class).size shouldBeGreaterThanOrEqual 1
        
    }

    context("pickupPassenger") {
        test("should decrease passenger count of the neighbouring field and increase passenger count of the ship") {
            val ship = Ship(team = Team.ONE, position = CubeCoordinates(0, 0))
            val nextPassengerField = board.findNearestFieldTypes(CubeCoordinates(0, 0), Field.PASSENGER::class).first()
            ship.position = nextPassengerField + (board[nextPassengerField] as Field.PASSENGER).direction.vector
            
            val initialShipPassengers: Int = ship.passengers
            val initialFieldPassengers: Int = board.neighboringFields(ship.position)
                                                 .filterIsInstance<Field.PASSENGER>().first().passenger
            
            val isPickedUp = board.pickupPassenger(ship)
            
            isPickedUp shouldBe true
            
            ship.passengers shouldBe initialShipPassengers + initialFieldPassengers
            board.neighboringFields(ship.position)
                    .filterIsInstance<Field.PASSENGER>().first().passenger shouldBe initialFieldPassengers - 1
        }
        
        test("should return false and not change passenger count when no neighbouring passenger fields") {
            val ship = Ship(team = Team.ONE, position = CubeCoordinates(0, 0))
            
            val initialShipPassengers = ship.passengers
            
            val isPickedUp = board.pickupPassenger(ship)
            
            isPickedUp shouldBe false
            ship.passengers shouldBe initialShipPassengers
        }
        
    }

    test("clones deeply") {
        val clone = board.clone()
        board shouldBe clone
        board.hashCode() shouldBe clone.hashCode()
        
        val coords = board.findNearestFieldTypes(CubeCoordinates(4, 0), Field.PASSENGER::class).first()
        val passengerSegment  = board.findSegment(coords)!!
        //val passengerSegment = board.segments.indexOfFirst { it.segment.any { it.any { it is Field.PASSENGER } } }
        (passengerSegment[coords] as Field.PASSENGER).passenger = 0
        board shouldNotBe clone
        board.hashCode() shouldNotBe clone.hashCode()
        (clone[coords] as Field.PASSENGER).passenger shouldBe 1
    }
    context("XML Serialization") {
        test("single segment") {
            val serializedSegment = """
                  <segment direction="RIGHT">
                    <center q="0" r="0" s="0"/>
                    <field-array>
                      <sc.plugin2024.Field_-WATER/>
                      <sc.plugin2024.Field_-WATER/>
                      <sc.plugin2024.Field_-WATER/>
                      <sc.plugin2024.Field_-WATER/>
                      <sc.plugin2024.Field_-WATER/>
                    </field-array>
                    <field-array>
                      <sc.plugin2024.Field_-WATER/>
                      <sc.plugin2024.Field_-WATER/>
                      <sc.plugin2024.Field_-WATER/>
                      <sc.plugin2024.Field_-WATER/>
                      <sc.plugin2024.Field_-WATER/>
                    </field-array>
                    <field-array>
                      <sc.plugin2024.Field_-WATER/>
                      <sc.plugin2024.Field_-WATER/>
                      <sc.plugin2024.Field_-WATER/>
                      <sc.plugin2024.Field_-WATER/>
                      <sc.plugin2024.Field_-WATER/>
                    </field-array>
                    <field-array>
                      <sc.plugin2024.Field_-WATER/>
                      <sc.plugin2024.Field_-WATER/>
                      <sc.plugin2024.Field_-WATER/>
                      <sc.plugin2024.Field_-WATER/>
                      <sc.plugin2024.Field_-WATER/>
                    </field-array>
                  </segment>"""
            val serialized = """
                <board nextDirection="RIGHT">$serializedSegment
                </board>"""
            val segment = Segment(CubeDirection.RIGHT, CubeCoordinates.ORIGIN, generateSegment(false, arrayOf()))
            Board(listOf(segment), 1) shouldSerializeTo serialized
            // TODO Board(listOf(segment, segment), 1) shouldSerializeTo serialized
            Board(listOf(segment, segment), 2) shouldSerializeTo """
                <board nextDirection="RIGHT">$serializedSegment$serializedSegment
                </board>"""
        }
        test("random Board has correct length") {
            testXStream.toXML(Board()) shouldHaveLineCount 265
        }
        test("Board with content") {
            val fieldTwo = "<field>TWO</field>"
            //testXStream.fromXML(fieldTwo) shouldBe Field(penguin = Team.TWO)
            //testXStream.fromXML("<board><list>$fieldTwo</list>") shouldBe Board(listOf(mutableListOf(Field(penguin = Team.TWO))))
            //testXStream.toXML(makeBoard(0 y 0 to 1)) shouldContainOnlyOnce fieldTwo
        }
    }
})