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
package com.aptana.ide.editor.js;

import org.eclipse.swt.graphics.RGB;

/**
 * @author Kevin Lindsey
 */
public interface IJSColorConstants
{
	/**
	 * PROPOSALS_FOREGROUND
	 */
	RGB PROPOSALS_FOREGROUND = new RGB(0, 0, 0);
	
	/**
	 * PROPOSALS_BACKGROUND
	 */
	RGB PROPOSALS_BACKGROUND = new RGB(255, 255, 255);
	
	/**
	 * DEFAULT
	 */
	RGB DEFAULT   	= new RGB(00, 00, 00);
	
	/**
	 * KEYWORD
	 */
	RGB KEYWORD   	= new RGB(00, 00, 192);
	
	/**
	 * NATIVETYPE
	 */
	RGB NATIVETYPE	= new RGB(64, 00, 128);
	
	/**
	 * PUNCTUATOR
	 */
	RGB PUNCTUATOR	= new RGB(92, 92, 92);
	
	/**
	 * RESERVED
	 */
	RGB RESERVED	= new RGB(255, 00, 00);
	
	/**
	 * FUTURE
	 */
	RGB FUTURE		= new RGB(255, 00, 00);
	
	/**
	 * STRING
	 */
	RGB STRING		= new RGB(00, 92, 00);
	
	/**
	 * NUMBER
	 */
	RGB NUMBER		= new RGB(00, 64, 128);
	
	/**
	 * LITERAL
	 */
	RGB LITERAL		= new RGB(128, 00, 64);
	
	/**
	 * COMMENT
	 */
	RGB COMMENT   	= new RGB(00, 128, 00);
	
	/**
	 * DOCUMENTATION
	 */
	RGB DOCUMENTATION = new RGB(00, 00, 255);

	/**
	 * ERROR
	 */
	RGB ERROR = new RGB(255, 00, 00);
	
	/**
	 * HTMLDOM
	 */
	RGB HTMLDOM = new RGB(00, 64, 128);
	
	/**
	 * JSCORE
	 */
	RGB JSCORE = new RGB(64, 64, 128);
}
