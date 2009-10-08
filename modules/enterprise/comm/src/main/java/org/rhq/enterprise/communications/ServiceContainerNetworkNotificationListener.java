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
package org.rhq.enterprise.communications;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.management.Notification;
import javax.management.NotificationListener;
import mazz.i18n.Logger;
import org.jboss.remoting.InvokerLocator;
import org.jboss.remoting.ident.Identity;
import org.jboss.remoting.network.NetworkNotification;
import org.rhq.enterprise.communications.command.server.discovery.AutoDiscoveryListener;
import org.rhq.enterprise.communications.i18n.CommI18NFactory;
import org.rhq.enterprise.communications.i18n.CommI18NResourceKeys;

/**
 * This is the listener that, on behalf of the Service Container, will listen and process notifications indicating new
 * servers have come online and gone offline.
 *
 * @author John Mazzitelli
 */
class ServiceContainerNetworkNotificationListener implements NotificationListener {
    /**
     * Logger
     */
    private static final Logger LOG = CommI18NFactory.getLogger(ServiceContainerNetworkNotificationListener.class);

    /**
     * The list of listeners that will receive notifications when new servers are discovered or old servers have died.
     */
    private List<AutoDiscoveryListener> m_discoveryListeners = new ArrayList<AutoDiscoveryListener>();

    /**
     * The map of all the servers and their connectors that have been discovered. By "server" it is meant a server VM
     * that hosts one or more Connectors. The key is the {@link Identity} that identifies the "server" and the values
     * are a <code>List</code> of {@link InvokerLocator} objects.
     */
    private Map<Identity, List<InvokerLocator>> m_discoveredServers = new HashMap<Identity, List<InvokerLocator>>();

    /**
     * Used for its monitor lock to limit access to the other data fields.
     */
    private Object m_lock = new Object();

    /**
     * Enables the given listener to receive notifications when servers come online and go offline.
     *
     * @param listener
     */
    public void add(AutoDiscoveryListener listener) {
        synchronized (m_lock) {
            m_discoveryListeners.add(listener);
        }
    }

    /**
     * Removes the given listener from the list of listeners this object will send notifications to. The given listener
     * will no longer receive notifications when servers come online and go offline.
     *
     * @param listener
     */
    public void remove(AutoDiscoveryListener listener) {
        synchronized (m_lock) {
            m_discoveryListeners.remove(listener);
        }
    }

    /**
     * Removes all listeners from this object. Once this returns, this object will not notify any object when servers
     * come online and go offline.
     */
    public void removeAll() {
        synchronized (m_lock) {
            m_discoveryListeners.clear();
        }
    }

    /**
     * When a new server is detected or an old server goes away, this method will be notified. This method notifies all
     * listeners when added/removed/updated notifications are received.
     *
     * @see NotificationListener#handleNotification(Notification, Object)
     */
    public void handleNotification(Notification notification, Object handback) {
        // check to see if its a network notification
        if (notification instanceof NetworkNotification) {
            LOG.debug(CommI18NResourceKeys.GOT_NOTIF, notification.getType());

            NetworkNotification networkNotification = (NetworkNotification) notification;

            if (NetworkNotification.SERVER_ADDED.equals(networkNotification.getType())) {
                serverAddedNotification(networkNotification);
            } else if (NetworkNotification.SERVER_REMOVED.equals(networkNotification.getType())) {
                serverRemovedNotification(networkNotification);
            } else if (NetworkNotification.SERVER_UPDATED.equals(networkNotification.getType())) {
                serverUpdatedNotification(networkNotification);
            }
        }

        return;
    }

    /**
     * Processes the {@link NetworkNotification#SERVER_ADDED} notification. This notification means a new "server VM"
     * has come online and has one or more remote endpoints in it (the remote endpoints are specified by their
     * {@link InvokerLocator} objects).
     *
     * @param networkNotification the notification
     */
    private void serverAddedNotification(NetworkNotification networkNotification) {
        List<AutoDiscoveryListener> listeners_copy;
        InvokerLocator[] notif_locators = networkNotification.getLocator();

        synchronized (m_lock) {
            // add the new server to the map of discovered servers (we shouldn't have seen this before, but assume we might have)
            List<InvokerLocator> server_invokers = m_discoveredServers.get(networkNotification.getIdentity());
            if (server_invokers == null) {
                // add the new server to our map of known servers and give it an empty list of invokers that we will fill
                server_invokers = new ArrayList<InvokerLocator>(networkNotification.getLocator().length);
                m_discoveredServers.put(networkNotification.getIdentity(), server_invokers);
            }

            // go through each locator in the notification and add it to the known invokers for our new server
            for (int x = 0; x < notif_locators.length; x++) {
                server_invokers.add(notif_locators[x]);
            }

            // make a copy while we are synchronized - then release the lock so we let other notifications to get processed
            listeners_copy = new ArrayList<AutoDiscoveryListener>(m_discoveryListeners);
        }

        // notify all of our listeners of the new invoker locators
        for (int x = 0; x < notif_locators.length; x++) {
            notifyListenersOnline(listeners_copy, notif_locators[x]);
        }

        return;
    }

    /**
     * Processes the {@link NetworkNotification#SERVER_REMOVED} notification. This notification means a previously known
     * "server VM" has gone offline and has taken all of its remote endpoints with it.
     *
     * @param networkNotification the notification
     */
    private void serverRemovedNotification(NetworkNotification networkNotification) {
        List<AutoDiscoveryListener> listeners_copy;
        InvokerLocator[] notif_locators = networkNotification.getLocator();

        synchronized (m_lock) {
            // the server has gone down, remove it from our map of known servers
            m_discoveredServers.remove(networkNotification.getIdentity());

            // make a copy while we are synchronized - then release the lock so we let other notifications to get processed
            listeners_copy = new ArrayList<AutoDiscoveryListener>(m_discoveryListeners);
        }

        // notify all of our listeners of the invoker locators that have gone down
        for (int x = 0; x < notif_locators.length; x++) {
            notifyListenersOffline(listeners_copy, notif_locators[x]);
        }

        return;
    }

    /**
     * Processes the {@link NetworkNotification#SERVER_UPDATED} notification. This notification means a previously known
     * "server VM" has either added a new remote endpoint or removed an old remote endpoint.
     *
     * @param networkNotification the notification
     */
    private void serverUpdatedNotification(NetworkNotification networkNotification) {
        List<AutoDiscoveryListener> listeners_copy;
        List<InvokerLocator> new_invokers = new ArrayList<InvokerLocator>();
        List<InvokerLocator> dead_invokers = new ArrayList<InvokerLocator>();
        InvokerLocator[] notif_locators = networkNotification.getLocator();

        synchronized (m_lock) {
            // get the updated server's list of currently known invokers
            // we should have seen this server before, but assume we might not have
            List<InvokerLocator> server_invokers = m_discoveredServers.get(networkNotification.getIdentity());

            // compare the known list of invokers with the updated invokers in the notification.
            // any found in the notification that aren't known yet are to be considered having gone online
            for (int i = 0; i < notif_locators.length; i++) {
                InvokerLocator cur_notif_locator = notif_locators[i];
                if (!server_invokers.contains(cur_notif_locator)) {
                    // haven't see this invoker before, its new so let's add it to our list
                    new_invokers.add(cur_notif_locator);
                    server_invokers.add(cur_notif_locator);
                }
            }

            // compare the known list of invokers with the updated invokers in the notification.
            // any not found in the notification that are known are to be considered having gone offline
            List notif_locators_list = Arrays.asList(notif_locators);
            for (Iterator iter = server_invokers.iterator(); iter.hasNext();) {
                InvokerLocator cur_server_invoker = (InvokerLocator) iter.next();
                if (!notif_locators_list.contains(cur_server_invoker)) {
                    dead_invokers.add(cur_server_invoker); // this invoker that used to be online is now down
                }
            }

            // since we can't modify the server_invokers list while we were iterating, we have to do it here
            // in a separate loop iterating the dead invokers list
            for (Iterator iter = dead_invokers.iterator(); iter.hasNext();) {
                InvokerLocator dead_invoker = (InvokerLocator) iter.next();
                server_invokers.remove(dead_invoker);
            }

            // make a copy while we are synchronized - then release the lock so we let other notifications to get processed
            listeners_copy = new ArrayList<AutoDiscoveryListener>(m_discoveryListeners);
        }

        // notify our listeners of those servers that have come online and gone offline
        for (Iterator iter = new_invokers.iterator(); iter.hasNext();) {
            InvokerLocator new_invoker = (InvokerLocator) iter.next();
            notifyListenersOnline(listeners_copy, new_invoker);
        }

        for (Iterator iter = dead_invokers.iterator(); iter.hasNext();) {
            InvokerLocator dead_invoker = (InvokerLocator) iter.next();
            notifyListenersOffline(listeners_copy, dead_invoker);
        }

        return;
    }

    /**
     * Notifies the list of listeners that a new server invoker is online. This is given a local copy of our listeners
     * (i.e. this is a list of all the listeners that will be notified, but is a copy of the
     * {@link #m_discoveryListeners} so we will not need to synchronize on its access)
     *
     * @param listeners_copy
     * @param new_server_invoker_locator
     */
    private void notifyListenersOnline(List listeners_copy, InvokerLocator new_server_invoker_locator) {
        LOG.debug(CommI18NResourceKeys.SERVICE_CONTAINER_NETWORK_NOTIF_LISTENER_SERVER_ONLINE,
            new_server_invoker_locator);

        // notify each of our listeners that a new invoker is now online
        for (Iterator iter = listeners_copy.iterator(); iter.hasNext();) {
            AutoDiscoveryListener listener = (AutoDiscoveryListener) iter.next();
            try {
                listener.serverOnline(new_server_invoker_locator);
            } catch (Throwable t) {
                LOG.error(t, CommI18NResourceKeys.SERVICE_CONTAINER_NETWORK_NOTIF_LISTENER_ONLINE_PROCESSING_FAILURE,
                    new_server_invoker_locator);
            }
        }

        return;
    }

    /**
     * Notifies the list of listeners that a known server invoker has gone offline. This is given a local copy of our
     * listeners (i.e. this is a list of all the listeners that will be notified, but is a copy of the
     * {@link #m_discoveryListeners} so we will not need to synchronize on its access)
     *
     * @param listeners_copy
     * @param dead_server_invoker_locator
     */
    private void notifyListenersOffline(List listeners_copy, InvokerLocator dead_server_invoker_locator) {
        LOG.debug(CommI18NResourceKeys.SERVICE_CONTAINER_NETWORK_NOTIF_LISTENER_SERVER_OFFLINE,
            dead_server_invoker_locator);

        // notify each of our listeners that an invoker is now offline
        for (Iterator iter = listeners_copy.iterator(); iter.hasNext();) {
            AutoDiscoveryListener listener = (AutoDiscoveryListener) iter.next();
            try {
                listener.serverOffline(dead_server_invoker_locator);
            } catch (Throwable t) {
                LOG.error(t, CommI18NResourceKeys.SERVICE_CONTAINER_NETWORK_NOTIF_LISTENER_OFFLINE_PROCESSING_FAILURE,
                    dead_server_invoker_locator);
            }
        }

        return;
    }
}