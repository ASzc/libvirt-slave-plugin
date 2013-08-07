/**
 *  Copyright (C) 2010, Byte-Code srl <http://www.byte-code.com>
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Date: Mar 04, 2010
 * @author Marco Mornati<mmornati@byte-code.com>
 */

package hudson.plugins.libvirt;

import java.util.Map;

import hudson.slaves.ComputerLauncher;
import hudson.slaves.SlaveComputer;
import hudson.model.Node;
import hudson.model.Slave;
import org.libvirt.Connect;
import org.libvirt.Domain;
import org.libvirt.DomainSnapshot;
import org.libvirt.LibvirtException;
import org.libvirt.DomainInfo.DomainState;

public class VirtualMachineSlaveComputer extends SlaveComputer {

    /**
     * Cached connection to the virtaul datacenter. Lazily fetched.
     */
    private volatile Connect hypervisorConnection;

    public VirtualMachineSlaveComputer(Slave slave) {
        super(slave);
    }

    @Override
    protected ComputerLauncher grabLauncher(Node node) {
        // Revert to beforeJobSnapshot before the job gets the launcher
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
                            //listener.getLogger().println("Reverting " + vmName + " to snapshot " + beforeJobSnapshotName + ".");

                            try {
                                DomainSnapshot snapshot = domain.snapshotLookupByName(beforeJobSnapshotName);
                                try {
                                    domain.revertToSnapshot(snapshot);

                                    DomainState state = domain.getInfo().state;
                                    if (!DomainState.VIR_DOMAIN_BLOCKED.equals(state) && !DomainState.VIR_DOMAIN_RUNNING.equals(state)) {
                                        //listener.getLogger().println("Starting VM.");
                                        try {
                                            domain.create();
                                        } catch (LibvirtException e) {
                                            //listener.fatalError("Could not start VM: " + e);
                                        }
                                    }

                                    slave.getComputer().connect(true);
                                } catch (LibvirtException e) {
                                    //listener.fatalError("No snapshot named " + beforeJobSnapshotName + " for VM: " + e);
                                }
                            } catch (LibvirtException e) {
                                //listener.fatalError("No snapshot named " + beforeJobSnapshotName + " for VM: " + e);
                            }
                        } else {
                            //listener.fatalError("No VM named " + vmName);
                        }
                    } catch (LibvirtException e) {
                        //listener.fatalError("Can't get VM domains: " + e);
                    }
                }
            }
        }

        return super.grabLauncher(node);
    }
}
