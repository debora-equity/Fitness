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
import java.util.ArrayList;
import java.util.List;
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

    private int[] getVideoResolution(Path video) throws IOException {
        ProcessBuilder pb = new ProcessBuilder(
                ffprobePath,
                "-v", "error",
                "-select_streams", "v:0",
                "-show_entries", "stream=width,height",
                "-of", "csv=p=0",
                video.toString()
        );

        Process p = pb.start();

        try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
            String line = r.readLine();
            if (line == null || !line.contains(",")) {
                throw new IOException("Could not detect video resolution");
            }
            String[] parts = line.split(",");
            return new int[] {
                    Integer.parseInt(parts[0]),
                    Integer.parseInt(parts[1])
            };
        }
    }

    private void transcodeToHls(Path input, Path outputDir) {
        try {
            Files.createDirectories(outputDir);

            int[] res = getVideoResolution(input);
            int height = res[1];

            List<String> filterParts = new ArrayList<>();
            List<String> maps = new ArrayList<>();
            List<String> varStreamMap = new ArrayList<>();

            int index = 0;

            filterParts.add("[0:v]scale=640:-2:flags=lanczos[v360]");
            maps.addAll(List.of(
                    "-map", "[v360]", "-map", "0:a?",
                    "-c:v:" + index, "libx264",
                    "-b:v:" + index, "800k",
                    "-pix_fmt", "yuv420p",
                    "-profile:v", "main",
                    "-c:a:" + index, "aac",
                    "-b:a:" + index, "96k"
            ));
            varStreamMap.add("v:" + index + ",a:" + index);
            index++;

            if (height >= 720) {
                filterParts.add("[0:v]scale=1280:-2:flags=lanczos[v720]");
                maps.addAll(List.of(
                        "-map", "[v720]", "-map", "0:a?",
                        "-c:v:" + index, "libx264",
                        "-b:v:" + index, "2500k",
                        "-pix_fmt", "yuv420p",
                        "-profile:v", "main",
                        "-c:a:" + index, "aac",
                        "-b:a:" + index, "128k"
                ));
                varStreamMap.add("v:" + index + ",a:" + index);
                index++;
            }

            if (height >= 1080) {
                filterParts.add("[0:v]scale=1920:-2:flags=lanczos[v1080]");
                maps.addAll(List.of(
                        "-map", "[v1080]", "-map", "0:a?",
                        "-c:v:" + index, "libx264",
                        "-b:v:" + index, "5000k",
                        "-pix_fmt", "yuv420p",
                        "-profile:v", "main",
                        "-c:a:" + index, "aac",
                        "-b:a:" + index, "160k"
                ));
                varStreamMap.add("v:" + index + ",a:" + index);
                index++;
            }

            if (height >= 1440) {
                filterParts.add("[0:v]scale=2560:-2:flags=lanczos[v1440]");
                maps.addAll(List.of(
                        "-map", "[v1440]", "-map", "0:a?",
                        "-c:v:" + index, "libx264",
                        "-b:v:" + index, "8000k",
                        "-pix_fmt", "yuv420p",
                        "-profile:v", "main",
                        "-c:a:" + index, "aac",
                        "-b:a:" + index, "192k"
                ));
                varStreamMap.add("v:" + index + ",a:" + index);
            }

            List<String> command = new ArrayList<>();
            command.add(ffmpegPath);
            command.add("-y");
            command.add("-i");
            command.add(input.toString());
            command.add("-filter_complex");
            command.add(String.join(";", filterParts));
            command.addAll(maps);
            command.addAll(List.of(
                    "-f", "hls",
                    "-hls_time", "6",
                    "-hls_playlist_type", "vod",
                    "-hls_flags", "independent_segments",
                    "-master_pl_name", "master.m3u8",
                    "-var_stream_map", String.join(" ", varStreamMap),
                    outputDir.resolve("stream_%v.m3u8").toString()
            ));
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);

            Process p = pb.start();
            try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                r.lines().forEach(System.out::println);
            }

            if (p.waitFor() != 0) {
                throw new RuntimeException("HLS transcoding failed");
            }

        } catch (Exception e) {
            throw new RuntimeException("FFmpeg HLS error", e);
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
