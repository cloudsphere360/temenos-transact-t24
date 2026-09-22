package com.temenos.fusion;

import com.temenos.api.TStructure;
import com.temenos.t24.api.complex.eb.templatehook.TransactionContext;
import com.temenos.t24.api.hook.system.RecordLifecycle;
import com.temenos.t24.api.records.customer.CustomerRecord;
import com.temenos.t24.api.records.ebfffunderdetails.EbFfFunderDetailsRecord;
import com.temenos.t24.api.system.DataAccess;

/**
 * TODO: Document me!
 *
 * @author sr115630
 *
 */
public class FflCustomerFunderVal extends RecordLifecycle {
    String funderAddress = "";
    String funderRef = "";
    String funderName="";

    @Override
    public void defaultFieldValues(String application, String currentRecordId, TStructure currentRecord,
            TStructure unauthorisedRecord, TStructure liveRecord, TransactionContext transactionContext) {
        DataAccess da = new DataAccess(this);
        EbFfFunderDetailsRecord ebfflCustomer = new EbFfFunderDetailsRecord(currentRecord);
        try {
            CustomerRecord cusRec = new CustomerRecord(da.getRecord("CUSTOMER", currentRecordId));
           
            funderName= cusRec.getShortName().get(0).getValue();          
            funderRef = cusRec.getMnemonic().getValue();
            ebfflCustomer.setFunderName(funderName);
            
            ebfflCustomer.setFunderRef(funderRef);
            currentRecord.set(ebfflCustomer.toStructure());
            
        } catch (Exception e) {
            e.getMessage();
        }
    }

    
    
}
