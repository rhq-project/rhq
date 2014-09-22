/*
 * RHQ Management Platform
 *  Copyright (C) 2005-2014 Red Hat, Inc.
 *  All rights reserved.
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation version 2 of the License.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */

package org.rhq.coregui.client.dashboard.portlets.resource;

import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.widgets.form.fields.FloatItem;
import com.smartgwt.client.widgets.form.fields.LinkItem;
import com.smartgwt.client.widgets.form.fields.PickerIcon;
import com.smartgwt.client.widgets.form.fields.StaticTextItem;
import com.smartgwt.client.widgets.form.fields.events.BlurEvent;
import com.smartgwt.client.widgets.form.fields.events.BlurHandler;
import com.smartgwt.client.widgets.form.fields.events.ClickEvent;
import com.smartgwt.client.widgets.form.fields.events.ClickHandler;
import com.smartgwt.client.widgets.form.fields.events.FormItemClickHandler;
import com.smartgwt.client.widgets.form.fields.events.FormItemIconClickEvent;
import com.smartgwt.client.widgets.form.fields.events.KeyUpEvent;
import com.smartgwt.client.widgets.form.fields.events.KeyUpHandler;

import org.rhq.core.domain.measurement.MeasurementBaseline;
import org.rhq.core.domain.measurement.MeasurementDefinition;
import org.rhq.core.domain.measurement.MeasurementUnits;
import org.rhq.core.domain.resource.Resource;
import org.rhq.coregui.client.CoreGUI;
import org.rhq.coregui.client.components.form.EnhancedDynamicForm;
import org.rhq.coregui.client.dashboard.portlets.groups.GroupMetricsPortlet;
import org.rhq.coregui.client.gwt.GWTServiceLookup;
import org.rhq.coregui.client.inventory.common.detail.summary.AbstractActivityView;
import org.rhq.coregui.client.inventory.resource.detail.monitoring.D3GraphListView;
import org.rhq.coregui.client.util.Log;
import org.rhq.coregui.client.util.MeasurementConverterClient;
import org.rhq.coregui.client.util.enhanced.Enhanced;
import org.rhq.coregui.client.util.measurement.GwtMonitorUtils;
import org.rhq.coregui.client.util.message.Message;

/**
 * Metrics chart Window with Baseline editing capabilities.
 */
public class MetricsChartWindow implements Enhanced {
    public static final String CHANGE_VALUE = MSG.chart_baseline_change_value_label();

    private static final String BASELINE_SECTION_TITLE = MSG.chart_baseline_section_title();
    private static final String BASELINE_MEAN = MSG.chart_baseline_baseline_mean();
    private static final String NEW_BASELINE_MEAN = MSG.chart_baseline_baseline_mean_new();
    private static final String BASELINE_HIGH = MSG.chart_baseline_baseline_high();
    private static final String NEW_BASELINE_HIGH = MSG.chart_baseline_baseline_high_new();
    private static final String BASELINE_LOW = MSG.chart_baseline_baseline_low();
    private static final String NEW_BASELINE_LOW = MSG.chart_baseline_baseline_low_new();

    private static final String CUSTOM_USER_BASELINE_MEAN_WAS_NOT_SAVED = MSG.chart_baseline_mean_not_saved();
    private static final String CUSTOM_USER_BASELINE_MEAN_SUCCESSFULLY_SAVED = MSG.chart_baseline_mean_saved();
    private static final String CUSTOM_USER_BASELINE_HIGH_WAS_NOT_SAVED = MSG.chart_baseline_high_not_saved();
    private static final String CUSTOM_USER_BASELINE_HIGH_BOUND_SUCCESSFULLY_SAVED = MSG.chart_baseline_high_saved();
    private static final String CUSTOM_USER_BASELINE_LOW_WAS_NOT_SAVED = MSG.chart_baseline_low_not_saved();
    private static final String CUSTOM_USER_BASELINE_LOW_BOUND_SUCCESSFULLY_SAVED = MSG.chart_baseline_low_saved();

    private StaticTextItem baselineText;
    private StaticTextItem expectedRangeHighText;
    private StaticTextItem expectedRangeLowText;

    private FloatItem newBaselineMeanText;
    private FloatItem newExpectedRangeHighText;
    private FloatItem newExpectedRangeLowText;

    private MeasurementDefinition measurementDefinition;

    private int resourceId = -1;

    public MetricsChartWindow() {
        baselineText = new StaticTextItem();
        expectedRangeHighText = new StaticTextItem();
        expectedRangeLowText = new StaticTextItem();

        newBaselineMeanText = new FloatItem();
        newBaselineMeanText.setKeyPressFilter("[0-9.]");
        newExpectedRangeHighText = new FloatItem();
        newExpectedRangeHighText.setKeyPressFilter("[0-9.]");
        newExpectedRangeLowText = new FloatItem();
        newExpectedRangeLowText.setKeyPressFilter("[0-9.]");

    }

    private EnhancedDynamicForm createMetricBaselineForm() {
        EnhancedDynamicForm metricsBaselineForm = new EnhancedDynamicForm();
        metricsBaselineForm.setGroupTitle(BASELINE_SECTION_TITLE);
        metricsBaselineForm.setIsGroup(true);
        metricsBaselineForm.setExtraSpace(10);
        metricsBaselineForm.setNumCols(3);
        metricsBaselineForm.setColWidths(200, 80, "*");
        metricsBaselineForm.setCellPadding(3);
        metricsBaselineForm.setWidth("95%");

        // Baseline
        baselineText.setTitle(BASELINE_MEAN);
        LinkItem baselineLink = AbstractActivityView.newLinkItem(CHANGE_VALUE, null);
        baselineLink.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent clickEvent) {
                newBaselineMeanText.show();
                newBaselineMeanText.setSelectOnFocus(true);
                newExpectedRangeHighText.hide();
                newExpectedRangeLowText.hide();

            }
        });
        newBaselineMeanText.setTitle(NEW_BASELINE_MEAN);
        newBaselineMeanText.addBlurHandler(new BlurHandler() {
            @Override
            public void onBlur(BlurEvent blurEvent) {
                hideBaselineEditingFields();
            }
        });
        PickerIcon cancelPicker = new PickerIcon(PickerIcon.CLEAR, new FormItemClickHandler() {
            public void onFormItemClick(FormItemIconClickEvent event) {
                hideBaselineEditingFields();
            }
        });
        newBaselineMeanText.setIcons(cancelPicker);
        newBaselineMeanText.addKeyUpHandler(new KeyUpHandler() {
            @Override
            public void onKeyUp(KeyUpEvent keyUpEvent) {
                if (keyUpEvent.getKeyName().equals("Enter")) {
                    hideBaselineEditingFields();
                    if (null != newBaselineMeanText.getValueAsString()) {
                        double newBaselineMean = Double.parseDouble(newBaselineMeanText.getValueAsString());
                        saveCustomBaselineMean(newBaselineMean);
                    }
                }

            }
        });

        // High Baseline
        expectedRangeHighText.setTitle(BASELINE_HIGH);
        LinkItem baselineHighLink = AbstractActivityView.newLinkItem(CHANGE_VALUE, null);
        baselineHighLink.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(ClickEvent clickEvent) {
                // High cannot be below the low
                newBaselineMeanText.hide();
                newExpectedRangeHighText.show();
                newExpectedRangeHighText.setSelectOnFocus(true);
                newExpectedRangeLowText.hide();

            }
        });
        newExpectedRangeHighText.setTitle(NEW_BASELINE_HIGH);
        newExpectedRangeHighText.addBlurHandler(new BlurHandler() {
            @Override
            public void onBlur(BlurEvent blurEvent) {
                hideBaselineEditingFields();
            }
        });
        newExpectedRangeHighText.setIcons(cancelPicker);
        newExpectedRangeHighText.addKeyUpHandler(new KeyUpHandler() {
            @Override
            public void onKeyUp(KeyUpEvent keyUpEvent) {
                if (keyUpEvent.getKeyName().equals("Enter")) {
                    hideBaselineEditingFields();
                    if (null != newExpectedRangeHighText.getValueAsString()) {
                        double newBaselineHigh = Double.parseDouble(newExpectedRangeHighText.getValueAsString());
                        saveCustomBaselineHigh(newBaselineHigh);
                    }
                }

            }
        });

        // Low Baseline
        expectedRangeLowText.setTitle(BASELINE_LOW);
        LinkItem baselineLowLink = AbstractActivityView.newLinkItem(CHANGE_VALUE, null);
        baselineLowLink.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent clickEvent) {
                newBaselineMeanText.hide();
                newExpectedRangeHighText.hide();
                newExpectedRangeLowText.show();
                newExpectedRangeLowText.setSelectOnFocus(true);

            }
        });
        newExpectedRangeLowText.setTitle(NEW_BASELINE_LOW);
        newExpectedRangeLowText.addBlurHandler(new BlurHandler() {
            @Override
            public void onBlur(BlurEvent blurEvent) {
                hideBaselineEditingFields();
            }
        });
        newExpectedRangeLowText.setIcons(cancelPicker);
        newExpectedRangeLowText.addKeyUpHandler(new KeyUpHandler() {
            @Override
            public void onKeyUp(KeyUpEvent keyUpEvent) {
                if (keyUpEvent.getKeyName().equals("Enter")) {
                    hideBaselineEditingFields();
                    if (null != newExpectedRangeLowText.getValueAsString()) {
                        double newBaselineLow = Double.parseDouble(newExpectedRangeLowText.getValueAsString());
                        saveCustomBaselineLow(newBaselineLow);
                    }
                }

            }
        });

        //@hack: the only way to hide is to wait a while for them to appear created in the DOM
        new Timer() {
            @Override
            public void run() {
                hideBaselineEditingFields();
            }
        }.schedule(150);

        metricsBaselineForm.setFields(baselineText, baselineLink, newBaselineMeanText, expectedRangeHighText,
            baselineHighLink, newExpectedRangeHighText, expectedRangeLowText, baselineLowLink, newExpectedRangeLowText);

        return metricsBaselineForm;

    }

    private void saveCustomBaselineMean(double newBaselineMean) {
        Log.debug("Saving baseline mean: " + newBaselineMean);
        GWTServiceLookup.getMeasurementDataService().setUserBaselineMean(resourceId, measurementDefinition.getId(),
            newBaselineMean, new AsyncCallback<Void>() {
                @Override
                public void onFailure(Throwable throwable) {
                    unSuccessfulSave(CUSTOM_USER_BASELINE_MEAN_WAS_NOT_SAVED, throwable);
                }

                @Override
                public void onSuccess(Void aVoid) {
                    successfulSave(CUSTOM_USER_BASELINE_MEAN_SUCCESSFULLY_SAVED);
                }
            });
    }

    private void saveCustomBaselineHigh(double newBaselineHigh) {
        Log.debug("Saving baseline high: " + newBaselineHigh);
        GWTServiceLookup.getMeasurementDataService().setUserBaselineMax(resourceId, measurementDefinition.getId(),
            newBaselineHigh, new AsyncCallback<Void>() {
                @Override
                public void onFailure(Throwable throwable) {
                    unSuccessfulSave(CUSTOM_USER_BASELINE_HIGH_WAS_NOT_SAVED, throwable);
                }

                @Override
                public void onSuccess(Void aVoid) {
                    successfulSave(CUSTOM_USER_BASELINE_HIGH_BOUND_SUCCESSFULLY_SAVED);
                }
            });
    }

    private void saveCustomBaselineLow(double newBaselineLow) {
        Log.debug("Saving baseline low: " + newBaselineLow);
        GWTServiceLookup.getMeasurementDataService().setUserBaselineMin(resourceId, measurementDefinition.getId(),
            newBaselineLow, new AsyncCallback<Void>() {
                @Override
                public void onFailure(Throwable throwable) {
                    unSuccessfulSave(CUSTOM_USER_BASELINE_LOW_WAS_NOT_SAVED, throwable);
                }

                @Override
                public void onSuccess(Void aVoid) {
                    successfulSave(CUSTOM_USER_BASELINE_LOW_BOUND_SUCCESSFULLY_SAVED);
                }
            });
    }

    private void successfulSave(String successMessage) {
        Log.debug(successMessage);
        CoreGUI.getMessageCenter().notify(new Message(successMessage, Message.Severity.Info));
        hideBaselineEditingFields();
        queryScheduleBaseline(resourceId, measurementDefinition);
    }

    private void unSuccessfulSave(String failureMessage, Throwable throwable) {
        Log.warn(failureMessage, throwable);
        CoreGUI.getMessageCenter().notify(new Message(failureMessage, throwable, Message.Severity.Error));
    }

    public void showPopupChart(String title, Resource resource, MeasurementDefinition measurementDefinition,
        GroupMetricsPortlet refreshablePortlet) {
        this.measurementDefinition = measurementDefinition;
        resourceId = resource.getId();
        GroupMetricsPortlet.ChartViewWindow window = new GroupMetricsPortlet.ChartViewWindow(title, "",
            refreshablePortlet);
        D3GraphListView graphView = D3GraphListView.createSingleGraph(resource, measurementDefinition.getId(), true);
        window.addItem(graphView);
        window.addItem(createMetricBaselineForm());
        window.show();
        queryScheduleBaseline(resourceId, measurementDefinition);
    }

    private void hideBaselineEditingFields() {
        newBaselineMeanText.hide();
        newExpectedRangeHighText.hide();
        newExpectedRangeLowText.hide();
    }

    private void queryScheduleBaseline(final int resourceId, final MeasurementDefinition md) {

        GWTServiceLookup.getMeasurementDataService().getBaselineForResourceAndSchedule(resourceId, md.getId(),
            new AsyncCallback<MeasurementBaseline>() {

                public void onSuccess(MeasurementBaseline measurementBaseline) {
                    if (null != measurementBaseline) {
                        MeasurementUnits units = md.getUnits();

                        baselineText.setValue(GwtMonitorUtils.formatSimpleMetric(measurementBaseline.getMean(), md));
                        expectedRangeHighText.setValue(GwtMonitorUtils.formatSimpleMetric(measurementBaseline.getMax(),
                            md));
                        expectedRangeLowText.setValue(GwtMonitorUtils.formatSimpleMetric(measurementBaseline.getMin(),
                            md));

                        baselineText.setTitle(baselineText.getTitle());
                        expectedRangeHighText.setTitle(expectedRangeHighText.getTitle());
                        expectedRangeLowText.setTitle(expectedRangeLowText.getTitle());

                        newBaselineMeanText.setValue(MeasurementConverterClient.fit(measurementBaseline.getMean(),
                            md.getUnits()).getValue());
                        newExpectedRangeHighText.setValue(MeasurementConverterClient.fit(measurementBaseline.getMax(),
                            units).getValue());
                        newExpectedRangeLowText.setValue(MeasurementConverterClient.fit(measurementBaseline.getMin(),
                            units).getValue());

                        newBaselineMeanText.setTitle(newBaselineMeanText.getTitle());
                        newExpectedRangeHighText.setTitle(newExpectedRangeHighText.getTitle());
                        newExpectedRangeLowText.setTitle(newExpectedRangeLowText.getTitle());
                    }
                }

                @Override
                public void onFailure(Throwable caught) {
                    Log.debug("Error retrieving out of bound metrics baseline for resource [" + resourceId + ","
                        + md.getId() + "]:" + caught.getMessage());
                }

            });
    }

}
