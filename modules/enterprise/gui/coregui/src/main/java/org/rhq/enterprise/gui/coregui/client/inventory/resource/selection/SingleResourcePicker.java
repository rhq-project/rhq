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

package org.rhq.enterprise.gui.coregui.client.inventory.resource.selection;

import java.util.HashSet;

/**
 * A window dialog box that lets you pick a single resource.
 * 
 * @author John Mazzitelli
 */
public class SingleResourcePicker extends ResourcePicker {

    public SingleResourcePicker(String locatorId, OkHandler okHandler, CancelHandler cancelHandler) {
        super(locatorId, okHandler, cancelHandler);
    }

    @Override
    protected String getDefaultTitle() {
        return "Select a Resource";
    }

    @Override
    protected ResourceSelector createResourceSelector() {
        return new SingleResourceSelector(extendLocatorId("singleResourceSelector"));
    }

    protected void ok() {
        OkHandler handler = getOkHandler();
        HashSet<Integer> selection = getResourceSelector().getSelection();

        if (selection == null || selection.size() != 1) {
            showWarningMessage("Please select a resource");
        } else {
            if (handler.ok(selection)) {
                markForDestroy();
            }
        }
    }
}
