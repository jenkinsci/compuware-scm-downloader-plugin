package com.compuware.jenkins.scm.global;

import hudson.Extension;
import jenkins.model.GlobalConfiguration;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.StaplerRequest;

/**
 * Global configuration used by both EndevorConfiguraton and PdsConfiguration
 */
@Extension
public class SCMGlobalConfiguration extends GlobalConfiguration{
	
	private String topazCLILocation;

	public SCMGlobalConfiguration()
	{
		super();
		load();
	}

    /**
	 * The method is called when the global configuration page is submitted. In the method the data in the web form should
	 * be copied to the Descriptor's fields. To persist the fields to the global configuration XML file, the
	 * <code>save()</code> method must be called. Data is defined in the config.jelly page.
	 * 
	 * @param req
	 *            Stapler request
	 * @param json
	 *            Form data
	 * @return TRUE if able to configure and continue to next page
	 * @throws FormException
	 */
    public boolean configure(StaplerRequest req, JSONObject json) throws FormException {
		topazCLILocation = req.getParameter("topazCLILocation"); //$NON-NLS-1$
        save();
        return true;
    }

    /**
     * Returns the global configuration class and its parameters
     * 
     * @return	Global Configuration class instance
     */
    public static SCMGlobalConfiguration get() {
        return GlobalConfiguration.all().get(SCMGlobalConfiguration.class);
    }
    
    /**
     * Returns the Topaz Workbench CLI location
     * @return CLI location
     */
    public String getTopazCLILocation()
    {
    	return topazCLILocation;
    }
}
