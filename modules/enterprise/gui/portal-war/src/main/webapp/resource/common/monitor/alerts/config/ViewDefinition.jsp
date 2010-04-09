<%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-html-el" prefix="html" %>

<tiles:insert definition=".page.title.events">
  <tiles:put name="titleKey" value="alert.config.props.ViewDef.PageTitle"/>
</tiles:insert>
<tiles:importAttribute name="id" ignore="true"/>
<tiles:importAttribute name="ad" ignore="true"/>

<tiles:insert definition=".portlet.error"/>
<tiles:insert definition=".portlet.confirm"/>

<tiles:insert definition=".events.config.view.nav"/>

<tiles:insert definition=".events.config.view.properties"/>

<tiles:insert definition=".events.config.view.conditionsbox"/>

<tiles:insert definition=".events.config.view.senders"/>

<%--<tiles:insert definition=".events.config.view.notifications"/>--%>

<%--<tiles:insert definition=".events.config.view.controlactionbox"/>--%>

<tiles:insert definition=".events.config.view.nav"/>

<tiles:insert definition=".page.footer"/>
