/**
 * 
 */
package nl.thanod.dragnshare;

import com.growl.GrowlWrapper;

/**
 * @author nilsdijk
 */
public final class DragnShareNotifier {
	public enum Type {
		RECEIVED("received", true);

		public final String type;
		private final boolean def;

		private Type(String type, boolean def) {
			this.def = def;
			this.type = type;
		}

		public static String[] getAllNotifications() {
			String[] nts = new String[Type.values().length];
			for (int i = 0; i < nts.length; i++)
				nts[i] = Type.values()[i].type;
			return nts;
		}

		public static String[] getDefaultNotifications() {
			int c = 0;
			for (Type t : values())
				if (t.def)
					c++;

			int index = 0;
			String[] nts = new String[c];
			for (Type t : values()) {
				if (t.def) {
					nts[index++] = t.type;
				}
			}
			return nts;
		}
	}
	
	private DragnShareNotifier(){}

	private static GrowlWrapper GROWL = new GrowlWrapper("Drag'nShare", "DragnShare", Type.getAllNotifications(), Type.getDefaultNotifications());
	
	public static void notify(DragnShareNotifier.Type type, String title, String message){
		if (GROWL != null)
			GROWL.notify(type.type, title, message);
	}
}
