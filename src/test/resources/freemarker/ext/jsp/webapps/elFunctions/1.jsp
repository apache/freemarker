<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="ef" uri="http://freemarker.org/test/taglibs/el-functions" %>

${ef:reverse("abc")}
${ef:reverseInt(123)}
${ef:reverseIntRadix(123, 2)}
<%-- Nested type resolution is broken in Jasper: ${ef:hypotenuse(3, 4)}--%>5
