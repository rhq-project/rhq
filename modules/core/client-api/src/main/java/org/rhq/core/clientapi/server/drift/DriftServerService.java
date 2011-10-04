/*
 * RHQ Management Platform
 * Copyright 2011, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
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
package org.rhq.core.clientapi.server.drift;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.rhq.core.communications.command.annotation.Asynchronous;
import org.rhq.core.domain.drift.DriftDefinition;
import org.rhq.core.domain.drift.DriftSnapshot;

/**
 * This class defines the drift server API that the agent uses for things like sending
 * change set reports, sending change set content, and performing inventory sync.
 *
 * @author Jay Shaughnessy
 * @author John Sanda
 */
public interface DriftServerService {

    // note that this guaranteed delivery is weak because most likely the stream will be dead if it
    // doesn't work the first time.
    /**
     * The agent invokes this method to send a change set in a zip file. The agent will call
     * this method to report the initial change set when starting drift detection and
     * subsequently whenever drift is detected. The format of the change set report is
     * described in {@link org.rhq.common.drift.ChangeSetWriter ChangeSetWriter}. This
     * method starts the following work flow:
     * <ul>
     *   <li>
     *     A request is sent to JMS queue so that the change set can be processed
     *     asynchronously allowing control to return back to the agent as quickly as
     *     possible. It also allows the server to be able to more quickly service other
     *     agent and/or web requests.
     *   </li>
     *   <li>The server streams and persists the change set.</li>
     *   <li>
     *     The server sends an acknowledgement to the agent to let it know that the change
     *     set was successfully persisted. The agent will refrain from sending any more
     *     change sets until this step has completed.
     *   </li>
     *   <li>
     *     The server sends a request to the agent for any content referenced in the change
     *     set that is not yet in the database.
     *   </li>
     * </ul>
     *
     * Note that because the change set bits are streamed out of band, if any errors occur
     * during the streaming, exception will not be propagated back up the call stack on the
     * agent side in the thread that initiated the call. This is the reason for the
     * acknowledgement step.
     * 
     * @param resourceId The id of the resource to which this change set belongs. This
     * parameter may obsolete since the resource id is specified in the change set headers.
     * @param zipSize The total number of bytes to be streamed
     * @param zipStream A RemoteInputStream
     */
    // TODO is the resourceId param needed since it is also included in the change set headers?
    void sendChangesetZip(int resourceId, long zipSize, InputStream zipStream);

    // note that this guaranteed delivery is weak because most likely the stream will be dead if it
    // doesn't work the first time.
    /**
     * The agent invokes this method to send change set content to the server. The content
     * are files referenced in a change set that the agent previously sent to the server.
     * The zip file is assumed to be flat, that is it contains only files, no directories.
     * The name of each file should be the SHA hash of the file contents. File names and
     * paths are not relevant as the server only stores content which is identified by the
     * SHA hashes. This method starts the following work flow:
     * <ul>
     *   <li>
     *     A request is sent to a JMS queue so that the work can be performed asynchronously
     *     allowing control to return back to the agent as quickly as possible. It also
     *     allows the server to more quickly service other agent and/or web requests.
     *   </li>
     *   <li>The server streams and persists each file.</li>
     *   <li>
     *     The server sends an acknowledgement to the agent to let it know that the content
     *     has been successfully persisted.
     *   </li>
     * </ul>
     *
     * Note that that the guaranteed delivery on this method is weak due to the streaming
     * being done asynchronously. If the first attempt at streaming fails the stream will
     * likey be dead and subsequent retries will fail as well. Because the streaming is
     * done out of band, any network IO errors will not be propagated back up the call stack
     * on the agent side in the thread that initiated this method call. This is the reaosn
     * for the acknowledgement step.
     * 
     * @param resourceId The id of the resource to which the change set content belongs
     * @param driftDefName The drift definition name. This is needed for the
     * acknowledgement step.
     * @param token A token needed for the acknowledgement step that allows the agent to
     * uniquely identify the zip file that was sent.
     * @param zipSize The total number of bytes to be streamed.
     * @param zipStream A RemoteStream
     */
    @Asynchronous(guaranteedDelivery = true)
    void sendFilesZip(int resourceId, String driftDefName, String token, long zipSize, InputStream zipStream);

    @Asynchronous
    void repeatChangeSet(int resourceId, String driftDefName, int version);

    Map<Integer, List<DriftDefinition>> getDriftDefinitions(Set<Integer> resourceIds);

    DriftSnapshot getCurrentSnapshot(int driftDefinitionId);

    DriftSnapshot getSnapshot(int driftDefinitionId, int startVersion, int endVersion);

}
