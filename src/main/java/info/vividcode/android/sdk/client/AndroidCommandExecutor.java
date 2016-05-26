/*
 * Copyright 2014 NOBUOKA Yu
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
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Executor for the tools/android command in Android SDK.
 */
public class AndroidCommandExecutor {

    /** File path of the tools/android command. */
    private final String mExecFilePath;

    public AndroidCommandExecutor(Path execFilePath) {
        mExecFilePath = execFilePath.toFile().getAbsolutePath();
    }

    Process executeCommand(String[] cmd) throws IOException {
        return Runtime.getRuntime().exec(cmd);
    }

    public void createAvd(String name, String target, String abi, ProcessIoHandler.Factory ioHandlerFactory) throws InterruptedException {
        String[] cmd = { mExecFilePath, "create", "avd",
        		"--name", name, "--target", target, "--abi", abi };
        final Process createAvdProc;
        try {
            createAvdProc = executeCommand(cmd);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        try (final ProcessIoHandler ioHandler = ioHandlerFactory.createHandler(createAvdProc)) {
            ioHandler.start();
            createAvdProc.waitFor();
        } catch (IOException err) {
            err.printStackTrace();
            throw new RuntimeException(err);
        }
    }

    public void updateSdkWithFilter(String filter, ProcessIoHandler.Factory ioHandlerFactory) throws InterruptedException {
        String[] cmd = new String[] { mExecFilePath, "update", "sdk", "--no-ui", "--all", "--filter", filter, };
        final Process updateSdkProc;
        try {
            updateSdkProc = executeCommand(cmd);
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        try (final ProcessIoHandler ioHandler = ioHandlerFactory.createHandler(updateSdkProc)) {
            ioHandler.start();
            updateSdkProc.waitFor();
        } catch (IOException err) {
            err.printStackTrace();
            throw new RuntimeException(err);
        }
    }

    private static Set<String> readAllDataFromBufferedReaderAndCreateList(BufferedReader r) throws IOException {
        Set<String> names = new HashSet<>();
        Pattern namePattern = Pattern.compile("id:\\s+\\d+\\s+or\\s+\"([^\"]+)\"");
        String line;
        while ((line = r.readLine()) != null) {
            Matcher m = namePattern.matcher(line);
            if (m.find()) {
                names.add(m.group(1));
            }
        }
        return names;
    }

    public Set<String> listSdk() {
        String[] cmd = new String[] { mExecFilePath, "list", "sdk", "--extended", };
        final Process listSdkProc;
        try {
            listSdkProc = executeCommand(cmd);
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        try (InputStream procStdout = listSdkProc.getInputStream();
                InputStream procErrout = listSdkProc.getErrorStream();
                OutputStream procInput = listSdkProc.getOutputStream();
                BufferedReader procStdoutReader = new BufferedReader(
                        new InputStreamReader(procStdout, StandardCharsets.UTF_8))) {
            Set<String> names = readAllDataFromBufferedReaderAndCreateList(procStdoutReader);
            try {
                listSdkProc.waitFor();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            if (listSdkProc.exitValue() != 0) {
                throw new RuntimeException("`android list sdk` command failed");
            }
            return names;
        } catch (IOException err) {
            throw new RuntimeException(err);
        }
    }

}
