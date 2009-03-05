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
package org.rhq.enterprise.client;

/**
 * @author Greg Hinkle
 */
public class OperationCommands {


    public static void invokeOperation(ClientMain client, int resourceId, String operationName) {

       /* OperationManagerLocal man = client.getRemoteClient().getOperationManager();
        SimpleTrigger trigger = new SimpleTrigger();

            // not deferred, so execute immediately
            Date now = new Date(System.currentTimeMillis());
            trigger.setStartTime(now);
        ResourceOperationSchedule schedule = man.scheduleResourceOperation(client.getSubject(), resourceId, operationName, null, trigger, "invoked from command line");

        client.getPrintWriter().println("Operation scheduled");
*/

    }


}