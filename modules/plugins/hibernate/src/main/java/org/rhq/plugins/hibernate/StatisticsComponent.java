 /*
  * Jopr Management Platform
  * Copyright (C) 2005-2008 Red Hat, Inc.
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
package org.rhq.plugins.hibernate;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertyList;
import org.rhq.core.domain.configuration.PropertyMap;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.pluginapi.operation.OperationResult;
import org.rhq.plugins.jmx.MBeanResourceComponent;

/**
 * @author Greg Hinkle
 */
public class StatisticsComponent extends MBeanResourceComponent {
    @Override
    public OperationResult invokeOperation(String name, Configuration parameters) throws Exception {
        if ("viewQueries".equals(name)) {
            String[] queryStrings = (String[]) getEmsBean().getAttribute("Queries").refresh();
            OperationResult result = new OperationResult();
            PropertyList queries = new PropertyList("queries");
            result.getComplexResults().put(queries);

            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            try {
                Thread.currentThread().setContextClassLoader(getEmsBean().getClass().getClassLoader());
                for (String queryString : queryStrings) {
                    Object queryStatistics = getEmsBean().getOperation("getQueryStatistics").invoke(
                        new Object[] { queryString });

                    Long executionCount = (Long) queryStatistics.getClass().getMethod("getExecutionCount").invoke(
                        queryStatistics);
                    Long executionRowCount = (Long) queryStatistics.getClass().getMethod("getExecutionRowCount")
                        .invoke(queryStatistics);
                    Long executionMinTime = (Long) queryStatistics.getClass().getMethod("getExecutionMinTime").invoke(
                        queryStatistics);
                    Long executionMaxTime = (Long) queryStatistics.getClass().getMethod("getExecutionMaxTime").invoke(
                        queryStatistics);
                    Long executionAvgTime = (Long) queryStatistics.getClass().getMethod("getExecutionAvgTime").invoke(
                        queryStatistics);

                    PropertyMap query = new PropertyMap("query", new PropertySimple("query", queryString),
                        new PropertySimple("executionCount", executionCount), new PropertySimple("executionRowCount",
                            executionRowCount), new PropertySimple("executionMinTime", executionMinTime),
                        new PropertySimple("executionMaxTime", executionMaxTime), new PropertySimple(
                            "executionAvgTime", executionAvgTime));

                    queries.add(query);
                }

                return result;
            } finally {
                Thread.currentThread().setContextClassLoader(cl);
            }
        }

        return super.invokeOperation(name, parameters);
    }
}