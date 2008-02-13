<%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-bean" prefix="bean" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-html-el" prefix="html" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/tld/hq.tld" prefix="hq" %>

<div class="effectsPortlet">
<!-- Content Block Contents -->
<tiles:importAttribute name="resources"/>
<tiles:insert definition=".header.tab">
  <tiles:put name="tabKey" value="dash.home.AutoDiscovery"/>  
  <tiles:put name="adminUrl" beanName="adminUrl" />
  <tiles:put name="portletName" beanName="portletName" />
  <tiles:put name="showRefresh" beanName="showRefresh" />  
</tiles:insert>

<script language="JavaScript" type="text/javascript">

// If the user has not checked a platform, uncheck all its servers.
// This may be obsolete with the introduction of setPlatformCheckbox
// but it doesn't hurt to do this as a final step before submitting the form.
function setImportCheckboxes (cbform)
{
  var pcChecked = false;
  for (var i=0; i<cbform.elements.length; i++)
  {
    if (cbform.elements[i].name == 'platformsToProcess')
    {
      pcChecked = cbform.elements[i].checked;
      continue;
    }

    if (cbform.elements[i].name == 'serversToProcess' && cbform.elements[i].checked)
    {
       cbform.elements[i].checked = pcChecked;
    }
  }
}

// when you select a server, the platform will be automatically selected if it isn't already
function setPlatformCheckbox(cbform, serverCheckbox)
{
   if (serverCheckbox.checked)
   {
      var platform;
      for (var i=0; i<cbform.elements.length; i++)
      {
        if (cbform.elements[i].name == 'platformsToProcess')
        {
           platform = cbform.elements[i];
           continue;
        }

        if (cbform.elements[i] == serverCheckbox)
        {
           platform.checked = true;
           return;
        }
      }
   }
}

// given a platform checkbox, it sets the servers to the state of the platform checkbox
function setAllServers(cbform, platformCheckbox)
{
   var checked = platformCheckbox.checked;
   var found_it = false;
   
   for (var i=0; i<cbform.elements.length; i++)
   {
      if (cbform.elements[i].name == 'platformsToProcess')
      {
         if (found_it)
            return; // we got to the next platform, we can stop now

         if (cbform.elements[i] == platformCheckbox)
         {
            found_it = true;
            continue;
         }
      }

      if (cbform.elements[i].name == 'serversToProcess' && found_it)
      {
         cbform.elements[i].checked = checked;
      }
   }
}

// this will (un)collapse platforms so show/hide their server children
function collapseExpandPlatform(platformId)
{
   var newStyleDisplay;
   var arrow = document.getElementById('arrow_' + platformId);
   
   // if icon is a minus sign, we are expanded, so we need to collapse
   // if icon is a plus sign, it means we are collapsed and need to be expanded
   if ( arrow.src.indexOf('minus') > -1 )
   {
      newStyleDisplay = 'none';
      arrow.src = '/images/plus.gif';
   }
   else
   {
      newStyleDisplay = 'block';
      arrow.src = '/images/minus.gif';
   }
   
   var all_elements = document.getElementsByTagName("span")
   for (var i=0; i < all_elements.length; i++)
   {
      if (all_elements[i].id.startsWith('serverOnPlatform_' + platformId))
         all_elements[i].style.display = newStyleDisplay;
   }
  
   return false;
}

</script>

<tiles:insert definition=".portlet.error"/>

    <c:choose>
      <c:when test="${empty resources}">
        <table width="100%" cellpadding="0" cellspacing="0" border="0">
          <c:choose>
            <c:when test="${hasNoAgents}">
              <tr class="ListRow">
                 <td class="ListCellHeader"><fmt:message key="dash.home.inventory.empty"/></td>
              </tr>
            </c:when>
            <c:otherwise>
              <tr class="ListRow">
                 <td class="ListCell"><fmt:message key="dash.home.no.resource.to.display"/></td>
              </tr>
              <tr class="ListRow">
                 <c:if test="${useroperations['MANAGE_INVENTORY']}">
                 <td class="ListCell"><a href="/rhq/discovery/queue.xhtml"/><html:img page="/images/tbb_viewall.gif" border="0"/></a></td>
                 </c:if>
              </tr>
            </c:otherwise>
          </c:choose>
        </table>
      </c:when>
      <c:otherwise>
        <html:form action="/dashboard/ProcessAutoDiscovery">
        <html:hidden property="queueAction"/>
        <c:forEach items="${resources}" var="resource">
          <table width="100%" cellpadding="0" cellspacing="0" border="0">
            <c:set var="platform" value="${resource.key}"/>
            <tr class="ListRowHeader">
              <td nowrap class="ListCell" align="left" width="5%"><a href="." onclick="return false;"><img id="arrow_${platform.id}" align="left" vspace="4" src="/images/plus.gif" onclick="collapseExpandPlatform('${platform.id}')"/></a><html:multibox property="platformsToProcess" value="${platform.id}" onclick="setAllServers(AIQueueForm,this);"/>&nbsp;</td>
              <td class="ListCell" align="left">
                  <c:out value="${platform.name}"/>
                  <c:choose>
                    <c:when test="${empty platform.description}">
                    - <c:out value="${platform.resourceType.name}"/>
                    </c:when>
                    <c:otherwise>
                    - <c:out value="${platform.description}"/>
                    </c:otherwise>
                  </c:choose>
              </td>
              <td nowrap class="ListCell" align="right"><c:out value="${platform.inventoryStatus}"/></td>
            </tr>
          </table>

          <span style="display: none" id="serverOnPlatform_${platform.id}">
          <table width="100%" cellpadding="0" cellspacing="0" border="0" >
            <c:forEach items="${resource.value}" var="server">
            <hq:constant classname="org.rhq.core.domain.resource.InventoryStatus" symbol="NEW" var="status_new"/>
            <c:if test="${server.inventoryStatus == status_new}">
              <tr class="ListRow" >
                <hq:shortenPath property="shortenedInstallPath" value="${server.resourceKey}" preChars="20" postChars="25"/>
                <td nowrap class="ListCell" align="left" width="5%">&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<html:multibox property="serversToProcess" value="${server.id}" onclick="setPlatformCheckbox(AIQueueForm,this);"/></td>
                <td nowrap class="ListCell" align="left"><hq:removePrefix prefix="IGNOREME__${platform.name}" value="${server.name}"/></td>
                <td nowrap class="ListCell" align="left">
                  <c:choose>
                    <c:when test="${wasShortened}">
                      <a href="." onclick="return false;" class="ListCellPopup2">
                        <c:out value="${shortenedInstallPath}"/>
                        <span><c:out value="${server.resourceKey}"/></span>
                      </a>
                    </c:when>
                    <c:otherwise>
                      <c:out value="${server.resourceKey}"/>
                    </c:otherwise>
                  </c:choose>
                </td>
                <td nowrap class="ListCell" align="right"><c:out value="${server.inventoryStatus}"/></td>
              </tr>
            </c:if>
            </c:forEach>
          </table>
          </span>
        </c:forEach>
        
        <c:if test="${useroperations['MANAGE_INVENTORY']}">
        <table width="100%" cellpadding="0" cellspacing="0" border="0">
          <tr class="ListRow">
            <td class="ListCell">
              <a href="." onclick="setImportCheckboxes(AIQueueForm); AIQueueForm.queueAction.value=<hq:constant classname="org.rhq.enterprise.gui.legacy.portlet.autodiscovery.AIQueueForm" symbol="Q_DECISION_APPROVE"/>; AIQueueForm.submit(); return false;"><html:img page="/images/tbb_import.gif" border="0"/></a>
              &nbsp;&nbsp;
              <a href="." onclick="AIQueueForm.queueAction.value=<hq:constant classname="org.rhq.enterprise.gui.legacy.portlet.autodiscovery.AIQueueForm" symbol="Q_DECISION_IGNORE"/>; AIQueueForm.submit(); return false;"><html:img page="/images/tbb_ignore.gif" border="0"/></a>
              &nbsp;&nbsp;
              <a href="/rhq/discovery/queue.xhtml"/><html:img page="/images/tbb_viewall.gif" border="0"/></a>
            </td>
          </tr>
        </table>
        </c:if>

        </html:form>
      </c:otherwise>

    </c:choose>
<tiles:insert definition=".dashContent.seeAll"/>
<!-- / -->
</div>
