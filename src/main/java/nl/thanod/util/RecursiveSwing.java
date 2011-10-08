/**
 * 
 */
package nl.thanod.util;

import java.awt.Container;
import java.awt.Window;

/**
 * @author nilsdijk
 *
 */
public class RecursiveSwing {
	public static Window getContainingWindow(Container c){
		while (c != null && !(c instanceof Window))
			c = c.getParent();
		return (Window)c;
	}
}
