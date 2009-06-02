/*
 * RHQ Management Platform
 * Copyright (C) 2005-2009 Red Hat, Inc.
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
package org.rhq.plugins.jbossas5.serviceBinding;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.managed.api.ManagedComponent;
import org.jboss.managed.api.ManagedProperty;
import org.jboss.managed.api.RunState;
import org.jboss.metatype.api.types.MetaType;
import org.jboss.metatype.api.values.MetaValue;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.plugins.jbossas5.ManagedComponentComponent;
import org.rhq.plugins.jbossas5.ProfileServiceComponent;

/**
 * Manager component for the Service Binding Manager.
 *
 * @author Heiko W. Rupp
 */
public class ManagerComponent extends ManagedComponentComponent {

    private final Log log = LogFactory.getLog(ManagerComponent.class);
   ResourceContext<ProfileServiceComponent> context ;

    @Override
    public AvailabilityType getAvailability() {
        RunState runState = getManagedComponent().getRunState();
        return (runState == RunState.RUNNING || runState == RunState.UNKNOWN) ? AvailabilityType.UP :
                AvailabilityType.DOWN;
    }

   @Override
   public void start(ResourceContext<ProfileServiceComponent> resourceContext) throws Exception {
      this.context = resourceContext;
      super.start(resourceContext);    // TODO: Customise this generated block
   }

    @Override
    public Configuration loadResourceConfiguration() {

        ManagedComponent comp = getManagedComponent();
        ManagedProperty bindingSets = comp.getProperty("bindingSets");
       MetaValue val = bindingSets.getValue();
       MetaType type = val.getMetaType();


      return super.loadResourceConfiguration();    // TODO: Customise this generated block
   }
}
