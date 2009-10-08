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
package org.rhq.enterprise.communications.command.server.discovery;

import javax.management.InstanceNotFoundException;
import javax.management.ListenerNotFoundException;
import javax.management.MalformedObjectNameException;
import javax.management.Notification;
import javax.management.NotificationListener;
import javax.management.ObjectName;
import org.jboss.remoting.network.NetworkNotification;
import org.rhq.enterprise.communications.ServiceContainer;
import org.rhq.enterprise.communications.command.server.CommandMBean;
import org.rhq.enterprise.communications.i18n.CommI18NResourceKeys;

/**
 * Provides a mechanism to integrate with JBoss/Remoting's Network Registry in order to detect new servers coming on and
 * offline.
 *
 * @author John Mazzitelli
 */
public abstract class AutoDiscovery extends CommandMBean implements AutoDiscoveryMBean, NotificationListener {
    /**
     * this is the default name of the Network Registery MBean
     */
    private static final String DEFAULT_NETWORK_REGISTRY_NAME = ServiceContainer.OBJECTNAME_NETWORK_REGISTRY.toString();

    /**
     * The name of the Network Registry MBean, as an <code>ObjectName</code>.
     */
    private ObjectName m_networkRegistryName = null;

    /**
     * When <code>true</code>, indicates this object is listening to the Network Registry.
     */
    private boolean m_listening = false;

    /**
     * @see AutoDiscoveryMBean#getNetworkRegistryName()
     */
    public String getNetworkRegistryName() {
        return m_networkRegistryName.toString();
    }

    /**
     * @see AutoDiscoveryMBean#setNetworkRegistryName(String)
     */
    public void setNetworkRegistryName(String name) {
        // if there is already a name defined, make sure we are no longer going to listen to that Network Registry
        if (m_networkRegistryName != null) {
            try {
                unregisterNotificationListener();
            } catch (AutoDiscoveryException ade) {
                getLog().warn(ade, CommI18NResourceKeys.FAILED_TO_STOP_LISTENING);
            }
        }

        // create the new Network Registry name
        try {
            m_networkRegistryName = new ObjectName(name);
        } catch (MalformedObjectNameException moe) {
            throw new IllegalArgumentException(getLog().getMsgString(CommI18NResourceKeys.INVALID_REGISTRY_NAME, moe));
        }

        getLog().info(CommI18NResourceKeys.REGISTRY_NAME, name);

        // now start listening to that new Network Registry
        try {
            registerNotificationListener();
        } catch (AutoDiscoveryException ade) {
            getLog().warn(ade, CommI18NResourceKeys.FAILED_TO_START_LISTENING, name);
        }

        return;
    }

    /**
     * Returns the Network Registry Name as an <code>ObjectName</code>.
     *
     * @return the MBean name of the Network Registry
     *
     * @see    AutoDiscoveryMBean#getNetworkRegistryName()
     */
    public ObjectName getNetworkRegistryObjectName() {
        return m_networkRegistryName;
    }

    /**
     * Receives notifications from the Network Registry indicating that servers are either coming on or offline.
     *
     * @see NotificationListener#handleNotification(javax.management.Notification, java.lang.Object)
     */
    public void handleNotification(Notification notification, Object handback) {
        // check to see if it is a network registry notification
        if (notification instanceof NetworkNotification) {
            NetworkNotification networkNotification = (NetworkNotification) notification;
            String notifType = networkNotification.getType();

            // check to see if notification is for new servers being added
            if (NetworkNotification.SERVER_ADDED.equals(notifType)) {
                discoveredNewServers(networkNotification);
            } else if (NetworkNotification.SERVER_REMOVED.equals(notifType)) {
                discoveredRemovedServers(networkNotification);
            } else if (NetworkNotification.SERVER_UPDATED.equals(notifType)) {
                discoveredUpdatedServers(networkNotification);
            } else if (NetworkNotification.DOMAIN_CHANGED.equals(notifType)) {
                discoveredChangedDomain(networkNotification);
            } else {
                getLog().warn(CommI18NResourceKeys.UNKNOWN_NOTIF_TYPE, notifType);
            }
        } else {
            getLog().warn(CommI18NResourceKeys.UNKNOWN_NOTIF, notification.getClass());
        }

        return;
    }

    /**
     * Unregisters this object as a notification listener on the Network Registry.
     */
    public void stopService() {
        try {
            unregisterNotificationListener();
        } catch (Exception e) {
            // ignore this - we don't care, we are shutting down anyway
        }

        return;
    }

    /**
     * Registers this object as a notification listener to the Network Registry. After this call, this object will start
     * getting notifications of servers coming on and offline as detected by the Network Registry.
     *
     * @throws AutoDiscoveryException if failed to start listening to the Network Registry
     */
    private void registerNotificationListener() throws AutoDiscoveryException {
        if (!m_listening) {
            try {
                if (m_networkRegistryName == null) {
                    setNetworkRegistryName(DEFAULT_NETWORK_REGISTRY_NAME);
                }

                getMBeanServer().addNotificationListener(m_networkRegistryName, this, null, null);

                m_listening = true;
            } catch (Exception e) {
                throw new AutoDiscoveryException(getLog().getMsgString(CommI18NResourceKeys.FAILED_TO_START_LISTENING,
                    m_networkRegistryName), e);
            }
        }

        return;
    }

    /**
     * Removes this object as a notification listener from the Network Registry. After this call, this object will no
     * longer get notifications from the Network Registry. Note that if this object is not yet listening to the Network
     * Registry, this method simply returns and does nothing.
     *
     * @throws AutoDiscoveryException if failed to unregister
     */
    private void unregisterNotificationListener() throws AutoDiscoveryException {
        if (m_listening) {
            try {
                getMBeanServer().removeNotificationListener(m_networkRegistryName, this);
            } catch (ListenerNotFoundException e) {
                // ignore this - looks like we weren't registered anyway, so we got what we want - we aren't registered
            } catch (InstanceNotFoundException e) {
                // ignore this - looks like the Network Registry has already gone away
            } catch (Exception e) {
                throw new AutoDiscoveryException(getLog().getMsgString(CommI18NResourceKeys.UNREGISTER_FAILURE,
                    m_networkRegistryName), e);
            }

            m_listening = false;
        }

        return;
    }

    /**
     * Processes a Network Registry notification that indicates new servers have come online.
     *
     * @param networkNotification the notification that was received from the Network Registry
     */
    public abstract void discoveredNewServers(NetworkNotification networkNotification);

    /**
     * Processes a Network Registry notification that indicates servers have gone offline.
     *
     * @param networkNotification the notification that was received from the Network Registry
     */
    public abstract void discoveredRemovedServers(NetworkNotification networkNotification);

    /**
     * Processes a Network Registry notification that indicates servers have been updated.
     *
     * @param networkNotification the notification that was received from the Network Registry
     */
    public abstract void discoveredUpdatedServers(NetworkNotification networkNotification);

    /**
     * Processes a Network Registry notification that indicates the domain has changed.
     *
     * @param networkNotification the notification that was received from the Network Registry
     */
    public abstract void discoveredChangedDomain(NetworkNotification networkNotification);
}