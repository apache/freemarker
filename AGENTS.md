# Instructions of AI Agents

## Environment requirements

- You need JDK 8, JDK 16, and JDK 17 (only for some tests) to be installed, in a way so that Gradle will find these.

## Project structure

`freemarker-*` directories (if not otherwise noted below): The **source sets** of the main jar artifact. The FreeMarker
project is a single Gradle project, that produces a single monolithic jar file (`freemarker.jar`). But the source code
is divided to multiple **source sets**, to allow using different Java versions and dependencies for compiling each. All
the resulting `class` files go into the final single jar file.

`freemarker-manual` contains the FreeMarker Manual (XDocBook, and Docgen configuration), and doesn't contribute to
`freemarker.jar` file or to the test suite.

`freemarker-test-graalvm-native` contains an isolated Gradle project for the basic testing of FreeMarker when used
in GraalVM native image. It doesn't contribute to the `freemarker.jar`, only used for testing.

`src/dist` contains files needed for release distribution artifacts, and doesn't contribute to `freemarker.jar` or to
the test suite.

`odgi.bnd` is used for generating the OSGi metadata that goes into `freemarker.jar`.

`rat-excludes` is used for Apache RAT license check, that's only relevant when building a distribution artifact.

## Building

Always use the Gradle wrapper script (`./gradlew` or `gradlew.bat`) in the project root directory, not other Gradle
installation.

- To run all JUnit tests and some other checks, run the `check` task (`./gradlew check`), not the `test` task. Avoid the
  `test` task, as that will only run the tests of the `core` source set!

- To run JUnit tests selectively, first consider which **source set** it is in.
  - If it's in `freemarker-core`, then run the `test` task with the optional `--tests` switch, like this: 
    `./gradlew :test --tests "freemarker.core.ASTTest"`
  - If it's in another source set, you cannot use the `test` task; instead, you must deduce a camel case test task name from
    the source set directory name. For example:
    - To test source set `freemarker-core16`: `./gradlew :core16Test`
    - To test source set `freemarker-javax-servlet`: `./gradlew :javaxServletTest`
    - And so on for the other source sets.
    The `--test` switch is supported by these tasks too, for example:
    `./gradlew :core16Test --tests "freemarker.ext.beans.NotExportedInternalPackageTest"`

- To try if the documentation is built successfully:
  - Generate HTML API documentation from the Java source code: Run the `javadoc` task (`./gradlew javadoc`)
  - Generate HTML from `book.xml` (FreeMarker Manual): Run the `manualOffline` task (`./gradlew manualOffline`)

- To build `freemarker.jar`, run the `jar` task (`./gradlew jar`)

- To try if the distribution artifacts can be built successfully (includes tests, jar, all documentation, Rat checks),
  run the `build` task (`./gradlew build`). 

## Contribution guidelines

- The default (main) development branch is `2.3-gae`; target that branch for pull requests.

- The code must be compatible with Java 8. Except, modules like `freemarker-core<version>` can use Java `<version>`
  (e.g. `freemarker-core16` can use Java 16).

- Take backward compatibility seriously! Behavioral changes that realistically break existing applications must be
  opt-in (inactive by default) via a `Configuration` or `Configurable` setting. If the changed behavior is preferable
  for new projects, and is not a highly incompatible change, then setting `incompatibleImprovements` to the next release
  version should enable it by default, and then that must be documented in the JavaDoc of the `Configuration(Version)`
  constructor.

- Add JavaDoc to things whose purpose is not obvious, or where there are edge-cases to explain. But, never add JavaDoc
  that doesn't add helpful information (like `@param file The file` is not helpful, so just omit that `@param`).

- Always add `@since` tag to the public types and members that you have added. 

- Add good JUnit test coverage to the new code.

## Security Scan Guidance

### Default Security Model

Core Assumption: Templates are trusted code. FreeMarker templates are **server-side code**, analogous to `.java` source
files. The baseline security model assumes templates are authored by developers or administrators and are
**not user-supplied input**. Vulnerabilities that require an attacker to control template content are **not in scope**
by default.

Regarding output escaping: For historical reasons, FreeMarker doesn't escape HTML/XML/JS/... output by
default. This can't be changed due to backward compatibility, but users are strongly encouraged to use the auto-escaping
features (such as `<#ftl output_format="HTML">`, or `ftlh` file extension). Legacy templates instead were encouraged to
use the `?html` built-in, or `<#escape x as x?html>...</#escape>` directive. That said, if proper escaping can be evaded
in an unexpected way when these are used, that's a security vulnerability.

### Constrained Execution Features

Some deployments allow a limited, accountable set of trusted-but-not-developer users to upload templates. FreeMarker
provides explicit APIs to impose certain limitations for these use-cases. These controls represent a public contract
with operators who rely on them for constrained deployments. Regressions here should be usually treated as security
vulnerabilities. Examples of such regressions are:

- A bypass of what a `MemberAccessPolicy` promises
- An unintended new path for `?new` to instantiate arbitrary classes
- Escaping the configured template root directory (i.e. causing a `TemplateLoader` to load a template from an unintended
  place)

For better understanding of constrained execution features, see the FAQ entry "Can I allow users to upload templates and
what are the security implications?" in `freemarker-manual/src/main/docgen/en_US/book.xml` (or the same content as HTML:
https://freemarker.apache.org/docs/app_faq.html#faq_template_uploading_security).
