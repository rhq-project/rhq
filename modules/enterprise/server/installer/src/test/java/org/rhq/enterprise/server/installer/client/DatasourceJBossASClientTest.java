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
package org.rhq.enterprise.server.installer.client;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;

import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.testng.annotations.Test;

import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.OperationMessageHandler;
import org.jboss.dmr.ModelNode;

import org.rhq.common.jbossas.client.controller.DatasourceJBossASClient;
import org.rhq.common.jbossas.client.controller.JBossASClient;
import org.rhq.enterprise.server.installer.ServerInstallUtil;
import org.rhq.enterprise.server.installer.ServerProperties;

@Test
public class DatasourceJBossASClientTest {
    private static final ModelNode mockSuccess;
    static {
        mockSuccess = new ModelNode();
        mockSuccess.get(JBossASClient.OUTCOME).set(JBossASClient.OUTCOME_SUCCESS);
    }

    public void testCreateDatasourcesDMR() throws Exception {
        ModelControllerClient mcc = mock(ModelControllerClient.class);

        // note that this doesn't test actually creating anything - it just tests the DMR can be parsed successfully
        when(mcc.execute(any(ModelNode.class), any(OperationMessageHandler.class))).thenAnswer(new Answer<ModelNode>() {
            public ModelNode answer(InvocationOnMock invocation) throws Throwable {
                System.out.println("~~~~~~~\n" + invocation.getArguments()[0]);
                ModelNode retNode = mockSuccess.clone();
                ModelNode resultNode = retNode.get(JBossASClient.RESULT);
                resultNode.get(DatasourceJBossASClient.DATA_SOURCE).setEmptyList();
                resultNode.get(DatasourceJBossASClient.XA_DATA_SOURCE).setEmptyList();
                return retNode;
            }
        });

        HashMap<String, String> serverProperties = new HashMap<String, String>();
        serverProperties.put(ServerProperties.PROP_DATABASE_TYPE, "Oracle");
        ServerInstallUtil.createNewDatasources(mcc, serverProperties);

        serverProperties.put(ServerProperties.PROP_DATABASE_TYPE, "PostgreSQL");
        ServerInstallUtil.createNewDatasources(mcc, serverProperties);
    }

    public void testCreateNewDatasourceDMR() throws Exception {
        ModelControllerClient mcc = mock(ModelControllerClient.class);

        // note that this doesn't test actually creating anything - it just tests the DMR can be parsed successfully
        when(mcc.execute(any(ModelNode.class), any(OperationMessageHandler.class))).thenReturn(mockSuccess);

        DatasourceJBossASClient client = new DatasourceJBossASClient(mcc);

        HashMap<String, String> connProps = new HashMap<String, String>(2);
        connProps.put("char.encoding", "UTF-8");
        connProps.put("SetBigStringTryClob", "true");

        ModelNode request = client.createNewDatasourceRequest("NoTxRHQDS", 30000,
            "${rhq.server.database.connection-url:jdbc:oracle:thin:@127.0.0.1:1521:rhq}", "oracle",
            "org.jboss.jca.adapters.jdbc.extensions.oracle.OracleExceptionSorter", 15, false, 2, 5, 75,
            "RHQDSSecurityDomain", "org.jboss.jca.adapters.jdbc.extensions.oracle.OracleStaleConnectionChecker",
            "TRANSACTION_READ_COMMITTED", "org.jboss.jca.adapters.jdbc.extensions.oracle.OracleValidConnectionChecker",
            true, connProps);

        System.out.println("==============\n" + request);

        ModelNode results = client.execute(request);
        assert JBossASClient.isSuccess(results);
    }

    public void testCreateNewXADatasourceDMR() throws Exception {
        ModelControllerClient mcc = mock(ModelControllerClient.class);

        // note that this doesn't test actually creating anything - it just tests the DMR can be parsed successfully
        when(mcc.execute(any(ModelNode.class), any(OperationMessageHandler.class))).thenReturn(mockSuccess);

        DatasourceJBossASClient client = new DatasourceJBossASClient(mcc);

        HashMap<String, String> xaDSProps = new HashMap<String, String>(2);
        xaDSProps.put("URL", "${rhq.server.database.connection-url:jdbc:oracle:thin:@127.0.0.1:1521:rhq}");
        xaDSProps.put("ConnectionProperties", "SetBigStringTryClob=true");

        ModelNode request = client.createNewXADatasourceRequest("RHQDS", 30000, "oracle",
            "oracle.jdbc.xa.client.OracleXADataSource",
            "org.jboss.jca.adapters.jdbc.extensions.oracle.OracleExceptionSorter", 15, 2, 5, (Boolean) null,
            Boolean.TRUE, 75, (String) null, "RHQDSSecurityDomain",
            "org.jboss.jca.adapters.jdbc.extensions.oracle.OracleStaleConnectionChecker", "TRANSACTION_READ_COMMITTED",
            "org.jboss.jca.adapters.jdbc.extensions.oracle.OracleValidConnectionChecker", xaDSProps);

        System.out.println("==============\n" + request);

        ModelNode results = client.execute(request);
        assert JBossASClient.isSuccess(results);
    }
}
