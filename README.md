Apache FreeMarker {version}
===========================

[![Build status](https://github.com/apache/freemarker/actions/workflows/ci.yml/badge.svg)](https://github.com/apache/freemarker/actions/workflows/ci.yml)

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


What is Apache FreeMarker?
--------------------------

FreeMarker is a "template engine"; a generic tool to generate text
output (anything from HTML to auto generated source code) based on
templates. It's a Java package, a class library for Java programmers.
It's not an application for end-users in itself, but something that
programmers can embed into their products. FreeMarker is designed to
be practical for the generation of HTML Web pages, particularly by
servlet-based applications following the MVC (Model View Controller)
pattern.


Licensing
---------

FreeMarker is licensed under the Apache License, Version 2.0.

See the LICENSE file for more details!


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
application's ClassLoader will find it. For example, if you are using
FreeMarker in a web application, you probably want to put
freemarker.jar into the WEB-INF/lib directory of your web application.

FreeMarker has no required dependencies. It has several optional
dependencies, but usually you don't have to deal with them, because if
you are using an optional feature that's certainly because your
application already uses the related library.

Attention: If you upgrade to OpenJDK 9 or later, and you are using
XPath queries in templates, you will need to add Apache Xalan as a
dependency, as freemarker.ext.dom can't use the XPath support
included in OpenJDK anymore. It's not needed on Oracle Java 9,
or if FreeMarker is configured to use Jaxen for XPath.

The minimum required Java version is currently Java SE 8. (The presence
of a later version may be detected on runtime and utilized by
FreeMarker.)


Change log
----------

Online (for stable releases only):
https://freemarker.apache.org/docs/app_versions.html

Offline:
In the binary release, open documentation/index.html, and you will find the
link.


Building FreeMarker
-------------------

If you haven't yet, download the source release, or checkout FreeMarker from
the source code repository. See repository locations here:
https://freemarker.apache.org/sourcecode.html

You need JDK 8 and JDK 16 to be installed
(and [visible to Gradle](https://docs.gradle.org/current/userguide/toolchains.html)).

Be sure that your default Java version (which Gradle should use automatically) is at
least 16!

To build `freemarker.jar`, just issue `./gradlew jar` in the project root directory,
and it should download all dependencies automatically and build `freemarker.jar`.

To run all checks, issue `./gradlew check`.

To generate documentation, issue `./gradlew javadoc` and `./gradlew manualOffline`.

To see how the project would be deployed to Maven Central, issue
`./gradlew publishAllPublicationsToLocalRepository`,
and check the `build/local-deployment` directory.

See `gradle.properties` for some Gradle properties that you may what to set,
especially if you are building a release.


IDE setup
---------

### Eclipse

Below you find the step-by-step setup for Eclipse (originally done on Mars.1):

- Start Eclipse
- You may prefer to start a new workspace (File -> "Switch workspace"), but
  it's optional.
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
    Ensure that you have JDK 16 installed, and that it was added to Eclipse.
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
- You will still have errors on these java files (because different java
  files depend on different versions of the same library, and Eclipse can't
  handle that). Exclude those java files from the Build Path (in the Package
  Explorer, right-click on the problematic file -> "Build Path" -> "Exclude"):
    _Jython20*.java,
    _Jython22*.java,
    _FreeMarkerPageContext2.java,
    FreeMarkerJspFactory2.java,
  Also, close these files if they are open. Now you shouldn't have any errors.
- At Project -> Properties -> Java Code Style -> Formatter, check in "Enable
  project specific settings", and then select "FreeMarker" as active profile.
- At Project -> Properties -> Java Editor -> Save Actions, check "Enable project
  specific settings", then "Perform the selected actions on save", and have
  only "Organize imports" and "Additional actions" checked (the list for the
  last should contain "Add missing @Override annotations",
  "Add missing @Override annotations to implementations of interface methods",
  "Add missing @Deprecated annotations", and "Remove unnecessary cast").
- Right click on the project -> Run As -> JUnit Test
  It should run without problems (all green).
- It's highly recommended to use the Eclipse FindBugs plugin.
  - Install it from Eclipse Marketplace (3.0.1 as of this writing)
  - Window -> Preferences -> Java -> FindBugs:
    Set all bug marker ranks from Warning to Error. (For false alarms we add
    @SuppressFBWarnings(value = "...", justification = "...") annotations.)
  - Project -> Properties -> FindBugs -> [x] Run Automatically
  - There should 0 errors. But sometimes the plugin fails to take the
    @SuppressFBWarnings annotations into account; then use Project -> Clean. 

### IntelliJ IDEA

Originally done on IntelliJ IDEA Community 2023.3.2:

- "File" -> "Open": Select the "settings.gradle.kts" within the freemarker root directory.
- If the project fails to load (or build), then adjust the following configuration
  in "File" -> "Settings" -> "Build, Execution, Deployment" -> "Build Tools" -> "Gradle":
  - Gradle JVM: JDK 16 (or higher)
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
