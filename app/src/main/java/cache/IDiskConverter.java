package cache;

import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Type;

public interface IDiskConverter {

    /*读取*/
    <T> T load(InputStream source, Type type);
    /*写入*/
    boolean writer(OutputStream sink, Object data);

}
