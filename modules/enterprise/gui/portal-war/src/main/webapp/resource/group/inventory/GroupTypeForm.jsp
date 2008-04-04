<%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-bean" prefix="bean" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-html-el" prefix="html" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-logic" prefix="logic" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/tld/hq.tld" prefix="hq" %>

<script language="JavaScript" type="text/javascript">
var compatibleArr = new Array();
var compatibleCount = 0;
// need to set the total size of the compatbility types
<c:choose>
<c:when test="GroupForm.platformTypeCount > 0 || GroupForm.serverTypeCount > 0 || GroupForm.serviceTypeCount > 0">
    compatibleArr.length = <c:out value="${GroupForm.clusterCount}"/> + 2;
</c:when>
<c:otherwise>
    compatibleArr.length = <c:out value="${GroupForm.clusterCount}"/> + 1;
</c:otherwise>
</c:choose>

// build the compatible types
compatibleArr[0] = new Option ("<fmt:message key="resource.group.inventory.New.props.SelectResourceType"/>", "-1");

<c:if test="${GroupForm.platformTypeCount > 0}">
compatibleArr[1] = new Option ("<fmt:message key="resource.group.inventory.New.props.PlatformType"/>", "-1");

compatibleCount = 1;

<c:forEach var="resType" varStatus="resourceCount" items="${GroupForm.platformTypes}">
  compatibleArr[<c:out value="${resourceCount.count}"/> + 1] = new Option ('<c:out value="${resType.label}"/>',
                '<c:out value="${resType.value}"/>');
  <c:if test="${resourceCount.last}">
      compatibleCount= compatibleCount + <c:out value="${resourceCount.count}"/>;
  </c:if>
</c:forEach>

</c:if>

<c:if test="${GroupForm.serverTypeCount > 0}">

<c:if test="${GroupForm.platformTypeCount > 0 }">
    compatibleArr[compatibleCount + 1] = new Option ("", "-1");
</c:if>

compatibleArr[compatibleCount + 2] = new Option ("<fmt:message key="resource.group.inventory.New.props.ServerType"/>", "-1");

<c:forEach var="resType" varStatus="resourceCount" items="${GroupForm.serverTypes}">
  compatibleArr[<c:out value="${resourceCount.count}"/> + compatibleCount + 2] = 
                                            new Option ('<c:out value="${resType.label}"/>',
                                                        '<c:out value="${resType.value}"/>');
  <c:if test="${resourceCount.last}">
      compatibleCount=compatibleCount+<c:out value="${resourceCount.count}"/>;
  </c:if>
</c:forEach>
compatibleCount = compatibleCount + 2;

</c:if>

<c:if test="${GroupForm.serviceTypeCount > 0}">

<c:if test="${GroupForm.platformTypeCount > 0 || GroupForm.serverTypeCount > 0}">
    compatibleArr[compatibleCount + 1] = new Option ("", "-1");
</c:if>

compatibleArr[compatibleCount + 2] = new Option ("<fmt:message key="resource.group.inventory.New.props.ServiceType"/>", "-1");

<c:forEach var="resType" varStatus="resourceCount" items="${GroupForm.serviceTypes}">
  compatibleArr[<c:out value="${resourceCount.count}"/> + compatibleCount + 2] = 
                                            new Option ('<c:out value="${resType.label}"/>',
                                                        '<c:out value="${resType.value}"/>');
</c:forEach>
</c:if>

var clusterArr = new Array();

// build the mixed types
clusterArr.length=2;

clusterArr[0] = new Option ("<fmt:message key="resource.group.inventory.New.props.SelectResourceType"/>", "-1");
clusterArr[1] = new Option ('<fmt:message key="resource.group.inventory.New.props.GroupOfMixed"/>',
                            '<c:out value="${CONST_ADHOC_PSS}"/>:-1');

var masterArr = new Array ("", compatibleArr, clusterArr);

function changeDropDown (masterSelName, selName, selectVal){
  var masterSel = document.getElementsByName(masterSelName)[0];
  var typeIndex = masterSel.selectedIndex;
  
  var sel = document.getElementsByName(selName)[0];
  sel.options.length = 0;
  
  if (typeIndex == 0)
  {
    // "Select..." has recursive disabled and nested options suppressed
    document.getElementsByName("recursive")[0].disabled = true;
    document.getElementsByName("recursive")[0].checked = false;
    sel.style.display = "none";
  }
    	
  if(typeIndex == 1) 
  {
    // "Compatible Resources" has recursive disabled with nested options
    document.getElementsByName("recursive")[0].disabled = true;
    document.getElementsByName("recursive")[0].checked = false;
    sel.style.display = "block";
  }
  
  if (typeIndex == 2)
  {
    // "Mixed Resources" has recursive enabled, but nested options suppressed
    document.getElementsByName("recursive")[0].disabled = false;
    sel.style.display = "none";
  }
  
  if (typeIndex == 1) {
    sel.options.length = masterArr[typeIndex].length;
    
    for(i=0; i<masterArr[typeIndex].length; i++) {
  		sel.options[i] = masterArr[typeIndex][i];
        if (selectVal != null && sel.options[i].value == selectVal)
            sel.options[i].selected=true;
  	}
  }
}

</script>

<!--  GENERAL PROPERTIES TITLE -->
<tiles:insert definition=".header.tab">
  <tiles:put name="tabKey" value="resource.group.inventory.New.GroupType.Title"/>
</tiles:insert>
<!--  /  -->

<!--  GENERAL PROPERTIES CONTENTS -->
<table width="100%" cellpadding="0" cellspacing="0" border="0">
	<tr>
		<td width="20%" class="BlockLabel" nowrap>
			<html:img page="/images/icon_required.gif" width="9" height="9" border="0"/>
			<fmt:message key="resource.group.inventory.New.Label.Contains"/>
		</td>

<logic:messagesPresent property="category">
    <td class="ErrorField">
	  <html:select property="category" onchange="changeDropDown('category', 'resourceTypeId');">
		<option value="-1"><fmt:message key="resource.common.inventory.props.SelectOption"/></option>
        <html:optionsCollection property="groupTypes" />
        <br>
      </html:select>
</logic:messagesPresent>
<logic:messagesNotPresent property="category">
      <td class="BlockContent">
          <html:select property="category" onchange="changeDropDown('category', 'resourceTypeId');" >
            <option value="-1"><fmt:message key="resource.common.inventory.props.SelectOption"/></option>
            <html:optionsCollection property="groupTypes" />
          </html:select>
      </td>
</logic:messagesNotPresent>
      
<logic:messagesNotPresent property="resourceTypeId">
      <td width="80%" class="BlockContent">      
          <html:select property="resourceTypeId"/>
          <script language="JavaScript" type="text/javascript">
            document.getElementsByName("resourceTypeId")[0].style.display = "none";
          </script>
      </td>
</logic:messagesNotPresent>
<logic:messagesPresent property="resourceTypeId">
      <td width="80%" class="ErrorField">      
          <html:select property="resourceTypeId"/>
          <script language="JavaScript" type="text/javascript">
            document.getElementsByName("resourceTypeId")[0].style.display = "none";
          </script>
      </td>          
</logic:messagesPresent>
	</tr>
	
	
<logic:messagesPresent property="category">
	<tr>
      <td width="20%" class="BlockLabel" nowrap>&nbsp;</td>
      <td width="20%" class="ErrorField">
          <span class="ErrorFieldContent">- <html:errors property="category"/></span>
      </td>
      <td width="60%" class="BlockContent">&nbsp;</td>
    </tr>
</logic:messagesPresent>      
<logic:messagesPresent property="resourceTypeId">
	<tr>
      <td width="20%" class="BlockLabel" nowrap>&nbsp;</td>
      <td width="20%" class="BlockContent">&nbsp;</td>
      <td width="60%" class="ErrorField">
          &nbsp;
          <span class="ErrorFieldContent">- <html:errors property="resourceTypeId"/></span>
      </td>
    </tr>
</logic:messagesPresent>
   <tr>
    <td width="20%" class="BlockLabel">
		<fmt:message key="resource.common.inventory.props.RecursiveLabel"/>
    </td>
    <td colspan="3" class="BlockContent">
      <html:checkbox property="recursive" />
    </td>
  </tr>
   <tr>
      <td colspan="4" class="BlockBottomLine"><html:img page="/images/spacer.gif" width="1" height="1" border="0"/></td>
    </tr>
</table>

<script language="JavaScript" type="text/javascript">
  changeDropDown('category', 'resourceTypeId','<c:out value="${GroupForm.resourceTypeId}"/>');
</script>
<!--  /  -->