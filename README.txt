===============================================================================

 FreeMarker {version}

 For the latest version or to report bugs visit:

 http://freemarker.org/
 (Mirror: http://freemarker.sourceforge.net/)

===============================================================================


What is FreeMarker?
-------------------

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

See LICENSE.txt for more details.


Documentation
-------------

The full documentation is available offline in this distribution, here:
documentation/index.html

Or, you can read it online: http://freemarker.org/docs/
(Mirror: http://freemarker.sourceforge.net/docs/)


Installing
----------

If you are using Maven, just add this dependency:

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

Otherwise simply copy freemarker.jar to a location where your Java
application's ClassLoader will find it. For example, if you are using
FreeMarker in a web application, you probably want to put
freemarker.jar into the WEB-INF/lib directory of your web application.

FreeMarker has no required dependencies. It has several optional
dependencies, but usually you don't have to deal with them, because if
you are using an optional feature that's certainly because your
application already uses the related library.


Building
--------

You need Apache Ant and Ivy be installed. (As of this writing it was
tested with Ant 1.8.1 and Ivy 2.3.0.)

If you need to ensure compliance with certain J2SE versions, copy
build.properties.sample into build.properties, and edit it
accordingly.

To build freemarker.jar, just issue "ant" in the project root
directory, and it should download all dependencies automatically and
build freemarker.jar.

If later you change the dependencies in ivy.xml, or otherwise want to
re-download some of them, it will not happen automatically anymore.
You have to issue "ant update-deps" for that.


Eclipse and other IDE-s
-----------------------

Run "ant ide-dependencies"; This will create an "ide-dependencies" library
that contains all the jars that you have to add to the classpath in the IDE.
Note that here we assume that you have run the build or at least
"ant update-deps" earlier. 

Known issue with workaround: An old org.jaxen is included in dom4j-*.jar,
which conflicts with jaxen-*.jar. If dom4j wins, your IDE will show some
errors in the XML related parts. To fix that, always add dom4j-*.jar last.

You could also use IvyDE instead, with configuration "IDE", but as the
dependencies hardly ever change, it might not worth the trouble.


Change log
----------

Open documentation/index.html, and you will find the link.

Online:
http://freemarker.org/docs/app_versions.html
(Mirror: http://freemarker.sourceforge.net/docs/app_versions.html)