<%@ page import="org.hibernate.EmptyInterceptor" %>
<%@ page import="org.hibernate.engine.NamedQueryDefinition" %>
<%@ page import="org.hibernate.engine.SessionFactoryImplementor" %>
<%@ page import="org.hibernate.hql.ParameterTranslations" %>
<%@ page import="org.hibernate.hql.QueryTranslator" %>
<%@ page import="org.hibernate.hql.ast.ASTQueryTranslatorFactory" %>
<%@ page import="org.hibernate.type.IntegerType" %>
<%@ page import="org.hibernate.type.LongType" %>
<%@ page import="org.hibernate.type.Type" %>
<%@ page import="org.rhq.enterprise.gui.legacy.util.SessionUtils" %>
<%@ page import="org.rhq.enterprise.server.util.LookupUtil" %>
<%@ page import="javax.naming.InitialContext" %>
<%@ page import="javax.persistence.EntityManager" %>
<%@ page import="javax.persistence.EntityManagerFactory" %>
<%@ page import="java.io.PrintWriter" %>
<%@ page import="java.io.StringWriter" %>
<%@ page import="java.util.*" %>
<%@ page import="org.hibernate.Session" %>
<%@ page import="org.hibernate.ejb.EntityManagerImpl" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<%--
  Author: Greg Hinkle
  Author: Joseph Marques
  Copyright: 2008 Red Hat
--%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<html>
<head><title>JPQL Translation and Execution Tool</title></head>
<body>

<jsp:include page="/admin/include/adminTestLinks.html" flush="true" />

<%!
    public static final String ENTITY_MANAGER_FACTORY_JNDI = "java:/RHQEntityManagerFactory";
    public static final int MAX_ROWS = 100;
%>


<form action="/admin/hibernate.jsp" method="post">


<%
    String hql = request.getParameter("hql");
    String namedQuery = request.getParameter("namedQuery");
    List results = null;
    boolean isDMLStyle = true;
    int resultSize = 0;
    long executionTime = 0;
    String error = null;
    QueryTranslator qt = null;
    final List<String> executedSQL = new ArrayList<String>();

    InitialContext ic = new InitialContext();

    EntityManagerFactory emf = (EntityManagerFactory) ic.lookup(ENTITY_MANAGER_FACTORY_JNDI);
    EntityManager em = emf.createEntityManager();


    org.hibernate.Session s = getHibernateSession(em);
    SessionFactoryImplementor sfi = (SessionFactoryImplementor) s.getSessionFactory();

    s = sfi.openSession(new EmptyInterceptor() {
        public String onPrepareStatement(String s) {
            executedSQL.add(s);
            return super.onPrepareStatement(s);
        }
    });

    if (namedQuery != null) {
        NamedQueryDefinition queryDef = sfi.getNamedQuery(namedQuery);
        if (queryDef != null) {
            hql = queryDef.getQueryString();
        }
    }

    request.setAttribute("hql", hql);
    request.setAttribute("namedQuery", namedQuery);


%>

<h3>Enter a query name or JPQL</h3>
<table>
    <tr>
        <td><b>Named Query: </b></td>
        <td><input type="text" name="namedQuery" size="100"
                   value="<%=request.getAttribute("namedQuery") != null ? request.getAttribute("namedQuery") : ""%>"/>
        </td>
    </tr>
    <tr>
        <td><b>JPQL: </b></td>
        <td><textarea name="hql" type="text" rows="8"
                      cols="120"><%=request.getAttribute("hql") != null ? request.getAttribute("hql") : ""%></textarea></td>
    </tr>

    <tr>
        <td><input name="translate" type="submit" value="translate"/>
            <input name="execute" type="submit" value="execute"/></td>
    </tr>
</table>

<hr/>

<%
    if (hql != null || namedQuery != null) {
        System.out.println("hql: " + hql);
        String sql = null;
        Set<String> parameterNames = null;
        try {
            qt = new ASTQueryTranslatorFactory().createQueryTranslator(
                    "test query",
                    hql,
                    null,
                    (SessionFactoryImplementor) s.getSessionFactory());

            qt.compile(null, false);
            sql = qt.getSQLString();

            if (sql == null) {
               out.write("Could not get SQL translation for DML-style operation");
            } else {
               out.write("<b>SQL: </b><textarea rows=\"10\" cols=\"120\">" + sql + "</textarea>");
            }

            ParameterTranslations pt = qt.getParameterTranslations();
            if (pt != null) {
                parameterNames = pt.getNamedParameterNames();
                request.setAttribute("parameterNames", parameterNames);
            }
        } catch (Exception e) {
            error = getExceptionString(e);
            request.setAttribute("error", error);
        }


%>
<br/>
<c:if test="${parameterNames != null}">
    <table>
        <c:forEach var="pn" items="${parameterNames}">
            <tr>
                <td><b>${pn}</b></td>
                <td><input type="text" name="${pn}" value="${param[pn]}"></td>
                <td>
                    <c:set value="${pn}" var="pn" scope="request"/>
                    <%=qt.getParameterTranslations().getNamedParameterExpectedType((String) request.getAttribute("pn")).getName()%>
                </td>
            </tr>
        </c:forEach>
    </table>
</c:if>
<%
        if (hql.replaceAll("\\s*", "").trim().toLowerCase().startsWith("select")) {
            isDMLStyle = false;
        }

        if (request.getParameter("execute") != null) {

            long start = System.currentTimeMillis();
            try {
                //results = qt.list((SessionImplementor) s, new QueryParameters());
                org.hibernate.Query q = s.createQuery(hql);
                Iterator iter = parameterNames.iterator();
                while (iter.hasNext()) {
                    String pn = (String) iter.next();
                    Object paramterValue = request.getParameter(pn);
                    Type type = qt.getParameterTranslations().getNamedParameterExpectedType(pn);
                    if (type instanceof LongType)
                        paramterValue = Long.parseLong((String) paramterValue);
                    else if (type instanceof IntegerType)
                        paramterValue = Integer.parseInt((String) paramterValue);

                    //out.println("parameter " + pn + " = " + paramterValue);
                    q.setParameter(pn, paramterValue);
                }
                
                if (isDMLStyle) {
                    resultSize = q.executeUpdate();
                    results = new ArrayList();
                } else {
                    q.setMaxResults(MAX_ROWS);
                    results = q.list();
                    resultSize = results.size();
                }

                request.setAttribute("results", results);

            } catch (Exception e) {
                error = getExceptionString(e);
                request.setAttribute("error", error);
            }
            executionTime = (System.currentTimeMillis() - start);
        }
    }

%>

</form>


<br/>

<c:if test="${param['execute'] != null and results != null}">
    <b>Executed in <%=executionTime%>ms. Found or updated <%=resultSize%> rows. <%=executedSQL.size()%> round trips.</b>
</c:if>


<c:if test="${error != null}">
    <pre>${error}</pre>
</c:if>
<c:if test="${(isDMLStyle == false) and (results != null)}">
    <hr>

    <table border="1">

        <%
            String[] aliases = qt.getReturnAliases();
            out.println("<tr>");
            for (String alias : aliases) {
                out.println("<th>" + alias + "</th>");
            }
            out.println("</tr>");

            for (Object row : results) {
                out.println("<tr>");
                if (row instanceof Object[]) {
                    Object[] arr = (Object[]) row;
                    for (Object col : arr) {
                        out.println("<td>" + col + "</td>");
                    }
                } else {
                    out.println("<td>" + row + "</td>");
                }
                out.println("</tr>");
            }
        %>

    </table>

    <hr>


    <b>Executed Sql: </b>
    <table border="1">

        <%
            for (String row : executedSQL) {
                out.println("<tr>");
                out.println("<td><span style=\"font-family: monospace\">" + row + "</span></td>");
                out.println("</tr>");
            }
        %>

    </table>
</c:if>

<%!
    private String getExceptionString(Throwable t) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        t.printStackTrace(pw);
        return sw.toString();
    }

    private static Session getHibernateSession(EntityManager entityManager) {
        Session session;
        if (entityManager.getDelegate() instanceof EntityManagerImpl) {
            EntityManagerImpl entityManagerImpl = (EntityManagerImpl) entityManager.getDelegate();
            session = entityManagerImpl.getSession();
        } else {
            session = (Session) entityManager.getDelegate();
        }

        return session;
    }

%>

</body>
</html>