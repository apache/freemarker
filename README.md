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
    <groupId>org.apache</groupId>
    <artifactId>freemarker</artifactId>
    <version>{version}</version>
  </dependency>
```

Otherwise simply copy freemarker.jar to a location where your Java
application's ClassLoader will find it. For example, if you are using
FreeMarker in a web application, you probably want to put
freemarker.jar into the WEB-INF/lib directory of your web application.

FreeMarker 3 has only one required dependency, `org.slf4j:slf4j-api`. (Of 
course, it will be automatically downloaded by Maven, Gradle, and the like, 
and is already there in almost all projects anyway. If it wasn't there, note 
that adding slf4j-api is not enough, as it needs an implementation, which is 
not downloaded automatically by Maven, etc. The most popular is 
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

You need JDK 8, Apache Ant (tested with 1.8.1) and Ivy (tested with 2.4.0) to
be installed. To install Ivy (but be sure it's not already installed), issue
`ant download-ivy`; it will copy Ivy under `~/.ant/lib`. (Alternatively, you
can copy `ivy-<version>.jar` into the Ant home `lib` subfolder manually.)

It's recommended to copy `build.properties.sample` into `build.properties`,
and edit its content to fit your system. (Although basic jar building should
succeeds without the build.properties file too.)

To build `freemarker.jar`, just issue `ant` in the project root directory, and
it should download all dependencies automatically and build `freemarker.jar`. 

If later you change the dependencies in `ivy.xml`, or otherwise want to
re-download some of them, it will not happen automatically anymore, and you
must issue `ant update-deps`.

To test your build, issue `ant test`.

To generate documentation, issue `ant javadoc` and `ant manualOffline`.


Eclipse and other IDE setup
---------------------------

Below you find the step-by-step setup for Eclipse Neon.1. If you are using a
different version or an entierly different IDE, still read this, and try to
apply it to your development environment:

- Install Ant and Ivy, if you haven't yet; see earlier.
- From the command line, run  `ant clean jar ide-dependencies`
  (Note that now the folders `ide-dependencies`, `build/generated-sources` and
  `META-INF` were created.)
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
    Ensure that you have JDK 8 installed, and that it was added to Eclipse.
    Note that it's not JRE, but JDK.
  - Java -> Compiler -> Javadoc:
    "Malformed Javadoc comments": Error
    "Only consider members as visible": Private
    "Validate tag argunebts": true
    "Missing tag descriptions": Validate @return tags
    "Missing Javadoc tags": Ignore
    "Missing Javadoc comments": Ignore
- Create new "Java Project" in Eclipse:
  - In the first window popping up:
    - Change the "location" to the directory of the FreeMarker project
    - Press "Next"
  - In the next window, you see the build path settings:
    - On "Source" tab, ensure that exactly these are marked as source
      directories (be careful, Eclipse doesn't auto-detect these well):
        build/generated-sources/java
        src/main/java
        src/main/resources
        src/test/java
        src/test/resources
    - On the "Libraries" tab:
      - Delete everyhing from there, except the "JRE System Library [...]"
      - Edit "JRE System Library [...]" to "Execution Environment" "JavaSE 1.8"
      - Add all jar-s that are directly under the "ide-dependencies" directory
        (use the "Add JARs..." and select all those files).
    - On the "Order and Export" tab find dom4j-*.jar, and send it to the
        bottom of the list (becase, an old org.jaxen is included inside
        dom4j-*.jar, which casues compilation errors if it wins over
        jaxen-*.jar).
   - Press "Finish"
- Eclipse will indicate many errors at this point; it's expected, read on.
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
- Right click on the project -> Run As -> JUnit Test
  It should run without problems (all green).
- It's highly recommened to use the Eclipse FindBugs plugin.
  - Install it from Eclipse Marketplace (3.0.1 as of this writing)
  - Window -> Preferences -> Java -> FindBugs:
    Set all bug marker ranks from Warning to Error. (For false alarms we add
    @SuppressFBWarnings(value = "...", justification = "...") annotations.)
  - Project -> Properties -> FindBugs -> [x] Run Automatically
  - There should 0 errors. But sometimes the plugin fails to take the
    @SuppressFBWarnings annotations into account; then use Project -> Clean. 
