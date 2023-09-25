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
import sun.misc.Signal;
import sun.misc.SignalHandler;

import javax.net.ssl.SSLContext;
import java.util.Scanner;

public class NeoGuard {

    public static void main(String[] args) throws Exception {
        NeoGuard neoGuard = new NeoGuard();
        neoGuard.start(neoGuard);

        Runtime.getRuntime().addShutdownHook(new Thread(neoGuard::stop));

        Scanner scanner = new Scanner(System.in);

        System.out.println("Type something and press Enter (or CTRL+C to shutdown):");
        System.out.println("Command list:");
        System.out.println("createAccess");
        System.out.println("deleteAccess TOKEN");
        System.out.println("stop");
        System.out.println("restartWebService");

        while (true) {
            if (scanner.hasNextLine()) {
                String userInput = scanner.nextLine();
                if(userInput.equalsIgnoreCase("createAccess")) {
                    System.out.println("Here is your access token:\n\n" + NeoGuard.getKeyManager().registerKey().get(1));
                } else if(userInput.toLowerCase().startsWith("deleteaccess")) {
                    NeoGuard.getKeyManager().getKeys().remove(userInput.split(" ")[1]);
                } else if(userInput.equalsIgnoreCase("stop")) {
                    System.out.println("Stopping...");
                    neoGuard.stop();
                    System.exit(0);
                } else if(userInput.equalsIgnoreCase("restartWebService")) {
                    System.out.println("Restarting...");
                    neoGuard.stop();
                    neoGuard.start(neoGuard);
                    System.out.println("Restarting finished!");
                }
            }
        }
    }

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

        Signal.handle(new Signal("INT"), new SignalHandler() {
            @Override
            public void handle(Signal signal) {
                System.out.println("Received SIGINT (Ctrl+C). Force shutting down...");
                waitForRunningProcessesToBeZero();
                // Perform cleanup or any necessary operations before exiting
                System.exit(0);
            }
        });
    }

    public static void waitForRunningProcessesToBeZero() {
        // Wait until NeoGuard.runningProcesses is 0
        while (NeoGuard.runningProcesses <= 0) {
            try {
                System.out.println("Waiting for NeoGuard.runningProcesses to be 0...");
                Thread.sleep(1000); // Sleep for 1 second (adjust as needed)
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

}
