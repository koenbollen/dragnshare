package nl.thanod.dragnshare.net;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.net.*;
import java.util.*;
import java.util.Map.Entry;

import nl.thanod.util.Settings;
import nl.thanod.util.Zipper;

import org.apache.commons.net.util.SubnetUtils;

public class MulticastShare extends Thread
{
	public interface Listener {
		void onReceive(Receiver receiver);
		void onSending(Sender sender);
		void onSent(Sender sender);
	}
	public static class Adapter implements Listener {
		@Override
		public void onReceive(Receiver receiver) {}
		@Override
		public void onSending(Sender sender) {}
		@Override
		public void onSent(Sender sender) {}
	}
	
	public static final int MULTICASTPORT = 5432;
	public static final String GROUP = "224.0.13.37";
	public static final int PACKET_SIZE = 4096;

	private InetAddress group;
	private MulticastSocket multisocket;

	private final Map<UUID, File> files;
	
	private final List<Listener> listeners;

	public MulticastShare()
	{
		super("Multisharer");
		this.setDaemon(true);
		this.multisocket = null;
		this.files = Collections.synchronizedMap(new HashMap<UUID, File>());
		this.listeners = Collections.synchronizedList(new ArrayList<Listener>());
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
				//System.err.println(m);
			} catch (Exception e)
			{
				e.printStackTrace();
				continue;
			}
			switch (m.getType())
			{
			case ACCEPT:
				if (this.files.containsKey(m.getID()))
				{
					Sender s = new Sender(this, this.files.get(m.getID()), packet.getAddress(), m.getPort());
					s.start();

					List<Listener> listeners = new ArrayList<MulticastShare.Listener>(this.listeners);
					for (Listener listener:listeners)
						listener.onSending(s);
				}
				break;
			case OFFER:
				if (this.files.containsKey(m.getID()))
					break;
				try
				{
					Receiver r = new Receiver( this, m, packet.getAddress() );
					
					long limit = Settings.instance.getInt("automaticDownloadLimit", 100) * 1024 * 1024;
					if( limit == 0 || m.getFilesize() <= limit )
						r.start();

					List<Listener> listeners = new ArrayList<MulticastShare.Listener>(this.listeners);
					for (Listener listener:listeners)
						listener.onReceive(r);
					
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
		DatagramPacket packet = new DatagramPacket(bytes, bytes.length, null, MULTICASTPORT);
		if( Settings.instance.getBool("bruteForceDiscover" ))
		{
			// TODO: Thread this? Or thread at a higher level when share( is called?
			for (NetworkInterface dev : Collections.list(NetworkInterface.getNetworkInterfaces()) ) {
				if(dev.isLoopback() || !dev.isUp())
					continue;
				
				for( InterfaceAddress addr : dev.getInterfaceAddresses() )
				{
					if( addr.getNetworkPrefixLength() != 24 )
						continue;
					if( !(addr.getAddress() instanceof Inet4Address) )
						continue;
					SubnetUtils.SubnetInfo info = new SubnetUtils(addr.getAddress().getHostAddress()+"/"+addr.getNetworkPrefixLength()).getInfo();
					for( String i : info.getAllAddresses() )
					{
						if( addr.getAddress().getHostAddress().equals(i) )
							continue;
						packet.setAddress(InetAddress.getByName(i));
						this.multisocket.send(packet);
					}
				}
			}
		}
		else
		{
			packet.setAddress(this.group);
			this.multisocket.send(packet);
		}
	}
	public void send(Message m, InetAddress addr) throws IOException
	{
		byte[] bytes = m.toString().getBytes();
		DatagramPacket packet = new DatagramPacket(bytes, bytes.length, addr, MULTICASTPORT);
		this.multisocket.send(packet);
	}

	public SendContainer share(final File file)
	{
		if( file.isDirectory() )
		{
			final SendContainer res = new SendContainer(this, file, null);
			Zipper z = new Zipper(file);
			z.addListener(new Zipper.Listener() {
				@Override
				public void finished(File directory, File zipfile)
				{
					UUID id = MulticastShare.this.doShare(zipfile, true);
					res.setID(id);
				}
			});
			return res;
		} 
		UUID id = this.doShare(file, false);
		return new SendContainer(this, file, id);
	}
	
	protected UUID doShare(File file, boolean dir)
	{
		try
		{
			Message m = new Message(file, dir);
			this.files.put(m.getID(), file);
			this.send(m);
			return m.getID();
		} catch (IOException e)
		{
			e.printStackTrace();
		}
		return null;
	}
	
	public void addMulticastListener(Listener listener){
		if (listener == null || this.listeners.contains(listener))
			return;
		this.listeners.add(listener);
	}
	
	public boolean removeMulticastListener(Listener listener){
		return this.listeners.remove(listener);
	}

	protected void fireSent(Sender s)
	{
		List<Listener> listeners = new ArrayList<MulticastShare.Listener>(this.listeners);
		for (Listener listener:listeners)
			listener.onSent(s);
	}

	public static void main(String[] args) throws Exception
	{
		System.out.println("MulticastShare test (files: " + args.length+")");
		MulticastShare dragnshare = new MulticastShare();
		dragnshare.addMulticastListener(new Adapter() {

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
		dragnshare.setDaemon(false);
		dragnshare.start();
		for (String s : args)
			dragnshare.share(new File(s));
	}

	public void cancel(File file)
	{
		if( this.files.containsValue(file) )
		{
			Iterator<Entry<UUID, File>> it = this.files.entrySet().iterator();
			while(it.hasNext())
			{
				Entry<UUID, File> e = it.next();
				if( e.getValue().equals(file) )
					it.remove();
			}
		}
	}

	public void cancel(UUID id)
	{
		this.files.remove(id);
	}
}
