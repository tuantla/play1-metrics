package play.metrics;

import io.prometheus.client.Counter;

public class Metrics {

    public static final Counter requests = Counter.build()
            .name("http_requests_total").help("Total play request.").labelNames("method", "path").register();

    public static final Counter exeptions = Counter.build()
            .name("http_request_exceptions_total").help("Total play exception request.").labelNames("method", "path").register();

    public static final Counter success = Counter.build()
            .name("http_request_success_total").help("Total play success request.").labelNames("method", "path").register();
}
