package com.temenos.fusion;

import java.util.List;

import com.temenos.api.TStructure;
import com.temenos.logging.facade.Logger;
import com.temenos.logging.facade.LoggerFactory;
import com.temenos.t24.api.complex.eb.servicehook.TransactionData;
import com.temenos.t24.api.complex.eb.templatehook.TransactionContext;
import com.temenos.t24.api.hook.system.RecordLifecycle;
import com.temenos.t24.api.records.aaarrangementactivity.AaArrangementActivityRecord;
import com.temenos.t24.api.records.account.AccountRecord;
import com.temenos.t24.api.records.fundstransfer.FundsTransferRecord;
import com.temenos.t24.api.system.DataAccess;
import com.temenos.t24.api.system.Session;


public class FfFtPostMatureActivity extends RecordLifecycle {
    private static final String L3API = "L3API";
    private static final Logger LOGGER = LoggerFactory.getLogger(L3API);

    
    @Override
    public void postUpdateRequest(String application, String currentRecordId, TStructure currentRecord,
            List<TransactionData> transactionData, List<TStructure> currentRecords,
            TransactionContext transactionContext) {
        LOGGER.info("CurrentRecord of FT post mature " + currentRecordId);
        String transType = "ACP2";

        try {
            LOGGER.info("Inside FT Post Routine try block");
            System.out.println("Inside FT Post Routine try block");
            Session ses = new Session(this);
            DataAccess da = new DataAccess(this);
            String ymnemonic = ses.getCompanyRecord().getFinancialMne().toString();
            FundsTransferRecord fundsTransferObj = new FundsTransferRecord(currentRecord);
            LOGGER.info("FundsTransfer Record: " + fundsTransferObj.toString());

            String credActNo = fundsTransferObj.getCreditAcctNo().toString();
            LOGGER.info("Credit Account Number: "+credActNo);
            String credValDate = fundsTransferObj.getCreditValueDate().toString();
            LOGGER.info("Credit Value Date: "+credValDate);
            AccountRecord acctRec = new AccountRecord(da.getRecord(ymnemonic, "ACCOUNT", "", credActNo));
            LOGGER.info("Account Record: "+acctRec);
            String yArrId = acctRec.getArrangementId().toString();
            LOGGER.info("Arrangement Id: "+yArrId);

            if (fundsTransferObj.getTransactionType().toString().equals(transType)) {
                LOGGER.info("If block mature executed successfully");
                
                postFtMatureArr(yArrId,transactionData,currentRecords);
                postFtResidualArr(yArrId,transactionData,currentRecords,credValDate);
                
                
            }

        } catch (Exception e) {
            e.getMessage();
        }

    }


    


    private void postFtMatureArr(String yArrId, List<TransactionData> transactionData,
            List<TStructure> currentRecords) {
        
        
        AaArrangementActivityRecord aaArrangementActivityRecord = new AaArrangementActivityRecord(this);
        aaArrangementActivityRecord.setArrangement(yArrId);
        aaArrangementActivityRecord.setActivity("LENDING-MATURE-ARRANGEMENT");
        currentRecords.add(aaArrangementActivityRecord.toStructure());
        TransactionData transactionDataObj = new TransactionData();
        transactionDataObj.setVersionId("AA.ARRANGEMENT.ACTIVITY,CANCEL");
        transactionDataObj.setFunction("INPUT");
        transactionDataObj.setSourceId("NETOFF.OFS");
        transactionDataObj.setNumberOfAuthoriser("0");
        transactionDataObj.setTransactionId("/");
        LOGGER.info("txnData arrActivity Mature: " + transactionDataObj.toString());
        transactionData.add(transactionDataObj);
        
    }
    
    
    private void postFtResidualArr(String yArrId, List<TransactionData> transactionData,
            List<TStructure> currentRecords, String credValDate) {
        
        AaArrangementActivityRecord aaArrangementActivityRec = new AaArrangementActivityRecord(this);
        aaArrangementActivityRec.setArrangement(yArrId);
        aaArrangementActivityRec.setActivity("LENDING-RESIDUAL-ACCOUNT");
        aaArrangementActivityRec.setEffectiveDate(credValDate);
        currentRecords.add(aaArrangementActivityRec.toStructure());
        TransactionData transactionDataObj1 = new TransactionData();
        transactionDataObj1.setVersionId("AA.ARRANGEMENT.ACTIVITY,CANCEL");
        transactionDataObj1.setFunction("INPUT");
        transactionDataObj1.setSourceId("NETOFF.OFS");
        transactionDataObj1.setNumberOfAuthoriser("0");
        transactionDataObj1.setTransactionId("/");
        LOGGER.info("txnData arrActivity Residual: " + transactionDataObj1.toString());
        transactionData.add(transactionDataObj1);
        
    }

}
