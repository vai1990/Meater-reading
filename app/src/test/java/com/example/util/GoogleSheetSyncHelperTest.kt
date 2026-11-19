package com.example.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GoogleSheetSyncHelperTest {

    @Test
    fun testParseTirupatiCrusherSheetFormat() {
        val sampleCsv = """
            TIRUPATI CRUSHER,,,,,
            DATE,TIME,Kwh,KvArh,KvAh,PF
            07-Sep-2026,8:41:00 AM,54654,24897,70422,513
            08-Sep-2026,8:15:00 AM,54667,24903,70436,985
            10-Sep-2026,8:23:00 AM,54746,24926,70520,-984
        """.trimIndent()

        val parsed = GoogleSheetSyncHelper.parseCustomerSheetCsv(sampleCsv)

        assertEquals("TIRUPATI CRUSHER", parsed.tabName)
        assertEquals("TIRUPATI CRUSHER", parsed.customer.name)
        assertEquals(3, parsed.readings.size)

        val first = parsed.readings[0]
        assertEquals("07-Sep-2026", first.readingDate)
        assertEquals("8:41:00 AM", first.readingTime)
        assertEquals(54654.0, first.kwh, 0.01)
        assertEquals(24897.0, first.kvarh, 0.01)
        assertEquals(70422.0, first.kvah, 0.01)
        assertEquals(0.513, first.powerFactor, 0.001)
        assertTrue("Remarks should include PF value", first.remarks.contains("513"))

        val second = parsed.readings[1]
        assertEquals(13.0, second.unitsConsumed ?: 0.0, 0.01)
        assertEquals(0.985, second.powerFactor, 0.001)

        val third = parsed.readings[2]
        assertEquals(0.984, third.powerFactor, 0.001)
        assertTrue("Negative PF should note Lead in remarks", third.remarks.contains("-984"))
    }

    @Test
    fun testExtractSpreadsheetIdAndGid() {
        val url = "https://docs.google.com/spreadsheets/d/1AbC_dEfGhIjKlMnOpQrStUvWxYz123456/edit#gid=987654"
        assertEquals("1AbC_dEfGhIjKlMnOpQrStUvWxYz123456", GoogleSheetSyncHelper.extractSpreadsheetId(url))
        assertEquals("987654", GoogleSheetSyncHelper.extractGid(url))
    }
}
