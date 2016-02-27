/*
 * Copyright 2016 NOBUOKA Yu
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package info.vividcode.android.sdk.client;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class Avd implements Closeable {

    private final Socket mAvdConsoleTcpSocket;
    private final BufferedWriter mSocketWriter;
    private final BufferedReader mSocketReader;
    private final ExecutorService mExecutor = Executors.newSingleThreadExecutor();
    private final int mConsolePort;

    public static Avd create(int avdConsolePort) throws IOException {
        return new Avd(avdConsolePort);
    }

    private Avd(int avdConsolePort) throws IOException {
        mConsolePort = avdConsolePort;
        mAvdConsoleTcpSocket = new Socket("localhost", avdConsolePort);
        mSocketWriter =
                new BufferedWriter(new OutputStreamWriter(mAvdConsoleTcpSocket.getOutputStream()));
        mSocketReader =
                new BufferedReader(new InputStreamReader(mAvdConsoleTcpSocket.getInputStream()));
    }

    public int getConsolePort() {
        return mConsolePort;
    }

    public String readConsoleOutput(final int timeout, final TimeUnit unit) {
        final CountDownLatch waiter = new CountDownLatch(1);
        final AtomicReference<String> response = new AtomicReference<>();
        mExecutor.execute(new Runnable() {
            @Override
            public void run() {
                final long startTime = System.currentTimeMillis();
                try {
                    StringBuilder sb = new StringBuilder();
                    while (true) {
                        if (!mAvdConsoleTcpSocket.isClosed() && mSocketReader.ready()) {
                            String line = mSocketReader.readLine();
                            if (line == null) break;
                            if (sb.length() != 0) sb.append("\n");
                            sb.append(line);
                        } else {
                            long currentTime = System.currentTimeMillis();
                            if (currentTime - startTime >= TimeUnit.MILLISECONDS.convert(timeout, unit)) {
                                break;
                            } else {
                                Thread.sleep(1);
                            }
                        }
                    }
                    response.set(sb.toString());
                    waiter.countDown();
                } catch (IOException | InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        try {
            waiter.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return response.get();
    }

    public void sendCommand(String command) throws IOException {
        mSocketWriter.write(command);
        mSocketWriter.newLine();
        mSocketWriter.flush();
    }

    public void kill() throws IOException {
        sendCommand("kill");
    }

    @Override
    public void close() throws IOException {
        mSocketReader.close();
        mSocketWriter.close();
        mExecutor.shutdown();
        mAvdConsoleTcpSocket.close();
    }

}
