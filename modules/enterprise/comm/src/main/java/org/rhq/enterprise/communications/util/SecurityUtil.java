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
package org.rhq.enterprise.communications.util;

import java.io.File;
import java.util.Arrays;
import mazz.i18n.Logger;
import org.rhq.enterprise.communications.command.impl.start.StartCommand;
import org.rhq.enterprise.communications.command.impl.start.StartCommandResponse;
import org.rhq.enterprise.communications.command.impl.start.server.ProcessExec;
import org.rhq.enterprise.communications.i18n.CommI18NFactory;
import org.rhq.enterprise.communications.i18n.CommI18NResourceKeys;

/**
 * Utilities to help perform security tasks.
 *
 * @author John Mazzitelli
 */
public class SecurityUtil {
    /**
     * Logger
     */
    private static final Logger LOG = CommI18NFactory.getLogger(SecurityUtil.class);

    /**
     * Prevents instantiation.
     */
    private SecurityUtil() {
    }

    /**
     * Given a JBoss/Remoting transport name (such as "sslsocket" or "http") this will return <code>true</code> if that
     * transport is considered secure via SSL.
     *
     * @param  transport the name of the transport protocol (can be the transport only or the entire locator URI)
     *
     * @return <code>true</code> if the transport protocol uses SSL to secure the data, <code>false</code> if not
     */
    public static boolean isTransportSecure(String transport) {
        return (transport.startsWith("ssl") || transport.startsWith("https"));
    }

    /**
     * Creates a keystore and places a new key in it that has the given key information. If the keystore file already
     * exists, this method does nothing. This will only create the key and keystore if the file does not yet exist.
     *
     * <p>If the key password is <code>null</code> or an empty string, it will be set to the same as the keystore
     * password. If the keystore password is <code>null</code>, an exception is thrown.</p>
     *
     * <p>If either the keystore password or key password is not at least 6 characters long, an exception is thrown.</p>
     *
     * <p>If validity is less than or equal to 0, it will default to 100 years.</p>
     *
     * @param  file_path         the path of the keystore file on the file system (if this exists already, this method
     *                           does nothing)
     * @param  key_alias         the alias name of the key that will be generated and placed in the key store
     * @param  domain_name       the domain name of the new key
     * @param  keystore_password the password of the keystore file (must not be <code>null</code>)
     * @param  key_password      the password of the key within the keystore
     * @param  key_algorithm     the algorithm used to generate the new key
     * @param  validity          the number of days the key is valid for
     *
     * @throws RuntimeException if failed to create the keystore file
     */
    public static void createKeyStore(String file_path, String key_alias, String domain_name, String keystore_password,
        String key_password, String key_algorithm, int validity) throws RuntimeException {
        File keystore = new File(file_path);

        // if the keystore already exists, don't do anything - just return immediately
        if (keystore.exists()) {
            LOG.debug(CommI18NResourceKeys.KEYSTORE_EXISTS, keystore);
            return;
        }

        // perform some input parameter validation
        if (validity <= 0) {
            validity = 36500;
        }

        if (keystore_password == null) {
            throw new RuntimeException(LOG.getMsgString(CommI18NResourceKeys.KEYSTORE_PASSWORD_NULL));
        }

        if ((key_password == null) || (key_password.length() == 0)) {
            LOG.debug(CommI18NResourceKeys.KEY_PASSWORD_UNSPECIFIED, keystore);
            key_password = keystore_password;
        }

        if (keystore_password.length() < 6) {
            throw new RuntimeException(LOG.getMsgString(CommI18NResourceKeys.KEYSTORE_PASSWORD_NOT_LONG_ENOUGH));
        }

        if (key_password.length() < 6) {
            throw new RuntimeException(LOG.getMsgString(CommI18NResourceKeys.KEY_PASSWORD_NOT_LONG_ENOUGH));
        }

        // execute the keytool utility to create our key store
        String keytool_dir = System.getProperty("java.home") + File.separator + "bin";
        String keytool_exe = System.getProperty("os.name").toLowerCase().startsWith("windows") ? "keytool.exe"
            : "keytool";
        StartCommand keytool_cmd = new StartCommand();
        keytool_cmd.setProgramDirectory(keytool_dir);
        keytool_cmd.setProgramExecutable(keytool_exe);
        keytool_cmd.setWaitForExit(Long.valueOf(30000L));
        keytool_cmd.setCaptureOutput(Boolean.FALSE);
        keytool_cmd.setCommandInResponse(true);

        keytool_cmd.setArguments(new String[] { "-genkey", "-alias", key_alias, "-dname", domain_name, "-keystore",
            keystore.getAbsolutePath(), "-storepass", keystore_password, "-keypass", key_password, "-keyalg",
            key_algorithm, "-validity", Integer.toString(validity) });

        StartCommandResponse keytool_results = new ProcessExec().execute(keytool_cmd);

        if (!keytool_results.isSuccessful() || !keystore.exists()) {
            throw new RuntimeException(LOG.getMsgString(CommI18NResourceKeys.KEYSTORE_CREATION_FAILURE,
                keytool_results, Arrays.asList(keytool_cmd.getArguments()).toString()), keytool_results.getException());
        }

        LOG.debug(CommI18NResourceKeys.KEYSTORE_CREATED);

        return;
    }
}