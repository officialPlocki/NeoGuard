package co.plocki.neoguard.server;

import co.plocki.neoguard.server.binary.BinaryManager;
import co.plocki.neoguard.server.cache.DataCache;
import co.plocki.neoguard.server.config.Config;
import co.plocki.neoguard.server.key.KeyManager;
import co.plocki.neoguard.server.util.SSLContextGenerator;
import co.plocki.neoguard.server.webserver.DataHandler;
import io.undertow.Undertow;
import io.undertow.UndertowOptions;
import io.undertow.server.HttpHandler;
import io.undertow.server.handlers.PathHandler;

import javax.net.ssl.SSLContext;

public class NeoGuard {

    private Undertow server;
    private static Config config;
    private static BinaryManager binaryManager;
    private static KeyManager keyManager;
    private static DataCache dataCache;

    public static boolean debug;
    public static boolean local_only;
    public static int pass_length;
    private static NeoGuard runningInstance;

    public static volatile int runningProcesses = 0;

    public void start(NeoGuard classInstance) throws Exception {
        runningInstance = classInstance;

        displayStartingInfo();

        displayLoadingAnimation();

        config = new Config();
        debug = config.isDebugEnabled();
        binaryManager = new BinaryManager();
        keyManager = new KeyManager();
        keyManager.loadKeysFromFile();
        dataCache = new DataCache();

        dataCache.init();

        if (server == null) {
            HttpHandler dataHandler = new DataHandler();

            PathHandler pathHandler = new PathHandler();
            pathHandler.addPrefixPath("/json-transfer", dataHandler);

            if(Boolean.parseBoolean(String.valueOf(config.getConfigurationData("security", "ssl")))) {
                String[] protocols = {"TLSv1.2"};
                String[] cipherSuites = {"TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256"};

                SSLContext sslContext = SSLContextGenerator.createSSLContext(String.valueOf(config.getConfigurationData("security", "ssl_cert")), String.valueOf(config.getConfigurationData("security", "ssl_key")), protocols, cipherSuites);

                server = Undertow.builder()
                        .addHttpsListener(Integer.parseInt(String.valueOf(config.getConfigurationData("server", "port"))), String.valueOf(config.getConfigurationData("server", "host")), sslContext)
                        .setServerOption(UndertowOptions.ENABLE_HTTP2, true)
                        .setHandler(pathHandler)
                        .build();
            } else {
                server = Undertow.builder()
                        .addHttpListener(Integer.parseInt(String.valueOf(config.getConfigurationData("server", "port"))), String.valueOf(config.getConfigurationData("server", "host")))
                        .setServerOption(UndertowOptions.ENABLE_HTTP2, true)
                        .setHandler(pathHandler)
                        .build();
            }

            local_only = Boolean.parseBoolean(String.valueOf(config.getConfigurationData("security", "local_access_only")));

            pass_length = Integer.parseInt(String.valueOf(config.getConfigurationData("security", "pass_length")));
        }

        // Display loading animation

        server.start();

        System.out.println("Server started on port " + config.getConfigurationData("server", "port"));

        Runtime.getRuntime().addShutdownHook(new Thread(this::stop));
    }

    private void displayStartingInfo() {
        System.err.println();
        System.err.println("                     NeoGuard Server - Starting");
        System.err.println("==============================================================");
        System.err.println(" WARNING:");
        System.err.println(" This software is for testing purposes only!");
        System.err.println(" Don't use it on production systems with real data!");
        System.err.println("==============================================================");
        System.err.println();

        String versionInfo = "NeoGuard Version: 1.0"; // Replace with actual version info
        System.out.println(versionInfo);
        System.out.println();
        System.out.println("Starting...");
    }

    private void displayLoadingAnimation() {
        // Define the loading animation frames using ASCII characters
        String[] frames = {
                "  Loading...",
                "  Loading.",
                "  Loading..",
                "  Loading..."
        };

        for (String frame : frames) {
            System.out.print("\r" + frame);
            try {
                Thread.sleep(500); // Adjust the delay between frames if needed
            } catch (InterruptedException e) {
                // Handle interruption if needed
            }
        }

        System.out.println(); // Print a new line after the animation
    }

    public static void fixRuntimeError() throws Exception {
        if(runningInstance.server != null) {
            runningInstance.stop();
        }
        runningInstance.start(runningInstance);
    }

    public static DataCache getDataCache() {
        return dataCache;
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
