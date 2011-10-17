package nl.thanod.util;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class FileUtils
{
	public static final String PREFIX = "dragnshare";
	
	protected static Set<File> createdFiles = Collections.synchronizedSet(new HashSet<File>());
	
	static
	{
		Runtime.getRuntime().addShutdownHook(new Thread("FileUtilsShutdownHook") {
			@Override
			public void run()
			{
				for( File f : FileUtils.createdFiles )
				{
					if( f.exists() )
						f.delete();
					f = f.getParentFile();
					if( f.exists() && f.isDirectory() && f.getName().startsWith(FileUtils.PREFIX) )
						f.delete();
				}
			}
		});
	}
	
	public static File createDragnShareFile( String filename ) throws IOException
	{
		File result;
		File dir = File.createTempFile(FileUtils.PREFIX, "");
		dir.delete();
		dir.mkdirs();
		result = new File(dir, filename);
		result.createNewFile();
		FileUtils.createdFiles.add(result);
		return result;
	}

	public static File createDragnShareDir(String dirname) throws IOException
	{
		File result;
		File dir = File.createTempFile(FileUtils.PREFIX, "");
		dir.delete();
		dir.mkdirs();
		result = new File(dir, dirname);
		result.mkdirs();
		FileUtils.createdFiles.add(result);
		return result;
	}
	
}
