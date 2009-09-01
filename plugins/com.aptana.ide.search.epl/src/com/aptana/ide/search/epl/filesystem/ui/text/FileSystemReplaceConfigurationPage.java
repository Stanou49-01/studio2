/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.aptana.ide.search.epl.filesystem.ui.text;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.PatternSyntaxException;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.fieldassist.ComboContentAdapter;
import org.eclipse.jface.text.FindReplaceDocumentAdapterContentProposalProvider;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.ui.refactoring.UserInputWizardPage;
import org.eclipse.search.internal.core.text.PatternConstructor;
import org.eclipse.search.internal.ui.ISearchHelpContextIds;
import org.eclipse.search.internal.ui.Messages;
import org.eclipse.search.internal.ui.SearchMessages;
import org.eclipse.search.internal.ui.SearchPlugin;
import org.eclipse.search.internal.ui.text.FileSearchQuery;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.fieldassist.ContentAssistCommandAdapter;
import org.eclipse.ui.texteditor.ITextEditorActionDefinitionIds;

/**
 * Modified for file system replace.
 */
public class FileSystemReplaceConfigurationPage extends UserInputWizardPage {

    private static final String SETTINGS_GROUP = "ReplaceDialog2"; //$NON-NLS-1$
    private static final String SETTINGS_REPLACE_WITH = "replace_with"; //$NON-NLS-1$

    private final FileSystemReplaceRefactoring fReplaceRefactoring;

    private Combo fTextField;
    private Button fReplaceWithRegex;
    private Label fStatusLabel;
    private ContentAssistCommandAdapter fTextFieldContentAssist;

    public FileSystemReplaceConfigurationPage(
            FileSystemReplaceRefactoring refactoring) {
        super("ReplaceConfigurationPage"); //$NON-NLS-1$
        fReplaceRefactoring = refactoring;
    }

    /**
     * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
     */
    public void createControl(Composite parent) {
        Composite result = new Composite(parent, SWT.NONE);
        GridLayout layout = new GridLayout(2, false);
        result.setLayout(layout);

        Label description = new Label(result, SWT.NONE);
        int numberOfMatches = fReplaceRefactoring.getNumberOfMatches();
        int numberOfFiles = fReplaceRefactoring.getNumberOfFiles();
        String[] arguments = { String.valueOf(numberOfMatches),
                String.valueOf(numberOfFiles) };
        if (numberOfMatches > 1 && numberOfFiles > 1) {
            description
                    .setText(Messages
                            .format(
                                    SearchMessages.ReplaceConfigurationPage_description_many_in_many,
                                    arguments));
        } else if (numberOfMatches == 1) {
            description
                    .setText(SearchMessages.ReplaceConfigurationPage_description_one_in_one);
        } else {
            description
                    .setText(Messages
                            .format(
                                    SearchMessages.ReplaceConfigurationPage_description_many_in_one,
                                    arguments));
        }
        description.setLayoutData(new GridData(GridData.BEGINNING,
                GridData.CENTER, false, false, 2, 1));

        FileSearchQuery query = fReplaceRefactoring.getQuery();

        Label label1 = new Label(result, SWT.NONE);
        label1.setText(SearchMessages.ReplaceConfigurationPage_replace_label);

        Text clabel = new Text(result, SWT.BORDER | SWT.READ_ONLY);
        clabel.setText(query.getSearchString());
        GridData gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.widthHint = convertWidthInCharsToPixels(50);
        clabel.setLayoutData(gd);

        Label label2 = new Label(result, SWT.NONE);
        label2.setText(SearchMessages.ReplaceConfigurationPage_with_label);

        fTextField = new Combo(result, SWT.DROP_DOWN);
        gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.widthHint = convertWidthInCharsToPixels(50);
        fTextField.setLayoutData(gd);
        fTextField.setFocus();
        fTextField.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                updateOKStatus();
            }
        });

        IDialogSettings settings = SearchPlugin.getDefault()
                .getDialogSettings().getSection(SETTINGS_GROUP);
        if (settings != null) {
            String[] previousReplaceWith = settings
                    .getArray(SETTINGS_REPLACE_WITH);
            if (previousReplaceWith != null) {
                fTextField.setItems(previousReplaceWith);
                fTextField.select(0);
            }
        }

        ComboContentAdapter contentAdapter = new ComboContentAdapter();
        FindReplaceDocumentAdapterContentProposalProvider replaceProposer = new FindReplaceDocumentAdapterContentProposalProvider(
                false);
        fTextFieldContentAssist = new ContentAssistCommandAdapter(fTextField,
                contentAdapter, replaceProposer,
                ITextEditorActionDefinitionIds.CONTENT_ASSIST_PROPOSALS,
                new char[] { '$', '\\' }, true);

        new Label(result, SWT.NONE);
        fReplaceWithRegex = new Button(result, SWT.CHECK);
        fReplaceWithRegex
                .setText(SearchMessages.ReplaceConfigurationPage_isRegex_label);
        fReplaceWithRegex.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                setContentAssistsEnablement(fReplaceWithRegex.getSelection());
            }
        });
        if (query.isRegexSearch()) {
            fReplaceWithRegex.setSelection(true);
        } else {
            fReplaceWithRegex.setSelection(false);
            fReplaceWithRegex.setEnabled(false);
        }

        fStatusLabel = new Label(result, SWT.NULL);
        gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.verticalAlignment = SWT.BOTTOM;
        gd.horizontalSpan = 2;
        fStatusLabel.setLayoutData(gd);

        setContentAssistsEnablement(fReplaceWithRegex.getSelection());

        setControl(result);

        Dialog.applyDialogFont(result);

        PlatformUI.getWorkbench().getHelpSystem().setHelp(getControl(),
                ISearchHelpContextIds.REPLACE_DIALOG);
    }

    final void updateOKStatus() {
        RefactoringStatus status = new RefactoringStatus();
        if (fReplaceWithRegex != null && fReplaceWithRegex.getSelection()) {
            try {
                PatternConstructor.interpretReplaceEscapes(fReplaceWithRegex
                        .getText(), fReplaceRefactoring.getQuery()
                        .getSearchString(), "\n"); //$NON-NLS-1$
            } catch (PatternSyntaxException e) {
                String locMessage = e.getLocalizedMessage();
                int i = 0;
                while (i < locMessage.length()
                        && "\n\r".indexOf(locMessage.charAt(i)) == -1) { //$NON-NLS-1$
                    i++;
                }
                status.addError(locMessage.substring(0, i)); // only take first
                // line
            }
        }
        setPageComplete(status);
    }

    private void setContentAssistsEnablement(boolean enable) {
        fTextFieldContentAssist.setEnabled(enable);
    }

    /**
     * @see org.eclipse.ltk.ui.refactoring.UserInputWizardPage#performFinish()
     */
    protected boolean performFinish() {
        initializeRefactoring();
        storeSettings();
        return super.performFinish();
    }

    /**
     * @see org.eclipse.ltk.ui.refactoring.UserInputWizardPage#getNextPage()
     */
    public IWizardPage getNextPage() {
        initializeRefactoring();
        storeSettings();
        return super.getNextPage();
    }

    private void storeSettings() {
        String[] items = fTextField.getItems();
        List<String> history = new ArrayList<String>();
        history.add(fTextField.getText());
        for (String curr : items) {
            if (!history.contains(curr)) {
                history.add(curr);
            }
        }
        IDialogSettings settings = SearchPlugin.getDefault()
                .getDialogSettings().addNewSection(SETTINGS_GROUP);
        settings.put(SETTINGS_REPLACE_WITH, history.toArray(new String[history
                .size()]));
    }

    private void initializeRefactoring() {
        fReplaceRefactoring.setReplaceString(fTextField.getText());
    }

}
