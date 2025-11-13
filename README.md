Apache FreeMarker™ {version}
============================

[![Build status](https://github.com/apache/freemarker/actions/workflows/ci.yml/badge.svg)](https://github.com/apache/freemarker/actions/workflows/ci.yml)
![GraalVM Ready](https://img.shields.io/badge/GraalVM-Ready-orange)

For the latest version or to report bugs visit:
https://freemarker.apache.org/


Regarding pull requests on Github
---------------------------------

By sending a pull request you grant the Apache Software Foundation
sufficient rights to use and release the submitted work under the
Apache license. You grant the same rights (copyright license, patent
license, etc.) to the Apache Software Foundation as if you have signed
a [Contributor License Agreement](https://www.apache.org/dev/new-committers-guide.html#cla).
For contributions that are judged to be non-trivial, you will be asked
to actually signing a Contributor License Agreement.


What is Apache FreeMarker™?
---------------------------

Apache FreeMarker™ is a "template engine"; a generic tool to generate
text output (anything from HTML to auto generated source code) based on
templates. It's a Java package, a class library for Java programmers.
It's not an application for end-users in itself, but something that
programmers can embed into their products. FreeMarker is designed to
be practical for the generation of HTML Web pages, particularly by
servlet-based applications following the MVC (Model View Controller)
pattern.


Licensing
---------

FreeMarker is licensed under the Apache License, Version 2.0.

See the `LICENSE` file for more details!


Documentation
-------------

Online: https://freemarker.apache.org/docs/

Offline: The full documentation is available in the binary distribution
in the documentation/index.html directory.


Installing
----------

If you are using Maven, just add this dependency:

```xml
  <!--
  Attention: Be sure nothing pulls in an old dependency with groupId
  "freemarker" (without the "org."), because then you will end up with
  two freemarker.jar-s and unpredictable behavior on runtime!
  -->
  <dependency>
    <groupId>org.freemarker</groupId>
    <artifactId>freemarker-gae</artifactId>
    <version>{version}</version>
  </dependency>
```

Otherwise, simply copy `freemarker.jar` to a location where your Java
application's `ClassLoader` will find it. For example, if you are using
FreeMarker in a web application, you probably want to put
`freemarker.jar` into the `WEB-INF/lib` directory of your web application.

FreeMarker has no required dependencies. It has several optional
dependencies, but usually you don't have to deal with them, because if
you are using an optional feature that's certainly because your
application already uses the related library.

Attention: If you upgrade to OpenJDK 9 or later, and you are using
XPath queries in templates, you will need to add Apache Xalan as a
dependency, as `freemarker.ext.dom` can't use the XPath support
included in OpenJDK anymore. It's not needed on Oracle Java 9,
or if FreeMarker is configured to use Jaxen for XPath.

The minimum required Java version is currently Java SE 8. (The presence
of a later version is detected on runtime and utilized by FreeMarker
automatically.)


Change log
----------

Online (for stable releases only):
https://freemarker.apache.org/docs/app_versions.html

Offline:
In the binary release, open `documentation/index.html`, and you will find the
link.


Building FreeMarker
-------------------

If you haven't yet, download the source release, or checkout FreeMarker from
the source code repository. See repository locations here:
https://freemarker.apache.org/sourcecode.html

You need JDK 8, JDK 16, and JDK 17 (only for some tests) to be installed
(and [visible to Gradle](https://docs.gradle.org/current/userguide/toolchains.html)).
That's because different parts of the source code target different Java versions,
and Gradle requires the exact JDK version (not higher) for each.

Be sure that your default Java version (which Gradle should use automatically) is at
least 17!

If you are building from the official source *release* (not from source that you
got from Git), `gradle/wrapper/gradle-wrapper.jar` is missing from that, and you
have to add it yourself! You can download it
[from GitHub source code page](https://github.com/apache/freemarker/tree/2.3-gae/gradle/wrapper)!
(Or, use your own Gradle installation instead of `gradlew`.)

To build `freemarker.jar`, just issue `./gradlew jar` (`gradlew.bat jar` on Windows) in the
project root directory, and it should download all dependencies automatically, and build
`freemarker.jar`.

To run all JUnit tests and some other checks, issue `./gradlew check`. (Avoid the
`test` task, as that will only run the tests of the `core` source set.)

To generate offline documentation, issue `./gradlew javadoc` and `./gradlew manualOffline`.

To build the distribution artifacts (the `tgz`-s that people can download), run `./gradlew build`. However,
for a stable (non-`SNAPSHOT`) version number, you must set up signing, or disable that verification
with `freemarker.allowUnsignedReleaseBuild=true`; see `gradle.properties` in this project for those!

Reproducible builds: If the resulting `freemarker.jar` is not identical with the official jar, see the build environment
in the `.buildinfo` file packed into the official source distribution, and also into the Maven "sources" artifact! At
least with identical Java versions, the resulting `freemarker.jar` meant to match exactly.

Note on trying things out with an ad-hoc class that has `main` method: Don't do that, instead write it as a JUnit test.
FreeMarker needs to be loaded from `freemarker.jar` that contains the `META-INF/versions` directory (as per JEP 238,
Multi-Release JAR Files). If you run a test, Gradle ensures that you have an up-to-date `freemarker.jar`, and
that it's in the classpath. Without that, FreeMarker will behave as if you are on a lower Java version.


### Maven-related build tasks

To see how the project would be deployed to Maven Central, issue
`./gradlew publishAllPublicationsToLocalRepository`,
and check the `build/local-deployment` directory.
 
To publish to the Apache Maven Repository (from where you can also promote releases to the Maven Central Repository)
issue `.\gradlew publish`. Note that for this the following Gradle properties must be properly set
(in `gradle.properties`, or pass them via `-P<name>=<value>` arguments):
`freemarker.signMethod`, `freemarker.deploy.apache.user`, `freemarker.deploy.apache.password`.

### FreeMarker website related build tasks

The website (the FreeMarker homepage) is build by the `freemarker-site` project, not this project (`freemaker`). Except,
the Manual and the API documentation (javadoc) is generated in this project.

The online API documentation is the same as the offline one, generated with `./gradlew javadoc`. The output is uploaded
manually into the `docs/api` directory of the website.

The online Manual is generated with `./gradlew manualOnline`, and the output is uploaded manually into the `docs`
directory of the webpage (without deleting `docs/api`). `manualOnline` requires Node.js to be already installed locally
(see the `freemarker-docgen` project for the minimum version). Node.js is only used to generate the Pagefind index, and
the output is purely static HTML. However, due to browser security restrictions, the search functionality will only work
if you visit via HTTP(S), and not via a `file:` URL (so for local testing use `npx http-server`).


IDE setup
---------

### IntelliJ IDEA

Originally done on IntelliJ IDEA Community 2023.3.2:

- "File" -> "Open": Select the "settings.gradle.kts" within the freemarker root directory.
- If the project fails to load (or build), then adjust the following configuration
  in "File" -> "Settings" -> "Build, Execution, Deployment" -> "Build Tools" -> "Gradle":
  - Gradle JVM: JDK 17 (or higher)
  - Build and run using: "Gradle"
  - Run tests using: "Gradle"

- "File" -> "Settings"
  - Under "Editor" / "Code style", import and use
    freemarker/src/ide-settings/IntelliJ-IDEA/Java-code-style-FreeMarker.xml
  - Under "Editor" / "Inspections", import and use
    freemarker/src/ide-settings/IntelliJ-IDEA/Editor-Inspections-FreeMarker.xml
  - Copy the copyright header comment from some of the java files, then
    under "Editor" / "Copyright" / "Copyright Profiles" click "+", enter "ASL2" as name,
    then paste the copyright header. Delete the `/*` and ` */` lines, and the ` *`
    prefixes (to select columns of text, hold Alt while selecting with the mouse.) Then
    go back to "Copyright" in the tree, and set "Default project copyright" to "ASL2".

### Eclipse

This section wasn't updated long ago. But you should import the project as any other
Gradle project. After that, it's recommended to set these preferences (based on Eclipse Mars):

- Window -> Preferences
  - General -> Workspace, set the text file encoding
    to "UTF-8". (Or, you can set the same later on project level instead.)
  - General -> Editors -> Text Editors, set:
    - Insert space for tabs
    - Show print margin, 120 columns
  - Java -> Code Style -> Formatter -> Import...
    Select src\ide-settings\Eclipse\Formatter-profile-FreeMarker.xml
    inside the FreeMarker project directory.
    (On IntelliJ IDEA, import
    src/ide-settings/IntelliJ-IDEA/Java-code-style-FreeMarker.xml instead)
    This profile uses space-only indentation policy and 120 character line
    width, and formatting rules that are pretty much standard in modern Java.
  - Java -> Code Style -> Organize imports
    (On IntelliJ IDEA, this was already configured by the Java code style
    import earlier.)
    The order is this (the Eclipse default): java, javax, org, com.
    Number of imports required for .*: 99
    Number of static imports needed for .*: 1
  - Java -> Installed JRE-s:
    Ensure that you have JDK 17 installed, and that it was added to Eclipse.
    Note that it's not JRE, but JDK.
  - Java -> Compiler -> Javadoc:
    "Malformed Javadoc comments": Error
    "Only consider members as visible": Private
    "Validate tag arguments": true
    "Missing tag descriptions": Validate @return tags
    "Missing Javadoc tags": Ignore
    "Missing Javadoc comments": Ignore
- Import the project as any other Gradle project.
- Eclipse will indicate many errors at this point; it's expected, read on.
- Project -> Properties -> Java Compiler
  - In Errors/Warnings, check in "Enable project specific settings", then set
    "Forbidden reference (access rules)" from "Error" to "Warning".
- At Project -> Properties -> Java Code Style -> Formatter, check in "Enable
  project specific settings", and then select "FreeMarker" as active profile.
- At Project -> Properties -> Java Editor -> Save Actions, check "Enable project
  specific settings", then "Perform the selected actions on save", and have
  only "Organize imports" and "Additional actions" checked (the list for the
  last should contain "Add missing @Override annotations",
  "Add missing @Override annotations to implementations of interface methods",
  "Add missing @Deprecated annotations", and "Remove unnecessary cast").
- Right-click on the project -> Run As -> JUnit Test
  It should run without problems (all green).
- It's highly recommended to use the Eclipse FindBugs plugin.
  - Install it from Eclipse Marketplace (3.0.1 as of this writing)
  - Window -> Preferences -> Java -> FindBugs:
    Set all bug marker ranks from Warning to Error. (For false alarms we add
    @SuppressFBWarnings(value = "...", justification = "...") annotations.)
  - Project -> Properties -> FindBugs -> [x] Run Automatically
  - There should 0 errors. But sometimes the plugin fails to take the
    @SuppressFBWarnings annotations into account; then use Project -> Clean. 
