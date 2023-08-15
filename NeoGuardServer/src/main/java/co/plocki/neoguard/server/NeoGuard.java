package co.plocki.neoguard.server;

import co.plocki.neoguard.server.binary.BinaryManager;
import co.plocki.neoguard.server.config.Config;
import co.plocki.neoguard.server.key.KeyManager;
import co.plocki.neoguard.server.webserver.DataHandler;
import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import io.undertow.server.handlers.PathHandler;

import java.util.concurrent.CountDownLatch;

public class NeoGuard {

    private Undertow server;
    private static Config config;
    private static BinaryManager binaryManager;
    private static KeyManager keyManager;

    public static boolean debug;

    public static volatile int runningProcesses = 0;

    public void start() throws Exception {

        config = new Config();
        debug = config.isDebugEnabled();
        binaryManager = new BinaryManager();
        keyManager = new KeyManager();
        keyManager.loadKeysFromFile();

        if(server == null) {
            HttpHandler dataHandler = new DataHandler();

            PathHandler pathHandler = new PathHandler();
            pathHandler.addPrefixPath("/json-transfer", dataHandler);

            server = Undertow.builder()
                    .addHttpListener(8080, "0.0.0.0")
                    .setHandler(pathHandler)
                    .build();
        }

        server.start();

        System.out.println("Server started on port 8080");

        Runtime.getRuntime().addShutdownHook(new Thread(this::stop));
    }

    public static Config getConfig() {
        return config;
    }

    public static KeyManager getKeyManager() {
        return keyManager;
    }

    public Undertow getServer() {
        return server;
    }

    public static BinaryManager getBinaryManager() {
        return binaryManager;
    }

    public void stop() {
        if(server == null) {
            throw new RuntimeException("Undertow server isn't running!");
        }

        new Thread(() -> {
            while (NeoGuard.runningProcesses != 0) {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }).start();

        binaryManager.save();
        config.save();
    }

}
