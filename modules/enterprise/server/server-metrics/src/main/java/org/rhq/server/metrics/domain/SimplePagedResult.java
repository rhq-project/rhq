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

import com.datastax.driver.core.Query;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.SimpleStatement;
import com.datastax.driver.core.exceptions.NoHostAvailableException;

import org.rhq.server.metrics.CQLException;
import org.rhq.server.metrics.StorageSession;

/**
 * This class is just a placeholder for future pagination implementations once Cassandra gets native support for paging results.
 * Right now the amount of data that is returned from Cassandra by any of the queries that run through this class
 * is small (under 10MB). So there is no performance impact for using this class as it is right now.
 *
 *  TODO: Update this code to support native pagination once Cassandra supports it.
 *
 * @author Stefan Negrea
 *
 */
public class SimplePagedResult<T extends NumericMetric> implements Iterable<T> {

    private static final int DEFAULT_PAGE_SIZE = 200;

    private final ResultSetMapper<T> mapper;
    private final Query query;
    private final StorageSession session;
    private final int pageSize;

    /**
     * @param query query to execute
     * @param mapper result set mapper
     * @param session Cassandra session
     * @param pageSize page size
     */
    public SimplePagedResult(Query query, ResultSetMapper<T> mapper, StorageSession session, int pageSize) {
        this.query = query;
        this.mapper = mapper;
        this.session = session;
        this.pageSize = pageSize;
    }

    /**
     * @param query query to execute
     * @param mapper result set mapper
     * @param session Cassandra session
     * @param pageSize page size
     */
    public SimplePagedResult(String query, ResultSetMapper<T> mapper, StorageSession session, int pageSize) {
        this(new SimpleStatement(query), mapper, session, pageSize);
    }

    /**
     * @param query query to execute
     * @param mapper result set mapper
     * @param session Cassandra session
     */
    public SimplePagedResult(Query query, ResultSetMapper<T> mapper, StorageSession session) {
        this(query, mapper, session, DEFAULT_PAGE_SIZE);
    }

    /**
     * @param query query to execute
     * @param mapper result set mapper
     * @param session Cassandra session
     * @param pageSize page size
     */
    public SimplePagedResult(String query, ResultSetMapper<T> mapper, StorageSession session) {
        this(new SimpleStatement(query), mapper, session);
    }

    /**
     * @return page size
     */
    public int getPageSize() {
        return this.pageSize;
    }

    /**
     * @throws Exception
     */
    private ResultSet retrieveInitialResultSet() {
        try {
            return session.execute(this.query);
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
            final ResultSet resultSet = retrieveInitialResultSet();
            private T lastRetrievedItem = null;

            public boolean hasNext() {
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

}
