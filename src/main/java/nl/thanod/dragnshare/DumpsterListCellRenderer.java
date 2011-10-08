/**
 * 
 */
package nl.thanod.dragnshare;

import java.awt.Color;
import java.awt.Component;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;

/**
 * @author nilsdijk
 */
public class DumpsterListCellRenderer implements ListCellRenderer {

	public enum ColorScheme {
		SELECTED(Color.BLUE, Color.BLUE),
		DEFAULT(Color.GRAY, Color.LIGHT_GRAY),
		RECEIVED(Color.CYAN, Color.CYAN),
		OFFERED(Color.ORANGE, Color.YELLOW);

		private final Color c1;
		private final Color c2;

		private ColorScheme(Color c1, Color c2) {
			this.c1 = c1;
			this.c2 = c2;
		}

		public Color getColor(int index) {
			if (index % 2 == 0)
				return c1;
			return c2;
		}
	}

	private final Map<Object, WeakReference<ShareInfo>> map = new HashMap<Object, WeakReference<ShareInfo>>();
	private final ShareInfo info = new ShareInfo();
	
	/*
	 * (non-Javadoc)
	 * @see
	 * javax.swing.ListCellRenderer#getListCellRendererComponent(javax.swing
	 * .JList, java.lang.Object, int, boolean, boolean)
	 */
	@Override
	public Component getListCellRendererComponent(JList list, Object obj, int index, boolean isSelected, boolean hasFocus) {
		if (obj instanceof SharedFile) {
			SharedFile shared = (SharedFile) obj;

			ColorScheme cs = getColorScheme(isSelected, shared);
			info.setSharedFile(shared);
			info.setBackground(cs.getColor(index));

			return info;
		}
		return new JLabel(obj.toString());
	}

	/**
	 * @param isSelected
	 * @param info
	 * @return
	 */
	private ColorScheme getColorScheme(boolean isSelected, SharedFile sf) {
		if (isSelected)
			return ColorScheme.SELECTED;
		return sf.getColorScheme();
	}
}
