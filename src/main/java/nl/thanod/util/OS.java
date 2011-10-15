package nl.thanod.util;


public class OS
{
	public enum OSType
	{
		WINDOWS, OSX, LINUX, OTHER
	}

	public static OSType getType()
	{
		if (isWindows())
		{
			return OSType.WINDOWS;
		}
		if (isOSX())
		{
			return OSType.OSX;
		}
		if (isLinux())
		{
			return OSType.LINUX;
		}
		return OSType.OTHER;
	}
	
	public static String getString()
	{
		switch( getType() )
		{
		case WINDOWS:
			return "windows";
		case OSX:
			return "osx";
		case LINUX:
			return "linux";
		default:
			return "other";
		}
	}

	public static boolean isWindows()
	{
		String os = System.getProperty("os.name").toLowerCase();
		return (os.indexOf("win") >= 0);
	}

	public static boolean isOSX()
	{
		String os = System.getProperty("os.name").toLowerCase();
		return (os.indexOf("mac") >= 0);
	}

	public static boolean isLinux()
	{
		String os = System.getProperty("os.name").toLowerCase();
		return (os.indexOf("nix") >= 0 || os.indexOf("nux") >= 0);
	}
	
	public static String getUsername()
	{
		String u = System.getProperty("user.name");
		if( u != null && u.length() > 0 )
			return u;
		String[] envs = { "USER", "USERNAME", "LOGNAME" };
		for( String e : envs )
		{
			u = System.getenv(e);
			if( u != null && u.length() > 0 )
				return u;
		}
		return null;
	}
}
