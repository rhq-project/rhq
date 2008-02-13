<%@page contentType="text/html"%>
<%@page pageEncoding="UTF-8"%>
<%@ page import="java.beans.BeanInfo" %>
<%@ page import="java.beans.Introspector" %>
<%@ page import="java.beans.PropertyDescriptor" %>
<%@ page import="java.util.Collection" %>
<%@ page import="java.util.Iterator" %>
<%@ page import="java.util.List" %>
<%@ page import="org.rhq.enterprise.gui.legacy.util.SessionUtils" %>
<%@ page import="org.rhq.enterprise.server.test.AccessLocal" %>
<%@ page import="org.rhq.enterprise.server.util.LookupUtil" %>
<%@taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>

<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN"
   "http://www.w3.org/TR/html4/loose.dtd">

<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <title>Greg's EJB3 Entity browser</title>
    </head>
    <body>

    <%
       boolean isAdmin = LookupUtil.getAuthorizationManager().isSystemSuperuser(SessionUtils.getWebUser(session).getSubject());
       if (!isAdmin) {
         out.println("<b>You don't have access privileges to this page</b>");
         return;
       }
    %>

    <h1>Entity browser</h1>
    ${param.entityClass}

    <c:if test="${param.pEntity != null}">
        <c:url var="url" value="browser.jsp?entityClass=${param.pEntity}&key=${param.pKey}"/>
        <a href="${url}">Up to ${param.pEntity} (${param.pKey})</a>
    </c:if>


    <c:if test="${param.entityClass == null}">
        <c:set var="entityClass" value="org.rhq.core.domain.resource.Resource"/>

       <ul>

        <li><b>Inventory:</b></li>
        <ul>
        <li><c:url var="url" value="browser.jsp?entityClass=org.rhq.core.domain.resource.Agent"/><a href="${url}">Agents</a></li>
        <li><c:url var="url" value="browser.jsp?entityClass=org.rhq.core.domain.resource.Resource"/><a href="${url}">Resources</a></li>
        </ul>

        <li><b>Content:</b></li>
          <ul>
             <li><c:url var="url" value="browser.jsp?entityClass=org.rhq.core.domain.content.Package"/><a href="${url}">Packages</a></li>
        <li><c:url var="url" value="browser.jsp?entityClass=org.rhq.core.domain.content.PackageVersion"/><a href="${url}">PackageVersions</a></li>
        <li><c:url var="url" value="browser.jsp?entityClass=org.rhq.core.domain.content.InstalledPackage"/><a href="${url}">InstalledPackage</a></li>
         </ul>

        <li><b>Metadata:</b></li>
          <ul>
             <li><c:url var="url" value="browser.jsp?entityClass=org.rhq.core.domain.resource.ResourceType"/><a href="${url}">Resource Types</a></li>
        <li><c:url var="url" value="browser.jsp?entityClass=org.rhq.core.domain.plugin.Plugin"/><a href="${url}">Plugins</a></li>
        <li><c:url var="url" value="browser.jsp?entityClass=org.rhq.core.domain.configuration.Configuration"/><a href="${url}">Configuration</a></li>
        </ul>

        <li><b>Measurement</b></li>
          <ul>
             <li><c:url var="url" value="browser.jsp?entityClass=org.rhq.core.domain.measurement.MeasurementDefinition"/><a href="${url}">Measurement Definition</a></li>
        <li><c:url var="url" value="browser.jsp?entityClass=org.rhq.core.domain.measurement.MeasurementSchedule"/><a href="${url}">Measurement Schedule</a></li>
       </ul>

        <li><b>Alerts</b></li>
          <ul>
             <li><c:url var="url" value="browser.jsp?entityClass=org.rhq.core.domain.event.alert.AlertDefinition"/><a href="${url}">AlertDefinitions</a></li>
        <li><c:url var="url" value="browser.jsp?entityClass=org.rhq.core.domain.event.alert.AlertDampeningEvent"/><a href="${url}">AlertDampeningEvents</a></li>
        <li><c:url var="url" value="browser.jsp?entityClass=org.rhq.core.domain.event.alert.Alert"/><a href="${url}">Alerts</a></li>
        </ul>

        <li><b>AAA</b></li>
          <ul>
             <li><c:url var="url" value="browser.jsp?entityClass=org.rhq.core.domain.auth.Subject"/><a href="${url}">Subjects</a></li>
        <li><c:url var="url" value="browser.jsp?entityClass=org.rhq.core.domain.auth.Principal"/><a href="${url}">Principals</a></li>
        <li><c:url var="url" value="browser.jsp?entityClass=org.rhq.core.domain.authz.Role"/><a href="${url}">Roles</a></li>
        </ul>
     </ul>
    </c:if>


    <c:if test="${param.entityClass != null}">
       <c:set var="entityClass" value="${param.entityClass}"/>
    </c:if>

    <%
       javax.naming.InitialContext ctx = new javax.naming.InitialContext();
       AccessLocal access = LookupUtil.getAccessLocal();
    %>

   <c:if test="${param.mode == 'delete'}">
      <%
         String entityName = (String)pageContext.getRequest().getParameter("entityClass");
         String key = (String)pageContext.getRequest().getParameter("key");
         access.delete(entityName,key);
         entityName = (String) pageContext.getRequest().getParameter("pEntity");
         key = (String) pageContext.getRequest().getParameter("pKey");
        ((HttpServletResponse)pageContext.getResponse()).sendRedirect(
            "browser.jsp?entityClass=" + entityName + "&key=" + key);
         if (true)
            return;
      %></c:if>


    <c:if test="${param.key == null}">
        <h3>Listing ${entityClass}</h3>
        <%
           String entityName = (String)pageContext.findAttribute("entityClass");
           entityName = entityName.substring(entityName.lastIndexOf('.')+1);
           List values = access.getAll(entityName);
            pageContext.setAttribute("values", values);%>
        <c:forEach var="entity" items="${values}">
            <c:url var="url" value="browser.jsp?entityClass=${entityClass}&key=${entity.id}"/>
            <a href="${url}">${entity}</a><br>
        </c:forEach>

    </c:if>

    <c:if test="${param.key != null}">
         <c:url var="url" value="browser.jsp?entityClass=${entityClass}"/>
         <h3>Details of ${entityClass} (${param.key})</h3>
         <a href="${url}">List all ${entityClass}</a><br>
        <% Object entity = access.findDeep((String)pageContext.findAttribute("entityClass"),pageContext.getRequest().getParameter("key"));
            pageContext.setAttribute("entity",entity);

            BeanInfo beanInfo = Introspector.getBeanInfo(entity.getClass());
            PropertyDescriptor[] pds = beanInfo.getPropertyDescriptors();
            int ln = pds.length;
        %>
        <c:set var="entity" value="${entity}"/>
            String view: ${entity}

        <h4>Simple Properties</h4>
        <table border="1"><tr><td align="right"><b>Name</b></td><td><b>Value</b></td></tr>
        <%
            int i = 0;
            for (i = 0; i < ln; i++) {
                PropertyDescriptor pd = pds[i];

                if (!Collection.class.isAssignableFrom(pd.getPropertyType()) ) {
                    out.println("<tr><td align='right'><b>" + pd.getName() + ": </b></td><td>");

                    Object reference = pd.getReadMethod() == null ? "-cant read-" : pd.getReadMethod().invoke(entity, new Object[]{});
                    if (reference != null && reference.getClass().getName().startsWith("org.jboss.on.")) {
                       java.lang.reflect.Method idMethod = null;
                       try {
                           idMethod = reference.getClass().getMethod("getId", new Class[0]);
                       } catch (NoSuchMethodException nsme) { }
                        if (idMethod != null) {
                           try {
                               Object id = idMethod.invoke(reference,new Object[0]);
                               pageContext.setAttribute("child", reference);
                               pageContext.setAttribute("cType", reference.getClass().getName());
                               %>
                                <c:url var="url" value="browser.jsp?entityClass=${cType}&key=${child.id}&pEntity=${entityClass}&pKey=${param.key}"/>
                                   <a href="${url}">${child}</a><br>
                               <%
                            } catch (Exception e) { out.println("{exception: " + e.toString() + "}"); }
                        } else {
                            out.println(reference);
                        }
                    } else {
                        out.println(reference);
                    }

                    out.println("</td></tr>");
                }
            }
        %>
        </table>


        <%
            for (i = 0; i < ln; i++) {
                PropertyDescriptor pd = pds[i];
                if (Collection.class.isAssignableFrom(pd.getPropertyType())) {
                    out.println("<h4>" + pd.getName() + ": </h4><ul>");

                    Collection c = (Collection) pd.getReadMethod().invoke(entity, new Object[]{});
                    for (Iterator iter = c.iterator(); iter.hasNext(); ) {
                        Object child = iter.next();
                        pageContext.setAttribute("child", child);
                        pageContext.setAttribute("cType", child.getClass().getName());
        %>
       <c:catch var="foo">
         <li><c:url var="url" value="browser.jsp?entityClass=${cType}&key=${child.id}&pEntity=${entityClass}&pKey=${param.key}"/>
            <a href="${url}">${child}</a>

          <c:url var="url" value="browser.jsp?entityClass=${cType}&key=${child.id}&pEntity=${entityClass}&pKey=${param.key}&mode=delete"/>
          <a href="${url}"><font color="red">x</font></a>

         </li>
       </c:catch>
        <%
                    }
                    out.write("</ul>");
                }
            }
        %>


    </c:if>

    </body>
</html>
