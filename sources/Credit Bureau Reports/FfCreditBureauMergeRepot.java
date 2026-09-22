package com.temenos.fusion;

import com.temenos.t24.api.complex.eb.servicehook.ServiceData;
import com.temenos.t24.api.hook.system.ServiceLifecycle;
import com.temenos.t24.api.records.company.CompanyRecord;
import com.temenos.t24.api.records.ebffcreditbureaureport.EbFfCreditBureauReportRecord;
import com.temenos.t24.api.records.ebffparameter.EbFfParameterRecord;
import com.temenos.t24.api.records.ebffparameter.ParamDescClass;
import com.temenos.t24.api.system.DataAccess;
import com.temenos.t24.api.system.Session;

import java.io.*;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;

/*-----------------------------------------------------------------------------
* @author Deepakumar S
* Date Created: 19-11-2025
* Attached as : Verification job in BATCH>BNK/FF.CREDIT.BUREAU.REPORT,
* BATCH>MFI/FF.CREDIT.BUREAU.REPORT 
* BATCH>BNK/FF.B.CBR.INTERIM.REPORT
* EB.API>FF.CREDIT.BUREAU.MERGE.REPORT
* PGM.FILE>FF.CREDIT.BUREAU.MERGE.REPORT
* TSA.SERVICE>BNK/FF.CREDIT.BUREAU.REPORT
* TSA.SERVICE>MFI/FF.CREDIT.BUREAU.REPORT
* EB.FF.PARAMETER>FF.CBR.REPORT.PATH        --->For path
* EB.FF.PARAMETER>FF.CBR.REPORT.FILE.NAME   --->For File Name
* 
*
* Description: Merge Credit Bureau temp files and create final report in Report path .cdf &.csv file with header & footer
* used for:  
* End Of Month Submission Report
* Mid Of Month Submission Report
* Correction Of Data Report
* Incremental Submissions Report
* Full Backup Submissions Report
*-----------------------------------------------------------------------------*/
public class FfCreditBureauMergeRepot extends ServiceLifecycle {
    DataAccess da = new DataAccess(this);
    Session ses = new Session(this);
    String todayDate = ses.getCurrentVariable("!TODAY");
    public static final String YYYYMMDD = "yyyyMMdd";
    public static final String DDMMYYYY = "ddMMyyyy";
    private static final DateTimeFormatter FORMA = DateTimeFormatter.ofPattern(YYYYMMDD);
    String nameOfTheSubmissionFile = "HMMFI";
    String submittingMFIID = "";
    String submittingMFIName = "";
    String companyId = "";
    String tmpFolder = "";
    String outputFolder = "";
    String fileName;
    int fileNameCount = 0;
    int cnt = 0;
    String fusion = "FUSION";
    String finMnemonic = "";
    String fromDate = "";
    String toDate = "";
    String cbrRecId = "";
    String datePattern = YYYYMMDD;
    String reserved = "Future";
    String password = "Fusion12345";
    String layoutVersionNumber = "3.3";
    EbFfCreditBureauReportRecord ffCreditBurRec = null;
    String format = "";
    String report = "-REPORT-";
    String fileFrequencyInt = "";
    String fileFrequencyCor = "";
    String fileFrequencyFull = "";
    String fileFrequencyMid = "";
    String fileFrequencyMonthEnd = "";

    @Override
    public void processSingleThreaded(ServiceData serviceData) {
        try {
            initialiseCompanyInfo(serviceData);
            getInterimVersion();
            String paramId = "FF.CBR.REPORT.PATH";
            EbFfParameterRecord paramRec = new EbFfParameterRecord(da.getRecord("EB.FF.PARAMETER", paramId));
            for (ParamDescClass paramDesc : paramRec.getParamDesc()) {
                if ("Reports Temp Path".equals(paramDesc.getParamName().getValue())) {
                    tmpFolder = paramDesc.getParamValue().getValue();
                }
                if ("Final Report Path".equals(paramDesc.getParamName().getValue())) {
                    outputFolder = paramDesc.getParamValue().getValue();
                }
            }
            String paramId1 = "FF.CBR.REPORT.FILE.NAME";
            EbFfParameterRecord paramRec1 = new EbFfParameterRecord(da.getRecord("EB.FF.PARAMETER", paramId1));
            companyId = serviceData.getCompanyId();
            for (ParamDescClass paramDesc : paramRec1.getParamDesc()) {
                fileName = paramDesc.getParamDesc().getValue();
                fileNameCount = paramRec1.getParamDesc().size();
                cnt++;

                if ("CIBIL".equals(fileName)) {
                    submittingMFIID = "NB6885";
                    submittingMFIName = fusion;
                } else if ("CRIF".equals(fileName)) {
                    submittingMFIID = "MFI0000044";
                    submittingMFIName = fusion;
                } else if ("EQUIFAX".equals(fileName)) {
                    submittingMFIID = "007FZ00025";
                    submittingMFIName = "FUSION FINANCE LIMITED";
                } else if ("EXPERIAN".equals(fileName)) {
                    submittingMFIID = "MFIFUSIO10";
                    submittingMFIName = fusion;
                }
                getFileGeneration(submittingMFIName, tmpFolder, outputFolder, cnt, fileName);
            }

        } catch (Exception e) {
            e.getMessage();
        }
    }

    private void getInterimVersion() {
        try {
            cbrRecId = todayDate + "-INTERIM";
            ffCreditBurRec = new EbFfCreditBureauReportRecord(
                    da.getRecord(finMnemonic, "EB.FF.CREDIT.BUREAU.REPORT", "", cbrRecId));
            fromDate = ffCreditBurRec.getFromDate().getValue();
            toDate = ffCreditBurRec.getToDate().getValue();
        } catch (Exception e) {
            e.getMessage();
        }
    }

    private void getFileGeneration(String submittingMFIName, String tmpFolder, String outputFolder, int cnt,
            String fileName) {
        try {
            finalFileGenerationEndOfMonth(submittingMFIName, tmpFolder, outputFolder, cnt, fileName);
        } catch (Exception e) {
            e.getMessage();
        }
        try {
            finalFileGenerationFullReport(submittingMFIName, tmpFolder, outputFolder, cnt, fileName);
        } catch (Exception e1) {
            e1.getMessage();
        }
        try {
            finalFileGenerationMidReport(submittingMFIName, tmpFolder, outputFolder, cnt, fileName);
        } catch (Exception e2) {
            e2.getMessage();
        }
        try {
            finalFileGenerationIncremental(submittingMFIName, tmpFolder, outputFolder, fromDate, toDate, cnt, fileName);
        } catch (Exception e3) {
            e3.getMessage();
        }
        try {
            finalFileGenerationLoanCorrection(submittingMFIName, tmpFolder, outputFolder, cnt, fileName);
        } catch (Exception e4) {
            e4.getMessage();
        }
    }

    private void finalFileGenerationMidReport(String submittingMFIName, String tmpFolder, String outputFolder, int cnt,
            String fileName) {
        try {
            File tmpDir = new File(tmpFolder);
            if (!tmpDir.exists()) {
                return;
            }
            File outDir = new File(outputFolder);
            if (!outDir.exists()) {
                outDir.mkdirs();
            }
            File[] inputFiles = null;
            inputFiles = tmpDir.listFiles(
                    (dir, name) -> name != null && name.contains("temp_CBRMidReportExtract_" + finMnemonic + "_"));
            if (inputFiles == null || inputFiles.length == 0) {
                return;
            }
            fileFrequencyMid = "ME";
            String outputPrefix5 = "CBR-MID-" + fileName + report + finMnemonic + "_" + todayDate;
            File outputFile = new File(outputFolder + File.separator + outputPrefix5 + ".cdf");
            File outputFile1 = new File(outputFolder + File.separator + outputPrefix5 + ".csv");
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile));
                    BufferedWriter writer1 = new BufferedWriter(new FileWriter(outputFile1))) {
                writer.write(
                        formingTheHeaderPartMidOfMonth(nameOfTheSubmissionFile, submittingMFIID, submittingMFIName));
                writer.newLine();
                writer1.write(
                        formingTheHeaderPartMidOfMonth(nameOfTheSubmissionFile, submittingMFIID, submittingMFIName));
                writer1.newLine();
                for (File file : inputFiles) {
                    try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            writer.write(line);
                            writer.newLine();
                            writer1.write(line);
                            writer1.newLine();
                        }
                    }
                    if (fileNameCount == cnt) {
                        Files.delete(file.toPath());
                    }
                }
                writer.write(formingTheFooterPart(submittingMFIID));
                writer.newLine();
                writer1.write(formingTheFooterPart(submittingMFIID));
                writer1.newLine();
            }

        } catch (Exception e) {
            e.getMessage();
        }
    }

    private void finalFileGenerationFullReport(String submittingMFIName, String tmpFolder, String outputFolder, int cnt,
            String fileName) {
        try {
            File tmpDir = new File(tmpFolder);
            if (!tmpDir.exists()) {
                return;
            }
            File outDir = new File(outputFolder);
            if (!outDir.exists()) {
                outDir.mkdirs();
            }
            File[] inputFiles = null;
            inputFiles = tmpDir.listFiles((dir, name) -> name != null
                    && name.contains("temp_CBRFullBackupReportExtract_" + finMnemonic + "_"));

            if (inputFiles == null || inputFiles.length == 0) {
                return;
            }
            fileFrequencyFull = "AH";
            String outputPrefix4 = "CBR-FULL-" + fileName + report + finMnemonic + "_" + todayDate;
            File outputFile = new File(outputFolder + File.separator + outputPrefix4 + ".cdf");
            File outputFile1 = new File(outputFolder + File.separator + outputPrefix4 + ".csv");
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile));
                    BufferedWriter writer1 = new BufferedWriter(new FileWriter(outputFile1))) {
                writer.write(
                        formingTheHeaderPartFullBackup(nameOfTheSubmissionFile, submittingMFIID, submittingMFIName));
                writer.newLine();
                writer1.write(
                        formingTheHeaderPartFullBackup(nameOfTheSubmissionFile, submittingMFIID, submittingMFIName));
                writer1.newLine();
                for (File file : inputFiles) {
                    try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            writer.write(line);
                            writer.newLine();
                            writer1.write(line);
                            writer1.newLine();
                        }
                    }
                    if (fileNameCount == cnt) {
                        Files.delete(file.toPath());
                    }
                }
                writer.write(formingTheFooterPart(submittingMFIID));
                writer.newLine();
                writer1.write(formingTheFooterPart(submittingMFIID));
                writer1.newLine();
            }

        } catch (Exception e) {
            e.getMessage();
        }
    }

    private void finalFileGenerationLoanCorrection(String submittingMFIName, String tmpFolder, String outputFolder,
            int cnt, String fileName) {
        try {
            File tmpDir = new File(tmpFolder);
            if (!tmpDir.exists()) {
                return;
            }
            File outDir = new File(outputFolder);
            if (!outDir.exists()) {
                outDir.mkdirs();
            }
            File[] inputFiles3 = null;
            inputFiles3 = tmpDir.listFiles(
                    (dir, name) -> name != null && name.contains("temp_CBRLoanCorrectionReport_" + finMnemonic + "_"));
            if (inputFiles3 == null || inputFiles3.length == 0) {
                return;
            }
            fileFrequencyCor = "DC";
            String outputPrefix3 = "CBR-LOAN-CORRECTION-" + fileName + report + finMnemonic + "_" + todayDate;
            File outputFile = new File(outputFolder + File.separator + outputPrefix3 + ".cdf");
            File outputFile1 = new File(outputFolder + File.separator + outputPrefix3 + ".csv");
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile));
                    BufferedWriter writer1 = new BufferedWriter(new FileWriter(outputFile1))) {
                writer.write(
                        formingTheHeaderPartCorrection(nameOfTheSubmissionFile, submittingMFIID, submittingMFIName));
                writer.newLine();
                writer1.write(
                        formingTheHeaderPartCorrection(nameOfTheSubmissionFile, submittingMFIID, submittingMFIName));
                writer1.newLine();
                for (File file : inputFiles3) {
                    try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            writer.write(line);
                            writer.newLine();
                            writer1.write(line);
                            writer1.newLine();
                        }
                    }
                    if (fileNameCount == cnt) {
                        Files.delete(file.toPath());
                    }
                }
                writer.write(formingTheFooterPart(submittingMFIID));
                writer.newLine();
                writer1.write(formingTheFooterPart(submittingMFIID));
                writer1.newLine();
            }
        } catch (Exception e) {
            e.getMessage();
        }
    }

    private void initialiseCompanyInfo(ServiceData serviceData) {
        try {
            companyId = serviceData.getCompanyId();
            CompanyRecord companyObj = new CompanyRecord(da.getRecord("COMPANY", companyId));
            finMnemonic = companyObj.getFinancialMne().getValue();
        } catch (Exception e) {
            e.getMessage();
        }
    }

    private void finalFileGenerationEndOfMonth(String submittingMFIName, String tmpFolder, String outputFolder, int cnt,
            String fileName) {
        try {
            File tmpDir = new File(tmpFolder);
            if (!tmpDir.exists()) {
                return;
            }
            File outDir = new File(outputFolder);
            if (!outDir.exists()) {
                outDir.mkdirs();
            }
            File[] inputFiles = null;
            inputFiles = tmpDir.listFiles(
                    (dir, name) -> name != null && name.contains("temp_CBRReportExtract_" + finMnemonic + "_"));
            if (inputFiles == null || inputFiles.length == 0) {
                return;
            }
            fileFrequencyMonthEnd = "ME";
            File outputFile = new File(
                    outputFolder + File.separator + fileName + "_" + finMnemonic + "_" + todayDate + ".cdf");
            File outputFile1 = new File(
                    outputFolder + File.separator + fileName + "_" + finMnemonic + "_" + todayDate + ".csv");
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile));
                    BufferedWriter writer1 = new BufferedWriter(new FileWriter(outputFile1))) {
                writer.write(formingTheHeaderPart(nameOfTheSubmissionFile, submittingMFIID, submittingMFIName));
                writer.newLine();
                writer1.write(formingTheHeaderPart(nameOfTheSubmissionFile, submittingMFIID, submittingMFIName));
                writer1.newLine();
                for (File file : inputFiles) {
                    try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            writer.write(line);
                            writer.newLine();
                            writer1.write(line);
                            writer1.newLine();
                        }
                    }
                    if (fileNameCount == cnt) {
                        Files.delete(file.toPath());
                    }
                }
                writer.write(formingTheFooterPart(submittingMFIID));
                writer.newLine();
                writer1.write(formingTheFooterPart(submittingMFIID));
                writer1.newLine();
            }
        } catch (Exception e) {
            e.getMessage();
        }
    }

    private void finalFileGenerationIncremental(String submittingMFIName, String tmpFolder, String outputFolder,
            String fromDate, String toDate, int cnt, String fileName) {
        try {
            File tmpDir = new File(tmpFolder);
            if (!tmpDir.exists()) {
                return;
            }
            File outDir = new File(outputFolder);
            if (!outDir.exists()) {
                outDir.mkdirs();
            }
            File[] inputFiles2 = null;
            inputFiles2 = tmpDir.listFiles((dir, name) -> name != null
                    && name.contains("temp_CBRIncrementalSubmissionsReportExtract_" + finMnemonic + "_"));
            if (inputFiles2 == null || inputFiles2.length == 0) {
                return;
            }
            fileFrequencyInt = "DL";
            String outputPrefix1 = "CBR-INTERIM-" + fileName + report + finMnemonic + "_" + todayDate;
            File outputFile = new File(outputFolder + File.separator + outputPrefix1 + ".cdf");
            File outputFile1 = new File(outputFolder + File.separator + outputPrefix1 + ".csv");
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile));
                    BufferedWriter writer1 = new BufferedWriter(new FileWriter(outputFile1))) {
                writer.write(formingTheHeaderPartWithDates(nameOfTheSubmissionFile, submittingMFIID, submittingMFIName,
                        fromDate, toDate));
                writer.newLine();
                writer1.write(formingTheHeaderPartWithDates(nameOfTheSubmissionFile, submittingMFIID, submittingMFIName,
                        fromDate, toDate));
                writer1.newLine();
                for (File file : inputFiles2) {
                    try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            writer.write(line);
                            writer.newLine();
                            writer1.write(line);
                            writer1.newLine();
                        }
                    }
                    if (fileNameCount == cnt) {
                        Files.delete(file.toPath());
                    }
                }
                writer.write(formingTheFooterPart(submittingMFIID));
                writer.newLine();
                writer1.write(formingTheFooterPart(submittingMFIID));
                writer1.newLine();
            }
        } catch (Exception e) {
            e.getMessage();
        }
    }

    private String formingTheHeaderPart(String nameOfTheSubmissionFile, String submittingMFIID,
            String submittingMFIName) {
        try {
            String reportedDate = determineReportedDate(todayDate);
            String formattedReportedDate = formatDateAsDdMMyyyy(reportedDate);
            String formattedFileCreationDate = formatDateAsDdMMyyyy(todayDate);
            String[] headers = { "HDR", nameOfTheSubmissionFile, layoutVersionNumber, submittingMFIID,
                    submittingMFIName, "", formattedReportedDate, formattedFileCreationDate, "", password, "",
                    fileFrequencyMonthEnd, reserved };
            int[] lengths = { 3, 5, 3, 10, 30, 30, 8, 8, 3, 30, 30, 10, 20 };
            StringBuilder headerLine = new StringBuilder();
            for (int i = 0; i < headers.length; i++) {
                headerLine.append(padRight(headers[i], lengths[i]));
            }
            return headerLine.toString();
        } catch (Exception e) {
            e.getMessage();
            return "";
        }
    }

    private String formingTheHeaderPartCorrection(String nameOfTheSubmissionFile, String submittingMFIID,
            String submittingMFIName) {
        try {
            String reportedDate = determineReportedDate(todayDate);
            String formattedReportedDate = formatDateAsDdMMyyyy(reportedDate);
            String formattedFileCreationDate = formatDateAsDdMMyyyy(todayDate);
            String[] headers = { "HDR", nameOfTheSubmissionFile, layoutVersionNumber, submittingMFIID,
                    submittingMFIName, "", formattedReportedDate, formattedFileCreationDate, "", password, "",
                    fileFrequencyCor, reserved };
            int[] lengths = { 3, 5, 3, 10, 30, 30, 8, 8, 3, 30, 30, 10, 20 };
            StringBuilder headerLine = new StringBuilder();
            for (int i = 0; i < headers.length; i++) {
                headerLine.append(padRight(headers[i], lengths[i]));
            }
            return headerLine.toString();
        } catch (Exception e) {
            e.getMessage();
            return "";
        }
    }

    private String formingTheHeaderPartFullBackup(String nameOfTheSubmissionFile, String submittingMFIID,
            String submittingMFIName) {
        try {
            String formattedReportedDate1 = determineReportedDateFull(todayDate);
            String formattedReportedDate = formatDateAsDdMMyyyy(formattedReportedDate1);

            String formattedFileCreationDate = formatDateAsDdMMyyyy(todayDate);
            String[] headers = { "HDR", nameOfTheSubmissionFile, layoutVersionNumber, submittingMFIID,
                    submittingMFIName, "", formattedReportedDate, formattedFileCreationDate, "", password, "",
                    fileFrequencyFull, reserved };
            int[] lengths = { 3, 5, 3, 10, 30, 30, 8, 8, 3, 30, 30, 10, 20 };

            StringBuilder headerLine = new StringBuilder();
            for (int i = 0; i < headers.length; i++) {
                headerLine.append(padRight(headers[i], lengths[i]));
            }
            return headerLine.toString();

        } catch (Exception e) {
            e.getMessage();
            return "";
        }
    }

    private String formingTheHeaderPartMidOfMonth(String nameOfTheSubmissionFile, String submittingMFIID,
            String submittingMFIName) {
        try {
            String formattedReportedDate1 = determineReportedDateFull(todayDate);
            String formattedReportedDate = formatDateAsDdMMyyyy(formattedReportedDate1);

            String formattedFileCreationDate = formatDateAsDdMMyyyy(todayDate);
            String[] headers = { "HDR", nameOfTheSubmissionFile, layoutVersionNumber, submittingMFIID,
                    submittingMFIName, "", formattedReportedDate, formattedFileCreationDate, "", password, "",
                    fileFrequencyMid, reserved };
            int[] lengths = { 3, 5, 3, 10, 30, 30, 8, 8, 3, 30, 30, 10, 20 };

            StringBuilder headerLine = new StringBuilder();
            for (int i = 0; i < headers.length; i++) {
                headerLine.append(padRight(headers[i], lengths[i]));
            }
            return headerLine.toString();

        } catch (Exception e) {
            e.getMessage();
            return "";
        }
    }

    private String determineReportedDate(String dateFromRecord) {
        try {
            DateTimeFormatter inputFormatter = dateFromRecord.matches("\\d{8}")
                    ? DateTimeFormatter.ofPattern(datePattern)
                    : DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH);

            LocalDate date = LocalDate.parse(dateFromRecord, inputFormatter);
            int day = date.getDayOfMonth();
            int lastDay = date.lengthOfMonth();

            if (day > lastDay - 4) {
                return date.format(DateTimeFormatter.ofPattern(datePattern));
            } else {
                LocalDate prevMonthEnd = date.minusMonths(1).withDayOfMonth(date.minusMonths(1).lengthOfMonth());
                return prevMonthEnd.format(DateTimeFormatter.ofPattern(datePattern));
            }

        } catch (DateTimeParseException e) {
            e.getMessage();
            return "";
        }
    }

    private String formingTheHeaderPartWithDates(String nameOfTheSubmissionFile, String submittingMFIID,
            String submittingMFIName, String fromDate, String toDate) {
        try {
            String formattedFromDate = formatDateAsDdMMyyyy(fromDate);
            String formattedToDate = formatDateAsDdMMyyyy(toDate);
            String[] headers = { "HDR", nameOfTheSubmissionFile, layoutVersionNumber, submittingMFIID,
                    submittingMFIName, "", formattedFromDate, formattedToDate, "", password, "", fileFrequencyInt,
                    reserved };
            int[] lengths = { 3, 5, 3, 10, 30, 30, 8, 8, 3, 30, 30, 10, 20 };
            StringBuilder headerLine = new StringBuilder();
            for (int i = 0; i < headers.length; i++) {
                headerLine.append(padRight(headers[i], lengths[i]));
            }
            return headerLine.toString();
        } catch (Exception e) {
            e.getMessage();
            return "";
        }
    }

    private String formingTheFooterPart(String submittingMFIID) {
        try {
            String[] footer = { "TRL", layoutVersionNumber, submittingMFIID, reserved };
            int[] lengths = { 3, 3, 10, 20 };
            StringBuilder footerLine = new StringBuilder();
            for (int i = 0; i < footer.length; i++) {
                footerLine.append(padRight(footer[i], lengths[i]));
            }
            return footerLine.toString();
        } catch (Exception e) {
            e.getMessage();
            return "";
        }
    }

    private String formatDateAsDdMMyyyy(String date) {
        try {
            return LocalDate.parse(date, DateTimeFormatter.ofPattern(datePattern))
                    .format(DateTimeFormatter.ofPattern(DDMMYYYY));
        } catch (Exception e) {
            e.getMessage();
            return "";
        }
    }

    public static String determineReportedDateFull(String todayDate) {
        try {
            return LocalDate.parse(todayDate, FORMA).minusDays(1).format(FORMA);
        } catch (Exception e) {
            e.getMessage();
            return todayDate;
        }
    }

    private String padRight(String value, int length) {
        if (value == null)
            value = "";
        format = String.format("%-" + length + "s", value);
        return format;
    }
}
