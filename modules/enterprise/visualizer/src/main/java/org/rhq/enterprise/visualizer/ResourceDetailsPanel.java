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
package org.rhq.enterprise.visualizer;

import org.jfree.data.time.Millisecond;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.criteria.MeasurementDefinitionCriteria;
import org.rhq.core.domain.measurement.DataType;
import org.rhq.core.domain.measurement.DisplayType;
import org.rhq.core.domain.measurement.MeasurementData;
import org.rhq.core.domain.measurement.MeasurementDataNumeric;
import org.rhq.core.domain.measurement.MeasurementDefinition;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.enterprise.client.RemoteClient;
import org.rhq.enterprise.server.measurement.MeasurementDataManagerRemote;
import org.rhq.enterprise.server.measurement.MeasurementDefinitionManagerRemote;
import org.rhq.enterprise.server.report.DataAccessManagerRemote;
import org.rhq.enterprise.server.resource.ResourceManagerRemote;
import org.rhq.enterprise.server.resource.ResourceTypeManagerRemote;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Greg Hinkle
 */
public class ResourceDetailsPanel extends JPanel {

    RemoteClient client;
    RandelshoferTreeNodeResource node;
    Subject subject;
    Resource resource;
    List<MeasurementDefinition> defs = new ArrayList<MeasurementDefinition>();

    String measurement;
    Set<Integer> defIds = new HashSet<Integer>();
    Map<String, MeasurementDefinition> defMap = new HashMap<String, MeasurementDefinition>();
    long lastCheck;
    Map<String, Double> data = new HashMap<String, Double>();
    List<MetricGraph> graphs = new ArrayList<MetricGraph>();

    Thread t;
    boolean running = true;

    public ResourceDetailsPanel(Subject subject, RemoteClient client, RandelshoferTreeNodeResource node, String measurement) {
        this.subject = subject;
        this.client = client;
        this.node = node;
        this.measurement = measurement;
        init();
        //setPreferredSize(new Dimension(550, 240));
    }

    public static void display(Subject subject, RemoteClient client, RandelshoferTreeNodeResource node) {
        display(subject, client, node, null);
    }
    public static void display(Subject subject, RemoteClient client, RandelshoferTreeNodeResource node, String measurementDef) {

        JFrame frame = new JFrame(node.getName() + " Details");
        frame.setSize(550, 240);

//        JScrollPane p = new JScrollPane();

        frame.setContentPane(new ResourceDetailsPanel(subject, client, node, measurementDef));
        frame.setVisible(true);

        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    }


    public void init() {

        ResourceManagerRemote resoruceManager = client.getResourceManagerRemote();
        ResourceTypeManagerRemote resourceTypeManager = client.getResourceTypeManagerRemote();
        MeasurementDefinitionManagerRemote measDef = client.getMeasurementDefinitionManagerRemote();
        DataAccessManagerRemote dataAccess = client.getDataAccessManagerRemote();

        this.resource = resoruceManager.getResource(subject, node.getId());

        ResourceType type = resourceTypeManager.getResourceTypeByNameAndPlugin(
                client.getSubject(),
                this.resource.getResourceType().getName(),
                this.resource.getResourceType().getPlugin());

        List typeData = dataAccess.executeQuery(subject, "SELECT rt.id FROM ResourceType rt WHERE LOWER(rt.name) = LOWER('" +
                this.resource.getResourceType().getName() +
                "') AND rt.plugin = '" +
                this.resource.getResourceType().getPlugin() +
                "'");

        int typeId = ((Number) typeData.get(0)).intValue();

        MeasurementDefinitionCriteria criteria = new MeasurementDefinitionCriteria();
        criteria.addFilterResourceTypeId(typeId);
        defs = measDef.findMeasurementDefinitionsByCriteria(subject, criteria);

        for (MeasurementDefinition def : defs) {
            if (def.getDataType() == DataType.MEASUREMENT) {

                if ((this.measurement == null && def.getDisplayType() == DisplayType.SUMMARY)|| (this.measurement != null && this.measurement.equals(def.getName()))) {
                    defIds.add(def.getId());
                    defMap.put(def.getName(), def);
                }
            }
        }


        if (defMap.size() == 1) {
            setLayout(new BorderLayout());
        } else {
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        }

        for (MeasurementDefinition def : defMap.values()) {
            MetricGraph graph = new MetricGraph(def.getName(), def.getDisplayName());
            graphs.add(graph);
            add(graph);
        }

        t = new Thread(new Runnable() {
            public void run() {
                while (running) {
                    refresh();

                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        t.start();
    }

    public void refresh() {
        MeasurementDataManagerRemote measMan = client.getMeasurementDataManagerRemote();

        long start = System.currentTimeMillis();

        int[] defIdArray = new int[defIds.size()];
        int i = 0;
        for (Integer id : defIds) {
            defIdArray[i] = id;
        }

        Set<MeasurementData> latestData = measMan.findLiveData(client.getSubject(), this.resource.getId(), defIdArray);
        System.out.println(System.currentTimeMillis() - start);
        for (MeasurementData d : latestData) {
            MeasurementDefinition def = defMap.get(d.getName());
            data.put(d.getName(), ((MeasurementDataNumeric) d).getValue());

//            System.out.println(def.getName() + ": " + d.getValue());
        }
        lastCheck = System.currentTimeMillis();

        for (MetricGraph graph : graphs) {
            graph.addObservationHandler();
        }

    }


    private class MetricGraph extends AbstractGraphPanel {
        private String timeSeries;

        public MetricGraph(String timeSeriesName, String title) {
            this.timeSeries = timeSeriesName;
            createTimeSeries(title, timeSeriesName);
            reschedule();
        }

        public void addObservation() {

            if (lastCheck < (System.currentTimeMillis() - 2000L)) {
                refresh();
            }
            getTimeSeries(this.timeSeries).add(
                    new Millisecond(), data.get(timeSeries));
        }
    }

    public void removeNotify() {
        super.removeNotify();
        this.running = false;
    }
}
