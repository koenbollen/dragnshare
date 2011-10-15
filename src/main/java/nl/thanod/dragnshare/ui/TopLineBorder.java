/**
 * 
 */
package nl.thanod.dragnshare.ui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Insets;

import javax.swing.border.Border;

/**
 * @author nilsdijk
 *
 */
public class TopLineBorder implements Border{

	
	private final Color c;

	public TopLineBorder(Color c){
		this.c = c;
	}
	/* (non-Javadoc)
	 * @see javax.swing.border.Border#paintBorder(java.awt.Component, java.awt.Graphics, int, int, int, int)
	 */
	@Override
	public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
		Color o = g.getColor();
		g.setColor(this.c);
		g.drawLine(x, y, x+width, y);
		g.setColor(o);
	}

	/* (non-Javadoc)
	 * @see javax.swing.border.Border#getBorderInsets(java.awt.Component)
	 */
	@Override
	public Insets getBorderInsets(Component paramComponent) {
		return new Insets(1, 0, 0, 0);
	}

	/* (non-Javadoc)
	 * @see javax.swing.border.Border#isBorderOpaque()
	 */
	@Override
	public boolean isBorderOpaque() {
		return true;
	}

}
