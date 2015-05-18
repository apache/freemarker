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