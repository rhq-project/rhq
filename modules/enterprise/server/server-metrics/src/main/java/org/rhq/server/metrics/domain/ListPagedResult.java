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

import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import com.datastax.driver.core.BoundStatement;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.exceptions.NoHostAvailableException;

import org.rhq.server.metrics.CQLException;
import org.rhq.server.metrics.StorageSession;

/**
 * This class helps paginate Cassandra results that have a list in the matching clause. Instead of running
 * a single big query, this class will run a single query for every element in the list.
 *
 * @author Stefan Negrea
 *
 */
public class ListPagedResult<T> implements Iterable<T> {


    private final List<Integer> scheduleIds;
    private final long startTime;
    private final long endTime;
    private final ResultSetMapper<T> mapper;
    private final StorageSession session;

    private final PreparedStatement preparedStatement;

    public ListPagedResult(PreparedStatement preparedStatement, List<Integer> scheduleIds, long startTime, long endTime,
        ResultSetMapper<T> mapper, StorageSession session) {
        this.preparedStatement = preparedStatement;
        this.scheduleIds = new LinkedList<Integer>(scheduleIds);
        this.startTime = startTime;
        this.endTime = endTime;
        this.mapper = mapper;
        this.session = session;
    }

    /**
     * @throws Exception
     */
    private ResultSet retrieveNextResultSet(ResultSet existingResultSet, List<Integer> ids) {
        try{
            while ((existingResultSet == null || existingResultSet.isExhausted()) && ids.size() != 0) {
                BoundStatement boundStatement = this.preparedStatement.bind(ids.remove(0), new Date(startTime),
                    new Date(endTime));
                existingResultSet = session.execute(boundStatement);
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
            private final List<Integer> ids = new LinkedList<Integer>(scheduleIds);
            private ResultSet resultSet = retrieveNextResultSet(null, ids);
            private T lastRetrievedItem = null;

            public boolean hasNext() {
                resultSet = retrieveNextResultSet(resultSet, ids);
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
