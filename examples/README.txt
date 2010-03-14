Examples
========

Examples for FreeMarker (FM).


Web application examples
------------------------

webapp1        Building Web app. framework around FM, step 1: Hello World!
webapp2        Building Web app. framework around FM, step 2: Guest-book
struts-webapp  Using FM with Struts 1.1 Web app. framework: Guest-book
jython-webapp  Web app. that uses Jython instead of Java Language
jsp-webapp     Embedding FM templates into JSP pages

In order not to duplicate too much JAR files around, we have not fully built
the example webapps. To build the example webapps, you need Ant
(http://ant.apache.org/) and run the following commands from the base
FreeMarker directory:

  ant example-<example-name>

where <example-name> is the name of the example as struts-webapp or webapp1.
The built Web applications will be in the build/examples directory.

You must create and edit dependencies.properties of the base FreeMarker
directory, or the above will fail. An example of dependencies.properties:

  lib.servlet=/home/java/servlet/servlet2_3_jsp1_2.jar
  lib.struts=/home/java/strtus1_1/lib/struts.jar
  jython.home=/home/java/jython

You need the lib.struts line only if you need the struts-webapp, and you need
the jython.home line only if you need the jython-webapp.

For more information please read the help.html or README.txt in the directory
of examples. Especially, please read these files if the Web application does
not show up after deployment.

Note that you can build all Web applications using the single command:
ant examples


Other examples
--------------

ant            The FreeMarker XML transform Ant-task