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

package org.rhq.server.metrics;

import com.datastax.driver.core.ResultSetFuture;


/**
 * @author Stefan Negrea
 *
 */
public class MetricResultFuture<Q> {

    private Q context;
    private ResultSetFuture resultSetFuture;

    /**
     * @param context
     * @param resultSetFuture
     * @param query
     */
    public MetricResultFuture(ResultSetFuture resultSetFuture, Q context) {
        this.context = context;
        this.resultSetFuture = resultSetFuture;
    }

    /**
     * @return the context
     */
    public Q getContext() {
        return context;
    }

    /**
     * @param context the context to set
     */
    public void setContext(Q context) {
        this.context = context;
    }

    /**
     * @return the resultSetFuture
     */
    public ResultSetFuture getResultSetFuture() {
        return resultSetFuture;
    }

    /**
     * @param resultSetFuture the resultSetFuture to set
     */
    public void setResultSetFuture(ResultSetFuture resultSetFuture) {
        this.resultSetFuture = resultSetFuture;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((context == null) ? 0 : context.hashCode());
        result = prime * result + ((resultSetFuture == null) ? 0 : resultSetFuture.hashCode());
        return result;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        MetricResultFuture other = (MetricResultFuture) obj;
        if (context == null) {
            if (other.context != null)
                return false;
        } else if (!context.equals(other.context))
            return false;
        if (resultSetFuture == null) {
            if (other.resultSetFuture != null)
                return false;
        } else if (!resultSetFuture.equals(other.resultSetFuture))
            return false;
        return true;
    }

}
