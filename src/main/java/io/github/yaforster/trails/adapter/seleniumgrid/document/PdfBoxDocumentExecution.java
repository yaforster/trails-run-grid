package io.github.yaforster.trails.adapter.seleniumgrid.document;

import io.github.yaforster.trails.core.test.FileData;
import io.github.yaforster.trails.core.test.action.document.DocumentExecution;
import io.github.yaforster.trails.core.test.action.document.DocumentTextExtractionException;
import io.github.yaforster.trails.core.test.action.document.ExtractedDocumentContent;
import io.github.yaforster.trails.core.test.action.document.PdfVisualProof;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Locale;
import java.util.Optional;

/**
 * Supplies downloaded-document inspection that Selenium does not provide. Combines PDFBox text extraction and page
 * rendering into Trails document results, converting PDF coordinates into image pixels for scrollable visual proofs.
 */
public class PdfBoxDocumentExecution implements DocumentExecution {

    private static final byte[] PDF_HEADER = "%PDF".getBytes(StandardCharsets.US_ASCII);
    private static final float PROOF_DPI = 150F;
    private static final float PDF_POINTS_PER_INCH = 72F;

    /** Creates the stateless PDF document executor. */
    public PdfBoxDocumentExecution() {}

    @Override
    public boolean isPdf(FileData file) {
        String fileName = file.fileName();
        if (fileName != null && fileName.toLowerCase(Locale.ROOT)
                                        .endsWith(".pdf")) {
            return true;
        }

        byte[] content = file.content();
        return content != null && hasPdfHeader(content);
    }

    @Override
    public ExtractedDocumentContent extract(FileData file) throws DocumentTextExtractionException {
        requireDocumentContent(file);
        if (!isPdf(file)) {
            byte[] content = file.content();
            String text = new String(content, StandardCharsets.UTF_8);
            String fileName = file.fileName();
            return new ExtractedDocumentContent(fileName, text, "Plain text");
        }
        return extractPdfText(file);
    }

    private void requireDocumentContent(FileData file) throws DocumentTextExtractionException {
        if (file.content() == null || file.content().length == 0) {
            throw new DocumentTextExtractionException("The downloaded document is empty.");
        }
    }

    private ExtractedDocumentContent extractPdfText(FileData file) throws DocumentTextExtractionException {
        byte[] content = file.content();
        try (PDDocument document = PDDocument.load(content)) {
            ensureUnencrypted(document);
            PDFTextStripper textStripper = new PDFTextStripper();
            String text = textStripper.getText(document);
            String fileName = file.fileName();
            return new ExtractedDocumentContent(fileName, text, "PDF");
        } catch (IOException exception) {
            throw new DocumentTextExtractionException("The PDF document could not be parsed.", exception);
        }
    }

    @Override
    public Optional<PdfVisualProof> renderPdfProof(FileData file, String expectedText, boolean caseSensitive)
            throws DocumentTextExtractionException {
        byte[] content = file.content();
        try (PDDocument document = PDDocument.load(content)) {
            ensureUnencrypted(document);
            PdfTextMatchLocator locator = new PdfTextMatchLocator(expectedText, caseSensitive);
            Optional<TextMatch> match = locator.findFirstMatch(document);
            if (match.isEmpty()) {
                return Optional.empty();
            }
            TextMatch textMatch = match.get();
            String fileName = file.fileName();
            PdfVisualProof proof = renderMatchingPage(document, fileName, expectedText, textMatch);
            return Optional.of(proof);
        } catch (IOException exception) {
            throw new DocumentTextExtractionException("The PDF document could not be parsed.", exception);
        }
    }

    private PdfVisualProof renderMatchingPage(PDDocument document, String fileName, String expectedText,
            TextMatch match) throws IOException {
        PDFRenderer renderer = new PDFRenderer(document);
        int pageIndex = match.pageIndex();
        BufferedImage image = renderer.renderImageWithDPI(pageIndex, PROOF_DPI, ImageType.RGB);
        float topEdgePoints = match.topEdgePoints();
        int scrollTop = imagePixelsFromPdfPoints(topEdgePoints);
        int pageNumber = pageIndex + 1;
        int imageWidth = image.getWidth();
        int imageHeight = image.getHeight();
        String encodedImage = encodePng(image);
        return new PdfVisualProof(fileName, expectedText, pageNumber, scrollTop, imageWidth, imageHeight, encodedImage);
    }

    private int imagePixelsFromPdfPoints(float points) {
        // PDF positions use 72 points per inch; the rendered image uses 150 pixels per
        // inch. Convert between them to scroll to the same place in the page image.
        return Math.round(points * PROOF_DPI / PDF_POINTS_PER_INCH);
    }

    private void ensureUnencrypted(PDDocument document) throws DocumentTextExtractionException {
        if (document.isEncrypted()) {
            throw new DocumentTextExtractionException("Encrypted PDF documents are not supported.");
        }
    }

    private boolean hasPdfHeader(byte[] content) {
        if (content.length < PDF_HEADER.length) {
            return false;
        }
        for (int index = 0; index < PDF_HEADER.length; index++) {
            if (content[index] != PDF_HEADER[index]) {
                return false;
            }
        }
        return true;
    }

    private String encodePng(BufferedImage image) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(image, "png", output);
        byte[] pngBytes = output.toByteArray();
        Base64.Encoder encoder = Base64.getEncoder();
        return encoder.encodeToString(pngBytes);
    }

}
