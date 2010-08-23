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

package org.rhq.enterprise.gui.coregui.client.components.tab;

import com.smartgwt.client.widgets.Canvas;

/**
 * Simple class to provide a SubTab a locatorId.
 *  
 * @author Jay Shaughnessy
 */
public class SubTab {
    private String locatorId;
    private String title;
    private Canvas canvas;

    public SubTab(String locatorId, String title, Canvas canvas) {
        super();
        this.title = title;
        this.canvas = canvas;
    }

    public String getLocatorId() {
        return locatorId;
    }

    public String getTitle() {
        return title;
    }

    public Canvas getCanvas() {
        return canvas;
    }

    public void setCanvas(Canvas canvas) {
        this.canvas = canvas;
    }

}
