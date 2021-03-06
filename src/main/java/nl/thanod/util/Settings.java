package nl.thanod.util;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.Toolkit;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import javax.swing.JOptionPane;

/**
 * @author Koen Bollen <meneer@koenbollen>
 */
public class Settings extends Properties
{
	private static final long serialVersionUID = -9049014928487362558L;
	
	public static final Settings instance = new Settings();
	
	public interface Listener 
	{
		void preStore( Settings instance );
		void postLoad( Settings instance );
		void onSettingChanged( String key, String old, String value );
	}
	public static class Adapter implements Listener
	{
		@Override
		public void preStore(Settings instance){}

		@Override
		public void postLoad(Settings instance){}
		
		@Override
		public void onSettingChanged( String key, String old, String value ){}
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
		List<Listener> listeners = new ArrayList<Settings.Listener>(this.listeners);
		for( Listener l : listeners )
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
		
		FileInputStream in = null;
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
		
		List<Listener> listeners = new ArrayList<Settings.Listener>(this.listeners);
		for( Listener l : listeners )
			if( l != null )
				l.postLoad(instance);
	}
	
	public static void singleInstance(){
		boolean locked;
		try {
			locked = lock(findFile());
		} catch (Throwable ball){
			locked = false;
		}
		if (!locked){
			JOptionPane.showMessageDialog(null, "Drag'n Share is already running.\nPlease open the application by clicking the icon in the system tray.","Drag'n Error", JOptionPane.INFORMATION_MESSAGE);
			System.exit(-1);
		}
	}

	/**
	 * @param file
	 * @throws FileNotFoundException 
	 */
	private static boolean lock(File file) throws FileNotFoundException {
		final RandomAccessFile ra = new RandomAccessFile(file, "rw");
		final FileChannel ch = ra.getChannel();
		try {
			if (ch.tryLock() == null)
				return false;
		} catch (IOException ball) {
			return false;
		}
		
		Runtime.getRuntime().addShutdownHook(new Thread(){
			@Override
			public void run()
			{
				try {
					ch.close();
				} catch (IOException ball) {
					ball.printStackTrace();
				}
				try {
					ra.close();
				} catch (IOException ball) {
					ball.printStackTrace();
				}
			}
		});
		
		return true;
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
	
	@Override
	public synchronized Object setProperty(String key, String value)
	{
		String old = this.getProperty(key);
		if( old == null || !old.equals(value) )
			fireSettingChanged(key, old, value);
		return super.setProperty(key, value);
	}
	
	
	private void fireSettingChanged(String key, String old, String value)
	{
		List<Listener> listeners = new ArrayList<Settings.Listener>(this.listeners);
		for( Listener l : listeners )
			if( l != null )
				l.onSettingChanged(key, old, value);
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
		// If linux, round location on screensize in case of multiple desktops.
		if( OS.isLinux() ) 
		{
			Dimension size = Toolkit.getDefaultToolkit().getScreenSize();
			p.x = (p.x + (size.width * 10)) % size.width;
			p.y = (p.y + (size.height * 10)) % size.height;
		}
		this.setProperty("location_x", Integer.toString(p.x));
		this.setProperty("location_y", Integer.toString(p.y));
	}
	
	private static File findFile()
	{
		String userdir = System.getProperty("user.home", "");
		return new File( userdir, ".dragnshare.xml" );
	}

	public boolean getBool(String key, boolean def)
	{
		String value = this.getProperty(key);
		if( value == null )
			return def;
		return Boolean.parseBoolean(value);
	}
	public boolean getBool(String key)
	{
		return Boolean.parseBoolean(this.getProperty(key));
	}
	public void setBool(String key, boolean value)
	{
		this.setProperty(key, Boolean.toString(value));
	}

	public int getInt(String key, int def)
	{
		String value = this.getProperty(key);
		if( value == null )
			return def;
		return Integer.parseInt(value);
	}

	public void setInt(String key, int value)
	{
		this.setProperty(key, Integer.toString(value));
	}
	
}
