/**
 * 
 */
package nl.thanod.dragnshare.ui;

import java.awt.*;

import javax.swing.border.Border;

/**
 * @author nilsdijk
 */
public class DottedLineBorder implements Border {

	private final Paint paint;
	private final Insets insets;

	private Stroke stroke;

	public DottedLineBorder(Paint paint) {
		this.stroke = new BasicStroke(1.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, new float[] { 1f }, 0.0f);
		this.paint = paint;
		this.insets = new Insets(1, 1, 1, 1);
	}

	/*
	 * (non-Javadoc)
	 * @see javax.swing.border.Border#paintBorder(java.awt.Component,
	 * java.awt.Graphics, int, int, int, int)
	 */
	@Override
	public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
		Graphics2D g2d = (Graphics2D) g;
		g2d.setStroke(this.stroke);
		g2d.setPaint(this.paint);
		g2d.drawRect(x, y, width-1, height-1);
	}

	/*
	 * (non-Javadoc)
	 * @see javax.swing.border.Border#getBorderInsets(java.awt.Component)
	 */
	@Override
	public Insets getBorderInsets(Component paramComponent) {
		return this.insets;
	}

	/*
	 * (non-Javadoc)
	 * @see javax.swing.border.Border#isBorderOpaque()
	 */
	@Override
	public boolean isBorderOpaque() {
		return false;
	}

}
