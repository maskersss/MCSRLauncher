package com.redlimerl.mcsrlauncher;

import com.formdev.flatlaf.FlatDarkLaf;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;

public class LauncherUpdater {
    private static final String ENDPOINT = "https://api.github.com/repos/MCSRLauncher/Launcher/releases/latest";
    private static final String JAR_NAME = "MCSRLauncher.jar";

    public static void main(String[] args) {
        Path basePath = Paths.get("");
        File launcherFile = basePath.resolve(JAR_NAME).toFile();

        FlatDarkLaf.setup();
        JDialog dialog = new JDialog((Frame) null, "MCSRLauncher Updater", true);
        dialog.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);

        JProgressBar progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);

        JLabel statusLabel = new JLabel("Downloading Update...");
        statusLabel.setHorizontalAlignment(JLabel.CENTER);

        dialog.setLayout(new BorderLayout(10, 10));
        dialog.add(statusLabel, BorderLayout.CENTER);
        dialog.add(progressBar, BorderLayout.SOUTH);

        dialog.setSize(400, 120);
        dialog.setLocationRelativeTo(null);

        if (!launcherFile.exists()) {
            statusLabel.setText("Error! \"" + launcherFile.getName() + "\" file is not exists in launcher directory.");
            dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
            return;
        }

        new SwingWorker<Void, Integer>() {
            @Override
            protected Void doInBackground() throws Exception {
                downloadUpdate(launcherFile, progressBar);
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    statusLabel.setText("Downloaded!");
                    progressBar.setValue(100);

                    dialog.dispose();
                } catch (Exception e) {
                    statusLabel.setText("Failed to download: " + e.getMessage());
                    dialog.setModal(false);
                    dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
                    e.printStackTrace();
                    return;
                }

                try {
                    var exeFile = basePath.resolve("launch.exe").toFile();
                    if (exeFile.exists()) {
                        new ProcessBuilder("launch.exe").start();
                    } else {
                        var shellFile = basePath.resolve("launch.sh").toFile();
                        if (shellFile.exists()) {
                            new ProcessBuilder("launch.sh").start();
                        } else {
                            new ProcessBuilder("java", "-jar", launcherFile.getName()).start();
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    JOptionPane.showMessageDialog(null, e.getMessage());
                }
            }
        }.execute();

        dialog.setVisible(true);
    }

    private static void downloadUpdate(File jarFile, JProgressBar progressBar) throws IOException {
        URL url = new URL(ENDPOINT);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();

        con.setRequestMethod("GET");
        con.setRequestProperty("Accept", "application/json");

        con.getResponseCode();
        BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
        String inputLine;
        StringBuilder content = new StringBuilder();

        while ((inputLine = in.readLine()) != null) {
            content.append(inputLine);
        }

        in.close();
        con.disconnect();

        JsonObject jsonObject = JsonParser.parseString(content.toString()).getAsJsonObject();
        for (JsonElement asset : jsonObject.getAsJsonArray("assets")) {
            JsonObject assetObject = asset.getAsJsonObject();
            if (assetObject.get("name").getAsString().equals(jarFile.getName())) {
                System.out.println("Found update, downloading...");

                url = new URL(assetObject.get("browser_download_url").getAsString());
                con = (HttpURLConnection) url.openConnection();
                con.getResponseCode();

                InputStream inputStream = con.getInputStream();
                FileOutputStream outputStream = new FileOutputStream(jarFile);

                byte[] buffer = new byte[4096];
                int bytesRead;
                long totalBytes = 0;
                long expectedSize = assetObject.get("size").getAsLong();

                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                    totalBytes += bytesRead;

                    int progress = (int) ((totalBytes * 100) / expectedSize);
                    progressBar.setValue(Math.min(progress, 100));
                }

                outputStream.close();
                inputStream.close();
                System.out.println("Downloaded to " + jarFile.getAbsolutePath());
                break;
            }
        }
    }
}