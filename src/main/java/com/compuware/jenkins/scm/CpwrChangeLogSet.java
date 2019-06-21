package com.compuware.jenkins.scm;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import hudson.model.Run;
import hudson.model.User;
import hudson.scm.ChangeLogSet;
import hudson.scm.RepositoryBrowser;

/**
 * Skeleton implementation for change log. Currently change log is not implemented, an empty entry list will be returned to fix
 * pipeline issue CWE-149820. This will make plugin compatible with Jenkins future upgrade. A complete change log solution can
 * be extended to support this feature.
 */
public class CpwrChangeLogSet extends ChangeLogSet<CpwrChangeLogSet.CpwrEntry>
{

	public List<CpwrChangeLogSet.CpwrEntry> logSet = new ArrayList<CpwrChangeLogSet.CpwrEntry>();

	public CpwrChangeLogSet(Run<?, ?> run, RepositoryBrowser<?> browser)
	{
		super(run, browser);
	}

	@Override
	public Iterator<CpwrEntry> iterator()
	{
		return logSet.iterator();
	}

	@Override
	public boolean isEmptySet()
	{
		return logSet.isEmpty();
	}

	public static class CpwrEntry extends ChangeLogSet.Entry
	{

		private Collection<String> paths;
		private User author;
		private String msg;

		public CpwrEntry(Collection<String> paths, User author, String msg)
		{
			this.paths = paths;
			this.author = author;
			this.msg = msg;
		}

		@Override
		public Collection<String> getAffectedPaths()
		{
			return paths;
		}

		@Override
		public User getAuthor()
		{
			return author;
		}

		@Override
		public String getMsg()
		{
			return msg;
		}

	}
}
