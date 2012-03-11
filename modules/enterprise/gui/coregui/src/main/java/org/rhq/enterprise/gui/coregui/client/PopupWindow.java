/*
 * RHQ Management Platform
 * Copyright (C) 2005-2011 Red Hat, Inc.
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

package org.rhq.enterprise.gui.coregui.client;

import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.events.CloseClickEvent;
import com.smartgwt.client.widgets.events.CloseClickHandler;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableWindow;

/**
 * Generic window that you can use to popup details. Populate the popup
 * window with any canvas.
 * 
 * @author John Mazzitelli
 */
public class PopupWindow extends LocatableWindow {
    /**
     * Create the popup dialog window.
     * 
     * @param locatorId
     * @param canvas if not <code>null</code>, this will be added to the window.
     */
    public PopupWindow(String locatorId, Canvas canvas) {
        super(locatorId);
        setTitle(MSG.common_title_details());
        setShowMinimizeButton(false);
        setShowMaximizeButton(true);
        setIsModal(true);
        setShowModalMask(true);
        setWidth(600);
        setHeight(400);
        setAutoCenter(true);
        setShowResizer(true);
        setCanDragResize(true);
        centerInPage();
        addCloseClickHandler(new CloseClickHandler() {
            public void onCloseClick(CloseClickEvent event) {
                markForDestroy();
            }
        });
        if (canvas != null) {
            addItem(canvas);
        }
    }
}
