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

import org.rhq.enterprise.gui.coregui.client.util.rpc.RemoteServiceStatistics.Record.Summary;

/**
 * @author Joseph Marques
 */
public class RemoteServiceStatistics {

    public static class Record {

        private String serviceName;
        private String methodName;
        private List<Long> data = new ArrayList<Long>();

        private int success;
        private int errored;
        private int dropped;

        private Record(String remoteService) {
            // remoteService format "{ServiceName}_Proxy.{MethodName}"
            this.serviceName = remoteService.substring(0, remoteService.indexOf("_Proxy"));
            this.methodName = remoteService.substring(remoteService.indexOf('.') + 1);
        }

        private void record(long millis, int status) {
            this.data.add(millis);
            switch(status) {
                case TrackerStatusEvent.VIEW_CHANGED:
                    dropped++;
                    break;
                case TrackerStatusEvent.RECV_FAILURE:
                    errored++;
                    break;
                case TrackerStatusEvent.RECV_SUCCESS:
                    success++;
                    break;
            }
        }

        public Summary getSummary() {
            return new Summary(serviceName, methodName, data, success, errored, dropped);
        }

        public static class Summary {
            public final String serviceName;
            public final String methodName;

            private final int success;
            private final int errored;
            private final int dropped;

            public final int count;

            public final long slowest;
            public final long average;
            public final long fastest;
            public final long stddev;

            private Summary(String serviceName, String methodName, List<Long> data, int success, int errored, int dropped) {
                this.serviceName = serviceName;
                this.methodName = methodName;
                this.success = success;
                this.errored = errored;
                this.dropped = dropped;
                this.count = data.size();

                if (!data.isEmpty()) {
                    int i = 0;
                    long total = 0, slow = 0, fast = 0;
                    for (long next : data) {
                        if (i++ == 0) {
                            total = slow = fast = next;
                        } else {
                            total += next;
                            if (next > slow) {
                                slow = next;
                            } else if (next < fast) {
                                fast = next;
                            }
                        }
                    }
                    slowest = slow;
                    fastest = fast;
                    double avg = (total) / (double) count;

                    double sumOfSquares = 0;
                    for (long next : data) {
                        sumOfSquares += Math.pow(next - avg, 2);
                    }
                    stddev = (long) Math.pow(sumOfSquares / count, 0.5);
                    average = (long) avg;
                } else {
                    slowest = average = fastest = stddev = 0;
                }
            }

            public String toString() {
                StringBuilder builder = new StringBuilder();
                builder.append("serviceName=").append(serviceName).append('.');
                builder.append("methodeName=").append(methodName).append(": ");
                if (count < 1) {
                    builder.append("empty");
                } else {
                    builder.append("count=").append(count).append(',');
                    builder.append("success=").append(success).append(',');
                    builder.append("errored=").append(errored).append(',');
                    builder.append("dropped=").append(dropped).append(',');
                    builder.append("slowest=").append(slowest).append(',');
                    builder.append("average=").append(average).append(',');
                    builder.append("fastest=").append(fastest).append(',');
                    builder.append("stddev=").append(stddev);
                }
                return builder.toString();
            }
        }

    }

    private static Map<String, Record> statistics = new HashMap<String, Record>();

    private RemoteServiceStatistics() {
        // static access only
    }

    public static void record(String remoteService, long millis) {
        record(remoteService, millis, TrackerStatusEvent.RECV_SUCCESS);
    }

    public static void record(String remoteService, long millis, int status) {
        Record record = statistics.get(remoteService);
        if (record == null) {
            record = new Record(remoteService);
            statistics.put(remoteService, record);
        }
        record.record(millis, status);
    }

    public static String print(String remoteService) {
        Record record = statistics.get(remoteService);
        if (record == null) {
            record = new Record(remoteService);
        }
        return "RemoteServiceStatistics: " + remoteService + ": " + record.getSummary();
    }

    public static Summary get(String remoteService) {
        Record stat = statistics.get(remoteService);
        if (stat == null) {
            stat = new Record(remoteService);
        }
        return stat.getSummary();
    }

    public static List<Summary> getAll() {
        List<Summary> stats = new ArrayList<Summary>();

        for (String remoteService : statistics.keySet()) {
            stats.add(get(remoteService));
        }

        return stats;
    }

    public static void clearAll() {
        statistics.clear();
    }

    public static class RemoteServiceStatisticsListenerAdapter implements TrackerStatusEventListener {
        @Override
        public void onStatusChanged(TrackerStatusEvent event) {
            record(event.getName(), event.getAge(), event.getKind());
        }
    }

    // Not very glamorous, but this class has static data.
    public static void registerListeners(TrackerEventDispatcher dispatcher) {
        dispatcher.addTrackerStatusEventListener(new RemoteServiceStatisticsListenerAdapter());
    }
}
