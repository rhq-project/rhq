package org.rhq.bundle.ant.task;

import java.io.File;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.taskdefs.Property;
import org.apache.tools.ant.util.FileUtils;

import org.rhq.bundle.ant.BundleAntProject;

/**
 * An extension of the Ant's standard property task that supports referencing files relative to the RHQ's Ant bundle's
 * deploy directory.
 *
 * @author Lukas Krejci
 * @since 4.12
 */
public class PropertyTask extends Property {

    private boolean relativeToDeployDir;

    public boolean isRelativeToDeployDir() {
        return relativeToDeployDir;
    }

    public void setRelativeToDeployDir(boolean relativeToDeployDir) {
        this.relativeToDeployDir = relativeToDeployDir;
    }

    @Override
    public void execute() throws BuildException {
        if (relativeToDeployDir && getFile() != null) {
            //the file is always set as an absolute path with project's basedir as the base directory.
            //let's "transplant" that on top of the deploy dir.

            try {
                String relativePath = FileUtils.getRelativePath(getProject().getBaseDir(), getFile());

                File deployDir = ((BundleAntProject) getProject()).getDeployDir();

                setFile(new File(deployDir, relativePath).getCanonicalFile());
            } catch (Exception e) {
                throw new BuildException("Failed to figure out the relative path for file " + getFile());
            }
        }

        super.execute();
    }
}
