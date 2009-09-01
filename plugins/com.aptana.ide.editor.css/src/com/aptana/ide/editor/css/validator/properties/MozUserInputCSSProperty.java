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
package com.aptana.ide.editor.css.validator.properties;

import org.w3c.css.util.ApplContext;
import org.w3c.css.util.InvalidParamException;
import org.w3c.css.values.CssExpression;

/**
 * -moz-user-input CSS property implementation.
 * @author Denis Denisenko
 */
public class MozUserInputCSSProperty extends CSSFixedSetProperty
{
    /**
     * Property name.
     */
    private static final String PROPERTY_NAME = "-moz-user-input"; //$NON-NLS-1$
    
    /**
     * Fixed values.
     */
    private static final String[] VALUES
        = new String[]{"inherit", "auto", "none", "enabled", "disabled"}; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$

    /**
     * MozUserSelectCSSProperty constructor.
     * 
     * @param context - context.
     * @param expression - expression to create property from.
     * @param check - whether to check property value.
     * 
     * @throws InvalidParamException IF expression is invalid
     */
    public MozUserInputCSSProperty(ApplContext context, CssExpression expression, boolean check)
            throws InvalidParamException {
        super(PROPERTY_NAME, VALUES, context, expression, check);
    }

    /**
     * MozUserSelectCSSProperty constructor.
     * 
     * @param context - context.
     * @param expression - expression to create property from.
     * 
     * @throws InvalidParamException IF expression is invalid
     */
    public MozUserInputCSSProperty(ApplContext context, CssExpression expression)
            throws InvalidParamException {
        super(PROPERTY_NAME, VALUES, context, expression);
    }

    /**
     * MozUserSelectCSSProperty constructor.
     */
    public MozUserInputCSSProperty() {
        super(PROPERTY_NAME, VALUES);
    }
}
