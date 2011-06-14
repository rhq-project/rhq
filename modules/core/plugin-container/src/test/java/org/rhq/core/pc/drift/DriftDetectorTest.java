package org.rhq.core.pc.drift;

import org.jmock.Expectations;
import org.testng.annotations.Test;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.drift.DriftConfiguration;
import org.rhq.test.JMockTest;

public class DriftDetectorTest extends JMockTest {

    @Test(enabled = false)
    public void generateInitialChangeset() {
        // Note that absence of meta data is not sufficient for determining that
        // no change sets have previously been generated. The user could have just
        // deleted the data directory. We need to track what the current change set
        // is. That probably needs to hang off of the resource so it is obtained
        // during inventory sync when the agent starts up.

        final ScheduleQueue queue = context.mock(ScheduleQueue.class);

        final ChangeSetManager changeSetMgr = context.mock(ChangeSetManager.class);

        final DriftDetectionSchedule schedule = new DriftDetectionSchedule(1,
            new DriftConfiguration(new Configuration()));

        context.checking(new Expectations() {{
            one(queue).nextSchedule(); will(returnValue(schedule));

            one(changeSetMgr).getMetadata(schedule.getResourceId()); will(returnValue(null));
        }});
    }

}
