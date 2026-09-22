package com.temenos.fusion;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import com.temenos.api.TStructure;
import com.temenos.api.TValidationResponse;
import com.temenos.t24.api.complex.eb.templatehook.TransactionContext;
import com.temenos.t24.api.hook.system.RecordLifecycle;
import com.temenos.t24.api.records.ebfileupload.EbFileUploadRecord;
import com.temenos.t24.api.records.ebfileuploadparam.EbFileUploadParamRecord;
import com.temenos.t24.api.records.ebfileuploadtype.EbFileUploadTypeRecord;
import com.temenos.t24.api.system.DataAccess;
import com.temenos.t24.api.system.Session;

/**
 * TODO: Document me!
 *
 * @author mp116300
 *
 */
public class FfBulkUploadValRout extends RecordLifecycle {

    private static final FusionFileLogger FfBulkUploadValRout = FusionFileLogger.getLogger(FfBulkUploadValRout.class);

    @Override
    public TValidationResponse validateRecord(String application, String currentRecordId, TStructure currentRecord,
            TStructure unauthorisedRecord, TStructure liveRecord, TransactionContext transactionContext) {

        DataAccess da = new DataAccess(this);
        Session ses = new Session(this);
        String finMne = ses.getCompanyRecord().getFinancialMne().getValue();
        FfBulkUploadValRout.info("Financial Mnemonic " + finMne);
        EbFileUploadRecord ebFileUploadRecord = new EbFileUploadRecord(currentRecord);
        String systemFileName = ebFileUploadRecord.getSystemFileName().getValue();
        FfBulkUploadValRout.info("System File Name " + systemFileName);
        String type = ebFileUploadRecord.getUploadType().getValue();
        FfBulkUploadValRout.info("Upload Type " + type);
        try {
            EbFileUploadTypeRecord ebFileUploadTyRecord = new EbFileUploadTypeRecord(
                    da.getRecord("EB.FILE.UPLOAD.TYPE", type));
            FfBulkUploadValRout.info("Eb Upload Type Record " + ebFileUploadTyRecord.toString());
            String uploadDir = ebFileUploadTyRecord.getUploadDir().getValue();
            FfBulkUploadValRout.info("Upload Directory " + uploadDir);
            EbFileUploadParamRecord ebFileUploadParamRecord = new EbFileUploadParamRecord(
                    da.getRecord("EB.FILE.UPLOAD.PARAM", "SYSTEM"));
            FfBulkUploadValRout.info("Eb Param Record " + ebFileUploadParamRecord.toString());
            String rootDir = ebFileUploadParamRecord.getTcUploadPath().getValue();
            FfBulkUploadValRout.info("Root Directory  " + rootDir.toString());
            String dirPath = rootDir + File.separator + uploadDir + File.separator + systemFileName;
            FfBulkUploadValRout.info("Full directory path  " + dirPath);
            if (!systemFileName.equals("")) {
                validateFile(Path.of(dirPath), type, da, ebFileUploadRecord, finMne);

            }

        } catch (Exception e) {
        }

        return ebFileUploadRecord.getValidationResponse();
    }

    public static void validateFile(Path csvPath, String type, DataAccess da, EbFileUploadRecord ebFileUploadRecord,
            String finMne) throws IOException {

        // Step 1: Check for empty file
        try (Stream<String> lines = Files.lines(csvPath)) {

            boolean hasData = lines.anyMatch(line -> !line.trim().isEmpty());

            if (!hasData) {
                ebFileUploadRecord.getFileName().setError("EB-FF.EMPTY.CONTENT");
                return;
            }
        }

    }

}
