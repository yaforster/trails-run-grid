package io.github.yaforster.trails.adapter.seleniumgrid.browser.download;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

import io.github.yaforster.trails.core.test.FileData;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.openqa.selenium.HasDownloads;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

class SeleniumDownloadRetrieverTest {

    @Test
    @ExtendWith(OutputCaptureExtension.class)
    void shouldLogTransferFailure_whenReturningNoArtifact(CapturedOutput output) throws Exception {
        HasDownloads downloads = mock();
        IOException transferFailure = new IOException("Grid transfer interrupted");
        doThrow(transferFailure).when(downloads)
                                .downloadFile(eq("invoice.pdf"), any(Path.class));
        SeleniumDownloadRetriever retriever = new SeleniumDownloadRetriever(downloads);

        Optional<FileData> artifact = retriever.retrieve("invoice.pdf");

        assertThat(artifact).isEmpty();
        assertThat(output.getOut()).contains("WARN",
                "Could not transfer or read a Grid download; skipping the download.",
                "java.io.IOException: Grid transfer interrupted");
    }

    @Test
    void shouldReadFinalizedDownload_whenGridReportsTemporaryName() throws Exception {
        HasDownloads downloads = mock();
        doAnswer(invocation -> {
            Path directory = invocation.getArgument(1);
            Files.write(directory.resolve("invoice.pdf"), new byte[]{1, 2, 3});
            return null;
        }).when(downloads)
          .downloadFile(eq("invoice.pdf.part"), any(Path.class));

        Optional<FileData> result = new SeleniumDownloadRetriever(downloads).retrieve("invoice.pdf.part");

        assertThat(result).hasValueSatisfying(file -> {
            assertThat(file.fileName()).isEqualTo("invoice.pdf");
            assertThat(file.content()).containsExactly(1, 2, 3);
        });
    }

    @Test
    void shouldRemoveTemporaryDirectory_whenTransferFailsAfterWritingAFile() throws Exception {
        HasDownloads downloads = mock();
        AtomicReference<Path> transferDirectory = new AtomicReference<>();
        doAnswer(invocation -> {
            Path directory = invocation.getArgument(1);
            transferDirectory.set(directory);
            Files.writeString(Files.createDirectory(directory.resolve("nested"))
                                   .resolve("partial"),
                    "partial");
            throw new IOException("Connection interrupted");
        }).when(downloads)
          .downloadFile(eq("invoice.pdf"), any(Path.class));

        new SeleniumDownloadRetriever(downloads).retrieve("invoice.pdf");

        assertThat(transferDirectory.get()).doesNotExist();
    }
}
