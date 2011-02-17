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

package org.rhq.bindings;

import java.io.FilePermission;
import java.lang.reflect.ReflectPermission;
import java.net.SocketPermission;
import java.security.Permission;
import java.security.PermissionCollection;
import java.security.Permissions;
import java.util.Enumeration;
import java.util.PropertyPermission;

/**
 * @author Lukas Krejci
 */
public class StandardScriptPermissions extends PermissionCollection {

    private static final long serialVersionUID = 1L;

    private Permissions perms = new Permissions();
    
    /**
     * Creates a new instance with the default set of permissions
     * already added. 
     */
    public StandardScriptPermissions() {
        //the JBoss specific perms that must be set
        add(new RuntimePermission("org.jboss.security.SecurityAssociation.getPrincipalInfo"));
        add(new RuntimePermission("org.jboss.security.SecurityAssociation.setPrincipalInfo "));
        add(new RuntimePermission("org.jboss.security.SecurityAssociation.setServer"));
        add(new RuntimePermission("org.jboss.security.SecurityAssociation.setRunAsRole"));
        
        //JVM defined runtime perms
        add(new RuntimePermission("getenv.*"));
        add(new RuntimePermission("getProtectionDomain"));
        add(new RuntimePermission("getFileSystemAttributes"));
        add(new RuntimePermission("readFileDescriptor"));
        add(new RuntimePermission("writeFileDescriptor"));
        add(new RuntimePermission("accessDeclaredMembers"));
        add(new RuntimePermission("queuePrintJob"));
        add(new RuntimePermission("getStackTrace"));
        add(new RuntimePermission("preferences"));
        
        //allow the scripts to connect via sockets
        add(new SocketPermission("*", "connect,accept"));
        
        //allow access to the server's file system. let the file perms 
        //guard what is writeable and what is not.
        add(new FilePermission("<<ALL FILES>>", "read,write,execute,delete"));
        
        //we don't suppose the serverside scripts to be malevolent, so let's
        //give them the read access to the system properties.
        add(new PropertyPermission("*", "read"));
        
        add(new ReflectPermission("suppressAccessChecks"));
    }
    
    public void add(Permission permission) {
        perms.add(permission);
    }

    public boolean implies(Permission permission) {
        return perms.implies(permission);
    }

    public Enumeration<Permission> elements() {
        return perms.elements();
    }

    public boolean isReadOnly() {
        return perms.isReadOnly();
    }

    public void setReadOnly() {
        perms.setReadOnly();
    }
}
