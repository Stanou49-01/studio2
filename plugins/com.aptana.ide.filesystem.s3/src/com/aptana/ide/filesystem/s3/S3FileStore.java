package com.aptana.ide.filesystem.s3;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileInfo;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.filesystem.provider.FileInfo;
import org.eclipse.core.filesystem.provider.FileStore;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;

import com.amazon.s3.AWSAuthConnection;
import com.amazon.s3.ListBucketResponse;
import com.amazon.s3.ListEntry;
import com.amazon.s3.Response;

public class S3FileStore extends FileStore
{

	private static final String FOLDER_SUFFIX = "_$folder$";
	private URI uri;
	private Path path;

	public S3FileStore(URI uri)
	{
		this.uri = uri;
		this.path = new Path(uri.getPath().replaceAll("%2F", "/"));
	}

	@Override
	public String[] childNames(int options, IProgressMonitor monitor) throws CoreException
	{
		return childNames(options, false, monitor);
	}

	public String[] childNames(int options, boolean includeHackFolderFiles, IProgressMonitor monitor)
			throws CoreException
	{
		try
		{
			String prefix = getPrefix();
			List<ListEntry> entries = listEntries();
			List<String> keys = new ArrayList<String>();

			for (ListEntry entry : entries)
			{
				if (prefix.length() >= entry.key.length())
					continue;
				try
				{
					String relative = entry.key.substring(prefix.length());
					if (prefix.length() == 0 || relative.startsWith("/"))
					{ // actual children
						if (prefix.length() > 0)
							relative = relative.substring(1);
						// only add direct children (so take up to next path separator)
						int index = relative.indexOf("/");
						if (index != -1)
						{
							relative = relative.substring(0, index);
						}
						else if (relative.endsWith(FOLDER_SUFFIX))
							relative = relative.substring(0, relative.length() - FOLDER_SUFFIX.length());
					}
					else
					{
						// file at same level (peer, not child), check just for the _$folder$ hack
						if (relative.equals(FOLDER_SUFFIX))
						{
							if (!includeHackFolderFiles)
								continue;
						}
						else
							continue;
					}
					if (relative.length() == 0)
						continue;
					if (!keys.contains(relative))
						keys.add(relative);
				}
				catch (Exception e)
				{
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			return keys.toArray(new String[keys.size()]);
		}
		catch (MalformedURLException e)
		{
			throw new CoreException(new Status(IStatus.ERROR, Activator.PLUGIN_ID, -1, e.getMessage(), e));
		}
		catch (IOException e)
		{
			throw new CoreException(new Status(IStatus.ERROR, Activator.PLUGIN_ID, -1, e.getMessage(), e));
		}
	}

	private String getPrefix()
	{
		String prefix = path.toPortableString();
		if (prefix.startsWith("/"))
		{
			prefix = prefix.substring(1);
		}
		return prefix;
	}

	@Override
	public IFileInfo fetchInfo(int options, IProgressMonitor monitor) throws CoreException
	{
		FileInfo info = new FileInfo(getName());
		try
		{
			HttpURLConnection connection = getAWSConnection().head(getBucket(), getKey(), null);
			if (connection.getResponseCode() < 400)
			{
				info.setExists(true);
				info.setDirectory(false);
				String length = connection.getHeaderField("Content-Length");
				if (length != null)
					info.setLength(Long.parseLong(length));
				try
				{
					String lastModified = connection.getHeaderField("Last-Modified");
					if (lastModified != null)
					{
						Date date = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss z").parse(lastModified);
						info.setLastModified(date.getTime());
					}
				}
				catch (ParseException e)
				{
					// ignore
				}
			}
			else
			{
				// Only "exists" if there's any children! There are no "directories" in S3. Make sure not to filter out
				// the _$folder$ hacks for this
				String[] children = childNames(options, true, monitor);
				if (children != null && children.length > 0)
				{
					info.setDirectory(true);
					info.setExists(true);
				}
			}
		}
		catch (MalformedURLException e)
		{
			throw new CoreException(new Status(IStatus.ERROR, Activator.PLUGIN_ID, -1, e.getMessage(), e));
		}
		catch (IOException e)
		{
			throw new CoreException(new Status(IStatus.ERROR, Activator.PLUGIN_ID, -1, e.getMessage(), e));
		}
		return info;
	}

	@Override
	public IFileStore getChild(String name)
	{
		try
		{
			IPath childPath = path.append(name);
			return new S3FileStore(getURI(childPath));
		}
		catch (URISyntaxException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	private URI getURI(IPath childPath) throws URISyntaxException
	{
		return new URI(uri.getScheme(), uri.getUserInfo(), uri.getHost(), uri.getPort(), childPath.toPortableString(),
				uri.getQuery(), uri.getFragment());
	}

	@Override
	public String getName()
	{
		return path.lastSegment();
	}

	@Override
	public IFileStore getParent()
	{
		if (path.segmentCount() == 0)
			return null;
		try
		{
			IPath parentPath = path.removeLastSegments(1);
			return new S3FileStore(getURI(parentPath));
		}
		catch (URISyntaxException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public InputStream openInputStream(int options, IProgressMonitor monitor) throws CoreException
	{
		try
		{
			HttpURLConnection connection = getAWSConnection().getRaw(getBucket(), getKey(), null);
			return connection.getInputStream();
		}
		catch (MalformedURLException e)
		{
			throw new CoreException(new Status(IStatus.ERROR, Activator.PLUGIN_ID, -1, e.getMessage(), e));
		}
		catch (IOException e)
		{
			throw new CoreException(new Status(IStatus.ERROR, Activator.PLUGIN_ID, -1, e.getMessage(), e));
		}
	}

	private AWSAuthConnection getAWSConnection()
	{
		if (getBucket().indexOf(".") != -1)
			return new AWSAuthConnection(getAccessKey(), getSecretAccessKey(), false);
		return new AWSAuthConnection(getAccessKey(), getSecretAccessKey());
	}

	private String getSecretAccessKey()
	{
		return uri.getUserInfo().split(":")[1];
	}

	private String getAccessKey()
	{
		return uri.getUserInfo().split(":")[0];
	}

	String getKey()
	{
		String key = path.toPortableString();
		if (key.startsWith("/") && key.length() > 1)
			return key.substring(1);
		return key;
	}

	private String getBucket()
	{
		return uri.getHost();
	}

	@Override
	public URI toURI()
	{
		return uri;
	}

	@Override
	public void delete(int options, IProgressMonitor monitor) throws CoreException
	{
		try
		{
			Response resp = getAWSConnection().delete(getBucket(), getKey(), null);
			resp.connection.getResponseCode(); // force connection to finish

			// Handle if we're faking a folder. try to delete the fake folder suffix file.
			resp = getAWSConnection().delete(getBucket(), getKey() + FOLDER_SUFFIX, null);
			resp.connection.getResponseCode(); // force connection to finish
		}
		catch (MalformedURLException e)
		{
			throw new CoreException(new Status(IStatus.ERROR, Activator.PLUGIN_ID, -1, e.getMessage(), e));
		}
		catch (IOException e)
		{
			throw new CoreException(new Status(IStatus.ERROR, Activator.PLUGIN_ID, -1, e.getMessage(), e));
		}
	}

	@Override
	public OutputStream openOutputStream(int options, IProgressMonitor monitor) throws CoreException
	{
		try
		{
			HttpURLConnection connection = getAWSConnection().putRaw(getBucket(), getKey(), null);
			return new HttpForcingOutputStream(connection.getOutputStream(), connection);
		}
		catch (MalformedURLException e)
		{
			throw new CoreException(new Status(IStatus.ERROR, Activator.PLUGIN_ID, -1, e.getMessage(), e));
		}
		catch (IOException e)
		{
			throw new CoreException(new Status(IStatus.ERROR, Activator.PLUGIN_ID, -1, e.getMessage(), e));
		}
	}

	@Override
	public IFileStore mkdir(int options, IProgressMonitor monitor) throws CoreException
	{
		try
		{
			HttpURLConnection connection = getAWSConnection().putRaw(getBucket(), getKey() + FOLDER_SUFFIX, null);
			connection.getOutputStream().write(new byte[] {});
			connection.getResponseCode();
		}
		catch (MalformedURLException e)
		{
			throw new CoreException(new Status(IStatus.ERROR, Activator.PLUGIN_ID, -1, e.getMessage(), e));
		}
		catch (IOException e)
		{
			throw new CoreException(new Status(IStatus.ERROR, Activator.PLUGIN_ID, -1, e.getMessage(), e));
		}
		return this;
	}

	@Override
	public void putInfo(IFileInfo info, int options, IProgressMonitor monitor) throws CoreException
	{
		// TODO Need impl to be able to say we handle writes
		super.putInfo(info, options, monitor);
	}

	@Override
	public File toLocalFile(int options, IProgressMonitor monitor) throws CoreException
	{
		SubMonitor sub = SubMonitor.convert(monitor, 100);
		if (options == EFS.CACHE)
		{
			try
			{
				IFileInfo myInfo = fetchInfo(EFS.NONE, sub.newChild(25));
				File result;
				if (!myInfo.exists())
					result = File.createTempFile("Non-Existent-", Long.toString(System.currentTimeMillis())); //$NON-NLS-1$
				else
				{
					if (myInfo.isDirectory())
					{
						File tmpDir = new File(System.getProperty("java.io.tmpdir"));
						result = getUniqueDirectory(tmpDir);
					}
					else
					{
						result = File.createTempFile("s3file", "efs"); //$NON-NLS-1$ //$NON-NLS-2$
					}
					sub.worked(25);
					IFileStore resultStore = EFS.getLocalFileSystem().fromLocalFile(result);
					copy(resultStore, EFS.OVERWRITE, sub.newChild(25));
				}
				result.deleteOnExit();
				return result;
			}
			catch (IOException e)
			{
				throw new CoreException(new Status(IStatus.ERROR, Activator.PLUGIN_ID, EFS.ERROR_WRITE,
						"Could not write file", e));
			}
		}
		return super.toLocalFile(options, monitor);
	}

	private File getUniqueDirectory(File parent)
	{
		File dir;
		long i = 0;
		// find an unused directory name
		do
		{
			dir = new File(parent, Long.toString(System.currentTimeMillis() + i++));
		}
		while (dir.exists());
		return dir;
	}

	@Override
	protected void copyFile(IFileInfo sourceInfo, IFileStore destination, int options, IProgressMonitor monitor)
			throws CoreException
	{
		// if we're copying from S3 to S3 we can do so using S3's special copy API shortcut!
		if (destination instanceof S3FileStore)
		{
			S3FileStore s3Dest = (S3FileStore) destination;
			try
			{
				getAWSConnection().copy(getBucket(), getKey(), s3Dest.getBucket(), s3Dest.getKey(), null);
			}
			catch (MalformedURLException e)
			{
				throw new CoreException(new Status(IStatus.ERROR, Activator.PLUGIN_ID, EFS.ERROR_INTERNAL, e
						.getMessage(), e));
			}
			catch (IOException e)
			{
				throw new CoreException(new Status(IStatus.ERROR, Activator.PLUGIN_ID, EFS.ERROR_INTERNAL, e
						.getMessage(), e));
			}
		}
		else
			super.copyFile(sourceInfo, destination, options, monitor);
	}

	private static class HttpForcingOutputStream extends OutputStream
	{

		private OutputStream out;
		private HttpURLConnection connection;

		HttpForcingOutputStream(OutputStream out, HttpURLConnection connection)
		{
			this.out = out;
			this.connection = connection;
		}

		@Override
		public void write(int b) throws IOException
		{
			out.write(b);
		}

		@Override
		public void flush() throws IOException
		{
			out.flush();
		}

		@Override
		public void close() throws IOException
		{
			out.close();
			connection.getResponseCode(); // force the connection to finish!
		}

		@Override
		public void write(byte[] b, int off, int len) throws IOException
		{
			out.write(b, off, len);
		}

		@Override
		public void write(byte[] b) throws IOException
		{
			out.write(b);
		}
	}

	@SuppressWarnings("unchecked")
	List<ListEntry> listEntries() throws MalformedURLException, IOException
	{
		ListBucketResponse resp = getAWSConnection().listBucket(getBucket(), getPrefix(), null, null, null);
		return resp.entries;
	}
}
