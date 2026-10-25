package de.vokabeltrainer.app.data

import org.junit.Assert.assertEquals
import org.junit.Test
import org.kxml2.io.KXmlParser
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class XlsxReaderTest {

    private val reader = XlsxReader { KXmlParser() }

    private fun xlsx(files: Map<String, String>): ByteArray {
        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { zip ->
            files.forEach { (name, content) ->
                zip.putNextEntry(ZipEntry(name))
                zip.write(content.toByteArray())
                zip.closeEntry()
            }
        }
        return out.toByteArray()
    }

    private val workbook = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
        <workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"
                  xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
          <sheets>
            <sheet name="Anleitung" sheetId="2" r:id="rId2"/>
            <sheet name="Vokabeln" sheetId="1" r:id="rId1"/>
          </sheets>
        </workbook>"""

    private val rels = """<?xml version="1.0" encoding="UTF-8"?>
        <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
          <Relationship Id="rId1" Type="worksheet" Target="worksheets/sheet1.xml"/>
          <Relationship Id="rId2" Type="worksheet" Target="/xl/worksheets/sheet2.xml"/>
        </Relationships>"""

    private val ns = "http://schemas.openxmlformats.org/spreadsheetml/2006/main"

    @Test
    fun readsInlineStringsFromNamedSheet() {
        val sheet = """<worksheet xmlns="$ns"><sheetData>
            <row r="1"><c r="A1" t="inlineStr"><is><t>Thema</t></is></c><c r="B1" t="inlineStr"><is><t>Französisch</t></is></c><c r="C1" t="inlineStr"><is><t>Deutsch</t></is></c></row>
            <row r="2"><c r="A2" t="inlineStr"><is><t>Politik</t></is></c><c r="B2" t="inlineStr"><is><t>l'ampleur (f)</t></is></c><c r="C2" t="inlineStr"><is><t>das Ausmaß</t></is></c><c r="E2" t="inlineStr"><is><t xml:space="preserve">Niemand &amp; keiner</t></is></c></row>
            <row r="4"><c r="A4" t="inlineStr"><is><t>So erweiterst du</t></is></c></row>
        </sheetData></worksheet>"""
        val other = """<worksheet xmlns="$ns"><sheetData><row r="1"><c r="A1" t="inlineStr"><is><t>falsch</t></is></c></row></sheetData></worksheet>"""
        val bytes = xlsx(
            mapOf(
                "xl/workbook.xml" to workbook,
                "xl/_rels/workbook.xml.rels" to rels,
                "xl/worksheets/sheet1.xml" to sheet,
                "xl/worksheets/sheet2.xml" to other,
            )
        )
        val rows = reader.readRows(bytes, "Vokabeln")
        assertEquals(listOf("Politik", "l'ampleur (f)", "das Ausmaß", "", "Niemand & keiner"), rows[1])

        val vocab = VocabParser.parse(Language.FRENCH, rows)
        assertEquals(1, vocab.size)
        assertEquals("Politik", vocab[0].topic)
        assertEquals("das Ausmaß", vocab[0].german)
        assertEquals("", vocab[0].example)
        assertEquals("Niemand & keiner", vocab[0].exampleGerman)
    }

    @Test
    fun readsSharedStringsWithRichTextAndNumbers() {
        val shared = """<sst xmlns="$ns">
            <si><t>Thema</t></si>
            <si><t>Englisch</t></si>
            <si><t>Deutsch</t></si>
            <si><r><t>Redewend</t></r><r><rPr><b/></rPr><t>ungen</t></r><rPh><t>x</t></rPh></si>
            <si><t>a mixed bag</t></si>
            <si><t>eine bunte Mischung</t></si>
        </sst>"""
        val sheet = """<worksheet xmlns="$ns"><sheetData>
            <row><c t="s"><v>0</v></c><c t="s"><v>1</v></c><c t="s"><v>2</v></c></row>
            <row><c t="s"><v>3</v></c><c t="s"><v>4</v></c><c t="s"><v>5</v></c><c><v>42</v></c></row>
        </sheetData></worksheet>"""
        val bytes = xlsx(
            mapOf(
                "xl/workbook.xml" to workbook.replace("Vokabeln", "Tabelle1"),
                "xl/_rels/workbook.xml.rels" to rels,
                "xl/sharedStrings.xml" to shared,
                "xl/worksheets/sheet1.xml" to sheet,
                "xl/worksheets/sheet2.xml" to sheet,
            )
        )
        val rows = reader.readRows(bytes, "Vokabeln")
        assertEquals(listOf("Redewendungen", "a mixed bag", "eine bunte Mischung", "42"), rows[1])
        val vocab = VocabParser.parse(Language.ENGLISH, rows)
        assertEquals(1, vocab.size)
        assertEquals("E|a mixed bag|eine bunte Mischung", vocab[0].id)
    }

    @Test
    fun stacksCapAtFive() {
        assertEquals(1, Progress(0).stack)
        assertEquals(2, Progress(1).stack)
        assertEquals(4, Progress(3).stack)
        assertEquals(5, Progress(4).stack)
        assertEquals(5, Progress(17).stack)
    }

    @Test
    fun convertsDriveLinks() {
        val id = "1US4zKXb47XQ1yqtle3yF2Hp2wWbPMZGX"
        val expected = "https://drive.google.com/uc?export=download&id=$id"
        assertEquals(expected, DriveLinks.downloadUrl("https://drive.google.com/file/d/$id/view?usp=sharing"))
        assertEquals(expected, DriveLinks.downloadUrl("https://drive.google.com/open?id=$id"))
        assertEquals(expected, DriveLinks.downloadUrl(" $id "))
        assertEquals(
            "https://docs.google.com/spreadsheets/d/$id/export?format=xlsx",
            DriveLinks.downloadUrl("https://docs.google.com/spreadsheets/d/$id/edit#gid=0"),
        )
        assertEquals(null, DriveLinks.downloadUrl(""))
    }
}
