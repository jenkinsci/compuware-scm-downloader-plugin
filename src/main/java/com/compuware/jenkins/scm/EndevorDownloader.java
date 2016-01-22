/**
 * The MIT License (MIT)
 * 
 * Copyright (c) 2016 Compuware Corporation
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software 
 * and associated documentation files (the "Software"), to deal in the Software without restriction, 
 * including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, 
 * and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, 
 * subject to the following conditions: The above copyright notice and this permission notice shall be 
 * included in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT 
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. 
 * IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, 
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE 
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 * 
*/
package com.compuware.jenkins.scm;

import hudson.EnvVars;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.BuildListener;
import hudson.model.AbstractBuild;
import hudson.remoting.VirtualChannel;
import hudson.util.ArgumentListBuilder;
import java.io.File;
import java.io.IOException;
import java.util.Properties;
import com.compuware.jenkins.scm.utils.Constants;

/**
 * Class used to download PDS members. This class will utilize the Topaz command line interface to do the download.
 */
public class EndevorDownloader extends AbstractDownloader
{
	private EndevorConfiguration m_endevorConfig;

	public EndevorDownloader(EndevorConfiguration config)
	{
		m_endevorConfig = config;
	}

	/* (non-Javadoc)
	 * @see com.compuware.jenkins.scm.AbstractDownloader#getSource(hudson.model.AbstractBuild, hudson.Launcher, hudson.FilePath, hudson.model.BuildListener, java.io.File, java.lang.String)
	 */
	@Override
	public boolean getSource(AbstractBuild<?, ?> build, Launcher launcher, FilePath workspaceFilePath, BuildListener listener,
			File changelogFile, String filterPattern) throws InterruptedException, IOException
	{
		String datasets = convertFilterPattern(filterPattern);
		listener.getLogger().println("Comma delimited datasets: " + datasets); //$NON-NLS-1$

        ArgumentListBuilder args = new ArgumentListBuilder();
        EnvVars env = build.getEnvironment(listener);
        env.overrideAll(build.getBuildVariables());
		
        VirtualChannel vChannel = launcher.getChannel();
        Properties remoteProperties = vChannel.call(new RemoteSystemProperties());
        String remoteFileSeparator = remoteProperties.getProperty(Constants.FILE_SEPARATOR);
        
		String osFile = launcher.isUnix() ? Constants.TOPAZ_CLI_SH : Constants.TOPAZ_CLI_BAT;
		String cliLocation = m_endevorConfig.getTopazCLILocation(launcher);
        
        String cliBatchFile = cliLocation + remoteFileSeparator + osFile;        
		listener.getLogger().println("cliBatchFile path: " + cliBatchFile); //$NON-NLS-1$
		
		FilePath cliBatchFileRemote = new FilePath(vChannel, cliBatchFile);
		listener.getLogger().println("cliBatchFile remote path: " + cliBatchFileRemote.getRemote()); //$NON-NLS-1$
		
		args.add(cliBatchFileRemote.getRemote());
		
		String topazCliWorkspace = workspaceFilePath.getRemote() + remoteFileSeparator + Constants.TOPAZ_CLI_WORKSPACE;
		listener.getLogger().println("TopazCLI workspace: " + topazCliWorkspace); //$NON-NLS-1$
		
		args.add(Constants.HOST_PARM, m_endevorConfig.getHost());
		args.add(Constants.PORT_PARM, m_endevorConfig.getPort());
		args.add(Constants.USERID_PARM, m_endevorConfig.getLoginInformation().getUsername());
		args.add(Constants.PASSWORD_PARM);
		args.add(m_endevorConfig.getLoginInformation().getPassword().getPlainText(), true);
		args.add(Constants.FILTER_PARM, wrapInQuotes(datasets));
		args.add(Constants.TARGET_FOLDER_PARM, workspaceFilePath.getRemote());
		args.add(Constants.SCM_TYPE_PARM, Constants.ENDEVOR);
		args.add(Constants.FILE_EXT_PARM, m_endevorConfig.getFileExtension());
		args.add(Constants.CODE_PAGE_PARM, m_endevorConfig.getCodePage());
		args.add(Constants.DATA_PARM, topazCliWorkspace);
		
		FilePath workDir = build.getModuleRoot();
		int exitValue = launcher.launch().cmds(args).envs(env).stdout(listener.getLogger()).pwd(workDir).join();

		listener.getLogger().println("Call " + osFile + " exited with exit value = " + exitValue); //$NON-NLS-1$ //$NON-NLS-2$

		return exitValue == 0;
	}
	
}