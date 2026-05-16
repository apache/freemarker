# Instructions of AI Agents

## Environment requirements

- You need JDK 8, JDK 16, and JDK 17 (only for some tests) to be installed, in a way so that Gradle will find these.

## Project structure

`freemarker-*` directories (if not otherwise noted below): The **source sets** of the main jar artifact. The FreeMarker
project is a single Gradle project that produces a single monolithic jar file (`freemarker.jar`). But the source code
is divided into multiple **source sets**, to allow using different Java versions and dependencies for compiling each. All
the resulting `class` files go into the final single jar file.

`freemarker-manual` contains the FreeMarker Manual (XDocBook and Docgen configuration), and doesn't contribute to
`freemarker.jar` or to the test suite.

`freemarker-test-graalvm-native` contains an isolated Gradle project for basic testing of FreeMarker when used
in a GraalVM native image. It doesn't contribute to `freemarker.jar`; it is only used for testing.

`src/dist` contains files needed for release distribution artifacts, and doesn't contribute to `freemarker.jar` or to
the test suite.

`odgi.bnd` is used for generating the OSGi metadata that goes into `freemarker.jar`.

`rat-excludes` is used for Apache RAT license check, which is only relevant when building a distribution artifact.

## Building

Always use the Gradle wrapper script (`./gradlew` or `gradlew.bat`) in the project root directory, not other Gradle
installation.

- To run all JUnit tests and some other checks, run the `check` task (`./gradlew check`), not the `test` task, because the
  `test` task only runs the tests of the `core` source set!

- To run JUnit tests selectively, first consider which **source set** it is in.
  - If it's in `freemarker-core`, then run the `test` task with the optional `--tests` switch, like this: 
    `./gradlew :test --tests "freemarker.core.ASTTest"`
  - If it's in another source set, you cannot use the `test` task; instead, you must deduce a camel case test task name from
    the source set directory name. For example:
    - To test source set `freemarker-core16`: `./gradlew :core16Test`
    - To test source set `freemarker-javax-servlet`: `./gradlew :javaxServletTest`
    - And so on for the other source sets.
    The `--tests` switch is supported by these tasks too, for example:
    `./gradlew :core16Test --tests "freemarker.ext.beans.NotExportedInternalPackageTest"`

- To check if the documentation builds successfully:
  - Generate HTML API documentation from the Java source code: Run the `javadoc` task (`./gradlew javadoc`)
  - Generate HTML from `book.xml` (FreeMarker Manual): Run the `manualOffline` task (`./gradlew manualOffline`)

- To build `freemarker.jar`, run the `jar` task (`./gradlew jar`)

- To check if the distribution artifacts can be built successfully (includes tests, jar, all documentation, Rat checks),
  run the `build` task (`./gradlew build`). 

## Contribution guidelines

- The default (main) development branch is `2.3-gae`; target that branch for pull requests.

- The code must be compatible with Java 8. However, modules like `freemarker-core<version>` can use Java `<version>`
  (e.g., `freemarker-core16` can use Java 16).

- Take backward compatibility seriously! Behavioral changes that realistically break existing applications must be
  opt-in (inactive by default) via a `Configuration` or `Configurable` setting. If the changed behavior is preferable
  for new projects and is not highly incompatible, setting `incompatibleImprovements` to the next release
  version should enable it by default, and that must be documented in the JavaDoc of the `Configuration(Version)`
  constructor.

- Add JavaDoc to things whose purpose is not obvious, or where there are edge cases to explain. But never add JavaDoc
  that doesn't add helpful information (like `@param file The file` is not helpful, so just omit that `@param`).

- Always add a `@since` tag to public types and members you add.

- Add good JUnit test coverage to the new code.

## Security Scan Guidance

See [SECURITY.md](SECURITY.md) for details.
