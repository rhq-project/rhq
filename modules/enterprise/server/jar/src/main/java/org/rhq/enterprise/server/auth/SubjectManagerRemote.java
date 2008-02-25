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
package org.rhq.enterprise.server.auth;

import javax.jws.WebParam;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;
import javax.security.auth.login.LoginException;
import javax.xml.bind.annotation.XmlSeeAlso;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.configuration.PropertyList;
import org.rhq.core.domain.configuration.PropertyMap;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;

@SOAPBinding(style = SOAPBinding.Style.DOCUMENT)
@WebService
@XmlSeeAlso( { PropertySimple.class, PropertyList.class, PropertyMap.class })
public interface SubjectManagerRemote {
    /**
     * Logs a user into the system. This will authenticate the given user with the given password. If the user was
     * already logged in, the current session will be used but the password will still need to be authenticated.

     * @param     username The name of the user.
     * @param     password The password.
     *
     * @return    The subject of the authenticated user.
     *
     * @exception LoginException if the login failed for some reason
     */
    Subject login(@WebParam(name = "username")
    String username, @WebParam(name = "password")
    String password) throws LoginException;

    /**
     * Logs out a user.
     *
     * @param sessionId The session id for the current user
     */

    void logout(@WebParam(name = "sessionId")
    int sessionId);

    /**
     * Loads in the given subject's {@link Subject#getUserConfiguration() configuration}.
     *
     * @param  subjectId identifies the subject whose user configuration is to be loaded
     *
     * @return the subject, with its user configuration loaded
     */
    Subject loadUserConfiguration(@WebParam(name = "subjectId")
    Integer subjectId);

    /**
     * Returns a list all subjects in the system, excluding internal system users.
     *
     * @param  pageControl A page control object.
     *
     * @return the list of subjects paged with the given page control
     */
    PageList<Subject> getAllSubjects(@WebParam(name = "pageControl")
    PageControl pc);
}