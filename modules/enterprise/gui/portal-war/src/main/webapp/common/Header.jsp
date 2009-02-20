<%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-html-el" prefix="html" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/tld/hq.tld" prefix="hq" %>
<script language="JavaScript" type="text/javascript">
  var headerPath = "<html:rewrite page="/images/"/>";
  var help = "<hq:help/>";
</script>

<div id="PageHeader">
    <jsp:include page="/rhq/common/menu/menu.xhtml"/>

</div>

<div id="about" class="dialog" style="display: none; position: absolute; top: 0; left: 0">
   <div class="DisplayContent" style="margin:0">
      <div style="margin:5px">
         <p class="compact">
            <span class="DisplaySubhead"><fmt:message key="footer.aboutLink"/></span><br/>
            <span class="DisplayLabel"><fmt:message key="footer.version"/>: <fmt:message key="product.version"/></span><br/>
            <span class="DisplayLabel"><fmt:message key="footer.buildNumber"/>: <fmt:message key="product.buildNumber"/></span>            
         </p>
         <p class="compact">
            <fmt:message key="footer.shortCopyright"/> <fmt:message key="about.Copyright.Content"/>
         </p>
         <p class="compact">
            <fmt:message key="about.MoreInfo.Label"/><br/>
            <a href="mailto:<fmt:message key="about.MoreInfo.LinkSales"/>"><fmt:message key="about.MoreInfo.LinkSales"/></a><br/>
            <a href="mailto:<fmt:message key="about.MoreInfo.LinkSupport"/>"><fmt:message key="about.MoreInfo.LinkSupport"/></a>
         </p>
      </div>
   </div>
</div>
