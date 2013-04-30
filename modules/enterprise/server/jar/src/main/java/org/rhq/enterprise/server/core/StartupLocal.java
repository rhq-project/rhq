/*
 * RHQ Management Platform
 * Copyright (C) 2005-2013 Red Hat, Inc.
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

package org.rhq.enterprise.server.core;

import javax.ejb.Local;

@Local
public interface StartupLocal {
    /**
     * When false is returned, it means the startup bean has not finished initializing the server.
     * Only when true is returned can we be assured that the server has been fully started and initialized, ready to accept
     * agent requests and user requests from browser/remote clients.
     * 
     * @return true if the startup bean has fully initialized the server
     */
    boolean isInitialized();

    /**
     * Tells the startup bean to do its work. This will finish the initialization of the server.
     */
    void init();
}
