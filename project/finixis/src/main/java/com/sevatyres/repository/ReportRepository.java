package com.sevatyres.repository;

import com.sevatyres.model.GeneratedFile;
import java.util.List;

public interface ReportRepository {
    List<GeneratedFile> findAll();
    GeneratedFile       save(GeneratedFile file);
}
