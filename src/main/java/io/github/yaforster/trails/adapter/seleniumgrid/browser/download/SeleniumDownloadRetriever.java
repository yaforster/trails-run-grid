package io.github.yaforster.trails.adapter.seleniumgrid.browser.download;

import io.github.yaforster.trails.core.test.FileData;
import org.openqa.selenium.HasDownloads;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Bridges Selenium's directory-based download transfer to Trails' in-memory artifacts. Owns temporary local storage and
 * best-effort cleanup, while keeping one failed transfer from discarding other collected downloads.
 */
final class SeleniumDownloadRetriever {

    private static final Logger LOGGER = LoggerFactory.getLogger(SeleniumDownloadRetriever.class);
    private final HasDownloads downloads;

    SeleniumDownloadRetriever(HasDownloads downloads) {
        this.downloads = downloads;
    }

    Optional<FileData> retrieve(String fileName) {
        Path directory;
        try {
            directory = Files.createTempDirectory("trails-managed-download-");
        } catch (Exception exception) {
            LOGGER.warn("Could not create temporary storage for a Grid download; skipping the download.", exception);
            return Optional.empty();
        }
        try {
            return transferDownloadIfAvailable(fileName, directory);
        } finally {
            removeTemporaryDownload(directory);
        }
    }

    private Optional<FileData> transferDownloadIfAvailable(String fileName, Path directory) {
        try {
            downloads.downloadFile(fileName, directory);
            return readMatchingDownload(directory, fileName);
        } catch (Exception exception) {
            // One unavailable download must not hide other files collected from the session.
            LOGGER.warn("Could not transfer or read a Grid download; skipping the download.", exception);
            return Optional.empty();
        }
    }

    private Optional<FileData> readMatchingDownload(Path directory, String fileName) throws IOException {
        try (Stream<Path> paths = Files.walk(directory)) {
            return paths.filter(Files::isRegularFile)
                        .filter(path -> DownloadFileNamePolicy.matches(path, fileName))
                        .findFirst()
                        .map(this::readArtifact);
        }
    }

    private FileData readArtifact(Path path) {
        try {
            byte[] content = Files.readAllBytes(path);
            String fileName = DownloadFileNamePolicy.artifactName(path);
            return new FileData(content, fileName);
        } catch (IOException exception) {
            throw new IllegalStateException("Could not read the transferred browser download.", exception);
        }
    }

    private void removeTemporaryDownload(Path directory) {
        try (Stream<Path> paths = Files.walk(directory)) {
            // Children must be removed before their containing directories can be deleted.
            Comparator<Path> deepestPathsFirst = Comparator.comparingInt(Path::getNameCount)
                                                           .reversed();
            paths.sorted(deepestPathsFirst)
                 .forEach(this::deleteIfAccessible);
        } catch (IOException exception) {
            // Cleanup is best effort and must not discard an already collected artifact.
            LOGGER.warn("Could not inspect temporary Grid download storage for cleanup; files may remain.", exception);
        }
    }

    private void deleteIfAccessible(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException exception) {
            // Continue removing the other files if the operating system locks one file.
            LOGGER.warn("Could not delete a temporary Grid download entry; continuing cleanup.", exception);
        }
    }

}
