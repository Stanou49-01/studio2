/** 
 * This file Copyright (c) 2005-2007 Aptana, Inc. This program is
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
 * with certain Eclipse Public Licensed code and certain additional terms
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
package com.aptana.ide.views.outline;

import java.util.Map;

import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;

import com.aptana.ide.editors.unified.ContributedOutline;
import com.aptana.ide.editors.unified.UnifiedEditor;

/**
 * Unified outline page.
 * @author Denis Denisenko
 */
interface IUnifiedOutlinePage extends IContentOutlinePage
{
    /**
     * Does the refresh.
     */
    void refresh();
    
    /**
     * Returns this page's tree viewer.
     *
     * @return this page's tree viewer, or <code>null</code> if 
     *   <code>createControl</code> has not been called yet
     */
    TreeViewer getTreeViewer();
    
    /**
     * Gets the editor.
     */
    UnifiedEditor getUnifiedEditor();
    
    /**
     * Gets outline sash.
     * 
     * @return outline sash
     */
    SashForm getOutlineSash();
    
    /**
     * Gets outline tabs
     * 
     * @return tab folder
     */
    CTabFolder getOutlineTabs();
    
    /**
     * Gets contributed outlines.
     * 
     * @return contributed outlines
     */
    Map<String, ContributedOutline> getContributedOutlines();
    
    /**
     * Toggles private members visibility.
     */
    void togglePrivateMemberVisibility();
    
    /**
     * Gets whether private members are hidden.
     * 
     * @return true if hidden false otherwise
     */
    boolean hidePrivateMembers();
}
