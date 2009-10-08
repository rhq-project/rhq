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
package org.rhq.enterprise.server.resource.group.definition.mbean;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

import javax.management.MBeanRegistration;
import javax.management.MBeanServer;
import javax.management.MBeanServerInvocationHandler;
import javax.management.ObjectName;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.resource.group.GroupDefinition;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * An MBean that exposes call-time metrics for the cost of recalculating
 * DynaGroups from their owning {@link GroupDefinition}s 
 * 
 * @author Joseph Marques
 */
public class GroupDefinitionRecalculationThreadMonitor implements GroupDefinitionRecalculationThreadMonitorMBean,
    MBeanRegistration {

    /*
     * synchronization policy: all private attributes of this statistics object are meant to be read and written
     * atomically.  as a result, all access to and from them must be guarded by the same lock. extensions to this 
     * class should use the intrinsic object lock to protect during access and/or modification. 
     */
    public class GroupDefinitionRecalculationStatistic {
        private long dynaGroupCount;
        private long recalculationCount;
        private long successfulCount;
        private long minExecutionTime;
        private long maxExecutionTime;
        private long totalEexecutionTime;

        public synchronized void update(int newDynaGroupCount, boolean success, long executionTime) {
            dynaGroupCount = newDynaGroupCount;

            if (success) {
                successfulCount++;
            }
            recalculationCount++;

            if (executionTime < minExecutionTime) {
                minExecutionTime = executionTime;
            } else if (executionTime > maxExecutionTime) {
                maxExecutionTime = executionTime;
            }
            totalEexecutionTime += executionTime;
        }

        public synchronized Map<String, Object> getStatistics() {
            Map<String, Object> stats = new HashMap<String, Object>();
            stats.put("dynaGroupCount", dynaGroupCount);
            stats.put("recalculationCount", recalculationCount);
            stats.put("successfulCount", successfulCount);
            stats.put("failureCount", recalculationCount - successfulCount);
            stats.put("minExecutionTime", minExecutionTime);
            stats.put("maxExecutionTime", maxExecutionTime);
            stats.put("avgEexecutionTime", totalEexecutionTime / (double) recalculationCount);
            return stats;
        }
    }

    private static AtomicLong lastAutoRecalculationThreadTime = new AtomicLong(0);
    private static ConcurrentMap<String, GroupDefinitionRecalculationStatistic> statistics = new ConcurrentHashMap<String, GroupDefinitionRecalculationStatistic>();

    private static MBeanServer mbeanServer;
    private static ObjectName objectName;

    private static GroupDefinitionRecalculationThreadMonitorMBean proxy;

    public static GroupDefinitionRecalculationThreadMonitorMBean getMBean() {
        if (proxy == null) {
            if (objectName != null) {
                proxy = (GroupDefinitionRecalculationThreadMonitorMBean) MBeanServerInvocationHandler.newProxyInstance(
                    mbeanServer, objectName, GroupDefinitionRecalculationThreadMonitorMBean.class, false);
            } else {
                // create a local object
                proxy = new GroupDefinitionRecalculationThreadMonitor();
            }
        }

        return proxy;
    }

    public ObjectName preRegister(MBeanServer server, ObjectName name) throws Exception {
        objectName = name;
        mbeanServer = server;
        return name;
    }

    public void clear() {
        statistics.clear();
    }

    public long getGroupDefinitionCount() {
        return LookupUtil.getGroupDefinitionManager().getGroupDefinitionCount(getOverlord());
    }

    public long getAutoRecalculatingGroupDefinitionCount() {
        return LookupUtil.getGroupDefinitionManager().getAutoRecalculationGroupDefinitionCount(getOverlord());
    }

    public long getDynaGroupCount() {
        return LookupUtil.getGroupDefinitionManager().getDynaGroupCount(getOverlord());
    }

    private Subject getOverlord() {
        return LookupUtil.getSubjectManager().getOverlord();
    }

    public long getAutoRecalculationThreadTime() {
        return lastAutoRecalculationThreadTime.get();
    }

    public void updateAutoRecalculationThreadTime(long timeInMillis) {
        lastAutoRecalculationThreadTime.set(timeInMillis);
    }

    public void updateStatistic(String groupDefinitionName, int newDynaGroupCount, boolean success, long executionTime) {
        statistics.putIfAbsent(groupDefinitionName, new GroupDefinitionRecalculationStatistic());
        GroupDefinitionRecalculationStatistic stat = statistics.get(groupDefinitionName);
        stat.update(newDynaGroupCount, success, executionTime);
    }

    public Map<String, Map<String, Object>> getStatistics() {
        Map<String, Map<String, Object>> results = new HashMap<String, Map<String, Object>>();
        for (Map.Entry<String, GroupDefinitionRecalculationStatistic> stat : statistics.entrySet()) {
            String groupDefinitionName = stat.getKey();
            Map<String, Object> groupDefinitionStatistics = stat.getValue().getStatistics();
            results.put(groupDefinitionName, groupDefinitionStatistics);
        }
        return results;
    }

    public void postRegister(Boolean registrationDone) {
    }

    public void preDeregister() throws Exception {
    }

    public void postDeregister() {
        mbeanServer = null;
    }

}