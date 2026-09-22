package com.temenos.fusion;

import com.temenos.api.TStructure;
import com.temenos.api.TValidationResponse;
import com.temenos.t24.api.complex.eb.templatehook.TransactionContext;
import com.temenos.t24.api.hook.system.RecordLifecycle;
import com.temenos.t24.api.records.ebffttbankcollection.EbFfTtBankCollectionRecord;
import com.temenos.t24.api.records.ebffttcollectbankdetails.EbFfTtCollectBankDetailsRecord;
import com.temenos.t24.api.system.DataAccess;

/**
 * TODO: Document me!
 *
 * @author Preethi
 *
 */
public class FfValidateBankId extends RecordLifecycle {
    EbFfTtCollectBankDetailsRecord collectBank = null;
    @Override
    public TValidationResponse validateRecord(String application, String currentRecordId, TStructure currentRecord,
            TStructure unauthorisedRecord, TStructure liveRecord, TransactionContext transactionContext) {
        DataAccess da = new DataAccess(this);
        //EbFfTtBankCollectionRecord bankDetails = new EbFfTtBankCollectionRecord(da.getRecord("EB.FF.TT.BANK.COLLECTION", currentRecordId));
        EbFfTtBankCollectionRecord bankDetails = new EbFfTtBankCollectionRecord(currentRecord);
        String bankId = bankDetails.getBankId().getValue();
        try {
             collectBank = new EbFfTtCollectBankDetailsRecord(da.getRecord("EB.FF.TT.COLLECT.BANK.DETAILS", bankId));
        } catch (Exception e) {
            bankDetails.getBankId().setError("Invalid Bank Id");
        }
        
        currentRecord.set(bankDetails.toStructure());
        return bankDetails.getValidationResponse();
    }
    

}
