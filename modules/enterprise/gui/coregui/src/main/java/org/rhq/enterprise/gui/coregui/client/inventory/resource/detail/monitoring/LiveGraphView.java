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

import java.util.Date;
import java.util.Set;

import ca.nanometrics.gflot.client.Axis;
import ca.nanometrics.gflot.client.DataPoint;
import ca.nanometrics.gflot.client.PlotItem;
import ca.nanometrics.gflot.client.PlotModel;
import ca.nanometrics.gflot.client.PlotModelStrategy;
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

import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.types.AnimationEffect;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.HTMLFlow;
import com.smartgwt.client.widgets.Label;
import com.smartgwt.client.widgets.Window;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.events.ClickHandler;
import com.smartgwt.client.widgets.events.CloseClickHandler;
import com.smartgwt.client.widgets.events.CloseClientEvent;
import com.smartgwt.client.widgets.events.MouseOutEvent;
import com.smartgwt.client.widgets.events.MouseOutHandler;

import org.rhq.core.domain.measurement.MeasurementConverterClient;
import org.rhq.core.domain.measurement.MeasurementData;
import org.rhq.core.domain.measurement.MeasurementDataNumeric;
import org.rhq.core.domain.measurement.MeasurementDefinition;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.gwt.MeasurementDataGWTServiceAsync;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableVLayout;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableWidgetCanvas;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableWindow;

/**
 * @author Greg Hinkle
 */
public class LiveGraphView extends LocatableVLayout {

    private static final String INSTRUCTIONS = MSG.view_resource_monitor_graph_instructions();

    private final Label selectedPointLabel = new Label(INSTRUCTIONS);
    private final Label positionLabel = new Label();

    private final Label hoverLabel = new Label();

    private int resourceId;
    private MeasurementDefinition definition;

    private SimplePlot plot;
    private Timer dataLoader;
    private long min, max;

    public LiveGraphView(String locatorId) {
        super(locatorId);
    }

    public LiveGraphView(String locatorId, int resourceId, MeasurementDefinition def) {
        super(locatorId);
        this.resourceId = resourceId;
        this.definition = def;
        setHeight100();
        setWidth100();
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
    protected void onDetach() {
        super.onDetach(); // TODO: Implement this method.
    }

    @Override
    protected void onUnload() {
        super.onUnload(); // TODO: Implement this method.
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
        plotOptions
            .setGridOptions(new GridOptions().setHoverable(true).setMouseActiveRadius(10).setAutoHighlight(true));

        // create a series
        loadData(model, plotOptions);

        // create the plot
        plot = new SimplePlot(model, plotOptions);
        plot.setSize(String.valueOf(getInnerContentWidth()), String.valueOf(getInnerContentHeight() - 20));

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

                    selectedPointLabel.setContents("x: " + item.getDataPoint().getX() + ", y: "
                        + item.getDataPoint().getY());
                } else {
                    hoverLabel.animateHide(AnimationEffect.FADE);
                    selectedPointLabel.setContents(INSTRUCTIONS);
                }
            }
        }, false);

        addMouseOutHandler(new MouseOutHandler() {
            public void onMouseOut(MouseOutEvent mouseOutEvent) {
                hoverLabel.animateHide(AnimationEffect.FADE);
            }
        });

        hoverLabel.setOpacity(80);
        hoverLabel.setWrap(false);
        hoverLabel.setHeight(25);
        hoverLabel.setBackgroundColor("yellow");
        hoverLabel.setBorder("1px solid orange");
        hoverLabel.hide();

        hoverLabel.draw();

        // put it on a panel

        if (definition != null) {

            HTMLFlow title = new HTMLFlow("<b>" + definition.getDisplayName() + "</b> " + definition.getDescription());
            title.addClickHandler(new ClickHandler() {
                public void onClick(ClickEvent clickEvent) {
                    //                    displayAsDialog();
                }
            });

            addMember(title);
        }

        addMember(new LocatableWidgetCanvas(this.getLocatorId(), plot));
    }

    private String getHover(PlotItem item) {
        if (definition != null) {
            DateTimeFormat df = DateTimeFormat.getMediumDateTimeFormat();
            return definition.getDisplayName() + ": "
                + MeasurementConverterClient.format(item.getDataPoint().getY(), definition.getUnits(), true) + "<br/>"
                + df.format(new Date((long) item.getDataPoint().getX()));
        } else {
            return "x: " + item.getDataPoint().getX() + ", y: " + item.getDataPoint().getY();
        }
    }

    private void loadData(final PlotModel model, final PlotOptions plotOptions) {
        final SeriesHandler handler = model.addSeries(definition.getDisplayName(), "#007f00");

        model.setStrategy(PlotModelStrategy.slidingWindowStrategy(60));
        final MeasurementDataGWTServiceAsync dataService = GWTServiceLookup.getMeasurementDataService();

        dataLoader = new Timer() {
            @Override
            public void run() {
                dataService.findLiveData(resourceId, new int[] { definition.getId() },
                    new AsyncCallback<Set<MeasurementData>>() {
                        public void onFailure(Throwable caught) {
                            CoreGUI.getErrorHandler()
                                .handleError(MSG.view_resource_monitor_graphs_loadFailed(), caught);
                        }

                        public void onSuccess(Set<MeasurementData> result) {
                            MeasurementDataNumeric d = (MeasurementDataNumeric) result.iterator().next();

                            handler.add(new DataPoint(d.getTimestamp(), d.getValue()));
                            plot.redraw();

                            if (d.getTimestamp() > max) {
                                max = System.currentTimeMillis();
                                min = max - (1000L * 60);

                                //                            plotOptions.setXAxisOptions(new AxisOptions().setMinimum(min).setMaximum(max));
                            }

                        }
                    });
            }
        };

        dataLoader.scheduleRepeating(1000);

        plotOptions.setYAxisOptions(new AxisOptions().setLabelWidth(70).setTicks(5).setTickFormatter(
            new TickFormatter() {
                public String formatTickValue(double v, Axis axis) {
                    return MeasurementConverterClient.format(v, definition.getUnits(), true);
                }
            }));

        min = System.currentTimeMillis();
        max = System.currentTimeMillis() + (1000L * 60);

        plotOptions.setXAxisOptions(new AxisOptions().setTicks(8).setTickFormatter(new TickFormatter() {
            public String formatTickValue(double tickValue, Axis axis) {
                DateTimeFormat dateFormat = DateTimeFormat.getMediumTimeFormat();
                return dateFormat.format(new Date((long) tickValue));
            }
        }));

    }

    public static void displayAsDialog(String locatorId, int resourceId, MeasurementDefinition def) {
        final LiveGraphView graph = new LiveGraphView(locatorId, resourceId, def);
        final Window graphPopup = new LocatableWindow(locatorId);
        graphPopup.setTitle(MSG.view_resource_monitor_detailed_graph_label());
        graphPopup.setWidth(800);
        graphPopup.setHeight(400);
        graphPopup.setIsModal(true);
        graphPopup.setShowModalMask(true);
        graphPopup.setCanDragResize(true);
        graphPopup.centerInPage();
        graphPopup.addItem(graph);
        graphPopup.show();

        graphPopup.addCloseClickHandler(new CloseClickHandler() {
            public void onCloseClick(CloseClientEvent closeClientEvent) {
                graph.stop();
                graphPopup.destroy();
            }
        });
    }

    protected void stop() {
        dataLoader.cancel();
        hoverLabel.destroy();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        dataLoader.cancel();
        hoverLabel.destroy();
    }
}