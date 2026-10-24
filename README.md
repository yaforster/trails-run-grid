# Trails Selenium Grid Adapter

`io.github.yaforster:trails-run-grid` supplies the one complete remote browser capability for Trails. Its
Spring Boot autoconfiguration registers exactly one `TrailsBrowserRegistration` with adapter ID `selenium-grid`; it
does not expose a partial browser registration.

The adapter runs Chrome, Edge, and Firefox through Selenium Grid, enables managed downloads, converts downloaded PDFs
to Trails document results, and contributes the configured download timeout to `/api/capabilities`. Remote session
creation has explicit, bounded retry and per-browser admission limits. Failed session initialization closes any
partially created driver and makes that browser unavailable for the current test set.

## Customer wrapper

Import `trails-bom`, then add `trails-sdk`, exactly one persistence adapter, and this adapter.

```xml
<dependency>
    <groupId>io.github.yaforster</groupId>
    <artifactId>trails-run-grid</artifactId>
</dependency>
```

```yaml
trails:
  adapter:
    selenium-grid:
      grid-url: http://selenium-hub:4444
      # Required when persisted localhost stage URLs must resolve within the Grid network.
      browser-base-url: http://trails-wrapper:8080
      file-download-timeout: 30s
      session-max-attempts: 1
      maximum-concurrent-sessions: 10
```

`grid-url` is required and must be an absolute HTTP(S) URI. `browser-base-url`, when set, replaces only a persisted
stage URL whose host is `localhost`, `127.0.0.1`, or `::1`; non-loopback stage URLs remain unchanged. All timeouts and
limits must be positive. `session-max-attempts` defaults to one, so no browser action is retried implicitly.

## Download diagnostics

Suppressed download storage, transfer/read, and cleanup exceptions are logged at WARN with their causes. Collection
timeouts are logged at DEBUG because a session may legitimately have no downloads. Enable DEBUG for
`io.github.yaforster.trails.adapter.seleniumgrid.browser.download.SeleniumDownloadCollector` to diagnose these timeouts.
Expected missing elements and stale-element retries remain quiet.
