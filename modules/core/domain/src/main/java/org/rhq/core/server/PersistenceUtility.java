/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2, as
 * published by the Free Software Foundation, and/or the GNU Lesser
 * General Public License, version 2.1, also as published by the Free
 * Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License and the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License
 * and the GNU Lesser General Public License along with this program;
 * if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package org.rhq.core.server;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.ObjectName;
import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.ejb.EntityManagerImpl;
import org.hibernate.engine.NamedQueryDefinition;
import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.jmx.StatisticsService;
import org.hibernate.stat.Statistics;
import org.hibernate.type.CustomType;
import org.hibernate.type.EntityType;
import org.hibernate.type.PrimitiveType;
import org.hibernate.type.Type;

import org.rhq.core.domain.util.OrderingField;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.domain.util.PageOrdering;

/**
 * Various persistence utility methods - mostly Hibernate-specific.
 *
 * @author Heiko Rupp
 * @author Joseph Marques
 * @author Greg Hinkle
 */
public class PersistenceUtility {
    private static final Log LOG = LogFactory.getLog(PersistenceUtility.class);

    private static final Pattern COUNT_QUERY_PATTERN = Pattern.compile("^(\\s*SELECT\\s+)(.*?)(\\s+FROM.*)",
        Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL);
    private static final Pattern COUNT_QUERY_REMOVE_FETCH = Pattern.compile("FETCH", Pattern.CASE_INSENSITIVE
        | Pattern.MULTILINE | Pattern.DOTALL);

    public static final String HIBERNATE_STATISTICS_MBEAN_OBJECTNAME = "Hibernate:type=statistics,application=RHQ";

    @SuppressWarnings("unchecked")
    // used in hibernate.jsp
    public static String getDisplayString(Type hibernateType) {
        if (hibernateType instanceof EntityType) {
            return hibernateType.getName() + " (enter integer of ID / primary key field)";
        } else if (hibernateType instanceof CustomType) {
            if (Enum.class.isAssignableFrom(hibernateType.getReturnedClass())) {
                Class<? extends Enum<?>> enumClass = (Class<? extends Enum<?>>) hibernateType.getReturnedClass();
                StringBuilder result = new StringBuilder();
                result.append(enumClass.getName());
                result.append(" (");
                boolean first = true;
                for (Enum<?> nextEnum : enumClass.getEnumConstants()) {
                    if (!first) {
                        result.append(" | ");
                    } else {
                        first = false;
                    }
                    result.append(nextEnum.name());
                }
                result.append(")");
                return result.toString();
            }
        }
        return hibernateType == null ? "" : hibernateType.getName();
    }

    @SuppressWarnings("unchecked")
    // used in hibernate.jsp
    public static Object cast(String value, Type hibernateType) {
        if (hibernateType instanceof PrimitiveType) {
            Class<?> type = ((PrimitiveType) hibernateType).getPrimitiveClass();
            if (type.equals(Byte.TYPE)) {
                return Byte.valueOf(value);
            } else if (type.equals(Short.TYPE)) {
                return Short.valueOf(value);
            } else if (type.equals(Integer.TYPE)) {
                return Integer.valueOf(value);
            } else if (type.equals(Long.TYPE)) {
                return Long.valueOf(value);
            } else if (type.equals(Float.TYPE)) {
                return Float.valueOf(value);
            } else if (type.equals(Double.TYPE)) {
                return Double.valueOf(value);
            } else if (type.equals(Boolean.TYPE)) {
                return Boolean.valueOf(value);
            }
        } else if (hibernateType instanceof EntityType) {
            String entityName = ((EntityType) hibernateType).getAssociatedEntityName();
            try {
                Class<?> entityClass = Class.forName(entityName);
                Object entity = entityClass.newInstance();

                Field primaryKeyField = entityClass.getDeclaredField("id");
                primaryKeyField.setAccessible(true);
                primaryKeyField.setInt(entity, Integer.valueOf(value));
                return entity;
            } catch (Throwable t) {
                throw new IllegalArgumentException("Type[" + entityName + "] must have PK field named 'id'");
            }
        } else if (hibernateType instanceof CustomType) {
            if (Enum.class.isAssignableFrom(hibernateType.getReturnedClass())) {
                Class<? extends Enum<?>> enumClass = hibernateType.getReturnedClass();
                Enum<?>[] enumValues = enumClass.getEnumConstants();
                try {
                    int enumOrdinal = Integer.valueOf(value);
                    try {
                        return enumValues[enumOrdinal];
                    } catch (ArrayIndexOutOfBoundsException aioobe) {
                        throw new IllegalArgumentException("There is no " + enumClass.getSimpleName()
                            + " enum with ordinal '" + enumOrdinal + "'");
                    }
                } catch (NumberFormatException nfe) {
                    String ucaseValue = value.toUpperCase();
                    for (Enum<?> nextEnum : enumValues) {
                        if (nextEnum.name().toUpperCase().equals(ucaseValue)) {
                            return nextEnum;
                        }
                    }
                    throw new IllegalArgumentException("There is no " + enumClass.getSimpleName() + " enum with name '"
                        + value + "'");
                }
            }
        }
        return value;
    }

    /**
     * Used to create queries to use with the {@link org.rhq.core.domain.util.PageControl} objects. The query will already have its sort column
     * and order appended as well as having its first result and max results set according to the page control data.
     *
     * @param  entityManager your entity manager
     * @param  queryName     name of the query
     * @param  pageControl   the controls on the paging and sorting
     *
     * @return a preconfigured query for ordered pagination
     */
    public static Query createQueryWithOrderBy(EntityManager entityManager, String queryName, PageControl pageControl) {
        Query query;

        if (pageControl.getPrimarySortColumn() != null) {
            query = createQueryWithOrderBy(entityManager, queryName, pageControl.getOrderingFieldsAsArray());
        } else {
            StackTraceElement caller = new Throwable().fillInStackTrace().getStackTrace()[1];
            LOG.warn("Queries should really supply default sort columns. Caller did not: " + caller);

            // Use the standard named query if no sorting is specified
            query = entityManager.createNamedQuery(queryName);
        }

        setDataPage(query, pageControl);

        return query;
    }

    /**
     * Create a query from a named query with a transformed order by clause with multiple new ordery by clauses.
     *
     * @param  entityManager the entity manager to build the query against
     * @param  queryName     the name of the query to transform
     * @param  orderByFields an array of clauses to contribute to the order by
     *
     * @return the transformed query
     */
    public static Query createQueryWithOrderBy(EntityManager entityManager, String queryName,
        OrderingField... orderByFields) {
        NamedQueryDefinition ndc = getNamedQueryDefinition(entityManager, queryName);
        StringBuilder query = new StringBuilder(ndc.getQueryString());
        buildOrderBy(query, orderByFields);
        return entityManager.createQuery(query.toString());
    }

    private static StringBuilder buildOrderBy(StringBuilder query, OrderingField... orderByFields) {
        boolean first = true;
        for (OrderingField orderingField : orderByFields) {
            if (first) {
                // TODO GH: We could see if there already is an order by clause and contribute or override it
                query.append(" ORDER BY ");
                first = false;
            } else {
                query.append(", ");
            }

            query.append(orderingField.getField()).append(" ").append(orderingField.getOrdering());
        }

        return query;
    }

    private static String getOrderByFragment(OrderingField... orderByFields) {
        boolean first = true;
        StringBuilder builder = new StringBuilder();
        for (OrderingField orderingField : orderByFields) {
            if (first) {
                builder.append(" ORDER BY ");
                first = false;
            } else {
                builder.append(", ");
            }

            builder.append(orderingField.getField()).append(" ").append(orderingField.getOrdering());
        }

        return builder.toString();
    }

    /**
     * Builds a count(*) version of the named query so we don't have duplicate all our queries to use two query
     * pagination model.
     *
     * @param  em        the entity manager to build the query for
     * @param  queryName the NamedQuery to transform
     *
     * @return a query that can be bound and executed to get the total count of results
     */
    public static Query createCountQuery(EntityManager em, String queryName) {
        return createCountQuery(em, queryName, "*");
    }

    /**
     * Builds a count(*) version of the named query so we don't have duplicate all our queries to use two query
     * pagination model.
     *
     * @param  entityManager the entity manager to build the query for
     * @param  queryName     the NamedQuery to transform
     * @param  countItem     the object or attribute that needs to be counted, when it's ambiguous
     *
     * @return a query that can be bound and executed to get the total count of results
     */
    public static Query createCountQuery(EntityManager entityManager, String queryName, String countItem) {
        NamedQueryDefinition namedQueryDefinition = getNamedQueryDefinition(entityManager, queryName);
        String query = namedQueryDefinition.getQueryString();

        Matcher matcher = COUNT_QUERY_PATTERN.matcher(query);
        if (!matcher.find()) {
            throw new RuntimeException("Unable to transform query into count query [" + queryName + " - " + query + "]");
        }

        String newQuery = matcher.group(1) + "COUNT(" + countItem + ")" + matcher.group(3);

        matcher = COUNT_QUERY_REMOVE_FETCH.matcher(newQuery);
        if (matcher.find()) {
            StringBuffer buffer = new StringBuffer();
            do {
                matcher.appendReplacement(buffer, "");
            } while (matcher.find());
            matcher.appendTail(buffer);
            newQuery = buffer.toString();
        }
        if (LOG.isTraceEnabled()) {
            LOG.trace("Transformed query to count query [" + queryName + "] resulting in [" + newQuery + "]");
        }

        return entityManager.createQuery(newQuery);
    }

    public static void setDataPage(Query query, PageControl pageControl) {
        if (pageControl.getPageSize() > 0) {
            query.setFirstResult(pageControl.getStartRow());
            query.setMaxResults(pageControl.getPageSize());
        }
    }

    /**
     * Creates and executes a filter query for a collection relationship. This executes without passing back the query
     * object because the most common case is to simply paginate for a relationship. Use the createFilter method to
     * create more generic filters and get access to the hibernate query object for setting parameters etc.
     *
     * @param  entityManager
     * @param  collection
     * @param  pageControl
     *
     * @return the result list of the entities from the filtered relationship
     */
    @SuppressWarnings("unchecked")
    public static PageList createPaginationFilter(EntityManager entityManager, Collection collection,
        PageControl pageControl) {
        if (collection == null) {
            return new PageList(pageControl);
        }

        String filter = "";
        if (pageControl.getPrimarySortColumn() != null) {
            PageOrdering order = (pageControl.getPrimarySortOrder() == null) ? PageOrdering.ASC : pageControl
                .getPrimarySortOrder();
            filter = getOrderByFragment(new OrderingField(pageControl.getPrimarySortColumn(), order));
        }

        org.hibernate.Query query = getHibernateSession(entityManager).createFilter(collection, filter);
        if (pageControl.getPageSize() > 0) {
            query.setFirstResult(pageControl.getPageNumber() * pageControl.getPageSize());
            query.setMaxResults(pageControl.getPageSize());
        }

        // TODO GH: Always flushing is probably not what we really want here
        // relationship filters don't seem to cause the proper flush, so manually flush
        getHibernateSession(entityManager).flush();

        // TODO GH: This can only create unbounded PageLists since I don't know how to do a count query to find the size
        return new PageList<Object>(query.list(), pageControl);
    }

    /**
     * Use this inside subclasses as a convenience method.
     */
    @SuppressWarnings("unchecked")
    public static <T> List<T> findByCriteria(EntityManager entityManager, Class<T> type,
        org.hibernate.criterion.Criterion... criterion) {
        // Using Hibernate, more difficult with EntityManager and EJB-QL
        org.hibernate.Criteria crit = getHibernateSession(entityManager).createCriteria(type);
        for (org.hibernate.criterion.Criterion c : criterion) {
            crit.add(c);
        }

        return crit.list();
    }

    public static Session getHibernateSession(EntityManager entityManager) {
        Session session;
        if (entityManager.getDelegate() instanceof EntityManagerImpl) {
            EntityManagerImpl entityManagerImpl = (EntityManagerImpl) entityManager.getDelegate();
            session = entityManagerImpl.getSession();
        } else {
            session = (Session) entityManager.getDelegate();
        }

        return session;
    }

    /**
     * Enables the hibernate statistics mbean to provide access to information on the ejb3 persistence tier.
     *
     * @param entityManager an inject entity manager whose session factory will be tracked with these statistics
     * @param server        the MBeanServer where the statistics MBean should be registered; if <code>null</code>, the
     *                      first one in the list returned by MBeanServerFactory.findMBeanServer(null) is used
     */
    public static void enableHibernateStatistics(EntityManager entityManager, MBeanServer server) {
        try {
            SessionFactory sessionFactory = PersistenceUtility.getHibernateSession(entityManager).getSessionFactory();

            if (server == null) {
                ArrayList<MBeanServer> list = MBeanServerFactory.findMBeanServer(null);
                server = list.get(0);
            }

            ObjectName objectName = new ObjectName(HIBERNATE_STATISTICS_MBEAN_OBJECTNAME);
            StatisticsService mBean = new StatisticsService();
            mBean.setSessionFactory(sessionFactory);
            server.registerMBean(mBean, objectName);
            sessionFactory.getStatistics().setStatisticsEnabled(true);
        } catch (InstanceAlreadyExistsException iaee) {
            LOG.info("Duplicate mbean registration ignored: " + HIBERNATE_STATISTICS_MBEAN_OBJECTNAME);
        } catch (Exception e) {
            LOG.warn("Couldn't register hibernate statistics mbean", e);
        }
    }

    public static Statistics getStatisticsService(EntityManager entityManager, MBeanServer server) {
        Session hibernateSession = PersistenceUtility.getHibernateSession(entityManager);
        SessionFactory hibernateSessionFactory = hibernateSession.getSessionFactory();
        Statistics hibernateStatistics = hibernateSessionFactory.getStatistics();
        return hibernateStatistics;
    }

    private static NamedQueryDefinition getNamedQueryDefinition(EntityManager entityManager, String queryName) {
        SessionFactoryImplementor sessionFactory = getHibernateSessionFactoryImplementor(entityManager);
        NamedQueryDefinition namedQueryDefinition = sessionFactory.getNamedQuery(queryName);
        if (namedQueryDefinition == null) {
            throw new RuntimeException("EJB3 query not found [" + queryName + "]");
        }

        return namedQueryDefinition;
    }

    private static SessionFactoryImplementor getHibernateSessionFactoryImplementor(EntityManager entityManager) {
        Session session = getHibernateSession(entityManager);
        SessionFactoryImplementor sessionFactory = (SessionFactoryImplementor) session.getSessionFactory();
        return sessionFactory;
    }

    // wanted to combine postgres and oracle methods, but org.rhq.core.db.DatabaseType objects are not visible to domain
    public static String addPostgresNativePagingSortingToQuery(String query, PageControl pageControl) {
        return addLimitOffsetToQuery(query, pageControl);
    }

    // wanted to combine postgres and oracle methods, but org.rhq.core.db.DatabaseType objects are not visible to domain
    public static String addOracleNativePagingSortingToQuery(String query, PageControl pageControl) {
        StringBuilder queryWithPagingSorting = new StringBuilder(query.length() + 50);

        int minRowNum = pageControl.getStartRow() + 1;
        int maxRowNum = minRowNum + pageControl.getPageSize() - 1;

        // pagination calculations based off of double-projection of the results
        queryWithPagingSorting.append("SELECT outerResults.* FROM ( ");

        queryWithPagingSorting.append("SELECT innerResults.*, ROWNUM rnum FROM ( ");
        queryWithPagingSorting.append(query);
        // for oracle, order by occurs at the end of the original query, whether grouped or not
        queryWithPagingSorting.append(getOrderByFragment(pageControl.getOrderingFieldsAsArray()));
        queryWithPagingSorting.append(" ) innerResults ");

        // for oracle, paginate high off of the inner projection
        queryWithPagingSorting.append(" WHERE ROWNUM <= ").append(maxRowNum);

        // for oracle, paginate low off of the outer projection
        queryWithPagingSorting.append(" ) outerResults ");
        queryWithPagingSorting.append(" WHERE rnum >= ").append(minRowNum);

        return queryWithPagingSorting.toString();
    }

    /**
     * Note: always put the rownum column at the END of the columns, so that code
     *       which relies on index-based access to the result set data doesn't break
     *
     * Method 1:
     *
     * SELECT outerResults.* FROM (
     *    SELECT innerResults.*,
     *           ROW_NUMBER() OVER( {orderByClause} ) AS rownum
     *    FROM ( {queryWithoutOrderBy} ) AS innerResults
     * ) AS outerResults
     * WHERE rownum <= maxRowNum AND rownum >= minRowNum
     *
     * The above method fails in circumstances where the orderByClause is built up with
     * aliases that aren't in the explicit select list returned from queryWithoutOrderBy
     *
     *
     * Method 2:
     *
     * Fix above shortcomings by pushing the orderByClause into the actual select list
     *
     * SELECT singleResults.* FROM (
     *    {queryWithoutOrderBySelectList}
     *    , ROW_NUMBER() OVER( {orderByClause} ) AS rownum
     *    {queryWithoutOrderByRestOfQuery}
     * ) AS singleResults
     * WHERE rownum <= maxRowNum AND rownum >= minRowNum
     *
     *
     * Actually, both of the above methods have small flaws.  The first can not sort by columns that
     * aren't in the explicit return list.  The second can not sort by computed columns and subqueries
     * in the select list.  The only way I see how this can work is by modifying the queryWithoutOrderBy
     * to explicitly return all parameters that will be sorted on (even if the use case wouldn't normally
     * require them to be in the select list), alias them, and order by the aliases by modifying the web
     * ui code to use those tokens when generating the sortable column headers (jmarques - June/2009)
     */
    public static String addSQLServerNativePagingSortingToQuery(String query, PageControl pageControl) {
        return addSQLServerNativePagingSortingToQuery(query, pageControl, false);
    }

    public static String addSQLServerNativePagingSortingToQuery(String query, PageControl pageControl,
        boolean alternatePagingStyle) {
        StringBuilder queryWithPagingSorting = new StringBuilder(query.length() + 50);

        int minRowNum = pageControl.getStartRow() + 1;
        int maxRowNum = minRowNum + pageControl.getPageSize() - 1;

        String orderByClause = getOrderByFragment(pageControl.getOrderingFieldsAsArray());

        if (alternatePagingStyle) {
            int index = findSelectListEndIndex(query);
            String selectList = query.substring(0, index);
            String restOfQuery = query.substring(index);
            queryWithPagingSorting.append("SELECT singleResults.* FROM ( ");
            queryWithPagingSorting.append(selectList);
            queryWithPagingSorting.append(", ROW_NUMBER() OVER( " + orderByClause + " ) AS rownum ");
            queryWithPagingSorting.append(restOfQuery);
            queryWithPagingSorting.append(") AS singleResults ");
        } else {
            queryWithPagingSorting.append("SELECT outerResults.* FROM ( ");
            queryWithPagingSorting.append("   SELECT innerResults.*, ");
            queryWithPagingSorting.append("          ROW_NUMBER() OVER( " + orderByClause + " ) AS rownum ");
            queryWithPagingSorting.append("   FROM ( " + query + " ) AS innerResults ");
            queryWithPagingSorting.append(" ) AS outerResults ");
        }
        queryWithPagingSorting.append("WHERE rownum <= ").append(maxRowNum);
        queryWithPagingSorting.append(" AND rownum >= ").append(minRowNum);

        return queryWithPagingSorting.toString();
    }

    // beginning of from clause (not counting retrievals via subqueries) should indicate end of select list
    private static int findSelectListEndIndex(String query) {
        int nesting = 0;
        query = query.toLowerCase();
        StringBuilder wordBuffer = new StringBuilder();
        for (int i = 0; i < query.length(); i++) {
            char next = query.charAt(i);
            if (next == '(') {
                nesting++;
            } else if (next == ')') {
                nesting--;
            } else {
                if (nesting != 0) {
                    continue;
                }
                if (Character.isLetter(next)) {
                    wordBuffer.append(next);
                    if (wordBuffer.toString().equals("from")) {
                        return i - 4; // return index representing the character just before "from"
                    }
                } else {
                    wordBuffer.setLength(0); // clear buffer if we find any non-letter
                }
            }
        }
        throw new IllegalArgumentException("Could not find select list end index");
    }

    // wanted to combine postgres and oracle methods, but org.rhq.core.db.DatabaseType objects are not visible to domain
    public static String addH2NativePagingSortingToQuery(String query, PageControl pageControl) {
        return addLimitOffsetToQuery(query, pageControl);
    }

    private static String addLimitOffsetToQuery(String query, PageControl pageControl) {
        StringBuilder queryWithPagingSorting = new StringBuilder(query.length() + 50);
        queryWithPagingSorting.append(query);

        // for postgres, first order by
        queryWithPagingSorting.append(getOrderByFragment(pageControl.getOrderingFieldsAsArray()));

        // for postgres, then paginate
        queryWithPagingSorting.append(" LIMIT ").append(pageControl.getPageSize());
        queryWithPagingSorting.append(" OFFSET ").append(pageControl.getStartRow());

        return queryWithPagingSorting.toString();
    }
}