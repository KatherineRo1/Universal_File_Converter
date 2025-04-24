// VideoConverter.java
package converter.video;

import util.FileUtils;
import util.Logger;
import java.io.*;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * Handles video file conversion by directly invoking FFmpeg using ProcessBuilder.
 */
public class VideoConverter {

    private static final Set<String> SUPPORTED_FORMATS = Set.of("mp4", "avi", "mkv", "mov", "webm");

    /**
     * Converts a video file to a given output format using FFmpeg.
     * Runs process in background and reads output to avoid blocking.
     */
    public boolean convert(File inputFile, String outputFormat, File outputFile) {
        try {
            String inputExt = FileUtils.getExtension(inputFile);
            if (!SUPPORTED_FORMATS.contains(inputExt) || !SUPPORTED_FORMATS.contains(outputFormat)) {
                Logger.logError("Unsupported format: " + inputExt + " or " + outputFormat);
                return false;
            }

            List<String> command = Arrays.asList(
                    "ffmpeg",
                    "-y",
                    "-i", inputFile.getAbsolutePath(),
                    outputFile.getAbsolutePath()
            );

            ProcessBuilder builder = new ProcessBuilder(command);
            builder.redirectErrorStream(true);
            Process process = builder.start();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println("[FFmpeg] " + line);
                }
            }

            int exitCode = process.waitFor();
            return exitCode == 0;

        } catch (IOException | InterruptedException e) {
            Logger.logError("Video conversion failed: " + e.getMessage());
            return false;
        }
    }

    /**
     * Returns supported video formats.
     */
    public List<String> getSupportedFormats() {
        return SUPPORTED_FORMATS.stream().sorted().toList();
    }
}
