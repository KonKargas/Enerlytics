
package com.kkargas.enerlytics.etl;

import java.nio.file.Path;

public record EtlResult(
        long totalRows,
        long cleanedRows,
        long rejectedRows,
        Path cleanedFile,
        Path rejectedFile
) {

    public boolean isConsistent() {
        return totalRows == cleanedRows + rejectedRows;
    }
}
