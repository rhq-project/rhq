 /*
  * RHQ Management Platform
  * Copyright (C) 2005-2008 Red Hat, Inc.
  * All rights reserved.
  *
  * This program is free software; you can redistribute it and/or modify
  * it under the terms of the GNU General Public License, version 2, as
  * published by the Free Software Foundation, and/or the GNU Lesser
  * General Public License, version 2.1, also as published by the Free
  * Software Foundation.
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
  * GNU General Public License and the GNU Lesser General Public License
  * for more details.
  *
  * You should have received a copy of the GNU General Public License
  * and the GNU Lesser General Public License along with this program;
  * if not, write to the Free Software Foundation, Inc.,
  * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
  */
package org.rhq.core.gui.util;

/**
 * A factory for JSF component id's within the context of a particular managed bean.
 */
public interface FacesComponentIdFactory {
    /**
     * A prefix implementors of this interface can use for the id's they create.
     */
    String UNIQUE_ID_PREFIX = "jon_id";

    /**
     * Creates a unique id that can be assigned to a new JSF {@link javax.faces.component.UIComponent}. Note, JSF id's
     * can only contain letters, numbers, dashes, or underscores.
     *
     * <p/>For more information on how to create JSF id's, see http://forum.java.sun.com/thread.jspa?threadID=524925.
     *
     * @return a unique id that can be assigned to a new JSF {@link javax.faces.component.UIComponent}
     */
    String createUniqueId();
}