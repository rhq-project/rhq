<%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-html-el" prefix="html" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-logic" prefix="logic" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<tiles:importAttribute name="formName"/>

<!-- Content Block Title -->
<tiles:insert definition=".header.tab">
<tiles:put name="tabKey" value="alert.config.props.CondBox"/>
</tiles:insert>

<script language="JavaScript" type="text/javascript">
var baselines = {<c:forEach var="baseline" items="${baselines}">
<c:out value="${baseline.value}"/>: new Array(<c:forEach var="lv" items="${baseline.relatedOptions}">{value: '<c:out value="${lv.value}"/>', label: '<c:out value="${lv.label}"/>'},</c:forEach>{ignore: 'ignore'}),</c:forEach>
ignore: 'ignore'
};
</script>

<script language="JavaScript" src="<html:rewrite page='/js/alertConfigFunctions.js'/>" type="text/javascript"></script>

<table width="100%" border="0" cellspacing="0" cellpadding="0">

  <logic:messagesPresent property="global">
  <tr>
    <td colspan="2" class="ErrorField">
      <span class="ErrorFieldContent">
        <html:errors property="global" />
      </span>
    </td>
  </tr>
  </logic:messagesPresent>

  <logic:messagesPresent property="condition[0].trigger">
  <tr>
    <td colspan="2" class="ErrorField">
      <span class="ErrorFieldContent">
        <html:errors property="condition[0].trigger"/>
      </span>
    </td>
  </tr>
  </logic:messagesPresent>
  
  <tiles:insert definition=".events.config.conditions.condition.expression">
    <tiles:put name="formName"><c:out value="${formName}"/></tiles:put>
  </tiles:insert>
  
  <tr>
    <td colspan="2" class="BlockBottomLine"><html:img page="/images/spacer.gif" width="1" height="1" border="0"/></td>
  </tr>
  
  <tiles:insert definition=".events.config.conditions.condition">
    <tiles:put name="formName"><c:out value="${formName}"/></tiles:put>
  </tiles:insert>

  <tr>
    <td colspan="2" class="BlockBottomLine"><html:img page="/images/spacer.gif" width="1" height="1" border="0"/></td>
  </tr>

  <tiles:insert definition=".events.config.conditions.enablement"/>
  <!--  
  <tiles:insert definition=".events.config.template.cascade"/>
  -->
</table>
