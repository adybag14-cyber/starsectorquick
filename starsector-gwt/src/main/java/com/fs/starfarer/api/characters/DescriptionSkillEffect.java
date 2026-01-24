package com.fs.starfarer.api.characters;

import com.fs.starfarer.stubs.java.awt.Color;

public interface DescriptionSkillEffect {
	Color getTextColor();
	String getString();
	String [] getHighlights();
	Color [] getHighlightColors();
}
