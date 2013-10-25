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

import hudson.Plugin;
import hudson.model.Hudson;
import hudson.slaves.Cloud;
import hudson.util.ListBoxModel;

import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletException;

import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

/**
 * @author pdam
 */
public class PluginImpl extends Plugin {
    private static final java.util.logging.Logger LOGGER = Logger.getLogger(PluginImpl.class.getName());

    /**
     * {@inheritDoc}
     */
    @Override
    public void start() throws Exception {
        LOGGER.log(Level.FINE, "Starting vcloud plugin");
        super.start();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void stop() throws Exception {
        LOGGER.log(Level.FINE, "Stopping vcloud plugin.");
        super.stop();
    }

    public void doComputerNameValues(StaplerRequest req, StaplerResponse rsp,
            @QueryParameter("value") String value)
            throws IOException, ServletException {
        ListBoxModel m = new ListBoxModel();
        List<VCloudVirtualMachine> virtualMachines = null;
        for (Cloud cloud : Hudson.getInstance().clouds) {
            if (cloud instanceof VCloudDirector) {
                VCloudDirector vcloud = (VCloudDirector) cloud;
                if (value != null &&
                        value.equals(vcloud.getvdDescription())) {
                    virtualMachines = vcloud.getVCloudVirtualMachines();
                    break;
                }
            }
        }
        if (virtualMachines != null) {
            for (VCloudVirtualMachine vm : virtualMachines) {
                m.add(new ListBoxModel.Option(vm.getName(), vm.getName()));
            }
            m.get(0).selected = true;
        }
        m.writeTo(req, rsp);
    }
}
