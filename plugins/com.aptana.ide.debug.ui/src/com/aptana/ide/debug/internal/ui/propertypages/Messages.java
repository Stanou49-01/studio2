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
package com.aptana.ide.debug.internal.ui.propertypages;

import org.eclipse.osgi.util.NLS;

/**
 * 
 * @author Ingo Muschenetz
 *
 */
public final class Messages extends NLS {
	private static final String BUNDLE_NAME = "com.aptana.ide.debug.internal.ui.propertypages.messages"; //$NON-NLS-1$

	private Messages() {
	}

	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	/**
	 * JSLineBreakpointPropertyPage_HitCountMustBePositiveInteger
	 */
	public static String JSLineBreakpointPropertyPage_HitCountMustBePositiveInteger;

	/**
	 * JSLineBreakpointPropertyPage_EnterCondition
	 */
	public static String JSLineBreakpointPropertyPage_EnterCondition;

	/**
	 * JSLineBreakpointPropertyPage_File
	 */
	public static String JSLineBreakpointPropertyPage_File;

	/**
	 * JSLineBreakpointPropertyPage_Enabled
	 */
	public static String JSLineBreakpointPropertyPage_Enabled;

	/**
	 * JSLineBreakpointPropertyPage_HitCount
	 */
	public static String JSLineBreakpointPropertyPage_HitCount;

	/**
	 * JSLineBreakpointPropertyPage_LineNumber
	 */
	public static String JSLineBreakpointPropertyPage_LineNumber;

	/**
	 * JSLineBreakpointPropertyPage_EnableCondition
	 */
	public static String JSLineBreakpointPropertyPage_EnableCondition;

	/**
	 * JSLineBreakpointPropertyPage_conditionIsTrue
	 */
	public static String JSLineBreakpointPropertyPage_conditionIsTrue;

	/**
	 * JSLineBreakpointPropertyPage_valueOfConditionChanges
	 */
	public static String JSLineBreakpointPropertyPage_valueOfConditionChanges;

	/**
	 * JSLineBreakpointPropertyPage_ExceptionWhileSavingBreakpointProperties
	 */
	public static String JSLineBreakpointPropertyPage_ExceptionWhileSavingBreakpointProperties;

	/**
	 * JSLineBreakpointPropertyPage_PageAllowedInputOfInvalidStringForHitCountValue_0
	 */
	public static String JSLineBreakpointPropertyPage_PageAllowedInputOfInvalidStringForHitCountValue_0;
}
