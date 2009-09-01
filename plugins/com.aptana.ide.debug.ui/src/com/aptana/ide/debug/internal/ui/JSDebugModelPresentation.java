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
package com.aptana.ide.debug.internal.ui;

import java.net.URI;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.ILineBreakpoint;
import org.eclipse.debug.core.model.IStackFrame;
import org.eclipse.debug.core.model.IThread;
import org.eclipse.debug.core.model.IValue;
import org.eclipse.debug.core.model.IVariable;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.IDebugModelPresentation;
import org.eclipse.debug.ui.IValueDetailListener;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.IEditorInput;

import com.aptana.ide.core.IdeLog;
import com.aptana.ide.core.PathUtils;
import com.aptana.ide.core.StringUtils;
import com.aptana.ide.core.resources.IUniformResourceMarker;
import com.aptana.ide.debug.core.IDebugConstants;
import com.aptana.ide.debug.core.JSDebugPlugin;
import com.aptana.ide.debug.core.model.IJSDebugTarget;
import com.aptana.ide.debug.core.model.IJSExceptionBreakpoint;
import com.aptana.ide.debug.core.model.IJSImplicitBreakpoint;
import com.aptana.ide.debug.core.model.IJSLineBreakpoint;
import com.aptana.ide.debug.core.model.IJSScriptElement;
import com.aptana.ide.debug.core.model.IJSStackFrame;
import com.aptana.ide.debug.core.model.IJSVariable;
import com.aptana.ide.debug.core.model.IJSWatchpoint;
import com.aptana.ide.debug.core.model.ISourceLink;
import com.aptana.ide.debug.core.model.JSInspectExpression;
import com.aptana.ide.debug.internal.ui.util.SourceDisplayUtil;
import com.aptana.ide.debug.ui.DebugUiPlugin;

/**
 * @author Max Stepanov
 */
public class JSDebugModelPresentation extends LabelProvider implements IDebugModelPresentation
{
	private boolean showTypes = false;

	/**
	 * @see org.eclipse.debug.ui.IDebugModelPresentation#setAttribute(java.lang.String, java.lang.Object)
	 */
	public void setAttribute(String attribute, Object value)
	{
		if (IDebugModelPresentation.DISPLAY_VARIABLE_TYPE_NAMES.equals(attribute))
		{
			showTypes = ((Boolean) value).booleanValue();
		}
	}

	/**
	 * @see org.eclipse.jface.viewers.ILabelProvider#getText(java.lang.Object)
	 */
	public String getText(Object element)
	{
		try
		{
			if (element instanceof IStackFrame)
			{
				return getStackFrameText((IStackFrame) element);
			}
			else if (element instanceof IThread)
			{
				return getThreadText((IThread) element);
			}
			else if (element instanceof IBreakpoint)
			{
				return getBreakpointText((IBreakpoint) element);
				// } else if ( element instanceof IVariable ) {
				// return getVariableText((IVariable)element);
				// } else if ( element instanceof IValue ) {
				// return getValueText((IValue) element);
			}
			else if (element instanceof IJSScriptElement)
			{
				return getScriptElementText((IJSScriptElement) element);
			}
			else if (element instanceof ISourceLink)
			{
				return ((ISourceLink)element).getLocation();
			}
			else if (element instanceof IMarker)
			{
				IBreakpoint breakpoint = getBreakpoint((IMarker) element);
				if (breakpoint != null)
				{
					return getBreakpointText(breakpoint);
				}
			}
		}
		catch (CoreException e)
		{
			IdeLog.logError(JSDebugPlugin.getDefault(), StringUtils.EMPTY, e);
		}
		return null;
	}

	/**
	 * @see org.eclipse.jface.viewers.ILabelProvider#getImage(java.lang.Object)
	 */
	public Image getImage(Object element)
	{
		try
		{
			if (element instanceof IVariable)
			{
				return getVariableImage((IVariable) element);
			}
			else if (element instanceof IBreakpoint)
			{
				return getBreakpointImage((IBreakpoint) element);
			}
			else if (element instanceof IJSScriptElement)
			{
				return getScriptElementImage((IJSScriptElement) element);
			}
			else if (element instanceof IMarker)
			{
				IBreakpoint breakpoint = getBreakpoint((IMarker) element);
				if (breakpoint != null)
				{
					return getBreakpointImage(breakpoint);
				}
			}
			else if (element instanceof JSInspectExpression)
			{
				return DebugUIImages.get(DebugUIImages.IMG_OBJS_INSPECT);
			}
		}
		catch (CoreException e)
		{
			IdeLog.logError(JSDebugPlugin.getDefault(), StringUtils.EMPTY, e);
		}
		return super.getImage(element);
	}

	/**
	 * getStackFrameText
	 * 
	 * @param frame
	 * @return String
	 * @throws DebugException
	 */
	private String getStackFrameText(IStackFrame frame) throws DebugException
	{
		String fileName;
		if (frame instanceof IJSStackFrame)
		{
			fileName = ((IJSStackFrame) frame).getSourceFileName();
			IFile file = PathUtils.findWorkspaceFile(fileName);
			if (file != null)
			{
				fileName = file.getFullPath().lastSegment();
			}
		}
		else
		{
			fileName = Messages.JSDebugModelPresentation_line;
		}
		int line = frame.getLineNumber();
		return StringUtils.format("{0} [{1}:{2}]", //$NON-NLS-1$
				new String[] { frame.getName(), fileName,
						line >= 0 ? Integer.toString(line) : Messages.JSDebugModelPresentation_notavailable });
	}

	/**
	 * getThreadText
	 * 
	 * @param thread
	 * @return String
	 * @throws CoreException
	 */
	private String getThreadText(IThread thread) throws CoreException
	{
		String stateString = null;
		if (thread.isTerminated())
		{
			stateString = Messages.JSDebugModelPresentation_Terminated;
		}
		else if (thread.isSuspended())
		{
			stateString = Messages.JSDebugModelPresentation_Suspended;
			IBreakpoint[] breakpoints = thread.getBreakpoints();
			if (breakpoints.length > 0)
			{
				IBreakpoint breakpoint = breakpoints[0];
				String fileName;
				String lineNumber;
				if (breakpoint instanceof IJSImplicitBreakpoint)
				{
					IJSImplicitBreakpoint implicitBreakpoint = (IJSImplicitBreakpoint) breakpoint;
					fileName = implicitBreakpoint.getFileName();
					IFile file = PathUtils.findWorkspaceFile(fileName);
					if (file != null)
					{
						fileName = file.getFullPath().toString();
					}

					try
					{
						lineNumber = Integer.toString(implicitBreakpoint.getLineNumber());
					}
					catch (CoreException impossible)
					{
						lineNumber = "-1"; //$NON-NLS-1$
					}
					String format = Messages.JSDebugModelPresentation_lineIn_0_1_2;
					if (implicitBreakpoint.isDebuggerKeyword())
					{
						format = Messages.JSDebugModelPresentation_keywordAtLine_0_1_2;
					}
					else if (implicitBreakpoint.isFirstLine())
					{
						format = Messages.JSDebugModelPresentation_atStartLine_0_1_2;
					}
					else if (implicitBreakpoint.isException())
					{
						format = Messages.JSDebugModelPresentation_exceptionAtLine_0_1_2;
					}
					else if (implicitBreakpoint.isWatchpoint())
					{
						format = Messages.JSDebugModelPresentation_watchpointAtLine_0_1_2;
					}
					stateString = StringUtils.format(format, new String[] { stateString, lineNumber, fileName });
				}
				else
				{
					IMarker marker = breakpoint.getMarker();
					if (marker instanceof IUniformResourceMarker)
					{
						fileName = PathUtils.getPath(((IUniformResourceMarker) marker).getUniformResource());
					}
					else if (marker.getResource() instanceof IWorkspaceRoot)
					{
						URI uri = URI.create((String) marker.getAttribute(IDebugConstants.BREAKPOINT_LOCATION));
						if ("file".equals(uri.getScheme())) { //$NON-NLS-1$
							fileName = PathUtils.getPath(uri);
							IFile file = PathUtils.findWorkspaceFile(fileName);
							if (file != null)
							{
								fileName = file.getFullPath().toString();
							}
						}
						else
						{
							fileName = uri.toString();
						}
					}
					else
					{
						fileName = marker.getResource().getFullPath().toString();
					}
					lineNumber = Integer.toString(marker.getAttribute(IMarker.LINE_NUMBER, -1));
					if (breakpoint instanceof IJSLineBreakpoint && ((IJSLineBreakpoint) breakpoint).isRunToLine())
					{
						stateString = StringUtils.format(Messages.JSDebugModelPresentation_runToLine_0_1_2,
								new String[] { stateString, lineNumber, fileName });
					}
					else
					{
						stateString = StringUtils.format(Messages.JSDebugModelPresentation_breakpointAtLine_0_1_2,
								new String[] { stateString, lineNumber, fileName });
					}
				}
			}
		}
		else if (thread.isStepping())
		{
			stateString = Messages.JSDebugModelPresentation_Stepping;
		}
		else
		{
			stateString = Messages.JSDebugModelPresentation_Running;
		}
		if (stateString != null)
		{
			return StringUtils.format("{0} ({1})", new String[] { thread.getName(), stateString }); //$NON-NLS-1$
		}
		return thread.getName();
	}

	/**
	 * getBreakpointText
	 * 
	 * @param breakpoint
	 * @return String
	 * @throws CoreException
	 */
	private String getBreakpointText(IBreakpoint breakpoint) throws CoreException
	{
		if (breakpoint instanceof IJSExceptionBreakpoint)
		{
			return getExceptionBreakpointText((IJSExceptionBreakpoint) breakpoint);
		}
		if(breakpoint instanceof IJSWatchpoint) {
			return getWatchpointText((IJSWatchpoint) breakpoint);
		}

		StringBuffer label = new StringBuffer();
		IMarker marker = breakpoint.getMarker();
		if (marker instanceof IUniformResourceMarker)
		{
			label.append(PathUtils.getPath(((IUniformResourceMarker) marker).getUniformResource()));
		}
		else
		{
			IResource resource = marker.getResource();
			if (resource != null)
			{
				label.append(resource.getFullPath().toString());
			}
		}
		if (breakpoint instanceof ILineBreakpoint)
		{
			try
			{
				int lineNumber = ((ILineBreakpoint) breakpoint).getLineNumber();
				label
						.append(StringUtils
								.format(
										" [{0}: {1}]", new String[] { Messages.JSDebugModelPresentation_line, Integer.toString(lineNumber) })); //$NON-NLS-1$
			}
			catch (CoreException e)
			{
			}
		}
		return label.toString();
	}

	/**
	 * getBreakpointImage
	 * 
	 * @param breakpoint
	 * @return Image
	 * @throws CoreException
	 */
	private Image getBreakpointImage(IBreakpoint breakpoint) throws CoreException
	{
		if (breakpoint instanceof IJSExceptionBreakpoint)
		{
			return DebugUIImages.get(DebugUIImages.IMG_OBJS_JSEXCEPTION);
		}
		else if(breakpoint instanceof IJSWatchpoint)
		{
			return DebugUIImages.get(DebugUIImages.IMG_OBJS_JSWATCHPOINT); 
		}
		else
		{
			int flags = computeBreakpointAdornmentFlags(breakpoint);
			JSDebugImageDescriptor descriptor = null;
			if (breakpoint.isEnabled())
			{
				descriptor = new JSDebugImageDescriptor(DebugUITools
						.getImageDescriptor(org.eclipse.debug.ui.IDebugUIConstants.IMG_OBJS_BREAKPOINT), flags);
			}
			else
			{
				descriptor = new JSDebugImageDescriptor(DebugUITools
						.getImageDescriptor(org.eclipse.debug.ui.IDebugUIConstants.IMG_OBJS_BREAKPOINT_DISABLED), flags);
			}
			return DebugUIImages.getImageDescriptorRegistry().get(descriptor);
		}
	}

	/**
	 * computeBreakpointAdornmentFlags
	 * 
	 * @param breakpoint
	 * @return int
	 */
	private int computeBreakpointAdornmentFlags(IBreakpoint breakpoint)
	{
		int flags = 0;
		try
		{
			if (breakpoint.isEnabled())
			{
				flags |= JSDebugImageDescriptor.ENABLED;
			}
			if (breakpoint instanceof IJSLineBreakpoint)
			{
				if (((IJSLineBreakpoint) breakpoint).isConditionEnabled()
						|| ((IJSLineBreakpoint) breakpoint).getHitCount() > 0)
				{
					flags |= JSDebugImageDescriptor.CONDITIONAL;
				}
			}
		}
		catch (CoreException e)
		{
			IdeLog.logError(DebugUiPlugin.getDefault(), StringUtils.EMPTY, e);
		}
		return flags;
	}

	/**
	 * getExceptionBreakpointText
	 * 
	 * @param breakpoint
	 * @return String
	 * @throws CoreException
	 */
	private String getExceptionBreakpointText(IJSExceptionBreakpoint breakpoint) throws CoreException
	{
		return StringUtils.format(Messages.JSDebugModelPresentation_Exception, new String[] { breakpoint
				.getExceptionTypeName() });
	}

	/**
	 * getWatchpointText
	 * 
	 * @param watchpoint
	 * @return String
	 * @throws CoreException
	 */
	private String getWatchpointText(IJSWatchpoint watchpoint) throws CoreException
	{
		return StringUtils.format("{0}", new String[] { watchpoint.getVariableName() }); //$NON-NLS-1$
	}

	/**
	 * getScriptElementText
	 * 
	 * @param scriptElement
	 * @return String
	 * @throws CoreException
	 */
	private String getScriptElementText(IJSScriptElement scriptElement) throws CoreException
	{
		if (scriptElement.getParent() == null)
		{
			return scriptElement.getName();
		}
		return StringUtils.format("{0}()", new String[] { scriptElement.getName() }); //$NON-NLS-1$
	}

	/**
	 * getScriptElementImage
	 * 
	 * @param scriptElement
	 * @return Image
	 */
	private Image getScriptElementImage(IJSScriptElement scriptElement)
	{
		if (scriptElement.getParent() == null)
		{
			return DebugUIImages.get(DebugUIImages.IMG_OBJS_TOP_SCRIPT_ELEMENT);
		}
		return DebugUIImages.get(DebugUIImages.IMG_OBJS_SCRIPT_ELEMENT);
	}

	/**
	 * @see org.eclipse.debug.ui.IDebugModelPresentation#computeDetail(org.eclipse.debug.core.model.IValue,
	 *      org.eclipse.debug.ui.IValueDetailListener)
	 */
	public void computeDetail(IValue value, IValueDetailListener listener)
	{
		IDebugTarget target = value.getDebugTarget();
		if (target.isSuspended() && target instanceof IJSDebugTarget)
		{
			Job job = new DetailsJob(value, listener);
			job.schedule();
			return;
		}
		String details = StringUtils.EMPTY;
		try
		{
			details = value.getValueString();
		}
		catch (DebugException e)
		{
			IdeLog.logError(DebugUiPlugin.getDefault(), StringUtils.EMPTY, e);
		}
		listener.detailComputed(value, details);
	}

	/**
	 * @see org.eclipse.debug.ui.ISourcePresentation#getEditorInput(java.lang.Object)
	 */
	public IEditorInput getEditorInput(Object element)
	{
		return SourceDisplayUtil.getEditorInput(element);
	}

	/**
	 * @see org.eclipse.debug.ui.ISourcePresentation#getEditorId(org.eclipse.ui.IEditorInput, java.lang.Object)
	 */
	public String getEditorId(IEditorInput input, Object element)
	{
		return SourceDisplayUtil.getEditorId(input, element);
	}

	/**
	 * getVariableText
	 * 
	 * @param variable
	 * @return String
	 */
	public String getVariableText(IVariable variable)
	{
		String varLabel = Messages.JSDebugModelPresentation_UnknownName;
		try
		{
			varLabel = variable.getName();
		}
		catch (DebugException e)
		{
		}
		String typeName = Messages.JSDebugModelPresentation_UnknownType;
		try
		{
			typeName = variable.getReferenceTypeName();
		}
		catch (DebugException e)
		{
		}
		IValue value = null;
		try
		{
			value = variable.getValue();
		}
		catch (DebugException e)
		{
		}
		String valueString = Messages.JSDebugModelPresentation_UnknownValue;
		if (value != null)
		{
			try
			{
				valueString = getValueText(value);
			}
			catch (DebugException e)
			{
			}
		}

		StringBuffer sb = new StringBuffer();
		if (showTypes)
		{
			sb.append(typeName).append(' ');
		}

		sb.append(varLabel);

		if (valueString.length() != 0)
		{
			sb.append("= "); //$NON-NLS-1$
			sb.append(valueString);
		}
		return sb.toString();
	}

	/**
	 * getValueText
	 * 
	 * @param value
	 * @return String
	 * @throws DebugException
	 */
	protected String getValueText(IValue value) throws DebugException
	{
		String valueString = value.getValueString();
		return valueString;
	}

	/**
	 * getVariableImage
	 * 
	 * @param variable
	 * @return Image
	 * @throws DebugException
	 */
	protected Image getVariableImage(IVariable variable) throws DebugException
	{
		if (variable instanceof IJSVariable)
		{
			IJSVariable jsVar = (IJSVariable) variable;
			if (jsVar.isException())
			{
				return DebugUIImages.get(DebugUIImages.IMG_OBJS_EXCEPTION_VARIABLE);
			}
			if (jsVar.isLocal())
			{
				return DebugUIImages.get(DebugUIImages.IMG_OBJS_LOCAL_VARIABLE);
			}
			if (jsVar.isTopLevel())
			{
				return DebugUIImages.get(DebugUIImages.IMG_OBJS_VARIABLE);
			}
			if (jsVar.isConst())
			{
				return DebugUIImages.get(DebugUIImages.IMG_OBJS_CONSTANT_FIELD);
			}
			return DebugUIImages.get(DebugUIImages.IMG_OBJS_FIELD);
		}
		return null;
	}

	/**
	 * getBreakpoint
	 * 
	 * @param marker
	 * @return IBreakpoint
	 */
	private IBreakpoint getBreakpoint(IMarker marker)
	{
		return DebugPlugin.getDefault().getBreakpointManager().getBreakpoint(marker);
	}

	/**
	 * Details evaluation job
	 */
	private final class DetailsJob extends Job
	{

		private IValue value;
		private IValueDetailListener listener;

		/**
		 * DetailsJob
		 * 
		 * @param value
		 * @param listener
		 */
		public DetailsJob(IValue value, IValueDetailListener listener)
		{
			super(Messages.JSDebugModelPresentation_DetailsComputing);
			setSystem(true);
			this.value = value;
			this.listener = listener;
		}

		protected IStatus run(IProgressMonitor monitor)
		{
			IJSDebugTarget target = (IJSDebugTarget) value.getDebugTarget();
			String details = StringUtils.EMPTY;
			try
			{
				details = target.computeValueDetails(value);
			}
			catch (DebugException e)
			{
				IdeLog.logError(DebugUiPlugin.getDefault(), StringUtils.EMPTY, e);
			}
			listener.detailComputed(value, details);
			return Status.OK_STATUS;
		}
	}
}
