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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.URL;
import java.security.KeyException;
import java.util.ArrayList;

import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import redstone.xmlrpc.XmlRpcClient;

/**
 * @author pkilambi
 * 
 */
public class RHNConnector {

    private String certificateFileName;
    private String certificateText;
    private String systemid;
    private XmlRpcClient client;
    private final Log log = LogFactory.getLog(RHNConnector.class);

    /**
     * RHN Connector Constructor
     * @param systemidIn systemId file path
     * @param certificateIn certificate file path
     * @param serverUrlIn hosted server url as a string
     * 
     */
    public RHNConnector(String systemidIn, String certificateIn, String serverUrlIn)
        throws Exception {
        this.certificateFileName = certificateIn;

        URL serverUrl = new URL(serverUrlIn);
        client = new XmlRpcClient(serverUrl, true);

        File systemid_file = new File(systemidIn);
        this.systemid = FileUtils.readFileToString(systemid_file);
    }

    /**
     * RHN Connector Constructor
     * @param certificateTextIn certificate text as a string
     * @param serverUrlIn hosted serverUrl
     * 
     */
    public RHNConnector(String certificateTextIn, String serverUrlIn) throws Exception {
        this.certificateText = certificateTextIn;

        // store the file to local server
        this.writeStringToFile();
        URL serverUrl = new URL(serverUrlIn);
        client = new XmlRpcClient(serverUrl, true);

        File systemid_file = new File(RHNConstants.DEFAULT_SYSTEM_ID);
        this.systemid = FileUtils.readFileToString(systemid_file);
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

        this.client.invoke("satellite.activateSatellite", params);

    }

    /**
     * Call that invokes the server object and passing in the xmlrpc
     * exposed call to deactivate the rhq server.
     *
     */
    public void processDeActivation() throws Exception {
        ArrayList<String> params = new ArrayList<String>();
        params.add(this.systemid);

        this.client.invoke("satellite.deactivateSatellite", params);

        // this.deleteCertTempFile(this.certificateFileName);
    }

    public PublicKeyRing readDefaultKeyRing()
        throws ClassNotFoundException, KeyException, IOException {
        InputStream keyringStream = new FileInputStream(RHNConstants.DEFAULT_WEBAPP_GPG_KEY_RING);
        return new PublicKeyRing(keyringStream);
    }

    /**
     * Stores the certificate text string as a file on the filesystem
     *
     */
    protected void writeStringToFile() throws Exception {
        String tmpDir = System.getProperty("java.io.tmpdir");

        this.certificateFileName = tmpDir + "/rhn-entitlement-cert" + ".cert";

        FileOutputStream out = new FileOutputStream(this.certificateFileName);
        PrintStream printer = new PrintStream(out);
        try {
            printer.println(this.certificateText);
        }
        finally {
            printer.close();
        }
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

    public static void main(String[] args) {
        // commandline test
        if (args.length > 0) {

            String systemid = args[0];
            String cert = args[1];
            String serverUrl = "http://satellite.rhn.redhat.com/rpc/api";
            try {
                RHNConnector rhqServer = new RHNConnector(systemid, cert, serverUrl);
                rhqServer.processActivation();
                System.out.println("Activation Complete");
                rhqServer.processDeActivation();
                System.out.println("De-Activation Complete");

            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }

    }
}
