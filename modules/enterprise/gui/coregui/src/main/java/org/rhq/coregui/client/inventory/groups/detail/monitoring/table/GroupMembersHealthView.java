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
package org.rhq.coregui.client.inventory.groups.detail.monitoring.table;

import java.util.List;

import com.smartgwt.client.widgets.grid.ListGridField;
import com.smartgwt.client.widgets.grid.ListGridRecord;

import org.rhq.core.domain.resource.group.composite.ResourceGroupComposite;
import org.rhq.coregui.client.components.table.TableAction;
import org.rhq.coregui.client.components.table.Table.TableActionInfo.ButtonColor;
import org.rhq.coregui.client.inventory.common.detail.summary.AbstractActivityView.ChartViewWindow;
import org.rhq.coregui.client.inventory.groups.detail.inventory.MembersView;

/**
 * The content pane for the group Monitoring>Tables subtab.
 *
 * @author Jay Shaughnessy
 * @author Simeon Pinder
 */
public class GroupMembersHealthView extends MembersView {

    private ResourceGroupComposite groupComposite;
    private boolean canModifyMembers;

    public GroupMembersHealthView(ResourceGroupComposite groupComposite, boolean canModifyMembers) {
        super(groupComposite.getResourceGroup().getId(), false);
        this.canModifyMembers = canModifyMembers;
        this.groupComposite = groupComposite;
        setShowFilterForm(false);
        setShowFooterRefresh(true);
        //disable search view
        setHideSearchBar(true);
        setTitle(MSG.common_title_group_member_health());
    }

    @Override
    protected void configureTable() {
        List<ListGridField> fields = createFields();
        //add extra list grid field for alerts
        setListGridFields(fields.toArray(new ListGridField[fields.size()]));

        addTableAction(MSG.common_title_compareMetrics(), ButtonColor.BLUE, new TableAction() {
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
                int[] resourceIds = new int[selection.length];
                int i = 0;
                for (ListGridRecord record : selection) {
                    Integer defId = record.getAttributeAsInt(FIELD_ID);
                    resourceIds[i++] = defId.intValue();
                }

                ChartViewWindow window = new ChartViewWindow("", MSG.common_title_compareMetrics());
                GroupMembersComparisonView view = new GroupMembersComparisonView(groupComposite, resourceIds);
                window.addItem(view);
                window.show();
                GroupMembersHealthView.this.refreshTableInfo();
            }
        });

    }

    public boolean isCanModifyMembers() {
        return canModifyMembers;
    }

}
