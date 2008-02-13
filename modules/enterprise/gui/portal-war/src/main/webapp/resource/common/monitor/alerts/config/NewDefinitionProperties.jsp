<%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-tiles" prefix="tiles" %>

<!-- Content Block Title: Properties -->
<tiles:insert definition=".header.tab">
<tiles:put name="tabKey" value="alert.config.props.Title"/>
</tiles:insert>

<tiles:insert definition=".events.config.properties"/>
