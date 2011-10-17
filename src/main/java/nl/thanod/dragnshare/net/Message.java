package nl.thanod.dragnshare.net;

import java.io.File;
import java.io.Serializable;
import java.util.UUID;

public class Message implements Serializable
{

	private static final long serialVersionUID = 6093736725319921251L;

	public enum MessageType
	{
		OFFER, ACCEPT,
	}

	private MessageType type;

	private String filename;
	private long filesize;
	private int port;
	private UUID id;
	private boolean directory;

	public Message(long filesize, String filename, UUID id)
	{
		super();
		this.type = MessageType.OFFER;
		this.filename = filename;
		this.filesize = filesize;
		this.id = id;
		this.directory = false;
	}

	public Message(long filesize, String filename)
	{
		super();
		this.type = MessageType.OFFER;
		this.filename = filename;
		this.filesize = filesize;
		this.id = UUID.randomUUID();
		this.directory = false;
	}

	public Message(File file, boolean dir)
	{
		this(file.length(), file.getName());
		this.directory = dir;
	}

	public Message(UUID id, int port)
	{
		super();
		this.id = id;
		this.type = MessageType.ACCEPT;
		this.port = port;
	}

	@Override
	public String toString()
	{
		StringBuilder sb = new StringBuilder();
		sb.append(this.type.ordinal());
		sb.append(":");
		sb.append(this.id);
		sb.append(":");
		if (this.type == MessageType.OFFER)
		{
			sb.append(this.filesize);
			sb.append(":");
			sb.append(this.filename);
			sb.append(":");
			sb.append(this.directory);
		} else
		{
			sb.append(this.port);
		}
		return sb.toString();
	}

	public static Message parse(String raw)
	{
		String[] pieces = raw.split(":");
		MessageType t = MessageType.values()[Integer.parseInt(pieces[0])];
		if (t == MessageType.OFFER)
		{
			Message m = new Message(Long.parseLong(pieces[2]), pieces[3], UUID.fromString(pieces[1]));
			if( pieces.length >= 5 )
				m.directory = Boolean.parseBoolean(pieces[4]);
			return m;
		}
		return new Message(UUID.fromString(pieces[1]), Integer.parseInt(pieces[2]));
	}

	public MessageType getType()
	{
		return this.type;
	}

	public UUID getID()
	{
		return this.id;
	}

	public int getPort()
	{
		if (this.type != MessageType.ACCEPT)
			throw new RuntimeException("Not a ACCEPT message.");
		return this.port;
	}

	public String getFilename()
	{
		if (this.type != MessageType.OFFER)
			throw new RuntimeException("Not a OFFER message.");
		return this.filename;
	}

	public long getFilesize()
	{
		if (this.type != MessageType.OFFER)
			throw new RuntimeException("Not a OFFER message.");
		return this.filesize;
	}

	public boolean isDirectory()
	{
		return this.directory;
	}

}
