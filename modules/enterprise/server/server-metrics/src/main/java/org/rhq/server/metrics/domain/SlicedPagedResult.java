/*
 *
 * RHQ Management Platform
 * Copyright (C) 2005-2013 Red Hat, Inc.
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
 *
 */

package org.rhq.server.metrics.domain;

import java.util.Iterator;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.exceptions.NoHostAvailableException;

import org.rhq.server.metrics.CQLException;

/**
 * This class provides a method to paginate Cassandra results based on user provided queries. The user is required to
 * provided the initial query to obtain the first slice and then additional queries to obtain subsequent slices based
 * on the last retrieved element from Cassandra.
 *
 * @author Stefan Negrea
 *
 */
public class SlicedPagedResult<T extends NumericMetric> implements Iterable<T> {

    private static final int DEFAULT_PAGE_SIZE = 30000;

    private final ResultSetMapper<T> mapper;
    private final Session session;
    private final int pageSize;
    private final QueryCreator<T> queryCreator;

    /**
     * @param query query to execute
     * @param mapper result set mapper
     * @param session Cassandra session
     * @param pageSize page size
     */
    public SlicedPagedResult(QueryCreator<T> queryCreator, ResultSetMapper<T> mapper, Session session, int pageSize) {
        this.mapper = mapper;
        this.session = session;
        this.pageSize = pageSize;
        this.queryCreator = queryCreator;
    }

    /**
     * @param query query to execute
     * @param mapper result set mapper
     * @param session Cassandra session
     * @param pageSize page size
     */
    public SlicedPagedResult(QueryCreator<T> queryCreator, ResultSetMapper<T> mapper, Session session) {
        this(queryCreator, mapper, session, DEFAULT_PAGE_SIZE);
    }

    /**
     * @return page size
     */
    public int getPageSize() {
        return this.pageSize;
    }

    private ResultSet retrieveNextResultSet(ResultSet existingResultSet, T lastRetrievedItem) {
        try{
            if (existingResultSet != null && existingResultSet.isExhausted() && existingResultSet.all().size() == pageSize) {
                return session.execute(queryCreator.buildNextQuery(lastRetrievedItem));
            }
        } catch (NoHostAvailableException e) {
            throw new CQLException(e);
        }

        return existingResultSet;
    }

    private ResultSet retrieveInitialResultSet() {
        try {
            return session.execute(queryCreator.buildInitialQuery());
        } catch (NoHostAvailableException e) {
            throw new CQLException(e);
        }
    }

    /* (non-Javadoc)
     * @see java.lang.Iterable#iterator()
     */
    @Override
    public Iterator<T> iterator() {
        return new Iterator<T>() {
            private T lastRetrievedItem = null;
            private ResultSet resultSet = retrieveInitialResultSet();

            public boolean hasNext() {
                resultSet = retrieveNextResultSet(resultSet, lastRetrievedItem);
                return resultSet != null && !resultSet.isExhausted();
            }

            public T next() {
                lastRetrievedItem = mapper.mapOne(resultSet);
                return lastRetrievedItem;
            }

            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    public interface QueryCreator<T> {
        String buildInitialQuery();
        String buildNextQuery(T object);
    }
}
