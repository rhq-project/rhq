
<%@page import="org.rhq.enterprise.gui.configuration.resource.ExistingResourceConfigurationUIBean"%><%
	response.setContentType("text/plain");
	Object o =	request.getSession().getValue("ExistingResourceConfigurationUIBean");
	if ( o == null){
		response.getWriter().append("Error message goes here.");	
	}else{
		ExistingResourceConfigurationUIBean existingResourceConfigurationUIBean = (ExistingResourceConfigurationUIBean)o; 
		response.setHeader("Content-Disposition", "attachment;filename="+existingResourceConfigurationUIBean.getCurrentPath()); 
		response.getWriter().append(new String(existingResourceConfigurationUIBean.getCurrentContents()));
	}%>
