<%@ taglib uri="http://jakarta.apache.org/struts/tags-html" prefix="html" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/tld/hq.tld" prefix="hq" %>

<%@ page import="java.io.IOException" %>
<%@ page import="java.util.Map" %>
<%@ page import="javax.naming.InitialContext" %>
<%@ page import="javax.persistence.EntityManager" %>
<%@ page import="javax.persistence.EntityManagerFactory" %>
<%@ page import="javax.transaction.TransactionManager" %>
<%@ page import="javax.persistence.Query" %>
<%@ page import="org.rhq.enterprise.gui.legacy.util.RequestUtils" %>
<%@ page import="org.rhq.core.domain.configuration.Configuration" %>
<%@ page import="org.rhq.core.domain.configuration.Property" %>
<%@ page import="org.rhq.core.domain.configuration.PropertySimple" %>
<%@ page import="org.rhq.core.domain.configuration.definition.ConfigurationDefinition" %>
<%@ page import="org.rhq.core.domain.configuration.definition.PropertyDefinition" %>
<%@ page import="org.rhq.core.domain.configuration.definition.PropertyDefinitionSimple" %>
<%@ page import="org.rhq.core.domain.resource.Resource" %>
<%@ page import="org.rhq.enterprise.server.measurement.MeasurementDataManagerLocal" %>
<%@ page import="org.rhq.enterprise.server.measurement.MeasurementScheduleManagerLocal" %>
<%@ page import="org.rhq.enterprise.server.resource.ResourceManagerLocal" %>
<%@ page import="org.rhq.enterprise.server.util.LookupUtil" %>
<%--
  Created by IntelliJ IDEA.
  User: ghinkle
  Date: Mar 13, 2007
  Time: 9:54:15 PM
  To change this template use File | Settings | File Templates.
--%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@page import="org.rhq.enterprise.server.RHQConstants"%>
<html>


<%
   int resourceId = Integer.parseInt(request.getParameter("id"));

   MeasurementScheduleManagerLocal scheduleManager = LookupUtil.getMeasurementScheduleManager();
   MeasurementDataManagerLocal dataManager = LookupUtil.getMeasurementDataManager();
   ResourceManagerLocal resourceManager = LookupUtil.getResourceManager();

   Resource resource = resourceManager.getResourceById(RequestUtils.getSubject(request), resourceId);

   InitialContext ic = new InitialContext();
   TransactionManager tm = (TransactionManager) ic.lookup("java:/TransactionManager");
   tm.begin();
   EntityManager em = ((EntityManagerFactory) ic.lookup(RHQConstants.ENTITY_MANAGER_JNDI_NAME)).createEntityManager();

   Query query = em.createQuery("SELECT md,mdn FROM MeasurementDefinition md JOIN md.schedules AS sch, MeasurementDataNumeric mdn " +
      "WHERE sch.resource.id= :resId and sch.id = mdn.schedule.id " +
      " and mdn.id.timestamp = (select max(mdn2.id.timestamp) FROM MeasurementDataNumeric mdn2 where mdn2.schedule.id = sch.id)");
   query.setParameter("resId", new Integer(resourceId));

   request.setAttribute("resource", resource);
   request.setAttribute("data", query.getResultList());


%>
<head><title>Resource Hack page</title></head>
<body>


<h1>Resource:
   <a href="/admin/browser.jsp?entityClass=org.rhq.core.domain.resource.Resource&key=${resource.id}">${resource.name}</a></h1>


Resource Type: <a href="/admin/browser.jsp?entityClass=org.rhq.core.domain.resource.ResourceType&key=${resource.resourceType.id}">${resource.resourceType}</a>



<h1>Definitions</h1>
<table border="1">
   <tr>
      <th>id</th>
      <th>displayName</th>
      <th>name</th>
      <th>category</th>
      <th>units</th>
      <th>NumericType</th>
      <th>Latest Value</th>
      <th>Last Collection</th>
   </tr>

   <c:forEach var="data" items="${data}">
   <tr>
      <td><c:out value="${data[0].id}"/></td>
      <td><c:out value="${data[0].displayName}"/></td>
      <td><c:out value="${data[0].name}"/></td>
      <td><c:out value="${data[0].category}"/></td>
      <td><c:out value="${data[0].units}"/></td>
      <td><c:out value="${data[0].numericType}"/></td>
      <td><c:out value="${data[1].value}"/></td>
      <td><c:out value="${data[1].timestamp}"/></td>

   </tr>
   </c:forEach>
</table>


<%!
   JspWriter out = null;

   void setOut(JspWriter out)
   {
      this.out = out;
   }

   void display(ConfigurationDefinition definition, Configuration config) throws IOException
   {
      Map<String, PropertyDefinition> defs = definition.getPropertyDefinitions();

      display(defs, config);
   }

   void display( Map<String, PropertyDefinition> defs, Configuration valueSet) throws IOException
   {
      out.write("<table width='100%' border='1'><tr><th>Name</th><th>Value</th><th>Display Name</th><th>Description</th></tr>");
      for (Property value : valueSet.getProperties())
      {
         out.write("<tr>");
         PropertyDefinition def = defs.get(value.getName());

         if (value instanceof PropertySimple)
         {
            out.println("<td><b>" + value.getName() + "</b></td><td>" + ((PropertySimple)value).getStringValue() + "</td>");
            if (def != null)
            {
               out.println("<td>" + def.getDisplayName() + "</td><td>" + def.getDescription() + "</td>");
            }
            else
            {
               out.println("<td>&nbsp;</td><td>&nbsp;</td>");
            }
         }
      }
   }

   void display(PropertyDefinitionSimple property, PropertySimple value) throws IOException
   {
      out.println("<b>" + property.getName() + ": </b>" + value.getStringValue() + "<br/>");
   }

%>
<h2>Plugin Configuration</h2>
<%
   Configuration pluginConfiguration = resource.getPluginConfiguration();
   ConfigurationDefinition definition = resource.getResourceType().getPluginConfigurationDefinition();
   if (definition != null)
   {
      definition = em.find(ConfigurationDefinition.class, definition.getId());
      out.write("&nbsp;&nbsp;Definition Id: " + definition.getId() + "<br/>");
   }
   out.write("&nbsp;&nbsp;Config Id: " + pluginConfiguration.getId() + "<br/>");

   if (definition != null)
   {
      setOut(out);
      display(definition, pluginConfiguration);
   }
%>





<%
   tm.rollback();
%>



</body>
</html>