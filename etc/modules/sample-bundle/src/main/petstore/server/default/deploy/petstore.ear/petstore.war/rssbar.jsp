<%-- Copyright 2006 Sun Microsystems, Inc. All rights reserved. You may not modify, use, reproduce, or distribute this software except in compliance with the terms of the License at: http://developer.sun.com/berkeley_license.html
$Id: rssbar.jsp,v 1.7 2006/05/06 01:44:59 yutayoshida Exp $ --%>

<%@page contentType="text/html"%>
<%@page pageEncoding="UTF-8"%>

<%@taglib prefix="f" uri="http://java.sun.com/jsf/core"%>
<%@taglib prefix="h" uri="http://java.sun.com/jsf/html"%>
<%@taglib prefix="ui" uri="http://java.sun.com/blueprints/ui" %>


    
    <f:view>
        <ui:rssBar url="https://blueprints.dev.java.net/servlets/ProjectRSS?type=news"
                  itemCount="4"
                  title="News from BluePrints"
                  speed="6000"
                  backgroundImage="no"
                  color="#00ffff" hoverColor="#ff00ff"></ui:rssBar>
    </f:view>
