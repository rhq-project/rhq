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
package org.rhq.enterprise.gui.coregui.client.inventory.resource.detail.operation.detail;

import java.util.Date;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.types.Overflow;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.Window;
import com.smartgwt.client.widgets.events.CloseClickHandler;
import com.smartgwt.client.widgets.events.CloseClientEvent;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.fields.LinkItem;
import com.smartgwt.client.widgets.form.fields.StaticTextItem;
import com.smartgwt.client.widgets.form.fields.events.ClickEvent;
import com.smartgwt.client.widgets.form.fields.events.ClickHandler;

import org.rhq.core.domain.criteria.ResourceOperationHistoryCriteria;
import org.rhq.core.domain.operation.OperationDefinition;
import org.rhq.core.domain.operation.OperationRequestStatus;
import org.rhq.core.domain.operation.ResourceOperationHistory;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.gui.coregui.client.BookmarkableView;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.ImageManager;
import org.rhq.enterprise.gui.coregui.client.ViewPath;
import org.rhq.enterprise.gui.coregui.client.components.configuration.ConfigurationEditor;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.detail.operation.OperationHistoryDataSource;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableHTMLPane;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableVLayout;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableWindow;

/**
 * @author Greg Hinkle
 */
public class OperationDetailsView extends LocatableVLayout implements BookmarkableView {

    private OperationDefinition definition;
    private ResourceOperationHistory operationHistory;

    private DynamicForm form;

    public OperationDetailsView(String locatorId) {
        super(locatorId);
    }

    public OperationDetailsView(String locatorId, OperationDefinition definition,
        ResourceOperationHistory operationHistory) {
        super(locatorId);

        this.definition = definition;
        this.operationHistory = operationHistory;
    }

    @Override
    protected void onDraw() {
        super.onDraw();

        for (Canvas child : getMembers()) {
            child.destroy();
        }

        if (this.operationHistory != null) {
            displayDetails(operationHistory);
        }
    }

    private void displayDetails(final ResourceOperationHistory operationHistory) {

        for (Canvas child : getMembers()) {
            removeChild(child);
        }

        this.definition = operationHistory.getOperationDefinition();
        this.operationHistory = operationHistory;

        // Information Form

        form = new DynamicForm();
        form.setWidth100();
        form.setWrapItemTitles(false);

        OperationRequestStatus status = operationHistory.getStatus();

        StaticTextItem operationItem = new StaticTextItem(OperationHistoryDataSource.Field.OPERATION_NAME, MSG
            .view_operationHistoryDetails_operation());
        operationItem.setValue(definition.getDisplayName());

        StaticTextItem submittedItem = new StaticTextItem(OperationHistoryDataSource.Field.STARTED_TIME, MSG
            .view_operationHistoryDetails_dateSubmitted());
        submittedItem.setValue(new Date(operationHistory.getStartedTime()));

        StaticTextItem completedItem = new StaticTextItem("completed", MSG.view_operationHistoryDetails_dateCompleted());
        if (status == OperationRequestStatus.INPROGRESS) {
            completedItem.setValue(MSG.common_val_na());
        } else if (status == OperationRequestStatus.CANCELED) {
            completedItem.setValue(MSG.common_val_never());
        } else {
            completedItem.setValue(new Date(operationHistory.getStartedTime() + operationHistory.getDuration()));
        }

        StaticTextItem requesterItem = new StaticTextItem(OperationHistoryDataSource.Field.SUBJECT, MSG
            .view_operationHistoryDetails_requestor());
        requesterItem.setValue(operationHistory.getSubjectName());

        LinkItem errorLinkItem = null;

        StaticTextItem statusItem = new StaticTextItem(OperationHistoryDataSource.Field.STATUS, MSG
            .view_operationHistoryDetails_status());
        String icon = ImageManager.getFullImagePath(ImageManager.getOperationResultsIcon(status));
        statusItem.setValue("<img src='" + icon + "'/>");
        switch (status) {
        case SUCCESS:
            statusItem.setTooltip(MSG.common_status_success());
            break;
        case FAILURE:
            statusItem.setTooltip(MSG.common_status_failed());
            errorLinkItem = new LinkItem("errorLink");
            errorLinkItem.setTitle(MSG.common_title_error());
            errorLinkItem.setLinkTitle(getShortErrorMessage(operationHistory));
            errorLinkItem.addClickHandler(new ClickHandler() {
                @Override
                public void onClick(ClickEvent event) {
                    final Window winModal = new LocatableWindow(OperationDetailsView.this.extendLocatorId("errorWin"));
                    winModal.setTitle(MSG.common_title_details());
                    winModal.setOverflow(Overflow.VISIBLE);
                    winModal.setShowMinimizeButton(false);
                    winModal.setShowMaximizeButton(true);
                    winModal.setIsModal(true);
                    winModal.setShowModalMask(true);
                    winModal.setAutoSize(true);
                    winModal.setAutoCenter(true);
                    winModal.setShowResizer(true);
                    winModal.setCanDragResize(true);
                    winModal.centerInPage();
                    winModal.addCloseClickHandler(new CloseClickHandler() {
                        @Override
                        public void onCloseClick(CloseClientEvent event) {
                            winModal.markForDestroy();
                        }
                    });

                    LocatableHTMLPane htmlPane = new LocatableHTMLPane(OperationDetailsView.this
                        .extendLocatorId("statusDetailsPane"));
                    htmlPane.setMargin(10);
                    htmlPane.setDefaultWidth(500);
                    htmlPane.setDefaultHeight(400);
                    String errorMsg = operationHistory.getErrorMessage();
                    if (errorMsg == null) {
                        errorMsg = MSG.common_status_failed();
                    }
                    htmlPane.setContents("<pre>" + errorMsg + "</pre>");
                    winModal.addItem(htmlPane);
                    winModal.show();
                }
            });

            break;
        case INPROGRESS:
            statusItem.setTooltip(MSG.common_status_inprogress());
            break;
        case CANCELED:
            statusItem.setTooltip(MSG.common_status_canceled());
            break;
        }

        /*
        Operation:      View Process List
        Date Submitted: 3/11/10, 12:24:02 PM, EST
        Date Completed: 3/11/10, 12:24:03 PM, EST
        Requester:      rhqadmin
        Status:         Failure
        Error Message:  __Exception: Cannot connect...__ 
        */

        if (errorLinkItem != null) {
            form.setItems(operationItem, submittedItem, completedItem, requesterItem, statusItem, errorLinkItem);
        } else {
            form.setItems(operationItem, submittedItem, completedItem, requesterItem, statusItem);
        }

        addMember(form);

        // params/results

        if (operationHistory.getParameters() != null) {
            ConfigurationEditor editor = new ConfigurationEditor(extendLocatorId("params"), definition
                .getParametersConfigurationDefinition(), operationHistory.getParameters());
            editor.setReadOnly(true);
            editor.setStructuredConfigTabTitle(MSG.view_operationHistoryDetails_parameters());
            addMember(editor);
        }

        if (status == OperationRequestStatus.SUCCESS && operationHistory.getResults() != null) {
            ConfigurationEditor editor = new ConfigurationEditor(extendLocatorId("results"), definition
                .getResultsConfigurationDefinition(), operationHistory.getResults());
            editor.setReadOnly(true);
            editor.setStructuredConfigTabTitle(MSG.view_operationHistoryDetails_results());
            addMember(editor);
        }
    }

    private void lookupDetails(int historyId) {
        ResourceOperationHistoryCriteria criteria = new ResourceOperationHistoryCriteria();

        criteria.addFilterId(historyId);

        criteria.fetchOperationDefinition(true);
        criteria.fetchParameters(true);
        criteria.fetchResults(true);

        GWTServiceLookup.getOperationService().findResourceOperationHistoriesByCriteria(criteria,
            new AsyncCallback<PageList<ResourceOperationHistory>>() {
                public void onFailure(Throwable caught) {
                    CoreGUI.getErrorHandler()
                        .handleError(MSG.view_operationHistoryDetails_error_fetchFailure(), caught);
                }

                public void onSuccess(PageList<ResourceOperationHistory> result) {
                    ResourceOperationHistory item = result.get(0);
                    displayDetails(item);
                }
            });
    }

    @Override
    public void renderView(ViewPath viewPath) {
        int historyId = viewPath.getCurrentAsInt();
        lookupDetails(historyId);
    }

    private String getShortErrorMessage(ResourceOperationHistory operationHistory) {
        String errMsg = operationHistory.getErrorMessage();
        if (errMsg == null) {
            errMsg = MSG.common_status_failed();
        } else if (errMsg.length() > 80) {
            errMsg = errMsg.substring(0, 80) + "...";
        }
        return errMsg;
    }
}
