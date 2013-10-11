/*
 * RHQ Management Platform
 * Copyright (C) 2005-2012 Red Hat, Inc.
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

package org.rhq.coregui.client.admin.topology;

import static org.rhq.coregui.client.admin.topology.PartitionEventDatasourceField.FIELD_CTIME;
import static org.rhq.coregui.client.admin.topology.PartitionEventDatasourceField.FIELD_EVENT_DETAIL;
import static org.rhq.coregui.client.admin.topology.PartitionEventDatasourceField.FIELD_EVENT_TYPE;
import static org.rhq.coregui.client.admin.topology.PartitionEventDatasourceField.FIELD_EXECUTION_STATUS;
import static org.rhq.coregui.client.admin.topology.PartitionEventDatasourceField.FIELD_SUBJECT_NAME;

import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.types.Overflow;
import com.smartgwt.client.types.VisibilityMode;
import com.smartgwt.client.widgets.Label;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.fields.StaticTextItem;
import com.smartgwt.client.widgets.grid.ListGrid;
import com.smartgwt.client.widgets.grid.ListGridField;
import com.smartgwt.client.widgets.grid.ListGridRecord;
import com.smartgwt.client.widgets.layout.SectionStack;
import com.smartgwt.client.widgets.layout.SectionStackSection;

import org.rhq.core.domain.cloud.PartitionEvent;
import org.rhq.core.domain.cloud.PartitionEventDetails;
import org.rhq.core.domain.criteria.PartitionEventCriteria;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.coregui.client.BookmarkableView;
import org.rhq.coregui.client.CoreGUI;
import org.rhq.coregui.client.ViewPath;
import org.rhq.coregui.client.components.table.TimestampCellFormatter;
import org.rhq.coregui.client.gwt.GWTServiceLookup;
import org.rhq.coregui.client.gwt.TopologyGWTServiceAsync;
import org.rhq.coregui.client.util.Log;
import org.rhq.coregui.client.util.enhanced.EnhancedVLayout;

/**
 * Shows details of a partition event.
 * 
 * @author Jirka Kremser
 */
public class PartitionEventDetailView extends EnhancedVLayout implements BookmarkableView {

    private final int partitionEventId;

    private static final int SECTION_COUNT = 2;
    private final SectionStack sectionStack;
    private SectionStackSection detailsSection = null;
    private SectionStackSection agentSection = null;

    private volatile int initSectionCount = 0;

    public PartitionEventDetailView(int partitionEventId) {
        super();
        this.partitionEventId = partitionEventId;
        setHeight100();
        setWidth100();
        setOverflow(Overflow.AUTO);

        sectionStack = new SectionStack();
        sectionStack.setVisibilityMode(VisibilityMode.MULTIPLE);
        sectionStack.setWidth100();
        sectionStack.setHeight100();
        sectionStack.setMargin(5);
        sectionStack.setOverflow(Overflow.VISIBLE);
    }

    @Override
    protected void onInit() {
        super.onInit();
        PartitionEventCriteria criteria = new PartitionEventCriteria();
        criteria.addFilterId(partitionEventId);
        final TopologyGWTServiceAsync service = GWTServiceLookup.getTopologyService();
        service.findPartitionEventsByCriteria(criteria, new AsyncCallback<PageList<PartitionEvent>>() {
            public void onSuccess(final PageList<PartitionEvent> events) {
                if (events == null || events.size() != 1) {
                    CoreGUI.getErrorHandler().handleError(
                        MSG.view_adminTopology_message_fetchPEventDetailsFail(String.valueOf(partitionEventId)));
                    initSectionCount = SECTION_COUNT;
                    return;
                }
                prepareDetailsSection(sectionStack, events.get(0));
                service.getPartitionEventDetails(partitionEventId, PageControl.getUnlimitedInstance(),
                    new AsyncCallback<PageList<PartitionEventDetails>>() {
                        public void onSuccess(PageList<PartitionEventDetails> result) {
                            prepareAssignmentsSection(sectionStack, result);
                        }

                        public void onFailure(Throwable caught) {
                            CoreGUI.getErrorHandler()
                                .handleError(
                                    MSG.view_adminTopology_message_fetchPEventDetailsFail(String
                                        .valueOf(partitionEventId)));
                            initSectionCount = SECTION_COUNT;
                            return;
                        }
                    });
            }

            public void onFailure(Throwable caught) {
                CoreGUI.getErrorHandler().handleError(
                    MSG.view_adminTopology_message_fetchPEventDetailsFail(String.valueOf(partitionEventId)));
                initSectionCount = SECTION_COUNT;
            }
        });
    }

    public boolean isInitialized() {
        return initSectionCount >= SECTION_COUNT;
    }

    @Override
    protected void onDraw() {
        super.onDraw();

        // wait until we have all of the sections before we show them. We don't use InitializableView because,
        // it seems they are not supported (in the applicable renderView()) at this level.
        new Timer() {
            final long startTime = System.currentTimeMillis();

            public void run() {
                if (isInitialized()) {
                    if (null != detailsSection) {
                        sectionStack.addSection(detailsSection);
                    }
                    if (null != agentSection) {
                        sectionStack.addSection(agentSection);
                    }

                    addMember(sectionStack);
                    markForRedraw();

                } else {
                    // don't wait forever, give up after 20s and show what we have
                    long elapsedMillis = System.currentTimeMillis() - startTime;
                    if (elapsedMillis > 20000) {
                        initSectionCount = SECTION_COUNT;
                    }
                    schedule(100); // Reschedule the timer.
                }
            }
        }.run(); // fire the timer immediately
    }

    private void prepareAssignmentsSection(SectionStack stack, PageList<PartitionEventDetails> eventDetails) {
        SectionStackSection section = new SectionStackSection(
            MSG.view_adminTopology_partitionEventsDetail_agentAssignments());
        section.setExpanded(true);
        if (eventDetails == null || eventDetails.size() == 0) {
            Label nothing = new Label(MSG.view_adminTopology_partitionEventsDetail_agentAssignments_nothing());
            nothing.setMargin(10);
            section.setItems(nothing);
        } else {
            // there is no need for datasource, it is a simple table with two columns
            ListGrid assignments = new ListGrid();
            ListGridField agentName = new ListGridField("agentName", MSG.view_adminTopology_agent_agentName());
            ListGridField serverName = new ListGridField("serverName",
                MSG.view_admin_systemSettings_serverDetails_serverName());
            assignments.setFields(agentName, serverName);
            ListGridRecord[] records = new ListGridRecord[eventDetails.size()];
            for (int i = 0; i < eventDetails.size(); i++) {
                records[i] = new ListGridRecord();
                records[i].setAttribute("agentName", eventDetails.get(i).getAgentName());
                records[i].setAttribute("serverName", eventDetails.get(i).getServerName());
            }
            assignments.setData(records);
            section.setItems(assignments);
        }

        agentSection = section;
        initSectionCount++;
    }

    private void prepareDetailsSection(SectionStack stack, PartitionEvent partitionEvent) {
        final DynamicForm form = new DynamicForm();
        form.setMargin(10);
        form.setWidth100();
        form.setWrapItemTitles(false);
        form.setNumCols(2);

        StaticTextItem ctimeItem = new StaticTextItem(FIELD_CTIME.propertyName(),
            MSG.view_adminTopology_partitionEventsDetail_eventExecutionTime());
        ctimeItem.setValue(TimestampCellFormatter.format(Long.valueOf(partitionEvent.getCtime()),
            TimestampCellFormatter.DATE_TIME_FORMAT_LONG));

        StaticTextItem addressItem = new StaticTextItem(FIELD_EVENT_TYPE.propertyName(),
            MSG.view_adminTopology_partitionEventsDetail_eventType());
        addressItem.setValue(partitionEvent.getEventType());

        StaticTextItem eventDetailItem = new StaticTextItem(FIELD_EVENT_DETAIL.propertyName(),
            MSG.view_adminTopology_partitionEventsDetail_eventDetails());
        eventDetailItem.setValue(partitionEvent.getEventDetail());

        StaticTextItem subjectPortItem = new StaticTextItem(FIELD_SUBJECT_NAME.propertyName(),
            FIELD_SUBJECT_NAME.title());
        subjectPortItem.setValue(partitionEvent.getSubjectName());

        StaticTextItem execStatusItem = new StaticTextItem(FIELD_EXECUTION_STATUS.propertyName(),
            FIELD_EXECUTION_STATUS.title());
        execStatusItem.setValue(partitionEvent.getExecutionStatus());

        form.setItems(ctimeItem, addressItem, eventDetailItem, subjectPortItem, execStatusItem);

        SectionStackSection section = new SectionStackSection(MSG.view_adminTopology_partitionEvents_details());
        section.setExpanded(true);
        section.setItems(form);

        detailsSection = section;
        initSectionCount++;
    }

    @Override
    public void renderView(ViewPath viewPath) {
        Log.debug("PartitionEventDetailView: " + viewPath);
    }
}
