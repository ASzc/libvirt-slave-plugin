package hudson.plugins.libvirt;

import hudson.Extension;
import hudson.model.TaskListener;
import hudson.model.Computer;
import hudson.model.Node;
import hudson.model.Run;
import hudson.model.listeners.RunListener;
import hudson.slaves.ComputerLauncher;
import hudson.slaves.SlaveComputer;

import java.io.IOException;
import java.util.Map;

import org.libvirt.Domain;
import org.libvirt.DomainSnapshot;
import org.libvirt.LibvirtException;

// Supertype doesn't declare generic types, can't do anything to fix that and still be able to override.
@SuppressWarnings("rawtypes")
@Extension
public class LibvirtSnapshotRevertRunListener extends RunListener<Run> {

    @Override
    public void onStarted(Run r, TaskListener listener) {
        Computer computer = r.getExecutor().getOwner();
        Node node = computer.getNode();

        if (node instanceof VirtualMachineSlave) {
            VirtualMachineSlave slave = (VirtualMachineSlave) node;

            // If a snapshot has been configured for this feature, proceed.
            String beforeJobSnapshotName = slave.getBeforeJobSnapshotName();
            if (beforeJobSnapshotName != null && beforeJobSnapshotName.length() > 0) {

                SlaveComputer slaveComputer = slave.getComputer();
                ComputerLauncher launcher = slave.getComputer().getLauncher();
                if (launcher instanceof VirtualMachineLauncher) {
                    VirtualMachineLauncher vmLauncher = (VirtualMachineLauncher) launcher;

                    VirtualMachine virtualMachine = vmLauncher.getVirtualMachine();

                    try {
                        Map<String, Domain> computers = virtualMachine.getHypervisor().getDomains();
                        String virtualMachineName = virtualMachine.getName();
                        Domain domain = computers.get(virtualMachineName);

                        if (domain != null) {
                            listener.getLogger().println("Reverting " + virtualMachineName + " to snapshot " + beforeJobSnapshotName + ".");

                            try {
                                DomainSnapshot snapshot = domain.snapshotLookupByName(beforeJobSnapshotName);
                                try {
                                    domain.revertToSnapshot(snapshot);

                                    listener.getLogger().println("Relaunching " + virtualMachineName + ".");
                                    // Relaunch
                                    try {
                                        vmLauncher.launch(slaveComputer, listener);
                                    } catch (IOException e) {
                                        listener.fatalError("Can't relaunch VM: " + e);
                                    } catch (InterruptedException e) {
                                        listener.fatalError("Can't relaunch VM: " + e);
                                    }
                                } catch (LibvirtException e) {
                                    listener.fatalError("No snapshot named " + beforeJobSnapshotName + " for VM: " + e);
                                }
                            } catch (LibvirtException e) {
                                listener.fatalError("No snapshot named " + beforeJobSnapshotName + " for VM: " + e);
                            }
                        } else {
                            listener.fatalError("No VM named " + virtualMachineName);
                        }
                    } catch (LibvirtException e) {
                        listener.fatalError("Can't get VM domains: " + e);
                    }
                }
            }
        }
    }
}
