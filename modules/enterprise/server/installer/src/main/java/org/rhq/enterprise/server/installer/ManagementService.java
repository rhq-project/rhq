/*
 * RHQ Management Platform
 * Copyright (C) 2005-2012 Red Hat, Inc.
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
package org.rhq.enterprise.server.installer;

import org.jboss.as.controller.client.ModelControllerClient;

/**
 * @author John Mazzitelli
 */
public class ManagementService {

    /**
     * The caller should call ModelControllerClient.close() when finished with the client.
     * 
     * @return the ModelControllerClient
     */
    public static ModelControllerClient getClient() {
        try {
            return ModelControllerClient.Factory.create("127.0.0.1", 9999);
        } catch (Exception e) {
            throw new RuntimeException("Cannot obtain client connection to the app server", e);
        }
    }
}
