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
package com.aptana.ide.syncing.ui.views;

import java.net.UnknownHostException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.ICellModifier;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jface.window.ToolTip;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Item;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.progress.UIJob;

import com.aptana.ide.core.ILogger;
import com.aptana.ide.core.IdeLog;
import com.aptana.ide.core.io.IConnectionPoint;
import com.aptana.ide.core.io.efs.EFSUtils;
import com.aptana.ide.core.io.syncing.ConnectionPointSyncPair;
import com.aptana.ide.core.io.syncing.SyncState;
import com.aptana.ide.core.io.syncing.VirtualFileSyncPair;
import com.aptana.ide.core.resources.IProjectProvider;
import com.aptana.ide.core.ui.CoreUIPlugin;
import com.aptana.ide.core.ui.CoreUIUtils;
import com.aptana.ide.core.ui.SWTUtils;
import com.aptana.ide.core.ui.preferences.IPreferenceConstants;
import com.aptana.ide.core.ui.syncing.SyncingConsole;
import com.aptana.ide.syncing.core.Synchronizer;
import com.aptana.ide.syncing.core.events.ISyncEventHandler;
import com.aptana.ide.syncing.ui.SyncingUIPlugin;
import com.aptana.ide.syncing.ui.handlers.SyncEventHandlerAdapterWithProgressMonitor;
import com.aptana.ide.syncing.ui.internal.SyncUtils;
import com.aptana.ide.ui.io.preferences.PermissionsGroup;

/**
 * @author Kevin Sawicki (ksawicki@aptana.com)
 * @author Michael Xia (mxia@aptana.com)
 */
public class SmartSyncDialog extends Window implements SelectionListener, ModifyListener, DirectionToolBar.Client,
		OptionsToolBar.Client, SyncJob.Client
{

	private static final String ICON = "icons/full/obj16/direction_both.gif"; //$NON-NLS-1$
	private static final String CLOSE_WHEN_DONE = "com.aptana.ide.syncing.views.CLOSE_WHEN_DONE"; //$NON-NLS-1$
	private static final String COMPARE_IN_BACKGROUND = IPreferenceConstants.COMPARE_IN_BACKGROUND;
	private static final String USE_CRC = IPreferenceConstants.USE_CRC;

	private static final String SKIPPED_LABEL = Messages.SmartSyncDialog_NumFilesToSkip;
	private static final String UPDATED_LABEL = Messages.SmartSyncDialog_NumFilesToUpdate;
	private static final String DELETED_LABEL = Messages.SmartSyncDialog_NumFilesToDelete;
	private static final String SYNC_LABEL = Messages.SmartSyncDialog_Comparing;

	private Composite displayArea;
	private Label updatedLabel;
	private Label skippedLabel;
	private Label deletedLabel;
	private Font boldFont;
	private SmartSyncViewer syncViewer;
	private Button startSync;
	private Button cancel;
	private Button closeWhenDone;
	private Button deleteRemoteFiles;
	private Button deleteLocalFiles;
	private Button useCrc;
	private Button syncInBackground;
	private PermissionsGroup filePermission;
	private PermissionsGroup dirPermission;

	private Composite loadingComp;
	private Label loadingLabel;

	private DirectionToolBar directionBar;
	private OptionsToolBar optionsBar;

	private SyncFolder root;
	private String end1;
	private String end2;
	private Synchronizer syncer;
	private IFileStore source;
	private IFileStore dest;
	private IConnectionPoint sourceConnectionPoint;
	private IConnectionPoint destConnectionPoint;
	private ISyncEventHandler handler;
	private boolean compareInBackground;

	private Composite swappable;
	private Composite errorComp;
	private Label errorLabel;
	private Link retryLink;
	private Composite synced;
	private Label syncedIcon;
	private Label syncedText;
	private SyncJob syncJob;

	private int skipped;

	private Job buildSmartSync;

	private IFileStore[] filesToBeSynced;

	/**
	 * Creates a new smart sync dialog.
	 * 
	 * @param parent
	 *            the parent shell
	 * @param file1
	 *            the first file element
	 * @param file2
	 *            the second file element
	 * @param end1
	 *            the first end point
	 * @param end2
	 *            the second end point
	 * @throws CoreException 
	 */
	public SmartSyncDialog(Shell parent, IConnectionPoint sourceManager, IConnectionPoint destManager,
			IFileStore source, IFileStore dest, String end1, String end2) throws CoreException
	{
		super(parent);
		setShellStyle(getDefaultOrientation() | SWT.RESIZE | SWT.APPLICATION_MODAL | SWT.DIALOG_TRIM);
		this.source = source;
		this.dest = dest;
		this.end1 = end1;
		this.end2 = end2;
		this.compareInBackground = getCoreUIPreferenceStore().getBoolean(COMPARE_IN_BACKGROUND);
		this.syncer = new Synchronizer(getCoreUIPreferenceStore().getBoolean(USE_CRC), 1000);
		if (sourceManager != null)
		{
			sourceConnectionPoint = sourceManager;
			this.syncer.setClientFileManager(sourceManager);
			this.syncer.setClientFileRoot(sourceManager.getRoot());
		}
		if (destManager != null)
		{
			destConnectionPoint = destManager;
			this.syncer.setServerFileManager(destManager);
			this.syncer.setServerFileRoot(destManager.getRoot());
		}

		this.syncer.setLogger(new ILogger()
		{			
			public void logWarning(String message, Throwable th)
			{
				SyncingConsole.println(message);
			}
			
			public void logWarning(String message)
			{
				SyncingConsole.println(message);
			}
			
			public void logInfo(String message, Throwable th)
			{
				SyncingConsole.println(message);
			}
			
			public void logInfo(String message)
			{
				SyncingConsole.println(message);
			}
			
			public void logError(String message, Throwable th)
			{
				SyncingConsole.println(message);
			}
			
			public void logError(String message)
			{
				SyncingConsole.println(message);
			}
		});
	}

	/**
	 * Creates a new smart sync dialog on a list of selected files.
	 * 
	 * @param parent
	 *            the parent shell
	 * @param conf
	 *            the file manager pair
	 * @param filesToBeSynced
	 *            the selected files to be synced
	 * @throws CoreException
	 * @throws CoreException
	 */
	public SmartSyncDialog(Shell parent, ConnectionPointSyncPair conf, IFileStore[] filesToBeSynced)
			throws CoreException
	{
		this(parent, conf.getSourceFileManager(), conf.getDestinationFileManager(), conf.getSourceFileManager()
				.getRoot(), conf.getDestinationFileManager().getRoot(), conf.getSourceFileManager()
				.getName(), conf.getDestinationFileManager().getName());
		this.syncer.setClientFileManager(conf.getSourceFileManager());
		this.syncer.setServerFileManager(conf.getDestinationFileManager());


		sourceConnectionPoint = conf.getSourceFileManager();
		destConnectionPoint = conf.getDestinationFileManager();
		if (filesToBeSynced == null || filesToBeSynced.length == 0)
		{
			this.filesToBeSynced = null;
		}
		else
		{
			this.filesToBeSynced = filesToBeSynced;
			if (filesToBeSynced.length == 1)
			{
				String path = EFSUtils.getRelativePath(sourceConnectionPoint, filesToBeSynced[0]);
				if (path == null || path.trim().length() == 0)
				{
					// the selection is from the project level, so we are doing
					// a full sync
					this.filesToBeSynced = null;
				}
				else
				{
					this.end1 = this.end1 + " (" + path + ")"; //$NON-NLS-1$ //$NON-NLS-2$
				}
			}
		}
	}

	private void disconnectAndClose()
	{
		// disconnects explicitly upon closing if the sync is completed
		Job disconnectJob = new Job("disconnect the sync file manager") //$NON-NLS-1$
		{

			@Override
			protected IStatus run(IProgressMonitor monitor)
			{
				if (buildSmartSync != null)
				{
					if (buildSmartSync.getResult() == null)
					{
						buildSmartSync.cancel();
					}
					try
					{
						buildSmartSync.join();
					}
					catch (InterruptedException e)
					{
					}
				}
				if (syncJob != null)
				{
					if (syncJob.getResult() == null)
					{
						syncJob.cancel();
					}
					try
					{
						syncJob.join();
					}
					catch (InterruptedException e)
					{
					}
				}
				syncer.disconnect();
				return Status.OK_STATUS;
			}

		};
		disconnectJob.setPriority(Job.INTERACTIVE);
		disconnectJob.setSystem(true);
		disconnectJob.schedule();
		close();
	}

	/**
	 * @see org.eclipse.jface.window.Window#configureShell(org.eclipse.swt.widgets.Shell)
	 */
	protected void configureShell(Shell newShell)
	{
		super.configureShell(newShell);
		newShell.setText(Messages.SmartSyncDialog_Title);
		newShell.setImage(SyncingUIPlugin.getImage(ICON));
	}

	private Composite createDirectionOptions(Composite parent)
	{
		Composite main = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout(3, false);
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		main.setLayout(layout);
		main.setLayoutData(new GridData(SWT.END, SWT.FILL, true, false));

		Label label = new Label(main, SWT.NONE);
		label.setText(Messages.SmartSyncDialog_DirectionMode);
		label.setLayoutData(new GridData(SWT.END, SWT.CENTER, false, false));

		directionBar = new DirectionToolBar(main, this, end1, end2);
		directionBar.setSelection(getDirectionPref());
		directionBar.setEnabled(false);
		GridData gridData = new GridData(SWT.END, SWT.CENTER, true, false);
		directionBar.getControl().setLayoutData(gridData);

		return main;
	}

	private Composite createHeader(Composite parent)
	{
		Composite top = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout(2, false);
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		top.setLayout(layout);
		top.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

		Composite description = new Composite(top, SWT.NONE);
		layout = new GridLayout();
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		layout.verticalSpacing = 0;
		description.setLayout(layout);
		description.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		Label descriptionLabel = new Label(description, SWT.LEFT);
		FontData[] data = SWTUtils.resizeFont(top.getFont(), 4);
		for (int i = 0; i < data.length; i++)
		{
			data[i].setStyle(SWT.BOLD);
		}
		final Font headerFont = new Font(top.getDisplay(), data);
		descriptionLabel.addDisposeListener(new DisposeListener()
		{
			public void widgetDisposed(DisposeEvent e)
			{
				headerFont.dispose();
			}
		});
		descriptionLabel.setFont(headerFont);
		descriptionLabel.setText(Messages.SmartSyncDialog_PreviewDescription);
		descriptionLabel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

		Composite endpoints = new Composite(description, SWT.NONE);
		layout = new GridLayout(2, false);
		layout.marginHeight = 0;
		layout.marginWidth = 10;
		layout.verticalSpacing = 0;
		endpoints.setLayout(layout);
		endpoints.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true));

		Label symbol = new Label(endpoints, SWT.VERTICAL);
		symbol.setImage(SyncingUIPlugin.getImage(ICON));
		GridData gridData = new GridData(SWT.FILL, SWT.CENTER, false, true);
		gridData.verticalSpan = 2;
		symbol.setLayoutData(gridData);

		if (this.filesToBeSynced == null || this.filesToBeSynced.length <= 1)
		{
			Label end1Label = new Label(endpoints, SWT.NONE);
			end1Label.setText("Source: '" + end1 + "' (" + source.toString() + ")");
			end1Label.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		}
		else
		{
			// multiple files/folders are selected; adds a custom label
			Composite end1Comp = new Composite(endpoints, SWT.NONE);
			layout = new GridLayout(2, false);
			layout.marginHeight = 0;
			layout.marginWidth = 0;
			end1Comp.setLayout(layout);
			end1Comp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));

			Label end1Label = new Label(end1Comp, SWT.NONE);
			end1Label.setText("Source: '" + end1 + "' (" + source.toString() + ")");
			end1Label.setLayoutData(new GridData(SWT.FILL, SWT.HORIZONTAL, false, false));

			final Label end1Extra = new Label(end1Comp, SWT.NONE);
			end1Extra.setText("(multiple files/folders)"); //$NON-NLS-1$
			end1Extra.setLayoutData(new GridData(SWT.BEGINNING, SWT.FILL, true, false));

			// uses a custom tooltip
			end1Extra.setToolTipText(null);
			new LabelToolTip(end1Extra);
		}

		Label end2Label = new Label(endpoints, SWT.NONE);
		end2Label.setText("Destination: '" + end2 + "' (" + dest.toString() + ")");
		end2Label.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

		// Commented out until we let the user switch endpoints
		//		end2Combo = new Combo(endpoints, SWT.READ_ONLY);
//		final List<IVirtualFileManager> vfms = new LinkedList<IVirtualFileManager>();
//		int selectedIndex = 0;
//		// Get sync pairs that contain a VFM that contains the source file i.e. file1
//		VirtualFileManagerSyncPair[] confs = SyncManager.getContainingSyncPairs(source, true);
//		int index = -1;
//		for (VirtualFileManagerSyncPair conf : confs)
//		{
//			try
//			{
//				if (conf.getSourceFileManager().getRoot().isParentOf(source))
//				{
//					// Only consider the sync pair whose source VFM contains the source file i.e. file1
//					IVirtualFileManager destinationFileManager = conf.getDestinationFileManager();
//					end2Combo.add(destinationFileManager.getNickName());
//					vfms.add(destinationFileManager);
//					index++;
//					if (destinationFileManager.getRoot().isParentOf(dest))
//					{
//						// Remember the index of originally passed in VFM
//						selectedIndex = index;
//					}
//				}
//			}
//			catch (CoreException e1)
//			{
//				// TODO Auto-generated catch block
//				e1.printStackTrace();
//			}
//		}
//		end2Combo.select(selectedIndex);
//		end2Combo.setLayoutData(new GridData(SWT.FILL, SWT.HORIZONTAL, true, false));
//		end2Combo.addSelectionListener(new SelectionListener()
//		{
//			public void widgetDefaultSelected(SelectionEvent e)
//			{
//				widgetSelected(e);
//			}
//
//			public void widgetSelected(SelectionEvent e)
//			{
//				int selectionIndex = end2Combo.getSelectionIndex();
//				if (selectionIndex != -1)
//				{
//					// Reset the destination VFM and target file i.e. file2
//					IVirtualFileManager virtualFileManager = vfms.get(selectionIndex);
//					try
//					{
//						source = virtualFileManager.getRoot();
//					}
//					catch (CoreException e1)
//					{
//						// TODO Auto-generated catch block
//						e1.printStackTrace();
//					}
//					load(true);
//				}
//			}
//		});

		Composite status = new Composite(top, SWT.NONE);
		layout = new GridLayout();
		layout.marginRight = 25;
		status.setLayout(layout);
		status.setLayoutData(new GridData(SWT.END, SWT.FILL, false, true));

		updatedLabel = new Label(status, SWT.LEFT);
		updatedLabel.setText(UPDATED_LABEL);
		skippedLabel = new Label(status, SWT.LEFT);
		skippedLabel.setText(SKIPPED_LABEL);
		deletedLabel = new Label(status, SWT.LEFT);
		deletedLabel.setText(DELETED_LABEL);

		return top;
	}

	private Composite createDeleteOptions(Composite parent)
	{
		Composite deletes = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		deletes.setLayout(layout);
		deletes.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));

		deleteLocalFiles = new Button(deletes, SWT.CHECK);
		deleteLocalFiles.setText(Messages.SmartSyncDialog_DeleteExtra + "'" + end1 + "'");
		deleteLocalFiles.setToolTipText(Messages.SmartSyncDialog_DeleteExtraTooltip + end1 + "'");
		deleteLocalFiles.setSelection(getDeleteLocalPreference());
		deleteLocalFiles.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		deleteLocalFiles.addSelectionListener(this);

		deleteRemoteFiles = new Button(deletes, SWT.CHECK);
		deleteRemoteFiles.setText(Messages.SmartSyncDialog_DeleteExtra + "'" + end2 + "'");
		deleteRemoteFiles.setToolTipText(Messages.SmartSyncDialog_DeleteExtraTooltip + "'" + end2 + "'");
		deleteRemoteFiles.setSelection(getDeleteRemotePreference());
		deleteRemoteFiles.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		deleteRemoteFiles.addSelectionListener(this);

		return deletes;
	}

	private Composite createFooter(Composite parent)
	{
		Composite footer = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout(2, false);
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		footer.setLayout(layout);
		footer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

		createDeleteOptions(footer);
		createDirectionOptions(footer);

		return footer;
	}

	private Composite createAdvancedSection(Composite parent)
	{
		final Composite advanced = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout(2, false);
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		advanced.setLayout(layout);
		advanced.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

		final Cursor hand = new Cursor(advanced.getDisplay(), SWT.CURSOR_HAND);
		final Font boldFont = new Font(advanced.getDisplay(), SWTUtils.boldFont(advanced.getFont()));
		advanced.addDisposeListener(new DisposeListener()
		{

			public void widgetDisposed(DisposeEvent e)
			{
				if (hand != null && !hand.isDisposed())
				{
					hand.dispose();
				}
				if (boldFont != null && !boldFont.isDisposed())
				{
					boldFont.dispose();
				}
			}

		});

		final Label advancedIcon = new Label(advanced, SWT.LEFT);
		advancedIcon.setImage(SyncingUIPlugin.getImage("icons/full/obj16/maximize.png")); //$NON-NLS-1$
		advancedIcon.setCursor(hand);
		advancedIcon.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		Label advancedLabel = new Label(advanced, SWT.LEFT);
		advancedLabel.setText(Messages.SmartSyncDialog_AdvancedOptions);
		advancedLabel.setCursor(hand);
		advancedLabel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		advancedLabel.setFont(boldFont);

		final Composite advancedOptions = new Composite(advanced, SWT.NONE);
		layout = new GridLayout();
		layout.marginLeft = 15;
		advancedOptions.setLayout(layout);
		GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, false);
		gridData.horizontalSpan = 2;
		gridData.exclude = true;
		advancedOptions.setLayoutData(gridData);
		advancedOptions.setVisible(false);

		MouseAdapter expander = new MouseAdapter()
		{

			public void mouseDown(MouseEvent e)
			{
				if (advancedOptions.isVisible())
				{
					advancedOptions.setVisible(false);
					advancedIcon.setImage(SyncingUIPlugin.getImage("icons/full/obj16/maximize.png")); //$NON-NLS-1$
					((GridData) advancedOptions.getLayoutData()).exclude = true;
				}
				else
				{
					advancedOptions.setVisible(true);
					advancedIcon.setImage(SyncingUIPlugin.getImage("icons/full/obj16/minimize.png")); //$NON-NLS-1$
					((GridData) advancedOptions.getLayoutData()).exclude = false;
				}

				displayArea.layout(true, true);
			}

		};
		advancedIcon.addMouseListener(expander);
		advancedLabel.addMouseListener(expander);

		useCrc = new Button(advancedOptions, SWT.CHECK);
		useCrc.setText(Messages.SmartSyncDialog_UseCrc);
		useCrc.setSelection(getCoreUIPreferenceStore().getBoolean(USE_CRC));
		useCrc.addSelectionListener(this);

		syncInBackground = new Button(advancedOptions, SWT.CHECK);
		syncInBackground.setText(Messages.SmartSyncDialog_SyncInBackground);
		syncInBackground.setSelection(getCoreUIPreferenceStore().getBoolean(COMPARE_IN_BACKGROUND));
		syncInBackground.addSelectionListener(this);

		Group group = new Group(advancedOptions, SWT.NONE);
		group.setText(Messages.SmartSyncDialog_Permissions);
		layout = new GridLayout(2, true);
		layout.marginWidth = 0;
		group.setLayout(layout);
		group.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		filePermission = new PermissionsGroup(group);
		filePermission.setText(Messages.SmartSyncDialog_PermForFiles);
		filePermission.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		filePermission.setPermissions(FilePrefUtils.getFilePermission());
		dirPermission = new PermissionsGroup(group);
		dirPermission.setText(Messages.SmartSyncDialog_PermForDirectories);
		dirPermission.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		dirPermission.setPermissions(FilePrefUtils.getDirectoryPermission());

		return advanced;
	}

	private Composite createMainSection(Composite parent)
	{
		Composite main = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		main.setLayout(layout);
		main.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		createViewOptions(main);
		swappable = createTable(main);
		createFooter(main);

		return main;
	}

//	private Composite createCommentSection(Composite parent)
//	{
//		Composite commentArea = new Composite(parent, SWT.NONE);
//		commentArea.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
//
//		GridLayout layout = new GridLayout(2, false);
//		layout.marginHeight = 0;
//		layout.marginWidth = 0;
//		commentArea.setLayout(layout);
//
//		Label commentLabel = new Label(commentArea, SWT.BEGINNING);
//		commentLabel.setText(Messages.SmartSyncDialog_CommentLabel);
//		GridData commentLabelGridData = new GridData(SWT.BEGINNING, SWT.CENTER, false, false);
//		commentLabel.setLayoutData(commentLabelGridData);
//
//		Link helpLink = new Link(commentArea, SWT.END);
//		helpLink.setText(Messages.SmartSyncDialog_WhatIsThisLink);
//		GridData helpLinkGridData = new GridData(SWT.END, SWT.CENTER, true, false);
//		helpLink.setLayoutData(helpLinkGridData);
//		helpLink.addSelectionListener(new SelectionAdapter()
//		{
//			@Override
//			public void widgetSelected(SelectionEvent e)
//			{
//				CoreUIUtils
//						.openBrowserURL("http://www.aptana.com/docs/index.php/My_Cloud_-_Team#What_is_the_Cloud_Team_Comment_feature.3F"); //$NON-NLS-1$
//			}
//		});
//
//		comment = new Text(commentArea, SWT.MULTI | SWT.BORDER);
//		GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, true);
//		gridData.horizontalSpan = 2;
//		gridData.heightHint = 50;
//		comment.setLayoutData(gridData);
//
//		boolean isCloudSync = isCloudSync();
//		comment.setEnabled(isCloudSync);
//		if (isCloudSync)
//		{
//			comment.setText(Messages.SmartSyncDialog_HintComment);
//			comment.selectAll();
//		}
//		comment.addModifyListener(new ModifyListener()
//		{
//
//			public void modifyText(ModifyEvent e)
//			{
//				commentStr = comment.getText();
//			}
//
//		});
//		comment.addFocusListener(new FocusListener()
//		{
//
//			public void focusGained(FocusEvent e)
//			{
//				if (firstEdit)
//				{
//					firstEdit = false;
//					comment.setText(""); //$NON-NLS-1$
//				}
//			}
//
//			public void focusLost(FocusEvent e)
//			{
//			}
//
//		});
//
//		return commentArea;
//	}

	private Composite createErrorSection(Composite parent)
	{
		Composite main = new Composite(parent, SWT.NONE);
		main.setLayout(new GridLayout());
		main.setBackgroundMode(SWT.INHERIT_DEFAULT);
		main.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_LIST_BACKGROUND));
		GridData gridData = new GridData(SWT.CENTER, SWT.CENTER, true, true);
		gridData.exclude = true;
		main.setLayoutData(gridData);

		errorLabel = new Label(main, SWT.CENTER | SWT.WRAP);
		final Font font = new Font(main.getDisplay(), "Arial", 12, SWT.NONE); //$NON-NLS-1$
		errorLabel.setFont(font);
		errorLabel.addDisposeListener(new DisposeListener()
		{
			public void widgetDisposed(DisposeEvent e)
			{
				font.dispose();
			}
		});
		errorLabel.setForeground(main.getDisplay().getSystemColor(SWT.COLOR_GRAY));
		errorLabel.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, true, false));
		retryLink = new Link(main, SWT.NONE);
		retryLink.setText("<a>" + Messages.SmartSyncDialog_Retry + "</a>"); //$NON-NLS-1$ //$NON-NLS-2$
		retryLink.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, false, true));
		retryLink.addSelectionListener(this);

		main.setVisible(false);

		return main;
	}

	private void updateStatLabels()
	{
		int deleted = 0;
		int updated = 0;
		int skipped = 0;
		int selection = directionBar.getSelection();
		ISyncResource[] resources = syncViewer.getCurrentResources();
		for (ISyncResource resource : resources)
		{
			if (resource.isSkipped())
			{
				skipped++;
			}
			else if ((selection == DirectionToolBar.BOTH || selection == DirectionToolBar.DOWNLOAD)
					&& deleteLocalFiles.getSelection() && resource.getSyncState() == SyncState.ClientItemOnly)
			{
				deleted++;
			}
			else if ((selection == DirectionToolBar.BOTH || selection == DirectionToolBar.UPLOAD)
					&& deleteRemoteFiles.getSelection() && resource.getSyncState() == SyncState.ServerItemOnly)
			{
				deleted++;
			}
			else
			{
				if (resource.getPair() != null)
				{
					updated++;
				}
			}
		}
		updatedLabel.setText(UPDATED_LABEL + updated);
		if (deleted == 0)
		{
			deletedLabel.setFont(updatedLabel.getFont());
			deletedLabel.setForeground(null);
		}
		else
		{
			// makes the delete label bold and red to make user aware there are
			// going to be files deleted
			if (boldFont == null)
			{
				FontData[] data = SWTUtils.boldFont(deletedLabel.getFont());
				boldFont = new Font(deletedLabel.getDisplay(), data);
				deletedLabel.addDisposeListener(new DisposeListener()
				{

					public void widgetDisposed(DisposeEvent e)
					{
						boldFont.dispose();
					}

				});
			}
			deletedLabel.setFont(boldFont);
			deletedLabel.setForeground(deletedLabel.getDisplay().getSystemColor(SWT.COLOR_RED));
		}
		deletedLabel.setText(DELETED_LABEL + deleted);
		skippedLabel.setText(SKIPPED_LABEL + skipped);
		this.skipped = skipped;
		skippedLabel.getParent().layout(true, true);
		startSync.setEnabled(deleted + updated > 0);
	}

	private Composite createViewOptions(Composite parent)
	{
		Composite main = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		main.setLayout(layout);
		main.setLayoutData(new GridData(SWT.END, SWT.FILL, true, false));

		optionsBar = new OptionsToolBar(main, this);
		optionsBar.setPresentationType(getPresentationTypePref());
		optionsBar.setShowDatesSelected(getShowModificationTimePref());
		optionsBar.setEnabled(false);

		return main;
	}

	private Composite createTable(Composite parent)
	{
		Composite main = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.marginHeight = 0;
		layout.horizontalSpacing = 0;
		main.setLayout(layout);
		GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, true);
		gridData.heightHint = 400;
		main.setLayoutData(gridData);
		main.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_LIST_BACKGROUND));

		syncViewer = new SmartSyncViewer(main, end1, end2);
		syncViewer.setPresentationType(getPresentationTypePref());
		syncViewer.setShowDatesSelected(getShowModificationTimePref());
		syncViewer.setCellModifier(new ICellModifier()
		{

			public void modify(Object element, String property, Object value)
			{
				// Only allow checking of skipped box when smart sync isn't
				// running
				if (startSync.getText().equals(Messages.SmartSyncDialog_StartSync))
				{
					if (element instanceof Item)
					{
						element = ((Item) element).getData();
					}
					ISyncResource resource = (ISyncResource) element;
					resource.setSkipped(Boolean.parseBoolean(value.toString()));
					syncViewer.update(element, null);
					if (resource instanceof SyncFolder)
					{
						// refreshes the children of the folder
						Collection<ISyncResource> children = ((SyncFolder) resource).getAllChildren();
						for (ISyncResource child : children)
						{
							syncViewer.update(child, null);
						}
					}
					if (!resource.isSkipped() && resource.getParent() != null)
					{
						resource.getParent().setSkipped(false, false);
						syncViewer.update(resource.getParent(), null);
					}
					updateStatLabels();
				}
			}

			public Object getValue(Object element, String property)
			{
				return Boolean.valueOf(((ISyncResource) element).isSkipped());
			}

			public boolean canModify(Object element, String property)
			{
				return Messages.SmartSyncDialog_ColumnSkip.equals(property);
			}

		});
		syncViewer.addFilter(new ViewerFilter()
		{

			public boolean select(Viewer viewer, Object parentElement, Object element)
			{
				if (element instanceof SyncFile || element instanceof SyncFolder)
				{
					return true;
				}
				return false;
			}

		});

		errorComp = createErrorSection(main);
		loadingComp = createLoadingSection(main);
		synced = createSyncedSection(main);

		return main;
	}

	private Composite createSyncedSection(Composite parent)
	{
		Composite main = new Composite(parent, SWT.NONE);
		main.setLayout(new GridLayout(2, false));
		main.setBackgroundMode(SWT.INHERIT_DEFAULT);
		main.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_LIST_BACKGROUND));
		GridData gridData = new GridData(SWT.CENTER, SWT.CENTER, true, false);
		gridData.exclude = true;
		main.setLayoutData(gridData);

		syncedIcon = new Label(main, SWT.CENTER);
		syncedIcon.setImage(SyncingUIPlugin.getImage("icons/full/obj16/synced.png")); //$NON-NLS-1$
		syncedIcon.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, false, false));
		syncedText = new Label(main, SWT.CENTER | SWT.WRAP);
		final Font font = new Font(main.getDisplay(), "Arial", 12, SWT.NONE); //$NON-NLS-1$
		syncedText.setFont(font);
		syncedText.addDisposeListener(new DisposeListener()
		{
			public void widgetDisposed(DisposeEvent e)
			{
				font.dispose();
			}
		});
		syncedText.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, true, false));
		syncedText.setText(end1 + " and " + end2 + Messages.SmartSyncDialog_InSync); //$NON-NLS-1$

		main.setVisible(false);

		return main;
	}

	private Composite createLoadingSection(Composite parent)
	{
		Composite loadingComp = new Composite(parent, SWT.NONE);
		loadingComp.setLayout(new GridLayout());
		loadingComp.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, true, false));
		loadingComp.setBackgroundMode(SWT.INHERIT_DEFAULT);
		loadingComp.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_LIST_BACKGROUND));

		loadingLabel = new Label(loadingComp, SWT.NONE);
		loadingLabel.setText(SYNC_LABEL + "..."); //$NON-NLS-1$
		loadingLabel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		ProgressBar loadingBar = new ProgressBar(loadingComp, SWT.SMOOTH | SWT.INDETERMINATE);
		loadingBar.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

		return loadingComp;
	}

	private Composite createActionsBar(Composite parent)
	{
		Composite main = new Composite(parent, SWT.NONE);
		main.setLayout(new GridLayout(3, false));
		main.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, false));

		closeWhenDone = new Button(main, SWT.CHECK);
		closeWhenDone.setText(Messages.SmartSyncDialog_CloseWhenDone);
		closeWhenDone.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		closeWhenDone.setSelection(getSyncingPreferenceStore().getBoolean(CLOSE_WHEN_DONE));
		closeWhenDone.addSelectionListener(this);

		cancel = new Button(main, SWT.PUSH);
		cancel.setText(Messages.SmartSyncDialog_Cancel);
		cancel.setLayoutData(new GridData(SWT.FILL, SWT.END, false, false));
		cancel.addSelectionListener(this);

		startSync = new Button(main, SWT.PUSH);
		startSync.setText(Messages.SmartSyncDialog_StartSync);
		GridData gridData = new GridData(SWT.FILL, SWT.END, false, false);
		GC gc = new GC(startSync);
		// calculates the ideal width
		gridData.widthHint = Math.max(gc.stringExtent(Messages.SmartSyncDialog_StartSync).x, gc
				.stringExtent(Messages.SmartSyncDialog_RunInBackground).x) + 50;
		gc.dispose();
		startSync.setLayoutData(gridData);
		startSync.addSelectionListener(this);

		return main;
	}

	private void setEnabled(boolean enabled)
	{
		//end2Combo.setEnabled(enabled);
		directionBar.setEnabled(enabled);
		optionsBar.setEnabled(enabled);
		boolean syncEnabled = enabled && syncViewer.getCurrentResources().length > 0;
		startSync.setEnabled(syncEnabled);

		if (enabled)
		{
			updateFileButtonsState();
		}
	}

	/**
	 * @see org.eclipse.jface.window.Window#open()
	 */
	public int open()
	{
		if (sourceConnectionPoint instanceof IProjectProvider)
		{
			IProjectProvider projectProvider = (IProjectProvider) sourceConnectionPoint;
			IProject project = projectProvider.getProject();
			if (project != null)
			{
				try
				{
					if (Boolean.TRUE.equals((Boolean) project.getSessionProperty(Synchronizer.SYNC_IN_PROGRESS)))
					{
						MessageDialog messageDialog = new MessageDialog(CoreUIUtils.getActiveShell(),
								Messages.SmartSyncDialog_SyncInProgressTitle, null,
								Messages.SmartSyncDialog_SyncInProgress, MessageDialog.QUESTION, new String[] {
										IDialogConstants.CANCEL_LABEL, Messages.SmartSyncDialog_ContinueLabel, }, 0);
						if (messageDialog.open() == 0)
						{
							return CANCEL;
						}
					}
				}
				catch (CoreException e)
				{
				}
			}
		}
		if (!compareInBackground)
		{
			super.open();
		}
		load(true);
		return OK;
	}

	private void load(final boolean showSyncedMessage)
	{
		if (!compareInBackground)
		{
			setEnabled(false);
			GridData data = (GridData) loadingComp.getLayoutData();
			data.exclude = false;
			loadingComp.setVisible(true);
			loadingComp.getParent().layout(true, true);
		}
		// displayArea.layout(true, true);
		final boolean forceUp = compareInBackground ? false
				: (directionBar.getSelection() == DirectionToolBar.FORCE_UPLOAD);
		final boolean forceDown = compareInBackground ? false
				: (directionBar.getSelection() == DirectionToolBar.FORCE_DOWNLOAD);

		if (buildSmartSync != null)
		{
			// cancels the existing one
			buildSmartSync.cancel();
		}
		buildSmartSync = new Job("Generating Smart Sync") //$NON-NLS-1$
		{

			protected IStatus run(final IProgressMonitor monitor)
			{
				syncer.setEventHandler(new SyncEventHandlerAdapterWithProgressMonitor(monitor)
				{

					public boolean syncEvent(final VirtualFileSyncPair item, int index, int totalItems)
					{
						if (item != null)
						{
							if (!compareInBackground)
							{
								CoreUIUtils.getDisplay().asyncExec(new Runnable()
								{

									public void run()
									{
										if (loadingLabel == null || loadingLabel.isDisposed())
										{
											return;
										}
										String name = getFilename(item);
										if (name != null)
										{
											loadingLabel.setText(SYNC_LABEL + name);
											loadingLabel.getParent().layout(true, true);
										}
									}

								});
							}
						}
						return super.syncEvent(item, index, totalItems);
					}

				});
				VirtualFileSyncPair[] items = new VirtualFileSyncPair[0];
				Exception error = null;
				try
				{
					if (forceUp)
					{
						IFileStore[] clientFiles = (IFileStore[]) ((filesToBeSynced == null) ? EFSUtils.getFiles(
								source, true, false, null) : EFSUtils.getAllFiles(filesToBeSynced, true, false, monitor));
						items = syncer.createSyncItems(clientFiles, new IFileStore[0], monitor);
						Map<String, VirtualFileSyncPair> pairs = new HashMap<String, VirtualFileSyncPair>();
						for (VirtualFileSyncPair item : items)
						{
							pairs.put(item.getRelativePath(), item);
						}
						IFileStore[] serverFiles = (IFileStore[]) EFSUtils.getFiles(dest, true, false, monitor);
						VirtualFileSyncPair pair;
						for (IFileStore file : serverFiles)
						{
							pair = pairs.get(EFSUtils.getRelativePath(destConnectionPoint.getRoot(), file));
							if (pair != null)
							{
								pair.setDestinationFile(file);
								pair.setSyncState(SyncState.ClientItemIsNewer);
							}
						}
					}
					else if (forceDown)
					{
						IFileStore[] serverFiles = (IFileStore[]) ((filesToBeSynced == null) ? EFSUtils.getFiles(dest,
								true, false, null) : SyncUtils.getDownloadFiles(sourceConnectionPoint,
								destConnectionPoint, filesToBeSynced, true, monitor));
						items = syncer.createSyncItems(new IFileStore[0], serverFiles, monitor);
						Map<String, VirtualFileSyncPair> pairs = new HashMap<String, VirtualFileSyncPair>();
						for (VirtualFileSyncPair item : items)
						{
							pairs.put(item.getRelativePath(), item);
						}
						IFileStore[] clientFiles = (IFileStore[]) EFSUtils.getFiles(source, true, false, monitor);
						VirtualFileSyncPair pair;
						for (IFileStore file : clientFiles)
						{
							pair = pairs.get(EFSUtils.getRelativePath(sourceConnectionPoint.getRoot(), file));
							if (pair != null)
							{
								pair.setSourceFile(file);
								pair.setSyncState(SyncState.ServerItemIsNewer);
							}
						}
					}
					else
					{
						if (filesToBeSynced == null)
						{
							items = syncer.getSyncItems(sourceConnectionPoint, destConnectionPoint, source, dest, monitor);
						}
						else
						{
							IFileStore[] clientFiles = EFSUtils.getAllFiles(filesToBeSynced, true, false, monitor);
							IFileStore[] serverFiles = SyncUtils.getUploadFiles(sourceConnectionPoint,
									destConnectionPoint, filesToBeSynced, monitor);

							items = syncer.createSyncItems(clientFiles, serverFiles, monitor);
						}
					}
				}
				catch (Exception e1)
				{
					IdeLog.logError(SyncingUIPlugin.getDefault(), Messages.SmartSyncDialog_ErrorSmartSync, e1);
					error = e1;
				}
				if (monitor.isCanceled())
				{
					return Status.CANCEL_STATUS;
				}
				if (items != null && error == null)
				{
					// no error
					root = SyncModelBuilder.buildSyncFolder(sourceConnectionPoint, destConnectionPoint, items);
					UIJob update = new UIJob("Loading Smart Sync") //$NON-NLS-1$
					{

						public IStatus runInUIThread(IProgressMonitor monitor)
						{
							if (compareInBackground)
							{
								SmartSyncDialog.super.open();
							}
							if (loadingComp != null && !loadingComp.isDisposed())
							{
								GridData data = (GridData) loadingComp.getLayoutData();
								data.exclude = true;
								loadingComp.setVisible(false);
								data = (GridData) errorComp.getLayoutData();
								data.exclude = true;
								errorComp.setVisible(false);
								syncViewer.setInput(root);

								if (syncViewer.getCurrentResources().length > 0)
								{
									data = (GridData) synced.getLayoutData();
									data.grabExcessVerticalSpace = true;
									data.exclude = true;
									synced.setVisible(false);
									data = (GridData) syncViewer.getTree().getLayoutData();
									data.exclude = false;
									syncViewer.setVisible(true);
									setEnabled(true);
									startSync.setFocus();
								}
								else if (showSyncedMessage)
								{
									data = (GridData) syncViewer.getTree().getLayoutData();
									data.exclude = true;
									syncViewer.setVisible(false);
									data = (GridData) synced.getLayoutData();
									data.grabExcessVerticalSpace = true;
									data.exclude = false;
									synced.setVisible(true);
									cancel.setText(Messages.SmartSyncDialog_Close);
									setEnabled(false);
									//end2Combo.setEnabled(true);
									syncer.disconnect();
								}
								else
								{
									setEnabled(true);
								}
								swappable.getParent().layout(true, true);
								updateStatLabels();
							}
							return Status.OK_STATUS;
						}

					};
					update.schedule();
				}
				else
				{
					final StringBuilder errorMessage = new StringBuilder();
					if (error != null)
					{
						// when it is UnknownHostException, adds some more details in the message
						if (error instanceof UnknownHostException)
						{
							errorMessage.append(MessageFormat.format(Messages.SmartSyncDialog_UnknownHostError, end1,
									end2));
						}
						else
						{
							errorMessage.append(Messages.SmartSyncDialog_ErrorSync);
							errorMessage.append("\n " + Messages.SmartSyncDialog_ErrorMessage + error.getMessage()); //$NON-NLS-1$
						}
					}
					UIJob showError = new UIJob("Showing smart sync error") //$NON-NLS-1$
					{

						public IStatus runInUIThread(IProgressMonitor monitor)
						{
							if (compareInBackground)
							{
								SmartSyncDialog.super.open();
							}
							if (loadingComp != null && !loadingComp.isDisposed())
							{
								GridData data = (GridData) loadingComp.getLayoutData();
								data.exclude = true;
								loadingComp.setVisible(false);
								data = (GridData) syncViewer.getTree().getLayoutData();
								data.exclude = true;
								syncViewer.setVisible(false);
								data = (GridData) errorComp.getLayoutData();
								data.exclude = false;
								errorComp.setVisible(true);
								errorLabel.setText(errorMessage.toString());
								swappable.getParent().layout(true, true);
								setEnabled(false);
								syncer.disconnect();
							}
							return Status.OK_STATUS;
						}

					};
					showError.schedule();
				}
				return Status.OK_STATUS;
			}

		};
		buildSmartSync.setPriority(Job.LONG);
		buildSmartSync.setSystem(true);
		buildSmartSync.schedule();
	}

	/**
	 * @see org.eclipse.jface.window.Window#createContents(org.eclipse.swt.widgets.Composite)
	 */
	protected Control createContents(Composite parent)
	{
		displayArea = new Composite(parent, SWT.NONE);
		displayArea.setLayout(new GridLayout());
		displayArea.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		createHeader(displayArea);
		createMainSection(displayArea);
		//createCommentSection(displayArea);
		createAdvancedSection(displayArea);
		createActionsBar(displayArea);

		syncViewer.setSyncDirection(directionBar.getSelection());
		setEnabled(false);
		updateDeleteStates();
		if (getShell() != null && getParentShell() != null)
		{
			SWTUtils.center(getShell(), getParentShell());
		}

		return displayArea;
	}

	/**
	 * Sets the handler for syncing events.
	 * 
	 * @param handler
	 *            the handler to set
	 */
	public void setHandler(ISyncEventHandler handler)
	{
		this.handler = handler;
	}

	/**
	 * @see org.eclipse.swt.events.SelectionListener#widgetDefaultSelected(SelectionEvent)
	 */
	public void widgetDefaultSelected(SelectionEvent e)
	{
	}

	/**
	 * @see org.eclipse.swt.events.SelectionListener#widgetSelected(SelectionEvent)
	 */
	public void widgetSelected(SelectionEvent e)
	{
		Object source = e.getSource();
		if (source == deleteLocalFiles)
		{
			saveDeleteLocalPreference(deleteLocalFiles.getSelection());
			updateDeleteStates();
		}
		else if (source == deleteRemoteFiles)
		{
			saveDeleteRemotePreference(deleteRemoteFiles.getSelection());
			updateDeleteStates();
		}
		else if (source == cancel)
		{
			cancel();
		}
		else if (source == closeWhenDone)
		{
			getSyncingPreferenceStore().setValue(CLOSE_WHEN_DONE, closeWhenDone.getSelection());
		}
		else if (source == useCrc)
		{
			getCoreUIPreferenceStore().setValue(USE_CRC, useCrc.getSelection());
		}
		else if (source == syncInBackground)
		{
			getCoreUIPreferenceStore().setValue(COMPARE_IN_BACKGROUND, syncInBackground.getSelection());
		}
		else if (source == retryLink)
		{
			load(true);
		}
		else if (source == startSync)
		{
			String text = startSync.getText();
			if (text.equals(Messages.SmartSyncDialog_StartSync))
			{
				startSync.setText(Messages.SmartSyncDialog_RunInBackground);
				startSync.getParent().layout();
				startSync();
			}
			else if (text.equals(Messages.SmartSyncDialog_RunInBackground))
			{
				setReturnCode(CANCEL);
				close();
			}
		}
	}

	/**
	 * @see org.eclipse.swt.events.ModifyListener#modifyText(org.eclipse.swt.events.ModifyEvent)
	 */
	public void modifyText(ModifyEvent e)
	{
	}

	/**
	 * @see com.aptana.ide.syncing.ui.views.views.DirectionToolBar.Client#selectionChanged(boolean)
	 */
	public void selectionChanged(int direction, boolean reload)
	{
		updateFileButtonsState();
		syncViewer.setSyncDirection(directionBar.getSelection());
		saveDirectionPref(direction);
		if (reload)
		{
			load(false);
		}
		else
		{
			syncViewer.refreshAndExpandTo(2);
		}
		updateStatLabels();
	}

	/**
	 * @see com.aptana.ide.syncing.ui.views.views.OptionsToolBar.Client#stateChanged(int)
	 */
	public void stateChanged(int type)
	{
		syncViewer.setPresentationType(type);
		savePresentationTypePref(type);
	}

	/**
	 * @see com.aptana.ide.syncing.ui.views.views.OptionsToolBar.Client#showDatesSelected(boolean)
	 */
	public void showDatesSelected(boolean show)
	{
		syncViewer.setShowDatesSelected(show);
		saveShowModificationTimePref(show);
	}

	public void syncItem(final VirtualFileSyncPair item)
	{
		// syncing on a specific item has started
		CoreUIUtils.getDisplay().asyncExec(new Runnable()
		{

			public void run()
			{
				ISyncResource resource = root.find(item);
				if (resource != null)
				{
					resource.setTransferState(ISyncResource.SYNCING);

					if (displayArea != null && !displayArea.isDisposed())
					{
						syncViewer.showProgress(item);
					}
				}
			}

		});
	}

	public void syncProgress(final VirtualFileSyncPair item, final long bytes)
	{
		// updates the progress on a specific item
		CoreUIUtils.getDisplay().asyncExec(new Runnable()
		{

			public void run()
			{
				ISyncResource resource = root.find(item);
				if (resource != null)
				{
					resource.setTransferredBytes(bytes);

					if (displayArea != null && !displayArea.isDisposed())
					{
						syncViewer.update(resource, null);
					}
				}
			}

		});
	}

	public void syncDone(final VirtualFileSyncPair item, boolean allDone)
	{
		// syncing is completed for a specific item
		CoreUIUtils.getDisplay().asyncExec(new Runnable()
		{

			public void run()
			{
				ISyncResource resource = root.find(item);
				if (resource != null)
				{
					resource.setTransferState(ISyncResource.SYNCED);

					if (displayArea != null && !displayArea.isDisposed())
					{
						syncViewer.update(resource, null);
						syncViewer.reveal(resource);
					}
				}
			}

		});

		if (allDone)
		{
			syncJobDone();
		}
	}

	public void syncError(final VirtualFileSyncPair item, boolean allDone)
	{
		// an error was encountered during syncing
		CoreUIUtils.getDisplay().asyncExec(new Runnable()
		{

			public void run()
			{
				ISyncResource resource = root.find(item);
				if (resource != null)
				{
					resource.setTransferState(ISyncResource.ERROR);

					if (syncViewer != null && !syncViewer.getTree().isDisposed())
					{
						syncViewer.update(resource, null);
						syncViewer.reveal(resource);
					}
				}
			}

		});

		if (allDone)
		{
			syncJobDone();
		}
	}

	private void startSync()
	{
		// disables the options when sync has started
		directionBar.setEnabled(false);
		optionsBar.setEnabled(false);
		//end2Combo.setEnabled(false);

		// saves the permission preferences
		// FilePlugin.getDefault().getPreferenceStore().setValue(
		// com.aptana.ide.core.ui.io.file.IPreferenceConstants.FILE_PERMISSION, filePermission.getPermissions());
		// FilePlugin.getDefault().getPreferenceStore().setValue(
		// com.aptana.ide.core.ui.io.file.IPreferenceConstants.DIRECTORY_PERMISSION,
		// dirPermission.getPermissions());

		List<VirtualFileSyncPair> pairs = new ArrayList<VirtualFileSyncPair>();
		ISyncResource[] resources = syncViewer.getCurrentResources();
		for (ISyncResource resource : resources)
		{
			VirtualFileSyncPair pair = resource.getPair();
			if (!resource.isSkipped() && pair != null)
			{
				pairs.add(pair);
			}
		}

		boolean deleteLocal = deleteLocalFiles.getEnabled() && deleteLocalFiles.getSelection();
		boolean deleteRemote = deleteRemoteFiles.getEnabled() && deleteRemoteFiles.getSelection();
		deleteRemoteFiles.setEnabled(false);
		deleteLocalFiles.setEnabled(false);

		int direction = -1;
		int selection = directionBar.getSelection();
		if (selection == DirectionToolBar.DOWNLOAD || selection == DirectionToolBar.FORCE_DOWNLOAD)
		{
			direction = SyncJob.DOWNLOAD;
		}
		else if (selection == DirectionToolBar.UPLOAD || selection == DirectionToolBar.FORCE_UPLOAD)
		{
			direction = SyncJob.UPLOAD;
		}
		else if (selection == DirectionToolBar.BOTH)
		{
			direction = SyncJob.BOTH;
		}

		if (syncJob != null)
		{
			// cancels the previous job if exists
			syncJob.cancel();
		}
		syncJob = new SyncJob(syncer, pairs, direction, deleteRemote, deleteLocal, this);
		syncJob.schedule();
	}

	private void syncJobDone()
	{
		if (handler != null)
		{
			handler.syncDone(null);
		}
		if (source != null && dest != null)
		{
			String comment = ""; //firstEdit ? "" : commentStr; //$NON-NLS-1$
			SmartSyncEventManager.getManager().fireEvent(syncJob.getCompletedPairs(), sourceConnectionPoint,
					destConnectionPoint, comment);
		}

		UIJob updateEndJob = new UIJob("Updating sync") //$NON-NLS-1$
		{

			public IStatus runInUIThread(IProgressMonitor monitor)
			{
				if (closeWhenDone != null && !closeWhenDone.isDisposed())
				{
					if (closeWhenDone.getSelection())
					{
						setReturnCode(CANCEL);
						disconnectAndClose();
					}
					else
					{
						cancel.setText(Messages.SmartSyncDialog_Close);
						startSync.setEnabled(false);

						int errorCount = syncJob.getErrorCount();
						if (errorCount == 0)
						{
							// completely synced
							if (skipped > 0)
							{
								// adds more words to the success text if there are skipped files
								syncedText.setText(end1 + " and " + end2 + Messages.SmartSyncDialog_InSync //$NON-NLS-1$ 
										+ "\n " + Messages.SmartSyncDialog_SkippedFilesInSync); //$NON-NLS-1$ 
							}
						}
						else
						{
							GridData data = (GridData) syncedIcon.getLayoutData();
							data.exclude = true;
							syncedIcon.setVisible(false);
							syncedText.setText(errorCount + Messages.SmartSyncDialog_SyncError);
						}
						GridData data = (GridData) synced.getLayoutData();
						data.exclude = false;
						synced.setVisible(true);
						setEnabled(false);
						swappable.layout(true, true);
					}
				}
				else
				{
					// disconnect directly
					syncer.disconnect();
				}
				return Status.OK_STATUS;
			}

		};
		updateEndJob.setSystem(true);
		updateEndJob.schedule();
	}

	private void updateDeleteStates()
	{
		syncViewer.setDeleteLocalFiles(deleteLocalFiles.getSelection());
		syncViewer.setDeleteRemoteFiles(deleteRemoteFiles.getSelection());
		updateStatLabels();
		boolean syncEnabled = syncViewer.getCurrentResources().length > 0;
		startSync.setEnabled(syncEnabled);
	}

	private void updateFileButtonsState()
	{
		int selection = directionBar.getSelection();
		switch (selection)
		{
			case DirectionToolBar.UPLOAD:
				deleteLocalFiles.setEnabled(false);
				deleteRemoteFiles.setEnabled(true);
				break;
			case DirectionToolBar.DOWNLOAD:
				deleteLocalFiles.setEnabled(true);
				deleteRemoteFiles.setEnabled(false);
				break;
			case DirectionToolBar.FORCE_DOWNLOAD:
			case DirectionToolBar.FORCE_UPLOAD:
				deleteLocalFiles.setEnabled(false);
				deleteRemoteFiles.setEnabled(false);
				break;
			default:
				deleteRemoteFiles.setEnabled(true);
				deleteLocalFiles.setEnabled(true);
		}
	}

	private void cancel()
	{
		if (buildSmartSync != null)
		{
			buildSmartSync.cancel();
		}
		if (syncJob != null)
		{
			syncJob.cancel();
		}
		disconnectAndClose();
	}

	private static String getFilename(VirtualFileSyncPair item)
	{
		if (item.getDestinationFile() != null)
		{
			return item.getDestinationFile().getName();
		}
		if (item.getSourceFile() != null)
		{
			return item.getSourceFile().getName();
		}
		return null;
	}

	private static IPreferenceStore getCoreUIPreferenceStore()
	{
		return CoreUIPlugin.getDefault().getPreferenceStore();
	}

	private static IPreferenceStore getSyncingPreferenceStore()
	{
		return SyncingUIPlugin.getDefault().getPreferenceStore();
	}

	private static int getPresentationTypePref()
	{
		String viewPref = getSyncingPreferenceStore().getString(
				com.aptana.ide.syncing.ui.preferences.IPreferenceConstants.VIEW_MODE);
		if (com.aptana.ide.syncing.ui.preferences.IPreferenceConstants.VIEW_TREE.equals(viewPref))
		{
			return OptionsToolBar.TREE_VIEW;
		}
		return OptionsToolBar.FLAT_VIEW;
	}

	private static int getDirectionPref()
	{
		String directionPref = getSyncingPreferenceStore().getString(
				com.aptana.ide.syncing.ui.preferences.IPreferenceConstants.DIRECTION_MODE);
		if (com.aptana.ide.syncing.ui.preferences.IPreferenceConstants.DIRECTION_BOTH.equals(directionPref))
		{
			return DirectionToolBar.BOTH;
		}
		if (com.aptana.ide.syncing.ui.preferences.IPreferenceConstants.DIRECTION_UPLOAD.equals(directionPref))
		{
			return DirectionToolBar.UPLOAD;
		}
		if (com.aptana.ide.syncing.ui.preferences.IPreferenceConstants.DIRECTION_DOWNLOAD.equals(directionPref))
		{
			return DirectionToolBar.DOWNLOAD;
		}
		if (com.aptana.ide.syncing.ui.preferences.IPreferenceConstants.DIRECTION_FORCE_UPLOAD.equals(directionPref))
		{
			return DirectionToolBar.FORCE_UPLOAD;
		}
		return DirectionToolBar.FORCE_DOWNLOAD;
	}

	private static boolean getDeleteLocalPreference()
	{
		return getSyncingPreferenceStore().getBoolean(
				com.aptana.ide.syncing.ui.preferences.IPreferenceConstants.DELETE_LOCAL_FILES);
	}

	private static boolean getDeleteRemotePreference()
	{
		return getSyncingPreferenceStore().getBoolean(
				com.aptana.ide.syncing.ui.preferences.IPreferenceConstants.DELETE_REMOTE_FILES);
	}

	private static boolean getShowModificationTimePref()
	{
		return getSyncingPreferenceStore().getBoolean(
				com.aptana.ide.syncing.ui.preferences.IPreferenceConstants.SHOW_MODIFICATION_TIME);
	}

	private static void savePresentationTypePref(int type)
	{
		IPreferenceStore prefs = getSyncingPreferenceStore();
		switch (type)
		{
			case OptionsToolBar.FLAT_VIEW:
				prefs.setValue(com.aptana.ide.syncing.ui.preferences.IPreferenceConstants.VIEW_MODE,
						com.aptana.ide.syncing.ui.preferences.IPreferenceConstants.VIEW_FLAT);
				break;
			case OptionsToolBar.TREE_VIEW:
				prefs.setValue(com.aptana.ide.syncing.ui.preferences.IPreferenceConstants.VIEW_MODE,
						com.aptana.ide.syncing.ui.preferences.IPreferenceConstants.VIEW_TREE);
				break;
		}
	}

	private static void saveDirectionPref(int direction)
	{
		IPreferenceStore prefs = getSyncingPreferenceStore();
		switch (direction)
		{
			case DirectionToolBar.BOTH:
				prefs.setValue(com.aptana.ide.syncing.ui.preferences.IPreferenceConstants.DIRECTION_MODE,
						com.aptana.ide.syncing.ui.preferences.IPreferenceConstants.DIRECTION_BOTH);
				break;
			case DirectionToolBar.UPLOAD:
				prefs.setValue(com.aptana.ide.syncing.ui.preferences.IPreferenceConstants.DIRECTION_MODE,
						com.aptana.ide.syncing.ui.preferences.IPreferenceConstants.DIRECTION_UPLOAD);
				break;
			case DirectionToolBar.DOWNLOAD:
				prefs.setValue(com.aptana.ide.syncing.ui.preferences.IPreferenceConstants.DIRECTION_MODE,
						com.aptana.ide.syncing.ui.preferences.IPreferenceConstants.DIRECTION_DOWNLOAD);
				break;
			case DirectionToolBar.FORCE_UPLOAD:
				prefs.setValue(com.aptana.ide.syncing.ui.preferences.IPreferenceConstants.DIRECTION_MODE,
						com.aptana.ide.syncing.ui.preferences.IPreferenceConstants.DIRECTION_FORCE_UPLOAD);
				break;
			case DirectionToolBar.FORCE_DOWNLOAD:
				prefs.setValue(com.aptana.ide.syncing.ui.preferences.IPreferenceConstants.DIRECTION_MODE,
						com.aptana.ide.syncing.ui.preferences.IPreferenceConstants.DIRECTION_FORCE_DOWNLOAD);
		}
	}

	private static void saveDeleteLocalPreference(boolean selected)
	{
		getSyncingPreferenceStore().setValue(
				com.aptana.ide.syncing.ui.preferences.IPreferenceConstants.DELETE_LOCAL_FILES, selected);
	}

	private static void saveDeleteRemotePreference(boolean selected)
	{
		getSyncingPreferenceStore().setValue(
				com.aptana.ide.syncing.ui.preferences.IPreferenceConstants.DELETE_REMOTE_FILES, selected);
	}

	private static void saveShowModificationTimePref(boolean selected)
	{
		getSyncingPreferenceStore().setValue(
				com.aptana.ide.syncing.ui.preferences.IPreferenceConstants.SHOW_MODIFICATION_TIME, selected);
	}

	/**
	 * The custom tooltip class for the end point label.
	 */
	private class LabelToolTip extends ToolTip
	{

		public LabelToolTip(Control control)
		{
			super(control, ToolTip.NO_RECREATE, false);
		}

		@Override
		protected Composite createToolTipContentArea(Event event, Composite parent)
		{
			Composite contentArea = new Composite(parent, SWT.NONE);
			contentArea.setLayout(new GridLayout());

			StringBuilder buf = new StringBuilder();
			for (int i = 0; i < filesToBeSynced.length; ++i)
			{
				buf.append(EFSUtils.getRelativePath(sourceConnectionPoint, filesToBeSynced[i]));
				buf.append("\n"); //$NON-NLS-1$
			}
			Text text;
			GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, true);
			GC gc = new GC(contentArea);
			if (gc.textExtent(buf.toString()).y > 200)
			{
				// uses scrollbar when there are many files
				text = new Text(contentArea, SWT.MULTI | SWT.READ_ONLY | SWT.V_SCROLL);
				gridData.heightHint = 200;
			}
			else
			{
				text = new Text(contentArea, SWT.MULTI | SWT.READ_ONLY);
			}
			gc.dispose();
			text.setLayoutData(gridData);
			text.setText(buf.toString());

			return contentArea;
		}

	}

}
