<%@page contentType="text/html"%>
<%@page pageEncoding="UTF-8"%>
<%@ page import="org.hibernate.engine.SessionFactoryImplementor" %>
<%@ page import="org.rhq.core.domain.util.PersistenceUtility" %>
<%@ page import="org.rhq.enterprise.gui.legacy.util.SessionUtils" %>
<%@ page import="org.rhq.enterprise.server.util.LookupUtil" %>
<%@ page import="javax.naming.InitialContext" %>
<%@ page import="javax.persistence.EntityManager" %>
<%@ page import="javax.persistence.EntityManagerFactory" %>
<%@ page import="javax.persistence.Id" %>
<%@ page import="java.beans.BeanInfo" %>
<%@ page import="java.beans.Introspector" %>
<%@ page import="java.beans.PropertyDescriptor" %>
<%@ page import="java.beans.PropertyEditor" %>
<%@ page import="java.beans.PropertyEditorManager" %>
<%@ page import="java.lang.reflect.Field" %>
<%@ page import="java.lang.reflect.Method" %>
<%@ page import="java.util.Collection" %>
<%@ page import="java.util.Iterator" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.Map" %>
<%@ page import="java.util.SortedSet" %>
<%@ page import="java.util.TreeSet" %>
<%@taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>

<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN"
   "http://www.w3.org/TR/html4/loose.dtd">

<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <title>EJB3 Entity browser</title>
    </head>
    <body>

    <jsp:include page="/admin/include/adminTestLinks.html" flush="true" />

    <%
       boolean isAdmin = LookupUtil.getAuthorizationManager().isSystemSuperuser(SessionUtils.getWebUser(session).getSubject());
       if (!isAdmin) {
         out.println("<b>You do not have the necessary access privileges to view this page</b>");
         return;
       }
    %>

    <h1>Entity browser</h1>
    ${param.entityClass}

    <c:if test="${param.pEntity != null}">
        <c:url var="url" value="browser.jsp?entityClass=${param.pEntity}&key=${param.pKey}"/>
        <a href="${url}">Up to ${param.pEntity} (${param.pKey})</a>
    </c:if>

    <%
        InitialContext ic = new InitialContext();

        EntityManagerFactory emf = (EntityManagerFactory) ic.lookup("java:/RHQEntityManagerFactory");
        EntityManager em = ((EntityManagerFactory) ic.lookup("java:/RHQEntityManagerFactory"))
                .createEntityManager();


        org.hibernate.Session s = PersistenceUtility.getHibernateSession(em);
        SessionFactoryImplementor sfi = (SessionFactoryImplementor) s.getSessionFactory();

    %>


    <c:if test="${param.entityClass == null}">
        <c:set var="entityClass" value="org.rhq.core.domain.resource.Resource"/>

        <%

            Map<String, Object> metadata = sfi.getAllClassMetadata();
            SortedSet<String> classes = new TreeSet<String>(metadata.keySet());

        %>

       <ul>

           <%
                for (String className : classes) {
                    Object val = metadata.get(className);
                    String url = "browser.jsp?entityClass=" + className;

                    className = className.replaceFirst(".*\\.(.*\\..*)","$1");

                    out.println(
                    "        <li><a href=\"" + url + "\">" + className +"</a></li>");
                }
           %>


     </ul>
    </c:if>


    <c:if test="${param.entityClass != null}">
       <c:set var="entityClass" value="${param.entityClass}"/>
    </c:if>

   <c:if test="${param.mode == 'delete'}">
      <%
         String entityName = (String)pageContext.getRequest().getParameter("entityClass");
         String key = (String)pageContext.getRequest().getParameter("key");

         em.remove(em.find(Class.forName(entityName),key));
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
           List values = em.createQuery("from " + entityName + " d").setMaxResults(100).getResultList();

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
        <%
            Class entityType = Class.forName((String)pageContext.findAttribute("entityClass"));
            Object entity = em.find(entityType,
                                    getConvertedKey(entityType,
                                            pageContext.getRequest().getParameter("key")));
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
                    if (reference != null && reference.getClass().getName().startsWith("org.rhq.core.domain")) {
                       java.lang.reflect.Method idMethod = null;
                       try {
                           idMethod = reference.getClass().getMethod("getId", new Class[0]);
                       } catch (NoSuchMethodException nsme) { }
                        if (idMethod != null) {
                           try {
                               Object id = idMethod.invoke(reference,new Object[0]);
                               pageContext.setAttribute("child", reference);
                               String className = reference.getClass().getName();
                               if (className.indexOf('_') > 0) {
                                   className = className.substring(0,className.indexOf('_'));
                               }
                               pageContext.setAttribute("cType", className);
                               
                               %>
                                <c:url var="url" value="browser.jsp?entityClass=${cType}&key=${child.id}&pEntity=${entityClass}&pKey=${param.key}"/>
                                   <a href="${url}">${child}</a><br>
                               <%
                            } catch (Exception e) { out.println("{exception: " + e.toString() + "}"); }
                        } else {
                            try {
                            out.println(reference);
                            } catch (Exception e) { out.println("<i>" + e.getClass().getSimpleName() + "</i>"); }
                        }
                    } else {
                        try {
                        out.println(reference);
                        } catch (Exception e) { out.println("<i>" + e.getClass().getSimpleName() + "</i>"); }
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

                    Method readMethod = pd.getReadMethod();
                    if (readMethod != null) {
                        Collection c = (Collection) readMethod.invoke(entity, new Object[]{});
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
                    }
                    out.write("</ul>");
                }
            }
        %>


    </c:if>


    <%!
    public Object getConvertedKey(Class type, Object key) {
        Class keyType = getKeyType(type);
        System.out.println("Key type is: " + keyType);
        PropertyEditor ed = PropertyEditorManager.findEditor(keyType);
        if (ed != null) {
            ed.setAsText(String.valueOf(key));
            return ed.getValue();
        } else {
            return key;
        }
    }

    public Class getKeyType(Class type) {
        Field[] fields = type.getDeclaredFields();
        for (Field f : fields) {
            Id id = f.getAnnotation(javax.persistence.Id.class);
            if (id != null) {
                return f.getType();
            }
        }

        return Integer.class;
    }

    %>
    </body>
</html>
