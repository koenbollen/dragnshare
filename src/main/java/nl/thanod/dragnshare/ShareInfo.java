/**
 * 
 */
package nl.thanod.dragnshare;

import java.awt.Dimension;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;

/**
 * @author nilsdijk
 */
public class ShareInfo extends JPanel {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	protected JLabel label;
	protected JProgressBar prog;

	public ShareInfo() {
		this.setPreferredSize(new Dimension(0, 50));
		this.add(label = new JLabel());
		this.add(prog = new JProgressBar(0,100));
		
		prog.setVisible(false);
	}
	
	public void setSharedFile(final SharedFile shared){
		this.label.setText(shared.getName());
		if (shared.getProgress() < 1){
			prog.setVisible(true);
			prog.setValue((int)(100*shared.getProgress()));
		} else {
			prog.setVisible(false);
		}
	}
}