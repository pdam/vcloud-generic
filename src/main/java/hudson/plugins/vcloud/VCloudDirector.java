/*
 * Copyright (c) 2013, iland Internet Solutions
 * 
 * 
 * This software is licensed under the Terms and Conditions contained within the
 * "LICENSE.txt" file that accompanied this software. Any inquiries concerning
 * the scope or enforceability of the license should be addressed to:
 * 
 * iland Internet Solutions 1235 North Loop West, Suite 205 Houston, TX 77008
 * USA
 * 
 * http://www.iland.com
 */
package hudson.plugins.vcloud;

import hudson.util.FormValidation;
import hudson.util.Scrambler;
import hudson.Util;
import hudson.model.Descriptor;
import hudson.model.Label;
import hudson.Extension;
import hudson.slaves.Cloud;
import hudson.slaves.NodeProvisioner;
import java.util.ArrayList;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentHashMap;

import net.sf.json.JSONObject;
import org.kohsuke.stapler.StaplerRequest;

import java.security.Security;
import java.security.KeyStore;
import java.security.Provider;
import java.security.cert.X509Certificate;
import javax.net.ssl.ManagerFactoryParameters;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactorySpi;
import javax.net.ssl.X509TrustManager;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeoutException;

import javax.xml.bind.JAXBElement;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.vmware.vcloud.api.rest.schema.InstantiateVAppTemplateParamsType;
import com.vmware.vcloud.api.rest.schema.InstantiationParamsType;
import com.vmware.vcloud.api.rest.schema.NetworkConfigSectionType;
import com.vmware.vcloud.api.rest.schema.NetworkConfigurationType;
import com.vmware.vcloud.api.rest.schema.NetworkConnectionSectionType;
import com.vmware.vcloud.api.rest.schema.NetworkConnectionType;
import com.vmware.vcloud.api.rest.schema.ObjectFactory;
import com.vmware.vcloud.api.rest.schema.ReferenceType;
import com.vmware.vcloud.api.rest.schema.VAppNetworkConfigurationType;
import com.vmware.vcloud.api.rest.schema.ovf.MsgType;
import com.vmware.vcloud.api.rest.schema.ovf.SectionType;
import com.vmware.vcloud.sdk.Organization;
import com.vmware.vcloud.sdk.Task;
import com.vmware.vcloud.sdk.VCloudException;
import com.vmware.vcloud.sdk.VM;
import com.vmware.vcloud.sdk.Vapp;
import com.vmware.vcloud.sdk.Vdc;
import static com.vmware.vcloud.sdk.Vdc.getVdcByReference;
import com.vmware.vcloud.sdk.constants.FenceModeValuesType;
import com.vmware.vcloud.sdk.constants.IpAddressAllocationModeType;
import com.vmware.vcloud.sdk.constants.UndeployPowerActionType;
import com.vmware.vcloudapi.ConfigConstants;
import com.vmware.vcloudapi.Configuration;
import java.security.KeyManagementException;
import java.security.UnrecoverableKeyException;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * Splunk Sample.
 *
 * @author <a href="mailto:janguenot@iland.com">Julien Anguenot</a>
 * Modified  by pdam  for  Splunk Inc
 *
 */

public class VCloudDirector extends Cloud {

    private static final Log log = LogFactory.getLog(VCloudDirector.class);
    private final String vcloudhost;
    private final String vdDescription;
    private final String organization;
    private final String vdconfiguration;
    private final String username;
    private final String password;
    private final int maxOnlineSlaves;
    private transient int currentOnlineSlaveCount = 0;
    private transient ArrayList currentOnlineSlaves;
    private final VCDConnection con;

    public String getVcloudhost() {
        return vcloudhost;
    }

    public String getvdDescription() {
        return vdDescription;
    }

    public String getvdOrganization() {
        return organization;
    }

    public String getvdConfiguration() {
        return vdconfiguration;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return Scrambler.descramble(password);
    }

    public int getMaxOnlineSlaves() {
        return maxOnlineSlaves;
    }

    /**
     * @param vmName The name of the slave we're bringing online.
     */
    public synchronized int markOneSlaveOnline(String vmName) {
        if (currentOnlineSlaves == null) {
            currentOnlineSlaves = new ArrayList();
        }
        currentOnlineSlaves.add(vmName);
        return ++currentOnlineSlaveCount;
    }

    /**
     * @param vmName The name of the slave we're bringing offline.
     */
    public synchronized int markOneSlaveOffline(String vmName) {
        if (currentOnlineSlaves.contains(vmName)) {
            currentOnlineSlaves.remove(vmName);
            return --currentOnlineSlaveCount;
        } else {
            return currentOnlineSlaveCount;
        }
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("VCloudHost");
        sb.append("{Host='").append(vcloudhost).append('\'');
        sb.append(", Description='").append(vdDescription).append('\'');
        sb.append(", Organization='").append(organization).append('\'');
        sb.append(", MaxSlavesOnline='").append(maxOnlineSlaves).append('\'');
        sb.append(", Configuration='").append(vdconfiguration).append('\'');
        sb.append('}');
        return sb.toString();
    }

    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) super.getDescriptor();
    }

    List<VCloudVirtualMachine> getVCloudVirtualMachines() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Extension
    public static final class DescriptorImpl extends Descriptor<Cloud> {

        public final ConcurrentMap<String, VCloudDirector> hypervisors = new ConcurrentHashMap<String, VCloudDirector>();
        private String vcloudHost;
        private String vdOrganization;
        private String vdcConfiguration;
        private String username;
        private String password;
        private int maxOnlineSlaves;
        private List<VCloudVirtualMachine> vmList;
        private String vdorganization;

        @Override
        public String getDisplayName() {
            return "VCloud";
        }

     
   public synchronized List<VCloudVirtualMachine> getVCloudVirtualMachines() {
         vmList = getVCloudVirtualMachines();
        List<VCloudVirtualMachine> vmList = new ArrayList<VCloudVirtualMachine>();
        /* Get the list of machines.  We do this by asking for our
         * configuration and then passing that ID to a request for
         * listMachines.
         */
        
        return vmList;
    }
        
        
        @Override
        public boolean configure(StaplerRequest req, JSONObject o)
                throws FormException {
            vcloudHost = o.getString("vcloudHost");
            vdorganization  = o.getString("vdOrganization");
            vdcConfiguration = o.getString("vdConfiguration");
            username = o.getString("username");
            password = o.getString("password");
            maxOnlineSlaves = o.getInt("maxOnlineSlaves");
            save();
            return super.configure(req, o);
        }

        /**
         * For UI.
         */
        public FormValidation doTestConnection(@QueryParameter String vcloudHost,
                @QueryParameter String vdOrganization,
                @QueryParameter String vdDescription,
                @QueryParameter String lmWorkspace,
                @QueryParameter String lmConfiguration,
                @QueryParameter String username,
                @QueryParameter String password,
                @QueryParameter int maxOnlineSlaves) {
            try {
                /* We know that these objects are not null */
                if (vcloudHost.length() == 0) {
                    return FormValidation.error("VCloud host is not specified");
                } else {
                    /* Perform other sanity checks. */
                    if (!vcloudHost.startsWith("https://")) {
                        return FormValidation.error("VCloud host must start with https://");
                    }
                }

                if (vdOrganization.length() == 0) {
                    return FormValidation.error("VCloud organization is not specified");
                }

                if (lmConfiguration.length() == 0) {
                    return FormValidation.error("VCloud configuration is not specified");
                }

                if (username.length() == 0) {
                    return FormValidation.error("Username is not specified");
                }

                if (password.length() == 0) {
                    return FormValidation.error("Password is not specified");
                }

                /* Install the all-trusting trust manager */
                Security.addProvider(new DummyTrustProvider());
                Security.setProperty("ssl.TrustManagerFactory.algorithm",
                        "TrustAllCertificates");

                /* Try and connect to it.  iLandCode*/
                VCloudDirector splunkVcd = new VCloudDirector(vcloudHost, vdDescription, vdOrganization, vdcConfiguration, username, password, maxOnlineSlaves);

                Vdc vdc = splunkVcd.getVdcByName(splunkVcd.vdconfiguration);
                if (vdc != null) {
                    System.out.println(String.format("We found the actual VDC with name=%s", vdc));
                    return FormValidation.ok("Connected successfully");
                } else {
                    return FormValidation.error("Could not login and retrieve basic information to confirm setup");
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    /* This is something that we need to make sure
     * happens when Hudson is restarted for example. */
    private void fixTrustManager() {
        /* Install the all-trusting trust manager */
        Security.addProvider(new DummyTrustProvider());
        Security.setProperty("ssl.TrustManagerFactory.algorithm",
                "TrustAllCertificates");
    }

    @DataBoundConstructor
    public VCloudDirector(String vcloudHost, String vdDescription, String vdOrganization, String vdcConfiguration, String username, String password, int maxOnlineSlaves) throws VCloudException, KeyManagementException, UnrecoverableKeyException {
        super("VCloud");
        this.vcloudhost = vcloudHost;
        this.vdDescription = vdDescription;
        this.organization = vdOrganization;
        this.vdconfiguration = vdcConfiguration;
        this.username = username;
        this.password = Scrambler.scramble(Util.fixEmptyAndTrim(password));
        this.maxOnlineSlaves = maxOnlineSlaves;
        /* Setup our auth token. */
        con = VCDConnection.getInstance();
        Vdc vdc = getVdcByName(this.vdconfiguration);
        if (vdc != null) {
            log.info(String.format("We found the actual VDC with name=%s", vdc
                    .getResource().getName()));
        }

    }

    public ArrayList<String> getListOfVappTemplates() throws VCloudException, KeyManagementException, UnrecoverableKeyException {
        ArrayList<String> templateLists = new ArrayList<String>();
        Organization org = getOrgByName(this.organization);
        ReferenceType vdcRef = org.getVdcRefByName(this.vdconfiguration);
        Collection<ReferenceType> vappTemplateLists = getVdcByReference(con.getClient(), vdcRef).getVappTemplateRefs();
        for (Iterator<ReferenceType> it = vappTemplateLists.iterator(); it.hasNext();) {
            String templateName = it.next().getName();
            System.out.println("Template :" + templateName);
            templateLists.add(templateName);
        }
        return templateLists;

    }

    public Organization getOrgByName(String orgName) throws VCloudException, KeyManagementException, UnrecoverableKeyException {
        return Organization.getOrganizationByReference(con.getClient(), con
                .getClient().getOrgRefByName(this.organization));
    }

    public Vdc getVdcByName(String vdcName) throws VCloudException, KeyManagementException, UnrecoverableKeyException {
        Organization org = getOrgByName(this.organization);
        ReferenceType vdcRef = org.getVdcRefByName(vdcName);
        return Vdc.getVdcByReference(con.getClient(), vdcRef);
    }

    public ReferenceType getVappTemplateByName(Vdc vdc, String templateName)
            throws VCloudException {
        Collection<ReferenceType> vappLinks = vdc
                .getVappTemplateRefsByName(templateName);
        if (!vappLinks.isEmpty()) {
            return vappLinks.iterator().next();
        }
        return null;
    }

    public Vapp newvAppFromTemplate(String templateName, Vdc vdc)
            throws VCloudException {

        ReferenceType vAppTemplateReference = getVappTemplateByName(vdc, templateName);
        NetworkConfigurationType networkConfiguration = new NetworkConfigurationType();
        networkConfiguration.setParentNetwork(vdc.getAvailableNetworkRefs()
                .iterator().next());
        networkConfiguration
                .setFenceMode(FenceModeValuesType.NATROUTED.value());

        VAppNetworkConfigurationType vAppNetworkConfiguration = new VAppNetworkConfigurationType();
        vAppNetworkConfiguration.setConfiguration(networkConfiguration);
        vAppNetworkConfiguration.setNetworkName(vdc.getAvailableNetworkRefs()
                .iterator().next().getName());

        NetworkConfigSectionType networkConfigSection = new NetworkConfigSectionType();
        MsgType networkInfo = new MsgType();
        networkConfigSection.setInfo(networkInfo);
        List<VAppNetworkConfigurationType> vAppNetworkConfigs = networkConfigSection
                .getNetworkConfig();
        vAppNetworkConfigs.add(vAppNetworkConfiguration);

        InstantiationParamsType instantiationParams = new InstantiationParamsType();
        List<JAXBElement<? extends SectionType>> sections = instantiationParams
                .getSection();
        sections.add(new ObjectFactory()
                .createNetworkConfigSection(networkConfigSection));

        InstantiateVAppTemplateParamsType instVappTemplParams = new InstantiateVAppTemplateParamsType();
        instVappTemplParams.setName("New " + getVappTemplateByName(vdc, templateName) + " "
                + UUID.randomUUID());
        instVappTemplParams.setSource(vAppTemplateReference);
        instVappTemplParams.setInstantiationParams(instantiationParams);

        Vapp vapp = vdc.instantiateVappTemplate(instVappTemplParams);
        return vapp;

    }

    public void configureVMsIPAddressingMode(ReferenceType vappRef, Vdc vdc)
            throws VCloudException, TimeoutException, KeyManagementException, UnrecoverableKeyException {
        Vapp vapp = Vapp.getVappByReference(con.getClient(), vappRef);
        List<VM> childVms = vapp.getChildrenVms();
        for (VM childVm : childVms) {
            NetworkConnectionSectionType networkConnectionSection = childVm
                    .getNetworkConnectionSection();
            List<NetworkConnectionType> networkConnections = networkConnectionSection
                    .getNetworkConnection();
            for (NetworkConnectionType networkConnection : networkConnections) {
                networkConnection
                        .setIpAddressAllocationMode(IpAddressAllocationModeType.POOL
                                .value());
                networkConnection.setNetwork(vdc.getAvailableNetworkRefs()
                        .iterator().next().getName());
            }
            childVm.updateSection(networkConnectionSection).waitForTask(0);
            for (String ip : VM
                    .getVMByReference(con.getClient(), childVm.getReference())
                    .getIpAddressesById().values()) {
                log.info("IP if vm is:" + ip);
            }
        }

    }

    /**
     * This is taken from:
     * http://knowledgehub.zeus.com/articles/2006/01/03/using_the_control_api_with_java
     * The following code disables certificate checking. Use the
     * Security.addProvider and Security.setProperty calls to enable it.
     *
     */
    private static class DummyTrustProvider extends Provider {

        public DummyTrustProvider() {
            super("DummyTrustProvider", 1.0, "Trust certificates");
            put("TrustManagerFactory.TrustAllCertificates",
                    MyTrustManagerFactory.class.getName());
        }

        protected static class MyTrustManagerFactory extends TrustManagerFactorySpi {

            public MyTrustManagerFactory() {
            }

            protected void engineInit(KeyStore keystore) {
            }

            protected void engineInit(ManagerFactoryParameters mgrparams) {
            }

            protected TrustManager[] engineGetTrustManagers() {
                return new TrustManager[]{new MyX509TrustManager()};
            }
        }

        protected static class MyX509TrustManager implements X509TrustManager {

            public void checkClientTrusted(X509Certificate[] chain, String authType) {
            }

            public void checkServerTrusted(X509Certificate[] chain, String authType) {
            }

            public X509Certificate[] getAcceptedIssuers() {
                return null;
            }
        }
    }

    public void powerOnVappTemplate(Vdc vdc, String vAppTemplateName) throws VCloudException, TimeoutException, KeyManagementException, UnrecoverableKeyException {
        ReferenceType refType = getVappTemplateByName(vdc,
                vAppTemplateName);
        if (refType != null) {
            log.info(String.format("We found the actual template with name=%s",
                    refType.getName()));
        }
        Vapp vapp = newvAppFromTemplate(vAppTemplateName, vdc);
        List<Task> tasks = vapp.getTasks();
        if (tasks.size() > 0) {
            tasks.get(0).waitForTask(0);
        }

        configureVMsIPAddressingMode(vapp.getReference(), vdc);

        String vappName = vapp.getReference().getName();
        log.info("Deploying the " + vappName);
        vapp.deploy(false, 1000000, false).waitForTask(0);

        log.info("PowerOn the " + vappName);
        vapp.powerOn().waitForTask(0);

        log.info("Suspend the " + vappName);
        vapp.suspend().waitForTask(0);

        log.info("PowerOn the " + vappName);
        vapp.powerOn().waitForTask(0);

        log.info("PowerOff the " + vappName);
        vapp.powerOff().waitForTask(0);

        log.info("Undeploy the " + vappName);
        vapp.undeploy(UndeployPowerActionType.SHUTDOWN).waitForTask(0);

        log.info("Delete the " + vappName);
        vapp.delete().waitForTask(0);
    }

    public static void main(String args[]) throws VCloudException,
            TimeoutException,
            KeyManagementException,
            UnrecoverableKeyException {

        String vcloudHost = "https://res01.ilandcloud.com";
        String vdDescription = "vdc1";
        String vdOrganization = "Splunk-130810582";
        String vdcConfiguration = "Splunk RES vDC";
        String username = "ilandsplunk@Splunk-130810582";
        String password = "splunk1235";
        int maxOnlineSlaves = Integer.parseInt("9");
        String vAppTemplateName = "centos-gold";
        VCloudDirector splunkVcd = new VCloudDirector(vcloudHost, vdDescription, vdOrganization, vdcConfiguration, username, password, maxOnlineSlaves);

        Vdc vdc = splunkVcd.getVdcByName(splunkVcd.vdconfiguration);
        if (vdc != null) {
            log.info(String.format("We found the actual VDC with name=%s", vdc
                    .getResource().getName()));
        }
        ReferenceType refType = splunkVcd.getVappTemplateByName(vdc,
                vAppTemplateName);
        if (refType != null) {
            log.info(String.format("We found the actual template with name=%s",
                    refType.getName()));
        }

        Vapp vapp = splunkVcd.newvAppFromTemplate(vAppTemplateName, vdc);
        List<Task> tasks = vapp.getTasks();
        if (tasks.size() > 0) {
            tasks.get(0).waitForTask(0);
        }
        splunkVcd.getListOfVappTemplates();
        splunkVcd.configureVMsIPAddressingMode(vapp.getReference(), vdc);

        String vappName = vapp.getReference().getName();
        log.info("Deploying the " + vappName);
        vapp.deploy(false, 1000000, false).waitForTask(0);

        log.info("PowerOn the " + vappName);
        vapp.powerOn().waitForTask(0);

        log.info("Suspend the " + vappName);
        vapp.suspend().waitForTask(0);

        log.info("PowerOn the " + vappName);
        vapp.powerOn().waitForTask(0);

        log.info("PowerOff the " + vappName);
        vapp.powerOff().waitForTask(0);

        log.info("Undeploy the " + vappName);
        vapp.undeploy(UndeployPowerActionType.SHUTDOWN).waitForTask(0);

        log.info("Delete the " + vappName);
        vapp.delete().waitForTask(0);

    }

     public Collection<NodeProvisioner.PlannedNode> provision(Label label, int i) {
        return Collections.emptySet();
    }

    public boolean canProvision(Label label) {
        return false;
    }
}
