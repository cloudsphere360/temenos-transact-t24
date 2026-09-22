package com.temenos.fusion;

import com.temenos.api.TStructure;
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
public class FfVerBankDefaultDetails extends RecordLifecycle {
    DataAccess da = new DataAccess(this);
     
    @Override
    public void defaultFieldValues(String application, String currentRecordId, TStructure currentRecord,
            TStructure unauthorisedRecord, TStructure liveRecord, TransactionContext transactionContext) {
        try {
               EbFfTtBankCollectionRecord bankDetails = new EbFfTtBankCollectionRecord(currentRecord);
               String branchCode = bankDetails.getBankId().getValue();
               if(branchCode==null) {
                   return;
               }
               EbFfTtCollectBankDetailsRecord collectBank = new EbFfTtCollectBankDetailsRecord(da.getRecord("EB.FF.TT.COLLECT.BANK.DETAILS", branchCode));
               String agency1 = collectBank.getAmountDepositThirdparty1().getValue();
               String agency2 = collectBank.getAmountDepositThirdparty2().getValue();
               String branchAc1 = collectBank.getCollectionAccount1().getValue();
               String branchAc2 = collectBank.getCollectionAccount2().getValue();
               String ho1 = collectBank.getHoAccount1().getValue();
               String ho2 = collectBank.getHoAccount2().getValue();
               String ho3 = collectBank.getHoAccount3().getValue();
               String ho4 = collectBank.getHoAccount4().getValue();
               String ho5 = collectBank.getHoAccount5().getValue();
               String otherAc = collectBank.getOtherCollectionAccount().getValue();
               bankDetails.setAmountDepositThirdparty1(agency1);
               bankDetails.setAmountDepositThirdparty2(agency2);
               bankDetails.setCollectionAccount1(branchAc1);
               bankDetails.setCollectionAccount2(branchAc2);
               bankDetails.setHoAccount1(ho1);
               bankDetails.setHoAccount2(ho2);
               bankDetails.setHoAccount3(ho3);
               bankDetails.setHoAccount4(ho4);
               bankDetails.setHoAccount5(ho5);
               bankDetails.setOtherCollectionAccount(otherAc);
               currentRecord.set(bankDetails.toStructure());
        } catch (Exception e) {
            logger.error("An error occured", e);
            
            }
    }
    

}
