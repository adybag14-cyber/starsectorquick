import java.util.Properties;

public class PropertyPrinter {
    public static void main(String[] args) {
        Properties props = System.getProperties();
        props.forEach((k, v) -> System.out.println(k + "=" + v));
    }
}
