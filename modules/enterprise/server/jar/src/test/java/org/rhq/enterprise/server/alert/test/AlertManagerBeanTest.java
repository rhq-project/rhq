package org.rhq.enterprise.server.alert.test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.testng.annotations.Test;

import org.rhq.core.domain.alert.Alert;
import org.rhq.core.domain.alert.AlertCondition;
import org.rhq.core.domain.alert.AlertConditionCategory;
import org.rhq.core.domain.alert.AlertConditionLog;
import org.rhq.core.domain.alert.AlertConditionOperator;
import org.rhq.core.domain.event.EventSeverity;
import org.rhq.core.domain.measurement.DataType;
import org.rhq.core.domain.measurement.MeasurementDefinition;
import org.rhq.core.domain.measurement.MeasurementUnits;
import org.rhq.core.domain.measurement.NumericType;
import org.rhq.core.domain.operation.OperationRequestStatus;
import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.enterprise.server.alert.AlertManagerBean;
import org.rhq.enterprise.server.alert.i18n.AlertI18NFactory;
import org.rhq.enterprise.server.alert.i18n.AlertI18NResourceKeys;

@Test
public class AlertManagerBeanTest {

    private static final String TEN_PERCENT = String.format("%2.1f%%", 10d);
    private static final String TWELVE_DOT_5_B = String.format("%2.1fB", 12.5d);
    private String pretty;

    public void testPrettyPrintAVAILABILITY() {
        AlertCondition condition = createCondition(AlertConditionCategory.AVAILABILITY,
            AlertConditionOperator.AVAIL_GOES_UP.name(), null, null, null, null);
        pretty = getPrettyAlertConditionString(condition);
        check(AlertI18NResourceKeys.ALERT_AVAILABILITY_GOES_UP);
        pretty = getShortPrettyAlertConditionString(condition);
        check(AlertI18NResourceKeys.ALERT_AVAILABILITY_GOES_UP_SHORT);

        condition = createCondition(AlertConditionCategory.AVAILABILITY, AlertConditionOperator.AVAIL_GOES_DOWN.name(),
            null, null, null, null);
        pretty = getPrettyAlertConditionString(condition);
        check(AlertI18NResourceKeys.ALERT_AVAILABILITY_GOES_DOWN);
        pretty = getShortPrettyAlertConditionString(condition);
        check(AlertI18NResourceKeys.ALERT_AVAILABILITY_GOES_DOWN_SHORT);

        condition = createCondition(AlertConditionCategory.AVAILABILITY,
            AlertConditionOperator.AVAIL_GOES_DISABLED.name(), null, null, null, null);
        pretty = getPrettyAlertConditionString(condition);
        check(AlertI18NResourceKeys.ALERT_AVAILABILITY_GOES_DISABLED);
        pretty = getShortPrettyAlertConditionString(condition);
        check(AlertI18NResourceKeys.ALERT_AVAILABILITY_GOES_DISABLED_SHORT);

        condition = createCondition(AlertConditionCategory.AVAILABILITY,
            AlertConditionOperator.AVAIL_GOES_UNKNOWN.name(), null, null, null, null);
        pretty = getPrettyAlertConditionString(condition);
        check(AlertI18NResourceKeys.ALERT_AVAILABILITY_GOES_UNKNOWN);
        pretty = getShortPrettyAlertConditionString(condition);
        check(AlertI18NResourceKeys.ALERT_AVAILABILITY_GOES_UNKNOWN_SHORT);

    }

    public void testPrettyPrintAVAILABILITY_DURATION() {
        AlertCondition condition = createCondition(AlertConditionCategory.AVAIL_DURATION,
            AlertConditionOperator.AVAIL_DURATION_DOWN.name(), null, null, "120", null);
        pretty = getPrettyAlertConditionString(condition);
        assert "Availability stays DOWN [2m]".equals(pretty) : pretty;
        pretty = getShortPrettyAlertConditionString(condition);
        assert "Avail stays DOWN [2m]".equals(pretty) : pretty;

        condition = createCondition(AlertConditionCategory.AVAIL_DURATION,
            AlertConditionOperator.AVAIL_DURATION_NOT_UP.name(), null, null, "120", null);
        pretty = getPrettyAlertConditionString(condition);
        assert "Availability stays NOT UP [2m]".equals(pretty) : pretty;
        pretty = getShortPrettyAlertConditionString(condition);
        assert "Avail stays NOT UP [2m]".equals(pretty) : pretty;
    }

    public void testPrettyPrintTHRESHOLD() {
        MeasurementDefinition md = createDynamicMeasurementDefinition();
        AlertCondition condition = createCondition(AlertConditionCategory.THRESHOLD, md.getDisplayName(), ">", 12.5d,
            null, md);
        pretty = getPrettyAlertConditionString(condition);
        String ref = String.format("Foo Prop > %2.1fB", 12.5d);
        assert ref.equals(pretty) : pretty;
        pretty = getShortPrettyAlertConditionString(condition);
        assert ref.equals(pretty) : pretty;
    }

    public void testPrettyPrintTHRESHOLD_Calltime() {
        MeasurementDefinition md = createCalltimeMeasurementDefinition();
        String regex = "some.*(reg)?ex$"; // this is the "name" of the condition

        AlertCondition condition = createCondition(AlertConditionCategory.THRESHOLD, regex, ">", 12.5d, "MAX", md);
        pretty = getPrettyAlertConditionString(condition);
        check(AlertI18NResourceKeys.ALERT_METRIC_CALLTIME_THRESHOLD_WITH_EXPR, "CT Prop", "MAX", ">", TWELVE_DOT_5_B,
            regex);
        pretty = getShortPrettyAlertConditionString(condition);
        check(AlertI18NResourceKeys.ALERT_METRIC_CALLTIME_THRESHOLD_WITH_EXPR_SHORT, "CT Prop", "MAX", ">",
            TWELVE_DOT_5_B, regex);

        // no regex
        condition = createCondition(AlertConditionCategory.THRESHOLD, null, ">", 12.5d, "MAX", md);
        pretty = getPrettyAlertConditionString(condition);
        check(AlertI18NResourceKeys.ALERT_METRIC_CALLTIME_THRESHOLD, "CT Prop", "MAX", ">", TWELVE_DOT_5_B);
        pretty = getShortPrettyAlertConditionString(condition);
        check(AlertI18NResourceKeys.ALERT_METRIC_CALLTIME_THRESHOLD_SHORT, "CT Prop", "MAX", ">", TWELVE_DOT_5_B);
    }

    public void testPrettyPrintBASELINE() {
        MeasurementDefinition md = createDynamicMeasurementDefinition();
        AlertCondition condition = createCondition(AlertConditionCategory.BASELINE, md.getDisplayName(), ">", 0.10d,
            "mean", md);
        pretty = getPrettyAlertConditionString(condition);
        check(AlertI18NResourceKeys.ALERT_BASELINE_MEAN, "Foo Prop", ">", TEN_PERCENT);
        pretty = getShortPrettyAlertConditionString(condition);
        check(AlertI18NResourceKeys.ALERT_BASELINE_MEAN_SHORT, "Foo Prop", ">", TEN_PERCENT);

        condition = createCondition(AlertConditionCategory.BASELINE, md.getDisplayName(), ">", 0.10d, "min", md);
        pretty = getPrettyAlertConditionString(condition);
        check(AlertI18NResourceKeys.ALERT_BASELINE_MIN, "Foo Prop", ">", TEN_PERCENT);
        pretty = getShortPrettyAlertConditionString(condition);
        check(AlertI18NResourceKeys.ALERT_BASELINE_MIN_SHORT, "Foo Prop", ">", TEN_PERCENT);

        condition = createCondition(AlertConditionCategory.BASELINE, md.getDisplayName(), ">", 0.10d, "max", md);
        pretty = getPrettyAlertConditionString(condition);
        check(AlertI18NResourceKeys.ALERT_BASELINE_MAX, "Foo Prop", ">", TEN_PERCENT);
        pretty = getShortPrettyAlertConditionString(condition);
        check(AlertI18NResourceKeys.ALERT_BASELINE_MAX_SHORT, "Foo Prop", ">", TEN_PERCENT);
    }

    public void testPrettyPrintCHANGE() {
        MeasurementDefinition md = createDynamicMeasurementDefinition();
        AlertCondition condition = createCondition(AlertConditionCategory.CHANGE, md.getDisplayName(), null, null,
            null, md);
        pretty = getPrettyAlertConditionString(condition);
        check(AlertI18NResourceKeys.ALERT_METRIC_CHANGED, "Foo Prop");
        pretty = getShortPrettyAlertConditionString(condition);
        check(AlertI18NResourceKeys.ALERT_METRIC_CHANGED_SHORT, "Foo Prop");
    }

    public void testPrettyPrintCHANGE_Calltime() {
        MeasurementDefinition md = createCalltimeMeasurementDefinition();
        String regex = "some.*(reg)?ex$"; // this is the "name" of the condition

        AlertCondition condition = createCondition(AlertConditionCategory.CHANGE, regex, "LO", 0.10d, "MIN", md);
        pretty = getPrettyAlertConditionString(condition);
        String msg = AlertI18NFactory.getMessage(AlertI18NResourceKeys.ALERT_METRIC_CALLTIME_CHANGE_SHRINKS);
        String ref = "Calltime Metric CT Prop MIN %s by at least %2.1f%% with calltime destination matching \"some.*(reg)?ex$\"";
        String refs = "CT Prop MIN %s by %2.1f%% matching \"some.*(reg)?ex$\"";
        assert String.format(ref, msg, 10.0f).equals(pretty) : pretty;
        pretty = getShortPrettyAlertConditionString(condition);
        assert String.format(refs, msg, 10.0f).equals(pretty) : pretty + " \n<=> " + String.format(refs, msg, 10.0f);

        condition = createCondition(AlertConditionCategory.CHANGE, regex, "CH", 0.10d, "MIN", md);
        pretty = getPrettyAlertConditionString(condition);
        msg = AlertI18NFactory.getMessage(AlertI18NResourceKeys.ALERT_METRIC_CALLTIME_CHANGE_CHANGES);
        assert String.format(ref, msg, 10.0f).equals(pretty);
        pretty = getShortPrettyAlertConditionString(condition);
        assert String.format(refs, msg, 10.0f).equals(pretty);

        condition = createCondition(AlertConditionCategory.CHANGE, regex, "HI", 0.10d, "MIN", md);
        msg = AlertI18NFactory.getMessage(AlertI18NResourceKeys.ALERT_METRIC_CALLTIME_CHANGE_GROWS);
        pretty = getPrettyAlertConditionString(condition);
        assert String.format(ref, msg, 10.0f).equals(pretty) : pretty;
        pretty = getShortPrettyAlertConditionString(condition);
        assert String.format(refs, msg, 10.0f).equals(pretty) : pretty + " \n<=> " + String.format(refs, msg, 10.0f);

        ref = "Calltime Metric CT Prop MAX %s by at least %2.1f%% with calltime destination matching \"some.*(reg)?ex$\"";
        refs = "CT Prop MAX %s by %2.1f%% matching \"some.*(reg)?ex$\"";

        condition = createCondition(AlertConditionCategory.CHANGE, regex, "LO", 0.10d, "MAX", md);
        msg = AlertI18NFactory.getMessage(AlertI18NResourceKeys.ALERT_METRIC_CALLTIME_CHANGE_SHRINKS);
        pretty = getPrettyAlertConditionString(condition);
        assert String.format(ref, msg, 10.0f).equals(pretty) : pretty;
        pretty = getShortPrettyAlertConditionString(condition);
        assert String.format(refs, msg, 10.0f).equals(pretty) : pretty + " \n<=> " + String.format(refs, msg, 10.0f);

        condition = createCondition(AlertConditionCategory.CHANGE, regex, "CH", 0.10d, "MAX", md);
        msg = AlertI18NFactory.getMessage(AlertI18NResourceKeys.ALERT_METRIC_CALLTIME_CHANGE_CHANGES);
        pretty = getPrettyAlertConditionString(condition);
        assert String.format(ref, msg, 10.0f).equals(pretty) : pretty;
        pretty = getShortPrettyAlertConditionString(condition);
        assert String.format(refs, msg, 10.0f).equals(pretty) : pretty + " \n<=> " + String.format(refs, msg, 10.0f);

        condition = createCondition(AlertConditionCategory.CHANGE, regex, "HI", 0.10d, "MAX", md);
        msg = AlertI18NFactory.getMessage(AlertI18NResourceKeys.ALERT_METRIC_CALLTIME_CHANGE_GROWS);
        pretty = getPrettyAlertConditionString(condition);
        assert String.format(ref, msg, 10.0f).equals(pretty) : pretty;
        pretty = getShortPrettyAlertConditionString(condition);
        assert String.format(refs, msg, 10.0f).equals(pretty) : pretty + " \n<=> " + String.format(refs, msg, 10.0f);

        ref = "Calltime Metric CT Prop AVG %s by at least %2.1f%% with calltime destination matching \"some.*(reg)?ex$\"";
        refs = "CT Prop AVG %s by %2.1f%% matching \"some.*(reg)?ex$\"";

        condition = createCondition(AlertConditionCategory.CHANGE, regex, "LO", 0.10d, "AVG", md);
        msg = AlertI18NFactory.getMessage(AlertI18NResourceKeys.ALERT_METRIC_CALLTIME_CHANGE_SHRINKS);
        pretty = getPrettyAlertConditionString(condition);
        assert String.format(ref, msg, 10.0f).equals(pretty) : pretty;
        pretty = getShortPrettyAlertConditionString(condition);
        assert String.format(refs, msg, 10.0f).equals(pretty) : pretty + " \n<=> " + String.format(refs, msg, 10.0f);

        condition = createCondition(AlertConditionCategory.CHANGE, regex, "CH", 0.10d, "AVG", md);
        msg = AlertI18NFactory.getMessage(AlertI18NResourceKeys.ALERT_METRIC_CALLTIME_CHANGE_CHANGES);
        pretty = getPrettyAlertConditionString(condition);
        assert String.format(ref, msg, 10.0f).equals(pretty) : pretty;
        pretty = getShortPrettyAlertConditionString(condition);
        assert String.format(refs, msg, 10.0f).equals(pretty) : pretty + " \n<=> " + String.format(refs, msg, 10.0f);

        condition = createCondition(AlertConditionCategory.CHANGE, regex, "HI", 0.10d, "AVG", md);
        msg = AlertI18NFactory.getMessage(AlertI18NResourceKeys.ALERT_METRIC_CALLTIME_CHANGE_GROWS);
        pretty = getPrettyAlertConditionString(condition);
        assert String.format(ref, msg, 10.0f).equals(pretty) : pretty;
        pretty = getShortPrettyAlertConditionString(condition);
        assert String.format(refs, msg, 10.0f).equals(pretty) : pretty + " \n<=> " + String.format(refs, msg, 10.0f);

        // no regex
        ref = "Calltime Metric CT Prop AVG %s by at least %2.1f%%";
        refs = "CT Prop AVG %s by %2.1f%%";

        condition = createCondition(AlertConditionCategory.CHANGE, null, "LO", 0.10d, "AVG", md);
        msg = AlertI18NFactory.getMessage(AlertI18NResourceKeys.ALERT_METRIC_CALLTIME_CHANGE_SHRINKS);
        pretty = getPrettyAlertConditionString(condition);
        assert String.format(ref, msg, 10.0f).equals(pretty) : pretty;
        pretty = getShortPrettyAlertConditionString(condition);
        assert String.format(refs, msg, 10.0f).equals(pretty) : pretty + " \n<=> " + String.format(refs, msg, 10.0f);

        condition = createCondition(AlertConditionCategory.CHANGE, null, "CH", 0.10d, "AVG", md);
        msg = AlertI18NFactory.getMessage(AlertI18NResourceKeys.ALERT_METRIC_CALLTIME_CHANGE_CHANGES);
        pretty = getPrettyAlertConditionString(condition);
        assert String.format(ref, msg, 10.0f).equals(pretty) : pretty;
        pretty = getShortPrettyAlertConditionString(condition);
        assert String.format(refs, msg, 10.0f).equals(pretty) : pretty + " \n<=> " + String.format(refs, msg, 10.0f);

        condition = createCondition(AlertConditionCategory.CHANGE, null, "HI", 0.10d, "AVG", md);
        msg = AlertI18NFactory.getMessage(AlertI18NResourceKeys.ALERT_METRIC_CALLTIME_CHANGE_GROWS);
        pretty = getPrettyAlertConditionString(condition);
        assert String.format(ref, msg, 10.0f).equals(pretty) : pretty;
        pretty = getShortPrettyAlertConditionString(condition);
        assert String.format(refs, msg, 10.0f).equals(pretty) : pretty + " \n<=> " + String.format(refs, msg, 10.0f);
    }

    public void testPrettyPrintTRAIT() {
        MeasurementDefinition md = createTraitMeasurementDefinition();
        AlertCondition condition = createCondition(AlertConditionCategory.TRAIT, md.getDisplayName(), null, null, null,
            md);
        pretty = getPrettyAlertConditionString(condition);
        check(AlertI18NResourceKeys.ALERT_METRIC_CHANGED, "Blah Trait");
        pretty = getShortPrettyAlertConditionString(condition);
        check(AlertI18NResourceKeys.ALERT_METRIC_CHANGED_SHORT, "Blah Trait");

        condition = createCondition(AlertConditionCategory.TRAIT, md.getDisplayName(), null, null, "RegexPattern", md);
        pretty = getPrettyAlertConditionString(condition);
        check(AlertI18NResourceKeys.ALERT_METRIC_CHANGED_WITH_EXPR, "Blah Trait", "RegexPattern");
        pretty = getShortPrettyAlertConditionString(condition);
        check(AlertI18NResourceKeys.ALERT_METRIC_CHANGED_WITH_EXPR_SHORT, "Blah Trait", "RegexPattern");
    }

    public void testPrettyPrintCONTROL() {
        AlertCondition condition = createCondition(AlertConditionCategory.CONTROL, "opNameHere", null, null,
            OperationRequestStatus.FAILURE.name(), null);
        pretty = getPrettyAlertConditionString(condition);
        check(AlertI18NResourceKeys.ALERT_OPERATION, "opNameHere", "FAILURE");
        pretty = getShortPrettyAlertConditionString(condition);
        check(AlertI18NResourceKeys.ALERT_OPERATION_SHORT, "opNameHere", "FAILURE");

        condition = createCondition(AlertConditionCategory.CONTROL, "opNameHere", null, null,
            OperationRequestStatus.SUCCESS.name(), null);
        pretty = getPrettyAlertConditionString(condition);
        check(AlertI18NResourceKeys.ALERT_OPERATION, "opNameHere", "SUCCESS");
        pretty = getShortPrettyAlertConditionString(condition);
        check(AlertI18NResourceKeys.ALERT_OPERATION_SHORT, "opNameHere", "SUCCESS");
    }

    public void testPrettyPrintEVENT() {
        String regex = "some.*(reg)?ex$";

        AlertCondition condition = createCondition(AlertConditionCategory.EVENT, EventSeverity.WARN.name(), null, null,
            regex, null);
        pretty = getPrettyAlertConditionString(condition);
        assert ("Event With Severity [WARN] Matching Expression \"" + regex + "\"").equals(pretty) : pretty;
        pretty = getShortPrettyAlertConditionString(condition);
        assert ("[WARN] Event Matching \"" + regex + "\"").equals(pretty) : pretty;

        // no regex
        condition = createCondition(AlertConditionCategory.EVENT, EventSeverity.WARN.name(), null, null, null, null);
        pretty = getPrettyAlertConditionString(condition);
        assert "Event With Severity [WARN]".equals(pretty) : pretty;
        pretty = getShortPrettyAlertConditionString(condition);
        assert "[WARN] Event".equals(pretty) : pretty;
    }

    public void testPrettyPrintRESOURCECONFIG() {
        AlertCondition condition = createCondition(AlertConditionCategory.RESOURCE_CONFIG, null, null, null, null, null);
        pretty = getPrettyAlertConditionString(condition);
        assert "Resource Configuration Changed".equals(pretty) : pretty;
        pretty = getShortPrettyAlertConditionString(condition);
        assert "Res Config Chg".equals(pretty) : pretty;
    }

    public void testPrettyPrintDRIFT() {
        AlertCondition condition = createCondition(AlertConditionCategory.DRIFT, "?riftName", null, null, "fil.*", null);
        pretty = getPrettyAlertConditionString(condition);
        assert "Drift detected for files that match \"fil.*\" and for drift definition [?riftName]".equals(pretty) : pretty;
        pretty = getShortPrettyAlertConditionString(condition);
        assert "Drift matching \"fil.*\", config=[?riftName]".equals(pretty) : pretty;

        condition = createCondition(AlertConditionCategory.DRIFT, null, null, null, "fil.*", null);
        pretty = getPrettyAlertConditionString(condition);
        assert "Drift detected for files that match \"fil.*\"".equals(pretty) : pretty;
        pretty = getShortPrettyAlertConditionString(condition);
        assert "Drift matching \"fil.*\"".equals(pretty) : pretty;

        condition = createCondition(AlertConditionCategory.DRIFT, "?riftName", null, null, null, null);
        pretty = getPrettyAlertConditionString(condition);
        assert "Drift detected for drift definition [?riftName]".equals(pretty) : pretty;
        pretty = getShortPrettyAlertConditionString(condition);
        assert "Drift! config=[?riftName]".equals(pretty) : pretty;

        condition = createCondition(AlertConditionCategory.DRIFT, null, null, null, null, null);
        pretty = getPrettyAlertConditionString(condition);
        assert "Drift Detected".equals(pretty) : pretty;
        pretty = getShortPrettyAlertConditionString(condition);
        assert "Drift!".equals(pretty) : pretty;
    }

    public void testPrettyPrintRANGE() {
        MeasurementDefinition md = createDynamicMeasurementDefinition();
        AlertCondition condition = createCondition(AlertConditionCategory.RANGE, md.getDisplayName(), "<=", 1.0,
            "22.2", md);
        pretty = getPrettyAlertConditionString(condition);
        check(AlertI18NResourceKeys.ALERT_RANGE_INSIDE_INCL, "Foo Prop", String.format("%1.1fB", 1d),
            String.format("%2.1fB", 22.2d));
        pretty = getShortPrettyAlertConditionString(condition);
        check(AlertI18NResourceKeys.ALERT_RANGE_INSIDE_INCL_SHORT, "Foo Prop", String.format("%1.1fB", 1d),
            String.format("%2.1fB", 22.2d));

        condition = createCondition(AlertConditionCategory.RANGE, md.getDisplayName(), ">=", 1.0, "22.2", md);
        pretty = getPrettyAlertConditionString(condition);
        check(AlertI18NResourceKeys.ALERT_RANGE_OUTSIDE_INCL, "Foo Prop", String.format("%1.1fB", 1d),
            String.format("%2.1fB", 22.2d));
        pretty = getShortPrettyAlertConditionString(condition);
        check(AlertI18NResourceKeys.ALERT_RANGE_OUTSIDE_INCL_SHORT, "Foo Prop", String.format("%1.1fB", 1d),
            String.format("%2.1fB", 22.2d));

        condition = createCondition(AlertConditionCategory.RANGE, md.getDisplayName(), "<", 1.0, "22.2", md);
        pretty = getPrettyAlertConditionString(condition);
        check(AlertI18NResourceKeys.ALERT_RANGE_INSIDE_EXCL, "Foo Prop", String.format("%1.1fB", 1d),
            String.format("%2.1fB", 22.2d));
        pretty = getShortPrettyAlertConditionString(condition);
        check(AlertI18NResourceKeys.ALERT_RANGE_INSIDE_EXCL_SHORT, "Foo Prop", String.format("%1.1fB", 1d),
            String.format("%2.1fB", 22.2d));

        condition = createCondition(AlertConditionCategory.RANGE, md.getDisplayName(), ">", 1.0, "22.2", md);
        pretty = getPrettyAlertConditionString(condition);
        check(AlertI18NResourceKeys.ALERT_RANGE_OUTSIDE_EXCL, "Foo Prop", String.format("%1.1fB", 1d),
            String.format("%2.1fB", 22.2d));
        pretty = getShortPrettyAlertConditionString(condition);
        check(AlertI18NResourceKeys.ALERT_RANGE_OUTSIDE_EXCL_SHORT, "Foo Prop", String.format("%1.1fB", 1d),
            String.format("%2.1fB", 22.2d));
    }

    private String getPrettyAlertConditionString(AlertCondition condition) {
        AlertManagerBean pojo = new AlertManagerBean();
        String s = extractCondition(pojo.prettyPrintAlertConditions(createAlert(condition), false));
        System.out.println("long===>" + s);
        return s;
    }

    private String getShortPrettyAlertConditionString(AlertCondition condition) {
        AlertManagerBean pojo = new AlertManagerBean();
        String s = extractCondition(pojo.prettyPrintAlertConditions(createAlert(condition), true));
        System.out.println("short-->" + s);
        return s;
    }

    private MeasurementDefinition createDynamicMeasurementDefinition() {
        ResourceType resourceType = new ResourceType("testType", "testPlugin", ResourceCategory.PLATFORM, null);
        MeasurementDefinition md = new MeasurementDefinition(resourceType, "fooMetric");
        md.setDataType(DataType.MEASUREMENT);
        md.setDisplayName("Foo Prop");
        md.setMeasurementType(NumericType.DYNAMIC);
        md.setRawNumericType(NumericType.DYNAMIC);
        md.setUnits(MeasurementUnits.BYTES);
        return md;
    }

    private MeasurementDefinition createCalltimeMeasurementDefinition() {
        ResourceType resourceType = new ResourceType("testType", "testPlugin", ResourceCategory.PLATFORM, null);
        MeasurementDefinition md = new MeasurementDefinition(resourceType, "ctMetric");
        md.setDataType(DataType.CALLTIME);
        md.setDisplayName("CT Prop");
        md.setMeasurementType(NumericType.DYNAMIC);
        md.setRawNumericType(NumericType.DYNAMIC);
        md.setUnits(MeasurementUnits.BYTES);
        md.setDestinationType("/wot gorilla");
        return md;
    }

    private MeasurementDefinition createTraitMeasurementDefinition() {
        ResourceType resourceType = new ResourceType("testType", "testPlugin", ResourceCategory.PLATFORM, null);
        MeasurementDefinition md = new MeasurementDefinition(resourceType, "traitMetric");
        md.setDataType(DataType.TRAIT);
        md.setDisplayName("Blah Trait");
        md.setUnits(MeasurementUnits.BYTES);
        return md;
    }

    private Alert createAlert(AlertCondition condition) {
        Alert alert = new Alert();
        AlertConditionLog conditionLog = new AlertConditionLog(condition, System.currentTimeMillis());
        alert.addConditionLog(conditionLog);
        return alert;
    }

    private AlertCondition createCondition(AlertConditionCategory category, String name, String comparator,
        Double threshold, String option, MeasurementDefinition measDef) {
        AlertCondition condition = new AlertCondition();
        condition.setCategory(category);
        condition.setName(name);
        condition.setComparator(comparator);
        condition.setThreshold(threshold);
        condition.setOption(option);
        condition.setMeasurementDefinition(measDef);
        return condition;
    }

    private String extractCondition(String prettyString) {
        String cond = AlertI18NFactory.getMessage(AlertI18NResourceKeys.ALERT_CONDITION_PATTERN); // Take i18n into account
        //System.out.println(prettyString);
        Pattern pattern = Pattern.compile("- " + cond + " 1: (.*)\n"); //en short form has " - Cond 1: ...", long form has " - Condition 1: ..."
        Matcher matcher = pattern.matcher(prettyString);
        assert matcher.find() : "could not find the condition string";
        return matcher.group(1);
    }

    private void check(String msg) {
        String ref = AlertI18NFactory.getMessage(msg);
        assert ref != null : "Could not find reference message";
        assert ref.equals(pretty) : pretty;
    }

    private void check(String msg, Object... args) {
        String ref = AlertI18NFactory.getMessage(msg, args);
        assert ref != null : "Could not find reference message";
        assert ref.equals(pretty) : "Got : >>" + pretty + "<<   Expect: >>" + ref + "<<";
    }
}
