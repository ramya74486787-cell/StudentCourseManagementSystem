package com.equipment.loan.controller;

import java.io.File;
import java.io.FileInputStream;

import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.equipment.loan.service.BulkUploadInExcelService;

@RestController
@RequestMapping("/excel")
public class ExcelFileController {

    private final BulkUploadInExcelService bulkUploadExcelService;

    public ExcelFileController(BulkUploadInExcelService bulkUploadExcelService) {
        this.bulkUploadExcelService = bulkUploadExcelService;
    }

    @PostMapping("/upload")
    public String upload(@RequestParam("file") MultipartFile file) throws Exception 
    {

        return bulkUploadExcelService.uploadExcel(file);
    }

    @GetMapping("/download")
    public ResponseEntity<Resource> downloadExcel() throws Exception 
    {

        File file = bulkUploadExcelService.createExcelReport();

        InputStreamResource resource = new InputStreamResource(new FileInputStream(file));

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=equipment-report.xlsx")
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(resource);
    }
}