package org.rhq.enterprise.server.alert.test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.testng.annotations.Test;

import org.rhq.core.domain.alert.Alert;
import org.rhq.core.domain.alert.AlertCondition;
import org.rhq.core.domain.alert.AlertConditionCategory;
import org.rhq.core.domain.alert.AlertConditionLog;
import org.rhq.core.domain.event.EventSeverity;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.measurement.DataType;
import org.rhq.core.domain.measurement.MeasurementDefinition;
import org.rhq.core.domain.measurement.MeasurementUnits;
import org.rhq.core.domain.measurement.NumericType;
import org.rhq.core.domain.operation.OperationRequestStatus;
import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.enterprise.server.alert.AlertManagerBean;

@Test
public class AlertManagerBeanTest {

    private String pretty;

    public void testPrettyPrintAVAILABILITY() {
        AlertCondition condition = createCondition(AlertConditionCategory.AVAILABILITY, null, null, null,
            AvailabilityType.UP.name(), null);
        pretty = getPrettyAlertConditionString(condition);
        assert "Availability goes UP".equals(pretty) : pretty;
        pretty = getShortPrettyAlertConditionString(condition);
        assert "Avail goes UP".equals(pretty) : pretty;

        condition = createCondition(AlertConditionCategory.AVAILABILITY, null, null, null,
            AvailabilityType.DOWN.name(), null);
        pretty = getPrettyAlertConditionString(condition);
        assert "Availability goes DOWN".equals(pretty) : pretty;
        pretty = getShortPrettyAlertConditionString(condition);
        assert "Avail goes DOWN".equals(pretty) : pretty;
    }

    public void testPrettyPrintTHRESHOLD() {
        MeasurementDefinition md = createDynamicMeasurementDefinition();
        AlertCondition condition = createCondition(AlertConditionCategory.THRESHOLD, md.getDisplayName(), ">", 12.5d,
            null, md);
        pretty = getPrettyAlertConditionString(condition);
        assert "Foo Prop > 12.5B".equals(pretty) : pretty;
        pretty = getShortPrettyAlertConditionString(condition);
        assert "Foo Prop > 12.5B".equals(pretty) : pretty;
    }

    public void testPrettyPrintTHRESHOLD_Calltime() {
        MeasurementDefinition md = createCalltimeMeasurementDefinition();
        String regex = "some.*(reg)?ex$"; // this is the "name" of the condition

        AlertCondition condition = createCondition(AlertConditionCategory.THRESHOLD, regex, ">", 12.5d, "MAX", md);
        pretty = getPrettyAlertConditionString(condition);
        assert "Calltime Metric CT Prop MAX > 12.5B with calltime destination matching \"some.*(reg)?ex$\""
            .equals(pretty) : pretty;
        pretty = getShortPrettyAlertConditionString(condition);
        assert "CT Prop MAX > 12.5B matching \"some.*(reg)?ex$\"".equals(pretty) : pretty;

        // no regex
        condition = createCondition(AlertConditionCategory.THRESHOLD, null, ">", 12.5d, "MAX", md);
        pretty = getPrettyAlertConditionString(condition);
        assert "Calltime Metric CT Prop MAX > 12.5B".equals(pretty) : pretty;
        pretty = getShortPrettyAlertConditionString(condition);
        assert "CT Prop MAX > 12.5B".equals(pretty) : pretty;
    }

    public void testPrettyPrintBASELINE() {
        MeasurementDefinition md = createDynamicMeasurementDefinition();
        AlertCondition condition = createCondition(AlertConditionCategory.BASELINE, md.getDisplayName(), ">", 0.10d,
            "mean", md);
        pretty = getPrettyAlertConditionString(condition);
        assert "Foo Prop > 10.0% of Baseline Mean Value".equals(pretty) : pretty;
        pretty = getShortPrettyAlertConditionString(condition);
        assert "Foo Prop > 10.0% bl mean".equals(pretty) : pretty;

        condition = createCondition(AlertConditionCategory.BASELINE, md.getDisplayName(), ">", 0.10d, "min", md);
        pretty = getPrettyAlertConditionString(condition);
        assert "Foo Prop > 10.0% of Baseline Minimum Value".equals(pretty) : pretty;
        pretty = getShortPrettyAlertConditionString(condition);
        assert "Foo Prop > 10.0% bl min".equals(pretty) : pretty;

        condition = createCondition(AlertConditionCategory.BASELINE, md.getDisplayName(), ">", 0.10d, "max", md);
        pretty = getPrettyAlertConditionString(condition);
        assert "Foo Prop > 10.0% of Baseline Maximum Value".equals(pretty) : pretty;
        pretty = getShortPrettyAlertConditionString(condition);
        assert "Foo Prop > 10.0% bl max".equals(pretty) : pretty;
    }

    public void testPrettyPrintCHANGE() {
        MeasurementDefinition md = createDynamicMeasurementDefinition();
        AlertCondition condition = createCondition(AlertConditionCategory.CHANGE, md.getDisplayName(), null, null,
            null, md);
        pretty = getPrettyAlertConditionString(condition);
        assert "Foo Prop Value Changed".equals(pretty) : pretty;
        pretty = getShortPrettyAlertConditionString(condition);
        assert "Foo Prop Val Chg".equals(pretty) : pretty;
    }

    public void testPrettyPrintCHANGE_Calltime() {
        MeasurementDefinition md = createCalltimeMeasurementDefinition();
        String regex = "some.*(reg)?ex$"; // this is the "name" of the condition

        AlertCondition condition = createCondition(AlertConditionCategory.CHANGE, regex, "LO", 0.10d, "MIN", md);
        pretty = getPrettyAlertConditionString(condition);
        assert "Calltime Metric CT Prop MIN shrinks by at least 10.0% with calltime destination matching \"some.*(reg)?ex$\""
            .equals(pretty) : pretty;
        pretty = getShortPrettyAlertConditionString(condition);
        assert "CT Prop MIN shrinks by 10.0% matching \"some.*(reg)?ex$\"".equals(pretty) : pretty;

        condition = createCondition(AlertConditionCategory.CHANGE, regex, "CH", 0.10d, "MIN", md);
        pretty = getPrettyAlertConditionString(condition);
        assert "Calltime Metric CT Prop MIN changes by at least 10.0% with calltime destination matching \"some.*(reg)?ex$\""
            .equals(pretty) : pretty;
        pretty = getShortPrettyAlertConditionString(condition);
        assert "CT Prop MIN changes by 10.0% matching \"some.*(reg)?ex$\"".equals(pretty) : pretty;

        condition = createCondition(AlertConditionCategory.CHANGE, regex, "HI", 0.10d, "MIN", md);
        pretty = getPrettyAlertConditionString(condition);
        assert "Calltime Metric CT Prop MIN grows by at least 10.0% with calltime destination matching \"some.*(reg)?ex$\""
            .equals(pretty) : pretty;
        pretty = getShortPrettyAlertConditionString(condition);
        assert "CT Prop MIN grows by 10.0% matching \"some.*(reg)?ex$\"".equals(pretty) : pretty;

        condition = createCondition(AlertConditionCategory.CHANGE, regex, "LO", 0.10d, "MAX", md);
        pretty = getPrettyAlertConditionString(condition);
        assert "Calltime Metric CT Prop MAX shrinks by at least 10.0% with calltime destination matching \"some.*(reg)?ex$\""
            .equals(pretty) : pretty;
        pretty = getShortPrettyAlertConditionString(condition);
        assert "CT Prop MAX shrinks by 10.0% matching \"some.*(reg)?ex$\"".equals(pretty) : pretty;

        condition = createCondition(AlertConditionCategory.CHANGE, regex, "CH", 0.10d, "MAX", md);
        pretty = getPrettyAlertConditionString(condition);
        assert "Calltime Metric CT Prop MAX changes by at least 10.0% with calltime destination matching \"some.*(reg)?ex$\""
            .equals(pretty) : pretty;
        pretty = getShortPrettyAlertConditionString(condition);
        assert "CT Prop MAX changes by 10.0% matching \"some.*(reg)?ex$\"".equals(pretty) : pretty;

        condition = createCondition(AlertConditionCategory.CHANGE, regex, "HI", 0.10d, "MAX", md);
        pretty = getPrettyAlertConditionString(condition);
        assert "Calltime Metric CT Prop MAX grows by at least 10.0% with calltime destination matching \"some.*(reg)?ex$\""
            .equals(pretty) : pretty;
        pretty = getShortPrettyAlertConditionString(condition);
        assert "CT Prop MAX grows by 10.0% matching \"some.*(reg)?ex$\"".equals(pretty) : pretty;

        condition = createCondition(AlertConditionCategory.CHANGE, regex, "LO", 0.10d, "AVG", md);
        pretty = getPrettyAlertConditionString(condition);
        assert "Calltime Metric CT Prop AVG shrinks by at least 10.0% with calltime destination matching \"some.*(reg)?ex$\""
            .equals(pretty) : pretty;
        pretty = getShortPrettyAlertConditionString(condition);
        assert "CT Prop AVG shrinks by 10.0% matching \"some.*(reg)?ex$\"".equals(pretty) : pretty;

        condition = createCondition(AlertConditionCategory.CHANGE, regex, "CH", 0.10d, "AVG", md);
        pretty = getPrettyAlertConditionString(condition);
        assert "Calltime Metric CT Prop AVG changes by at least 10.0% with calltime destination matching \"some.*(reg)?ex$\""
            .equals(pretty) : pretty;
        pretty = getShortPrettyAlertConditionString(condition);
        assert "CT Prop AVG changes by 10.0% matching \"some.*(reg)?ex$\"".equals(pretty) : pretty;

        condition = createCondition(AlertConditionCategory.CHANGE, regex, "HI", 0.10d, "AVG", md);
        pretty = getPrettyAlertConditionString(condition);
        assert "Calltime Metric CT Prop AVG grows by at least 10.0% with calltime destination matching \"some.*(reg)?ex$\""
            .equals(pretty) : pretty;
        pretty = getShortPrettyAlertConditionString(condition);
        assert "CT Prop AVG grows by 10.0% matching \"some.*(reg)?ex$\"".equals(pretty) : pretty;

        // no regex
        condition = createCondition(AlertConditionCategory.CHANGE, null, "LO", 0.10d, "AVG", md);
        pretty = getPrettyAlertConditionString(condition);
        assert "Calltime Metric CT Prop AVG shrinks by at least 10.0%".equals(pretty) : pretty;
        pretty = getShortPrettyAlertConditionString(condition);
        assert "CT Prop AVG shrinks by 10.0%".equals(pretty) : pretty;

        condition = createCondition(AlertConditionCategory.CHANGE, null, "CH", 0.10d, "AVG", md);
        pretty = getPrettyAlertConditionString(condition);
        assert "Calltime Metric CT Prop AVG changes by at least 10.0%".equals(pretty) : pretty;
        pretty = getShortPrettyAlertConditionString(condition);
        assert "CT Prop AVG changes by 10.0%".equals(pretty) : pretty;

        condition = createCondition(AlertConditionCategory.CHANGE, null, "HI", 0.10d, "AVG", md);
        pretty = getPrettyAlertConditionString(condition);
        assert "Calltime Metric CT Prop AVG grows by at least 10.0%".equals(pretty) : pretty;
        pretty = getShortPrettyAlertConditionString(condition);
        assert "CT Prop AVG grows by 10.0%".equals(pretty) : pretty;
    }

    public void testPrettyPrintTRAIT() {
        MeasurementDefinition md = createTraitMeasurementDefinition();
        AlertCondition condition = createCondition(AlertConditionCategory.TRAIT, md.getDisplayName(), null, null, null,
            md);
        pretty = getPrettyAlertConditionString(condition);
        assert "Blah Trait Value Changed".equals(pretty) : pretty;
        pretty = getShortPrettyAlertConditionString(condition);
        assert "Blah Trait Val Chg".equals(pretty) : pretty;
    }

    public void testPrettyPrintCONTROL() {
        AlertCondition condition = createCondition(AlertConditionCategory.CONTROL, "opNameHere", null, null,
            OperationRequestStatus.FAILURE.name(), null);
        pretty = getPrettyAlertConditionString(condition);
        assert "Operation [opNameHere] has status=[FAILURE]".equals(pretty) : pretty;
        pretty = getShortPrettyAlertConditionString(condition);
        assert "Op [opNameHere]=FAILURE".equals(pretty) : pretty;

        condition = createCondition(AlertConditionCategory.CONTROL, "opNameHere", null, null,
            OperationRequestStatus.SUCCESS.name(), null);
        pretty = getPrettyAlertConditionString(condition);
        assert "Operation [opNameHere] has status=[SUCCESS]".equals(pretty) : pretty;
        pretty = getShortPrettyAlertConditionString(condition);
        assert "Op [opNameHere]=SUCCESS".equals(pretty) : pretty;
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
        assert "Drift detected for files that match \"fil.*\" and for drift configuration [?riftName]".equals(pretty) : pretty;
        pretty = getShortPrettyAlertConditionString(condition);
        assert "Drift matching \"fil.*\", config=[?riftName]".equals(pretty) : pretty;

        condition = createCondition(AlertConditionCategory.DRIFT, null, null, null, "fil.*", null);
        pretty = getPrettyAlertConditionString(condition);
        assert "Drift detected for files that match \"fil.*\"".equals(pretty) : pretty;
        pretty = getShortPrettyAlertConditionString(condition);
        assert "Drift matching \"fil.*\"".equals(pretty) : pretty;

        condition = createCondition(AlertConditionCategory.DRIFT, "?riftName", null, null, null, null);
        pretty = getPrettyAlertConditionString(condition);
        assert "Drift detected for drift configuration [?riftName]".equals(pretty) : pretty;
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
        assert "Foo Prop Value is Between 1.0B and 22.2B, Inclusive".equals(pretty) : pretty;
        pretty = getShortPrettyAlertConditionString(condition);
        assert "Foo Prop Between 1.0B - 22.2B, incl".equals(pretty) : pretty;

        condition = createCondition(AlertConditionCategory.RANGE, md.getDisplayName(), ">=", 1.0, "22.2", md);
        pretty = getPrettyAlertConditionString(condition);
        assert "Foo Prop Value is Outside 1.0B and 22.2B, Inclusive".equals(pretty) : pretty;
        pretty = getShortPrettyAlertConditionString(condition);
        assert "Foo Prop Outside 1.0B - 22.2B, incl".equals(pretty) : pretty;

        condition = createCondition(AlertConditionCategory.RANGE, md.getDisplayName(), "<", 1.0, "22.2", md);
        pretty = getPrettyAlertConditionString(condition);
        assert "Foo Prop Value is Between 1.0B and 22.2B, Exclusive".equals(pretty) : pretty;
        pretty = getShortPrettyAlertConditionString(condition);
        assert "Foo Prop Between 1.0B - 22.2B, excl".equals(pretty) : pretty;

        condition = createCondition(AlertConditionCategory.RANGE, md.getDisplayName(), ">", 1.0, "22.2", md);
        pretty = getPrettyAlertConditionString(condition);
        assert "Foo Prop Value is Outside 1.0B and 22.2B, Exclusive".equals(pretty) : pretty;
        pretty = getShortPrettyAlertConditionString(condition);
        assert "Foo Prop Outside 1.0B - 22.2B, excl".equals(pretty) : pretty;
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
        //System.out.println(prettyString);
        Pattern pattern = Pattern.compile(" - Cond(?:ition)? 1: (.*)\n"); // short form has " - Cond 1: ...", long form has " - Condition 1: ..."
        Matcher matcher = pattern.matcher(prettyString);
        assert matcher.find() : "could not find the condition string";
        return matcher.group(1);
    }
}
