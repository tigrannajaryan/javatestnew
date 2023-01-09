package com.mycompany.app;

//import java.util.UUID;

import org.apache.logging.log4j.LogManager;

//import io.opentelemetry.api.OpenTelemetry;
//import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.Scope;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.exporter.otlp.metrics.OtlpGrpcMetricExporter;
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter;
import io.opentelemetry.exporter.logging.LoggingSpanExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.metrics.export.PeriodicMetricReader;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import io.opentelemetry.semconv.resource.attributes.ResourceAttributes;
import io.opentelemetry.api.logs.GlobalLoggerProvider;
import io.opentelemetry.api.logs.Logger;
import io.opentelemetry.api.logs.LoggerProvider;
import io.opentelemetry.sdk.logs.SdkLoggerProvider;

/**
 * Hello world!
 *
 */
public class App {

    public static OpenTelemetrySdk openTelemetry;
    
    private static final org.apache.logging.log4j.Logger logger = LogManager.getLogger(App.class);

    public static void main(String[] args) {
        logger.info("App is starting");
        initOtel();
        createSpan();
        createRawLogRecord();
    }    

    // public static void initOtelAuto() {
    //     AutoConfiguredOpenTelemetrySdk.builder()
    //     .addResourceCustomizer(
    //         (resource, configProperties) ->
    //             resource.merge(
    //                 Resource.builder()
    //                     .put("service.instance.id", UUID.randomUUID().toString())
    //                     .build()))
    //     .build();

    // }

    public static void initOtel() 
    {
        Resource resource = Resource.getDefault()
        .merge(Resource.create(Attributes.of(ResourceAttributes.SERVICE_NAME, "logical-service-name")));

        SdkTracerProvider sdkTracerProvider = SdkTracerProvider.builder()
            .addSpanProcessor(BatchSpanProcessor.builder(OtlpGrpcSpanExporter.builder().build()).build())
            .addSpanProcessor(SimpleSpanProcessor.create(LoggingSpanExporter.create()))
            .setResource(resource)
            .build();

        SdkMeterProvider sdkMeterProvider = SdkMeterProvider.builder()
            .registerMetricReader(PeriodicMetricReader.builder(OtlpGrpcMetricExporter.builder().build()).build())
            .setResource(resource)
            .build();

        SdkLoggerProvider sdkLoggerProvider =
            SdkLoggerProvider.builder()
              .setResource(resource)
              //.addLogProcessor(...)
              .build();

        openTelemetry = OpenTelemetrySdk.builder()
            .setTracerProvider(sdkTracerProvider)
            .setMeterProvider(sdkMeterProvider)
            .setLoggerProvider(sdkLoggerProvider)
            .setPropagators(ContextPropagators.create(W3CTraceContextPropagator.getInstance()))
            .buildAndRegisterGlobal();

        GlobalLoggerProvider.set(sdkLoggerProvider);

        //GlobalLoggerProvider.set(openTelemetry.getSdkLoggerProvider());

        logger.info("Otel is initialized");
    }

    public static void createSpan() {
        Tracer tracer = openTelemetry.getTracer("instrumentation-library-name", "1.0.0");
        Span span = tracer.spanBuilder("my span").startSpan();
        // Make the span the current span
        try (Scope ss = span.makeCurrent()) {
            // In this scope, the span is the current/active span
            logger.info("Inside span");
        } finally {
            span.end();
        }
    }

    public static void createRawLogRecord() {
        LoggerProvider provider = GlobalLoggerProvider.get();

        Logger logger = provider.get("instrumentation-library-name");
        logger.logRecordBuilder().setBody("test log").emit();
    }
}
