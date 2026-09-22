package com.temenos.fusion;

import java.util.List;

import com.temenos.api.TStructure;
import com.temenos.t24.api.complex.eb.servicehook.TransactionData;
import com.temenos.t24.api.complex.eb.templatehook.TransactionContext;
import com.temenos.t24.api.hook.system.RecordLifecycle;
import com.temenos.t24.api.records.customer.CustomerRecord;
import com.temenos.t24.api.records.ebffcustomerfunder.EbFfCustomerFunderRecord;
import com.temenos.t24.api.records.ebfffunderdetails.EbFfFunderDetailsRecord;

/**
 *
 * @author sr115630
 *
 */
public class FfCusIdUpdate extends RecordLifecycle {
    String funderName = "";
    String funderRef = "";
    private static final FusionFileLogger FF_CUS_ID_UPDATE = FusionFileLogger.getLogger(FfCusIdUpdate.class);

    @Override
    public void postUpdateRequest(String application, String currentRecordId, TStructure currentRecord,
            List<TransactionData> transactionData, List<TStructure> currentRecords,
            TransactionContext transactionContext) {
        try {
            CustomerRecord cusRec = new CustomerRecord(currentRecord);
            FF_CUS_ID_UPDATE.info("cusRec" + cusRec);
            funderName = cusRec.getShortName().get(0).getValue();
            FF_CUS_ID_UPDATE.info("funderName" + funderName);
            funderRef = cusRec.getMnemonic().getValue();
            FF_CUS_ID_UPDATE.info("funderRef" + funderRef);
            
            EbFfCustomerFunderRecord ebFfCustomerRec = new EbFfCustomerFunderRecord();
            FF_CUS_ID_UPDATE.info("ebFfCustomerRec" + ebFfCustomerRec);
            ebFfCustomerRec.setFunderName(funderName);
            FF_CUS_ID_UPDATE.info("ebFfCustomerRec setting the value" + ebFfCustomerRec);
            ebFfCustomerRec.setFunderRef(funderRef);
            FF_CUS_ID_UPDATE.info("ebFfCustomerRec setting value" + ebFfCustomerRec);
            TransactionData txnData = new TransactionData();
            txnData.setVersionId("EB.FF.CUSTOMER.FUNDER,FUNDER.CUSTOMER");
            txnData.setTransactionId(currentRecordId);
            txnData.setFunction("INPUT");
            txnData.setNumberOfAuthoriser("0");
            transactionData.add(txnData);
            txnData.setSourceId("FF.FUNDER.PROCESS");
            currentRecords.add(ebFfCustomerRec.toStructure());

            EbFfFunderDetailsRecord ebFffunderRec = new EbFfFunderDetailsRecord();
            FF_CUS_ID_UPDATE.info("ebFffunderRec" + ebFffunderRec);
            ebFffunderRec.setFunderName(funderName);
            FF_CUS_ID_UPDATE.info("ebFffunderRec setting the value" + ebFffunderRec);
            ebFffunderRec.setFunderRef(funderRef);
            FF_CUS_ID_UPDATE.info("ebFffunderRec setting value" + ebFffunderRec);
            TransactionData txnValue = new TransactionData();
            txnValue.setVersionId("EB.FF.FUNDER.DETAILS,OFS.FUNDER.CUSTOMER");
            txnValue.setTransactionId(currentRecordId);
            txnValue.setFunction("INPUT");           
            transactionData.add(txnValue);
            txnValue.setSourceId("FF.FUNDER.PROCESS");
            currentRecords.add(ebFffunderRec.toStructure());
        } catch (Exception e) {
            e.getMessage();
        }
    }

}
