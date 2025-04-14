package model;

import java.time.LocalDateTime;

/**
 * Represents a single file conversion record to be stored in the database.
 * Stores metadata such as file name, formats, status, and timestamp.
 */
public class ConversionHistory {
    private String fileName;         // Name of the original file
    private String sourceFormat;     // Original format (e.g., "png")
    private String targetFormat;     // Converted format (e.g., "jpg")
    private String status;           // Conversion result (e.g., "Success" or "Failed")
    private LocalDateTime timestamp; // Time of conversion

    /**
     * Constructs a ConversionHistory record with all required fields.
     */
    public ConversionHistory(String fileName, String sourceFormat, String targetFormat, String status, LocalDateTime timestamp) {
        this.fileName = fileName;
        this.sourceFormat = sourceFormat;
        this.targetFormat = targetFormat;
        this.status = status;
        this.timestamp = timestamp;
    }

    public String getFileName() {
        return fileName;
    }

    public String getSourceFormat() {
        return sourceFormat;
    }

    public String getTargetFormat() {
        return targetFormat;
    }

    public String getStatus() {
        return status;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    /**
     * Returns a human-readable string representation of the conversion record.
     */
    @Override
    public String toString() {
        return fileName + " | " + sourceFormat + " → " + targetFormat + " | " + status + " | " + timestamp;
    }
}