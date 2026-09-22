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
import com.temenos.t24.api.records.aaarrangement.AaArrangementRecord;
import com.temenos.t24.api.records.aaarrangement.LinkedApplClass;
import com.temenos.t24.api.records.aaarrangementactivity.AaArrangementActivityRecord;
import com.temenos.t24.api.records.aaarrangementactivity.FieldNameClass;
import com.temenos.t24.api.records.aaarrangementactivity.PropertyClass;
import com.temenos.t24.api.records.account.AccountRecord;
import com.temenos.t24.api.records.company.CompanyRecord;
import com.temenos.t24.api.records.ebfileupload.EbFileUploadRecord;
import com.temenos.t24.api.records.ebfileuploadparam.EbFileUploadParamRecord;
import com.temenos.t24.api.records.ebfileuploadtype.EbFileUploadTypeRecord;
import com.temenos.t24.api.system.DataAccess;
import com.temenos.t24.api.system.Session;

public class FusionFile extends ServiceLifecycle {

    private static final FusionFileLogger Fusion_File = FusionFileLogger.getLogger(FusionFile.class);

    public static final String OFS_SOURCE = "BULK.UPLOAD";
    public static final String OFS_SOURCE_AA = "FF.BULK.OFS";
    public static final String ZERO = "0";
    public static final String EMPTY = "";

    public static final String FILE_STATUS_PENDING = "PENDING";
    public static final String FILE_STATUS_IN_PROGRESS = "INPROGRESS";
    public static final String FILE_STATUS_PROCESSED = "PROCESSED";

    public static final String LOAN_ALLOC_DEALLOC = "LOAN.ALLOC.DEALLOC";
    public static final String LOAN_ALLOC_DEALLOC_UPDATE = "LOAN.ALLOC.DEALLOC.UPDATE";

    public static final String APPLICATION_ACCOUNT = "ACCOUNT";
    public static final String INPUT = "INPUT";

    public static final String VERSION = "EB.FF.LOAN.DETAILS,";
    public static final String VERSION_AAA = "AA.ARRANGEMENT.ACTIVITY,";

    public static final String EB_FF_CENTRE_DETAIL = "EB.FF.CENTRE.DETAIL";
    public static final String EB_FF_LOAN_DETAILS = "EB.FF.LOAN.DETAILS";
    public static final String EB_FF_LOAN_ACTIVITY = "EB.FF.LOAN.ACTIVITY";
    public static final String EB_FILE_UPLOAD = "EB.FILE.UPLOAD";
    public static final String EB_FILE_UPLOAD_TYPE = "EB.FILE.UPLOAD.TYPE";
    public static final String EB_FILE_UPLOAD_PARAM = "EB.FILE.UPLOAD.PARAM";

    private static final String STATUS_LOCALREF = "L.UPLOAD.STATUS";
    private static final String FF_FUNDER_NAME = "FF.FUNDER.NAME";

    public static final String DOUBLE_COLON_DELIMITER = "::";
    public static final String COMMA_DELIMITER = ",";
    public static final String COMMA_REGEX = "\\,";

    public static final String STATEMENT1 = "WITH UPLOAD.TYPE EQ ";
    public static final String STATEMENT2 = " AND  WITH L.UPLOAD.STATUS EQ PENDING";

    List<String> nFileRecords = new ArrayList<>();

    DataAccess da = new DataAccess(this);
    Session session = new Session(this);

    @Override
    public List<String> getIds(ServiceData serviceData, List<String> controlList) {

        if (controlList.isEmpty()) {

            controlList.add(LOAN_ALLOC_DEALLOC);
            controlList.add(LOAN_ALLOC_DEALLOC_UPDATE);
        }

        String currentStage = controlList.get(0);

        currentStage1(currentStage);

        currentStage2(currentStage);

        return nFileRecords;
    }

    private void currentStage2(String currentStage) {

        try {

            if (currentStage.equals(LOAN_ALLOC_DEALLOC_UPDATE)) {

                nFileRecords.clear();

                nFileRecords = da.selectRecords("", EB_FILE_UPLOAD, "", STATEMENT1 + LOAN_ALLOC_DEALLOC + STATEMENT2);

            }

        } catch (Exception e) {
            e.getMessage();
        }
    }

    private void currentStage1(String currentStage) {
        try {

            if (currentStage.equals(LOAN_ALLOC_DEALLOC)) {
                List<String> selectStage1 = da.selectRecords("", EB_FILE_UPLOAD, "",
                        STATEMENT1 + LOAN_ALLOC_DEALLOC + STATEMENT2);
                nFileRecords.addAll(getLines(selectStage1));
            }

        } catch (Exception e) {

            Fusion_File.error("Error in currentStage1", e);
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
                        da.getRecord(EB_FILE_UPLOAD_TYPE, type));

                String uploadDir = ebFileUploadTyRecord.getUploadDir().getValue();

                EbFileUploadParamRecord ebFileUploadParamRecord = new EbFileUploadParamRecord(
                        da.getRecord(EB_FILE_UPLOAD_PARAM, "SYSTEM"));

                String rootDir = ebFileUploadParamRecord.getTcUploadPath().getValue();

                String dirPath = rootDir + File.separator + uploadDir + File.separator + systemFileName;

                try (BufferedReader br = new BufferedReader(new FileReader(dirPath))) {

                    String line;

                    while ((line = br.readLine()) != null) {

                        tempLine.add(line + "&" + eBfileUpld);
                    }
                }

                if (!tempLine.isEmpty()) {

                    tempLine.remove(0);

                }

                linesFromFile.addAll(tempLine);

            } catch (Exception e) {
                e.getMessage();
            }
        }

        return linesFromFile;
    }

    @Override
    public void postUpdateRequest(String id, ServiceData serviceData, String controlItem,
            List<TransactionData> transactionData, List<TStructure> records) {
        postRecords(id, controlItem, transactionData, records);

        postFieldUpdate(id, controlItem);
    }

    private void postFieldUpdate(String id, String controlItem) {
        List<String> controlItemList = new ArrayList<>();

        controlItemList.add(LOAN_ALLOC_DEALLOC_UPDATE);

        if (controlItemList.contains(controlItem)) {

            EbFileUploadRecord ebFileUploadRecord = new EbFileUploadRecord(this);

            ebFileUploadRecord.getLocalRefField(STATUS_LOCALREF).setValue(FILE_STATUS_PROCESSED);

            da.updateLocalfields(EB_FILE_UPLOAD, id, ebFileUploadRecord.toStructure());

        }
    }

    private void postRecords(String id, String controlItem, List<TransactionData> transactionData,
            List<TStructure> records) {

        try {

            String[] updIdSplit = id.split("[&]", -1);

            String[] idSplit = updIdSplit[0].split("[,]", -1);
            boolean idLength = idSplit.length > 1;

            if (!idLength) {
                return;
            }

            if (controlItem.equals(LOAN_ALLOC_DEALLOC)) {

                records.add(getAaCentreRecord(idSplit));
                String companyId = skipnull(getAccFromArrangementandFinComp(
                        getAccFromArrangementandFinComp(idSplit[0].trim(), session.getCompanyId(), false), "", true),
                        session.getCompanyId());

                TransactionData ofsData = createTransactionData(companyId, INPUT, ZERO, OFS_SOURCE_AA,
                        VERSION_AAA + updIdSplit[1], "");

                transactionData.add(ofsData);
            }

        } catch (Exception e) {

            e.getMessage();
        }
    }

    private String skipnull(String company, String companyfromSession) {

        if (company.equals("")) {

            return companyfromSession;
        }

        return company;
    }

    private TransactionData createTransactionData(String companyId, String function, String numberOfAuthoriser,
            String sourceId, String versionId, String transactionId) {

        TransactionData txnData = new TransactionData();

        txnData.setCompanyId(companyId);

        txnData.setFunction(function);

        txnData.setNumberOfAuthoriser(numberOfAuthoriser);

        txnData.setSourceId(sourceId);

        txnData.setVersionId(versionId);

        if (!transactionId.equals("")) {

            txnData.setTransactionId(transactionId);
        }

        return txnData;
    }

    private TStructure getAaCentreRecord(String[] idSplit) {

        AaArrangementActivityRecord aaArrangementActivityRecord = new AaArrangementActivityRecord(this);

        aaArrangementActivityRecord.setArrangement(idSplit[0].trim());

        aaArrangementActivityRecord.setActivity("LENDING-UPDATE-ACCOUNT");

        PropertyClass propertyClass = new PropertyClass();

        FieldNameClass fieldNameClass = new FieldNameClass();

        fieldNameClass.setFieldName(FF_FUNDER_NAME);

        fieldNameClass.setFieldValue(idSplit[1].trim());

        propertyClass.addFieldName(fieldNameClass);

        FieldNameClass fieldNameClas = new FieldNameClass();

        fieldNameClas.setFieldName("FF.ALLOC.MODE");

        fieldNameClas.setFieldValue(idSplit[2].trim());

        propertyClass.addFieldName(fieldNameClas);
        //
        FieldNameClass fieldNameClas1 = new FieldNameClass();

        fieldNameClas1.setFieldName("FF.FN.TRANCHE");

        fieldNameClas1.setFieldValue(idSplit[3].trim());

        propertyClass.addFieldName(fieldNameClas1);
        //
        FieldNameClass fieldNameClas2 = new FieldNameClass();      
        fieldNameClas2.setFieldName("FF.EFFECTIVE.DA");
        Fusion_File.info("fieldNameClas2 DA"+fieldNameClas2);
        fieldNameClas2.setFieldValue(idSplit[4].trim());
        Fusion_File.info("idSplit"+idSplit);
        propertyClass.addFieldName(fieldNameClas2);
        Fusion_File.info("setting sucessfully1");
        //

        propertyClass.setProperty("ACCOUNT");
        Fusion_File.info("adding property");
        aaArrangementActivityRecord.addProperty(propertyClass);
        Fusion_File.info("property added");
        return aaArrangementActivityRecord.toStructure();
    }

    private String getAccFromArrangementandFinComp(String accNum, String company, boolean companyFlag) {

        try {

            if (companyFlag) {

                AccountRecord accountRecord = new AccountRecord(da.getRecord(APPLICATION_ACCOUNT, accNum));

                return accountRecord.getCoCode();
            }

            if (accNum.startsWith("AA")) {

                CompanyRecord companyRecord = new CompanyRecord(da.getRecord("COMPANY", company));

                AaArrangementRecord aaarrangementRecord = new AaArrangementRecord(
                        da.getRecord(companyRecord.getFinancialMne().getValue(), "AA.ARRANGEMENT", "", accNum));

                for (LinkedApplClass linkedApplClass : aaarrangementRecord.getLinkedAppl()) {

                    if (linkedApplClass.getLinkedAppl().getValue().equals(APPLICATION_ACCOUNT)) {

                        return linkedApplClass.getLinkedApplId().getValue();
                    }
                }
            }

        } catch (Exception e) {

            e.getMessage();
        }

        return accNum;
    }
}