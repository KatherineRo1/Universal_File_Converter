package util;

import java.io.File;

public class FileUtils {

    /**
     * Returns the file extension.
     */
    public static String getExtension(File file) {
        String name = file.getName();
        int lastIndex = name.lastIndexOf('.');
        return (lastIndex > 0) ? name.substring(lastIndex + 1) : "";
    }

    /**
     * Generates a new output path based on the input file and desired new format.
     */
    public static File generateOutputPath(File inputFile, String newFormat) {
        String name = inputFile.getName();
        int dotIndex = name.lastIndexOf('.');
        String baseName = (dotIndex != -1) ? name.substring(0, dotIndex) : name;
        String newName = baseName + "_converted." + newFormat;
        return new File(inputFile.getParentFile(), newName);
    }
}
