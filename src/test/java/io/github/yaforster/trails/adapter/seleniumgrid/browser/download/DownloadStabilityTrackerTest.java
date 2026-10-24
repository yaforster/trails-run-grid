package io.github.yaforster.trails.adapter.seleniumgrid.browser.download;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class DownloadStabilityTrackerTest {

    @Test
    void shouldAcceptDownloads_whenThreeConsecutiveObservationsAreUnchanged() {
        DownloadStabilityTracker tracker = new DownloadStabilityTracker();
        List<DownloadSnapshot> files = List.of(new DownloadSnapshot("invoice.pdf", 42, 100));
        tracker.observeSnapshots(files);
        tracker.observeSnapshots(files);

        assertThat(tracker.observeSnapshots(files)).contains(files);
    }

    @Test
    void shouldRestartStabilityWindow_whenFileSizeChanges() {
        DownloadStabilityTracker tracker = new DownloadStabilityTracker();
        List<DownloadSnapshot> initial = List.of(new DownloadSnapshot("invoice.pdf", 42, 100));
        List<DownloadSnapshot> growing = List.of(new DownloadSnapshot("invoice.pdf", 84, 100));
        tracker.observeSnapshots(initial);
        tracker.observeSnapshots(initial);

        assertThat(tracker.observeSnapshots(growing)).isEmpty();
    }

    @Test
    void shouldRestartStabilityWindow_whenDownloadsDisappear() {
        DownloadStabilityTracker tracker = new DownloadStabilityTracker();
        List<DownloadSnapshot> files = List.of(new DownloadSnapshot("invoice.pdf", 42, 100));
        tracker.observeSnapshots(files);
        tracker.observeSnapshots(files);
        tracker.observeSnapshots(List.of());

        assertThat(tracker.observeSnapshots(files)).isEmpty();
    }
}
