<#--
  Licensed to the Apache Software Foundation (ASF) under one
  or more contributor license agreements.  See the NOTICE file
  distributed with this work for additional information
  regarding copyright ownership.  The ASF licenses this file
  to you under the Apache License, Version 2.0 (the
  "License"); you may not use this file except in compliance
  with the License.  You may obtain a copy of the License at
  
    http://www.apache.org/licenses/LICENSE-2.0
  
  Unless required by applicable law or agreed to in writing,
  software distributed under the License is distributed on an
  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
  KIND, either express or implied.  See the License for the
  specific language governing permissions and limitations
  under the License.
-->
<html>
<head>
<title>FreeMarker: Extended Hash Test</title>
</head>
<body>

<p>A simple test follows:</p>

<p>${message}</p>

<p>A hash set of ${animals?size} animals follows:</p>
<assign animalKeys = animals?keys>
<p><foreach animal in animalKeys>
  ${animal}<if animal_has_next>, <else>.</if>
</foreach></p>

<p>The first animal is an ${animalKeys?first}, and the last is a 
${animalKeys?last}.</p>

<p>A hash set of ${animals?size} digits follows:<p>
<assign animalValues = animals._values>
<p><foreach number in animalValues>
  ${number}<if number_has_next>, <else>.</if>
</foreach></p>

<p>The zebra number is ${animals.zebra}.</p>

<p>The end.</p>
</body>
</html>
