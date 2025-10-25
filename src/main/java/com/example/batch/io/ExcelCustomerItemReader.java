package com.example.batch.io;

import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemStreamException;
import org.springframework.batch.item.support.AbstractItemStreamItemReader;
import org.springframework.batch.item.file.ResourceAwareItemReaderItemStream;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Iterator;

public class ExcelCustomerItemReader extends AbstractItemStreamItemReader<CustomerRecord>
        implements ResourceAwareItemReaderItemStream<CustomerRecord> {

    private Resource resource;
    private Workbook workbook;
    private Iterator<Row> rowIterator;
    private final DataFormatter dataFormatter = new DataFormatter();
    private final DateTimeFormatter[] dateFormats = new DateTimeFormatter[] {
            DateTimeFormatter.ISO_LOCAL_DATE,
            DateTimeFormatter.ofPattern("dd/MM/yyyy"),
            DateTimeFormatter.ofPattern("MM/dd/yyyy")
    };

    public ExcelCustomerItemReader() {
        setName("excelCustomerItemReader");
    }

    @Override
    public void setResource(Resource resource) {
        this.resource = resource;
    }

    @Override
    public void open(ExecutionContext executionContext) throws ItemStreamException {
        closeWorkbook();
        if (resource == null) {
            throw new ItemStreamException("No resource specified for ExcelCustomerItemReader");
        }
        try (InputStream is = resource.getInputStream()) {
            this.workbook = WorkbookFactory.create(is);
            Sheet sheet = workbook.getSheetAt(0);
            this.rowIterator = sheet.iterator();
            skipHeader();
        } catch (IOException ex) {
            throw new ItemStreamException("Unable to open Excel file: " + resource.getFilename(), ex);
        }
    }

    private void skipHeader() {
        if (rowIterator != null && rowIterator.hasNext()) {
            rowIterator.next();
        }
    }

    @Override
    public CustomerRecord read() throws Exception {
        if (rowIterator == null) {
            return null;
        }
        while (rowIterator.hasNext()) {
            Row row = rowIterator.next();
            if (row == null || row.getPhysicalNumberOfCells() == 0) {
                continue;
            }
            String externalId = readStringCell(row, 0);
            if (externalId == null || externalId.isBlank()) {
                continue;
            }
            String firstName = readStringCell(row, 1);
            String lastName = readStringCell(row, 2);
            String email = readStringCell(row, 3);
            LocalDate registrationDate = readDateCell(row, 4);
            return new CustomerRecord(externalId, firstName, lastName, email, registrationDate);
        }
        return null;
    }

    private String readStringCell(Row row, int index) {
        if (row.getCell(index) == null) {
            return null;
        }
        return dataFormatter.formatCellValue(row.getCell(index)).trim();
    }

    private LocalDate readDateCell(Row row, int index) {
        if (row.getCell(index) == null) {
            return null;
        }
        switch (row.getCell(index).getCellType()) {
            case NUMERIC -> {
                if (DateUtil.isCellDateFormatted(row.getCell(index))) {
                    return row.getCell(index).getLocalDateTimeCellValue().toLocalDate();
                }
                String value = dataFormatter.formatCellValue(row.getCell(index)).trim();
                return parseDateFromString(value);
            }
            default -> {
                String value = dataFormatter.formatCellValue(row.getCell(index)).trim();
                return parseDateFromString(value);
            }
        }
    }

    private LocalDate parseDateFromString(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        for (DateTimeFormatter formatter : dateFormats) {
            try {
                return LocalDate.parse(value, formatter);
            } catch (DateTimeParseException ignored) {
                // try next formatter
            }
        }
        throw new IllegalArgumentException("Cannot parse date value '%s' in file '%s'".formatted(value, resource.getFilename()));
    }

    @Override
    public void close() throws ItemStreamException {
        closeWorkbook();
    }

    private void closeWorkbook() {
        if (workbook == null) {
            return;
        }
        try {
            workbook.close();
        } catch (IOException ex) {
            throw new ItemStreamException("Failed to close workbook", ex);
        } finally {
            workbook = null;
            rowIterator = null;
        }
    }
}
