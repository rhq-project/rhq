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
package org.rhq.plugins.jbossas5.helper;

import org.jboss.managed.api.ComponentType;
import org.jboss.deployers.spi.management.KnownComponentTypes;

/**
 * @author Ian Springer
 */
public interface MoreKnownComponentTypes
{
   /**
    * An enum of additional MBean:* ManagedComponent types not defined in {@link KnownComponentTypes}.
    */
   public enum MBean
   {
      WebApplication,
      Servlet,
      WebApplicationManager;

      public String type()
      {
         return this.getClass().getSimpleName();
      }
      public String subtype()
      {
         return this.name();
      }
      public ComponentType getType()
      {
         return new ComponentType(type(), subtype());
      }
   }
}
