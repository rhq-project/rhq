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
package org.rhq.enterprise.communications.command.param;

import java.io.Serializable;

/**
 * Class used to encapsulate information about how clients should render parameters when displaying them as an unordered
 * list.
 *
 * @author <a href="mazz@jboss.com">John Mazzitelli</a>
 */
public class UnorderedListRenderingInformation extends ParameterRenderingInformation implements Serializable {
    /**
     * the UID to identify the serializable version of this class
     */
    private static final long serialVersionUID = 1L;

    /**
     * @see ParameterRenderingInformation#ParameterRenderingInformation()
     */
    public UnorderedListRenderingInformation() {
        super();
    }

    /**
     * @see ParameterRenderingInformation#ParameterRenderingInformation(String, String)
     */
    public UnorderedListRenderingInformation(String labelKey, String descriptionKey) {
        super(labelKey, descriptionKey);
    }
}