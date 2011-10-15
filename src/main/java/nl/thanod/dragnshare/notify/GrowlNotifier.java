package nl.thanod.dragnshare.notify;

import nl.thanod.util.OS;

import com.growl.GrowlWrapper;

class GrowlNotifier implements Notifier
{
	private static GrowlWrapper GROWL = new GrowlWrapper("Drag'n Share", "DragnShare", Type.getAllNotifications(), Type.getDefaultNotifications());
	
	@Override
	public boolean canNotify()
	{
		return OS.isOSX();
	}
	
	@Override
	public void notify(Notifier.Type type, String title, String message)
	{
		GROWL.notify(type.type, title, message);
	}

}
