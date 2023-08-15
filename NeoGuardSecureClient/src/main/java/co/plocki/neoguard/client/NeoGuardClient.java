package co.plocki.neoguard.client;

import co.plocki.neoguard.client.util.AESUtil;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class NeoGuardClient {

    private String SERVER_URL;
    public static String PASSPHRASE = ""; //read from config
    private String SESSION_KEY;


    public void start() throws IOException {
        SERVER_URL = "http://localhost:8080/json-transfer";

        SESSION_KEY = connectAndAuthenticate();
    }

    public JSONObject request(String thread, List<String> array) throws Exception {
        SecretKeySpec keySpec = new SecretKeySpec(Base64.getDecoder().decode(PASSPHRASE.getBytes(StandardCharsets.UTF_8)), "AES");

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


        JSONObject response = sendRequestSync(object, false);

        try {
            response.put("data", new JSONObject(new String(AESUtil.decrypt(keySpec.getEncoded(), response.getString("data").getBytes(StandardCharsets.UTF_8)))));
        } catch (JSONException exception) {

            if(response.getString("status-code").equalsIgnoreCase("FAILED")) {
                try {
                    if(response.getJSONObject("data").getString("status-code").equalsIgnoreCase("KEY-TIMEOUT")) {
                        SESSION_KEY = connectAndAuthenticate();
                        return request(thread, array);
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                    throw new RuntimeException("Error on self-catch - please contact a administrator - Error-Code: E58");
                }
            }
        }
        JSONObject data = response.getJSONObject("data");
        data.put("encryptedData", new JSONObject(new String(Base64.getDecoder().decode(data.getString("encryptedData")))));

        response.put("data", data);

        processResponse(response);

        return response;
    }

    public JSONObject post(String thread, List<String> arrays, List<Object> data) throws Exception {
        debug("Calling 'post' method.");

        JSONObject object = new JSONObject();
        object.put("dataThread", thread);
        object.put("type", "POST");
        object.put("arrays", new JSONArray(arrays));
        object.put("data", new JSONArray(data));

        JSONObject resp = sendEncryptedData(object);
        processResponse(resp);

        return resp;
    }

    private String connectAndAuthenticate() throws IOException {
        debug("Calling 'connectAndAuthenticate' method.");

        JSONObject requestObj = new JSONObject();
        JSONObject dataObj = new JSONObject();
        dataObj.put("passphrase", PASSPHRASE);

        requestObj.put("data", dataObj);

        JSONObject responseObj = sendRequestSync(requestObj, true);

        return responseObj.getJSONObject("data").getString("key");
    }







    public JSONObject searchThreads(String searchTerm) throws Exception {
        debug("Calling 'searchThreads' method.");

        JSONObject object = new JSONObject();
        object.put("type", "search");
        object.put("searchType", "threads");
        object.put("searchTerm", searchTerm);
        object.put("dataThread", searchTerm);

        JSONObject resp = sendEncryptedData(object);
        processResponse(resp);

        return resp;
    }

    public JSONObject searchArrays(String searchTerm, String dataThread) throws Exception {
        debug("Calling 'searchArrays' method.");

        JSONObject object = new JSONObject();
        object.put("type", "search");
        object.put("searchType", "arrays");
        object.put("searchTerm", searchTerm);
        object.put("dataThread", dataThread);

        JSONObject resp = sendEncryptedData(object);
        processResponse(resp);

        return resp;
    }

    public JSONObject updateRow(String thread, String array, String row, String newData) throws Exception {
        debug("Calling 'updateRow' method.");

        JSONObject object = new JSONObject();
        object.put("type", "update");
        object.put("dataThread", thread);
        object.put("array", array);
        object.put("row", row);
        object.put("newData", newData);

        JSONObject resp = sendEncryptedData(object);
        processResponse(resp);

        return resp;
    }

    public JSONObject deleteRow(String thread, String array, String row) throws Exception {
        debug("Calling 'deleteRow' method.");

        JSONObject object = new JSONObject();
        object.put("type", "delete");
        object.put("deleteType", "row");
        object.put("dataThread", thread);
        object.put("array", array);
        object.put("row", row);

        JSONObject resp = sendEncryptedData(object);
        processResponse(resp);

        return resp;
    }

    public JSONObject deleteArray(String thread, String array) throws Exception {
        debug("Calling 'deleteArray' method.");

        JSONObject object = new JSONObject();
        object.put("type", "delete");
        object.put("deleteType", "array");
        object.put("dataThread", thread);
        object.put("array", array);

        JSONObject resp = sendEncryptedData(object);
        processResponse(resp);

        return resp;
    }

    public JSONObject deleteThread(String thread) throws Exception {
        debug("Calling 'deleteThread' method.");

        JSONObject object = new JSONObject();
        object.put("type", "delete");
        object.put("deleteType", "thread");
        object.put("dataThread", thread);

        JSONObject resp = sendEncryptedData(object);
        processResponse(resp);

        return resp;
    }



















    private JSONObject sendRequestSync(JSONObject requestObj, boolean isConnect) throws IOException {
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
        SecretKeySpec keySpec = new SecretKeySpec(Base64.getDecoder().decode(PASSPHRASE.getBytes(StandardCharsets.UTF_8)), "AES");

        debug("Calling 'sendEncryptedData' method.");

        JSONObject requestObj = new JSONObject();
        JSONObject dataObj = new JSONObject();

        dataObj.put("request", data); // Wrap the original request in a "request" object
        dataObj.put("encryptedData", Base64.getEncoder().encodeToString(data.toString().getBytes(StandardCharsets.UTF_8)));

        requestObj.put("key", SESSION_KEY);
        requestObj.put("data", new String(AESUtil.encrypt(keySpec.getEncoded(), dataObj.toString().getBytes(StandardCharsets.UTF_8))));

        JSONObject response = sendRequestSync(requestObj, false);

        try {
            JSONObject responseData = new JSONObject(new String(AESUtil.decrypt(keySpec.getEncoded(), response.getString("data").getBytes(StandardCharsets.UTF_8))));
            response.put("data", responseData);

            if (responseData.has("encryptedData")) {
                JSONObject decryptedData = new JSONObject(new String(Base64.getDecoder().decode(responseData.getString("encryptedData"))));
                response.getJSONObject("data").put("encryptedData", decryptedData);
            }
        } catch (JSONException exception) {
            if (response.getString("status-code").equalsIgnoreCase("FAILED")) {
                try {
                    if (response.getJSONObject("data").getString("status-code").equalsIgnoreCase("KEY-TIMEOUT")) {
                        SESSION_KEY = connectAndAuthenticate();
                        return sendEncryptedData(data);
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                    throw new RuntimeException("Error on self-catch - please contact an administrator - Error-Code: E58");
                }
            }
        }

        return response;
    }

    private void processResponse(JSONObject responseObj) {

        if(responseObj.has("status-code")) return; //disable

        //System.out.println(responseObj);

        try {
            String message = responseObj.optString("message", "");

            if (!message.isEmpty()) {
                System.out.println("Message (1st layer): " + message);
            }
        } catch (Exception ignored) {}
        try {
            String statusCode = responseObj.optString("status-code", "");

            if (!statusCode.isEmpty()) {
                System.out.println("Status-Code (1st layer): " + statusCode);
            }
        } catch (Exception ignored) {}

        if(responseObj.has("data")) {
            try {
                String messageDeep = responseObj.getJSONObject("data").optString("message", "");

                if (!messageDeep.isEmpty()) {
                    System.out.println("Message (2nd layer): " + messageDeep);
                }
            } catch (Exception ignored) {}
            try {
                String statusCodeDeep = responseObj.getJSONObject("data").optString("status-code", "");

                if (!statusCodeDeep.isEmpty()) {
                    System.out.println("Status-Code (2nd layer): " + statusCodeDeep);
                }
            } catch (Exception ignored) {}

            if(responseObj.getJSONObject("data").has("encryptedData")) {
                try {
                    String messageDeepDeeper = responseObj.getJSONObject("data").getJSONObject("encryptedData").optString("message", "");

                    if (!messageDeepDeeper.isEmpty()) {
                        System.out.println("Message (3rd layer): " + messageDeepDeeper);
                    }
                } catch (Exception ignored) {}
                try {
                    String statusCodeDeepDeeper = responseObj.getJSONObject("data").getJSONObject("encryptedData").optString("status-code", "");

                    if (!statusCodeDeepDeeper.isEmpty()) {
                        System.out.println("Status-Code (3rd layer): " + statusCodeDeepDeeper);
                    }
                } catch (Exception ignored) {}

                if(responseObj.getJSONObject("data").getJSONObject("encryptedData").has("response")) {
                    try {
                        String messageDeepDeeperDeepest = responseObj.getJSONObject("data").getJSONObject("encryptedData").getJSONObject("response").optString("message", "");

                        if (!messageDeepDeeperDeepest.isEmpty()) {
                            System.out.println("Message (4th layer): " + messageDeepDeeperDeepest);
                        }
                    } catch (Exception ignored) {}
                    try {
                        String statusCodeDeepDeeperDeepest = responseObj.getJSONObject("data").getJSONObject("encryptedData").getJSONObject("response").optString("status-code", "");

                        if (!statusCodeDeepDeeperDeepest.isEmpty()) {
                            System.out.println("Status-Code (4th layer): " + statusCodeDeepDeeperDeepest);
                        }
                    } catch (Exception ignored) {}
                }
            }
            if(responseObj.getJSONObject("data").has("response")) {
                try {
                    String messageDeepDeeperDeepest = responseObj.getJSONObject("data").getJSONObject("response").optString("message", "");

                    if (!messageDeepDeeperDeepest.isEmpty()) {
                        System.out.println("Message (3rd layer): " + messageDeepDeeperDeepest);
                    }
                } catch (Exception ignored) {}
                try {
                    String statusCodeDeepDeeperDeepest = responseObj.getJSONObject("data").getJSONObject("response").optString("status-code", "");

                    if (!statusCodeDeepDeeperDeepest.isEmpty()) {
                        System.out.println("Status-Code (3rd layer): " + statusCodeDeepDeeperDeepest);
                    }
                } catch (Exception ignored) {}
            }
        }
    }

    private static void debug(String message) {
        //System.out.println("Debug [" + NeoGuardClient.class.getSimpleName() + "]: " + message);
    }
}
