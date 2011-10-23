package nl.thanod.dragnshare.notify;

import java.io.IOException;

import nl.thanod.util.OS;

public class LibNotifyNotifier implements Notifier
{
	public static final String EXECUTABLE = "notify-send";
	
	private static boolean executableChecked = false;
	private static boolean haveExecutable = false;

	@Override
	public boolean canNotify()
	{
		return LibNotifyNotifier.hasExecutable();
	}
	
	private static boolean hasExecutable()
	{
		if( LibNotifyNotifier.executableChecked )
			return LibNotifyNotifier.haveExecutable;

		LibNotifyNotifier.executableChecked = true;
		Process p = null;
		try
		{
			p = Runtime.getRuntime().exec(LibNotifyNotifier.EXECUTABLE);
		} catch( IOException e )
		{
			if( OS.isLinux() )
			{
				System.err.println("missing `" + LibNotifyNotifier.EXECUTABLE + "`. Please install the 'libnotify-bin' package." ); 
			}
			return (LibNotifyNotifier.haveExecutable=false);
		}
		p.destroy();
		return (LibNotifyNotifier.haveExecutable=true);
	}

	@Override
	public void notify(Type type, String title, String message)
	{
		if( !canNotify() )
			return;
		try
		{
			String[] cmd = { LibNotifyNotifier.EXECUTABLE, title, message };
			//System.out.println(Arrays.toString(cmd));
			Runtime.getRuntime().exec( cmd );
		} catch( IOException e )
		{
			e.printStackTrace();
		}
	}
	
	public static boolean isAvailable()
	{
		return OS.isLinux() && LibNotifyNotifier.hasExecutable();
	}

}
