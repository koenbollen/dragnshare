package it.koen.dragnshare.net;

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
		if (target == null)
		{
			File dir = File.createTempFile("dragnshare", "");
			dir.delete();
			dir.mkdirs();
			target = new File(dir, this.filename);
			target.createNewFile();
		}
		s = new ServerSocket(0, 1);
	}

	@Override
	public void run()
	{
		if (s == null)
			throw new RuntimeException("connect() before start()ing thread.");
		this.currentStatus = Status.LISTENING;
		try
		{
			int n = 0, count = 0;
			byte[] buffer = new byte[BUFFERSIZE + 1];
			Socket client = s.accept();
			InputStream in = client.getInputStream();
			OutputStream out = new FileOutputStream(this.target);

			List<Listener> listeners = new ArrayList<Receiver.Listener>(this.listeners);
			for (Listener listener:listeners)
				listener.onStart(this.target,filename, this.filesize);
			
			currentStatus = Status.TRANSFERRING;
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
						listener.onProgress(this.target,filename, this.filesize, count);
				}
			} while (n > 0);

			if (count != this.filesize)
				throw new IOException("didn't receive enough bytes.");

			this.currentStatus = Status.COMPLETED;

			listeners = new ArrayList<Receiver.Listener>(this.listeners);
			for (Listener listener:listeners)
				listener.onCompleted(target, filename, this.filesize);

		} catch (IOException e)
		{
			this.currentStatus = Status.FAILED;
			
			List<Listener> listeners = new ArrayList<Receiver.Listener>(this.listeners);
			for (Listener listener:listeners)
				listener.onError(target, filename, this.filesize, e);
					
			e.printStackTrace();
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
		if (s == null)
			throw new RuntimeException("connect() before getting port.");
		return s.getLocalPort();
	}

	public Status getCurrentStatus()
	{
		return this.currentStatus;
	}

	public File getFile()
	{
		return this.target;
	}

	public boolean isCompleted()
	{
		return this.currentStatus == Status.COMPLETED;
	}
	
	public File getTarget() {
		return target;
	}

	public String getFilename() {
		return filename;
	}

	public long getFilesize() {
		return filesize;
	}
}
