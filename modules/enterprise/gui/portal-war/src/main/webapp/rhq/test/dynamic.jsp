<%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>

<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h" %>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f" %>
<%@ taglib uri="http://richfaces.ajax4jsf.org/rich" prefix="rich" %>

<html>
	<head>
		<title>dynamic richfaces tree w/struts</title>
	</head>
	<body>

		<h3>Your Resource Forest</h3>

	    <f:view>
	    	<h:form>
				<rich:tree value="#{forest.data}" var="resource">
					<rich:treeNode type="ResourceNodeType">
						<h:outputText value="#{resource.name}" />
					</rich:treeNode>
				</rich:tree>
			</h:form>
		</f:view>

	</body>
</html>