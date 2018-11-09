package play.metrics;

import javassist.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Field;
import java.net.JarURLConnection;
import java.security.ProtectionDomain;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;


public class MetricAgent implements ClassFileTransformer {

    private  static  Map<String, InputStream> metricResources;

    public static final String metricsResourcePath = "metrics/";
    public static Map<String, InputStream> listJar(JarFile jar) {
        Map<String, InputStream> result = new HashMap<>();
        try {
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry jarEntry = entries.nextElement();
                String name = jarEntry.getName();
                if (name.startsWith(metricsResourcePath)) { //filter according to the path
                    String entry = name.substring(metricsResourcePath.length());
                    if (!entry.isEmpty() && !entry.endsWith("/")) {
                        InputStream is = jar.getInputStream(jarEntry);
                        result.put(entry, is);
                    }
                }
            }

        } catch (Exception e) {
            System.out.println(Utils.stackTrace(e));
        }

        return result;
    }

    public static void premain( final String agentArgument, final Instrumentation instrumentation ) {
        System.out.println("MetricAgent start");
        try {
            JarURLConnection connection = (JarURLConnection)
                    MetricsModule.class.getResource("MetricsModule.class").openConnection();
            metricResources  =  listJar(connection.getJarFile());
            instrumentation.appendToBootstrapClassLoaderSearch(connection.getJarFile());

        } catch (Exception ex) {

        }

        instrumentation.addTransformer( new MetricAgent() );
    }

    public static void appendJavaSource(List<?> playJavaPath, File playApplicationPath, Class<?> virtualFileClazz) {
        try {
            File metricRootFile = new File(playApplicationPath.getAbsolutePath() + "/tmp/metrics");
            if (metricRootFile.exists()) {
                metricRootFile.delete();
            }
            metricRootFile.mkdirs();
            for (String javaPath : metricResources.keySet()) {
                File f = new File(metricRootFile.getAbsolutePath() + "/" + javaPath);
                f.getParentFile().mkdirs();
                //f.createNewFile();
                FileOutputStream fos = new FileOutputStream(f);
                try {
                    fos.write(Utils.inputStreamAsByteArray(metricResources.get(javaPath)));
                } finally {
                    fos.close();
                }
            }

            Object metricRoot = virtualFileClazz.getDeclaredMethod("open", File.class).invoke(null, metricRootFile);
            Object metricsJavaPath = virtualFileClazz.getMethod("child", String.class).invoke(metricRoot, "app");
            playJavaPath.getClass().getMethod("add", Object.class).invoke(playJavaPath, metricsJavaPath);

        } catch (Exception e) {
            System.out.println(Utils.stackTrace(e));
        }
    }

    public static void compileMetricsCode(Class<?> playClazz, Object playClasses, Object playClassLoader) {
        try {
            List<String> hookClasses = new ArrayList<>();
            for (String javaPath : metricResources.keySet()) {
                if (javaPath.startsWith("app/") && javaPath.endsWith(".java")) {
                    String fullClassName = javaPath.substring(4).replace('/','.').replaceAll(".java","");
                    hookClasses.add(fullClassName);
                }
            }
            for (String name : hookClasses) {
                Object applicationClass = playClasses.getClass().getDeclaredMethod("getApplicationClass", String.class).invoke(playClasses, name);

                applicationClass.getClass().getDeclaredMethod("compile").invoke(applicationClass);
                Field enhancedByteCodeField = applicationClass.getClass().getField("enhancedByteCode");
                byte[] enhancedByteCode = (byte[]) enhancedByteCodeField.get(applicationClass);

                File f = (File) playClazz.getDeclaredMethod("getFile", String.class)
                        .invoke(null, "precompiled/java/" + (name.replace(".", "/")) + ".class");
                f.getParentFile().mkdirs();
                FileOutputStream fos = new FileOutputStream(f);
                try {
                    fos.write(enhancedByteCode);
                } finally {
                    fos.close();
                }


                Class<?> clz = (Class<?>) playClassLoader.getClass().getDeclaredMethod("loadApplicationClass", String.class).invoke(playClassLoader, name);
                applicationClass.getClass().getDeclaredField("javaClass").set(applicationClass, clz);
                //System.out.println(clz.getCanonicalName());
            }


            List<?> all = (List<?>)playClasses.getClass().getDeclaredMethod("all").invoke(playClasses);
        } catch (Exception e) {
            System.out.println(Utils.stackTrace(e));
        }
    }

    static  final  String playClazzLoader = "play/classloading/ApplicationClassloader";
    static  final  String playApplicationClasses = "play/classloading/ApplicationClasses";
    static  final  String playPluginCollectionClazz = "play/plugins/PluginCollection";

    static  final  String playClazz = "play/Play";
    static  final  String routerClazz = "play/mvc/Router";
    static  final  String def = "private static boolean _loadMetrics;";

    public  static File   metricModuleRoot = new File("/etc/metrics");



    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {

        if (className.equals(playClazzLoader)) {
            return doClass( className, classBeingRedefined, classfileBuffer );
        } else if (className.equals(routerClazz)) {
            return prependRouter( className, classBeingRedefined, classfileBuffer );
        } else if (className.equals(playPluginCollectionClazz)) {
            System.out.println("transform " + className);
            return doPluginCollectionClass( className, classBeingRedefined, classfileBuffer );
        }
        return classfileBuffer;
    }

    @SuppressWarnings("Duplicates")
    private byte[] prependRouter( final String name, final Class clazz, byte[] b ) {
        ClassPool pool = ClassPool.getDefault();
        CtClass cl = null;

        try {
            cl = pool.makeClass( new java.io.ByteArrayInputStream( b ) );
            CtMethod loadModules = cl.getDeclaredMethod("load");

            StringBuilder sb = new StringBuilder();
            sb.append("prependRoute(\"GET\", \"/metrics\", \"Metrics.index\");");
            loadModules.insertAfter(sb.toString());


            b = cl.toBytecode();
        } catch( Exception e ) {
            System.err.println( "Could not instrument  " + name + ",  exception : " + e.getMessage() );
        } finally {

            if( cl != null ) {
                cl.detach();
            }
        }

        return b;
    }

    private byte[] doPluginCollectionClass( final String name, final Class clazz, byte[] b ) {
        ClassPool pool = ClassPool.getDefault();
        CtClass cl = null;


        try {
            cl = pool.makeClass( new java.io.ByteArrayInputStream( b ) );
            String defMetrics = "private static boolean _compileMetricsOnce;";
            CtField field = CtField.make( defMetrics, cl );
            cl.addField( field, "false");

            CtMethod loadPlugins = cl.getDeclaredMethod("loadPlugins");
            StringBuilder sb = new StringBuilder();
            sb.append("if (!_compileMetricsOnce) {")
                    .append("System.out.println(\"Compile metrics code before loadPlugins \");")
                    .append("play.metrics.MetricAgent.compileMetricsCode(play.Play.class, play.Play.classes, play.Play.classloader);")
                    .append("_compileMetricsOnce = true; }");
            loadPlugins.insertBefore(sb.toString());
            b = cl.toBytecode();
        } catch( Exception e ) {
            System.err.println( "Could not instrument  " + name + ",  exception : " + e.getMessage() );
            e.printStackTrace();
        } finally {

            if( cl != null ) {
                cl.detach();
            }
        }

        return b;
    }

    private byte[] doClass( final String name, final Class clazz, byte[] b ) {
        ClassPool pool = ClassPool.getDefault();
        CtClass cl = null;

        StringBuilder sb = new StringBuilder();
        sb.append("if (!_loadMetrics) {")
                .append("System.out.println(\"Load metrics modules \");")
                .append("play.metrics.MetricAgent.appendJavaSource(play.Play.javaPath, play.Play.applicationPath, play.vfs.VirtualFile.class);")
                .append("_loadMetrics = true; }");

        String beforeBody = sb.toString();

        try {
            cl = pool.makeClass( new java.io.ByteArrayInputStream( b ) );
            CtField field = CtField.make( def, cl );
            cl.addField( field, "false");
            for (CtConstructor ctConstructor : cl.getConstructors()) {
                ctConstructor.insertAfter(beforeBody);
            }

            String defMetrics = "private static boolean _compileMetricsOnce;";
            field = CtField.make( defMetrics, cl );
            cl.addField( field, "false");

            CtMethod getAllClasses = cl.getDeclaredMethod("getAllClasses");
            sb = new StringBuilder();
            sb.append("if (!_compileMetricsOnce) {")
              .append("System.out.println(\"before execution play.classloader.getAllClasses \");")  //getAllClasses
              //.append("play.metrics.MetricAgent.compileMetricsCode(play.Play.class, play.Play.classes, this);")
              .append("_compileMetricsOnce = true; }");
            getAllClasses.insertBefore(sb.toString());
            b = cl.toBytecode();
        } catch( Exception e ) {
            System.err.println( "Could not instrument  " + name + ",  exception : " + e.getMessage() );
        } finally {

            if( cl != null ) {
                cl.detach();
            }
        }

        return b;
    }

}
