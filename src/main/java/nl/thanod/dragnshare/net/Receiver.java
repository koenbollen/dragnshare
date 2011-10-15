package nl.thanod.dragnshare.net;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

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
		STARTING, LISTENING, TRANSFERRING, COMPLETED, FAILED;
	}

	private ServerSocket s;
	private File target;
	private String filename;
	private long filesize;
	private volatile Status currentStatus;
	private final List<Listener> listeners;

	public Receiver(UUID id, String filename, long filesize, File target)
	{
		this.setDaemon(true);
		this.target = target;
		this.filename = filename;
		this.filesize = filesize;
		this.currentStatus = Status.STARTING;
		this.listeners = Collections.synchronizedList(new ArrayList<Receiver.Listener>());

		this.s = null;
	}

	public void connect() throws IOException
	{
		if (this.target == null)
		{
			File dir = File.createTempFile("dragnshare", "");
			dir.delete();
			dir.mkdirs();
			this.target = new File(dir, this.filename);
			this.target.createNewFile();
		}
		this.s = new ServerSocket(0, 1);
	}

	@Override
	public void run()
	{
		if (this.s == null)
			throw new RuntimeException("connect() before start()ing thread.");
		this.currentStatus = Status.LISTENING;
		Socket client = null;
		OutputStream out = null;
		try
		{
			int n = 0, count = 0;
			byte[] buffer = new byte[BUFFERSIZE + 1];
			client = this.s.accept();
			InputStream in = client.getInputStream();
			out = new FileOutputStream(this.target);

			List<Listener> listeners = new ArrayList<Receiver.Listener>(this.listeners);
			for (Listener listener:listeners)
				listener.onStart(this.target, this.filename, this.filesize);
			
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
						listener.onProgress(this.target, this.filename, this.filesize, count);
				}
			} while (n > 0);

			if (count != this.filesize)
				throw new IOException("didn't receive enough bytes.");

			this.currentStatus = Status.COMPLETED;

			listeners = new ArrayList<Receiver.Listener>(this.listeners);
			for (Listener listener:listeners)
				listener.onCompleted(this.target, this.filename, this.filesize);

		} catch (IOException e)
		{
			this.currentStatus = Status.FAILED;
			
			List<Listener> listeners = new ArrayList<Receiver.Listener>(this.listeners);
			for (Listener listener:listeners)
				listener.onError(this.target, this.filename, this.filesize, e);
					
			e.printStackTrace();
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

	public void addCompletionListener(Receiver.Listener listener)
	{
		if (!this.listeners.contains(listener))
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
	
	public File getTarget() {
		return this.target;
	}

	public String getFilename() {
		return this.filename;
	}

	public long getFilesize() {
		return this.filesize;
	}
}