# Trails Selenium Grid Adapter repository map

```text
adapter/trails-run-grid/
|-- pom.xml                                Single adapter JAR, dependency boundary, and release configuration
|-- format/java-formatter.xml              Java formatting rules
|-- src/
|   |-- main/java/.../seleniumgrid/         Grid registration, configuration, providers, and resilience
|   |   |-- browser/                       Session facade, elements, and viewport
|   |   |   `-- download/                  Managed download collection, stability, transfer, and cleanup
|   |   |-- document/                      PDF text extraction, positional matching, and proof rendering
|   |   `-- execution/                     Complete browser initialization and executable test inputs
|   |-- main/resources/META-INF/spring/    Boot auto-configuration import
|   `-- test/java/.../seleniumgrid/         Adapter-owned configuration and implementation tests
|-- README.md                              Customer configuration guidance
`-- REPOMAP.md                             This map
```
