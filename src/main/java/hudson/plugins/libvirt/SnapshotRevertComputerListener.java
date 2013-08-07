package hudson.plugins.libvirt;

import hudson.Extension;
import hudson.model.TaskListener;
import hudson.model.Computer;
import hudson.model.Node;
import hudson.slaves.ComputerListener;
import hudson.slaves.ComputerLauncher;

import java.io.IOException;
import java.util.Map;

import org.libvirt.Domain;
import org.libvirt.DomainSnapshot;
import org.libvirt.LibvirtException;

@Extension
public class SnapshotRevertComputerListener extends ComputerListener {
    @Override
    public void preLaunch(Computer c, TaskListener taskListener) throws IOException, InterruptedException {
        // Revert the snapshot before the node launches

        Node node = c.getNode();
        if (node instanceof VirtualMachineSlave) {
            VirtualMachineSlave slave = (VirtualMachineSlave) node;

            String beforeJobSnapshotName = slave.getBeforeJobSnapshotName();
            if (beforeJobSnapshotName != null && beforeJobSnapshotName.length() > 0) {

                ComputerLauncher launcher = slave.getLauncher();
                if (launcher instanceof ComputerLauncher) {

                    VirtualMachineLauncher slaveLauncher = (VirtualMachineLauncher) launcher;
                    Hypervisor hypervisor = slaveLauncher.findOurHypervisorInstance();

                    try {
                        Map<String, Domain> domains = hypervisor.getDomains();

                        String vmName = slaveLauncher.getVirtualMachineName();
                        Domain domain = domains.get(vmName);
                        if (domain != null) {
                            taskListener.getLogger().println("Reverting " + vmName + " to snapshot " + beforeJobSnapshotName + ".");

                            try {
                                DomainSnapshot snapshot = domain.snapshotLookupByName(beforeJobSnapshotName);
                                try {
                                    domain.revertToSnapshot(snapshot);
                                } catch (LibvirtException e) {
                                    taskListener.fatalError("No snapshot named " + beforeJobSnapshotName + " for VM: " + e);
                                }
                            } catch (LibvirtException e) {
                                taskListener.fatalError("No snapshot named " + beforeJobSnapshotName + " for VM: " + e);
                            }
                        } else {
                            taskListener.fatalError("No VM named " + vmName);
                        }
                    } catch (LibvirtException e) {
                        taskListener.fatalError("Can't get VM domains: " + e);
                    }
                }
            }
        }
    }
}