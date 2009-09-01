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
package com.aptana.ide.editor.js.contentassist;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.templates.Template;
import org.eclipse.jface.text.templates.TemplateBuffer;
import org.eclipse.jface.text.templates.TemplateContext;
import org.eclipse.jface.text.templates.TemplateContextType;
import org.eclipse.jface.text.templates.TemplateException;
import org.eclipse.jface.text.templates.TemplateVariable;

/**
 * 
 */
public class UnifiedTemplateContext extends TemplateContext
{

	/**
	 * @param contextType
	 */
	protected UnifiedTemplateContext(TemplateContextType contextType)
	{
		super(contextType);
	}

	/**
	 * @see org.eclipse.jface.text.templates.TemplateContext#evaluate(org.eclipse.jface.text.templates.Template)
	 */
	public TemplateBuffer evaluate(Template template) throws BadLocationException, TemplateException
	{
		TemplateVariable[] vars = new TemplateVariable[] 
        {
				new TemplateVariable("int1", new String[] {"aa", "aa"}, new int[]{9, 29}), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				new TemplateVariable("int2", new String[] {"bb", "bb"}, new int[]{13, 43}) //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		};
		TemplateBuffer tb = new TemplateBuffer("function(aa, bb)\n{\n\tvar xx = aa;\n\tvar yy = bb;\n}", vars) ; //$NON-NLS-1$
		return tb; 
	}

	/**
	 * @see org.eclipse.jface.text.templates.TemplateContext#canEvaluate(org.eclipse.jface.text.templates.Template)
	 */
	public boolean canEvaluate(Template template)
	{
		return true;
	}

}
