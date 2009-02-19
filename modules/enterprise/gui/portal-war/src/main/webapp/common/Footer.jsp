<%@ taglib uri="http://jakarta.apache.org/struts/tags-html" prefix="html" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/tld/hq.tld" prefix="hq" %>

<table width="100%" border="0" cellspacing="0" cellpadding="0">
  <tr>
  	<td colspan="2"><html:img page="/images/spacer.gif" width="1" height="15" alt="" border="0" styleId="footerSpacer"/></td>
  </tr>
  <tr> 
    <td width="100%"><html:img page="/images/spacer.gif" width="1" height="1" alt="" border="0"/></td>
  </tr>
  <tr> 
    <td>
      <table width="80%" border="0" cellspacing="0" cellpadding="0">
        <c:set var="tmpTime"><%= java.lang.System.currentTimeMillis() %></c:set>
        <tr>
          <td class="FooterRegular" nowrap="nowrap"><a href="http://www.redhat.com"><html:img page="/images/logo_rh_home.png" alt="" border="0"/></a></td>
        </tr>
        <tr>
          <td class="FooterRegular" nowrap="nowrap"><fmt:message key="footer.copyright"/></td>
        </tr>
        <tr>
          <td class="FooterRegular" nowrap="nowrap">
             <script language="JavaScript" type="text/javascript">var aboutWindowTitle = '<fmt:message key="about.Title"/>';</script>
             <a href="#" onclick="openAbout(aboutWindowTitle)"><fmt:message key="footer.aboutLink"/></a>
          </td>
        </tr>
<hq:licenseExpiration/>
      </table>
    </td>
  </tr>
</table>

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

<script language="JavaScript" type="text/javascript">
  setFoot();
</script>
