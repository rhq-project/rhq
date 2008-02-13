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
package org.rhq.enterprise.gui.common.tabbar;

import java.util.HashMap;
import java.util.Map;
import javax.faces.component.UIComponentBase;
import javax.faces.context.FacesContext;
import org.jetbrains.annotations.NotNull;
import org.rhq.core.gui.util.FacesComponentUtility;

/**
 * The base class for {@link TabComponent} and {@link SubtabComponent}.
 *
 * @author Ian Springer
 */
public abstract class AbstractTabComponent extends UIComponentBase {
    private String name;
    private String url;
    private String alt;
    private boolean selected;
    private Map<String, String> parameters = new HashMap<String, String>();

    public String getFamily() {
        return null;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUrl() {
        if (this.url == null) {
            this.url = FacesComponentUtility.getExpressionAttribute(this, "url");
        }

        return this.url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getAlt() {
        if (this.alt == null) {
            this.alt = FacesComponentUtility.getExpressionAttribute(this, "alt");
        }

        return this.alt;
    }

    public void setAlt(String alt) {
        this.alt = alt;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public boolean isSelected() {
        return this.selected;
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
            this.stateValues = new Object[4];
        }

        this.stateValues[0] = super.saveState(facesContext);
        this.stateValues[1] = this.name;
        this.stateValues[2] = this.url;
        this.stateValues[3] = this.alt;
        return this.stateValues;
    }

    public void restoreState(FacesContext context, Object stateValues) {
        this.stateValues = (Object[]) stateValues;
        super.restoreState(context, this.stateValues[0]);
        this.name = (String) this.stateValues[1];
        this.url = (String) this.stateValues[2];
        this.alt = (String) this.stateValues[3];
    }
}