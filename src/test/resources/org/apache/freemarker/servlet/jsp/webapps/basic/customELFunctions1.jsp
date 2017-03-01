<%--
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
--%>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="ef" uri="http://freemarker.org/test/taglibs/el-functions" %>

${ef:reverse("abc")}
${ef:reverseInt(123)}
${ef:reverseIntRadix(123, 2)}
<%-- Nested type resolution is broken in Jasper: ${ef:hypotenuse(3, 4)}--%>5
${ef:sum(ef:testArray())}
<%-- Not possible in JSP 2.2 EL: ${ef:sum([1, 2, 3])} --%>6
<%-- Not possible in JSP 2.2 EL: ${ef:sum(1, 2, 3)}} --%>6
<%-- Not possible in JSP 2.2 EL: ${ef:sum(1)}} --%>1
${ef:sumMap(ef:testMap())}
<%-- Not possible in JSP 2.2 EL: ${ef.sumMap({ 'a': 1?int, 'b': 2?int, 'c': 3?int })} --%>abc=6