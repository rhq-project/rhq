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

public class AgentKeystore {

    private File file;
    private String type;
    private String alias;
    private String algorithm;
    private String password;
    private String keyPassword;

    public AgentKeystore(File file, String type, String alias, String algorithm, String password, String keyPassword) {
        this.setFile(file);
        this.setAlias(alias);
        this.setType(type);
        this.setAlgorithm(algorithm);
        this.setPassword(password);
        this.setKeyPassword(keyPassword);
    }

    public File getFile() {
        return file;
    }

    public String getType() {
        return type;
    }

    public String getAlias() {
        return alias;
    }

    public String getAlgorithm() {
        return algorithm;
    }

    public String getPassword() {
        return password;
    }

    public String getKeyPassword() {
        return keyPassword;
    }

    public void setFile(File file) {
        this.file = file;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public void setAlgorithm(String algorithm) {
        this.algorithm = algorithm;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setKeyPassword(String keyPassword) {
        this.keyPassword = keyPassword;
    }
}
