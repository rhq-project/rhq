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
package org.rhq.enterprise.server.alert.engine.mbean;

import org.jboss.system.ServiceMBeanSupport;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * An MBean that exposes various structures contained with the AlertConditionCache
 */
public class AlertConditionCacheMonitor extends ServiceMBeanSupport implements AlertConditionCacheMonitorMBean {
    public String[] getCacheNames() {
        return LookupUtil.getAlertConditionCacheManager().getCacheNames();
    }

    public void printCache(String cacheName) {
        LookupUtil.getAlertConditionCacheManager().printCache(cacheName);
    }

    public void printAllCaches() {
        LookupUtil.getAlertConditionCacheManager().printAllCaches();
    }

    public boolean isCacheValid() {
        return LookupUtil.getAlertConditionCacheManager().isCacheValid();
    }
}