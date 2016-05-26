package info.vividcode.android.sdk.client.demo;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;

import info.vividcode.android.sdk.client.AdbCommandExecutor;
import info.vividcode.android.sdk.client.AndroidCommandExecutor;
import info.vividcode.android.sdk.client.AndroidSdk;
import info.vividcode.android.sdk.client.AutoRespondingProcessIoHandler;
import info.vividcode.android.sdk.client.Avd;
import info.vividcode.android.sdk.client.EmulatorCommandExecutor;

public class Main {

    public static void main(String[] args) throws InterruptedException, IOException {
        Path sdkDir = Paths.get("C:/Users/nobuoka/.android-sdk/");
        AndroidSdk androidSdk = new AndroidSdk(sdkDir);
        EmulatorCommandExecutor emulatorCmd = androidSdk.emulatorCommand();
        // 既に存在する AVD の名前一覧を取得。
        List<String> avdNames = emulatorCmd.listAvds();
        // 使用する AVD の名前。
        String targetAvdName = "test2";
        // 使用しようとしている AVD が既にに存在するかどうか。
        boolean targetAvdExists = avdNames.contains(targetAvdName);

        // まだ存在しない場合は作成する。
        if (!targetAvdExists) {
            AndroidCommandExecutor androidCmd = androidSdk.androidCommand();
            // 既にインストール済みかどうか気にせず、とりあえず必要なシステムイメージなどのインストール (アップデート)。
            AutoRespondingProcessIoHandler.Factory f = new AutoRespondingProcessIoHandler.Factory(Pattern.compile("Do you accept the license .*"), "y");
            androidCmd.updateSdkWithFilter("android-19", f);
            androidCmd.updateSdkWithFilter("sys-img-x86-android-19", f);
            // AVD 作成。
            AutoRespondingProcessIoHandler.Factory f2 = new AutoRespondingProcessIoHandler.Factory(Pattern.compile("Do you wish to create a custom hardware profile .*"), "");
            androidCmd.createAvd(targetAvdName, "android-19", "x86", f2);
        }

        // AVD 起動。
        try (Avd avd = emulatorCmd.startAvdWithConsolePort(targetAvdName, 5554, Collections.<String>emptyList())) {
            // AVD のコンソールポートといろいろやり取りできる状態。
            System.out.println("AVD started");

            // adb で操作できるようになるまで待つ。
            // (あくまで adb デーモンの起動を待つだけなので、ブート完了は別に待つ必要がある。)
            AdbCommandExecutor adbCommand = androidSdk.adbCommand();
            while (true) {
                System.out.println("wait for device...");
                final AtomicBoolean complete = new AtomicBoolean(false);
                final java.lang.Thread mainThread = Thread.currentThread();
                java.lang.Thread t = new Thread() {
                    public void run() {
                        try {
                            java.lang.Thread.sleep(TimeUnit.MILLISECONDS.convert(10, TimeUnit.SECONDS));
                            if (!complete.get()) {
                                mainThread.interrupt();
                            }
                        } catch (InterruptedException e) {
                            // do nothing
                        }
                    }
                };
                t.start();
                try {
                    adbCommand.waitForDevice(avd.getConsolePort());
                    complete.set(true);
                    t.interrupt();
                    break;
                } catch (InterruptedException e) {
                    System.out.println("avd status!");
                    avd.sendCommand("avd status");
                    System.out.println(avd.readConsoleOutput(2, TimeUnit.SECONDS));
                }
            }
            System.out.println("done!!!");

            // ブート完了を待つ。
            // See: https://devmaze.wordpress.com/2011/12/12/starting-and-stopping-android-emulators/
            // See: http://android.stackexchange.com/questions/83726/how-to-adb-wait-for-device-until-the-home-screen-shows-up
            System.out.println("wait for device boot complete");
            while (true) {
                String bootComplete = adbCommand.shell(avd.getConsolePort(), "getprop dev.bootcomplete");
                if (bootComplete.equals("1")) break;
                else Thread.sleep(500);
            }
            System.out.println("device boot complete!!!");
            final Thread t = Thread.currentThread();
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    t.interrupt();
                }
            }).start();
            System.out.println("while 1");
            try {
                String bootComplete = adbCommand.shell(avd.getConsolePort(), "while [ true ] ; do sleep 1; done");
                System.out.println("comp!!!! " + bootComplete);
            } catch (InterruptedException e) {
                System.out.println("here?");
                e.printStackTrace();
            }
            System.out.println("yea");

            // ここで Avd を使っていろいろやる。

            // 終了。
            avd.kill();
            System.out.println("AVD stopping");
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println("finish!");
        /*
        Set<String> componentNames = androidCmdExec.listSdk();
        for (String s : componentNames) {
        	System.out.println(s);
        }
        */
    }

}
