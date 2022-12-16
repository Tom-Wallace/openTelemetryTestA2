//Tom Wallace
//6482558
//COSC 5P07 Assignment 2 Question 2
//This file connects to a threadServer, and sequentially sends text files to it which are received by forkFileReaders

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
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Scanner;

public class fileSender {
    FileInputStream fs;

    BufferedInputStream bs;

    OutputStream o;

    ObjectOutputStream oo;

    GlobalOpenTelemetry openTelemetry;

    public fileSender() {
        //Initializing resources used by OpenTelemetry
        Resource resource = Resource.getDefault().merge(Resource.create(Attributes.of(ResourceAttributes.SERVICE_NAME, "logical-service-name")));
        SdkTracerProvider sdkTracerProvider = SdkTracerProvider.builder().addSpanProcessor((SpanProcessor)BatchSpanProcessor.builder((SpanExporter)OtlpGrpcSpanExporter.builder().build()).build()).setResource(resource).build();
        SdkMeterProvider sdkMeterProvider = SdkMeterProvider.builder().registerMetricReader((MetricReader)PeriodicMetricReader.builder((MetricExporter)OtlpGrpcMetricExporter.builder().build()).build()).setResource(resource).build();
        Tracer tracer = GlobalOpenTelemetry.getTracer("fileSender", "1.0.0");
        int port = 4000;
        Span mSpan = tracer.spanBuilder("beginLoop").startSpan(); //first span comprises whole execution
        try {
            Scope ms = mSpan.makeCurrent();
            try {
                for (int i = 0; i < 10; i++) { //for each text file
                    Span sSpan = tracer.spanBuilder("sendFile" + i).startSpan(); //span for each file
                    Scope ss = sSpan.makeCurrent();
                    try {
                        try {
                            Socket socket = new Socket(InetAddress.getLocalHost().getHostAddress(), port);
                            try {
                                this.o = socket.getOutputStream();
                                File toSend = new File("inputFiles/file" + i + ".txt"); //assumes each text file is named file0.txt, file1.txt, etc.
                                byte[] sendFile = new byte[4092]; //buffer for sending files
                                this.fs = new FileInputStream(toSend); //reads in text file
                                this.bs = new BufferedInputStream(this.fs);
                                int count = 0;
                                sSpan.addEvent("beginSend");
                                while ((count = this.bs.read(sendFile)) > 0) { //While data remains to be sent
                                    this.o.write(sendFile, 0, count);
                                    this.o.flush();
                                }
                                sSpan.addEvent("endSend");
                                System.out.println("Sent");
                                this.o.close();
                                this.fs.close();
                                this.bs.close();
                                socket.close(); //close connection to forkFileReader and prepare for next one
                            } catch (Throwable throwable) {
                                try {
                                    socket.close();
                                } catch (Throwable throwable1) {
                                    throwable.addSuppressed(throwable1);
                                }
                                throw throwable;
                            }
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
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
                    sSpan.end();
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
            mSpan.end();
        }
        Scanner keyboard = new Scanner(System.in);
        System.out.println("Any input to end");
        keyboard.next();
    }

    public static void main(String[] args) throws UnknownHostException {
        fileSender fileS = new fileSender();
    }
}
