<%-- Copyright 2006 Sun Microsystems, Inc. All rights reserved. You may not modify, use, reproduce, or distribute this software except in compliance with the terms of the License at: http://developer.sun.com/berkeley_license.html
$Id: fileuploadstatus.jsp,v 1.8 2006/05/05 21:05:47 yutayoshida Exp $ --%>

<%@page contentType="text/html"%>
<%@page pageEncoding="UTF-8"%>

<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <title>Seller Photo Submit Status</title>
        <style type="text/css">
            #status { background-color : #E0FFFF;
                      border : none;
                      width : 50%;
                    }
        </style>
        <script type="text/javascript">
            window.onload = function() {
                var thumbfile = "${param.thumb}";
                if (thumbfile == "") {
                    thumbfile = "${sessionScope['fileuploadResponse'].thumbnail}";
                }
                thumbpath = "http://${pageContext.request.serverName}:${pageContext.request.serverPort}${pageContext.servletContext.contextPath}/ImageServlet/";
                thumbpath += thumbfile;
                var divNode = document.getElementById("thumb");
                var imgNode = document.createElement("img");
                imgNode.setAttribute("src", thumbpath);
                divNode.appendChild(imgNode);
                
                // initialize buttons
                initButtonImage();
            }
            
            var imageLayerId = new Array();
            var imageLayerG = new Array();
            var imageLayerC = new Array();
            var imageText = new Array();
            function initButtonImage(){
                imageLayerId[1] ="seller"
                imageLayerG[1]  = "../images/seller-thumb-g.jpg";
                imageLayerC[1]   = "../images/seller-thumb.jpg";
                imageLayerId[2] ="catalog"
                imageLayerG[2]  = "../images/catalog-thumb-g.jpg";
                imageLayerC[2]   = "../images/catalog-thumb.jpg";
                imageLayerId[3] ="home"
                imageLayerG[3]  = "../images/index-thumb-g.jpg";
                imageLayerC[3]   = "../images/index-thumb.jpg";
                
                imageText[1] = "Submit another pet";
                imageText[2] = "Go to your pet page";
                imageText[3] = "Go back to PetStore home";
             }
             
             function highlightButton(n) {
                 switchButton(true, n);
             }
             function darkenButton(n) {
                 switchButton(false, n);
             }

            function switchButton(highlight, n){
                var id = imageLayerId[n];
                var btn = document.getElementById(id);
                if (highlight) {
                    btn.src = imageLayerC[n];
                    popupText(imageText[n]);
                } else {
                    btn.src = imageLayerG[n];
                    popupText(null);
                }
            }
            var Mx;
            var My;
            function popupText(txt) {
                var pNode = document.getElementById("popupText");
                var rx;
                var ry;
                if (document.all) {
                    rx = event.clientX + document.body.scrollLeft +10;
                    ry = event.clientY + document.body.scrollTop -20;
                } else {
                    rx = Mx + 10;
                    ry = My -20;
                }
                if (txt) {
                    pNode.style.display = "block";
                    pNode.style.left = rx + "px";
                    pNode.style.top = ry + "px";
                    pNode.innerHTML = txt;
                } else {
                    pNode.style.display = "none";
                    pNode.innerHTML = "";
                }
            }
            function getMouseXY(mEvent) {
                Mx = mEvent.pageX;
                My = mEvent.pageY;
            }
            window.onmousemove = getMouseXY;
        </script>
    </head>
    <body>
    <jsp:include page="banner.jsp"/>
    <center>
    <div id="status">
        <h4>${param.message}</h4>
        Here's the uploaded photo of your pet<br/><br/>
        <div id="thumb"></div>
        <br/><br/>
        Would you like to :-<br/><br/>
        
        <div id="popupText" style="position:absolute;z-index:2;border:1px solid;padding:5px;
             border-color:blue;font-size:10pt;background-color:#00ffff;color:blue;display:none"></div>
        <table border="0" cellpadding="4" cellspacing="4">
            <tr>
            <td><a href="fileupload.jsp" onmouseover="highlightButton(1)" onmouseout="darkenButton(1)">
      <img name="seller" id="seller" src="../images/seller-thumb-g.jpg" alt="seller" width="105" height="60" border="0">
    </a>
            </td>
            <td><a href="catalog.jsp?pid=${param.productId}&itemId=${param.id}" onmouseover="highlightButton(2)" onmouseout="darkenButton(2)">
      <img name="catalog" id="catalog" src="../images/catalog-thumb-g.jpg" alt="catalog" width="105" height="60" border="0">
    </a>
            </td>
            <td><a href="http://${pageContext.request.serverName}:${pageContext.request.serverPort}${pageContext.servletContext.contextPath}/index.jsp" onmouseover="highlightButton(3)" onmouseout="darkenButton(3)">
      <img name="home" id="home" src="../images/index-thumb-g.jpg" alt="index" width="105" height="60" border="0">
    </a>
            </td>
            </tr>
        </table>
    </div>
    </center>
    <jsp:include page="footer.jsp" />    
    </body>
</html>
