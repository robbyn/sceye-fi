package org.tastefuljava.sceyefi.server;

import org.tastefuljava.sceyefi.spi.UploadHandler;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import org.tastefuljava.sceyefi.conf.EyeFiCard;
import org.tastefuljava.sceyefi.conf.Media;
import org.tastefuljava.sceyefi.spi.EyeFiHandler;

public class DiskFileHandler implements EyeFiHandler {
    private static final int BUFFER_SIZE = 4096;

    public void handleFile(EyeFiCard card, String fileName, InputStream in)
            throws IOException {
        Media media = card.getMedia(Media.TYPE_PHOTO);
        if (media == null) {
            throw new IOException("No photo media in Eye-Fi settings");
        }
        File folder = media.getFolder();
        if (!folder.isDirectory() && !folder.mkdirs()) {
            throw new IOException("Could not create folder " + folder);
        }
        File file = new File(folder, fileName);
        OutputStream out = new FileOutputStream(file);
        try {
            byte buf[] = new byte[BUFFER_SIZE];
            for (int n = in.read(buf); n >= 0; n = in.read(buf)) {
                out.write(buf, 0, n);
            }
        } finally {
            out.close();
        }
    }

    public UploadHandler startUpload(final EyeFiCard card, String archiveName) {
        return new UploadHandler() {
            private List<File> files = new ArrayList<File>();

            public void handleFile(String fileName, InputStream in)
                    throws IOException {
                Media media = card.getMedia(Media.TYPE_PHOTO);
                if (media == null) {
                    throw new IOException("No photo media in Eye-Fi settings");
                }
                File folder = media.getFolder();
                if (!folder.isDirectory() && !folder.mkdirs()) {
                    throw new IOException("Could not create folder " + folder);
                }
                File file = new File(folder, fileName);
                OutputStream out = new FileOutputStream(file);
                try {
                    byte buf[] = new byte[BUFFER_SIZE];
                    for (int n = in.read(buf); n >= 0; n = in.read(buf)) {
                        out.write(buf, 0, n);
                    }
                } finally {
                    out.close();
                }
                files.add(file);
            }

            public void abort() {
                while (!files.isEmpty()) {
                    File file = files.remove(0);
                    file.delete();
                }
            }

            public void commit() {
                files.clear();
            }
        };
    }
}
