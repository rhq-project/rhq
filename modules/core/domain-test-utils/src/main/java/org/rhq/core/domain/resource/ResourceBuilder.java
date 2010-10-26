/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
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

package org.rhq.core.domain.resource;

import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.measurement.ResourceAvailability;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * ResoureBuilder is a builder object that creates Resource objects. The builder ensures that a Resource is created
 * in a valid state, specifically, fields that are not nullable are required to have non-null values. Using a builder
 * should help make the intent of tests clearer and more self-documenting.
 * <br/><br/>
 * A couple things need to be pointed out. First, this class currently does not yet provide support for all Resource
 * fields/properties. Secondly, this class will likely be moved to a test utility module in a subsequent commit so that
 * it can be reused by other tests in other modules.
 *
 * @author John Sanda
 */
public class ResourceBuilder {

    private ResourceBuilder parentBuilder;

    private Resource resource;

    private Random random;

    private boolean useDefaultResourceType;

    private ResourceCategory category;

    private List<ResourceBuilder> childBuilders = new ArrayList<ResourceBuilder>();

    public static class AssociationBuilder {
        private ResourceBuilder resourceBuilder;

        private int count;

        AssociationBuilder(ResourceBuilder builder, int count) {
            resourceBuilder = builder;
            this.count = count;
        }

        public ResourceBuilder randomChildServers() {
            for (int i = 0; i < count; ++i) {
                ResourceBuilder childBuilder = new ResourceBuilder(ResourceCategory.SERVER, resourceBuilder);
                resourceBuilder.childBuilders.add(childBuilder.createRandomServer());
            }
            return resourceBuilder;
        }

        public ResourceBuilder randomChildServices() {
            for (int i = 0; i < count; ++i) {
                ResourceBuilder childBuilder = new ResourceBuilder(ResourceCategory.SERVICE, resourceBuilder);
                resourceBuilder.childBuilders.add(childBuilder.createRandomService());
            }
            return resourceBuilder;
        }
    }

    public static class ChildrenResourceBuilder {
        private ResourceBuilder parentBuilder;

        ChildrenResourceBuilder(ResourceBuilder builder, ResourceCategory category, int numChildren) {
            parentBuilder = builder;
            parentBuilder.childBuilders = new ArrayList<ResourceBuilder>(numChildren);
            for (int i = 0; i < numChildren; ++i) {
                parentBuilder.childBuilders.add(new ResourceBuilder(category, parentBuilder).createResource());
            }
        }

        public ChildrenResourceBuilder inInventory() {
            for (ResourceBuilder childBuilder : parentBuilder.childBuilders) {
                childBuilder.inInventory();
            }
            return this;
        }

        public ChildrenResourceBuilder notInInventory() {
            for (ResourceBuilder childBuilder : parentBuilder.childBuilders) {
                childBuilder.notInInventory();
            }
            return this;
        }

        public ResourceBuilder included() {
            return parentBuilder;
        }
    }


    public ResourceBuilder() {
    }

    private ResourceBuilder(ResourceCategory category, ResourceBuilder parentBuilder) {
        this.category = category;
        this.parentBuilder = parentBuilder;
    }

    public ResourceBuilder createResource() {
        resource = new Resource();
        random = new Random();
        return this;
    }

    public ResourceBuilder createPlatform() {
        category = ResourceCategory.PLATFORM;
        return createResource();
    }

    public ResourceBuilder createServer() {
        category = ResourceCategory.SERVER;
        return createResource();
    }

    public ResourceBuilder createRandomServer() {
        category = ResourceCategory.SERVER;
        createResource();
        withRandomId();
        withRandomName("server:");
        withRandomResourceKey("server:");
        withRandomUuid("server:");
        withDefaultServerResourceType();

        return this;
    }

    public ResourceBuilder createService() {
        category = ResourceCategory.SERVICE;
        return createResource();
    }

    public ResourceBuilder createRandomService() {
        category = ResourceCategory.SERVICE;
        createResource();
        withRandomId();
        withRandomName("service:");
        withRandomResourceKey("service:");
        withRandomUuid("service:");
        withDefaultServiceResourceType();

        return this;
    }

    /**
     * Using a default resource type results in <code>Resource.resourceType</code> being assigned to a new
     * ResourceType object that has some default values applied to it. If the Resource being created is a platform, then
     * the ResourceType will be a platform. More specifically, <code>ResourceType.category</code> will be assigned a
     * value of {@link ResourceCategory#PLATFORM}. Likewise, if the Resource being created is a server, then its
     * ResourceType object will have a category of {@link ResourceCategory#SERVER}. And if the Resource is a service,
     * then the ResourceType category will be {@link ResourceCategory#SERVICE}.
     * <br/><br/>
     * The resource type name defaults to the name of resource. And the plugin name (as specified by
     * ResourceType.plugin) defaults to <code>Resource.name + " Plugin"</code>
     * <br/><br/>
     * When using a default resource type, the resource must be created using one of {@link #createPlatform()},
     * {@link #createServer()}, or {@link #createService()}; otherwise, an exception will be thrown since the builder
     * will not have sufficient information to create the resource type.
     * <br/><br/>
     * Lastly, if you specify that a default resource type by calling this method and also specify the resource tye
     * with {@link #withResourceType(ResourceType)}, the latter will be overwritten regardless of when it is called. The
     * default will be used instead.
     *
     * @return The builder
     *
     */
    public ResourceBuilder usingDefaultResourceType() {
        useDefaultResourceType = true;
        return this;
    }

    public ResourceBuilder withId(int id) {
        resource.setId(id);
        return this;
    }

    public ResourceBuilder withRandomId() {
        resource.setId(random.nextInt());
        return this;
    }

    public ResourceBuilder withResourceKey(String key) {
        resource.setResourceKey(key);
        return this;
    }

    public ResourceBuilder withRandomResourceKey(String prefix) {
        resource.setResourceKey(prefix + randomString());
        return this;
    }

    public ResourceBuilder withRandomResourceKey() {
        return withRandomResourceKey("");
    }

    public ResourceBuilder withName(String name) {
        resource.setName(name);
        return this;
    }

    public ResourceBuilder withRandomName(String prefix) {
        resource.setName(prefix + randomString());
        return this;
    }

    public ResourceBuilder withRandomName() {
        return withRandomName("");
    }

    public ResourceBuilder withResourceType(ResourceType resourceType) {
        resource.setResourceType(resourceType);
        return this;
    }

    public ResourceBuilder withUuid(String uuid) {
        resource.setUuid(uuid);
        return this;
    }

    public ResourceBuilder withRandomUuid(String prefix) {
        resource.setUuid(prefix + randomString());
        return this;
    }

    public ResourceBuilder withRandomUuid() {
        return withRandomUuid("");
    }

    public ResourceBuilder withVersion(String version) {
        resource.setVersion(version);
        return this;
    }

    public ResourceBuilder withCurrentAvailability(AvailabilityType availabilityType) {
        ResourceAvailability availability = new ResourceAvailability(resource,  availabilityType);
        resource.setCurrentAvailability(availability);
        return this;
    }

    public ResourceBuilder withInventoryStatus(InventoryStatus inventoryStatus) {
        resource.setInventoryStatus(inventoryStatus);
        return this;
    }

    /**
     * Set the <code>inventoryStatus</code> to {@link InventoryStatus#COMMITTED}
     *
     * @return The builder
     */
    public ResourceBuilder inInventory() {
        resource.setInventoryStatus(InventoryStatus.COMMITTED);
        return this;
    }

    /**
     * Set the <code>inventoryStatus</code> to {@link InventoryStatus#NEW}
     *
     * @return The builder
     */
    public ResourceBuilder notInInventory() {
        resource.setInventoryStatus(InventoryStatus.NEW);
        return this;
    }

    public AssociationBuilder with(int count) {
        return new AssociationBuilder(this, count);
    }

    public ResourceBuilder withChildService() {
        ResourceBuilder childBuilder = new ResourceBuilder(ResourceCategory.SERVICE, this);
        childBuilders.add(childBuilder.createService());
        return childBuilder;
    }

    public ResourceBuilder included() {
        return this.parentBuilder;
    }

    public Resource build() {
        String errors = validate();
        if (errors != null) {
            throw new BuilderException(errors);
        }

        if (useDefaultResourceType) {
            withDefaultResourceType();
        }

        for (ResourceBuilder childBuilder : childBuilders) {
            resource.addChildResource(childBuilder.build());
        }

        return resource;
    }

    private String validate() {
        StringBuilder errors = new StringBuilder();

        if (resource.getUuid() == null) {
            // Making uuid required since it is used in equals/hashCode
            errors.append("uuid is a required property\n");
        }

        if (resource.getName() == null) {
            errors.append("name is a required property\n");
        }

        if (useDefaultResourceType && category == null) {
            errors.append("When using default resource type, the resource must be created with one of " +
                "createPlatform(), createServer(), or createService()\n");
        }

        // We only care that resourceType is set if we are not using a default type. If we are using a default,
        // then the resourceType property will be set after validation, assuming there are no validation errors.
        if (!useDefaultResourceType && resource.getResourceType() == null) {
            errors.append("resourceType is a required property\n");
        }

        for (ResourceBuilder childBuilder : childBuilders) {
            String childErrors = childBuilder.validate();
            if (childErrors != null) {
                errors.append("The following child resource errors were found:\n" + childErrors);
            }
        }

        if (errors.length() == 0) {
            return null;
        }

        return "Unable to build Resource instance due to the following validation errors:\n" + errors;
    }

    private ResourceBuilder withDefaultResourceType() {
        switch (category) {
            case PLATFORM: return withDefaultPlatformResourceType();
            case SERVER:   return withDefaultServerResourceType();
            default:       return withDefaultServiceResourceType();
        }
    }

    /**
     * The default platform resource type is as its name implies a platform type whose name defaults to the resource
     * name and the plugin name (i.e., ResourceType.plugin property) defaults to resource name + 'Plugin'.
     *
     * @return The builder
     */
    private ResourceBuilder withDefaultPlatformResourceType() {
        resource.setResourceType(new ResourceTypeBuilder().createPlatformResourceType()
            .withName(resource.getName())
            .withPlugin(resource.getName() + " Plugin")
            .withParentResourceType(resource.getResourceType())
            .build());
        return this;
    }

    /**
     * The default server resource type is as its name implies a server type whose name defaults to the resource name
     * and the plugin name (i.e., ResourceType.plugin property) defaults to resource name + 'Plugin'
     *
     * @return The builder
     */
    private ResourceBuilder withDefaultServerResourceType() {
        resource.setResourceType(new ResourceTypeBuilder().createServerResourceType()
            .withName(resource.getName())
            .withPlugin(resource.getName() + " Plugin")
            .withParentResourceType(resource.getResourceType())
            .build());
        return this;
    }

    /**
     * The default service resource type is as its name implies a service type whose name defaults to the resource name
     * and the plugin name (i.e., ResourceType.plugin property) defaults to resource name + 'Plugin'
     *
     * @return The builder
     */
    private ResourceBuilder withDefaultServiceResourceType() {
        resource.setResourceType(new ResourceTypeBuilder().createServerResourceType()
            .withName(resource.getName())
            .withPlugin(resource.getName() + " Plugin")
            .withParentResourceType(resource.getResourceType())
            .build());
        return this;
    }

    private String randomString() {
        return new BigInteger(16, random).toString(32);
    }

}
