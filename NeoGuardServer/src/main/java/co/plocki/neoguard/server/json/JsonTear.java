package co.plocki.neoguard.server.json;

import java.io.IOException;

public interface JsonTear<T> {

    /**
     * Saves the given object to a file.
     * @param object The object to save.
     * @param filename The name of the file to save to.
     * @throws IOException If there's an I/O error during the save process.
     */
    void saveObject(T object, String filename) throws IOException;

    /**
     * Retrieves the saved object from a file.
     * @param filename The name of the file to load from.
     * @return The loaded object.
     * @throws IOException If there's an I/O error during the load process.
     * @throws ClassNotFoundException If the class of the loaded object cannot be found.
     */
    T loadObject(String filename) throws IOException, ClassNotFoundException;
}