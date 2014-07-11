/*
 * RHQ Management Platform
 * Copyright (C) 2014 Red Hat, Inc.
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
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA
 */

package org.rhq.modules.plugins.jbossas7.util;

import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.map.ObjectMapper;

import org.rhq.modules.plugins.jbossas7.json.Result;

/**
 * @author Lukas Krejci
 * @since 4.13
 */
public final class PatchDetails {
    private static final Log LOG = LogFactory.getLog(PatchDetails.class);

    private final String id;
    private final Type type;
    private final Date appliedAt;

    public static List<PatchDetails> fromHistory(String patchHistoryOutcome) {
        ObjectMapper mapper = new ObjectMapper();

        Result result;
        try {
            result = mapper.readValue(patchHistoryOutcome, Result.class);
        } catch (IOException e) {
            LOG.warn("Failed to parse the output of the 'patch history' command with message '" + e.getMessage() +
                "'. The output was:\n" + patchHistoryOutcome);
            return Collections.emptyList();
        }

        if (!result.isSuccess()) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("'patch history' command didn't succeed: " + result);
            }
            return Collections.emptyList();
        }

        // expecting a list of maps of string->string
        if (!(result.getResult() instanceof List)) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Unexpected patch history results. Expected list but found " +
                    (result.getResult() == null ? "null" : result.getResult().getClass().toString()));
            }

            return Collections.emptyList();
        }

        @SuppressWarnings("unchecked")
        List<Map<String, String>> patches = (List<Map<String, String>>) result.getResult();

        if (patches.isEmpty()) {
            return Collections.emptyList();
        }

        List<PatchDetails> ret = new ArrayList<PatchDetails>();

        for(Map<String, String> patchDescr : patches) {
            String patchId = patchDescr.get("patch-id");
            Date appliedAt = null;
            Type type = Type.fromJsonValue(patchDescr.get("type"));

            try {
                //This seems to be the date format AS are using
                DateFormat format = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);

                appliedAt = format.parse(patchDescr.get("applied-at"));
            } catch (ParseException e) {
                LOG.info("Failed to parse the installation date of the patch " + patchId + ": '" +
                    patchDescr.get("applied-at") + "' with error message: " + e.getMessage());
            }

            ret.add(new PatchDetails(patchId, type, appliedAt));
        }

        return ret;
    }

    public static List<PatchDetails> fromInfo(String patchInfoOutcome, String patchHistoryOutcome) {
        ObjectMapper mapper = new ObjectMapper();

        Result result;
        try {
            result = mapper.readValue(patchInfoOutcome, Result.class);
        } catch (IOException e) {
            LOG.warn("Failed to parse the output of the 'patch info' command with message '" + e.getMessage() +
                "'. The output was:\n" + patchInfoOutcome);
            return Collections.emptyList();
        }

        if (!result.isSuccess()) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("'patch info' command didn't succeed: " + result);
            }
            return Collections.emptyList();
        }

        if (!(result.getResult() instanceof Map)) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Unexpected patch history results. Expected map but found " +
                    (result.getResult() == null ? "null" : result.getResult().getClass().toString()));
            }

            return Collections.emptyList();
        }

        List<PatchDetails> history = fromHistory(patchHistoryOutcome);

        List<PatchDetails> ret = new ArrayList<PatchDetails>();

        @SuppressWarnings("unchecked")
        Map<String, Object> resultMap = (Map<String, Object>) result.getResult();

        @SuppressWarnings("unchecked")
        List<String> oneOffs = (List<String>) resultMap.get("patches");

        for (String id : oneOffs) {
            PatchDetails p = findById(id, history);
            if (p != null) {
                ret.add(p);
            }
        }

        String cumulativePatchId = (String) resultMap.get("cumulative-patch-id");
        if (!"base".equals(cumulativePatchId)) {
            PatchDetails p = findById(cumulativePatchId, history);
            if (p != null) {
                ret.add(p);
            }
        }

        return ret;
    }

    private static PatchDetails findById(String patchId, List<PatchDetails> patchDetails) {
        for (PatchDetails p : patchDetails) {
            if (patchId.equals(p.getId())) {
                return p;
            }
        }

        return null;
    }

    public PatchDetails(String id, Type type, Date appliedAt) {
        this.id = id;
        this.type = type;
        this.appliedAt = appliedAt;
    }

    public String getId() {
        return id;
    }

    public Type getType() {
        return type;
    }

    public Date getAppliedAt() {
        return appliedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        PatchDetails that = (PatchDetails) o;

        if (!id.equals(that.id)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    public enum Type {
        CUMULATIVE, ONE_OFF;

        public static Type fromJsonValue(String value) {
            if ("one-off".equals(value)) {
                return ONE_OFF;
            } else if ("cumulative".equals(value)) {
                return CUMULATIVE;
            } else {
                return null;
            }
        }

        @Override
        public String toString() {
            switch (this) {
            case CUMULATIVE:
                return "cumulative";
            case ONE_OFF:
                return "one-off";
            default:
                throw new AssertionError("Non-exhaustive toString() implementation of PatchDetails.Type");
            }
        }
    }
}
