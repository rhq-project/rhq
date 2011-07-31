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

import static org.rhq.enterprise.gui.coregui.client.components.table.TimestampCellFormatter.DATE_TIME_FORMAT_FULL;

import java.util.LinkedHashMap;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.fields.StaticTextItem;

import org.rhq.core.domain.criteria.DriftCriteria;
import org.rhq.core.domain.criteria.DriftJPACriteria;
import org.rhq.core.domain.drift.Drift;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.gui.coregui.client.BookmarkableView;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.ViewPath;
import org.rhq.enterprise.gui.coregui.client.components.table.TimestampCellFormatter;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableDynamicForm;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableVLayout;

/**
 * @author Jay Shaughnessy
 */
public class DriftDetailsView extends LocatableVLayout implements BookmarkableView {

    private String driftId;

    private static DriftDetailsView INSTANCE = new DriftDetailsView("DriftDetailsView");

    public static DriftDetailsView getInstance() {
        return INSTANCE;
    }

    private DriftDetailsView(String id) {
        // access through the static singleton only (see renderView)
        super(id);

        setMembersMargin(10);
    }

    private void show(String driftId) {
        DriftCriteria criteria = new DriftJPACriteria();
        criteria.addFilterId(driftId);
        criteria.fetchChangeSet(true);
        GWTServiceLookup.getDriftService().findDriftsByCriteria(criteria, new AsyncCallback<PageList<Drift>>() {
            @Override
            public void onSuccess(PageList<Drift> result) {
                Drift drift = result.get(0);
                show(drift);
            }

            @Override
            public void onFailure(Throwable caught) {
                CoreGUI.getErrorHandler().handleError(MSG.view_drift_failure_load(), caught);
            }
        });
    }

    private void show(Drift drift) {
        for (Canvas child : getMembers()) {
            removeChild(child);
        }

        // the change set to which the drift belongs

        DynamicForm changeSetForm = new LocatableDynamicForm(extendLocatorId("changeSetForm"));
        changeSetForm.setIsGroup(true);
        changeSetForm.setGroupTitle(MSG.view_drift_table_changeSet());
        changeSetForm.setWrapItemTitles(false);

        StaticTextItem changeSetId = new StaticTextItem("changeSetId", MSG.common_title_id());
        changeSetId.setValue(drift.getChangeSet().getId());
        StaticTextItem changeSetCategory = new StaticTextItem("changeSetCategory", MSG.common_title_category());
        changeSetCategory.setValue(drift.getChangeSet().getCategory().name());
        StaticTextItem changeSetVersion = new StaticTextItem("changeSetVersion", MSG.common_title_version());
        changeSetVersion.setValue(drift.getChangeSet().getVersion());
        changeSetForm.setItems(changeSetId, changeSetCategory, changeSetVersion);

        addMember(changeSetForm);

        // the drift history item itself

        DynamicForm driftForm = new LocatableDynamicForm(extendLocatorId("form"));
        driftForm.setIsGroup(true);
        driftForm.setGroupTitle(MSG.view_drift());
        driftForm.setWrapItemTitles(false);

        StaticTextItem id = new StaticTextItem("id", MSG.common_title_id());
        id.setValue(drift.getId());

        StaticTextItem path = new StaticTextItem("path", MSG.common_title_path());
        path.setValue(drift.getPath());

        StaticTextItem timestamp = new StaticTextItem("timestamp", MSG.common_title_timestamp());
        timestamp.setValue(TimestampCellFormatter.format(drift.getCtime(), DATE_TIME_FORMAT_FULL));

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
        StaticTextItem newFile = new StaticTextItem("newFile", MSG.view_drift_table_newFile());

        switch (drift.getCategory()) {
        case FILE_ADDED:
            category.setValue(DriftDataSource.CATEGORY_ICON_ADD);
            oldFile.setValue(MSG.common_label_none());
            newFile.setValue(drift.getNewDriftFile().getHashId());
            break;

        case FILE_CHANGED:
            category.setValue(DriftDataSource.CATEGORY_ICON_CHANGE);
            oldFile.setValue(drift.getOldDriftFile().getHashId());
            newFile.setValue(drift.getNewDriftFile().getHashId());
            break;

        case FILE_REMOVED:
            category.setValue(DriftDataSource.CATEGORY_ICON_REMOVE);
            oldFile.setValue(drift.getOldDriftFile().getHashId());
            newFile.setValue(MSG.common_label_none());
            break;
        }

        driftForm.setItems(id, path, category, timestamp, oldFile, newFile);

        addMember(driftForm);
    }

    @Override
    public void renderView(ViewPath viewPath) {
        driftId = Integer.toString(viewPath.getCurrentAsInt());
        show(driftId);
    }

}
