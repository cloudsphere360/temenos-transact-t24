package com.bct.fusion.bulk.upload;

import com.temenos.t24.api.complex.eb.messagehook.MessageContext;
import com.temenos.t24.api.hook.system.MessageLifecycle;
import com.temenos.t24.api.records.ebfileupload.EbFileUploadRecord;
import com.temenos.t24.api.records.ebfileuploadparam.EbFileUploadParamRecord;
import com.temenos.t24.api.records.ofsrequestdetail.OfsRequestDetailRecord;
import com.temenos.t24.api.system.DataAccess;
import com.temenos.t24.api.system.Date;
import com.temenos.tafj.api.client.impl.T24Context;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FusionOfsPostWaifoff extends MessageLifecycle {

    private static final Logger logg = Logger.getLogger(FusionOfsPostWaifoff.class.getName());

    private static final String AA_ARR_ACTIVITY = "AA.ARRANGEMENT.ACTIVITY";
    private static final String CSV_HEADER = "Application,Status,Transaction Reference,Date Time,Messages";
    private static final Pattern AA_PATTERN = Pattern.compile("AA\\d{5}[A-Z0-9]{5}");

    private String tempPath;
    private DataAccess da = new DataAccess((T24Context) this);

    @Override
    public void postProcess(OfsRequestDetailRecord requestDetailRecord, MessageContext messageContext) {

        logg.info("PostProcess Triggered");
        String msgOut = requestDetailRecord.getMsgOut().getValue();
        String msgIn = requestDetailRecord.getMsgIn().getValue();

        logg.info("msgOut : " + msgOut);
        logg.info("msgIn  : " + msgIn);

        processBulkUpload(requestDetailRecord, msgOut, msgIn);
    }

    private void processBulkUpload(OfsRequestDetailRecord requestDetailRecord, String msgOut, String msgIn) {

        if (!AA_ARR_ACTIVITY.equals(requestDetailRecord.getApplication().getValue())) {
            return;
        }
        if (requestDetailRecord.getVersion().getError().equals("")) {
            return;
        }

        try {
            EbFileUploadParamRecord paramRec = new EbFileUploadParamRecord(
                    da.getRecord("EB.FILE.UPLOAD.PARAM", "SYSTEM"));

            String rootDir = paramRec.getTcUploadPath().getValue();
            tempPath = rootDir + File.separator + "ERROR";

            File dir = new File(tempPath);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            Date date = new Date(this);
            String today = date.getDates().getToday().getValue();

            String errorMessage = getErrorMessage(msgOut);

            String upldId = requestDetailRecord.getVersion().getValue();
            String dateTime = "";

            if (!upldId.equals("")) {
                EbFileUploadRecord ebFileRec = new EbFileUploadRecord(da.getRecord("EB.FILE.UPLOAD", upldId));
                dateTime = ebFileRec.getDateTime(0);
            }

            if (!errorMessage.isEmpty()) {
                writeError(requestDetailRecord, errorMessage, today, dateTime);
            } else {
                writeSuccess(requestDetailRecord, today, msgIn, dateTime);
            }

        } catch (Exception e) {
            logg.severe("Processing failed: " + e.getMessage());
        }
    }

    private String getErrorMessage(String msgOut) {
        try {
            String[] slashSplit = msgOut.split("/");
            if (slashSplit.length > 2 && "-1".equals(slashSplit[2])) {
                String[] commaSplit = msgOut.split(",");
                if (commaSplit.length > 1) {
                    return commaSplit[1];
                }
            }
        } catch (Exception e) {
            logg.warning("Error parsing msgOut: " + e.getMessage());
        }
        return "";
    }

    private void writeError(OfsRequestDetailRecord rec, String errorMessage, String today, String dateTime) {

        String fileName = rec.getVersion().getValue() + "_" + today + ".csv";

        String filePath = tempPath + File.separator + fileName;

        StringBuilder line = new StringBuilder();
        line.append(rec.getApplication().getValue()).append(",");
        line.append("ERROR").append(",");
        line.append("").append(",");
        line.append(dateTime).append(",");
        line.append(errorMessage);

        writeFile(filePath, line.toString());
    }

    private void writeSuccess(OfsRequestDetailRecord rec, String today, String msgIn, String dateTime) {

        String fileName = rec.getVersion().getValue() + "_" + today + ".csv";

        String filePath = tempPath + File.separator + fileName;
        String aaId = "";

        Matcher matcher = AA_PATTERN.matcher(msgIn);
        if (matcher.find()) {
            aaId = matcher.group();
            StringBuilder line = new StringBuilder();
            line.append(rec.getApplication().getValue()).append(",");
            line.append("SUCCESS").append(",");
            line.append(rec.getTransReference().getValue()).append(",");
            line.append(dateTime).append(",");
            line.append(aaId);

            writeFile(filePath, line.toString());
        }

        
    }

    private void writeFile(String filePath, String content) {

        logg.info("Writing file : " + filePath);

        File file = new File(filePath);

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file, true))) {

            if (!file.exists() || file.length() == 0) {
                writer.write(CSV_HEADER);
                writer.newLine();
            }

            writer.write(content);
            writer.newLine();

            logg.info("File write successful");

        } catch (IOException e) {
            logg.severe("File write failed: " + e.getMessage());
        }
    }
}