/*
 * Copyright 2002-2004 Greg Hinkle
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.rhq.enterprise.visualizer;



import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.LogarithmicAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.DefaultXYItemRenderer;
import org.jfree.chart.title.TextTitle;
import org.jfree.data.time.Millisecond;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;


import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Set;

/**
 * This is an abstract base class for dynamic, running Graphs in MC4J. This
 * includes a base set of features like an included timer for adding
 * observations and a popup toolbox of chart controls to dynamically change
 * how the chart is displaying.
 *
 * @author Greg Hinkle (ghinkle@users.sourceforge.net), September 2002
 * @version $Revision: 573 $($Author: ghinkl $ / $Date: 2006-04-17 08:01:27 -0400 (Mon, 17 Apr 2006) $)
 */
public abstract class AbstractGraphPanel<T> extends JPanel {


    protected TimeSeriesCollection dataset = new TimeSeriesCollection();

    protected JFreeChart chart;
//    protected Timer dataGeneratorTimer;
//    protected TimerTask dataGeneratorTimerTask;

    protected Map<T,TimeSeries> timeSeriesMap = new HashMap<T, TimeSeries>();
    protected XYPlot xyplot;


    // These properties build and manage the control popup for the graph
    private JCheckBox controlsButton;
    private JPanel controlsPanel;

    private ButtonGroup buttonGroupScale;
    private ButtonGroup buttonGroupTimeRange;
    private JRadioButton jRadioButtonScaleLinear;
    private JRadioButton jRadioButtonScaleLogarithmic;
    private JRadioButton jRadioTimeHours;
    private JRadioButton jRadioTimeMinutes;
    private JRadioButton jRadioTimeSeconds;
    protected JLabel sleepDelay;


    /** Manages if the component is removed and re-added to a container */
    private boolean removed = false;

    protected int failures = 0;
    protected static final int MAX_FAILURES = 10;
    protected ChartPanel chartPanel;


    public AbstractGraphPanel() {
        setDoubleBuffered(false);
        initGraphPanel();
    }


    public String getChartTitle() {
        return chart.getTitle().getText();
    }
    public void setChartTitle(final String name) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                setName(name);
            }
        });

         chart.setTitle(
            new TextTitle(name,
            new Font("SansSerif",Font.BOLD, 12)));

    }

    protected void initGraphPanel() {

        //setPreferredSize(new Dimension(500,450));
        setBackground(Color.white);

        DateAxis domain = new DateAxis("Time");
        NumberAxis range = new NumberAxis("");

        this.xyplot = new XYPlot();
        this.xyplot.setDataset(dataset);
        this.xyplot.setDomainAxis(domain);
        this.xyplot.setRangeAxis(range);
        DefaultXYItemRenderer renderer = new DefaultXYItemRenderer();
        renderer.setShapesVisible(false);
        this.xyplot.setRenderer(renderer);

        domain.setAutoRange(true);
        domain.setLowerMargin(0.0);
        domain.setUpperMargin(0.0);
        domain.setTickLabelsVisible(true);

        range.setStandardTickUnits(NumberAxis.createIntegerTickUnits());

        this.chart =
            new JFreeChart(
                "",
                JFreeChart.DEFAULT_TITLE_FONT,
                xyplot,
                true);

//        chart.setTitle(
//            new TextTitle("Graph ???",
//                new Font("SansSerif",Font.BOLD, 12)));

        chartPanel = new ChartPanel(chart,false,true,true,false,false);
        //chartPanel.setPopupMenu(null);
        chartPanel.setOpaque(true);
        chartPanel.setDoubleBuffered(false);
        chart.setBackgroundPaint(this.getBackground());
        chartPanel.setBackground(this.getBackground());
        chartPanel.setBorder(new LineBorder(Color.BLACK,1));

        setLayout(new BorderLayout());
        setOpaque(false);
        add(chartPanel,BorderLayout.CENTER);

        buildGraphControls();

//        ControlsPopupButton dataButton = new ControlsPopupButton(new GraphSeriesPanel(dataset),"images/GraphElements.gif");
//        this.controlsButton = new ControlsPopupButton(this.controlsPanel,"images/GraphSettings2.gif");

        JPanel northPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
//        northPanel.add(dataButton);
//        northPanel.add(this.controlsButton);
        northPanel.setOpaque(false);
        add(northPanel,BorderLayout.NORTH);
        this.controlsPanel.setLocation(5,5);

        doLayout();
        repaint();
    }


    /**
     * A custom JCheckBox that will popup the controls toolbox for the chart
     */
    public static class ControlsPopupButton extends JCheckBox {

        private JPopupMenu popup;
        private Icon icon;//  = createImageIcon("images/GraphSettings2.gif");

        private JComponent payload;

        public ControlsPopupButton(JComponent payload, String img) {
            this.payload = payload;
            this.icon = createImageIcon(img);

            setIcon(icon);
            setOpaque(false);
            init();
        }

        /** Returns an ImageIcon, or null if the path was invalid. */
        protected ImageIcon createImageIcon(String path) {
//            java.net.URL imgURL = RefreshControlComponent.class.getClassLoader().getResource(path);
//            if (imgURL != null) {
//                return new ImageIcon(imgURL);
//            } else {
//                System.err.println("Couldn't find file: " + path);
//                return null;
//            }
            return null;
        }

        public void showPopup() {
//            GlassWindow.show(this.payload, this);
            //JXGlassBox gb = new JXGlassBox(0.8f);
//            gb.add(this.payload);
//            gb.setDismissOnClick(true);
//            gb.setVisible(true);
//            gb.showOnGlassPane((Container) this.getRootPane().getGlassPane(),this.getX() - this.payload.getWidth(), this.getY() + this.getHeight());
//            if (popup == null) {
//                popup = new JPopupMenu();
//                popup.add(this.payload);
//            }
//            popup.show(this, 0, this.getHeight());
        }

        private void init() {
            addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    showPopup();
                }
            });
        }
    }



    public long getUpdateDelay() {
//        if (sleepSlider != null)
//            return sleepSlider.getValue();
//        else
            return 1000L;
    }

    /**
     * Cancels any existing scheduled task and reschedules for the
     * current status of the delay control.
     */
    public void reschedule() {
//        if (this.dataGeneratorTimer == null) {
//            this.dataGeneratorTimer = new Timer();
//        }
//
//        if (this.dataGeneratorTimerTask != null) {
//            try {
//                this.dataGeneratorTimerTask.cancel();
//            } catch(Exception e)  { e.printStackTrace();}
//        }
//
//        this.dataGeneratorTimer.schedule(
//            this.dataGeneratorTimerTask = getDataGeneratorTask(),
//            getUpdateDelay(),
//            getUpdateDelay());

    }

    /**
     * Stops taking measurements for the graph.
     */
    public void pauseSchedule() {
//        this.dataGeneratorTimerTask.cancel();
//        this.dataGeneratorTimerTask = null;
    }


    protected TimerTask getDataGeneratorTask() {
        return new DataGenerator();
    }


    public void addNotify() {
        super.addNotify();
        if (removed) {
            reschedule();
            this.removed = false;
        }
    }


    public void removeNotify() {
        super.removeNotify();
//        if (this.dataGeneratorTimer != null)
//            this.dataGeneratorTimer.cancel();
//        this.dataGeneratorTimer = null;
        this.removed = true;
    }



    /**
     * Define this graph as logarithmic or not
     * @param logarithmic if true, graph will be logarithmic,
     *   otherwise it will be linear
     */
    public void setLogarithmic(boolean logarithmic) {
        if (logarithmic) {
            try {
                this.xyplot.setRangeAxis(new LogarithmicAxis(""));
            } catch(Exception e) {
                // In case we try to use a logarithmic axis when the values go below 1
                this.xyplot.setRangeAxis(new NumberAxis(""));
            }
        } else {
            this.xyplot.setRangeAxis(new NumberAxis(""));
        }
    }

    public void setBackground(Color bg) {
        super.setBackground(bg);
        //this.sleepSlider.setBackground(bg);
    }

    public void updateRange() {
        DateAxis dateAxis = (DateAxis) this.xyplot.getDomainAxis();

        Calendar start = new GregorianCalendar();

        start.setTime(new Date());

        if (this.jRadioTimeSeconds.isSelected()) {
            start.add(Calendar.SECOND, -60);
        } else if (this.jRadioTimeMinutes.isSelected()) {
            //this.sleepSlider.setEnabled(false);
            start.add(Calendar.MINUTE, -60);
        } else if (this.jRadioTimeHours.isSelected()) {
            start.add(Calendar.HOUR, -48);
        }

        // this is a hack to slow down the data gathering for the longer periods...
        // TODO GH: Build a TimeSeries that modulates the time periods automatically
        // i.e.
        //    |     Seconds     |     Minutes     |     Hours     |
        //     ***************** * * * * * * * * *   *   *   *   *
//        if (this.jRadioTimeMinutes.isSelected()) {
//            sleepSlider.setValue(10000);
//        } else if (this.jRadioTimeHours.isSelected()) {
//            sleepSlider.setValue(100000);
//        }


        dateAxis.setRange(start.getTime(),new Date());
    }


    protected JPanel buildGraphControls() {

        this.controlsPanel = new JPanel();
        //this.controlsPanel.setBorder(BorderFactory.createLineBorder(Color.black));
        this.controlsPanel.setLayout(new BoxLayout(this.controlsPanel,BoxLayout.X_AXIS));

        // Seconds, Minutes, Hours
        JPanel timeDurationPanel = new JPanel();
        timeDurationPanel.setLayout(new BoxLayout(timeDurationPanel, BoxLayout.Y_AXIS));
        timeDurationPanel.setBorder(BorderFactory.createTitledBorder("Time Range"));

        buttonGroupTimeRange = new javax.swing.ButtonGroup();

        jRadioTimeSeconds = new javax.swing.JRadioButton("seconds", true);
        jRadioTimeSeconds.setToolTipText("Track the graph for 60 seconds");
        jRadioTimeSeconds.setOpaque(false);

        jRadioTimeMinutes = new javax.swing.JRadioButton("minutes", false);
        jRadioTimeMinutes.setToolTipText("Track this graph for 60 minutes.");
        jRadioTimeMinutes.setOpaque(false);

        jRadioTimeHours = new javax.swing.JRadioButton("hours", false);
        jRadioTimeHours.setToolTipText("Track this graph for 48 hours.");
        jRadioTimeHours.setOpaque(false);

        buttonGroupTimeRange.add(jRadioTimeSeconds);
        buttonGroupTimeRange.add(jRadioTimeMinutes);
        buttonGroupTimeRange.add(jRadioTimeHours);

        timeDurationPanel.add(jRadioTimeSeconds);
        timeDurationPanel.add(jRadioTimeMinutes);
        timeDurationPanel.add(jRadioTimeHours);

        // Scale (linear vs. logarithmic)
        JPanel timeScalePanel = new JPanel();
        timeScalePanel.setLayout(new BoxLayout(timeScalePanel, BoxLayout.Y_AXIS));
        timeScalePanel.setBorder(BorderFactory.createTitledBorder("Time Scale"));

        buttonGroupScale = new javax.swing.ButtonGroup();

        jRadioButtonScaleLinear = new javax.swing.JRadioButton("Linear", true);
        jRadioButtonScaleLinear.setOpaque(false);

        jRadioButtonScaleLogarithmic = new javax.swing.JRadioButton("Logarithmic", false);
        jRadioButtonScaleLogarithmic.setOpaque(false);

        buttonGroupScale.add(jRadioButtonScaleLinear);
        buttonGroupScale.add(jRadioButtonScaleLogarithmic);

        timeScalePanel.add(jRadioButtonScaleLinear);
        timeScalePanel.add(jRadioButtonScaleLogarithmic);

        // Update speed (in milleseconds)
        JPanel updateSpeedPanel = new JPanel();
        updateSpeedPanel.setLayout(new BoxLayout(updateSpeedPanel, BoxLayout.Y_AXIS));
        updateSpeedPanel.setBorder(BorderFactory.createTitledBorder("Update Speed"));

        //sleepSlider = new javax.swing.JSlider(100,10000,1000);
        //sleepSlider.setPaintLabels(true);
        //sleepSlider.setPaintTicks(true);
        //sleepSlider.setMinorTickSpacing(500);
        //sleepSlider.setMajorTickSpacing(2000);

//        sleepSlider = new LogarithmicTimeJSlider(100, 100000, 1000);
//
//        sleepSlider.setPaintTicks(true);
//        sleepSlider.setPaintLabels(true);
//        sleepSlider.setMajorTickSpacing(10);
//        sleepSlider.setMinorTickSpacing(10);
//
//        sleepSlider.setToolTipText("Time between updates");
//        sleepSlider.setOpaque(false);

        sleepDelay = new JLabel("Delay: ");// + sleepSlider.getTime());

//        sleepSlider.addChangeListener(new javax.swing.event.ChangeListener() {
//            public void stateChanged(javax.swing.event.ChangeEvent evt) {
//                sleepSliderPropertyChange(null);
//                sleepDelay.setText("Delay: " + sleepSlider.getTime());
//            }
//        });


//        updateSpeedPanel.add(sleepSlider);
        updateSpeedPanel.add(sleepDelay);

        jRadioButtonScaleLinear.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                scaleChanged(evt);
            }
        });

        jRadioButtonScaleLogarithmic.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                scaleChanged(evt);
            }
        });


        this.controlsPanel.add(timeDurationPanel);
        this.controlsPanel.add(updateSpeedPanel);
        this.controlsPanel.add(timeScalePanel);

        return this.controlsPanel;
    }


    private void scaleChanged(javax.swing.event.ChangeEvent evt) {
        setLogarithmic(jRadioButtonScaleLogarithmic.isSelected());
    }


    private void sleepSliderPropertyChange(java.beans.PropertyChangeEvent evt) {
//        if (!sleepSlider.getValueIsAdjusting()) {
//            reschedule();
//        }
    }


    /**
     * Should be overridden by non-abstract subclasses to provide an observation
     * of all time series datasets.
     */
    public abstract void addObservation() throws Exception;

    public void addObservationHandler() {
        try {
            addObservation();
            if (failures > 0)
                failures--;
        } catch (Exception e) {
            failures++;
//            org.openide.windows.IOProvider.getDefault().getStdOut().println(e);
        }
        if (failures > MAX_FAILURES) {
            pauseSchedule();
        }

        updateRange();

    }

    /**
     *
     * @param name The attribute name
     * @param key The key to the value, usually an EmsAttribute
     */
    protected void createTimeSeries(String name, T key) {
        TimeSeries ts = new TimeSeries(name, Millisecond.class);
        ts.setMaximumItemAge(1000 * 60 * 60 * 48); // 48 hours
        this.timeSeriesMap.put(key, ts);
        //ts.setKey((Comparable) key); Duh, don't do this
        dataset.addSeries(ts);
    }

    protected TimeSeries getTimeSeries(T key) {
        return this.timeSeriesMap.get(key);
    }

    public Set<T> getTimeSeriesKeys() {
        return this.timeSeriesMap.keySet();
    }

    /**
     * The data generator.
     */
    protected class DataGenerator extends TimerTask {

        public void run() {
            addObservationHandler();
        }
    }



    private static class TestGraph extends AbstractGraphPanel {
        public static final String TEST = "test";
        private int x = 1;
        public TestGraph() {
            createTimeSeries(TEST, TEST);
            reschedule();
        }
        public void addObservation() throws Exception {
            getTimeSeries(TEST).
                add(new Millisecond(), new Integer(x++));
        }
    }

    public static void main(String[] args) {
        TestGraph graph = new TestGraph();

        JFrame frame = new JFrame();
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        frame.getContentPane().add(panel);
        panel.add(graph, BorderLayout.CENTER);

        frame.pack();
        frame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                System.exit(0);
            }
        });
        frame.setVisible(true);
    }

}