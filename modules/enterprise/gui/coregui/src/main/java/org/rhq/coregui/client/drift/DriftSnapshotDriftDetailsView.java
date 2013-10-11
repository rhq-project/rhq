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

package org.rhq.coregui.client.drift;

import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.fields.FormItem;
import com.smartgwt.client.widgets.form.fields.SpacerItem;
import com.smartgwt.client.widgets.form.fields.StaticTextItem;

import org.rhq.core.domain.drift.DriftDetails;

/**
 * Similar to {@link DriftDetailsView} but omits information already present from the context of the
 * snapshot view.
 *  
 * @author Jay Shaughnessy
 */
public class DriftSnapshotDriftDetailsView extends DriftDetailsView {

    public DriftSnapshotDriftDetailsView(String driftId) {
        super(driftId);
    }

    @Override
    protected void show(DriftDetails driftDetails) {
        for (Canvas child : getMembers()) {
            removeMember(child);
            child.destroy();
        }

        DynamicForm driftForm = new DynamicForm();
        //driftForm.setIsGroup(true);
        //driftForm.setGroupTitle(MSG.view_tabs_common_drift());
        driftForm.setWrapItemTitles(false);
        driftForm.setNumCols(4);

        SpacerItem spacer = new SpacerItem();

        //StaticTextItem id = new StaticTextItem("id", MSG.common_title_id());
        //id.setValue(driftDetails.getDrift().getId());

        StaticTextItem path = new StaticTextItem("path", MSG.common_title_path());
        path.setValue(driftDetails.getDrift().getPath());

        StaticTextItem file = new StaticTextItem("File", MSG.view_tabs_common_content());
        FormItem fileLink = null;

        if (driftDetails.isBinaryFile()) {
            file.setValue(driftDetails.getDrift().getNewDriftFile().getHashId());
            fileLink = spacer;
            driftForm.setItems(path, spacer, file, fileLink);

        } else {
            file.setValue(driftDetails.getDrift().getNewDriftFile().getHashId());
            fileLink = createViewFileLink(driftDetails.getDrift().getNewDriftFile().getHashId(), driftDetails
                .getDrift().getPath(), driftDetails.getChangeSet().getVersion(), driftDetails.getNewFileStatus());
            driftForm.setItems(path, spacer, file, fileLink);
        }

        addMember(driftForm);
    }
}
