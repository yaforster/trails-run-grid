package io.github.yaforster.trails.adapter.seleniumgrid.browser.download;

import org.openqa.selenium.HasDownloads;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * A Grid listing alone does not establish that a download has finished. Tracks unchanged metadata across three nonempty
 * polls as a readiness heuristic before transfer; stable metadata is not proof of file completeness.
 */
final class DownloadStabilityTracker {

    private static final int REQUIRED_STABLE_POLLS = 3;
    private List<DownloadSnapshot> previousSnapshots = Collections.emptyList();
    private int consecutiveStablePolls;

    Optional<List<DownloadSnapshot>> observe(List<HasDownloads.DownloadedFile> files) {
        Comparator<DownloadSnapshot> byFileName = Comparator.comparing(DownloadSnapshot::name);
        List<DownloadSnapshot> artifacts = files.stream()
                                                .map(DownloadSnapshot::from)
                                                .filter(file -> {
                                                    String fileName = file.name();
                                                    return !DownloadFileNamePolicy.isTemporaryBrowserFile(fileName);
                                                })
                                                .sorted(byFileName)
                                                .toList();
        return observeSnapshots(artifacts);
    }

    Optional<List<DownloadSnapshot>> observeSnapshots(List<DownloadSnapshot> artifacts) {
        if (artifacts.isEmpty()) {
            previousSnapshots = Collections.emptyList();
            consecutiveStablePolls = 0;
            return Optional.empty();
        }
        // A listed file may still be growing. Accept it only after its name, size and
        // modification time stay unchanged across three consecutive observations.
        consecutiveStablePolls = artifacts.equals(previousSnapshots) ? consecutiveStablePolls + 1 : 1;
        previousSnapshots = artifacts;
        return consecutiveStablePolls >= REQUIRED_STABLE_POLLS ? Optional.of(artifacts) : Optional.empty();
    }

}
