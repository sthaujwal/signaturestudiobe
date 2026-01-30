package com.wellsfargo.signaturestudio.service;

import com.wellsfargo.signaturestudio.exception.ErrorCode;
import com.wellsfargo.signaturestudio.exception.ServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/**
 * Service for managing report file storage and cleanup.
 * Handles file storage, retrieval, and automatic cleanup of old files.
 */
@Service
public class ReportFileService {

    private static final Logger logger = LoggerFactory.getLogger(ReportFileService.class);

    @Value("${report.output.dir:./reports}")
    private String reportOutputDir;

    @Value("${report.retention.days:30}")
    private int retentionDays;

    /**
     * Save report data to filesystem.
     *
     * @param csvData CSV file content as byte array
     * @param fileName Original filename
     * @return Full file path where the file was saved
     * @throws IOException if file save fails
     */
    public String saveReportFile(byte[] csvData, String fileName) throws IOException {
        logger.debug("Saving report file: {}", fileName);

        // Ensure output directory exists
        Path outputPath = Paths.get(reportOutputDir);
        if (!Files.exists(outputPath)) {
            Files.createDirectories(outputPath);
            logger.info("Created report output directory: {}", reportOutputDir);
        }

        // Generate unique filename to prevent collisions
        String uniqueFileName = UUID.randomUUID() + "_" + fileName;
        Path filePath = outputPath.resolve(uniqueFileName);

        // Write file
        Files.write(filePath, csvData);
        logger.info("Saved report file: {} ({} bytes)", filePath, csvData.length);

        return filePath.toString();
    }

    /**
     * Get report file as Resource for download.
     *
     * @param filePath Full path to the report file
     * @return Resource for file download
     * @throws ServiceException if file not found or not accessible
     */
    public Resource getReportFile(String filePath) {
        logger.debug("Retrieving report file: {}", filePath);

        File file = new File(filePath);

        if (!file.exists()) {
            logger.warn("Report file not found: {}", filePath);
            throw new ServiceException(ErrorCode.REPORT_FILE_NOT_FOUND,
                "Report file not found: " + filePath);
        }

        if (!file.canRead()) {
            logger.error("Cannot read report file: {}", filePath);
            throw new ServiceException(ErrorCode.REPORT_FILE_NOT_FOUND,
                "Cannot access report file: " + filePath);
        }

        return new FileSystemResource(file);
    }

    /**
     * Delete report file from filesystem.
     *
     * @param filePath Full path to the report file
     * @return true if file was deleted, false if file doesn't exist
     */
    public boolean deleteReportFile(String filePath) {
        logger.debug("Deleting report file: {}", filePath);

        if (filePath == null || filePath.isEmpty()) {
            logger.warn("Cannot delete report file: null or empty path");
            return false;
        }

        File file = new File(filePath);

        if (!file.exists()) {
            logger.debug("Report file already deleted or doesn't exist: {}", filePath);
            return false;
        }

        boolean deleted = file.delete();
        if (deleted) {
            logger.info("Deleted report file: {}", filePath);
        } else {
            logger.error("Failed to delete report file: {}", filePath);
        }

        return deleted;
    }

    /**
     * Get the size of a report file in bytes.
     *
     * @param filePath Full path to the report file
     * @return File size in bytes, or 0 if file doesn't exist
     */
    public long getFileSize(String filePath) {
        if (filePath == null || filePath.isEmpty()) {
            return 0;
        }

        File file = new File(filePath);
        if (!file.exists()) {
            return 0;
        }

        return file.length();
    }

    /**
     * Check if a report file exists.
     *
     * @param filePath Full path to the report file
     * @return true if file exists and is readable
     */
    public boolean fileExists(String filePath) {
        if (filePath == null || filePath.isEmpty()) {
            return false;
        }

        File file = new File(filePath);
        return file.exists() && file.canRead();
    }

    /**
     * Scheduled cleanup of old report files.
     * Runs daily at 3 AM to delete files older than retention period.
     */
    @Scheduled(cron = "0 0 3 * * *")
    public void cleanupOldReportFiles() {
        logger.info("Starting scheduled cleanup of old report files (retention: {} days)", retentionDays);

        try {
            Path outputPath = Paths.get(reportOutputDir);

            if (!Files.exists(outputPath)) {
                logger.debug("Report output directory doesn't exist, skipping cleanup");
                return;
            }

            Instant cutoffTime = Instant.now().minus(retentionDays, ChronoUnit.DAYS);
            logger.debug("Deleting files older than: {}", cutoffTime);

            int deletedCount = 0;
            long deletedSize = 0;

            File directory = outputPath.toFile();
            File[] files = directory.listFiles();

            if (files == null || files.length == 0) {
                logger.debug("No files found in report directory");
                return;
            }

            for (File file : files) {
                if (file.isFile()) {
                    Instant fileModifiedTime = Instant.ofEpochMilli(file.lastModified());

                    if (fileModifiedTime.isBefore(cutoffTime)) {
                        long fileSize = file.length();
                        boolean deleted = file.delete();

                        if (deleted) {
                            deletedCount++;
                            deletedSize += fileSize;
                            logger.debug("Deleted old report file: {} ({} bytes, modified: {})",
                                       file.getName(), fileSize, fileModifiedTime);
                        } else {
                            logger.warn("Failed to delete old report file: {}", file.getName());
                        }
                    }
                }
            }

            logger.info("Cleanup completed: deleted {} files ({} bytes total)",
                       deletedCount, deletedSize);

        } catch (Exception e) {
            logger.error("Error during report file cleanup", e);
        }
    }

    /**
     * Get total size of all report files in bytes.
     *
     * @return Total size in bytes
     */
    public long getTotalReportFilesSize() {
        try {
            Path outputPath = Paths.get(reportOutputDir);

            if (!Files.exists(outputPath)) {
                return 0;
            }

            File directory = outputPath.toFile();
            File[] files = directory.listFiles();

            if (files == null) {
                return 0;
            }

            long totalSize = 0;
            for (File file : files) {
                if (file.isFile()) {
                    totalSize += file.length();
                }
            }

            return totalSize;

        } catch (Exception e) {
            logger.error("Error calculating total report files size", e);
            return 0;
        }
    }

    /**
     * Get count of report files in storage.
     *
     * @return Number of report files
     */
    public int getReportFileCount() {
        try {
            Path outputPath = Paths.get(reportOutputDir);

            if (!Files.exists(outputPath)) {
                return 0;
            }

            File directory = outputPath.toFile();
            File[] files = directory.listFiles();

            if (files == null) {
                return 0;
            }

            int count = 0;
            for (File file : files) {
                if (file.isFile()) {
                    count++;
                }
            }

            return count;

        } catch (Exception e) {
            logger.error("Error counting report files", e);
            return 0;
        }
    }
}
