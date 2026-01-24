import os

root_dir = r"C:\Users\adyba\ss2-wasm\starsector-gwt\src\main\java"

replacements = {
    "import java.awt.Color;": "import com.fs.starfarer.stubs.java.awt.Color;",
    "import java.awt.image.BufferedImage;": "import com.fs.starfarer.stubs.java.awt.image.BufferedImage;",
    "import java.awt.image.Raster;": "import com.fs.starfarer.stubs.java.awt.image.Raster;",
    "import javax.imageio.ImageIO;": "import com.fs.starfarer.stubs.javax.imageio.ImageIO;",
    "import java.util.regex.Pattern;": "import com.fs.starfarer.stubs.java.util.regex.Pattern;",
    "import java.util.regex.Matcher;": "import com.fs.starfarer.stubs.java.util.regex.Matcher;",
    "import java.util.concurrent.atomic.AtomicLong;": "import com.fs.starfarer.stubs.java.util.concurrent.atomic.AtomicLong;",
    "import java.nio.Buffer;": "import com.fs.starfarer.stubs.java.nio.Buffer;",
    "import java.nio.ByteBuffer;": "import com.fs.starfarer.stubs.java.nio.ByteBuffer;",
    "import java.nio.FloatBuffer;": "import com.fs.starfarer.stubs.java.nio.FloatBuffer;",
    "import java.nio.ByteOrder;": "import com.fs.starfarer.stubs.java.nio.ByteOrder;",
    "import java.text.DecimalFormat;": "import com.fs.starfarer.stubs.java.text.DecimalFormat;",
    "import java.text.DecimalFormatSymbols;": "import com.fs.starfarer.stubs.java.text.DecimalFormatSymbols;",
    "import java.util.zip.Inflater;": "import com.fs.starfarer.stubs.java.util.zip.Inflater;",
    "import java.util.zip.Deflater;": "import com.fs.starfarer.stubs.java.util.zip.Deflater;",
    "import java.util.zip.DataFormatException;": "import com.fs.starfarer.stubs.java.util.zip.DataFormatException;",
    "import java.util.UUID;": "import com.fs.starfarer.stubs.java.util.UUID;",
    "import javax.xml.bind.DatatypeConverter;": "import com.fs.starfarer.stubs.javax.xml.bind.DatatypeConverter;",
}

count = 0
for root, dirs, files in os.walk(root_dir):
    if "stubs" in root: continue
    for file in files:
        if file.endswith(".java"):
            path = os.path.join(root, file)
            with open(path, "r", encoding="utf-8", errors="ignore") as f:
                content = f.read()
            
            new_content = content
            changed = False
            for old, new in replacements.items():
                if old in new_content:
                    new_content = new_content.replace(old, new)
                    changed = True
            
            if changed:
                with open(path, "w", encoding="utf-8") as f:
                    f.write(new_content)
                count += 1

print(f"Updated imports in {count} files.")
