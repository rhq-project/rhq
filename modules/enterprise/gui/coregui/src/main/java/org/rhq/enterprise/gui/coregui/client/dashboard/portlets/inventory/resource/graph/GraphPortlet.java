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
package org.rhq.enterprise.gui.coregui.client.dashboard.portlets.inventory.resource.graph;

import com.google.gwt.core.client.GWT;
import com.smartgwt.client.widgets.Canvas;

import org.rhq.enterprise.gui.coregui.client.dashboard.PortletView;
import org.rhq.enterprise.gui.coregui.client.dashboard.PortletViewFactory;
import org.rhq.enterprise.gui.coregui.client.dashboard.store.StoredPortlet;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.detail.monitoring.SmallGraphView;

/**
 * @author Greg Hinkle
 */
public class GraphPortlet extends SmallGraphView implements PortletView {

    public static final String KEY = "Resource Graph";

    public GraphPortlet() {
        super(10001, 10100);
    }

    public void configure(StoredPortlet storedPortlet) {
        // TODO: Implement this method.
    }

    public Canvas getHelpCanvas() {
        return null;  // TODO: Implement this method.
    }

    public Canvas getSettingsCanvas() {
        return null;  // TODO: Implement this method.
    }

    public static final class Factory implements PortletViewFactory {
        public static PortletViewFactory INSTANCE = new Factory();

        public final PortletView getInstance() {
            return GWT.create(GraphPortlet.class);
        }
    }
}
