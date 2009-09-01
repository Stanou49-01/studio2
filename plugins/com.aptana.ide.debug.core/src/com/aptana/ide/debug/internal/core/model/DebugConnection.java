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

package com.aptana.ide.debug.internal.core.model;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.net.Socket;
import java.net.SocketException;
import java.util.Hashtable;
import java.util.Map;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugException;

import com.aptana.ide.core.IdeLog;
import com.aptana.ide.core.StringUtils;
import com.aptana.ide.debug.core.JSDebugPlugin;

/**
 * @author Max Stepanov
 *
 */
public class DebugConnection
{
	public interface IHandler {
		void handleMessage(String message);
		void handleShutdown();
	}
	
	/**
	 * COMMAND_TIMEOUT
	 */
	protected static final int COMMAND_TIMEOUT = 20000;

	/**
	 * SOCKET_TIMEOUT
	 */
	public static final int SOCKET_TIMEOUT = 30000;

	private static final String ARGS_SPLIT = "\\*"; //$NON-NLS-1$

	private Socket socket;
	private Reader reader;
	private Writer writer;
	private boolean connected = false;
	private boolean terminated = false;

	private Map<String, Object> locks = new Hashtable<String, Object>();/* synchronized get/put required */
	private volatile long lastReqId = System.currentTimeMillis();
	
	private IHandler handler;

	/**
	 * @throws DebugException 
	 * @throws DebugException 
	 * @throws IOException 
	 * 
	 */
	public static DebugConnection createConnection(Socket socket) throws DebugException
	{	
		try
		{
			return new DebugConnection(socket,
					new InputStreamReader(socket.getInputStream()),
					new OutputStreamWriter(socket.getOutputStream()));
		}
		catch (IOException e)
		{
			throwDebugException(e);
			return null;
		}
	}
	
	protected DebugConnection(Socket socket, Reader reader, Writer writer) {
		this.socket = socket;
		this.reader = reader;
		this.writer = writer;
	}
	
	public void start(IHandler handler) {
		this.handler = handler;
		
		connected = true;
		new Thread("Aptana: JS Debugger") { //$NON-NLS-1$
			public void run()
			{
				while ((socket != null && !socket.isClosed()) || reader != null)
				{
					try
					{
						String message = readMessage();
						if (message == null)
						{
							break;
						}
						handleMessage(message);
					}
					catch (SocketException e)
					{
						break;
					}
					catch (Exception e)
					{
						IdeLog.logError(JSDebugPlugin.getDefault(), StringUtils.EMPTY, e);
					}
				}
				handleConnectionTerminated();
			}

		}.start();
	}
	
	public void stop() {
		if (!connected)
		{
			return;
		}
		connected = false;
		synchronized (locks)
		{
			Object[] list = locks.values().toArray();
			locks.clear();
			for (int i = 0; i < list.length; ++i)
			{
				Object lock = list[i];
				synchronized (lock)
				{
					lock.notify();
				}
			}
		}
		
	}
	
	public void dispose() throws IOException {
		if (reader != null) {
			reader.close();
			writer.close();
			reader = null;
			writer = null;
		}
		if (socket != null)
		{
			socket.close();
			socket = null;
		}		
	}
	
	private void handleMessage(String message) {
		if (message.endsWith("*")) { //$NON-NLS-1$
			message += "* "; //$NON-NLS-1$
		}
		handler.handleMessage(message);
		
		if (!connected)
		{
			return;
		}
		/* check if action comes to waiting commands */
		String[] args = message.split(ARGS_SPLIT);
		String action = args[0];
		Object lock = locks.get(action);
		if (lock != null)
		{
			locks.put(action, args);
			synchronized (lock)
			{
				lock.notify();
			}
			return;
		}
	}

	/**
	 * handleConnectionTerminated
	 */
	private void handleConnectionTerminated()
	{
		if (terminated)
		{
			return;
		}
		terminated = true;
		handler.handleShutdown();
	}

	public boolean isConnected() {
		return connected;
	}
	
	public boolean isTerminated() {
		return terminated;
	}
	
	/**
	 * sendCommand
	 * 
	 * @param command
	 * @throws DebugException
	 */
	protected void sendCommand(String command) throws DebugException
	{
		sendCommand(StringUtils.EMPTY, command);
	}

	/**
	 * sendCommand
	 * 
	 * @param reqid
	 * @param command
	 * @throws DebugException
	 */
	protected void sendCommand(String reqid, String command) throws DebugException
	{
		try
		{
			writer.write(StringUtils.format("{0}*{1}*{2}", //$NON-NLS-1$
					new String[] { Integer.toString(command.length() + reqid.length() + 1), reqid, command }));
			writer.flush();
		}
		catch (IOException e)
		{
			throwDebugException(e);
		}
	}

	/**
	 * sendCommandAndWait
	 * 
	 * @param command
	 * @return String[]
	 * @throws DebugException
	 */
	protected String[] sendCommandAndWait(String command) throws DebugException
	{
		long reqid = ++lastReqId;
		return sendCommandAndWait(command, Long.toString(reqid));
	}

	/**
	 * sendCommandAndWait
	 * 
	 * @param command
	 * @param reqid
	 * @return String[]
	 * @throws DebugException
	 */
	protected String[] sendCommandAndWait(String command, String reqid) throws DebugException
	{
		if (!connected)
		{
			return null;
		}
		Object lock = new Object();
		synchronized (lock)
		{
			try
			{
				locks.put(reqid, lock);
				sendCommand(reqid, command);
				lock.wait(COMMAND_TIMEOUT);
			}
			catch (InterruptedException e)
			{
				throwDebugException(e);
			}
		}
		lock = locks.remove(reqid);
		if (lock instanceof String[])
		{
			return (String[]) lock;
		}
		return null;
	}

	/**
	 * readMessage
	 * 
	 * @return String
	 * @throws IOException
	 */
	protected String readMessage() throws IOException
	{
		StringBuffer sb = new StringBuffer();
		int messageSize = 0;
		int i;
		char ch;
		while ((i = reader.read()) != -1)
		{
			ch = (char) i;
			if (ch == '*' && sb.length() > 0)
			{
				try
				{
					messageSize = Integer.parseInt(sb.toString());
					break;
				}
				catch (NumberFormatException e)
				{
				}
				sb.setLength(0);
			}
			else if (ch >= '0' && ch <= '9')
			{
				sb.append(ch);
			}
			else if (sb.length() > 0)
			{
				sb.setLength(0);
			}
		}
		if (i == -1)
		{
			return null;
		}

		char[] buffer = new char[1024];
		sb.setLength(0); // clear the buffer
		while (messageSize > sb.length())
		{
			int n = reader.read(buffer, 0, Math.min(messageSize - sb.length(), buffer.length));
			if (n == -1)
			{
				return null;
			}
			sb.append(buffer, 0, n);
		}
		return sb.toString();
	}

	/**
	 * throwDebugException
	 * 
	 * @param exception
	 * @throws DebugException
	 */
	protected static void throwDebugException(Exception exception) throws DebugException
	{
		throw new DebugException(new Status(IStatus.ERROR, JSDebugPlugin.ID, DebugException.TARGET_REQUEST_FAILED,
				StringUtils.EMPTY, exception));
	}

}
