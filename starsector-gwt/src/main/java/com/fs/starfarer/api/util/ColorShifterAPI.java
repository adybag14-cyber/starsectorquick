package com.fs.starfarer.api.util;

import com.fs.starfarer.stubs.java.awt.Color;

public interface ColorShifterAPI {

	Color getBase();
	void setBase(Color base);
	Color getCurr();
	void shift(Object source, Color to, float durIn, float durOut, float shift);
	Color getCurrForBase(Color diffBase);
}
