package model;

import java.time.LocalDateTime;

/**
 * Represents a single file conversion record to be stored in the database.
 * Stores metadata such as file name, formats, status, and timestamp.
 *
 * @param fileName     Name of the original file
 * @param sourceFormat Original format (e.g., "png")
 * @param targetFormat Converted format (e.g., "jpg")
 * @param status       Conversion result (e.g., "Success" or "Failed")
 * @param timestamp    Time of conversion
 */
public record ConversionHistory(String fileName, String sourceFormat, String targetFormat, String status,
                                LocalDateTime timestamp) {
    /**
     * Constructs a ConversionHistory record with all required fields.
     */
    public ConversionHistory {
    }

    /**
     * Returns a human-readable string representation of the conversion record.
     */
    @Override
    public String toString() {
        return fileName + " | " + sourceFormat + " → " + targetFormat + " | " + status + " | " + timestamp;
    }
}