package co.plocki.neoguard.server.process;

import co.plocki.neoguard.server.NeoGuard;
import co.plocki.neoguard.server.binary.BinaryManager;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class DataProcessor {

    private static final String ALGORITHM = "AES";
    private static final String ENCODING = "UTF-8";
    private static final BinaryManager binaryManager = NeoGuard.getBinaryManager();

    public static JSONObject handleRequest(JSONObject request) {
        debug("Calling 'handleRequest' method in DataProcessor class.");

        if (request == null) {
            JSONObject errorResponse = new JSONObject();
            errorResponse.put("responseType", "ERROR");
            errorResponse.put("message", "Null request received.");
            return errorResponse;
        }

        debug("Received Request JSON: " + request);

        String type = request.optString("type"); // Use optString to handle missing key gracefully
        JSONObject response = new JSONObject();
        response.put("status-code", "FAILED");

        if (type.isEmpty()) {
            response.put("responseType", "ERROR");
            response.put("message", "Invalid request type.");
            return response;
        }

        System.out.println("IMPORTANT: " + request);


        switch (type.toLowerCase()) {
            case "get":
                response = DataProcessor.processData(request.toString());
                break;
            case "post":
                response = DataProcessor.processData(request.toString());
                break;
            case "search":
                String searchTerm = request.getString("searchTerm");
                String dataThread = request.getString("dataThread");
                String searchType = request.optString("searchType", "default");

                if ("threads".equalsIgnoreCase(searchType)) {
                    response = DataProcessor.searchThreads(searchTerm);
                } else if ("arrays".equalsIgnoreCase(searchType)) {
                    response = DataProcessor.searchArrays(searchTerm, dataThread);
                } else {
                    response.put("responseType", "ERROR");
                    response.put("message", "Invalid delete type.");
                }
                break;
            case "update":
                String updateThread = request.getString("dataThread");
                String updateArray = request.getString("array");
                String updateRow = request.getString("row");
                Object newData = request.getString("newData");
                response = DataProcessor.updateRow(updateThread, updateArray, updateRow, newData);
                break;
            case "delete":
                String deleteType = request.getString("deleteType");
                if ("row".equalsIgnoreCase(deleteType)) {
                    String deleteThread = request.getString("dataThread");
                    String deleteArray = request.getString("array");
                    String deleteRow = request.getString("row");
                    response = DataProcessor.deleteRow(deleteThread, deleteArray, deleteRow);
                } else if ("array".equalsIgnoreCase(deleteType)) {
                    String deleteThread = request.getString("dataThread");
                    String deleteArray = request.getString("array");
                    response = DataProcessor.deleteArray(deleteThread, deleteArray);
                } else if ("thread".equalsIgnoreCase(deleteType)) {
                    String deleteThread = request.getString("dataThread");
                    response = DataProcessor.deleteThread(deleteThread);
                } else {
                    response.put("responseType", "ERROR");
                    response.put("message", "Invalid delete type.");
                }
                break;
            default:
                response.put("responseType", "ERROR");
                response.put("message", "Invalid request type.");
                break;
        }


        debug("Sending Response JSON: " + response.toString());
        return response;
    }

    public static JSONObject processData(String decryptedData) {
        debug("Calling 'processData' method in DataProcessor class.");

        if (decryptedData == null) {
            JSONObject errorResponse = new JSONObject();
            errorResponse.put("responseType", "ERROR");
            errorResponse.put("message", "Null decryptedData received.");
            return errorResponse;
        }

        debug("Received Decrypted JSON Data: " + decryptedData);

        JSONObject json = new JSONObject(decryptedData);

        String type = json.optString("type"); // Use optString to handle missing key gracefully
        JSONObject responseObj = new JSONObject();
        responseObj.put("responseType", "DATA-POOL");

        if (type.isEmpty()) {
            responseObj.put("status-code", "FAILED");
            responseObj.put("message", "Invalid request type.");
            JSONObject responseData = new JSONObject();
            responseData.put("response", responseObj);
            json.put("data", responseData);
            return json;
        }

        if (type.equalsIgnoreCase("get")) {
            String requestedThread = json.getString("dataThread");
            List<String> arrays = new ArrayList<>();
            try {
                for (Object array : json.getJSONArray("arrays").toList()) {
                    arrays.add((String) array);
                }
            } catch (ClassCastException exception) {
                for (Object array : (ArrayList<?>) json.getJSONArray("arrays").toList().get(0)) {
                    arrays.add((String) array);
                }
                exception.printStackTrace();
            }

            System.out.println("arrays: " + arrays);

            if (binaryManager.hasThread(requestedThread)) {
                JSONArray arrayData = new JSONArray();

                // Iterate through arrays and retrieve row data
                for (String array : arrays) {
                    if (binaryManager.hasArray(requestedThread, array)) {
                        List<String> rowKeys = binaryManager.getAllRowData(requestedThread, array);
                        JSONArray rowDataArray = new JSONArray();

                        for (String rowKey : rowKeys) {
                            JSONObject rowDataLocation = binaryManager.getRowData(requestedThread, array, rowKey);

                            if (rowDataLocation != null) {
                                JSONObject rowObj = new JSONObject();

                                String location = rowDataLocation.getString("location");

                                // Debug: Print location
                                System.out.println("Location: " + location);

                                Object data = NeoGuard.getDataCache().getData(location);

                                // Debug: Print data
                                System.out.println("Temporary debug - data: " + data);

                                rowObj.put("data", data);
                                rowDataArray.put(rowObj);
                            } else {
                                debug("rowDataLocation is null for thread: " + requestedThread + ", array: " + array + ", row: " + rowKey);
                            }
                        }

                        JSONObject arrayObj = new JSONObject();
                        arrayObj.put("array", array);

                        // Debug: Print array
                        System.out.println("Array: " + array);

                        arrayObj.put("rows", rowDataArray);

                        // Debug: Print rowDataArray
                        System.out.println("RowDataArray: " + rowDataArray);

                        arrayData.put(arrayObj);

                        System.out.println("arrayData 2: " + arrayData);
                    }
                }

                // Debug: Print arrayData
                System.out.println("ArrayData: " + arrayData);

                responseObj.put("dataThread", requestedThread);
                responseObj.put("arrays", arrays.toArray());
                responseObj.put("arrayData", arrayData);
                responseObj.put("status-code", "SUCCEED");
            } else {
                responseObj.put("status-code", "FAILED");
                responseObj.put("message", "Thread not found.");
            }

        } else if (type.equalsIgnoreCase("post")) {
            String requestedThread = json.getString("dataThread");
            List<String> arrays = new ArrayList<>();
            for (Object array : json.getJSONArray("arrays").toList()) {
                arrays.add((String) array);
            }

            List<Object> data = new ArrayList<>();
            data.addAll(json.getJSONArray("data").toList());

            // Check if the requested thread exists, if not, create it
            if (!binaryManager.hasThread(requestedThread)) {
                binaryManager.createThread(requestedThread);
            }

            // Iterate through the arrays and create/update them
            for (String array : arrays) {
                if (binaryManager.hasArray(requestedThread, array)) {
                    // Skip processing if array already exists
                    debug("Array " + array + " already exists in thread " + requestedThread);
                } else {
                    // Create the array
                    binaryManager.addArray(requestedThread, array);
                }

                // Iterate through the data and create/update rows
                for (int i = 0; i < data.size(); i++) {
                    Object rowData = data.get(i);

                    String dataKey = UUID.randomUUID() + "-" + UUID.randomUUID() + "-" + UUID.randomUUID() + "-" + UUID.randomUUID();
                    dataKey = dataKey.replaceAll("-", "");

                    if (binaryManager.hasRow(requestedThread, array, String.valueOf(i))) {
                        // Skip processing if row already exists
                        debug("Row " + i + " already exists in thread " + requestedThread + ", array " + array);
                    } else {
                        binaryManager.addRow(requestedThread, array, String.valueOf(i), dataKey);
                    }

                    NeoGuard.getDataCache().updateData(dataKey, rowData);
                }
            }

            responseObj.put("dataThread", requestedThread);
            responseObj.put("arrays", arrays.toArray());
            responseObj.put("status-code", "SUCCEED");
            responseObj.put("resolvedData", new JSONObject()); // Placeholder
            responseObj.put("resolvedValue", new JSONObject()); // Placeholder
            responseObj.put("updateData", new JSONObject()); // Placeholder
        }

        JSONObject responseData = new JSONObject();
        responseData.put("response", responseObj);
        json.put("data", responseData);

        debug("Returning Processed JSON Data: " + json.toString());
        return json;
    }

    public static JSONObject searchThreads(String searchTerm) {
        debug("Calling 'searchThreads' method in DataProcessor class.");

        JSONObject responseObj = new JSONObject();
        responseObj.put("responseType", "SEARCH-THREADS");

        List<String> matchingThreads = binaryManager.searchThreads(searchTerm);

        if (!matchingThreads.isEmpty()) {
            responseObj.put("status-code", "SUCCEED");
            responseObj.put("matchingThreads", new JSONArray(matchingThreads));
        } else {
            responseObj.put("status-code", "FAILED");
            responseObj.put("message", "No matching threads found.");
        }

        JSONObject responseData = new JSONObject();
        responseData.put("response", responseObj);

        return responseData;
    }

    public static JSONObject deleteArray(String thread, String array) {
        debug("Calling 'deleteArray' method in DataProcessor class.");

        JSONObject responseObj = new JSONObject();
        responseObj.put("responseType", "DELETE-ARRAY");

        if (binaryManager.hasArray(thread, array)) {
            binaryManager.deleteArray(thread, array);
            responseObj.put("status-code", "SUCCEED");
        } else {
            responseObj.put("status-code", "FAILED");
            responseObj.put("message", "Array not found.");
        }

        JSONObject responseData = new JSONObject();
        responseData.put("response", responseObj);

        return responseData;
    }

    public static JSONObject deleteThread(String thread) {
        debug("Calling 'deleteThread' method in DataProcessor class.");

        JSONObject responseObj = new JSONObject();
        responseObj.put("responseType", "DELETE-THREAD");

        if (binaryManager.hasThread(thread)) {
            binaryManager.deleteThread(thread);
            responseObj.put("status-code", "SUCCEED");
        } else {
            responseObj.put("status-code", "FAILED");
            responseObj.put("message", "Thread not found.");
        }

        JSONObject responseData = new JSONObject();
        responseData.put("response", responseObj);

        return responseData;
    }

    public static JSONObject deleteRow(String requestedThread, String array, String row) {
        debug("Calling 'deleteRow' method in DataProcessor class.");

        if (binaryManager.hasRow(requestedThread, array, row)) {
            // Delete the row
            NeoGuard.getDataCache().deleteData(binaryManager.getRowData(requestedThread, array, row).getString("location"));
            binaryManager.removeRow(requestedThread, array, Integer.parseInt(row));

            JSONObject responseObj = new JSONObject();
            responseObj.put("responseType", "DELETE-RESULT");
            responseObj.put("dataThread", requestedThread);
            responseObj.put("array", array);
            responseObj.put("row", row);
            responseObj.put("status-code", "SUCCEED");

            JSONObject responseData = new JSONObject();
            responseData.put("response", responseObj);

            return responseData;
        } else {
            JSONObject responseObj = new JSONObject();
            responseObj.put("responseType", "DELETE-RESULT");
            responseObj.put("dataThread", requestedThread);
            responseObj.put("array", array);
            responseObj.put("row", row);
            responseObj.put("status-code", "FAILED");
            responseObj.put("message", "Row not found.");

            JSONObject responseData = new JSONObject();
            responseData.put("response", responseObj);

            return responseData;
        }
    }

    public static JSONObject updateRow(String requestedThread, String array, String row, Object newData) {
        debug("Calling 'updateRow' method in DataProcessor class.");

        System.out.println("updating");


        if (binaryManager.hasRow(requestedThread, array, row)) {
            // Update the row data
            NeoGuard.getDataCache().updateData(binaryManager.getRowData(requestedThread, array, row).getString("location"), newData);

            JSONObject responseObj = new JSONObject();
            responseObj.put("responseType", "UPDATE-RESULT");
            responseObj.put("dataThread", requestedThread);
            responseObj.put("array", array);
            responseObj.put("row", row);
            responseObj.put("newData", newData);
            responseObj.put("status-code", "SUCCEED");

            JSONObject responseData = new JSONObject();
            responseData.put("response", responseObj);

            return responseData;
        } else {
            JSONObject responseObj = new JSONObject();
            responseObj.put("responseType", "UPDATE-RESULT");
            responseObj.put("dataThread", requestedThread);
            responseObj.put("array", array);
            responseObj.put("row", row);
            responseObj.put("status-code", "FAILED");
            responseObj.put("message", "Row not found.");

            JSONObject responseData = new JSONObject();
            responseData.put("response", responseObj);

            return responseData;
        }
    }

    public static JSONObject searchArrays(String searchTerm, String requestedThread) {
        debug("Calling 'searchArrays' method in DataProcessor class.");

        List<String> matchingArrays = binaryManager.searchArrays(searchTerm, requestedThread);

        JSONObject responseObj = new JSONObject();
        responseObj.put("responseType", "SEARCH-RESULT");
        responseObj.put("dataThread", requestedThread);
        responseObj.put("searchTerm", searchTerm);
        responseObj.put("matchingArrays", matchingArrays.toArray());

        JSONObject responseData = new JSONObject();
        responseData.put("response", responseObj);

        return responseData;
    }


    private static void debug(String message) {
        if(NeoGuard.debug) {
            System.out.println("Debug [" + DataProcessor.class.getSimpleName() + "]: " + message);
        }
    }
}
