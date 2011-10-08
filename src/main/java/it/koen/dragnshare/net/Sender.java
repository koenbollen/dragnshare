package it.koen.dragnshare.net;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;

public class Sender extends Thread
{

	public static final int BUFFERSIZE = 4096;

	private final File file;
	private final InetAddress addr;
	private final int port;

	public Sender(File file, InetAddress addr, int port)
	{
		this.setDaemon(true);
		this.file = file;
		this.addr = addr;
		this.port = port;
	}

	@Override
	public void run()
	{

		Socket s = null;
		InputStream in = null;
		try
		{
			int n;
			byte[] buffer = new byte[BUFFERSIZE];

			s = new Socket(addr, port);

			in = new FileInputStream(this.file);
			OutputStream out = s.getOutputStream();

			do
			{
				n = in.read(buffer, 0, BUFFERSIZE);
				if (n > 0)
				{
					out.write(buffer, 0, n);
				}
			} while (n > 0);

		} catch (Exception e)
		{
			e.printStackTrace();
		} finally
		{
			try
			{
				if (s != null)
					s.close();
			} catch (IOException e)
			{
			}
			try
			{
				if (in != null)
					in.close();
			} catch (IOException e)
			{

			}
		}
	}
}
