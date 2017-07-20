Apache FreeMarker {version}
===========================

[![Build Status](https://travis-ci.org/apache/incubator-freemarker.svg?branch=3)](https://travis-ci.org/apache/incubator-freemarker)

For the latest version or to report bugs visit:
http://freemarker.org/


DISCLAIMER
----------

Apache FreeMarker is an effort undergoing incubation at The Apache
Software Foundation (ASF). Incubation is required of all newly accepted
projects until a further review indicates that the infrastructure,
communications, and decision making process have stabilized in a manner
consistent with other successful ASF projects. While incubation status is
not necessarily a reflection of the completeness or stability of the
code, it does indicate that the project has yet to be fully endorsed by
the ASF.


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

Online: http://freemarker.org/docs/

Offline: The full documentation is available in the binary distribution
in the documentation/index.html directory.


Installing
----------

If you are using Maven, just add this dependency:

```xml
  <dependency>
    <groupId>org.apache.freemarker</groupId>
    <artifactId>freemarker-core</artifactId>
    <version>{version}</version>
  </dependency>
```

Otherwise simply copy freemarker-core-<version>.jar to a location where your
Java application's ClassLoader will find it. For example, if you are using
FreeMarker in a web application, you probably want to put the jar into the
WEB-INF/lib directory of your web application.

FreeMarker 3 has only one required dependency, `org.slf4j:slf4j-api`. (Of 
course, it will be automatically downloaded by Maven, Gradle, and the like, 
and is already there in almost all projects anyway. If it wasn't there, note 
that adding slf4j-api is not enough, as it needs an implementation, which is 
not downloaded automatically by Maven, Gradle, etc. The most popular is 
`ch.qos.logback:logback-classic`. FreeMarker has several optional dependencies,
but usually you don't have to deal with them, because if you are using an
optional feature that's certainly because your application already uses the
related library.

The minimum required Java version is currently Java SE 7. (The presence
of a later version may be detected on runtime and utilized by
FreeMarker.)


Change log
----------

Online (for stable releases only):
http://freemarker.org/docs/app_versions.html

Offline:
In the binary release, open documentation/index.html, and you will find the
link.


Building FreeMarker
-------------------

If you haven't yet, download the source release, or checkout FreeMarker from
the source code repository. See repository locations here:
http://freemarker.org/sourcecode.html

You need JDK 8 to be installed.

You must copy `gradle.properties.sample` into `gradle.properties`, and edit
its content to fit your system.

To build the jar-s of all modules (freemarker-core, freemarker-servlet, etc.),
issue `./gradlew jar` in the project root directory (Windows users see the note
below though). It will automatically download all dependencies on first run too
(including the proper version of Gradle itself). The built jar-s will be in the
build/libs subdirectory of each module (freemarker-core, freemarker-servlet,
etc.). You can also install the jar-s into your local Maven repository with
`./gradlew install`.

Note for Windows users: If you are using an Apache source release (as opposed
to checking the project out from the Git repository), ./gradlew will fail as
`gradle\wrapper\gradle-wrapper.jar` is missing. Due to Apache policy restricton
we can't include that file in distributions, so you have to download that very
common artifact from somewhere manually (like from Git repository of
FreeMarker). (On UN*X-like systems you don't need that jar, as our custom
`gradlew` shell script does everything itself.)

To test your build, issue `./gradlew test`. Issued from the top directory,
this will run the tests of all modules.

To generate the aggregated API documention (contains the API of several modules
that are deemed to be used commonly enough), issue `./gradlew aggregateJavadoc`
from the root module; the output will appear in the `build/docs/javadoc`
subdirectory. To generate API documentation per module, issue
`./gradlew javadoc`; the output will appear in the `build/docs/javadoc`
subdirectory of the module.

To generate the FreeMarker Manual, issue `./gradlew manualOffline`
(TODO: not yet working); the output will appear under
`freemarker-manual/build/docgen`.


IDE setup
---------

### Eclipse

Last tested Eclipse Neon.1.

- Start Eclipse
- You may prefer to start a new workspace (File -> "Switch workspace"), but
  it's optional.
- Window -> Preferences
  - General -> Workspace, set the text file encoding
    to "UTF-8". (Or, you can set the same later on project level instead.)
  - General -> Editors, set:
    - Insert space for tabs
    - Show print margin, 120 columns
  - Java -> Code Style -> Formatter -> Import...
    Select src\ide-settings\Eclipse\Formatter-profile-FreeMarker.xml
    inside the FreeMarker project directory.
    This profile uses space-only indentation policy and 120 character line
    width, and formatting rules that are pretty much standard in modern Java.
  - Java -> Code Style -> Organize imports
    The order is this (the Eclipse default): java, javax, org, com.
    Number of imports required for .*: 99
    Number of static imports needed for .*: 1
  - Java -> Installed JRE-s:
    Ensure that you have JDK 7 and JDK 8 installed, and that it was added to
    Eclipse. Note that it's not JRE, but JDK.
  - Java -> Compiler -> Javadoc:
    "Malformed Javadoc comments": Error
    "Only consider members as visible": Private
    "Validate tag argunebts": true
    "Missing tag descriptions": Validate @return tags
    "Missing Javadoc tags": Ignore
    "Missing Javadoc comments": Ignore
- Project -> Properties -> Java Compiler -> Errors/Warnings:
  Check in "Enable project specific settings", then set "Forbidden reference
  (access rules)" from "Error" to "Warning".
- At Project -> Properties -> Java Code Style -> Formatter, check in "Enable
  project specific settings", and then select "FreeMarker" as active profile.
- At Project -> Properties -> Java Editor -> Save Actions, check "Enable project
  specific settings", then "Perform the selected actions on save", and have
  only "Organize imports" and "Additional actions" checked (the list for the
  last should contain "Add missing @Override annotations",
  "Add missing @Override annotations to implementations of interface methods",
  "Add missing @Deprecated annotations", and "Remove unnecessary cast").
- Right click on the root project -> Run As -> JUnit Test [TODO: Try this]
  It should run without problems (all green).
- It's highly recommened to use the Eclipse FindBugs plugin.
  - Install it from Eclipse Marketplace (3.0.1 as of this writing)
  - Window -> Preferences -> Java -> FindBugs:
    Set all bug marker ranks from Warning to Error. (For false alarms we add
    @SuppressFBWarnings(value = "...", justification = "...") annotations.)
  - Project -> Properties -> FindBugs -> [x] Run Automatically
  - There should 0 errors. But sometimes the plugin fails to take the
    @SuppressFBWarnings annotations into account; then use Project -> Clean. 

### IntelliJ IDEA

Last tested on IntelliJ IDEA Community 2017.1.5.
    
- First build the project with Gradle if you haven't yet (see earlier how)
- "New..." -> "Project from existing source"
  - Point to the root project `incubator-freemarker`) directory
  - On the next screen, select "Import project from external model" and "Gradle"
  - On the next screen, select "Use gradle wrapper task configuration".
    Be sure at least Java 8 is selected for Gradle. Other defaults should be fine.
  - On the next window, all modules will be selected, that's fine, go on
  - On the next window, it will prompt to remove the `incubator-freemarker` from the project.
    Let it do it (as it's an incorrect duplication of the `freemarker` root project).
  - At the end of this process you should have all modules Project tree view.
- "File" -> "Project Structure..."
  Under "Project", set the SDK to 1.7, and the language level to 7.
  Under "Modules", for "freemarker-core-test-java8" / "freemarker-core-test-java8_main" and
  "freemarker-core-test-java8_test", change the "Module SDK" to 1.8 (on the "Dependencies" tab).
- "File" -> "Settings"
  - Under "Editor" / "Code style", import and use
    incubator-freemarker/src/ide-settings/IntelliJ-IDEA/Java-code-style-FreeMarker.xml
  - Under "Editor" / "Inspections", import and use
    incubator-freemarker/src/ide-settings/IntelliJ-IDEA/Editor-Inspections-FreeMarker.xml
- Testing your setup:
  - You may do "Bulild"/"Build project" (Ctrl+F9) to see if everyting compiles now.
  - You may ran the `test` task of the root project with Gradle to see that everything works as
    expected. To do that from IntelliJ, create a run configuration:
    "Run" \ "Run Configurations..." \ "+" \ "Gradle" then:
    - Set "Name" to `All tests` for example
    - Set "Gradle project" to the root project (`freemarker` aka. `incubator-freemarker`)
    - Set "Tasks" to `test`
- TODO Setting up the FindBugs plugin
