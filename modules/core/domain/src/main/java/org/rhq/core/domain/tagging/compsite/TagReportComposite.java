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
package org.rhq.core.domain.tagging.compsite;

import org.rhq.core.domain.tagging.Tag;

/**
 * @author Greg Hinkle
 */
public class TagReportComposite extends Tag {

    private static final long serialVersionUID = 1L;

    private long total;
    private long resourceCount;
    private long resourceGroupCount;
    private long bundleCount;
    private long bundleVersionCount;
    private long bundleDeploymentCount;
    private long bundleDestinationCount;

    public TagReportComposite() {
    }

    public TagReportComposite(int id, String namespace, String semantic, String name, long total, long resourceCount,
        long resourceGroupCount, long bundleCount, long bundleVersionCount, long bundleDeploymentCount,
        long bundleDestinationCount) {
        super(namespace, semantic, name);
        setId(id);
        this.total = total;
        this.resourceCount = resourceCount;
        this.resourceGroupCount = resourceGroupCount;
        this.bundleCount = bundleCount;
        this.bundleVersionCount = bundleVersionCount;
        this.bundleDeploymentCount = bundleDeploymentCount;
        this.bundleDestinationCount = bundleDestinationCount;
    }

    public Tag getTag() {
        return this;
    }

    public long getTotal() {
        return total;
    }

    public long getResourceCount() {
        return resourceCount;
    }

    public long getResourceGroupCount() {
        return resourceGroupCount;
    }

    public long getBundleCount() {
        return bundleCount;
    }

    public long getBundleVersionCount() {
        return bundleVersionCount;
    }

    public long getBundleDeploymentCount() {
        return bundleDeploymentCount;
    }

    public long getBundleDestinationCount() {
        return bundleDestinationCount;
    }
}
