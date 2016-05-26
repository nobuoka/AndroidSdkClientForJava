package info.vividcode.android.sdk.client;

import org.junit.Assert;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.regex.Pattern;

import info.vividcode.android.sdk.client.test.AbstractDummyProcess;

public class AutoRespondingProcessIoHandlerTest {

    @Test
    public void communicateSynchronously_automaticalResponsing() {
        Pattern testPattern = Pattern.compile("test response required.*");
        String testResponse = "test response";

        try (
                PipedInputStream procStdout = new PipedInputStream();
                PipedInputStream procErrout = new PipedInputStream();
                PipedOutputStream procInput = new PipedOutputStream()) {
            try (final AutoRespondingProcessIoHandler pua =
                         new AutoRespondingProcessIoHandler(
                                 testPattern, testResponse, procStdout, procErrout, procInput)) {
                final ConcurrentLinkedQueue<String> actualResponses =
                        new ConcurrentLinkedQueue<String>();
                AbstractDummyProcess dummyProc = new AbstractDummyProcess(procStdout, procErrout, procInput) {
                    @Override
                    protected void process(OutputStream procStdoutSrc,
                                           OutputStream procErroutSrc, BufferedReader procInputDst) throws IOException {
                        procStdoutSrc.write("test response required: ".getBytes(StandardCharsets.UTF_8));
                        procStdoutSrc.flush();
                        String actualResponse = procInputDst.readLine();
                        actualResponses.add(actualResponse);
                    }
                };

                dummyProc.start();
                pua.start();
                dummyProc.join();

                Assert.assertFalse("There is no error in the process.", dummyProc.errorOccured());
                Assert.assertArrayEquals("Expected response.",
                        new String[]{ "test response" }, actualResponses.toArray());
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
