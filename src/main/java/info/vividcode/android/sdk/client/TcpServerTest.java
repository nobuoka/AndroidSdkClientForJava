/*
 * Copyright 2015 NOBUOKA Yu
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
import java.io.ByteArrayOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class TcpServerTest {
    public static void main(String[] argv) throws Exception {
        try (ServerSocket server = new ServerSocket(0)) {
            System.out.println(
                    "クライアントからの接続を待ちます。 port : " +
                            server.getLocalPort());
            try (Socket socket = server.accept()) {
                System.out.println("クライアント接続。");

                final byte[] readBytes;
                try (
                        BufferedInputStream bis = new BufferedInputStream(socket.getInputStream());
                        ByteArrayOutputStream buf = new ByteArrayOutputStream()
                ) {
                    int read;
                    while ((read = bis.read()) != -1) {
                        buf.write(read);
                    }
                    readBytes = buf.toByteArray();
                }
                String port = new String(readBytes, StandardCharsets.UTF_8);
                System.out.println(port);
            }
        }
    }
}
