package cache;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.lang.reflect.Type;

public class SerializableDiskConverter implements IDiskConverter {




    @Override
    public <T> T load(InputStream source, Type type) {
        T value=null;
        ObjectInputStream inputStream=null;
        try {
             inputStream=new ObjectInputStream(source);
            value= (T) inputStream.readObject();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }finally {
            try {
                inputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return value;
    }

    @Override
    public boolean writer(OutputStream sink, Object data) {
        ObjectOutputStream oos=null;
        try {
            oos=new ObjectOutputStream(sink);
            oos.writeObject(data);
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            try {
                oos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return false;
    }
}
