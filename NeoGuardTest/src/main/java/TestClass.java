import co.plocki.neoguard.client.NeoGuardClient;
import co.plocki.neoguard.client.interfaces.NeoArray;
import co.plocki.neoguard.client.interfaces.NeoRow;
import co.plocki.neoguard.client.interfaces.NeoThread;
import co.plocki.neoguard.client.post.NeoPost;
import co.plocki.neoguard.client.request.NeoRequest;
import co.plocki.neoguard.server.NeoGuard;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

public class TestClass {

    public static void main(String[] args) throws Exception {
        System.setProperty("java.net.debug", "all");

        NeoGuard neoGuard = new NeoGuard();
        neoGuard.start(neoGuard);

        NeoGuardClient client = new NeoGuardClient();

        //System.out.println(new String(Base64.getDecoder().decode(NeoGuardClient.PASSPHRASE)));

        NeoGuardClient.PASSPHRASE = (String) NeoGuard.getKeyManager().registerKey().get(1);

        client.start();

        System.out.println("testing output post: " + new NeoPost(new NeoThread("test1"), List.of(new NeoArray("t1"), new NeoArray("t2"), new NeoArray("t3")), List.of("td1", "td2", "td3")));
        System.out.println("testing output request: " + new NeoRequest(new NeoThread("test1"), List.of(new NeoArray("t1"), new NeoArray("t2"), new NeoArray("t3"))));


        System.out.println("testing output search arrays: " + new NeoArray(null).searchArrays("t1", "test8"));
        System.out.println("testing output search threads: " + new NeoThread(null).searchThreads("test1"));
        System.out.println("testing output update row: " + new NeoRow().updateRow("test1", "t1", "0", "td800"));

        System.out.println("testing output delete row: " + new NeoRow().deleteRow("test1", "t2", "1"));
        System.out.println("testing output delete array: " + new NeoArray(null).deleteArray("test1", "t3"));
        System.out.println("testing output search array: " + new NeoArray(null).searchArrays("t3", "test1"));
        System.out.println("testing output delete thread: " + new NeoThread(null).deleteThread("test1"));
        System.out.println("testing output search thread: " + new NeoThread(null).searchThreads("test1"));

        Random random = new Random();

        int numberOfRequests = 100000;

        long startTime = System.currentTimeMillis();

        System.out.println("Running " + numberOfRequests + " requests...");

        for (int i = 0; i < numberOfRequests; i++) {
            long requestStartTime = System.currentTimeMillis();
            updateLoadingAnimation(i, numberOfRequests);

            String threadName = "test" + (i + 1);
            List<NeoArray> neoArrays = generateNeoArrays(random.nextInt(3) + 1);
            List<String> arrayData = generateRandomArrayData(neoArrays.size());

            // Simulate different request types randomly
            int requestType = random.nextInt(8);
            switch (requestType) {
                case 0:
                    new NeoPost(new NeoThread(threadName), neoArrays, arrayData);
                    break;
                case 1:
                case 2:
                    new NeoRequest(new NeoThread(threadName), neoArrays);
                    break;
                case 3:
                    new NeoArray(null).searchArrays("t1", threadName);
                    break;
                case 4:
                case 5:
                    NeoRow neoRow = new NeoRow();
                    int rowIndex = random.nextInt(neoArrays.size());
                    neoRow.deleteRow(threadName, neoArrays.get(rowIndex).getName(), Integer.toString(rowIndex));
                    break;
                case 6:
                    NeoArray neoArrayDelete = new NeoArray(null);
                    neoArrayDelete.deleteArray(threadName, neoArrays.get(0).getName());
                    break;
                case 7:
                    NeoRow neoRowUpdate = new NeoRow();
                    neoRowUpdate.updateRow(threadName, neoArrays.get(0).getName(), "0", "td" + random.nextInt(1000));
                    break;
            }

            long requestEndTime = System.currentTimeMillis();
            long requestTime = requestEndTime - requestStartTime;
            System.out.println(" - Request " + (i + 1) + " completed in " + requestTime + " milliseconds.");
        }

        long endTime = System.currentTimeMillis();
        long totalTime = endTime - startTime;

        System.out.println("All requests completed in " + totalTime + " milliseconds.");
        neoGuard.stop();
        System.exit(4);

    }

    private static List<NeoArray> generateNeoArrays(int count) {
        List<NeoArray> neoArrays = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            NeoArray neoArray = new NeoArray("t" + i);
            neoArrays.add(neoArray);
        }
        return neoArrays;
    }

    private static List<String> generateRandomArrayData(int count) {
        List<String> arrayData = new ArrayList<>();
        Random random = new Random();
        for (int i = 0; i < count; i++) {
            arrayData.add("td" + random.nextInt(1000));
        }
        return arrayData;
    }

    public static void updateLoadingAnimation(int current, int total) {
        int progress = (current * 100) / total;
        String animation = "◐◐◓◓◑◑◒◒";
        String status = "Processing " + progress + "%";

        System.out.print("\r" + animation.charAt(current % animation.length()) + " " + status);
    }

}
