/**
 * 
 */
package nl.thanod.dragnshare;

import java.awt.Color;

/**
 * @author nilsdijk
 */
public enum ColorScheme {
	OFFERED(fromHex("#FFF0D0"), fromHex("#FFE0C0")),
	RECEIVED(fromHex("#ddFFdd"), fromHex("#eeFFee")),
	DEFAULT(fromHex("#dFdFFF"), fromHex("#eFeFFF")),
	ERROR(fromHex("#FFd0d0"), fromHex("#FFe0e0")),
	SELECTED(fromHex("#aaaaFF"), fromHex("#9999ff"));

	public final Color odd;
	public final Color even;

	private ColorScheme(Color odd, Color even) {
		this.odd = odd;
		this.even = even;
	}

	public Color getColor(int index) {
		if (index % 2 == 0)
			return this.even;
		return this.odd;
	}
	
	private static Color fromHex(String hex){
		if (!hex.startsWith("#") || hex.length() != 7)
			throw new IllegalArgumentException("not a hex value");
		String r = hex.substring(1,3);
		String g = hex.substring(3,5);
		String b = hex.substring(5,7);		
		return new Color(Integer.parseInt(r,16),Integer.parseInt(g,16),Integer.parseInt(b,16));
	}
}
