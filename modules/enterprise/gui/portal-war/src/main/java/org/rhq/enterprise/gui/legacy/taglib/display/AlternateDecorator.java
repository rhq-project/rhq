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
package org.rhq.enterprise.gui.legacy.taglib.display;

import javax.servlet.jsp.JspException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.taglibs.standard.tag.common.core.NullAttributeException;

/**
 * usage <display:column property="priority" width=="10" title"alerts.alert.listheader.priority"/>
 * <display:prioritydecorator flagKey="application.properties.key.prefix"/>
 */
public class AlternateDecorator extends BaseDecorator {
    private static Log log = LogFactory.getLog(AlternateDecorator.class.getName());

    /**
     * Holds value of property secondChoice.
     */
    private String secondChoice;

    // our ColumnDecorator

    /**
     * If string column value exists, use that. Otherwise, return 2nd choice.
     *
     * @see org.apache.taglibs.display.ColumnDecorator#decorate(java.lang.Object)
     */
    public String decorate(Object columnValue) {
        String firstChoice = null;
        try {
            firstChoice = (String) columnValue;
            firstChoice = (String) evalAttr("firstChoice", firstChoice, String.class);
        } catch (NullAttributeException ne) {
            log.debug("bean " + firstChoice + " not found");
            return "";
        } catch (JspException je) {
            log.debug("can't evaluate name [" + firstChoice + "]: ", je);
            return "";
        } catch (ClassCastException cce) {
            log.debug("class cast exception: ", cce);
        }

        if ((firstChoice == null) || "".equals(firstChoice.trim())) {
            try {
                secondChoice = (String) evalAttr("secondChoice", secondChoice, String.class);
            } catch (NullAttributeException ne) {
                log.debug("bean " + secondChoice + " not found");
                return "";
            } catch (JspException je) {
                log.debug("can't evaluate name [" + secondChoice + "]: ", je);
                return "";
            }

            return secondChoice;
        } else {
            return firstChoice;
        }
    }

    /* (non-Javadoc)
     * @see javax.servlet.jsp.tagext.Tag#release()
     */
    public void release() {
        super.release();
        secondChoice = null;
    }

    /**
     * Getter for property secondChoice.
     *
     * @return Value of property secondChoice.
     */
    public String getSecondChoice() {
        return this.secondChoice;
    }

    /**
     * Setter for property secondChoice.
     *
     * @param secondChoice New value of property secondChoice.
     */
    public void setSecondChoice(String secondChoice) {
        this.secondChoice = secondChoice;
    }
}