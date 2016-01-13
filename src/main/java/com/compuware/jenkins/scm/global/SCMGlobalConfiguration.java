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
*/
package com.compuware.jenkins.scm.global;

import hudson.Extension;
import hudson.Launcher;
import jenkins.model.GlobalConfiguration;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.StaplerRequest;

/**
 * Global configuration used by both EndevorConfiguraton and PdsConfiguration
 */
@Extension
public class SCMGlobalConfiguration extends GlobalConfiguration{
	
	private String topazCLILocationWindows;
	private String topazCLILocationLinux;

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
		topazCLILocationWindows = req.getParameter("topazCLILocationWindows"); //$NON-NLS-1$
		topazCLILocationLinux = req.getParameter("topazCLILocationLinux"); //$NON-NLS-1$
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
	 * Returns the Topaz Workbench CLI location based on node
	 * 
	 * @return CLI location
	 */
	public String getTopazCLILocation(Launcher launcher)
	{
		if (launcher.isUnix())
		{
			return topazCLILocationLinux;
		}
		else
		{
			return topazCLILocationWindows;
		}
	}
	
	
	/**
	 * Returns the value of the topazCLILocationLinux. Used for databinding.
	 * @return CLI location - Windows
	 */
	public String getTopazCLILocationWindows()
	{
		return topazCLILocationWindows;
	}

	/**
	 * Returns the value of the topazCLILocationLinux field. Used for databinding.
	 * @return CLI location - Linux
	 */
	public String getTopazCLILocationLinux()
	{
		return topazCLILocationLinux;
	}
}
