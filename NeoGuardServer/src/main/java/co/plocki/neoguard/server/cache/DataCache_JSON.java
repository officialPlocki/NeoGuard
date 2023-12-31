package co.plocki.neoguard.server.cache;

import co.plocki.json.JSONFile;

import java.io.File;
import java.io.IOException;

public class DataCache_JSON {

    //private final HashMap<String, JSONObject> object = new HashMap<>();
    //private static final HashMap<String, Long> objectTime = new HashMap<>();

    public void init() {
        /*Thread thread = new Thread(() -> {
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
        Runtime.getRuntime().addShutdownHook(new Thread(thread::interrupt));*/
    }

    public Object getData(String file) {
        /*if(object.containsKey(file)) {
            objectTime.put(file, System.currentTimeMillis());

            System.out.println("returning data: " + object.get(file));

            return object.get(file).get("data");
        } else {*/
            JSONFile jsonFile = new JSONFile("structure" + File.separator + file + ".jdat");

            try {
                System.out.println("returning data: " + jsonFile.getFileObject());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            if(jsonFile.isNew()) {

                jsonFile.put("data", null);

                try {
                    jsonFile.save();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

                return null;
            } else {
                /*try {
                    object.put(file, jsonFile.getFileObject());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                objectTime.put(file, System.currentTimeMillis());*/
                //return object.get(file).get("data");
                return jsonFile.get("data");
            }
        //}
    }

    public void updateData(String file, Object data) {
        JSONFile jsonFile = new JSONFile("structure" + File.separator + file + ".jdat");

        System.out.println("data obj: " + data);

        jsonFile.put("data", data);

        /*try {
            object.put(file, jsonFile.getFileObject());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        objectTime.put(file, System.currentTimeMillis());*/

        try {
            System.out.println("updated Object: " + jsonFile.getFileObject());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        try {
            jsonFile.save(jsonFile.getFileObject());
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            System.out.println("file after update: " + new JSONFile("structure" + File.separator + file + ".jdat").getFileObject());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void deleteData(String file) {
        //object.remove(file);
        //objectTime.remove(file);
        JSONFile jsonFile = new JSONFile("structure" + File.separator + file + ".jdat");
        jsonFile.getFile().delete();
    }

}
