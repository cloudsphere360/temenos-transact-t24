package com.temenos.fusion;

import java.util.List;

import com.temenos.api.LocalRefGroup;
import com.temenos.api.LocalRefList;
import com.temenos.api.TStructure;
import com.temenos.logging.facade.Logger;
import com.temenos.logging.facade.LoggerFactory;
import com.temenos.t24.api.arrangement.Product;
import com.temenos.t24.api.arrangement.accounting.Contract;
import com.temenos.t24.api.complex.eb.servicehook.TransactionData;
import com.temenos.t24.api.complex.eb.templatehook.TransactionContext;
import com.temenos.t24.api.hook.system.RecordLifecycle;
import com.temenos.t24.api.records.aaarrangement.AaArrangementRecord;
import com.temenos.t24.api.records.aaarrangementactivity.AaArrangementActivityRecord;
import com.temenos.t24.api.records.aasimulationcapture.AaSimulationCaptureRecord;
import com.temenos.t24.api.records.account.AccountRecord;
import com.temenos.t24.api.records.customer.CustomerRecord;
import com.temenos.t24.api.records.ebffloandetails.EbFfLoanDetailsRecord;
import com.temenos.t24.api.records.ebffloandetails.FfCurDodStsClass;
import com.temenos.t24.api.records.ebffloandetails.FfGuarDodStsClass;
import com.temenos.t24.api.system.DataAccess;
import com.temenos.t24.api.system.Session;

public class VerUpdateAAActvities extends RecordLifecycle {
    private static final String L3API = "L3API";
    private static final Logger verLogger = LoggerFactory.getLogger(L3API);
    VerDeathDateUpdate verDeathUpdObj = new VerDeathDateUpdate();
    VerGuarantorDeathDateUpd verGuarantorUpdObj = new VerGuarantorDeathDateUpd();
    public static final String CONFIRMED_STS = "CONFIRMED";
    public static final String PAYOFF_STS = "PAYOFF";
    public static final String WRITEOFF_STS = "WRITEOFF";

    @Override
    public void postUpdateRequest(String application, String currentRecordId, TStructure currentRecord,
            List<TransactionData> transactionData, List<TStructure> currentRecords,
            TransactionContext transactionContext) {
        DataAccess daObj = new DataAccess(this);
        Session ssObj = new Session(this);
        Contract contractObj = new Contract(this);
        String activityId = "LENDING-WRITE.OFF-BAL.MAINTAIN";
        String activityId1 = "LENDING-SUSPEND-ARRANGEMENT";
        String activityId2 = "LENDING-CALCULATE-FORECLOSURE";
        String status = "";
        String currStatus = "";
        String arrId = "";
        try {
            String compMne = ssObj.getCompanyRecord().getFinancialMne().getValue();

            CustomerRecord cusRecObj = new CustomerRecord(currentRecord);
            currStatus = cusRecObj.getLocalRefField("FF.CUR.DOD.STS").getValue();
            LocalRefList guarDodList = cusRecObj.getLocalRefGroups("FF.GUAR.DOD.STS");
            int guarDodSize = guarDodList.size();
            if (guarDodSize > 0) {
                status = guarDodList.get(guarDodSize - 1).getLocalRefField("FF.GUAR.DOD.STS").getValue();

            }
            LocalRefList loanNum = cusRecObj.getLocalRefGroups("FF.LOAN.NUMBER");
            int loanSize = loanNum.size();
            verLogger.info("currStatus " + currStatus);
            verLogger.info("status " + status);
            verLogger.info(compMne);

            verLogger.info("currStatus " + currStatus);
            if (!currStatus.isEmpty() || !(status.isEmpty() && loanSize > 0)) {
                if (!status.isEmpty()) {
                    for (LocalRefGroup loanNumber : loanNum) {
                        Boolean isGuarantor = false;
                        arrId = loanNumber.getLocalRefField("FF.LOAN.NUMBER").getValue();
                        if (!arrId.isEmpty()) {
                            AaArrangementRecord aaObj = new AaArrangementRecord(
                                    daObj.getRecord(compMne, "AA.ARRANGEMENT", "", arrId));
                            EbFfLoanDetailsRecord ebFfLoanDetailsObj = verGuarantorUpdObj.readEbFfRecord(compMne, daObj,
                                    arrId);
                            verLogger.info("ebFfLoanDetailsObj" + ebFfLoanDetailsObj);
                            if (ebFfLoanDetailsObj != null) {
                                isGuarantor = verGuarantorUpdObj.getGuarantorDets(ebFfLoanDetailsObj);
                                if (currStatus.equals(CONFIRMED_STS) || (status.equals(CONFIRMED_STS) && isGuarantor)) {
                                    updateFinalDeathDetails(currentRecords, transactionData, activityId1, aaObj, arrId,
                                            contractObj);

                                } else if (currStatus.equals(PAYOFF_STS)
                                        || (status.equals(PAYOFF_STS) && isGuarantor)) {

                                    updatePayOffDeathDetails(currentRecords, transactionData, activityId2, aaObj, arrId,
                                            contractObj);

                                } else if (currStatus.equals(WRITEOFF_STS)
                                        || (status.equals(WRITEOFF_STS) && isGuarantor)) {
                                    updateFinalDeathDetails(currentRecords, transactionData, activityId, aaObj, arrId,
                                            contractObj);
                                }
                            }
                        }
                    }
                }
                if (!currStatus.isEmpty()) {
                    List<String> accountRecList = verDeathUpdObj.getAccountList(daObj, currentRecordId);
                    for (String accId : accountRecList) {
                        AccountRecord accRec = new AccountRecord(daObj.getRecord(compMne, "ACCOUNT", "", accId));
                        arrId = accRec.getArrangementId().getValue();
                        if (!arrId.isEmpty()) {
                            AaArrangementRecord aaObj = new AaArrangementRecord(
                                    daObj.getRecord(compMne, "AA.ARRANGEMENT", "", arrId));
                            if (currStatus.equals(CONFIRMED_STS)) {
                                updateFinalDeathDetails(currentRecords, transactionData, activityId1, aaObj, arrId,
                                        contractObj);

                            } else if (currStatus.equals(PAYOFF_STS)) {

                                updatePayOffDeathDetails(currentRecords, transactionData, activityId2, aaObj, arrId,
                                        contractObj);

                            } else if (currStatus.equals(WRITEOFF_STS)) {
                                updateFinalDeathDetails(currentRecords, transactionData, activityId, aaObj, arrId,
                                        contractObj);
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
     * @param activityId1
     * @param aaObj
     * @param arrId
     * @param contractObj
     */
    private void updateFinalDeathDetails(List<TStructure> currentRecords, List<TransactionData> transactionData,
            String activityId, AaArrangementRecord aaObj, String arrId, Contract contractObj) {
        try {
            contractObj.setContractId(arrId);
            String coCode = aaObj.getCoCodeRec().getValue();
            String prodId = aaObj.getProduct().get(0).getProduct().getValue();
            verLogger.info(coCode + "," + prodId);
            AaArrangementActivityRecord aaaObj = new AaArrangementActivityRecord(this);
            aaaObj.setArrangement(arrId);
            aaaObj.setActivity(activityId);
            Product prod = new Product(this);
            prod.setProductId(prodId);
            TransactionData syncTransactionData = new TransactionData();
            syncTransactionData.setVersionId("AA.ARRANGEMENT.ACTIVITY,INSURANCE.UPD");
            syncTransactionData.setTransactionId("/");
            syncTransactionData.setSourceId("OFS.INSURANCE.UPD");
            syncTransactionData.setCompanyId(coCode);
            transactionData.add(syncTransactionData);
            currentRecords.add(aaaObj.toStructure());
        } catch (Exception e) {
            verLogger.info(e.getMessage());

        }

    }

    private void updatePayOffDeathDetails(List<TStructure> currentRecords, List<TransactionData> transactionData,
            String activityId, AaArrangementRecord aaObj, String arrId, Contract contractObj) {
        try {
            contractObj.setContractId(arrId);
            String coCode = aaObj.getCoCodeRec().getValue();
            String prodId = aaObj.getProduct().get(0).getProduct().getValue();
            verLogger.info(coCode + "," + prodId);
            AaSimulationCaptureRecord aaSimObj = new AaSimulationCaptureRecord(this);

            aaSimObj.setArrangement(arrId);
            aaSimObj.setActivity(activityId);
            aaSimObj.setAutoRun("DIRECT.EXECUTE");
            Product prod = new Product(this);
            prod.setProductId(prodId);
            TransactionData syncTransactionData = new TransactionData();
            syncTransactionData.setVersionId("AA.SIMULATION.CAPTURE,FF.INSURANCE");
            syncTransactionData.setTransactionId("/");
            syncTransactionData.setSourceId("OFS.INSURANCE.UPD");
            syncTransactionData.setCompanyId(coCode);
            transactionData.add(syncTransactionData);
            currentRecords.add(aaSimObj.toStructure());
        } catch (Exception e) {
            verLogger.info(e.getMessage());

        }

    }

}
