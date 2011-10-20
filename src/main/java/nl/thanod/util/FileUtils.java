package nl.thanod.util;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class FileUtils
{
	public static final String PREFIX = "dragnshare";

	protected static final String[] byteSuffixes = new String[]{" B"," KB"," MB"," GB"," TB"};
	protected static final String[] timeSuffixes = new String[]{" second"," minute"," hour"};
	protected static final DecimalFormat humanizedSpeedFormat = new DecimalFormat("#.0");
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

	public static String humanizeBytes(double size)
	{
		int index = 0;
		while (size > 1024 && index < FileUtils.byteSuffixes.length-1){
			index++;
			size /= 1024;
		}
		return FileUtils.humanizedSpeedFormat.format(size) + FileUtils.byteSuffixes[index];
	}

	public static String humanizeTime(long time)
	{
		double t = time;
		int index = 0;
		while (t > 60 && index < FileUtils.timeSuffixes.length-1){
			index++;
			t /= 60;
		}
		time = Math.round(t);
		String res = time + FileUtils.timeSuffixes[index];
		if( time != 1 )
			res += "s";
			
		return res;
	}

	public static void clean(File f)
	{
		FileUtils.createdFiles.remove(f);
		if( f.exists() )
		{
			f.delete();
		}
		File zip = new File( f+".zip" );
		if( zip.exists() )
			zip.delete();
		f = f.getParentFile();
		if( f.exists() && f.isDirectory() && f.getName().startsWith(FileUtils.PREFIX) )
			f.delete();
	}

	/**
	 * @return
	 * @throws IOException 
	 */
	public static File createDragnShareTemp() throws IOException {
		File dir = File.createTempFile(FileUtils.PREFIX, "");
		dir.delete();
		dir.mkdirs();
		FileUtils.createdFiles.add(dir);
		return dir;
	}
}
