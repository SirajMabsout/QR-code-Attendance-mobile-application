package Capstone.QR.service;


import Capstone.QR.model.Attendance;
import Capstone.QR.model.Klass;
import Capstone.QR.repository.AttendanceRepository;
import Capstone.QR.repository.KlassRepository;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AttendanceExportService {

    private final AttendanceRepository attendanceRepository;
    private final KlassRepository klassRepository;

    public ByteArrayInputStream exportAttendanceSheet(Long classId) throws IOException {
        Klass klass = klassRepository.findById(classId)
                .orElseThrow(() -> new RuntimeException("Class not found"));

        List<Attendance> attendanceList = attendanceRepository.findBySession_Klass_Id(klass.getId());

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Attendance");

            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("Student Name");
            header.createCell(1).setCellValue("Email");
            header.createCell(2).setCellValue("Status");
            header.createCell(3).setCellValue("Date");

            int rowNum = 1;
            for (Attendance att : attendanceList) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(att.getStudent().getName());
                row.createCell(1).setCellValue(att.getStudent().getEmail());
                row.createCell(2).setCellValue(att.getStatus().name());
                row.createCell(3).setCellValue(att.getRecordedAt().toString());
            }

            workbook.write(out);
            return new ByteArrayInputStream(out.toByteArray());
        }
    }
}
