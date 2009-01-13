/*
 * RHQ Management Platform
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
package org.rhq.plugins.jmx;

import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertyList;
import org.rhq.core.domain.configuration.PropertyMap;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.pluginapi.operation.OperationResult;

public class ThreadDataMeasurementComponent extends MBeanResourceComponent {
    public OperationResult invokeOperation(String name, Configuration parameters) throws InterruptedException,
        Exception {
        if ("threadDump".equals(name)) {
            ThreadMXBean threadBean = getEmsBean().getProxy(ThreadMXBean.class);

            long[] threadIds = threadBean.getAllThreadIds();
            ThreadInfo[] threadInfos = threadBean.getThreadInfo(threadIds, Integer.MAX_VALUE);
            OperationResult result = new OperationResult();
            Configuration resultConfig = result.getComplexResults();
            resultConfig.put(new PropertySimple("totalCount", threadBean.getThreadCount()));
            PropertyList threadList = new PropertyList("threadList");
            resultConfig.put(threadList);
            for (ThreadInfo threadInfo : threadInfos) {
                PropertyMap map = new PropertyMap("thread");
                map.put(new PropertySimple("name", threadInfo.getThreadName()));
                map.put(new PropertySimple("id", threadInfo.getThreadId()));
                map.put(new PropertySimple("state", threadInfo.getThreadState().name()));
                map.put(new PropertySimple("stack", getStringStackTrace(threadInfo.getStackTrace())));
                threadList.add(map);
            }

            return result;
        } else {
            return super.invokeOperation(name, parameters);
        }
    }

    private String getStringStackTrace(StackTraceElement[] st) {
        StringBuilder buf = new StringBuilder(2000);
        for (StackTraceElement e : st) {
            buf.append(e.getClassName());
            buf.append(".");
            buf.append(e.getMethodName());
            buf.append("(");
            buf.append(e.getFileName());
            buf.append(":");
            buf.append(e.getLineNumber());
            buf.append(")\n");
        }

        if (buf.length() > 2000) {
            return buf.substring(0, 2000);
        } else {
            return buf.toString();
        }
    }
}