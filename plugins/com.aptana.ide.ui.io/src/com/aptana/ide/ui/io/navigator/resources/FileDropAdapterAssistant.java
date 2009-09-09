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
package com.aptana.ide.ui.io.navigator.resources;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.filesystem.IFileInfo;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.FileTransfer;
import org.eclipse.swt.dnd.TransferData;
import org.eclipse.ui.navigator.CommonDropAdapter;
import org.eclipse.ui.navigator.CommonDropAdapterAssistant;

import com.aptana.ide.core.io.LocalRoot;
import com.aptana.ide.ui.io.IOUIPlugin;
import com.aptana.ide.ui.io.actions.CopyFilesOperation;
import com.aptana.ide.ui.io.actions.MoveFilesOperation;
import com.aptana.ide.ui.io.internal.Utils;

/**
 * @author Michael Xia (mxia@aptana.com)
 */
public class FileDropAdapterAssistant extends CommonDropAdapterAssistant {

    public FileDropAdapterAssistant() {
    }

    public boolean isSupportedType(TransferData aTransferType) {
        return super.isSupportedType(aTransferType)
                || FileTransfer.getInstance().isSupportedType(aTransferType);
    }

    @Override
    public IStatus handleDrop(CommonDropAdapter aDropAdapter, DropTargetEvent aDropTargetEvent,
            Object aTarget) {
        if (aDropAdapter.getCurrentTarget() == null || aDropTargetEvent.data == null) {
            return Status.CANCEL_STATUS;
        }

        IStatus status = null;
        IAdaptable[] sources = null;
        TransferData currentTransfer = aDropAdapter.getCurrentTransfer();
        if (LocalSelectionTransfer.getTransfer().isSupportedType(currentTransfer)) {
            sources = getSelectedSourceFiles();
            aDropTargetEvent.detail = DND.DROP_NONE;
        }

        if (FileTransfer.getInstance().isSupportedType(currentTransfer)) {
            status = performDrop(aDropAdapter, aDropTargetEvent.data);
        } else if (sources != null && sources.length > 0) {
            if (aDropAdapter.getCurrentOperation() == DND.DROP_COPY) {
                status = performCopy(aDropAdapter, sources);
            } else {
                status = performMove(aDropAdapter, sources);
            }
        }
        openError(status);

        // TODO: improves to refresh only the folders that contain the change
        IOUIPlugin.refreshNavigatorView(null);

        return status;
    }

    @Override
    public IStatus validateDrop(Object target, int operation, TransferData transferType) {
        if (!(target instanceof IAdaptable)) {
            return createStatus("Target must be an IAdaptable");
        }

        IAdaptable destination = (IAdaptable) target;
        IFileStore fileStore = Utils.getFileStore(destination);
        if (fileStore == null) {
            return createStatus("Target must be able to adapt to a IFileStore");
        }

        if (LocalSelectionTransfer.getTransfer().isSupportedType(transferType)) {
            IAdaptable[] files = getSelectedSourceFiles();
            if (files.length == 0) {
                return createStatus("There are no valid selections to drop");
            }
            for (Object file : files) {
                if (file instanceof LocalRoot) {
                    return createStatus("Cannot drop a LocalRoot object into another directory");
                }
            }

            String message = CopyFilesOperation.validateDestination(destination, files);
            if (message != null) {
                return createStatus(message);
            }
        } else if (FileTransfer.getInstance().isSupportedType(transferType)) {
            String[] sourceNames = (String[]) FileTransfer.getInstance().nativeToJava(transferType);
            if (sourceNames == null) {
                sourceNames = new String[0];
            }

            String message = CopyFilesOperation.validateDestination(destination, sourceNames);
            if (message != null) {
                return createStatus(message);
            }
        }

        return Status.OK_STATUS;
    }

    private IStatus performCopy(CommonDropAdapter dropAdapter, IAdaptable[] sources) {
        MultiStatus problems = new MultiStatus(IOUIPlugin.PLUGIN_ID, 1,
                "Problems occurred while copying files.", null);
        IStatus validate = validateDrop(dropAdapter.getCurrentTarget(), dropAdapter
                .getCurrentOperation(), dropAdapter.getCurrentTransfer());
        if (!validate.isOK()) {
            problems.merge(validate);
        }

        IFileStore destination = getFolderStore((IAdaptable) dropAdapter.getCurrentTarget());
        CopyFilesOperation operation = new CopyFilesOperation(getShell());
        operation.copyFiles(sources, destination, null);

        return problems;
    }

    private IStatus performDrop(CommonDropAdapter dropAdapter, Object data) {
        MultiStatus problems = new MultiStatus(IOUIPlugin.PLUGIN_ID, 0,
                "Problems occurred while importing files.", null);
        IStatus validate = validateDrop(dropAdapter.getCurrentTarget(), dropAdapter
                .getCurrentOperation(), dropAdapter.getCurrentTransfer());
        if (!validate.isOK()) {
            problems.merge(validate);
        }

        IFileStore destination = getFolderStore((IAdaptable) dropAdapter.getCurrentTarget());
        CopyFilesOperation operation = new CopyFilesOperation(getShell());
        operation.copyFiles((String[]) data, destination, null);

        return problems;
    }

    private IStatus performMove(CommonDropAdapter dropAdapter, IAdaptable[] sources) {
        MultiStatus problems = new MultiStatus(IOUIPlugin.PLUGIN_ID, 1,
                "Problems occurred while moving files.", null);
        IStatus validate = validateDrop(dropAdapter.getCurrentTarget(), dropAdapter
                .getCurrentOperation(), dropAdapter.getCurrentTransfer());
        if (!validate.isOK()) {
            problems.merge(validate);
        }

        IFileStore destination = getFolderStore((IAdaptable) dropAdapter.getCurrentTarget());
        MoveFilesOperation operation = new MoveFilesOperation(getShell());
        operation.copyFiles(sources, destination, null);

        return problems;
    }

    private void openError(IStatus status) {
        if (status == null) {
            return;
        }

        String title = "Drag and Drop Error";
        int codes = IStatus.ERROR | IStatus.WARNING;

        if (status.isMultiStatus()) {
            IStatus[] children = status.getChildren();
            if (children.length == 1) {
                ErrorDialog.openError(getShell(), status.getMessage(), null, children[0], codes);
            } else {
                ErrorDialog.openError(getShell(), title, null, status, codes);
            }
        } else {
            ErrorDialog.openError(getShell(), title, null, status, codes);
        }
    }

    private static IAdaptable[] getSelectedSourceFiles() {
        ISelection selection = LocalSelectionTransfer.getTransfer().getSelection();
        if (selection instanceof IStructuredSelection) {
            return getSelectedSourceFiles((IStructuredSelection) selection);
        }
        return new IAdaptable[0];
    }

    @SuppressWarnings("unchecked")
    private static IAdaptable[] getSelectedSourceFiles(IStructuredSelection selection) {
        List<IAdaptable> selectedFiles = new ArrayList<IAdaptable>();

        Iterator iter = selection.iterator();
        Object object;
        IFileStore fileStore;
        while (iter.hasNext()) {
            object = iter.next();
            if (object instanceof IAdaptable) {
                IAdaptable adaptable = (IAdaptable) object;
                fileStore = Utils.getFileStore(adaptable);
                if (fileStore != null) {
                    // valid selection
                    selectedFiles.add(adaptable);
                }
            }
        }

        return selectedFiles.toArray(new IAdaptable[selectedFiles.size()]);
    }

    private static IFileStore getFolderStore(IAdaptable destination) {
        IFileStore store = Utils.getFileStore(destination);
        IFileInfo info = Utils.getFileInfo(destination);
        if (store != null && info != null && !info.isDirectory()) {
            store = store.getParent();
        }
        return store;
    }

    private static Status createStatus(String message) {
        return new Status(IStatus.INFO, IOUIPlugin.PLUGIN_ID, 0, message, null);
    }
}
