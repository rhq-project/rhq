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
package org.rhq.enterprise.client.utility;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.CtNewMethod;
import javassist.NotFoundException;
import javassist.CtConstructor;
import javassist.util.proxy.MethodHandler;
import javassist.util.proxy.ProxyFactory;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.criteria.MeasurementDefinitionCriteria;
import org.rhq.core.domain.criteria.ResourceCriteria;
import org.rhq.core.domain.measurement.DataType;
import org.rhq.core.domain.measurement.MeasurementCategory;
import org.rhq.core.domain.measurement.MeasurementData;
import org.rhq.core.domain.measurement.MeasurementDefinition;
import org.rhq.core.domain.measurement.MeasurementUnits;
import org.rhq.core.domain.measurement.util.MeasurementConverter;
import org.rhq.core.domain.operation.OperationDefinition;
import org.rhq.core.domain.operation.ResourceOperationHistory;
import org.rhq.core.domain.operation.JobId;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.domain.util.PageControl;
import org.rhq.enterprise.client.RemoteClient;
import org.rhq.enterprise.server.exception.LoginException;
import org.rhq.enterprise.server.operation.ResourceOperationSchedule;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Implements a local object that exposes resource related data as
 * if it were local.
 *
 * @author Greg Hinkle
 */
public class ResourceClientProxy {


    private RemoteClient remoteClient;
    private int resourceId;
    private Resource resource;

    Map<String, Object> allProperties = new HashMap<String, Object>();

    // Metadata
    private List<MeasurementDefinition> measurementDefinitions;
    private Map<String, Measurement> measurementMap = new HashMap<String, Measurement>();


    private List<OperationDefinition> operationDefinitions;
    private Map<String, Operation> operationMap = new HashMap<String, Operation>();


    private List<ResourceClientProxy> children;

    public ResourceClientProxy() {
    }


    public ResourceClientProxy(ResourceClientProxy parentProxy) {
        this.remoteClient = parentProxy.remoteClient;
        this.resourceId = parentProxy.resourceId;
        this.resource = parentProxy.resource;
        this.allProperties = parentProxy.allProperties;
        this.measurementDefinitions = parentProxy.measurementDefinitions;
        this.measurementMap = parentProxy.measurementMap;
        this.children = parentProxy.children;;

    }

    public ResourceClientProxy(RemoteClient remoteClient, int resourceId) {
        this.remoteClient = remoteClient;
        this.resourceId = resourceId;

        init();
    }


    public String getName() {
        return resource.getName();
    }

    public String getDescription() {
        return resource.getDescription();
    }

    public String getVersion() {
        return resource.getVersion();
    }

    public Date getCreatedDate() {
        return new Date(resource.getCtime());
    }

    public Date getModifiedDate() {
        return new Date(resource.getCtime());
    }


    public Measurement getMeasurement(String name) {
        return this.measurementMap.get(name);
    }

    public Collection<Measurement> getMeasurements() {
        return this.measurementMap.values();
    }

    public Collection<Operation> getOperations() {
        return this.operationMap.values();
    }


    public List<ResourceClientProxy> getChildren() {
        if (children == null) {
            children = new ArrayList<ResourceClientProxy>();

            initChildren();

        }
        return children;
    }

    public ResourceClientProxy getChild(String name) {
        for (ResourceClientProxy child : getChildren()) {
            if (name.equals(child.getName()))
                return child;
        }
        return null;
    }


    public String toString() {
        return resource.getName() + "(" + resource.getResourceType().getName() + "::" + resource.getResourceType().getPlugin() + ")";
    }



    

    public void init() {

        this.resource = remoteClient.getResourceManagerRemote().getResource(remoteClient.getSubject(), resourceId);

        // Lazy init children, not here
        initMeasurments();
        initOperations();
    }


    public void initChildren() {
        ResourceCriteria criteria = new ResourceCriteria();
        criteria.addFilterParentResourceId(resourceId);
        PageList<Resource> childResources =
                remoteClient.getResourceManagerRemote().findResourcesByCriteria(
                        remoteClient.getSubject(),
                        criteria);

        for (Resource child : childResources) {
            this.children.add(new Factory(remoteClient).getResourceProxy(child.getId()));
        }
    }


    public void initMeasurments() {
        MeasurementDefinitionCriteria criteria = new MeasurementDefinitionCriteria();
        criteria.addFilterResourceTypeName(resource.getResourceType().getName());

        this.measurementDefinitions =
                remoteClient.getMeasurementDefinitionManagerRemote().findMeasurementDefinitionsByCriteria(
                        remoteClient.getSubject(),
                        criteria);

        this.measurementMap = new HashMap<String, Measurement>();
        for (MeasurementDefinition def : measurementDefinitions) {
            Measurement m = new Measurement(def);

            String name = def.getDisplayName().replaceAll("\\W", "");
            name = decapitalize(name);

            this.measurementMap.put(name, m);
            this.allProperties.put(name, m);
        }
    }


    public void initOperations() {

        this.operationDefinitions =
                remoteClient.getOperationManagerRemote().
                findSupportedResourceOperations(remoteClient.getSubject(), resourceId, true);

        for (OperationDefinition def : operationDefinitions) {
            Operation o = new Operation(def);
            this.operationMap.put(o.getName(), o);
            this.allProperties.put(o.getName(), o);
        }
    }





    public class Measurement {

        MeasurementDefinition definition;

        public Measurement(MeasurementDefinition definition) {
            this.definition = definition;
        }

        public String getName() {
            return definition.getDisplayName();
        }

        public String getDescription() {
            return definition.getDescription();
        }

        public DataType getDataType() {
            return definition.getDataType();
        }

        public MeasurementCategory getCategory() {
            return definition.getCategory();
        }

        public MeasurementUnits getUnits() {
            return definition.getUnits();
        }

        public Object getValue() {
            try {
                Set<MeasurementData> d =
                        remoteClient.getMeasurementDataManagerRemote().findLiveData(remoteClient.getSubject(), resourceId, new int[]{definition.getId()});
                MeasurementData data = d.iterator().next();
                return data.getValue();
            } catch (Exception e) {
                return "?";
            }
        }

        public String getDisplayValue() {
            Object val = getValue();
            if (val instanceof Number) {
                return MeasurementConverter.format(((Number) val).doubleValue(), getUnits(), true);
            } else {
                return String.valueOf(val);
            }

        }

        public String toString() {
            return getDisplayValue();
        }
    }


    public class Operation {
        OperationDefinition definition;

        public Operation(OperationDefinition definition) {
            this.definition = definition;
        }
        public OperationDefinition getDefinition() {
            return definition;
        }
        public String getName() {
            return simpleName(this.definition.getDisplayName());
        }
        public String getDescription() {
            return this.definition.getDescription();
        }


        public Object invoke(Object[] args) throws Exception {

            Configuration parameters = ConfigurationClassBuilder.translateParametersToConfig(definition.getParametersConfigurationDefinition(), args);

            ResourceOperationSchedule schedule =
                remoteClient.getOperationManagerRemote().scheduleResourceOperation(
                    remoteClient.getSubject(),
                    resourceId,
                    definition.getName(),
                    0,0,0,
                    30000,
                    parameters, "Executed from commandline");

            JobId jid = schedule.getJobId();

            ResourceOperationHistory history = null;
            long start = System.currentTimeMillis();
            while (history == null && !((System.currentTimeMillis() - start) > 10000)) {
                PageList<ResourceOperationHistory> histories =
                remoteClient.getOperationManagerRemote().findCompletedResourceOperationHistories(
                        remoteClient.getSubject(),
                        resourceId, System.currentTimeMillis() - 100000, System.currentTimeMillis() + 10000, PageControl.getUnlimitedInstance());
                for (ResourceOperationHistory h : histories) {
                    if (h.getJobName().equals(jid.getJobName()) && h.getJobGroup().equals(jid.getJobGroup())) {

                        history =
                                (ResourceOperationHistory)
                                        remoteClient.getOperationManagerRemote().getOperationHistoryByHistoryId(
                                    remoteClient.getSubject(), h.getId());
                        break;
                    }
                }
                Thread.sleep(1000);
            }

            Configuration result = (history != null?  history.getResults() : null);

            Object returnResults = ConfigurationClassBuilder.translateResults(definition.getResultsConfigurationDefinition(), result);

            return returnResults;
        }
    }


    public static class ClientProxyMethodHandler implements MethodHandler {

        ResourceClientProxy resourceClientProxy;

        public ClientProxyMethodHandler(ResourceClientProxy resourceClientProxy) {
            this.resourceClientProxy = resourceClientProxy;
        }

        public Object invoke(Object proxy, Method method, Method proceedMethod, Object[] args) throws Throwable {

            if (proceedMethod != null) {
                Method realMethod = ResourceClientProxy.class.getMethod(method.getName(), method.getParameterTypes());
                return realMethod.invoke(resourceClientProxy, args);
            } else {
                String name = method.getName();
                Object key = resourceClientProxy.allProperties.get(name);
                if (key == null) {
                    name = decapitalize(method.getName().substring(3, method.getName().length()));
                    key = resourceClientProxy.allProperties.get(name);
                }

                if (key != null) {
                    if (key instanceof Measurement) {
                        return String.valueOf(key);
                    } else if (key instanceof Operation) {
                        System.out.println("Need to invoke op" + key);

                        return ((Operation)key).invoke(args);

                    }
                }

                throw new RuntimeException("Can't find custom method: " + method);
            }
        }
    }


    public static class Factory {
        private RemoteClient remoteClient;

        public Factory(RemoteClient remoteClient) {
            this.remoteClient = remoteClient;
        }

        private static AtomicInteger classIndex = new AtomicInteger();

        public ResourceClientProxy getResourceProxy(int resourceId) {

            ResourceClientProxy proxy = new ResourceClientProxy(remoteClient, resourceId);
            Class customInterface = null;
            try {
                // define the dynamic class
                ClassPool pool = ClassPool.getDefault();
                CtClass customClass = pool.makeInterface(ResourceClientProxy.class.getName() + "__Custom__" + classIndex.getAndIncrement());

//                CtConstructor constructor = new CtConstructor(new CtClass[] { pool.get(ResourceClientProxy.class.getName()) }, customClass);
//                customClass.addConstructor(constructor);

                for (String key : proxy.allProperties.keySet()) {
                    Object prop = proxy.allProperties.get(key);

                    if (prop instanceof Measurement) {

                        
                        Measurement m = (Measurement) prop;
                        String name = getterName(key);

                        try {
                            ResourceClientProxy.class.getMethod(name);
                        } catch (NoSuchMethodException nsme) {
                            CtMethod method =
                                    CtNewMethod.abstractMethod(
                                            pool.get(String.class.getName()),
                                            getterName(key),
                                            new CtClass[0],
                                            new CtClass[0],
                                            customClass);
                            customClass.addMethod(method);
                        }
                    } else if (prop instanceof Operation) {
                        Operation o = (Operation) prop;

                        LinkedHashMap<String,CtClass> types =
                                ConfigurationClassBuilder.translateParameters(
                                        o.getDefinition().getParametersConfigurationDefinition());

                        CtClass[] params = new CtClass[types.size()];
                        int x = 0;
                        for (String param : types.keySet()) {
                            params[x] = types.get(param);
                        }


                        CtMethod method =
                                CtNewMethod.abstractMethod(
                                    ConfigurationClassBuilder.translateConfiguration(
                                        o.getDefinition().getResultsConfigurationDefinition()),
                                        simpleName(key),
                                        params,
                                        new CtClass[0],
                                        customClass);
                        customClass.addMethod(method);
                    }
                }

                customInterface = customClass.toClass();
            } catch (NotFoundException e) {
                e.printStackTrace();
            } catch (CannotCompileException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }

            if (customInterface != null) {
                ProxyFactory proxyFactory = new ProxyFactory();
                proxyFactory.setInterfaces(new Class[] { customInterface });
                proxyFactory.setSuperclass(ResourceClientProxy.class);
                ResourceClientProxy proxied = null;
                try {
                    proxied =
                            (ResourceClientProxy) 
                            proxyFactory.create(new Class[] {  }, new Object[] {  }, new ClientProxyMethodHandler(proxy));
                } catch (InstantiationException e) {
                    e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                } catch (IllegalAccessException e) {
                    e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                } catch (NoSuchMethodException e) {
                    e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                } catch (InvocationTargetException e) {
                    e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                }
                return proxied;
            }
            return proxy;

        }
    }






    private static String simpleName(String name) {
        return decapitalize(name.replaceAll("\\W",""));
    }


    private static String decapitalize(String name) {
        return Character.toLowerCase(name.charAt(0)) + name.substring(1, name.length());
    }


    private static String getterName(String name) {
        return "get" + Character.toUpperCase(name.charAt(0)) + name.substring(1, name.length());
    }

    public static void main(String[] args) throws LoginException {
        RemoteClient rc = new RemoteClient("localhost", 7080);


        rc.login("rhqadmin", "rhqadmin");

        Factory factory = new Factory(rc);

        ResourceClientProxy resource = factory.getResourceProxy(10571);


        for (Measurement m : resource.getMeasurements()) {
            System.out.println(m.toString());
        }
    }
}
