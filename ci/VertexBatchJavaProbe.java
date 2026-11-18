package org.lwjgl.opengl;

import java.lang.reflect.Field;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import sun.misc.Unsafe;

// The Python driver compiles the production GL11 with only its native entry
// points replaced by this recording backend. Geometry reads use the real direct
// address, not GL11's FloatBuffer, so address/position/order failures are visible.
public final class VertexBatchJavaProbe {
    private static final Unsafe MEMORY;
    private static final List<String> EVENTS = new ArrayList<String>();
    private static final float[] COLOR = {1, 1, 1, 1};
    private static boolean enabled, rendererImmediateActive;
    private static int batchCalls, quadCalls, scalarCalls, maxBatch, checks;
    static {
        try {
            Field f = Unsafe.class.getDeclaredField("theUnsafe");
            f.setAccessible(true);
            MEMORY = (Unsafe) f.get(null);
        } catch (Exception e) { throw new AssertionError(e); }
    }
    static String bits(float n) { return Integer.toHexString(Float.floatToRawIntBits(n)); }
    public static boolean begin(int mode) {
        EVENTS.add("begin:" + mode);
        rendererImmediateActive=true;
        return enabled;
    }
    public static void end() { rendererImmediateActive=false; EVENTS.add("end"); }
    public static void color(float r, float g, float b, float a) {
        COLOR[0]=r; COLOR[1]=g; COLOR[2]=b; COLOR[3]=a;
        EVENTS.add("color:"+bits(r)+":"+bits(g)+":"+bits(b)+":"+bits(a));
    }
    private static void recordVertex(float x,float y,float z,float s,float t) {
        EVENTS.add("v:"+bits(x)+":"+bits(y)+":"+bits(z)+":"+bits(s)+":"+bits(t)
                +":"+bits(COLOR[0])+":"+bits(COLOR[1])+":"+bits(COLOR[2])+":"+bits(COLOR[3]));
    }
    public static void vertex(float x,float y,float z,float s,float t) {
        scalarCalls++;
        recordVertex(x,y,z,s,t);
    }
    public static void batch(long address,int count) {
        require(rendererImmediateActive,"batch after display list ended immediate mode");
        require(address > 0 && (address & 3) == 0,"invalid direct address");
        require(count > 0 && count <= 1024,"unbounded batch");
        try {
            FloatBuffer transport=(FloatBuffer)field("vertexBatchBuffer").get(null);
            require(transport.position()==0,"native receiver observed nonzero direct-buffer position");
            require(transport.limit()==transport.capacity(),"transport limit changed");
            require(field("vertexBatchAddress").getLong(null)==address,"cached native address changed");
        } catch (Exception error) { throw new AssertionError(error); }
        batchCalls++; maxBatch=Math.max(maxBatch,count);
        for(int i=0;i<count;i++) {
            long p=address+i*20L;
            recordVertex(MEMORY.getFloat(p),MEMORY.getFloat(p+4),MEMORY.getFloat(p+8),
                    MEMORY.getFloat(p+12),MEMORY.getFloat(p+16));
        }
    }
    public static void quad(
            float x0,float y0,float z0,float s0,float t0,
            float x1,float y1,float z1,float s1,float t1,
            float x2,float y2,float z2,float s2,float t2,
            float x3,float y3,float z3,float s3,float t3) {
        require(rendererImmediateActive,"quad after display list ended immediate mode");
        try {
            FloatBuffer transport=(FloatBuffer)field("vertexBatchBuffer").get(null);
            require(transport.position()==0,"quad changed private direct-buffer position");
        } catch (Exception error) { throw new AssertionError(error); }
        batchCalls++;quadCalls++;maxBatch=Math.max(maxBatch,4);
        recordVertex(x0,y0,z0,s0,t0);recordVertex(x1,y1,z1,s1,t1);
        recordVertex(x2,y2,z2,s2,t2);recordVertex(x3,y3,z3,s3,t3);
    }

    public static void nativeEvent(String name,Object... args) {
        EVENTS.add(name+Arrays.toString(args));
        // Exercise a list changing the renderer's current color without changing
        // any Java-side mirror. Batching must consume the new renderer color.
        if(name.equals("nglCallList")) {
            COLOR[0]=.2f; COLOR[1]=.7f; COLOR[2]=.4f; COLOR[3]=.8f;
            if(((Number)args[0]).intValue()==99) {
                rendererImmediateActive=false;
                EVENTS.add("list-ended-primitive");
            }
        }
    }
    private static Field field(String name) throws Exception {
        Field f=GL11.class.getDeclaredField(name);f.setAccessible(true);return f;
    }
    private static void reset(boolean on) throws Exception {
        enabled=on; rendererImmediateActive=false; EVENTS.clear();batchCalls=quadCalls=scalarCalls=maxBatch=0;
        COLOR[0]=COLOR[1]=COLOR[2]=COLOR[3]=1;
        field("vertexBatchCount").setInt(null,0);
        field("vertexBatchActive").setBoolean(null,false);
        field("compilingList").setBoolean(null,false);
        field("currentTexCoordS").setFloat(null,0);
        field("currentTexCoordT").setFloat(null,0);
        field("vertexBatchUnavailable").setBoolean(null,false);
    }
    private static void require(boolean condition,String message) {
        checks++;
        if(!condition)throw new AssertionError(message);
    }
    private static void sequence(int count, boolean stateChanges) {
        Random random=new Random(0x51a7);
        GL11.glColor4f(.25f,.5f,.75f,1);
        GL11.glTexCoord2f(-0.0f,Float.intBitsToFloat(0x3eaaaaab));
        GL11.glBegin(7);
        for(int i=0;i<count;i++) {
            if(i%13==0) GL11.glTexCoord2f(random.nextFloat(),random.nextFloat());
            if(stateChanges && i%37==0) GL11.glColor4ub((byte)i,(byte)(i>>1),(byte)255,(byte)179);
            if(stateChanges && i%83==0) GL11.glColor3f(.1f,.9f,.3f);
            if(stateChanges && i%101==0) GL11.glTranslatef(1,2,3);
            if(stateChanges && i%127==0) GL11.glPushAttrib(0x4000);
            if(stateChanges && i%139==0) GL11.glPopAttrib();
            if(stateChanges && i%149==0) GL11.glBindTexture(0x0de1,3);
            if(stateChanges && i%173==0) GL11.glScissor(2,3,77,88);
            float x=i==0 ? -0.0f : i==1 ? Float.NaN : i==2 ? Float.POSITIVE_INFINITY : random.nextFloat()*12345-4321;
            if(i%3==0) GL11.glVertex2f(x,-i);
            else if(i%3==1) GL11.glVertex3f(x,i,random.nextFloat());
            else GL11.glVertex3d(x,i,Math.PI);
        }
        GL11.glEnd();
        GL11.glBegin(4);
        GL11.glVertex3f(1,2,3); // Texcoords deliberately persist on the Java side.
        GL11.glEnd();
    }
    private static void compare(int count,boolean changes) throws Exception {
        reset(false);sequence(count,changes);
        List<String> control=new ArrayList<String>(EVENTS);
        require(batchCalls==0,"OFF submitted a batch");
        int direct=scalarCalls;
        reset(true);sequence(count,changes);
        require(EVENTS.equals(control),"event/Float32 ordering differs count="+count+" changes="+changes);
        require(scalarCalls==0,"eligible ON vertices escaped buffering");
        require(batchCalls>0,"candidate never ran");
        if(!changes && count>1024)require(batchCalls<direct,"native transitions not reduced");
        require(maxBatch<=1024,"capacity exceeded");
        if(!changes && (count==4 || count==1028))require(quadCalls==1,"four-vertex remainder missed direct transport");
        if(!changes && count==5)require(quadCalls==0,"nonquad batch did not retain generic transport");
        require(((FloatBuffer)field("vertexBatchBuffer").get(null)).position()==0,"direct buffer position moved");
    }
    private static void listSequence(int list) {
        GL11.glBegin(7);
        GL11.glVertex2f(1,2);
        GL11.glCallList(list);
        GL11.glVertex2f(3,4);
        GL11.glColor3f(.4f,.5f,.6f);
        GL11.glVertex2f(5,6);
        GL11.glEnd();
        GL11.glBegin(4);GL11.glVertex2f(7,8);GL11.glEnd();
    }
    private static void checkListBoundary(int list) throws Exception {
        reset(false);listSequence(list);
        List<String> control=new ArrayList<String>(EVENTS);
        reset(true);listSequence(list);
        require(EVENTS.equals(control),"list boundary reordered geometry/color/end");
        require(batchCalls==2,"list boundary should batch prefix and next primitive only");
        require(scalarCalls==2,"list remainder should retain scalar behavior");
    }
    private static void checkQuadDirectTransport() throws Exception {
        float[][] rows={
            {-0.0f,Float.NaN,Float.POSITIVE_INFINITY,.25f,-0.0f},
            {Float.NEGATIVE_INFINITY,Float.MIN_VALUE,Float.MAX_VALUE,-.5f,1.0f},
            {Float.intBitsToFloat(0x3eaaaaab),-13.25f,Float.intBitsToFloat(0x80000001),0,Float.NaN},
            {12345.5f,-123.75f,Float.NaN,Float.POSITIVE_INFINITY,Float.NEGATIVE_INFINITY}
        };
        reset(false);
        GL11.glBegin(7);
        for(float[] row:rows){GL11.glTexCoord2f(row[3],row[4]);GL11.glVertex3f(row[0],row[1],row[2]);}
        GL11.glEnd();
        List<String> control=new ArrayList<String>(EVENTS);
        reset(true);
        GL11.glBegin(7);
        FloatBuffer buffer=(FloatBuffer)field("vertexBatchBuffer").get(null);
        long address=field("vertexBatchAddress").getLong(null);
        for(int i=0;i<buffer.capacity();i++)MEMORY.putFloat(address+i*4L,123456.0f);
        for(float[] row:rows){GL11.glTexCoord2f(row[3],row[4]);GL11.glVertex3f(row[0],row[1],row[2]);}
        GL11.glEnd();
        require(EVENTS.equals(control),"primitive quad parameters changed Float32 geometry or ordering");
        require(quadCalls==1 && batchCalls==1 && scalarCalls==0,"quad coverage counters differ");
        for(int i=0;i<buffer.capacity();i++)require(MEMORY.getFloat(address+i*4L)==123456.0f,
                "four-vertex flush wrote direct memory");
        require(buffer.position()==0,"quad transport moved direct-buffer position");
    }

    private static void checkStagedPrefix() throws Exception {
        reset(true);
        GL11.glBegin(7);
        FloatBuffer buffer=(FloatBuffer)field("vertexBatchBuffer").get(null);
        float[] data=(float[])field("vertexBatchData").get(null);
        long address=field("vertexBatchAddress").getLong(null);
        require(data.length==1024*5,"staging array is not bounded to one batch");
        for(int i=0;i<buffer.capacity();i++)MEMORY.putFloat(address+i*4L,123456.0f);
        GL11.glTexCoord2f(.25f,-0.0f);
        GL11.glVertex3f(-0.0f,Float.POSITIVE_INFINITY,Float.NaN);
        require(MEMORY.getFloat(address)==123456.0f,"vertex store touched direct memory before flush");
        require(Float.floatToRawIntBits(data[0])==Float.floatToRawIntBits(-0.0f),"array lost signed zero");
        GL11.glEnd();
        require(batchCalls==1 && scalarCalls==0,"one staged vertex did not use one flush");
        require(Float.floatToRawIntBits(MEMORY.getFloat(address))==Float.floatToRawIntBits(-0.0f),"bulk copy lost signed zero");
        require(MEMORY.getFloat(address+4)==Float.POSITIVE_INFINITY,"bulk copy changed infinity");
        require(Float.isNaN(MEMORY.getFloat(address+8)),"bulk copy changed NaN");
        require(MEMORY.getFloat(address+12)==.25f,"bulk copy changed texcoord");
        require(Float.floatToRawIntBits(MEMORY.getFloat(address+16))==Float.floatToRawIntBits(-0.0f),"bulk copy lost texcoord signed zero");
        for(int i=5;i<buffer.capacity();i++)require(MEMORY.getFloat(address+i*4L)==123456.0f,
                "flush copied beyond the used staging prefix");
        require(buffer.position()==0,"bulk flush did not reset private buffer position");
    }

    public static void main(String[] args) throws Exception {
        require(field("vertexBatchBuffer").get(null)==null && field("vertexBatchData").get(null)==null,
                "transport allocation was not lazy");
        reset(false);GL11.glBegin(4);GL11.glVertex2f(1,2);GL11.glEnd();
        require(field("vertexBatchBuffer").get(null)==null && field("vertexBatchData").get(null)==null,
                "OFF allocated staging storage");
        for(int count:new int[]{0,1,2,3,4,5,1023,1024,1025,1028,2048,2501}) {
            compare(count,false);compare(count,true);
        }
        checkListBoundary(4);checkListBoundary(99);
        Object firstBuffer=field("vertexBatchBuffer").get(null);
        Object firstData=field("vertexBatchData").get(null);
        long firstAddress=field("vertexBatchAddress").getLong(null);
        compare(2049,false);
        require(field("vertexBatchBuffer").get(null)==firstBuffer,"direct allocation repeated");
        require(field("vertexBatchData").get(null)==firstData,"staging array allocation repeated");
        require(field("vertexBatchAddress").getLong(null)==firstAddress,"direct address was not reused");
        checkStagedPrefix();
        checkQuadDirectTransport();

        reset(true);
        GL11.glTexCoord2f(.33f,.66f);
        GL11.glNewList(1,0x1300);
        GL11.glBegin(7);GL11.glTexCoord2f(.9f,.1f);
        GL11.glVertex2f(1,2);GL11.glVertex3f(3,4,5);GL11.glEnd();GL11.glEndList();
        require(batchCalls==0 && scalarCalls==2,"list compilation captured mutable batched memory");
        GL11.glBegin(4);GL11.glVertex2f(6,7);GL11.glEnd();
        require(batchCalls==1,"batching did not resume after compiling list");
        String lastVertex=EVENTS.get(EVENTS.size()-2);
        require(lastVertex.contains(":"+bits(.33f)+":"+bits(.66f)+":"),"list texcoord restore changed");

        reset(true);
        GL11.glVertex3f(1,2,3);
        require(scalarCalls==1 && batchCalls==0,"outside-begin scalar fallback missing");
        GL11.glBegin(4);GL11.glVertex2f(1,2);
        enabled=false;GL11.glVertex2f(3,4);GL11.glEnd();
        require(batchCalls==1 && scalarCalls==1,"toggle changed midway through primitive");
        GL11.glBegin(4);GL11.glVertex2f(5,6);GL11.glEnd();
        require(scalarCalls==2,"next primitive did not adopt disabled toggle");

        reset(true);
        field("vertexBatchBuffer").set(null,null);
        field("vertexBatchUnavailable").setBoolean(null,true);
        GL11.glBegin(4);GL11.glVertex2f(2,3);GL11.glEnd();
        require(batchCalls==0 && scalarCalls==1,"unavailable direct-memory fallback failed");

        System.out.println("verify-vertex-batch-java: OK checks="+checks
                +" raw direct-address Float32 parity, native order, lists, bounded chunks, toggles, fallback, lazy array reuse, prefix-only bulk copies and direct quad arguments without NIO writes");
    }
}
