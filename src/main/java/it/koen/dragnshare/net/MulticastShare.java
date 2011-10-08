package it.koen.dragnshare.net;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.HashMap;
import java.util.UUID;

public class MulticastShare extends Thread implements Receiver.CompletionListener
{

	public static final int MULTICASTPORT = 5432;
	public static final String GROUP = "224.0.13.37";
	public static final int PACKET_SIZE = 4096;

	private InetAddress group;
	private MulticastSocket multisocket;

	private HashMap<UUID, File> files;

	public MulticastShare()
	{
		this.files = new HashMap<UUID, File>();
		this.multisocket = null;
	}

	public void connect() throws IOException
	{
		if (this.multisocket != null)
		{
			throw new RuntimeException("Already connected");
		}
		this.multisocket = new MulticastSocket(MULTICASTPORT);
		this.group = InetAddress.getByName(GROUP);
		this.multisocket.joinGroup(this.group);
	}

	@Override
	public void run()
	{
		if (this.multisocket == null)
			throw new RuntimeException("Connect before starting thread.");
		byte[] buffer = new byte[PACKET_SIZE];
		DatagramPacket packet = new DatagramPacket(buffer, PACKET_SIZE);
		Message m = null;
		while (true)
		{
			try
			{
				this.multisocket.receive(packet);
				m = Message.parse(new String(packet.getData(), packet.getOffset(), packet.getLength()));
			} catch (Exception e)
			{
				continue;
			}
			switch (m.getType())
			{
			case ACCEPT:
				if (this.files.containsKey(m.getID()))
				{
					new Sender(this.files.get(m.getID()), packet.getAddress(), m.getPort()).start();
				}
				break;
			case OFFER:
				if (this.files.containsKey(m.getID()))
					break;
				Receiver r = new Receiver(m.getID(), m.getFilename(), m.getFilesize(), null);
				r.addCompletionListener(this);
				try
				{
					r.connect();
					int port = r.getLocalPort();
					r.start();
					this.send(new Message(m.getID(), port));
				} catch (IOException e)
				{
					e.printStackTrace();
				}
				break;
			}
		}
	}

	public void send(Message m) throws IOException
	{
		byte[] bytes = m.toString().getBytes();
		DatagramPacket packet = new DatagramPacket(bytes, bytes.length, this.group, MULTICASTPORT);
		this.multisocket.send(packet);
	}

	public void share(File file)
	{
		try
		{
			Message m = new Message(file);
			this.files.put(m.getID(), file);
			this.send(m);
		} catch (IOException e)
		{
			e.printStackTrace();
			// TODO: Error handling.
		}

	}

	@Override
	public void onCompleted(File result, String filename, long filesize)
	{
		System.out.println("File recevied: " + filename);
		if (Desktop.isDesktopSupported())
		{
			Desktop d = Desktop.getDesktop();
			try
			{
				d.open(result);
			} catch (IOException e)
			{
			}
		}
	}

	public static void main(String[] args) throws Exception
	{
		System.out.println("MulticastShare test (files: " + args.length+")");
		MulticastShare dragnshare = new MulticastShare();
		dragnshare.connect();
		dragnshare.setDaemon(false); // Should be true if use irl.
		dragnshare.start();
		for (String s : args)
			dragnshare.share(new File(s));
	}

}
