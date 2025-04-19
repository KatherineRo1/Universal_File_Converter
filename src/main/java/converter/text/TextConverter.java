package converter.text;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Converts plain text (TXT) files into XLSX spreadsheet format
 * without using any third-party libraries like Apache POI.
 * The resulting XLSX file is created manually by assembling
 * the necessary ZIP entries and XML components of the Open XML format.
 */
public class TextConverter {

    // Delimiter used to split each line into columns (default is comma)
    private String delimiter = ",";

    /**
     * Allows external classes (e.g., UI controllers) to set the delimiter.
     */
    public void setDelimiter(String delimiter) {
        this.delimiter = delimiter;
    }

    /**
     * Converts a TXT file to an Excel XLSX file.
     * This method manually creates an XLSX file by writing appropriate
     * XML parts and packing them into a ZIP container.
     */
    public void convertTxtToExcel(File inputFile, File outputFile) throws IOException {
        List<List<String>> rows = new ArrayList<>();
        Set<String> sharedStringsSet = new LinkedHashSet<>();

        // Read and parse the TXT file line by line
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(inputFile), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                // Split each line using the selected delimiter
                String[] rawCells = line.split(delimiter, -1);
                List<String> cleanedCells = new ArrayList<>();

                // Remove surrounding spaces from each cell
                for (String cell : rawCells) {
                    cleanedCells.add(cell.trim());
                }

                rows.add(cleanedCells);
                sharedStringsSet.addAll(cleanedCells); // Used for sharedStrings.xml
            }
        }

        // Preserve the insertion order of unique string values
        List<String> sharedStrings = new ArrayList<>(sharedStringsSet);

        // Start creating XLSX ZIP structure
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(outputFile))) {
            addZipEntry(zos, "[Content_Types].xml", getContentTypes());
            addZipEntry(zos, "_rels/.rels", getRels());
            addZipEntry(zos, "docProps/core.xml", getCore());
            addZipEntry(zos, "docProps/app.xml", getApp());
            addZipEntry(zos, "xl/workbook.xml", getWorkbook());
            addZipEntry(zos, "xl/_rels/workbook.xml.rels", getWorkbookRels());
            addZipEntry(zos, "xl/worksheets/sheet1.xml", getSheet(rows, sharedStrings));
            addZipEntry(zos, "xl/sharedStrings.xml", getSharedStrings(sharedStrings));
            addZipEntry(zos, "xl/styles.xml", getStyles());
        }
    }

    /**
     * Utility method to add a file entry inside the XLSX ZIP structure.
     */
    private void addZipEntry(ZipOutputStream zos, String path, String content) throws IOException {
        zos.putNextEntry(new ZipEntry(path));
        zos.write(content.getBytes(StandardCharsets.UTF_8));
        zos.closeEntry();
    }

    /**
     * Generates the [Content_Types].xml part, which tells Excel
     * the types of all files included in the XLSX package.
     */
    private String getContentTypes() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                "<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">" +
                "<Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/>" +
                "<Default Extension=\"xml\" ContentType=\"application/xml\"/>" +
                "<Override PartName=\"/xl/workbook.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/>" +
                "<Override PartName=\"/xl/worksheets/sheet1.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>" +
                "<Override PartName=\"/xl/sharedStrings.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sharedStrings+xml\"/>" +
                "<Override PartName=\"/xl/styles.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml\"/>" +
                "<Override PartName=\"/docProps/core.xml\" ContentType=\"application/vnd.openxmlformats-package.core-properties+xml\"/>" +
                "<Override PartName=\"/docProps/app.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.extended-properties+xml\"/>" +
                "</Types>";
    }

    /**
     * Creates the global relationship file (_rels/.rels) that maps
     * top-level document parts such as workbook and document properties.
     */
    private String getRels() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">" +
                "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"xl/workbook.xml\"/>" +
                "<Relationship Id=\"rId2\" Type=\"http://schemas.openxmlformats.org/package/2006/relationships/metadata/core-properties\" Target=\"docProps/core.xml\"/>" +
                "<Relationship Id=\"rId3\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/extended-properties\" Target=\"docProps/app.xml\"/>" +
                "</Relationships>";
    }

    /**
     * Defines internal relationships for the workbook part (sheet, styles, strings).
     */
    private String getWorkbookRels() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">" +
                "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet1.xml\"/>" +
                "<Relationship Id=\"rId2\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles\" Target=\"styles.xml\"/>" +
                "<Relationship Id=\"rId3\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/sharedStrings\" Target=\"sharedStrings.xml\"/>" +
                "</Relationships>";
    }

    /**
     * Returns workbook.xml which references sheet1.xml and links its relationship ID.
     */
    private String getWorkbook() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                "<workbook xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\">" +
                "<sheets><sheet name=\"Sheet1\" sheetId=\"1\" r:id=\"rId1\"/></sheets></workbook>";
    }

    /**
     * Generates the sheet1.xml file which contains all cell data
     * represented by references to shared string indexes.
     */
    private String getSheet(List<List<String>> rows, List<String> sharedStrings) {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        sb.append("<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\">");
        sb.append("<sheetData>");

        for (int r = 0; r < rows.size(); r++) {
            sb.append("<row r=\"").append(r + 1).append("\">");
            List<String> row = rows.get(r);
            for (int c = 0; c < row.size(); c++) {
                String value = row.get(c);
                int sstIndex = sharedStrings.indexOf(value);
                String cellRef = getCellRef(c, r + 1);
                sb.append("<c r=\"").append(cellRef).append("\" t=\"s\"><v>")
                        .append(sstIndex).append("</v></c>");
            }
            sb.append("</row>");
        }

        sb.append("</sheetData></worksheet>");
        return sb.toString();
    }

    /**
     * Generates the Excel-style reference (e.g., "A1", "B2") for a cell.
     */
    private String getCellRef(int col, int row) {
        StringBuilder colRef = new StringBuilder();
        int c = col;
        do {
            colRef.insert(0, (char) ('A' + (c % 26)));
            c = (c / 26) - 1;
        } while (c >= 0);
        return colRef + String.valueOf(row);
    }

    /**
     * Builds the sharedStrings.xml, which is a required part of Excel files
     * and stores all unique strings used in the workbook.
     */
    private String getSharedStrings(List<String> strings) {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        sb.append("<sst xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" count=\"")
                .append(strings.size()).append("\" uniqueCount=\"").append(strings.size()).append("\">");
        for (String str : strings) {
            sb.append("<si><t xml:space=\"preserve\">").append(escapeXml(str)).append("</t></si>");
        }
        sb.append("</sst>");
        return sb.toString();
    }

    /**
     * Escapes special characters in strings so they are valid in XML.
     */
    private String escapeXml(String text) {
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }

    /**
     * Provides an empty styles.xml which is required for valid Excel files
     * even if no styling is applied.
     */
    private String getStyles() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                "<styleSheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\"/>";
    }

    /**
     * Supplies metadata for core properties like title, creator, and date.
     */
    private String getCore() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                "<cp:coreProperties xmlns:cp=\"http://schemas.openxmlformats.org/package/2006/metadata/core-properties\" " +
                "xmlns:dc=\"http://purl.org/dc/elements/1.1/\" " +
                "xmlns:dcterms=\"http://purl.org/dc/terms/\" " +
                "xmlns:dcmitype=\"http://purl.org/dc/dcmitype/\" " +
                "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">" +
                "<dc:title>Converted XLSX</dc:title>" +
                "<dc:creator>Universal File Converter</dc:creator>" +
                "<cp:lastModifiedBy>Converter</cp:lastModifiedBy>" +
                "<dcterms:created xsi:type=\"dcterms:W3CDTF\">2025-04-15T00:00:00Z</dcterms:created>" +
                "</cp:coreProperties>";
    }

    /**
     * Provides additional metadata used by Microsoft Office apps,
     * such as the name of the application that created the file.
     */
    private String getApp() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                "<Properties xmlns=\"http://schemas.openxmlformats.org/officeDocument/2006/extended-properties\" " +
                "xmlns:vt=\"http://schemas.openxmlformats.org/officeDocument/2006/docPropsVTypes\">" +
                "<Application>Java Converter</Application>" +
                "</Properties>";
    }
}
