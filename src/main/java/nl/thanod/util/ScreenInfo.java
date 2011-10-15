package nl.thanod.util;

import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;

public class ScreenInfo
{
	public static int GetID( int x, int y )
	{
		GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
		GraphicsDevice[] devices = ge.getScreenDevices();
		for( int i = 0; i < devices.length; i++ )
		{
			GraphicsDevice gd = devices[i];
			if( gd.getDefaultConfiguration().getBounds().contains(x, y) )
				return i;
		}
		return 0;
	}
	
	public static Rectangle getBounds( int screenid )
	{
		GraphicsDevice[] devices = GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices();
		if( screenid < 0 || screenid >= devices.length )
			return null;
		return devices[screenid].getDefaultConfiguration().getBounds();
	}
	
	public static boolean intersects( Rectangle window )
	{
		GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
		GraphicsDevice[] devices = ge.getScreenDevices();
		
		for( int i = 0; i < devices.length; i++ )
		{
			GraphicsDevice gd = devices[i];
			Rectangle bounds = gd.getDefaultConfiguration().getBounds();
			if( bounds.intersects(bounds) )
				return true;
		}
		return false;
	}

}
