package com.fs.starfarer.api.campaign;

import com.fs.starfarer.stubs.java.awt.Color;


public interface NascentGravityWellAPI extends SectorEntityToken {
	SectorEntityToken getTarget();

	Color getColorOverride();
	void setColorOverride(Color colorOverride);
}
