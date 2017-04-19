package com.compuware.jenkins.scm;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import com.compuware.jenkins.scm.utils.Constants;

import hudson.EnvVars;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.remoting.VirtualChannel;
import hudson.util.ArgumentListBuilder;

public class IspwDownloader
{
	private IspwConfiguration m_ispwConfig;

	/**
	 * Constructs the PDS downloader for the given configuration.
	 * 
	 * @param config
	 *            PDS configuration
	 */
	public IspwDownloader(IspwConfiguration config)
	{
		m_ispwConfig = config;
	}
	
	public boolean getSource(Run<?, ?> build, Launcher launcher, FilePath workspaceFilePath, TaskListener listener,
			File changelogFile) throws InterruptedException, IOException
	{
        ArgumentListBuilder args = new ArgumentListBuilder();
        EnvVars env = build.getEnvironment(listener);

        VirtualChannel vChannel = launcher.getChannel();
        Properties remoteProperties = vChannel.call(new RemoteSystemProperties());
        String remoteFileSeparator = remoteProperties.getProperty(Constants.FILE_SEPARATOR);
        
		String osFile = launcher.isUnix() ? Constants.TOPAZ_CLI_SH : Constants.TOPAZ_CLI_BAT;
		String cliLocation = m_ispwConfig.getTopazCLILocation(launcher);
        
		String cliBatchFile = cliLocation + remoteFileSeparator + osFile;
		listener.getLogger().println("cliBatchFile path: " + cliBatchFile); //$NON-NLS-1$
		
		FilePath cliBatchFileRemote = new FilePath(vChannel, cliBatchFile);
		listener.getLogger().println("cliBatchFile remote path: " + cliBatchFileRemote.getRemote()); //$NON-NLS-1$
		
		args.add(cliBatchFileRemote.getRemote());
		
		String topazCliWorkspace = workspaceFilePath.getRemote() + remoteFileSeparator + Constants.TOPAZ_CLI_WORKSPACE;
		listener.getLogger().println("TopazCLI workspace: " + topazCliWorkspace); //$NON-NLS-1$
		
		args.add(Constants.HOST_PARM, m_ispwConfig.getHost());
		args.add(Constants.PORT_PARM, m_ispwConfig.getPort());
		args.add(Constants.USERID_PARM, m_ispwConfig.getLoginInformation(build.getParent()).getUsername());
		args.add(Constants.PASSWORD_PARM);
		args.add(m_ispwConfig.getLoginInformation(build.getParent()).getPassword().getPlainText(), true);
		
		args.add(Constants.ISPW_SERVER_CONFIG_PARAM, m_ispwConfig.getServerConfig());
		args.add(Constants.ISPW_SERVER_STREAM_PARAM, m_ispwConfig.getServerStream());
		args.add(Constants.ISPW_SERVER_APP_PARAM, m_ispwConfig.getServerApplication());
		args.add(Constants.ISPW_SERVER_LEVEL_PARAM, m_ispwConfig.getServerLevel());
		args.add(Constants.ISPW_LEVEL_OPTION_PARAM, m_ispwConfig.getLevelOption());
		args.add(Constants.ISPW_FILTER_NAME_PARAM, m_ispwConfig.getFilterName());
		args.add(Constants.ISPW_FILTER_TYPE_PARAM, m_ispwConfig.getFilterType());

		args.add(Constants.TARGET_FOLDER_PARM, workspaceFilePath.getRemote());
		args.add(Constants.SCM_TYPE_PARM, Constants.ISPW);
		args.add(Constants.CODE_PAGE_PARM, m_ispwConfig.getCodePage());
		args.add(Constants.DATA_PARM, topazCliWorkspace);
		
		//FilePath workDir = new FilePath (vChannel, workspaceFilePath.getRemote());
		//workDir.mkdirs();
		//int exitValue = launcher.launch().cmds(args).envs(env).stdout(listener.getLogger()).pwd(workDir).join();
		
		return false;
	}

}
