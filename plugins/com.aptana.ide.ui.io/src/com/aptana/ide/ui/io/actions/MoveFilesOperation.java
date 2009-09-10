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
package com.aptana.ide.ui.io.actions;

import java.text.MessageFormat;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.dialogs.IOverwriteQuery;

/**
 * @author Michael Xia (mxia@aptana.com)
 */
public class MoveFilesOperation extends CopyFilesOperation {

    public MoveFilesOperation(Shell shell) {
        super(shell);
    }

    @Override
    protected boolean copyFile(IFileStore sourceStore, IFileStore destinationStore,
            IProgressMonitor monitor) {
        if (sourceStore == null) {
            return false;
        }

        boolean success = true;
        monitor.subTask(MessageFormat.format("Moving {0} to {1}", sourceStore.getName(),
                destinationStore.getName()));
        try {
            if (getAlwaysOverwrite()) {
                sourceStore.move(destinationStore, EFS.OVERWRITE, monitor);
            } else if (destinationStore.fetchInfo(0, monitor).exists()) {
                String overwrite = getOverwriteQuery().queryOverwrite(destinationStore.toString());
                if (overwrite.equals(IOverwriteQuery.ALL) || overwrite.equals(IOverwriteQuery.YES)) {
                    sourceStore.move(destinationStore, EFS.OVERWRITE, monitor);
                } else {
                    success = false;
                }
            } else {
                sourceStore.move(destinationStore, EFS.NONE, monitor);
            }
        } catch (CoreException e) {
            // TODO: report the error
            success = false;
        }
        return success;
    }
}
