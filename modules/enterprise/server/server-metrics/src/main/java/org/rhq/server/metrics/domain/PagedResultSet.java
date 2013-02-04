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

import com.datastax.driver.core.ResultSet;

/**
 *
 * @author Stefan Negrea
 *
 */
public class PagedResultSet<T> implements Iterable<List<T>> {

    private static final int DEFAULT_PAGE_SIZE = 200;

    private final ResultSetMapper<T> mapper;
    private final ResultSet resultSet;

    private int pageSize = DEFAULT_PAGE_SIZE;

    /**
     * @param resultSet result set to map
     * @param mapper mapper
     * @param pageSize page size
     */
    public PagedResultSet(ResultSet resultSet, ResultSetMapper<T> mapper, int pageSize) {
        this(resultSet, mapper);
        this.pageSize = pageSize;
    }

    /**
      * @param resultSet result set to map
      * @param mapper mapper
      */
    public PagedResultSet(ResultSet resultSet, ResultSetMapper<T> mapper) {
        this.resultSet = resultSet;
        this.mapper = mapper;
    }

    /**
     * @return the page size
     */
    public int getPageSize() {
        return pageSize;
    }

    /**
     * @param pageSize the page size to set
     */
    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    public List<T> getNextPage() {
        List<T> result = new ArrayList<T>();
        for (int i = 0; i < this.getPageSize(); i++) {
            if (!resultSet.isExhausted()) {
                result.add((T) mapper.mapOne(resultSet));
            } else {
                break;
            }
        }

        return null;
    }

    /**
     * @return true if the result is exhausted, false otherwise
     */
    public boolean isExhausted() {
        return resultSet.isExhausted();
    }

    /* (non-Javadoc)
     * @see java.lang.Iterable#iterator()
     */
    @Override
    public Iterator<List<T>> iterator() {

        return new Iterator<List<T>>() {

            public boolean hasNext() {
                return !isExhausted();
            }

            public List<T> next() {
                return getNextPage();
            }

            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

}
