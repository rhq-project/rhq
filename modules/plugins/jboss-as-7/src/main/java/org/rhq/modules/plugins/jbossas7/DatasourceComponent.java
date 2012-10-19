package org.rhq.modules.plugins.jbossas7;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.Property;
import org.rhq.core.domain.configuration.PropertyList;
import org.rhq.core.domain.configuration.PropertyMap;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.pluginapi.configuration.ConfigurationFacet;
import org.rhq.core.pluginapi.configuration.ConfigurationUpdateReport;
import org.rhq.core.pluginapi.operation.OperationFacet;
import org.rhq.core.pluginapi.operation.OperationResult;
import org.rhq.modules.plugins.jbossas7.json.Address;
import org.rhq.modules.plugins.jbossas7.json.CompositeOperation;
import org.rhq.modules.plugins.jbossas7.json.Operation;
import org.rhq.modules.plugins.jbossas7.json.Result;

/**
 * Handle JDBC-driver related stuff
 * @author Heiko W. Rupp
 */
public class DatasourceComponent extends BaseComponent implements OperationFacet, ConfigurationFacet {

    private static final String NOTSET = "-notset-";
    private final Log log = LogFactory.getLog(DatasourceComponent.class);

    @Override
    public OperationResult invokeOperation(String operationName,
                                           Configuration parameters) throws Exception {

        OperationResult result = new OperationResult();
        ASConnection connection = getASConnection();
        Operation op;

        if (operationName.equals("addDriver")) { // TODO decide if we need this at all. See also the plugin-descriptor
            String drivername = parameters.getSimpleValue("driver-name", NOTSET);


            Address theAddress = new Address(address);
            theAddress.add("jdbc-driver", drivername);

            op =  new Operation("add",theAddress);
            op.addAdditionalProperty("driver-name",drivername);
            op.addAdditionalProperty("deployment-name",parameters.getSimpleValue("deployment-name", NOTSET));
            op.addAdditionalProperty("driver-class-name",parameters.getSimpleValue("driver-class-name", NOTSET));


        }
        else if (operationName.equals("addDatasource")) {
            String name = parameters.getSimpleValue("name",NOTSET);

            Address theAddress = new Address(address);
            theAddress.add("data-source", name);
            op = new Operation("add",theAddress);
            addRequiredToOp(op,parameters,"driver-name");
            addRequiredToOp(op,parameters,"jndi-name");
            addRequiredToOp(op, parameters, "connection-url");
            addOptionalToOp(op, parameters, "user-name");
            addOptionalToOp(op,parameters,"password");
        }
        else if (operationName.equals("addXADatasource")) {
            String name = parameters.getSimpleValue("name",NOTSET);

            Address theAddress = new Address(address);
            theAddress.add("xa-data-source",name);
            op = new CompositeOperation();
            Operation step1 = new Operation("add",theAddress);
            addRequiredToOp(step1,parameters,"driver-name");
            addRequiredToOp(step1,parameters,"jndi-name");
            addOptionalToOp(step1,parameters,"user-name");
            addOptionalToOp(step1,parameters,"password");
            addRequiredToOp(step1,parameters,"xa-datasource-class");

            ((CompositeOperation)op).addStep(step1);

            // handling of xa-properties -- this is now a subresource in AS7 and at least needs a connection url
            String connectionUrl = parameters.getSimpleValue("connection-url",null);
            if (connectionUrl==null || connectionUrl.isEmpty())
                throw new IllegalArgumentException("Connection-url must not be empty");
            Address cuAddress = new Address(theAddress);
            cuAddress.add("xa-datasource-properties","connection-url");
            Operation step2 = new Operation("add",cuAddress);
            step2.addAdditionalProperty("value",connectionUrl);
            ((CompositeOperation)op).addStep(step2);


            PropertyList xaPropList = parameters.getList("xa-properties");
            if (xaPropList != null) {
                List<Property> xaProps = xaPropList.getList();
                for (Property prop : xaProps) {
                    PropertyMap pMap = (PropertyMap) prop;
                    PropertySimple keyProp = pMap.getSimple("key");
                    PropertySimple valProp = pMap.getSimple("value");
                    Address propAddress = new Address(theAddress);
                    propAddress.add("xa-datasource-properties",keyProp.getStringValue());
                    Operation step = new Operation("add",propAddress);
                    step.addAdditionalProperty("value",valProp.getStringValue()); // TODO ??
                    ((CompositeOperation)op).addStep(step);
                }
            }
        }
        else {
            /*
             * This is a catch all for operations that are not explicitly treated above.
             */
            op = new Operation(operationName,address);
        }

        Result res = connection.execute(op);
        if (res.isSuccess()) {
            result.setSimpleResult("Success");
        }
        else {
            result.setErrorMessage(res.getFailureDescription());
        }


        return result;
    }

    void addAdditionalToOp(Operation op, Configuration parameters, String property, boolean optional)  {

        PropertySimple ps = parameters.getSimple(property);
        if (ps==null) {
            if (!optional)
                throw new IllegalArgumentException("Property " + property + " not found for required parameter");
        }
        else {
            String tmp = ps.getStringValue();
            if (tmp!=null) {
                op.addAdditionalProperty(property,tmp);
            }
        }
    }


    void addRequiredToOp(Operation op, Configuration parameters, String property)  {
        addAdditionalToOp(op,parameters,property,false);
    }

    void addOptionalToOp(Operation op, Configuration parameters, String property) {
        addAdditionalToOp(op,parameters,property,true);
    }

    @Override
    public void updateResourceConfiguration(ConfigurationUpdateReport report) {

        Operation op = new Operation("disable",getAddress());
        Result res = getASConnection().execute(op);
        if (!res.isSuccess()) {
            report.setErrorMessage("Was not able to disable the datasource for config changes: " + res.getFailureDescription());
            return;
        }

        super.updateResourceConfiguration(report);

        op = new Operation("enable",getAddress());
        res = getASConnection().execute(op);

    }
}
