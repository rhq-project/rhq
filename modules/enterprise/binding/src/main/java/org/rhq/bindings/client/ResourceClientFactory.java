package org.rhq.bindings.client;

import java.io.PrintWriter;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import javassist.ClassPool;
import javassist.CtNewMethod;
import javassist.bytecode.ParameterAnnotationsAttribute;
import javassist.util.proxy.MethodHandler;

import org.rhq.bindings.util.ConfigurationClassBuilder;
import org.rhq.core.domain.resource.ResourceCreationDataType;

/**
 *
 * @author Greg Hinkle
 * @author Lukas Krejci
 */
public class ResourceClientFactory {

    private org.rhq.bindings.client.RhqFacade rhqFacade;
    private PrintWriter outputWriter;
    
    public ResourceClientFactory(org.rhq.bindings.client.RhqFacade remoteClient, PrintWriter outputWriter) {
        this.rhqFacade = remoteClient;
        this.outputWriter = outputWriter;
    }

    private static java.util.concurrent.atomic.AtomicInteger classIndex = new java.util.concurrent.atomic.AtomicInteger();

    public org.rhq.bindings.client.RhqFacade getRemoteClient() {
        return rhqFacade;
    }
    
    public PrintWriter getOutputWriter() {
        return outputWriter;
    }
    
    public org.rhq.bindings.client.ResourceClientProxy getResource(int resourceId) {

        org.rhq.bindings.client.ResourceClientProxy proxy = new org.rhq.bindings.client.ResourceClientProxy(this, resourceId);
        java.lang.Class<?> customInterface = null;
        try {
            // define the dynamic class
            javassist.ClassPool pool = ClassPool.getDefault();
            javassist.CtClass customClass = pool.makeInterface(org.rhq.bindings.client.ResourceClientProxy.class.getName() + "__Custom__"
                    + classIndex.getAndIncrement());

            for (java.lang.String key : proxy.allProperties.keySet()) {
                java.lang.Object prop = proxy.allProperties.get(key);

                if (prop instanceof org.rhq.bindings.client.ResourceClientProxy.Measurement) {
                    org.rhq.bindings.client.ResourceClientProxy.Measurement m = (org.rhq.bindings.client.ResourceClientProxy.Measurement) prop;
                    java.lang.String name = org.rhq.bindings.client.ResourceClientProxy.getterName(key);

                    try {
                        org.rhq.bindings.client.ResourceClientProxy.class.getMethod(name);
                    } catch (java.lang.NoSuchMethodException nsme) {
                        javassist.CtMethod method = CtNewMethod.abstractMethod(pool.get(org.rhq.bindings.client.ResourceClientProxy.Measurement.class.getName()),
                                org.rhq.bindings.client.ResourceClientProxy.getterName(key), new javassist.CtClass[0], new javassist.CtClass[0], customClass);
                        customClass.addMethod(method);
                    }
                } else if (prop instanceof org.rhq.bindings.client.ResourceClientProxy.Operation) {
                    org.rhq.bindings.client.ResourceClientProxy.Operation o = (org.rhq.bindings.client.ResourceClientProxy.Operation) prop;

                    java.util.LinkedHashMap<java.lang.String, javassist.CtClass> types = ConfigurationClassBuilder.translateParameters(o
                            .getDefinition().getParametersConfigurationDefinition());

                    javassist.CtClass[] params = new javassist.CtClass[types.size()];
                    int x = 0;
                    for (java.lang.String param : types.keySet()) {
                        params[x++] = types.get(param);
                    }

                    javassist.CtMethod method = CtNewMethod.abstractMethod(ConfigurationClassBuilder.translateConfiguration(o
                            .getDefinition().getResultsConfigurationDefinition()), org.rhq.bindings.client.ResourceClientProxy.simpleName(key), params,
                            new javassist.CtClass[0], customClass);

                    // Setup @WebParam annotations so the signatures have the config prop names
                    javassist.bytecode.annotation.Annotation[][] newAnnotations = new javassist.bytecode.annotation.Annotation[params.length][1];
                    int i = 0;
                    for (java.lang.String paramName : types.keySet()) {
                        newAnnotations[i] = new javassist.bytecode.annotation.Annotation[1];

                        newAnnotations[i][0] = new javassist.bytecode.annotation.Annotation(javax.jws.WebParam.class.getName(), method.getMethodInfo()
                                .getConstPool());
                        newAnnotations[i][0].addMemberValue("name", new javassist.bytecode.annotation.StringMemberValue(paramName, method
                                .getMethodInfo().getConstPool()));
                        i++;
                    }

                    javassist.bytecode.ParameterAnnotationsAttribute newAnnotationsAttribute = new javassist.bytecode.ParameterAnnotationsAttribute(
                            method.getMethodInfo().getConstPool(), ParameterAnnotationsAttribute.visibleTag);
                    newAnnotationsAttribute.setAnnotations(newAnnotations);
                    method.getMethodInfo().addAttribute(newAnnotationsAttribute);

                    customClass.addMethod(method);
                }
            }

            customInterface = customClass.toClass();
        } catch (javassist.NotFoundException e) {
            e.printStackTrace();
        } catch (javassist.CannotCompileException e) {
            e.printStackTrace();
        } catch (java.lang.Exception e) {
            e.printStackTrace();
        }

        if (customInterface != null) {

            java.util.List<java.lang.Class<?>> interfaces = new java.util.ArrayList<java.lang.Class<?>>();
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
            
            javassist.util.proxy.ProxyFactory proxyFactory = new javassist.util.proxy.ProxyFactory();
            proxyFactory.setInterfaces(interfaces.toArray(new java.lang.Class[interfaces.size()]));
            proxyFactory.setSuperclass(org.rhq.bindings.client.ResourceClientProxy.class);
            org.rhq.bindings.client.ResourceClientProxy proxied = null;
            try {
                proxied = (org.rhq.bindings.client.ResourceClientProxy) proxyFactory.create(new java.lang.Class[]{}, new java.lang.Object[]{},
                        instantiateMethodHandler(proxy, interfaces, rhqFacade));
            } catch (java.lang.InstantiationException e) {
                e.printStackTrace(); //To change body of catch statement use File | Settings | File Templates.
            } catch (java.lang.IllegalAccessException e) {
                e.printStackTrace(); //To change body of catch statement use File | Settings | File Templates.
            } catch (java.lang.NoSuchMethodException e) {
                e.printStackTrace(); //To change body of catch statement use File | Settings | File Templates.
            } catch (java.lang.reflect.InvocationTargetException e) {
                e.printStackTrace(); //To change body of catch statement use File | Settings | File Templates.
            }
            return proxied;
        }
        return proxy;

    }
    
    protected Class<?> getResourceConfigurableInterface() {
        return org.rhq.bindings.client.ResourceClientProxy.ResourceConfigurable.class;
    }
    
    protected Class<?> getPluginConfigurableInterface() {
        return org.rhq.bindings.client.ResourceClientProxy.PluginConfigurable.class;
    }
    
    protected Class<?> getContentBackedInterface() {
        return org.rhq.bindings.client.ResourceClientProxy.ContentBackedResource.class;
    }
    
    protected Set<Class<?>> getAdditionalInterfaces(ResourceClientProxy proxy) {
        return Collections.emptySet();
    }
    
    protected MethodHandler instantiateMethodHandler(ResourceClientProxy proxy, List<Class<?>> interfaces, org.rhq.bindings.client.RhqFacade remoteClient) {
        return new org.rhq.bindings.client.ResourceClientProxy.ClientProxyMethodHandler(proxy, remoteClient);
    }
}
