package nl.thanod.dragnshare.ui;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import javax.imageio.ImageIO;

import nl.thanod.util.OS;

/**
 * @author Koen Bollen <meneer@koenbollen>
 */
public class Tray extends TrayIcon implements Runnable
{
	public static final long BLINKING_UP_SPEED = 700;
	public static final long BLINKING_DOWN_SPEED = 300;
	
	private static SystemTray systray = SystemTray.getSystemTray();
	
	private final Map<String, BufferedImage> images;
	private final BufferedImage base;
	
	private String currentDecorator;
	private boolean blinking;
	private boolean bonk;

	public Tray()
	{
		super(staticinit());
		
		this.setImageAutoSize(false);
		
		this.setPopupMenu(new TrayMenu(null));
		
		this.base = (BufferedImage)this.getImage();
		this.images = new HashMap<String, BufferedImage>();
		this.images.put("base", this.base);
		this.currentDecorator = null;
		this.blinking = false;
		
		Thread thr = new Thread(this, "TrayBlinkingThread");
		thr.setDaemon(true);
		thr.start();
		
		try
		{
			systray.add(this);
		} catch( AWTException e )
		{
			e.printStackTrace();
		}
	}

	private static Image staticinit()
	{
		Dimension size = systray.getTrayIconSize();

		String os = OS.getString();
		URL icon = Tray.class.getClassLoader().getResource("dragn-icon-"+os+".png");
		if( icon == null )
			icon = Tray.class.getClassLoader().getResource("dragn-icon.png");
		return createImage( icon, size );
	}

	@Override
	public void run()
	{
		try
		{
			while( true )
			{
				if( this.blinking )
				{
					if( this.bonk )
					{
						this.setImage(this.base);
						Thread.sleep(BLINKING_DOWN_SPEED);
					}
					else
					{
						this.setImage(this.images.get(this.currentDecorator));
						Thread.sleep(BLINKING_UP_SPEED);
					}
					this.bonk = !this.bonk;
				}
				else
				{
					Thread.sleep( Math.min( BLINKING_DOWN_SPEED, BLINKING_UP_SPEED) );
				}
			}
		} catch( InterruptedException e )
		{
			e.printStackTrace();
		}
	}
	
	public void setDefaultIcon()
	{
		this.setDecorator(null);
	}
	
	public void setDecorator( String decorator )
	{
		this.setDecorator(decorator, false);
	}
	
	public void setDecorator( String decorator, boolean blinking )
	{
		
		if( decorator != null && ( !decorator.endsWith(".png") && !decorator.endsWith(".jpg") ) )
			decorator += ".png";
		
		this.currentDecorator = decorator;
		this.blinking = blinking;
		this.bonk = true;
		
		this.setImage(this.getIcon( decorator ));
		
	}
	
	private BufferedImage getIcon( String decorator )
	{
		if( decorator == null || decorator.length() <= 0 )
			decorator = "base";
		
		if( !this.images.containsKey(decorator) && !"base".equals(decorator) )
		{
			BufferedImage img = createImage( decorator );
			if( img == null ) // couldn't find decorator
				return this.getIcon(null);
			int imgwidth = img.getWidth();
			int imgheight = img.getHeight();
			
			int width = this.base.getWidth();
			int height = this.base.getHeight();
			BufferedImage res = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
			
			// Check whether the decorator image fits, rescale if not:
			if( imgwidth > width )
			{
				float factor = width/(float)imgwidth;
				imgwidth = width;
				imgheight = (int)(imgheight * factor);
			}
			if( imgheight > height )
			{
				float factor = height/(float)imgheight;
				imgheight = height;
				imgwidth = (int)(imgwidth * factor);
			}
			
			// Draw both images in the result:
			Graphics2D graphics = res.createGraphics();
			graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
			graphics.drawImage(this.base, 0, 0, width, height, null);
			graphics.drawImage(img, width-imgwidth, height-imgheight, imgwidth, imgheight, null);
			graphics.dispose();
			this.images.put(decorator, res);
		}
		
		return this.images.get( decorator );
	}

	private static BufferedImage createImage(URL url, Dimension size) {
		try {
			BufferedImage img = ImageIO.read(url);
			if( img == null )
				return null;
			if( size != null )
			{
				BufferedImage scaled = new BufferedImage(size.width, size.height, BufferedImage.TYPE_INT_ARGB);
				Graphics2D graphics = scaled.createGraphics();
				graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
				graphics.drawImage(img, 0, 0, size.width, size.height, null);
				graphics.dispose();
				return scaled;
			}
			return img;
		} catch (Throwable ball) {
			ball.printStackTrace();
			return null;
		}
	}
	private static BufferedImage createImage( String name )
	{
		URL u =  Tray.class.getClassLoader().getResource(name);
		if( u == null )
			return null;
		return createImage( u, null );
	}
}
