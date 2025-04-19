package converter.text;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Converts CSV files to XLSX format without using external libraries.
 * The XLSX format is actually a ZIP archive containing XML files,
 * so this class manually creates the required ZIP structure and content.
 */
public class CsvConverter {

    // Delimiter used to split CSV fields (default is comma)
    private String delimiter = ",";

    /**
     * Sets the delimiter used when parsing CSV fields.
     */
    public void setDelimiter(String delimiter) {
        this.delimiter = delimiter;
    }

    /**
     * Converts a CSV file into a simplified XLSX file.
     * This method parses the CSV content, organizes it into rows,
     * then constructs the XML parts required for a valid Excel file,
     * and writes them into a ZIP archive with appropriate structure.
     */
    public void convertCsvToXlsx(File inputFile, File outputFile) throws IOException {
        List<List<String>> rows = new ArrayList<>(); // Each inner list represents a row of cells
        Set<String> sharedStringsSet = new LinkedHashSet<>(); // Collect unique strings for sharedStrings.xml

        // Read the CSV file line by line using UTF-8 encoding
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(inputFile), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] cells = line.split(delimiter, -1); // Keep empty strings
                List<String> cleanCells = new ArrayList<>();
                for (String cell : cells) {
                    cleanCells.add(cell.trim()); // Remove leading/trailing spaces
                }
                rows.add(cleanCells);
                sharedStringsSet.addAll(cleanCells); // Add all values to shared strings set
            }
        }

        List<String> sharedStrings = new ArrayList<>(sharedStringsSet); // Preserve insertion order

        // Start writing the XLSX archive (ZIP with XML entries)
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
     * Automatically detects whether a CSV file uses comma or semicolon
     * as a delimiter, based on the first line.
     */
    public static String detectDelimiter(File file) {
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line = reader.readLine();
            if (line == null) return ","; // default if empty
            int commas = line.split(",").length;
            int semicolons = line.split(";").length;
            return (semicolons > commas) ? ";" : ",";
        } catch (IOException e) {
            return ","; // Fallback to comma on error
        }
    }

    /**
     * Helper method to write a string as a ZIP entry to the XLSX archive.
     */
    private void addZipEntry(ZipOutputStream zos, String path, String content) throws IOException {
        zos.putNextEntry(new ZipEntry(path));
        zos.write(content.getBytes(StandardCharsets.UTF_8));
        zos.closeEntry();
    }

    /**
     * Returns the [Content_Types].xml part, required for defining MIME types in the XLSX archive.
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
     * Defines top-level relationships in the archive (_rels/.rels).
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
     * Defines internal workbook relationships (sheet, style, strings).
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
     * Declares the workbook and references the first worksheet.
     */
    private String getWorkbook() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                "<workbook xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" " +
                "xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\">" +
                "<sheets><sheet name=\"Sheet1\" sheetId=\"1\" r:id=\"rId1\"/></sheets></workbook>";
    }

    /**
     * Creates the main sheet content with all rows and cells.
     * Each cell value is referenced by its index in sharedStrings.xml.
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
     * Converts a column index and row number to Excel cell reference
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
     * Generates the sharedStrings.xml content from unique string list.
     */
    private String getSharedStrings(List<String> strings) {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        sb.append("<sst xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" ")
                .append("count=\"").append(strings.size()).append("\" uniqueCount=\"").append(strings.size()).append("\">");
        for (String str : strings) {
            sb.append("<si><t xml:space=\"preserve\">").append(escapeXml(str)).append("</t></si>");
        }
        sb.append("</sst>");
        return sb.toString();
    }

    /**
     * Escapes reserved XML characters in a cell value.
     */
    private String escapeXml(String text) {
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }

    /**
     * Provides a minimal valid styles.xml to satisfy XLSX schema.
     */
    private String getStyles() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                "<styleSheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\"/>";
    }

    /**
     * Describes the document's metadata, such as title and creator.
     */
    private String getCore() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                "<cp:coreProperties xmlns:cp=\"http://schemas.openxmlformats.org/package/2006/metadata/core-properties\" " +
                "xmlns:dc=\"http://purl.org/dc/elements/1.1/\" xmlns:dcterms=\"http://purl.org/dc/terms/\" " +
                "xmlns:dcmitype=\"http://purl.org/dc/dcmitype/\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">" +
                "<dc:title>Converted XLSX</dc:title>" +
                "<dc:creator>Universal File Converter</dc:creator>" +
                "<cp:lastModifiedBy>Converter</cp:lastModifiedBy>" +
                "<dcterms:created xsi:type=\"dcterms:W3CDTF\">2025-04-15T00:00:00Z</dcterms:created>" +
                "</cp:coreProperties>";
    }

    /**
     * Supplies application-level metadata like the generator application name.
     */
    private String getApp() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                "<Properties xmlns=\"http://schemas.openxmlformats.org/officeDocument/2006/extended-properties\" " +
                "xmlns:vt=\"http://schemas.openxmlformats.org/officeDocument/2006/docPropsVTypes\">" +
                "<Application>Java Converter</Application>" +
                "</Properties>";
    }
}
