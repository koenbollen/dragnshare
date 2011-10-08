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

	void addShareListener(ShareListener listener);

	void removeShareListener(ShareListener listener);
	
	void safeTo(File file);
	
	float getProgress();
}
