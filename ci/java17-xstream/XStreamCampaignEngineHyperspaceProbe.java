import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.io.xml.StaxDriver;
import com.thoughtworks.xstream.mapper.Mapper;
import com.thoughtworks.xstream.converters.reflection.ReflectionProvider;
import java.lang.reflect.Constructor;

public final class XStreamCampaignEngineHyperspaceProbe {
    public static void main(String[] args) throws Exception {
        configureJvmIdentity();
        System.out.println("XStreamCampaignEngineHyperspaceProbe: START");
        final Class<?> engineClass = Class.forName("com.fs.starfarer.campaign.CampaignEngine");
        final Class<?> hyperspaceClass = Class.forName("com.fs.starfarer.campaign.Hyperspace");
        final Class<?> converterClass = Class.forName("com.fs.starfarer.campaign.save.O");
        final XStream xstream = new XStream(new StaxDriver());
        xstream.alias("CampaignEngine", engineClass);
        xstream.alias("Hyperspace", hyperspaceClass);
        xstream.setMode(XStream.ID_REFERENCES);
        final Constructor<?> ctor = converterClass.getConstructor(Mapper.class, ReflectionProvider.class);
        final Converter converter = (Converter) ctor.newInstance(xstream.getMapper(), xstream.getReflectionProvider());
        xstream.registerConverter(converter);
        System.out.println("XStreamCampaignEngineHyperspaceProbe: converter-ready provider="
                + xstream.getReflectionProvider().getClass().getName());
        final String xml = "<CampaignEngine><isFastForwardIteration>false</isFastForwardIteration><hyperspace></hyperspace></CampaignEngine>";
        System.out.println("XStreamCampaignEngineHyperspaceProbe: fromXML-before");
        final Object engine = xstream.fromXML(xml);
        System.out.println("XStreamCampaignEngineHyperspaceProbe: fromXML-after class="
                + (engine == null ? "<null>" : engine.getClass().getName()));
        if (engine == null || engine.getClass() != engineClass) {
            throw new AssertionError("CampaignEngine hyperspace probe returned wrong object");
        }
        final java.lang.reflect.Field field = engineClass.getDeclaredField("hyperspace");
        field.setAccessible(true);
        final Object hyperspace = field.get(engine);
        System.out.println("XStreamCampaignEngineHyperspaceProbe: hyperspace="
                + (hyperspace == null ? "<null>" : hyperspace.getClass().getName()));
        if (hyperspace == null || hyperspace.getClass() != hyperspaceClass) {
            throw new AssertionError("Hyperspace field was not restored");
        }
        System.out.println("XStreamCampaignEngineHyperspaceProbe: DONE");
    }

    private static void configureJvmIdentity() {
        setIfMissing("java.vm.vendor", "CheerpJ");
        setIfMissing("java.vendor", "CheerpJ");
        setIfMissing("java.vm.name", "CheerpJ Runtime");
        setIfMissing("java.specification.version", "1.8");
        setIfMissing("java.specification.vendor", "Oracle Corporation");
        setIfMissing("java.specification.name", "Java Platform API Specification");
    }

    private static void setIfMissing(final String key, final String value) {
        final String current = System.getProperty(key);
        if (current == null || current.trim().length() == 0) System.setProperty(key, value);
    }
}
