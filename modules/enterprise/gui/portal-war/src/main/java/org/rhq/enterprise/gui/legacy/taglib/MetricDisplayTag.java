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
package org.rhq.enterprise.gui.legacy.taglib;

import java.io.IOException;
import java.text.NumberFormat;
import java.util.Locale;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.TagSupport;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.util.RequestUtils;
import org.apache.taglibs.standard.tag.el.core.ExpressionUtil;
import org.rhq.core.domain.measurement.MeasurementUnits;
import org.rhq.core.domain.measurement.util.MeasurementConverter;

/**
 * This is a locale aware tag for displaying metrics and units. Suppose you have a metricDisplaySummary.setMin(9234289)
 * and metricDisplaySummary.setUnit("bytes") set in the bean, this tag allows you to say: <spider:metric
 * metric="${metricDisplaySummary.min}" unit="${metricDisplaySummary.unit}" /> and get this output: <span>9,018
 * KB</span> This class assumes that any re-scaling of the metric value and unit string as well as any localization of
 * the unit string was performed prior to the data being parameterized for this tag. To assure that the locale of the
 * unit string agrees with the locale of the metric, use the jakarta-struts key org.apache.struts.Globals.LOCALE_KEY
 */
public class MetricDisplayTag extends TagSupport {
    private static final String TAG_NAME = "metric";

    public static Log log = LogFactory.getLog(MetricDisplayTag.class.getName());

    private Double metricVal;
    private String unitVal;
    private String spanVal;
    private String defaultKeyVal;

    // tag attributes
    private String metric_el;
    private String unit_el;
    private String span_el;
    private String longDate_el;
    private String defaultKey_el;

    // internal bookkeeping attributes
    private boolean metricIsSet = false;
    private boolean unitIsSet = false;
    private boolean spanIsSet = false;
    private boolean defaultKeyIsSet = false;

    protected String locale = org.apache.struts.Globals.LOCALE_KEY;
    protected String bundle = org.apache.struts.Globals.MESSAGES_KEY;

    /* (non-Javadoc)
     * @see javax.servlet.jsp.tagext.Tag#doEndTag()
     */
    @Override
    public int doEndTag() throws JspException {
        Locale userLocale = RequestUtils.retrieveUserLocale(pageContext, locale);
        if (unitIsSet) {
            setUnitVal((String) evalAttr("unit", unit_el, String.class));
        }

        if (defaultKeyIsSet) {
            setDefaultKeyVal((String) evalAttr("defaultKey", defaultKey_el, String.class));
        }

        // if the metric value is empty, converting to a Double will
        // give a value of 0.0. this makes it impossible for us to
        // distinguish further down the line whether the metric was
        // actually collected with a value of 0.0 or whether it was
        // not collected at all. therefore, we'll let metricVal be
        // null if the metric was not collected, and we'll check for
        // null later when handling the not-avail case.
        // PR: 7588
        String mval = (String) evalAttr("metric", metric_el, String.class);
        if ((mval != null) && !mval.equals("")) {
            setMetricVal(new Double(mval));
        }

        StringBuffer sb = new StringBuffer("<span");
        if (spanIsSet && (getSpan().length() > 0)) {
            setSpanVal((String) evalAttr("span", span_el, String.class));
            sb.append(" class=\"");
            sb.append(getSpanVal());
            sb.append("\"");
        }

        sb.append(">");

        if ((getMetricVal() == null) || (Double.isNaN(getMetricVal().doubleValue()) && defaultKeyIsSet)) {
            sb.append(RequestUtils.message(pageContext, bundle, userLocale.toString(), getDefaultKeyVal()));
        }

        // XXXX remove duplication with the metric decorator
        // and the UnitsConvert/UnitsFormat stuff
        else if (getUnitVal().equals("ms")) {
            NumberFormat f = NumberFormat.getNumberInstance(userLocale);
            f.setMinimumFractionDigits(3);
            f.setMaximumFractionDigits(3);
            String formatted = f.format(getMetricVal().doubleValue() / 1000);
            String[] args = new String[] { formatted };
            sb.append(RequestUtils.message(pageContext, bundle, userLocale.toString(), "metric.tag.units.s.arg", args));
        } else {
            MeasurementUnits units = MeasurementUnits.valueOf(getUnitVal());
            Double dataValue = getMetricVal();
            String formattedValue = MeasurementConverter.format(dataValue, units, true);

            sb.append(formattedValue);
        }

        sb.append("</span>");

        try {
            pageContext.getOut().print(sb.toString());
        } catch (IOException e) {
            log.debug("could not write output: ", e);
            throw new JspException("Could not access metrics tag");
        }

        release();
        return EVAL_PAGE;
    }

    public void validate() throws JspException {
    }

    /* (non-Javadoc)
     * @see javax.servlet.jsp.tagext.Tag#release()
     */
    @Override
    public void release() {
        super.release();
        metric_el = null;
        unit_el = null;
        span_el = null;
        defaultKey_el = null;
        metricVal = null;
        unitVal = null;
        spanVal = null;
        defaultKeyVal = null;
        metricIsSet = false;
        unitIsSet = false;
        spanIsSet = false;
        defaultKeyIsSet = false;
    }

    /* (non-Javadoc)
     * @see javax.servlet.jsp.tagext.Tag#doStartTag()
     */
    @Override
    public int doStartTag() throws JspException {
        validate();
        return super.doStartTag();
    }

    /**
     * Returns the metric.
     *
     * @return String
     */
    public String getMetric() {
        return metric_el;
    }

    /**
     * Sets the metric.
     *
     * @param metric The metric to set
     */
    public void setMetric(String metric) {
        metricIsSet = true;
        this.metric_el = metric;
    }

    /**
     * Returns the span.
     *
     * @return String
     */
    public String getSpan() {
        return span_el;
    }

    /**
     * Sets the defaultKey.
     *
     * @param defaultKey The defaultKey to set
     */
    public void setDefaultKey(String defaultKey) {
        defaultKeyIsSet = true;
        this.defaultKey_el = defaultKey;
    }

    /**
     * Returns the defaultKey.
     *
     * @return String
     */
    public String getDefaultKey() {
        return defaultKey_el;
    }

    /**
     * Sets the span.
     *
     * @param span The span to set
     */
    public void setSpan(String span) {
        spanIsSet = true;
        this.span_el = span;
    }

    /**
     * Returns the unit.
     *
     * @return String
     */
    public String getUnit() {
        return unit_el;
    }

    /**
     * @param longDate
     */
    public void setLongDate(String longDate) {
        this.longDate_el = longDate;
    }

    /**
     * Returns the flag for whether or not the metric is a long/date
     *
     * @return String
     */
    public String getLongDate() {
        return longDate_el;
    }

    /**
     * Sets the unit.
     *
     * @param unit The unit to set
     */
    public void setUnit(String unit) {
        unitIsSet = true;
        this.unit_el = unit;
    }

    // private stuph

    private Object evalAttr(String attributeName, String expression, Class expectedType) throws JspException {
        return ExpressionUtil.evalNotNull(TAG_NAME, attributeName, expression, expectedType, this, pageContext);
    }

    private Double getMetricVal() {
        return metricVal;
    }

    private String getSpanVal() {
        return spanVal;
    }

    private String getDefaultKeyVal() {
        return defaultKeyVal;
    }

    private String getUnitVal() {
        return unitVal;
    }

    private void setMetricVal(Double metricVal) {
        this.metricVal = metricVal;
    }

    private void setSpanVal(String spanVal) {
        this.spanVal = spanVal;
    }

    private void setDefaultKeyVal(String defaultKeyVal) {
        this.defaultKeyVal = defaultKeyVal;
    }

    private void setUnitVal(String unitVal) {
        this.unitVal = unitVal;
    }
}