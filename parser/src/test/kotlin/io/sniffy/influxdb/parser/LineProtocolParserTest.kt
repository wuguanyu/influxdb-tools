package io.sniffy.influxdb.parser

import io.sniffy.influxdb.lineprotocol.FieldBooleanValue
import io.sniffy.influxdb.lineprotocol.FieldFloatValue
import io.sniffy.influxdb.lineprotocol.FieldIntegerValue
import io.sniffy.influxdb.lineprotocol.FieldStringValue
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import ru.yandex.qatools.allure.annotations.Issue

internal class LineProtocolParserTest {

    @Test
    fun simpleTest() {
        assertTrue(true)
    }

    @Test
    fun parseOneValidLine() {
        val parser = LineProtocolParser("weather,location=us-midwest temperature=82 1465839830100400200")

        assertTrue(parser.hasNext())

        val point = parser.next()

        assertEquals("weather", point.measurement)
        assertEquals(mapOf("location" to "us-midwest"), point.tags)
        assertEquals(mapOf("temperature" to FieldFloatValue(82.0)), point.values)
        assertEquals(1465839830100400200, point.timestamp)

        assertFalse(parser.hasNext())
    }

    @Test
    fun parseEmptyLine() {
        val parser = LineProtocolParser("\nweather,location=us-midwest temperature=82 1465839830100400200")

        assertTrue(parser.hasNext())

        val point = parser.next()

        assertEquals("weather", point.measurement)
        assertEquals(mapOf("location" to "us-midwest"), point.tags)
        assertEquals(mapOf("temperature" to FieldFloatValue(82.0)), point.values)
        assertEquals(1465839830100400200, point.timestamp)

        assertFalse(parser.hasNext())
    }

    @Test
    fun parseNoTimestamp() {
        val parser = LineProtocolParser("weather,location=us-midwest temperature=82")

        assertTrue(parser.hasNext())

        val point = parser.next()

        assertEquals("weather", point.measurement)
        assertEquals(mapOf("location" to "us-midwest"), point.tags)
        assertEquals(mapOf("temperature" to FieldFloatValue(82.0)), point.values)
        assertNull(point.timestamp)

        assertFalse(parser.hasNext())
    }

    @Test
    fun parseNoTimestampTwoLines() {
        val parser = LineProtocolParser("weather,location=us-midwest temperature=82\n" +
                "weather,location=us-midwest temperature=83")

        assertTrue(parser.hasNext())

        val point = parser.next()

        assertEquals("weather", point.measurement)
        assertEquals(mapOf("location" to "us-midwest"), point.tags)
        assertEquals(mapOf("temperature" to FieldFloatValue(82.0)), point.values)
        assertNull(point.timestamp)

        assertTrue(parser.hasNext())
    }

    @Test
    fun parseMultipleSpacesBeforeFields() {
        val parser = LineProtocolParser("weather,location=us-midwest   temperature=82 1465839830100400200")

        assertTrue(parser.hasNext())

        val point = parser.next()

        assertEquals("weather", point.measurement)
        assertEquals(mapOf("location" to "us-midwest"), point.tags)
        assertEquals(mapOf("temperature" to FieldFloatValue(82.0)), point.values)
        assertEquals(1465839830100400200, point.timestamp)

        assertFalse(parser.hasNext())
    }

    @Test
    fun parseMultipleSpacesAfterFields() {
        val parser = LineProtocolParser("weather,location=us-midwest temperature=82    1465839830100400200")

        assertTrue(parser.hasNext())

        val point = parser.next()

        assertEquals("weather", point.measurement)
        assertEquals(mapOf("location" to "us-midwest"), point.tags)
        assertEquals(mapOf("temperature" to FieldFloatValue(82.0)), point.values)
        assertEquals(1465839830100400200, point.timestamp)

        assertFalse(parser.hasNext())
    }

    @Test
    fun parseTwoValidLine() {
        val parser = LineProtocolParser("weather,location=us-midwest temperature=82 1465839830100400200\n" +
                "weather,location=us-midwest temperature=83 1465839830101400200")

        run {
            assertTrue(parser.hasNext())

            val point = parser.next()

            assertEquals("weather", point.measurement)
            assertEquals(mapOf("location" to "us-midwest"), point.tags)
            assertEquals(mapOf("temperature" to FieldFloatValue(82.0)), point.values)
            assertEquals(1465839830100400200, point.timestamp)
        }

        run {
            assertTrue(parser.hasNext())

            val point = parser.next()

            assertEquals("weather", point.measurement)
            assertEquals(mapOf("location" to "us-midwest"), point.tags)
            assertEquals(mapOf("temperature" to FieldFloatValue(83.0)), point.values)
            assertEquals(1465839830101400200, point.timestamp)
        }

        assertFalse(parser.hasNext())
    }

    @Test
    fun hasNextSkippedThrowsException() {
        val parser = LineProtocolParser("weather,location=us-midwest temperature=82 1465839830100400200")

        assertThrows(NoSuchElementException::class.java, {
            parser.next()
        })
    }

    @Test
    fun parseOneLineErrorInMeasurement() {
        run {
            val parser = LineProtocolParser(",location=us-midwest temperature=82 1465839830100400200")
            assertFalse(parser.hasNext())
        }
        run {
            val parser = LineProtocolParser("\n,location=us-midwest temperature=82 1465839830100400200")
            assertFalse(parser.hasNext())
        }
    }

    @Test
    fun parseOneLineEscapedMeasurement() {
        run {
            val parser = LineProtocolParser("\\,,location=us-midwest temperature=82 1465839830100400200")
            assertTrue(parser.hasNext())

            val point = parser.next()

            assertEquals(",", point.measurement)
            assertEquals(mapOf("location" to "us-midwest"), point.tags)
            assertEquals(mapOf("temperature" to FieldFloatValue(82.0)), point.values)
            assertEquals(1465839830100400200, point.timestamp)

            assertFalse(parser.hasNext())
        }

        run {

            val parser = LineProtocolParser("\\ ,location=us-midwest temperature=82 1465839830100400200")
            assertTrue(parser.hasNext())

            val point = parser.next()

            assertEquals(" ", point.measurement)
            assertEquals(mapOf("location" to "us-midwest"), point.tags)
            assertEquals(mapOf("temperature" to FieldFloatValue(82.0)), point.values)
            assertEquals(1465839830100400200, point.timestamp)

            assertFalse(parser.hasNext())
        }

        run {

            val parser = LineProtocolParser("\\\\ ,location=us-midwest temperature=82 1465839830100400200")
            assertTrue(parser.hasNext())

            val point = parser.next()

            assertEquals("\\ ", point.measurement)
            assertEquals(mapOf("location" to "us-midwest"), point.tags)
            assertEquals(mapOf("temperature" to FieldFloatValue(82.0)), point.values)
            assertEquals(1465839830100400200, point.timestamp)

            assertFalse(parser.hasNext())
        }
    }

    @Test
    fun parseValidLineAfterComment() {
        val parser = LineProtocolParser("#comment\nweather,location=us-midwest temperature=82 1465839830100400200")

        assertTrue(parser.hasNext())

        val point = parser.next()

        assertEquals("weather", point.measurement)
        assertEquals(mapOf("location" to "us-midwest"), point.tags)
        assertEquals(mapOf("temperature" to FieldFloatValue(82.0)), point.values)
        assertEquals(1465839830100400200, point.timestamp)

        assertFalse(parser.hasNext())
    }

    @Test
    fun parseOneLineErrorInTagKey() {
        run {
            val parser = LineProtocolParser("weather,loc\nation=us-midwest temperature=82 1465839830100400200")
            assertFalse(parser.hasNext())
        }
        run {
            val parser = LineProtocolParser("weather,loc ation=us-midwest temperature=82 1465839830100400200")
            assertFalse(parser.hasNext())
        }
        run {
            val parser = LineProtocolParser("weather,\n=us-midwest temperature=82 1465839830100400200")
            assertFalse(parser.hasNext())
        }
        run {
            val parser = LineProtocolParser("weather,=us-midwest temperature=82 1465839830100400200")
            assertFalse(parser.hasNext())
        }
        run {
            val parser = LineProtocolParser("weather,location=us-midwest,\n=bar temperature=82 1465839830100400200")
            assertFalse(parser.hasNext())
        }
        run {
            val parser = LineProtocolParser("weather,location=us-midwest,=bar temperature=82 1465839830100400200")
            assertFalse(parser.hasNext())
        }
    }

    @Test
    fun parseOneLineEscapedTagKey() {
        run {
            val parser = LineProtocolParser("measurement,loc\\,ation=us-midwest temperature=82 1465839830100400200")
            assertTrue(parser.hasNext())

            val point = parser.next()

            assertEquals("measurement", point.measurement)
            assertEquals(mapOf("loc,ation" to "us-midwest"), point.tags)
            assertEquals(mapOf("temperature" to FieldFloatValue(82.0)), point.values)
            assertEquals(1465839830100400200, point.timestamp)

            assertFalse(parser.hasNext())
        }

        run {
            val parser = LineProtocolParser("measurement,loc\\ ation=us-midwest temperature=82 1465839830100400200")
            assertTrue(parser.hasNext())

            val point = parser.next()

            assertEquals("measurement", point.measurement)
            assertEquals(mapOf("loc ation" to "us-midwest"), point.tags)
            assertEquals(mapOf("temperature" to FieldFloatValue(82.0)), point.values)
            assertEquals(1465839830100400200, point.timestamp)

            assertFalse(parser.hasNext())
        }

        run {
            val parser = LineProtocolParser("measurement,loc\\=ation=us-midwest temperature=82 1465839830100400200")
            assertTrue(parser.hasNext())

            val point = parser.next()

            assertEquals("measurement", point.measurement)
            assertEquals(mapOf("loc=ation" to "us-midwest"), point.tags)
            assertEquals(mapOf("temperature" to FieldFloatValue(82.0)), point.values)
            assertEquals(1465839830100400200, point.timestamp)

            assertFalse(parser.hasNext())
        }

        run {
            val parser = LineProtocolParser("measurement,\\,location=us-midwest temperature=82 1465839830100400200")
            assertTrue(parser.hasNext())

            val point = parser.next()

            assertEquals("measurement", point.measurement)
            assertEquals(mapOf(",location" to "us-midwest"), point.tags)
            assertEquals(mapOf("temperature" to FieldFloatValue(82.0)), point.values)
            assertEquals(1465839830100400200, point.timestamp)

            assertFalse(parser.hasNext())
        }

        run {
            val parser = LineProtocolParser("measurement,\\ location=us-midwest temperature=82 1465839830100400200")
            assertTrue(parser.hasNext())

            val point = parser.next()

            assertEquals("measurement", point.measurement)
            assertEquals(mapOf(" location" to "us-midwest"), point.tags)
            assertEquals(mapOf("temperature" to FieldFloatValue(82.0)), point.values)
            assertEquals(1465839830100400200, point.timestamp)

            assertFalse(parser.hasNext())
        }

        run {
            val parser = LineProtocolParser("measurement,\\=location=us-midwest temperature=82 1465839830100400200")
            assertTrue(parser.hasNext())

            val point = parser.next()

            assertEquals("measurement", point.measurement)
            assertEquals(mapOf("=location" to "us-midwest"), point.tags)
            assertEquals(mapOf("temperature" to FieldFloatValue(82.0)), point.values)
            assertEquals(1465839830100400200, point.timestamp)

            assertFalse(parser.hasNext())
        }

        run {
            val parser = LineProtocolParser("measurement,loc\\\\,ation=us-midwest temperature=82 1465839830100400200")
            assertTrue(parser.hasNext())

            val point = parser.next()

            assertEquals("measurement", point.measurement)
            assertEquals(mapOf("loc\\,ation" to "us-midwest"), point.tags)
            assertEquals(mapOf("temperature" to FieldFloatValue(82.0)), point.values)
            assertEquals(1465839830100400200, point.timestamp)

            assertFalse(parser.hasNext())
        }

    }

    @Test
    fun parseOneLineEscapedTagValue() {
        run {
            val parser = LineProtocolParser("measurement,location=us-mid\\,west temperature=82 1465839830100400200")
            assertTrue(parser.hasNext())

            val point = parser.next()

            assertEquals("measurement", point.measurement)
            assertEquals(mapOf("location" to "us-mid,west"), point.tags)
            assertEquals(mapOf("temperature" to FieldFloatValue(82.0)), point.values)
            assertEquals(1465839830100400200, point.timestamp)

            assertFalse(parser.hasNext())
        }

        run {
            val parser = LineProtocolParser("measurement,location=us-mid\\ west temperature=82 1465839830100400200")
            assertTrue(parser.hasNext())

            val point = parser.next()

            assertEquals("measurement", point.measurement)
            assertEquals(mapOf("location" to "us-mid west"), point.tags)
            assertEquals(mapOf("temperature" to FieldFloatValue(82.0)), point.values)
            assertEquals(1465839830100400200, point.timestamp)

            assertFalse(parser.hasNext())
        }

        run {
            val parser = LineProtocolParser("measurement,location=us-mid\\=west temperature=82 1465839830100400200")
            assertTrue(parser.hasNext())

            val point = parser.next()

            assertEquals("measurement", point.measurement)
            assertEquals(mapOf("location" to "us-mid=west"), point.tags)
            assertEquals(mapOf("temperature" to FieldFloatValue(82.0)), point.values)
            assertEquals(1465839830100400200, point.timestamp)

            assertFalse(parser.hasNext())
        }

        run {
            val parser = LineProtocolParser("measurement,location=\\,us-midwest temperature=82 1465839830100400200")
            assertTrue(parser.hasNext())

            val point = parser.next()

            assertEquals("measurement", point.measurement)
            assertEquals(mapOf("location" to ",us-midwest"), point.tags)
            assertEquals(mapOf("temperature" to FieldFloatValue(82.0)), point.values)
            assertEquals(1465839830100400200, point.timestamp)

            assertFalse(parser.hasNext())
        }

        run {
            val parser = LineProtocolParser("measurement,location=\\ us-midwest temperature=82 1465839830100400200")
            assertTrue(parser.hasNext())

            val point = parser.next()

            assertEquals("measurement", point.measurement)
            assertEquals(mapOf("location" to " us-midwest"), point.tags)
            assertEquals(mapOf("temperature" to FieldFloatValue(82.0)), point.values)
            assertEquals(1465839830100400200, point.timestamp)

            assertFalse(parser.hasNext())
        }

        run {
            val parser = LineProtocolParser("measurement,location=\\=us-midwest temperature=82 1465839830100400200")
            assertTrue(parser.hasNext())

            val point = parser.next()

            assertEquals("measurement", point.measurement)
            assertEquals(mapOf("location" to "=us-midwest"), point.tags)
            assertEquals(mapOf("temperature" to FieldFloatValue(82.0)), point.values)
            assertEquals(1465839830100400200, point.timestamp)

            assertFalse(parser.hasNext())
        }

        run {
            val parser = LineProtocolParser("measurement,location=us-mid\\\\,west temperature=82 1465839830100400200")
            assertTrue(parser.hasNext())

            val point = parser.next()

            assertEquals("measurement", point.measurement)
            assertEquals(mapOf("location" to "us-mid\\,west"), point.tags)
            assertEquals(mapOf("temperature" to FieldFloatValue(82.0)), point.values)
            assertEquals(1465839830100400200, point.timestamp)

            assertFalse(parser.hasNext())
        }

    }

    @Test
    fun parseOneLineEscapedFieldKey() {
        run {
            val parser = LineProtocolParser("measurement,location=us-midwest temp\\,erature=82 1465839830100400200")
            assertTrue(parser.hasNext())

            val point = parser.next()

            assertEquals("measurement", point.measurement)
            assertEquals(mapOf("location" to "us-midwest"), point.tags)
            assertEquals(mapOf("temp,erature" to FieldFloatValue(82.0)), point.values)
            assertEquals(1465839830100400200, point.timestamp)

            assertFalse(parser.hasNext())
        }

        run {
            val parser = LineProtocolParser("measurement,location=us-midwest temp\\ erature=82 1465839830100400200")
            assertTrue(parser.hasNext())

            val point = parser.next()

            assertEquals("measurement", point.measurement)
            assertEquals(mapOf("location" to "us-midwest"), point.tags)
            assertEquals(mapOf("temp erature" to FieldFloatValue(82.0)), point.values)
            assertEquals(1465839830100400200, point.timestamp)

            assertFalse(parser.hasNext())
        }

        run {
            val parser = LineProtocolParser("measurement,location=us-midwest temp\\=erature=82 1465839830100400200")
            assertTrue(parser.hasNext())

            val point = parser.next()

            assertEquals("measurement", point.measurement)
            assertEquals(mapOf("location" to "us-midwest"), point.tags)
            assertEquals(mapOf("temp=erature" to FieldFloatValue(82.0)), point.values)
            assertEquals(1465839830100400200, point.timestamp)

            assertFalse(parser.hasNext())
        }

        run {
            val parser = LineProtocolParser("measurement,location=us-midwest \\,temperature=82 1465839830100400200")
            assertTrue(parser.hasNext())

            val point = parser.next()

            assertEquals("measurement", point.measurement)
            assertEquals(mapOf("location" to "us-midwest"), point.tags)
            assertEquals(mapOf(",temperature" to FieldFloatValue(82.0)), point.values)
            assertEquals(1465839830100400200, point.timestamp)

            assertFalse(parser.hasNext())
        }

        run {
            val parser = LineProtocolParser("measurement,location=us-midwest \\ temperature=82 1465839830100400200")
            assertTrue(parser.hasNext())

            val point = parser.next()

            assertEquals("measurement", point.measurement)
            assertEquals(mapOf("location" to "us-midwest"), point.tags)
            assertEquals(mapOf(" temperature" to FieldFloatValue(82.0)), point.values)
            assertEquals(1465839830100400200, point.timestamp)

            assertFalse(parser.hasNext())
        }

        run {
            val parser = LineProtocolParser("measurement,location=us-midwest \\=temperature=82 1465839830100400200")
            assertTrue(parser.hasNext())

            val point = parser.next()

            assertEquals("measurement", point.measurement)
            assertEquals(mapOf("location" to "us-midwest"), point.tags)
            assertEquals(mapOf("=temperature" to FieldFloatValue(82.0)), point.values)
            assertEquals(1465839830100400200, point.timestamp)

            assertFalse(parser.hasNext())
        }

        run {
            val parser = LineProtocolParser("measurement,location=us-midwest temp\\\\,erature=82 1465839830100400200")
            assertTrue(parser.hasNext())

            val point = parser.next()

            assertEquals("measurement", point.measurement)
            assertEquals(mapOf("location" to "us-midwest"), point.tags)
            assertEquals(mapOf("temp\\,erature" to FieldFloatValue(82.0)), point.values)
            assertEquals(1465839830100400200, point.timestamp)

            assertFalse(parser.hasNext())
        }

    }

    @Test
    fun parseOneLineEscapedStringFieldValue() {
        run {
            val parser = LineProtocolParser("measurement,location=us-midwest temperature=\"82\" 1465839830100400200")
            assertTrue(parser.hasNext())

            val point = parser.next()

            assertEquals("measurement", point.measurement)
            assertEquals(mapOf("location" to "us-midwest"), point.tags)
            assertEquals(mapOf("temperature" to FieldStringValue("82")), point.values)
            assertEquals(1465839830100400200, point.timestamp)

            assertFalse(parser.hasNext())
        }
        run {
            val parser = LineProtocolParser("measurement,location=us-midwest temperature=\"8\n2\" 1465839830100400200")
            assertTrue(parser.hasNext())

            val point = parser.next()

            assertEquals("measurement", point.measurement)
            assertEquals(mapOf("location" to "us-midwest"), point.tags)
            assertEquals(mapOf("temperature" to FieldStringValue("8\n2")), point.values)
            assertEquals(1465839830100400200, point.timestamp)

            assertFalse(parser.hasNext())
        }
        run {
            val parser = LineProtocolParser("measurement,location=us-midwest temperature=\"8\\\"2\" 1465839830100400200")
            assertTrue(parser.hasNext())

            val point = parser.next()

            assertEquals("measurement", point.measurement)
            assertEquals(mapOf("location" to "us-midwest"), point.tags)
            assertEquals(mapOf("temperature" to FieldStringValue("8\"2")), point.values)
            assertEquals(1465839830100400200, point.timestamp)

            assertFalse(parser.hasNext())
        }
    }

    @Test
    @Issue("https://github.com/sniffy/influxdb-tools/issues/1")
    fun parseOneLineEscapedStringFieldValueWithManyEscapeCharacters() {
        run {
            val line = "measurement,location=us-midwest temperature=\"8\\\"2\" 1465839830100400200"
            val parser = LineProtocolParser(line)
            assertTrue(parser.hasNext())

            val point = parser.next()

            assertEquals("measurement", point.measurement)
            assertEquals(mapOf("location" to "us-midwest"), point.tags)
            assertEquals(mapOf("temperature" to FieldStringValue("8\"2")), point.values)
            assertEquals(1465839830100400200, point.timestamp)

            assertFalse(parser.hasNext())

            assertEquals(line, point.toString())
        }
        run {
            val parser = LineProtocolParser("measurement,location=us-midwest temperature=\"8\\\\\"2\" 1465839830100400200")
            assertTrue(parser.hasNext())

            val point = parser.next()

            assertEquals("measurement", point.measurement)
            assertEquals(mapOf("location" to "us-midwest"), point.tags)
            assertEquals(mapOf("temperature" to FieldStringValue("8\\")), point.values)

            assertFalse(parser.hasNext())

            assertEquals("measurement,location=us-midwest temperature=\"8\\\\\"", point.toString())
        }
        run {
            val parser = LineProtocolParser("measurement,location=us-midwest temperature=\"8\\\\\"2\" 1465839830100400200", true)
            assertFalse(parser.hasNext())
        }
        run {
            val parser = LineProtocolParser("measurement,location=us-midwest temperature=\"8\\\\\\\"2\" 1465839830100400200")
            assertTrue(parser.hasNext())

            val point = parser.next()

            assertEquals("measurement", point.measurement)
            assertEquals(mapOf("location" to "us-midwest"), point.tags)
            assertEquals(mapOf("temperature" to FieldStringValue("8\\\"2")), point.values)
            assertEquals(1465839830100400200, point.timestamp)

            assertFalse(parser.hasNext())

            assertEquals("measurement,location=us-midwest temperature=\"8\\\\\\\"2\" 1465839830100400200", point.toString())
        }
        run {
            val parser = LineProtocolParser("measurement,location=us-midwest temperature=\"8\\\\\\\\\"2\" 1465839830100400200")
            assertTrue(parser.hasNext())

            val point = parser.next()

            assertEquals("measurement", point.measurement)
            assertEquals(mapOf("location" to "us-midwest"), point.tags)
            assertEquals(mapOf("temperature" to FieldStringValue("8\\\\")), point.values)

            assertFalse(parser.hasNext())

            assertEquals("measurement,location=us-midwest temperature=\"8\\\\\\\\\"", point.toString())
        }
        run {
            val parser = LineProtocolParser("measurement,location=us-midwest temperature=\"8\\\\\\\\\"2\" 1465839830100400200", true)
            assertFalse(parser.hasNext())
        }
    }

    @Test
    fun parseOneLineIntegerFieldValue() {
        run {
            val parser = LineProtocolParser("measurement,location=us-midwest temperature=82i 1465839830100400200")
            assertTrue(parser.hasNext())

            val point = parser.next()

            assertEquals("measurement", point.measurement)
            assertEquals(mapOf("location" to "us-midwest"), point.tags)
            assertEquals(mapOf("temperature" to FieldIntegerValue(82)), point.values)
            assertEquals(1465839830100400200, point.timestamp)

            assertFalse(parser.hasNext())
        }
    }

    @Test
    fun parseOneLineBooleanFieldValue() {
        run {
            val parser = LineProtocolParser("measurement,location=us-midwest temperature=true 1465839830100400200")
            assertTrue(parser.hasNext())

            val point = parser.next()

            assertEquals("measurement", point.measurement)
            assertEquals(mapOf("location" to "us-midwest"), point.tags)
            assertEquals(mapOf("temperature" to FieldBooleanValue(true)), point.values)
            assertEquals(1465839830100400200, point.timestamp)

            assertFalse(parser.hasNext())
        }
        run {
            val parser = LineProtocolParser("measurement,location=us-midwest temperature=True 1465839830100400200")
            assertTrue(parser.hasNext())

            val point = parser.next()

            assertEquals("measurement", point.measurement)
            assertEquals(mapOf("location" to "us-midwest"), point.tags)
            assertEquals(mapOf("temperature" to FieldBooleanValue(true)), point.values)
            assertEquals(1465839830100400200, point.timestamp)

            assertFalse(parser.hasNext())
        }
        run {
            val parser = LineProtocolParser("measurement,location=us-midwest temperature=TRUE 1465839830100400200")
            assertTrue(parser.hasNext())

            val point = parser.next()

            assertEquals("measurement", point.measurement)
            assertEquals(mapOf("location" to "us-midwest"), point.tags)
            assertEquals(mapOf("temperature" to FieldBooleanValue(true)), point.values)
            assertEquals(1465839830100400200, point.timestamp)

            assertFalse(parser.hasNext())
        }
        run {
            val parser = LineProtocolParser("measurement,location=us-midwest temperature=t 1465839830100400200")
            assertTrue(parser.hasNext())

            val point = parser.next()

            assertEquals("measurement", point.measurement)
            assertEquals(mapOf("location" to "us-midwest"), point.tags)
            assertEquals(mapOf("temperature" to FieldBooleanValue(true)), point.values)
            assertEquals(1465839830100400200, point.timestamp)

            assertFalse(parser.hasNext())
        }
        run {
            val parser = LineProtocolParser("measurement,location=us-midwest temperature=T 1465839830100400200")
            assertTrue(parser.hasNext())

            val point = parser.next()

            assertEquals("measurement", point.measurement)
            assertEquals(mapOf("location" to "us-midwest"), point.tags)
            assertEquals(mapOf("temperature" to FieldBooleanValue(true)), point.values)
            assertEquals(1465839830100400200, point.timestamp)

            assertFalse(parser.hasNext())
        }
        run {
            val parser = LineProtocolParser("measurement,location=us-midwest temperature=false 1465839830100400200")
            assertTrue(parser.hasNext())

            val point = parser.next()

            assertEquals("measurement", point.measurement)
            assertEquals(mapOf("location" to "us-midwest"), point.tags)
            assertEquals(mapOf("temperature" to FieldBooleanValue(false)), point.values)
            assertEquals(1465839830100400200, point.timestamp)

            assertFalse(parser.hasNext())
        }
        run {
            val parser = LineProtocolParser("measurement,location=us-midwest temperature=False 1465839830100400200")
            assertTrue(parser.hasNext())

            val point = parser.next()

            assertEquals("measurement", point.measurement)
            assertEquals(mapOf("location" to "us-midwest"), point.tags)
            assertEquals(mapOf("temperature" to FieldBooleanValue(false)), point.values)
            assertEquals(1465839830100400200, point.timestamp)

            assertFalse(parser.hasNext())
        }
        run {
            val parser = LineProtocolParser("measurement,location=us-midwest temperature=FALSE 1465839830100400200")
            assertTrue(parser.hasNext())

            val point = parser.next()

            assertEquals("measurement", point.measurement)
            assertEquals(mapOf("location" to "us-midwest"), point.tags)
            assertEquals(mapOf("temperature" to FieldBooleanValue(false)), point.values)
            assertEquals(1465839830100400200, point.timestamp)

            assertFalse(parser.hasNext())
        }
        run {
            val parser = LineProtocolParser("measurement,location=us-midwest temperature=f 1465839830100400200")
            assertTrue(parser.hasNext())

            val point = parser.next()

            assertEquals("measurement", point.measurement)
            assertEquals(mapOf("location" to "us-midwest"), point.tags)
            assertEquals(mapOf("temperature" to FieldBooleanValue(false)), point.values)
            assertEquals(1465839830100400200, point.timestamp)

            assertFalse(parser.hasNext())
        }
        run {
            val parser = LineProtocolParser("measurement,location=us-midwest temperature=F 1465839830100400200")
            assertTrue(parser.hasNext())

            val point = parser.next()

            assertEquals("measurement", point.measurement)
            assertEquals(mapOf("location" to "us-midwest"), point.tags)
            assertEquals(mapOf("temperature" to FieldBooleanValue(false)), point.values)
            assertEquals(1465839830100400200, point.timestamp)

            assertFalse(parser.hasNext())
        }
    }

}