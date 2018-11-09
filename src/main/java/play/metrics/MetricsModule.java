package play.metrics;

import com.google.inject.AbstractModule;
import io.prometheus.client.CollectorRegistry;

/**
 *
 * @author tuan.vu
 */
public class MetricsModule extends AbstractModule {

    public void configure() {
        System.out.println("\n------------- MetricsModule ------- \n\n\n\n");
        CollectorRegistry registry = CollectorRegistry.defaultRegistry;
        bind(CollectorRegistry.class).toInstance(registry);
    }
}
