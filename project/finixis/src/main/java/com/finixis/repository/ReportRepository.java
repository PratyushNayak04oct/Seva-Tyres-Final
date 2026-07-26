package com.finixis.repository;

import com.finixis.model.GeneratedFile;
import java.util.List;

public interface ReportRepository {
    List<GeneratedFile> findAll();
    GeneratedFile       save(GeneratedFile file);
}
