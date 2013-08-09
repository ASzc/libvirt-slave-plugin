package hudson.plugins.libvirt;

import hudson.Extension;
import hudson.model.JobProperty;
import hudson.model.JobPropertyDescriptor;
import hudson.model.Job;

import org.kohsuke.stapler.DataBoundConstructor;

public class BeforeJobSnapshotJobProperty extends JobProperty<Job<?, ?>> {

    @Extension
    public static class DescriptorImpl extends JobPropertyDescriptor {

        @Override
        public String getDisplayName() {
            return "Pre-execution node snapshot";
        }
    }

    private String snapshotName;

    @DataBoundConstructor
    public BeforeJobSnapshotJobProperty(String snapshotName) {
        setSnapshotName(snapshotName);
    }

    public String getSnapshotName() {
        return snapshotName;
    }

    private void setSnapshotName(String snapshotName) {

        this.snapshotName = snapshotName;
    }
}
