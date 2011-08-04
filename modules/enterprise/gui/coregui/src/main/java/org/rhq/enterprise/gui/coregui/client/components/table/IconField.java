/*
 * RHQ Management Platform
 * Copyright 2011, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
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
package org.rhq.enterprise.gui.coregui.client.components.table;

import com.google.gwt.core.client.JavaScriptObject;
import com.smartgwt.client.types.Alignment;
import com.smartgwt.client.types.ListGridFieldType;
import com.smartgwt.client.widgets.grid.ListGridField;

/**
 * @author Ian Springer
 */
public class IconField extends ListGridField {

    private static final String DEFAULT_NAME = "icon";
    private static final String DEFAULT_TITLE = "&nbsp;";
    private static final int DEFAULT_WIDTH = 25;

    public IconField() {
        super(DEFAULT_NAME, DEFAULT_TITLE, DEFAULT_WIDTH);
        init();
    }

    public IconField(JavaScriptObject jsObj) {
        super(DEFAULT_NAME, DEFAULT_TITLE, DEFAULT_WIDTH);
        setJsObj(jsObj);
        init();
    }

    public IconField(String name) {
        super(name, DEFAULT_TITLE, DEFAULT_WIDTH);
        init();
    }

    public IconField(String name, int width) {
        super(name, DEFAULT_TITLE, width);
        init();
    }

    public IconField(String name, String title) {
        super(name, title, DEFAULT_WIDTH);
        init();
    }

    public IconField(String name, String title, int width) {
        super(name, title, width);
        init();
    }

    private void init() {
        setType(ListGridFieldType.IMAGE);
        setAlign(Alignment.CENTER);
        setShowDefaultContextMenu(false);
        if (DEFAULT_TITLE.equals(getTitle())) {
            setCanHide(false);
        }
    }

}
