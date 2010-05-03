<%-- Copyright 2006 Sun Microsystems, Inc. All rights reserved. You may not modify, use, reproduce, or distribute this software except in compliance with the terms of the License at: http://developer.sun.com/berkeley_license.html
$Id: catalog.jsp,v 1.20 2006/12/01 21:38:40 basler Exp $ --%>
<script type="text/javascript" src="common.js"></script>
<script type="text/javascript" src="scroller.js"></script>
<link rel="stylesheet" type="text/css" href="scroller.css"></link>
<script type="text/javascript" src="accordion.js"></script>
<link rel="stylesheet" type="text/css" href="accordion.css"></link>
<script type="text/javascript" src="catalog.js"></script>
<%@taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %> 
<%@taglib uri="http://java.sun.com/jsf/html" prefix="h" %>
<%@taglib uri="http://java.sun.com/jsf/core" prefix="f" %>
<%@taglib prefix="ui" uri="http://java.sun.com/blueprints/ui" %>
<body>
<jsp:include page="banner.jsp" />
<script type="text/javascript">
    dojo.event.connect(window, "onload", function(){initCatalog();});
</script>
<center>
    <table border="0">
        <tr>
            <td>
                <table id="accordion">
                    <tr id="accordionTitle" class="accordionTitle"><td>Pets</td></tr>
                    <tr><td>
                            <table id="accordionBody" border="0" class="accordionBody">
                            </table>
                    </td></tr>
                </table>
            </td>
            <td style="min-width:500px;">
                <div id="CatalogBrowser">
                <table width="500px"border="1" class="slider" >
                    <tr height="415" valign="top">
                        <td id="bodySpace" border="0" align="center"></td>
                    </tr>
                    <tr id="targetRow">
                        <td width="500px" height="70px" align="top">
                            <div class="nav" id="right_button">
                                <img src="${pageContext.request.contextPath}/images/right.gif" name="nextRoll" title="Show More Items" border="0">
                            </div>
                            <div class="nav" id="left_button">
                                <img src="${pageContext.request.contextPath}/images/left.gif" name="previousRoll" title="Show Previous Items" border="0">
                            </div>
                            <div id="infopane" class="infopane">
                                <table class="infopaneTable">
                                    <tr>
                                        <td id="infopaneName" class="infopaneTitle">
                                        </td>
                                        <td id="infopaneRating" class="infopaneRating">
                                            <f:view>
                                            <ui:rating id="rating" maxGrade="5" includeNotInterested="false" includeClear="false" 
                                                       hoverTexts="#{RatingBean.ratingText}" notInterestedHoverText="Not Interested" clearHoverText="Clear Rating"
                                                       grade="#{RatingBean.grade}"/>
                                        <f:verbatim></td><td id="infopanePrice" class="infopanePrice"></td><td id="infopanePayPal" class="infopanePayPal"></f:verbatim>
                                            <ui:buyNow business="donate@animalfoundation.com" id="buyNow1" itemName="Buy Item One"
                                                       amount="100.00" quantity="1" type="BuyNow" postData="#{PayPalBean.postData}" target="paypal"/>    
                                            </f:view>
                                        </td>
                                        <td id="infopaneIndicator" class="infopaneIndicator">
                                        </td>
                                        <td id="infopaneDetailsIcon">
                                        </td>
                                    </tr>
                                    <tr>
                                        <td id="infopaneShortDescription" colspan="7" class="infopaneShortDescription">
                                        </td>
                                    </tr>
                                    <tr style="height:20px">
                                        <td></td>
                                    </tr>
                                    <tr>
                                        <td id="infopaneDescription" colspan="6" class="infopaneDescription">
                                        </td>
                                    </tr>
                                </table>
                            </div>                         
                        </td>                    
                    </tr>
                </table>
            </td>
            <td style="vertical-align:top; width:300px; text-align:right;">
                <div id="downloadAds">
                    <jsp:include page="download.jsp" />
                </div>
            </td>
        </tr>
    </table>
    </div>
    <div id="status" style="text-align:left"></div> <div id="status_2"></div>
    <div id="dstatus"></div>
    <div id="injection_point"></div>
</center>
<jsp:include page="footer.jsp" />
</body>
</html>