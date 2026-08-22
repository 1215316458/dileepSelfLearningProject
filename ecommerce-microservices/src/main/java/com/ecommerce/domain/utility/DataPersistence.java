package com.ecommerce.domain.utility;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class DataPersistence {

    private DataPersistence() {}

    // Converts object to byte stream and writes to file.
    // ObjectOutputStream wraps FileOutputStream — adds object serialization on top of raw file writing.
    public static void serialize(Object obj, String filename) {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(filename))) {
            oos.writeObject(obj);
            System.out.println("  Serialized to: " + filename);
        } catch (IOException e) {
            throw new RuntimeException("Serialization failed for file: " + filename, e);
        }
    }

    // Reads byte stream from file and reconstructs the object.
    // Cast is unchecked — caller is responsible for passing the correct type.
    @SuppressWarnings("unchecked")
    public static <T> T deserialize(String filename) {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(filename))) {
            T obj = (T) ois.readObject();
            System.out.println("  Deserialized from: " + filename);
            return obj;
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException("Deserialization failed for file: " + filename, e);
        }
    }
}
