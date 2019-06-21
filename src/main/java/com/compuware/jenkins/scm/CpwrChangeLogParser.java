package com.compuware.jenkins.scm;

import java.io.File;
import java.io.IOException;
import org.xml.sax.SAXException;
import hudson.Util;
import hudson.model.AbstractBuild;
import hudson.model.Run;
import hudson.scm.ChangeLogParser;
import hudson.scm.ChangeLogSet;
import hudson.scm.RepositoryBrowser;
import hudson.scm.ChangeLogSet.Entry;

/**
 * Change Log parser
 */
public class CpwrChangeLogParser extends ChangeLogParser
{
	public ChangeLogSet<? extends Entry> parse(Run build, RepositoryBrowser<?> browser, File changelogFile)
			throws IOException, SAXException
	{
		if (build instanceof AbstractBuild
				&& Util.isOverridden(ChangeLogParser.class, getClass(), "parse", AbstractBuild.class, File.class))
		{
			return parse((AbstractBuild) build, changelogFile);
		}
		else
		{
			return new CpwrChangeLogSet(build, browser);
		}
	}

	@Deprecated
	public ChangeLogSet<? extends Entry> parse(AbstractBuild build, File changelogFile) throws IOException, SAXException
	{
		return parse(build, build.getProject().getScm().getEffectiveBrowser(), changelogFile);
	}

}
