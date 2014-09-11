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

import java.util.List;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;

/**
 * @author John Sanda
 */
public interface ResultSetMapper<T extends NumericMetric> {

    /**
     * Maps the entire result set to a list.
     *
     * @param resultSet result set to map
     * @return a list of mapped rows
     */
    List<T> mapAll(ResultSet resultSet);

    /**
     * Return only one mapped domain object. One mapped object could
     * require multiple rows from the result set.
     *
     * @param resultSet result set to map
     * @return
     */
    T mapOne(ResultSet resultSet);

    /**
     * Map all the rows passed to domain objects. More than one row could
     * be required for mapping a single object.
     *
     * @param row
     * @return
     */
    List<T> map(Row... row);

    /**
     * Map a sigle row to a domain object.
     *
     * @param row
     * @return
     */
    T map(Row row);
}
