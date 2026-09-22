package com.temenos.fusion;

import com.temenos.t24.api.complex.eb.servicehook.ServiceData;
import com.temenos.t24.api.hook.system.ServiceLifecycle;
import com.temenos.t24.api.records.company.CompanyRecord;
import com.temenos.t24.api.records.ebffparameter.EbFfParameterRecord;
import com.temenos.t24.api.records.ebffparameter.ParamDescClass;
import com.temenos.t24.api.system.DataAccess;
import com.temenos.t24.api.system.Session;

import java.io.*;
import java.nio.file.Files;

public class FfServiceMergeForCobReports extends ServiceLifecycle {

    @Override
    public void processSingleThreaded(ServiceData serviceData) {
        String currentReportName = "";
        String todayDate = "";
        String finMnemonic = "";

        DataAccess da = new DataAccess(this);
        Session session = new Session(this);
        todayDate = session.getCurrentVariable("!TODAY");

        String tempfolderPath = "";
        String outfolderPath = "";
        String paramId = "FF.COB.REPORT.EXTRACT";
        EbFfParameterRecord paramRec = new EbFfParameterRecord(da.getRecord("EB.FF.PARAMETER", paramId));
        for (ParamDescClass paramDesc : paramRec.getParamDesc()) {
            if (paramDesc.getParamName().getValue().equals("Reports Temp Path")) {
                tempfolderPath = paramDesc.getParamValue().getValue();
            }
            if (paramDesc.getParamName().getValue().equals("Final Report Path")) {
                outfolderPath = paramDesc.getParamValue().getValue();
            }
        }

        CompanyRecord companyRec = new CompanyRecord(da.getRecord("COMPANY", serviceData.getCompanyId()));
        finMnemonic = companyRec.getFinancialMne().getValue();

        if (!serviceData.getJobData().isEmpty()) {
            for (String jobData : serviceData.getJobData()) {
                if (jobData != null && !jobData.isEmpty()) {
                    currentReportName = jobData;
                    String reportName = currentReportName + "_" + finMnemonic + "_" + todayDate;

                    mergeCsvFiles(reportName, tempfolderPath, outfolderPath);
                }
            }
        }
    }

    private void mergeCsvFiles(String reportName, String tempfolderPath, String outfolderPath) {
        try {
            File tmpDir = new File(tempfolderPath);
            if (!tmpDir.exists()) {
                return;
            }

            File outDir = new File(outfolderPath);
            if (!outDir.exists())
                outDir.mkdirs();

            File[] csvFiles = tmpDir.listFiles((dir, name) -> name.startsWith(reportName) && name.endsWith(".csv"));
            if (csvFiles == null || csvFiles.length == 0) {
                return;
            }

            File mergedFile = new File(outfolderPath, reportName + ".csv");

            writeMergedCsv(csvFiles, mergedFile);

        } catch (Exception e) {
            e.getMessage();
        }
    }

    private void writeMergedCsv(File[] csvFiles, File mergedFile) throws IOException {
        boolean firstFile = true;
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(mergedFile))) {
            for (File csv : csvFiles) {
                try (BufferedReader reader = new BufferedReader(new FileReader(csv))) {
                    String line;
                    boolean isHeader = true;
                    while ((line = reader.readLine()) != null) {
                        if (isHeader) {
                            if (firstFile) {
                                writer.write(line);
                                writer.newLine();
                                firstFile = false;
                            }
                            isHeader = false;
                            continue;
                        }
                        writer.write(line);
                        writer.newLine();
                    }
                }
                Files.delete(csv.toPath());
            }
        }
    }
}
