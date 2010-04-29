/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2, as
 * published by the Free Software Foundation, and/or the GNU Lesser
 * General Public License, version 2.1, also as published by the Free
 * Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License and the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License
 * and the GNU Lesser General Public License along with this program;
 * if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */

package org.rhq.enterprise.gui.inventory.resource;

import java.util.List;

import javax.faces.component.UIComponentBase;
import javax.faces.context.FacesContext;

import org.rhq.core.domain.resource.composite.DisambiguationReport;
import org.rhq.core.gui.util.FacesComponentUtility;

/**
 * A component for displaying partial resource lineage that comes out of the resource name
 * disambiguation procedure.
 * 
 * @author Lukas Krejci
 */
public class ResourcePartialLineageComponent extends UIComponentBase {
    public static final String COMPONENT_TYPE = "org.jboss.on.ResourcePartialLineage";
    public static final String COMPONENT_FAMILY = "org.jboss.on.ResourcePartialLineage";

    public static final String DEFAULT_SEPARATOR = " > ";
    
    private static final String PARENTS_ATTRIBUTE = "parents";
    private static final String RENDER_LINKS_ATTRIBUTE = "renderLinks";
    private static final String SEPARATOR_ATTRIBUTE = "separator";

    private Boolean renderLinks;
    private String separator;
    private List<DisambiguationReport.Resource> parents;
    
    public String getFamily() {
        return COMPONENT_FAMILY;
    }

    public Boolean getRenderLinks() {
        if (renderLinks == null) {
            renderLinks = FacesComponentUtility.getExpressionAttribute(this, RENDER_LINKS_ATTRIBUTE, Boolean.class);
            if (renderLinks == null) {
                renderLinks = true;
            }
        }
        return renderLinks;
    }


    public void setRenderLinks(Boolean renderLinks) {
        this.renderLinks = renderLinks;
    }


    public String getSeparator() {
        if (separator == null) {
            separator = FacesComponentUtility.getExpressionAttribute(this, SEPARATOR_ATTRIBUTE, String.class);
            if (separator == null) {
                separator = DEFAULT_SEPARATOR;
            }
        }
        return separator;
    }

    public void setSeparator(String separator) {
        this.separator = separator;
    }

    @SuppressWarnings("unchecked")
    public List<DisambiguationReport.Resource> getParents() {
        if (parents == null) {
            //do *NOT* store this value into the parents explicitly
            //unless dynamic updates (if the expression is in loop for example)
            //won't work.
            return FacesComponentUtility.getExpressionAttribute(this, PARENTS_ATTRIBUTE, List.class);
        } else {
            return parents;
        }
    }

    public void setParents(List<DisambiguationReport.Resource> parents) {
        this.parents = parents;
    }
    
    public Object saveState(FacesContext facesContext) {
        Object[] state = new Object[3];
        state[0] = super.saveState(facesContext);
        state[1] = this.renderLinks;
        state[2] = this.separator;
        return state;
    }

    public void restoreState(FacesContext facesContext, Object stateValues) {
        Object[] state = (Object[]) stateValues;
        super.restoreState(facesContext, state[0]);
        this.renderLinks = (Boolean) state[1];
        this.separator = (String) state[2];
    }
}
