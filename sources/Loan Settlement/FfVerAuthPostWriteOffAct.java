package com.temenos.fusion;

import java.util.List;

import com.temenos.api.TStructure;

import com.temenos.logging.facade.Logger;

import com.temenos.logging.facade.LoggerFactory;

import com.temenos.t24.api.complex.eb.servicehook.TransactionData;

import com.temenos.t24.api.complex.eb.templatehook.TransactionContext;

import com.temenos.t24.api.hook.system.RecordLifecycle;

import com.temenos.t24.api.records.aaarrangementactivity.AaArrangementActivityRecord;

import com.temenos.t24.api.records.ebffwrtchgoffactivity.EbFfWrtChgOffActivityRecord;

import com.temenos.t24.api.system.Session;

public class FfVerAuthPostWriteOffAct extends RecordLifecycle {

    private static final String L3API = "L3API";

    private static final Logger verLogger = LoggerFactory.getLogger(L3API);

    @Override

    public void postUpdateRequest(String application, String currentRecordId, TStructure currentRecord,

            List<TransactionData> transactionData, List<TStructure> currentRecords,

            TransactionContext transactionContext) {

        verLogger.info("FfVerAuthPostWriteOffAct Triggering");

        EbFfWrtChgOffActivityRecord ffWrtChgOffRec = new EbFfWrtChgOffActivityRecord(currentRecord);

        Session session = new Session(this);

        String arrId = "";

        String activityId = "";

        try {

            arrId = ffWrtChgOffRec.getArrangementId().getValue();

            verLogger.info("FfVerAuthPostWriteOffAct arrId" + arrId);

            if (!arrId.equals("") && ffWrtChgOffRec.getFullOutstandingWaiver().getValue().equals("WAIVEOFF")) {

                activityId = "LENDING-WRITE.OFF-BAL.MAINTAIN";

            }

            AaArrangementActivityRecord aaaRec = new AaArrangementActivityRecord(this);

            aaaRec.setArrangement(arrId);

            aaaRec.setActivity(activityId);

            verLogger.info("aaaRec  " + aaaRec);

            TransactionData txnData = new TransactionData();

            txnData.setVersionId("AA.ARRANGEMENT.ACTIVITY,FF.LOAN.CHARGE.OFF");

            txnData.setSourceId("OFS.INSURANCE.UPD");

            txnData.setFunction("INPUT");

            txnData.setNumberOfAuthoriser("0");

            txnData.setCompanyId(session.getCompanyId());

            transactionData.add(txnData);

            currentRecords.add(aaaRec.toStructure());

            verLogger.info("FfVerAuthPostWriteOffAct Ending");

        } catch (Exception e) {

            verLogger.info(e.getMessage());

        }

    }

}