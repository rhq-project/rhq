/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package org.rhq.enterprise.server.measurement;

import java.sql.SQLException;

import javax.ejb.Local;

/**
 * @author Greg Hinkle
 */
@Local
public interface MeasurementCompressionManagerLocal {
    static final String TAB_DATA_1H = "RHQ_MEASUREMENT_DATA_NUM_1H";
    static final String TAB_DATA_6H = "RHQ_MEASUREMENT_DATA_NUM_6H";
    static final String TAB_DATA_1D = "RHQ_MEASUREMENT_DATA_NUM_1D";

    void compressData() throws SQLException;

    long compressData(String fromTable, String toTable, long interval, long now) throws SQLException;

    void purgeMeasurements(String tableName, long purgeAfter) throws SQLException;

    void truncateMeasurements(String tableName) throws SQLException;
}