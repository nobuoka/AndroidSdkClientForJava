/*
 * Copyright 2014-2016 NOBUOKA Yu
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

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Executor for the tools/emulator command in Android SDK.
 */
public class EmulatorCommandExecutor {

    /** File path of the tools/emulator command. */
    private final String mExecFilePath;

    public EmulatorCommandExecutor(Path execFilePath) {
        mExecFilePath = execFilePath.toFile().getAbsolutePath();
    }

    Process executeCommand(String[] cmd) throws IOException {
        return Runtime.getRuntime().exec(cmd);
    }

    private static class AvdConsolePortReceiver implements Closeable {
        @Override
        public void close() {
            mSocketThread.interrupt();
        }
        private AvdConsolePortReceiver(SocketThread t) {
            mSocketThread = t;
        }
        private final SocketThread mSocketThread;
        public static AvdConsolePortReceiver create() {
            SocketThread t = new SocketThread();
            t.start();
            return new AvdConsolePortReceiver(t);
        }
        public int getLocalPortSync() throws InterruptedException {
            return mSocketThread.getLocalPortSync();
        }
        public int waitForAvdConsolePort() throws InterruptedException {
            return mSocketThread.getAvdConsolePortSync();
        }
        public void waitForFinish() throws InterruptedException {
            mSocketThread.join();
        }
        private static class SocketThread extends Thread {
            private final AtomicInteger mLocalPort = new AtomicInteger();
            private final CountDownLatch mLocalPortWaiter = new CountDownLatch(1);

            private final AtomicInteger mAvdConsolePort = new AtomicInteger();
            private final CountDownLatch mAvdConsolePortWaiter = new CountDownLatch(1);

            public int getLocalPortSync() throws InterruptedException {
                mLocalPortWaiter.await();
                return mLocalPort.get();
            }
            public int getAvdConsolePortSync() throws InterruptedException {
                mAvdConsolePortWaiter.await();
                return mAvdConsolePort.get();
            }
            public void run() {
                try (ServerSocket server = new ServerSocket(0)) {
                    int localPort = server.getLocalPort();
                    mLocalPort.set(localPort);
                    mLocalPortWaiter.countDown();
                    //System.out.println("クライアントからの接続を待ちます。 port : " + localPort);
                    try (Socket socket = server.accept()) {
                        //System.out.println("クライアント接続。");
                        byte[] readBytes = readAllBytesFromInputStream(socket.getInputStream());
                        int port = Integer.parseInt(new String(readBytes, StandardCharsets.UTF_8), 10);
                        mAvdConsolePort.set(port);
                        mAvdConsolePortWaiter.countDown();
                    }
                } catch (Throwable error) {
                    // TODO : なんとかする。
                }
            }
            private byte[] readAllBytesFromInputStream(InputStream is) throws IOException {
                try (
                        BufferedInputStream bis = new BufferedInputStream(is);
                        ByteArrayOutputStream buf = new ByteArrayOutputStream()
                ) {
                    int read;
                    while ((read = bis.read()) != -1) {
                        buf.write(read);
                    }
                    return buf.toByteArray();
                }
            }
        }
    }

    private List<String> readLines(BufferedReader r) throws IOException {
        List<String> lines = new ArrayList<>();
        String line;
        while ((line = r.readLine()) != null) {
            lines.add(line);
        }
        return lines;
    }

    public List<String> listAvds() throws InterruptedException {
        String[] cmdArray = { mExecFilePath, "-list-avds" };
        final Process cmd;
        try {
            cmd = executeCommand(cmdArray);
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        try (InputStream procStdout = cmd.getInputStream();
             InputStream procErrout = cmd.getErrorStream();
             OutputStream procInput = cmd.getOutputStream();
             BufferedReader procStdoutReader = new BufferedReader(
                     new InputStreamReader(procStdout, StandardCharsets.UTF_8))) {
            List<String> avdNames = readLines(procStdoutReader);
            cmd.waitFor();
            if (cmd.exitValue() != 0) {
                throw new RuntimeException();
            }
            return avdNames;
        } catch (IOException err) {
            throw new RuntimeException(err);
        }
    }

    public Avd startAvd(String name) throws InterruptedException, IOException {
        return startAvd(name, Arrays.asList("-no-window", "-no-boot-anim"));
    }

    public Avd startAvdWithConsolePort(String name, int port, List<String> options) throws InterruptedException, IOException {
        if (!checkPortIsAvailable(port)) {
            throw new RuntimeException("Specified port " + port + " is in use.");
        }

        ArrayList<String> cmdList = new ArrayList<>();
        cmdList.addAll(Arrays.asList(mExecFilePath, "-avd", name, "-port", "5554"));
        cmdList.addAll(options);
        final Process createAvdProc;
        try {
            // TODO : エラー出力を出すようにする？
            createAvdProc = executeCommand(cmdList.toArray(new String[cmdList.size()]));
            // TODO : IO の扱い何とかする必要がありそう。
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        long startTime = System.currentTimeMillis();
        while (!checkPortIsAvailable(port)) {
            long elapsedTimeInMs = System.currentTimeMillis() - startTime;
            if (elapsedTimeInMs < 5000) {
                System.out.println("Waiting for AVD console");
                Thread.sleep(100);
            } else {
                // TODO : kill process and read error output.
                throw new RuntimeException("Timeout! AVD console is not available");
            }
        }
        Avd avd = Avd.create(port);
        avd.readConsoleOutput(10, TimeUnit.SECONDS);
            /*
            avd.sendCommand("avd status");
            System.out.println(avd.readConsoleOutput(2, TimeUnit.SECONDS));
            */
        return avd;
    }

    public Avd startAvd(String name, List<String> options) throws InterruptedException, IOException {
        try (AvdConsolePortReceiver avdConsolePortReceiver = AvdConsolePortReceiver.create()) {
            int receivingLocalPort = avdConsolePortReceiver.getLocalPortSync();
            ArrayList<String> cmdList = new ArrayList<>();
            cmdList.addAll(Arrays.asList(mExecFilePath, "-avd", name,
                    "-report-console", "tcp:" + Integer.toString(receivingLocalPort, 10)));
            cmdList.addAll(options);
            // C:\Users\nobuoka\.android-sdk\tools\emulator.exe -avd test1 -report-console tcp:8001
            final Process createAvdProc;
            try {
                // TODO : エラー出力を出すようにする？
                createAvdProc = executeCommand(cmdList.toArray(new String[cmdList.size()]));
                // TODO : IO の扱い何とかする必要がありそう。
                // Linux ではエミュレータが動いている間はこのプロセスが動き続けるので終了を待たない。
                /*
                createAvdProc.waitFor();
                int exitCode = createAvdProc.exitValue();
                if (exitCode != 0) {
                    throw new RuntimeException("`emulator` command failed. Status code: " + exitCode);
                }
                */
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            int avdConsolePort = avdConsolePortReceiver.waitForAvdConsolePort();
            avdConsolePortReceiver.waitForFinish();
            System.out.println("avdConsolePort: " + avdConsolePort);
            Avd avd = Avd.create(avdConsolePort);
            avd.readConsoleOutput(10, TimeUnit.SECONDS);
            /*
            avd.sendCommand("avd status");
            System.out.println(avd.readConsoleOutput(2, TimeUnit.SECONDS));
            */
            return avd;
        }
    }

    private static boolean checkPortIsAvailable(int port) {
        try (Socket ignored = new Socket("localhost", port)) {
            return false;
        } catch (IOException ignored) {
            return true;
        }
    }

}
