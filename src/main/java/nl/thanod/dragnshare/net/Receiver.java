package nl.thanod.dragnshare.net;

import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import nl.thanod.util.FileUtils;

public class Receiver extends Thread
{

	public static final int BUFFERSIZE = 4096;

	public interface Listener
	{
		void onStart(File result, String filename, long filesize);
		void onProgress(File result, String filename, long filesize, long received);
		void onCompleted(File result, String filename, long filesize);
		void onError(File target, String filename, long filesize, IOException e);
	}
	
	public static abstract class Adapter implements Listener
	{

		/* (non-Javadoc)
		 * @see it.koen.dragnshare.net.Receiver.Listener#onStart(java.io.File, java.lang.String, long)
		 */
		@Override
		public void onStart(File result, String filename, long filesize) {}

		/* (non-Javadoc)
		 * @see it.koen.dragnshare.net.Receiver.Listener#onProgress(java.io.File, java.lang.String, long, long)
		 */
		@Override
		public void onProgress(File result, String filename, long filesize, long received) {}

		/* (non-Javadoc)
		 * @see it.koen.dragnshare.net.Receiver.Listener#onCompleted(java.io.File, java.lang.String, long)
		 */
		@Override
		public void onCompleted(File result, String filename, long filesize) {}

		/* (non-Javadoc)
		 * @see it.koen.dragnshare.net.Receiver.Listener#onError(java.io.File, java.lang.String, long, java.io.IOException)
		 */
		@Override
		public void onError(File target, String filename, long filesize, IOException e) {}
		
	}

	public enum Status
	{
		STARTING, LISTENING, TRANSFERRING, UNPACKING, COMPLETED, FAILED;
	}

	private final Message message;
	private final InetAddress addr;
	private final MulticastShare parent;
	private File target;
	private File result;
	private ServerSocket s;
	private volatile Status currentStatus;
	private final List<Listener> listeners;
	private boolean started;

	public Receiver( MulticastShare parent, Message message, InetAddress addr ) throws IOException
	{
		this.setDaemon(true);
		this.message = message;
		this.addr = addr;
		this.parent = parent;
		this.currentStatus = Status.STARTING;
		this.listeners = Collections.synchronizedList(new ArrayList<Receiver.Listener>());

		this.target = FileUtils.createDragnShareFile(this.message.getFilename());
		if( this.message.isDirectory() )
		{
			String name = this.message.getFilename();
			name = name.substring(0, name.length()-4);
			this.result = FileUtils.createDragnShareDir( name );
		}
		else
			this.result = this.target;
		this.started = false;

		this.s = null;
	}
	
	@Override
	public synchronized void start()
	{
		if( this.started )
			return;
		this.started = true;

		try
		{
			this.s = new ServerSocket(0, 1);
		} catch (IOException e1)
		{
			e1.printStackTrace();
			return;
		}
		super.start();
		try
		{
			this.parent.send(new Message(this.message.getID(), this.getLocalPort()), this.addr);
		} catch(IOException e )
		{
			e.printStackTrace();
			this.interrupt();
		}
	}

	@Override
	public void run()
	{
		if (this.s == null)
			throw new RuntimeException("no socket at run");
		this.currentStatus = Status.LISTENING;
		String filename = this.message.getFilename();
		long filesize = this.message.getFilesize();
		Socket client = null;
		InputStream in = null;
		OutputStream out = null;
		try
		{
			int n = 0;
			long count = 0;
			byte[] buffer = new byte[BUFFERSIZE + 1];
			client = this.s.accept();
			in = client.getInputStream();
			out = new FileOutputStream(this.target);

			List<Listener> listeners = new ArrayList<Receiver.Listener>(this.listeners);
			for (Listener listener:listeners)
				listener.onStart(this.result, filename, filesize);
			
			this.currentStatus = Status.TRANSFERRING;
			do
			{
				n = in.read(buffer, 0, BUFFERSIZE);
				if (n > 0)
				{
					count += n;
					out.write(buffer, 0, n);
					
					// notify listeners of received content
					listeners = new ArrayList<Receiver.Listener>(this.listeners);
					for (Listener listener:listeners)
						listener.onProgress(this.result, filename, filesize, count);
				}
			} while (n > 0 && !Thread.interrupted());

			if (count != filesize)
				throw new IOException("didn't receive enough bytes (expected "+filesize+" bytes, got "+count+").");

			if( this.message.isDirectory() )
			{
				this.currentStatus = Status.UNPACKING;
				unzip();
			}
			
			this.currentStatus = Status.COMPLETED;

			listeners = new ArrayList<Receiver.Listener>(this.listeners);
			for (Listener listener:listeners)
				listener.onCompleted(this.result, filename, filesize);

		} catch (IOException e)
		{
			e.printStackTrace();
			this.currentStatus = Status.FAILED;

			List<Listener> listeners = new ArrayList<Receiver.Listener>(this.listeners);
			for (Listener listener:listeners)
				listener.onError(this.result, filename, filesize, e);
					
		} finally
		{
			try
			{
				if (this.s != null)
					this.s.close();
			} catch (IOException e)
			{
			}
			try
			{
				if (out != null)
					out.close();
			} catch (IOException e)
			{

			}
		}
	}

	private void unzip() throws ZipException, IOException, FileNotFoundException
	{
		int n;
		InputStream in = null;
		OutputStream out = null;
		byte[] buffer = new byte[BUFFERSIZE + 1];
		ZipFile zip = new ZipFile(this.target);
		try
		{
			Enumeration<? extends ZipEntry> entries = zip.entries();
	
			while(entries.hasMoreElements())
			{
				ZipEntry entry = entries.nextElement();
	
				try
				{
					in = zip.getInputStream(entry);
					File f = new File( this.result.getParentFile(), entry.getName() );
					f.getParentFile().mkdirs();
					f.createNewFile();
					out = new FileOutputStream(f);
					do
					{
						n = in.read(buffer, 0, BUFFERSIZE);
						if (n > 0)
							out.write(buffer, 0, n);
					} while (n > 0 && !Thread.interrupted());
				} finally {
					if( in != null )
						in.close();
					if( out != null )
						out.close();
				}
			}
		} finally
		{
			zip.close();
		}
	}

	public void addCompletionListener(Receiver.Listener listener)
	{
		if (listener != null && !this.listeners.contains(listener))
			this.listeners.add(listener);
	}

	public boolean removeCompletionListener(Listener listener)
	{
		return this.listeners.remove(listener);
	}

	public int getLocalPort()
	{
		if (this.s == null)
			throw new RuntimeException("connect() before getting port.");
		return this.s.getLocalPort();
	}

	public Status getCurrentStatus()
	{
		return this.currentStatus;
	}

	public boolean isCompleted()
	{
		return this.currentStatus == Status.COMPLETED;
	}
	
	public UUID getMessageID()
	{
		return this.message.getID();
	}

	public File getTarget() {
		return this.result;
	}

	public String getFilename() {
		return this.message.getFilename();
	}

	public long getFilesize() {
		return this.message.getFilesize();
	}
	
	public boolean isStarted()
	{
		return this.started;
	}
}
