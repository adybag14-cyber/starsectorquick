import java.io.*;

public class FileInstaller {
    public static void main(String[] args) {
        if (args.length < 2) return;
        String path = args[0];
        String content = args[1];
        try {
            File file = new File(path);
            file.getParentFile().mkdirs();
            try (FileWriter fw = new FileWriter(file)) {
                fw.write(content);
            }
            System.out.println("FileInstaller: Successfully installed " + path);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
