package org.rhq.modules.plugins.jbossas7;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.pluginapi.operation.OperationFacet;
import org.rhq.core.pluginapi.operation.OperationResult;
import org.rhq.modules.plugins.jbossas7.json.Operation;
import org.rhq.modules.plugins.jbossas7.json.PROPERTY_VALUE;
import org.rhq.modules.plugins.jbossas7.json.Result;

/**
 * Handle JDBC-driver related stuff
 * @author Heiko W. Rupp
 */
public class DatasourceComponent extends BaseComponent implements OperationFacet {

    private static final String NOTSET = "-notset-";
    private final Log log = LogFactory.getLog(DatasourceComponent.class);

    @Override
    public OperationResult invokeOperation(String operationName,
                                           Configuration parameters) throws Exception {

        OperationResult result = new OperationResult();
        if (operationName.equals("addDriver")) { // TODO decide if we need this at all. See also the plugin-descriptor
            String drivername = parameters.getSimpleValue("driver-name", NOTSET);


            List<PROPERTY_VALUE> address = pathToAddress(getPath());
            address.add(new PROPERTY_VALUE("jdbc-driver",drivername));

            Operation op =  new Operation("add",address,"driver-name",drivername);
            op.addAdditionalProperty("deployment-name",parameters.getSimpleValue("deployment-name", NOTSET));
            op.addAdditionalProperty("driver-class-name",parameters.getSimpleValue("driver-class-name", NOTSET));

            ASConnection connection = getASConnection();
            Result res = connection.execute(op);
            if (res.isSuccess()) {
                result.setSimpleResult("Success");
            }
            else {
                result.setErrorMessage(res.getFailureDescription().toString());
            }
        }
        else if (operationName.equals("addDatasource")) {
            String driver = parameters.getSimpleValue("driver", NOTSET);
            String jndiName = parameters.getSimpleValue("jndi-name", NOTSET);
            String poolName = parameters.getSimpleValue("pool-name", NOTSET);
            String connectionUrl = parameters.getSimpleValue("connection-url",NOTSET);
            String userName = parameters.getSimpleValue("user-name","");
            String password = parameters.getSimpleValue("password","");
            String name = parameters.getSimpleValue("name",NOTSET);

            List<PROPERTY_VALUE> address = pathToAddress(getPath());
            address.add(new PROPERTY_VALUE("data-source",name));
            Operation op = new Operation("add",address);
            op.addAdditionalProperty("driver-name",driver);
            op.addAdditionalProperty("jndi-name",jndiName);
            op.addAdditionalProperty("pool-name",poolName);
            op.addAdditionalProperty("connection-url",connectionUrl);
            if (userName!=null && !userName.isEmpty())
                op.addAdditionalProperty("user-name",userName);
            if (password!=null && !password.isEmpty())
                op.addAdditionalProperty("password",password);

            Result res = connection.execute(op);
            if (res.isSuccess()) {
                result.setSimpleResult("Success");
            }
            else {
                result.setErrorMessage(res.getFailureDescription().toString());
            }

        }
        else {
            result.setErrorMessage("Unknown operation " + operationName);
        }

        return result;
    }

    void addAdditionalToOp(Operation op, Configuration parameters, String property, boolean optional) {

    }
}
