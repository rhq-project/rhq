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

        DynamicForm form = new LocatableDynamicForm(extendLocatorId("form"));
        form.setWidth100();
        form.setHeight100();
        form.setWrapItemTitles(false);

        StaticTextItem id = new StaticTextItem("id", MSG.common_title_id());
        id.setValue(drift.getId());

        StaticTextItem path = new StaticTextItem("path", MSG.common_title_path());
        path.setValue(drift.getPath());

        StaticTextItem timestamp = new StaticTextItem("timestamp", MSG.common_title_timestamp());
        timestamp.setValue(TimestampCellFormatter.format(drift.getCtime(), DATE_TIME_FORMAT_FULL));

        StaticTextItem category = new StaticTextItem("category", MSG.common_title_category());
        StaticTextItem oldFile = new StaticTextItem("oldFile", MSG.view_drift_table_oldFile());
        StaticTextItem newFile = new StaticTextItem("newFile", MSG.view_drift_table_newFile());

        switch (drift.getCategory()) {
        case FILE_ADDED:
            category.setValue(MSG.view_drift_category_fileAdded());
            oldFile.setValue(MSG.common_label_none());
            newFile.setValue(drift.getNewDriftFile().getHashId());
            break;

        case FILE_CHANGED:
            category.setValue(MSG.view_drift_category_fileChanged());
            oldFile.setValue(drift.getOldDriftFile().getHashId());
            newFile.setValue(drift.getNewDriftFile().getHashId());
            break;

        case FILE_REMOVED:
            category.setValue(MSG.view_drift_category_fileRemoved());
            oldFile.setValue(drift.getOldDriftFile().getHashId());
            newFile.setValue(MSG.common_label_none());
            break;
        }

        form.setItems(id, path, category, timestamp, oldFile, newFile);

        addMember(form);
    }

    @Override
    public void renderView(ViewPath viewPath) {
        driftId = Integer.toString(viewPath.getCurrentAsInt());
        show(driftId);
    }

}
