/*
 * RHQ Management Platform
 * Copyright (C) 2005-2014 Red Hat, Inc.
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
package org.rhq.core.domain.measurement.util;

import java.io.Serializable;
import java.util.Date;

/**
 * A time representation of a particular moment with precision to minutes. This class is used 
 * because Date class uses the long as an internal representation and it causes issues if the
 * instance is created with different timezone (on the client-side - taken from browser settings)
 * and then processed with another one (server-side - server time settings).
 *
 * @author Jirka Kremser
 */
public class Instant implements Serializable {
    private static final long serialVersionUID = 1L;
    private final int year;
    private final int month;
    private final int date;
    private final int hours;
    private final int minutes;
    
    private static final String SEPARATOR = "-";
    
    // GWT serialization needs this
    public Instant() {
        this.year = 0;
        this.month = 0;
        this.date = 0;
        this.hours = 0;
        this.minutes = 0;
    }
    
    public Instant(int year, int month, int date, int hours, int minutes) {
        this.year = year;
        this.month = month;
        this.date = date;
        this.hours = hours;
        this.minutes = minutes;
    }
    
    // Calendar is not supported in GWT
    @SuppressWarnings("deprecation")
    public Instant(Date date) {
        this.year = date.getYear();
        this.month = date.getMonth();
        this.date = date.getDate();
        this.hours = date.getHours();
        this.minutes = date.getMinutes();
    }
    
    public static Instant parseMoment(String stringRepresentation) {
        if (stringRepresentation == null) {
            return null;
        }
        String[] chunks = stringRepresentation.split(SEPARATOR);
        if (null == chunks || chunks.length != 5) {
            return null;
        }
        try {
            int year = Integer.parseInt(chunks[0]);
            int month = Integer.parseInt(chunks[1]);
            int date = Integer.parseInt(chunks[2]);
            int hours = Integer.parseInt(chunks[3]);
            int minutes = Integer.parseInt(chunks[4]);
            return new Instant(year, month, date, hours, minutes);
        } catch (NumberFormatException nfe) {
            return null;
        }
    }

    public int getYear() {
        return year;
    }

    public int getMonth() {
        return month;
    }

    public int getDate() {
        return date;
    }

    public int getHours() {
        return hours;
    }

    public int getMinutes() {
        return minutes;
    }
    
    @SuppressWarnings("deprecation")
    public Date toDate() {
        return new Date(year, month, date, hours, minutes);
    }
    
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + date;
        result = prime * result + hours;
        result = prime * result + minutes;
        result = prime * result + month;
        result = prime * result + year;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Instant other = (Instant) obj;
        if (date != other.date)
            return false;
        if (hours != other.hours)
            return false;
        if (minutes != other.minutes)
            return false;
        if (month != other.month)
            return false;
        if (year != other.year)
            return false;
        return true;
    }

    @Override
    public String toString() {
        return year + SEPARATOR + month + SEPARATOR + date + SEPARATOR + hours + SEPARATOR + minutes;
    }
}
