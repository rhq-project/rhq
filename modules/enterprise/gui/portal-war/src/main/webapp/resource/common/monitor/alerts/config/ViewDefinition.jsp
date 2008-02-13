<%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-tiles" prefix="tiles" %>

<tiles:insert definition=".page.title.events">
  <tiles:put name="titleKey" value="alert.config.props.ViewDef.PageTitle"/>
</tiles:insert>

<tiles:insert definition=".portlet.error"/>
<tiles:insert definition=".portlet.confirm"/>

<tiles:insert definition=".events.config.view.nav"/>

<tiles:insert definition=".events.config.view.properties"/>

<tiles:insert definition=".events.config.view.conditionsbox"/>

<tiles:insert definition=".events.config.view.notifications"/>

<tiles:insert definition=".events.config.view.controlactionbox"/>

<tiles:insert definition=".events.config.view.syslogactionbox"/>

<tiles:insert definition=".events.config.view.nav"/>

<tiles:insert definition=".page.footer"/>
