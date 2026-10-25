package de.vokabeltrainer.app.data

import org.xmlpull.v1.XmlPullParser
import java.io.ByteArrayInputStream
import java.io.IOException
import java.util.zip.ZipInputStream

/**
 * Minimal reader for .xlsx files: returns the cell texts of one worksheet as rows.
 * Supports shared strings, inline strings and plain values, which covers files
 * written by Excel, Google Sheets and LibreOffice.
 */
class XlsxReader(private val newParser: () -> XmlPullParser) {

    /** Reads the sheet named [preferredSheet] if it exists, otherwise the first sheet. */
    fun readRows(bytes: ByteArray, preferredSheet: String? = null): List<List<String>> {
        val entries = unzip(bytes)
        val workbook = entries["xl/workbook.xml"] ?: throw IOException("Keine gültige xlsx-Datei")
        val sheets = readSheets(workbook)
        if (sheets.isEmpty()) throw IOException("Die Datei enthält kein Tabellenblatt")
        val sheet = sheets.firstOrNull { it.first.equals(preferredSheet, ignoreCase = true) } ?: sheets.first()

        val rels = entries["xl/_rels/workbook.xml.rels"]?.let { readRelationships(it) } ?: emptyMap()
        val target = rels[sheet.second] ?: "worksheets/sheet1.xml"
        val path = if (target.startsWith("/")) target.removePrefix("/") else "xl/$target"
        val sheetXml = entries[path] ?: throw IOException("Tabellenblatt $path fehlt")

        val shared = entries["xl/sharedStrings.xml"]?.let { readSharedStrings(it) } ?: emptyList()
        return readSheet(sheetXml, shared)
    }

    private fun unzip(bytes: ByteArray): Map<String, ByteArray> {
        val result = mutableMapOf<String, ByteArray>()
        ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                if (!entry.isDirectory) result[entry.name.removePrefix("/")] = zip.readBytes()
            }
        }
        if (result.isEmpty()) throw IOException("Keine gültige xlsx-Datei")
        return result
    }

    private fun parser(bytes: ByteArray): XmlPullParser = newParser().apply {
        setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true)
        setInput(ByteArrayInputStream(bytes), "UTF-8")
    }

    private fun XmlPullParser.attr(localName: String): String? {
        for (i in 0 until attributeCount) {
            if (getAttributeName(i) == localName) return getAttributeValue(i)
        }
        return null
    }

    /** Returns (sheet name, relationship id) in workbook order. */
    private fun readSheets(xml: ByteArray): List<Pair<String, String>> {
        val p = parser(xml)
        val sheets = mutableListOf<Pair<String, String>>()
        while (p.next() != XmlPullParser.END_DOCUMENT) {
            if (p.eventType == XmlPullParser.START_TAG && p.name == "sheet") {
                sheets += (p.attr("name") ?: "") to (p.attr("id") ?: "")
            }
        }
        return sheets
    }

    private fun readRelationships(xml: ByteArray): Map<String, String> {
        val p = parser(xml)
        val rels = mutableMapOf<String, String>()
        while (p.next() != XmlPullParser.END_DOCUMENT) {
            if (p.eventType == XmlPullParser.START_TAG && p.name == "Relationship") {
                val id = p.attr("Id")
                val target = p.attr("Target")
                if (id != null && target != null) rels[id] = target
            }
        }
        return rels
    }

    private fun readSharedStrings(xml: ByteArray): List<String> {
        val p = parser(xml)
        val strings = mutableListOf<String>()
        while (p.next() != XmlPullParser.END_DOCUMENT) {
            if (p.eventType == XmlPullParser.START_TAG && p.name == "si") {
                strings += readRichText(p, "si")
            }
        }
        return strings
    }

    /** Concatenates all <t> texts until the end of [endTag], skipping phonetic runs. */
    private fun readRichText(p: XmlPullParser, endTag: String): String {
        val sb = StringBuilder()
        var phoneticDepth = 0
        while (true) {
            when (p.next()) {
                XmlPullParser.START_TAG -> when (p.name) {
                    "rPh" -> phoneticDepth++
                    "t" -> if (phoneticDepth == 0) sb.append(p.nextText()) else p.nextText()
                }
                XmlPullParser.END_TAG -> {
                    if (p.name == "rPh") phoneticDepth--
                    if (p.name == endTag) return sb.toString()
                }
                XmlPullParser.END_DOCUMENT -> return sb.toString()
            }
        }
    }

    private fun readSheet(xml: ByteArray, shared: List<String>): List<List<String>> {
        val p = parser(xml)
        val rows = mutableListOf<List<String>>()
        var current: MutableMap<Int, String>? = null
        var nextColumn = 0
        while (p.next() != XmlPullParser.END_DOCUMENT) {
            when (p.eventType) {
                XmlPullParser.START_TAG -> when (p.name) {
                    "row" -> {
                        current = mutableMapOf()
                        nextColumn = 0
                    }
                    "c" -> {
                        val column = p.attr("r")?.let { columnIndex(it) } ?: nextColumn
                        nextColumn = column + 1
                        val text = readCell(p, p.attr("t"), shared)
                        if (text.isNotEmpty()) current?.put(column, text)
                    }
                }
                XmlPullParser.END_TAG -> if (p.name == "row") {
                    val cells = current ?: continue
                    val width = (cells.keys.maxOrNull() ?: -1) + 1
                    rows += List(width) { cells[it] ?: "" }
                    current = null
                }
            }
        }
        return rows
    }

    private fun readCell(p: XmlPullParser, type: String?, shared: List<String>): String {
        var value = ""
        while (true) {
            when (p.next()) {
                XmlPullParser.START_TAG -> when (p.name) {
                    "v" -> value = p.nextText()
                    "is" -> value = readRichText(p, "is")
                }
                XmlPullParser.END_TAG -> if (p.name == "c") break
                XmlPullParser.END_DOCUMENT -> break
            }
        }
        return when (type) {
            "s" -> value.trim().toIntOrNull()?.let { shared.getOrNull(it) } ?: ""
            "b" -> if (value == "1") "WAHR" else "FALSCH"
            else -> value
        }
    }

    /** "C12" -> 2 */
    private fun columnIndex(ref: String): Int {
        var index = 0
        for (ch in ref) {
            if (!ch.isLetter()) break
            index = index * 26 + (ch.uppercaseChar() - 'A' + 1)
        }
        return index - 1
    }
}
