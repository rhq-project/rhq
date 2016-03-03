/*
 * RHQ Management Platform
 * Copyright (C) 2005-2015 Red Hat, Inc.
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
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA
 */
package org.rhq.modules.plugins.wildfly10.helper;

import org.w3c.dom.Node;

/**
 * 
 * @author lzoubek
 *
 */
public class TruststoreConfig {

    public static TruststoreConfig fromXmlNode(Node node) {
        if (node == null) {
            return null;
        }

        Node attrAlias = node.getAttributes().getNamedItem("alias");
        Node attrPath = node.getAttributes().getNamedItem("path");
        Node attrKeystorePass = node.getAttributes().getNamedItem("keystore-password");
        Node attrKeyPass = node.getAttributes().getNamedItem("keys-password");

        return new TruststoreConfig(
            attrAlias != null ? attrAlias.getNodeValue() : null,
            attrPath != null ? attrPath.getNodeValue() : null,
            attrKeystorePass != null ? attrKeystorePass.getNodeValue() : null,
            attrKeyPass != null ? attrKeyPass.getNodeValue() : null);
    }

    private TruststoreConfig(String alias, String path, String keystorePassword, String keyPassword) {
        this.alias = alias;
        this.path = path;
        this.keystorePassword = keystorePassword;
        this.keyPassword = keyPassword;
    }

    private final String alias;
    private final String path;
    private final String keystorePassword;
    private final String keyPassword;

    public String getAlias() {
        return alias;
    }

    public String getPath() {
        return path;
    }

    public String getKeystorePassword() {
        return keystorePassword;
    }

    public String getKeyPassword() {
        return keyPassword;
    }

}
