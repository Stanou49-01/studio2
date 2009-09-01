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
package com.aptana.ide.debug.internal.ui.actions;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.model.IDebugElement;
import org.eclipse.debug.core.model.ISourceLocator;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.actions.SelectionProviderAction;
import org.eclipse.ui.plugin.AbstractUIPlugin;

import com.aptana.ide.core.StringUtils;
import com.aptana.ide.debug.core.model.IJSScriptElement;
import com.aptana.ide.debug.internal.ui.util.SourceDisplayUtil;
import com.aptana.ide.debug.ui.DebugUiPlugin;

/**
 * @author Max Stepanov
 */
public class OpenScriptSourceAction extends SelectionProviderAction
{
	private IJSScriptElement scriptElement;

	/**
	 * @param provider
	 */
	public OpenScriptSourceAction(ISelectionProvider provider)
	{
		super(provider, Messages.OpenScriptSourceAction_GoToFile);
		setToolTipText(Messages.OpenScriptSourceAction_GoToFileForScript);
		setImageDescriptor(AbstractUIPlugin.imageDescriptorFromPlugin(
				"com.aptana.ide.debug.ui", "icons/full/elcl16/gotoobj_tsk.gif")); //$NON-NLS-1$ //$NON-NLS-2$
		setEnabled(false);
	}

	/**
	 * @see org.eclipse.ui.actions.SelectionProviderAction#selectionChanged(org.eclipse.jface.viewers.IStructuredSelection)
	 */
	public void selectionChanged(IStructuredSelection selection)
	{
		if (selection.size() == 1)
		{
			Object element = selection.getFirstElement();
			if (element instanceof IJSScriptElement)
			{
				scriptElement = (IJSScriptElement) element;
				String location = scriptElement.getLocation();
				if (location != null)
				{
					setEnabled(true);
					int lineNumber = scriptElement.getBaseLine();
					Object sourceElement = DebugUITools.lookupSource(scriptElement, getSourceLocator(scriptElement))
							.getSourceElement();
					IEditorInput editorInput = SourceDisplayUtil.getEditorInput(sourceElement);
					if (editorInput != null)
					{
						IEditorPart editorPart = SourceDisplayUtil.findEditor(editorInput);
						if (editorPart != null)
						{
							SourceDisplayUtil.revealLineInEditor(editorPart, lineNumber);
						}
					}
					return;
				}
			}
		}
		else
		{
			scriptElement = null;
		}
		setEnabled(false);
	}

	/**
	 * @see org.eclipse.jface.action.IAction#run()
	 */
	public void run()
	{
		if (scriptElement == null)
		{
			selectionChanged(getStructuredSelection());
			if (scriptElement == null)
			{
				return;
			}
		}
		String location = scriptElement.getLocation();
		if (location == null || location.length() == 0)
		{
			return;
		}
		int lineNumber = scriptElement.getBaseLine();

		try
		{
			Object sourceElement = DebugUITools.lookupSource(scriptElement, getSourceLocator(scriptElement))
					.getSourceElement();
			IEditorInput editorInput = SourceDisplayUtil.getEditorInput(sourceElement);
			if (editorInput != null)
			{
				SourceDisplayUtil.openInEditor(editorInput, lineNumber);
				return;
			}
			MessageDialog.openInformation(DebugUiPlugin.getActiveWorkbenchShell(),
					Messages.OpenScriptSourceAction_Information, StringUtils.format(
							Messages.OpenScriptSourceAction_SourceNotFoundFor_0, new String[] { location }));
		}
		catch (CoreException e)
		{
			DebugUiPlugin.errorDialog(Messages.OpenScriptSourceAction_ExceptionWhileOpeningScriptSource, e);
		}
	}

	private ISourceLocator getSourceLocator(IDebugElement debugElement)
	{
		ISourceLocator sourceLocator = null;
		ILaunch launch = debugElement.getLaunch();
		if (launch != null)
		{
			sourceLocator = launch.getSourceLocator();
		}
		return sourceLocator;
	}
}
