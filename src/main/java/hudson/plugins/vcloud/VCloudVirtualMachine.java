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

import org.kohsuke.stapler.DataBoundConstructor;

import java.io.Serializable;

/**
 * 
 */
public class VCloudVirtualMachine implements Serializable, Comparable<VCloudVirtualMachine> {
    private final VCloudDirector vcloud;
    private final String name;

    @DataBoundConstructor
    public VCloudVirtualMachine(VCloudDirector vdirector, String name) {
        this.vcloud = vdirector;
        this.name = name;
    }

    public VCloudDirector getVcloud() {
        return vcloud;
    }

    public String getId() {
        return name;
    }
    
    
    public String getName() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof VCloudVirtualMachine)) {
            return false;
        }

        VCloudVirtualMachine that = (VCloudVirtualMachine) o;

        if (vcloud != null ? !vcloud.equals(that.vcloud) : that.vcloud != null) {
            return false;
        }
        if (name != null ? !name.equals(that.name) : that.name != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        return 31 * result + (vcloud != null ? vcloud.hashCode() : 0);
    }

    public String getDisplayName() {
        return this.toString();
    }

    public int compareTo(VCloudVirtualMachine o) {
        return name.compareTo(o.getName());
    }

    @Override
    public String toString() {
        return vcloud.toString() + ":" + name;
    }

    int getStatus() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
