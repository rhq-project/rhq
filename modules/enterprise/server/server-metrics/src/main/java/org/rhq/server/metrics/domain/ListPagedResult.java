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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.datastax.driver.core.BoundStatement;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.exceptions.NoHostAvailableException;

import org.rhq.server.metrics.CQLException;

/**
 * This class helps paginate Cassandra results that have a list in the matching clause. Instead of running
 * a single big query, this class will run a single query for every element in the list.
 *
 * @author Stefan Negrea
 *
 */
public class ListPagedResult<T> implements Iterable<T> {

    private static final int DEFAULT_PAGE_SIZE = 200;

    private final ResultSetMapper<T> mapper;
    private final String query;
    private final Session session;
    private final int pageSize;
    private final List<?> valuesToBind;

    private final PreparedStatement preparedStatement;

    /**
     * @param query query to execute
     * @param mapper result set mapper
     * @param session Cassandra session
     * @param pageSize page size
     */
    public ListPagedResult(String query, List<?> valuesToBind, ResultSetMapper<T> mapper, Session session, int pageSize) {
        this.query = query;
        this.mapper = mapper;
        this.session = session;
        this.pageSize = pageSize;
        this.valuesToBind = valuesToBind;

        try {
            this.preparedStatement = this.session.prepare(this.query);
        } catch (NoHostAvailableException e) {
            throw new CQLException(e);
        }
    }

    /**
     * @param query query to execute
     * @param mapper result set mapper
     * @param session Cassandra session
     * @param pageSize page size
     */
    public ListPagedResult(String query, List<?> valuesToBind, ResultSetMapper<T> mapper, Session session) {
        this(query, valuesToBind, mapper, session, DEFAULT_PAGE_SIZE);
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
    private ResultSet retrieveNextResultSet(ResultSet existingResultSet, List<?> valuesToBind) {
        try{
            while ((existingResultSet == null || existingResultSet.isExhausted()) && valuesToBind.size() != 0) {
                BoundStatement boundStatement = this.preparedStatement.bind(valuesToBind.remove(0));
                return session.execute(boundStatement);
            }
        } catch (NoHostAvailableException e) {
            throw new CQLException(e);
        }

        return existingResultSet;
    }

    /* (non-Javadoc)
     * @see java.lang.Iterable#iterator()
     */
    @Override
    public Iterator<T> iterator() {
        return new Iterator<T>() {
            @SuppressWarnings({ "unchecked", "rawtypes" })
            private final List<?> localValuesToBind = new ArrayList(valuesToBind);
            private ResultSet resultSet = retrieveNextResultSet(null, localValuesToBind);
            private T lastRetrievedItem = null;

            public boolean hasNext() {
                resultSet = retrieveNextResultSet(resultSet, localValuesToBind);
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
