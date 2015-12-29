package com.compuware.jenkins.scm;

import com.compuware.jenkins.scm.utils.Constants;

/**
 * 
 */
public abstract class AbstractDownloader
{
	/**
	 * Convert the filter pattern from a multi-line String to a comma delimited string.
	 * 
	 * @param filter
	 *            the dataset filter
	 * @return comma delimited list of dataset filters
	 */
	protected String convertFilterPattern(String filter)
	{
		String convertedFilter = filter;

		if (convertedFilter != null)
		{
			convertedFilter = convertedFilter.replace(Constants.LINE_RETURN, Constants.COMMA);
			char lastChar = convertedFilter.charAt(convertedFilter.length() - 1);
			if (lastChar == ',')
			{
				convertedFilter = convertedFilter.substring(0, (convertedFilter.length() - 1));
			}
		}

		return convertedFilter;
	}

	/**
	 * Wrap a string in quotes.
	 * 
	 * @param text
	 *            the string to wrap in quotes
	 * @return the quoted string
	 */
	protected String wrapInQuotes(String text)
	{
		String quotedValue = text;
		if (text != null)
		{
			quotedValue = String.format("\"%s\"", text); //$NON-NLS-1$
		}
		return quotedValue;
	}
}
