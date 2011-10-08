package it.koen.dragnshare.net;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.*;

public class MulticastShare extends Thread
{
	public interface Listener {
		void onReceive(Receiver receiver);
	}

	public static final int MULTICASTPORT = 5432;
	public static final String GROUP = "224.0.13.37";
	public static final int PACKET_SIZE = 4096;

	private InetAddress group;
	private MulticastSocket multisocket;

	private Map<UUID, File> files;
	
	private final List<Listener> listeners;
	
	private final Set<File> filesCreated;

	public MulticastShare()
	{
		super("Multisharer");
		this.setDaemon(true);
		this.multisocket = null;
		this.files = Collections.synchronizedMap(new HashMap<UUID, File>());
		this.listeners = Collections.synchronizedList(new ArrayList<Listener>());
		this.filesCreated = Collections.synchronizedSet(new HashSet<File>());
		
		Runtime.getRuntime().addShutdownHook(new Thread(){
			@Override
			public void run()
			{
				for( File f : MulticastShare.this.filesCreated )
				{
					if( f.exists() )
						f.delete();
					f = f.getParentFile();
					if( f.exists() && f.isDirectory() && f.getName().startsWith("dragnshare") )
						f.delete();
				}
			}
		});
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
				try
				{
					r.connect();
					
					this.filesCreated.add(r.getTarget());
					List<Listener> listeners = new ArrayList<MulticastShare.Listener>(this.listeners);
					for (Listener listener:listeners)
						listener.onReceive(r);
							
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
	
	public void addMulticastListener(Listener listener){
		if (this.listeners.contains(listener))
			return;
		this.listeners.add(listener);
	}
	
	public boolean removeMulticastListener(Listener listener){
		return this.listeners.remove(listener);
	}

	public static void main(String[] args) throws Exception
	{
		System.out.println("MulticastShare test (files: " + args.length+")");
		MulticastShare dragnshare = new MulticastShare();
		dragnshare.addMulticastListener(new Listener() {
			
			@Override
			public void onReceive(Receiver receiver) {
				receiver.addCompletionListener(new Receiver.Adapter() {
					@Override
					public void onCompleted(File result, String filename, long filesize) {
						System.out.println("received " + result.getName());
						if (Desktop.isDesktopSupported()){
							try {
								Desktop.getDesktop().open(result);
							} catch (IOException ball) {
							}
						}
					}
				});
			}
		});
		
		dragnshare.connect();
		dragnshare.setDaemon(false); // Should be true if use irl.
		dragnshare.start();
		for (String s : args)
			dragnshare.share(new File(s));
	}

}
