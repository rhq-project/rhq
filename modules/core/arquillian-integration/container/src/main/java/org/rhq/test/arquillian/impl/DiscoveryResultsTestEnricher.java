package org.rhq.test.arquillian.impl;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.Set;

import org.jboss.arquillian.core.api.Instance;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.test.spi.annotation.TestScoped;

import org.rhq.core.clientapi.agent.metadata.PluginMetadataManager;
import org.rhq.core.clientapi.agent.metadata.ResourceTypeNotEnabledException;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.pc.PluginContainer;
import org.rhq.core.pc.inventory.InventoryManager;
import org.rhq.core.pc.inventory.ResourceContainer;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.test.arquillian.DiscoveredResources;
import org.rhq.test.arquillian.ResourceComponentInstances;
import org.rhq.test.arquillian.ResourceContainers;
import org.rhq.test.arquillian.spi.PostPrepareEnricher;

public class DiscoveryResultsTestEnricher implements PostPrepareEnricher {

    @Inject
    @TestScoped
    private Instance<PluginContainer> pluginContainer;
    
    @Override
    public void enrich(Object testCase) {
        Set<Field> discoveredResourceFields = new HashSet<Field>();
        Set<Field> resourceComponentFields = new HashSet<Field>();
        Set<Field> resourceContainerFields = new HashSet<Field>();
        
        for (Field f : testCase.getClass().getDeclaredFields()) {
            collectDiscoveredResourceFields(f, discoveredResourceFields);
            collectResourceComponentFields(f, resourceComponentFields);
            collectResourceContainerFields(f, resourceContainerFields);
        }
        
        for(Field f : discoveredResourceFields) {
            assignDiscoveredResourceField(testCase, f);
        }
        
        for(Field f : resourceComponentFields) {
            assignResourceComponentField(testCase, f);
        }
        
        for(Field f : resourceContainerFields) {
            assignResourceContainerField(testCase, f);            
        }
    }

    private PluginContainer getPluginContainer() {
        return pluginContainer.get();
    }
    
    private void collectDiscoveredResourceFields(Field f, Set<Field> fields) {
        DiscoveredResources a = f.getAnnotation(DiscoveredResources.class);
        if (a != null && hasType(f, Set.class, Resource.class)) {
             fields.add(f);
        }
    }
    
    private void collectResourceComponentFields(Field f, Set<Field> fields) {
        ResourceComponentInstances a = f.getAnnotation(ResourceComponentInstances.class);
        if (a != null) {
            ResourceType rt = getResourceType(a.plugin(), a.resourceType());
            Class<?> resourceComponentClass = getResourceComponentClass(rt);
            if (hasType(f, Set.class, resourceComponentClass)) {
                fields.add(f);
            }
        }
    }
    
    private void collectResourceContainerFields(Field f, Set<Field> fields) {
        ResourceContainers a = f.getAnnotation(ResourceContainers.class);
        if (a != null && hasType(f, Set.class, ResourceContainer.class)) {
            fields.add(f);
        }
    }
    
    private void assignDiscoveredResourceField(Object testCase, Field f) {
        DiscoveredResources config = f.getAnnotation(DiscoveredResources.class);
        
        String pluginName = config.plugin();
        String resourceTypeName = config.resourceType();
        
        ResourceType resourceType = getPluginContainer().getPluginManager().getMetadataManager().getType(resourceTypeName, pluginName);
        if (resourceType == null) {
            return;
        }
        
        Set<Resource> resources = getPluginContainer().getInventoryManager().getResourcesWithType(resourceType);
        
        f.setAccessible(true);
        try {
            f.set(testCase, resources);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException(
                "Could not enrich the test class with the discovered resources on field " + f, e);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(
                "Could not enrich the test class with the discovered resources on field " + f, e);
        }
    }

    private void assignResourceComponentField(Object testCase, Field f) {
        ResourceComponentInstances config = f.getAnnotation(ResourceComponentInstances.class);
        
        String pluginName = config.plugin();
        String resourceTypeName = config.resourceType();

        ResourceType resourceType = getResourceType(pluginName, resourceTypeName);
        Class<?> componentClass = getResourceComponentClass(resourceType);
        
        if (hasType(f, Set.class, componentClass)) {                
            InventoryManager im = getPluginContainer().getInventoryManager();
            Set<Resource> resources = im.getResourcesWithType(resourceType);
            
            Set<ResourceComponent<?>> components = new HashSet<ResourceComponent<?>>(resources.size());
            for(Resource r : resources) {
                components.add(im.getResourceComponent(r));
            }
            
            f.setAccessible(true);
            try {
                f.set(testCase, components);
            } catch (IllegalArgumentException e) {
                throw new IllegalStateException(
                    "Could not enrich the test class with the discovered resources on field " + f, e);
            } catch (IllegalAccessException e) {
                throw new IllegalStateException(
                    "Could not enrich the test class with the discovered resources on field " + f, e);
            }
        }        
    }
    

    private void assignResourceContainerField(Object testCase, Field f) {
        ResourceContainers config = f.getAnnotation(ResourceContainers.class);
        
        String pluginName = config.plugin();
        String resourceTypeName = config.resourceType();
        
        ResourceType resourceType = getPluginContainer().getPluginManager().getMetadataManager().getType(resourceTypeName, pluginName);
        if (resourceType == null) {
            return;
        }
        
        Set<Resource> resources = getPluginContainer().getInventoryManager().getResourcesWithType(resourceType);

        Set<ResourceContainer> containers = new HashSet<ResourceContainer>();
        for(Resource r : resources) {
            containers.add(getPluginContainer().getInventoryManager().getResourceContainer(r));
        }
        
        f.setAccessible(true);
        try {
            f.set(testCase, containers);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException(
                "Could not enrich the test class with the discovered resources on field " + f, e);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(
                "Could not enrich the test class with the discovered resources on field " + f, e);
        }
    }

    private ResourceType getResourceType(String pluginName, String resourceTypeName) {
        PluginMetadataManager pmm = getPluginContainer().getPluginManager().getMetadataManager();
        ResourceType resourceType = pmm.getType(resourceTypeName, pluginName);
        
        if (resourceType == null) {
            throw new IllegalArgumentException("No resource type called '" + resourceTypeName + "' found in plugin '" + pluginName + "'.");
        }
        
        return resourceType;
    }
    
    private Class<?> getResourceComponentClass(ResourceType resourceType) {
        PluginMetadataManager pmm = getPluginContainer().getPluginManager().getMetadataManager();
                
        String componentClassName;
        try {
            componentClassName = pmm.getComponentClass(resourceType);
        } catch (ResourceTypeNotEnabledException rtne) {
            throw new RuntimeException(rtne);
        }

        try {
            return Class.forName(componentClassName);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("Could not find the component class " + componentClassName, e);
        }
    }
    
    private static boolean hasType(Field f, Class<?> type, Class<?>... typeParams) {
        Type fieldType = f.getGenericType();
        
        if (typeParams.length == 0) {
            return type.equals(fieldType);
        }
        
        if (!(fieldType instanceof ParameterizedType)) {
            return false;
        }
        
        ParameterizedType ptype = (ParameterizedType) fieldType;
        
        Type rawType = ptype.getRawType();
        Type[] params = ptype.getActualTypeArguments();
        
        if (!rawType.equals(type)) {
            return false;
        }
        
        if (params.length != typeParams.length) {
            return false;
        }
        
        for(int i = 0; i < params.length; ++i) {
            Type fieldTypeParam = params[i];
            Class<?> expectedTypeParam = typeParams[i];
            
            if (!fieldTypeParam.equals(expectedTypeParam)) {
                return false;
            } else if (fieldTypeParam instanceof Class) {
                if (!((Class<?>)fieldTypeParam).isAssignableFrom(expectedTypeParam)) {
                    return false;
                }
            }
        }
        
        return true;
    }
}
