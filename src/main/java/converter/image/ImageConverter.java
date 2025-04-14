package converter.image;

import javax.imageio.*;
import javax.imageio.stream.ImageOutputStream;
import java.awt.image.BufferedImage;
import java.awt.Graphics2D;
import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.logging.*;

/**
 * Utility class for converting images between different formats.
 * Supports JPG (with compression), PNG, and fallback formats.
 * Includes logging for error tracking.
 */
public class ImageConverter {

    private static final Logger logger = Logger.getLogger(ImageConverter.class.getName());

    static {
        try {
            Handler fileHandler = new FileHandler("logs/log.txt", 1024 * 1024, 1, true);
            fileHandler.setFormatter(new SimpleFormatter());
            logger.addHandler(fileHandler);
            logger.setLevel(Level.ALL);
        } catch (IOException e) {
            System.err.println("Logger initialization failed: " + e.getMessage());
        }
    }

    /**
     * Converts the input image to the specified format with optional compression.
     */
    public void convert(File inputFile, String outputFormat, File outputFile, float quality) throws IOException {
        BufferedImage inputImage = ImageIO.read(inputFile);
        if (inputImage == null) {
            String msg = "Could not read image: " + inputFile.getAbsolutePath();
            logger.severe(msg);
            throw new IOException(msg);
        }

        if ("jpg".equalsIgnoreCase(outputFormat) && inputImage.getColorModel().hasAlpha()) {
            inputImage = addWhiteBackgroundIfTransparent(inputImage);
        }

        try {
            if ("jpg".equalsIgnoreCase(outputFormat)) {
                writeJpg(inputImage, outputFile, quality);
            } else if ("png".equalsIgnoreCase(outputFormat)) {
                writePng(inputImage, outputFile);
            } else {
                if (!ImageIO.write(inputImage, outputFormat, outputFile)) {
                    String msg = "Unsupported output format: " + outputFormat;
                    logger.severe(msg);
                    throw new IOException(msg);
                }
            }
            logger.info("Image successfully converted to: " + outputFormat + " -> " + outputFile.getAbsolutePath());
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Image conversion failed", e);
            throw e;
        }
    }

    /**
     * Writes image to JPG with compression settings.
     */
    private void writeJpg(BufferedImage image, File outputFile, float quality) throws IOException {
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpg");
        if (!writers.hasNext()) {
            throw new IOException("No JPG writer found");
        }

        ImageWriter writer = writers.next();
        ImageWriteParam param = writer.getDefaultWriteParam();
        if (param.canWriteCompressed()) {
            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            param.setCompressionQuality(quality);
        }

        try (ImageOutputStream ios = ImageIO.createImageOutputStream(outputFile)) {
            writer.setOutput(ios);
            writer.write(null, new IIOImage(image, null, null), param);
        } finally {
            writer.dispose();
        }
    }

    /**
     * Writes image to PNG format.
     */
    private void writePng(BufferedImage image, File outputFile) throws IOException {
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("png");
        if (!writers.hasNext()) {
            throw new IOException("No PNG writer found");
        }

        ImageWriter writer = writers.next();
        try (ImageOutputStream ios = ImageIO.createImageOutputStream(outputFile)) {
            writer.setOutput(ios);
            writer.write(null, new IIOImage(image, null, null), writer.getDefaultWriteParam());
        } finally {
            writer.dispose();
        }
    }

    /**
     * Adds white background for transparent images saved as JPG.
     */
    private BufferedImage addWhiteBackgroundIfTransparent(BufferedImage inputImage) {
        BufferedImage newImage = new BufferedImage(
                inputImage.getWidth(), inputImage.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = newImage.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, inputImage.getWidth(), inputImage.getHeight());
        g.drawImage(inputImage, 0, 0, null);
        g.dispose();
        return newImage;
    }

    /**
     * Converts HEIC images to the specified format using FFmpeg.
     */
    public void convertHeicWithFFmpeg(File inputFile, String outputFormat, File outputFile)
            throws IOException, InterruptedException {
        ProcessBuilder processBuilder = new ProcessBuilder(
                "ffmpeg",
                "-i", inputFile.getAbsolutePath(),
                outputFile.getAbsolutePath()
        );

        Process process = processBuilder.start();
        int exitCode = process.waitFor();

        if (exitCode != 0) {
            String msg = "FFmpeg failed with exit code: " + exitCode;
            logger.severe(msg);
            throw new IOException(msg);
        }

        logger.info("HEIC image successfully converted using FFmpeg to: " + outputFile.getAbsolutePath());
    }
}
