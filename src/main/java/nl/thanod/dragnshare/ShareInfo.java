/**
 * 
 */
package nl.thanod.dragnshare;

import java.awt.*;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DragGestureEvent;
import java.awt.dnd.DragGestureListener;
import java.awt.dnd.DragSource;
import java.awt.dnd.DragSourceDragEvent;
import java.awt.dnd.DragSourceDropEvent;
import java.awt.dnd.DragSourceEvent;
import java.awt.dnd.DragSourceListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.util.Collections;
import java.util.Observable;
import java.util.Observer;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JRootPane;

import nl.thanod.util.RecursiveSwing;

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

	public ShareInfo(final SharedFile shared, int count) {
		this.setPreferredSize(new Dimension(0, 50));
		if (count % 2 == 0)
			setBackground(Color.WHITE);

		this.shared = shared;

		this.add(label = new JLabel(shared.getName()));
		
		if (shared instanceof Observable){
			final JProgressBar prog = new JProgressBar(0,100);
			this.add(prog);
			((Observable)shared).addObserver(new Observer() {
				@Override
				public void update(Observable o, Object arg) {
					prog.setValue((int)(shared.getProgress()*100));
				}
			});
		}

		initDragable();
		
		this.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if (e.getClickCount() == 2){
					if (Desktop.isDesktopSupported()){
						try {
							Desktop.getDesktop().open(shared.getFile());
						} catch (IOException ball) {
							ball.printStackTrace();
						}
					}
				}
			}
		});
	}

	/**
	 * 
	 */
	private void initDragable() {
		final DragSource ds = new DragSource();
		ds.createDefaultDragGestureRecognizer(this, DnDConstants.ACTION_COPY_OR_MOVE, new DragGestureListener() {

			@Override
			public void dragGestureRecognized(DragGestureEvent evt) {
				Transferable t = new FileTransferable(Collections.singleton(shared.getFile()));
				ds.startDrag(evt, DragSource.DefaultMoveDrop, t, new DragSourceListener() {
					
					@Override
					public void dropActionChanged(DragSourceDragEvent dsde) {
						// TODO Auto-generated method stub
						
					}
					
					@Override
					public void dragOver(DragSourceDragEvent dsde) {
						// TODO Auto-generated method stub
						
					}
					
					@Override
					public void dragExit(DragSourceEvent dse) {
						// TODO Auto-generated method stub
						
					}
					
					@Override
					public void dragEnter(DragSourceDragEvent dsde) {
						// TODO Auto-generated method stub
						
					}
					
					@Override
					public void dragDropEnd(DragSourceDropEvent dsde) {
						Window w = RecursiveSwing.getContainingWindow(ShareInfo.this);
						if( dsde.getDropAction() == DnDConstants.ACTION_MOVE )
						{
							JRootPane p = ShareInfo.this.getRootPane();
							Container c = ShareInfo.this.getParent();
							c.remove(ShareInfo.this);
							p.revalidate();
						}						
						if (w != null)
							w.setVisible(false);
					}
				});
			}
		});
	}
}