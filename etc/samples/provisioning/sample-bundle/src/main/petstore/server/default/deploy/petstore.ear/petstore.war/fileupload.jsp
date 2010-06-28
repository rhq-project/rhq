<%-- Copyright 2006 Sun Microsystems, Inc. All rights reserved. You may not modify, use, reproduce, or distribute this software except in compliance with the terms of the License at: http://developer.sun.com/berkeley_license.html
$Id: fileupload.jsp,v 1.57 2007/03/08 21:58:48 inder Exp $ --%>

<%@page contentType="text/html"%>
<%@page pageEncoding="UTF-8"%>
<%@page import="com.sun.javaee.blueprints.petstore.util.PetstoreConstants"%>
<%@taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %> 
<%@taglib uri="http://java.sun.com/jsf/html" prefix="h" %>
<%@taglib uri="http://java.sun.com/jsf/core" prefix="f" %>
<%@taglib prefix="ui" uri="http://java.sun.com/blueprints/ui" %>

<html>
    <head>
        <title>Petstore Seller page</title>
    <script type="text/javascript">
    var submittingForm=false;
    
    function testRetFunction(type, data, evt){
        if (evt.readyState == 4) {
            if(evt.status == 200) {
                // check for error
                submittingForm=false;
                var resultx=data.getElementsByTagName("response")[0];
                var message=resultx.getElementsByTagName("message")[0].childNodes[0].nodeValue;
                if(message == "Captchas Filter Error") {
                    // captcha error
                    alert("Authorization failed : please enter the correct captcha string");
                } else if(message == "Validation Error") {
                    alert("Validation failed on the Server :\n" + resultx.getElementsByTagName("detail")[0].childNodes[0].nodeValue);     
                } else if(message == "Upload Size Error") {
                    alert("The size of the uploaded image must not be more than 100 KB");     
                } else {
                    // fileupload complete
                    var thumbpath=resultx.getElementsByTagName("thumbnail")[0].childNodes[0].nodeValue;
                    var productId=resultx.getElementsByTagName("productId")[0].childNodes[0].nodeValue;
                    var itemid=resultx.getElementsByTagName("itemId")[0].childNodes[0].nodeValue;
                    // forward to status page
                    location.href="fileuploadstatus.jsp?message=" + message + "&id=" + itemid + "&productId=" + productId + "&thumb=" + thumbpath;
                }
            } else {
                // server error, send to error page
                // can't forward to errorpage because null pointer gets thrown on lookup of status code ("javax.servlet.error.status_code")
                //ajaxBindError(type, data.message);
                alert("Persistence failed : Please check if the server logs for more information!")
            }
        }
    }
    
    
   function storeCookie() {
       currentcap = "j_captcha_response="+document.getElementById("TestFileuploadForm:captcharesponse").value;
       document.cookie = currentcap;
   }
   
   function extractCity(citystatezip) {
       var index = citystatezip.indexOf(',');
       var nextcity = citystatezip.substring(0, index+4);
       return nextcity; 
   }

   function chooseCity(city) {
       var index = city.indexOf(',');
       var state = city.substring(index+2, index+4);
       var zip = city.substring(index+5);
       city = city.substring(0, index);
       
       document.getElementById('TestFileuploadForm:cityField').value = city;
       document.getElementById('TestFileuploadForm:stateField').value = state;
       document.getElementById('TestFileuploadForm:zipField').value = zip;
   }
   
   function fileuploadOnsubmit() {
        if(!submittingForm) {
            var valMess="";
            
            // save rich text editor text to element
            var descx=dojo.widget.byId('rtEditor').getEditorContent()
            var lowDescx=descx.toLowerCase();

            // START: check validation
            if(dojo.byId("TestFileuploadForm:name").value == "") {
                valMess += "Error: Pet Name is required.\n";
            }
            
            // make sure there isn't a script/link tag in the description
            if(lowDescx == "" || lowDescx.indexOf("<script") > -1 || lowDescx.indexOf("<link") > -1) {
            valMess += "Error: The Description must exist and the field can't have a '<script>' and/or a '<link>' tag in it\n";
            }

            // make sure price is a number
            var pricex=dojo.byId("TestFileuploadForm:price").value;
            if(pricex == "" || isNaN(parseInt(pricex))) {
                // price should be a number
                valMess += "Error: Price should should exist and be a number in American Dollars in the format '*.00'.\n";
            }
            
            // make sure the upload file ends in an suffix
            var filex=dojo.byId("fileToUploadId").value;
            var lengthx=filex.length;
            var suffix=filex.substr(lengthx-4).toLowerCase();
            if(lengthx < 1 || (suffix != ".jpg" && suffix != ".gif" && suffix != ".png")) {
                // not a proper upload so error
                valMess += "Error: The image upload file must exist and be of type .jpg, .gif or .png\n";
            }
            

            // make sure make and address is entered
            if(dojo.byId("TestFileuploadForm:firstName").value == "") {
                // price should be a number
                valMess += "Error: First Name is required.\n";
            }
            if(dojo.byId("TestFileuploadForm:lastName").value == "") {
                valMess += "Error: Last Name is required.\n";
            }
            if(dojo.byId("TestFileuploadForm:street1").value == "") {
                valMess += "Error: Street is required.\n";
            }
            if(dojo.byId("TestFileuploadForm:cityField").value == "") {
                valMess += "Error: City is required.\n";
            }
            if(dojo.byId("TestFileuploadForm:stateField").value == "") {
                valMess += "Error: State is required.\n";
            }
            if(dojo.byId("TestFileuploadForm:zipField").value == "") {
                valMess += "Error: Zip Code is required.\n";
            }
            
            if(valMess != "") {
                // error, show message
                alert(valMess + "\nPlease addresses the error(s) and re-submit your entry!");
            } else {
                // no validation errors, so submit form
                submittingForm=true;
                // set description
                dojo.byId('description').value=descx;

                storeCookie()
               document.forms['TestFileuploadForm'].onsubmit();
           }
        }
   }
   
   function showFU() {
       document.getElementById("fucomponent").style.visibility = "visible";
   }
   
   function switchPanes(fromDivId, toDivId) {
        // show pane
        var divx=document.getElementById(fromDivId);
        divx.style.visibility='hidden';
        divx=document.getElementById(toDivId);
        divx.style.visibility='visible';
   }
</script>
<style>
span.button {    
    background-color: #6699CC; 
    color: white; 
    cursor:pointer;
    border: thin outset black;
    padding: 1px 5px;
}
div.pane {
    width: 90%; 
    background-color: #EEEEEE;
    border: thin double blue;
    padding: .5cm;
    font: 12px arial;
}

.nameCol {
    width: 45%; 
}
.dataCol {
    width: 55%; 
}

</style>
    </head>
    <body onload="showFU()">
        <jsp:include page="banner.jsp"/>
        <script>dojo.require("dojo.widget.Editor2");</script>        
        <br/>
        <div id="fucomponent" style="visibility:hidden;">
        <f:view>
    
            <ui:fileUploadTag id="TestFileuploadForm" serverLocationDir="#{FileUploadBean.uploadImageDirectory}" 
                postProcessingMethod="#{FileUploadBean.postProcessingMethod}"
                retMimeType="text/xml" retFunction="testRetFunction" 
                progressBarDivId="progress" progressBarSize="40">
                <div id="pane2" class="pane" style="visibility: hidden;">
                    <h:panelGrid  border="0" columns="2" style="width: 100%" columnClasses="nameCol, dataCol">
                        <f:facet name="header">
                            <h:outputText value="Information about yourself"/>
                        </f:facet>
                        <h:outputText value="*First Name"/>
                        <h:inputText size="20" id="firstName"></h:inputText>
                        <h:outputText value="*Last Name"/>
                        <h:inputText size="20" id="lastName"></h:inputText>
                        <h:outputText value="Seller Email"/>
                        <h:inputText size="20" id="email"></h:inputText>
                        <h:outputText value="*Street"/>
                        <h:inputText size="20" id="street1"></h:inputText>
                        <h:outputText value="*City"/>
                        <ui:autoComplete size="20" maxlength="10" id="cityField"
                        completionMethod="#{AutocompleteBean.completeCity}"
                        value="#{AddressBean.city}" required="true"
                        ondisplay="function(item) { return extractCity(item); }"
                        onchoose="function(item) { return chooseCity(item); }" />
                        <h:outputText value="*State"/>
                        <ui:autoComplete size="2"  maxlength="10" id="stateField" 
                        completionMethod="#{AutocompleteBean.completeState}" 
                        value="#{AddressBean.state}" required="true" />
                        <h:outputText value="*Zip Code"/>
                        <h:inputText size="5" id="zipField" value="#{AddressBean.zip}" required="true" />

                        <h:outputText value="Enter the text as it is shown below (case insensitive)"/>
                        <h:outputText />
                        <h:graphicImage id="captchaImg" url="CaptchaServlet"/>
                        <h:inputText id="captcharesponse"></h:inputText>
                        <br/><span class="button" onclick="switchPanes('pane2', 'pane1');">&lt;&lt; Previous</span>
                        &nbsp;&nbsp;&nbsp;<span class="button" onclick="fileuploadOnsubmit()">Submit</span>
                        <br/><div id="progress"></div><br/>

                    </h:panelGrid>
                </div>
                <div class="pane"style="position:absolute; top:125px;" id="pane1">
                    <h:panelGrid  border="0" columns="2" style="width: 100%" columnClasses="nameCol, dataCol">
                        <f:facet name="header">
                            <h:outputText value="Information about your pet"/>
                        </f:facet>

                        <h:outputText value="Category"/>
                        <h:selectOneMenu id="product">
                            <f:selectItems value="#{FileUploadBean.products}"/>
                        </h:selectOneMenu>

                        <h:outputText value="*Pet's Name"/>
                        <h:inputText size="20" id="name"></h:inputText>

                        <h:outputText value="*Description (3 lines max display in catalog)"/>
                        
                        <div style="border-style:inset; border-width:thin; background-color:white">
                            <textarea wrap="soft" dojoType="Editor2" widgetId="rtEditor" id="description" name="TestFileuploadForm:description" 
                            toolbarTemplatePath="${pageContext.request.contextPath}/rteToolBar.html"></textarea>
                        </div>
                        
                        <h:outputText value="*Price (is US dollars)"/>
                        <h:inputText size="20" id="price"></h:inputText>

                        <h:outputText value="*Image File"/>                 
                        <input type="file" size="20" name="fileToUpload" id="fileToUploadId"/>

                        <h:outputText value="Custom Tag Keywords (separated by spaces)"/>
                        <h:inputText size="20" id="tags"></h:inputText>
                    </h:panelGrid>
                    <br/><span class="button" onclick="switchPanes('pane1', 'pane2');">Next &gt;&gt;</span>
                </div>
                Required fields are designated by a *
            </ui:fileUploadTag>        
        </f:view>
        </div>
 
    <jsp:include page="footer.jsp" />    
    </body>
</html>
