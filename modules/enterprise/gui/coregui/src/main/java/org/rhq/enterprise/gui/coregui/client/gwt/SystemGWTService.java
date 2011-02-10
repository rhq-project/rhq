/*
 * RHQ Management Platform
 * Copyright (C) 2010 Red Hat, Inc.
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
package org.rhq.enterprise.gui.coregui.client.gwt;

import java.util.HashMap;

import com.google.gwt.user.client.rpc.RemoteService;

import org.rhq.core.domain.common.ProductInfo;
import org.rhq.core.domain.common.ServerDetails;

/**
 * Provides information about the server as well as a way to reconfigure parts of the server.
 * 
 * @author John Mazzitelli
 * @author Ian Springer
 */
public interface SystemGWTService extends RemoteService {

    ProductInfo getProductInfo() throws RuntimeException;

    ServerDetails getServerDetails() throws RuntimeException;

    HashMap<String, String> getSystemConfiguration() throws RuntimeException;

    void setSystemConfiguration(HashMap<String, String> properties, boolean skipValidation) throws RuntimeException;
}
