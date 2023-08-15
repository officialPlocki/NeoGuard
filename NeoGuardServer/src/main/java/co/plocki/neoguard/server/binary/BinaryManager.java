package co.plocki.neoguard.server.binary;

import co.plocki.json.JSONFile;
import co.plocki.json.JSONValue;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class BinaryManager {

    private final JSONFile binary;

    public BinaryManager() {
        binary = new JSONFile("contents" + File.separator + "delivered" + File.separator + "binary.jbin");

        if(binary.isNew()) {
            binary.put("threads", new JSONArray());
            try {
                binary.save();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void save() {
        try {
            binary.save();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void createThread(String thread) {
        JSONArray array = binary.getArray("threads");
        array.put(thread);

        JSONObject threadObject = new JSONObject();
        binary.put(thread, threadObject);
    }

    public void deleteThread(String thread) {
        JSONArray array = binary.getArray("threads");
        List<Object> objects = array.toList();
        objects.remove(thread);
        array.clear();
        array.putAll(objects);

        binary.remove(thread);
    }

    public void addArray(String thread, String array) {
        JSONObject threadObject = binary.get(thread);
        JSONArray arrays = threadObject.optJSONArray("arrays");
        if (arrays == null) {
            arrays = new JSONArray();
            threadObject.put("arrays", arrays);
        }
        arrays.put(array);

        JSONObject arrayObject = new JSONObject();
        arrayObject.put("rows", new JSONArray());

        threadObject.put(array, arrayObject);
    }

    public void deleteArray(String thread, String array) {
        JSONObject threadObject = binary.get(thread);
        JSONArray arrays = threadObject.optJSONArray("arrays");
        if (arrays != null) {
            List<Object> objects = arrays.toList();
            objects.remove(array);
            arrays.clear();
            arrays.putAll(objects);

            threadObject.remove(array);
        }
    }

    public List<String> getAllRowData(String thread, String array) {
        List<String> rowDataList = new ArrayList<>();
        JSONObject threadObject = binary.get(thread);
        JSONObject arrayObject = threadObject.getJSONObject(array);

        for (Object rows : arrayObject.getJSONArray("rows").toList()) {
            rowDataList.add((String) rows);
        }

        return rowDataList;
    }

    public void addRow(String thread, String array, String row, String fileLocation) {
        JSONObject threadObject = binary.get(thread);
        JSONObject arrayObject = threadObject.getJSONObject(array);
        JSONArray rows = arrayObject.getJSONArray("rows");
        rows.put(row);

        JSONObject rowObject = new JSONObject();
        rowObject.put("location", fileLocation);

        arrayObject.put(row, rowObject);
    }

    public void removeRow(String thread, String array, int row) {
        JSONObject threadObject = binary.get(thread);
        JSONObject arrayObject = threadObject.getJSONObject(array);

        if (arrayObject.has(String.valueOf(row))) {
            arrayObject.remove(String.valueOf(row));

            JSONArray rows = arrayObject.getJSONArray("rows");
            List<Object> rowList = rows.toList();
            rowList.remove(String.valueOf(row));
            rows.clear();
            rows.putAll(rowList);
        }
    }

    public JSONObject getRowData(String thread, String array, String row) {
        JSONObject threadObject = binary.get(thread);
        JSONObject arrayObject = threadObject.getJSONObject(array);

        if (arrayObject.has(row)) {
            return arrayObject.getJSONObject(row);
        }

        return null; // Row not found
    }

    public boolean hasThread(String thread) {
        return binary.has(thread);
    }

    public boolean hasArray(String thread, String array) {
        JSONObject threadObject = binary.get(thread);
        return threadObject.has(array);
    }

    public boolean hasRow(String thread, String array, String row) {
        if (hasArray(thread, array)) {
            JSONObject threadObject = binary.get(thread);
            JSONObject arrayObject = threadObject.getJSONObject(array);
            return arrayObject.has(row);
        }
        return false;
    }

    public List<String> searchThreads(String searchTerm) {
        List<String> matchingThreads = new ArrayList<>();
        JSONArray threadArray = binary.getArray("threads");

        for (Object obj : threadArray) {
            String thread = (String) obj;
            if (thread.contains(searchTerm)) {
                matchingThreads.add(thread);
            }
        }

        return matchingThreads;
    }

    public List<String> searchArrays(String searchTerm, String thread) {
        List<String> matchingArrays = new ArrayList<>();
        JSONObject threadObject = binary.get(thread);

        for (String array : threadObject.keySet()) {
            if (array.contains(searchTerm)) {
                matchingArrays.add(array);
            }
        }

        return matchingArrays;
    }


}
