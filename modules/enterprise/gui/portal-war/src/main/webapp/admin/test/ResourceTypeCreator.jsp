<%@ page language="java" contentType="text/html; charset=ISO-8859-1" pageEncoding="ISO-8859-1"%>
<%@page import="javax.naming.*"%>
<%@page import="javax.ejb.*"%>
<%@page import="java.rmi.*"%>
<%@page import="java.util.*"%>
<%@page import="org.rhq.enterprise.server.resource.metadata.*"%>
<%@page import="org.rhq.core.domain.plugin.*"%>

<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">

<%@page import="org.rhq.core.domain.resource.ResourceType"%>
<%@page import="org.rhq.core.domain.resource.ResourceCategory"%>
<%@page import="javax.persistence.NoResultException"%>
<%@page import="org.rhq.enterprise.server.resource.ResourceTypeManagerLocal"%>
<%@page import="org.rhq.enterprise.server.resource.ResourceTypeManagerBean"%>
<%@page import="org.apache.commons.logging.*"%>

<%@page import="org.rhq.enterprise.server.util.LookupUtil"%><html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
<title>ResourceTypeCreator</title>
</head>
<body>
	<h1>ResourceTypeCreator JSP Page</h1>
	
	<form action="ResourceTypeCreator.jsp" method="post">
		
		Enter the name of the new RessourceType: 
		<input type="text" name="resourceTypeName" width="150px" height="35px"><br>
		Enter the name of first metric: 
		<input type="text" name="resourceTypeMetric1Input" width="150px" height="35px"><br> 
		<!-- 
		Enter the name of second metric: 
		<input type="text" name="resourceTypeMetric2Input" width="150px" height="35px"><br>
		Enter the name third metric: 
		<input type="text" name="resourceTypeMetric3Input" width="150px" height="35px"><br>
 		-->

		<input type="submit" value="Create new ResourceType">

	</form>
	
	<%
		/** Creation of parameters for the new ResourceType */
		String resourceTypeName = request.getParameter("resourceTypeName");
		String metricName = request.getParameter("resourceTypeMetric1Input");
		
		if( resourceTypeName != null )
		{
			//After name of new resourceType has been given
			LookupUtil.getResourceMetadataManager().addNewResourceType(resourceTypeName, metricName);
		}
		else
		{
			out.println("Please insert the name of the new ResourceType");
		}
	%>
	
</body>
</html>