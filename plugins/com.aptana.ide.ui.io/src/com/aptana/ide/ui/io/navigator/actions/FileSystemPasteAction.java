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
package com.aptana.ide.ui.io.navigator.actions;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.filesystem.IFileInfo;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.FileTransfer;
import org.eclipse.swt.dnd.TransferData;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.BaseSelectionListenerAction;

import com.aptana.ide.ui.io.FileSystemUtils;
import com.aptana.ide.ui.io.IOUIPlugin;
import com.aptana.ide.ui.io.actions.CopyFilesOperation;

/**
 * @author Michael Xia (mxia@aptana.com)
 */
public class FileSystemPasteAction extends BaseSelectionListenerAction {

    /**
     * The id of this action
     */
    public static final String ID = IOUIPlugin.PLUGIN_ID + ".PasteAction"; //$NON-NLS-1$

    /**
     * The shell in which to show any dialogs
     */
    private Shell fShell;

    /**
     * System clipboard
     */
    private Clipboard fClipboard;

    private IFileStore[] fClipboardData;
    private List<IFileStore> fDestFileStores;

    public FileSystemPasteAction(Shell shell, Clipboard clipboard) {
        super(Messages.FileSystemPasteAction_TXT);
        fShell = shell;
        fClipboard = clipboard;
        fDestFileStores = new ArrayList<IFileStore>();

        setToolTipText(Messages.FileSystemPasteAction_TTP);
        setId(ID);
        PlatformUI.getWorkbench().getHelpSystem().setHelp(this, "HelpId"); //$NON-NLS-1$
    }

    @Override
    public void run() {
        // try file transfer
        JobChangeAdapter jobAdapter = new JobChangeAdapter() {

            @Override
            public void done(IJobChangeEvent event) {
                IOUIPlugin.refreshNavigatorView(getStructuredSelection().getFirstElement());
            }
        };
        if (fClipboardData != null && fClipboardData.length > 0 && fDestFileStores.size() > 0) {
            CopyFilesOperation operation = new CopyFilesOperation(fShell);
            operation.copyFiles(fClipboardData, fDestFileStores.get(0), jobAdapter);
            return;
        }

        // try other transfer
        FileTransfer fileTransfer = FileTransfer.getInstance();
        String[] fileData = (String[]) fClipboard.getContents(fileTransfer);

        if (fileData != null && fileData.length > 0 && fDestFileStores.size() > 0) {
            CopyFilesOperation operation = new CopyFilesOperation(fShell);
            operation.copyFiles(fileData, fDestFileStores.get(0), jobAdapter);
        }
    }

    @Override
    protected boolean updateSelection(IStructuredSelection selection) {
        fDestFileStores.clear();
        fClipboardData = new IFileStore[0];
        if (!super.updateSelection(selection)) {
            return false;
        }

        if (selection == null || selection.isEmpty()) {
            return false;
        }
        Object[] elements = selection.toArray();

        IFileStore fileStore;
        for (Object element : elements) {
            fileStore = FileSystemUtils.getFileStore(element);
            if (fileStore != null) {
                fDestFileStores.add(fileStore);
            }
        }
        if (fDestFileStores.size() == 0) {
            return false;
        }

        fShell.getDisplay().syncExec(new Runnable() {

            public void run() {
                // clipboard must have files
                LocalSelectionTransfer transfer = LocalSelectionTransfer.getTransfer();
                Object contents = fClipboard.getContents(transfer);
                if (contents instanceof StructuredSelection) {
                    Object[] elements = ((StructuredSelection) contents).toArray();
                    List<IFileStore> fileStores = new ArrayList<IFileStore>();
                    for (Object element : elements) {
                        if (element instanceof IFileStore) {
                            fileStores.add((IFileStore) element);
                        }
                    }
                    fClipboardData = fileStores.toArray(new IFileStore[fileStores.size()]);
                }
            }
        });
        if (fClipboardData.length > 0) {
            return true;
        }

        TransferData[] transfers = fClipboard.getAvailableTypes();
        FileTransfer fileTransfer = FileTransfer.getInstance();
        for (int i = 0; i < transfers.length; ++i) {
            if (fileTransfer.isSupportedType(transfers[i])) {
                return true;
            }
        }
        return false;
    }
}
