/**
 * 
 */
package nl.thanod.dragnshare;

import java.io.File;

;

/**
 * @author nilsdijk
 */
public interface SharedFile {

	File getFile();
	
	String getName();
	
	float getProgress();
	
	DumpsterListCellRenderer.ColorScheme getColorScheme();
}
