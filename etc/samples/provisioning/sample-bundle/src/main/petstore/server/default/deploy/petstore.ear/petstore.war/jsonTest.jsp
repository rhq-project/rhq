<%@page contentType="text/html"%>
<%@page pageEncoding="UTF-8"%>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>

<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <title>JSON Test Page</title>
        <script type="text/javascript" src="${pageContext.request.contextPath}/faces/static/META-INF/dojo/bpcatalog/dojo.js"></script>

        <script type="text/javascript">
            function getJSON() {
                // get data and send to controller servlet
                var bindArgs = {
                    url: "${pageContext.request.contextPath}/catalog?command=items&pid=feline01&start=0&length=2&format=json",
                    mimetype: "text/json",
                    error: function(){ alert("error")},
                    load: callbackx
                    };

                dojo.io.bind(bindArgs);      
            }

            function callbackx(type, data, evt) {
                // check successful response
                if (evt.readyState == 4) {
                    if (evt.status == 200) {
                        var prodsx=data.products;
                        rep="Products:<br\>";
                        for(ii=0; ii < prodsx.length; ii++) {
                            rep += prodsx[ii].name + "<br/>";
                        }
                        rep+="<br\>";
                        document.getElementById("testResult").innerHTML=rep;
                    }
                }
            }
            
        </script>        
        
    </head>
    <body>

    <h1>JSON Test Page</h1>
    <span onclick="getJSON();"> GET DATA </span><br/><br/>
    
    <div id="testResult">
        This should be populated with result.  If not, look at the error console for errors.
    </div>
    
    </body>
</html>
