<%@ page import="java.io.ByteArrayOutputStream" %>
<%@ page import="java.io.PrintStream" %>
<%@ page import="java.util.Map" %>

<%@ page import="org.rhq.core.domain.auth.Subject" %>
<%@ page import="org.rhq.core.domain.server.PersistenceUtility" %>

<%@ page import="org.rhq.enterprise.gui.legacy.util.SessionUtils"%>
<%@ page import="org.rhq.enterprise.gui.util.WebUtility"%>

<%@ page import="org.rhq.enterprise.server.test.TestLocal" %>
<%@ page import="org.rhq.enterprise.server.measurement.MeasurementBaselineManagerLocal" %>
<%@ page import="org.rhq.enterprise.server.measurement.MeasurementScheduleManagerLocal" %>
<%@ page import="org.rhq.enterprise.server.core.AgentManagerLocal" %>
<%@ page import="org.rhq.enterprise.server.system.SystemManagerLocal" %>
<%@ page import="org.rhq.enterprise.server.auth.SubjectManagerLocal" %>
<%@ page import="org.rhq.enterprise.server.support.SupportManagerLocal" %>
<%@ page import="org.rhq.enterprise.server.util.LookupUtil" %>
<%@ page import="org.rhq.enterprise.server.scheduler.jobs.DataPurgeJob"%>
<%@ page import="org.rhq.enterprise.server.scheduler.jobs.DataCalcJob"%>

<%@ page import="javax.naming.NamingException" %>

<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<html>
<head><title>RHQ Test Control Page</title></head>
<body>

<jsp:include page="/admin/include/adminTestLinks.html" flush="true" />

<%
   TestLocal coreTestBean;
   MeasurementBaselineManagerLocal measurementBaselineManager;
   MeasurementScheduleManagerLocal measurementScheduleManager;
   AgentManagerLocal agentManager;
   SystemManagerLocal systemManager;
   SubjectManagerLocal subjectManager;
   SupportManagerLocal supportManager;

   coreTestBean = LookupUtil.getTest();
   measurementBaselineManager = LookupUtil.getMeasurementBaselineManager();
   measurementScheduleManager = LookupUtil.getMeasurementScheduleManager();
   agentManager = LookupUtil.getAgentManager();
   systemManager = LookupUtil.getSystemManager();
   subjectManager = LookupUtil.getSubjectManager();
   supportManager = LookupUtil.getSupportManager();

   String result = null;   
   String resultNoEscape = null;   
   String mode = pageContext.getRequest().getParameter("mode");
   String failure = null;
   try
   {
      if ("calculateAutoBaselines".equals(mode))
      {
         measurementBaselineManager.calculateAutoBaselines();
         result = "Calculate auto-baselines done";
      }
      else if ("calculateOOBs".equals(mode))
      {
          result = "Cannot calculate OOBs currently";
      }
      else if ("checkForSuspectAgents".equals(mode))
      {
         agentManager.checkForSuspectAgents();
         result = "Check for suspect agents done";
      }
      else if ("dataPurgeJob".equals(mode))
      {
         DataPurgeJob.purgeNow();
         result = "Data purge done";
      }
      else if ("dataCalcJob".equals(mode))
      {
         DataCalcJob.calcNow();
         result = "Data calc done";
      }
      else if ("dbMaintenance".equals(mode))
      {
         systemManager.vacuum(subjectManager.getOverlord());
         result = "DB vacuum done... ";
         systemManager.reindex(subjectManager.getOverlord());
         result += "DB reindex done... ";
         systemManager.analyze(subjectManager.getOverlord());
         result += "DB analyze done";
      }
      else if ("errorCorrectSchedules".equals(mode))
      {
         measurementScheduleManager.errorCorrectSchedules();
         result = "Measurement schedules checked for invalid (too low) intervals and corrected if need be";
      }
      else if ("generateSnapshotReport".equals(mode))
      {
         int resourceId = Integer.parseInt(request.getParameter("resourceId"));
         String name = request.getParameter("name");
         String description = request.getParameter("description");
         java.net.URL url = supportManager.getSnapshotReport(subjectManager.getOverlord(), resourceId, name, description);
         result = "Snapshot Report is located here: " + url.toString();
      }
      else if ("getMeasurementTableStats".equals(mode))
      {
         resultNoEscape = "<table>";
         Map<String, Long> tableCounts = coreTestBean.getMeasurementTableStats();
         for (Map.Entry<String, Long> nextCount : tableCounts.entrySet()) {
             String tableAlias = nextCount.getKey();
             Long tableCount = nextCount.getValue();
             resultNoEscape += "<tr><td>" + tableAlias + "</td><td>" + tableCount + "</td></tr>";
         }
         resultNoEscape += "</table>"; 
      }
      else if ("enableHibernateStats".equals(mode))
      {
          coreTestBean.enableHibernateStatistics();
          result = "Started Hibernate statistics collection";
      }
      else if ("disableHibernateStats".equals(mode))
      {
          coreTestBean.disableHibernateStatistics();
          result = "Stopped Hibernate statistics collection";
      }
   }
   catch (Exception e)
   {
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      e.printStackTrace(new PrintStream(baos));
      failure = baos.toString();
   }

   pageContext.setAttribute("executed", mode);
   pageContext.setAttribute("result", result);
   pageContext.setAttribute("resultNoEscape", resultNoEscape);   
   pageContext.setAttribute("failure", failure);
%>

<c:if test="${executed != null}">
   <b>Executed <c:out value="${executed}"/></b>

   <c:if test="${result != null}">
      : <c:out value="${result}"/><br/>
   </c:if>

   <c:if test="${resultNoEscape != null}">
      : ${resultNoEscape} <br/>
   </c:if>
   <br/>
   <c:if test="${failure != null}">
      <pre style="background-color: yellow;"><c:out value="${failure}"/></pre>
   </c:if>   
</c:if>

<h2>Administration Controls</h2>

<ul>
  <li><c:url var="url" value="/admin/test/control.jsp?mode=dbMaintenance"/>
      <a href="<c:out value="${url}"/>">Perform Database Maintenance (vacuum/reindex/analyze)</a></li>
  <li><c:url var="url" value="/admin/test/control.jsp?mode=calculateAutoBaselines"/>
      <a href="<c:out value="${url}"/>">Calculate Auto Baselines</a></li>
  <li><c:url var="url" value="/admin/test/control.jsp?mode=checkForSuspectAgents"/>
      <a href="<c:out value="${url}"/>">Check For Suspect Agents</a></li>
  <li><c:url var="url" value="/admin/test/control.jsp?mode=dataPurgeJob"/>
      <a href="<c:out value="${url}"/>">Perform Data Purge</a></li>
  <li><c:url var="url" value="/admin/test/control.jsp?mode=dataCalcJob"/>
      <a href="<c:out value="${url}"/>">Perform Data Calc</a></li>
  <li><c:url var="url" value="/admin/test/control.jsp?mode=calculateOOBs"/>
      <a href="<c:out value="${url}"/>">Calculate OOBs</a></li>
  <li><c:url var="url" value="/admin/test/control.jsp?mode=errorCorrectSchedules"/>
      <a href="<c:out value="${url}"/>">Correct Schedule Collection Intervals</a></li>
  <li><c:url var="url" value="/admin/test/control.jsp?mode=getMeasurementTableStats"/>
      <a href="<c:out value="${url}"/>">Get Measurement Tables Stats</a></li>
  <li><c:url var="url" value="/admin/test/control.jsp?mode=enableHibernateStats"/>
      <a href="<c:out value="${url}"/>">Start Hibernate Statistics Collection</a></li>    
  <li><c:url var="url" value="/admin/test/control.jsp?mode=disableHibernateStats"/>
      <a href="<c:out value="${url}"/>">Stop Hibernate Statistics Collection</a></li>    
</ul>

<h2>Snapshot Report</h2>

<c:url var="url" value="/admin/test/control.jsp?mode=generateSnapshotReport"/>
Generate Snapshot Report
<form action="<c:out value="${url}"/>" method="get">
   <input type="hidden" name="mode" value="generateSnapshotReport"/>
   Resource ID: <input type="text" name="resourceId" size="10"/><br/>
   Name: <input type="text" name="name" size="30"/><br/>
   Description: <input type="text" name="description" size="100"/><br/>
   <input type="submit" value="Generate Snapshot" name="Generate Snapshot"/>
</form>

</body>
</html>
