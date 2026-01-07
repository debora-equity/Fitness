package com.softgenia.playlist.service;

import jakarta.annotation.PostConstruct;
import net.bramp.ffmpeg.FFmpeg;
import net.bramp.ffmpeg.FFmpegExecutor;
import net.bramp.ffmpeg.FFprobe;
import net.bramp.ffmpeg.builder.FFmpegBuilder;
import net.bramp.ffmpeg.probe.FFmpegProbeResult;
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class FileStorageService {

    @Value("${upload.path}")
    private String uploadPath;

    private Path uploadRoot;

    @PostConstruct
    public void init() {
        this.uploadRoot = Paths.get(uploadPath).toAbsolutePath().normalize();
        try {
            Files.createDirectories(uploadRoot);
        } catch (IOException e) {
            throw new RuntimeException("Could not create upload directory: " + uploadRoot, e);
        }
    }

    @Value("${ffprobe.path}")
    private String ffprobePath;

    @Value("${ffmpeg.path}")
    private String ffmpegPath;

    public String saveFile(MultipartFile file) throws IOException {
        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String uniqueFilename = UUID.randomUUID() + extension;

        Path destinationPath = Paths.get(uploadPath).resolve(uniqueFilename).normalize();
        Files.createDirectories(destinationPath.getParent());
        Files.copy(file.getInputStream(), destinationPath);

        String contentType = file.getContentType();
        if (contentType != null && contentType.startsWith("video")) {
            optimizeVideoForStreaming(destinationPath.toString());
        }

        return "/uploads/" + uniqueFilename;
    }

    public Resource loadAsResource(String filename) {
        try {

            if (filename.startsWith("/")) {
                filename = filename.substring(1);
            }


            if (filename.startsWith("uploads/")) {
                filename = filename.substring("uploads/".length());
            }

            String decodedFilename = URLDecoder.decode(filename, StandardCharsets.UTF_8);

            if (decodedFilename.contains("..")) {
                throw new SecurityException("Path traversal attempt: " + decodedFilename);
            }

            Path filePath = uploadRoot.resolve(decodedFilename).normalize().toAbsolutePath();

            if (!filePath.startsWith(uploadRoot)) {
                throw new SecurityException("Invalid file path: " + filePath);
            }

            Resource resource = new UrlResource(filePath.toUri());

            if (!resource.exists() || !resource.isReadable()) {
                throw new FileNotFoundException("File not found: " + decodedFilename);
            }

            return resource;

        } catch (Exception ex) {
            throw new SecurityException("Invalid file path", ex);
        }
    }

    private void optimizeVideoForStreaming(String inputPath) {
        Path tempPath = null;
        try {
            tempPath = Paths.get(inputPath.replace(".", "_optimized."));

            ProcessBuilder pb = new ProcessBuilder(
                    ffmpegPath,
                    "-i", inputPath,
                    "-c:v", "libx264",
                    "-profile:v", "baseline",
                    "-level", "3.0",
                    "-pix_fmt", "yuv420p",
                    "-preset", "veryfast",
                    "-crf", "28",
                    "-vf", "scale=-2:min(720\\,ih)",
                    "-c:a", "aac",
                    "-ac", "2",
                    "-b:a", "128k",
                    "-movflags", "+faststart",
                    tempPath.toString());

            pb.redirectErrorStream(true);

            Process process = pb.start();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                }
            }

            int exitCode = process.waitFor();

            if (exitCode == 0 && Files.exists(tempPath) && Files.size(tempPath) > 0) {

                Files.move(tempPath, Paths.get(inputPath), StandardCopyOption.REPLACE_EXISTING);
            } else {
                Files.deleteIfExists(tempPath);
            }

        } catch (Exception e) {
            e.printStackTrace();

            if (tempPath != null) {
                try {
                    Files.deleteIfExists(tempPath);
                } catch (IOException ignored) {
                }
            }
        }
    }

    public int getVideoDurationInSeconds(String videoPath) throws IOException {
        String fullVideoPath = Paths.get(uploadPath).resolve(Paths.get(videoPath).getFileName()).toString();
        FFprobe ffprobe = new FFprobe(ffprobePath);
        FFmpegProbeResult probeResult = ffprobe.probe(fullVideoPath);
        double durationInSeconds = probeResult.getFormat().duration;
        return (int) Math.round(durationInSeconds);
    }

    public String generateThumbnailFromVideo(String videoPath) throws IOException {
        String fullVideoPath = Paths.get(uploadPath)
                .resolve(Paths.get(videoPath).getFileName())
                .toString();

        String thumbnailFilename = UUID.randomUUID() + ".jpg";
        String fullThumbnailPath = Paths.get(uploadPath)
                .resolve(thumbnailFilename)
                .toString();

        FFmpeg ffmpeg = new FFmpeg(ffmpegPath);

        FFmpegBuilder builder = new FFmpegBuilder()
                .setInput(fullVideoPath)
                .addExtraArgs("-ss", "00:00:01")
                .addExtraArgs("-v", "error")
                .overrideOutputFiles(true)
                .addOutput(fullThumbnailPath)
                .setFrames(1)
                .done();

        new FFmpegExecutor(ffmpeg).createJob(builder).run();

        return "/uploads/" + thumbnailFilename;
    }

    public void deleteFile(String fileUrl) {
        if (fileUrl == null || fileUrl.isBlank()) {
            return;
        }
        try {
            Path filePath = Paths.get(uploadPath).resolve(Paths.get(fileUrl).getFileName()).normalize();
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            System.err.println("Failed to delete file: " + fileUrl + " with error: " + e.getMessage());
        }
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
}