<%-- Copyright 2006 Sun Microsystems, Inc. All rights reserved. You may not modify, use, reproduce, or distribute this software except in compliance with the terms of the License at: http://developer.sun.com/berkeley_license.html
$Id: tagItemLookup.jsp,v 1.4 2007/01/19 21:47:31 basler Exp $ --%>

<%@page contentType="text/xml"%>
<%@page pageEncoding="UTF-8"%>
<%@page import="java.util.*, java.text.NumberFormat, com.sun.javaee.blueprints.petstore.model.CatalogFacade, com.sun.javaee.blueprints.petstore.model.Item, com.sun.javaee.blueprints.petstore.model.Tag"%>

<%
    String sxTag=request.getParameter("tag");

    try {
        response.setHeader("Pragma", "No-Cache");
        response.setHeader("Cache-Control", "no-cache,no-store,max-age=0");
        response.setDateHeader("Expires", 1);        

        ServletContext context=config.getServletContext();
        CatalogFacade cf=(CatalogFacade)context.getAttribute("CatalogFacade");
        Tag tag=cf.getTagWithPersistentItems(sxTag);
        out.println("<response>");

        if(tag != null) {
            out.println("<tag>" + tag.getTag() + "</tag>");
            out.println("<items>");
            Collection<Item> items=tag.getItems();
            for(Item item : items) {
                if(item.getDisabled() == 0) {
                    out.println("<item>");
                    out.println("<itemID>" + item.getItemID() + "</itemID>");
                    out.println("<productID>" + item.getProductID() + "</productID>");
                    out.println("<name><![CDATA[" + item.getName() + "]]></name>");
                    out.println("<description><![CDATA[" + item.getDescription() + "]]></description>");
                    out.println("<tags><![CDATA[" + item.tagsAsString() + "]]></tags>");
                    out.println("<price><![CDATA[" + NumberFormat.getCurrencyInstance(java.util.Locale.US).format(item.getPrice()) + "]]></price>");
                    out.println("</item>");
                }
            }
            out.println("</items>");
        }
        out.println("</response>");
        out.flush();
    } catch(Exception ee) {
        ee.printStackTrace();
    }

%>
