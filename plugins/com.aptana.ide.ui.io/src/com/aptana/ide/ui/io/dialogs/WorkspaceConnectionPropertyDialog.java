/**
 * This file Copyright (c) 2005-2010 Aptana, Inc. This program is
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

package com.aptana.ide.ui.io.dialogs;

import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.internal.ide.dialogs.FileFolderSelectionDialog;

import com.aptana.ide.core.CoreStrings;
import com.aptana.ide.core.IdeLog;
import com.aptana.ide.core.StringUtils;
import com.aptana.ide.core.io.CoreIOPlugin;
import com.aptana.ide.core.io.WorkspaceConnectionPoint;
import com.aptana.ide.core.io.efs.EFSUtils;
import com.aptana.ide.core.ui.PixelConverter;
import com.aptana.ide.ui.IPropertyDialog;
import com.aptana.ide.ui.io.IOUIPlugin;

/**
 * @author Max Stepanov
 *
 */
@SuppressWarnings("restriction")
public class WorkspaceConnectionPropertyDialog extends TitleAreaDialog implements IPropertyDialog {

	private static final String DEFAULT_NAME = "New Project Shortcut";
	
	private WorkspaceConnectionPoint workspaceConnectionPoint;
	private boolean isNew = false;

	private Text nameText;
	private Text workspacePathText;
	private Button browseButton;

	private Image titleImage;

	private ModifyListener modifyListener;

	/**
	 * @param parentShell
	 */
	public WorkspaceConnectionPropertyDialog(Shell parentShell) {
		super(parentShell);
	}

	/* (non-Javadoc)
	 * @see com.aptana.ide.ui.io.IPropertyDialog#setPropertySource(java.lang.Object)
	 */
	public void setPropertySource(Object element) {
		workspaceConnectionPoint = null;
		if (element instanceof WorkspaceConnectionPoint) {
			workspaceConnectionPoint = (WorkspaceConnectionPoint) element;
		}
	}

	/* (non-Javadoc)
	 * @see com.aptana.ide.ui.IPropertyDialog#getPropertySource()
	 */
	public Object getPropertySource() {
		return workspaceConnectionPoint;
	}

	private String getConnectionPointType() {
		return WorkspaceConnectionPoint.TYPE;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.TitleAreaDialog#createDialogArea(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	protected Control createDialogArea(Composite parent) {
		Composite dialogArea = (Composite) super.createDialogArea(parent);

		titleImage = IOUIPlugin.getImageDescriptor("/icons/full/wizban/workspace.png").createImage();
		dialogArea.addDisposeListener(new DisposeListener() {
			public void widgetDisposed(DisposeEvent e) {
				if (titleImage != null) {
					setTitleImage(null);
					titleImage.dispose();
					titleImage = null;
				}
			}
		});
		
		setTitleImage(titleImage);
		if (workspaceConnectionPoint != null) {
			setTitle("Edit the Project Shortcut");
			getShell().setText("Edit Project Shortcut");
		} else {
			setTitle("Create a Project Shortcut");
			getShell().setText("New Project Shortcut");
		}
		
		Composite container = new Composite(dialogArea, SWT.NONE);
		container.setLayoutData(GridDataFactory.fillDefaults().grab(true, true).create());
		container.setLayout(GridLayoutFactory.swtDefaults()
				.margins(convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_MARGIN), convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_MARGIN))
				.spacing(convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_SPACING), convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_SPACING))
				.numColumns(3).create());
		
		/* row 1 */
		Label label = new Label(container, SWT.NONE);
		label.setLayoutData(GridDataFactory.swtDefaults().hint(
				new PixelConverter(label).convertHorizontalDLUsToPixels(IDialogConstants.LABEL_WIDTH),
				SWT.DEFAULT).create());
		label.setText(StringUtils.makeFormLabel("Shortcut Name"));
		
		nameText = new Text(container, SWT.SINGLE | SWT.BORDER);
		nameText.setLayoutData(GridDataFactory.fillDefaults()
				.hint(convertHorizontalDLUsToPixels(IDialogConstants.ENTRY_FIELD_WIDTH), SWT.DEFAULT)
				.span(2, 1).grab(true, false).create());
		
		/* row 2 */
		label = new Label(container, SWT.NONE);
		label.setLayoutData(GridDataFactory.swtDefaults().hint(
				new PixelConverter(label).convertHorizontalDLUsToPixels(IDialogConstants.LABEL_WIDTH),
				SWT.DEFAULT).create());
		label.setText(StringUtils.makeFormLabel("Workspace Path"));

		workspacePathText = new Text(container, SWT.SINGLE | SWT.BORDER);
		workspacePathText.setLayoutData(GridDataFactory.swtDefaults()
				.hint(convertHorizontalDLUsToPixels(IDialogConstants.ENTRY_FIELD_WIDTH), SWT.DEFAULT)
				.grab(true, false).create());
		workspacePathText.setEditable(false);
		
		browseButton = new Button(container, SWT.PUSH);
		browseButton.setText('&'+StringUtils.ellipsify(CoreStrings.BROWSE));
		browseButton.setLayoutData(GridDataFactory.fillDefaults().hint(
				Math.max(
					new PixelConverter(browseButton).convertHorizontalDLUsToPixels(IDialogConstants.BUTTON_WIDTH),
					browseButton.computeSize(SWT.DEFAULT, SWT.DEFAULT, true).x
				), SWT.DEFAULT).create());
		
		/* -- */
		addListeners();

		browseButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				browseWorkspace();
			}
		});
		
		if (workspaceConnectionPoint == null) {
			try {
				workspaceConnectionPoint = (WorkspaceConnectionPoint) CoreIOPlugin.getConnectionPointManager().createConnectionPoint(getConnectionPointType());
				workspaceConnectionPoint.setName(DEFAULT_NAME);
				isNew = true;
			} catch (CoreException e) {
				IdeLog.logError(IOUIPlugin.getDefault(), "Create new connection failed", e);
				close();
			}
		}
		loadPropertiesFrom(workspaceConnectionPoint);

		return dialogArea;
	}

	protected void addListeners() {
		if (modifyListener == null) {
			modifyListener = new ModifyListener() {
				public void modifyText(ModifyEvent e) {
					validate();
				}
			};
		}
		nameText.addModifyListener(modifyListener);
		workspacePathText.addModifyListener(modifyListener);
	}
	
	protected void removeListeners() {
		if (modifyListener != null) {
			nameText.removeModifyListener(modifyListener);
			workspacePathText.removeModifyListener(modifyListener);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.Dialog#okPressed()
	 */
	@Override
	protected void okPressed() {
		if (!isValid()) {
			return;
		}
		if (savePropertiesTo(workspaceConnectionPoint)) {
			/* TODO: notify */
		}
		if (isNew) {
			CoreIOPlugin.getConnectionPointManager().addConnectionPoint(workspaceConnectionPoint);
		}
		super.okPressed();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.TitleAreaDialog#createContents(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	protected Control createContents(Composite parent) {
		try {
			return super.createContents(parent);
		} finally {
			validate();
		}
	}

	protected void loadPropertiesFrom(WorkspaceConnectionPoint connectionPoint) {
		removeListeners();
		try {
			nameText.setText(valueOrEmpty(connectionPoint.getName()));
			IContainer resource = connectionPoint.getResource();
			workspacePathText.setText(resource != null ? resource.getFullPath().toPortableString() : StringUtils.EMPTY);
		} finally {
			addListeners();
		}
	}

	protected boolean savePropertiesTo(WorkspaceConnectionPoint connectionPoint) {
		boolean updated = false;
		String name = nameText.getText();
		if (!name.equals(connectionPoint.getName())) {
			connectionPoint.setName(name);
			updated = true;
		}
		IPath path = Path.fromPortableString(workspacePathText.getText());
		IResource resource = ResourcesPlugin.getWorkspace().getRoot().findMember(path);
		if (resource instanceof IContainer) {
			if (!resource.equals(connectionPoint.getResource())) {
				connectionPoint.setResource((IContainer) resource);
				updated = true;
			}
		}
		return updated;
	}

	private void browseWorkspace() {
		FileFolderSelectionDialog dlg = new FileFolderSelectionDialog(getShell(), false, IResource.FOLDER);
		IPath path = Path.fromPortableString(workspacePathText.getText());
		IResource resource = ResourcesPlugin.getWorkspace().getRoot().findMember(path);
		if (resource instanceof IContainer) {
			dlg.setInitialSelection(EFSUtils.getFileStore(resource));
		}
		dlg.setInput(EFSUtils.getFileStore(ResourcesPlugin.getWorkspace().getRoot()));
		if (dlg.open() == Window.OK) {
			IFileStore fileStore = (IFileStore) dlg.getFirstResult();
			if (fileStore != null) {
				resource = (IResource) fileStore.getAdapter(IResource.class);
				if (resource instanceof IContainer) {
					workspacePathText.setText(resource.getFullPath().toPortableString());
					if (DEFAULT_NAME.equals(nameText.getText())) {
						nameText.setText(resource.getName());
					}
				}
			}
		}
	}
	
	public void validate() {
		boolean valid = isValid();
		getButton(OK).setEnabled(valid);
	}
	
	public boolean isValid() {
		String message = null;
		if (nameText.getText().length() == 0) {
			message = "Please specify shortcut name";
		} else {
			IPath path = Path.fromPortableString(workspacePathText.getText());
			IContainer container = null;
			if (path.segmentCount() == 1) {
				container = ResourcesPlugin.getWorkspace().getRoot().getProject(path.segment(0));			
			} else if (path.segmentCount() > 1) {
				container = ResourcesPlugin.getWorkspace().getRoot().getFolder(path);
			} else {
				message = "Please specify workspace location";
			}
			if (message == null && (container == null || !container.exists())) {
				message = "The resource doesn't exist";
			}
		}
		if (message != null) {
			setErrorMessage(message);
		} else {
			setErrorMessage(null);
			setMessage(null);
			return true;
		}
		return false;
	}

	protected static String valueOrEmpty(String value) {
		if (value != null) {
			return value;
		}
		return StringUtils.EMPTY;
	}

}
