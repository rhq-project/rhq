<%-- Copyright 2006 Sun Microsystems, Inc. All rights reserved. You may not modify, use, reproduce, or distribute this software except in compliance with the terms of the License at: http://developer.sun.com/berkeley_license.html
$Id: lookup.jsp,v 1.7 2007/01/19 21:47:31 basler Exp $ --%>

<%@page contentType="text/xml"%>
<%@page pageEncoding="UTF-8"%>
<%@page import="com.sun.javaee.blueprints.petstore.model.CatalogFacade, com.sun.javaee.blueprints.petstore.model.Item"%>

<%
    String itemId=request.getParameter("itemId");
    String popupView=request.getParameter("popupView");

    try {
        ServletContext context=config.getServletContext();
        CatalogFacade cf=(CatalogFacade)context.getAttribute("CatalogFacade");
        Item item=cf.getItem(itemId);

        if(itemId != null) {
            out.println("<response>");
            out.println("<name>" + item.getName() + "</name>");
            String tmp=null;
            if(popupView != null && popupView.equals("2")) {
                tmp=item.getAddress().addressToString();
            } else {
                tmp=item.getDescription();
            }
            out.println("<description><![CDATA[" + tmp + "]]></description>");
            out.println("<price>" + java.text.NumberFormat.getCurrencyInstance(java.util.Locale.US).format(item.getPrice()) + "</price>");
            out.println("<image>" + request.getContextPath() + "/ImageServlet/" + item.getImageThumbURL() + "</image>");
            out.println("</response>");
            out.flush();
        }
    } catch(Exception ee) {
        ee.printStackTrace();
    }

%>
