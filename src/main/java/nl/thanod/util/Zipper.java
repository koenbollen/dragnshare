package nl.thanod.util;

import java.io.*;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class Zipper implements Runnable
{
	public static final int BUFFERSIZE = 4096;
	
	public interface Listener
	{
		void finished(File directory, File zipfile);
	}
	class ListenerContainer implements Listener
	{
		private final List<Listener> listeners = Collections.synchronizedList(new ArrayList<Zipper.Listener>());
		
		@Override
		public void finished(File directory, File zipfile)
		{
			List<Listener> listeners = new ArrayList<Listener>(this.listeners);
			for( Listener l : listeners )
				l.finished(directory, zipfile);
		}

		public void addListener( Listener l )
		{
			if( l != null && !this.listeners.contains(l) )
				this.listeners.add(l);
		}
		
		public boolean removeListener( Listener l )
		{
			return this.listeners.remove(l);
		}
	}
	
	private static int threadCounter = 0;
	
	private final ListenerContainer listeners;
	private final File directory;

	public Zipper( File directory )
	{
		this.listeners = new ListenerContainer();
		this.directory = directory;
		Thread thr = new Thread( this, "Zipper-"+(threadCounter++) );
		thr.setDaemon(false);
		thr.start();
	}
	
	@Override
	public void run()
	{
		File zipfile = null;
		ZipOutputStream zipout = null;
		try
		{
			zipfile = FileUtils.createDragnShareFile( this.directory.getName() + ".zip" );
			
		    zipout = new ZipOutputStream(new BufferedOutputStream( new FileOutputStream(zipfile)) );
		    zipout.setComment("Drag'n Share directory: " + this.directory.getName());
		    this.walk( this.directory, zipout );
			zipout.flush();
			zipout.close();
			this.listeners.finished(this.directory, zipfile);
		} catch(Exception e )
		{
			e.printStackTrace();
		} finally 
		{
			try
			{
				if( zipout != null )
					zipout.close();
			} catch( IOException e )
			{
			}
		}
	}
	
	private void walk( File dir, ZipOutputStream out ) throws IOException
	{
		URI base = this.directory.getParentFile().toURI();
		if( !dir.exists() || !dir.isDirectory() )
			return;
		for( File f : dir.listFiles() )
		{
			if( !f.exists() ) //???
				continue;
			if( f.isDirectory() )
				walk( f, out );

			String path = base.relativize(f.toURI()).getPath();
			
			ZipEntry entry = new ZipEntry(path);
			out.putNextEntry(entry);
			writeFile( f, out );
			out.closeEntry();
		}
	}
	
	private static void writeFile(File f, ZipOutputStream out) throws IOException
	{
		InputStream in = null;
		byte[] buffer = new byte[Zipper.BUFFERSIZE];
		try
		{
			int n;
			long count = 0;
			
			in = new FileInputStream( f );
			do
			{
				n = in.read(buffer, 0, buffer.length);
				if (n > 0)
				{
					count += n;
					out.write(buffer, 0, n);
				}
			} while( n > 0 );
			if( count != f.length() )
				throw new IOException("didn't write enough bytes to zip for file " + f.getPath() + ". (expected " + f.length() + " bytes but got " + count + ")");
		} finally
		{
			try
			{
				if( in != null )
					in.close();
			} catch( IOException e )
			{
			}
		}
	}

	public void addListener(Listener l)
	{
		this.listeners.addListener(l);
	}

	public boolean removeListener(Listener l)
	{
		return this.listeners.removeListener(l);
	}
}
