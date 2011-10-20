/**
 * 
 */
package nl.thanod.dragnshare.net;

import java.io.File;
import java.io.IOException;
import java.net.*;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import nl.thanod.util.Settings;

import org.apache.commons.net.util.SubnetUtils;

/**
 * @author nilsdijk
 */
public class MulticastSharer implements Sharer, Runnable {

	public static final String GROUP = "224.0.13.37";

	public static final int BUFFER_SIZE = 4096;

	public static int MULTICAST_PORT = 3724;

	public final int port;
	
	private final Map<UUID, MulticastSender> map = Collections.synchronizedMap(new HashMap<UUID, MulticastSender>());

	private MulticastSocket socket = null;
	private InetAddress group;
	
	public final Sharer.Listeners listeners = new Sharer.Listeners();


	public MulticastSharer() {
		this(MulticastSharer.MULTICAST_PORT);
	}

	public MulticastSharer(int port) {
		this.port = port;
	}

	public void connect() throws IOException {
		this.group = InetAddress.getByName(MulticastSharer.GROUP);
		this.socket = new MulticastSocket(this.port);
		this.socket.joinGroup(this.group);
	}

	/*
	 * (non-Javadoc)
	 * @see nl.thanod.dragnshare.net.Sharer#share(java.io.File)
	 */
	@Override
	public MulticastSender share(File file) throws IOException {
		Message m = Message.file(file);
		MulticastSender s = new MulticastSender(this, file, m);
		this.map.put(m.id, s);
		try {
			this.send(m, null);
		} catch (IOException ball) {
			this.map.remove(m.id);
			throw ball;
		}
		return s;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run() {		
		if (this.socket == null)
			throw new IllegalStateException("Not connected");
		
		byte[] buffer = new byte[MulticastSharer.BUFFER_SIZE];
		DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
		
		while (true){
			Message m;
			try {
				this.socket.receive(packet);
				m = Message.parse(new String(packet.getData(), packet.getOffset(), packet.getLength()));
			} catch (Throwable ball) {
				ball.printStackTrace();
				continue;
			}
			
			switch(m.type){
				case ACCEPT:
					this.map.get(m.id).process(packet.getAddress(), m.port);
					break;
				case FILE:
				case DIRECTORY:
					if (this.map.containsKey(m.id))
						break;
					try {
						MulticastReceiver r = new MulticastReceiver(this, packet.getAddress(), m);
						this.listeners.offered(r);
					} catch (IOException ball) {
						ball.printStackTrace();
					}
					break;
			}
		}
	}

	/**
	 * @param m
	 * @throws IOException 
	 */
	public void send(Message m, InetAddress target) throws IOException {
		byte[] bytes = m.toString().getBytes();
		DatagramPacket packet = new DatagramPacket(bytes, bytes.length, null, this.port);
		
		if (target != null)
		{
			packet.setAddress(target);
			this.socket.send(packet);
		}
		else if( Settings.instance.getBool("bruteForceDiscover" ))
		{
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
						this.socket.send(packet);
					}
				}
			}
		}
		else
		{
			packet.setAddress(this.group);
			this.socket.send(packet);
		}
	}
	
	@Override
	public Thread start() throws IOException{
		this.connect();
		
		Thread t = new Thread(this, "Multicast Sharer");
		t.setDaemon(true);
		t.start();
		return t;
	}
	
	@Override
	public Listeners listeners(){
		return this.listeners;
	}
}
