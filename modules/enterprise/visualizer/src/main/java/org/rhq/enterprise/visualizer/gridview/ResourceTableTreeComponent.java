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
package org.rhq.enterprise.visualizer.gridview;

import org.rhq.enterprise.client.RemoteClient;
import org.rhq.enterprise.server.measurement.MeasurementDataManagerRemote;
import org.rhq.enterprise.visualizer.RandelshoferTreeNodeResource;
import org.rhq.enterprise.visualizer.Visualizer;
import org.rhq.enterprise.server.measurement.MeasurementDataManagerLocal;
import org.rhq.core.domain.measurement.MeasurementDefinition;
import org.rhq.core.domain.measurement.MeasurementData;
import org.rhq.core.domain.measurement.MeasurementDataNumeric;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceCategory;
import org.jdesktop.swingx.painter.*;
import org.jdesktop.swingx.JXPanel;

import javax.swing.*;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.ArrayList;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseAdapter;
import java.awt.geom.RoundRectangle2D;
import java.awt.geom.Point2D;
import java.text.NumberFormat;


/**
 * @author Greg Hinkle
 */
public class ResourceTableTreeComponent extends JXPanel {

    private RandelshoferTreeNodeResource node;
    private Resource resource;
    private List<GraphComponent> graphs = new ArrayList<GraphComponent>();
    private GridView parentView;
    private boolean mousedOver = false;


    public ResourceTableTreeComponent(RandelshoferTreeNodeResource child, GridView parentView) {
        this.node = child;
        this.parentView = parentView;

        this.setLayout(new BorderLayout());
        JLabel name = new JLabel("  " + child.getName());

        name.setFont(name.getFont().deriveFont(Font.PLAIN));
        this.add(new JLabel(child.getName()), BorderLayout.NORTH);

        setMinimumSize(new Dimension(100, 80));
        setPreferredSize(new Dimension(120, 60));

        resource = Visualizer.getResource(node.getId());

        addMouseListener(new MouseHandler());

        addGraphs();


        GlossPainter glossPainter = new GlossPainter();
        Color c = node.isUp() ? Visualizer.COLOR_UP : Visualizer.COLOR_DOWN;

        /*
        glossPainter.setPaint(new Color(c.getRed(), c.getGreen(), c.getBlue()));
        glossPainter.setPosition(GlossPainter.GlossPosition.TOP);

        ShapePainter sp = new ShapePainter(new RoundRectangle2D.Float(0, 0, getWidth() - 1, getHeight() - 1, 8, 8), c);

        CompoundPainter cp = new CompoundPainter(sp, glossPainter);

        Color blue = new Color(0x417DDD);
        Color translucent = new Color(blue.getRed(), blue.getGreen()-50, blue.getBlue(), 0);
        this.setBackground(c);
        this.setForeground(Color.LIGHT_GRAY);
        GradientPaint blueToTranslucent = new GradientPaint(
                new Point2D.Double(.4, 0),
                blue,
                new Point2D.Double(1, 0),
                translucent);
//        Painter veil =  new BasicGradientPainter(blueToTranslucent);
        Painter backgroundPainter = new ShapePainter(new RoundRectangle2D.Float(0, 0, getWidth() - 1, getHeight() - 1, 8, 8), c);

        Painter p = new CompoundPainter(backgroundPainter, glossPainter);*/
        //this.setBackgroundPainter(p);

//        this.setBackgroundPainter(cp);

        GradientPaint gradientPaint = new GradientPaint(
                new Point2D.Double(0, 100),
                c,
                new Point2D.Double(0, 0),
                alpha(Color.white, .5f));

        //        alpha(Color.white, .6f));

        Shape s = new RoundRectangle2D.Float(0, 0, getWidth() - 1, getHeight() - 1, 28, 28);

        // ShapePainter backgroundPainter =
        //     new ShapePainter(new RoundRectangle2D.Float(0, 0, 120 - 1, 60 - 1, 28, 28), alpha(c,0.5f), AbstractAreaPainter.Style.FILLED);
        MattePainter mp = new MattePainter(gradientPaint); //alpha(c, 0.8f));
        GlossPainter gp = new GlossPainter(alpha(Color.white, 0.3f), GlossPainter.GlossPosition.TOP);

        CheckerboardPainter pp = new CheckerboardPainter(alpha(Color.gray, 0.1f), alpha(Color.gray, 0.2f));

//        Painter veil =  new BasicGradientPainter(blueToTranslucent);

        CompoundPainter p = new CompoundPainter(/*backgroundPainter,*/ mp, pp, gp);

        p.setClipPreserved(true);
        this.setBackgroundPainter(p);

        this.setBackground(Color.white);

        ToolTipManager.sharedInstance().registerComponent(this);
    }


    public static Color alpha(Color c, float alpha) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), (int) (alpha * 255f));
    }


    private void addGraphs() {
        if (resource.getResourceType().getCategory() == ResourceCategory.PLATFORM) {
            RemoteClient client = Visualizer.getRemoteClient();

            graphs.add(new GraphComponent("Native.MemoryInfo.total", "Native.MemoryInfo.used", "Memory"));
            graphs.add(new GraphComponent(null, "CpuPerc.idle", "CPU", true));

        }

        if (!graphs.isEmpty()) {
            List<MeasurementDefinition> defs = Visualizer.getMeasurements(node);

            for (GraphComponent g : graphs) {
                for (MeasurementDefinition d : defs) {
                    if (d.getName().equals(g.getTopMetricName())) {
                        g.topMetric = d.getId();
                    }
                    if (d.getName().equals(g.getValueMetricName())) {
                        g.valueMetric = d.getId();
                    }
                }

            }
        }
        JPanel graphHolder = new JPanel(new FlowLayout(FlowLayout.LEFT));
        graphHolder.setOpaque(false);
        for (GraphComponent g : graphs) {
            graphHolder.add(g);
        }

        add(graphHolder, BorderLayout.CENTER);


    }

    public void refresh() {
        RemoteClient client = Visualizer.getRemoteClient();
        MeasurementDataManagerRemote measMan = client.getMeasurementDataManagerRemote();

        Set<Integer> defIds = new HashSet<Integer>();
        for (GraphComponent g : graphs) {
            int topMetric = g.getTopMetric();
            if (topMetric > 0) {
                defIds.add(topMetric);
            }
            defIds.add(g.getValueMetric());
        }


        try {
            int[] defArry = new int[defIds.size()];
            int i = 0;
            for (Integer id : defIds) {
                defArry[i++] = id.intValue();
            }

            Set<MeasurementData> latestData = measMan.findLiveData(client.getSubject(), this.resource.getId(), defArry);
            for (GraphComponent g : graphs) {
                double topValue = 0, value = 0;

                for (MeasurementData d : latestData) {
                    if (d.getName().equals(g.getTopMetricName())) {
                        topValue = ((MeasurementDataNumeric) d).getValue();
                    }
                    if (d.getName().equals(g.getValueMetricName())) {
                        value = ((MeasurementDataNumeric) d).getValue();
                    }
                }
                if (topValue != 0) {
                    value = value / topValue;
                }
                g.setValue(value);
            }
        } catch (Exception e) {
            System.out.println("Unable to update: " + this.resource);
        }
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;

        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

//        g2.setBackground(node.isUp() ? Color.green : Color.yellow);
        g2.setColor(node.isUp() ? Visualizer.COLOR_UP : Visualizer.COLOR_DOWN);

//        g2.fill3DRect(0, 0, getWidth() - 1, getHeight() - 1, true);
//        g2.draw3DRect(0, 0, getWidth() - 1, getHeight() - 1, true)
//        g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
//        g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);

        Shape s = new RoundRectangle2D.Float(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
        g2.setClip(s);
        super.paintComponent(g);

        if (mousedOver) {
            g2.setColor(alpha(Color.white, 0.2f));
            g2.fill(s);
        }

    }


    public String getToolTipText(MouseEvent event) {

        return Visualizer.getToolTipText(node);
    }

    public class GraphComponent extends JProgressBar {

        String title;
        int topMetric;
        String topMetricName;
        int valueMetric;
        String valueMetricName;
        double value;
        boolean invert;

        public GraphComponent(String topMetricName, String valueMetricName, String title) {
            this(topMetricName, valueMetricName, title, false);
        }

        public GraphComponent(String topMetricName, String valueMetricName, String title, boolean invert) {
            this.title = title;
            this.topMetricName = topMetricName;
            this.valueMetricName = valueMetricName;
            setPreferredSize(new Dimension(20, 35));
            setOrientation(VERTICAL);
            setToolTipText(title);
            setBorder(BorderFactory.createLoweredBevelBorder());
            ToolTipManager.sharedInstance().registerComponent(this);
            this.invert = invert;
        }


        @Override
        public String getToolTipText() {
            return title + ": " + NumberFormat.getPercentInstance().format(value);
        }


        /*public double getValue() {
            return value;
        }*/

        public void setValue(double value) {
            this.value = invert ? 1 - value : value;

            super.setMaximum(100);
            super.setValue((int) (100 * this.value));

        }

        public int getTopMetric() {
            return topMetric;
        }

        public int getValueMetric() {
            return valueMetric;
        }

        public String getTopMetricName() {
            return topMetricName;
        }

        public void setTopMetricName(String topMetricName) {
            this.topMetricName = topMetricName;
        }

        public String getValueMetricName() {
            return valueMetricName;
        }

        public void setValueMetricName(String valueMetricName) {
            this.valueMetricName = valueMetricName;
        }

        /*@Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g;

            Dimension d = getSize();

//            g2.setColor(Color.red);
//            g2.drawRect(0, 0, getWidth() - 1, getHeight() - 1);

            int y = (int) (((double) getHeight() - 3) * getValue());

            g2.setColor(Color.blue);
            g2.fillRect(1, getHeight() - y, getWidth() - 3, y);

            super.paintComponent(g);

        }*/
    }


    public RandelshoferTreeNodeResource getNode() {
        return node;
    }


    public class MouseHandler extends MouseAdapter {
        @Override
        public void mousePressed(MouseEvent e) {
            if (e.isPopupTrigger())
                Visualizer.getPopupMenuForResource(node).show(ResourceTableTreeComponent.this, e.getX(), e.getY());
            else
                parentView.setSelection(node);
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            if (e.isPopupTrigger())
                Visualizer.getPopupMenuForResource(node).show(ResourceTableTreeComponent.this, e.getX(), e.getY());
        }

        @Override
        public void mouseEntered(MouseEvent e) {
            super.mouseEntered(e);
            mousedOver = true;
            repaint();
        }

        @Override
        public void mouseExited(MouseEvent e) {
            super.mouseExited(e);
            mousedOver = false;
            repaint();
        }
    }

}
