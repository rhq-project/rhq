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
package org.rhq.enterprise.gui.coregui.client.alert.definitions;

import com.smartgwt.client.data.Criteria;
import com.smartgwt.client.widgets.grid.ListGridRecord;
import com.smartgwt.client.widgets.layout.VLayout;

import org.rhq.core.domain.resource.Resource;
import org.rhq.enterprise.gui.coregui.client.components.table.Table;
import org.rhq.enterprise.gui.coregui.client.components.table.TableAction;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.detail.alert.AlertEditView;

/**
 * @author Greg Hinkle
 */
public class AlertDefinitionsView extends VLayout {

    private Resource resource;

    public AlertDefinitionsView(Resource resource) {
        setWidth100();
        this.resource = resource;
    }

    @Override
    protected void onDraw() {
        super.onDraw();

        Criteria criteria = new Criteria();
        criteria.addCriteria("resourceId",resource.getId());

        Table table = new Table("Alert Definitions", criteria);
        table.setDataSource(new AlertDefinitionsDataSource());
        table.getListGrid().setUseAllDataSourceFields(true);


        table.addTableAction("New", new TableAction() {
            public void executeAction(ListGridRecord[] selection) {
                new AlertEditView(resource).displayAsDialog();
            }
        });

        addMember(table);
    }


    public static AlertDefinitionsView getResourceView(Resource resource) {
        return new AlertDefinitionsView(resource);
    }


}
