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
package com.aptana.ide.core.ui.syncing;

import java.io.IOException;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.MessageConsole;
import org.eclipse.ui.console.MessageConsoleStream;

import com.aptana.ide.core.IdeLog;
import com.aptana.ide.core.ui.CoreUIPlugin;

/**
 * @author Paul Colton
 */
public class SyncingConsole extends MessageConsole
{
	private static SyncingConsole _console;
	private MessageConsoleStream _consoleStream;

	/*
	 * Constructors
	 */

	/**
	 * ScriptingConsole
	 * 
	 * @param name
	 * @param imageDescriptor
	 */
	public SyncingConsole(String name, ImageDescriptor imageDescriptor)
	{
		super(name, imageDescriptor);
		_consoleStream = this.newMessageStream();
	}

	/**
	 * getMessageStream
	 * 
	 * @return MessageConsoleStream
	 */
	public MessageConsoleStream getMessageStream()
	{
		return _consoleStream;
	}

	/**
	 * getConsole
	 * 
	 * @return SyncingConsole
	 */
	public static SyncingConsole getConsole()
	{
		if (_console == null)
		{
			initConsole();
		}

		return _console;
	}

	/**
	 * initConsole
	 */
	private static void initConsole()
	{
		_console = new SyncingConsole(Messages.SyncingConsole_AptanaSyncingConsole, null);
		ConsolePlugin.getDefault().getConsoleManager().addConsoles(new IConsole[] { _console });
	}

	/**
	 * @param message
	 */
	public static void println(String message)
	{
		try
		{
			SyncingConsole console = getConsole();
			if (console != null)
			{
				MessageConsoleStream ms = console.getMessageStream();
				ms.write(message);
			}
		}
		catch (Exception e)
		{
			// IdeLog.logError(CoreUIPlugin.getDefault(),
			// StringUtils.format(Messages.SyncingConsole_UnableToWriteToConsole, message), e);
		}
	}

	/**
	 * @see org.eclipse.ui.console.AbstractConsole#dispose()
	 */
	protected void dispose()
	{
		super.dispose();

		try
		{
			_consoleStream.close();
		}
		catch (IOException e)
		{
			IdeLog.logError(CoreUIPlugin.getDefault(), Messages.SyncingConsole_ErrorClosingStream, e);
		}
	}
}
