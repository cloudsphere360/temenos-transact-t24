package com.temenos.fusion;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

import com.temenos.api.TStructure;
import com.temenos.t24.api.complex.eb.servicehook.ServiceData;
import com.temenos.t24.api.complex.eb.servicehook.TransactionData;
import com.temenos.t24.api.hook.system.ServiceLifecycle;
import com.temenos.t24.api.records.ebfileupload.EbFileUploadRecord;
import com.temenos.t24.api.records.ebfileuploadparam.EbFileUploadParamRecord;
import com.temenos.t24.api.records.ebfileuploadtype.EbFileUploadTypeRecord;
import com.temenos.t24.api.records.ebpettycashlimit.EbPettyCashLimitRecord;
import com.temenos.t24.api.system.DataAccess;
import com.temenos.t24.api.system.Session;

/**
 * TODO: Document me!
 *
 * @author mp116300
 *
 */
public class FfFileUploadPettyCash extends ServiceLifecycle {

    private static final FusionFileLogger FfFileUploadPettyCash = FusionFileLogger
            .getLogger(FfFileUploadPettyCash.class);
    public static final String PETTY_CASH_LIMIT = "PETTY.CASH.LIMIT";
    public static final String PETTY_CASH_LIMIT_UPDATE = "PETTY.CASH.LIMIT.UPDATE";
    public static final String EB_FILE_UPLOAD = "EB.FILE.UPLOAD";
    public static final String STATEMENT1 = "WITH UPLOAD.TYPE EQ ";
    public static final String STATEMENT2 = " AND  WITH L.UPLOAD.STATUS NE PROCESSED";
    public static final String INPUT = "INPUT";
    public static final String ZERO = "0";
    public static final String OFS_SOURCE = "PETTYCASH.UPLOAD";
    public static final String VERSION_PETTY_LIMIT_CHANGE = "EB.PETTY.CASH.LIMIT,OFS";
    private static final String STATUS_LOCALREF = "L.UPLOAD.STATUS";
    List<String> nFileRecords = new ArrayList<>();
    DataAccess da = new DataAccess(this);
    Session session = new Session(this);

    @Override
    public List<String> getIds(ServiceData serviceData, List<String> controlList) {

        FfFileUploadPettyCash.info("Petty Cash Bulk Upload Service Started ");
        if (controlList.isEmpty()) {
            controlList.add(PETTY_CASH_LIMIT);
            controlList.add(PETTY_CASH_LIMIT_UPDATE);
        }
        FfFileUploadPettyCash.info("controlList : "+controlList.toString());

        
        String currentStage = controlList.get(0);
        FfFileUploadPettyCash.info("currentStage "+currentStage);
        currentstage1(currentStage);
        currentstage2(currentStage);
        return nFileRecords;

    }

    @Override
    public void postUpdateRequest(String id, ServiceData serviceData, String controlItem,
            List<TransactionData> transactionData, List<TStructure> records) {
        System.out.println("Posting the data from the csv file to Transact initiated");
        FfFileUploadPettyCash.info("id "+id);
        postRecords(id, controlItem, transactionData, records);
        postFieldUpdate(id, controlItem);

    }

    private void currentstage1(String currentStage) {
        if (currentStage.equals(PETTY_CASH_LIMIT)) {
            FfFileUploadPettyCash.info("currentStage" + currentStage);
            List<String> selectStage1 = da.selectRecords("", EB_FILE_UPLOAD, "", STATEMENT1 + PETTY_CASH_LIMIT + STATEMENT2);
            FfFileUploadPettyCash.info("selectStage1" + selectStage1);

            nFileRecords.addAll(getLines(selectStage1));
        }
    }

    private void currentstage2(String currentStage) {
        if (currentStage.equals(PETTY_CASH_LIMIT_UPDATE)) {
            nFileRecords.clear();
            nFileRecords = da.selectRecords("", EB_FILE_UPLOAD, "", STATEMENT1 + PETTY_CASH_LIMIT + STATEMENT2);
        }

    }

    private List<String> getLines(List<String> selectStage1) {
        List<String> linesFromFile = new ArrayList<>();
        for (String eBfileUpld : selectStage1) {
            try {
                List<String> tempLine = new ArrayList<>();
                EbFileUploadRecord ebFileUploadRecord = new EbFileUploadRecord(
                        da.getRecord(EB_FILE_UPLOAD, eBfileUpld));

                String systemFileName = ebFileUploadRecord.getSystemFileName().getValue();

                String type = ebFileUploadRecord.getUploadType().getValue();

                EbFileUploadTypeRecord ebFileUploadTyRecord = new EbFileUploadTypeRecord(
                        da.getRecord("EB.FILE.UPLOAD.TYPE", type));

                String uploadDir = ebFileUploadTyRecord.getUploadDir().getValue();

                EbFileUploadParamRecord ebFileUploadParamRecord = new EbFileUploadParamRecord(
                        da.getRecord("EB.FILE.UPLOAD.PARAM", "SYSTEM"));

                String rootDir = ebFileUploadParamRecord.getTcUploadPath().getValue();

                String dirPath = rootDir + File.separator + uploadDir + File.separator + systemFileName;
                FfFileUploadPettyCash.info("dirPath" + dirPath);
                try (BufferedReader br = new BufferedReader(new FileReader(dirPath))) {
                    String line;
                    while ((line = br.readLine()) != null) {

                        tempLine.add(line + "&" + eBfileUpld);
                    }
                }
                linesFromFile.addAll(tempLine);
            } catch (Exception e) {
                FfFileUploadPettyCash.info("Exception" + e.getMessage());
            }
        }
        return linesFromFile;
    }

    private void postRecords(String id, String controlItem, List<TransactionData> transactionData,
            List<TStructure> records) {
        FfFileUploadPettyCash.info("postRecords method initiated");
        String[] updIdSplit = id.split("[&]", -1);
        String[] idSplit = updIdSplit[0].split("[,]", -1);
        boolean idLength = idSplit.length > 1;
        FfFileUploadPettyCash.info("idLength" + idLength);
        FfFileUploadPettyCash.info("controlItem: " + controlItem);
        if (controlItem.equals((PETTY_CASH_LIMIT)) && (idLength)) {
            records.add(getPettyCashRecord(idSplit));
            TransactionData txnData = new TransactionData();
            txnData.setCompanyId(session.getCompanyId());
            txnData.setFunction(INPUT);
            txnData.setNumberOfAuthoriser(ZERO);
            txnData.setSourceId(OFS_SOURCE);
            txnData.setVersionId(VERSION_PETTY_LIMIT_CHANGE);
            txnData.setTransactionId(idSplit[0].trim());
            transactionData.add(txnData);

        }
    }
    
    private void postFieldUpdate(String id, String controlItem) {
        FfFileUploadPettyCash.info("UpdateLocalRef");
        List<String> controlItemList = new ArrayList<>();
        controlItemList.add(PETTY_CASH_LIMIT_UPDATE);
        FfFileUploadPettyCash.info("controlItemList" + controlItemList);
        FfFileUploadPettyCash.info("controlItem" + controlItem);
        if (controlItemList.contains(controlItem)) {
            FfFileUploadPettyCash.info("updatedlocRef");
            EbFileUploadRecord ebFileUploadRecord = new EbFileUploadRecord(this);
            ebFileUploadRecord.getLocalRefField(STATUS_LOCALREF).setValue("PROCESSED");
            da.updateLocalfields(EB_FILE_UPLOAD, id, ebFileUploadRecord.toStructure());
        }

    }
    
    private TStructure getPettyCashRecord(String[] idSplit) {

        EbPettyCashLimitRecord ebPettyCashLimitRec = new EbPettyCashLimitRecord(this);
        ebPettyCashLimitRec.setPettyCash(idSplit[1].trim());
        FfFileUploadPettyCash.info("ebPettyCashLimitRec "+ebPettyCashLimitRec.toString());
        return ebPettyCashLimitRec.toStructure();
    }

}
