/*
 * RHQ Management Platform
 * Copyright (C) 2005-2014 Red Hat, Inc.
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

package org.rhq.core.domain.util;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.HibernateException;
import org.hibernate.ScrollableResults;
import org.hibernate.engine.spi.QueryParameters;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.event.spi.EventSource;
import org.hibernate.hql.internal.ast.QueryTranslatorImpl;

/**
 * This class can be used to quickly identify and analyze usages of JOIN FETCH together with limits on JPA queries. It
 * will log the JPA, generated SQL and a filtered stacktrace for each such usage. This is to enhance the diagnostics
 * that Hibernate itself offers that merely dumps a message about in-memory filtering of results resulting from the use
 * of JOIN FETCH together with limits.
 *
 * @author Lukas Krejci
 */
public class JoinFetchReportingQueryTranslator extends QueryTranslatorImpl {

    private static Log LOG = LogFactory.getLog("JOIN FETCH Performance");

    public JoinFetchReportingQueryTranslator(String queryIdentifier, String query, Map enabledFilters,
        SessionFactoryImplementor factory) {
        super(queryIdentifier, query, enabledFilters, factory);
    }

    private class JoinFetchUsage {
        private long time;
        private Integer firstRow;
        private Integer maxRows;

        public JoinFetchUsage(QueryParameters queryParameters) {
            boolean collect = containsCollectionFetches();
            boolean hasLimit =
                queryParameters.getRowSelection() != null && queryParameters.getRowSelection().definesLimits();

            if (collect && hasLimit) {
                firstRow = queryParameters.getRowSelection().getFirstRow();
                maxRows = queryParameters.getRowSelection().getMaxRows();

                time = System.currentTimeMillis();
            }
        }

        public void report(String method) {
            if (time != 0) {
                time = System.currentTimeMillis() - time;
                LOG.warn("Encountered a query with potentially bad performance. While this is not a bug and the " +
                    "system functions as designed, please report this to RHQ community so that we can reimplement our" +
                    " code to work better.\n" + method + "() with first: " + firstRow + ", max: " + maxRows + " took "
                    + time + "ms:\n" + getQueryString() + "\n\nSQL:\n" + getSQLString() + "\n" +
                    extractRHQCalls(new Exception()));
            }
        }

        private String extractRHQCalls(Throwable t) {
            StringBuilder bld = new StringBuilder();

            StackTraceElement[] elements = t.getStackTrace();

            //skip the report() and list() calls, hence 2
            for (int i = 2; i < elements.length; ++i) {
                StackTraceElement e = elements[i];
                if (e.getClassName().startsWith("org.rhq")) {
                    bld.append("\n").append(e.toString());
                }
            }

            return bld.toString();
        }
    }

    @Override
    public List list(SessionImplementor session, QueryParameters queryParameters) throws HibernateException {
        JoinFetchUsage usage = new JoinFetchUsage(queryParameters);

        List ret = super.list(session, queryParameters);

        usage.report("list");

        return ret;
    }

    @Override
    public Iterator iterate(QueryParameters queryParameters, EventSource session) throws HibernateException {
        JoinFetchUsage usage = new JoinFetchUsage(queryParameters);

        Iterator ret = super.iterate(queryParameters, session);

        usage.report("iterate");

        return ret;
    }

    @Override
    public ScrollableResults scroll(QueryParameters queryParameters, SessionImplementor session)
        throws HibernateException {
        JoinFetchUsage usage = new JoinFetchUsage(queryParameters);

        ScrollableResults ret = super.scroll(queryParameters, session);

        usage.report("scroll");

        return ret;
    }
}
