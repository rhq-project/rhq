/*
 * RHQ Management Platform
 * Copyright (C) 2005-2009 Red Hat, Inc.
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

package org.rhq.core.domain.bundle;

import java.io.Serializable;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.Property;
import org.rhq.core.domain.configuration.PropertyList;
import org.rhq.core.domain.configuration.PropertyMap;
import org.rhq.core.domain.configuration.PropertySimple;

/**
 * If a resource type can be a target for bundle deployment, it will define some metadata values inside this
 * configuration object.
 * We store these in a Configuration to support extensibility in the future. Stored in this configuration object will be
 * things like the bundle destination base directory definitions (the base locations where bundles can be deployed for
 * resources that are of the given type). Rather than expect users of this object to know the internal properties stored
 * in the config, this object has strongly-typed methods to extract the properties into more easily consumable POJOs,
 * such as {@link #getBundleDestinationBaseDirectories()} and {@link #addBundleDestinationBaseDirectory(String, String,
 * String, String)}.
 *
 * @author John Mazzitelli
 * @author Lukas Krejci
 */
public class ResourceTypeBundleConfiguration implements Serializable {
    private static final long serialVersionUID = 1L;

    // The value of these constants is kept as it was originally, even though it no longer reflects the reality.
    // This is to keep the backwards compatibility and avoid the need for a database upgrade job.
    private static final String BUNDLE_DEST_LIST_NAME = "bundleDestBaseDirsList";
    private static final String BUNDLE_DEST_LIST_ITEM_NAME = "bundleDestBaseDirsListItem";

    private static final String BUNDLE_DEST_BASE_DIR_NAME_NAME = "name";
    private static final String BUNDLE_DEST_BASE_DIR_VALUE_CONTEXT_NAME = "valueContext";
    private static final String BUNDLE_DEST_BASE_DIR_VALUE_NAME_NAME = "valueName";
    private static final String BUNDLE_DEST_BASE_DIR_DESCRIPTION_NAME = "description";

    private static final String BUNDLE_DEST_LOCATION_EXPRESSION_NAME = "expression";

    // this is the actual bundle configuration - see ResourceType.bundleConfiguration
    private Configuration bundleConfiguration;

    public ResourceTypeBundleConfiguration() {
        this.bundleConfiguration = null;
    }

    public ResourceTypeBundleConfiguration(Configuration bundleConfiguration) {
        this.bundleConfiguration = bundleConfiguration;
    }

    /**
     * Returns the actual, raw configuration. Callers should rarely want to use this - use the more
     * strongly typed methods such as {@link #getBundleDestinationDefinitions()}.
     *
     * @return the raw bundle configuration object
     */
    public Configuration getBundleConfiguration() {
        return this.bundleConfiguration;
    }

    public void setBundleConfiguration(Configuration bundleConfiguration) {
        this.bundleConfiguration = bundleConfiguration;
    }

    /**
     * A generic method to obtain all types of bundle destination definitions.
     * If this bundle configuration doesn't have any definitions, null is returned (though this
     * should never happen if the bundle configuration has been fully prepared for a resource type).
     * @return of bundle destination definitions that can be targets for bundle deployments
     */
    public Set<BundleDestinationDefinition> getBundleDestinationDefinitions() {
        return getBundleDestinationDefinitionsOfType(BundleDestinationDefinition.class);
    }

    /**
     * Returns the different destination base directories that can be the target for a bundle deployment.
     * If this bundle configuration doesn't have any base directories, null is returned (though this
     * should never happen if the bundle configuration has been fully prepared for a resource type).
     *
     * @return the set of destination base directories that can be targets for bundle deployments
     * @deprecated use the {@link #getBundleDestinationDefinitions()} in preference to this legacy method
     */
    @Deprecated
    public Set<BundleDestinationBaseDirectory> getBundleDestinationBaseDirectories() {
        return getBundleDestinationDefinitionsOfType(BundleDestinationBaseDirectory.class);
    }

    /**
     * Adds a destination base directory that can be used as a target for a bundle deployment.
     *
     * @param name         the name of this bundle destination base directory (must not be <code>null</code>)
     * @param valueContext indicates where the value's name can be looked up and found. This
     *                     must be the string form of one of the enums found
     *                     in {@link BundleDestinationBaseDirectory.Context}
     * @param valueName    the name of the property found in the given context where the value
     *                     of the base directory is
     * @param description  optional explanation for what this destination location is
     */
    public void addBundleDestinationBaseDirectory(String name, String valueContext, String valueName,
        String description) {
        // we create this just to make sure the context and value are valid. An exception will be thrown if they are not.
        addBundleDestinationDefinition(new BundleDestinationBaseDirectory(name, valueContext, valueName,
            description));

    }

    /**
     * Adds a destination location that can be used as a target for a bundle deployment.
     *
     * @param name         the name of this bundle destination location (must not be <code>null</code>)
     * @param expression   the expression of the value
     * @param description  optional explanation for what this destination location is
     */
    public void addBundleDestinationLocation(String name, String expression, String description) {
        // we create this just to make sure the context and value are valid. An exception will be thrown if they are not.
        addBundleDestinationDefinition(new BundleDestinationLocation(name, expression, description));
    }

    private void addBundleDestinationDefinition(BundleDestinationDefinition def) {
        if (this.bundleConfiguration == null) {
            throw new NullPointerException("bundleConfiguration == null");
        }

        PropertyList propertyList = this.bundleConfiguration.getList(BUNDLE_DEST_LIST_NAME);
        if (propertyList == null) {
            propertyList = new PropertyList(BUNDLE_DEST_LIST_NAME);
            this.bundleConfiguration.put(propertyList);
        }

        PropertyMap map = new PropertyMap(BUNDLE_DEST_LIST_ITEM_NAME);
        def.fillPropertyMap(map);

        propertyList.add(map);
    }

    private <T extends BundleDestinationDefinition> Set<T> getBundleDestinationDefinitionsOfType(Class<T> type) {
        if (this.bundleConfiguration == null) {
            return null;
        }

        PropertyList propertyList = this.bundleConfiguration.getList(BUNDLE_DEST_LIST_NAME);
        if (propertyList == null) {
            return null;
        }

        List<Property> list = propertyList.getList();
        if (list.size() == 0) {
            return null;
        }

        Set<T> retVal = new HashSet<T>(list.size());
        for (Property listItem : list) {
            PropertyMap map = (PropertyMap) listItem;
            T item = BundleDestinationDefinition.from(map, type);
            retVal.add(item);
        }

        return retVal;
    }

    @Override
    public int hashCode() {
        return ((bundleConfiguration == null) ? 0 : bundleConfiguration.hashCode());
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof ResourceTypeBundleConfiguration)) {
            return false;
        }
        ResourceTypeBundleConfiguration other = (ResourceTypeBundleConfiguration) obj;
        if (this.bundleConfiguration == null) {
            if (other.bundleConfiguration != null) {
                return false;
            }
        } else if (!this.bundleConfiguration.equals(other.bundleConfiguration)) {
            return false;
        }
        return true;
    }

    public static abstract class BundleDestinationDefinition implements Serializable {
        private static final long serialVersionUID = 1L;

        private final String name;
        private final String description;

        private BundleDestinationDefinition(String name, String description) {
            if (name == null) {
                throw new NullPointerException("name == null");
            }

            this.name = name;
            this.description = description;
        }

        private static <T extends BundleDestinationDefinition> T from(PropertyMap map, Class<T> expectedClass) {
            String name = map.getSimpleValue(BUNDLE_DEST_BASE_DIR_NAME_NAME, null);
            String valueContext = map.getSimpleValue(BUNDLE_DEST_BASE_DIR_VALUE_CONTEXT_NAME, null);
            String valueName = map.getSimpleValue(BUNDLE_DEST_BASE_DIR_VALUE_NAME_NAME, null);
            String description = map.getSimpleValue(BUNDLE_DEST_BASE_DIR_DESCRIPTION_NAME, null);
            String expression = map.getSimpleValue(BUNDLE_DEST_LOCATION_EXPRESSION_NAME, null);

            Class<?> determinedClass = Object.class;
            if (valueContext == null) {
                if (expression != null) {
                    determinedClass = BundleDestinationLocation.class;
                }
            } else {
                if (expression == null) {
                    determinedClass = BundleDestinationBaseDirectory.class;
                }
            }


            //this would be the preferred impl, but GWT thinks otherwise :(
            //if (expectedClass.isAssignableFrom(determinedClass)) {
            //    if (determinedClass.equals(BundleDestinationBaseDirectory.class)) {
            //        return expectedClass.cast(new BundleDestinationBaseDirectory(name, valueContext, valueName,
            //            description));
            //    } else if (determinedClass.equals(BundleDestinationLocation.class)) {
            //        return expectedClass.cast(new BundleDestinationLocation(name, expression, description));
            //    }
            //}

            if (determinedClass.equals(BundleDestinationBaseDirectory.class) && (expectedClass.equals(
                BundleDestinationBaseDirectory.class) || expectedClass.equals(BundleDestinationDefinition.class))) {
                @SuppressWarnings("unchecked")
                T ret = (T) new BundleDestinationBaseDirectory(name, valueContext, valueName, description);
                return ret;
            } else if (determinedClass.equals(BundleDestinationLocation.class) && (expectedClass.equals(
                BundleDestinationLocation.class) || expectedClass.equals(BundleDestinationDefinition.class))) {

                @SuppressWarnings("unchecked")
                T ret = (T) new BundleDestinationLocation(name, expression, description);
                return ret;
            }

            return null;
        }

        public String getName() {
            return name;
        }

        /**
         * @return an explanation for what this directory location is
         */
        public String getDescription() {
            return description;
        }

        protected void fillPropertyMap(PropertyMap map) {
            map.put(new PropertySimple(BUNDLE_DEST_BASE_DIR_NAME_NAME, getName()));

            if (getDescription() != null) {
                PropertySimple descriptionProp = new PropertySimple(BUNDLE_DEST_BASE_DIR_DESCRIPTION_NAME,
                    getDescription());
                map.put(descriptionProp);
            }

        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof BundleDestinationDefinition)) {
                return false;
            }

            BundleDestinationDefinition that = (BundleDestinationDefinition) o;

            if (!name.equals(that.name)) {
                return false;
            }

            return true;
        }

        @Override
        public int hashCode() {
            return name.hashCode();
        }
    }

    /**
     * Defines where bundles can be deployed on a resource that is of our resource type.
     */
    public static class BundleDestinationBaseDirectory extends BundleDestinationDefinition {
        private static final long serialVersionUID = 2L;

        /**
         * Defines the different places where we can lookup the named value that contains
         * the actual location of the destination base directory.
         * The names of these enum constants match the valid values that the agent
         * plugin XML schema accepts, to allow for easy translation from the XML
         * to this Java representation.
         */
        public enum Context {
            /** the value is to be found in the resource's plugin configuration */
            pluginConfiguration,

            /** the value is to be found in the resource's resource configuration */
            resourceConfiguration,

            /** the value is to be found as a measurement trait */
            measurementTrait,

            /** the value is a hardcoded location on the file system - usually the root "/" directory */
            fileSystem
        };

        private final Context valueContext;
        private final String valueName;

        public BundleDestinationBaseDirectory(String name, String valueContext, String valueName, String description) {
            super(name, description);
            this.valueContext = Context
                .valueOf(valueContext); // will throw an exception if its not valid, which is what we want
            this.valueName = valueName;
        }


        /**
         * @return indicates where the {@link #getValueName() value's name} can be looked up
         * and found. This must be one of the enums found in {@link BundleDestinationBaseDirectory.Context}
         */
        public Context getValueContext() {
            return valueContext;
        }

        /**
         * @return the name of the property found in the given {@link #getValueContext() context}
         * where the value of the base directory is
         */
        public String getValueName() {
            return valueName;
        }

        @Override
        protected void fillPropertyMap(PropertyMap map) {
            super.fillPropertyMap(map);
            PropertySimple valueContextProp = new PropertySimple(BUNDLE_DEST_BASE_DIR_VALUE_CONTEXT_NAME,
                getValueContext().name());
            PropertySimple valueNameProp = new PropertySimple(BUNDLE_DEST_BASE_DIR_VALUE_NAME_NAME, getValueName());

            map.put(valueContextProp);
            map.put(valueNameProp);
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append("BundleDestinationBaseDirectory [name=").append(getName()).append(", valueContext=").append(
                valueContext).append(", valueName=").append(valueName).append(", description=").append(getDescription())
                .append("]");
            return builder.toString();
        }
    }

    /**
     * Bundle destination location is another way of specifying the target location of a destination.
     * It is a generic expression that can be evaluated and can express anything, not just a filesystem location
     * as with the destination base directory.
     */
    public static final class BundleDestinationLocation extends BundleDestinationDefinition {
        private static final long serialVersionUID = 1L;

        private final String expression;

        public BundleDestinationLocation(String name, String expression, String description) {
            super(name, description);
            if (expression == null) {
                throw new IllegalArgumentException("expression == null");
            }

            this.expression = expression;
        }

        public String getExpression() {
            return expression;
        }

        @Override
        public String toString() {
            return "BundleDestinationLocation[name=" + getName() + ", expression=" + expression + ", description=" +
                getDescription();
        }
    }
}
