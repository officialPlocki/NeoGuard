package co.plocki.neoguard.server.config;

import co.plocki.json.JSONFile;
import co.plocki.json.JSONValue;
import org.json.JSONObject;

import java.io.IOException;

public class Config {

    private final JSONFile config;

    public Config() {
        config = new JSONFile(

                "config.json",
                new JSONValue() {
                    @Override
                    public JSONObject object() {
                        JSONObject object = new JSONObject();
                        object.put("aes", 256);
                        object.put("tls", false);
                        object.put("keypass_length", 2048);
                        object.put("local_access_only", true);
                        object.put("save_files_encrypted", false);

                        return object;
                    }

                    @Override
                    public String objectName() {
                        return "security";
                    }
                },

                new JSONValue() {
                    @Override
                    public JSONObject object() {
                        JSONObject object = new JSONObject();
                        object.put("port", 5551);
                        object.put("host", "localhost");

                        return object;
                    }

                    @Override
                    public String objectName() {
                        return "server";
                    }
                }

                );
    }

    public void save() {
        try {
            config.save();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean isDebugEnabled() {
        if(config.has("debug")) {
            return config.getBoolean("debug");
        }
        return false;
    }

    public Object getConfigurationData(String object, String value) {
        return config.get(object).get(value);
    }

}
