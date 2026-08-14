import com.fs.starfarer.TextureUploadCompat;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.nio.file.Paths;
import javax.imageio.ImageIO;

/** Compare bulk texture preparation to the stock per-pixel algorithm on real assets. */
public final class VerifyTexturePreparedAssets {
    private static final String[] ASSETS = {
        "ships/lasher/lasher_base.png",
        "cursors/cursor_blue.png",
        "hud/player_status_bg2.png",
        "fx/shields128c.png",
        "illustrations/sf_splash_compressed.jpg"
    };

    private VerifyTexturePreparedAssets() {}

    public static void main(String[] args) throws Exception {
        if (args.length != 1) throw new IllegalArgumentException("usage: VerifyTexturePreparedAssets <graphics-root>");
        Path root = Paths.get(args[0]);
        for (String rel : ASSETS) {
            Path path = root.resolve(rel);
            BufferedImage image = ImageIO.read(path.toFile());
            if (image == null) throw new AssertionError("ImageIO returned null: " + path);
            TextureUploadCompat.PreparedTexture actual = TextureUploadCompat.prepareTexture(image);
            Expected expected = stockReference(image);
            assertBuffer(path.toString(), actual.getBuffer(), expected.buffer);
            assertColor(path + " average", actual.getAverageColor(), expected.average);
            assertColor(path + " median", actual.getMedianColor(), expected.median);
            assertColor(path + " accent", actual.getAccentColor(), expected.accent);
            image.flush();
            System.out.println("VerifyTexturePreparedAssets: OK " + rel);
        }
    }

    private static Expected stockReference(BufferedImage image) {
        int width=image.getWidth(), height=image.getHeight();
        int pw=2, ph=2; while(pw<width)pw*=2; while(ph<height)ph*=2;
        boolean alpha=image.getColorModel().hasAlpha(); int channels=alpha?4:3;
        ByteBuffer out=ByteBuffer.allocateDirect(pw*ph*channels);
        int[] pixel=new int[alpha?4:3];
        float sr=0,sg=0,sb=0,count=0; float[] hr=new float[256],hg=new float[256],hb=new float[256];
        for(int y=0;y<height;y++) for(int x=0;x<width;x++) {
            image.getRaster().getPixel(x,height-y-1,pixel);
            if(alpha && pixel[3]==0) continue;
            int d=(y*pw+x)*channels;
            out.put(d,(byte)pixel[0]); out.put(d+1,(byte)pixel[1]); out.put(d+2,(byte)pixel[2]);
            if(alpha) out.put(d+3,(byte)pixel[3]);
            sr+=pixel[0]; sg+=pixel[1]; sb+=pixel[2]; hr[pixel[0]]++; hg[pixel[1]]++; hb[pixel[2]]++; count++;
        }
        Color avg=Color.white, median=Color.white, accent=Color.white;
        if(count>0) {
            avg=new Color(clamp((int)(sr/count)),clamp((int)(sg/count)),clamp((int)(sb/count)),255);
            float half=count*0.5f;
            median=new Color(clamp((int)weightedHigh(hr,half)),clamp((int)weightedHigh(hg,half)),clamp((int)weightedHigh(hb,half)),255);
            accent=new Color(clamp((int)low(hr,half)),clamp((int)low(hg,half)),clamp((int)weightedHigh(hb,count)),255);
        }
        out.position(0); out.limit(out.capacity()); return new Expected(out,avg,median,accent);
    }

    private static void assertBuffer(String label, ByteBuffer a, ByteBuffer e) {
        ByteBuffer aa=a.duplicate(), ee=e.duplicate(); aa.position(0); ee.position(0);
        if(aa.remaining()!=ee.remaining()) throw new AssertionError(label+" length "+aa.remaining()+" != "+ee.remaining());
        for(int i=0;i<aa.remaining();i++) if(aa.get(i)!=ee.get(i)) throw new AssertionError(label+" byte mismatch at "+i+" actual="+(aa.get(i)&255)+" expected="+(ee.get(i)&255));
    }
    private static void assertColor(String label, Color a, Color e){ if(!a.equals(e)) throw new AssertionError(label+" actual="+a+" expected="+e); }
    private static int clamp(int v){return v<0?0:(v>255?255:v);}
    private static float low(float[] h,float threshold){float a=0;for(int i=0;i<=255;i++){a+=h[i];if(a>=threshold)return i;}return 0;}
    private static float weightedHigh(float[] h,float threshold){float a=0,w=0;for(int i=255;i>=0;i--){float v=h[i],take=v;if(a+v>threshold)take=threshold-a;a+=take;w+=i*take;if(a>=threshold)break;}return a>0?w/a:0;}
    private static final class Expected { final ByteBuffer buffer; final Color average,median,accent; Expected(ByteBuffer b,Color a,Color m,Color c){buffer=b;average=a;median=m;accent=c;} }
}
