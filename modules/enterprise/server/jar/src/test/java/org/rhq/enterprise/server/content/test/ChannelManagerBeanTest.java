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
package org.rhq.enterprise.server.content.test;

import javax.transaction.TransactionManager;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.content.Channel;
import org.rhq.enterprise.server.content.ChannelManagerLocal;
import org.rhq.enterprise.server.test.AbstractEJB3Test;
import org.rhq.enterprise.server.util.LookupUtil;

@Test
public class ChannelManagerBeanTest extends AbstractEJB3Test {
    private ChannelManagerLocal channelManager;
    private Subject overlord;

    @BeforeMethod
    public void setupBeforeMethod() throws Exception {
        TransactionManager tx = getTransactionManager();
        tx.begin();
        channelManager = LookupUtil.getChannelManagerLocal();
        overlord = LookupUtil.getSubjectManager().getOverlord();
    }

    @AfterMethod
    public void tearDownAfterMethod() throws Exception {
        TransactionManager tx = getTransactionManager();
        if (tx != null) {
            tx.rollback();
        }
    }

    @Test
    public void testCreateDeleteChannel() throws Exception {
        Channel channel = new Channel("testCreateContentSourceChannel");
        int id = channelManager.createChannel(overlord, channel).getId();
        Channel lookedUp = channelManager.getChannel(overlord, id);
        assert lookedUp != null;
        assert id == lookedUp.getId();

        channelManager.deleteChannel(overlord, id);
        lookedUp = channelManager.getChannel(overlord, id);
        assert lookedUp == null;
    }
}