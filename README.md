Apache FreeMarker {version}
===========================

[![Build Status](https://travis-ci.org/apache/freemarker.svg?branch=2.3-gae)](https://travis-ci.org/apache/freemarker)

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
    <artifactId>freemarker</artifactId>
    <version>{version}</version>
  </dependency>
```

Otherwise simply copy freemarker.jar to a location where your Java
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

The minimum required Java version is currently Java SE 7. (The presence
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

You need JDK 8 (not JDK 9!), Apache Ant (tested with 1.9.6) and Ivy (tested
with 2.4.0) to be installed. To install Ivy (but be sure it's not already
installed), issue `ant download-ivy`; it will copy Ivy under `~/.ant/lib`.
(Alternatively, you can copy `ivy-<version>.jar` into the Ant home `lib`
subfolder manually.)

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


IDE setup
---------

### First steps for all IDE-s

Do these first, regardless of which IDE you are using:

- Install Ant and Ivy, if you haven't yet; see earlier.

- From the command line, run  `ant clean jar ide-dependencies`
  (Note that now the folders `ide-dependencies`, `build/generated-sources` and
  `META-INF` were created.)

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
    Ensure that you have JDK 8 installed, and that it was added to Eclipse.
    Note that it's not JRE, but JDK.
  - Java -> Compiler -> Javadoc:
    "Malformed Javadoc comments": Error
    "Only consider members as visible": Private
    "Validate tag arguments": true
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
        src/main/java,
        src/main/resources,
        src/test/java,
        src/test/resources
    - On the "Libraries" tab:
      - Delete everyhing from there, except the "JRE System Library [...]"
      - Edit "JRE System Library [...]" to "Execution Environment" "JavaSE 1.8"
      - Add all jar-s that are directly under the "ide-dependencies" directory
        (use the "Add JARs..." and select all those files).
    - On the "Order and Export" tab find dom4j-*.jar, and send it to the
        bottom of the list (because, an old org.jaxen is included inside
        dom4j-*.jar, which causes compilation errors if it wins over
        jaxen-*.jar).
   - Press "Finish"
- Eclipse will indicate many errors at this point; it's expected, read on.
- Project -> Properties -> Java Compiler
  - Set "Compiler Compliance Level" to "1.7" (you will have to uncheck
    "Use compliance from execution environment" for that)
  - In Errors/Warnings, check in "Enable project specific settings", then set
    "Forbidden reference (access rules)" from "Error" to "Warning".
- You will still have errors on these java files (because different java
  files depend on different versions of the same library, and Eclipse can't
  handle that). Exclude those java files from the Build Path (in the Package
  Explorer, right click on the problematic file -> "Build Path" -> "Exclude"):
    _Jython20*.java,
    _Jython22*.java,
    _FreeMarkerPageContext2.java,
    FreeMarkerJspFactory2.java,
    Java8*.java
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

Originally done on IntelliJ IDEA Community 2018.2.4:

- "New" -> "Project". In order as the IntelliJ will prompt you:

  - Select "Java" on the left side, and "1.8" for SDK on the right side. Press "Next".
  
  - Template selection: Don't chose anything, "Next"
  
  - Project name: "FreeMarker-2.3-gae".
    Project location: Wherever you have checked out the 2.3-gae branch from Git.
    Press "Finish"

- Open your newly created "FreeMarker-2.3-gae" project

- "File" -> "Project Structure..."

  - Select "Modules" (on the left) / "Sources" (tab on the right). Now you see a Content Root
    that was automatically added (at the rightmost side, under the "Add Content Root" button).
    Remove it (click the "X" next to it); no Content Root should remain.
    Now "Add Content Root", and select the FreeMarker project folder. IntelliJ will now add the new
    Content Root, and automatically add some "Source Folders" and maybe some more under it, but it
    won't be correct, so edit it until your newly added Source Root has this content:
    
    - Source Folders:  
      src/main/java,  
      build/generated-sources/java [generated]
    
    - Test Source folders:  
      src/test/java  
      
    - Resource Folders:  
      src/main/resources

    - Test Resource Folders:  
      src/test/resources
      
  - Still inside the "Sources" tab, change the "Language level" to "7". (Yes, we use Java 8 SDK with
    language level 7 in the IDE, due to the tricks FreeMarker uses to support different Java versions.)
    
  - Switch over to the "Dependencies" tab (still inside "Project Structure" / "Modules"), and add
    all the jar-s inside the `ide-dependencies` directory as dependency. (How: Click the "+" icon
    at the right edge, select "JARs or directory", navigate to `ide-dependencies` directory, expand
    it, then range-select all the jars in it. Thus you add all of them at once.) After all jar-s were added,
    find  dom4j-*.jar in the table, and move it to the bottom of the table (otherwise it shadows some
    Jaxen classes with a too old version).

- "File" -> "Settings" -> "Build, Execution, Deployment" -> "Compiler" -> "Excludes":
  Add source files that match these (you simply find them manually, and add their absolute path):  
    _Jython20*.java,  
    _Jython22*.java,  
    _FreeMarkerPageContext2.java,  
    FreeMarkerJspFactory2.java,  
    Java8*.java  

- You may do "Build" / "Build project" (Ctrl+F9) to see if everything compiles now.
    
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
