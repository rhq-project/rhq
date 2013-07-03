/*
 * RHQ Management Platform
 * Copyright 2013, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
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

package org.rhq.server.metrics.migrator.workers;

/**
 * @author Stefan Negrea
 *
 */
public interface CallableMigrationWorker {

    public static final int MAX_RECORDS_TO_LOAD_FROM_SQL = 30000;
    public static final int MAX_RAW_BATCH_TO_CASSANDRA = 100;
    public static final int MAX_AGGREGATE_BATCH_TO_CASSANDRA = 50;
    public static final int NUMBER_OF_BATCHES_FOR_ESTIMATION = 4;
    public static final int MAX_NUMBER_OF_FAILURES = 5;

    long estimate() throws Exception;

    void migrate() throws Exception;
}
