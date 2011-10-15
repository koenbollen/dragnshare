package nl.thanod.dragnshare.notify;

import java.awt.SystemTray;
import java.awt.TrayIcon.MessageType;

import nl.thanod.dragnshare.ui.Tray;

/**
 * @author Koen Bollen <meneer@koenbollen>
 */
public class JavaNotifier implements Notifier
{	
	
	@Override
	public boolean canNotify()
	{
		return SystemTray.isSupported() && Tray.findTray() != null;
	}

	@Override
	public void notify(Type type, String title, String message)
	{
		Tray t = Tray.findTray();
		if( t != null )
		{
			t.displayMessage(title, message, MessageType.INFO);
		}
	}

}
