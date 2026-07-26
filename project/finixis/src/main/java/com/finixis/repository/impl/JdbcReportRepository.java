package com.finixis.repository.impl;

import com.finixis.db.DatabaseConfig;
import com.finixis.model.GeneratedFile;
import com.finixis.repository.ReportRepository;

import java.io.File;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class JdbcReportRepository implements ReportRepository {

    @Override
    public List<GeneratedFile> findAll() {
        String sql = "SELECT export_id,file_name,file_type,format,file_path,creation_date"
                + " FROM Generated_Report ORDER BY creation_date DESC";
        List<GeneratedFile> list = new ArrayList<>();
        try (Connection con = DatabaseConfig.get();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(map(rs));
        } catch (SQLException e) { throw new RuntimeException(e); }
        return list;
    }

    @Override
    public GeneratedFile save(GeneratedFile gf) {
        String sql = "INSERT INTO Generated_Report(file_name,file_type,format,file_path,creation_date) VALUES(?,?,?,?,?)";
        try (Connection con = DatabaseConfig.get();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, gf.getName());
            ps.setString(2, gf.getFileType());
            ps.setString(3, gf.getFormat());
            ps.setString(4, gf.getFile() != null ? gf.getFile().getAbsolutePath() : null);
            ps.setTimestamp(5, Timestamp.valueOf(gf.getGeneratedAt()));
            ps.executeUpdate();
            try (ResultSet k = ps.getGeneratedKeys()) {
                if (k.next()) gf.setId(k.getInt(1));
            }
        } catch (SQLException e) { throw new RuntimeException(e); }
        return gf;
    }

    private GeneratedFile map(ResultSet rs) throws SQLException {
        int id = rs.getInt("export_id");
        String name = rs.getString("file_name");
        String type = rs.getString("file_type");
        String fmt  = rs.getString("format");
        Timestamp ts = rs.getTimestamp("creation_date");
        LocalDateTime dt = ts != null ? ts.toLocalDateTime() : LocalDateTime.now();
        String path = rs.getString("file_path");
        File file = (path != null && !path.isBlank()) ? new File(path) : null;
        return new GeneratedFile(id, name, type, fmt, dt, file);
    }
}
