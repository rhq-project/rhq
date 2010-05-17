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

package org.rhq.enterprise.gui.legacy.taglib.display;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspTagException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.tagext.TagSupport;

import org.rhq.core.domain.resource.composite.DisambiguationReport;
import org.rhq.enterprise.gui.inventory.resource.DisambiguatedResourceLineageComponent;

/**
 * Renders the location of a disambiguated resource.
 * 
 * @author Lukas Krejci
 */
public class DisambiguatedResourceLineageTag extends TagSupport {
    private static final long serialVersionUID = 1L;

    private List<DisambiguationReport.Resource> parents;
    private Boolean renderLinks;

    public List<DisambiguationReport.Resource> getParents() {
        return parents;
    }

    public void setParents(List<DisambiguationReport.Resource> parents) {
        this.parents = parents;
    }

    public Boolean getRenderLinks() {
        return renderLinks;
    }

    public void setRenderLinks(Boolean renderLinks) {
        this.renderLinks = renderLinks;
    }

    @Override
    public int doEndTag() throws JspException {
        JspWriter writer = pageContext.getOut();

        try {
            if (parents != null && parents.size() > 0) {

                Iterator<DisambiguationReport.Resource> it = parents.iterator();
                DisambiguationReport.Resource parent = it.next();
                DisambiguatedResourceNameTag.writeResource(writer, renderLinks, parent.getId(), parent.getName(),
                    parent.getType());
                
                while (it.hasNext()) {
                    writer.append(DisambiguatedResourceLineageComponent.DEFAULT_SEPARATOR);
                    parent = it.next();
                    DisambiguatedResourceNameTag.writeResource(writer, renderLinks, parent.getId(), parent.getName(),
                        parent.getType());                    
                }
            }

            return super.doEndTag();
        } catch (IOException e) {
            throw new JspTagException(e);
        }
    }

}
