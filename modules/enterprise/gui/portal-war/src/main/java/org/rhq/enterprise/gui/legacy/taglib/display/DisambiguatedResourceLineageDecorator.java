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
import java.io.StringWriter;
import java.util.List;

import org.rhq.core.domain.resource.composite.DisambiguationReport;

/**
 * The same as {@link DisambiguatedResourceLineageTag} but for use inside
 * display:column.
 * 
 * @author Lukas Krejci
 */
public class DisambiguatedResourceLineageDecorator extends BaseDecorator {

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

    @SuppressWarnings("unchecked")
    public String decorate(Object obj) {
        List<DisambiguationReport.Resource> parents = getParents();
        if (parents == null) {
            parents = (List<DisambiguationReport.Resource>) obj;
        }

        try {
            StringWriter writer = new StringWriter();
            boolean renderLinks = this.renderLinks == null || this.renderLinks;
            
            DisambiguatedResourceLineageTag.writeParents(writer, parents, renderLinks, true);
            return writer.toString();
        } catch (IOException e) {
            return null;
        }
    }

}
