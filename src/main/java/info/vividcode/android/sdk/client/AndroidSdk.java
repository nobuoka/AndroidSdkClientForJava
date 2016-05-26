package info.vividcode.android.sdk.client;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

public class AndroidSdk {

    private final Path mSdkDirPath;

    public AndroidSdk(Path sdkDirPath) {
        mSdkDirPath = sdkDirPath;
    }

    private final List<String> mAndroidCommandRelPathCandidates = Arrays.asList(
            "tools/android",
            "tools/android.bat"
    );
    private final List<String> mEmulatorCommandRelPathCandidates = Arrays.asList(
            "tools/emulator",
            "tools/emulator.exe"
    );
    private final List<String> mAdbCommandRelPathCandidates = Arrays.asList(
            "platform-tools/adb",
            "platform-tools/adb.exe"
    );

    public AndroidCommandExecutor androidCommand() {
        Path cmd = findPathOrThrow(mAndroidCommandRelPathCandidates, "tools/android");
        return new AndroidCommandExecutor(cmd);
    }

    public EmulatorCommandExecutor emulatorCommand() {
        Path cmd = findPathOrThrow(mEmulatorCommandRelPathCandidates, "tools/emulator");
        return new EmulatorCommandExecutor(cmd);
    }

    public AdbCommandExecutor adbCommand() {
        Path cmd = findPathOrThrow(mAdbCommandRelPathCandidates, "platform-tools/adb");
        return new AdbCommandExecutor(cmd);
    }

    private Path findPathOrThrow(List<String> relPathList, String targetFileLabel) {
        Path p = findPathOrNull(relPathList);
        if (p != null) {
            return p;
        } else {
            throw new RuntimeException("Target file not found: " + targetFileLabel);
        }
    }

    private Path findPathOrNull(List<String> relPathList) {
        for (String relPath : relPathList) {
            Path emulatorCommandCandidate = mSdkDirPath.resolve(relPath);
            if (emulatorCommandCandidate.toFile().exists()) {
                return emulatorCommandCandidate;
            }
        }
        return null;
    }

}
