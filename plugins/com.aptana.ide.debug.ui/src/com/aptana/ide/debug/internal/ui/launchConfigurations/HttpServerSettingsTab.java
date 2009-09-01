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
package com.aptana.ide.debug.internal.ui.launchConfigurations;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTab;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ColumnPixelData;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableLayout;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;

import com.aptana.ide.core.IdeLog;
import com.aptana.ide.core.StringUtils;
import com.aptana.ide.core.ui.PixelConverter;
import com.aptana.ide.core.ui.PreferenceUtils;
import com.aptana.ide.debug.core.ILaunchConfigurationConstants;
import com.aptana.ide.debug.core.JSDebugOptionsManager;
import com.aptana.ide.debug.core.JSDebugPlugin;
import com.aptana.ide.debug.internal.ui.dialogs.HttpServerPathDialog;
import com.aptana.ide.debug.ui.DebugUiPlugin;

/**
 * @author Max Stepanov
 */
public class HttpServerSettingsTab extends AbstractLaunchConfigurationTab
{
	
	/**
	 * PathElement 
	 *
	 */
	private class PathElement
	{
		protected boolean enabled;
		protected String serverPath;
		protected String localPath;
		
		protected PathElement(boolean enabled, String serverPath, String localPath)
		{
			this.enabled = enabled;
			this.serverPath = serverPath;
			this.localPath = localPath;
		}
	}
	
	/**
	 * TableLabelProvider 
	 *
	 */
	private class TableLabelProvider extends LabelProvider implements ITableLabelProvider {

		public Image getColumnImage(Object element, int columnIndex) {
			return null;
		}

		public String getColumnText(Object element, int columnIndex) {
			if ( element instanceof PathElement )
			{
				PathElement pathElement = (PathElement) element;
				switch(columnIndex) {
				case 1:
					return pathElement.serverPath;
				case 2:
					return pathElement.localPath;
				default:
				}
			}
			return null;
		}
		
	}
	
	private Image image;
	private CheckboxTableViewer fListViewer;
	private Button fAddButton;
	private Button fRemoveButton;
	private Button fEditButton;
	
	private List<PathElement> elements;

	/**
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#createControl(org.eclipse.swt.widgets.Composite)
	 */
	public void createControl(Composite parent)
	{
		Composite composite = new Composite(parent, SWT.NONE);
		composite.setFont(parent.getFont());
		composite.setLayout(new GridLayout(2, false));
		composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		Label label = new Label(composite, SWT.NONE);
		label.setText(Messages.HttpServerSettingsTab_WebServerPathConfiguration);
		label.setFont(parent.getFont());
		label.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1));

		fListViewer = CheckboxTableViewer.newCheckList(composite, SWT.CHECK | SWT.BORDER | SWT.MULTI | SWT.FULL_SELECTION);
		
		Table table = fListViewer.getTable();
		table.setHeaderVisible(true);
		table.setLinesVisible(true);
		
		TableColumn[] columns = new TableColumn[] {
				new TableColumn(table, SWT.NONE),
				new TableColumn(table, SWT.NONE),
				new TableColumn(table, SWT.NONE),
		};
		columns[1].setText(Messages.HttpServerSettingsTab_ServerPath);
		columns[2].setText(Messages.HttpServerSettingsTab_WorkspacePath);

		TableLayout tableLayout = new TableLayout();
		tableLayout.addColumnData( new ColumnPixelData(24) );
		tableLayout.addColumnData( new ColumnWeightData(40) );
		tableLayout.addColumnData( new ColumnWeightData(60) );
		table.setLayout(tableLayout);
			
		fListViewer.setContentProvider(new ArrayContentProvider());
		fListViewer.setLabelProvider(new TableLabelProvider());

		table.setFont(parent.getFont());
		table.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		// button container
		Composite buttonContainer = new Composite(composite, SWT.NONE);
		buttonContainer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		GridLayout buttonLayout = new GridLayout(1, false);
		buttonLayout.marginHeight = 0;
		buttonLayout.marginWidth = 0;
		buttonContainer.setLayout(buttonLayout);

		GridData data;
		
		// Add type button
		fAddButton = new Button(buttonContainer, SWT.PUSH);
		fAddButton.setText(StringUtils.ellipsify(Messages.HttpServerSettingsTab_Add));
		fAddButton.setToolTipText(Messages.HttpServerSettingsTab_AddServerPath);
		fAddButton.setFont(parent.getFont());
		data = new GridData(SWT.FILL, SWT.DEFAULT);
		data.widthHint = Math.max(
				new PixelConverter(fAddButton).convertHorizontalDLUsToPixels(IDialogConstants.BUTTON_WIDTH),
				fAddButton.computeSize(SWT.DEFAULT, SWT.DEFAULT, true).x);
		fAddButton.setLayoutData(data);

		// Edit button
		fEditButton = new Button(buttonContainer, SWT.PUSH);
		fEditButton.setText(StringUtils.ellipsify(Messages.HttpServerSettingsTab_Edit));
		fEditButton.setToolTipText(Messages.HttpServerSettingsTab_EditSelectedPath);
		fEditButton.setFont(parent.getFont());
		data = new GridData(SWT.FILL, SWT.DEFAULT);
		data.widthHint = Math.max(
				new PixelConverter(fEditButton).convertHorizontalDLUsToPixels(IDialogConstants.BUTTON_WIDTH),
				fEditButton.computeSize(SWT.DEFAULT, SWT.DEFAULT, true).x);
		fEditButton.setLayoutData(data);

		// Remove button
		fRemoveButton = new Button(buttonContainer, SWT.PUSH);
		fRemoveButton.setText(Messages.HttpServerSettingsTab_Remove);
		fRemoveButton.setToolTipText(Messages.HttpServerSettingsTab_RemoveSelectedPath);
		fRemoveButton.setFont(parent.getFont());
		data = new GridData(SWT.FILL, SWT.DEFAULT);
		data.widthHint = Math.max(
				new PixelConverter(fRemoveButton).convertHorizontalDLUsToPixels(IDialogConstants.BUTTON_WIDTH),
				fRemoveButton.computeSize(SWT.DEFAULT, SWT.DEFAULT, true).x);
		fRemoveButton.setLayoutData(data);
		
		fListViewer.addCheckStateListener(new ICheckStateListener()
		{
			public void checkStateChanged(CheckStateChangedEvent event)
			{
				((PathElement) event.getElement()).enabled = event.getChecked();
				setDirty(true);
				updateLaunchConfigurationDialog();
			}
		});
		fListViewer.addSelectionChangedListener( new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				updatePage((IStructuredSelection) event.getSelection());
			}
		});
		fListViewer.addDoubleClickListener(new IDoubleClickListener()
		{
			public void doubleClick(DoubleClickEvent event)
			{
				if (!event.getSelection().isEmpty())
				{
					editPath();
				}
			}
		});
		table.addKeyListener(new KeyAdapter()
		{
			public void keyPressed(KeyEvent event)
			{
				if (event.character == SWT.DEL && event.stateMask == 0)
				{
					removePaths();
				}
			}
		});

		fAddButton.addListener(SWT.Selection, new Listener()
		{
			public void handleEvent(Event e)
			{
				addPath();
			}
		});

		fEditButton.addListener(SWT.Selection, new Listener()
		{
			public void handleEvent(Event e)
			{
				editPath();
			}
		});
		fEditButton.setEnabled(false);

		fRemoveButton.addListener(SWT.Selection, new Listener()
		{
			public void handleEvent(Event e)
			{
				removePaths();
			}
		});
		fRemoveButton.setEnabled(false);

		setControl(composite);
		PreferenceUtils.persist(DebugUiPlugin.getDefault().getPreferenceStore(), table, "httpServerSettings"); //$NON-NLS-1$
	}

	private void addPath() {
		HttpServerPathDialog dlg = new HttpServerPathDialog(getShell(), Messages.HttpServerSettingsTab_AddNewPath);
		if ( dlg.open() == Window.OK ) {
			PathElement element = new PathElement(true, dlg.getServerPath(),
					dlg.getWorkspaceResource().getFullPath().toPortableString());
			elements.add(element);
			fListViewer.refresh();
			fListViewer.setSelection(new StructuredSelection(element));
			
			refreshViewer();
			setDirty(true);
			updateLaunchConfigurationDialog();
		}
	}

	private void editPath() {
		PathElement element = (PathElement) ((IStructuredSelection)fListViewer.getSelection()).getFirstElement();
		if ( element == null ) {
			return;
		}
		HttpServerPathDialog dlg = new HttpServerPathDialog(getShell(), Messages.HttpServerSettingsTab_EditPath);
		dlg.setServerPath(element.serverPath);
		dlg.setWorkspaceResource(ResourcesPlugin.getWorkspace().getRoot().findMember(new Path(element.localPath)));
		if ( dlg.open() == Window.OK ) {
			element.serverPath = dlg.getServerPath();
			element.localPath = dlg.getWorkspaceResource().getFullPath().toPortableString();
			element.enabled = true;
			
			fListViewer.update(element, null);
			refreshViewer();
			setDirty(true);
			updateLaunchConfigurationDialog();
		}
	}

	private void removePaths() {
		IStructuredSelection selection = (IStructuredSelection) fListViewer.getSelection();
		Object first = selection.getFirstElement();
		int index = -1;
		for (int i = 0; i < elements.size(); i++)
		{
			Object object = elements.get(i);
			if (object.equals(first))
			{
				index = i;
				break;
			}
		}
		elements.removeAll(selection.toList());
		if (index > elements.size() - 1)
		{
			index = elements.size() - 1;
		}
		if (index >= 0)
		{
			fListViewer.setSelection(new StructuredSelection(elements.get(index)));
		}
		setDirty(true);
		updateLaunchConfigurationDialog();
	}

	/**
	 * Refresh the formatter list viewer.
	 */
	private void refreshViewer()
	{
		ArrayList<PathElement> checkedElements = new ArrayList<PathElement>();
		for (Iterator i = elements.iterator(); i.hasNext();)
		{
			PathElement pathElement = (PathElement) i.next();
			if ( pathElement.enabled )
			{
				checkedElements.add(pathElement);
			}
		}
		fListViewer.setAllChecked(false);
		fListViewer.setCheckedElements(checkedElements.toArray(new PathElement[checkedElements.size()]));
	}

	private void updatePage(IStructuredSelection selection)
	{
		fRemoveButton.setEnabled(!selection.isEmpty());
		fEditButton.setEnabled(selection.size() == 1);
	}

	/**
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#setDefaults(org.eclipse.debug.core.ILaunchConfigurationWorkingCopy)
	 */
	public void setDefaults(ILaunchConfigurationWorkingCopy configuration)
	{
		configuration.setAttribute(ILaunchConfigurationConstants.CONFIGURATION_SERVER_PATHS_MAPPING, StringUtils.EMPTY);
	}

	/**
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#initializeFrom(org.eclipse.debug.core.ILaunchConfiguration)
	 */
	public void initializeFrom(ILaunchConfiguration configuration)
	{
		try
		{
			String[] list = JSDebugOptionsManager.parseList(configuration.getAttribute(
					ILaunchConfigurationConstants.CONFIGURATION_SERVER_PATHS_MAPPING, StringUtils.EMPTY));
			elements = new ArrayList<PathElement>();
			for (int i = 0, length = list.length; i < length; )
			{
				String serverPath = list[i++];
				String localPath = list[i++];
				boolean enabled = !"0".equals(list[i++]); //$NON-NLS-1$
				elements.add(new PathElement(enabled, serverPath, localPath));
			}
			fListViewer.setInput(elements);
			refreshViewer();
		}
		catch (CoreException e)
		{
			IdeLog.logError(JSDebugPlugin.getDefault(), "Reading launch configuration fails", e); //$NON-NLS-1$
		}
	}

	/**
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#performApply(org.eclipse.debug.core.ILaunchConfigurationWorkingCopy)
	 */
	public void performApply(ILaunchConfigurationWorkingCopy configuration)
	{
		String[] values = new String[elements.size() * 3];
		int index = 0;
		for (Iterator i = elements.iterator(); i.hasNext(); )
		{
			PathElement element = (PathElement) i.next();
			values[index++] = element.serverPath;
			values[index++] = element.localPath;
			values[index++] = element.enabled ? "1" : "0"; //$NON-NLS-1$ //$NON-NLS-2$
		}

		configuration.setAttribute(ILaunchConfigurationConstants.CONFIGURATION_SERVER_PATHS_MAPPING,
				JSDebugOptionsManager.serializeList(values));
	}

	/**
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#getName()
	 */
	public String getName()
	{
		return Messages.HttpServerSettingsTab_Title;
	}

	/**
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#getImage()
	 */
	public Image getImage()
	{
		if (image == null)
		{
			image = DebugUiPlugin.getImageDescriptor("icons/full/obj16/launch-tree.gif").createImage(); //$NON-NLS-1$
		}
		return image;
	}

	/**
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#dispose()
	 */
	public void dispose()
	{
		if (image != null)
		{
			image.dispose();
		}
		super.dispose();
	}
}
