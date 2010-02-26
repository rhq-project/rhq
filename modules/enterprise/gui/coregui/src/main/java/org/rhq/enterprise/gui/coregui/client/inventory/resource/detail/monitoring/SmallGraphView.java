/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
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
package org.rhq.enterprise.gui.coregui.client.inventory.resource.detail.monitoring;

import ca.nanometrics.gflot.client.Axis;
import ca.nanometrics.gflot.client.DataPoint;
import ca.nanometrics.gflot.client.PlotItem;
import ca.nanometrics.gflot.client.PlotModel;
import ca.nanometrics.gflot.client.PlotPosition;
import ca.nanometrics.gflot.client.SeriesHandler;
import ca.nanometrics.gflot.client.SimplePlot;
import ca.nanometrics.gflot.client.event.PlotHoverListener;
import ca.nanometrics.gflot.client.jsni.Plot;
import ca.nanometrics.gflot.client.options.AxisOptions;
import ca.nanometrics.gflot.client.options.GridOptions;
import ca.nanometrics.gflot.client.options.LineSeriesOptions;
import ca.nanometrics.gflot.client.options.PlotOptions;
import ca.nanometrics.gflot.client.options.PointsSeriesOptions;
import ca.nanometrics.gflot.client.options.TickFormatter;

import org.rhq.core.domain.measurement.MeasurementConverterClient;
import org.rhq.core.domain.measurement.MeasurementDefinition;
import org.rhq.core.domain.measurement.MeasurementUnits;
import org.rhq.core.domain.measurement.composite.MeasurementDataNumericHighLowComposite;

import com.google.gwt.i18n.client.DateTimeFormat;
import com.smartgwt.client.types.AnimationEffect;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.HTMLFlow;
import com.smartgwt.client.widgets.Label;
import com.smartgwt.client.widgets.WidgetCanvas;
import com.smartgwt.client.widgets.layout.VLayout;

import java.util.Date;
import java.util.List;

/**
 * @author Greg Hinkle
 */
public class SmallGraphView extends VLayout {

    private static final String INSTRUCTIONS = "Point your mouse to a data point on the chart";

    private static final String[] MONTH_NAMES = {"jan", "feb", "mar", "apr", "may", "jun", "jul", "aug", "sep", "oct", "nov", "dec"};

    private final Label selectedPointLabel = new Label(INSTRUCTIONS);
    private final Label positionLabel = new Label();

    private final Label hoverLabel = new Label();

    private MeasurementDefinition definition;
    private List<MeasurementDataNumericHighLowComposite> data;


    public SmallGraphView() {
        super();
    }

    public SmallGraphView(MeasurementDefinition def, List<MeasurementDataNumericHighLowComposite> data) {
        super();
        this.definition = def;
        this.data = data;
//        setHeight(250);
        setHeight100();
        setWidth100();
//        setPadding(10);


    }

    public String getName() {
        return "PlotHoverListener";
    }

    @Override
    protected void onDraw() {
        super.onDraw();

        for (Canvas c : getChildren()) {
            c.destroy();
        }

        drawGraph();
    }


    @Override
    public void parentResized() {
        super.parentResized();
        onDraw();
    }

    private void drawGraph() {

        PlotModel model = new PlotModel();
        PlotOptions plotOptions = new PlotOptions();
        plotOptions.setDefaultLineSeriesOptions(new LineSeriesOptions().setLineWidth(1).setShow(true));
        plotOptions.setDefaultPointsOptions(new PointsSeriesOptions().setRadius(2).setShow(true));
        plotOptions.setDefaultShadowSize(0);



        // You need make the grid hoverable <<<<<<<<<
        plotOptions.setGridOptions(new GridOptions().setHoverable(true).setMouseActiveRadius(10).setAutoHighlight(true));


        // create a series
        if (definition != null && data != null) {
            loadData(model, plotOptions);
        } else {
            loadFakeData(model, plotOptions);
        }

        // create the plot
        SimplePlot plot = new SimplePlot(model, plotOptions);
        plot.setSize(String.valueOf(getInnerContentWidth()), String.valueOf(getInnerContentHeight() - 20));
//                "80%","80%");



        // add hover listener
        plot.addHoverListener(new PlotHoverListener() {
            public void onPlotHover(Plot plot, PlotPosition position, PlotItem item) {
                if (position != null) {
                    positionLabel.setContents("position: (" + position.getX() + "," + position.getY() + ")");
                }
                if (item != null) {
                    hoverLabel.setContents(getHover(item));

                    hoverLabel.animateShow(AnimationEffect.FADE);
                    if (hoverLabel.getLeft() > 0 || hoverLabel.getTop() > 0) {
                        hoverLabel.animateMove(item.getPageX() + 5, item.getPageY() + 5);
                    } else {
                        hoverLabel.moveTo(item.getPageX() + 5, item.getPageY() + 5);
                    }
                    hoverLabel.redraw();

                    selectedPointLabel.setContents("x: " + item.getDataPoint().getX() + ", y: " + item.getDataPoint().getY());
                } else {
                    hoverLabel.animateHide(AnimationEffect.FADE);
                    selectedPointLabel.setContents(INSTRUCTIONS);
                }
            }
        }, false);

        hoverLabel.setOpacity(80);
        hoverLabel.setWrap(false);
        hoverLabel.setHeight(25);
        hoverLabel.setBackgroundColor("yellow");
        hoverLabel.setBorder("1px solid orange");
        hoverLabel.hide();

        hoverLabel.draw();

        // put it on a panel

        if (definition != null) {
            addMember(new HTMLFlow("<b>" + definition.getDisplayName() + "</b> " + definition.getDescription()));
        }

        addMember(new WidgetCanvas(plot));
    }

    private String getHover(PlotItem item) {
        if (definition != null) {
            com.google.gwt.i18n.client.DateTimeFormat df = DateTimeFormat.getMediumDateTimeFormat();            
            return definition.getDisplayName() + ": " + MeasurementConverterClient.format(item.getDataPoint().getY(),definition.getUnits(), true)
                    + "<br/>" + df.format(new Date((long) item.getDataPoint().getX()));
        } else {
            return "x: " + item.getDataPoint().getX() + ", y: " + item.getDataPoint().getY();
        }
    }

    private void loadData(PlotModel model, PlotOptions plotOptions) {
        SeriesHandler handler = model.addSeries(definition.getDisplayName(), "#007f00");

        for (MeasurementDataNumericHighLowComposite d : data) {
            handler.add(new DataPoint(d.getTimestamp(), d.getValue()));
        }

        plotOptions.setYAxisOptions(new AxisOptions().setTicks(5).setTickFormatter(new TickFormatter() {
            public String formatTickValue(double v, Axis axis) {
                return MeasurementConverterClient.format(v, definition.getUnits(), true);
            }
        }));

        long max = System.currentTimeMillis();
        long min = max - (1000L * 60 * 60 *8);

        plotOptions.setXAxisOptions(new AxisOptions().setTicks(8). setMinimum(min).setMaximum(max).setTickFormatter(new TickFormatter() {
            public String formatTickValue(double tickValue, Axis axis) {
                com.google.gwt.i18n.client.DateTimeFormat dateFormat = DateTimeFormat.getShortDateTimeFormat();
                return dateFormat.format(new Date((long)tickValue));
//                return String.valueOf(new Date((long) tickValue));
//                return MONTH_NAMES[(int) (tickValue - 1)];
            }
        }));

    }

    private void loadFakeData(PlotModel model, PlotOptions plotOptions) {
        SeriesHandler handler = model.addSeries("Ottawa's Month Temperatures", "#007f00");

        // add data
        handler.add(new DataPoint(1, -10.5));
        handler.add(new DataPoint(2, -8.6));
        handler.add(new DataPoint(3, -2.4));
        handler.add(new DataPoint(4, 6));
        handler.add(new DataPoint(5, 13.6));
        handler.add(new DataPoint(6, 18.4));
        handler.add(new DataPoint(7, 21));
        handler.add(new DataPoint(8, 19.7));
        handler.add(new DataPoint(9, 14.7));
        handler.add(new DataPoint(10, 8.2));
        handler.add(new DataPoint(11, 1.5));
        handler.add(new DataPoint(12, -6.6));

        plotOptions.setXAxisOptions(new AxisOptions().setTicks(12).setTickFormatter(new TickFormatter() {
            public String formatTickValue(double tickValue, Axis axis) {
                return MONTH_NAMES[(int) (tickValue - 1)];
            }
        }));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        hoverLabel.destroy();
    }
}
