package org.lwjgl.opengl;

public final class PixelFormat implements PixelFormatLWJGL {
    public PixelFormat() {}
    public PixelFormat(int alpha, int depth, int stencil) {}
    public PixelFormat(int alpha, int depth, int stencil, int samples) {}
    public PixelFormat(int bpp, int alpha, int depth, int stencil, int samples) {}
    public PixelFormat(int bpp, int alpha, int depth, int stencil, int samples, int num_aux_buffers, int accum_bpp, int accum_alpha, boolean stereo) {}
    public PixelFormat(int bpp, int alpha, int depth, int stencil, int samples, int num_aux_buffers, int accum_bpp, int accum_alpha, boolean stereo, boolean floating_point) {}
    
    public int getBitsPerPixel() { return 32; }
    public int getAlphaBits() { return 8; }
    public int getDepthBits() { return 24; }
    public int getStencilBits() { return 8; }
    public int getSamples() { return 0; }
}
