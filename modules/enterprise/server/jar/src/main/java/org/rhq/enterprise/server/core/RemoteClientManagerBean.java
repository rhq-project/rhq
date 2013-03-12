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
package org.rhq.enterprise.server.core;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.Properties;

import javax.ejb.Stateless;
import javax.interceptor.ExcludeDefaultInterceptors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.util.MessageDigestGenerator;
import org.rhq.core.util.file.FileUtil;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * Provides access to the remote client binary distribution (the CLI).
 *
 * @author John Mazzitelli
 */
@Stateless
public class RemoteClientManagerBean implements RemoteClientManagerLocal {
    private final Log log = LogFactory.getLog(RemoteClientManagerBean.class);

    // constants used for the cli version file
    private static final String RHQ_SERVER_VERSION = "rhq-server.version";
    private static final String RHQ_SERVER_BUILD_NUMBER = "rhq-server.build-number";
    private static final String RHQ_CLIENT_MD5 = "rhq-client.md5";

    @ExcludeDefaultInterceptors
    @Override
    public File getRemoteClientVersionFile() throws Exception {
        File versionFile = new File(getDataDownloadDir(), "rhq-client-version.properties");
        File zip = getRemoteClientBinaryFile();
        Boolean needVersionFile = FileUtil.isNewer(zip, versionFile);
        if (needVersionFile == null || needVersionFile.booleanValue()) {
            // we do not have the version properties file yet or it must be regenerated, let's extract some info and create one
            StringBuilder serverVersionInfo = new StringBuilder();

            CoreServerMBean coreServer = LookupUtil.getCoreServer();
            serverVersionInfo.append(RHQ_SERVER_VERSION + '=').append(coreServer.getVersion()).append('\n');
            serverVersionInfo.append(RHQ_SERVER_BUILD_NUMBER + '=').append(coreServer.getBuildNumber()).append('\n');

            // calculate the MD5 of the client zip
            log.info("Remote Client Binary File: " + zip.getAbsolutePath());

            String md5Property = RHQ_CLIENT_MD5 + '=' + MessageDigestGenerator.getDigestString(zip) + '\n';

            // now write the server version info in our internal version file our servlet will use
            FileOutputStream versionFileOutputStream = new FileOutputStream(versionFile);
            try {
                versionFileOutputStream.write(serverVersionInfo.toString().getBytes());
                versionFileOutputStream.write(md5Property.getBytes());
            } finally {
                try {
                    versionFileOutputStream.close();
                } catch (Exception ignore) {
                }
            }

            log.info("Remote Client Version File: " + versionFile);
        }

        return versionFile;
    }

    @ExcludeDefaultInterceptors
    @Override
    public Properties getRemoteClientVersionFileContent() throws Exception {
        FileInputStream stream = new FileInputStream(getRemoteClientVersionFile());
        try {
            Properties props = new Properties();
            props.load(stream);
            return props;
        } finally {
            try {
                stream.close();
            } catch (Exception e) {
            }
        }
    }

    @ExcludeDefaultInterceptors
    @Override
    public File getRemoteClientBinaryFile() throws Exception {
        File downloadDir = getDownloadDir();
        for (File file : downloadDir.listFiles()) {
            if (file.getName().endsWith(".zip")) {
                return file;
            }
        }

        throw new FileNotFoundException("Missing CLI binary in [" + downloadDir + "]");
    }

    /**
     * The directory on the server's file system where the CLI binary file is found.
     *
     * @return directory where the downloads are found
     *
     * @throws Exception if could not determine the location or it does not exist
     */
    private File getDownloadDir() throws Exception {
        File earDir = LookupUtil.getCoreServer().getEarDeploymentDir();
        File downloadDir = new File(earDir, "rhq-downloads/rhq-client");
        if (!downloadDir.isDirectory()) {
            throw new FileNotFoundException("Missing remote client download directory at [" + downloadDir + "]");
        }
        return downloadDir;
    }

    /**
     * The directory on the server's file system where the CLI version file is found.
     *
     * @return directory where the version file is found
     *
     * @throws Exception if could not determine the location or it does not exist
     */
    private File getDataDownloadDir() throws Exception {
        File earDir = LookupUtil.getCoreServer().getJBossServerDataDir();
        File downloadDir = new File(earDir, "rhq-downloads/rhq-client");
        if (!downloadDir.isDirectory()) {
            downloadDir.mkdirs();
            if (!downloadDir.isDirectory()) {
                throw new FileNotFoundException("Missing remote client data download directory [" + downloadDir + "]");
            }
        }
        return downloadDir;
    }
}