/*
 * RHQ Management Platform
 * Copyright (C) 2005-2012 Red Hat, Inc.
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
package org.rhq.common.jbossas.client.controller;

import java.util.Map;

import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.dmr.ModelNode;

/**
 * Provides convenience methods associated with datasource management.
 *
 * @author John Mazzitelli
 */
public class DatasourceJBossASClient extends JBossASClient {

    public static final String SUBSYSTEM_DATASOURCES = "datasources";
    public static final String DATA_SOURCE = "data-source";
    public static final String XA_DATA_SOURCE = "xa-data-source";
    public static final String JDBC_DRIVER = "jdbc-driver";
    public static final String CONNECTION_PROPERTIES = "connection-properties";
    public static final String XA_DATASOURCE_PROPERTIES = "xa-datasource-properties";
    public static final String OP_ENABLE = "enable";

    public DatasourceJBossASClient(ModelControllerClient client) {
        super(client);
    }

    /**
     * Completely removes the named datasource. If the datasource does not exist,
     * this returns silently (in other words, no exception is thrown).
     *
     * Note that no distinguishing between XA and non-XA datasource is needed - if any datasource
     * (XA or non-XA) exists with the given name, it will be removed.
     *
     * @param name the name of the datasource to remove
     * @throws Exception
     */
    public void removeDatasource(String name) throws Exception {
        Address addr = Address.root().add(SUBSYSTEM, SUBSYSTEM_DATASOURCES);
        if (isDatasource(name)) {
            addr.add(DATA_SOURCE, name);
        } else if (isXADatasource(name)) {
            addr.add(XA_DATA_SOURCE, name);
        } else {
            return; // there is no datasource (XA or non-XA) with the given name, just return silently
        }

        remove(addr);
        return;
    }

    public boolean isDatasourceEnabled(String name) throws Exception {
        return isDatasourceEnabled(false, name);
    }

    public boolean isXADatasourceEnabled(String name) throws Exception {
        return isDatasourceEnabled(true, name);
    }

    private boolean isDatasourceEnabled(boolean isXA, String name) throws Exception {
        Address addr = Address.root().add(SUBSYSTEM, SUBSYSTEM_DATASOURCES);
        addr.add((isXA) ? XA_DATA_SOURCE : DATA_SOURCE, name);
        ModelNode results = readResource(addr);
        boolean enabledFlag = false;
        if (results.hasDefined("enabled")) {
            ModelNode enabled = results.get("enabled");
            enabledFlag = enabled.asBoolean(false);
        }
        return enabledFlag;
    }

    public void enableDatasource(String name) throws Exception {
        enableDatasource(false, name);
    }

    public void enableXADatasource(String name) throws Exception {
        enableDatasource(true, name);
    }

    private void enableDatasource(boolean isXA, String name) throws Exception {
        if (isDatasourceEnabled(isXA, name)) {
            return; // nothing to do - its already enabled
        }

        Address addr = Address.root().add(SUBSYSTEM, SUBSYSTEM_DATASOURCES);
        addr.add((isXA) ? XA_DATA_SOURCE : DATA_SOURCE, name);
        ModelNode request = createRequest(OP_ENABLE, addr);
        request.get(PERSISTENT).set(true);
        ModelNode results = execute(request);
        if (!isSuccess(results)) {
            throw new FailureException(results);
        }
        return; // everything is OK
    }

    /**
     * Checks to see if there is already a JDBC driver with the given name.
     *
     * @param jdbcDriverName the name to check
     * @return true if there is a JDBC driver with the given name already in existence
     */
    public boolean isJDBCDriver(String jdbcDriverName) throws Exception {
        Address addr = Address.root().add(SUBSYSTEM, SUBSYSTEM_DATASOURCES);
        String haystack = JDBC_DRIVER;
        return null != findNodeInList(addr, haystack, jdbcDriverName);
    }

    /**
     * Checks to see if there is already a datasource with the given name.
     *
     * @param datasourceName the name to check
     * @return true if there is a datasource with the given name already in existence
     */
    public boolean isDatasource(String datasourceName) throws Exception {
        Address addr = Address.root().add(SUBSYSTEM, SUBSYSTEM_DATASOURCES);
        String haystack = DATA_SOURCE;
        return null != findNodeInList(addr, haystack, datasourceName);
    }

    /**
     * Checks to see if there is already a XA datasource with the given name.
     *
     * @param datasourceName the name to check
     * @return true if there is a XA datasource with the given name already in existence
     */
    public boolean isXADatasource(String datasourceName) throws Exception {
        Address addr = Address.root().add(SUBSYSTEM, SUBSYSTEM_DATASOURCES);
        String haystack = XA_DATA_SOURCE;
        return null != findNodeInList(addr, haystack, datasourceName);
    }

    /**
     * Returns a ModelNode that can be used to create a JDBC driver configuration for use by datasources.
     * Callers are free to tweek the JDBC driver request that is returned,
     * if they so choose, before asking the client to execute the request.
     *
     * NOTE: the JDBC module must have already been installed in the JBossAS's modules/ location.
     *
     * @param name the name of the JDBC driver (this is not the name of the JDBC jar or the module name, it is
     *             just a convenience name of the JDBC driver configuration).
     * @param moduleName the name of the JBossAS module where the JDBC driver is installed
     * @param driverXaClassName the JDBC driver's XA datasource classname (null if XA is not supported)
     *
     * @return the request to create the JDBC driver configuration.
     */
    public ModelNode createNewJdbcDriverRequest(String name, String moduleName, String driverXaClassName) {
        String dmrTemplate;
        String dmr;

        if (driverXaClassName != null) {
            dmrTemplate = "" //
                + "{" //
                + "\"driver-module-name\" => \"%s\" " //
                + ", \"driver-name\" => \"%s\" " //
                + ", \"driver-xa-datasource-class-name\" => \"%s\" " //
                + "}";
            dmr = String.format(dmrTemplate, moduleName, name,  driverXaClassName);
        } else {
            dmrTemplate = "" //
                + "{" //
                + "\"driver-module-name\" => \"%s\" " //
                + ", \"driver-name\" => \"%s\" " //
                + "}";
            dmr = String.format(dmrTemplate, name, moduleName);
        }

        Address addr = Address.root().add(SUBSYSTEM, SUBSYSTEM_DATASOURCES, JDBC_DRIVER, name);
        final ModelNode request = ModelNode.fromString(dmr);
        request.get(OPERATION).set(ADD);
        request.get(ADDRESS).set(addr.getAddressNode());

        return request;
    }

    /**
     * Returns a ModelNode that can be used to create a datasource.
     * Callers are free to tweek the datasource request that is returned,
     * if they so choose, before asking the client to execute the request.
     *
     * @param name
     * @param blockingTimeoutWaitMillis
     * @param connectionUrlExpression
     * @param driverName
     * @param exceptionSorterClassName
     * @param idleTimeoutMinutes
     * @param jta true if this DS should support transactions; false if not
     * @param minPoolSize
     * @param maxPoolSize
     * @param preparedStatementCacheSize
     * @param securityDomain
     * @param staleConnectionCheckerClassName
     * @param transactionIsolation
     * @param validConnectionCheckerClassName
     * @param connectionProperties
     *
     * @return the request that can be used to create the datasource
     */
    public ModelNode createNewDatasourceRequest(String name, int blockingTimeoutWaitMillis,
        String connectionUrlExpression, String driverName, String exceptionSorterClassName, int idleTimeoutMinutes,
        boolean jta, int minPoolSize, int maxPoolSize, int preparedStatementCacheSize, String securityDomain,
        String staleConnectionCheckerClassName, String transactionIsolation, String validConnectionCheckerClassName,
        Map<String, String> connectionProperties) {

        String jndiName = "java:jboss/datasources/" + name;

        String dmrTemplate = "" //
            + "{" //
            + "\"blocking-timeout-wait-millis\" => %dL " //
            + ", \"connection-url\" => expression \"%s\" " //
            + ", \"driver-name\" => \"%s\" " //
            + ", \"exception-sorter-class-name\" => \"%s\" " //
            + ", \"idle-timeout-minutes\" => %dL " //
            + ", \"jndi-name\" => \"%s\" " //
            + ", \"jta\" => %s " //
            + ", \"min-pool-size\" => %d " //
            + ", \"max-pool-size\" => %d " //
            + ", \"prepared-statements-cache-size\" => %dL " //
            + ", \"security-domain\" => \"%s\" " //
            + ", \"stale-connection-checker-class-name\" => \"%s\" " //
            + ", \"transaction-isolation\" => \"%s\" " //
            + ", \"use-java-context\" => true " //
            + ", \"valid-connection-checker-class-name\" => \"%s\" " //
            + "}";

        String dmr = String.format(dmrTemplate, blockingTimeoutWaitMillis, connectionUrlExpression, driverName,
            exceptionSorterClassName, idleTimeoutMinutes, jndiName, jta, minPoolSize, maxPoolSize,
            preparedStatementCacheSize, securityDomain, staleConnectionCheckerClassName, transactionIsolation,
            validConnectionCheckerClassName);

        Address addr = Address.root().add(SUBSYSTEM, SUBSYSTEM_DATASOURCES, DATA_SOURCE, name);
        final ModelNode request1 = ModelNode.fromString(dmr);
        request1.get(OPERATION).set(ADD);
        request1.get(ADDRESS).set(addr.getAddressNode());

        // if there are no conn properties, no need to create a batch request, there is only one ADD request to make
        if (connectionProperties == null || connectionProperties.size() == 0) {
            return request1;
        }

        // create a batch of requests - the first is the main one, the rest create each conn property
        ModelNode[] batch = new ModelNode[1 + connectionProperties.size()];
        batch[0] = request1;
        int n = 1;
        for (Map.Entry<String, String> entry : connectionProperties.entrySet()) {
            addr = Address.root().add(SUBSYSTEM, SUBSYSTEM_DATASOURCES, DATA_SOURCE, name, CONNECTION_PROPERTIES,
                entry.getKey());
            final ModelNode requestN = new ModelNode();
            requestN.get(OPERATION).set(ADD);
            requestN.get(ADDRESS).set(addr.getAddressNode());
            setPossibleExpression(requestN, VALUE, entry.getValue());
            batch[n++] = requestN;
        }

        return createBatchRequest(batch);
    }

    public ModelNode createNewDatasourceRequest(String name, String connectionUrlExpression, String driverName,
        boolean jta, Map<String, String> connectionProperties) {

        String jndiName = "java:jboss/datasources/" + name;

        String dmrTemplate = "" //
            + "{" //
            + "\"connection-url\" => expression \"%s\" " //
            + ", \"driver-name\" => \"%s\" " //
            + ", \"jndi-name\" => \"%s\" " //
            + ", \"jta\" => %s " //
            + ", \"use-java-context\" => true " //
            + "}";

        String dmr = String.format(dmrTemplate, connectionUrlExpression, driverName, jndiName, jta);

        Address addr = Address.root().add(SUBSYSTEM, SUBSYSTEM_DATASOURCES, DATA_SOURCE, name);
        final ModelNode request1 = ModelNode.fromString(dmr);
        request1.get(OPERATION).set(ADD);
        request1.get(ADDRESS).set(addr.getAddressNode());

        // if there are no conn properties, no need to create a batch request, there is only one ADD request to make
        if (connectionProperties == null || connectionProperties.size() == 0) {
            return request1;
        }

        // create a batch of requests - the first is the main one, the rest create each conn property
        ModelNode[] batch = new ModelNode[1 + connectionProperties.size()];
        batch[0] = request1;
        int n = 1;
        for (Map.Entry<String, String> entry : connectionProperties.entrySet()) {
            addr = Address.root().add(SUBSYSTEM, SUBSYSTEM_DATASOURCES, DATA_SOURCE, name, CONNECTION_PROPERTIES,
                entry.getKey());
            final ModelNode requestN = new ModelNode();
            requestN.get(OPERATION).set(ADD);
            requestN.get(ADDRESS).set(addr.getAddressNode());
            if (entry.getValue().indexOf("${") > -1) {
                requestN.get(VALUE).setExpression(entry.getValue());
            } else {
                requestN.get(VALUE).set(entry.getValue());
            }
            batch[n++] = requestN;
        }

        return createBatchRequest(batch);
    }

    /**
     * Returns a ModelNode that can be used to create an XA datasource.
     * Callers are free to tweek the datasource request that is returned,
     * if they so choose, before asking the client to execute the request.
     *
     * @param name
     * @param blockingTimeoutWaitMillis
     * @param driverName
     * @param xaDataSourceClass
     * @param exceptionSorterClassName
     * @param idleTimeoutMinutes
     * @param minPoolSize
     * @param maxPoolSize
     * @param noRecovery optional, left unset if null
     * @param noTxSeparatePool optional, left unset if null
     * @param preparedStatementCacheSize
     * @param recoveryPluginClassName optional, left unset if null
     * @param securityDomain
     * @param staleConnectionCheckerClassName optional, left unset if null
     * @param transactionIsolation
     * @param validConnectionCheckerClassName
     * @param xaDatasourceProperties
     *
     * @return the request that can be used to create the XA datasource
     */
    public ModelNode createNewXADatasourceRequest(String name, int blockingTimeoutWaitMillis, String driverName, String xaDataSourceClass,
        String exceptionSorterClassName, int idleTimeoutMinutes, int minPoolSize, int maxPoolSize, Boolean noRecovery,
        Boolean noTxSeparatePool, int preparedStatementCacheSize, String recoveryPluginClassName,
        String securityDomain, String staleConnectionCheckerClassName, String transactionIsolation,
        String validConnectionCheckerClassName, Map<String, String> xaDatasourceProperties) {

        String jndiName = "java:jboss/datasources/" + name;

        String dmrTemplate = "" //
            + "{" //
            + "\"xa-datasource-class\" => \"%s\""
            + ", \"blocking-timeout-wait-millis\" => %dL " //
            + ", \"driver-name\" => \"%s\" " //
            + ", \"exception-sorter-class-name\" => \"%s\" " //
            + ", \"idle-timeout-minutes\" => %dL " //
            + ", \"jndi-name\" => \"%s\" " //
            + ", \"jta\" => true " //
            + ", \"min-pool-size\" => %d " //
            + ", \"max-pool-size\" => %d " //
            + ", \"no-recovery\" => %b " //
            + ", \"no-tx-separate-pool\" => %b " //
            + ", \"prepared-statements-cache-size\" => %dL " //
            + ", \"recovery-plugin-class-name\" => \"%s\" " //
            + ", \"security-domain\" => \"%s\" " //
            + ", \"stale-connection-checker-class-name\" => \"%s\" " //
            + ", \"transaction-isolation\" => \"%s\" " //
            + ", \"use-java-context\" => true " //
            + ", \"valid-connection-checker-class-name\" => \"%s\" " //
            + "}";

        String dmr = String.format(dmrTemplate, xaDataSourceClass, blockingTimeoutWaitMillis, driverName, exceptionSorterClassName,
            idleTimeoutMinutes, jndiName, minPoolSize, maxPoolSize, noRecovery, noTxSeparatePool,
            preparedStatementCacheSize, recoveryPluginClassName, securityDomain, staleConnectionCheckerClassName,
            transactionIsolation, validConnectionCheckerClassName);

        Address addr = Address.root().add(SUBSYSTEM, SUBSYSTEM_DATASOURCES, XA_DATA_SOURCE, name);
        final ModelNode request1 = ModelNode.fromString(dmr);
        request1.get(OPERATION).set(ADD);
        request1.get(ADDRESS).set(addr.getAddressNode());

        // if there are no xa datasource properties, no need to create a batch request, there is only one ADD request to make
        if (xaDatasourceProperties == null || xaDatasourceProperties.size() == 0) {
            return request1;
        }

        // create a batch of requests - the first is the main one, the rest create each conn property
        ModelNode[] batch = new ModelNode[1 + xaDatasourceProperties.size()];
        batch[0] = request1;
        int n = 1;
        for (Map.Entry<String, String> entry : xaDatasourceProperties.entrySet()) {
            addr = Address.root().add(SUBSYSTEM, SUBSYSTEM_DATASOURCES, XA_DATA_SOURCE, name, XA_DATASOURCE_PROPERTIES,
                entry.getKey());
            final ModelNode requestN = new ModelNode();
            requestN.get(OPERATION).set(ADD);
            requestN.get(ADDRESS).set(addr.getAddressNode());
            setPossibleExpression(requestN, VALUE, entry.getValue());
            batch[n++] = requestN;
        }

        ModelNode result = createBatchRequest(batch);

        // remove unset args
        if (null == noRecovery) {
            result.get("steps").get(0).remove("no-recovery");
        }
        if (null == noTxSeparatePool) {
            result.get("steps").get(0).remove("no-tx-separate-pool");
        }
        if (null == recoveryPluginClassName) {
            result.get("steps").get(0).remove("recovery-plugin-class-name");
        }
        if (null == staleConnectionCheckerClassName) {
            result.get("steps").get(0).remove("stale-connection-checker-class-name");
        }

        return result;
    }
}
