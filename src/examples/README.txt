/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
 
Examples
========

Examples for using FreeMarker (FM):

webapp1        Building Web app. framework around FM, step 1: Hello World!
webapp2        Building Web app. framework around FM, step 2: Guest-book
struts-webapp  Using FM in a Model-2 framework (Struts 1.1) with JSP taglibs
ant            Demonstrates the FreeMarker XML transform Ant-task

To build the example webapps, you need Ant (http://ant.apache.org/) and run
the following commands from the base FreeMarker directory:

  ant example-<example-name>

where <example-name> is the name of the example as struts-webapp or webapp1.
The built Web applications will be in the build/examples directory.

For more information read the help.html or README.txt in the directory of
examples.
