package com.temenos.fusion;

import java.util.List;
import com.temenos.logging.facade.Logger;
import com.temenos.logging.facade.LoggerFactory;
import com.temenos.api.LocalRefList;
import com.temenos.api.TField;
import com.temenos.api.TStructure;
import com.temenos.t24.api.arrangement.Product;
import com.temenos.t24.api.arrangement.PropertyClass;
import com.temenos.t24.api.arrangement.accounting.Contract;
import com.temenos.t24.api.complex.eb.servicehook.TransactionData;
import com.temenos.t24.api.complex.eb.templatehook.TransactionContext;
import com.temenos.t24.api.hook.system.RecordLifecycle;
import com.temenos.t24.api.records.aaarrangement.AaArrangementRecord;
import com.temenos.t24.api.records.aaarrangementactivity.AaArrangementActivityRecord;
import com.temenos.t24.api.records.aaarrangementactivity.FieldNameClass;
import com.temenos.t24.api.records.aaprddesaccount.AaPrdDesAccountRecord;
import com.temenos.t24.api.records.aaproductcatalog.AaProductCatalogRecord;
import com.temenos.t24.api.records.account.AccountRecord;
import com.temenos.t24.api.records.customer.CustomerRecord;
import com.temenos.t24.api.system.DataAccess;
import com.temenos.t24.api.system.Session;

public class VerDeathInfoUpdatePostRtn extends RecordLifecycle {
   private static final String L3API = "L3API";
    private static final Logger verLogger = LoggerFactory.getLogger(L3API);
//private static final FusionFileLogger verLogger = //FusionFileLogger.getLogger(VerDeathInfoUpdatePostRtn.class);
    VerDeathDateUpdate verDeathUpdObj = new VerDeathDateUpdate();
    public static final String CUR_DOD_STS = "FF.CUR.DOD.STS";
    public static final String CUR_DOD_STS_FLD = "FF.CUR.DOD.STS:";
    public static final String CUR_DOD_DATE_FLD = "FF.CUR.DOD.DATE:";
    public static final String CUR_DOD_ACT_FLD = "FF.DOD.STS.ACT:";
    public static final String CUR_DOD_CONF_FLD = "FF.VER.DOD.CONF:";
    public static final String ACCOUNT_APP = "ACCOUNT";

    @Override
    public void postUpdateRequest(String application, String currentRecordId, TStructure currentRecord,
            List<TransactionData> transactionData, List<TStructure> currentRecords,
            TransactionContext transactionContext) {

        String activityId = "LENDING-UPDATE-ACCOUNT";
        try {
verLogger.info("rtn triggered ");
            DataAccess daObj = new DataAccess(this);
            Session ssObj = new Session(this);
            Contract contractObj = new Contract(this);
            String compMne = ssObj.getCompanyRecord().getFinancialMne().getValue();
            CustomerRecord cusRecObj = new CustomerRecord(currentRecord);

            String currStatus = "";
            String notDeathDate = "";
            String statusAct = "";
            String deathDate = "";
            currStatus = cusRecObj.getLocalRefField(CUR_DOD_STS).getValue();
            notDeathDate = cusRecObj.getNotificationOfDeath().getValue();
            deathDate = cusRecObj.getDeathDate().getValue();
            List<TField> statusActList = cusRecObj.getText();
            if (!statusActList.isEmpty()) {
                statusAct = statusActList.get(statusActList.size() - 1).getValue();
            }
verLogger.info("currStatus " + currStatus);
            if (!currStatus.isEmpty()) {
                List<String> accountRecList = verDeathUpdObj.getAccountList(daObj, currentRecordId);
                for (String accId : accountRecList) {
                    AccountRecord accRec = new AccountRecord(daObj.getRecord(compMne, ACCOUNT_APP, "", accId));

                    String arrId = accRec.getArrangementId().getValue();
verLogger.info("arrId  " + arrId);

                    if (!arrId.isEmpty()) {
                        AaArrangementRecord aaObj = new AaArrangementRecord(
                                daObj.getRecord(compMne, "AA.ARRANGEMENT", "", arrId));
                        contractObj.setContractId(arrId);
                        if (currStatus.equals("DECEASED") || currStatus.equals("CONFIRMATION")) {

                            updateAAwithDeaceasedDetails(currentRecords, transactionData, activityId, aaObj, arrId,
                                    contractObj, deathDate, currStatus, statusAct);
                        } else if (currStatus.equals("CONFIRMED") || currStatus.equals("PAYOFF")
                                || currStatus.equals("REJECTED") || currStatus.equals("WRITEOFF")) {

                            updateFinalDeathDetails(currentRecords, transactionData, activityId, aaObj, arrId,
                                    contractObj, notDeathDate, currStatus, statusAct);

                        }

                    }
                }
            }
        } catch (Exception e) {
            verLogger.info(e.getMessage());
        }
    }

    /**
     * @param currentRecords
     * @param transactionData
     * @param activityId
     * @param aaObj
     * @param arrId
     * @param contractObj
     * @param notDeathDate
     * @param statusAct
     * @param currStatus
     */
    private void updateAAwithDeaceasedDetails(List<TStructure> currentRecords, List<TransactionData> transactionData,
            String activityId, AaArrangementRecord aaObj, String arrId, Contract contractObj, String deathDate,
            String currStatus, String statusAct) {
        try {
verLogger.info("decased dets updation  " );

            String coCode = aaObj.getCoCodeRec().getValue();
            String prodId = aaObj.getProduct().get(0).getProduct().getValue();
            verLogger.info(coCode + "," + prodId);
            AaPrdDesAccountRecord aaAcctObj = new AaPrdDesAccountRecord(
                    contractObj.getConditionForProperty(ACCOUNT_APP));

            LocalRefList curList = aaAcctObj.getLocalRefGroups(CUR_DOD_STS);

            AaArrangementActivityRecord aaaObj = new AaArrangementActivityRecord(this);
            aaaObj.setArrangement(arrId);
            aaaObj.setActivity(activityId);
            Product prod = new Product(this);
            prod.setProductId(prodId);
            AaProductCatalogRecord aaPrd = prod.getProduct();
            PropertyClass propClass = new PropertyClass(this);
            propClass.setPropertyClassId(ACCOUNT_APP);
            List<String> propList = propClass.getPropertyIdsForProduct(aaPrd);

            com.temenos.t24.api.records.aaarrangementactivity.PropertyClass acctClass = new com.temenos.t24.api.records.aaarrangementactivity.PropertyClass();

            acctClass.setProperty(propList.get(0));
            FieldNameClass fld1 = new FieldNameClass();
            FieldNameClass fld2 = new FieldNameClass();
            FieldNameClass fld3 = new FieldNameClass();

            for (int i = 0; i <= curList.size(); i++) {

                int j = i + 1;
                int k = j + 1;
                if (i == curList.size()) {
                    fld1.setFieldName(CUR_DOD_STS_FLD + j);
                    fld1.setFieldValue(currStatus);
                    acctClass.setFieldName(fld1, i);

                    fld2.setFieldName(CUR_DOD_DATE_FLD + j);
                    fld2.setFieldValue(deathDate);
                    acctClass.setFieldName(fld2, j);

                    fld3.setFieldName(CUR_DOD_ACT_FLD + j);
                    fld3.setFieldValue(statusAct);
                    acctClass.setFieldName(fld3, k);

                } else {
                    String currDodStatus = curList.get(i).getLocalRefField(CUR_DOD_STS).getValue();
                    String currDodDate = curList.get(i).getLocalRefField("FF.CUR.DOD.DATE").getValue();
                    String currStsAct = curList.get(i).getLocalRefField("FF.DOD.STS.ACT").getValue();

                    fld1.setFieldName(CUR_DOD_STS_FLD + j);
                    fld1.setFieldValue(currDodStatus);
                    acctClass.setFieldName(fld1, i);
                    fld2.setFieldName(CUR_DOD_DATE_FLD + j);
                    fld2.setFieldValue(currDodDate);
                    acctClass.setFieldName(fld2, j);
                    fld3.setFieldName(CUR_DOD_ACT_FLD + j);
                    fld3.setFieldValue(currStsAct);
                    acctClass.setFieldName(fld3, k);
                }
            }
            verLogger.info("acctClass " + acctClass);
            TransactionData syncTransactionData = new TransactionData();
            syncTransactionData.setVersionId("AA.ARRANGEMENT.ACTIVITY,INSURANCE.UPD");
            syncTransactionData.setTransactionId("/");
            syncTransactionData.setSourceId("OFS.INSURANCE.UPD");
            syncTransactionData.setCompanyId(coCode);
            transactionData.add(syncTransactionData);
            aaaObj.setProperty(acctClass, 0);
            currentRecords.add(aaaObj.toStructure());
        } catch (Exception e) {
            verLogger.info(e.getMessage());
        }
    }

    /**
     * @param currentRecords
     * @param transactionData
     * @param activityId1
     * @param aaObj
     * @param arrId
     * @param contractObj
     * @param notDeathDate
     * @param currStatus
     * @param finVerDate
     * @param statusAct
     */
    private void updateFinalDeathDetails(List<TStructure> currentRecords, List<TransactionData> transactionData,
            String activityId, AaArrangementRecord aaObj, String arrId, Contract contractObj, String notDeathDate,
            String currStatus, String statusAct) {
        try {
verLogger.info("inside updateFinalDeathDetails  ");

            String coCode = aaObj.getCoCodeRec().getValue();
            String prodId = aaObj.getProduct().get(0).getProduct().getValue();
            verLogger.info(coCode + "," + prodId);
            AaPrdDesAccountRecord aaAcctObj = new AaPrdDesAccountRecord(
                    contractObj.getConditionForProperty(ACCOUNT_APP));

            LocalRefList curList = aaAcctObj.getLocalRefGroups(CUR_DOD_STS);

            AaArrangementActivityRecord aaaObj = new AaArrangementActivityRecord(this);
            
            aaaObj.setArrangement(arrId);
            aaaObj.setActivity(activityId);
            Product prod = new Product(this);
            prod.setProductId(prodId);
            AaProductCatalogRecord aaPrd = prod.getProduct();
            PropertyClass propClass = new PropertyClass(this);
            propClass.setPropertyClassId(ACCOUNT_APP);
            List<String> propList = propClass.getPropertyIdsForProduct(aaPrd);
            com.temenos.t24.api.records.aaarrangementactivity.PropertyClass acctClass = new com.temenos.t24.api.records.aaarrangementactivity.PropertyClass();
            acctClass.setProperty(propList.get(0));
            FieldNameClass fld1 = new FieldNameClass();
            FieldNameClass fld2 = new FieldNameClass();
            FieldNameClass fld3 = new FieldNameClass();
            FieldNameClass fld4 = new FieldNameClass();
            for (int i = 0; i <= curList.size(); i++) {
                int j = i + 1;
                int k = j + 1;
                if (i == curList.size()) {
                    fld1.setFieldName(CUR_DOD_STS_FLD + j);
                    fld1.setFieldValue(currStatus);

                    acctClass.setFieldName(fld1, i);
                    fld2.setFieldName(CUR_DOD_DATE_FLD + j);
                    fld2.setFieldValue(notDeathDate);

                    acctClass.setFieldName(fld2, j);
                    fld3.setFieldName(CUR_DOD_ACT_FLD + j);
                    fld3.setFieldValue(statusAct);

                    acctClass.setFieldName(fld3, k);

                    if (currStatus.equals("REJECTED")) {
                        fld4.setFieldName(CUR_DOD_CONF_FLD + 1);
                        fld4.setFieldValue("");

                        acctClass.setFieldName(fld4, 0);
                    } else {
                        fld4.setFieldName(CUR_DOD_CONF_FLD + 1);
                        fld4.setFieldValue(notDeathDate);

                        acctClass.setFieldName(fld4, 0);
                    }

                } else {
                    String currDodStatus = curList.get(i).getLocalRefField(CUR_DOD_STS).getValue();
                    String currDodDate = curList.get(i).getLocalRefField("FF.CUR.DOD.DATE").getValue();

                    fld1.setFieldName(CUR_DOD_STS_FLD + j);
                    fld1.setFieldValue(currDodStatus);
                    acctClass.setFieldName(fld1, i);
                    fld2.setFieldName(CUR_DOD_DATE_FLD + j);
                    fld2.setFieldValue(currDodDate);
                    acctClass.setFieldName(fld2, j);
                    fld3.setFieldName(CUR_DOD_ACT_FLD + j);
                    fld3.setFieldValue(statusAct);

                    acctClass.setFieldName(fld3, k);

                }
            }
verLogger.info("acctClass "+ acctClass);
            TransactionData syncTransactionData = new TransactionData();
            syncTransactionData.setVersionId("AA.ARRANGEMENT.ACTIVITY,INSURANCE.UPD");
            syncTransactionData.setTransactionId("/");
            syncTransactionData.setSourceId("OFS.INSURANCE.UPD");
            syncTransactionData.setCompanyId(coCode);
            transactionData.add(syncTransactionData);
            aaaObj.setProperty(acctClass, 0);
            currentRecords.add(aaaObj.toStructure());
verLogger.info("aaaObj  "+ aaaObj);
verLogger.info("currentRecords "+ currentRecords);
        } catch (Exception e) {
            verLogger.info(e.getMessage());

        }

    }

}
