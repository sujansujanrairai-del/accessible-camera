import java.io.File;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class VerifyZipStreams {
    public static void main(String[] args) {
        if (args.length == 0) {
            System.err.println("Usage: java VerifyZipStreams <zip-file> [expected-files...]");
            System.exit(1);
        }

        File zipFile = new File(args[0]);
        if (!zipFile.exists()) {
            System.err.println("File does not exist: " + zipFile.getAbsolutePath());
            System.exit(1);
        }

        try (ZipFile zf = new ZipFile(zipFile)) {
            Enumeration<? extends ZipEntry> entries = zf.entries();
            byte[] buffer = new byte[8192];
            int count = 0;

            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                count++;
                try (InputStream is = zf.getInputStream(entry)) {
                    while (is.read(buffer) != -1) {
                        // drain stream
                    }
                }
            }
            System.out.println("Verified " + count + " zip stream entries in " + zipFile.getName());
        } catch (Exception e) {
            System.err.println("Error verifying zip stream: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
