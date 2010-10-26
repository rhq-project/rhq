package org.rhq.enterprise.server.discovery;

import java.util.List;

import org.jmock.Expectations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import org.rhq.core.clientapi.server.discovery.InventoryReport;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.criteria.ResourceTypeCriteria;
import org.rhq.core.domain.resource.Agent;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceBuilder;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.resource.ResourceTypeBuilder;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.server.auth.SubjectManagerLocal;
import org.rhq.enterprise.server.resource.ResourceTypeManagerLocal;
import org.rhq.test.JMockTest;

import static org.testng.Assert.*;

public class DeletedResourceTypeFilterTest extends JMockTest {

    SubjectManagerLocal subjectMgr;

    ResourceTypeManagerLocal resourceTypeMgr;

    DeletedResourceTypeFilter filter;

    @BeforeMethod
    public void init() {
        subjectMgr = context.mock(SubjectManagerLocal.class);
        resourceTypeMgr = context.mock(ResourceTypeManagerLocal.class);
    }

    @Test
    public void acceptReportWithNoDeletedTypes() {
        context.checking(new Expectations() {{
            allowing(subjectMgr).getOverlord();
            will(returnValue(new Subject("overlord", true, true)));

            allowing(resourceTypeMgr).findResourceTypesByCriteria(with(aNonNull(Subject.class)),
                with(aNonNull(ResourceTypeCriteria.class)));
            will(returnValue(new PageList<ResourceType>()));
        }});

        InventoryReport report = createReport();
        report.addAddedRoot(new ResourceBuilder()
            .createRandomServer()
            .with(2).randomChildServices()
            .build());
        report.addAddedRoot(new ResourceBuilder()
            .createRandomService()
            .with(2).randomChildServices()
            .build());

        filter = new DeletedResourceTypeFilter(subjectMgr, resourceTypeMgr);

        assertTrue(filter.accept(report), "Expected report to be accepted when it does not contain any deleted " +
            "resource types");
    }

    @Test
    public void rejectReportWithDeletedTypes() {
        ResourceType deletedServiceType = new ResourceTypeBuilder()
            .createServiceResourceType()
            .withName("TestService")
            .withPlugin("TestPlugin")
            .thatIsDeleted()
            .build();

        final PageList<ResourceType> deletedTypes = new PageList<ResourceType>();
        deletedTypes.add(deletedServiceType);

        context.checking(new Expectations() {{
            allowing(subjectMgr).getOverlord();
            will(returnValue(new Subject("overlord", true, true)));

            allowing(resourceTypeMgr).findResourceTypesByCriteria(with(aNonNull(Subject.class)),
                with(aNonNull(ResourceTypeCriteria.class)));
            will(returnValue(deletedTypes));
        }});

        InventoryReport report = createReport();
        report.addAddedRoot(new ResourceBuilder()
            .createRandomServer()
            .withChildService()
                .withName("ChildService")
                .withUuid("c1")
                .withResourceType(new ResourceTypeBuilder()
                    .createServiceResourceType()
                    .withName("TestService")
                    .withPlugin("TestPlugin")
                    .build())
                .included()
            .build());

        filter = new DeletedResourceTypeFilter(subjectMgr, resourceTypeMgr);

        assertFalse(filter.accept(report), "Expected report to be rejected since it contains deleted resource types");
    }

    InventoryReport createReport() {
        Agent agent = new Agent("localhost", "localhost", 1234, "1234", "test-token");
        return new InventoryReport(agent);
    }

}
