package org.rhq.enterprise.client.proxy;

import javassist.ClassPool;
import javassist.CtNewMethod;
import javassist.bytecode.ParameterAnnotationsAttribute;
import org.rhq.enterprise.client.ClientMain;
import org.rhq.enterprise.client.utility.ConfigurationClassBuilder;
import org.rhq.core.domain.resource.ResourceCreationDataType;

/**
 *
 * @author Greg Hinkle
 */
public class ResourceClientFactory {

    private ClientMain clientMain;

    private org.rhq.enterprise.client.RemoteClient remoteClient;

    public ResourceClientFactory(ClientMain clientMain) {
        this.clientMain = clientMain;
        this.remoteClient = clientMain.getRemoteClient();
    }

    private static java.util.concurrent.atomic.AtomicInteger classIndex = new java.util.concurrent.atomic.AtomicInteger();

    public org.rhq.enterprise.client.proxy.ResourceClientProxy getResource(int resourceId) {

        org.rhq.enterprise.client.proxy.ResourceClientProxy proxy = new org.rhq.enterprise.client.proxy.ResourceClientProxy(clientMain, resourceId);
        java.lang.Class customInterface = null;
        try {
            // define the dynamic class
            javassist.ClassPool pool = ClassPool.getDefault();
            javassist.CtClass customClass = pool.makeInterface(org.rhq.enterprise.client.proxy.ResourceClientProxy.class.getName() + "__Custom__"
                    + classIndex.getAndIncrement());

            for (java.lang.String key : proxy.allProperties.keySet()) {
                java.lang.Object prop = proxy.allProperties.get(key);

                if (prop instanceof org.rhq.enterprise.client.proxy.ResourceClientProxy.Measurement) {
                    org.rhq.enterprise.client.proxy.ResourceClientProxy.Measurement m = (org.rhq.enterprise.client.proxy.ResourceClientProxy.Measurement) prop;
                    java.lang.String name = org.rhq.enterprise.client.proxy.ResourceClientProxy.getterName(key);

                    try {
                        org.rhq.enterprise.client.proxy.ResourceClientProxy.class.getMethod(name);
                    } catch (java.lang.NoSuchMethodException nsme) {
                        javassist.CtMethod method = CtNewMethod.abstractMethod(pool.get(org.rhq.enterprise.client.proxy.ResourceClientProxy.Measurement.class.getName()),
                                org.rhq.enterprise.client.proxy.ResourceClientProxy.getterName(key), new javassist.CtClass[0], new javassist.CtClass[0], customClass);
                        customClass.addMethod(method);
                    }
                } else if (prop instanceof org.rhq.enterprise.client.proxy.ResourceClientProxy.Operation) {
                    org.rhq.enterprise.client.proxy.ResourceClientProxy.Operation o = (org.rhq.enterprise.client.proxy.ResourceClientProxy.Operation) prop;

                    java.util.LinkedHashMap<java.lang.String, javassist.CtClass> types = ConfigurationClassBuilder.translateParameters(o
                            .getDefinition().getParametersConfigurationDefinition());

                    javassist.CtClass[] params = new javassist.CtClass[types.size()];
                    int x = 0;
                    for (java.lang.String param : types.keySet()) {
                        params[x++] = types.get(param);
                    }

                    javassist.CtMethod method = CtNewMethod.abstractMethod(ConfigurationClassBuilder.translateConfiguration(o
                            .getDefinition().getResultsConfigurationDefinition()), org.rhq.enterprise.client.proxy.ResourceClientProxy.simpleName(key), params,
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

            java.util.List<java.lang.Class> interfaces = new java.util.ArrayList<java.lang.Class>();
            interfaces.add(customInterface);
            if (proxy.resourceConfigurationDefinition != null) {
                interfaces.add(org.rhq.enterprise.client.proxy.ResourceClientProxy.ResourceConfigurable.class);
            }
            if (proxy.pluginConfigurationDefinition != null) {
                interfaces.add(org.rhq.enterprise.client.proxy.ResourceClientProxy.PluginConfigurable.class);
            }

            if (proxy.getResourceType().getCreationDataType() == ResourceCreationDataType.CONTENT) {
                interfaces.add(org.rhq.enterprise.client.proxy.ResourceClientProxy.ContentBackedResource.class);
            }

            javassist.util.proxy.ProxyFactory proxyFactory = new javassist.util.proxy.ProxyFactory();
            proxyFactory.setInterfaces(interfaces.toArray(new java.lang.Class[interfaces.size()]));
            proxyFactory.setSuperclass(org.rhq.enterprise.client.proxy.ResourceClientProxy.class);
            org.rhq.enterprise.client.proxy.ResourceClientProxy proxied = null;
            try {
                proxied = (org.rhq.enterprise.client.proxy.ResourceClientProxy) proxyFactory.create(new java.lang.Class[]{}, new java.lang.Object[]{},
                        new org.rhq.enterprise.client.proxy.ResourceClientProxy.ClientProxyMethodHandler(proxy, remoteClient));
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
}
