Jenkins  VDirectorManager plugin
================

Read more: [ TODO : Doc  Link  published  after  workflow   is  standardised ]


Development
===========

Start the local Jenkins instance:

    mvn hpi:run


How to install
--------------

Run

	mvn clean package

to create the plugin .hpi file.


To install:

1. copy the resulting ./target/credentials.hpi file to the $JENKINS_HOME/plugins directory. Don't forget to restart Jenkins afterwards.

2. or use the plugin management console (http://example.com:8080/pluginManager/advanced) to upload the hpi file. You have to restart Jenkins in order to find the pluing in the installed plugins list.


Plugin releases
---------------

	mvn release:prepare release:perform -B


User Guidelines
===============

This plugin add to Jenkins CI a way to control Virtual Machines hosted on VMware  VCloud Director 
You can configure a Jenkins Slave, selecting a virtual machine from a Organization / Workspace / Configuration triplet,
In this way, when you need to build a Job on a specific Slave, this VM will be startup up and shutdown or suspended again after the build process.


Cloud  Config 
---------------

The first step is to configure Hudson to know what configuration in VCloud Director you will be using. 

To do this you need to add a new "Cloud" in the Hudson "Configure System" menu.


The required parameters to setup are:


	1.VCloud Director URL : https://<mycloud>/
	2.Brief description: A textual description
	3.Organization:  
	4.Username: username to use to connect with VCloud Director 
	5.Password: the password for this account 
	6.Catalogue Name : The Catalog  name in VCloud Director where the virtual machines reside. 

To verify all you parameters you can click on Test button and check the output reported.


Slaves Config
---------------

Now you can setup your nodes in hudson and use them to build your projects.


On the creation page you just simply select the correct radio button to configure a slave that runs inside of VCloud Director.
Going ahead with configuration you can see a page that looks like the normal node creation page, 
with three combo box added.

	1. Select the VCloud Director instance  (the brief description provided in the configuration section). 
	2. Name of the Virtual Machine in VCloud Director configuration that you are using. 
	3. Action to be taken when the VM is idle (it is recommended to pick shutdown over suspend due to overhead in VCloud Director).
	4. Script  to run   before actual  job is run  (  Maybe a  shell script  or  an  Artifactory  Plugin Invlcation to fetch packages )
	5. Post  install  Run Script 
	6. Slave  Credentials
	7. Delay between telling to get the VM online and Jenkins attempting to connect to it as a slave.



License
=========

	(The MIT License)

    Copyright © 2013

    Permission is hereby granted, free of charge, to any person obtaining a copy
    of this software and associated documentation files (the "Software"), to deal
    in the Software without restriction, including without limitation the rights
    to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
    copies of the Software, and to permit persons to whom the Software is
    furnished to do so, subject to the following conditions:

    The above copyright notice and this permission notice shall be included in
    all copies or substantial portions of the Software.

    THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
    IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
    FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
    AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
    LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
    OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
    THE SOFTWARE.
