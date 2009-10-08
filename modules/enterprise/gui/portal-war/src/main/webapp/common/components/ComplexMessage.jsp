<%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-html-el" prefix="html" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<tiles:importAttribute name="complexMsg"/>

<c:forEach items="${complexMsg}" var="elt">

  <c:choose>

    <c:when test="${elt.url == null}">

      <c:choose>
        <c:when test="${elt.mouseover == null}">

          <%-- no url, no mouseover --%>
          <fmt:message key="${elt.text}">
            <c:forEach items="${elt.params}" var="p">
              <fmt:param value="${p}"/>
            </c:forEach>
          </fmt:message>

        </c:when>

        <c:otherwise>
          <%-- no url, has mouseover --%>
          <html:link href="." onclick="return false;" styleClass="ListCellPopup1">
            <fmt:message key="${elt.text}">
              <c:forEach items="${elt.params}" var="p">
                <fmt:param value="${p}"/>
              </c:forEach>
            </fmt:message>
            <span><c:out value="${elt.mouseover}"/></span>
          </html:link>
        </c:otherwise>
      </c:choose>

    </c:when>

    <c:otherwise>

      <c:choose>
        <c:when test="${elt.mouseover == null}">

          <%-- no mouseover, has url --%>
          <html:link page="${elt.url}">
            <fmt:message key="${elt.text}">
            <c:forEach items="elt.params" var="p">
              <fmt:param value="${p}"/>
            </c:forEach>
            </fmt:message>
          </html:link>

        </c:when>

        <c:otherwise> 

          <%-- has url and mouseover --%>
          <html:link page="${elt.url}" styleClass="ListCellPopup3">
            <fmt:message key="${elt.text}">
            <c:forEach items="elt.params" var="p">
              <fmt:param value="${p}"/>
            </c:forEach>
            </fmt:message>
            <span><c:out value="${elt.mouseover}"/></span>
          </html:link>

        </c:otherwise>
      </c:choose>

   </c:otherwise>

  </c:choose>

</c:forEach>
