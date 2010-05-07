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
package org.rhq.enterprise.gui.navigation.resource;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.composite.DisambiguationReport;
import org.rhq.core.domain.resource.composite.ResourceComposite;
import org.rhq.core.domain.resource.composite.ResourceNamesDisambiguationResult;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.util.IntExtractor;
import org.rhq.enterprise.gui.util.EnterpriseFacesContextUtility;
import org.rhq.enterprise.server.resource.ResourceManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * @author Greg Hinkle
 */
public class ResourceSelectUIBean {

    private Resource resource;

    private ResourceManagerLocal resourceManager = LookupUtil.getResourceManager();

    private String searchString;

    private Log log = LogFactory.getLog(this.getClass());
    
    private static final IntExtractor<ResourceComposite> RESOURCE_ID_EXTRACTOR = new IntExtractor<ResourceComposite>() {
        public int extract(ResourceComposite resource) {
            return resource.getResource().getId();
        }
    };
    
    public static class DisambiguationReportWrapper extends DisambiguationReport<ResourceComposite> {
        private static final long serialVersionUID = 1L;
        
        private boolean typeResolutionNeeded;
        private boolean parentResolutionNeeded;
        private boolean pluginResolutionNeeded;
        
        /**
         * @param original
         * @param parents
         * @param resourceTypeName
         * @param resourceTypePluginName
         */
        public DisambiguationReportWrapper(DisambiguationReport<ResourceComposite> report, boolean typeResolutionNeeded, boolean parentResolutionNeeded, boolean pluginResolutionNeeded) {
            super(report.getOriginal(), report.getParents(), report.getResourceType());
            this.typeResolutionNeeded = typeResolutionNeeded;
            this.parentResolutionNeeded = parentResolutionNeeded;
            this.pluginResolutionNeeded = pluginResolutionNeeded;
        }

        public boolean isTypeResolutionNeeded() {
            return typeResolutionNeeded;
        }

        public boolean isParentResolutionNeeded() {
            return parentResolutionNeeded;
        }

        public boolean isPluginResolutionNeeded() {
            return pluginResolutionNeeded;
        }
    }
    
    public Resource getResource() {
        return resource;
    }

    public void setResource(Resource resource) {
        this.resource = resource;
    }

    public String getSearchString() {
        return searchString;
    }

    public void setSearchString(String searchString) {
        this.searchString = searchString;
    }

    public List<DisambiguationReportWrapper> autocomplete(Object suggest) {
        String pref = (String) suggest;
        ArrayList<ResourceComposite> result;

        PageControl pc = new PageControl();
        pc.setPageSize(50);

        result = resourceManager.findResourceComposites(EnterpriseFacesContextUtility.getSubject(), null, null, null,
            null, pref, true, pc);

        return wrap(resourceManager.disambiguate(result, false, RESOURCE_ID_EXTRACTOR));
    }
    
    private List<DisambiguationReportWrapper> wrap(ResourceNamesDisambiguationResult<ResourceComposite> result) {
        List<DisambiguationReportWrapper> ret = new ArrayList<DisambiguationReportWrapper>();
        if (result == null) {
            return ret;
        }
        
        boolean typeRes = result.isTypeResolutionNeeded();
        boolean parentRes = result.isParentResolutionNeeded();
        boolean pluginRes = result.isPluginResolutionNeeded();
        
        
        for (DisambiguationReport<ResourceComposite> r : result.getResolution()) {
            ret.add(new DisambiguationReportWrapper(r, typeRes, parentRes, pluginRes));
        }
        
        return ret;
    }
}
