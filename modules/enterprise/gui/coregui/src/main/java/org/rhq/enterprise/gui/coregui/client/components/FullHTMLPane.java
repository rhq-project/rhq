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
package org.rhq.enterprise.gui.coregui.client.components;

import com.smartgwt.client.types.ContentsType;
import com.smartgwt.client.widgets.HTMLPane;

/**
 * @author Greg Hinkle
 */
public class FullHTMLPane extends HTMLPane {
    public FullHTMLPane() {
        setWidth100();
        setHeight100();
        setContentsType(ContentsType.PAGE);
    }

    public FullHTMLPane(String url) {
        this();
        com.allen_sauer.gwt.log.client.Log.debug("Creating IFrame pane with URL [" + url + "]...");
        setContentsURL(url);
    }
}
