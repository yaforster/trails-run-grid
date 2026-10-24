package io.github.yaforster.trails.adapter.seleniumgrid.document;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.yaforster.trails.core.test.FileData;
import io.github.yaforster.trails.core.test.action.document.DocumentTextExtractionException;
import io.github.yaforster.trails.core.test.action.document.PdfVisualProof;
import java.io.ByteArrayInputStream;
import java.util.Base64;
import javax.imageio.ImageIO;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.junit.jupiter.api.Test;

class PdfBoxDocumentExecutionTest {

    @Test
    void shouldRecognizePdfByHeader_whenFilenameIsMissing() {
        FileData file = new FileData("%PDF-document".getBytes(StandardCharsets.US_ASCII), null);

        assertThat(new PdfBoxDocumentExecution().isPdf(file)).isTrue();
    }

    @Test
    void shouldExtractTextFromAPdfWhoseFilenameDoesNotHaveAnExtension() throws Exception {
        PdfBoxDocumentExecution documents = new PdfBoxDocumentExecution();

        String text = documents.extract(new FileData(pdf("Invoice total 42"), "download"))
                               .text();

        assertThat(text.trim()).isEqualTo("Invoice total 42");
    }

    @Test
    void shouldRenderFirstMatchingPage_whenTextMatchesIgnoringWhitespaceAndCase() throws Exception {
        FileData file = new FileData(pdf("Cover", "Invoice total 42", "Invoice total 42"), "invoice.pdf");

        PdfVisualProof proof = new PdfBoxDocumentExecution().renderPdfProof(file, "INVOICEtotal42", false)
                                                            .orElseThrow();

        assertThat(proof.pageNumber()).isEqualTo(2);
    }

    @Test
    void shouldReturnPngAtConfiguredResolution_whenRenderingProof() throws Exception {
        FileData file = new FileData(pdf("Invoice total 42"), "invoice.pdf");
        PdfVisualProof proof = new PdfBoxDocumentExecution().renderPdfProof(file, "total", true)
                                                            .orElseThrow();

        assertThat(ImageIO.read(new ByteArrayInputStream(Base64.getDecoder()
                                                               .decode(proof.pageImageBase64())))).satisfies(image -> {
                                                                   assertThat(image.getWidth()).isEqualTo(1275);
                                                                   assertThat(image.getHeight()).isEqualTo(1649);
                                                               });
    }

    @Test
    void shouldReturnNoProof_whenCaseSensitiveTextDoesNotMatch() throws Exception {
        FileData file = new FileData(pdf("Invoice total 42"), "invoice.pdf");

        assertThat(new PdfBoxDocumentExecution().renderPdfProof(file, "INVOICE", true)).isEmpty();
    }

    @Test
    void shouldRejectMalformedPdf_whenExtractingText() {
        FileData file = new FileData("%PDF-invalid".getBytes(StandardCharsets.US_ASCII), "invoice.pdf");

        assertThatThrownBy(
                () -> new PdfBoxDocumentExecution().extract(file)).isInstanceOf(DocumentTextExtractionException.class)
                                                                  .hasMessage("The PDF document could not be parsed.");
    }

    private byte[] pdf(String... pageTexts) throws Exception {
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            for (String text : pageTexts) {
                PDPage page = new PDPage();
                document.addPage(page);
                try (PDPageContentStream stream = new PDPageContentStream(document, page)) {
                    stream.beginText();
                    stream.setFont(PDType1Font.HELVETICA, 12);
                    stream.newLineAtOffset(72, 720);
                    stream.showText(text);
                    stream.endText();
                }
            }
            document.save(output);
            return output.toByteArray();
        }
    }

}
