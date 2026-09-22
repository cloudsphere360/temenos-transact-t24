package com.temenos.fusion;

import com.temenos.api.TStructure;

import com.temenos.t24.api.complex.eb.templatehook.TransactionContext;
import com.temenos.t24.api.hook.system.RecordLifecycle;
import com.temenos.t24.api.records.customer.CustomerRecord;
import com.temenos.t24.api.records.ebffcustomerfunder.EbFfCustomerFunderRecord;
import com.temenos.t24.api.system.DataAccess;

/**
 * TODO: Document me!
 *
 * @author sr115630
 *
 */
public class FflFunderCustomerVal extends RecordLifecycle {

    private static final FusionFileLogger FflFunderCustomerValLog = FusionFileLogger
            .getLogger(FflFunderCustomerVal.class);

    String funderAddress = "";
    String funderRef = "";
    String funderName="";
    @Override
    public void defaultFieldValues(String application, String currentRecordId, TStructure currentRecord,
            TStructure unauthorisedRecord, TStructure liveRecord, TransactionContext transactionContext) {
        DataAccess da = new DataAccess(this);

        EbFfCustomerFunderRecord ebfflCustomer = new EbFfCustomerFunderRecord(currentRecord);
        try {
            CustomerRecord cusRec = new CustomerRecord(da.getRecord("CUSTOMER", currentRecordId));
           
            funderName= cusRec.getShortName().get(0).getValue();
            FflFunderCustomerValLog.info("funderName" + funderName);

            try {
                funderAddress = cusRec.getStreet(0).getValue();
            } catch (Exception e) {
                e.getMessage();
            }          
            funderRef = cusRec.getMnemonic().getValue();
            ebfflCustomer.setFunderName(funderName);
            ebfflCustomer.setFunderAddress(funderAddress);
            ebfflCustomer.setFunderRef(funderRef);
            currentRecord.set(ebfflCustomer.toStructure());
            
        } catch (Exception e) {
            e.getMessage();
        }

    }

}
