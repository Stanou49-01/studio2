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

package com.aptana.ide.debug.internal.ui.preferences;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Table;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.PlatformUI;

import com.aptana.ide.core.StringUtils;
import com.aptana.ide.debug.core.DetailFormatter;
import com.aptana.ide.debug.core.JSDetailFormattersManager;
import com.aptana.ide.debug.internal.ui.IDebugHelpContextIds;
import com.aptana.ide.debug.internal.ui.IDebugUIConstants;
import com.aptana.ide.debug.internal.ui.dialogs.DetailFormatterDialog;
import com.aptana.ide.debug.ui.DebugUiPlugin;

/**
 * @author Max Stepanov
 */
public class JSDetailFormattersPreferencePage extends PreferencePage implements IWorkbenchPreferencePage
{
	private Label listLabel;
	private Button addFormatterButton;
	private Button removeFormatterButton;
	private Button editFormatterButton;
	private Button inlineButton;
	private Button inlineAllButton;

	private CheckboxTableViewer listViewer;
	private SourceViewer sourceViewer;

	private List<String> types;
	private Set<DetailFormatter> formatters;

	/**
	 * JSDetailFormattersPreferencePage
	 */
	public JSDetailFormattersPreferencePage()
	{
		super();
		setTitle(Messages.JSDetailFormattersPreferencePage_DetailFormatters);
		setPreferenceStore(DebugUiPlugin.getDefault().getPreferenceStore());
		setDescription(Messages.JSDetailFormattersPreferencePage_OverrideDefault);
	}

	protected Control createContents(Composite parent)
	{
		initializeDialogUnits(parent);
		
		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayout(GridLayoutFactory.swtDefaults().numColumns(2).margins(0, 0).create());

		GridDataFactory.fillDefaults().applyTo(composite);
		composite.setFont(parent.getFont());

		createFormattersList(composite);
		createOptions(composite);

		Dialog.applyDialogFont(composite);
		noDefaultAndApplyButton();
		
		PlatformUI.getWorkbench().getHelpSystem().setHelp(getControl(),
				IDebugHelpContextIds.JS_DETAIL_FORMATTER_PREFERENCE_PAGE);

		return composite;
	}

	public void init(IWorkbench workbench)
	{
	}

	private void createOptions(Composite parent)
	{
		Group group = new Group(parent, SWT.NONE);
		group.setText(Messages.JSDetailFormattersPreferencePage_ShowVariableDetails);
		GridDataFactory.fillDefaults().span(2, 1).applyTo(group);
		group.setLayout(new GridLayout());

		String preference = getPreferenceStore().getString(IDebugUIConstants.PREF_SHOW_DETAILS);

		inlineButton = new Button(group, SWT.RADIO);
		inlineButton.setText(Messages.JSDetailFormattersPreferencePage_AsLabelForVariablesWithDetailFormatters);
		inlineButton.setSelection(preference.equals(IDebugUIConstants.INLINE_FORMATTERS));

		inlineAllButton = new Button(group, SWT.RADIO);
		inlineAllButton.setText(Messages.JSDetailFormattersPreferencePage_AsLabelForAllVariables);
		inlineAllButton.setSelection(preference.equals(IDebugUIConstants.INLINE_ALL));

		Button detailPane = new Button(group, SWT.RADIO);
		detailPane.setText(Messages.JSDetailFormattersPreferencePage_InDetailPaneOnly);
		detailPane.setSelection(preference.equals(IDebugUIConstants.DETAIL_PANE) || preference.length() == 0);
	}

	private Control createFormattersList(Composite parent)
	{
		Font font = parent.getFont();
		listLabel = new Label(parent, SWT.NONE);
		listLabel.setText(Messages.JSDetailFormattersPreferencePage_TypesWithDetailFormatters);
		GridDataFactory.swtDefaults().span(2, 1).applyTo(listLabel);
		listLabel.setFont(font);

		listViewer = CheckboxTableViewer.newCheckList(parent, SWT.CHECK | SWT.FULL_SELECTION | SWT.MULTI | SWT.BORDER);
		Table table = (Table) listViewer.getControl();
		GridDataFactory.fillDefaults().grab(true, true)
			.hint(convertWidthInCharsToPixels(10), SWT.DEFAULT).applyTo(table);
		table.setFont(font);
		
		listViewer.setContentProvider(new ArrayContentProvider());
		listViewer.setLabelProvider(new LabelProvider()
		{
			public String getText(Object element)
			{
				if (element instanceof DetailFormatter)
				{
					return ((DetailFormatter) element).getTypeName();
				}
				return null;
			}
		});

		// button container
		Composite composite = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(false, true).applyTo(composite);
		composite.setLayout(GridLayoutFactory.fillDefaults().create());

		// Add button
		addFormatterButton = new Button(composite, SWT.PUSH);
		addFormatterButton.setText(StringUtils.ellipsify(Messages.JSDetailFormattersPreferencePage_Add));
		addFormatterButton.setToolTipText(Messages.JSDetailFormattersPreferencePage_AllowToCreateNewDetailFormatter);
		setButtonLayoutData(addFormatterButton);
		addFormatterButton.setFont(font);
		
		// Edit button
		editFormatterButton = new Button(composite, SWT.PUSH);
		editFormatterButton.setText(StringUtils.ellipsify(Messages.JSDetailFormattersPreferencePage_Edit));
		editFormatterButton.setToolTipText(Messages.JSDetailFormattersPreferencePage_EditSelectedDetailFormatter);
		setButtonLayoutData(editFormatterButton);
		editFormatterButton.setFont(font);

		// Remove button
		removeFormatterButton = new Button(composite, SWT.PUSH);
		removeFormatterButton.setText(Messages.JSDetailFormattersPreferencePage_Remove);
		removeFormatterButton.setToolTipText(Messages.JSDetailFormattersPreferencePage_RemoveAllSelectedDetailFormatters);
		setButtonLayoutData(removeFormatterButton);
		removeFormatterButton.setFont(font);

		Label label = new Label(parent, SWT.NONE);
		label.setText(Messages.JSDetailFormattersPreferencePage_DetailFormatterCodeSnippetDefinedForSelectedType);
		GridDataFactory.fillDefaults().grab(true, false).span(2, 1).applyTo(label);
		label.setFont(font);

		sourceViewer = new SourceViewer(parent, null, SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
		sourceViewer.getTextWidget().setBackground(getShell().getDisplay().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));
		sourceViewer.getTextWidget().setFont(JFaceResources.getTextFont());
		GridDataFactory.fillDefaults().grab(true, true).span(2, 1).applyTo(sourceViewer.getControl());

		// listeners 
		listViewer.addSelectionChangedListener(new ISelectionChangedListener()
		{
			public void selectionChanged(SelectionChangedEvent event)
			{
				updatePage((IStructuredSelection) event.getSelection());
			}
		});
		listViewer.addDoubleClickListener(new IDoubleClickListener()
		{
			public void doubleClick(DoubleClickEvent event)
			{
				if (!event.getSelection().isEmpty())
				{
					editType();
				}
			}
		});
		listViewer.addCheckStateListener(new ICheckStateListener()
		{
			public void checkStateChanged(CheckStateChangedEvent event)
			{
				((DetailFormatter) event.getElement()).setEnabled(event.getChecked());
			}
		});

		table.addKeyListener(new KeyAdapter()
		{
			public void keyPressed(KeyEvent event)
			{
				if (event.character == SWT.DEL && event.stateMask == 0)
				{
					removeTypes();
				}
			}
		});

		addFormatterButton.addListener(SWT.Selection, new Listener()
		{
			public void handleEvent(Event e)
			{
				addType();
			}
		});
		editFormatterButton.addListener(SWT.Selection, new Listener()
		{
			public void handleEvent(Event e)
			{
				editType();
			}
		});
		removeFormatterButton.addListener(SWT.Selection, new Listener()
		{
			public void handleEvent(Event e)
			{
				removeTypes();
			}
		});

		editFormatterButton.setEnabled(false);
		removeFormatterButton.setEnabled(false);
		
		sourceViewer.setEditable(false);
		sourceViewer.setDocument(new Document());

		loadDetailFormatters();
		listViewer.setInput(formatters);
		updateViewerCheckboxes();
		
		return parent;
	}

	private void updatePage(IStructuredSelection selection)
	{
		removeFormatterButton.setEnabled(!selection.isEmpty());
		editFormatterButton.setEnabled(selection.size() == 1);
		sourceViewer.getDocument().set(selection.size() == 1
				? ((DetailFormatter) selection.getFirstElement()).getSnippet()
				: StringUtils.EMPTY);
	}

	private void addType()
	{
		DetailFormatter detailFormatfer = new DetailFormatter(StringUtils.EMPTY, StringUtils.EMPTY, true);
		DetailFormatterDialog dlg = new DetailFormatterDialog(getShell(), detailFormatfer, types, false);
		if (dlg.open() == Window.OK)
		{
			addDetailFormatter(detailFormatfer);
		}
	}

	private void removeTypes()
	{
		Object[] list = formatters.toArray();
		IStructuredSelection selection = (IStructuredSelection) listViewer.getSelection();
		Object first = selection.getFirstElement();
		int index = -1;
		for (int i = 0; i < list.length; i++)
		{
			if (list[i].equals(first))
			{
				index = i;
				break;
			}
		}
		
		removeDetailFormatters(selection.toArray());
		
		list = formatters.toArray();
		if (index > list.length - 1)
		{
			index = list.length - 1;
		}
		if (index >= 0)
		{
			listViewer.setSelection(new StructuredSelection(list[index]));
		}
	}

	private void editType()
	{
		IStructuredSelection selection = (IStructuredSelection) listViewer.getSelection();
		DetailFormatterDialog dlg = new DetailFormatterDialog(getShell(), (DetailFormatter) (selection).getFirstElement(), null, true, true);
		if (dlg.open() == Window.OK)
		{
			listViewer.refresh();
			updateViewerCheckboxes();
			updatePage(selection);
		}
	}

	public boolean performOk()
	{
		JSDetailFormattersManager.getDefault().setDetailFormatters(formatters);

		String value = IDebugUIConstants.DETAIL_PANE;
		if (inlineAllButton.getSelection())
		{
			value = IDebugUIConstants.INLINE_ALL;
		}
		else if (inlineButton.getSelection())
		{
			value = IDebugUIConstants.INLINE_FORMATTERS;
		}
		getPreferenceStore().setValue(IDebugUIConstants.PREF_SHOW_DETAILS, value);
		DebugUiPlugin.getDefault().savePluginPreferences();
		return true;
	}

	private void updateViewerCheckboxes()
	{
		DetailFormatter[] checkedElementsTmp = new DetailFormatter[formatters.size()];
		int i = 0;
		for (Iterator iter = formatters.iterator(); iter.hasNext();)
		{
			DetailFormatter detailFormatter = (DetailFormatter) iter.next();
			if (detailFormatter.isEnabled())
			{
				checkedElementsTmp[i++] = detailFormatter;
			}
		}
		DetailFormatter[] checkedElements = new DetailFormatter[i];
		System.arraycopy(checkedElementsTmp, 0, checkedElements, 0, i);
		listViewer.setAllChecked(false);
		listViewer.setCheckedElements(checkedElements);
	}

	private void addDetailFormatter(DetailFormatter detailFormatter)
	{
		formatters.add(detailFormatter);
		types.add(detailFormatter.getTypeName());
		listViewer.refresh();
		updateViewerCheckboxes();
		IStructuredSelection selection = new StructuredSelection(detailFormatter);
		listViewer.setSelection(selection);
		updatePage(selection);
	}
	
	private void removeDetailFormatters(Object[] detailFormatters)
	{
		for (int i = 0; i < detailFormatters.length; ++i)
		{
			formatters.remove(detailFormatters[i]);
			types.remove(((DetailFormatter) detailFormatters[i]).getTypeName());
		}
		listViewer.refresh();
		listViewer.setSelection(StructuredSelection.EMPTY);
		updatePage(StructuredSelection.EMPTY);
	}

	private void loadDetailFormatters()
	{
		formatters = new TreeSet<DetailFormatter>();
		Collection detailFormatters = JSDetailFormattersManager.getDefault().getDetailFormatters();
		types = new ArrayList<String>(detailFormatters.size());
		for (Iterator i = detailFormatters.iterator(); i.hasNext();)
		{
			DetailFormatter detailFormatter = (DetailFormatter) i.next();
			formatters.add(detailFormatter);
			types.add(detailFormatter.getTypeName());
		}
	}
	
}
