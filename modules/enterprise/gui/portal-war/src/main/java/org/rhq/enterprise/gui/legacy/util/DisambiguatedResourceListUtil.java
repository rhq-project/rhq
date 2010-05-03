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

package org.rhq.enterprise.gui.legacy.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.rhq.core.domain.resource.composite.DisambiguationReport;
import org.rhq.core.domain.resource.composite.ResourceNamesDisambiguationResult;
import org.rhq.core.domain.resource.composite.ResourceParentFlyweight;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.gui.inventory.resource.ResourcePartialLineageComponent;

/**
 * A utility class to provide page lists of disambiguated resource lists for the 
 * Struts legacy UI.
 * 
 * @author Lukas Krejci
 */
public class DisambiguatedResourceListUtil {

    private static final String RESOURCE_URL = "/rhq/resource/summary/overview.xhtml";
    
    private DisambiguatedResourceListUtil() {
        
    }
    
    public static class Record<T> {
    
        private T original;
        private String lineage;
        
        public Record(T original, String lineage) {
            this.original = original;
            this.lineage = lineage;
        }
        
        public T getOriginal() {
            return original;
        }
        
        public String getLineage() {
            return lineage;
        }
    }
    
    public static <T> PageList<Record<T>> buildResourceList(ResourceNamesDisambiguationResult<T> results, int totalSize, PageControl pageControl, boolean renderLinks) {
        ArrayList<Record<T>> convertedResults = new ArrayList<Record<T>>(results.getResolution().size());
        
        for(DisambiguationReport<T> dr : results.getResolution()) {
            convertedResults.add(new Record<T>(dr.getOriginal(), buildLineage(dr.getParents(), renderLinks)));
        }
        return new PageList<Record<T>>(convertedResults, totalSize, pageControl);
    }
    
    private static String buildLineage(List<ResourceParentFlyweight> parents, boolean renderLinks) {
        if (parents == null || parents.size() == 0) {
            return "";
        }
        
        Iterator<ResourceParentFlyweight> it = parents.iterator();
        
        StringBuilder bld = new StringBuilder();
        
        appendParentName(bld, it.next(), renderLinks);
        
        while (it.hasNext()) {
            bld.append(ResourcePartialLineageComponent.DEFAULT_SEPARATOR);
            appendParentName(bld, it.next(), renderLinks);
        }
        
        return bld.toString();
    }
    
    private static void appendParentName(StringBuilder bld, ResourceParentFlyweight parent, boolean renderLinks) {
        if (renderLinks) {
            bld.append("<a href=\"").append(RESOURCE_URL).append("?id=").append(parent.getParentId())
                .append("\">");
        }
        
        bld.append(parent.getParentName());
        
        if (renderLinks) {
            bld.append("</a>");
        }
    }
}
