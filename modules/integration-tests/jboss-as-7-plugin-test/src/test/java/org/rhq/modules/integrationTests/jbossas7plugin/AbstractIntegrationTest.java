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
package org.rhq.modules.integrationTests.jbossas7plugin;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.codehaus.jackson.JsonNode;

import org.rhq.modules.plugins.jbossas7.ASConnection;
import org.rhq.modules.plugins.jbossas7.ASUploadConnection;
import org.rhq.modules.plugins.jbossas7.json.Operation;
import org.rhq.modules.plugins.jbossas7.json.PROPERTY_VALUE;

/**
 * Abstract base class for integration tests
 * @author Heiko W. Rupp
 */
public abstract class AbstractIntegrationTest {
    protected static final String DC_HOST = "localhost";
    protected static final int DC_HTTP_PORT = 9990;

    String uploadToAs(String deploymentName) throws IOException {
        ASUploadConnection conn = new ASUploadConnection(DC_HOST, DC_HTTP_PORT);
        OutputStream os = conn.getOutputStream(deploymentName);


        URL url = getClass().getClassLoader().getResource(".");
        System.out.println(url);


        InputStream fis = getClass().getClassLoader().getResourceAsStream(deploymentName);
        if (fis==null)
            throw new FileNotFoundException("Input stream for resource [" + deploymentName + "] could not be opened - does the file exist?");
        final byte[] buffer = new byte[1024];
        int numRead = 0;

        while(numRead > -1) {
            numRead = fis.read(buffer);
            if(numRead > 0) {
                os.write(buffer,0,numRead);
            }
        }
        fis.close();
        JsonNode node = conn.finishUpload();
        System.out.println(node);
        assert node != null : "No result from upload - node was null";
        assert node.has("outcome") : "No outcome from upload";
        String outcome = node.get("outcome").getTextValue();
        assert outcome.equals("success") : "Upload was no success" + outcome;

        JsonNode resultNode = node.get("result");
        return resultNode.get("BYTES_VALUE").getTextValue();
    }

    ASConnection getASConnection() {
        ASConnection connection = new ASConnection(DC_HOST, DC_HTTP_PORT);
        return connection;
    }

    Operation addDeployment(String deploymentName,String bytes_value)
    {
        List<PROPERTY_VALUE> deploymentsAddress = new ArrayList<PROPERTY_VALUE>(1);
        deploymentsAddress.add(new PROPERTY_VALUE("deployment", deploymentName));
        Operation op = new Operation("add",deploymentsAddress);
        List<Object> content = new ArrayList<Object>(1);
        Map<String,Object> contentValues = new HashMap<String,Object>();
        contentValues.put("hash",new PROPERTY_VALUE("BYTES_VALUE",bytes_value));
        content.add(contentValues);
        op.addAdditionalProperty("content",content);
        op.addAdditionalProperty("name", deploymentName); // this needs to be unique per upload
        op.addAdditionalProperty("runtime-name", deploymentName);

        return op;
    }
}
