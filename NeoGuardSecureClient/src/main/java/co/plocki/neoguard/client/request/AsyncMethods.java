package co.plocki.neoguard.client.request;

import co.plocki.neoguard.client.util.AESUtil;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

public class AsyncMethods {


    private String SERVER_URL;
    private byte[] PASSPHRASE;
    private String SESSION_KEY;

    public AsyncMethods(String url, byte[] passphrase, String session_key) {
        SERVER_URL = url;
        PASSPHRASE = passphrase;
        SESSION_KEY = session_key;
    }

    public JSONObject request(String thread, List<String> array) throws Exception {
        SecretKeySpec keySpec = new SecretKeySpec(PASSPHRASE, "AES");

        debug("Calling 'request' method.");

        JSONObject requestData = new JSONObject();
        requestData.put("type", "GET");
        requestData.put("dataThread", thread);

        JSONArray arrays = new JSONArray();
        arrays.put(array);

        requestData.put("arrays", arrays);

        JSONObject encData = new JSONObject();
        encData.put("encryptedData", new String(Base64.getEncoder().encode(requestData.toString().getBytes(StandardCharsets.UTF_8))));

        JSONObject object = new JSONObject();
        object.put("key", SESSION_KEY);
        object.put("data", new String(AESUtil.encrypt(keySpec.getEncoded(), encData.toString().getBytes(StandardCharsets.UTF_8))));


        JSONObject response = sendRequest(object, false);

        response.put("data", new JSONObject(new String(AESUtil.decrypt(keySpec.getEncoded(), response.getString("data").getBytes(StandardCharsets.UTF_8)))));

        JSONObject data = response.getJSONObject("data");
        data.put("encryptedData", new JSONObject(new String(Base64.getDecoder().decode(data.getString("encryptedData")))));

        response.put("data", data);

        return response;
    }

    public String connectAndAuthenticate() throws IOException {
        debug("Calling 'connectAndAuthenticate' method.");

        JSONObject requestObj = new JSONObject();
        JSONObject dataObj = new JSONObject();
        dataObj.put("passphrase", Base64.getEncoder().encodeToString(PASSPHRASE));

        requestObj.put("data", dataObj);

        JSONObject responseObj = sendRequest(requestObj, true);

        return responseObj.getJSONObject("data").getString("key");
    }

    private JSONObject sendRequest(JSONObject requestObj, boolean isConnect) throws IOException {
        debug("Calling 'sendRequest' method.");

        URL url = new URL(SERVER_URL);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setDoOutput(true);

        if(!isConnect) {
            connection.setRequestProperty("DATA", "true");
        } else {
            connection.setRequestProperty("CONNECT", "true");
        }

        try (OutputStream os = connection.getOutputStream()) {
            byte[] input = requestObj.toString().getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }

        try (InputStream is = connection.getInputStream()) {
            byte[] buffer = new byte[is.available()];
            is.read(buffer);

            String receivedData = new String(buffer, StandardCharsets.UTF_8);
            if (receivedData.isEmpty()) {
                return new JSONObject();
            }

            try {
                return new JSONObject(receivedData);
            } catch (JSONException e) {
                return new JSONObject();
            }
        }
    }

    private JSONObject sendEncryptedData(JSONObject data) throws Exception {

        SecretKeySpec keySpec = new SecretKeySpec(PASSPHRASE, "AES");


        debug("Calling 'sendEncryptedData' method.");

        JSONObject requestObj = new JSONObject();
        JSONObject dataObj = new JSONObject();

        dataObj.put("encryptedData", Base64.getEncoder().encodeToString(data.toString().getBytes(StandardCharsets.UTF_8)));

        requestObj.put("key", SESSION_KEY);
        requestObj.put("data", new String(AESUtil.encrypt(keySpec.getEncoded(), dataObj.toString().getBytes(StandardCharsets.UTF_8))));

        JSONObject response = sendRequest(requestObj, false);

        response.put("data", new JSONObject(new String(AESUtil.decrypt(keySpec.getEncoded(), response.getString("data").getBytes(StandardCharsets.UTF_8)))));

        JSONObject dat = response.getJSONObject("data");
        dat.put("encryptedData", new JSONObject(new String(Base64.getDecoder().decode(dat.getString("encryptedData")))));

        response.put("data", dat);
        return response;
    }

    public JSONObject post(String thread, List<String> arrays, List<Object> data) throws Exception {
        debug("Calling 'post' method.");

        JSONObject object = new JSONObject();
        object.put("dataThread", thread);
        object.put("type", "POST");
        object.put("arrays", new JSONArray(arrays));
        object.put("data", new JSONArray(data));

        return sendEncryptedData(object);
    }

    private static void debug(String message) {
        //System.out.println("Debug [" + this.getClass().getSimpleName() + "]: " + message);
    }

}
