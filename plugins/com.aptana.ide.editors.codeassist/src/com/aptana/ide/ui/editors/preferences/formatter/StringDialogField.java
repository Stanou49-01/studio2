/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.aptana.ide.ui.editors.preferences.formatter;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import org.eclipse.jface.text.contentassist.IContentAssistProcessor;


/**
 * Dialog field containing a label and a text control.
 */
public class StringDialogField extends DialogField {
		
	private String fText;
	private Text fTextControl;
	private ModifyListener fModifyListener;
    private IContentAssistProcessor fContentAssistProcessor;
	
	/**
	 * 
	 */
	public StringDialogField() {
		super();
		fText= ""; //$NON-NLS-1$
	}
			
	/**
	 * @param processor
	 */
	public void setContentAssistProcessor(IContentAssistProcessor processor) {
	    
	}

	/**
	 * @return IContentAssistProcessor
	 */
	public IContentAssistProcessor getContentAssistProcessor() {
	    return fContentAssistProcessor;
	}
	
	/**
	 * @see com.aptana.ide.ui.editors.preferences.formatter.DialogField#doFillIntoGrid(org.eclipse.swt.widgets.Composite, int)
	 */
	// ------- layout helpers
		
	/*
	 * @see DialogField#doFillIntoGrid
	 */
	public Control[] doFillIntoGrid(Composite parent, int nColumns) {
		assertEnoughColumns(nColumns);
		
		Label label= getLabelControl(parent);
		label.setLayoutData(gridDataForLabel(1));
		Text text= getTextControl(parent);
		text.setLayoutData(gridDataForText(nColumns - 1));
		
		return new Control[] { label, text };
	} 

	/*
	 * @see DialogField#getNumberOfControls
	 */
	/**
	 * @see com.aptana.ide.ui.editors.preferences.formatter.DialogField#getNumberOfControls()
	 */
	public int getNumberOfControls() {
		return 2;	
	}
	
	/**
	 * @param span
	 * @return GridData
	 */
	protected static GridData gridDataForText(int span) {
		GridData gd= new GridData();
		gd.horizontalAlignment= GridData.FILL;
		gd.grabExcessHorizontalSpace= false;
		gd.horizontalSpan= span;
		return gd;
	}	
	
	// ------- focus methods
	
	/*
	 * @see DialogField#setFocus
	 */
	/**
	 * @see com.aptana.ide.ui.editors.preferences.formatter.DialogField#setFocus()
	 */
	public boolean setFocus() {
		if (isOkToUse(fTextControl)) {
			fTextControl.setFocus();
			fTextControl.setSelection(0, fTextControl.getText().length());
		}
		return true;
	}
		
	// ------- ui creation			

	/**
	 * Creates or returns the created text control.
	 * @param parent The parent composite or <code>null</code> when the widget has
	 * already been created.
	 * @return Text
	 */		
	public Text getTextControl(Composite parent) {
		if (fTextControl == null) {
			assertCompositeNotNull(parent);
			fModifyListener= new ModifyListener() {
				public void modifyText(ModifyEvent e) {
					doModifyText(e);
				}
			};
			
			fTextControl= new Text(parent, SWT.SINGLE | SWT.BORDER);
			// moved up due to 1GEUNW2
			fTextControl.setText(fText);
			fTextControl.setFont(parent.getFont());
			fTextControl.addModifyListener(fModifyListener);
			
			fTextControl.setEnabled(isEnabled());
			if (fContentAssistProcessor != null) {
			    //ControlContentAssistHelper.createTextContentAssistant(fTextControl, fContentAssistProcessor);
			}
		}
		return fTextControl;
	}
	
	private void doModifyText(ModifyEvent e) {
		if (isOkToUse(fTextControl)) {
			fText= fTextControl.getText();
		}
		dialogFieldChanged();
	}		
	
	// ------ enable / disable management
	
	/*
	 * @see DialogField#updateEnableState
	 */		
	/**
	 * @see com.aptana.ide.ui.editors.preferences.formatter.DialogField#updateEnableState()
	 */
	protected void updateEnableState() {
		super.updateEnableState();		
		if (isOkToUse(fTextControl)) {
			fTextControl.setEnabled(isEnabled());
		}	
	}		
		
	// ------ text access 
	
	/**
	 * Gets the text. Can not be <code>null</code>
	 * @return String
	 */	
	public String getText() {
		return fText;
	}
	
	/**
	 * Sets the text. Triggers a dialog-changed event.
	 * @param text 
	 */
	public void setText(String text) {
		fText= text;
		if (isOkToUse(fTextControl)) {
			fTextControl.setText(text);
		} else {
			dialogFieldChanged();
		}	
	}

	/**
	 * Sets the text without triggering a dialog-changed event.
	 * @param text 
	 */
	public void setTextWithoutUpdate(String text) {
		fText= text;
		if (isOkToUse(fTextControl)) {
			fTextControl.removeModifyListener(fModifyListener);
			fTextControl.setText(text);
			fTextControl.addModifyListener(fModifyListener);
		}
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.ui.wizards.dialogfields.DialogField#refresh()
	 */
	/**
	 * @see com.aptana.ide.ui.editors.preferences.formatter.DialogField#refresh()
	 */
	public void refresh() {
		super.refresh();
		if (isOkToUse(fTextControl)) {
			setTextWithoutUpdate(fText);
		}
	}
	
}
