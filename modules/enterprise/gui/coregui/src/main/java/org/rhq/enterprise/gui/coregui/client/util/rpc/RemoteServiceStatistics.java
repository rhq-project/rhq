/*
 * RHQ Management Platform
 * Copyright (C) 2010 Red Hat, Inc.
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
package org.rhq.enterprise.gui.coregui.client.util.rpc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Joseph Marques
 */
public class RemoteServiceStatistics {

    public static class Stat {
        private String serviceName;
        private String methodName;
        private int count;
        private long total;
        private long latest;
        private long slowest;
        private long fastest;

        protected Stat(String remoteService, long millis) {
            // remoteService format "{ServiceName}_Proxy.{MethodName}"
            this.serviceName = remoteService.substring(0, remoteService.indexOf("_Proxy"));
            this.methodName = remoteService.substring(remoteService.indexOf('.') + 1);
            count = 1;
            total = latest = slowest = fastest = millis;
        }

        private void record(long millis) {
            count++;
            total += millis;
            latest = millis;
            if (millis > slowest) {
                slowest = millis;
            } else if (millis < fastest) {
                fastest = millis;
            }
        }

        public String getServiceName() {
            return serviceName;
        }

        public String getMethodName() {
            return methodName;
        }

        public int getCount() {
            return count;
        }

        public long getTotal() {
            return total;
        }

        public long getLatest() {
            return latest;
        }

        public long getSlowest() {
            return slowest;
        }

        public long getFastest() {
            return fastest;
        }

        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append("serviceName=").append(serviceName).append(',');
            builder.append("methodeName=").append(methodName).append(": ");
            if (count < 1) {
                builder.append("empty");
            } else {
                builder.append("count=").append(count).append(',');
                builder.append("total=").append(total).append(',');
                builder.append("avg=").append(total / count).append(',');
                builder.append("latest=").append(latest).append(',');
                builder.append("slowest=").append(slowest).append(',');
                builder.append("fastest=").append(fastest);
            }
            return builder.toString();
        }

    }

    private static Map<String, Stat> statistics = new HashMap<String, Stat>();

    private RemoteServiceStatistics() {
        // static access only
    }

    public static void record(String remoteService, long millis) {
        Stat stat = statistics.get(remoteService);
        if (stat == null) {
            stat = new Stat(remoteService, millis);
            statistics.put(remoteService, stat);
        } else {
            stat.record(millis);
        }
    }

    public static String recordAndPrint(String remoteService, long millis) {
        record(remoteService, millis);
        return print(remoteService);
    }

    public static String print(String remoteService) {
        Stat stat = statistics.get(remoteService);
        if (stat == null) {
            stat = new Stat(remoteService, 0);
        }
        return "RemoteServiceStatistics: " + remoteService + ": " + stat.toString();
    }

    public static List<String> printAll() {
        List<String> stats = new ArrayList<String>();

        for (String remoteService : statistics.keySet()) {
            stats.add(print(remoteService));
        }

        return stats;
    }

    public static Stat get(String remoteService) {
        Stat stat = statistics.get(remoteService);
        if (stat == null) {
            stat = new Stat(remoteService, 0);
        }
        return stat;
    }

    public static List<Stat> getAll() {
        List<Stat> stats = new ArrayList<Stat>();

        for (String remoteService : statistics.keySet()) {
            stats.add(statistics.get(remoteService));
        }

        return stats;
    }
}
