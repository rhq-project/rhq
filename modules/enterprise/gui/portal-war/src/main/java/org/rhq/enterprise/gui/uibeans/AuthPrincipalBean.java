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
package org.rhq.enterprise.gui.uibeans;

/**
 * Simple validation bean used for storing data during validation of auth related bean properties.
 */
public class AuthPrincipalBean {
    public AuthPrincipalBean() {
    }

    public AuthPrincipalBean(String p, String pw, String pwv) {
        this.principal = p;
        this.password = pw;
        this.passVerify = pwv;
    }

    public String getPrincipal() {
        return principal;
    }

    public void setPrincipal(String principal) {
        this.principal = principal;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    private String principal;
    private String password;
    private String passVerify;

    public String toString() {
        return getPrincipal() + " - " + getPassword() + "-" + getPassVerify();
    }

    public String getPassVerify() {
        return passVerify;
    }

    public void setPassVerify(String passVerify) {
        this.passVerify = passVerify;
    }
}