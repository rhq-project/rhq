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

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Semaphore;
import mazz.i18n.Logger;
import org.rhq.enterprise.communications.command.Command;
import org.rhq.enterprise.communications.command.CommandResponse;
import org.rhq.enterprise.communications.command.client.CommandPreprocessor;
import org.rhq.enterprise.communications.command.server.CommandListener;
import org.rhq.enterprise.communications.i18n.CommI18NFactory;
import org.rhq.enterprise.communications.i18n.CommI18NResourceKeys;
import org.rhq.enterprise.communications.util.ConcurrencyManager.Permit;
import org.rhq.enterprise.communications.util.NotPermittedException;

/**
 * This is a listener for commands coming into the {@link ServiceContainer}'s {@link CommandPreprocessor} and will
 * immediately drop the command if the global concurrent limit has been exceeded. In other words, if we've received too
 * many incoming commands at the same time, we'll start dropping additional incoming commands under the command
 * processor can catch up.
 *
 * @author John Mazzitelli
 */
public class GlobalConcurrencyLimitCommandListener implements CommandListener {
    public static final String CONCURRENCY_LIMIT_NAME = "rhq.communications.global-concurrency-limit-semaphore";

    private static final Logger LOG = CommI18NFactory.getLogger(GlobalConcurrencyLimitCommandListener.class);

    private final ServiceContainer serviceContainer;
    private final ConcurrentLinkedQueue<Permit> permitsObtained;
    private final Semaphore droppedCommands;

    public GlobalConcurrencyLimitCommandListener(ServiceContainer serviceContainer) {
        this.serviceContainer = serviceContainer;
        this.permitsObtained = new ConcurrentLinkedQueue<Permit>();
        this.droppedCommands = new Semaphore(0);
    }

    /**
     * This will be called for every command coming in. We'll check to see if we are permitted to process this command -
     * if we've reached the global concurrency limit, this method will throw a {@link NotPermittedException}.
     *
     * @see CommandListener#receivedCommand(Command)
     */
    public void receivedCommand(Command command) {
        try {
            Permit permit = this.serviceContainer.getConcurrencyManager().getPermit(CONCURRENCY_LIMIT_NAME);
            this.permitsObtained.add(permit);
        } catch (NotPermittedException npe) {
            LOG.debug(CommI18NResourceKeys.COMMAND_NOT_PERMITTED, command, npe.getSleepBeforeRetry());
            this.droppedCommands.release(); // adds 1 to the semaphore count so we know we dropped another command
            throw npe; // command processor will get this and abort the command
        }
    }

    /**
     * This is called for every command that finished which includes all commands that succeeded, failed or was not
     * permitted due to {@link #receivedCommand(Command)} throwing a {@link NotPermittedException}.
     *
     * @see CommandListener#processedCommand(Command, CommandResponse)
     */
    public void processedCommand(Command command, CommandResponse response) {
        // The droppedCommands is a semaphore so we can use it for its thread-safety.
        // If there are 1 or more droppedCommand permits, it means we dropped commands
        // (number of commands dropped were the number of permits in the droppedCommands semaphore).
        // If we can acquire a droppedCommands permit, it means this listener needs to finish processing
        // dropped commands and thus we should not release a concurrency manager permit (since
        // we only add to permitsObtained when we got a concurrency manager permit).
        // If we cannot acquire a droppedCommands permit, it means this listener has no more
        // dropped commands to process and we can start releasing concurrency manager permits.
        if (!this.droppedCommands.tryAcquire()) {
            // Notice that we don't care which permit we release, just get the head of the queue
            // and release it. Permits are not directly associated with a command - concurrency
            // manager just cares about the number of permits, not which commands go with which permits.
            // Note also that if the concurrency manager happened to be swapped out between the time
            // we got the permit and now, this release request will be ignored and nothing will break
            // since permits are associated with the concurrency manager that granted them and giving a
            // permit to a concurrency manager that didn't grant it will simply be a no-op.
            this.serviceContainer.getConcurrencyManager().releasePermit(this.permitsObtained.poll());
        }
    }
}