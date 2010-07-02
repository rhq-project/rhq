/*
 * RHQ Management Platform
 * Copyright (C) 2005-2009 Red Hat, Inc.
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
package org.rhq.core.domain.alert.notification;

import java.util.ArrayList;
import java.util.List;

/**
 * The result of sending an alert notification via AlertSender#send()
 * @author Heiko W. Rupp
 */
public class SenderResult {

    /*
     * the state is UNKNOWN by default, but will be determined as messages are added.  if only success messages 
     * are added, then the result is considered a SUCCESS.  if only failure messages are added, then the result
     * is considered a FAILURE.  if a mixed of success and failure messages are added, then the result is considered
     * PARTIAL.  the user is free to manually set 
     */
    private boolean deferred;
    private String summary; // can be set, will not affect calculated ResultState
    private List<String> successMessages = new ArrayList<String>();
    private List<String> failureMessages = new ArrayList<String>();

    public SenderResult() {
        this.deferred = false;
    }

    public static SenderResult getSimpleSuccess(String message) {
        SenderResult result = new SenderResult();
        result.addSuccessMessage(message);
        return result;
    }

    public static SenderResult getSimpleFailure(String message) {
        SenderResult result = new SenderResult();
        result.addFailureMessage(message);
        return result;
    }

    public static SenderResult getSimpleDeffered(String message) {
        SenderResult result = new SenderResult();
        result.setSummary(message);
        result.setDeffered();
        return result;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getSummary() {
        return summary;
    }

    public ResultState getState() {
        if (deferred) {
            return ResultState.DEFERRED;
        }

        if (successMessages.size() != 0) {
            if (failureMessages.size() == 0) {
                return ResultState.SUCCESS;
            } else {
                return ResultState.PARTIAL;
            }
        } else {
            if (failureMessages.size() != 0) {
                return ResultState.FAILURE;
            }
        }

        return ResultState.UNKNOWN;
    }

    public void setDeffered() {
        deferred = true;
    }

    public void addSuccessMessage(String message) {
        successMessages.add(message);
    }

    public List<String> getSuccessMessages() {
        return successMessages;
    }

    public void addFailureMessage(String message) {
        failureMessages.add(message);
    }

    public List<String> getFailureMessages() {
        return failureMessages;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("SenderResult");
        sb.append("{state=").append(getState());
        sb.append(", summary=").append(summary);
        sb.append(", successMessages=").append(successMessages);
        sb.append(", failureMessages=").append(failureMessages);
        sb.append('}');
        return sb.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj == null) {
            return false;
        }

        if (getClass() != obj.getClass()) {
            return false;
        }

        SenderResult other = (SenderResult) obj;

        if (getState() != other.getState()) {
            return false;
        }

        if (summary == null) {
            if (other.summary != null) {
                return false;
            }
        } else if (!summary.equals(other.summary)) {
            return false;
        }

        if (failureMessages == null) {
            if (other.failureMessages != null) {
                return false;
            }
        } else if (!failureMessages.equals(other.failureMessages)) {
            return false;
        }

        if (successMessages == null) {
            if (other.successMessages != null) {
                return false;
            }
        } else if (!successMessages.equals(other.successMessages)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((summary == null) ? 0 : summary.hashCode());
        result = prime * result + ((failureMessages == null) ? 0 : failureMessages.hashCode());
        result = prime * result + ((successMessages == null) ? 0 : successMessages.hashCode());
        result = prime * result + (getState() != null ? getState().hashCode() : 0);
        return result;
    }

}
