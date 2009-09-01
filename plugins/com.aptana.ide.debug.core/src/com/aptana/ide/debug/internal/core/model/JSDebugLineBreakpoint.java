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

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.debug.core.model.LineBreakpoint;

import com.aptana.ide.core.IdeLog;
import com.aptana.ide.core.PathUtils;
import com.aptana.ide.core.StringUtils;
import com.aptana.ide.core.resources.IUniformResource;
import com.aptana.ide.core.resources.MarkerUtils;
import com.aptana.ide.debug.core.IDebugConstants;
import com.aptana.ide.debug.core.JSDebugPlugin;
import com.aptana.ide.debug.core.model.IJSLineBreakpoint;

/**
 * @author Max Stepanov
 */
public class JSDebugLineBreakpoint extends LineBreakpoint implements IJSLineBreakpoint
{

	/**
	 * Default constructor is required for the breakpoint manager to re-create persisted breakpoints. After
	 * instantiating a breakpoint, the <code>setMarker(...)</code> method is called to restore this breakpoint's
	 * attributes.
	 */
	public JSDebugLineBreakpoint()
	{
		super();
	}

	/**
	 * Constructs a line breakpoint on the given resource at the given line number. The line number is 1-based (i.e. the
	 * first line of a file is line number 1).
	 * 
	 * @param resource
	 *            file on which to set the breakpoint
	 * @param lineNumber
	 *            1-based line number of the breakpoint
	 * @throws CoreException
	 *             if unable to create the breakpoint
	 */
	public JSDebugLineBreakpoint(IResource resource, int lineNumber) throws CoreException
	{
		this(resource, lineNumber, new HashMap(), true);
	}

	/**
	 * JSDebugLineBreakpoint
	 * 
	 * @param resource
	 * @param lineNumber
	 * @throws CoreException
	 */
	public JSDebugLineBreakpoint(IUniformResource resource, int lineNumber) throws CoreException
	{
		this(resource, lineNumber, new HashMap(), true);
	}

	/**
	 * Constructs a line breakpoint on the given resource at the given line number. The line number is 1-based (i.e. the
	 * first line of a file is line number 1).
	 * 
	 * @param resource
	 *            file on which to set the breakpoint
	 * @param lineNumber
	 *            1-based line number of the breakpoint
	 * @param attributes
	 *            the marker attributes to set
	 * @param register
	 *            whether to add this breakpoint to the breakpoint manager
	 * @throws CoreException
	 *             if unable to create the breakpoint
	 */
	public JSDebugLineBreakpoint(final IResource resource, final int lineNumber, final Map attributes,
			final boolean register) throws CoreException
	{
		IWorkspaceRunnable wr = new IWorkspaceRunnable()
		{
			@SuppressWarnings("unchecked")
			public void run(IProgressMonitor monitor) throws CoreException
			{
				IMarker marker = resource.createMarker(IDebugConstants.ID_LINE_BREAKPOINT_MARKER);
				setMarker(marker);
				attributes.put(IBreakpoint.ENABLED, Boolean.TRUE);
				attributes.put(IMarker.LINE_NUMBER, new Integer(lineNumber));
				attributes.put(IBreakpoint.ID, getModelIdentifier());
				attributes.put(IMarker.MESSAGE, StringUtils.format(Messages.JSDebugLineBreakpoint_JSBreakpoint_0_1,
						new String[] { resource.getFullPath().toString(), Integer.toString(lineNumber) }));
				ensureMarker().setAttributes(attributes);

				register(register);
			}
		};
		run(getMarkerRule(resource), wr);
	}

	/**
	 * JSDebugLineBreakpoint
	 * 
	 * @param resource
	 * @param lineNumber
	 * @param attributes
	 * @param register
	 * @throws CoreException
	 */
	public JSDebugLineBreakpoint(final IUniformResource resource, final int lineNumber, final Map attributes,
			final boolean register) throws CoreException
	{
		IWorkspaceRunnable wr = new IWorkspaceRunnable()
		{
			@SuppressWarnings("unchecked")
			public void run(IProgressMonitor monitor) throws CoreException
			{
				IMarker marker = MarkerUtils.createMarker(resource, attributes,
						IDebugConstants.ID_LINE_BREAKPOINT_MARKER);
				setMarker(marker);
				attributes.put(IBreakpoint.ENABLED, Boolean.TRUE);
				attributes.put(IMarker.LINE_NUMBER, new Integer(lineNumber));
				attributes.put(IBreakpoint.ID, getModelIdentifier());
				attributes.put(IMarker.MESSAGE, StringUtils.format(Messages.JSDebugLineBreakpoint_JSBreakpoint_0_1,
						new String[] { PathUtils.getPath(resource), Integer.toString(lineNumber) }));
				ensureMarker().setAttributes(attributes);

				register(register);
			}
		};
		try
		{
			ResourcesPlugin.getWorkspace().run(wr, null, 0, new NullProgressMonitor());
		}
		catch (CoreException e)
		{
			IdeLog.logError(JSDebugPlugin.getDefault(), Messages.JSDebugLineBreakpoint_BreakpointMarkerCreationFailed,
					e);
		}
	}

	/**
	 * @see org.eclipse.debug.core.model.IBreakpoint#getModelIdentifier()
	 */
	public String getModelIdentifier()
	{
		return IDebugConstants.ID_DEBUG_MODEL;
	}

	/**
	 * @return whether this breakpoint is a run to line breakpoint
	 * @throws CoreException
	 */
	public boolean isRunToLine() throws CoreException
	{
		return ensureMarker().getAttribute(IDebugConstants.RUN_TO_LINE, false);
	}

	/**
	 * Add this breakpoint to the breakpoint manager, or sets it as unregistered.
	 */
	private void register(boolean register) throws CoreException
	{
		if (register)
		{
			org.eclipse.debug.core.DebugPlugin.getDefault().getBreakpointManager().addBreakpoint(this);
		}
		else
		{
			setRegistered(false);
		}
	}

	/**
	 * @see com.aptana.ide.debug.core.model.IJSLineBreakpoint#getHitCount()
	 */
	public int getHitCount() throws CoreException
	{
		IMarker m = getMarker();
		if (m != null)
		{
			return m.getAttribute(IDebugConstants.BREAKPOINT_HIT_COUNT, -1);
		}
		return -1;
	}

	/**
	 * @see com.aptana.ide.debug.core.model.IJSLineBreakpoint#setHitCount(int)
	 */
	public void setHitCount(int count) throws CoreException
	{
		IMarker m = getMarker();
		if (m != null)
		{
			m.setAttribute(IDebugConstants.BREAKPOINT_HIT_COUNT, count);
		}
	}

	/**
	 * @see com.aptana.ide.debug.core.model.IJSLineBreakpoint#getCondition()
	 */
	public String getCondition() throws CoreException
	{
		IMarker m = getMarker();
		if (m != null)
		{
			return m.getAttribute(IDebugConstants.BREAKPOINT_CONDITION, StringUtils.EMPTY);
		}
		return StringUtils.EMPTY;
	}

	/**
	 * @see com.aptana.ide.debug.core.model.IJSLineBreakpoint#setCondition(java.lang.String)
	 */
	public void setCondition(String condition) throws CoreException
	{
		IMarker m = getMarker();
		if (m != null)
		{
			m.setAttribute(IDebugConstants.BREAKPOINT_CONDITION, condition);
		}
	}

	/**
	 * @see com.aptana.ide.debug.core.model.IJSLineBreakpoint#isConditionEnabled()
	 */
	public boolean isConditionEnabled() throws CoreException
	{
		IMarker m = getMarker();
		if (m != null)
		{
			return m.getAttribute(IDebugConstants.BREAKPOINT_CONDITION_ENABLED, false);
		}
		return false;
	}

	/**
	 * @see com.aptana.ide.debug.core.model.IJSLineBreakpoint#setConditionEnabled(boolean)
	 */
	public void setConditionEnabled(boolean enabled) throws CoreException
	{
		IMarker m = getMarker();
		if (m != null)
		{
			m.setAttribute(IDebugConstants.BREAKPOINT_CONDITION_ENABLED, enabled);
		}
	}

	/**
	 * @see com.aptana.ide.debug.core.model.IJSLineBreakpoint#isConditionSuspendOnTrue()
	 */
	public boolean isConditionSuspendOnTrue() throws CoreException
	{
		IMarker m = getMarker();
		if (m != null)
		{
			return m.getAttribute(IDebugConstants.BREAKPOINT_CONDITION_SUSPEND_ON_TRUE, true);
		}
		return true;
	}

	/**
	 * @see com.aptana.ide.debug.core.model.IJSLineBreakpoint#setConditionSuspendOnTrue(boolean)
	 */
	public void setConditionSuspendOnTrue(boolean suspendOnTrue) throws CoreException
	{
		IMarker m = getMarker();
		if (m != null)
		{
			m.setAttribute(IDebugConstants.BREAKPOINT_CONDITION_SUSPEND_ON_TRUE, suspendOnTrue);
		}
	}
}
