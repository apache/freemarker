<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>

${n + 1}


<%-- JSTL: --%>

<c:if test="${t}">
  True
</c:if>

<c:choose>
  <c:when test="${n == 123}">
      Do this
  </c:when>
  <c:otherwise>
      Do that
  </c:otherwise>
</c:choose>

<c:forEach var="i" items="${ls}">
- ${i}
</c:forEach>

[${fn:trim(" foo ")}]
