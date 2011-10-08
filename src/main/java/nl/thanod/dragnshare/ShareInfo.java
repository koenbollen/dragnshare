/**
 * 
 */
package nl.thanod.dragnshare;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.Collections;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;

import nl.thanod.DebugUtil;

/**
 * @author nilsdijk
 */
public class ShareInfo extends JPanel {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	protected SharedFile shared;
	protected JLabel label;
	protected JButton safe;

	public ShareInfo(SharedFile shared, int count) {
		this.setPreferredSize(new Dimension(0, 50));
		if (count % 2 == 0)
			setBackground(Color.WHITE);

		this.shared = shared;

		this.add(label = new JLabel(shared.getName()));

		this.add(safe = new JButton("safe"));
		safe.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				JFileChooser jfc = new JFileChooser();
				jfc.setSelectedFile(new File(ShareInfo.this.shared.getName()));
				if (jfc.showSaveDialog(ShareInfo.this) == JFileChooser.APPROVE_OPTION) {
					if (jfc.getSelectedFile() != null)
						ShareInfo.this.shared.safeTo(jfc.getSelectedFile());
				}
			}
		});

		initDragable();
	}

	/**
	 * 
	 */
	private void initDragable() {
		final DragSource ds = new DragSource();
		ds.createDefaultDragGestureRecognizer(this, DnDConstants.ACTION_COPY, new DragGestureListener() {

			@Override
			public void dragGestureRecognized(DragGestureEvent evt) {
				Transferable t = new FileTransferable(Collections.singleton(shared.getFile()));
				ds.startDrag(evt, DragSource.DefaultCopyDrop, t, null);
			}
		});
	}
}