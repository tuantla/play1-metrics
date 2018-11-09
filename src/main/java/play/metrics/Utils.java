package play.metrics;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;

public class Utils {

    public static String stackTrace(Throwable e) {
        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }

    public static byte[] inputStreamAsByteArray(InputStream in) {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            int next = in.read();
            while (next > -1) {
                bos.write(next);
                next = in.read();
            }
            bos.flush();
            byte[] result = bos.toByteArray();
            bos.close();

            return result;
        } catch (Exception e) {
            System.out.println(stackTrace(e));
        }
        return null;    }
}
