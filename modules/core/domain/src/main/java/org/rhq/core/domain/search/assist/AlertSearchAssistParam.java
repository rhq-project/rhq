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
