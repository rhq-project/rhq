/*
 * RHQ Management Platform
 * Copyright (C) 2005-2011 Red Hat, Inc.
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

package org.rhq.bindings.util;

import java.util.Collection;
import java.util.Map;

import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.configuration.definition.PropertyDefinition;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionList;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionMap;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionSimple;
import org.rhq.core.domain.content.PackageType;
import org.rhq.core.domain.measurement.MeasurementDefinition;
import org.rhq.core.domain.operation.OperationDefinition;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.util.MessageDigestGenerator;

/**
 * This type is used as an identification of a resource type not based
 * on its id but rather on its structure.
 * 
 * @author Lukas Krejci
 */
public class ResourceTypeFingerprint {

    private String digest;

    public ResourceTypeFingerprint(ResourceType rt, Collection<MeasurementDefinition> measurements,
        Collection<OperationDefinition> operations, Collection<PackageType> packageTypes,
        ConfigurationDefinition pluginConfigurationDefinition, ConfigurationDefinition resourceConfigurationDefinition) {

        digest = computeDigest(rt, measurements, operations, packageTypes, pluginConfigurationDefinition,
            resourceConfigurationDefinition);
    }

    @Override
    public int hashCode() {
        return digest.hashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }

        if (!(other instanceof ResourceTypeFingerprint)) {
            return false;
        }

        ResourceTypeFingerprint o = (ResourceTypeFingerprint) other;

        return digest.equals(o.digest);
    }

    @Override
    public String toString() {
        return digest;
    }

    private static String computeDigest(ResourceType rt, Collection<MeasurementDefinition> measurements,
        Collection<OperationDefinition> operations, Collection<PackageType> packageTypes,
        ConfigurationDefinition pluginConfigurationDefinition, ConfigurationDefinition resourceConfigurationDefinition) {

        StringBuilder representation = new StringBuilder();

        addResourceTypeRepresentation(rt, representation);
        addMeasurementDefinitionsRepresentations(measurements, representation);
        addOperationDefinitionsRepresentations(operations, representation);
        addPackageTypesRepresentations(packageTypes, representation);
        addRepresentation(pluginConfigurationDefinition, representation);
        addRepresentation(resourceConfigurationDefinition, representation);

        return new MessageDigestGenerator(MessageDigestGenerator.SHA_256).calcDigestString(representation.toString());
    }

    private static void addResourceTypeRepresentation(ResourceType rt, StringBuilder bld) {
        bld.append(rt.getName()).append(rt.getPlugin());
    }

    private static void addMeasurementDefinitionsRepresentations(Collection<MeasurementDefinition> defs,
        StringBuilder bld) {
        if (defs == null) {
            bld.append("null");
        } else {
            for (MeasurementDefinition d : defs) {
                addRepresentation(d, bld);
            }
        }
    }

    private static void addOperationDefinitionsRepresentations(Collection<OperationDefinition> defs, StringBuilder bld) {
        if (defs == null) {
            bld.append("null");
        } else {
            for (OperationDefinition d : defs) {
                addRepresentation(d, bld);
            }
        }
    }

    private static void addPackageTypesRepresentations(Collection<PackageType> defs, StringBuilder bld) {
        if (defs == null) {
            bld.append("null");
        } else {
            for (PackageType d : defs) {
                addRepresentation(d, bld);
            }
        }
    }

    private static void addRepresentation(MeasurementDefinition md, StringBuilder bld) {
        bld.append(md.getName());
    }

    private static void addRepresentation(OperationDefinition od, StringBuilder bld) {
        bld.append(od.getName());
        addRepresentation(od.getResultsConfigurationDefinition(), bld);
        addRepresentation(od.getParametersConfigurationDefinition(), bld);
    }

    private static void addRepresentation(PackageType pt, StringBuilder bld) {
        bld.append(pt.getName());
    }

    private static void addRepresentation(ConfigurationDefinition cd, StringBuilder bld) {
        if (cd == null) {
            bld.append("null");
        } else {
            addRepresentation(cd.getPropertyDefinitions(), bld);
        }
    }

    private static void addRepresentation(Map<String, PropertyDefinition> defs, StringBuilder bld) {
        for (Map.Entry<String, PropertyDefinition> entry : defs.entrySet()) {
            PropertyDefinition def = entry.getValue();
            addRepresentation(def, bld);
        }
    }

    private static void addRepresentation(PropertyDefinition def, StringBuilder bld) {
        if (def instanceof PropertyDefinitionSimple) {
            addRepresentation((PropertyDefinitionSimple) def, bld);
        } else if (def instanceof PropertyDefinitionMap) {
            addRepresentation((PropertyDefinitionMap) def, bld);
        } else if (def instanceof PropertyDefinitionList) {
            addRepresentation((PropertyDefinitionList) def, bld);
        }
    }

    private static void addRepresentation(PropertyDefinitionSimple p, StringBuilder bld) {
        bld.append(p.getName()).append(p.getType().name());
    }

    private static void addRepresentation(PropertyDefinitionMap pm, StringBuilder bld) {
        bld.append(pm.getName());
        addRepresentation(pm.getPropertyDefinitions(), bld);
    }

    private static void addRepresentation(PropertyDefinitionList pl, StringBuilder bld) {
        bld.append(pl.getName());
        addRepresentation(pl.getMemberDefinition(), bld);
    }
}
