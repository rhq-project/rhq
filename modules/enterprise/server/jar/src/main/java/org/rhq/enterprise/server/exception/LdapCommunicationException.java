package org.rhq.enterprise.server.exception;

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

/*
* This exception should be used whenever there was an error while trying to communicate 
* with an ldap server.  
* @author Simeon Pinder */

public class LdapCommunicationException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public LdapCommunicationException() {
        super();
    }

    public LdapCommunicationException(String message) {
        super(message);
    }

    public LdapCommunicationException(Throwable cause) {
        super(cause);
    }

    public LdapCommunicationException(String message, Throwable cause) {
        super(message, cause);
    }

}
