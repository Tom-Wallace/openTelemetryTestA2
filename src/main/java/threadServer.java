//Tom Wallace
//6482558
//COSC 5P07 Assignment 2 Question 2
//This file is a server which connects to fileSender, then forks off into forkFileReaders which receive files

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import io.opentelemetry.exporter.otlp.metrics.OtlpGrpcMetricExporter;
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.metrics.export.MetricExporter;
import io.opentelemetry.sdk.metrics.export.MetricReader;
import io.opentelemetry.sdk.metrics.export.PeriodicMetricReader;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.SpanProcessor;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import io.opentelemetry.semconv.resource.attributes.ResourceAttributes;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;
import java.util.concurrent.ForkJoinPool;

public class threadServer {
    ForkJoinPool threadPool;

    GlobalOpenTelemetry openTelemetry;

    public threadServer() {
        //Initializing resources used by OpenTelemetry
        Resource resource = Resource.getDefault().merge(Resource.create(Attributes.of(ResourceAttributes.SERVICE_NAME, "logical-service-name")));
        SdkTracerProvider sdkTracerProvider = SdkTracerProvider.builder().addSpanProcessor((SpanProcessor)BatchSpanProcessor.builder((SpanExporter)OtlpGrpcSpanExporter.builder().build()).build()).setResource(resource).build();
        SdkMeterProvider sdkMeterProvider = SdkMeterProvider.builder().registerMetricReader((MetricReader)PeriodicMetricReader.builder((MetricExporter)OtlpGrpcMetricExporter.builder().build()).build()).setResource(resource).build();
        Tracer tracer = GlobalOpenTelemetry.getTracer("threadServer", "1.0.0");
        Span mSpan = tracer.spanBuilder("establishPool").startSpan(); //first span for starting thread pool
        int port = 4000;
        try {
            Scope ss = mSpan.makeCurrent();
            try {
                this.threadPool = ForkJoinPool.commonPool();
                if (ss != null)
                    ss.close();
            } catch (Throwable throwable) {
                if (ss != null)
                    try {
                        ss.close();
                    } catch (Throwable throwable1) {
                        throwable.addSuppressed(throwable1);
                    }
                throw throwable;
            }
        } finally {
            mSpan.end(); //end of first span
        }
        Span sSpan = tracer.spanBuilder("startLoop").startSpan();
        try {
            Scope ms = sSpan.makeCurrent();
            try {
                try {
                    ServerSocket serverSocket = new ServerSocket(port); //starting serversocket
                    try {
                        Socket socket = null;
                        Socket[] sockets = new Socket[10];
                        forkFileReader[] readers = new forkFileReader[10];
                        int i = 0;
                        System.out.println("Now accepting connections");
                        while (i < 10) {//for each file
                            sSpan.addEvent("socket" + i);
                            try {
                                sockets[i] = serverSocket.accept();
                                System.out.println("Accepted");
                                readers[i] = new forkFileReader(sockets[i], i, mSpan);//creating new fork/thread
                                this.threadPool.execute(readers[i]);//starting fork
                                i++;
                            } catch (Exception e) {
                                sockets[i].close();
                                e.printStackTrace();
                            }
                        }
                        serverSocket.close();
                    } catch (Throwable throwable) {
                        try {
                            serverSocket.close();
                        } catch (Throwable throwable1) {
                            throwable.addSuppressed(throwable1);
                        }
                        throw throwable;
                    }
                } catch (IOException ex) {
                    System.out.println("Server exception: " + ex.getMessage());
                    ex.printStackTrace();
                }
                if (ms != null)
                    ms.close();
            } catch (Throwable throwable) {
                if (ms != null)
                    try {
                        ms.close();
                    } catch (Throwable throwable1) {
                        throwable.addSuppressed(throwable1);
                    }
                throw throwable;
            }
        } finally {
            sSpan.end();
        }
        Scanner keyboard = new Scanner(System.in);
        System.out.println("Any input to end");
        keyboard.next();
    }

    public static void main(String[] args) {
        threadServer ts = new threadServer();
    }
}
