/**
 * 
 */
package nl.thanod.dragnshare.notify;

import java.util.ArrayList;
import java.util.List;

import nl.thanod.dragnshare.DropZone;
import nl.thanod.util.Settings;

/**
 * @author nilsdijk
 * @author Koen Bollen <meneer@koenbollen>
 */
public interface Notifier {
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

	public static class Factory
	{
		public static DropZone dropZone = null;
		private static List<Notifier> notifiers;
		static
		{
			Factory.notifiers = new ArrayList<Notifier>();
			Factory.notifiers.add(new GrowlNotifier());
			Factory.notifiers.add(new LibNotifyNotifier());
			Factory.notifiers.add(new JavaNotifier());
		}
		
		public static Notifier notifier()
		{
			for( Notifier dsn : Factory.notifiers )
			{
				if( dsn.canNotify() )
					return dsn;
			}
			return Factory.notifiers.get(Factory.notifiers.size()-1);
		}
		
		public static void notify(Notifier.Type type, String title, String message)
		{
			if( !Settings.instance.getBool("showNotifications", true) )
				return;
			if( Factory.dropZone != null && Factory.dropZone.isFocused() ) // TODO: Test isFocused() on OSX
				return;
			Notifier notifier = Factory.notifier();
			if( notifier != null )
				notifier.notify(type, "Drag'n Share - " + title, message);
		}
		
	}
	
	public boolean canNotify();
	
	public void notify(Notifier.Type type, String title, String message);
}
