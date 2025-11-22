
package com.softgenia.playlist.service;

import net.bramp.ffmpeg.FFmpeg;
import net.bramp.ffmpeg.FFmpegExecutor;
import net.bramp.ffmpeg.FFprobe;
import net.bramp.ffmpeg.builder.FFmpegBuilder;
import net.bramp.ffmpeg.probe.FFmpegProbeResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
public class FileStorageService {

    @Value("${upload.path}")
    private String uploadPath;

    @Value("${ffprobe.path}")
    private String ffprobePath;

    @Value("${ffmpeg.path}")
    private String ffmpegPath;

    public String saveFile(MultipartFile file) throws IOException {
        String originalFilename = file.getOriginalFilename();
        String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        String uniqueFilename = UUID.randomUUID().toString() + extension;
        Path destinationPath = Paths.get(uploadPath).resolve(uniqueFilename).normalize();
        Files.createDirectories(destinationPath.getParent());
        Files.copy(file.getInputStream(), destinationPath);
        return "/uploads/" + uniqueFilename;
    }


    public int getVideoDurationInSeconds(String videoPath) throws IOException {

        String fullVideoPath = Paths.get(uploadPath).resolve(Paths.get(videoPath).getFileName()).toString();


        FFprobe ffprobe = new FFprobe(ffprobePath);

        FFmpegProbeResult probeResult = ffprobe.probe(fullVideoPath);


        double durationInSeconds = probeResult.getFormat().duration;

        return (int) Math.round(durationInSeconds);
    }

    public String generateThumbnailFromVideo(String videoPath) throws IOException {

        String fullVideoPath = Paths.get(uploadPath).resolve(Paths.get(videoPath).getFileName()).toString();


        String thumbnailFilename = UUID.randomUUID().toString() + ".jpg";
        String fullThumbnailPath = Paths.get(uploadPath).resolve(thumbnailFilename).toString();


        FFmpeg ffmpeg = new FFmpeg(ffmpegPath);


        FFmpegBuilder builder = new FFmpegBuilder()
                .setInput(fullVideoPath)
                .overrideOutputFiles(true)
                .addOutput(fullThumbnailPath)
                .setFormat("image2")
                .setFrames(1)
                .setVideoFrameRate(1)
                .done();

        FFmpegExecutor executor = new FFmpegExecutor(ffmpeg);
        executor.createJob(builder).run();


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

    public void optimizePdf(String relativeFilePath) {
        try {
            Path inputPath = Paths.get(uploadPath).resolve(relativeFilePath).normalize();
            String inputStr = inputPath.toString();
            // Create a temporary filename for the output
            String outputStr = inputStr.replace(".pdf", "_optimized.pdf");

            // --- GHOSTSCRIPT COMMAND ---
            // -dPDFSETTINGS=/ebook : Medium quality/size (good for reading)
            // -dFastWebView=true   : Enables streaming (Linearization) - CRITICAL FOR SPEED
            ProcessBuilder pb = new ProcessBuilder(
                    "gs",
                    "-sDEVICE=pdfwrite",
                    "-dCompatibilityLevel=1.4",
                    "-dPDFSETTINGS=/ebook",
                    "-dFastWebView=true",
                    "-dNOPAUSE",
                    "-dQUIET",
                    "-dBATCH",
                    "-sOutputFile=" + outputStr,
                    inputStr
            );

            Process process = pb.start();
            int exitCode = process.waitFor();

            if (exitCode == 0) {
                // Optimization successful.
                // Delete original and rename optimized to original name
                java.io.File original = new java.io.File(inputStr);
                java.io.File optimized = new java.io.File(outputStr);

                if (original.delete()) {
                    optimized.renameTo(original);
                }
            } else {
                System.err.println("Ghostscript failed to optimize PDF");
            }

        } catch (Exception e) {
            e.printStackTrace();
            // If optimization fails, we just keep the original file, so don't throw exception
        }
    }
}