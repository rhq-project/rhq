/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
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
package org.rhq.enterprise.gui.alert.validator;

import javax.faces.application.FacesMessage;
import javax.faces.validator.ValidatorException;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.AutoCreate;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.faces.FacesMessages;

@AutoCreate
@Scope(ScopeType.APPLICATION)
@Name("validatorUtil")
public class AlertValidatorUtil {

    public void error(String message) throws ValidatorException {
        FacesMessage facesMessage = FacesMessages.createFacesMessage(FacesMessage.SEVERITY_ERROR , message);

        throw new ValidatorException(facesMessage);
    }

    public void templateError(String key, Object... params) throws ValidatorException {
        FacesMessage facesMessage = FacesMessages.createFacesMessage(FacesMessage.SEVERITY_ERROR , key, key, params);

        throw new ValidatorException(facesMessage);
    }
}