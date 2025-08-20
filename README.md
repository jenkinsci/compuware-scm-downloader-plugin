# BMC AMI DevX Source Code Download for Endevor, PDS, and Code Pipeline

[![Jenkins Plugin](https://img.shields.io/jenkins/plugin/v/compuware-scm-downloader.svg)](https://plugins.jenkins.io/compuware-scm-downloader) [![GitHub release](https://img.shields.io/github/v/release/jenkinsci/compuware-scm-downloader.svg?label=release)](https://github.com/jenkinsci/compuware-scm-downloader-plugin/releases) [![Jenkins Plugin Installs](https://img.shields.io/jenkins/plugin/i/compuware-scm-downloader.svg?color=blue)](https://plugins.jenkins.io/compuware-scm-downloader)

<!-- ![](docs/images/compuware.topaz.png)![](docs/images/compuware.ispw.png) -->

## Overview

The BMC AMI DevX Source Code Download for Endevor, PDS, and Code Pipeline plugin allows users to download Endevor, PDS, and BMC AMI DevX Code Pipeline members from the mainframe. Downloaded Source can then be accessed for example, for SonarQube analysis and reporting or to populate a Git repository.

## Prerequisites

The following are required to use this plugin:
- Jenkins
- Jenkins Credentials Plugin
- Workbench CLI. Refer to the [Workbench Install Guide](https://docs.bmc.com/docs/bctwb/2007/topaz-workbench-installation-guide-1010112445.html) for instructions
- For PDS and Endevor downloads, BMC AMI DevX Code Analysis license is required
- Host Communications Interface

#### See also
* [License](LICENSE.txt)
* [Change Log](https://github.com/jenkinsci/compuware-scm-downloader-plugin/releases)

## Installing in a Jenkins Instance

1. Install the BMC AMI DevX Source Code Download for Endevor, PDS, and Code Pipeline plugin according to the Jenkins instructions for installing plugins. Dependent plugins will automatically be installed.
2. Install the Workbench CLI on the Jenkins instances that will execute the plugin. The Workbench CLI is available in the Workbench installation package. If you do not have the installation package, please visit [https://support.bmc.com](https://support.bmc.com). For Workbench CLI installation instructions, please refer to the [Workbench Install Guide](https://docs.bmc.com/docs/bctwb/2007/topaz-workbench-installation-guide-1010112445.html).

## Configuring for Workbench CLI & Host Connections

In order to download Endevor, PDS, and Code Pipeline members you will need to point to an installed Workbench Command Line Interface (CLI). The Workbench CLI will work with host connection(s) you also need to configure to download Endevor, PDS, and Code Pipeline members.

- See [Configuring for Workbench CLI & Host Connections](https://github.com/jenkinsci/compuware-common-configuration-plugin/blob/master/README.md#user-content-configuring-for-topaz-workbench-cli--host-connections)

### Downloading Endevor or PDS members

This integration allows downloading of Endevor or PDS members from the mainframe to the PC.

On the **Configuration** page of the job or project, select one of the following from the Source Code Management section:
- Endevor
- PDS

This source code management action has following parameters:

- **Host connection** : Select the host connection to be used to connect to the z/OS host.

![](docs/images/info.svg) Alternatively, to add a connection, click **Manage connections**. The **Host connections** section of the Jenkins configuration page appears so a connection can be added.

- **Login credentials** : Select the stored credentials to use for logging onto the z/OS host.

![](docs/images/info.svg) Alternatively, click **Add** to add credentials using the [Credentials Plugin](https://plugins.jenkins.io/credentials/). Refer to the Jenkins documentation for the Credentials Plugin.

Do the following:

- **Filter pattern** : Enter the datasets for which to download members, delimiting datasets with white space or commas.

    - Wildcards can be used for the last character only.
    - For Endevor, if the stage is wildcarded, only the most recent revision of each source member will be downloaded.
    - Migrated PDSs are recalled before being downloaded.

- **File extension to assign** :  Enter the extension to be added to the downloaded files.

- **Source download location** :  Optionally enter the absolute or relative path to the project workspace location to which to download the source. If this field is left blank, source is downloaded to a folder based on the filter name relative to the default location (the project workspace). For each PDS, a directory is created with the name of the PDS relative to the filter folder that contains the members that were downloaded.

Click **Save**.

Run the job, which downloads files to the location in the **Source download location** field or, if that field was left blank, to the default location (the project workspace location).

![](docs/images/info.svg) Optionally, to perform SonarQube analysis, install the SonarQube plugin and refer to the documentation for the SonarQube plugin at [https://jenkins-ci.org](https://jenkins-ci.org/).

<img src="docs/images/download.pds.members.png" height="250"/>

### Downloading Code Pipeline Container members

This integration allows downloading of Code Pipeline Container members from the mainframe to the PC.

On the **Configuration** page of the job or project, select **Code Pipeline Container** from the **Source Code Management** section.

This source code management action has following parameters:

- **Host connection** : Select the host connection to be used to connect to the z/OS host.

![](docs/images/info.svg) Alternatively, to add a connection, click **Manage connections**. The **Host connections** section of the Jenkins configuration page appears so a connection can be added.

- **Runtime configuration** : Enter the host runtime configuration. To use the default configuration, leave the field blank.

- **Login credentials** : Select the stored credentials to use for logging onto the z/OS host.

![](docs/images/info.svg) Alternatively, click **Add** to add credentials using the [Credentials Plugin](https://plugins.jenkins.io/credentials/). Refer to the Jenkins documentation for the Credentials Plugin.

Do the following in the **Filter** section to identify Code Pipeline members to be downloaded:

- **Container name** : Enter the name of the container to target for the download.

- **Container type** list (do one of the following):

     - **Assignment** : Select if the specified **Container name** is an assignment.
     - **Release** : Select if the specified **Container name** is a release.
     - **Set** : Select if the specified **Container name** is a set.

- **Level** : Optionally use to identify components at a specific level in the life cycle to download (such as DEV1, STG1, or PRD).
- **Component type** : Optionally use to identify components of a specific type to download (such as COB, COPY, or JOB).
- **Force download of unchanged source** : Optionally use to indicate that all source matching the current filter should be downloaded, regardless of whether it has been changed recently or not. If this box is left unchecked, it will delete any files in the workspace that no longer match the filter specified above. Leaving it unchecked will also only download source that has been changed since the last time the job was run.
- **Download related includes**: Optionally use to download all files matching the filter, along with their impacts.
- **Categorize On Component Type**: Optionally enable to categorize downloaded source files into folders based on Component Type.
- **Categorize On Sub Application**: Optionally enable to categorize downloaded source files into folders based on Sub Application.

Click **Save**.

Run the job, which by default the following occurs:
- Mainframe source is downloaded to the project's or job's workspace into an <ISPW Application name>/MF_Source folder.
- Folder components are downloaded into an <ISPW Application name> folder.

![](docs/images/info.svg) Optionally, to perform SonarQube analysis, install the SonarQube plugin and refer to the documentation for the SonarQube plugin at [https://jenkins-ci.org](https://jenkins-ci.org/).

<img src="docs/images/download.ispw.container.members.png" height="350"/>

### Downloading Code Pipeline Repository members

This integration allows downloading of Code Pipeline Repository members from the mainframe to the PC.

On the Configuration page of the job or project, select Code Pipeline Repository from the Source Code Management section.

This source code management action has following parameters:

- **Host connection** : Select the host connection to be used to connect to the z/OS host.

![](docs/images/info.svg) Alternatively, to add a connection, click **Manage connections**. The **Host connections** section of the Jenkins configuration page appears so a connection can be added.

- **Runtime configuration** : Enter the host runtime configuration. To use the default configuration, leave the field blank.

- **Login credentials** : Select the stored credentials to use for logging onto the z/OS host.

![](docs/images/info.svg) Alternatively, click **Add** to add credentials using the [Credentials Plugin](https://plugins.jenkins.io/credentials/). Refer to the Jenkins documentation for the Credentials Plugin.

Do the following in the **Filter** section to identify Code Pipeline members to be downloaded:

- **Stream** : Enter the two- to eight-character code that defines the application structure with which the application is associated.
- **Application** : Enter the container's primary application code. Containers may include components from multiple applications.
- **SubAppl** : Enter the container's primary subapplication code. Containers may include components from multiple subapplications.
- **Level** : Enter the life cycle level.
- **Level option** list (do one of the following):
     - **Selected level only** : Select to display only components at the selected life cycle level in the view.
     - **First found in level and above** : Select to display the first version found of each component at the selected level and above. In other words, if there are multiple versions in the life cycle, display one version of the component that is the first one found at the selected level and any levels in the path above it.
- **Component types** and/or **Application root folder names** : Optionally use to identify components and application root folders to download.
      - To download a folder that matches the name specified (and all of its contents) and download all components outside of a folder that match the specified type, enter values in both the **Component types** and **Application root folder names** fields. Enter in the **Component types** field the component type (such as COB, COPY, or JOB) on which to filter. Enter in the **Application root folder names** field the name of the folder on which to filter. For example, entering **COB** in the Component types field and **FolderX** in the **Application root folder names** field will download FolderX and all of its contents, as well as all of the COB files that exist outside of folders.
      - To download all components of a specified type regardless of whether they are within folders, use only the **Component types** field by entering the component type (such as COB, COPY, or JOB) on which to filter.
      - To download a folder that matches the name specified (and all of its contents), as well as all components that are not within a folder, use only the **Application root folder names** field by entering the name of the folder on which to filter.
      - To download all components and folders in the application and level selected, leave both fields empty.
      ![](docs/images/info.svg) To download multiple folders or types, comma-separate the values.
      - **Force download of unchanged source** : Optionally use to indicate that all source matching the current filter should be downloaded, regardless of whether it has been changed recently or not. If this box is left unchecked, it will delete any files in the workspace that no longer match the filter specified above. Leaving it unchecked will also only download source that has been changed since the last time the job was run.

Click **Save**.

Run the job, which by default the following occurs:
- Mainframe source is downloaded to the project's or job's workspace into an <ISPW Application name>/MF_Source folder.
- Folder components are downloaded into an <ISPW Application name> folder.

![](docs/images/info.svg) Optionally, to perform SonarQube analysis, install the SonarQube plugin and refer to the documentation for the SonarQube plugin at [https://jenkins-ci.org](https://jenkins-ci.org/).

<img src="docs/images/download.ispw.repository.members.png" height="400"/>

## Using Pipeline Syntax to Generate Pipeline Script

- Do one of the following:

    - When working with an existing Pipeline job, click the **Pipeline Syntax** link in the left panel. The **Snippet Generator** appears.

    - When configuring a Pipeline job, click the **Pipeline Syntax** link at the bottom of the **Pipeline** configuration section. The **Snippet Generator** appears.

- **Sample Step** : Select **checkout: General SCM** .

- **SCM** : Select **Endevor**, **Code Pipeline Container**, **Code Pipeline Repository**, or **PDS** as the version control system from which to get the code.

- Complete the remaining fields for the selected SCM.

- Click **Generate Pipeline Script**. The Groovy script to invoke the BMC AMI DevX Source Code Download for Endevor, PDS, and Code Pipeline plugin appears. The script can be added to the Pipeline section when configuring a Pipeline job. A sample script is shown below:

~~~
stage("Download PDS") {
    node {
        checkout([$class: 'PdsConfiguration',
        connectionId: 'f5264789-8b54-6522-al25-ag54gh85re42',
        credentialsId: 'f4393474-9b86-4155-ae2c-ac11ab71ae47',
        fileExtension: 'cbl',
        filterPattern: 'abc.def'])
    }
}
~~~

![](docs/images/info.svg) The **Include in polling?** and **Include in changelog?** check boxes have no effect in the BMC AMI DevX Source Code Download for Endevor, PDS, and Code Pipeline plugin.

## Product Assistance

BMC provides assistance for customers with its documentation and the support web site.

### BMC Support Center

You can access online information for BMC products via our Support Center site at [https://support.bmc.com](https://support.bmc.com/). Support Center provides access to critical information about your BMC products. You can review frequently asked questions, read or download documentation, access product fixes, or e-mail your questions or comments. The first time you access Support Center, you must register and obtain a password. Registration is free.

### Contacting Customer Support

At BMC, we strive to make our products and documentation the best in the industry. Feedback from our customers helps us maintain our quality standards. If you need support services, please obtain the following information :

- The Jenkins job console output that contains any error messages or pertinent information.

- The name, release number, and build number of your product. This information is displayed in the Jenkins / Plugin Manager and go to the Installed tab. Apply filter: BMC in order to display all of the installed BMC plugins.

- Job information, whether the job uses Pipeline script or Freestyle project.

- Environment information, such as the operating system and release on which the Workbench CLI is installed.

#### Web

You can report issues via BMC Support Center: [https://support.bmc.com](https://support.bmc.com/).
## Corporate Web Site

To access BMC site on the Web, go to [https://www.bmc.com/](https://www.bmc.com/). The BMC site provides a variety of product and support information.
