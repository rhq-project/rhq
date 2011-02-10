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
package org.rhq.enterprise.gui.coregui.client.inventory.common.detail.operation.history;

import com.smartgwt.client.types.Overflow;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.Label;
import com.smartgwt.client.widgets.Window;
import com.smartgwt.client.widgets.events.CloseClickHandler;
import com.smartgwt.client.widgets.events.CloseClientEvent;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.fields.FormItem;
import com.smartgwt.client.widgets.form.fields.LinkItem;
import com.smartgwt.client.widgets.form.fields.StaticTextItem;
import com.smartgwt.client.widgets.form.fields.events.ClickEvent;
import com.smartgwt.client.widgets.form.fields.events.ClickHandler;
import com.smartgwt.client.widgets.layout.VLayout;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.operation.OperationDefinition;
import org.rhq.core.domain.operation.OperationHistory;
import org.rhq.core.domain.operation.OperationRequestStatus;
import org.rhq.enterprise.gui.coregui.client.BookmarkableView;
import org.rhq.enterprise.gui.coregui.client.ImageManager;
import org.rhq.enterprise.gui.coregui.client.ViewPath;
import org.rhq.enterprise.gui.coregui.client.components.configuration.ConfigurationEditor;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableHTMLPane;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableVLayout;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableWindow;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @author Greg Hinkle
 */
public abstract class AbstractOperationHistoryDetailsView<T extends OperationHistory> extends LocatableVLayout
        implements BookmarkableView {

    private T operationHistory;

    private DynamicForm form;

    public AbstractOperationHistoryDetailsView(String locatorId) {
        super(locatorId);

        setWidth100();
        setHeight100();
        setOverflow(Overflow.AUTO);
        setLayoutMargin(0);
        setMembersMargin(16);
    }

    @Override
    protected void onDraw() {
        super.onDraw();

        destroyMembers();

        if (this.operationHistory != null) {
            displayDetails(this.operationHistory);
        }
    }

    /**
     * This method should be called by {@link #lookupDetails(int)} upon successful lookup of an operation history.
     *
     * @param operationHistory the operation history to be displayed
     */
    protected void displayDetails(final T operationHistory) {

        for (Canvas child : getMembers()) {
            removeChild(child);
        }

        this.operationHistory = operationHistory;

        // Information Form

        form = new DynamicForm();
        form.setWidth100();
        form.setWrapItemTitles(false);
        List<FormItem> items = createFields(operationHistory);
        form.setFields(items.toArray(new FormItem[items.size()]));
        addMember(form);

        // params/results

        if (operationHistory.getParameters() != null) {
            LocatableVLayout parametersSection = new LocatableVLayout(extendLocatorId("ParametersSection"));

            Label title = new Label("<h4>" + MSG.view_operationHistoryDetails_parameters() + "</h4>");
            title.setHeight(27);
            parametersSection.addMember(title);

            OperationDefinition operationDefinition = operationHistory.getOperationDefinition();
            ConfigurationDefinition parametersConfigurationDefinition = operationDefinition
                    .getParametersConfigurationDefinition();
            if (parametersConfigurationDefinition != null &&
                    !parametersConfigurationDefinition.getPropertyDefinitions().isEmpty()) {
                ConfigurationEditor editor = new ConfigurationEditor(extendLocatorId("params"),
                        parametersConfigurationDefinition, operationHistory.getParameters());
                editor.setReadOnly(true);
                parametersSection.addMember(editor);
            } else {
                Label noParametersLabel = new Label("This operation does not take any parameters.");
                noParametersLabel.setHeight(17);
                parametersSection.addMember(noParametersLabel);
            }

            addMember(parametersSection);
        }

        Canvas resultsSection = buildResultsSection(operationHistory);
        if (resultsSection != null) {
            addMember(resultsSection);
        }

        VLayout verticalSpacer = new VLayout();
        verticalSpacer.setHeight100();
        addMember(verticalSpacer);
    }

    protected List<FormItem> createFields(final T operationHistory) {
        List<FormItem> items = new ArrayList<FormItem>();

        OperationRequestStatus status = operationHistory.getStatus();

        StaticTextItem operationItem = new StaticTextItem(AbstractOperationHistoryDataSource.Field.OPERATION_NAME, MSG
            .view_operationHistoryDetails_operation());
        OperationDefinition operationDefinition = operationHistory.getOperationDefinition();
        operationItem.setValue(operationDefinition.getDisplayName());
        items.add(operationItem);

        StaticTextItem submittedItem = new StaticTextItem(AbstractOperationHistoryDataSource.Field.STARTED_TIME, MSG
            .view_operationHistoryDetails_dateSubmitted());
        submittedItem.setValue(new Date(operationHistory.getStartedTime()));
        items.add(submittedItem);

        StaticTextItem completedItem = new StaticTextItem("completed", MSG.view_operationHistoryDetails_dateCompleted());
        if (status == OperationRequestStatus.INPROGRESS) {
            completedItem.setValue(MSG.common_val_na());
        } else if (status == OperationRequestStatus.CANCELED) {
            completedItem.setValue(MSG.common_val_never());
        } else {
            completedItem.setValue(new Date(operationHistory.getStartedTime() + operationHistory.getDuration()));
        }
        items.add(completedItem);

        StaticTextItem requesterItem = new StaticTextItem(AbstractOperationHistoryDataSource.Field.SUBJECT, MSG
            .view_operationHistoryDetails_requestor());
        requesterItem.setValue(operationHistory.getSubjectName());
        items.add(requesterItem);

        StaticTextItem statusItem = new StaticTextItem(AbstractOperationHistoryDataSource.Field.STATUS, MSG
            .view_operationHistoryDetails_status());
        String icon = ImageManager.getFullImagePath(ImageManager.getOperationResultsIcon(status));
        statusItem.setValue("<img src='" + icon + "'/>");
        switch (status) {
        case SUCCESS:
            statusItem.setTooltip(MSG.common_status_success());
            break;
        case FAILURE:
            statusItem.setTooltip(MSG.common_status_failed());
            LinkItem errorLinkItem = new LinkItem("errorLink");
            errorLinkItem.setTitle(MSG.common_title_error());
            errorLinkItem.setLinkTitle(getShortErrorMessage(operationHistory));
            errorLinkItem.addClickHandler(new ClickHandler() {
                @Override
                public void onClick(ClickEvent event) {
                    final Window winModal = new LocatableWindow(AbstractOperationHistoryDetailsView.this
                        .extendLocatorId("errorWin"));
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

                    LocatableHTMLPane htmlPane = new LocatableHTMLPane(AbstractOperationHistoryDetailsView.this
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
            items.add(errorLinkItem);
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

        return items;
    }

    protected abstract Canvas buildResultsSection(T operationHistory);

    protected abstract void lookupDetails(int historyId);

    protected T getOperationHistory() {
        return this.operationHistory;
    }

    @Override
    public void renderView(ViewPath viewPath) {
        int historyId = viewPath.getCurrentAsInt();
        lookupDetails(historyId);
    }

    private static String getShortErrorMessage(OperationHistory operationHistory) {
        String errMsg = operationHistory.getErrorMessage();
        if (errMsg == null) {
            errMsg = MSG.common_status_failed();
        } else if (errMsg.length() > 80) {
            errMsg = errMsg.substring(0, 80) + "...";
        }
        return errMsg;
    }

}
