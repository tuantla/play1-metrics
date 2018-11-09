package plugins;

import play.PlayPlugin;
import play.metrics.Metrics;
import play.mvc.Http;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public class MetricPlugin extends PlayPlugin {

    @Override
    public void routeRequest(Http.Request request) {
        Metrics.requests.labels(request.method, request.path).inc();
    }

    @Override
    public void onInvocationSuccess() {
        Metrics.success.labels(Http.Request.current().method, Http.Request.current().path).inc();
    }

    @Override
    public void onInvocationException(Throwable e) {
        Metrics.exeptions.labels(Http.Request.current().method, Http.Request.current().path).inc();
    }
}
