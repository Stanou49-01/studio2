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
package com.aptana.ide.debug.internal.core.model;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.PlatformObject;
import com.aptana.ide.debug.core.IDebugConstants;
import com.aptana.ide.debug.core.model.IJSImplicitBreakpoint;

/**
 * @author Max Stepanov
 */
public class JSDebugImplicitBreakpoint extends PlatformObject implements IJSImplicitBreakpoint
{
	/**
	 * TYPE_DEBUGGER_KEYWORD
	 */
	protected static final int TYPE_DEBUGGER_KEYWORD = 1;

	/**
	 * TYPE_FIRST_LINE
	 */
	protected static final int TYPE_FIRST_LINE = 2;

	/**
	 * TYPE_EXCEPTION
	 */
	protected static final int TYPE_EXCEPTION = 3;

	/**
	 * TYPE_WATCHPOINT
	 */
	protected static final int TYPE_WATCHPOINT = 4;

	private String fileName;
	private int lineNumber;
	private int type;

	/**
	 * JSDebugImplicitBreakpoint
	 * 
	 * @param fileName
	 * @param lineNumber
	 * @param type
	 */
	public JSDebugImplicitBreakpoint(String fileName, int lineNumber, int type)
	{
		this.fileName = fileName;
		this.lineNumber = lineNumber;
		this.type = type;
	}

	/**
	 * @see com.aptana.ide.debug.core.model.IJSImplicitBreakpoint#getFileName()
	 */
	public String getFileName() throws CoreException
	{
		return fileName;
	}

	/**
	 * @see com.aptana.ide.debug.core.model.IJSImplicitBreakpoint#isDebuggerKeyword()
	 */
	public boolean isDebuggerKeyword()
	{
		return (type == TYPE_DEBUGGER_KEYWORD);
	}

	/**
	 * @see com.aptana.ide.debug.core.model.IJSImplicitBreakpoint#isFirstLine()
	 */
	public boolean isFirstLine()
	{
		return (type == TYPE_FIRST_LINE);
	}

	/**
	 * @see com.aptana.ide.debug.core.model.IJSImplicitBreakpoint#isException()
	 */
	public boolean isException()
	{
		return (type == TYPE_EXCEPTION);
	}

	/**
	 * @see com.aptana.ide.debug.core.model.IJSImplicitBreakpoint#isWatchpoint()
	 */
	public boolean isWatchpoint()
	{
		return (type == TYPE_EXCEPTION);
	}

	/**
	 * @see org.eclipse.debug.core.model.IBreakpoint#getModelIdentifier()
	 */
	public String getModelIdentifier()
	{
		return IDebugConstants.ID_DEBUG_MODEL;
	}

	/**
	 * @see org.eclipse.debug.core.model.ILineBreakpoint#getLineNumber()
	 */
	public int getLineNumber() throws CoreException
	{
		return lineNumber;
	}

	/**
	 * @see org.eclipse.debug.core.model.ILineBreakpoint#getCharStart()
	 */
	public int getCharStart() throws CoreException
	{
		return -1;
	}

	/**
	 * @see org.eclipse.debug.core.model.ILineBreakpoint#getCharEnd()
	 */
	public int getCharEnd() throws CoreException
	{
		return -1;
	}

	/**
	 * @see org.eclipse.debug.core.model.IBreakpoint#setMarker(org.eclipse.core.resources.IMarker)
	 */
	public void setMarker(IMarker marker) throws CoreException
	{
	}

	/**
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(Object item)
	{
		if (item instanceof JSDebugImplicitBreakpoint)
		{
			return fileName.equals(((JSDebugImplicitBreakpoint) item).fileName)
					&& lineNumber == ((JSDebugImplicitBreakpoint) item).lineNumber;
		}
		return false;
	}

	/**
	 * @see java.lang.Object#hashCode()
	 */
	public int hashCode()
	{
		return (fileName + "\n" + lineNumber).hashCode(); //$NON-NLS-1$
	}

	/**
	 * @see org.eclipse.debug.core.model.IBreakpoint#isEnabled()
	 */
	public boolean isEnabled() throws CoreException
	{
		return true;
	}

	/**
	 * @see org.eclipse.debug.core.model.IBreakpoint#isPersisted()
	 */
	public boolean isPersisted() throws CoreException
	{
		return false;
	}

	/**
	 * @see org.eclipse.debug.core.model.IBreakpoint#isRegistered()
	 */
	public boolean isRegistered() throws CoreException
	{
		return false;
	}

	/**
	 * @see org.eclipse.debug.core.model.IBreakpoint#setEnabled(boolean)
	 */
	public void setEnabled(boolean enabled) throws CoreException
	{
	}

	/**
	 * @see org.eclipse.debug.core.model.IBreakpoint#setPersisted(boolean)
	 */
	public void setPersisted(boolean persisted) throws CoreException
	{
	}

	/**
	 * @see org.eclipse.debug.core.model.IBreakpoint#setRegistered(boolean)
	 */
	public void setRegistered(boolean registered) throws CoreException
	{
	}

	/**
	 * @see org.eclipse.debug.core.model.IBreakpoint#delete()
	 */
	public void delete() throws CoreException
	{
	}

	/**
	 * @see org.eclipse.debug.core.model.IBreakpoint#getMarker()
	 */
	public IMarker getMarker()
	{
		return null;
	}
}
