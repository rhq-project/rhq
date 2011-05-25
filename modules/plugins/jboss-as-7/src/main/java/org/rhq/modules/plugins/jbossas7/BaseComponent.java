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
import org.codehaus.jackson.map.ObjectMapper;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.ConfigurationUpdateStatus;
import org.rhq.core.domain.configuration.Property;
import org.rhq.core.domain.configuration.PropertyList;
import org.rhq.core.domain.configuration.PropertyMap;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.configuration.definition.PropertyDefinition;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionList;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionMap;
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
import org.rhq.modules.plugins.jbossas7.json.ComplexResult;
import org.rhq.modules.plugins.jbossas7.json.CompositeOperation;
import org.rhq.modules.plugins.jbossas7.json.NameValuePair;
import org.rhq.modules.plugins.jbossas7.json.Operation;
import org.rhq.modules.plugins.jbossas7.json.PROPERTY_VALUE;
import org.rhq.modules.plugins.jbossas7.json.ReadAttribute;
import org.rhq.modules.plugins.jbossas7.json.ReadResource;
import org.rhq.modules.plugins.jbossas7.json.Result;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class BaseComponent implements ResourceComponent, MeasurementFacet, ConfigurationFacet, DeleteResourceFacet,
        CreateChildResourceFacet, OperationFacet
{
    final Log log = LogFactory.getLog(this.getClass());

    ResourceContext context;
    Configuration conf;
    String myServerName;
    ASConnection connection;
    String path;
    String key;
    String host;
    int port;

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
        conf = context.getPluginConfiguration();
        // TODO add code to start the resource / connection to it

        String typeName = context.getResourceType().getName();
        host = conf.getSimpleValue("hostname","localhost");
        String portString = conf.getSimpleValue("port","9990");
        port = Integer.parseInt(portString);
        connection = new ASConnection(host,port);

        path = conf.getSimpleValue("path", null);
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


            Operation op = new ReadAttribute(pathToAddress(path),req.getName()); // TODO batching
            //JsonNode obj = connection.executeRaw(op);
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


    protected ASConnection getASConnection() {
        return connection;
    }


    protected String getPath() { return path; }

    // TODO this needs completeion and a big fat refactoring
    public Configuration loadResourceConfiguration() throws Exception {
        ConfigurationDefinition configDef = context.getResourceType().getResourceConfigurationDefinition();
//        String myPath = getResultingPath();

        List<PROPERTY_VALUE> address = pathToAddress(path);
        Operation op = new ReadResource(address); // TOTO set recursive flag?
        op.addAdditionalProperty("recursive","true");
        JsonNode json = connection.executeRaw(op);

        Configuration ret = new Configuration();
        ObjectMapper mapper = new ObjectMapper();

        Set<Map.Entry<String, PropertyDefinition>> entrySet = configDef.getPropertyDefinitions().entrySet();
        for (Map.Entry<String,PropertyDefinition> propDefEntry: entrySet) {
            PropertyDefinition propDef = propDefEntry.getValue();
            JsonNode sub = json.findValue(propDef.getName());
            if (sub==null) {
                log.error("No value for property [" + propDef.getName() + "] found - check the descriptor");
                continue;
            }
            if (propDef instanceof PropertyDefinitionSimple) {
                PropertySimple propertySimple;

                if (sub!=null) {
                    // Property is non-null -> return it.
                    propertySimple = new PropertySimple(propDef.getName(),sub.getValueAsText());
                    ret.put(propertySimple);
                }
                else {
                    // property is null? Check if it is required
                    if (propDef.isRequired()) {
                        String defaultValue = ((PropertyDefinitionSimple) propDef).getDefaultValue();
                        propertySimple = new PropertySimple(propDef.getName(),defaultValue);
                        ret.put(propertySimple);
                    }
                }
            } else if (propDef instanceof PropertyDefinitionList) {
                PropertyList propertyList = new PropertyList(propDef.getName());
                PropertyDefinition memberDefinition = ((PropertyDefinitionList) propDef).getMemberDefinition();
                if (memberDefinition ==null) {
                    if (sub.isObject()) {
                        Iterator<String> fields = sub.getFieldNames();
                        while(fields.hasNext()) {
                            String fieldName = fields.next();
                            JsonNode subNode = sub.get(fieldName);
                            PropertySimple propertySimple = new PropertySimple(propDef.getName(),fieldName);
                            propertyList.add(propertySimple);
                        }
                    } else {
                        System.out.println("===Sub not object==="); // TODO evaluate this branch again
                        Iterator<JsonNode> values = sub.getElements();
                        while (values.hasNext()) {
                            JsonNode node = values.next();
                            String value = node.getTextValue();
                            PropertySimple propertySimple = new PropertySimple(propDef.getName(),value);
                            propertyList.add(propertySimple);
                        }
                    }
                }
                else if (memberDefinition instanceof PropertyDefinitionMap) {
                    PropertySimple propertySimple;

                    if (sub.isArray()) {
                        Iterator<JsonNode> entries = sub.getElements();
                        while (entries.hasNext()) {
                            JsonNode entry = entries.next(); // -> one row in the list i.e. one map

                            // Distinguish here?

                            PropertyMap map = new PropertyMap(memberDefinition.getName()); // TODO : name from def or 'entryKey' ?
                            Iterator<JsonNode> fields = entry.getElements(); // TODO loop over fields from map and not from json
                            while (fields.hasNext()) {
                                JsonNode field  = fields.next();
                                if (field.isObject()) {
                                    // TODO only works for tuples at the moment - migrate to some different kind of parsing!
                                    PROPERTY_VALUE prop = mapper.readValue(field,PROPERTY_VALUE.class);
                                    // now need to find the names of the properties
                                    List<PropertyDefinition> defList = ((PropertyDefinitionMap) memberDefinition).getSummaryPropertyDefinitions();
                                    if (defList.isEmpty())
                                        throw new IllegalArgumentException("Map " + memberDefinition.getName() + " has no members");
                                    String key = defList.get(0).getName();
                                    String value = prop.getKey();
                                    propertySimple = new PropertySimple(key,value);
                                    map.put(propertySimple);
                                    if (defList.size()>1) {
                                        key = defList.get(1).getName();
                                        value = prop.getValue();
                                        propertySimple = new PropertySimple(key,value);
                                        map.put(propertySimple);

                                    }
                                } else { // TODO reached?
                                    String key = field.getValueAsText();
                                    if (key.equals("PROPERTY_VALUE")) { // TODO this may change in the future in the AS implementation
                                        JsonNode pv = entry.findValue(key);
                                        String k = pv.toString();
                                        String v = pv.getValueAsText();
                                        propertySimple = new PropertySimple(k,v);
                                        map.put(propertySimple);

                                    }
                                    else {
                                        JsonNode value = entry.findValue(key);
                                        if (value!=null){
                                            propertySimple = new PropertySimple(key, value.getValueAsText());
                                            map.put(propertySimple);
                                        }

                                    }
                                }
                            }
                            propertyList.add(map);
                        }
                    }
                    else if (sub.isObject()) {
                        Iterator<String> keys = sub.getFieldNames();
                        while(keys.hasNext()) {
                            String entryKey = keys.next();

                            JsonNode node = sub.findPath(entryKey);
                            PropertyMap map = new PropertyMap(memberDefinition.getName()); // TODO : name from def or 'entryKey' ?
                            if (node.isObject()) {
                                Iterator<String> fields = node.getFieldNames(); // TODO loop over fields from map and not from json
                                while (fields.hasNext()) {
                                    String key = fields.next();

                                    propertySimple = new PropertySimple(key,node.findValue(key).getValueAsText());
                                    map.put(propertySimple);
                                }
                                propertyList.add(map);
                            } else if (sub.isNull()) {
                                List<PropertyDefinition> defList = ((PropertyDefinitionMap) memberDefinition).getSummaryPropertyDefinitions();
                                String key = defList.get(0).getName();
                                propertySimple = new PropertySimple(key,entryKey);
                                map.put(propertySimple);
                            }
                        }

                    }
                }
                else if (memberDefinition instanceof PropertyDefinitionSimple) {
                    String name = memberDefinition.getName();
                    Iterator<JsonNode> keys = sub.getElements();
                    while(keys.hasNext()) {
                        JsonNode entry = keys.next();

                        PropertySimple propertySimple = new PropertySimple(name,entry.getTextValue());
                        propertyList.add(propertySimple);
                    }
                }
                ret.put(propertyList);
            } // end List of ..
            else if (propDef instanceof PropertyDefinitionMap) {
                PropertyDefinitionMap mapDef = (PropertyDefinitionMap) propDef;
                PropertyMap pm = new PropertyMap(mapDef.getName());

                Map<String,PropertyDefinition> memberDefMap = mapDef.getPropertyDefinitions();
                for (Map.Entry<String,PropertyDefinition> maEntry : memberDefMap.entrySet()) {
                    JsonNode valueNode = json.findValue(maEntry.getKey());
                    Property p;
                    if (maEntry.getValue() instanceof PropertyDefinitionSimple) {
                        p = putProperty(valueNode, maEntry.getValue());
                        pm.put(p);
                    }
                    else if (maEntry.getValue() instanceof  PropertyDefinitionMap) { // TODO make this recursive?

                        PropertyDefinitionMap pdm = (PropertyDefinitionMap) maEntry.getValue();
                        Map<String,PropertyDefinition> mmDefMap = pdm.getPropertyDefinitions();
                        for (Map.Entry<String,PropertyDefinition> mmDefEntry : mmDefMap.entrySet()) {
                            if (valueNode!=null) {
                                JsonNode node2 = valueNode.findValue(mmDefEntry.getKey());
                                System.err.println("Map not yet implemented " + node2.toString());
                            }
                            else
                                System.err.println("Value node was null " );
                        }
                    }
                    else { // PropDefList
                        System.err.println("List not yet implemented");
                    }

//                    pm.put(p);
                }
                ret.put(pm);
            }
        }


        return ret;
    }


    PropertySimple putProperty(JsonNode value, PropertyDefinition def) {
        String name = def.getName();
        PropertySimple ps;

        if (value==null) {
            if (def instanceof PropertyDefinitionSimple) {
                PropertyDefinitionSimple pds = (PropertyDefinitionSimple) def;
                return new PropertySimple(name,pds.getDefaultValue());
            }
            else
                return new PropertySimple(name,null);
        }
        PropertySimpleType type = ((PropertyDefinitionSimple) def).getType();

        switch (type) {
        case BOOLEAN:
            ps = new PropertySimple(name,value.getBooleanValue());
            break;
        case FLOAT:
        case DOUBLE:
            ps = new PropertySimple(name,value.getDoubleValue());
            break;
        case INTEGER:
            ps = new PropertySimple(name,value.getIntValue());
            break;
        case LONG:
            ps = new PropertySimple(name,value.getLongValue());
            break;
        default:
            ps = new PropertySimple(name,value.getTextValue());
        }

        return ps;
    }

    protected String getResultingPath() {
        ResourceComponent parentResourceComponent = context.getParentResourceComponent();
        String parentPath =null;
        String myPath;
        if (parentResourceComponent instanceof BaseComponent) {
            BaseComponent parentComponent = (BaseComponent) parentResourceComponent;
            parentPath = parentComponent.getPath();
        }

        if (parentPath!=null) {
                myPath = parentPath + "," + path;
        }
        else
            myPath = path;
        return myPath;
    }

    public void updateResourceConfiguration(ConfigurationUpdateReport report) {


        Configuration conf = report.getConfiguration();
        for (Map.Entry<String, PropertySimple> entry : conf.getSimpleProperties().entrySet()) {

            NameValuePair nvp = new NameValuePair(entry.getKey(),entry.getValue().getStringValue());
            Operation writeAttribute = new Operation("write-attribute",pathToAddress(getResultingPath()),nvp);
            JsonNode result= connection.executeRaw(writeAttribute);
            if(ASConnection.isErrorReply(result)) {
                report.setStatus(ConfigurationUpdateStatus.FAILURE);
                report.setErrorMessage(ASConnection.getFailureDescription(result));
            }
        }

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
            // This was a server group level deployment - we also need to remove the entry in /deployments
            for (PROPERTY_VALUE val : address) {
                if (val.getKey().equals("deployment")) {
                    ComplexResult res2 = connection.executeComplex(new Operation("remove",val.getKey(),val.getValue()));
                    if (!res2.isSuccess())
                        throw new IllegalArgumentException("Removal of [" + path + "] falied : " + res2.getFailureDescription());
                }
            }
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
        System.out.println(uploadResult);
        if (ASConnection.isErrorReply(uploadResult)) {
            report.setStatus(CreateResourceStatus.FAILURE);
            report.setErrorMessage(ASConnection.getFailureDescription(uploadResult));

            return report;
        }

        String fileName = details.getFileName();

        if (fileName.startsWith("C:\\fakepath\\")) {   // TODO this is a hack as the server adds the fake path somehow
            fileName=fileName.substring("C:\\fakepath\\".length());
        }

        log.info("Deploying [" + fileName + "] ...");

        String tmpName = fileName; // TODO figure out the tmp-name biz with the AS guys

        JsonNode resultNode = uploadResult.get("result");
        String hash = resultNode.get("BYTES_VALUE").getTextValue();
        ASConnection connection = getASConnection();

        Operation step1 = new Operation("add","deployment",tmpName);
//        step1.addAdditionalProperty("hash", new PROPERTY_VALUE("BYTES_VALUE", hash));
        List<Object> content = new ArrayList<Object>(1);
        Map<String,Object> contentValues = new HashMap<String,Object>();
        contentValues.put("hash",new PROPERTY_VALUE("BYTES_VALUE",hash));
        content.add(contentValues);
        step1.addAdditionalProperty("content",content);

        step1.addAdditionalProperty("name", tmpName);
        step1.addAdditionalProperty("runtime-name", fileName);

        CompositeOperation cop = new CompositeOperation();
        cop.addStep(step1);
        String resourceKey;

        /*
         * We need to check here if this is an upload to /deployment only
         * or if this should be deployed to a server group too
         */
        if (context.getResourceKey().contains("server-group=")) {

            List<PROPERTY_VALUE> serverGroupAddress = new ArrayList<PROPERTY_VALUE>();
            serverGroupAddress.addAll(pathToAddress(context.getResourceKey()));
            serverGroupAddress.add(new PROPERTY_VALUE("deployment", tmpName));
            Operation step2 = new Operation("add",serverGroupAddress);

            cop.addStep(step2);

            Operation step3 = new Operation("deploy",serverGroupAddress);
            cop.addStep(step3);

            resourceKey = addressToPath(serverGroupAddress);
        }
        else {

            List<PROPERTY_VALUE> address = step1.getAddress();
            Operation step3 = new Operation("deploy",address);
            cop.addStep(step3);

            resourceKey = addressToPath(address);
        }

        JsonNode result = connection.executeRaw(cop);
        if (ASConnection.isErrorReply(result)) {
            String failureDescription = ASConnection.getFailureDescription(result);
            report.setErrorMessage(failureDescription);
            report.setStatus(CreateResourceStatus.FAILURE);
            log.warn(" ... done with failure: " + failureDescription);
        }
        else {
            report.setStatus(CreateResourceStatus.SUCCESS);
            report.setResourceName(fileName);
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
            String groupName = parameters.getSimpleValue("name",null);
            String profile = parameters.getSimpleValue("profile","default");

            address.add(new PROPERTY_VALUE("server-group",groupName));

            operation = new Operation(op,address,"profile",profile);
        } else if (what.equals("server")) {

            if (context.getResourceType().getName().equals("JBossAS-Managed")) {
                String host = conf.getSimpleValue("domainHost","local");
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
        } else if (what.equals("destination")) {
            address.addAll(pathToAddress(getPath()));
            String newName = parameters.getSimpleValue("name","");
//            String type = parameters.getSimpleValue("type","Queue").toLowerCase();
//            address.add(new PROPERTY_VALUE(type,newName));
            String queueName = parameters.getSimpleValue("queue-address","");
            Map<String,Object> props = new HashMap<String, Object>();
            props.put("queue-address",queueName);
            operation = new Operation(op,address);
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
        }

        OperationResult operationResult = new OperationResult();
        if (operation!=null) {
            JsonNode result = connection.executeRaw(operation);

            if (ASConnection.isErrorReply(result)) {
                operationResult.setErrorMessage(ASConnection.getFailureDescription(result));
            }
            else {
                operationResult.setSimpleResult(ASConnection.getSuccessDescription(result));
            }
        }
        else {
            operationResult.setErrorMessage("No valid operation was given");
        }
        // TODO throw an exception if the operation failed?
        return operationResult;
    }
}
