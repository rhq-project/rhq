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
package org.rhq.enterprise.gui.coregui.client.admin.roles;

import java.util.Collection;

import com.smartgwt.client.data.Criteria;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.grid.ListGridRecord;

import org.rhq.core.domain.auth.Subject;
import org.rhq.enterprise.gui.coregui.client.admin.users.UsersDataSource;
import org.rhq.enterprise.gui.coregui.client.components.selector.AbstractSelector;
import org.rhq.enterprise.gui.coregui.client.util.RPCDataSource;

/**
 * @author Greg Hinkle
 */
public class RoleSubjectSelector extends AbstractSelector<Subject> {
    private static final String ITEM_ICON = "global/User_16.png";

    public RoleSubjectSelector(String id, Collection<Subject> subjects) {
        super(id);
        if (subjects != null) {
            ListGridRecord[] data = (new UsersDataSource()).buildRecords(subjects);
            setAssigned(data);
        }
    }

    @Override
    protected RPCDataSource<Subject> getDataSource() {
        return new UsersDataSource();
    }

    @Override
    protected DynamicForm getAvailableFilterForm() {
        return null; // No Filters Currently
    }

    @Override
    protected Criteria getLatestCriteria(DynamicForm availableFilterForm) {
        return null; // No Filters Currently
    }

    @Override
    protected String getItemTitle() {
        return "user";
    }

    @Override
    protected String getItemIcon() {
        return ITEM_ICON;
    }
    
}
