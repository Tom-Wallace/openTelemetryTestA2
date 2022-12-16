//Tom Wallace
//6482558
//COSC 5P07 Assignment 2 Question 2
//This file is a thread forking from threadServer, which receives and saves a file from fileSender

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.ImplicitContextKeyed;
import io.opentelemetry.context.Scope;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.concurrent.RecursiveAction;

public class forkFileReader extends RecursiveAction {
    Socket socket;

    InputStream input;

    OutputStream output;

    ObjectOutputStream objOut;

    ObjectInputStream objIn;

    FileOutputStream fs;

    int num;

    Span importSpan;

    Span threadSpan;

    Tracer tracer;

    GlobalOpenTelemetry openTelemetry;

    public forkFileReader(Socket s, int i, Span newSpan) throws IOException {
        this.socket = s;
        this.num = i;
        this.input = this.socket.getInputStream();
        this.fs = null;
        this.importSpan = newSpan;
        this.tracer = GlobalOpenTelemetry.getTracer("forkFileReader" + i, "1.0.0");
    }

    protected void compute() {
        Span threadSpan = this.tracer.spanBuilder("child" + this.num).setParent(Context.current().with((ImplicitContextKeyed)this.importSpan)).startSpan();
        try {
            Scope st = threadSpan.makeCurrent();
            try {
                byte[] recFile = new byte[4092]; //buffer for reading
                this.fs = new FileOutputStream("outputFiles/file" + this.num + ".txt"); //prepare output
                int count = 0;
                threadSpan.addEvent("beginRead");
                while ((count = this.input.read(recFile)) > 0) { //while data remains to be read
                    this.fs.write(recFile, 0, count);
                    this.fs.flush();
                    if (count != 4092) //If last read was less than buffer size; means last arrival
                        break;
                }
                threadSpan.addEvent("endRead");
                System.out.println("Download complete");
                this.fs.close();
                this.input.close();
                this.socket.close();//closing connection and output
                if (st != null)
                    st.close();
            } catch (Throwable throwable) {
                if (st != null)
                    try {
                        st.close();
                    } catch (Throwable throwable1) {
                        throwable.addSuppressed(throwable1);
                    }
                throw throwable;
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            threadSpan.end();
        }
    }
}
