Change Log
=========

2.0.8
------

- Handle multi-threaded source downloading for PDS and Endevor files. Create a unique TopazCLI workspace for each Topaz download step. 

2.0.7
------

- Updated to keep compatible with latest Jenkins pipeline change (require Jenkins 2.130 or above, include fixes in v2.0.6, but lower requirement for Jenkins version)

2.0.6
------

- Updated to keep compatible with latest Jenkins pipeline change (require Jenkins 2.181 or above, original Jenkins version where pipeline is reported incompatible)

2.0.5
------

- Updated referenced Compuware Common Configuration plugin version

2.0.4
------

This release requires Topaz Workbench CLI version 19.04.01 or higher.

- Added support for encryption protocol for a host connection.

2.0.0
------

- The plugin now integrates with the Compuware Common Configuration plugin, which allows the Host Connection configurations and Topaz Workbench CLI to be defined centrally for other Compuware Jenkins plugins instead of needing to be specified in each Jenkins project's configuration. Host Connection configuration is now defined in the Jenkins/Manage Jenkins/Configure System screen.
 
- Jenkins console logs produced by the plugin have been streamlined to improve readability.

- ISPW filters can now be specified to only download specified folders in the ISPW repository.

- ISPW mainframe source files will now be downloaded to the following directory structure under the Jenkins Workspace <Jenkins Project>/<ISPW Application Name>/MF_Source directory instead of to the root of the <ISPW Application Name> directory.

- PDS and Endevor configurations now allow a source download folder to be specified in the configuration.

- Version checking has been introduced between the plugin and the Topaz Workbench CLI.

Upgrade considerations

- The 18.2.3 version of the Topaz Workbench CLI is required. The Topaz Workbench CLI is included in the Topaz Workbench installation media provided by Compuware.

- Existing Freestyle Jenkins projects will automatically migrate data into the new versions plugin, but the user should validate the project settings and save the updates before executing the project.

- Existing Pipeline Jenkins projects will need to be updated to the new plugins syntax. Please see the section regarding Pipeline Syntax.

1.8
------

- ISPW is now a separate option from PDS in the SCM section of the configuration. By using ISPW, users are able to configure the source download using the same selection and filtering options as the ISPW Repository Explorer in Topaz Workbench.

1.7
------

- Maintenance release

1.6
------

- Maintenance release

1.5
------

- Support for the Jenkins Pipeline Syntax

- Support for Cloudbees Folder plugin
