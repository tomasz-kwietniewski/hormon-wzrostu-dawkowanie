package pl.hormonwzrostu.util

import pl.hormonwzrostu.data.CsvLabels
import pl.hormonwzrostu.data.IntakeRow
import java.io.ByteArrayOutputStream
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Minimalny generator pliku .xlsx (Office Open XML) bez zewnętrznych bibliotek.
 * .xlsx to ZIP z plikami XML w UTF-8 — dzięki temu polskie znaki i podział na
 * kolumny są poprawne w Excelu i Arkuszach Google, bez problemów z kodowaniem CSV.
 */
fun buildIntakeXlsx(sheetName: String, labels: CsvLabels, rows: List<IntakeRow>): ByteArray {
    val headers = listOf(labels.date, labels.day, labels.dose, labels.status, labels.comment)
    val cols = "ABCDE"

    val sheet = StringBuilder()
    sheet.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
    sheet.append("""<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"><sheetData>""")

    sheet.append("""<row r="1">""")
    headers.forEachIndexed { i, h -> sheet.append(strCell("${cols[i]}1", h)) }
    sheet.append("</row>")

    rows.forEachIndexed { r, row ->
        val rn = r + 2
        sheet.append("""<row r="$rn">""")
        sheet.append(strCell("A$rn", row.date))
        sheet.append(numCell("B$rn", row.day.toString()))
        sheet.append(numCell("C$rn", doseXml(row.doseMg)))
        sheet.append(strCell("D$rn", row.status))
        sheet.append(strCell("E$rn", row.comment))
        sheet.append("</row>")
    }
    sheet.append("</sheetData></worksheet>")

    val baos = ByteArrayOutputStream()
    ZipOutputStream(baos).use { zip ->
        zip.put("[Content_Types].xml", CONTENT_TYPES)
        zip.put("_rels/.rels", RELS)
        zip.put("xl/workbook.xml", workbookXml(sheetName))
        zip.put("xl/_rels/workbook.xml.rels", WORKBOOK_RELS)
        zip.put("xl/worksheets/sheet1.xml", sheet.toString())
    }
    return baos.toByteArray()
}

private fun ZipOutputStream.put(name: String, content: String) {
    putNextEntry(ZipEntry(name))
    write(content.toByteArray(Charsets.UTF_8))
    closeEntry()
}

private fun strCell(ref: String, value: String): String =
    """<c r="$ref" t="inlineStr"><is><t xml:space="preserve">${xmlEscape(value)}</t></is></c>"""

private fun numCell(ref: String, value: String): String =
    """<c r="$ref"><v>$value</v></c>"""

private fun doseXml(value: Double): String =
    BigDecimal(value).setScale(2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()

private fun xmlEscape(s: String): String =
    s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")

private const val CONTENT_TYPES =
    """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""" +
        """<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">""" +
        """<Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>""" +
        """<Default Extension="xml" ContentType="application/xml"/>""" +
        """<Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>""" +
        """<Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>""" +
        """</Types>"""

private const val RELS =
    """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""" +
        """<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">""" +
        """<Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>""" +
        """</Relationships>"""

private const val WORKBOOK_RELS =
    """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""" +
        """<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">""" +
        """<Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>""" +
        """</Relationships>"""

private fun workbookXml(sheetName: String): String =
    """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""" +
        """<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" """ +
        """xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">""" +
        """<sheets><sheet name="${xmlEscape(sheetName.take(31))}" sheetId="1" r:id="rId1"/></sheets></workbook>"""
