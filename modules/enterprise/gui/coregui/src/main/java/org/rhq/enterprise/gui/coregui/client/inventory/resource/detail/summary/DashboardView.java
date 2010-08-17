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
package org.rhq.enterprise.gui.coregui.client.inventory.resource.detail.summary;

import org.rhq.core.domain.resource.composite.ResourceComposite;
import org.rhq.enterprise.gui.coregui.client.components.FullHTMLPane;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.ResourceSelectListener;

/**
 * The Resource Summary>Dashboard tab.
 *
 * @author Greg Hinkle
 */
public class DashboardView extends FullHTMLPane implements ResourceSelectListener {
    private ResourceComposite resourceComposite;

    public DashboardView(ResourceComposite resourceComposite) {
        this.resourceComposite = resourceComposite;
    }

    @Override
    protected void onDraw() {
        super.onDraw();

        if (this.resourceComposite != null) {
            onResourceSelected(this.resourceComposite);
        }
    }

    @Override
    public void onResourceSelected(ResourceComposite resourceComposite) {
        this.resourceComposite = resourceComposite;
        setContentsURL("/rhq/resource/summary/summary-plain.xhtml?id="
                + resourceComposite.getResource().getId());
    }
}
