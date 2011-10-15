package nl.thanod.dragnshare.notify;

import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.TrayIcon.MessageType;

import nl.thanod.dragnshare.ui.Tray;

/**
 * @author Koen Bollen <meneer@koenbollen>
 */
public class JavaNotifier implements Notifier
{	
	private static Tray findTray()
	{
		SystemTray systray = SystemTray.getSystemTray();
		Tray t = null;
		for( TrayIcon ti : systray.getTrayIcons() )
		{
			if( ti instanceof Tray )
			{
				t = (Tray)ti;
				break;
			}
		}
		return t;
	}
	
	@Override
	public boolean canNotify()
	{
		return SystemTray.isSupported() && JavaNotifier.findTray() != null;
	}

	@Override
	public void notify(Type type, String title, String message)
	{
		Tray t = JavaNotifier.findTray();
		if( t != null )
		{
			t.displayMessage(title, message, MessageType.INFO);
		}
	}

}
