/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2, as
 * published by the Free Software Foundation, and/or the GNU Lesser
 * General Public License, version 2.1, also as published by the Free
 * Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License and the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License
 * and the GNU Lesser General Public License along with this program;
 * if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */

package org.rhq.enterprise.gui.coregui.client.alert.definitions;

import com.smartgwt.client.data.Criteria;
import com.smartgwt.client.util.SC;
import com.smartgwt.client.widgets.grid.ListGridRecord;

import org.rhq.core.domain.resource.group.ResourceGroup;

/**
 * @author John Mazzitelli
 */
public class GroupAlertDefinitionsView extends AbstractAlertDefinitionsView {

    public static final String CRITERIA_GROUP_ID = "groupId";

    private ResourceGroup group;

    public GroupAlertDefinitionsView(ResourceGroup group) {
        super();
        this.group = group;
    }

    @Override
    protected Criteria getCriteria() {
        Criteria criteria = new Criteria();
        criteria.addCriteria(CRITERIA_GROUP_ID, group.getId());
        return criteria;
    }

    @Override
    protected AbstractAlertDefinitionsDataSource getAlertDefinitionDataSource() {
        return new GroupAlertDefinitionsDataSource();
    }

    @Override
    protected String getTableTitle() {
        return "Group Alert Definitions";
    }

    @Override
    protected boolean isAllowedToModifyAlerts() {
        // TODO: see if user can modify group alerts on this group
        return true;
    }

    @Override
    protected void newButtonPressed(ListGridRecord[] selection) {
        // TODO Auto-generated method stub
        String str = "this is not implemented yet but you selected";
        for (ListGridRecord record : selection) {
            str += ": " + record.getAttribute("name");
        }
        SC.say(str);
    }

    @Override
    protected void enableButtonPressed(ListGridRecord[] selection) {
        // TODO Auto-generated method stub
        String str = "this is not implemented yet but you selected";
        for (ListGridRecord record : selection) {
            str += ": " + record.getAttribute("name");
        }
        SC.say(str);
    }

    @Override
    protected void disableButtonPressed(ListGridRecord[] selection) {
        // TODO Auto-generated method stub
        String str = "this is not implemented yet but you selected";
        for (ListGridRecord record : selection) {
            str += ": " + record.getAttribute("name");
        }
        SC.say(str);
    }

    @Override
    protected void deleteButtonPressed(ListGridRecord[] selection) {
        // TODO Auto-generated method stub
        String str = "this is not implemented yet but you selected";
        for (ListGridRecord record : selection) {
            str += ": " + record.getAttribute("name");
        }
        SC.say(str);
    }
}
