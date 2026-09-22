package com.bct.fusion.bulk.upload;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import com.temenos.api.TStructure;
import com.temenos.api.TValidationResponse;
import com.temenos.t24.api.complex.eb.templatehook.TransactionContext;
import com.temenos.t24.api.hook.system.RecordLifecycle;
import com.temenos.t24.api.records.aaarrangement.AaArrangementRecord;
import com.temenos.t24.api.records.ebfileupload.EbFileUploadRecord;
import com.temenos.t24.api.records.ebfileuploadparam.EbFileUploadParamRecord;
import com.temenos.t24.api.records.ebfileuploadtype.EbFileUploadTypeRecord;
import com.temenos.t24.api.system.DataAccess;
import com.temenos.t24.api.system.Session;

/*---------------------------------------------------------------------------------
* * Product          :
* * Developed by     : 
* * Routine Type     : 
* * Date             :  
* * Description      :  
* * Attached To      : 
* * EB.API Record ID :
* * In Parameters    : 
* * Out Parameters   :
* * Reference        :
* *--------------------------------------------------------------------------------
* * Revision History :
* *-----------------
* * Date          - <Developer> -  Description
* *---------------------------------------------------------------------------------
* *--------------------------------------------------------------------------------*/
public class FusionVInpFileValidations extends RecordLifecycle {
    private static final Pattern AA_PATTERN = Pattern.compile("AA\\d{5}[A-Z0-9]{5}");

    @Override
    public TValidationResponse validateRecord(String application, String currentRecordId, TStructure currentRecord,
            TStructure unauthorisedRecord, TStructure liveRecord, TransactionContext transactionContext) {
        DataAccess da = new DataAccess(this);
        String companymnemonic = (new Session(this)).getCompanyRecord().getFinancialMne().getValue();
        EbFileUploadRecord ebFileUploadRecord = new EbFileUploadRecord(currentRecord);

        String systemFileName = ebFileUploadRecord.getSystemFileName().getValue();

        String type = ebFileUploadRecord.getUploadType().getValue();

        try {
            EbFileUploadTypeRecord ebFileUploadTyRecord = new EbFileUploadTypeRecord(
                    da.getRecord("EB.FILE.UPLOAD.TYPE", type));

            String uploadDir = ebFileUploadTyRecord.getUploadDir().getValue();

            EbFileUploadParamRecord ebFileUploadParamRecord = new EbFileUploadParamRecord(
                    da.getRecord("EB.FILE.UPLOAD.PARAM", "SYSTEM"));

            String rootDir = ebFileUploadParamRecord.getTcUploadPath().getValue();

            String dirPath = rootDir + File.separator + uploadDir + File.separator + systemFileName;

            if (!systemFileName.equals("")) {
                validateFile(Path.of(dirPath), type, da, ebFileUploadRecord, companymnemonic);

            }

        } catch (Exception e) {
        }

        return ebFileUploadRecord.getValidationResponse();
    }

    public static void validateFile(Path csvPath, String type, DataAccess da, EbFileUploadRecord ebFileUploadRecord,
            String finMne) throws IOException {

        Set<String> seen = new HashSet<>();
        Set<String> aaIDset = new HashSet<>();
        // First pass: check empty file efficiently
        try (Stream<String> lines = Files.lines(csvPath)) {
            long lineCount = lines.limit(2).count(); // only need first 2 lines
            if (lineCount < 2) {
                ebFileUploadRecord.getFileName().setError("EB-FF.EMPTY.CONTENT");
            }
        }

        // Second pass: fail-fast validation (lineCounter now accurate)
        AtomicLong lineCounter = new AtomicLong(0);

        try (Stream<String> lines = Files.lines(csvPath)) {

            lines.skip(1).map(line -> new AbstractMap.SimpleEntry<>(lineCounter.incrementAndGet(), line)).map(entry -> {
                long lineNumber = entry.getKey();
                String line = entry.getValue();

                // Duplicate check
                if (!seen.add(line)) {
                    List<String> dupError = new ArrayList<>();
                    dupError.add("EB-FF.DUPLICATE.CONTENT");
                    dupError.add(String.valueOf(lineNumber));
                    ebFileUploadRecord.getFileName().setError(dupError.toString());
                    return dupError.toString();
                }
                if (getAAError(ebFileUploadRecord, da, line, lineNumber, finMne, aaIDset)
                        && (!type.equals("RO.CHANGE"))) {
                    return "AA.ERROR";
                }
                // AA check

                return null; // valid line
            }).filter(Objects::nonNull).findFirst();

        }
    }

    /**
     * @param ebFileUploadRecord
     * @param da
     * @param lineNumber
     * @param line
     * @param aaIDset
     * @return
     */
    private static boolean getAAError(EbFileUploadRecord ebFileUploadRecord, DataAccess da, String line,
            long lineNumber, String finMne, Set<String> aaIDset) {
        Matcher matcher = AA_PATTERN.matcher(line);
        if (!matcher.find()) {
            List<String> noAA = new ArrayList<>();
            noAA.add("EB-FF.INVALID.AA");
            noAA.add("");
            noAA.add(String.valueOf(lineNumber));
            ebFileUploadRecord.getFileName().setError(noAA.toString());
            return true;
        }

        String aaId = matcher.group();
        if (!aaIDset.add(aaId)) {
            List<String> dupError = new ArrayList<>();
            dupError.add("EB-FF.AA.DUPLICATE.CONTENT");
            dupError.add(aaId);
            dupError.add(String.valueOf(lineNumber));
            ebFileUploadRecord.getFileName().setError(dupError.toString());
            return true;
        }

        if (inValidAAId(aaId, da, finMne)) {
            List<String> noAA = new ArrayList<>();
            noAA.add("EB-FF.INVALID.AA");
            noAA.add(aaId);
            noAA.add(String.valueOf(lineNumber));
            ebFileUploadRecord.getFileName().setError(noAA.toString());
            return true;
        }
        return false;
    }

    private static boolean inValidAAId(String aaId, DataAccess da, String finMne) {

        try {
            AaArrangementRecord aaarrangementRecord = new AaArrangementRecord(
                    da.getRecord(finMne, "AA.ARRANGEMENT", "", aaId));
            aaarrangementRecord.getActiveBranch();
            return false;
        } catch (Exception e) {
            return true;
        }

    }

}
