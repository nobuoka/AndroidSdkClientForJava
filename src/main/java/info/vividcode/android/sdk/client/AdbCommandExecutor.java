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

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * Executor for the platform-tools/adb command in Android SDK.
 */
public class AdbCommandExecutor {

    /** File path of the platform-tools/adb command. */
    private final String mExecFilePath;

    public AdbCommandExecutor(Path execFilePath) {
        mExecFilePath = execFilePath.toFile().getAbsolutePath();
    }

    static Process executeCommand(String[] cmd) throws IOException {
        return Runtime.getRuntime().exec(cmd);
    }

    static <T extends ProcessIoHandler> Command<T> executeCommand(
            List<String> cmdArgsList, ProcessIoHandler.Factory<T> f)
            throws IOException {
        Process p = new ProcessBuilder().command(cmdArgsList).start();
        final T h;
        if (f != null) {
            h = f.createHandler(p);
            h.start();
        } else {
            h = null;
        }
        return new Command<>(p, h);
    }

    public void waitForDevice(int emulatorConsolePort) throws InterruptedException {
        List<String> cmdArgsList = Arrays.asList(mExecFilePath,
                "-s", "emulator-" + Integer.toString(emulatorConsolePort, 10),
                "wait-for-device");
        try (Command cmd = executeCommand(cmdArgsList, AdbOutputBufferingIoHandler.FACTORY)) {
            System.out.println("wait for device command progress...");
            cmd.waitFor();
            System.out.println("wait for device command finished");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public String shell(int emulatorConsolePort, String adbShellCommand) throws InterruptedException {
        List<String> cmdArgsList = Arrays.asList(mExecFilePath,
                "-s", "emulator-" + Integer.toString(emulatorConsolePort, 10),
                "shell", adbShellCommand);
        final Command<AdbOutputBufferingIoHandler> cmd;
        try {
            cmd = executeCommand(cmdArgsList, AdbOutputBufferingIoHandler.FACTORY);
            cmd.waitFor();
            return cmd.getProcessIoHandler().getResultSync();
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

}
