package converter.text;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * Utility class for converting images (PNG or JPG) into PDF documents.
 * Supports both single image conversion and multipage PDF generation.
 * Includes optional compression and image rotation.
 */
public class PngPdfConverter {

    /**
     * Converts a single PNG image into a one-page PDF file.
     * The image is placed directly on a PDF page with matching dimensions.
     */
    public static void convert(File inputPng, File outputPdf) throws Exception {
        BufferedImage image = ImageIO.read(inputPng);
        if (image == null) {
            throw new IllegalArgumentException("Invalid or unreadable image file.");
        }

        try (PDDocument document = new PDDocument()) {
            // Set PDF page size to match image dimensions
            PDRectangle pageSize = new PDRectangle(image.getWidth(), image.getHeight());
            PDPage page = new PDPage(pageSize);
            document.addPage(page);

            // Convert image to byte array and wrap as PDFBox image object
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "png", baos);
            PDImageXObject pdImage = PDImageXObject.createFromByteArray(document, baos.toByteArray(), inputPng.getName());

            // Draw image on the page using PDF content stream
            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                contentStream.drawImage(pdImage, 0, 0, image.getWidth(), image.getHeight());
            }

            document.save(outputPdf);
        }
    }

    /**
     * Converts multiple PNG/JPG images into a single PDF document.
     * Each image becomes one page. Supports compression and rotation.
     */
    public static void convertMultipleImagesToPdf(List<File> imageFiles, Map<File, Integer> rotationMap, File outputPdf, boolean compress) throws Exception {
        try (PDDocument document = new PDDocument()) {
            for (File imageFile : imageFiles) {
                BufferedImage image = ImageIO.read(imageFile);
                if (image == null) continue;

                // Apply image compression by scaling down large images
                if (compress) {
                    int maxWidth = 1024;
                    if (image.getWidth() > maxWidth) {
                        double ratio = (double) maxWidth / image.getWidth();
                        int newHeight = (int) (image.getHeight() * ratio);

                        Image scaled = image.getScaledInstance(maxWidth, newHeight, Image.SCALE_SMOOTH);
                        BufferedImage resized = new BufferedImage(maxWidth, newHeight, BufferedImage.TYPE_INT_RGB);
                        Graphics2D g2d = resized.createGraphics();
                        g2d.drawImage(scaled, 0, 0, null);
                        g2d.dispose();
                        image = resized;
                    }
                }

                // Apply rotation (90°, 180°, or 270°) if needed
                int rotation = rotationMap.getOrDefault(imageFile, 0);
                if (rotation != 0) {
                    image = rotateImage(image, rotation);
                }

                // Create a PDF page matching the image size
                PDRectangle pageSize = new PDRectangle(image.getWidth(), image.getHeight());
                PDPage page = new PDPage(pageSize);
                document.addPage(page);

                // Convert image to byte stream and embed into the page
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ImageIO.write(image, "png", baos);
                PDImageXObject pdImage = PDImageXObject.createFromByteArray(document, baos.toByteArray(), imageFile.getName());

                try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                    contentStream.drawImage(pdImage, 0, 0, image.getWidth(), image.getHeight());
                }
            }
            document.save(outputPdf);
        }
    }

    /**
     * Rotates a BufferedImage by a given angle (degrees).
     * Handles correct width/height after rotation and centers the image.
     */
    private static BufferedImage rotateImage(BufferedImage originalImage, int degrees) {
        int w = originalImage.getWidth();
        int h = originalImage.getHeight();
        int newW = (degrees == 90 || degrees == 270) ? h : w;
        int newH = (degrees == 90 || degrees == 270) ? w : h;

        BufferedImage rotated = new BufferedImage(newW, newH, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = rotated.createGraphics();
        g2d.translate(newW / 2.0, newH / 2.0);  // Move origin to center
        g2d.rotate(Math.toRadians(degrees));   // Rotate around center
        g2d.translate(-w / 2.0, -h / 2.0);      // Move back origin for image
        g2d.drawImage(originalImage, 0, 0, null);
        g2d.dispose();

        return rotated;
    }
}
