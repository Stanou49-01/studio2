/**
 * This file Copyright (c) 2005-2009 Aptana, Inc. This program is
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

import java.net.URI;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import com.aptana.ide.core.IdeLog;
import com.aptana.ide.core.StringUtils;
import com.aptana.ide.core.io.CoreIOPlugin;
import com.aptana.ide.core.io.GenericConnectionPoint;
import com.aptana.ide.core.ui.PixelConverter;
import com.aptana.ide.ui.IPropertyDialog;
import com.aptana.ide.ui.io.IOUIPlugin;

/**
 * @author Max Stepanov
 *
 */
public class GenericConnectionPropertyDialog extends TitleAreaDialog implements IPropertyDialog {

	private static final String DEFAULT_NAME = "New Connection";
	
	private GenericConnectionPoint genericConnectionPoint;
	private boolean isNew = false;

	private Text nameText;
	private Text uriText;

	private ModifyListener modifyListener;

	/**
	 * @param parentShell
	 */
	public GenericConnectionPropertyDialog(Shell parentShell) {
		super(parentShell);
	}

	/* (non-Javadoc)
	 * @see com.aptana.ide.ui.io.IPropertyDialog#setPropertySource(java.lang.Object)
	 */
	public void setPropertySource(Object element) {
		genericConnectionPoint = null;
		if (element instanceof GenericConnectionPoint) {
			genericConnectionPoint = (GenericConnectionPoint) element;
		}
	}

	/* (non-Javadoc)
	 * @see com.aptana.ide.ui.IPropertyDialog#getPropertySource()
	 */
	public Object getPropertySource() {
		return genericConnectionPoint;
	}

	private String getConnectionPointType() {
		return GenericConnectionPoint.TYPE;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.TitleAreaDialog#createDialogArea(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	protected Control createDialogArea(Composite parent) {
		Composite dialogArea = (Composite) super.createDialogArea(parent);

		if (genericConnectionPoint != null) {
			setTitle("Edit the Connection");
			getShell().setText("Edit Connection");
		} else {
			setTitle("Create a Connection");
			getShell().setText("New Connection");
		}
		
		Composite container = new Composite(dialogArea, SWT.NONE);
		container.setLayoutData(GridDataFactory.fillDefaults().grab(true, true).create());
		container.setLayout(GridLayoutFactory.swtDefaults()
				.margins(convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_MARGIN), convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_MARGIN))
				.spacing(convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_SPACING), convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_SPACING))
				.numColumns(2).create());
		
		/* row 1 */
		Label label = new Label(container, SWT.NONE);
		label.setLayoutData(GridDataFactory.swtDefaults().hint(
				new PixelConverter(label).convertHorizontalDLUsToPixels(IDialogConstants.LABEL_WIDTH),
				SWT.DEFAULT).create());
		label.setText(StringUtils.makeFormLabel("Name"));
		
		nameText = new Text(container, SWT.SINGLE | SWT.BORDER);
		nameText.setLayoutData(GridDataFactory.fillDefaults()
				.hint(convertHorizontalDLUsToPixels(IDialogConstants.ENTRY_FIELD_WIDTH), SWT.DEFAULT)
				.grab(true, false).create());
		
		/* row 2 */
		label = new Label(container, SWT.NONE);
		label.setLayoutData(GridDataFactory.swtDefaults().hint(
				new PixelConverter(label).convertHorizontalDLUsToPixels(IDialogConstants.LABEL_WIDTH),
				SWT.DEFAULT).create());
		label.setText(StringUtils.makeFormLabel("URI"));

		uriText = new Text(container, SWT.SINGLE | SWT.BORDER);
		uriText.setLayoutData(GridDataFactory.swtDefaults()
				.hint(convertHorizontalDLUsToPixels(IDialogConstants.ENTRY_FIELD_WIDTH), SWT.DEFAULT)
				.grab(true, false).create());
				
		/* -- */
		addListeners();

		if (genericConnectionPoint == null) {
			try {
				genericConnectionPoint = (GenericConnectionPoint) CoreIOPlugin.getConnectionPointManager().createConnectionPoint(getConnectionPointType());
				genericConnectionPoint.setName(DEFAULT_NAME);
				isNew = true;
			} catch (CoreException e) {
				IdeLog.logError(IOUIPlugin.getDefault(), "Create new connection failed", e);
				close();
			}
		}
		loadPropertiesFrom(genericConnectionPoint);

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
		uriText.addModifyListener(modifyListener);
	}
	
	protected void removeListeners() {
		if (modifyListener != null) {
			nameText.removeModifyListener(modifyListener);
			uriText.removeModifyListener(modifyListener);
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
		if (savePropertiesTo(genericConnectionPoint)) {
			/* TODO: notify */
		}
		if (isNew) {
			CoreIOPlugin.getConnectionPointManager().addConnectionPoint(genericConnectionPoint);
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
		} catch (RuntimeException e) {
			throw e;
		} finally {
			validate();
		}
	}

	protected void loadPropertiesFrom(GenericConnectionPoint connectionPoint) {
		removeListeners();
		try {
			nameText.setText(valueOrEmpty(connectionPoint.getName()));
			URI uri = connectionPoint.getURI();
			uriText.setText(uri != null ? uri.toString() : StringUtils.EMPTY);
		} finally {
			addListeners();
		}
	}

	protected boolean savePropertiesTo(GenericConnectionPoint connectionPoint) {
		boolean updated = false;
		String name = nameText.getText();
		if (!name.equals(connectionPoint.getName())) {
			connectionPoint.setName(name);
			updated = true;
		}
		URI uri = URI.create(uriText.getText());
		if (!uri.equals(connectionPoint.getURI())) {
			connectionPoint.setURI(uri);
			updated = true;
		}
		return updated;
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
			try {
				if (!URI.create(uriText.getText()).isAbsolute()) {
					message = "Please specify a valid absolute URI";
				}
			} catch (Exception e) {
				message = "Please specify a valid URI";
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
