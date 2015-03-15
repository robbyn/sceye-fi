# Embedding sceye-fi in your application #

First, you need to load a configuration file with one of the load() static methods in class `org.tastefuljava.sceyefi.conf.EyeFiConf`:

```
    public static EyeFiConf load() throws IOException;
    public static EyeFiConf load(File file) throws IOException;
    public static EyeFiConf load(URL url) throws IOException;
```

The one that takes no parameter will try to find a Settings.xml file either in `<user.home>/Application Data/Eye-Fi/` or `<user.home>/Library/Eye-Fi/`.

An Eye-Fi server is started by invoking the start method of class `org.tastefuljava.sceyefi.server.EyeFiServer`. The method returns an instance of the `EyeFiServer` class. To shut it down, call the close() method:

```
    EyeFiConf conf = EyeFiConf.load();
    EyeFiServer server = EyeFiServer.start(conf, new DiskFileHandler());
    try {
        // ... a lot of great stuff
    } finally {
        server.close();
    }
```

As you can see in the above code, you can also pass a handler to the server. The handler must implement the `org.tastefuljava.sceyefi.spi.EyeFiHandler` interface. The class `org.tastefuljava.sceyefi.DiskFileHandler` implements that interface. It's an instance of that class that is passed to the Eye-Fi server when sceye-fi is used standalone.

You may want to write your own handler to customize the handling of the uploaded files.

When the server receives a file from an Eye-Fi card, it calls the `startUpload` method of the `EyeFiHandler`:

```
public UploadHandler startUpload(EyeFiCard card, String archiveName);
```

It passes the `EyeFiCard` object that it had found in the configuration, as well as the name of the archive being received.

The method returns an `UploadHandler` that is responsible for storing the uploaded files. The server calls the `handleFile` method for each received file (including the log files), then calls the `commit` method if everything is ok, or the `abort` method if anything went wrong, such as if the integrity check fails.

## Example: ##

```

public class FileEyeFiHandler implements EyeFiHandler {
    private File folder;

    public FileEyeFiHandler(File folder) {
        this.folder = folder;
    }

    public UploadHandler startUpload(EyeFiCard card, String archiveName) {
        return new UploadHandler() {
            private List<File> files = new ArrayList<File>();

            public void handleFile(String fileName, Date timestamp,
                    InputStream in) throws IOException {
                File file = new File(folder, fileName);
                OutputStream out = new FileOutputStream(file);
                try {
                    byte buf[] = new byte[4096];
                    for (int n = in.read(buf); n >= 0; n = in.read(buf)) {
                        out.write(buf, 0, n);
                    }
                } finally {
                    out.close();
                }
                file.setLastModified(timestamp.getTime());
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
```