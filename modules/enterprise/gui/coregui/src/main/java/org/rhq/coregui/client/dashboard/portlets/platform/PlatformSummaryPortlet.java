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
package org.rhq.coregui.client.dashboard.portlets.platform;

import com.smartgwt.client.types.AutoFitWidthApproach;
import com.smartgwt.client.types.Autofit;
import com.smartgwt.client.types.Overflow;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.HTMLFlow;
import com.smartgwt.client.widgets.Img;
import com.smartgwt.client.widgets.Label;
import com.smartgwt.client.widgets.grid.CellFormatter;
import com.smartgwt.client.widgets.grid.ListGrid;
import com.smartgwt.client.widgets.grid.ListGridField;
import com.smartgwt.client.widgets.grid.ListGridRecord;
import com.smartgwt.client.widgets.layout.HLayout;

import org.rhq.core.domain.common.EntityContext;
import org.rhq.core.domain.dashboard.DashboardPortlet;
import org.rhq.core.domain.measurement.MeasurementUnits;
import org.rhq.core.domain.resource.composite.PlatformMetricsSummary.CPUMetric;
import org.rhq.core.domain.resource.composite.PlatformMetricsSummary.MemoryMetric;
import org.rhq.core.domain.resource.composite.PlatformMetricsSummary.SwapMetric;
import org.rhq.coregui.client.IconEnum;
import org.rhq.coregui.client.LinkManager;
import org.rhq.coregui.client.components.ReportExporter;
import org.rhq.coregui.client.components.table.AbstractTableAction;
import org.rhq.coregui.client.components.table.IconField;
import org.rhq.coregui.client.components.table.Table;
import org.rhq.coregui.client.components.view.HasViewName;
import org.rhq.coregui.client.components.view.ViewName;
import org.rhq.coregui.client.dashboard.Portlet;
import org.rhq.coregui.client.dashboard.PortletViewFactory;
import org.rhq.coregui.client.dashboard.PortletWindow;
import org.rhq.coregui.client.inventory.resource.ResourceDataSourceField;
import org.rhq.coregui.client.util.MeasurementConverterClient;
import org.rhq.coregui.client.util.enhanced.EnhancedIButton.ButtonColor;


public class PlatformSummaryPortlet extends Table<PlatformMetricDataSource> implements Portlet, HasViewName {

    public static final ViewName VIEW_ID = new ViewName("PlatformUtilization", MSG.view_reports_platforms(),
        IconEnum.PLATFORM_UTILIZATION);

    // A non-displayed, persisted identifier for the portlet
    public static final String KEY = "PlatformSummary";
    // A default displayed, persisted name for the portlet    
    public static final String NAME = MSG.view_portlet_defaultName_platformSummary();

    public static final String FIELD_CPU = "cpu";
    public static final String FIELD_MEMORY = "memory";
    public static final String FIELD_SWAP = "swap";

    private boolean exportable;

    public PlatformSummaryPortlet() {
        this(false);
    }

    public PlatformSummaryPortlet(boolean isExportable) {
        super();
        setDataSource(new PlatformMetricDataSource(this));
        exportable = isExportable;
    }

    @Override
    protected void configureTable() {

        ListGridField nameField = new ListGridField(ResourceDataSourceField.NAME.propertyName(),
            MSG.common_title_name());
        nameField.setCellFormatter(new CellFormatter() {
            public String format(Object o, ListGridRecord listGridRecord, int i, int i1) {
                return "<a href=\"" + LinkManager.getResourceLink(listGridRecord.getAttributeAsInt("id")) + "\">" + o
                    + "</a>";
            }
        });

        IconField availabilityField = new IconField(ResourceDataSourceField.AVAILABILITY.propertyName(),
            MSG.common_title_availability(), 70);

        ListGridField versionField = new ListGridField(ResourceDataSourceField.VERSION.propertyName(),
            MSG.common_title_version());

        ListGridField cpuField = new ListGridField(FIELD_CPU, MSG.dataSource_platforms_field_cpu());
        ListGridField memoryField = new ListGridField(FIELD_MEMORY, MSG.dataSource_platforms_field_memory());
        ListGridField swapField = new ListGridField(FIELD_SWAP, MSG.dataSource_platforms_field_swap());

        nameField.setWidth("20%");
        availabilityField.setWidth(70);
        versionField.setWidth("20%");
        cpuField.setWidth("20%");
        memoryField.setWidth("20%");
        swapField.setWidth("20%");

        // the way the field data is calculated, we can't sort on the graph columns
        cpuField.setCanSort(false);
        memoryField.setCanSort(false);
        swapField.setCanSort(false);

        setListGridFields(nameField, availabilityField, versionField, cpuField, memoryField, swapField);

        if (exportable) {
            addExportAction();
        }
    }

    private void addExportAction() {
        addTableAction("Export", MSG.common_button_reports_export(), ButtonColor.BLUE, new AbstractTableAction() {
            @Override
            public boolean isEnabled(ListGridRecord[] selection) {
                return enableIfRecordsExist(getListGrid());
            }

            @Override
            public void executeAction(ListGridRecord[] selection, Object actionValue) {
                ReportExporter exporter = ReportExporter.createStandardExporter("platformUtilization");
                exporter.export();
                refreshTableInfo();
            }
        });
    }

    @Override
    protected ListGrid createListGrid() {
        return new ListGrid() {
            {
                setShowRecordComponents(true);
                setShowRecordComponentsByCell(true);
                setAutoFitData(Autofit.VERTICAL);
                setAutoFitWidthApproach(AutoFitWidthApproach.BOTH);
                setOverflow(Overflow.AUTO);
                setShowEmptyMessage(false);
            }

            @Override
            protected Canvas createRecordComponent(ListGridRecord listGridRecord, Integer colNum) {

                String fieldName = this.getFieldName(colNum);

                try {
                    if (fieldName.equals(FIELD_CPU)) {
                        if (listGridRecord.getAttribute(CPUMetric.Idle.getProperty()) != null) {
                            HLayout bar = new HLayout();
                            bar.setHeight(18);
                            bar.setWidth100();

                            double value = listGridRecord.getAttributeAsDouble(CPUMetric.Idle.getProperty());
                            value = 1 - value;

                            Label text = new Label();
                            text.setWrap(false);
                            text.setAutoFit(true);
                            text.setContents(MeasurementConverterClient
                                .format(value, MeasurementUnits.PERCENTAGE, true));
                            bar.addMember(text);

                            Img first = new Img("availBar/up.png");
                            first.setHeight(18);
                            first.setWidth((value * 100) + "%");
                            bar.addMember(first);

                            Img second = new Img("availBar/unknown.png");
                            second.setHeight(18);
                            second.setWidth((100 - (value * 100)) + "%");
                            bar.addMember(second);

                            return bar;
                        }

                    } else if (fieldName.equals(FIELD_MEMORY)) {
                        if (listGridRecord.getAttribute(MemoryMetric.Total.getProperty()) != null) {
                            HLayout bar = new HLayout();
                            bar.setHeight(18);
                            bar.setWidth100();

                            double total = listGridRecord.getAttributeAsDouble(MemoryMetric.Total.getProperty());
                            double used = listGridRecord.getAttributeAsDouble(MemoryMetric.ActualUsed.getProperty());
                            double percent = used / total;
                            Label text = new Label();
                            text.setWrap(false);
                            text.setAutoFit(true);
                            text.setContents(MeasurementConverterClient.format(percent, MeasurementUnits.PERCENTAGE,
                                true));
                            bar.addMember(text);

                            Img first = new Img("availBar/up.png");
                            first.setHeight(18);
                            first.setWidth((percent * 100) + "%");
                            bar.addMember(first);

                            Img second = new Img("availBar/unknown.png");
                            second.setHeight(18);
                            second.setWidth((100 - (percent * 100)) + "%");
                            bar.addMember(second);

                            return bar;
                        }
                    } else if (fieldName.equals(FIELD_SWAP)) {
                        if (listGridRecord.getAttribute(SwapMetric.Total.getProperty()) != null) {
                            HLayout bar = new HLayout();
                            bar.setHeight(18);
                            bar.setWidth100();

                            double total = listGridRecord.getAttributeAsDouble(SwapMetric.Total.getProperty());
                            double value = listGridRecord.getAttributeAsDouble(SwapMetric.Used.getProperty());
                            double percent = value / total;

                            Label text = new Label();
                            text.setWrap(false);
                            text.setAutoFit(true);
                            text.setContents(MeasurementConverterClient.format(percent, MeasurementUnits.PERCENTAGE,
                                true));
                            bar.addMember(text);

                            Img first = new Img("availBar/up.png");
                            first.setHeight(18);
                            first.setWidth((percent * 100) + "%");
                            bar.addMember(first);

                            Img second = new Img("availBar/unknown.png");
                            second.setHeight(18);
                            second.setWidth((100 - (percent * 100)) + "%");
                            bar.addMember(second);

                            return bar;
                        }
                    }
                    return null;

                } catch (Exception e) {
                    // expected until first data loaded
                    return null;
                }

            }
        };
    }

    public void configure(PortletWindow portletWindow, DashboardPortlet storedPortlet) {
        // This portlet has no configuration settings
    }

    public Canvas getHelpCanvas() {
        return new HTMLFlow(MSG.view_portlet_help_platformSummary());
    }

    @Override
    public ViewName getViewName() {
        return VIEW_ID;
    }

    public static final class Factory implements PortletViewFactory {
        public static final PortletViewFactory INSTANCE = new Factory();

        public final Portlet getInstance(EntityContext context) {
            return new PlatformSummaryPortlet();
        }
    }

}
