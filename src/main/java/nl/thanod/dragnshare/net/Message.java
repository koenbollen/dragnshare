/**
 * 
 */
package nl.thanod.dragnshare.net;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.ProtocolException;
import java.util.Set;
import java.util.UUID;

/**
 * @author Nils Dijk <me@thanod.nl>
 * @author Koen Bollen <meneer@koenbollen.nl>
 */
public class Message implements Serializable {

	public enum Type {
		
		// Message types for file sharing:
		ACCEPT,
		FILE,
		DIRECTORY,
		
		// Message types for the YellowPages:
		JOIN,
		NETWORK,
		HELLO;

		public static Type getByName(String name) throws ProtocolException {
			name = name.toLowerCase();
			for (Type t : Type.values())
				if (t.name().toLowerCase().equals(name))
					return t;
			throw new ProtocolException("Unknown message type: " + name);
		}
	}

	private static final long serialVersionUID = -8087115562340780366L;

	public final Type type;
	public final UUID id;
	public final String name;
	public final long size;

	public final int port;

	private Message(Type type, UUID id, int port, long size, String name) {
		this.type = type;

		if (id == null)
			this.id = UUID.randomUUID();
		else
			this.id = id;

		this.port = port;
		this.size = size;
		this.name = name.trim();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((this.id == null) ? 0 : this.id.hashCode());
		result = prime * result + ((this.name == null) ? 0 : this.name.hashCode());
		result = prime * result + this.port;
		result = prime * result + (int) (this.size ^ (this.size >>> 32));
		result = prime * result + ((this.type == null) ? 0 : this.type.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Message other = (Message) obj;
		if (this.id == null) {
			if (other.id != null)
				return false;
		} else if (!this.id.equals(other.id))
			return false;
		if (this.name == null) {
			if (other.name != null)
				return false;
		} else if (!this.name.equals(other.name))
			return false;
		if (this.port != other.port)
			return false;
		if (this.size != other.size)
			return false;
		if (this.type != other.type)
			return false;
		return true;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();

		sb.append(this.type.name().toLowerCase());
		sb.append(':');
		sb.append(this.id);
		sb.append(':');
		sb.append(this.port);
		sb.append(':');
		sb.append(this.size);
		sb.append(':');
		sb.append(this.name);

		return sb.toString();
	}

	public static Message accept(UUID id, int port) {
		return new Message(Type.ACCEPT, id, port, 0, "");
	}

	public static Message file(File file) {
		return new Message(file.isDirectory() ? Type.DIRECTORY : Type.FILE, UUID.randomUUID(), -1, measureLength(file), file.getName());
	}

	public static Message parse(String message) throws IOException {
		String[] parts = message.split(":", 5);
		try {
			return new Message(Type.getByName(parts[0]), UUID.fromString(parts[1]), Integer.parseInt(parts[2]), Long.parseLong(parts[3]), parts[4]);
		} catch (NumberFormatException ball) {
			throw new IOException("could not parse message", ball);
		}
	}

	public static Message join()
	{
		return new Message( Type.JOIN, new UUID(0,0), -1, -1, "" );
	}
	public static Message hello( UUID id )
	{
		return new Message( Type.HELLO, id, -1, -1, "" );
	}
	public static Message network( UUID id, Set<InetAddress> network )
	{
		long size = network.size();
		StringBuilder data = new StringBuilder();
		for( InetAddress a : network )
		{
			if( data.length() > 0 )
				data.append( ',' );
			data.append( a.getHostAddress() );
		}
		return new Message( Type.NETWORK, id, -1, size, data.toString() );
	}

	private static long measureLength(File file) {
		if (file.isFile())
			return file.length();
		if (!file.isDirectory())
			return 0;
		long size = 0;
		for (File f : file.listFiles()) {
			size += measureLength(f);
		}
		return size;
	}
}
