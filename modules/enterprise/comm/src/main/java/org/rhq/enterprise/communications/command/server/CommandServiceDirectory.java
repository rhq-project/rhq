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
package org.rhq.enterprise.communications.command.server;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.management.MBeanRegistration;
import javax.management.MBeanServer;
import javax.management.MBeanServerInvocationHandler;
import javax.management.MBeanServerNotification;
import javax.management.Notification;
import javax.management.NotificationListener;
import javax.management.ObjectName;
import org.rhq.enterprise.communications.command.CommandType;
import org.rhq.enterprise.communications.i18n.CommI18NResourceKeys;

/**
 * An MBean that maintains a directory of all currently deployed {@link CommandService} MBean services for all
 * subsystems. In effect, it provides a service that allows a client to determine what MBean service provides support
 * and can execute a particular type of command.
 *
 * <p>Each set of command services are organized into separate subsystems. A command service belongs to a subsystem if,
 * in its <code>ObjectName</code>, it has the {@link KeyProperty#SUBSYSTEM subsystem} key property specified. If it does
 * not specify that key property in its name, it is considered in the unnamed, anonymous subsystem.</p>
 *
 * <p>This directory service is used by the {@link CommandProcessor} invocation handler to help determine where to
 * direct a command request for processing.</p>
 *
 * <p>This directory is part of the mechanism by which command services can be dynamically added/updated/removed. This
 * service will listen for command services getting deployed and undeployed to/from the MBeanServer and will dynamically
 * update itself accordingly.</p>
 *
 * @author John Mazzitelli
 */
public class CommandServiceDirectory extends CommandMBean implements CommandServiceDirectoryMBean, NotificationListener {
    /**
     * This is a special subsystem identifier that is used as a key into our directory map. It is used to denote the
     * unnamed, anonymous subsystem name. Note that it has special characters to make sure that it can never be part of
     * a JMX <code>ObjectName</code> (this is to avoid a deployer unknowingly naming his subsystem the same as our
     * special null name). The unnamed, anonymous subsystem is really identified by a <code>null</code> subsystem name.
     * Package scope so {@link CommandServiceDirectoryEntry} can use this.
     */
    static final String NULL_SUBSYSTEM = ",null,";

    /**
     * Contains a map of all supported command types and their associated command services - this is "the directory".
     * This map is keyed on subsystem string - the values of the map are maps themselves. Each subsystem has its own map
     * of command types/command services. Each inner subsystem map is keyed on {@link CommandType command type} with
     * each value being the <code>ObjectName</code> of the {@link CommandServiceMBean} that provides support for the
     * command type. Note that this object is used as a monitor lock when needing to synchronize access to the
     * directory.
     */
    private Map<String, Map<CommandType, ObjectName>> m_allCommandTypes;

    /**
     * If <code>true</code>, any new command service MBean that gets registered will be added to the directory; any
     * current command service MBean that gets deregistered will be removed from the directory. If <code>false</code>,
     * only an initial inventory of the current set of registered command service MBeans will be added to the directory.
     */
    private boolean m_allowDynamicDiscovery;

    /**
     * Creates a new {@link CommandServiceDirectory} object.
     */
    public CommandServiceDirectory() {
        m_allowDynamicDiscovery = false;
        m_allCommandTypes = new HashMap<String, Map<CommandType, ObjectName>>();
    }

    /**
     * Verifies that the <code>ObjectName</code> of this directory MBean has the appropriate key properties and
     * initializes the directory with an inventory of the current set of supported command types/command services.
     *
     * @see MBeanRegistration#preRegister(MBeanServer, ObjectName)
     */
    public ObjectName preRegister(MBeanServer mbs, ObjectName name) throws Exception {
        if (!KeyProperty.TYPE_DIRECTORY.equals(name.getKeyProperty(KeyProperty.TYPE))) {
            String errorMsg = getLog().getMsgString(CommI18NResourceKeys.COMMAND_SERVICE_DIRECTORY_INVALID_SELF_NAME,
                name, KeyProperty.TYPE, KeyProperty.TYPE_DIRECTORY);
            throw new IllegalArgumentException(errorMsg);
        }

        return super.preRegister(mbs, name);
    }

    /**
     * Clears the directory of all entries then takes an inventory of existing command services and adds them to the
     * directory.
     *
     * @throws RuntimeException
     */
    public void startService() {
        try {
            // lock out all others to prevent new command registrations from getting added until we finish the inventory
            synchronized (m_allCommandTypes) {
                m_allCommandTypes.clear();

                // remember the original value - in case it was previously configured, we want to restore it
                boolean discoveryFlagBackup = m_allowDynamicDiscovery;

                // we are ready to start accepting (un)register notifications to dynamically detect commands being (un)deployed
                // the notification handler will deal with the check for permission for dynamic discovery
                // note that we locked m_allCommandTypes so any incoming notifications will block until we finish inventory
                startListening();

                // take an initial inventory of the command services already deployed
                m_allowDynamicDiscovery = true;
                inventory();

                // by default, we do not allow dynamic discovery for security purposes
                m_allowDynamicDiscovery = discoveryFlagBackup;
            }
        } catch (Exception e) {
            throw new RuntimeException(getLog().getMsgString(
                CommI18NResourceKeys.COMMAND_SERVICE_DIRECTORY_FAILED_TO_START), e);
        }

        return;
    }

    /**
     * Clean up any resources that were initialized during start.
     */
    public void stopService() {
        synchronized (m_allCommandTypes) {
            stopListening();
            m_allCommandTypes.clear();
        }

        return;
    }

    /**
     * As command services are deployed and undeployed, this notification handler will detect this and update the
     * directory accordingly.
     *
     * <p>This method ensures thread-safety during its modifications to the directory data structures.</p>
     *
     * <p>If the directory is not {@link #getAllowDynamicDiscovery() allowed to perform dynamic discovery}, this method
     * does nothing; the notification is ignored.</p>
     *
     * @see NotificationListener#handleNotification(Notification, Object)
     */
    public void handleNotification(Notification notification, Object handback) {
        MBeanServerNotification mbsNotif = null;

        if (notification instanceof MBeanServerNotification) {
            mbsNotif = (MBeanServerNotification) notification;

            // synchronize now so we only have to do it once, any mods done to the directory
            // are thread-safe from this point on
            synchronized (m_allCommandTypes) {
                if (MBeanServerNotification.REGISTRATION_NOTIFICATION.equals(mbsNotif.getType())) {
                    addAllSupportedCommandTypes(mbsNotif.getMBeanName());
                } else if (MBeanServerNotification.UNREGISTRATION_NOTIFICATION.equals(mbsNotif.getType())) {
                    removeAllSupportedCommandTypes(mbsNotif.getMBeanName());
                } else {
                    mbsNotif = null;
                }
            }
        }

        if (mbsNotif == null) {
            getLog().warn(
                getLog().getMsgString(CommI18NResourceKeys.COMMAND_SERVICE_DIRECTORY_UNKNOWN_NOTIF, notification));
        }

        return;
    }

    /**
     * Finds the provider of the given command type by looking it up in the directory in a thread-safe way.
     *
     * @see CommandServiceDirectoryMBean#getCommandTypeProvider(String, CommandType)
     */
    public CommandServiceDirectoryEntry getCommandTypeProvider(String subsystem, CommandType commandType) {
        CommandServiceDirectoryEntry retEntry = null;
        ObjectName commandServiceName = null;

        synchronized (m_allCommandTypes) {
            Map subsystemServices = getSubsystemCommandTypes(subsystem);
            commandServiceName = (ObjectName) subsystemServices.get(commandType);
        }

        if (commandServiceName != null) {
            retEntry = new CommandServiceDirectoryEntry(subsystem, commandType, commandServiceName);
        }

        return retEntry;
    }

    /**
     * @see CommandServiceDirectoryMBean#getSubsystemEntries(String)
     */
    public CommandServiceDirectoryEntry[] getSubsystemEntries(String subsystem) {
        List<CommandServiceDirectoryEntry> entries = new ArrayList<CommandServiceDirectoryEntry>();

        synchronized (m_allCommandTypes) {
            Map subsystemServices = getSubsystemCommandTypes(subsystem);
            for (Iterator iter = subsystemServices.entrySet().iterator(); iter.hasNext();) {
                Map.Entry mapEntry = (Map.Entry) iter.next();
                entries.add(new CommandServiceDirectoryEntry(subsystem, (CommandType) mapEntry.getKey(),
                    (ObjectName) mapEntry.getValue()));
            }
        }

        return entries.toArray(new CommandServiceDirectoryEntry[entries.size()]);
    }

    /**
     * @see CommandServiceDirectoryMBean#getAllEntries()
     */
    public CommandServiceDirectoryEntry[] getAllEntries() {
        List<CommandServiceDirectoryEntry> entries = new ArrayList<CommandServiceDirectoryEntry>();

        synchronized (m_allCommandTypes) {
            for (Iterator iter = m_allCommandTypes.keySet().iterator(); iter.hasNext();) {
                String subsystem = (String) iter.next();
                CommandServiceDirectoryEntry[] subsystemEntries = getSubsystemEntries(subsystem);
                for (int i = 0; i < subsystemEntries.length; i++) {
                    entries.add(subsystemEntries[i]);
                }
            }
        }

        return entries.toArray(new CommandServiceDirectoryEntry[entries.size()]);
    }

    /**
     * @see CommandServiceDirectoryMBean#setAllowDynamicDiscovery(boolean)
     */
    public void setAllowDynamicDiscovery(boolean flag) {
        if (flag) {
            getLog().info(CommI18NResourceKeys.COMMAND_SERVICE_DIRECTORY_DYNAMIC_DISCOVERY_ALLOWED);
        }

        m_allowDynamicDiscovery = flag;
    }

    /**
     * @see CommandServiceDirectoryMBean#getAllowDynamicDiscovery()
     */
    public boolean getAllowDynamicDiscovery() {
        return m_allowDynamicDiscovery;
    }

    /**
     * Starts listening to the MBeanServer for (un)register notifications. This method registers this object as a
     * notification listener on the MBeanServer delegate.
     *
     * @throws Exception if failed for some reason to register as a listener on the MBeanServer delegate
     */
    private void startListening() throws Exception {
        ObjectName delegate = new ObjectName("JMImplementation:type=MBeanServerDelegate");

        getMBeanServer().addNotificationListener(delegate, this, null, null);

        return;
    }

    /**
     * Stops this MBean from listening for (un)register notifications. This method should be called during the
     * deregistration of this object.
     */
    private void stopListening() {
        try {
            ObjectName delegate = new ObjectName("JMImplementation:type=MBeanServerDelegate");

            getMBeanServer().removeNotificationListener(delegate, this);
        } catch (Exception e) {
            // ignore this to allow for us to continue deregistering; this exception should never occur anyway
            getLog().warn(e, CommI18NResourceKeys.COMMAND_SERVICE_DIRECTORY_LISTENER_REMOVAL_FAILURE);
        }

        return;
    }

    /**
     * This takes inventory of the command services already deployed in the <code>MBeanServer</code>. All command
     * services deployed in all subsystems will be stored in this directory, mapped to their supported command types.
     *
     * @throws Exception if failed to obtain the full inventory of command services
     */
    private void inventory() throws Exception {
        // an MBean matches our query if its name follows the pattern: *:*,type=command
        // the returned set will contain all the object names of all command service MBeans in any subsystem
        String type = KeyProperty.TYPE + "=" + KeyProperty.TYPE_COMMAND;
        ObjectName query = new ObjectName("*:*," + type);
        Set commandServices = getMBeanServer().queryNames(query, null);

        // for each command service, send a registration notification to simluate the new service getting deploy
        // this will add it to the directory immediately
        for (Iterator iter = commandServices.iterator(); iter.hasNext();) {
            ObjectName newCommandServiceName = (ObjectName) iter.next();
            MBeanServerNotification notif = new MBeanServerNotification(
                MBeanServerNotification.REGISTRATION_NOTIFICATION, this, 0L, newCommandServiceName);
            handleNotification(notif, null);
        }

        return;
    }

    /**
     * Given a command service <code>ObjectName</code>, this will return the subsystem in which the command service
     * belongs.
     *
     * @param  commandServiceName the name of the command service
     *
     * @return the name of the subsystem
     */
    private String getSubsystem(ObjectName commandServiceName) {
        String retSubsystem = commandServiceName.getKeyProperty(KeyProperty.SUBSYSTEM);

        if (retSubsystem == null) {
            retSubsystem = NULL_SUBSYSTEM;
        }

        return retSubsystem;
    }

    /**
     * Returns <code>true</code> if the given <code>ObjectName</code> matches that of a command service. See
     * {@link KeyProperty#TYPE_COMMAND}.
     *
     * @param  name the name of the MBean to test
     *
     * @return <code>true</code> if it appears that the <code>name</code> belongs to a command service, <code>
     *         false</code> otherwise
     */
    private boolean isCommandService(ObjectName name) {
        return (KeyProperty.TYPE_COMMAND.equals(name.getKeyProperty(KeyProperty.TYPE)));
    }

    /**
     * This method invokes a JMX call to the named command service to retrieve its array of all supported
     * {@link CommandType command types} - that array is then returned. Because this actually invokes a JMX call on the
     * given MBean <code>name</code>, that MBean must actually be registered and available (i.e. this method cannot be
     * called if we know the MBean is being deregistered or has been deregistered).
     *
     * <p>Note that if <code>name</code> does not identify a {@link CommandServiceMBean command service}, this method
     * simply returns <code>null</code> (i.e. no exception is thrown).</p>
     *
     * @param  name the <code>ObjectName</code> of the command service
     *
     * @return array of all {@link CommandType}s supported by the command service, or <code>null</code> if the given
     *         <code>name</code> does not correspond to a command service.
     */
    private CommandType[] getCommandServiceCommands(ObjectName name) {
        CommandType[] retComandTypes = null;

        try {
            CommandServiceMBean proxy = (CommandServiceMBean) MBeanServerInvocationHandler.newProxyInstance(
                getMBeanServer(), name, CommandServiceMBean.class, false);
            retComandTypes = proxy.getSupportedCommandTypes();
        } catch (Exception e) {
            // ignore, "name" does not correspond to a command service
            // log at trace mainly for debugging purposes in case we want to see the exception
            getLog().trace(CommI18NResourceKeys.EXCEPTION, e);
        }

        return retComandTypes;
    }

    /**
     * Given a subsystem, this will return a map of all supported command types for that subsystem and the command
     * services that provide them.
     *
     * <p>The returned map is keyed on {@link CommandType command type} and the values are the command service <code>
     * ObjectName</code> s.</p>
     *
     * <p>To find out the command service that provides a given command type, look up the
     * {@link CommandType command type} as the key and get the name of the command service that can execute commands of
     * that type.</p>
     *
     * <p><code>subsystem</code> may be <code>null</code> to denote the unnamed, anonymous subsystem.</p>
     *
     * <p>This method is not thread-safe.</p>
     *
     * @param  subsystem the subsystem map to obtain (may be <code>null</code>)
     *
     * @return map of all supported command types and their command services in the given subsystem
     */
    private Map<CommandType, ObjectName> getSubsystemCommandTypes(String subsystem) {
        if (subsystem == null) {
            subsystem = NULL_SUBSYSTEM;
        }

        Map<CommandType, ObjectName> retSubsystemMap;

        retSubsystemMap = m_allCommandTypes.get(subsystem);

        if (retSubsystemMap == null) {
            retSubsystemMap = new HashMap<CommandType, ObjectName>();
            m_allCommandTypes.put(subsystem, retSubsystemMap);
        }

        return retSubsystemMap;
    }

    /**
     * If the given subsystem no longer has any support command types, it is purged from the directory. If the given
     * subsystem still has one or more supported command types, this method does nothing.
     *
     * <p>This method is not thread-safe.</p>
     *
     * @param subsystem the subsystem to check and purge if empty of supported command types
     */
    private void removeSubsystemCommandTypesIfEmpty(String subsystem) {
        if (subsystem == null) {
            subsystem = NULL_SUBSYSTEM;
        }

        Map subsystemMap = m_allCommandTypes.get(subsystem);
        if (subsystemMap.size() == 0) {
            m_allCommandTypes.remove(subsystem);
        }

        return;
    }

    /**
     * Adds a command type/command service pair to the directory.
     *
     * <p>This method is not thread-safe.</p>
     *
     * @param commandType        the type of command that is being added to the directory
     * @param commandServiceName the name of the service that provides the command type functionality
     */
    private void addSupportedCommandType(CommandType commandType, ObjectName commandServiceName) {
        String subsystem = getSubsystem(commandServiceName);
        Map<CommandType, ObjectName> subsystemMap = getSubsystemCommandTypes(subsystem);

        subsystemMap.put(commandType, commandServiceName);

        getLog().info(CommI18NResourceKeys.COMMAND_SERVICE_DIRECTORY_NEW_SUPPORTED_COMMAND, commandType,
            commandServiceName);

        return;
    }

    /**
     * Removes a command type/command service pair from the directory.
     *
     * <p>This method is not thread-safe.</p>
     *
     * @param commandType        the type of command that is being removed from the directory
     * @param commandServiceName the name of the service that used to provide the command type functionality
     */
    private void removeSupportedCommandType(CommandType commandType, ObjectName commandServiceName) {
        String subsystem = getSubsystem(commandServiceName);
        Map<CommandType, ObjectName> subsystemMap = getSubsystemCommandTypes(subsystem);
        ObjectName mappedCommandService = subsystemMap.get(commandType);

        if (commandServiceName.equals(mappedCommandService)) {
            // remove the command type, and if no more command types in this subsystem are supported, remove the map itself
            subsystemMap.remove(commandType);

            getLog().info(CommI18NResourceKeys.COMMAND_SERVICE_DIRECTORY_REMOVED_COMMAND_SUPPORT, commandType,
                commandServiceName);

            removeSubsystemCommandTypesIfEmpty(subsystem);
        } else {
            getLog().warn(CommI18NResourceKeys.COMMAND_SERVICE_DIRECTORY_REMOVAL_FAILURE, commandType,
                commandServiceName, mappedCommandService);
        }

        return;
    }

    /**
     * Determines what (if any) command types the given MBean supports and adds them to the directory. The given MBean
     * may or may not be a {@link CommandServiceMBean}; if it is not, this method simply ignores it and does nothing.
     *
     * <p>This method is not thread-safe.</p>
     *
     * @param name the MBean whose supported command types will be added to the directory
     *
     * @see   #isCommandService(ObjectName)
     */
    private void addAllSupportedCommandTypes(ObjectName name) {
        if (isCommandService(name)) {
            if (m_allowDynamicDiscovery) {
                CommandType[] supportedCommandTypes = getCommandServiceCommands(name);

                if (supportedCommandTypes != null) {
                    for (int i = 0; i < supportedCommandTypes.length; i++) {
                        addSupportedCommandType(supportedCommandTypes[i], name);
                    }
                }
            } else {
                getLog().warn(CommI18NResourceKeys.COMMAND_SERVICE_DIRECTORY_DETECTED_BUT_NOT_ADDED, name);
            }
        }

        return;
    }

    /**
     * Determines what (if any) command types the given MBean supports and removes them from the directory. The given
     * MBean may or may not be a {@link CommandServiceMBean}; if it is not, this method simply ignores it and does
     * nothing.
     *
     * <p>This method is not thread-safe.</p>
     *
     * @param name the MBean whose supported command types will be removed from the directory
     *
     * @see   #isCommandService(ObjectName)
     */
    private void removeAllSupportedCommandTypes(ObjectName name) {
        if (isCommandService(name)) {
            String subsystem = getSubsystem(name);
            Map<CommandType, ObjectName> subsystemMap = getSubsystemCommandTypes(subsystem);
            List<CommandType> doomedCommandTypes = new ArrayList<CommandType>();

            // remember which command types that this command service provided
            Set<Map.Entry<CommandType, ObjectName>> subsystemMapEntrySet = subsystemMap.entrySet();
            for (Map.Entry<CommandType, ObjectName> subsystemMapEntry : subsystemMapEntrySet) {
                if (name.equals(subsystemMapEntry.getValue())) {
                    doomedCommandTypes.add(subsystemMapEntry.getKey());
                }
            }

            // remove each command type from the directory
            for (CommandType doomedCommandType : doomedCommandTypes) {
                removeSupportedCommandType(doomedCommandType, name);
            }
        }

        return;
    }
}