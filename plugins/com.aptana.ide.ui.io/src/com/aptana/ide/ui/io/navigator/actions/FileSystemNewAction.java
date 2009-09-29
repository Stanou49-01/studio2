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
package com.aptana.ide.ui.io.navigator.actions;

import java.util.List;

import org.eclipse.core.filesystem.IFileInfo;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.IMenuCreator;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.actions.BaseSelectionListenerAction;
import org.eclipse.ui.internal.navigator.wizards.WizardShortcutAction;
import org.eclipse.ui.internal.wizards.NewWizardRegistry;
import org.eclipse.ui.wizards.IWizardDescriptor;

import com.aptana.ide.core.StringUtils;
import com.aptana.ide.core.io.IConnectionPoint;
import com.aptana.ide.core.ui.CoreUIPlugin;
import com.aptana.ide.core.ui.WebPerspectiveFactory;
import com.aptana.ide.core.ui.preferences.IPreferenceConstants;
import com.aptana.ide.ui.io.FileSystemUtils;
import com.aptana.ide.ui.io.internal.Utils;

/**
 * @author Michael Xia (mxia@aptana.com)
 */
@SuppressWarnings("restriction")
public class FileSystemNewAction extends BaseSelectionListenerAction {

    private class MenuCreator implements IMenuCreator {

        private MenuManager dropDownMenuMgr;
        private NewFolderAction fNewFolderAction;

        public MenuCreator() {
            fNewFolderAction = new NewFolderAction(fWindow);
        }

        public void dispose() {
            if (dropDownMenuMgr != null) {
                dropDownMenuMgr.dispose();
                dropDownMenuMgr = null;
            }
        }

        public Menu getMenu(Control parent) {
            createDropDownMenuMgr();
            return dropDownMenuMgr.createContextMenu(parent);
        }

        public Menu getMenu(Menu parent) {
            createDropDownMenuMgr();

            Menu menu = new Menu(parent);
            IContributionItem[] items = dropDownMenuMgr.getItems();
            for (IContributionItem item : items) {
                if (item instanceof ActionContributionItem) {
                    item = new ActionContributionItem(((ActionContributionItem) item).getAction());
                }
                item.fill(menu, -1);
            }
            return menu;
        }

        public void selectionChanged(IStructuredSelection selection) {
            fNewFolderAction.selectionChanged(selection);

            if (selection != null && !selection.isEmpty()) {
                Object element = selection.getFirstElement();
                if (element instanceof IAdaptable) {
                    IFileStore fileStore = Utils.getFileStore((IAdaptable) element);
                    IFileInfo fileInfo = getFileInfo((IAdaptable) element);
                    if (fileStore != null && fileInfo != null) {
                        String path = StringUtils.EMPTY;
                        if (fileInfo.isDirectory()) {
                            path = fileStore.toString();
                        } else if (fileStore.getParent() != null) {
                            path = fileStore.getParent().toString();
                        }
                        CoreUIPlugin.getDefault().getPreferenceStore().setValue(
                                IPreferenceConstants.PREF_CURRENT_DIRECTORY, path);
                    }
                }
            }
        }

        private void createDropDownMenuMgr() {
            if (dropDownMenuMgr == null) {
                dropDownMenuMgr = new MenuManager();
                dropDownMenuMgr.add(fNewFolderAction);
                dropDownMenuMgr.add(new Separator());
                // adds actions related to creating new filesystem files
                List<String> wizardIds = WebPerspectiveFactory.getFileWizardShortcuts();
                IWizardDescriptor descriptor;
                for (String id : wizardIds) {
                    descriptor = NewWizardRegistry.getInstance().findWizard(id);
                    if (descriptor != null) {
                        dropDownMenuMgr.add(new WizardShortcutAction(fWindow, descriptor));
                    }
                }
                dropDownMenuMgr.add(new Separator());
                // adds the "Other..." action
                dropDownMenuMgr.add(ActionFactory.NEW.create(fWindow));
            }
        }

        private IFileInfo getFileInfo(IAdaptable adaptable) {
            IFileInfo fileInfo = (IFileInfo) adaptable.getAdapter(IFileInfo.class);
            if (fileInfo == null && !(adaptable instanceof IConnectionPoint)) {
                IFileStore fileStore = Utils.getFileStore(adaptable);
                if (fileStore != null) {
                    fileInfo = FileSystemUtils.fetchFileInfo(fileStore);
                }
            }
            return fileInfo;
        }

    };

    private IWorkbenchWindow fWindow;
    private MenuCreator fMenuCreator;

    public FileSystemNewAction(IWorkbenchWindow window) {
        super(Messages.FileSystemNewAction_Text);
        fWindow = window;
        setMenuCreator(fMenuCreator = new MenuCreator());
    }

    protected boolean updateSelection(IStructuredSelection selection) {
        fMenuCreator.selectionChanged(selection);

        return super.updateSelection(selection);
    }
}
