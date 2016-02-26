package org.rhq.coregui.client.inventory.groups.detail.operation.schedule;

import com.smartgwt.client.data.DSRequest;

import org.rhq.core.domain.util.PageControl;
import org.rhq.coregui.client.inventory.resource.ResourceDatasource;
import org.rhq.coregui.client.util.Log;

public class GroupOperationScheduleResourceDatasource extends ResourceDatasource {

    @Override
    protected PageControl getPageControl(DSRequest request) {
        PageControl pageControl = super.getPageControl(request);
        Log.debug("WARNING: " + getClass().getName() + " is not using paging for fetch request.");
        pageControl = PageControl.getUnlimitedInstance();
        return pageControl;
    }

}
