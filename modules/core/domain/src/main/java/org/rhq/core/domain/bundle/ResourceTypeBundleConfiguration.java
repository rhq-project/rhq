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
import java.util.ArrayList;
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

    private static final String BUNDLE_DEST_DEFINITION_CONNECTION_NAME = "connection";
    private static final String BUNDLE_DEST_DEFINITION_REF_LIST_NAME = "refs";
    private static final String BUNDLE_DEST_DEFINITION_REF_LIST_MEMBER_NAME = "ref";

    private static final String BUNDLE_DEST_DEF_REF_TARGET_NAME_NAME = "targetName";
    private static final String BUNDLE_DEST_DEF_REF_NAME_NAME = "name";
    private static final String BUNDLE_DEST_DEF_REF_CONTEXT_NAME = "context";
    private static final String BUNDLE_DEST_DEF_REF_TYPE_NAME = "type";


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
     * strongly typed methods such as {@link #getBundleDestinationSpecifications()}.
     *
     * @return the raw bundle configuration object
     */
    public Configuration getBundleConfiguration() {
        return this.bundleConfiguration;
    }

    /**
     * @deprecated Do not use this method, instead use the provided type-safe modification methods provided
     *             in this class.
     */
    @Deprecated
    public void setBundleConfiguration(Configuration bundleConfiguration) {
        this.bundleConfiguration = bundleConfiguration;
    }

    /**
     * A generic method to obtain all types of bundle destination specifications.
     * If this bundle configuration doesn't have any specifications, null is returned (though this
     * should never happen if the bundle configuration has been fully prepared for a resource type).
     *
     * @return of bundle destination specifications that can be targets for bundle deployments
     */
    public Set<BundleDestinationSpecification> getBundleDestinationSpecifications() {
        return getBundleDestinationSpecificationsOfType(BundleDestinationSpecification.class);
    }

    /**
     * Returns the different destination base directories that can be the target for a bundle deployment.
     * If this bundle configuration doesn't have any base directories, null is returned (though this
     * should never happen if the bundle configuration has been fully prepared for a resource type).
     *
     * @return the set of destination base directories that can be targets for bundle deployments
     *
     * @deprecated use the {@link #getBundleDestinationSpecifications()} in preference to this legacy method
     */
    @Deprecated
    public Set<BundleDestinationBaseDirectory> getBundleDestinationBaseDirectories() {
        return getBundleDestinationSpecificationsOfType(BundleDestinationBaseDirectory.class);
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
        addBundleDestinationSpecification(new BundleDestinationBaseDirectory(name, valueContext, valueName,
            description));

    }

    /**
     * Creates a new destination definition builder initialized with the provided name that will add
     * the built destination definition into this bundle configuration instance automatically.
     *
     * @param name        the name of this bundle destination location (must not be <code>null</code>)
     */
    public BundleDestinationDefinition.Builder createDestinationDefinitionBuilder(String name) {
        if (name == null) {
            throw new IllegalArgumentException("name == null");
        }

        return BundleDestinationDefinition.builderAddingTo(this).withName(name);
    }

    private void addBundleDestinationSpecification(BundleDestinationSpecification def) {
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

    private <T extends BundleDestinationSpecification> Set<T> getBundleDestinationSpecificationsOfType(Class<T> type) {
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
            T item = BundleDestinationSpecification.from(map, type);
            if (item != null) {
                retVal.add(item);
            }
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

    public static abstract class BundleDestinationSpecification implements Serializable {
        private static final long serialVersionUID = 1L;

        private final String name;
        private final String description;

        private BundleDestinationSpecification(String name, String description) {
            if (name == null) {
                throw new NullPointerException("name == null");
            }

            this.name = name;
            this.description = description;
        }

        private static <T extends BundleDestinationSpecification> T from(PropertyMap map, Class<T> expectedClass) {
            String name = map.getSimpleValue(BUNDLE_DEST_BASE_DIR_NAME_NAME, null);
            String valueContext = map.getSimpleValue(BUNDLE_DEST_BASE_DIR_VALUE_CONTEXT_NAME, null);
            String description = map.getSimpleValue(BUNDLE_DEST_BASE_DIR_DESCRIPTION_NAME, null);
            String connection = map.getSimpleValue(BUNDLE_DEST_DEFINITION_CONNECTION_NAME, null);
            PropertyList refs = map.getList(BUNDLE_DEST_DEFINITION_REF_LIST_NAME);

            Class<?> determinedClass = Object.class;
            if (valueContext == null) {
                if (connection != null || refs != null) {
                    determinedClass = BundleDestinationDefinition.class;
                }
            } else {
                if (connection == null && refs == null) {
                    determinedClass = BundleDestinationBaseDirectory.class;
                }
            }


            //this would be the preferred impl, but GWT thinks otherwise :(
            //if (expectedClass.isAssignableFrom(determinedClass)) {
            //    if (determinedClass.equals(BundleDestinationBaseDirectory.class)) {
            //        return expectedClass.cast(new BundleDestinationBaseDirectory(...);
            //    } else if (determinedClass.equals(BundleDestinationLocation.class)) {
            //        return expectedClass.cast(new BundleDestinationLocation(...));
            //    }
            //}

            if (determinedClass.equals(BundleDestinationBaseDirectory.class) && (expectedClass.equals(
                BundleDestinationBaseDirectory.class) || expectedClass.equals(BundleDestinationSpecification.class))) {

                String valueName = map.getSimpleValue(BUNDLE_DEST_BASE_DIR_VALUE_NAME_NAME, null);

                @SuppressWarnings("unchecked")
                T ret = (T) new BundleDestinationBaseDirectory(name, valueContext, valueName, description);
                return ret;
            } else if (determinedClass.equals(BundleDestinationDefinition.class) && (expectedClass.equals(
                BundleDestinationDefinition.class) || expectedClass.equals(BundleDestinationSpecification.class))) {

                BundleDestinationDefinition.Builder bld = BundleDestinationDefinition.builder().withName(name)
                    .withConnectionString(connection).withDescription(description);

                if (refs != null) {
                    for (Property p : refs.getList()) {
                        PropertyMap ref = (PropertyMap) p;

                        String type = ref.getSimpleValue(BUNDLE_DEST_DEF_REF_TYPE_NAME, null);
                        if (type == null) {
                            continue;
                        }

                        BundleDestinationDefinition.ConfigRef.Type refType =
                            BundleDestinationDefinition.ConfigRef.Type.valueOf(type);

                        String context = ref.getSimpleValue(BUNDLE_DEST_DEF_REF_CONTEXT_NAME, null);
                        if (context == null) {
                            continue;
                        }

                        BundleDestinationDefinition.ConfigRef.Context refContext =
                            BundleDestinationDefinition.ConfigRef.Context.valueOf(context);

                        String refName = ref.getSimpleValue(BUNDLE_DEST_DEF_REF_NAME_NAME, null);
                        if (name == null) {
                            continue;
                        }

                        String refTargetName = ref.getSimpleValue(BUNDLE_DEST_DEF_REF_TARGET_NAME_NAME, refName);

                        bld.addPropertyReference(refType, refContext, refName, refTargetName);
                    }
                }

                @SuppressWarnings("unchecked")
                T ret = (T) bld.build();
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
            if (!(o instanceof BundleDestinationSpecification)) {
                return false;
            }

            BundleDestinationSpecification that = (BundleDestinationSpecification) o;

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
    public static class BundleDestinationBaseDirectory extends BundleDestinationSpecification {
        private static final long serialVersionUID = 2L;

        /**
         * Defines the different places where we can lookup the named value that contains
         * the actual location of the destination base directory.
         * The names of these enum constants match the valid values that the agent
         * plugin XML schema accepts, to allow for easy translation from the XML
         * to this Java representation.
         */
        public enum Context {
            /**
             * the value is to be found in the resource's plugin configuration
             */
            pluginConfiguration,

            /**
             * the value is to be found in the resource's resource configuration
             */
            resourceConfiguration,

            /**
             * the value is to be found as a measurement trait
             */
            measurementTrait,

            /**
             * the value is a hardcoded location on the file system - usually the root "/" directory
             */
            fileSystem
        }

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
     * Bundle destination definition is another way of specifying the target location of a destination.
     * It has a generic "connection" string, which is an expression for specifying the location of the target
     * (which might not be filesystem based as with destination base dirs) and also allows for passing parts
     * of the configurations of the resources being deployed to to the bundle handler.
     */
    public static final class BundleDestinationDefinition extends BundleDestinationSpecification {
        private static final long serialVersionUID = 1L;

        private final String connectionString;
        private final List<ConfigRef> referencedConfiguration;

        public static Builder builder() {
            return new Builder(null);
        }

        public static Builder builderAddingTo(ResourceTypeBundleConfiguration bundleConfig) {
            return new Builder(bundleConfig);
        }

        public BundleDestinationDefinition(String name, String connectionString, String description,
            List<ConfigRef> refs) {
            super(name, description);

            this.connectionString = connectionString;
            this.referencedConfiguration = refs;
        }

        @Override
        protected void fillPropertyMap(PropertyMap map) {
            super.fillPropertyMap(map);
            map.put(new PropertySimple(BUNDLE_DEST_DEFINITION_CONNECTION_NAME, connectionString));
            PropertyList list = new PropertyList(BUNDLE_DEST_DEFINITION_REF_LIST_NAME);
            map.put(list);

            for (ConfigRef ref : referencedConfiguration) {
                PropertyMap refMap = new PropertyMap(BUNDLE_DEST_DEFINITION_REF_LIST_MEMBER_NAME);
                refMap.put(new PropertySimple(BUNDLE_DEST_DEF_REF_TYPE_NAME, ref.getType().name()));
                refMap.put(new PropertySimple(BUNDLE_DEST_DEF_REF_CONTEXT_NAME, ref.getContext().name()));
                refMap.put(new PropertySimple(BUNDLE_DEST_DEF_REF_NAME_NAME, ref.getName()));
                refMap.put(new PropertySimple(BUNDLE_DEST_DEF_REF_TARGET_NAME_NAME, ref.getTargetName()));
                list.add(refMap);
            }
        }

        public String getConnectionString() {
            return connectionString;
        }

        public List<ConfigRef> getReferencedConfiguration() {
            return referencedConfiguration;
        }

        @Override
        public String toString() {
            return "BundleDestinationLocation[name=" + getName() + ", conn=" + connectionString + ", refs=" +
                referencedConfiguration + ", description=" + getDescription();
        }

        public static final class ConfigRef {
            public enum Type {
                MAP, LIST, SIMPLE, FULL
            }

            public enum Context {
                PLUGIN_CONFIGURATION, RESOURCE_CONFIGURATION, MEASUREMENT_TRAIT
            }

            private final String targetName;
            private final String name;
            private final Type type;
            private final Context context;

            public ConfigRef(String targetName, String name, Type type, Context context) {
                if (type != Type.FULL && name == null) {
                    throw new IllegalArgumentException("name == null");
                }

                if (type == null) {
                    throw new IllegalArgumentException("type == null");
                }

                if (context == null) {
                    throw new IllegalArgumentException("context == null");
                }

                this.targetName = targetName == null ? name : targetName;
                this.name = name;
                this.type = type;
                this.context = context;
            }

            public String getTargetName() {
                return targetName;
            }

            public String getName() {
                return name;
            }

            public Type getType() {
                return type;
            }

            public Context getContext() {
                return context;
            }

            @Override
            public String toString() {
                final StringBuilder sb = new StringBuilder("PropertyRef[");
                sb.append("context=").append(context);
                sb.append(", name='").append(name).append('\'');
                sb.append(", targetName='").append(targetName).append('\'');
                sb.append(", type=").append(type);
                sb.append(']');
                return sb.toString();
            }
        }

        public static final class Builder {
            private final ResourceTypeBundleConfiguration targetConfig;
            private String connString;
            private String name;
            private String description;
            private final List<ConfigRef> refs = new ArrayList<ConfigRef>();

            private Builder(ResourceTypeBundleConfiguration targetConfig) {
                this.targetConfig = targetConfig;
            }

            public Builder withName(String name) {
                this.name = name;
                return this;
            }

            public Builder withDescription(String description) {
                this.description = description;
                return this;
            }

            public Builder withConnectionString(String connString) {
                this.connString = connString;
                return this;
            }

            public Builder addPropertyReference(ConfigRef.Type type, ConfigRef.Context context, String name,
                String targetName) {

                refs.add(new ConfigRef(targetName, name, type, context));
                return this;
            }

            public Builder addPluginConfigurationSimplePropertyReference(String name, String targetName) {
                refs.add(new ConfigRef(targetName, name, ConfigRef.Type.SIMPLE,
                    ConfigRef.Context.PLUGIN_CONFIGURATION));
                return this;
            }

            public Builder addPluginConfigurationListPropertyReference(String name, String targetName) {
                refs.add(new ConfigRef(targetName, name, ConfigRef.Type.LIST,
                    ConfigRef.Context.PLUGIN_CONFIGURATION));
                return this;
            }

            public Builder addPluginConfigurationMapPropertyReference(String name, String targetName) {
                refs.add(new ConfigRef(targetName, name, ConfigRef.Type.MAP,
                    ConfigRef.Context.PLUGIN_CONFIGURATION));
                return this;
            }

            public Builder addFullPluginConfigurationReference(String prefix) {
                refs.add(new ConfigRef(prefix, null, ConfigRef.Type.FULL, ConfigRef.Context.PLUGIN_CONFIGURATION));
                return this;
            }

            public Builder addResourceConfigurationSimplePropertyReference(String name, String targetName) {
                refs.add(new ConfigRef(targetName, name, ConfigRef.Type.SIMPLE,
                    ConfigRef.Context.RESOURCE_CONFIGURATION));
                return this;
            }

            public Builder addResourceConfigurationListPropertyReference(String name, String targetName) {
                refs.add(new ConfigRef(targetName, name, ConfigRef.Type.LIST,
                    ConfigRef.Context.RESOURCE_CONFIGURATION));
                return this;
            }

            public Builder addResourceConfigurationMapPropertyReference(String name, String targetName) {
                refs.add(new ConfigRef(targetName, name, ConfigRef.Type.MAP,
                    ConfigRef.Context.RESOURCE_CONFIGURATION));
                return this;
            }

            public Builder addFullResourceConfigurationReference(String prefix) {
                refs.add(new ConfigRef(null, prefix, ConfigRef.Type.FULL, ConfigRef.Context.RESOURCE_CONFIGURATION));
                return this;
            }

            public Builder addMeasurementTraitReference(String name, String targetName) {
                refs.add(new ConfigRef(targetName, name, null, ConfigRef.Context.MEASUREMENT_TRAIT));
                return this;
            }

            public Builder addFullMeasurementTraitsReference(String prefix) {
                refs.add(new ConfigRef(null, prefix, ConfigRef.Type.FULL, ConfigRef.Context.MEASUREMENT_TRAIT));
                return this;
            }

            public BundleDestinationDefinition build() {
                BundleDestinationDefinition ret = new BundleDestinationDefinition(name, connString, description, refs);
                if (targetConfig != null) {
                    targetConfig.addBundleDestinationSpecification(ret);
                }

                return ret;
            }
        }
    }
}
