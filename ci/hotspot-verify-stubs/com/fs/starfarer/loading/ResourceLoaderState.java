package com.fs.starfarer.loading;

/**
 * Verification-only descriptor stub for HotSpot.
 *
 * The stock 0.98a-RC8 ResourceLoaderState contains obfuscated identifiers that
 * HotSpot rejects as class-format errors even though the browser/CheerpJ runtime
 * accepts the original class. Rules has ResourceLoaderState in a method
 * descriptor, so resolving Rules methods would otherwise fail before HotSpot can
 * verify Rules' transformed bytecode. This class is compiled only into a CI
 * verification directory and is never packaged into the runtime JARs.
 */
public final class ResourceLoaderState {
    private ResourceLoaderState() {}
}
