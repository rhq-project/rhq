/*
 * RHQ Management Platform
 * Copyright (C) 2005-2016 Red Hat, Inc.
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
package org.rhq.enterprise.agent;

import java.io.File;

/**
 * Provides a convenient method to create a SecureConnector using an
 * AgentConfiguration object.
 */
public class SecureConnectorFactory {

    SecureConnectorFactory() {

    }

    public SecureConnector getInstanceWithAgentConfiguration(AgentConfiguration config, String agentHomeDirectory) {
        String secureSocketProtocol = config.getClientSenderSecuritySocketProtocol();
        SecureConnector secureConnector;
        if (config.isClientSenderSecurityServerAuthMode()) {
            AgentKeystore agentKeystore;
            {
                File file = new File(config.getClientSenderSecurityKeystoreFile());
                if (!file.isAbsolute()) {
                    file = new File(agentHomeDirectory, file.getPath());
                }
                String password = config.getClientSenderSecurityKeystorePassword();
                String keyPassword = config.getClientSenderSecurityKeystoreKeyPassword();
                String algorithm = config.getClientSenderSecurityKeystoreAlgorithm();
                String alias = config.getClientSenderSecurityKeystoreAlias();
                String type = config.getClientSenderSecurityKeystoreType();
                agentKeystore = new AgentKeystore(file, type, alias, algorithm, password, keyPassword);
            }

            AgentTrustStore agentTrustStore;
            {
                File file = new File(config.getClientSenderSecurityTruststoreFile());
                if (!file.isAbsolute()) {
                    file = new File(agentHomeDirectory, file.getPath());
                }
                String password = config.getClientSenderSecurityTruststorePassword();
                String type = config.getClientSenderSecurityTruststoreType();
                String algorithm = config.getClientSenderSecurityTruststoreAlgorithm();
                agentTrustStore = new AgentTrustStore(file, type, algorithm, password);
            }

            secureConnector = new SecureConnector(secureSocketProtocol, agentTrustStore, agentKeystore);
        } else {
            secureConnector = new SecureConnector(secureSocketProtocol);
        }
        return secureConnector;
    }

}
