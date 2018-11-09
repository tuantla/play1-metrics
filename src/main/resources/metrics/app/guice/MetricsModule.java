package guice;

import com.google.inject.AbstractModule;
import io.prometheus.client.CollectorRegistry;
/**
 *
 * @author tuan.vu
 */
public class MetricsModule extends AbstractModule {

    public void configure() {
        System.out.println("MetricsModule loaded");
        CollectorRegistry registry = CollectorRegistry.defaultRegistry;
        bind(CollectorRegistry.class).toInstance(registry);
    }
}
