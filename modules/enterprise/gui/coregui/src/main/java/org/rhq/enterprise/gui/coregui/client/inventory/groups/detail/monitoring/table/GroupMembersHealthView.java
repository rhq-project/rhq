/*
 * RHQ Management Platform
 * Copyright 2010, Red Hat Middleware LLC, and individual contributors
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
package org.rhq.enterprise.gui.coregui.client.inventory.groups.detail.monitoring.table;

import java.util.HashMap;
import java.util.List;

import com.smartgwt.client.widgets.grid.ListGridField;
import com.smartgwt.client.widgets.grid.ListGridRecord;

import org.rhq.enterprise.gui.coregui.client.components.FullHTMLPane;
import org.rhq.enterprise.gui.coregui.client.components.table.TableAction;
import org.rhq.enterprise.gui.coregui.client.inventory.common.detail.summary.AbstractActivityView.ChartViewWindow;
import org.rhq.enterprise.gui.coregui.client.inventory.groups.detail.inventory.MembersView;

/**
 * The content pane for the group Monitoring>Tables subtab.
 *
 * @author Jay Shaughnessy
 * @author Simeon Pinder
 */
public class GroupMembersHealthView extends MembersView {

    private int groupId;
    private boolean canModifyMembers;

    public GroupMembersHealthView(String locatorId, int groupId, boolean canModifyMembers) {
        super(locatorId, groupId, false);
        this.canModifyMembers = canModifyMembers;
        this.groupId = groupId;
        setShowFilterForm(false);
        setShowFooterRefresh(false);
        //diable search view
        setHideSearchBar(true);
        setTitle(MSG.common_title_group_member_health());
    }

    @Override
    protected void configureTable() {
        List<ListGridField> fields = createFields();
        //add extra list grid field for alerts
        setListGridFields(fields.toArray(new ListGridField[fields.size()]));

        //add chart selected metric action
        addTableAction(extendLocatorId("chartValues"), MSG.common_title_compare_metrics(), new TableAction() {
            @Override
            public boolean isEnabled(ListGridRecord[] selection) {
                return selection != null && selection.length > 1;
            }

            @Override
            public void executeAction(ListGridRecord[] selection, Object actionValue) {
                if (selection == null || selection.length == 0) {
                    return;
                }
                // keyed on metric name - string[0] is the metric label, [1] is the units
                final HashMap<String, String[]> scheduleNamesAndUnits = new HashMap<String, String[]>();
                int[] resourceIds = new int[selection.length];
                int i = 0;
                for (ListGridRecord record : selection) {
                    Integer defId = record.getAttributeAsInt(FIELD_ID);
                    resourceIds[i++] = defId.intValue();
                }

                //build portal.war chart page to iFrame
                String destination = "/resource/common/monitor/Visibility.do?mode=compareMetrics&&groupId=" + groupId;
                for (int rId : resourceIds) {
                    destination += "&r=" + rId;
                }
                ChartViewWindow window = new ChartViewWindow(extendLocatorId("CompareWindow"), "", MSG
                    .common_title_compare_metrics());
                //generate and include iframed content
                FullHTMLPane iframe = new FullHTMLPane(extendLocatorId("View"), destination);
                window.addItem(iframe);
                window.show();
            }
        });
    }
}
