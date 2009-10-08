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

import javax.management.MBeanRegistration;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import mazz.i18n.Logger;
import org.rhq.enterprise.communications.i18n.CommI18NFactory;

/**
 * The superclass for all services that are to be deployed as MBeans. This class itself is not a valid MBean - there is
 * no MBean interface. Subclasses need to provide their own MBean interface. This class simply provides some convienent
 * lifecycle methods.
 *
 * @author John Mazzitelli
 */
public abstract class CommandMBean implements MBeanRegistration {
    /**
     * The MBeanServer where this command service is registered.
     */
    private MBeanServer m_mbs;

    /**
     * The object name this command service is registered under.
     */
    private ObjectName m_objectName;

    /**
     * A log for subclasses to be able to use.
     */
    private Logger m_log;

    /**
     * Creates a new {@link CommandMBean} object.
     */
    public CommandMBean() {
        m_mbs = null;
        m_objectName = null;
        m_log = CommI18NFactory.getLogger(this.getClass());

        return;
    }

    /**
     * @see MBeanRegistration#preRegister(MBeanServer, ObjectName)
     */
    public ObjectName preRegister(MBeanServer mbs, ObjectName name) throws Exception {
        m_mbs = mbs;
        m_objectName = name;

        return name;
    }

    /**
     * @see javax.management.MBeanRegistration#postRegister(java.lang.Boolean)
     */
    public void postRegister(Boolean arg0) {
        startService();

        return;
    }

    /**
     * @see javax.management.MBeanRegistration#preDeregister()
     */
    public void preDeregister() throws Exception {
        stopService();

        return;
    }

    /**
     * @see javax.management.MBeanRegistration#postDeregister()
     */
    public void postDeregister() {
        m_mbs = null;
        m_objectName = null;

        return;
    }

    /**
     * Returns the MBeanServer where this service is registered; will be <code>null</code> if not registered anywhere.
     *
     * @return the MBeanServer where this MBean is registered
     */
    public MBeanServer getMBeanServer() {
        return m_mbs;
    }

    /**
     * Returns the name of this MBean service as it is registered under; will be <code>null</code> if not registered in
     * an MBeanServer.
     *
     * @return the object name that this MBean is registered under
     */
    public ObjectName getObjectName() {
        return m_objectName;
    }

    /**
     * Returns a logger that can be used to log messages under the instance's own category.
     *
     * @return a logger whose category is the name of this object's implementation class
     */
    public Logger getLog() {
        return m_log;
    }

    /**
     * This is called after the MBean is fully registered with the MBeanServer. This implementation is a no-op that
     * subclasses are free to override.
     */
    public void startService() {
        return;
    }

    /**
     * This is called when the MBean is being deregistered from the MBeanServer. This implementation is a no-op that
     * subclasses are free to override.
     */
    public void stopService() {
        return;
    }
}