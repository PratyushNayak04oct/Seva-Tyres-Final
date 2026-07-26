package com.finixis.model;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Represents a generated report, invoice, or export file produced by the app.
 * Kept in memory for the life of the running session so files can be re-downloaded.
 */
public class GeneratedFile {
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("MMM d, yyyy h:mm a");

    private int id;
    private final String name;
    private final String fileType;   // "Report", "Invoice", "Export"
    private final String format;     // "PDF", "Excel"
    private final LocalDateTime generatedAt;
    private File file;

    public GeneratedFile(int id, String name, String fileType, String format,
                         LocalDateTime generatedAt, File file) {
        this.id = id;
        this.name = name;
        this.fileType = fileType;
        this.format = format;
        this.generatedAt = generatedAt;
        this.file = file;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getName() { return name; }
    public String getFileType() { return fileType; }
    public String getFormat() { return format; }
    public LocalDateTime getGeneratedAt() { return generatedAt; }
    public File getFile() { return file; }
    public void setFile(File file) { this.file = file; }

    public String getTimestampDisplay() { return generatedAt.format(FMT); }
    public boolean isAvailable() { return file != null && file.exists(); }

    @Override
    public String toString() { return name + " (" + format + ")"; }
}
