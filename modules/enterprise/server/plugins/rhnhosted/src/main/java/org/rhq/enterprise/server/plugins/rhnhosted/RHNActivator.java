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

package org.rhq.enterprise.server.plugins.rhnhosted;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.KeyException;
import java.util.ArrayList;

import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.enterprise.server.plugins.rhnhosted.certificate.PublicKeyRing;
import org.rhq.enterprise.server.plugins.rhnhosted.xmlrpc.XmlRpcExecutor;
import org.rhq.enterprise.server.plugins.rhnhosted.xmlrpc.XmlRpcExecutorFactory;

/**
 * @author pkilambi
 * 
 */
public class RHNActivator {

    private String certificateText;
    private String systemid;
    private XmlRpcExecutor client;
    private final Log log = LogFactory.getLog(RHNActivator.class);

    /**
     * Constructor
     * @param systemIdString xml systemid
     * @param certificateTextIn certificate file path
     * @param serverUrlIn hosted server url as a string
     * @throws Exception
     */
    public RHNActivator(String systemIdString, String certificateTextIn, String serverUrlIn) throws Exception {

        this.certificateText = certificateTextIn;
        this.systemid = systemIdString;
        URL serverUrl = new URL(serverUrlIn);
        client = newClient(serverUrl);

    }

    /**
     * RHN Connector Constructor
     * @param certificateTextIn certificate text as a string
     * @param serverUrlIn hosted serverUrl
     * 
     */
    public RHNActivator(String certificateTextIn, String serverUrlIn) throws Exception {
        this.certificateText = certificateTextIn;

        URL serverUrl = new URL(serverUrlIn);
        client = newClient(serverUrl);
        File systemid_file = new File(RHNConstants.DEFAULT_SYSTEM_ID);
        this.systemid = FileUtils.readFileToString(systemid_file);
    }

    protected XmlRpcExecutor newClient(URL serverUrl) {
        return XmlRpcExecutorFactory.getClient(serverUrl.toString());
    }

    /**
     * Call that invokes the server object and passing in the xmlrpc
     * exposed call to activate the rhq server.
     *
     */
    public void processActivation() throws Exception {

        ArrayList<String> params = new ArrayList<String>();
        params.add(this.systemid);
        params.add(this.certificateText);

        this.client.execute("satellite.activateSatellite", params);
        log.info("Activation successful");
    }

    /**
     * Call that invokes the server object and passing in the xmlrpc
     * exposed call to deactivate the rhq server.
     *
     */
    public void processDeActivation() throws Exception {
        ArrayList<String> params = new ArrayList<String>();
        params.add(this.systemid);

        this.client.execute("satellite.deactivateSatellite", params);
        log.info("Activation successful");

        // this.deleteCertTempFile(this.certificateFileName);
    }

    public PublicKeyRing readDefaultKeyRing() throws ClassNotFoundException, KeyException, IOException {
        InputStream keyringStream = new FileInputStream(RHNConstants.DEFAULT_WEBAPP_GPG_KEY_RING);
        return new PublicKeyRing(keyringStream);
    }

    /**
     * Delete the certificate file from the filesystem.
     * @param fileName certificate filename
     * @return boolean returns delete operation status
     *
     */
    protected boolean deleteCertTempFile(String fileName) {
        File f = new File(fileName);
        return f.delete();
    }

    public static void main(String[] args) throws Exception {
        // commandline test
        if (args.length > 0) {

            String systemId = FileUtils.readFileToString(new File(args[0]));
            String cert = FileUtils.readFileToString(new File(args[1]));

            String serverUrl = "http://satellite.rhn.redhat.com/rpc/api";
            try {
                RHNActivator rhqServer = new RHNActivator(systemId, cert, serverUrl);
                rhqServer.processActivation();
                System.out.println("Activation Complete");
                rhqServer.processDeActivation();
                System.out.println("De-Activation Complete");

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }
}
