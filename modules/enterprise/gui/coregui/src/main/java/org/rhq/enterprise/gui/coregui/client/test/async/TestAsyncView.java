package org.rhq.enterprise.gui.coregui.client.test.async;

import com.google.gwt.user.client.Timer;
import com.smartgwt.client.widgets.IButton;
import com.smartgwt.client.widgets.Label;
import com.smartgwt.client.widgets.layout.HLayout;
import org.rhq.core.domain.authz.Permission;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.PermissionsLoadedListener;
import org.rhq.enterprise.gui.coregui.client.PermissionsLoader;
import org.rhq.enterprise.gui.coregui.client.util.async.AsyncOperation;
import org.rhq.enterprise.gui.coregui.client.util.async.AsyncOperationCallback;
import org.rhq.enterprise.gui.coregui.client.util.async.ParallelCompoundAsyncOperation;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableVLayout;

import java.util.Map;
import java.util.Set;

/**
 * @author Ian Springer
 */
public class TestAsyncView extends LocatableVLayout implements AsyncOperationCallback<Map<AsyncOperation, Object>> {

    private AsyncOperation globalPermsAsyncOperation;
    private AsyncOperation resourcePermsAsyncOperation;
    private Set<Permission> globalPerms;
    private Set<Permission> resourcePerms;
    private Label label;
    private HLayout buttonBar;
    private IButton button1;
    private IButton button2;

    public TestAsyncView(String locatorId) {
        super(locatorId);
        setMargin(10);

        final ParallelCompoundAsyncOperation compoundAsyncOperation = new ParallelCompoundAsyncOperation();        
        
        this.globalPermsAsyncOperation = new AsyncOperation() {
            public void execute(final AsyncOperationCallback callback, Object... params) {
                new PermissionsLoader().loadExplicitGlobalPermissions(new PermissionsLoadedListener() {
                    public void onPermissionsLoaded(Set<Permission> permissions) {
                        globalPerms = permissions;
                        Timer timer = new Timer() {
                            public void run() {
                                callback.onSuccess(globalPermsAsyncOperation, globalPerms);
                            }
                        };
                        timer.schedule(1000);
                    }
                });
            }
        };        
        compoundAsyncOperation.addOperation(this.globalPermsAsyncOperation);

        this.resourcePermsAsyncOperation = new AsyncOperation() {
            public void execute(final AsyncOperationCallback callback, Object... params) {
                new PermissionsLoader().loadExplicitResourcePermissions(10001, new PermissionsLoadedListener() {
                    public void onPermissionsLoaded(Set<Permission> permissions) {
                        resourcePerms = permissions;
                        Timer timer = new Timer() {
                            public void run() {
                                callback.onSuccess(resourcePermsAsyncOperation, resourcePerms);
                            }
                        };
                        timer.schedule(2000);
                    }
                });
            }
        };
        compoundAsyncOperation.addOperation(this.resourcePermsAsyncOperation);

        compoundAsyncOperation.execute(this);

        this.label = new Label("Loading...");
        addMember(this.label);

        this.buttonBar = new HLayout();
        this.buttonBar.setMargin(15);
        this.buttonBar.setMembersMargin(5);
        this.buttonBar.setVisible(false);

        this.button1 = new IButton("Do Action requiring MANAGE_SECURITY");
        this.button1.setWidth(220);
        this.button1.setDisabled(true);
        this.buttonBar.addMember(this.button1);

        this.button2 = new IButton("Do Action requiring CONFIGURE_WRITE on platform Resource");
        this.button2.setWidth(320);
        this.button2.setDisabled(true);
        this.buttonBar.addMember(this.button2);

        addMember(this.buttonBar);
    }

    public void onSuccess(AsyncOperation operation, Map<AsyncOperation, Object> result) {
        this.label.hide();

        if (this.globalPerms.contains(Permission.MANAGE_SECURITY)) {
            this.button1.enable();
        }

        if (this.resourcePerms.contains(Permission.CONFIGURE_WRITE)) {
            this.button2.enable();
        }

        this.buttonBar.show();
    }
    
    public void onFailure(AsyncOperation operation, Throwable caught) {
        CoreGUI.getErrorHandler().handleError("Async init failed.", caught);
    }

}
