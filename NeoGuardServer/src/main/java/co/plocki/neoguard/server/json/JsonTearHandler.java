package co.plocki.neoguard.server.json;

import java.io.IOException;

public class JsonTearHandler<T> implements JsonTear<T> {
    
    @Override
    public void saveObject(T object, String filename) throws IOException {

    }

    @Override
    public T loadObject(String filename) throws IOException, ClassNotFoundException {
        return null;
    }
}
