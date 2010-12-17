/*
 * RHQ Management Platform
 * Copyright 2010, Red Hat Middleware LLC, and individual contributors
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
package org.rhq.core.db.upgrade;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.rhq.core.db.DatabaseType;

/**
 * Sets values on the id column for all rows in RHQ_OPERATION_SCHEDULE. The id's are set, starting at 10001, in the
 * order that the rows were created. This is done by parsing the value of the JOB_NAME column, which includes the
 * creation time.
 *
 * @author Ian Springer
 */
public class OperationScheduleIdSetterUpgradeTask implements DatabaseUpgradeTask {

    @Override
    public void execute(DatabaseType databaseType, Connection connection) throws SQLException {
        String selectSQL = "SELECT job_name FROM rhq_operation_schedule;";
        System.out.println("Executing: " + selectSQL);
        List<Object[]> results = databaseType.executeSelectSql(connection, selectSQL);
        Map<Long, String> sortedJobNames = new TreeMap<Long, String>();
        for (Object[] result : results) {
            String jobName = (String)result[0]; // e.g.: rhq-resource-10001--121207376-1292542028679
            // last portion is the job's creation time, e.g.: 1292542028679
            Long ctime = Long.valueOf(jobName.substring(jobName.lastIndexOf('-') + 1));
            sortedJobNames.put(ctime, jobName);
        }
        int id = 10001;
        for (String jobName : sortedJobNames.values()) {
            String updateSQL = "UPDATE rhq_operation_schedule SET id = " + (id++) + " WHERE job_name = '" + jobName +
                "';";
            System.out.println("Executing: " + updateSQL);
            databaseType.executeSql(connection, updateSQL);
        }
    }
    
}
