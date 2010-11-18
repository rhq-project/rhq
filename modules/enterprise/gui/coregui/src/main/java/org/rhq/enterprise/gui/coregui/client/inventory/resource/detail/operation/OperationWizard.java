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
package org.rhq.enterprise.gui.coregui.client.inventory.resource.detail.operation;

import org.rhq.core.domain.resource.Resource;

import com.smartgwt.client.widgets.Window;
import com.smartgwt.client.widgets.events.CloseClickHandler;
import com.smartgwt.client.widgets.events.CloseClientEvent;
import com.smartgwt.client.widgets.layout.VLayout;

/**
 * @author Greg Hinkle
 */
// TODO (ips, 11/17/10): Can this class be deleted?
public class OperationWizard extends VLayout{

    private Resource resource;
    private String operation;

    public OperationWizard(Resource resource, String operation) {
        this.resource = resource;
        this.operation = operation;
    }

    public static void startPopupOperationWizard(Resource resource, String operation) {
        final Window dialog = new Window();
        dialog.setTitle("Execute Operation");
        dialog.setWidth(800);
        dialog.setHeight(800);
        dialog.setIsModal(true);
        dialog.setShowModalMask(true);
        dialog.setCanDragResize(true);
        dialog.setShowResizer(true);
        dialog.centerInPage();
        dialog.addCloseClickHandler(new CloseClickHandler() {
            public void onCloseClick(CloseClientEvent closeClientEvent) {
                dialog.destroy();
            }
        });
        dialog.addItem(new OperationWizard(resource, operation));
        dialog.show();
    }

}
