package co.plocki.neoguard.server.webserver;

import co.plocki.neoguard.server.NeoGuard;
import co.plocki.neoguard.server.process.DataProcessor;
import co.plocki.neoguard.server.util.AESUtil;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.HttpString;
import org.json.JSONObject;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.UUID;

public class DataHandler implements HttpHandler {

    private final HashMap<String, String> keys = new HashMap<>();
    private final HashMap<String, Long> keyTime = new HashMap<>();

    @Override
    public void handleRequest(HttpServerExchange exchange) {
        
        if(NeoGuard.local_only) {
            debug(exchange.getSourceAddress().getHostName());
            if(!exchange.getSourceAddress().getHostName().contains("0.0.0.0") && !exchange.getSourceAddress().getHostName().contains("127.0.0.1") && !exchange.getSourceAddress().getHostName().contains(exchange.getHostName())) {
                JSONObject respDataObj = new JSONObject();
                respDataObj.put("status-code", "NO-ACCESS");
                respDataObj.put("message", "This NeoGuard server is not configured for public access.");

                JSONObject responseBody = new JSONObject();
                responseBody.put("status-code", "FAILED");
                responseBody.put("data", respDataObj);

                sendResponse(exchange, 200, responseBody.toString());
                return;
            }
        }

        debug("Received request: " + exchange.getRequestMethod() + " " + exchange.getRequestURI());

        if ("POST".equalsIgnoreCase(exchange.getRequestMethod().toString())) {
            if (exchange.getRequestHeaders().contains("DATA")) {
                exchange.getRequestReceiver().receiveFullString((httpServerExchange, requestData) -> {
                    NeoGuard.runningProcesses += 1;
                    JSONObject requestObj = new JSONObject(requestData);

                    if (System.currentTimeMillis() > keyTime.get(requestObj.getString("key"))) {
                        JSONObject respDataObj = new JSONObject();
                        respDataObj.put("status-code", "KEY-TIMEOUT");
                        respDataObj.put("message", "Key timeout. Request a new one.");

                        JSONObject responseBody = new JSONObject();
                        responseBody.put("status-code", "FAILED");
                        responseBody.put("data", respDataObj);

                        sendResponse(exchange, 200, responseBody.toString());
                    } else {
                        SecretKeySpec spec = new SecretKeySpec(Base64.getDecoder().decode(keys.get(requestObj.getString("key"))), "AES");


                        JSONObject dataObj;
                        try {
                            dataObj = new JSONObject(new String(AESUtil.decrypt(spec.getEncoded(), requestObj.getString("data").getBytes(StandardCharsets.UTF_8))));
                        } catch (Exception e) {
                            NeoGuard.runningProcesses -= 1;
                            throw new RuntimeException(e);
                        }


                        JSONObject respDataObj = new JSONObject();
                        respDataObj.put("status-code", "SUCCESSFUL");
                        respDataObj.put("message", "Data has been processed.");

                        JSONObject responseBody = new JSONObject();
                        responseBody.put("status-code", "SUCCESSFUL");

                        debug("Listened: " + dataObj.get("encryptedData"));
                        debug("Listened (decrypted): " + new String(Base64.getDecoder().decode(dataObj.get("encryptedData").toString())));

                        JSONObject processObject = DataProcessor.handleRequest(new JSONObject(new String(Base64.getDecoder().decode(dataObj.get("encryptedData").toString()))));

                        String encryptedData = new String(Base64.getEncoder().encode(processObject.toString().getBytes(StandardCharsets.UTF_8)));

                        respDataObj.put("encryptedData", encryptedData);

                        try {
                            responseBody.put("data", new String(AESUtil.encrypt(spec.getEncoded(), respDataObj.toString().getBytes(StandardCharsets.UTF_8))));
                        } catch (Exception e) {
                            NeoGuard.runningProcesses -= 1;
                            throw new RuntimeException(e);
                        }

                        sendResponse(exchange, 200, responseBody.toString());
                    }


                    NeoGuard.getBinaryManager().save();
                    NeoGuard.runningProcesses -= 1;
                });

            } else if (exchange.getRequestHeaders().contains("CONNECT")) {
                exchange.getRequestReceiver().receiveFullString((httpServerExchange, requestData) -> {
                    debug(requestData);
                    NeoGuard.runningProcesses += 1;

                    JSONObject object = new JSONObject(requestData).getJSONObject("data");
                    String passphrase = object.getString("passphrase");

                    try {
                        if(!NeoGuard.getKeyManager().containsKey(new String(Base64.getDecoder().decode(passphrase)))) {
                            JSONObject responseObj = new JSONObject();
                            responseObj.put("status-code", "FAILED");
                            responseObj.put("message", "Passphrase isn't known.");

                            sendResponse(exchange, 200, responseObj.toString());

                            NeoGuard.runningProcesses -= 1;
                            return;
                        }
                    } catch (Exception e) {
                        JSONObject responseObj = new JSONObject();
                        responseObj.put("status-code", "FAILED");
                        responseObj.put("message", "Error on checking passphrase!");

                        sendResponse(exchange, 200, responseObj.toString());

                        NeoGuard.runningProcesses -= 1;
                        throw new RuntimeException(e);
                    }


                    String uuid = UUID.randomUUID() + ":" + UUID.randomUUID() + ":" + UUID.randomUUID();
                    String key = Base64.getEncoder().encodeToString(uuid.getBytes(StandardCharsets.UTF_8));

                    keys.put(key, passphrase);
                    keyTime.put(key, System.currentTimeMillis() + 16000);

                    JSONObject responseObj = new JSONObject();
                    responseObj.put("status-code", "CONNECT-AUTHENTICATED");

                    JSONObject dataObj = new JSONObject();
                    dataObj.put("key", key);

                    responseObj.put("data", dataObj);

                    sendResponse(exchange, 200, responseObj.toString());
                });
            }

            NeoGuard.getBinaryManager().save();
            NeoGuard.runningProcesses -= 1;
        } else if ("GET".equalsIgnoreCase(exchange.getRequestMethod().toString())) {
            exchange.getRequestReceiver().receiveFullString((httpServerExchange, requestData) -> {
                NeoGuard.runningProcesses += 1;

                JSONObject jsonObject = new JSONObject(requestData);

                JSONObject processorResponse = new JSONObject();
                if (System.currentTimeMillis() > keyTime.get(jsonObject.getString("key"))) {
                    JSONObject respDataObj = new JSONObject();
                    respDataObj.put("status-code", "KEY-TIMEOUT");
                    respDataObj.put("message", "Key timeout. Request a new one.");

                    processorResponse.put("data", respDataObj);
                } else {
                    SecretKeySpec spec = new SecretKeySpec(Base64.getDecoder().decode(keys.get(jsonObject.getString("key"))), "AES");

                    JSONObject data;

                    try {
                        data = new JSONObject(new String(AESUtil.decrypt(spec.getEncoded(), jsonObject.getString("data").getBytes(StandardCharsets.UTF_8))));
                    } catch (Exception e) {

                        NeoGuard.runningProcesses -= 1;
                        throw new RuntimeException(e);
                    }

                    String encryptedData = data.getString("encryptedData");

                    String decryptedData = new String(Base64.getDecoder().decode(encryptedData.getBytes(StandardCharsets.UTF_8)));
                    processorResponse = DataProcessor.handleRequest(new JSONObject(decryptedData));
                }

                JSONObject respObj = new JSONObject();
                respObj.put("status-code", "SUCCESSFUL");
                JSONObject dataObj = new JSONObject();
                dataObj.put("encryptedData", processorResponse.toString());
                respObj.put("data", dataObj);

                sendResponse(exchange, 200, respObj.toString());
            });

            NeoGuard.getBinaryManager().save();
            NeoGuard.runningProcesses -= 1;
        } else {
            debug("Method Not Allowed: " + exchange.getRequestMethod());
            sendResponse(exchange, 405, "");
        }
    }

    private void sendResponse(HttpServerExchange exchange, int statusCode, String response) {
        exchange.setStatusCode(statusCode);
        exchange.getResponseHeaders().put(HttpString.tryFromString("Content-Type"), "application/json");
        exchange.getResponseSender().send(response, StandardCharsets.UTF_8);
        exchange.endExchange();
    }

    private static void debug(String message) {
        if(NeoGuard.debug) {
            String className = DataHandler.class.getSimpleName();
            System.out.println("Debug [" + className + "]: " + message);
        }
    }
}
