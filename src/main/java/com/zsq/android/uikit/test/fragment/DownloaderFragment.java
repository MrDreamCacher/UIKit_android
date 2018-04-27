package com.zsq.android.uikit.test.fragment;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.util.Xml;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.white.progressview.HorizontalProgressView;
import com.zsq.android.uikit.R;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.impl.client.DefaultHttpClient;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.lang.ref.WeakReference;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;


/**
 * Created by ZHAOSHENGQI467 on 2017/9/4.
 * 断点续传下载类，修改自google DownloaderActivity
 */
public class DownloaderFragment extends Fragment {

    private final static String EXTRA_FILE_CONFIG_URL = "DownloaderActivity_config_url";
    private final static String EXTRA_CONFIG_VERSION = "DownloaderActivity_config_version";
    private final static String EXTRA_DATA_PATH = "DownloaderActivity_data_path";

    private final static int MSG_DOWNLOAD_SUCCEEDED = 0;
    private final static int MSG_DOWNLOAD_FAILED = 1;
    private final static int MSG_REPORT_PROGRESS = 2;
    private final static int MSG_REPORT_VERIFYING = 3;

    private final static long MS_PER_SECOND = 1000;
    private final static long MS_PER_MINUTE = 60 * 1000;
    private final static long MS_PER_HOUR = 60 * 60 * 1000;
    private final static long MS_PER_DAY = 24 * 60 * 60 * 1000;

    private final static String LOCAL_CONFIG_FILE = ".downloadConfig";
    private final static String LOCAL_CONFIG_FILE_TEMP = ".downloadConfig_temp";
    private final static String LOCAL_FILTERED_FILE = ".downloadConfig_filtered";

    private String mFileConfigUrl;
    private String mConfigVersion;
    private String mDataPath;
    private File mDataDir;

    private HorizontalProgressView mProgressView;
    private TextView mRemainingTime;

    private HttpClient mHttpClient;

    private Thread mDownloadThread;

    private Handler mHandler;

    private long mStartTime = 0;

    private static class DownloadHandler extends Handler {

        final WeakReference<DownloaderFragment> mReference;

        DownloadHandler(DownloaderFragment fragment) {
            mReference = new WeakReference<>(fragment);
        }

        @Override
        public void handleMessage(Message msg) {
            DownloaderFragment f = mReference.get();
            if (f == null) {
                return;
            }
            switch (msg.what) {
                case MSG_DOWNLOAD_SUCCEEDED:
                    f.onDownloadSucceeded();
                    break;
                case MSG_DOWNLOAD_FAILED:
                    f.onDownloadFailed((String) msg.obj);
                    break;
                case MSG_REPORT_PROGRESS:
                    f.onReportProgress(msg.arg1);
                    break;
                case MSG_REPORT_VERIFYING:
                    f.onReportVerifying();
                    break;
                default:
                    throw new IllegalArgumentException("unknown message id " + msg.what);
            }
        }
    }

    private void onReportVerifying() {
        mRemainingTime.setText("");
    }

    private void onReportProgress(int progress) {
        long now = SystemClock.elapsedRealtime();
        if (mStartTime == 0) {
            mStartTime = now;
        }
        long delta = now - mStartTime;
        String timeRemaining = getString(R.string.time_remaining_unknown);
        if ((delta > 3 * MS_PER_SECOND) && (progress > 100)) {
            long totalTime = 10000 * delta / progress;
            long timeLeft = Math.max(0L, totalTime - delta);
            if (timeLeft > MS_PER_DAY) {
                timeRemaining = Long.toString(
                        (timeLeft + MS_PER_DAY - 1) / MS_PER_DAY)
                        + " "
                        + getString(R.string.time_remaining_days);
            } else if (timeLeft > MS_PER_HOUR) {
                timeRemaining = Long.toString(
                        (timeLeft + MS_PER_HOUR - 1) / MS_PER_HOUR)
                        + " "
                        + getString(R.string.time_remaining_hours);
            } else if (timeLeft > MS_PER_MINUTE) {
                timeRemaining = Long.toString(
                        (timeLeft + MS_PER_MINUTE - 1) / MS_PER_MINUTE)
                        + " "
                        + getString(R.string.time_remaining_minutes);
            } else {
                timeRemaining = Long.toString(
                        (timeLeft + MS_PER_SECOND - 1) / MS_PER_SECOND)
                        + " "
                        + getString(R.string.time_remaining_seconds);
            }
        }
        mRemainingTime.setText(timeRemaining);
    }

    private void onDownloadFailed(String reason) {
        Log.e("DownloaderFragment", "Download stopped: " + reason);
    }

    private void onDownloadSucceeded() {

    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        mFileConfigUrl = args.getString(EXTRA_FILE_CONFIG_URL);
        mConfigVersion = args.getString(EXTRA_CONFIG_VERSION);
        mDataPath = args.getString(EXTRA_DATA_PATH);

        mHandler = new DownloadHandler(this);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.downloader, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mProgressView = (HorizontalProgressView) view.findViewById(R.id.progress_view);
        mRemainingTime = (TextView) view.findViewById(R.id.time_remaining);
        view.findViewById(R.id.cancel_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mDownloadThread != null) {
                    mDownloadThread.interrupt();
                }
            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mDownloadThread != null) {
            mDownloadThread.interrupt();
            try {
                mDownloadThread.join();
            } catch (InterruptedException e) {
                // Don't care
            }
        }
    }

    private void ensureDownload(String fileConfigUrl, String configVersion,
                                String dataPath, String filePath, String userAgent) {
        File dest = new File(dataPath);
        if (dest.exists()) {
            File file = new File(filePath);
            if (file.exists() && versionMatches(dest, configVersion)) {
                return;
            }
        }

        Log.d("DownloaderFragment", "start downloading file...");
        startDownloadThread();
    }

    private void startDownloadThread() {
        mProgressView.setProgress(0);
        mRemainingTime.setText("");
        mDownloadThread = new Thread(new Downloader());
        mDownloadThread.setPriority(Thread.NORM_PRIORITY - 1);
        mDownloadThread.start();
    }

    private boolean versionMatches(File dest, String configVersion) {
        return true;
    }

    private static class Config {
        long getSize() {
            long result = 0;
            for (File file : mFiles) {
                result += file.getSize();
            }
            return result;
        }

        static class File {
            File(String src, String dest, String md5, long size) {
                if (src != null) {
                    this.mParts.add(new Part(src, md5, size));
                }
                this.dest = dest;
            }

            static class Part {
                Part(String src, String md5, long size) {
                    this.src = src;
                    this.md5 = md5;
                    this.size = size;
                }

                String src;
                String md5;
                long size;
            }

            ArrayList<Part> mParts = new ArrayList<>();
            String dest;

            long getSize() {
                long result = 0;
                for (Part part : mParts) {
                    if (part.size > 0) {
                        result += part.size;
                    }
                }
                return result;
            }
        }

        String version;
        ArrayList<File> mFiles = new ArrayList<>();
    }

    private static class ConfigHandler extends DefaultHandler {

        static Config parse(InputStream is) throws SAXException,
                UnsupportedEncodingException, IOException {
            ConfigHandler handler = new ConfigHandler();
            Xml.parse(is, Xml.findEncodingByName("UTF-8"), handler);
            return handler.mConfig;
        }

        private ConfigHandler() {
            mConfig = new Config();
        }

        @Override
        public void startElement(String uri, String localName, String qName,
                                 Attributes attributes) throws SAXException {
            if (localName.equals("config")) {
                mConfig.version = getRequiredString(attributes, "version");
            } else if (localName.equals("file")) {
                String src = attributes.getValue("", "src");
                String dest = getRequiredString(attributes, "dest");
                String md5 = attributes.getValue("", "md5");
                long size = getLong(attributes, "size", -1);
                mConfig.mFiles.add(new Config.File(src, dest, md5, size));
            } else if (localName.equals("part")) {
                String src = getRequiredString(attributes, "src");
                String md5 = attributes.getValue("", "md5");
                long size = getLong(attributes, "size", -1);
                int length = mConfig.mFiles.size();
                if (length > 0) {
                    mConfig.mFiles.get(length - 1).mParts.add(
                            new Config.File.Part(src, md5, size));
                }
            }
        }

        private static String getRequiredString(Attributes attributes,
                                                String localName) throws SAXException {
            String result = attributes.getValue("", localName);
            if (result == null) {
                throw new SAXException("Expected attribute " + localName);
            }
            return result;
        }

        private static long getLong(Attributes attributes, String localName,
                                    long defaultValue) {
            String value = attributes.getValue("", localName);
            if (value == null) {
                return defaultValue;
            } else {
                return Long.parseLong(value);
            }
        }

        Config mConfig;
    }

    private class DownloaderException extends Exception {
        DownloaderException(String reason) {
            super(reason);
        }
    }

    private class Downloader implements Runnable {

        private final static int CHUNK_SIZE = 32 * 1024;
        private long mDownloadedSize;
        private long mTotalExpectedSize;
        private int mReportedProgress;
        private HttpGet mHttpGet;
        private byte[] mFileIOBuffer = new byte[CHUNK_SIZE];

        @Override
        public void run() {
            mDataDir = new File(mDataPath);
            mHttpClient = new DefaultHttpClient();
            try {
                Config config = getConfig();

                filter(config);

                persistentDownload(config);

            } catch (Exception e) {
                reportFailure(e.toString() + "");
            }
        }

        private void persistentDownload(Config config)
                throws IOException, DownloaderException {
            download(config);
        }

        private Config getConfig() throws DownloaderException,
                ClientProtocolException, IOException, SAXException {
            Config config = null;
            if (mDataDir.exists()) {
                config = getLocalConfig(mDataDir, LOCAL_CONFIG_FILE_TEMP);
                if (config == null || !mConfigVersion.equals(config.version)) {
                    if (config == null) {
                        Log.i("DownloaderFragment", "Couldn't find local config.");
                    } else {
                        Log.i("DownloaderFragment", "Local version out of sync. Wanted " +
                                mConfigVersion + " but have " + config.version);
                    }
                    config = null;
                }
            } else {
                Log.i("DownloaderFragment", "Creating directory " + mDataPath);
                if (!mDataDir.mkdirs()) {
                    throw new DownloaderException(
                            "Could not create the directory " + mDataPath);
                }
            }
            if (config == null) {
                File localConfig = download(mFileConfigUrl,
                        LOCAL_CONFIG_FILE_TEMP);
                InputStream is = new FileInputStream(localConfig);
                try {
                    config = ConfigHandler.parse(is);
                } finally {
                    quietClose(is);
                }
                if (!config.version.equals(mConfigVersion)) {
                    throw new DownloaderException(
                            "Configuration file version mismatch. Expected " +
                                    mConfigVersion + " received " +
                                    config.version);
                }
            }
            return config;
        }

        private void filter(Config config) throws IOException, DownloaderException {
            File filteredFile = new File(mDataDir, LOCAL_FILTERED_FILE);
            if (filteredFile.exists()) {
                return;
            }
            File localConfigFile = new File(mDataDir, LOCAL_CONFIG_FILE_TEMP);
            HashSet<String> keepSet = new HashSet<>();
            keepSet.add(localConfigFile.getCanonicalPath());

            HashMap<String, Config.File> fileMap = new HashMap<>();
            for (Config.File file : config.mFiles) {
                // 规范化的绝对路径, 具有唯一性
                String canonicalPath = new File(mDataDir, file.dest).getCanonicalPath();
                fileMap.put(canonicalPath, file);
            }
            recursiveFilter(mDataDir, fileMap, keepSet, false);
        }

        private boolean recursiveFilter(File base, HashMap<String, Config.File> fileMap,
                                        HashSet<String> keepSet, boolean filterBase)
                throws IOException, DownloaderException {
            boolean result = true;
            if (base.isDirectory()) {
                for (File child : base.listFiles()) {
                    result &= recursiveFilter(child, fileMap, keepSet, true);
                }
            }
            if (filterBase) {
                if (base.isDirectory() && base.listFiles().length == 0) {
                    result &= base.delete();
                } else {
                    if (shouldKeepFile(base, fileMap, keepSet)) {
                        result &= base.delete();
                    }
                }
            }
            return result;
        }

        private boolean shouldKeepFile(File file, HashMap<String, Config.File> fileMap,
                                       HashSet<String> keepSet) throws IOException, DownloaderException {
            String canonicalPath = file.getCanonicalPath();
            if (keepSet.contains(canonicalPath)) {
                return true;
            }
            Config.File configFile = fileMap.get(canonicalPath);
            return configFile != null && verifyFile(configFile, true);
        }

        private boolean verifyFile(Config.File file, boolean deleteInvalid)
                throws IOException, DownloaderException {
            reportVerifying();
            File dest = new File(mDataDir, file.dest);
            if (!dest.exists()) {
                return false;
            }
            long fileSize = file.getSize();
            long destLength = dest.length();
            if (fileSize != destLength) {
                Log.e("DownloaderFragment", "Length doesn't match. Expected " + fileSize
                        + " got " + destLength);
                if (deleteInvalid) {
                    dest.delete();
                    return false;
                }
            }
            FileInputStream is = new FileInputStream(dest);
            try {
                for (Config.File.Part part : file.mParts) {
                    if (part.md5 == null) {
                        continue;
                    }
                    MessageDigest digest = createDigest();
                    readIntoDigest(is, part.size, digest);
                    String hash = getHash(digest);
                    if (!hash.equalsIgnoreCase(part.md5)) {
                        Log.e("DownloaderFragment", "MD5 checksums don't match. " +
                                part.src + " Expected "
                                + part.md5 + " got " + hash);
                        if (deleteInvalid) {
                            quietClose(is);
                            dest.delete();
                        }
                        return false;
                    }
                }
            } finally {
                quietClose(is);
            }
            return true;
        }

        private MessageDigest createDigest() throws DownloaderException {
            MessageDigest digest;
            try {
                digest = MessageDigest.getInstance("MD5");
            } catch (NoSuchAlgorithmException e) {
                throw new DownloaderException("Couldn't create MD5 digest");
            }
            return digest;
        }

        private void readIntoDigest(FileInputStream is, long bytesToRead,
                                    MessageDigest digest) throws IOException {
            while(bytesToRead > 0) {
                int chunkSize = (int) Math.min(mFileIOBuffer.length,
                        bytesToRead);
                int bytesRead = is.read(mFileIOBuffer, 0, chunkSize);
                if (bytesRead < 0) {
                    break;
                }
                updateDigest(digest, bytesRead);
                bytesToRead -= bytesRead;
            }
        }

        private String getHash(MessageDigest digest) {
            StringBuilder builder = new StringBuilder();
            for(byte b : digest.digest()) {
                builder.append(Integer.toHexString((b >> 4) & 0xf));
                builder.append(Integer.toHexString(b & 0xf));
            }
            return builder.toString();
        }

        private void download(Config config) throws DownloaderException,
                ClientProtocolException, IOException {
            mDownloadedSize = 0;
            getSizes(config);
            Log.i("DownloaderFragment", "Total bytes to download: "
                    + mTotalExpectedSize);
            for(Config.File file : config.mFiles) {
                downloadFile(file);
            }
        }

        private void downloadFile(Config.File file) throws DownloaderException,
                FileNotFoundException, IOException, ClientProtocolException {
            boolean append = false;
            File dest = new File(mDataDir, file.dest);
            long bytesToSkip = 0;
            if (dest.exists() && dest.isFile()) {
                append = true;
                bytesToSkip = dest.length();
                mDownloadedSize += bytesToSkip;
            }
            FileOutputStream os = null;
            long offsetOfCurrentPart = 0;
            try {
                for(Config.File.Part part : file.mParts) {
                    if ((part.size > bytesToSkip) || (part.size == 0)) {
                        MessageDigest digest = null;
                        if (part.md5 != null) {
                            digest = createDigest();
                            if (bytesToSkip > 0) {
                                FileInputStream is = openInput(file.dest);
                                try {
                                    is.skip(offsetOfCurrentPart);
                                    readIntoDigest(is, bytesToSkip, digest);
                                } finally {
                                    quietClose(is);
                                }
                            }
                        }
                        if (os == null) {
                            os = openOutput(file.dest, append);
                        }
                        downloadPart(part.src, os, bytesToSkip,
                                part.size, digest);
                        if (digest != null) {
                            String hash = getHash(digest);
                            if (!hash.equalsIgnoreCase(part.md5)) {
                                Log.e("DownloaderFragment", "web MD5 checksums don't match. "
                                        + part.src + "\nExpected "
                                        + part.md5 + "\n     got " + hash);
                                quietClose(os);
                                dest.delete();
                                throw new DownloaderException(
                                        "Received bad data from web server");
                            } else {
                                Log.i("DownloaderFragment", "web MD5 checksum matches.");
                            }
                        }
                    }
                    bytesToSkip -= Math.min(bytesToSkip, part.size);
                    offsetOfCurrentPart += part.size;
                }
            } finally {
                quietClose(os);
            }
        }

        private FileInputStream openInput(String src)
                throws FileNotFoundException, DownloaderException {
            File srcFile = new File(mDataDir, src);
            File parent = srcFile.getParentFile();
            if (! parent.exists()) {
                parent.mkdirs();
            }
            if (! parent.exists()) {
                throw new DownloaderException("Could not create directory "
                        + parent.toString());
            }
            return new FileInputStream(srcFile);
        }

        private void getSizes(Config config)
                throws ClientProtocolException, IOException, DownloaderException {
            for (Config.File file : config.mFiles) {
                for(Config.File.Part part : file.mParts) {
                    if (part.size < 0) {
                        part.size = getSize(part.src);
                    }
                }
            }
            mTotalExpectedSize = config.getSize();
        }

        private long getSize(String url) throws ClientProtocolException,
                IOException {
            url = normalizeUrl(url);
            Log.i("DownloaderFragment", "Head " + url);
            HttpHead httpGet = new HttpHead(url);
            HttpResponse response = mHttpClient.execute(httpGet);
            if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
                throw new IOException("Unexpected Http status code "
                        + response.getStatusLine().getStatusCode());
            }
            Header[] clHeaders = response.getHeaders("Content-Length");
            if (clHeaders.length > 0) {
                Header header = clHeaders[0];
                return Long.parseLong(header.getValue());
            }
            return -1;
        }

        private File download(String src, String dest)
                throws DownloaderException, ClientProtocolException, IOException {
            File destFile = new File(mDataDir, dest);
            FileOutputStream os = openOutput(dest, false);
            try {
                downloadPart(src, os, 0, -1, null);
            } finally {
                quietClose(os);
            }
            return destFile;
        }

        private FileOutputStream openOutput(String dest, boolean append)
                throws DownloaderException, FileNotFoundException {
            File destFile = new File(mDataDir, dest);
            File parent = destFile.getParentFile();
            if (!parent.exists()) {
                throw new DownloaderException("Could not create directory "
                        + parent.toString());
            }
            return new FileOutputStream(destFile, append);
        }

        // 断点下载方法
        private void downloadPart(String src, FileOutputStream os, long startOffset,
                                  long expectedLength, MessageDigest digest)
                throws IOException, DownloaderException {
            boolean lengthIsKnown = expectedLength >= 0;
            if (startOffset < 0) {
                throw new IllegalArgumentException("Negative startOffset:"
                        + startOffset);
            }
            if (lengthIsKnown && (startOffset > expectedLength)) {
                throw new IllegalArgumentException(
                        "startOffset > expectedLength" + startOffset + " "
                                + expectedLength);
            }

            InputStream is = get(src, startOffset, expectedLength);
            try {
                long bytesRead = downloadStream(is, os, digest);
                if (lengthIsKnown) {
                    long expectedBytesRead = expectedLength - startOffset;
                    if (expectedBytesRead != bytesRead) {
                        throw new DownloaderException(
                                "Incorrect number of bytes received from server");
                    }
                }
            } finally {
                quietClose(is);
                mHttpGet = null;
            }
        }

        private long downloadStream(InputStream is, OutputStream os, MessageDigest digest)
                throws DownloaderException, IOException {
            long totalBytesRead = 0;
            while (true) {
                if (Thread.interrupted()) {
                    Log.i("DownloaderFragment", "downloader thread interrupted.");
                    mHttpGet.abort();
                    throw new DownloaderException("Thread interrupted");
                }
                int bytesRead = is.read(mFileIOBuffer);
                if (bytesRead < 0) {
                    break;
                }
                if (digest != null) {
                    updateDigest(digest, bytesRead);
                }
                totalBytesRead += bytesRead;
                os.write(mFileIOBuffer, 0, bytesRead);
                mDownloadedSize += bytesRead;
                int progress = (int) (Math.min(mTotalExpectedSize,
                        mDownloadedSize * 10000 / Math.max(1, mTotalExpectedSize)));
                if (progress != mReportedProgress) {
                    mReportedProgress = progress;
                    reportProgress(progress);
                }
            }
            return totalBytesRead;
        }

        private InputStream get(String url, long startOffset, long expectedLength)
                throws IOException {
            url = normalizeUrl(url);
            mHttpGet = new HttpGet(url);
            int expectedStatusCode = HttpStatus.SC_OK;
            if (startOffset > 0) {
                String range = "bytes=" + startOffset + "-";
                if (expectedLength >= 0) {
                    // range的排列从0开始，所以要减1
                    range += expectedLength - 1;
                    mHttpGet.addHeader("Range", range);
                    expectedStatusCode = HttpStatus.SC_PARTIAL_CONTENT;
                }
            }
            HttpResponse response = mHttpClient.execute(mHttpGet);
            long bytesToSkip = 0;
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode != expectedStatusCode) {
                if ((statusCode == HttpStatus.SC_OK)
                        && (expectedStatusCode
                        == HttpStatus.SC_PARTIAL_CONTENT)) {
                    Log.i("DownloaderFragment", "Byte range request ignored");
                    bytesToSkip = startOffset;
                } else {
                    throw new IOException("Unexpected Http status code "
                            + statusCode + " expected "
                            + expectedStatusCode);
                }
            }
            HttpEntity entity = response.getEntity();
            InputStream is = entity.getContent();
            if (bytesToSkip > 0) {
                is.skip(bytesToSkip);
            }
            return is;
        }

        private String normalizeUrl(String url) throws MalformedURLException {
            return (new URL(new URL(mFileConfigUrl), url)).toString();
        }

        private void reportProgress(int progress) {
            mHandler.sendMessage(
                    Message.obtain(mHandler, MSG_REPORT_PROGRESS, progress, 0));
        }

        private void reportFailure(String reason) {
            mHandler.sendMessage(
                    Message.obtain(mHandler, MSG_DOWNLOAD_FAILED, reason));
        }

        private void reportVerifying() {
            mHandler.sendMessage(
                    Message.obtain(mHandler, MSG_REPORT_VERIFYING));
        }

        private void updateDigest(MessageDigest digest, int bytesRead) {
            if (bytesRead == mFileIOBuffer.length) {
                digest.update(mFileIOBuffer);
            } else {
                byte[] temp = new byte[bytesRead];
                System.arraycopy(mFileIOBuffer, 0,
                        temp, 0, bytesRead);
                digest.update(temp);
            }
        }
    }

    private Config getLocalConfig(File destPath, String configFileName) {
        File configFile = new File(destPath, configFileName);
        FileInputStream is;
        try {
            is = new FileInputStream(configFile);
        } catch (FileNotFoundException e) {
            return null;
        }
        try {
            return ConfigHandler.parse(is);
        } catch (Exception e) {
            Log.e("DownloaderFragment", "Unable to read local config file", e);
            return null;
        } finally {
            quietClose(is);
        }
    }

    private void quietClose(InputStream is) {
        try {
            if (is != null) {
                is.close();
            }
        } catch (IOException e) {
            // Don't care
        }
    }

    private void quietClose(OutputStream os) {
        try {
            if (os != null) {
                os.close();
            }
        } catch (IOException e) {
            // Don't care
        }
    }
 }
