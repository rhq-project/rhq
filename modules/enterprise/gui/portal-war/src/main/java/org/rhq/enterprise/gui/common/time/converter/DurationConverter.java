package org.rhq.enterprise.gui.common.time.converter;

import java.text.DecimalFormat;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;

public class DurationConverter implements Converter {

    private static final long MILLIS_IN_HOUR = 3600000L;
    private static final long MILLIS_IN_MINUTE = 60000L;
    private static final long MILLIS_IN_SECOND = 1000L;

    private static final DecimalFormat twoDigitFormatter = new DecimalFormat("00");

    public Object getAsObject(FacesContext context, UIComponent component, String value) {
        long millis = 0;
        if (value != null) {
            String[] parts = value.split(";");
            millis += (Integer.parseInt(parts[0]) * MILLIS_IN_HOUR);
            millis += (Integer.parseInt(parts[1]) * MILLIS_IN_MINUTE);
            millis += (Integer.parseInt(parts[2]) * MILLIS_IN_SECOND);
        }
        return Long.valueOf(millis);
    }

    public String getAsString(FacesContext context, UIComponent component, Object value) {
        if (value == null) {
            return "00:00:00"; // visual indicator of issue
        }
        long millis = (Long) value;

        int hours = (int) (millis / MILLIS_IN_HOUR);
        millis %= MILLIS_IN_HOUR;

        int mins = (int) (millis / MILLIS_IN_MINUTE);
        millis %= MILLIS_IN_MINUTE;

        int secs = (int) (millis / MILLIS_IN_SECOND);

        return format(hours) + ":" + format(mins) + ":" + format(secs);
    }

    private String format(int number) {
        return twoDigitFormatter.format(number);
    }

}
