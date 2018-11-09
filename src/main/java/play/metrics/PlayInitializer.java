package play.metrics;

import io.prometheus.client.hotspot.DefaultExports;

import java.util.Iterator;
import java.util.Vector;

public class PlayInitializer {

    static {
        System.out.println("PlayInitializer loaded");
        DefaultExports.initialize();
    }


    private static  Class<?> getPlayClazz() {
        Class<?> playClazz = null;
        try {
            ClassLoader myCL = Thread.currentThread().getContextClassLoader();
            while (myCL != null) {
                //System.out.println("\n\nClassLoader: " + myCL);
                for (Iterator iter = list(myCL); iter.hasNext(); ) {
                    String className = "" + iter.next();
                    System.out.println("\t" + className);

                    if(className.startsWith("class play.Play")) {
                        playClazz = myCL.loadClass("play.Play");
                    }
                }
                myCL = myCL.getParent();
            }
        } catch (Exception e) {}

        //System.out.println(playClazz.getClassLoader());
        return playClazz;
    }

    private static Iterator list(ClassLoader CL)
            throws NoSuchFieldException, SecurityException,
            IllegalArgumentException, IllegalAccessException {
        Class CL_class = CL.getClass();
        while (CL_class != java.lang.ClassLoader.class) {
            CL_class = CL_class.getSuperclass();
        }
        java.lang.reflect.Field ClassLoader_classes_field = CL_class
                .getDeclaredField("classes");
        ClassLoader_classes_field.setAccessible(true);
        Vector classes = (Vector) ClassLoader_classes_field.get(CL);
        return classes.iterator();
    }

}
