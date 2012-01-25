package nl.thanod.dragnshare.net;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.UUID;

public class YellowPages implements Iterable<InetAddress>
{
	private Set<InetAddress> network = new HashSet<InetAddress>();

	private UUID id = UUID.randomUUID();
	private UUID master = null;

	private MulticastSharer sharer;

	public YellowPages( MulticastSharer sharer )
	{
		this.sharer = sharer;
	}
	
	public void onMessageReceived( Message m, InetAddress address )
	{
		if( !address.isLoopbackAddress() )
			this.network.add( address );
		switch( m.type )
		{
			case JOIN:
				Set<InetAddress> copy = new HashSet<InetAddress>(this.network);
				copy.remove( address );
				Message reply = Message.network( this.id, copy );
				try
				{
					this.sharer.send( reply, address );
				} catch( IOException ball )
				{
					ball.printStackTrace();
				}
				break;
			case NETWORK:
				if( this.master == null )
				{
					this.master = m.id;
					Message hello = Message.hello( m.id );
					for( String a : m.name.split( ",", (int)m.size ) )
					{
						try
						{
							InetAddress addr = InetAddress.getByName( a );
							if( !addr.isLoopbackAddress() )
							{
								this.network.add( addr );
								this.sharer.send( hello, addr );
							}
						} catch( UnknownHostException ball )
						{
						} catch( IOException ball )
						{
							ball.printStackTrace();
						}
					}
				}
				break;
			//case HELLO:
			//	break;
		}
	}

	private static YellowPages instance = null;
	
	public static YellowPages getInstance(MulticastSharer sharer)
	{
		if( YellowPages.instance == null )
			YellowPages.instance = new YellowPages(sharer);
		return YellowPages.instance;
	}

	@Override
	public Iterator<InetAddress> iterator()
	{
		return this.network.iterator();
	}

	public void join( String masters )
	{
		if( masters == null )
			return;
		Message join = Message.join();
		for( String master : masters.split( "," ) )
		{
			try
			{
				InetAddress addr = InetAddress.getByName( master );
				this.network.add( addr );
				this.sharer.send( join, addr );
			} catch( UnknownHostException ball )
			{
				ball.printStackTrace();
			} catch( IOException ball )
			{
				ball.printStackTrace();
			}
		}
	}
	
}
