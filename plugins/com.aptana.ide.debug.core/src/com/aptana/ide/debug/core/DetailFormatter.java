/**
 * This file Copyright (c) 2005-2008 Aptana, Inc. This program is
 * dual-licensed under both the Aptana Public License and the GNU General
 * Public license. You may elect to use one or the other of these licenses.
 * 
 * This program is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT. Redistribution, except as permitted by whichever of
 * the GPL or APL you select, is prohibited.
 *
 * 1. For the GPL license (GPL), you can redistribute and/or modify this
 * program under the terms of the GNU General Public License,
 * Version 3, as published by the Free Software Foundation.  You should
 * have received a copy of the GNU General Public License, Version 3 along
 * with this program; if not, write to the Free Software Foundation, Inc., 51
 * Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 * 
 * Aptana provides a special exception to allow redistribution of this file
 * with certain other free and open source software ("FOSS") code and certain additional terms
 * pursuant to Section 7 of the GPL. You may view the exception and these
 * terms on the web at http://www.aptana.com/legal/gpl/.
 * 
 * 2. For the Aptana Public License (APL), this program and the
 * accompanying materials are made available under the terms of the APL
 * v1.0 which accompanies this distribution, and is available at
 * http://www.aptana.com/legal/apl/.
 * 
 * You may view the GPL, Aptana's exception and additional terms, and the
 * APL in the file titled license.html at the root of the corresponding
 * plugin containing this source file.
 * 
 * Any modifications to this file must keep this entire header intact.
 */

package com.aptana.ide.debug.core;

/**
 * Information about a detail formatter.
 */
public class DetailFormatter implements Comparable
{

	private boolean fEnabled;

	private String fTypeName;

	private String fSnippet;

	/**
	 * DetailFormatter
	 * 
	 * @param typeName
	 * @param snippet
	 * @param enabled
	 */
	public DetailFormatter(String typeName, String snippet, boolean enabled)
	{
		fTypeName = typeName;
		fSnippet = snippet;
		fEnabled = enabled;
	}

	/**
	 * Indicate if this pretty should be used or not.
	 * 
	 * @return boolean
	 */
	public boolean isEnabled()
	{
		return fEnabled;
	}

	/**
	 * Returns the code snippet.
	 * 
	 * @return String
	 */
	public String getSnippet()
	{
		return fSnippet;
	}

	/**
	 * Returns the type name.
	 * 
	 * @return String
	 */
	public String getTypeName()
	{
		return fTypeName;
	}

	/**
	 * Sets the enabled flag.
	 * 
	 * @param enabled
	 *            the new value of the flag
	 */
	public void setEnabled(boolean enabled)
	{
		fEnabled = enabled;
	}

	/**
	 * Sets the code snippet.
	 * 
	 * @param snippet
	 *            the snippet to set
	 */
	public void setSnippet(String snippet)
	{
		fSnippet = snippet;
	}

	/**
	 * Sets the type name.
	 * 
	 * @param typeName
	 *            the type name to set
	 */
	public void setTypeName(String typeName)
	{
		fTypeName = typeName;
	}

	/**
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	public int compareTo(Object another)
	{
		DetailFormatter detailFormatter = (DetailFormatter) another;
		if (fTypeName == null)
		{
			if (detailFormatter.fTypeName == null)
			{
				return 0;
			}
			return detailFormatter.fTypeName.compareTo(fTypeName);
		}
		return fTypeName.compareTo(detailFormatter.fTypeName);
	}

}
