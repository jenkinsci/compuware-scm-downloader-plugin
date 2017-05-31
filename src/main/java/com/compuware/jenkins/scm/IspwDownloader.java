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

public class IspwDownloader extends AbstractDownloader
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
	
	@Override
	public boolean getSource(Run<?, ?> build, Launcher launcher, FilePath workspaceFilePath, TaskListener listener,
			File changelogFile) throws InterruptedException, IOException
	{
        ArgumentListBuilder args = new ArgumentListBuilder();
        EnvVars env = build.getEnvironment(listener);

        VirtualChannel vChannel = launcher.getChannel();
        Properties remoteProperties = vChannel.call(new RemoteSystemProperties());
        String remoteFileSeparator = remoteProperties.getProperty(Constants.FILE_SEPARATOR);
        
        boolean isShell = launcher.isUnix();
		String osFile = launcher.isUnix() ? Constants.TOPAZ_CLI_SH : Constants.TOPAZ_CLI_BAT;
		String cliLocation = m_ispwConfig.getTopazCLILocation(launcher);
        
		String cliBatchFile = cliLocation + remoteFileSeparator + osFile;
		listener.getLogger().println("cliBatchFile path: " + cliBatchFile); //$NON-NLS-1$
		
		FilePath cliBatchFileRemote = new FilePath(vChannel, cliBatchFile);
		listener.getLogger().println("cliBatchFile remote path: " + cliBatchFileRemote.getRemote()); //$NON-NLS-1$
		
		args.add(cliBatchFileRemote.getRemote());
		
		String topazCliWorkspace = workspaceFilePath.getRemote() + remoteFileSeparator + Constants.TOPAZ_CLI_WORKSPACE;
		listener.getLogger().println("TopazCLI workspace: " + topazCliWorkspace); //$NON-NLS-1$
		
		String host = escapeForScript(m_ispwConfig.getHost(), isShell);
		String port = escapeForScript(m_ispwConfig.getPort(), isShell);
		String username = escapeForScript(m_ispwConfig.getLoginInformation(build.getParent()).getUsername(), isShell);
		String password = escapeForScript(m_ispwConfig.getLoginInformation(build.getParent()).getPassword().getPlainText(), isShell);
		String serverStream = escapeForScript(m_ispwConfig.getServerStream(), isShell);
		String serverApp = escapeForScript(m_ispwConfig.getServerApplication(), isShell);
		String serverLevel = escapeForScript(m_ispwConfig.getServerLevel(), isShell);
		String levelOption = escapeForScript(m_ispwConfig.getLevelOption(), isShell);
				
		args.add(Constants.HOST_PARM, host);
		args.add(Constants.PORT_PARM, port);
		args.add(Constants.USERID_PARM, username);
		args.add(Constants.PASSWORD_PARM);
		args.add(password, true);	
		args.add(Constants.ISPW_SERVER_STREAM_PARAM, serverStream);
		args.add(Constants.ISPW_SERVER_APP_PARAM, serverApp);
		args.add(Constants.ISPW_SERVER_LEVEL_PARAM, serverLevel);
		args.add(Constants.ISPW_LEVEL_OPTION_PARAM, levelOption);
		
		String runtimeConfig = m_ispwConfig.getServerConfig();
		String componentName = m_ispwConfig.getFilterName();
		String componentType = m_ispwConfig.getFilterType();
		
		if (!runtimeConfig.isEmpty())
		{
			runtimeConfig = escapeForScript(runtimeConfig, isShell);
			args.add(Constants.ISPW_SERVER_CONFIG_PARAM, runtimeConfig);
		}
		
		if (!componentName.isEmpty())
		{
			componentName = escapeForScript(componentName, isShell);
			args.add(Constants.ISPW_FILTER_NAME_PARAM, componentName);
		}
		
		if (!componentType.isEmpty())
		{
			componentType = escapeForScript(componentType, isShell);
			args.add(Constants.ISPW_FILTER_TYPE_PARAM, componentType);
		}

		args.add(Constants.TARGET_FOLDER_PARM, workspaceFilePath.getRemote());
		args.add(Constants.SCM_TYPE_PARM, Constants.ISPW);
		args.add(Constants.CODE_PAGE_PARM, m_ispwConfig.getCodePage());
		args.add(Constants.DATA_PARM, topazCliWorkspace);
		
		FilePath workDir = new FilePath (vChannel, workspaceFilePath.getRemote());
		workDir.mkdirs();
		int exitValue = launcher.launch().cmds(args).envs(env).stdout(listener.getLogger()).pwd(workDir).join();
		
		return (exitValue == 0);
	}

}
