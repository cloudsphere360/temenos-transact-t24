package com.temenos.fusion;

import java.util.List;

import com.temenos.api.TStructure;
import com.temenos.logging.facade.Logger;
import com.temenos.logging.facade.LoggerFactory;
import com.temenos.t24.api.complex.eb.servicehook.TransactionData;
import com.temenos.t24.api.complex.eb.templatehook.TransactionContext;
import com.temenos.t24.api.hook.system.RecordLifecycle;
import com.temenos.t24.api.records.aaarrangement.AaArrangementRecord;
import com.temenos.t24.api.records.aaarrangementactivity.AaArrangementActivityRecord;
import com.temenos.t24.api.records.ebffwrtchgoffactivity.EbFfWrtChgOffActivityRecord;
import com.temenos.t24.api.records.ebffwrtchgoffactivity.PartialWaiveOffClass;
import com.temenos.t24.api.records.ebffwrtchgoffactivity.WaiveOffOptionsClass;
import com.temenos.t24.api.system.DataAccess;
import com.temenos.t24.api.system.Session;

public class FfVerAuthPostChargeOffAct extends RecordLifecycle {
    private static final String L3API = "L3API";
    private static final Logger verLogger = LoggerFactory.getLogger(L3API);
    double waiveOptionAmt = 0.0;
    String partialWaiveOffAmt = "";
    String productId = "";
    String arrId = "";
    String coCode = "";
    String compMne = "";
    String activityId = "LENDING-CHARGEOFF-ACCOUNT";

    @Override
    public void postUpdateRequest(String application, String currentRecordId, TStructure currentRecord,
            List<TransactionData> transactionData, List<TStructure> currentRecords,
            TransactionContext transactionContext) {

        
        verLogger.info("FfVerAuthPostChargeOffAct Triggering");
        EbFfWrtChgOffActivityRecord ffWrtChgOffRec = new EbFfWrtChgOffActivityRecord(currentRecord);
    
        try {
         
            arrId = ffWrtChgOffRec.getArrangementId().getValue();
            verLogger.info("FfVerAuthPostChargeOffAct arrId" + arrId);
            if (!arrId.equals("")) {
                if (!ffWrtChgOffRec.getWaiveOffOptions().isEmpty()) {
                    for (WaiveOffOptionsClass waiveOptionsList : ffWrtChgOffRec.getWaiveOffOptions()) {
                        if (!waiveOptionsList.getAmount().getValue().equals("")) {
                            waiveOptionAmt += Double.parseDouble(waiveOptionsList.getAmount().getValue());
                        }
                    }
                    executeChageoffActivity(currentRecords, transactionData, activityId, arrId,
                            String.valueOf(waiveOptionAmt));
                }
                if (!ffWrtChgOffRec.getPartialWaiveOff().isEmpty() && ffWrtChgOffRec.getWaiveOffOptions().isEmpty()) {
                    for (PartialWaiveOffClass partialWaiveList : ffWrtChgOffRec.getPartialWaiveOff()) {
                        if (partialWaiveList.getPartialWaiveOff().getValue().equals("YES")) {
                            partialWaiveOffAmt = partialWaiveList.getSettledAmount().getValue();
                            executeChageoffActivity(currentRecords, transactionData, activityId, arrId,
                                    partialWaiveOffAmt);
                        }
                    }                  
                }
            }
        } catch (Exception e) {
verLogger.info(e.getMessage());
        }
    }

    public void executeChageoffActivity(List<TStructure> currentRecords, List<TransactionData> transactionData,
            String activityId, String arrId, String amt) {
        try {
            DataAccess da = new DataAccess(this);
            Session session = new Session(this);
            compMne = session.getCompanyRecord().getFinancialMne().getValue();
            AaArrangementRecord aaObj = new AaArrangementRecord(da.getRecord(compMne, "AA.ARRANGEMENT", "", arrId));
            coCode = aaObj.getCoCodeRec().getValue();
            productId = aaObj.getProduct().get(0).getProduct().getValue();

            AaArrangementActivityRecord aaaRec = new AaArrangementActivityRecord(this);
            aaaRec.setArrangement(arrId);
            aaaRec.setActivity(activityId);
            aaaRec.setCurrency(aaObj.getCurrency().getValue());
            if (!amt.equals("")) {
                aaaRec.setTxnAmount(amt);
            }
            aaaRec.setProduct(productId);
            TransactionData txnData = new TransactionData();
            txnData.setVersionId("AA.ARRANGEMENT.ACTIVITY,FF.LOAN.CHARGE.OFF");
            txnData.setTransactionId("/");
            txnData.setSourceId("OFS.INSURANCE.UPD");
            txnData.setFunction("INPUT");
            txnData.setNumberOfAuthoriser("0");
            txnData.setCompanyId(coCode);
            transactionData.add(txnData);
            verLogger.info("txnData  " + txnData);
            currentRecords.add(aaaRec.toStructure());
        } catch (Exception e) {
            verLogger.info(e.getMessage());
        }
    }
}
