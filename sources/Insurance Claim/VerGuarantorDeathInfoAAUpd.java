package com.temenos.fusion;

import java.util.List;

import com.temenos.api.LocalRefGroup;
import com.temenos.api.LocalRefList;
import com.temenos.api.TStructure;
import com.temenos.logging.facade.Logger;
import com.temenos.logging.facade.LoggerFactory;
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
import com.temenos.t24.api.records.customer.CustomerRecord;
import com.temenos.t24.api.records.ebffloandetails.EbFfLoanDetailsRecord;
import com.temenos.t24.api.system.DataAccess;
import com.temenos.t24.api.system.Session;

public class VerGuarantorDeathInfoAAUpd extends RecordLifecycle {
    private static final String L3API = "L3API";
    private static final Logger verLogger = LoggerFactory.getLogger(L3API);
    VerDeathDateUpdate verDeathUpdObj = new VerDeathDateUpdate();
    VerGuarantorDeathDateUpd verGuarantorUpdObj = new VerGuarantorDeathDateUpd();
    public static final String GUR_DOD_STS = "FF.GUAR.DOD.STS";
    public static final String REJECTED_STS = "REJECTED";
    public static final String GUR_DOD_DATE = "FF.GUR.DOD.DATE";
    public static final String GUAR_STS_ACT = "FF.GUAR.STS.ACT";
    public static final String GUR_DOD_STS_FLD = "FF.GUAR.DOD.STS:";
    public static final String GUR_DOD_DATE_FLD = "FF.GUR.DOD.DATE:";
    public static final String GUAR_STS_ACT_FLD = "FF.GUAR.STS.ACT:";
    public static final String ACCOUNT_APP = "ACCOUNT";

    @Override
    public void postUpdateRequest(String application, String currentRecordId, TStructure currentRecord,
            List<TransactionData> transactionData, List<TStructure> currentRecords,
            TransactionContext transactionContext) {

        String activityId = "LENDING-UPDATE-ACCOUNT";
        try {
            DataAccess daObj = new DataAccess(this);
            Session ssObj = new Session(this);

            Contract contractObj = new Contract(this);
            String compMne = ssObj.getCompanyRecord().getFinancialMne().getValue();
            CustomerRecord cusRecObj = new CustomerRecord(currentRecord);

            String currStatus = "";
            String notDeathDate = "";
            String statusAct = "";
            LocalRefList loanNum = cusRecObj.getLocalRefGroups("FF.LOAN.NUMBER");
            int loanSize = loanNum.size();
            if (loanSize > 0) {
                LocalRefList guarDodList = cusRecObj.getLocalRefGroups(GUR_DOD_STS);
                int guarDodSize = cusRecObj.getLocalRefGroups(GUR_DOD_STS).size();
                if (guarDodSize > 0) {
                    currStatus = guarDodList.get(guarDodSize - 1).getLocalRefField(GUR_DOD_STS).getValue();
                    if (!currStatus.equals(REJECTED_STS)) {
                        notDeathDate = guarDodList.get(guarDodSize - 1).getLocalRefField(GUR_DOD_DATE).getValue();

                    }
                    statusAct = guarDodList.get(guarDodSize - 1).getLocalRefField(GUAR_STS_ACT).getValue();

                }
                verLogger.info("currStatus " + currStatus);

                if (!currStatus.isEmpty()) {
                    for (LocalRefGroup loanNumber : loanNum) {
                        Boolean isGuarantor = false;
                        String arrId = loanNumber.getLocalRefField("FF.LOAN.NUMBER").getValue();
                        if (!arrId.isEmpty()) {
                            AaArrangementRecord aaObj = new AaArrangementRecord(
                                    daObj.getRecord(compMne, "AA.ARRANGEMENT", "", arrId));
                            contractObj.setContractId(arrId);
                            EbFfLoanDetailsRecord ebFfLoanDetailsObj = verGuarantorUpdObj.readEbFfRecord(compMne, daObj,
                                    arrId);

                            if (ebFfLoanDetailsObj != null) {
                                isGuarantor = verGuarantorUpdObj.getGuarantorDets(ebFfLoanDetailsObj);
                                if (Boolean.TRUE.equals(isGuarantor)) {
                                    if (currStatus.equals("DECEASED") || currStatus.equals("CONFIRMATION")) {
                                        updateAAwithDeaceasedDetails(currentRecords, transactionData, activityId, aaObj,
                                                arrId, contractObj, notDeathDate, statusAct, currStatus);

                                    } else if (currStatus.equals("CONFIRMED") || currStatus.equals("PAYOFF")
                                            || currStatus.equals(REJECTED_STS) || currStatus.equals("WRITEOFF")) {

                                        updateFinalDeathDetails(currentRecords, transactionData, activityId, aaObj,
                                                arrId, contractObj, notDeathDate, currStatus, statusAct);

                                    }
                                }
                            }
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
            String statusAct, String currStatus) {
        String coCode = aaObj.getCoCodeRec().getValue();
        String prodId = aaObj.getProduct().get(0).getProduct().getValue();
        verLogger.info(coCode + "," + prodId);
        AaPrdDesAccountRecord aaAcctObj = new AaPrdDesAccountRecord(contractObj.getConditionForProperty(ACCOUNT_APP));

        AaArrangementActivityRecord aaaObj = new AaArrangementActivityRecord(this);
        LocalRefList curList = aaAcctObj.getLocalRefGroups(GUR_DOD_STS);
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
                fld1.setFieldName(GUR_DOD_STS_FLD + j);
                fld1.setFieldValue(currStatus);
                acctClass.setFieldName(fld1, i);
                fld2.setFieldName(GUR_DOD_DATE_FLD + j);
                fld2.setFieldValue(deathDate);
                acctClass.setFieldName(fld2, j);
                fld3.setFieldName(GUAR_STS_ACT_FLD + j);
                fld3.setFieldValue(statusAct);
                acctClass.setFieldName(fld3, k);
            } else {
                String currDodStatus = curList.get(i).getLocalRefField(GUR_DOD_STS).getValue();
                String currDodDate = curList.get(i).getLocalRefField(GUR_DOD_DATE).getValue();
                String currStsAct = curList.get(i).getLocalRefField(GUAR_STS_ACT).getValue();

                fld1.setFieldName(GUR_DOD_STS_FLD + j);
                fld1.setFieldValue(currDodStatus);
                acctClass.setFieldName(fld1, i);
                fld2.setFieldName(GUR_DOD_DATE_FLD + j);
                fld2.setFieldValue(currDodDate);
                acctClass.setFieldName(fld2, j);
                fld3.setFieldName(GUAR_STS_ACT_FLD + j);
                fld3.setFieldValue(currStsAct);
                acctClass.setFieldName(fld3, k);
            }
        }

        TransactionData syncTransactionData = new TransactionData();
        syncTransactionData.setVersionId("AA.ARRANGEMENT.ACTIVITY,INSURANCE.UPD");
        syncTransactionData.setTransactionId("/");
        syncTransactionData.setSourceId("OFS.INSURANCE.UPD");
        syncTransactionData.setCompanyId(coCode);
        transactionData.add(syncTransactionData);
        aaaObj.setProperty(acctClass, 0);
        currentRecords.add(aaaObj.toStructure());

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
            String coCode = aaObj.getCoCodeRec().getValue();
            String prodId = aaObj.getProduct().get(0).getProduct().getValue();

            AaPrdDesAccountRecord aaAcctObj = new AaPrdDesAccountRecord(
                    contractObj.getConditionForProperty(ACCOUNT_APP));

            LocalRefList curList = aaAcctObj.getLocalRefGroups(GUR_DOD_STS);

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
                verLogger.info("i " + i);
                int j = i + 1;
                int k = j + 1;
                if (i == curList.size()) {
                    fld1.setFieldName(GUR_DOD_STS_FLD + j);
                    fld1.setFieldValue(currStatus);

                    acctClass.setFieldName(fld1, i);
                    fld2.setFieldName(GUR_DOD_DATE_FLD + j);
                    fld2.setFieldValue(notDeathDate);
                    acctClass.setFieldName(fld2, j);
                    fld3.setFieldName(GUAR_STS_ACT_FLD + j);
                    fld3.setFieldValue(statusAct);
                    acctClass.setFieldName(fld3, k);
                    if (currStatus.equals(REJECTED_STS)) {
                        fld4.setFieldName("FF.VER.GUR.CONF:" + 1);
                        fld4.setFieldValue("");
                        acctClass.setFieldName(fld4, 0);
                    } else {
                        fld4.setFieldName("FF.VER.GUR.CONF:" + 1);
                        fld4.setFieldValue(notDeathDate);
                        verLogger.info("fld4 " + fld4);
                        acctClass.setFieldName(fld4, 0);

                    }
                } else {
                    String currDodStatus = curList.get(i).getLocalRefField(GUR_DOD_STS).getValue();
                    String currDodDate = "";
                    if (!currDodStatus.equals(REJECTED_STS)) {
                        currDodDate = curList.get(i).getLocalRefField(GUR_DOD_DATE).getValue();
                    }
                    String statusAct1 = curList.get(i).getLocalRefField(GUAR_STS_ACT).getValue();

                    fld1.setFieldName(GUR_DOD_STS_FLD + j);
                    fld1.setFieldValue(currDodStatus);
                    acctClass.setFieldName(fld1, i);
                    fld2.setFieldName(GUR_DOD_DATE_FLD + j);
                    fld2.setFieldValue(currDodDate);
                    acctClass.setFieldName(fld2, j);
                    fld3.setFieldName(GUAR_STS_ACT_FLD + j);
                    fld3.setFieldValue(statusAct1);
                    acctClass.setFieldName(fld3, k);
                }
            }

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

}
