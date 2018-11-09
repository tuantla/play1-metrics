# play1-metrics
Prometheus metrics for play1.x

- Download  [play1-metrics-0.1.jar](https://github.com/thunderbird84/play1-metrics/releases/download/0.1/play1-metrics-0.1-SNAPSHOT.jar) to /tmp/play1-metrics-0.1.jar

 - Modify  `<app_path>/conf/application.conf`
    ```...
    jvm.memory=-Xmx512M -Xms256M  -javaagent:/tmp/play1-metrics-0.1.jar
    ...
    ```

## Metrics supports:
```
#JMX default exporter DefaultExports.initialize();
http_requests_total

http_request_exceptions_total

http_request_success_total

```