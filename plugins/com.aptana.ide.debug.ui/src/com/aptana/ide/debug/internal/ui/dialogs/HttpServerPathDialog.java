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

package com.aptana.ide.debug.internal.ui.dialogs;

import java.util.regex.Pattern;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.dialogs.StatusDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.ElementTreeSelectionDialog;
import org.eclipse.ui.model.WorkbenchContentProvider;
import org.eclipse.ui.model.WorkbenchLabelProvider;
import org.eclipse.ui.views.navigator.ResourceSorter;

import com.aptana.ide.core.StringUtils;
import com.aptana.ide.debug.internal.ui.StatusInfo;

/**
 * @author Max Stepanov
 *
 */
public class HttpServerPathDialog extends StatusDialog {

	private static final Pattern SERVER_PATH_PATTERN = Pattern.compile("(/[a-zA-Z0-9_!~*'().;?:@&=+$,%#-]+)*/?"); //$NON-NLS-1$
	
	private String serverPath = StringUtils.EMPTY;
	private IResource resource;
	
	/**
	 * @param parent
	 */
	public HttpServerPathDialog(Shell parent, String title) {
		super(parent);
		setTitle(title);
		setShellStyle(getShellStyle() | SWT.RESIZE);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.Dialog#createDialogArea(org.eclipse.swt.widgets.Composite)
	 */
	protected Control createDialogArea(Composite parent) {
		Composite container = (Composite) super.createDialogArea(parent);
		((GridLayout)container.getLayout()).numColumns = 3;

		Label label = new Label(container, SWT.NONE);
		label.setText(Messages.HttpServerPathDialog_ServerPath);
		GridDataFactory.swtDefaults().applyTo(label);
		
		Text serverPathText = new Text(container, SWT.BORDER);
		serverPathText.setText(serverPath);
		GridDataFactory.fillDefaults().span(2, 1).grab(true, false).hint(250, SWT.DEFAULT).applyTo(serverPathText);
		
		label = new Label(container, SWT.NONE);
		label.setText(Messages.HttpServerPathDialog_WorkspaceLocation);
		GridDataFactory.swtDefaults().applyTo(label);

		final Text workspacePathText = new Text(container, SWT.BORDER|SWT.READ_ONLY);
		if ( resource != null ) {
			workspacePathText.setText(resource.getFullPath().toPortableString());
		}
		GridDataFactory.fillDefaults().grab(true, false).applyTo(workspacePathText);

		Button browseButton = new Button(container, SWT.PUSH);
		browseButton.setText(StringUtils.ellipsify(Messages.HttpServerPathDialog_Browse));
		GridDataFactory.fillDefaults().applyTo(browseButton);
		
		serverPathText.addModifyListener( new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				serverPath = ((Text)e.widget).getText();
				checkValues();
			}
		});
		
		browseButton.addSelectionListener( new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				ElementTreeSelectionDialog dialog = new ElementTreeSelectionDialog(getShell(),
						new WorkbenchLabelProvider(),
						new WorkbenchContentProvider());
				dialog.setInput(ResourcesPlugin.getWorkspace().getRoot());
				dialog.setSorter(new ResourceSorter(ResourceSorter.NAME));
				dialog.addFilter( new ViewerFilter() {
					public boolean select(Viewer viewer, Object parentElement, Object element) {
						return element instanceof IContainer;
					}
				});
				dialog.setMessage(Messages.HttpServerPathDialog_SelectWorkspaceFolder);
				if ( resource != null ) {
					dialog.setInitialSelection(resource);
				}
				if ( dialog.open() == Window.OK ) {
					resource = (IResource) dialog.getFirstResult();
					workspacePathText.setText(resource.getFullPath().toPortableString());
					checkValues();
				}

			}
		});
		
		updateButtonsEnableState(new StatusInfo(IStatus.ERROR, StringUtils.EMPTY));
		
		return container;
	}

	/**
	 * Check the field values and display a message in the status if needed.
	 */
	private void checkValues()
	{
		StatusInfo status = new StatusInfo();
		
		if ( serverPath.length() == 0 || !SERVER_PATH_PATTERN.matcher(serverPath).matches() ) {
			status.setError(Messages.HttpServerPathDialog_Error_IncompleteServerPath);
		} else if ( resource == null ) {
			status.setError(Messages.HttpServerPathDialog_Error_EmptyWorkspaceLocation);
		}
		
		updateStatus(status);
	}

	/**
	 * @return the resource
	 */
	public IResource getWorkspaceResource() {
		return resource;
	}

	/**
	 * @return the serverPath
	 */
	public String getServerPath() {
		return serverPath;
	}

	/**
	 * @param resource the resource to set
	 */
	public void setWorkspaceResource(IResource resource) {
		this.resource = resource;
	}

	/**
	 * @param serverPath the serverPath to set
	 */
	public void setServerPath(String serverPath) {
		this.serverPath = serverPath;
	}
	

}
