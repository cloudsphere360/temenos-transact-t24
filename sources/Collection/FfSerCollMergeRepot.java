package com.temenos.fusion;
/*-----------------------------------------------------------------------------
* @author Vinothini P
* Date Created:19-11-2025
* Attached as : Verfication job in BATCH>MFI/FF.DAILY.REPORT.EXTRACT, BATCH>BNK/FF.DAILY.REPORT.EXTRACT
* EB.API : 
* Attached to :
* Description: 
*------------------------------------------------------------------------------ 
* Modification History :
*----------------------------------------------------------------------------- 
*18-Nov-2025   Development      Initial Version
*-----------------------------------------------------------------------------
*/

import com.temenos.t24.api.complex.eb.servicehook.ServiceData;
import com.temenos.t24.api.hook.system.ServiceLifecycle;
import com.temenos.t24.api.records.ebffparameter.EbFfParameterRecord;
import com.temenos.t24.api.records.ebffparameter.ParamDescClass;
import com.temenos.t24.api.system.DataAccess;

import java.io.*;
import java.nio.file.Files;
import java.util.*;

public class FfSerCollMergeRepot extends ServiceLifecycle {
    private static final FusionFileLogger FfSerMergeRepotlog = FusionFileLogger.getLogger(FfSerCollMergeRepot.class);

    @Override
    public void processSingleThreaded(ServiceData serviceData) {
        DataAccess da = new DataAccess(this);
        String tmpFolder = "";
        String outputFolder = "";
        String paramId = "FF.COB.REPORT.EXTRACT";
        EbFfParameterRecord paramRec = new EbFfParameterRecord(da.getRecord("EB.FF.PARAMETER", paramId));
        for (ParamDescClass paramDesc : paramRec.getParamDesc()) {
            if (paramDesc.getParamName().getValue().equals("Reports Temp Path")) {
                tmpFolder = paramDesc.getParamValue().getValue() + "inprogress";
            }
            if (paramDesc.getParamName().getValue().equals("Final Report Path")) {
                outputFolder = paramDesc.getParamValue().getValue();
            }
        }
        FfSerMergeRepotlog.info("TMP_FOLDER " + tmpFolder);
        FfSerMergeRepotlog.info("OUTPUT_FOLDER " + outputFolder);
        mergeCsvFiles(tmpFolder, outputFolder);

    }

    private void mergeCsvFiles(String tmpFolder, String outputFolder) {
        try {
            File tmpDir = new File(tmpFolder);
            if (!tmpDir.exists()) {
                FfSerMergeRepotlog.error("TMP folder not found!");
                return;
            }

            File outDir = new File(outputFolder);
            if (!outDir.exists())
                outDir.mkdirs();

            File[] csvFiles = tmpDir.listFiles((dir, name) -> name.endsWith(".csv"));

            if (csvFiles == null || csvFiles.length == 0) {
                FfSerMergeRepotlog.info("No CSV files found.");
                return;
            }

            // Split 1: group files by ReportName_Company_Date
            Map<String, List<File>> groupedFiles = groupCsvFiles(csvFiles);

            // Split 2: merge each group
            mergeGroupedFiles(groupedFiles, outputFolder);

        } catch (Exception e) {
            FfSerMergeRepotlog.error("Error merging CSV files: " + e.getMessage(), e);
        }
    }

    private Map<String, List<File>> groupCsvFiles(File[] csvFiles) {
        Map<String, List<File>> groupedFiles = new HashMap<>();

        for (File f : csvFiles) {
            String name = f.getName();
            String[] parts = name.split("_");

            if (parts.length < 5) {
                FfSerMergeRepotlog.warn("Invalid file name format: " + name);
                continue;
            }

            String reportName = parts[0];
            String company = parts[1];
            String date = parts[2];

            String groupKey = reportName + "_" + company + "_" + date;
            groupedFiles.computeIfAbsent(groupKey, k -> new ArrayList<>()).add(f);
        }

        return groupedFiles;
    }

    private void mergeGroupedFiles(Map<String, List<File>> groupedFiles, String outputFolder) {
        for (Map.Entry<String, List<File>> entry : groupedFiles.entrySet()) {
            String groupKey = entry.getKey();
            List<File> filesToMerge = entry.getValue();
            File mergedFile = new File(outputFolder + groupKey + ".csv");

            FfSerMergeRepotlog.info("Creating merged file: " + mergedFile.getAbsolutePath());

            try (BufferedWriter writer = new BufferedWriter(new FileWriter(mergedFile))) {
                mergeFilesIntoWriter(filesToMerge, writer);
            } catch (IOException e) {
                FfSerMergeRepotlog.error("Error merging files for group " + groupKey + ": " + e.getMessage(), e);
            }

            FfSerMergeRepotlog.info("Merged file created: " + mergedFile.getAbsolutePath());
        }
    }

    /** Helper method to merge CSV files into the given writer */
    private void mergeFilesIntoWriter(List<File> filesToMerge, BufferedWriter writer) throws IOException {
        boolean firstFile = true;

        for (File csv : filesToMerge) {
            FfSerMergeRepotlog.info("Merging: " + csv.getName());

            try (BufferedReader reader = new BufferedReader(new FileReader(csv))) {
                writeCsvContent(reader, writer, firstFile);
                firstFile = false; // after first file header is handled
            }

            Files.delete(csv.toPath());
        }
    }

    /** Helper method to write CSV content, skipping header if needed */
    private void writeCsvContent(BufferedReader reader, BufferedWriter writer, boolean writeHeader) throws IOException {
        String line;
        boolean isHeader = true;

        while ((line = reader.readLine()) != null) {
            if (isHeader) {
                if (writeHeader) {
                    writer.write(line);
                    writer.newLine();
                }
                isHeader = false;
                continue;
            }
            writer.write(line);
            writer.newLine();
        }
    }
}