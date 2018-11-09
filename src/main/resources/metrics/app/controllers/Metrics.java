package controllers;

import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.exporter.common.TextFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.mvc.Controller;

import javax.inject.Inject;
import java.io.StringWriter;
import java.util.regex.Pattern;
import com.google.inject.name.Named;

public class Metrics extends Controller {

    private static Logger logger = LoggerFactory.getLogger(Metrics.class);

    @Inject
    protected static CollectorRegistry registry;

    public static void index(){
        if (registry ==null) {
            registry = CollectorRegistry.defaultRegistry;
        }
        StringWriter writer = new StringWriter();
        try {
            TextFormat.write004(
                    writer, registry.metricFamilySamples());
        } catch (Exception e) {
           logger.error("metrics export error", e);
        }
        play.mvc.Http.Response.current().contentType = TextFormat.CONTENT_TYPE_004;
        renderText(writer.toString());
    }
    
    public static String[] $index0 = new String[]{};
}
