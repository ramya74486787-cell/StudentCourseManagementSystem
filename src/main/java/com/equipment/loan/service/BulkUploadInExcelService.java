//	public EquipmentResponse Same(MultipartFile file, EquipmentRequest request) {
//
//	    String fileName = StringUtils.cleanPath(file.getOriginalFilename());
//
//	    if (fileName.contains("..")) {
//	        throw new DataNotAcceptableException("File Name Contains invalid character");
//	    }
//
//	    Equipment equipment1 = new Equipment();
//
//	    equipment1.setName(request.getName());
//	    equipment1.setCategory(request.getCategory());
//	    equipment1.setTotalUnits(request.getTotalUnits());
//	    equipment1.setAvailableUnits(request.getTotalUnits());
//	    equipment1.setLocation(request.getLocation());
//	    equipment1.setCondition(request.getCondition());
//
//	    Equipment equipment = equipmentRepository.save(equipment1);
//
//	    EquipmentResponse response = new EquipmentResponse();
//
//	    response.setName(equipment.getName());
//	    response.setCategory(equipment.getCategory());
//	    response.setTotalUnits(equipment.getTotalUnits());
//	    response.setAvailableUnits(equipment.getAvailableUnits());
//	    response.setLocation(equipment.getLocation());
//	    response.setCondition(equipment.getCondition());
//
//	    return response;
//		
//		
//	} 
package com.equipment.loan.service;

import java.util.ArrayList;
import java.util.List;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import com.equipment.loan.entity.Equipment;
import com.equipment.loan.exception.DataNotAcceptableException;
import com.equipment.loan.repository.EquipmentRepository;

//    public String uploadExcel(MultipartFile file) throws Exception {
//
//        String fileName = StringUtils.cleanPath(file.getOriginalFilename());
//
//        if (fileName.contains("..")) {
//            throw new DataNotAcceptableException(
//                    "File Name Contains invalid character"
//            );
//        }
//
//        List<Equipment> equipmentList = new ArrayList<>();
//
//        Workbook workbook = new XSSFWorkbook(file.getInputStream());
//
//        Sheet sheet = workbook.getSheetAt(0);
//
//        for (Row row : sheet) {
//
//            // Skip Excel header row
//            if (row.getRowNum() == 0) {
//                continue;
//            }
//
//            Equipment equipment = new Equipment();
//
//            equipment.setName(
//                    row.getCell(0).getStringCellValue()
//            );
//
//            equipment.setCategory(
//                    row.getCell(1).getStringCellValue()
//            );
//
//            equipment.setTotalUnits(
//                    (int) row.getCell(2).getNumericCellValue()
//            );
//
//            // Initially all units are available
//            equipment.setAvailableUnits(
//                    (int) row.getCell(2).getNumericCellValue()
//            );
//
//            equipment.setLocation(
//                    row.getCell(3).getStringCellValue()
//            );
//
//            equipment.setCondition(
//                    row.getCell(4).getStringCellValue()
//            );
//
//            equipmentList.add(equipment);
//        }
//
//        workbook.close();
//
//        equipmentRepository.saveAll(equipmentList);
//
//        return "✅ " + equipmentList.size()
//                + " equipment records uploaded successfully";
//    }



//@Service
//public class BulkUploadInExcelService {
//
//    private final EquipmentRepository equipmentRepository;
//
//    public BulkUploadInExcelService(EquipmentRepository equipmentRepository) {
//        this.equipmentRepository = equipmentRepository;
//    }
//
//    public String uploadExcel(MultipartFile file) throws Exception {
//
//        String fileName = StringUtils.cleanPath(file.getOriginalFilename());
//
//        if (fileName.contains("..")) {
//            throw new DataNotAcceptableException("File Name Contains invalid character");
//        }
//
//        List<Equipment> equipmentList = new ArrayList<>();
//
//        Workbook workbook = new XSSFWorkbook(file.getInputStream());
//        Sheet sheet = workbook.getSheetAt(0);
//
//        for (Row row : sheet) {
//
//            if (row.getRowNum() == 0) {
//                continue; 
//            }
//
//            Equipment equipment = new Equipment();
//
//            equipment.setName(row.getCell(0).getStringCellValue());
//            equipment.setCategory(row.getCell(1).getStringCellValue());
//            equipment.setTotalUnits((int) row.getCell(2).getNumericCellValue());
//            equipment.setAvailableUnits((int) row.getCell(2).getNumericCellValue());
//            equipment.setLocation(row.getCell(3).getStringCellValue());
//            equipment.setCondition(row.getCell(4).getStringCellValue());
//
//            equipmentList.add(equipment);
//        }
//
//        workbook.close();
//
//        equipmentRepository.saveAll(equipmentList);
//
//        return "✅ " + equipmentList.size() + " equipment records uploaded successfully";
//    }
//}















import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import com.equipment.loan.entity.Equipment;
import com.equipment.loan.exception.DataNotAcceptableException;
import com.equipment.loan.repository.EquipmentRepository;


@Service
public class BulkUploadInExcelService {

    private final EquipmentRepository equipmentRepository;


    public BulkUploadInExcelService(EquipmentRepository equipmentRepository) 
    {
        this.equipmentRepository = equipmentRepository;
    }


    public String uploadExcel(MultipartFile file) throws Exception 
    {

        String fileName = StringUtils.cleanPath(file.getOriginalFilename());

        if (fileName.contains("..")) 
        {
            throw new DataNotAcceptableException("File Name Contains invalid character");
        }


        List<Equipment> equipmentList = new ArrayList<>();

        Workbook workbook = new XSSFWorkbook(file.getInputStream());

        Sheet sheet = workbook.getSheetAt(0);


        for (Row row : sheet) 
        {

            //Skip header
            if (row.getRowNum() == 0) 
            {
                continue;
            }


            Equipment equipment = new Equipment();

            equipment.setName(row.getCell(0).getStringCellValue());

            equipment.setCategory(row.getCell(1).getStringCellValue());

            equipment.setTotalUnits((int) row.getCell(2).getNumericCellValue());

            equipment.setAvailableUnits((int) row.getCell(2).getNumericCellValue());

            equipment.setLocation(row.getCell(3).getStringCellValue());

            equipment.setCondition(row.getCell(4).getStringCellValue());

            equipmentList.add(equipment);
        }


        workbook.close();


        equipmentRepository.saveAll(equipmentList);


        return equipmentList.size() + " equipment records uploaded successfully";
    }



    //download
    public File createExcelReport() throws Exception {


        List<Equipment> equipmentList =
                equipmentRepository.findAll();


        Workbook workbook = new XSSFWorkbook();

        Sheet sheet = workbook.createSheet("Equipment");


        // Header row
        Row header = sheet.createRow(0);

        header.createCell(0).setCellValue("Name");
        header.createCell(1).setCellValue("Category");
        header.createCell(2).setCellValue("Total Units");
        header.createCell(3).setCellValue("Available Units");
        header.createCell(4).setCellValue("Location");
        header.createCell(5).setCellValue("Condition");

        int rowNumber = 1;


        for (Equipment equipment : equipmentList) {

            Row row = sheet.createRow(rowNumber++);


            row.createCell(0)
                    .setCellValue(equipment.getName());

            row.createCell(1)
                    .setCellValue(equipment.getCategory());

            row.createCell(2)
                    .setCellValue(equipment.getTotalUnits());

            row.createCell(3)
                    .setCellValue(equipment.getAvailableUnits());

            row.createCell(4)
                    .setCellValue(equipment.getLocation());

            row.createCell(5)
                    .setCellValue(equipment.getCondition());
        }



        File file = new File("equipment-report.xlsx");


        FileOutputStream outputStream = new FileOutputStream(file);


        workbook.write(outputStream);


        outputStream.close();

        workbook.close();


        return file;
    }
}