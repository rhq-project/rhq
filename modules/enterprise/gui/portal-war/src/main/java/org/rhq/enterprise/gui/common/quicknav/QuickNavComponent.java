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
package org.rhq.enterprise.gui.common.quicknav;

import java.util.HashMap;
import java.util.Map;
import javax.faces.component.UIComponentBase;
import org.jetbrains.annotations.NotNull;

/**
 * A component that represents a row of quick-navigation icons (i.e. the MICA icons).
 *
 * @author Ian Springer
 */
public class QuickNavComponent extends UIComponentBase {
    public static final String COMPONENT_TYPE = "org.jboss.on.QuickNav";
    public static final String COMPONENT_FAMILY = "org.jboss.on.QuickNav";

    private Map<String, String> parameters = new HashMap<String, String>();

    public String getFamily() {
        return COMPONENT_FAMILY;
    }

    @NotNull
    public Map<String, String> getParameters() {
        return parameters;
    }

    public void setParameters(@NotNull
    Map<String, String> parameters) {
        this.parameters = parameters;
    }
}