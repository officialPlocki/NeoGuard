package co.plocki.neoguard.server.key;

import co.plocki.json.JSONFile;
import co.plocki.neoguard.server.NeoGuard;
import org.json.JSONArray;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

public class KeyManager {

    private List<String> encryptedKeys = new ArrayList<>();

    public static String generateRandomPassword(int length) {
        SecureRandom random = new SecureRandom();
        byte[] passwordBytes = new byte[length];
        random.nextBytes(passwordBytes);

        // Convert bytes to hexadecimal representation
        StringBuilder password = new StringBuilder();
        for (byte b : passwordBytes) {
            password.append(String.format("%02x", b));
        }

        return password.toString();
    }

    public List<Object> registerKey() throws Exception {
        String uuid = UUID.randomUUID().toString().replaceAll("-", "");
        String encryptedKeyString = new String(generateRandomPassword(NeoGuard.pass_length).getBytes(StandardCharsets.UTF_8));

        encryptedKeys.add(new String(Base64.getDecoder().decode(encryptedKeyString)));
        saveEncryptedKeysToFile();

        return List.of(encryptedKeys.size() - 1, new String(Base64.getEncoder().encode(encryptedKeyString.getBytes(StandardCharsets.UTF_8))));
    }

    public void unregisterKey(int index) throws Exception {
        if (index >= 0 && index < encryptedKeys.size()) {
            encryptedKeys.remove(index);
            saveEncryptedKeysToFile();
        }
    }

    public void loadKeysFromFile() throws Exception {
        JSONFile jsonFile = new JSONFile("contents" + File.separator + "delivered" + File.separator + "keys.jbin");

        if (!jsonFile.isNew()) {
            JSONArray array = jsonFile.getArray("keys");
            List<String> keys = array.toList().stream()
                    .map(Object::toString)
                    .toList();

            encryptedKeys = new ArrayList<>();

            for (String key : keys) {
                encryptedKeys.add(new String(Base64.getDecoder().decode(key)));
            }
        }
    }

    public boolean containsKey(String encryptedKey) throws Exception {
        return encryptedKeys.contains(new String(Base64.getDecoder().decode(encryptedKey)));
    }

    public List<String> getKeys() {
        return encryptedKeys;
    }

    private void saveEncryptedKeysToFile() throws IOException {
        JSONFile jsonFile = new JSONFile("contents" + File.separator + "delivered" + File.separator + "keys.jbin");
        JSONArray array = new JSONArray();
        for (String encryptedKey : encryptedKeys) {
            array.put(Base64.getEncoder().encodeToString(encryptedKey.getBytes(StandardCharsets.UTF_8)));
        }
        jsonFile.put("keys", array);
        jsonFile.save();
    }
}