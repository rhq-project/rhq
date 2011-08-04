package org.rhq.bindings.client;

import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

import javax.jws.WebParam;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.CtNewMethod;
import javassist.NotFoundException;
import javassist.bytecode.ParameterAnnotationsAttribute;
import javassist.bytecode.annotation.Annotation;
import javassist.bytecode.annotation.StringMemberValue;
import javassist.util.proxy.MethodHandler;
import javassist.util.proxy.ProxyFactory;

import org.rhq.bindings.util.ConfigurationClassBuilder;
import org.rhq.bindings.util.ResourceTypeFingerprint;
import org.rhq.core.domain.resource.ResourceCreationDataType;

/**
 *
 * @author Greg Hinkle
 * @author Lukas Krejci
 */
public class ResourceClientFactory {

    private static class CacheRecord {
        public ResourceTypeFingerprint fingerprint;
        public Class<?> iface;
    }
    
    private static final Log LOG = LogFactory.getLog(ResourceClientFactory.class);
    
    //keys are resource type ids
    private static HashMap<Integer, CacheRecord> CUSTOM_CLASS_CACHE = new HashMap<Integer, CacheRecord>();

    private RhqFacade rhqFacade;
    private PrintWriter outputWriter;
    
    public ResourceClientFactory(RhqFacade remoteClient, PrintWriter outputWriter) {
        this.rhqFacade = remoteClient;
        this.outputWriter = outputWriter;
    }

    public RhqFacade getRemoteClient() {
        return rhqFacade;
    }
    
    public PrintWriter getOutputWriter() {
        return outputWriter;
    }
    
    public ResourceClientProxy getResource(int resourceId) {

        ResourceClientProxy proxy = new ResourceClientProxy(this, resourceId);
        Class<?> customInterface = null;
        synchronized (CUSTOM_CLASS_CACHE) {
            Integer resourceTypeId = proxy.getResourceType().getId();
            CacheRecord record = CUSTOM_CLASS_CACHE.get(resourceTypeId);
            
            //if we don't have a cached custom interface for this resource type
            //or if the resource type has changed, generate the custom iface for it.
            if (record == null || !record.fingerprint.equals(proxy.fingerprint)) {
                record = new CacheRecord();
                record.fingerprint = proxy.fingerprint;
                record.iface = defineCustomInterface(proxy);
                CUSTOM_CLASS_CACHE.put(resourceTypeId, record);                               
            }
            
            customInterface = record.iface;
        }
        
        if (customInterface != null) {

            List<Class<?>> interfaces = new ArrayList<Class<?>>();
            interfaces.add(customInterface);
            if (proxy.resourceConfigurationDefinition != null) {
                interfaces.add(getResourceConfigurableInterface());
            }
            if (proxy.pluginConfigurationDefinition != null) {
                interfaces.add(getPluginConfigurableInterface());
            }

            if (proxy.getResourceType().getCreationDataType() == ResourceCreationDataType.CONTENT) {
                interfaces.add(getContentBackedInterface());
            }

            interfaces.addAll(getAdditionalInterfaces(proxy));
            
            ProxyFactory proxyFactory = new ProxyFactory();
            proxyFactory.setInterfaces(interfaces.toArray(new Class[interfaces.size()]));
            proxyFactory.setSuperclass(ResourceClientProxy.class);
            ResourceClientProxy proxied = null;
            try {
                proxied = (ResourceClientProxy) proxyFactory.create(new Class[]{}, new Object[]{},
                        instantiateMethodHandler(proxy, interfaces, rhqFacade));
            } catch (InstantiationException e) {
                LOG.error("Could not instantiate a ResourceClientProxy, this is a bug.", e);
            } catch (IllegalAccessException e) {
                LOG.error("Could not instantiate a ResourceClientProxy, this is a bug.", e);
            } catch (NoSuchMethodException e) {
                LOG.error("Could not instantiate a ResourceClientProxy, this is a bug.", e);
            } catch (InvocationTargetException e) {
                LOG.error("Could not instantiate a ResourceClientProxy, this is a bug.", e);
            }
            return proxied;
        }
        return proxy;

    }
    
    protected Class<?> getResourceConfigurableInterface() {
        return ResourceClientProxy.ResourceConfigurable.class;
    }
    
    protected Class<?> getPluginConfigurableInterface() {
        return ResourceClientProxy.PluginConfigurable.class;
    }
    
    protected Class<?> getContentBackedInterface() {
        return ResourceClientProxy.ContentBackedResource.class;
    }
    
    protected Set<Class<?>> getAdditionalInterfaces(ResourceClientProxy proxy) {
        return Collections.emptySet();
    }
    
    protected MethodHandler instantiateMethodHandler(ResourceClientProxy proxy, List<Class<?>> interfaces, RhqFacade remoteClient) {
        return new ResourceClientProxy.ClientProxyMethodHandler(proxy, remoteClient);
    }
    
    private Class<?> defineCustomInterface(ResourceClientProxy proxy) {
        try {
            // define the dynamic class
            ClassPool pool = ClassPool.getDefault();
            CtClass customClass = pool.makeInterface(ResourceClientProxy.class.getName() + proxy.fingerprint);

            for (String key : proxy.allProperties.keySet()) {
                Object prop = proxy.allProperties.get(key);

                if (prop instanceof ResourceClientProxy.Measurement) {
                    String name = ResourceClientProxy.getterName(key);

                    try {
                        ResourceClientProxy.class.getMethod(name);
                    } catch (NoSuchMethodException nsme) {
                        CtMethod method = CtNewMethod.abstractMethod(pool.get(ResourceClientProxy.Measurement.class.getName()),
                                ResourceClientProxy.getterName(key), new CtClass[0], new CtClass[0], customClass);
                        customClass.addMethod(method);
                    }
                } else if (prop instanceof ResourceClientProxy.Operation) {
                    ResourceClientProxy.Operation o = (ResourceClientProxy.Operation) prop;

                    LinkedHashMap<String, CtClass> types = ConfigurationClassBuilder.translateParameters(o
                            .getDefinition().getParametersConfigurationDefinition());

                    CtClass[] params = new CtClass[types.size()];
                    int x = 0;
                    for (String param : types.keySet()) {
                        params[x++] = types.get(param);
                    }

                    CtMethod method = CtNewMethod.abstractMethod(ConfigurationClassBuilder.translateConfiguration(o
                            .getDefinition().getResultsConfigurationDefinition()), ResourceClientProxy.simpleName(key), params,
                            new javassist.CtClass[0], customClass);

                    // Setup @WebParam annotations so the signatures have the config prop names
                    Annotation[][] newAnnotations = new Annotation[params.length][1];
                    int i = 0;
                    for (String paramName : types.keySet()) {
                        newAnnotations[i] = new Annotation[1];

                        newAnnotations[i][0] = new Annotation(WebParam.class.getName(), method.getMethodInfo()
                                .getConstPool());
                        newAnnotations[i][0].addMemberValue("name", new StringMemberValue(paramName, method
                                .getMethodInfo().getConstPool()));
                        i++;
                    }

                    ParameterAnnotationsAttribute newAnnotationsAttribute = new ParameterAnnotationsAttribute(
                            method.getMethodInfo().getConstPool(), ParameterAnnotationsAttribute.visibleTag);
                    newAnnotationsAttribute.setAnnotations(newAnnotations);
                    method.getMethodInfo().addAttribute(newAnnotationsAttribute);

                    customClass.addMethod(method);
                }
            }

            return customClass.toClass();
        } catch (NotFoundException e) {
            LOG.error("Could not create custom interface for resource with id " + proxy.getId(), e);
        } catch (CannotCompileException e) {
            LOG.error("Could not create custom interface for resource with id " + proxy.getId(), e);
        } catch (Exception e) {
            LOG.error("Could not create custom interface for resource with id " + proxy.getId(), e);
        }
        
        return null;
    }
}
