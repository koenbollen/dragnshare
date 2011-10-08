package nl.thanod.util;

import java.awt.Point;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

public class Settings extends Properties
{
	private static final long serialVersionUID = -9049014928487362558L;
	
	public static final Settings instance = new Settings();
	
	public interface Listener 
	{
		void preStore( Settings instance );
		void postLoad( Settings instance );
	}
	public static class Adapter implements Listener
	{
		@Override
		public void preStore(Settings instance){}

		@Override
		public void postLoad(Settings instance){}
	}
	
	private List<Listener> listeners;
	
	private Settings()
	{
		this.listeners = Collections.synchronizedList(new ArrayList<Listener>());
		this.load();
		Runtime.getRuntime().addShutdownHook(new Thread(){
			@Override
			public void run()
			{
				Settings.this.store();
			}
		});
	}
	
	public void store()
	{
		for( Listener l : this.listeners )
			if( l != null )
				l.preStore(this);
		
		File file = findFile();
		OutputStream out = null;
		try
		{
			file.createNewFile();
			out = new FileOutputStream(file);
			this.storeToXML(out, "Drag'n Share - Settings");
		} catch( IOException e )
		{
			e.printStackTrace();
		}finally
		{
			try {
			if( out != null )
				out.close();
			} catch( Exception e ){}
		}
	}
	
	public void load()
	{
		this.clear();
		File file = findFile();
		InputStream in = null;
		try
		{
			in = new FileInputStream(file);
			this.loadFromXML(in);
		} catch( FileNotFoundException e )
		{
		} catch( IOException e )
		{
			e.printStackTrace();
		}finally
		{
			try {
			if( in != null )
				in.close();
			} catch( Exception e ){}
		}
		
		for( Listener l : this.listeners )
			if( l != null )
				l.postLoad(instance);
	}

	public void addListener( Listener listener )
	{
		if( !this.listeners.contains(listener) )
			this.listeners.add(listener);
	}
	public boolean removeListener( Listener listener )
	{
		return this.listeners.remove(listener);
	}
	
	
	public Point getLocation()
	{
		try
		{
			int x = Integer.parseInt(this.getProperty("location_x"));
			int y = Integer.parseInt(this.getProperty("location_y"));
			if( x < 0 || y < 0 )
				return null;
			return new Point(x, y);
		} catch (NumberFormatException e) {
			return null;
		}
	}
	public void setLocation( Point p )
	{
		this.setProperty("location_x", Integer.toString(p.x));
		this.setProperty("location_y", Integer.toString(p.y));
	}
	
	private File findFile()
	{
		String userdir = System.getProperty("user.home", "");
		return new File( userdir, ".dragnshare.xml" );
	}
}
