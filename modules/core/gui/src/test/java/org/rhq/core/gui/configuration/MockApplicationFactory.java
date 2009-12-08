package org.rhq.core.gui.configuration;

import javax.faces.application.Application;
import javax.faces.application.ApplicationFactory;

public class MockApplicationFactory extends ApplicationFactory {

    Application application = new MockApplication();

    @Override
    public Application getApplication() {
        return application;
    }

    @Override
    public void setApplication(Application application) {
        this.application = application;
    }

}
