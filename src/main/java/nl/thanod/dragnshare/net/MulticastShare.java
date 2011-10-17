package nl.thanod.dragnshare.net;

import java.io.File;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import nl.thanod.util.Settings;

import org.apache.commons.net.util.SubnetUtils;

public class MulticastShare extends Thread
{
	public interface Listener {
		public void onAvailable( UUID id, AvailableFile info );
		void onReceive(Receiver receiver);
		void onSending(Sender sender);
		void onSent(Sender sender);
	}
	public static class Adapter implements Listener {
		@Override
		public void onAvailable( UUID id, AvailableFile info ) {}
		@Override
		public void onReceive(Receiver receiver) {}
		@Override
		public void onSending(Sender sender) {}
		@Override
		public void onSent(Sender sender) {}
	}

	public class AvailableFile 
	{
		public final Message message;
		public final InetAddress address;
		public AvailableFile(Message message, InetAddress address)
		{
			super();
			this.message = message;
			this.address = address;
		}
	}
	
	public static final int MULTICASTPORT = 5432;
	public static final String GROUP = "224.0.13.37";
	public static final int PACKET_SIZE = 4096;

	private InetAddress group;
	private MulticastSocket multisocket;

	private final Map<UUID, File> files;
	private final Map<UUID, AvailableFile> available;
	
	private final List<Listener> listeners;
	
	protected final Set<File> filesCreated;

	public MulticastShare()
	{
		super("Multisharer");
		this.setDaemon(true);
		this.multisocket = null;
		this.files = Collections.synchronizedMap(new HashMap<UUID, File>());
		this.available = Collections.synchronizedMap(new HashMap<UUID, AvailableFile>());
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
				//System.out.println(m);
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
				long limit = Settings.instance.getInt("automaticDownloadLimit", 100) * 1024 * 1024;
				if( limit != 0 && m.getFilesize() > limit )
				{
					AvailableFile info = new AvailableFile(m, packet.getAddress() );
					this.available.put( m.getID(), info );
					List<Listener> listeners = new ArrayList<MulticastShare.Listener>(this.listeners);
					for (Listener listener:listeners)
						listener.onAvailable(m.getID(), info);
				} else {
					startReceiving(m, packet.getAddress());
				}
				break;
			}
		}
	}

	private void startReceiving(Message m, InetAddress addr)
	{
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
			this.send(new Message(m.getID(), port), addr);
			
		} catch (IOException e)
		{
			e.printStackTrace();
		}
	}
	
	public void accept( UUID id )
	{
		if( !this.available.containsKey(id) )
			return;
		AvailableFile a = this.available.get(id);
		this.startReceiving( a.message, a.address );
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
		}

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

}
