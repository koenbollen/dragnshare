package it.koen.dragnshare.net;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Receiver extends Thread
{

	public static final int BUFFERSIZE = 4096;

	public interface CompletionListener
	{
		void onCompleted(File result, String filename, long filesize);
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
	private List<CompletionListener> completionListeners;

	public Receiver(UUID id, String filename, long filesize, File target)
	{
		this.setDaemon(true);
		this.target = target;
		this.filename = filename;
		this.filesize = filesize;
		this.currentStatus = Status.STARTING;
		this.completionListeners = new ArrayList<Receiver.CompletionListener>();

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

			currentStatus = Status.TRANSFERRING;
			do
			{
				n = in.read(buffer, 0, BUFFERSIZE);
				if (n > 0)
				{
					count += n;
					out.write(buffer, 0, n);
				}
			} while (n > 0);

			if (count != this.filesize)
				throw new IOException("didn't receive enough bytes.");

			this.currentStatus = Status.COMPLETED;

			for (CompletionListener l : this.completionListeners)
				l.onCompleted(target, filename, this.filesize);

		} catch (IOException e)
		{
			this.currentStatus = Status.FAILED;
			e.printStackTrace();
		}
	}

	public void addCompletionListener(CompletionListener listener)
	{
		if (!this.completionListeners.contains(listener))
			this.completionListeners.add(listener);
	}

	public boolean removeCompletionListener(CompletionListener listener)
	{
		return this.completionListeners.remove(listener);
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
}
