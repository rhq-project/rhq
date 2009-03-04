/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
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
package org.rhq.enterprise.gui.inventory.group;

import org.jboss.seam.ScopeType;
import org.jboss.seam.faces.Redirect;
import org.jboss.seam.annotations.Create;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.End;
import org.jboss.seam.annotations.In;

/**
 * A POJO Seam component that handles loading plugin configurations across a compatible Group in view-only mode.
 *
 * @author Ian Springer
 */
@Name("ViewGroupPluginConfigurationUIBean")
@Scope(ScopeType.EVENT)
public class ViewGroupPluginConfigurationUIBean extends AbstractGroupPluginConfigurationUIBean
{
    public static final String VIEW_ID = "/rhq/group/inventory/view-connection.xhtml";

    @In(value = "org.jboss.seam.faces.redirect")
    private Redirect redirect;

    @Create
    public void init() {
        loadConfigurations();
        return;
    }

    /**
     * Redirect to editCurrent.xhtml. This gets called when user clicks the EDIT button.
     */
    @End
    public void edit() {
        this.redirect.setViewId(EditGroupPluginConfigurationUIBean.VIEW_ID);
        this.redirect.execute();
        return;
    }
}