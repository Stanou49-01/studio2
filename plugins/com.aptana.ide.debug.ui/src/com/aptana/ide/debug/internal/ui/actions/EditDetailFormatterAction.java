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

import org.eclipse.debug.core.DebugException;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.window.Window;

import com.aptana.ide.core.IdeLog;
import com.aptana.ide.core.StringUtils;
import com.aptana.ide.debug.core.DetailFormatter;
import com.aptana.ide.debug.core.JSDetailFormattersManager;
import com.aptana.ide.debug.core.model.IJSVariable;
import com.aptana.ide.debug.internal.ui.dialogs.DetailFormatterDialog;
import com.aptana.ide.debug.ui.DebugUiPlugin;

/**
 * @author Max Stepanov
 */
public class EditDetailFormatterAction extends ObjectActionDelegate
{
	/**
	 * @see org.eclipse.ui.IActionDelegate#run(org.eclipse.jface.action.IAction)
	 */
	public void run(IAction action)
	{
		IStructuredSelection selection = getCurrentSelection();
		if (selection == null || selection.size() != 1)
		{
			return;
		}
		Object element = selection.getFirstElement();
		String typeName;
		try
		{
			if (element instanceof IJSVariable)
			{
				typeName = ((IJSVariable) element).getReferenceTypeName();
			}
			else
			{
				return;
			}
		}
		catch (DebugException e)
		{
			IdeLog.logError(DebugUiPlugin.getDefault(), StringUtils.EMPTY, e);
			return;
		}
		JSDetailFormattersManager detailFormattersManager = JSDetailFormattersManager.getDefault();
		DetailFormatter detailFormatter = detailFormattersManager.getAssociatedDetailFormatter(typeName);
		if (new DetailFormatterDialog(DebugUiPlugin.getActivePage().getWorkbenchWindow().getShell(), detailFormatter,
				null, false, true).open() == Window.OK)
		{
			detailFormattersManager.setAssociatedDetailFormatter(detailFormatter);
			refreshCurrentSelection();
		}
	}
}
