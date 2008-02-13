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
    Subject login(String username, String password) throws LoginException;

    void logout(int sessionId);

    Subject loadUserConfiguration(Integer subjectId);

    PageList<Subject> getAllSubjects(PageControl pc);
}