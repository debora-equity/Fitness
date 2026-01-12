package com.softgenia.playlist.service;

import com.softgenia.playlist.model.dto.video.StoredVideoResult;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.UUID;

@Service
public class FileStorageService {

    @Value("${upload.path}")
    private String uploadPath;

    @Value("${ffmpeg.path}")
    private String ffmpegPath;

    @Value("${ffprobe.path}")
    private String ffprobePath;

    private Path uploadRoot;

    @PostConstruct
    public void init() {
        uploadRoot = Paths.get(uploadPath).toAbsolutePath().normalize();
        try {
            Files.createDirectories(uploadRoot.resolve("videos"));
            Files.createDirectories(uploadRoot.resolve("thumbnails"));
            Files.createDirectories(uploadRoot.resolve("documents"));
        } catch (IOException e) {
            throw new RuntimeException("Could not initialize upload folders", e);
        }
    }


    public StoredVideoResult saveFile(MultipartFile file) throws IOException, InterruptedException {

        String extension = getExtension(file.getOriginalFilename());
        String videoId = UUID.randomUUID().toString();

        Path videoFolder = uploadRoot.resolve("videos").resolve(videoId);
        Files.createDirectories(videoFolder);

        Path mp4Path = videoFolder.resolve("input" + extension);
        Files.copy(file.getInputStream(), mp4Path, StandardCopyOption.REPLACE_EXISTING);

        int duration = getDurationFromMp4(mp4Path);
        String thumbnailUrl = generateThumbnailFromMp4(mp4Path);
        transcodeToHls(mp4Path, videoFolder);

        Files.deleteIfExists(mp4Path);

        return new StoredVideoResult(
                "videos/" + videoId + "/master.m3u8",
                thumbnailUrl,
                duration);
    }

    public String saveImage(MultipartFile file) throws IOException {
        String extension = getExtension(file.getOriginalFilename());
        String filename = UUID.randomUUID().toString() + extension;
        Path targetPath = uploadRoot.resolve("thumbnails").resolve(filename);
        Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
        return "thumbnails/" + filename;
    }

    public String savePdf(MultipartFile file) throws IOException {
        String extension = getExtension(file.getOriginalFilename());
        String filename = UUID.randomUUID().toString() + extension;
        Path targetPath = uploadRoot.resolve("documents").resolve(filename);
        Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
        return "documents/" + filename;
    }

    public void linearizePdf(String relativeFilePath) {
        try {
            Path inputPath = Paths.get(uploadPath).resolve(relativeFilePath).normalize();
            String inputStr = inputPath.toString();

            String outputStr = inputStr.replace(".pdf", "_linearized.pdf");

            ProcessBuilder pb = new ProcessBuilder(
                    "qpdf",
                    "--linearize",
                    "--replace-input",
                    inputStr);

            pb.redirectErrorStream(true);

            Process process = pb.start();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                while (reader.readLine() != null) {
                }
            }

            int exitCode = process.waitFor();

            if (exitCode != 0) {
                System.err.println("QPDF failed to linearize the PDF. Exit code: " + exitCode);
            } else {
                System.out.println("PDF Linearization successful: " + relativeFilePath);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void transcodeToHls(Path input, Path outputDir) {
        try {
            ProcessBuilder pb = new ProcessBuilder(
                    ffmpegPath,
                    "-i", input.toString(),

                    "-filter_complex",
                    "[0:v]split=4[v1][v2][v3][v4];" +
                            "[v1]scale=640:360[v360];" +
                            "[v2]scale=1280:720[v720];" +
                            "[v3]scale=1920:1080[v1080];" +
                            "[v4]scale=2560:1440[v1440]",

                    "-map", "[v360]", "-map", "a:0",
                    "-c:v:0", "libx264", "-b:v:0", "800k",
                    "-c:a:0", "aac", "-b:a:0", "96k",

                    "-map", "[v720]", "-map", "a:0",
                    "-c:v:1", "libx264", "-b:v:1", "2500k",
                    "-c:a:1", "aac", "-b:a:1", "128k",

                    "-map", "[v1080]", "-map", "a:0",
                    "-c:v:2", "libx264", "-b:v:2", "5000k",
                    "-c:a:2", "aac", "-b:a:2", "160k",

                    "-map", "[v1440]", "-map", "a:0",
                    "-c:v:3", "libx264", "-b:v:3", "8000k",
                    "-c:a:3", "aac", "-b:a:3", "192k",

                    "-f", "hls",
                    "-hls_time", "6",
                    "-hls_playlist_type", "vod",
                    "-hls_flags", "independent_segments",
                    "-var_stream_map", "v:0,a:0 v:1,a:1 v:2,a:2 v:3,a:3",
                    "-master_pl_name", "master.m3u8",
                    outputDir.resolve("stream_%v.m3u8").toString()
            );

            pb.redirectErrorStream(true);
            Process p = pb.start();

            StringBuilder output = new StringBuilder();
            try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                String line;
                while ((line = r.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }

            if (p.waitFor() != 0) {
                throw new RuntimeException("HLS transcoding failed. Output: " + output.toString());
            }

        } catch (Exception e) {
            throw new RuntimeException("FFmpeg HLS error: " + e.getMessage(), e);
        }
    }


    private String generateThumbnailFromMp4(Path mp4) throws IOException, InterruptedException {
        String name = UUID.randomUUID() + ".jpg";
        Path thumb = uploadRoot.resolve("thumbnails").resolve(name);

        ProcessBuilder pb = new ProcessBuilder(
                ffmpegPath,
                "-y",
                "-ss", "00:00:01",
                "-i", mp4.toString(),
                "-vframes", "1",
                "-vf", "scale=320:-1",
                thumb.toString());

        pb.redirectErrorStream(true);
        Process p = pb.start();

        try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
            while (r.readLine() != null) {
            }
        }

        if (p.waitFor() != 0) {
            throw new RuntimeException("Thumbnail generation failed");
        }

        return "thumbnails/" + name;
    }

    private int getDurationFromMp4(Path mp4) throws IOException {
        ProcessBuilder pb = new ProcessBuilder(
                ffprobePath,
                "-v", "error",
                "-show_entries", "format=duration",
                "-of", "default=noprint_wrappers=1:nokey=1",
                mp4.toString());

        Process p = pb.start();
        try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
            return (int) Math.round(Double.parseDouble(r.readLine()));
        }
    }

    public Resource loadAsResource(String path) {
        try {
            String decoded = URLDecoder.decode(path, StandardCharsets.UTF_8);
            Path file = uploadRoot.resolve(decoded).normalize();

            System.out.println("DEBUG: path=" + path);
            System.out.println("DEBUG: decoded=" + decoded);
            System.out.println("DEBUG: uploadRoot=" + uploadRoot);
            System.out.println("DEBUG: file=" + file);
            System.out.println("DEBUG: startsWith=" + file.startsWith(uploadRoot));

            if (!file.startsWith(uploadRoot)) {
                throw new SecurityException("Invalid path");
            }

            Resource res = new UrlResource(file.toUri());
            if (!res.exists()) {
                throw new FileNotFoundException(path);
            }

            return res;
        } catch (Exception e) {
            throw new RuntimeException("Could not load file", e);
        }
    }


    public void deleteFile(String relativePath) {
        if (relativePath == null)
            return;

        Path target = uploadRoot.resolve(relativePath).normalize();

        try {
            if (Files.isDirectory(target)) {
                Files.walk(target)
                        .sorted((a, b) -> b.compareTo(a))
                        .forEach(p -> {
                            try {
                                Files.deleteIfExists(p);
                            } catch (IOException ignored) {
                            }
                        });
            } else {
                Files.deleteIfExists(target);
            }
        } catch (IOException ignored) {
        }
    }

    private String getExtension(String name) {
        if (name == null || !name.contains("."))
            return ".mp4";
        return name.substring(name.lastIndexOf("."));
    }
}
