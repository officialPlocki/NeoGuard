package co.plocki.neoguard.server.cache;

import co.plocki.json.JSONFile;
import co.plocki.json.JSONValue;
import co.plocki.neoguard.server.NeoGuard;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

public class DataCache {

    private final HashMap<String, JSONObject> object = new HashMap<>();
    private static final HashMap<String, Long> objectTime = new HashMap<>();

    public void init() {
        Thread thread = new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(32000);
                } catch (InterruptedException e) {

                    try {
                        NeoGuard.fixRuntimeError();
                    } catch (Exception ex) {
                        NeoGuard.getBinaryManager().save();

                        System.exit(3);
                        System.err.println("Error on fixing Runtime");
                        return;
                    }
                }

                objectTime.forEach((file, time) -> {
                    if((System.currentTimeMillis() - time) > 16000) {
                        objectTime.remove(file);
                        object.remove(file);
                    }
                });
            }
        });
        thread.start();
        Runtime.getRuntime().addShutdownHook(new Thread(thread::interrupt));
    }

    public Object getData(String file) {
        if(object.containsKey(file)) {
            objectTime.put(file, System.currentTimeMillis());
            return object.get(file).get("data");
        } else {
            JSONFile jsonFile = new JSONFile("structure" + File.separator + file + ".jdat");

            if(jsonFile.isNew()) {

                jsonFile.put("data", null);

                try {
                    jsonFile.save();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

                return null;
            } else {
                try {
                    object.put(file, jsonFile.getFileObject());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                objectTime.put(file, System.currentTimeMillis());
                return jsonFile.get("data");
            }
        }
    }

    public void updateData(String file, Object data) {
        JSONFile jsonFile = new JSONFile("structure" + File.separator + file + ".jdat");
        jsonFile.put("data", data);

        try {
            object.put(file, jsonFile.getFileObject());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        objectTime.put(file, System.currentTimeMillis());

        try {
            jsonFile.save();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void deleteData(String file) {
        object.remove(file);
        objectTime.remove(file);
        JSONFile jsonFile = new JSONFile("structure" + File.separator + file + ".jdat");
        jsonFile.getFile().delete();
    }

}
