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
package com.aptana.ide.syncing.ui.actions;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.ui.IDecoratorManager;

import com.aptana.ide.core.StringUtils;
import com.aptana.ide.syncing.ui.SyncingUIPlugin;
import com.aptana.ide.syncing.ui.preferences.IPreferenceConstants;

/**
 * @author Michael Xia (mxia@aptana.com)
 */
public class CloakingUtils {

    /**
     * Adds a file type to be cloaked.
     * 
     * @param filetype
     *            the file type
     */
    public static void addCloakFileType(String filetype) {
        List<String> newList = new ArrayList<String>();
        String[] filetypes = getCloakedFileTypes();
        boolean found = false;
        for (String extension : filetypes) {
            if (extension.equals(filetype)) {
                found = true;
            }
            newList.add(extension);
        }
        if (!found) {
            newList.add(filetype);
        }

        setCloakedFileTypes(newList.toArray(new String[newList.size()]));
    }

    /**
     * Removes a file type from being cloaked.
     * 
     * @param filetype
     *            the file type
     */
    public static void removeCloakFileType(String filetype) {
        List<String> newList = new ArrayList<String>();
        String[] filetypes = getCloakedFileTypes();
        for (String extension : filetypes) {
            if (extension.equals(filetype)) {
                continue;
            }
            newList.add(extension);
        }

        setCloakedFileTypes(newList.toArray(new String[newList.size()]));
    }

    /**
     * @param filename
     *            the filename to be checked
     * @return true if the file should be cloaked, false otherwise
     */
    public static boolean isFileCloaked(String filename) {
        String[] expressions = getCloakedExpressions();
        for (String expression : expressions) {
            if (filename.matches(expression)) {
                return true;
            }
        }
        return false;
    }

    /**
     * @return the array of filetypes being cloaked in regular expression
     */
    public static String[] getCloakedExpressions() {
        String[] filetypes = getCloakedFileTypes();
        String[] expressions = new String[filetypes.length];
        for (int i = 0; i < expressions.length; ++i) {
            expressions[i] = convertCloakExpressionToRegex(filetypes[i]);
        }
        return expressions;
    }

    /**
     * Refreshes the cloaking decorator.
     */
    public static void updateDecorator() {
        IDecoratorManager dm = SyncingUIPlugin.getDefault().getWorkbench().getDecoratorManager();
        dm.update("com.aptana.ide.syncing.ui.decorators.CloakedFileDecorator"); //$NON-NLS-1$
    }

    private static String convertCloakExpressionToRegex(String expression) {
        if (expression == null) {
            return null;
        }
        String result = null;

        if (expression.startsWith("/") && expression.endsWith("/")) { //$NON-NLS-1$//$NON-NLS-2$
            // already an regular expression
            return expression.substring(1, expression.length() - 1);
        }

        if (expression.contains("\\")) { //$NON-NLS-1$
            expression = expression.replaceAll("\\\\", "/"); //$NON-NLS-1$ //$NON-NLS-2$
        }

        // escape all '.' characters which aren't followed by '*'
        result = expression.replaceAll("\\.(?=[^\\*])", "\\\\."); //$NON-NLS-1$//$NON-NLS-2$

        // convert all '*' characters that are not preceded by '.' to ".*"
        result = "(?i)" + result.replaceAll("(?<!\\.)\\*", ".*"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

        return result;
    }

    private static String[] getCloakedFileTypes() {
        IPreferenceStore store = getPreferenceStore();
        String extensions = store.getString(IPreferenceConstants.GLOBAL_CLOAKING_EXTENSIONS);
        if (extensions.equals("")) { //$NON-NLS-1$
            return new String[0];
        }
        return extensions.split(";"); //$NON-NLS-1$
    }

    private static void setCloakedFileTypes(String[] filetypes) {
        IPreferenceStore store = getPreferenceStore();
        store.setValue(IPreferenceConstants.GLOBAL_CLOAKING_EXTENSIONS, StringUtils.join(";", //$NON-NLS-1$
                filetypes));
    }

    private static IPreferenceStore getPreferenceStore() {
        return SyncingUIPlugin.getDefault().getPreferenceStore();
    }
}
