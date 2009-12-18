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
package org.rhq.enterprise.server.perspective.activator;

import org.rhq.core.domain.resource.ResourceTypeFacet;
import org.rhq.enterprise.server.perspective.activator.context.AbstractResourceOrGroupActivationContext;

/**
 * @author Ian Springer
 */
public class FacetActivator extends AbstractResourceOrGroupActivator {
    static final long serialVersionUID = 1L;

    private ResourceTypeFacet facet;

    public FacetActivator(ResourceTypeFacet facet) {
        this.facet = facet;
    }

    public boolean matches(AbstractResourceOrGroupActivationContext context) {
        return context.getFacets().contains(this.facet);
    }
}
