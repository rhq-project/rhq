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
package org.rhq.enterprise.gui.startup;

import org.rhq.core.server.ExternalizableStrategy;
import org.rhq.enterprise.communications.command.Command;
import org.rhq.enterprise.communications.command.CommandResponse;
import org.rhq.enterprise.communications.command.client.CommandPreprocessor;
import org.rhq.enterprise.communications.command.server.CommandListener;
import org.rhq.enterprise.server.safeinvoker.HibernateDetachUtility;

/**
 * This is a listener for commands coming into the {@link ServiceContainer}'s {@link CommandPreprocessor} and will
 * set the ExternalizableStratagy for the command processing thread. This ensures that the proper strategy is applied to 
 * the Return value serialization.
 *
 * @author Jay Shaughnessy
 */
public class ExternalizableStrategyCommandListener implements CommandListener {

    private final String CMDCONFIG_PROP_EXTERNALIZABLE_STRATEGY = "rhq.externalizable-strategy";

    private final ExternalizableStrategy.Subsystem defaultSubsystem;

    public ExternalizableStrategyCommandListener(ExternalizableStrategy.Subsystem defaultSubsystem) {
        this.defaultSubsystem = (null != defaultSubsystem) ? defaultSubsystem : ExternalizableStrategy.Subsystem.AGENT;
    }

    /**
     * This will be called for every command coming in. Set the correct strategy based on the command config. This will
     * be applied to the return serialization.
     *
     * @see CommandListener#receivedCommand(Command)
     */
    public void receivedCommand(Command command) {
        ExternalizableStrategy.Subsystem subsystem = null;

        try {
            subsystem = ExternalizableStrategy.Subsystem.valueOf(command.getConfiguration().getProperty(
                this.CMDCONFIG_PROP_EXTERNALIZABLE_STRATEGY));
        } catch (Exception e) {
            // set to the default if property is not set or invalid 
            subsystem = defaultSubsystem;
        }

        ExternalizableStrategy.setStrategy(subsystem);
    }

    /**
     * This is called for every command that finished. It does nothing since the serialization is complete.
     *
     * @see CommandListener#processedCommand(Command, CommandResponse)
     */
    public void processedCommand(Command command, CommandResponse response) {
        // nothing to do
        //long start = System.currentTimeMillis();
        try {
            HibernateDetachUtility.nullOutUninitializedFields(response, HibernateDetachUtility.SerializationType.SERIALIZATION);
            //System.out.println("HDU: " + (System.currentTimeMillis() - start));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (!(obj instanceof ExternalizableStrategyCommandListener)) {
            return false;
        }

        ExternalizableStrategyCommandListener listener = (ExternalizableStrategyCommandListener) obj;

        return (this.defaultSubsystem == listener.defaultSubsystem);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((this.defaultSubsystem == null) ? 0 : this.defaultSubsystem.hashCode());
        return result;
    }

}