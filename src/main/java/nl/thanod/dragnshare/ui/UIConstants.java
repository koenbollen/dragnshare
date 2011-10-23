package nl.thanod.dragnshare.ui;

import java.awt.Font;
import java.awt.FontFormatException;
import java.io.FileNotFoundException;
import java.io.IOException;

public final class UIConstants {
	public static final Font DefaultFont;
	public static final Font StatusFont;
	public static final Font DropHereFont;
	
	static
	{
		DefaultFont = loadFont("dejavusans.ttf", 12);
		StatusFont = loadFont("dejavusans.ttf", 10);
		DropHereFont = DefaultFont;
	}
	
	
	public static Font loadFont( String filename, int size )
	{
		try {
			Font f = Font.createFont(Font.TRUETYPE_FONT, UIConstants.class.getClassLoader().getResourceAsStream(filename));
			return f.deriveFont(Font.PLAIN, size);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return new Font(filename, Font.PLAIN, size);
		} catch (FontFormatException e) {
			e.printStackTrace();
			return new Font(filename, Font.PLAIN, size);
		} catch (IOException e) {
			e.printStackTrace();
			return new Font(filename, Font.PLAIN, size);
		}
	}
	
}
