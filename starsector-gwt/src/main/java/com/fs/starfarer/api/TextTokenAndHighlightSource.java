package com.fs.starfarer.api;

import com.fs.starfarer.stubs.java.awt.Color;
import java.util.Map;

public interface TextTokenAndHighlightSource {
	/**
	 * For the description that shows up in the tooltip.
	 * @return
	 */
	Map<String, String> getTokenReplacements();
	
	/**
	 * For the description, which is shown in the tooltip.
	 * @return
	 */
	String [] getHighlights();
	
	Color [] getHighlightColors();
}
