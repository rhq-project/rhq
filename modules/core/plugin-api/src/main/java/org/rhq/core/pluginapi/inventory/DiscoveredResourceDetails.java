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
package org.rhq.core.pluginapi.inventory;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.configuration.definition.ConfigurationTemplate;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.system.ProcessInfo;

/**
 * This contains all the details for a resource that was discovered by a {@link ResourceDiscoveryComponent}.
 *
 * <p>Note that any newly discovered resource must have a unique resource key as compared to other sibling resources.
 * That is to say, a parent resource's children must have unique resource keys (the children cannot share keys). A
 * correlary to this is that if a discovery component "re-discovers" a resource, the discovery component must ensure
 * that it assigns the same key to that re-discovered resource (i.e. resource keys must be consistent and stable across
 * multiple discoveries of that same resource). As an example, if you discover a resource "foo" and assign it a resource
 * key of "fooKey"; then the next time that "foo" resource is discovered again, the discovery component must ensure that
 * it discovers it with the resource key "fooKey" again.</p>
 *
 * @author John Mazzitelli
 */
public class DiscoveredResourceDetails {
    private static final int RESOURCE_KEY_MAX_LENGTH = 500;
    private static final int RESOURCE_NAME_MAX_LENGTH = 100;
    private static final int RESOURCE_VERSION_MAX_LENGTH = 50;
    private static final int RESOURCE_DESCRIPTION_MAX_LENGTH = 1000;

    private ResourceType resourceType;
    private String resourceKey;
    private String resourceName;
    private String resourceVersion;
    private String resourceDescription;
    private Configuration pluginConfiguration;
    private ProcessInfo processInfo;

    /**
     * This creates a new instance that provides details for a newly discovered resource.
     *
     * <p>Both resource key and resource name must be non-<code>null</code>; otherwise, an exception is thrown. If
     * resource version or resource description are <code>null</code>, their values will be set to an empty string.</p>
     *
     * @param  resourceType        the type of resource that was discovered (must not be <code>null</code>)
     * @param  resourceKey         the discovered resource's key where the key must be unique among the resource's
     *                             sibling resources; that is, a parent's direct child resources must all have unique
     *                             resource keys, but those keys need not be unique across the entire inventory of all
     *                             resources (must not be <code>null</code>)
     * @param  resourceName        the name of the discovered resource, used mainly for UI display purposes (must not be
     *                             <code>null</code>)
     * @param  resourceVersion     the discovered resource's version string (which may have any form or syntax
     *                             appropriate for the resource and may be <code>null</code>)
     * @param  resourceDescription a simple description of the discovered resource, which may or may not be an
     *                             internationalized string (may be <code>null</code>)
     * @param  pluginConfiguration the discovered resource's plugin configuration that will be used by the plugin to
     *                             connect to it (may be <code>null</code>, which means the new resource will just use
     *                             the default plugin configuration as defined by its resource type)
     * @param  processInfo         information on the process in which the newly discovered resource is running (this
     *                             may be <code>null</code> if unknown or not applicable)
     *
     * @throws IllegalArgumentException if the resource type, key or name is <code>null</code> or one of the String
     *                                  values are too long
     */
    public DiscoveredResourceDetails(@NotNull
    ResourceType resourceType, @NotNull
    String resourceKey, @NotNull
    String resourceName, @Nullable
    String resourceVersion, @Nullable
    String resourceDescription, @Nullable
    Configuration pluginConfiguration, @Nullable
    ProcessInfo processInfo) {
        if (resourceType == null) {
            throw new IllegalArgumentException("resourceType==null");
        }

        this.resourceType = resourceType;
        this.processInfo = processInfo;

        setResourceKey(resourceKey);
        setResourceName(resourceName);
        setResourceVersion(resourceVersion);
        setResourceDescription(resourceDescription);
        setPluginConfiguration(pluginConfiguration);

        return;
    }

    /**
     * The type of resource that was discovered.
     *
     * @return new resource's type
     */
    public ResourceType getResourceType() {
        return resourceType;
    }

    /**
     * The discovered resource's key where the key must be unique among the resource's sibling resources; that is, a
     * parent's direct child resources must all have unique resource keys, but those keys need not be unique across the
     * entire inventory of all resources.
     *
     * @return resource's unique key (will not be <code>null</code>)
     */
    public String getResourceKey() {
        return resourceKey;
    }

    /**
     * Sets the discovered resource's unique key. The key must be unique among the resource's sibling resources; that
     * is, a parent's direct child resources must all have unique resource keys, but those keys need not be unique
     * across the entire inventory of all resources.
     *
     * @param  resourceKey the discovered resource's key (must not be <code>null</code>)
     *
     * @throws IllegalArgumentException if <code>resourceKey</code> is <code>null</code> or too long
     */
    public void setResourceKey(String resourceKey) {
        if (resourceKey == null) {
            throw new IllegalArgumentException("resourceKey==null");
        }

        if (resourceKey.length() > RESOURCE_KEY_MAX_LENGTH) {
            throw new IllegalArgumentException("Resource key is longer than maximum length (" + RESOURCE_KEY_MAX_LENGTH
                + ") [" + resourceKey + "]");
        }

        this.resourceKey = resourceKey;
    }

    /**
     * The name of the discovered resource, which is used mainly for UI display purposes. This has no uniqueness
     * requirements (that is, resources can have the same names; this is true even for sibling resources).
     *
     * @return resource's name (will not be <code>null</code>)
     */
    public String getResourceName() {
        return resourceName;
    }

    /**
     * Sets the name of the discovered resource, which is used mainly for UI display purposes. The name can be anything
     * (other than <code>null</code>); it has no uniqueness requirements (that is, even sibling resources can have the
     * same name).
     *
     * @param  resourceName the discovered resource's name (must not be <code>null</code>)
     *
     * @throws IllegalArgumentException if <code>resourceName</code> is <code>null</code>
     */
    public void setResourceName(String resourceName) {
        if (resourceName == null) {
            throw new IllegalArgumentException("resourceName==null");
        }

        if (resourceName.length() > RESOURCE_NAME_MAX_LENGTH) {
            throw new IllegalArgumentException("Resource name is longer than maximum length ("
                + RESOURCE_NAME_MAX_LENGTH + ") [" + resourceName + "]");
        }

        this.resourceName = resourceName;
    }

    /**
     * The discovered resource's version string (which may have any form or syntax that is appropriate for the resource
     * and may be <code>null</code>)
     *
     * @return resource version string
     */
    public String getResourceVersion() {
        return resourceVersion;
    }

    /**
     * Sets the discovered resource's version string (which may have any form or syntax that is appropriate for the
     * resource and may be <code>null</code> which correlates to an empty version string).
     *
     * @param  resourceVersion the discovered resource's version string (may be <code>null</code>, which correlates to
     *                         an empty string)
     *
     * @throws IllegalArgumentException if the version string is too long
     */
    public void setResourceVersion(String resourceVersion) {
        if (resourceVersion == null) {
            resourceVersion = "";
        }

        if (resourceVersion.length() > RESOURCE_VERSION_MAX_LENGTH) {
            throw new IllegalArgumentException("Resource version is longer than maximum length ("
                + RESOURCE_VERSION_MAX_LENGTH + ") [" + resourceVersion + "]");
        }

        this.resourceVersion = resourceVersion;
    }

    /**
     * Gets the simple description of the resource, which may or may not be an internationalized string
     *
     * @return discovered resource's simple description string (may be <code>null</code>)
     */
    public String getResourceDescription() {
        return resourceDescription;
    }

    /**
     * Sets a simple description of the resource, which may or may not be an internationalized string
     *
     * @param  resourceDescription the discovered resource's description (may be <code>null</code>, which correlates to
     *                             an empty string)
     *
     * @throws IllegalArgumentException if the description string is too long
     */
    public void setResourceDescription(String resourceDescription) {
        if (resourceDescription == null) {
            resourceDescription = "";
        }

        if (resourceDescription.length() > RESOURCE_DESCRIPTION_MAX_LENGTH) {
            throw new IllegalArgumentException("Resource description is longer than maximum length ("
                + RESOURCE_DESCRIPTION_MAX_LENGTH + ") [" + resourceDescription + "]");
        }

        this.resourceDescription = resourceDescription;
    }

    /**
     * Returns the discovered resource's plugin configuration. If this was never
     * {@link #setPluginConfiguration(Configuration) set} before, a copy of the default plugin configuration, as defined
     * in the {@link #getResourceType() resource type}'s default template, is returned.
     *
     * @return copy of the resource's default plugin configuration
     */
    public Configuration getPluginConfiguration() {
        if (pluginConfiguration == null) {
            pluginConfiguration = getDefaultPluginConfiguration();
        }

        return pluginConfiguration;
    }

    /**
     * Returns the information on the operating system process in which the resource is running.
     *
     * @return resource's process information or <code>null</code> if not known
     */
    public ProcessInfo getProcessInfo() {
        return processInfo;
    }

    /**
     * Defines the discovered resource's plugin configuration. You normally call {@link #getPluginConfiguration()} first
     * to get a copy of the resource's default plugin configuration, and then modify that default configuration with
     * custom values.
     *
     * <p>If you never need to customize or change a discovered resource's plugin configuration, you will not have to
     * call this method. The plugin container will simply use the default plugin configuration from the resource's
     * {@link #getResourceType() type}.</p>
     *
     * @param pluginConfiguration the discovered resource's new plugin configuration
     *
     * @see   #setPluginConfiguration(Configuration)
     */
    public void setPluginConfiguration(Configuration pluginConfiguration) {
        this.pluginConfiguration = pluginConfiguration;
    }

    public String toString() {
        StringBuffer buf = new StringBuffer();

        buf.append("key=");
        buf.append(getResourceKey());
        buf.append(",name=");
        buf.append(getResourceName());
        buf.append(",type=");
        buf.append(getResourceType().getName());
        buf.append(",version=");
        buf.append(getResourceVersion());
        buf.append(",description=");
        buf.append(getResourceDescription());

        return buf.toString();
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if ((o == null) || (getClass() != o.getClass())) {
            return false;
        }

        DiscoveredResourceDetails that = (DiscoveredResourceDetails) o;

        if (!resourceKey.equals(that.resourceKey)) {
            return false;
        }

        if (!resourceType.equals(that.resourceType)) {
            return false;
        }

        return true;
    }

    public int hashCode() {
        int result;
        result = resourceType.hashCode();
        result = (31 * result) + resourceKey.hashCode();
        return result;
    }

    /**
     * Returns a copy of the {@link #getResourceType() resource type}'s default plugin configuration.
     *
     * @return copy of the resource's default plugin configuration
     */
    private Configuration getDefaultPluginConfiguration() {
        ConfigurationDefinition definition = resourceType.getPluginConfigurationDefinition();
        if (definition != null) {
            ConfigurationTemplate template = definition.getDefaultTemplate();
            if (template != null) {
                return template.getConfiguration().deepCopy();
            }
        }

        return new Configuration(); // there is no default plugin config available, return an empty one
    }
}