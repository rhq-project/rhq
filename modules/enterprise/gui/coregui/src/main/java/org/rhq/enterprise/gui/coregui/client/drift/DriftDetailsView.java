/*
 * RHQ Management Platform
 * Copyright 2011, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
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

package org.rhq.enterprise.gui.coregui.client.drift;

import static org.rhq.core.domain.drift.DriftFileStatus.LOADED;
import static org.rhq.enterprise.gui.coregui.client.components.table.TimestampCellFormatter.DATE_TIME_FORMAT_FULL;

import java.util.LinkedHashMap;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.fields.FormItem;
import com.smartgwt.client.widgets.form.fields.LinkItem;
import com.smartgwt.client.widgets.form.fields.SpacerItem;
import com.smartgwt.client.widgets.form.fields.StaticTextItem;
import com.smartgwt.client.widgets.form.fields.TextAreaItem;
import com.smartgwt.client.widgets.form.fields.events.ClickEvent;
import com.smartgwt.client.widgets.form.fields.events.ClickHandler;
import com.smartgwt.client.widgets.layout.VLayout;

import org.rhq.core.domain.drift.Drift;
import org.rhq.core.domain.drift.DriftChangeSet;
import org.rhq.core.domain.drift.DriftDetails;
import org.rhq.core.domain.drift.DriftFileStatus;
import org.rhq.core.domain.drift.FileDiffReport;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.PopupWindow;
import org.rhq.enterprise.gui.coregui.client.components.table.TimestampCellFormatter;
import org.rhq.enterprise.gui.coregui.client.drift.util.DiffUtility;
import org.rhq.enterprise.gui.coregui.client.gwt.DriftGWTServiceAsync;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableDynamicForm;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableVLayout;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableWindow;

/**
 * @author Jay Shaughnessy
 */
public class DriftDetailsView extends LocatableVLayout {

    private String driftId;

    public DriftDetailsView(String locatorId, String driftId) {
        super(locatorId);
        this.driftId = driftId;
        setMembersMargin(10);
    }

    @Override
    protected void onDraw() {
        super.onDraw();
        show(this.driftId);
    }

    private void show(final String driftId) {
        final DriftGWTServiceAsync driftService = GWTServiceLookup.getDriftService();
        driftService.getDriftDetails(driftId, new AsyncCallback<DriftDetails>() {
            @Override
            public void onFailure(Throwable caught) {
                CoreGUI.getErrorHandler().handleError("Failed to load drift details for drift id: " + driftId, caught);
            }

            @Override
            public void onSuccess(final DriftDetails details) {
                show(details);
            }
        });

    }

    protected void show(DriftDetails driftDetails) {
        for (Canvas child : getMembers()) {
            removeMember(child);
            child.destroy();
        }

        addMember(createChangeSetForm(driftDetails.getDrift()));

        DynamicForm driftForm = new LocatableDynamicForm(extendLocatorId("form"));
        driftForm.setIsGroup(true);
        driftForm.setGroupTitle(MSG.view_tabs_common_drift());
        driftForm.setWrapItemTitles(false);
        driftForm.setNumCols(4);

        SpacerItem spacer = new SpacerItem();

        StaticTextItem id = new StaticTextItem("id", MSG.common_title_id());
        id.setValue(driftDetails.getDrift().getId());

        StaticTextItem path = new StaticTextItem("path", MSG.common_title_path());
        path.setValue(driftDetails.getDrift().getPath());

        StaticTextItem timestamp = new StaticTextItem("timestamp", MSG.common_title_timestamp());
        timestamp.setValue(TimestampCellFormatter.format(driftDetails.getDrift().getCtime(), DATE_TIME_FORMAT_FULL));

        StaticTextItem category = new StaticTextItem("category", MSG.common_title_category());

        LinkedHashMap<String, String> catIconsMap = new LinkedHashMap<String, String>(3);
        catIconsMap.put(DriftDataSource.CATEGORY_ICON_ADD, DriftDataSource.CATEGORY_ICON_ADD);
        catIconsMap.put(DriftDataSource.CATEGORY_ICON_CHANGE, DriftDataSource.CATEGORY_ICON_CHANGE);
        catIconsMap.put(DriftDataSource.CATEGORY_ICON_REMOVE, DriftDataSource.CATEGORY_ICON_REMOVE);
        LinkedHashMap<String, String> catValueMap = new LinkedHashMap<String, String>(3);
        catValueMap.put(DriftDataSource.CATEGORY_ICON_ADD, MSG.view_drift_category_fileAdded());
        catValueMap.put(DriftDataSource.CATEGORY_ICON_CHANGE, MSG.view_drift_category_fileChanged());
        catValueMap.put(DriftDataSource.CATEGORY_ICON_REMOVE, MSG.view_drift_category_fileRemoved());
        category.setValueMap(catValueMap);
        category.setValueIcons(catIconsMap);
        category.setShowIcons(true);

        StaticTextItem oldFile = new StaticTextItem("oldFile", MSG.view_drift_table_oldFile());
        FormItem oldFileLink = null;

        StaticTextItem newFile = new StaticTextItem("newFile", MSG.view_drift_table_newFile());
        FormItem newFileLink = null;

        if (driftDetails.isBinaryFile()) {
            switch (driftDetails.getDrift().getCategory()) {
            case FILE_ADDED:
                category.setValue(DriftDataSource.CATEGORY_ICON_ADD);
                oldFile.setValue(MSG.common_label_none());
                oldFileLink = spacer;
                newFile.setValue(driftDetails.getDrift().getNewDriftFile().getHashId());
                newFileLink = spacer;
                break;
            case FILE_CHANGED:
                category.setValue(DriftDataSource.CATEGORY_ICON_CHANGE);
                oldFile.setValue(driftDetails.getDrift().getOldDriftFile().getHashId());
                oldFileLink = spacer;
                newFile.setValue(driftDetails.getDrift().getNewDriftFile().getHashId());
                newFileLink = spacer;
                break;
            case FILE_REMOVED:
                category.setValue(DriftDataSource.CATEGORY_ICON_REMOVE);
                oldFile.setValue(driftDetails.getDrift().getOldDriftFile().getHashId());
                oldFileLink = spacer;
                newFile.setValue(MSG.common_label_none());
                newFileLink = spacer;
                break;
            }
            driftForm.setItems(id, spacer, path, spacer, category, spacer, timestamp, spacer, oldFile, oldFileLink,
                newFile, newFileLink);
        } else {
            FormItem viewDiffLink = spacer;
            switch (driftDetails.getDrift().getCategory()) {
            case FILE_ADDED:
                category.setValue(DriftDataSource.CATEGORY_ICON_ADD);
                oldFile.setValue(MSG.common_label_none());
                oldFileLink = spacer;
                newFile.setValue(driftDetails.getDrift().getNewDriftFile().getHashId());
                newFileLink = createViewFileLink(driftDetails.getDrift().getNewDriftFile().getHashId(), driftDetails
                    .getDrift().getPath(), driftDetails.getChangeSet().getVersion(), driftDetails.getNewFileStatus());
                break;
            case FILE_CHANGED:
                category.setValue(DriftDataSource.CATEGORY_ICON_CHANGE);
                oldFile.setValue(driftDetails.getDrift().getOldDriftFile().getHashId());
                oldFileLink = createViewFileLink(driftDetails.getDrift().getOldDriftFile().getHashId(), driftDetails
                    .getDrift().getPath(), driftDetails.getPreviousChangeSet().getVersion(),
                    driftDetails.getOldFileStatus());
                newFile.setValue(driftDetails.getDrift().getNewDriftFile().getHashId());
                newFileLink = createViewFileLink(driftDetails.getDrift().getNewDriftFile().getHashId(), driftDetails
                    .getDrift().getPath(), driftDetails.getChangeSet().getVersion(), driftDetails.getNewFileStatus());
                if (driftDetails.getNewFileStatus() == LOADED && driftDetails.getOldFileStatus() == LOADED) {
                    viewDiffLink = createViewDiffLink(driftDetails.getDrift(), driftDetails.getPreviousChangeSet()
                        .getVersion());
                }
                break;
            case FILE_REMOVED:
                category.setValue(DriftDataSource.CATEGORY_ICON_REMOVE);
                oldFile.setValue(driftDetails.getDrift().getOldDriftFile().getHashId());
                oldFileLink = createViewFileLink(driftDetails.getDrift().getOldDriftFile().getHashId(), driftDetails
                    .getDrift().getPath(), driftDetails.getChangeSet().getVersion(), driftDetails.getOldFileStatus());
                newFile.setValue(MSG.common_label_none());
                newFileLink = spacer;
                break;
            }
            driftForm.setItems(id, spacer, path, spacer, category, spacer, timestamp, spacer, oldFile, oldFileLink,
                newFile, newFileLink, spacer, spacer, spacer, viewDiffLink);
        }
        addMember(driftForm);
    }

    private DynamicForm createChangeSetForm(Drift<?, ?> drift) {
        DynamicForm changeSetForm = new LocatableDynamicForm(extendLocatorId("changeSetForm"));
        changeSetForm.setIsGroup(true);
        changeSetForm.setGroupTitle(MSG.view_drift_table_snapshot());
        changeSetForm.setWrapItemTitles(false);

        DriftChangeSet<?> changeSet = drift.getChangeSet();
        StaticTextItem changeSetId = new StaticTextItem("changeSetId", MSG.common_title_id());
        changeSetId.setValue(changeSet.getId());
        StaticTextItem changeSetCategory = new StaticTextItem("changeSetCategory", MSG.common_title_category());
        changeSetCategory.setValue(changeSet.getCategory().name());
        StaticTextItem changeSetVersion = new StaticTextItem("changeSetVersion", MSG.common_title_version());
        changeSetVersion.setValue(changeSet.getVersion());
        StaticTextItem changeSetDriftHandling = new StaticTextItem("changeSetDriftHandling",
            MSG.view_drift_table_driftHandlingMode());
        changeSetDriftHandling.setValue(DriftDefinitionDataSource.getDriftHandlingModeDisplayName(changeSet
            .getDriftHandlingMode()));

        changeSetForm.setItems(changeSetId, changeSetCategory, changeSetVersion, changeSetDriftHandling);

        return changeSetForm;
    }

    protected FormItem createViewFileLink(String hash, String path, int version, DriftFileStatus status) {
        if (status == LOADED) {
            return createViewFileLink(hash, path, version);
        }
        StaticTextItem item = new StaticTextItem(hash + "_fileLink");
        item.setShowTitle(false);
        item.setValue("(file not ready for viewing)");

        return item;
    }

    private LinkItem createViewFileLink(final String hash, final String path, final int version) {
        LinkItem link = new LinkItem(hash + "_fileLink");
        link.setShowTitle(false);
        link.setLinkTitle("(view)");

        link.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent clickEvent) {
                GWTServiceLookup.getDriftService().getDriftFileBits(hash, new AsyncCallback<String>() {
                    @Override
                    public void onFailure(Throwable caught) {
                        try {
                            throw caught;
                        } catch (Throwable r) {
                            CoreGUI.getErrorHandler().handleError("Failed to load file", r);
                        }
                    }

                    @Override
                    public void onSuccess(String contents) {
                        LocatableWindow fileViewer = createFileViewer(contents, path, version);
                        fileViewer.show();
                    }
                });
            }
        });

        return link;
    }

    private LocatableWindow createFileViewer(String contents, String path, int version) {
        VLayout layout = new VLayout();
        DynamicForm form = new DynamicForm();
        form.setWidth100();
        form.setHeight100();

        TextAreaItem textArea = new TextAreaItem();
        textArea.setShowTitle(false);
        textArea.setColSpan(2);
        textArea.setValue(contents);
        textArea.setWidth("*");
        textArea.setHeight("*");

        form.setItems(textArea);
        layout.addMember(form);

        PopupWindow window = new PopupWindow("fileViewer", layout);
        window.setIsModal(false);
        window.setTitle(path + ":" + version);

        return window;
    }

    private LinkItem createViewDiffLink(final Drift<?, ?> drift, final int oldVersion) {
        LinkItem viewDiffLink = new LinkItem("viewDiff");
        viewDiffLink.setLinkTitle("(view diff)");
        viewDiffLink.setShowTitle(false);

        viewDiffLink.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent clickEvent) {
                GWTServiceLookup.getDriftService().generateUnifiedDiff(drift, new AsyncCallback<FileDiffReport>() {
                    public void onFailure(Throwable caught) {
                        CoreGUI.getErrorHandler().handleError("Failed to generate diff.", caught);
                    }

                    public void onSuccess(FileDiffReport diffReport) {
                        int newVersion = drift.getChangeSet().getVersion();
                        String diffContents = DiffUtility.formatAsHtml(diffReport.getDiff(), oldVersion, newVersion);
                        LocatableWindow window = DiffUtility.createDiffViewerWindow(diffContents, drift.getPath(),
                            oldVersion, newVersion);
                        window.show();
                    }
                });
            }
        });
        return viewDiffLink;
    }

}
