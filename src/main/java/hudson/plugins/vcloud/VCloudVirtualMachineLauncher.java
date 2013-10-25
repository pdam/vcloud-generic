/**
 *  Copyright 
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
 * Based on the libvirt-plugin which is:
 *  Copyright (C) 2010, Byte-Code srl <http://www.byte-code.com>
 *
 *  Date: Oct  26 , 2013
 * Author: Pratik  Dam
 */
package hudson.plugins.vcloud;

import com.vmware.vcloud.sdk.VCloudException;
import com.vmware.vcloud.sdk.VcloudClient;
import com.vmware.vcloud.sdk.Vdc;
import hudson.slaves.ComputerLauncher;
import hudson.slaves.SlaveComputer;
import hudson.model.TaskListener;
import hudson.model.Descriptor;
import hudson.model.Hudson;
import hudson.slaves.Cloud;
import hudson.Util;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.UnrecoverableKeyException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.kohsuke.stapler.DataBoundConstructor;

import java.util.ArrayList;

/**
 * {@link ComputerLauncher} for VCloud that waits for the Virtual Machine
 * to really come up before proceeding to the real user-specified
 * {@link ComputerLauncher}.
 *
 * @author Tom Rini <tom_rini@mentor.com>
 */
public class VCloudVirtualMachineLauncher extends ComputerLauncher {

    private static final Logger LOGGER = Logger.getLogger(VCloudVirtualMachineLauncher.class.getName());
    private ComputerLauncher delegate;
    private String vdDescription;
    private String vmName;
    private int idleAction;
    private Boolean overrideLaunchSupported;
    private int launchDelay;

    /**
     * Constants.
     */
    /* Machine status codes. */
    private static final int MACHINE_STATUS_OFF = 1;
    private static final int MACHINE_STATUS_ON = 2;
    private static final int MACHINE_STATUS_SUSPENDED = 3;
    private static final int MACHINE_STATUS_STUCK = 4;
    private static final int MACHINE_STATUS_INVALID = 128;

    /* Machine action codes. */
    private static final int MACHINE_ACTION_ON = 1;
    private static final int MACHINE_ACTION_OFF = 2;
    private static final int MACHINE_ACTION_SUSPEND = 3;
    private static final int MACHINE_ACTION_RESUME = 4;
    private static final int MACHINE_ACTION_RESET = 5;
    private static final int MACHINE_ACTION_SNAPSHOT = 6;
    private static final int MACHINE_ACTION_REVERT = 7;
    private static final int MACHINE_ACTION_SHUTDOWN = 8;
    private VcloudClient vdc;

    /**
     * @param delegate The real {@link ComputerLauncher} we have been passed.
     * @param vdDescription Human reable description of the VCloud
     * instance.
     * @param vmName The 'VM Name' field in the configuration in VCloud.
     * @param idleOption The choice of action to take when the slave is deemed
     * idle.
     * @param overrideLaunchSupported Boolean to set of we force
     * isLaunchSupported to always return True.
     * @param launchDelay How long to wait between bringing up the VM and
     * trying to connect to it as a slave.
     */
    @DataBoundConstructor
    public VCloudVirtualMachineLauncher(ComputerLauncher delegate,
                    String vdDescription, String vmName, String idleOption,
                    Boolean overrideLaunchSupported, String launchDelay) {
        super();
        this.delegate = delegate;
        this.vdDescription = vdDescription;
        this.vmName = vmName;
        if ("Shutdown".equals(idleOption))
            idleAction = MACHINE_ACTION_SHUTDOWN;
        else if ("Shutdown and Revert".equals(idleOption))
            idleAction = MACHINE_ACTION_REVERT;
        else
            idleAction = MACHINE_ACTION_SUSPEND;
        this.overrideLaunchSupported = overrideLaunchSupported;
        this.launchDelay = Util.tryParseNumber(launchDelay, 60).intValue();
    }

    /**
     * Determine what VCloudDirector object controls this slave.  Once we have
     * that we can call and get the information out that we need to perform
     * SOAP calls.
     */
    public VCloudDirector findOurLmInstance() throws RuntimeException {
        if (vdDescription != null && vmName != null) {
            VCloudDirector vcloud = null;
            for (Cloud cloud : Hudson.getInstance().clouds) {
                if (cloud instanceof VCloudDirector && ((VCloudDirector) cloud).getvdDescription().equals(vdDescription)) {
                    vcloud = (VCloudDirector) cloud;
                    return vcloud;
                }
            }
        }
        LOGGER.log(Level.SEVERE, "Could not find our VCloud instance!");
        throw new RuntimeException("Could not find our VCloud instance!");
    }

    /**
     * We have stored inside of the VCloudDirector object all of the information
 needed to get the Machine object back out.  We cannot store the
     * machineId value itself so we work our way towards it by getting the
     * Configuration we know the machine lives in and then returning the
     * Machine object.  We know that the machine name is unique to the
     * configuration.
     */
    private  VCloudVirtualMachine getMachine(VCloudDirector vdirect,
                    Vdc vdc ,  String  template) throws VCloudException, KeyManagementException, UnrecoverableKeyException {
        VCloudVirtualMachine vm =new VCloudVirtualMachine(vdirect ,  template) ; 
        String  vName  =   vm.getName();
        ArrayList<String>  aom   = vdirect.getListOfVappTemplates();
        for (String mach : aom) {
            if (mach.equals(this.vmName))
               return vm;
        }

        return vm;
    }

    /**
     * Perform the specified action on the specified machine via SOAP.
     */
    private static void performAction(VCloudDirector vdirect, Vdc vdc
            , VCloudVirtualMachine vm, int action) 
            throws VCloudException {
        /**MachinePerformAction mpaReq = new MachinePerformAction();
        mpaReq.setAction(action);
        mpaReq.setMachineId(vm.getId());
         We can't actually do anything here, problems come
         s an exception I believe. 
        vdirect.machinePerformAction(mpaReq, vdc , action);*/
    }

    /**
     * Do the real work of launching the machine via SOAP.
     */
    @Override
    public void launch(SlaveComputer slaveComputer, TaskListener taskListener)
            throws IOException, InterruptedException {
        taskListener.getLogger().println("Starting Virtual Machine...");
        /**
         * What we know is that at least at one point this particular
         * machine existed.  But we want to be sure it still exists.
         * If it exists we can check the status.  If we are off,
         * power on.  If we are suspended, resume.  If we are on,
         * do nothing.  The problem is that we don't have the machineId
         * right now so we need to call our getMachine.
         */
        VCloudDirector vdirector  = getVdDescription();
       
        int machineAction = 0;
        VCloudVirtualMachine vm = null;
        try {
            try {
                vm = getMachine(vdirector, Vdc.getVdcById(vdc, vmName) , vmName);
            } catch (KeyManagementException ex) {
                Logger.getLogger(VCloudVirtualMachineLauncher.class.getName()).log(Level.SEVERE, null, ex);
            } catch (UnrecoverableKeyException ex) {
                Logger.getLogger(VCloudVirtualMachineLauncher.class.getName()).log(Level.SEVERE, null, ex);
            }
        } catch (VCloudException ex) {
            Logger.getLogger(VCloudVirtualMachineLauncher.class.getName()).log(Level.SEVERE, null, ex);
        }

        /* Determine the current state of the VM. */
        switch (vm.getStatus()) {
            case MACHINE_STATUS_OFF:
                machineAction = MACHINE_ACTION_ON;
                break;
            case MACHINE_STATUS_SUSPENDED:
                machineAction = MACHINE_ACTION_RESUME;
                break;
            case MACHINE_STATUS_ON:
                /* Nothing to do */
                break;
            case MACHINE_STATUS_STUCK:
            case MACHINE_STATUS_INVALID:
                LOGGER.log(Level.SEVERE, "Problem with the machine status!");
                throw new IOException("Problem with the machine status");
        }

        /* Perform the action, if needed.  This will be sleeping until
         * it returns from the server. */
        if (machineAction != 0)
            try {
                performAction(vdirector, Vdc.getVdcById(vdc, vmName), vm, machineAction);
        } catch (VCloudException ex) {
            Logger.getLogger(VCloudVirtualMachineLauncher.class.getName()).log(Level.SEVERE, null, ex);
        }

        try {
            /* At this point we have told VCloud to get the VM going.
             * Now we wait our launch delay amount before trying to connect. */
            Thread.sleep(launchDelay * 1000);
            delegate.launch(slaveComputer, taskListener);
        } finally {
            /* If the rest of the launcher fails, we free up a space. */
            if (slaveComputer.getChannel() == null)
                vdirector.markOneSlaveOffline(slaveComputer.getDisplayName());
        }
    }

    /**
     * Handle bringing down the Virtual Machine.
     */
    @Override
    public void afterDisconnect(SlaveComputer slaveComputer,
                    TaskListener taskListener) {
        taskListener.getLogger().println("Running disconnect procedure...");
        delegate.afterDisconnect(slaveComputer, taskListener);
        taskListener.getLogger().println("Shutting down Virtual Machine...");

        VCloudDirector vdirector = findOurLmInstance();
        vdirector.markOneSlaveOffline(slaveComputer.getDisplayName());
        Vdc vdc = null;
        try {
            vdc = vdirector.getVdcByName(this.vdDescription);
        } catch (VCloudException ex) {
            Logger.getLogger(VCloudVirtualMachineLauncher.class.getName()).log(Level.SEVERE, null, ex);
        } catch (KeyManagementException ex) {
            Logger.getLogger(VCloudVirtualMachineLauncher.class.getName()).log(Level.SEVERE, null, ex);
        } catch (UnrecoverableKeyException ex) {
            Logger.getLogger(VCloudVirtualMachineLauncher.class.getName()).log(Level.SEVERE, null, ex);
        }

        try {
            int machineAction = 0;
            VCloudVirtualMachine vm = getMachine(vdirector, vdc , vmName);

            /* Determine the current state of the VM. */
            switch (vm.getStatus()) {
                case MACHINE_STATUS_OFF:
                case MACHINE_STATUS_SUSPENDED:
                    break;
                case MACHINE_STATUS_ON:
                    /* In the case where our idleAction is Suspend and Revert
                     * we need to first perform the shutdown and then the
                     * revert.  We will make the shutdown action and sleep for
                     * 60 seconds to try and make sure we have shutdown or at
                     * least that our JNLP connection has terminated.  In the
                     * case of Suspend or just Shutdown, we perform the action.
                     */
                    switch (idleAction) {
                        case MACHINE_ACTION_REVERT:
                            performAction(vdirector, vdc , vm,
                                            MACHINE_ACTION_OFF);
                            taskListener.getLogger().println("Waiting 60 seconds for shutdown to complete.");
                            Thread.sleep(60000);
                        case MACHINE_ACTION_SUSPEND:
                        case MACHINE_ACTION_SHUTDOWN:
                            performAction(vdirector, vdc, vm,
                                            idleAction);
                            break;
                    }
                    break;
                case MACHINE_STATUS_STUCK:
                case MACHINE_STATUS_INVALID:
                    LOGGER.log(Level.SEVERE, "Problem with the machine status!");
            }
        } catch (Throwable t) {
            taskListener.fatalError(t.getMessage(), t);
        }
    }

    public String getLmDescription() {
        return vdDescription;
    }

    public String getVmName() {
        return vmName;
    }

    public ComputerLauncher getDelegate() {
        return delegate;
    }

    public Boolean getOverrideLaunchSupported() {
        return overrideLaunchSupported;
    }

    public void setOverrideLaunchSupported(Boolean overrideLaunchSupported) {
        this.overrideLaunchSupported = overrideLaunchSupported;
    }

    @Override
    public boolean isLaunchSupported() {
        if (this.overrideLaunchSupported == null)
            return delegate.isLaunchSupported();
        else {
                LOGGER.log(Level.FINE, "Launch support is overridden to always return: " + overrideLaunchSupported);
                return overrideLaunchSupported;
        }
    }

    @Override
    public void beforeDisconnect(SlaveComputer slaveComputer, TaskListener taskListener) {
        delegate.beforeDisconnect(slaveComputer, taskListener);
    }

    @Override
    public Descriptor<ComputerLauncher> getDescriptor() {
        // Don't allow creation of launcher from UI
        throw new UnsupportedOperationException();
    }

    private VCloudDirector getVdDescription() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
