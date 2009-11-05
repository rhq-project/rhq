<%@page import="org.rhq.enterprise.gui.configuration.resource.RawConfigCollection"%><%
	response.setContentType("text/plain");
	Object o =	request.getSession().getValue("RawConfigCollection");
	if ( o == null){
		response.getWriter().append("Error message goes here.");	
	}else{
		RawConfigCollection rawConfigCollection = (RawConfigCollection)o; 
		response.setHeader("Content-Disposition", "attachment;filename="+rawConfigCollection.getCurrentPath()); 
		response.getWriter().append(new String(rawConfigCollection.getCurrentContents()));
	}%>
