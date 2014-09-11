/*
 * RHQ Management Platform
 * Copyright (C) 2005-2014 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2, as
 * published by the Free Software Foundation, and/or the GNU Lesser
 * General Public License, version 2.1, also as published by the Free
 * Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License and the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License
 * and the GNU Lesser General Public License along with this program;
 * if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */

package org.rhq.core.domain.search.assist;

public enum AlertSearchAssistParam {
    lastMins05(1000 * 60 * 5), //
    lastMins10(1000 * 60 * 10), //
    lastMins30(1000 * 60 * 30), //
    lastMins60(1000 * 60 * 60), //
    lastHours02(1000 * 60 * 60 * 2), //
    lastHours04(1000 * 60 * 60 * 4), //
    lastHours08(1000 * 60 * 60 * 8), //
    lastHours24(1000 * 60 * 60 * 24);

    private long lastMillis;

    private AlertSearchAssistParam(long lastMillis) {
        this.lastMillis = lastMillis;
    }

    public long getLastMillis() {
        return lastMillis;
    }

    public static String getLastTime(String param) {
        AlertSearchAssistParam enumParam = getCaseInsensitively(param);
        long now = System.currentTimeMillis();
        long lastTime = now - enumParam.getLastMillis();
        return String.valueOf(lastTime);
    }

    private static AlertSearchAssistParam getCaseInsensitively(String param) {
        param = param.toLowerCase();
        for (AlertSearchAssistParam next : AlertSearchAssistParam.values()) {
            if (next.name().toLowerCase().equals(param)) {
                return next;
            }
        }
        return null;
    }

}
