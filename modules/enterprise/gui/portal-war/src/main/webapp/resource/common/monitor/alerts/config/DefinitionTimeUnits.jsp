<%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-html-el" prefix="html" %>
<%@ taglib uri="/WEB-INF/tld/hq.tld" prefix="hq" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-tiles" prefix="tiles" %>

<tiles:importAttribute name="property"/>
<tiles:importAttribute name="enableFunc"/>

        <html:select property="${property}" onchange="javascript:${enableFunc}();">
        <hq:optionMessageList property="timeUnits" baseKey="alert.config.props.CB.Enable.TimeUnit" filter="true"/>
        </html:select>
