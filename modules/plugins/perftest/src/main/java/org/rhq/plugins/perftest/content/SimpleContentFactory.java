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
package org.rhq.plugins.perftest.content;

import java.util.HashSet;
import java.util.Set;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.rhq.core.domain.content.PackageDetailsKey;
import org.rhq.core.domain.content.PackageType;
import org.rhq.core.domain.content.transfer.ResourcePackageDetails;
import org.rhq.plugins.perftest.scenario.SimpleContentGenerator;

/**
 * Artifact factory implementation that will return a configurable number of artifacts. Subsequent requests for artifact
 * discovery will produce the same set of artifacts.
 *
 * @author Jason Dobies
 */
public class SimpleContentFactory implements ContentFactory {
    // Attributes  --------------------------------------------

    /**
     * Read from the scenario, used to govern what will be returned from calls to this instance.
     */
    private SimpleContentGenerator generator;

    private final Log log = LogFactory.getLog(this.getClass());

    // Constructors  --------------------------------------------

    public SimpleContentFactory(SimpleContentGenerator generator) {
        this.generator = generator;
    }

    // ContentFactory Implementation  --------------------------------------------

    public Set<ResourcePackageDetails> discoverContent(PackageType type) {
        int numPackages = getNumberOfPackages();

        Set<ResourcePackageDetails> details = new HashSet<ResourcePackageDetails>(numPackages);
        for (int ii = 0; ii < numPackages; ii++) {
            String name = type.getName() + "-" + ii;
            String key = name;

            PackageDetailsKey detailsKey = new PackageDetailsKey(name, "1", type.getName(), "noarch");
            ResourcePackageDetails oneDetail = new ResourcePackageDetails(detailsKey);
            details.add(oneDetail);
        }

        return details;
    }

    // Private  --------------------------------------------

    /**
     * Determines how many artifacts to create based on the generator's configuration.
     *
     * @return number of artifacts to create
     */
    private int getNumberOfPackages() {
        // If the property is set, use that value
        String propertyName = generator.getProperty();

        if (propertyName != null) {
            String propertyString = System.getProperty(propertyName);

            if (propertyString != null) {
                return Integer.parseInt(propertyString);
            } else {
                log.warn("Property was specified but no value was set. Property: " + propertyName);
            }
        }

        return generator.getNumberOfPackages();
    }
}