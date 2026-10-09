package com.buildflow.tally.controller;

import com.buildflow.tally.service.TallyExportService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

@RestController
public class TallyExportController {

    private final TallyExportService tallyExportService;

    public TallyExportController(TallyExportService tallyExportService) {
        this.tallyExportService = tallyExportService;
    }

    @GetMapping("/api/tally/export/sales-vouchers")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROJECT_MANAGER')")
    public ResponseEntity<String> exportSalesVouchers(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        String xml = tallyExportService.exportSalesVouchersXml(from, to);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/xml; charset=UTF-8"))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename("tally-sales-vouchers.xml", StandardCharsets.UTF_8).build().toString())
                .body(xml);
    }
}
