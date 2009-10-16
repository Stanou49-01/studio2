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
package com.aptana.ide.ui.io.navigator;

import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.navigator.CommonNavigator;
import org.eclipse.ui.navigator.CommonViewer;

import com.aptana.ide.core.ui.PreferenceUtils;

public class FileNavigatorView extends CommonNavigator {

    public static final String ID = "com.aptana.ide.ui.io.fileExplorerView"; //$NON-NLS-1$

    @Override
    public void createPartControl(Composite aParent) {
        super.createPartControl(aParent);

        getCommonViewer().setLabelProvider(
                new FileNavigatorDecoratingLabelProvider(getNavigatorContentService()
                        .createCommonLabelProvider()));
        getCommonViewer().setComparer(new FileSystemElementComparer());
        ColumnViewerToolTipSupport.enableFor(getCommonViewer());

        PreferenceUtils.registerBackgroundColorPreference(getCommonViewer().getControl(),
                "com.aptana.ide.ui.io.background.color.fileView"); //$NON-NLS-1$
        PreferenceUtils.registerForegroundColorPreference(getCommonViewer().getControl(),
                "com.aptana.ide.ui.io.foreground.color.fileView"); //$NON-NLS-1$
    }

    public void setSelection(Object[] selectionPath) {
        setSelection(selectionPath, false);
    }

    public void setSelection(Object[] selectionPath, boolean deferred) {
        if (selectionPath == null || selectionPath.length == 0) {
            return;
        }

        CommonViewer treeViewer = getCommonViewer();
        if (deferred) {
            // TODO: makes the use of DeferredTreeSelectionExpander
            DeferredTreeSelectionExpander selectionExpander = (DeferredTreeSelectionExpander) treeViewer
                    .getData(FileTreeContentProvider.SELECTION_EXPANDER_KEY);
            if (selectionExpander != null) {
                selectionExpander.setSelection(new TreePath(selectionPath));
            }
        } else {
            treeViewer.expandToLevel(new TreePath(selectionPath), 1);
            treeViewer.setSelection(
                    new StructuredSelection(selectionPath[selectionPath.length - 1]), true);
        }
    }
}
