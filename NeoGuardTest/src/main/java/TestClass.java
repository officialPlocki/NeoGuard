import co.plocki.neoguard.client.NeoGuardClient;
import co.plocki.neoguard.server.NeoGuard;

import java.util.Base64;
import java.util.List;

public class TestClass {

    public static void main(String[] args) throws Exception {
        NeoGuard neoGuard = new NeoGuard();
        neoGuard.start();

        NeoGuardClient client = new NeoGuardClient();

        NeoGuardClient.PASSPHRASE = (String) NeoGuard.getKeyManager().registerKey().get(1);
        //System.out.println(new String(Base64.getDecoder().decode(NeoGuardClient.PASSPHRASE)));

        client.start();

/*
        System.out.println("testing output post: " + client.post("test1", List.of("t1", "t2", "t3"), List.of("td1", "td2", "td3")));
        System.out.println("testing output request: " + client.request("test1", List.of("t1", "t2", "t3")));
        System.out.println("testing output search arrays: " + client.searchArrays("t1", "test1"));
        System.out.println("testing output search threads: " + client.searchThreads("test1"));
        System.out.println("testing output update row: " + client.updateRow("test1", "t1", "0", "td800"));
        System.out.println("testing output request: " + client.request("test1", List.of("t1", "t2", "t3")));
        System.out.println("testing output delete row: " + client.deleteRow("test1", "t2", "1"));
        System.out.println("testing output request: " + client.request("test1", List.of("t1", "t2", "t3")));
        System.out.println("testing output delete array: " + client.deleteArray("test1", "t3"));
        System.out.println("testing output search array: " + client.searchArrays("t3", "test1"));
        System.out.println("testing output delete thread: " + client.deleteThread("test1"));
        System.out.println("testing output search thread: " + client.searchThreads("test1"));
*/

        int numberOfRequests = 1000000;

        long startTime = System.currentTimeMillis();

        System.out.println("Running " + numberOfRequests + " requests...");

        for(int i = 0; i < numberOfRequests; i++) {
            long requestStartTime = System.currentTimeMillis();
            updateLoadingAnimation(i, numberOfRequests);

            client.post("test1", List.of("t1", "t2", "t3"), List.of("td1", "td2", "td3"));
            client.request("test1", List.of("t1", "t2", "t3"));
            client.searchArrays("t1", "test1");
            client.searchThreads("test1");
            client.updateRow("test1", "t1", "0", "td800");
            client.request("test1", List.of("t1", "t2", "t3"));
            client.deleteRow("test1", "t2", "1");
            client.request("test1", List.of("t1", "t2", "t3"));
            client.deleteArray("test1", "t3");
            client.searchArrays("t3", "test1");
            client.deleteThread("test1");
            client.searchThreads("test1");

            long requestEndTime = System.currentTimeMillis();
            long requestTime = requestEndTime - requestStartTime;
            System.out.println(" - Request " + (i + 1) + " completed in " + requestTime + " milliseconds.");
        }

        long endTime = System.currentTimeMillis();

        long totalTime = endTime - startTime;

        System.out.println("All requests completed in " + totalTime + " milliseconds.");
        System.exit(3);
    }

    public static void updateLoadingAnimation(int current, int total) {
        int progress = (current * 100) / total;
        String animation = "◐◐◓◓◑◑◒◒";
        String status = "Processing " + progress + "%";

        System.out.print("\r" + animation.charAt(current % animation.length()) + " " + status);
    }

}
