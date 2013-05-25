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

FreeMarker is licensed under a liberal BSD-style open source license. 

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

See LICENSE.txt for more details.


Documentation
-------------

Documentation is available in the docs directory of this distribution;
open docs/index.html.

Or, online: http://freemarker.org/docs/
(Mirror: http://freemarker.sourceforge.net/docs/)


Installing
----------

If you are using Maven, just add this dependency:

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

You need Apache Ant and Ivy installed. (As of this writing it was
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

For development under Eclipse, you will need IvyDE installed. You are
advised to use the Eclipse project files included (otherwise set up
IvyDE to use the "IDE" configuration and the included
ivysettings.xml). Note that IvyDE will not find the dependencies until
"ant update-deps" has run once, because it uses the mini-repository
built by that Ant task. If you change the dependencies in ivy.xml, you
will have to run "ant update-deps" again, and only after that tell
IvyDE to resolve the dependencies.


Change log
----------

See:
docs/docs/app_versions.html

Or, online:
http://freemarker.org/docs/app_versions.html
(Mirror: http://freemarker.sourceforge.net/docs/app_versions.html)