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
import javax.faces.context.FacesContext;
import org.jetbrains.annotations.NotNull;
import org.rhq.core.gui.util.FacesComponentUtility;

/**
 * A component that represents a quick-navigation icon (i.e. a MICA icon).
 *
 * @author Ian Springer
 */
public class IconComponent extends UIComponentBase {
    public static final String COMPONENT_TYPE = "org.jboss.on.Icon";
    public static final String COMPONENT_FAMILY = "org.jboss.on.Icon";

    private String name;
    private String url;
    private String alt;
    private boolean visible = true;
    private boolean visibleSet;
    private Map<String, String> parameters = new HashMap<String, String>();

    public String getFamily() {
        return COMPONENT_FAMILY;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUrl() {
        return (this.url != null) ? this.url : FacesComponentUtility.getExpressionAttribute(this, "url");
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getAlt() {
        return (this.alt != null) ? this.alt : FacesComponentUtility.getExpressionAttribute(this, "alt");
    }

    public void setAlt(String alt) {
        this.alt = alt;
    }

    public Boolean isVisible() {
        if (this.visibleSet) {
            return this.visible;
        }

        Boolean result = FacesComponentUtility.getExpressionAttribute(this, "visible", Boolean.class);
        return (result != null) ? result : this.visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
        this.visibleSet = true;
    }

    @NotNull
    public Map<String, String> getParameters() {
        return parameters;
    }

    public void setParameters(@NotNull
    Map<String, String> parameters) {
        this.parameters = parameters;
    }

    private Object[] stateValues;

    public Object saveState(FacesContext facesContext) {
        if (this.stateValues == null) {
            this.stateValues = new Object[6];
        }

        this.stateValues[0] = super.saveState(facesContext);
        this.stateValues[1] = this.name;
        this.stateValues[2] = this.url;
        this.stateValues[3] = this.alt;
        this.stateValues[4] = this.visible;
        this.stateValues[5] = this.visibleSet;
        return this.stateValues;
    }

    public void restoreState(FacesContext context, Object stateValues) {
        this.stateValues = (Object[]) stateValues;
        super.restoreState(context, this.stateValues[0]);
        this.name = (String) this.stateValues[1];
        this.url = (String) this.stateValues[2];
        this.alt = (String) this.stateValues[3];
        this.visible = (Boolean) this.stateValues[4];
        this.visibleSet = (Boolean) this.stateValues[5];
    }
}