Examples
========

Examples for using FreeMarker (FM):

webapp1        Building Web app. framework around FM, step 1: Hello World!
webapp2        Building Web app. framework around FM, step 2: Guest-book
struts-webapp  Using FM in a Model-2 framework (Struts 1.1) with JSP taglibs
jython-webapp  Web app. that uses Jython instead of Java Language
ant            The FreeMarker XML transform Ant-task

To build the example webapps, you need Ant (http://ant.apache.org/) and run
the following commands from the base FreeMarker directory:

  ant example-<example-name>

where <example-name> is the name of the example as struts-webapp or webapp1.
The built Web applications will be in the build/examples directory.

For more information read the help.html or README.txt in the directory of
examples.

Note that you can build all Web applications using the single command:

  ant examples
