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
	
	ColorScheme getColorScheme();
	
	boolean isReady();
	
	void remove();
	
	boolean shouldStart();
	
	void start();
	
	String getStatus();
	
	void setView( ShareInfo view );
}
