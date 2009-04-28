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
package org.jboss.on.plugins.jbossOsgi.JBossOSGi;

import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jetbrains.annotations.NotNull;

import org.rhq.core.pluginapi.event.EventPoller;
import org.rhq.core.domain.event.Event;

/**
 * TODO document me
 *
 * @author Heiko W. Rupp
 */
public class OsgiEventPoller implements EventPoller{

    private final Log log = LogFactory.getLog(OsgiEventPoller.class);

    public OsgiEventPoller() {

    }


    public void tearDown() {

    }

    @NotNull
    public String getEventType() {
        return JBossOsgiServerComponent.EVENT_TYPE_LOG;
    }

    public Set<Event> poll() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
