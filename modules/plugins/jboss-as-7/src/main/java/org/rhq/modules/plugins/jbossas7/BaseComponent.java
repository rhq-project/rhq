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
package org.rhq.modules.plugins.jbossas7;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.JsonNode;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.Property;
import org.rhq.core.domain.configuration.PropertyList;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionSimple;
import org.rhq.core.domain.configuration.definition.PropertySimpleType;
import org.rhq.core.domain.content.transfer.ResourcePackageDetails;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.measurement.DataType;
import org.rhq.core.domain.measurement.MeasurementDataNumeric;
import org.rhq.core.domain.measurement.MeasurementDataTrait;
import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.measurement.MeasurementScheduleRequest;
import org.rhq.core.domain.resource.CreateResourceStatus;
import org.rhq.core.pluginapi.configuration.ConfigurationFacet;
import org.rhq.core.pluginapi.configuration.ConfigurationUpdateReport;
import org.rhq.core.pluginapi.content.ContentContext;
import org.rhq.core.pluginapi.content.ContentServices;
import org.rhq.core.pluginapi.inventory.CreateChildResourceFacet;
import org.rhq.core.pluginapi.inventory.CreateResourceReport;
import org.rhq.core.pluginapi.inventory.DeleteResourceFacet;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.core.pluginapi.measurement.MeasurementFacet;
import org.rhq.core.pluginapi.operation.OperationFacet;
import org.rhq.core.pluginapi.operation.OperationResult;
import org.rhq.modules.plugins.jbossas7.json.Address;
import org.rhq.modules.plugins.jbossas7.json.ComplexResult;
import org.rhq.modules.plugins.jbossas7.json.CompositeOperation;
import org.rhq.modules.plugins.jbossas7.json.Operation;
import org.rhq.modules.plugins.jbossas7.json.PROPERTY_VALUE;
import org.rhq.modules.plugins.jbossas7.json.ReadAttribute;
import org.rhq.modules.plugins.jbossas7.json.ReadChildrenNames;
import org.rhq.modules.plugins.jbossas7.json.ReadResource;
import org.rhq.modules.plugins.jbossas7.json.Result;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class BaseComponent implements ResourceComponent, MeasurementFacet, ConfigurationFacet, DeleteResourceFacet,
        CreateChildResourceFacet, OperationFacet
{
    private static final String INTERNAL = "_internal:";
    private static final int INTERNAL_SIZE = INTERNAL.length();
    private static final String LOCALHOST = "localhost";
    private static final String DEFAULT_HTTP_MANAGEMENT_PORT = "9990";
    final Log log = LogFactory.getLog(this.getClass());

    ResourceContext context;
    Configuration pluginConfiguration;
    String myServerName;
    ASConnection connection;
    String path;
    String key;
    String host;
    int port;
    private boolean verbose = ASConnection.verbose;

    /**
     * Return availability of this resource
     *  @see org.rhq.core.pluginapi.inventory.ResourceComponent#getAvailability()
     */
    public AvailabilityType getAvailability() {

        ReadResource op = new ReadResource(pathToAddress(path));
        Result res = connection.execute(op);

        return res.isSuccess()? AvailabilityType.UP: AvailabilityType.DOWN;
    }


    /**
     * Start the resource connection
     * @see org.rhq.core.pluginapi.inventory.ResourceComponent#start(org.rhq.core.pluginapi.inventory.ResourceContext)
     */
    public void start(ResourceContext context) throws InvalidPluginConfigurationException, Exception {
        this.context = context;
        pluginConfiguration = context.getPluginConfiguration();

        if (!(context.getParentResourceComponent() instanceof BaseComponent)) {
            host = pluginConfiguration.getSimpleValue("hostname", LOCALHOST);
            String portString = pluginConfiguration.getSimpleValue("port", DEFAULT_HTTP_MANAGEMENT_PORT);
            port = Integer.parseInt(portString);
            connection = new ASConnection(host,port);
        }
        else {
            connection = ((BaseComponent)context.getParentResourceComponent()).getASConnection();
        }

        path = pluginConfiguration.getSimpleValue("path", null);
        key = context.getResourceKey();

        myServerName = context.getResourceKey().substring(context.getResourceKey().lastIndexOf("/")+1);


    }


    /**
     * Tear down the resource connection
     * @see org.rhq.core.pluginapi.inventory.ResourceComponent#stop()
     */
    public void stop() {


    }



    /**
     * Gather measurement data
     * @see org.rhq.core.pluginapi.measurement.MeasurementFacet#getValues(org.rhq.core.domain.measurement.MeasurementReport, java.util.Set)
     */
    public  void getValues(MeasurementReport report, Set<MeasurementScheduleRequest> metrics) throws Exception {


        for (MeasurementScheduleRequest req : metrics) {


            if (req.getName().startsWith(INTERNAL))
                processPluginStats(req,report);
            else {
                // Metrics from the application server

                Operation op = new ReadAttribute(pathToAddress(path),req.getName()); // TODO batching
                Result res = connection.execute(op, false);
                if (!res.isSuccess())
                    continue;

                 String val = (String) res.getResult();

                if (req.getDataType()== DataType.MEASUREMENT) {
                    if (!val.equals("no metrics available")) { // AS 7 returns this
                        try {
                            Double d = Double.parseDouble(val);
                            MeasurementDataNumeric data = new MeasurementDataNumeric(req,d);
                            report.addData(data);
                        } catch (NumberFormatException e) {
                            log.warn("Non numeric input for [" + req.getName() + "] : [" + val + "]");
                        }
                    }
                } else if (req.getDataType()== DataType.TRAIT) {
                    MeasurementDataTrait data = new MeasurementDataTrait(req,val);
                    report.addData(data);
                }
            }
        }
    }

    /**
     * Return internal statistics data
     * @param req Schedule for the requested data
     * @param report report to add th data to.
     */
    private void processPluginStats(MeasurementScheduleRequest req, MeasurementReport report) {

        String name = req.getName();
        if (!name.startsWith(INTERNAL))
            return;

        name = name.substring(INTERNAL_SIZE);

        PluginStats stats = PluginStats.getInstance();
        MeasurementDataNumeric data;
        Double val;
        if (name.equals("mgmtRequests")) {
             val= (double) stats.getRequestCount();
        }
        else if (name.equals("requestTime")) {
            val =(double) stats.getRequestTime();
        }
        else if (name.equals("maxTime")) {
            val = (double) stats.getMaxTime();
        }
        else
            val = Double.NaN;

        data = new MeasurementDataNumeric(req,val);
        report.addData(data);
    }

    protected ASConnection getASConnection() {
        return connection;
    }


    protected String getPath() { return path; }

    public Configuration loadResourceConfiguration() throws Exception {

        List<PROPERTY_VALUE> address = pathToAddress(path);
        ConfigurationDefinition configDef = context.getResourceType().getResourceConfigurationDefinition();
        ConfigurationLoadDelegate delegate = new ConfigurationLoadDelegate(configDef,connection,new Address(address));
        return delegate.loadResourceConfiguration();
    }


    public void updateResourceConfiguration(ConfigurationUpdateReport report) {

        List<PROPERTY_VALUE> address = pathToAddress(path);
        ConfigurationDefinition configDef = context.getResourceType().getResourceConfigurationDefinition();
        ConfigurationWriteDelegate delegate = new ConfigurationWriteDelegate(configDef,connection,new Address(address));
        delegate.updateResourceConfiguration(report);
    }

    /**
     * Convert a path in the form key=value,key=value... to a List of properties.
     * @param path Path to translate
     * @return List of properties
     */
    public List<PROPERTY_VALUE> pathToAddress(String path) {
        if (path==null || path.isEmpty())
            return Collections.emptyList();

        List<PROPERTY_VALUE> result = new ArrayList<PROPERTY_VALUE>();
        String[] components = path.split(",");
        for (String component : components) {
            String tmp = component.trim();

            if (tmp.contains("=")) {
                // strip / from the start of the key if it happens to be there
                if (tmp.startsWith("/"))
                    tmp = tmp.substring(1);

                String[] pair = tmp.split("=");
                PROPERTY_VALUE valuePair = new PROPERTY_VALUE(pair[0], pair[1]);
                result.add(valuePair);
            }
        }

        return result;
    }

    public String addressToPath(List<PROPERTY_VALUE> address)
    {
        StringBuilder builder = new StringBuilder();
        Iterator<PROPERTY_VALUE> iter = address.iterator();
        while (iter.hasNext()) {
            PROPERTY_VALUE val = iter.next();
            builder.append(val.getKey()).append('=').append(val.getValue());
            if (iter.hasNext())
                builder.append(',');
        }
        return builder.toString();
    }

    @Override
    public void deleteResource() throws Exception {

        log.info("delete resource: " + path + " ...");
        List<PROPERTY_VALUE> address = pathToAddress(path);
        Operation op = new Operation("remove", address);
        ComplexResult res = connection.executeComplex(op);
        if (!res.isSuccess())
            throw new IllegalArgumentException("Delete for [" + path + "] failed: " + res.getFailureDescription());
        if (path.contains("server-group")) {
            // This was a server group level deployment - TODO do we also need to remove the entry in /deployments ?
/*

            for (PROPERTY_VALUE val : address) {
                if (val.getKey().equals("deployment")) {
                    ComplexResult res2 = connection.executeComplex(new Operation("remove",val.getKey(),val.getValue()));
                    if (!res2.isSuccess())
                        throw new IllegalArgumentException("Removal of [" + path + "] falied : " + res2.getFailureDescription());
                }
            }
*/
        }
        log.info("   ... done");

    }


    @Override
    public CreateResourceReport createResource(CreateResourceReport report) {


        ResourcePackageDetails details = report.getPackageDetails();

        ContentContext cctx = context.getContentContext();
        ContentServices contentServices = cctx.getContentServices();
        String resourceTypeName = report.getResourceType().getName();

        ASUploadConnection uploadConnection = new ASUploadConnection(host,port);
        OutputStream out = uploadConnection.getOutputStream(details.getFileName());
        contentServices.downloadPackageBitsForChildResource(cctx, resourceTypeName, details.getKey(), out);

        JsonNode uploadResult = uploadConnection.finishUpload();
        if (verbose)
            log.info(uploadResult);

        if (ASUploadConnection.isErrorReply(uploadResult)) {
            report.setStatus(CreateResourceStatus.FAILURE);
            report.setErrorMessage(ASUploadConnection.getFailureDescription(uploadResult));

            return report;
        }

        String fileName = details.getFileName();

        if (fileName.startsWith("C:\\fakepath\\")) {   // TODO this is a hack as the server adds the fake path somehow
            fileName=fileName.substring("C:\\fakepath\\".length());
        }


        String tmpName = fileName; // TODO figure out the tmp-name biz with the AS guys

        JsonNode resultNode = uploadResult.get("result");
        String hash = resultNode.get("BYTES_VALUE").getTextValue();

        return runDeploymentMagicOnServer(report, fileName, tmpName, hash);

    }

    /**
     * Do the actual fumbling with the domain api to deploy the uploaded content
     * @param report CreateResourceReport to report the result
     * @param runtimeName File name to use as runtime name
     * @param deploymentName Name of the deployment
     * @param hash Hash of the content bytes
     * @return the passed report with success or failure settings
     */
    public CreateResourceReport runDeploymentMagicOnServer(CreateResourceReport report, String runtimeName,
                                                           String deploymentName, String hash) {

        boolean toServerGroup = context.getResourceKey().contains("server-group=");
        log.info("Deploying [" + runtimeName + "] to domain only= " + !toServerGroup + " ...");


        ASConnection connection = getASConnection();

        Operation step1 = new Operation("add","deployment",deploymentName);
//        step1.addAdditionalProperty("hash", new PROPERTY_VALUE("BYTES_VALUE", hash));
        List<Object> content = new ArrayList<Object>(1);
        Map<String,Object> contentValues = new HashMap<String,Object>();
        contentValues.put("hash",new PROPERTY_VALUE("BYTES_VALUE",hash));
        content.add(contentValues);
        step1.addAdditionalProperty("content",content);

        step1.addAdditionalProperty("name", deploymentName);
        step1.addAdditionalProperty("runtime-name", runtimeName);

        String resourceKey;
        Result result ;

        CompositeOperation cop = new CompositeOperation();
        cop.addStep(step1);
        /*
         * We need to check here if this is an upload to /deployment only
         * or if this should be deployed to a server group too
         */

        if (!toServerGroup) {

            // if standalone, then :deploy the deployment anyway
            if (context.getResourceType().getName().contains("Standalone")) {
                Operation step2 = new Operation("deploy",step1.getAddress());
                cop.addStep(step2);
            }

            result = connection.execute(cop);
            resourceKey = addressToPath(step1.getAddress());

        }
        else {

            List<PROPERTY_VALUE> serverGroupAddress = new ArrayList<PROPERTY_VALUE>();
            serverGroupAddress.addAll(pathToAddress(context.getResourceKey()));
            serverGroupAddress.add(new PROPERTY_VALUE("deployment", deploymentName));
            Operation step2 = new Operation("add",serverGroupAddress);

            cop.addStep(step2);

            Operation step3 = new Operation("deploy",serverGroupAddress);
            cop.addStep(step3);

            resourceKey = addressToPath(serverGroupAddress);

            if (verbose)
                log.info("Deploy operation: " + cop);

            result = connection.execute(cop);
        }

        if ((!result.isSuccess())) {
            String failureDescription = result.getFailureDescription();
            report.setErrorMessage(failureDescription);
            report.setStatus(CreateResourceStatus.FAILURE);
            log.warn(" ... done with failure: " + failureDescription);
        }
        else {
            report.setStatus(CreateResourceStatus.SUCCESS);
            report.setResourceName(runtimeName);
            report.setResourceKey(resourceKey);
            log.info(" ... with success and key [" + resourceKey + "]" );
        }

        return report;
    }

    @Override
    public OperationResult invokeOperation(String name,
                                           Configuration parameters) throws InterruptedException, Exception {

        if (!name.contains(":")) {
            OperationResult badName = new OperationResult("Operation name did not contain a ':'");
            badName.setErrorMessage("Operation name did not contain a ':'");
            return badName;
        }

        int colonPos = name.indexOf(':');
        String what = name.substring(0, colonPos);
        String op = name.substring(colonPos+1);
        Operation operation=null;

        List<PROPERTY_VALUE> address = new ArrayList<PROPERTY_VALUE>();

        if (what.equals("server-group")) {
            String groupName = parameters.getSimpleValue("name","");
            String profile = parameters.getSimpleValue("profile","default");

            address.add(new PROPERTY_VALUE("server-group",groupName));

            operation = new Operation(op,address,"profile",profile);
        } else if (what.equals("server")) {
            if (context.getResourceType().getName().equals("JBossAS-Managed")) {
                String host = pluginConfiguration.getSimpleValue("domainHost","local");
                address.add(new PROPERTY_VALUE("host",host));
                address.add(new PROPERTY_VALUE("server-config",myServerName));
                operation = new Operation(op,address);
            }
            else if (context.getResourceType().getName().equals("Host")) {
                address.addAll(pathToAddress(getPath()));
                String serverName = parameters.getSimpleValue("name",null);
                address.add(new PROPERTY_VALUE("server-config",serverName));
                Map<String,Object> props = new HashMap<String, Object>();
                String serverGroup = parameters.getSimpleValue("group",null);
                props.put("group",serverGroup);
                if (op.equals("add")) {
                    props.put("name",serverName);
                    boolean autoStart = parameters.getSimple("auto-start").getBooleanValue();
                    props.put("auto-start",autoStart);
                    // TODO put more properties in
                }

                operation = new Operation(op,address,props);
            }
            else {
                operation = new Operation(op,address);
            }
        } else if (what.equals("destination")) {
            address.addAll(pathToAddress(getPath()));
            String newName = parameters.getSimpleValue("name","");
            String type = parameters.getSimpleValue("type","jms-queue").toLowerCase();
            address.add(new PROPERTY_VALUE(type,newName));
            PropertyList jndiNamesProp = parameters.getList("entries");
            if (jndiNamesProp==null || jndiNamesProp.getList().isEmpty()) {
                OperationResult fail = new OperationResult();
                fail.setErrorMessage("No jndi bindings given");
                return fail;
            }
            List<String> jndiNames = new ArrayList<String>();
            for (Property p : jndiNamesProp.getList()) {
                PropertySimple ps = (PropertySimple) p;
                jndiNames.add(ps.getStringValue());
            }

            operation = new Operation(op,address);
            operation.addAdditionalProperty("entries",jndiNames);
            if (type.equals("jms-queue")) {
                PropertySimple ps = (PropertySimple) parameters.get("durable");
                if (ps!=null) {
                    boolean durable = ps.getBooleanValue();
                    operation.addAdditionalProperty("durable",durable);
                }
                String selector = parameters.getSimpleValue("selector","");
                if (!selector.isEmpty())
                    operation.addAdditionalProperty("selector",selector);
            }


        } else if (what.equals("managed-server")) {
            String chost = parameters.getSimpleValue("hostname","");
            String serverName = parameters.getSimpleValue("servername","");
            String serverGroup = parameters.getSimpleValue("server-group","");
            String socketBindings = parameters.getSimpleValue("socket-bindings","");
            String portS = parameters.getSimpleValue("port-offset","0");
            int port = Integer.parseInt(portS);
            String autostartS = parameters.getSimpleValue("auto-start","false");
            boolean autoStart = Boolean.getBoolean(autostartS);

            address.add(new PROPERTY_VALUE("host", chost));
            address.add(new PROPERTY_VALUE("server-config",serverName));
            Map<String,Object> props = new HashMap<String, Object>();
            props.put("name",serverName);
            props.put("group",serverGroup);
            props.put("socket-binding-group",socketBindings);
            props.put("socket-binding-port-offset",port);
            props.put("auto-start",autoStart);

            operation = new Operation(op,address,props);
        } else if (what.equals("domain")) {
            operation = new Operation(op,Collections.<PROPERTY_VALUE>emptyList());
        } else if (what.equals("domain-deployment")) {
            if (op.equals("promote")) {
                String serverGroup = parameters.getSimpleValue("server-group","-not set-");
                List<String> serverGroups = new ArrayList<String>();
                if (serverGroup.equals("__all")) {
                    serverGroups.addAll(getServerGroups());
                }
                else {
                    serverGroups.add(serverGroup);
                }
                String resourceKey = context.getResourceKey();
                resourceKey = resourceKey.substring(resourceKey.indexOf("=")+1);

                log.info("Promoting [" + resourceKey + "] to server group(s) [" + Arrays.asList(serverGroups) + "]");

                PropertySimple simple = parameters.getSimple("enabled");
                Boolean enabled = false;
                if (simple!=null && simple.getBooleanValue()!=null)
                    enabled= simple.getBooleanValue();

                operation = new CompositeOperation();
                for (String theGroup : serverGroups) {
                    address = new ArrayList<PROPERTY_VALUE>();
                    address.add(new PROPERTY_VALUE("server-group",theGroup));

                    address.add(new PROPERTY_VALUE("deployment", resourceKey));
                    Operation step = new Operation("add",address,"enabled",enabled);
                    ((CompositeOperation)operation).addStep(step);
                }
            }
        } else if (what.equals("naming")) {
            if (op.equals("jndi-view")) {
                address.addAll(pathToAddress(getPath()));
                operation = new Operation("jndi-view",address);
            }
        }


        OperationResult operationResult = new OperationResult();
        if (operation!=null) {
            Result result = connection.execute(operation);

            if (!result.isSuccess()) {
                operationResult.setErrorMessage(result.getFailureDescription());
            }
            else {
                String tmp;
                if (result.getResult()==null)
                    tmp = "-none provided by the server-";
                else
                    tmp = result.getResult().toString();
                operationResult.setSimpleResult(tmp);
            }
        }
        else {
            operationResult.setErrorMessage("No valid operation was given for input [" + name + "]");
        }
        return operationResult;
    }

    @SuppressWarnings("unchecked")
    private Collection<String> getServerGroups() {
        Operation op = new ReadChildrenNames(Collections.<PROPERTY_VALUE>emptyList(),"server-group");
        Result res = connection.execute(op);

        return (Collection<String>) res.getResult();
    }

    Object getObjectForProperty(PropertySimple prop, PropertyDefinitionSimple propDef) {

        PropertySimpleType type = propDef.getType();
        switch (type) {
            case STRING:
                return prop.getStringValue();
            case INTEGER:
                return prop.getIntegerValue();
            case BOOLEAN:
                return prop.getBooleanValue();
            case LONG:
                return prop.getLongValue();
            case FLOAT:
                return prop.getFloatValue();
            case DOUBLE:
                return prop.getDoubleValue();
            default:
                return prop.getStringValue();
        }


    }

    ///// Those two are used to 'inject' the connection and the path from tests.
    public void setConnection(ASConnection connection) {
        this.connection = connection;
    }

    public void setPath(String path) {
        this.path = path;
    }


}
