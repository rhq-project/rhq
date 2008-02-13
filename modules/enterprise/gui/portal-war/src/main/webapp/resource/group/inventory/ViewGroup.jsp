<%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-bean" prefix="bean" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-html-el" prefix="html" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-logic" prefix="logic" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/tld/hq.tld" prefix="hq" %>

<!-- CONSTANT DEFINITIONS -->
<hq:constant
    classname="org.rhq.enterprise.gui.legacy.Constants"
    symbol="CONTROL_ENABLED_ATTR" var="CONST_CONTROLLABLE" /> 

<c:set var="canControl" value="${requestScope[CONST_CONTROLLABLE]}"/>
<c:url var="selfAction" value="/resource/group/Inventory.do?mode=view&groupId=${group.id}"/>
    
<!-- TITLE BAR -->
<tiles:insert definition=".page.title.resource.group.full">
    <tiles:put name="titleName" beanName="TitleParam"/>
    <tiles:put name="group" beanName="group"/>
</tiles:insert>

<!-- CONTROL BAR -->
<c:choose>
    <c:when test="${category == 'MIXED'}"> 
        <tiles:insert definition=".tabs.resource.group.inventory.inventoryonly">
            <tiles:put name="group" beanName="group" />
            <tiles:put name="id" beanName="group" beanProperty="id"/>
        </tiles:insert>
    </c:when> 
    <c:when test="${ canControl }"> 
        <tiles:insert definition=".tabs.resource.group.inventory">
            <tiles:put name="group" beanName="group" />
            <tiles:put name="id" beanName="group" beanProperty="id"/>
        </tiles:insert>
    </c:when>
    <c:otherwise>
        <tiles:insert definition=".tabs.resource.group.inventory.nocontrol">
            <tiles:put name="group" beanName="group" />
            <tiles:put name="id" beanName="group" beanProperty="id"/>
        </tiles:insert>
    </c:otherwise>
</c:choose>

&nbsp;<br>

<tiles:insert definition=".portlet.confirm"/>
<tiles:insert definition=".portlet.error"/>

<tiles:insert definition=".resource.group.inventory.generalProperties.view">
  <tiles:put name="group" beanName="group"/>
</tiles:insert>

&nbsp;<br>

<!-- RESOURCE COUNTS SECTION -->
<tiles:insert page="/resource/group/inventory/ResourceCounts.jsp">
    <tiles:put name="resourceCount" beanName="NumChildResources" />
    <tiles:put name="resourceTypeMap" beanName="ResourceTypeMap"/>
</tiles:insert>

&nbsp;<br>

<!-- LIST RESOURCES SECTION -->
<tiles:insert page="/resource/group/inventory/ListResources.jsp"/>

<!-- FOOTER SECTION -->
<tiles:insert definition=".page.footer"/>


