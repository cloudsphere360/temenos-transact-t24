package com.temenos.fusion;

import com.temenos.t24.api.system.DataAccess;
import com.temenos.api.exceptions.T24CoreException;
import com.temenos.t24.api.complex.eb.templatehook.TransactionContext;
import com.temenos.t24.api.hook.system.RecordLifecycle;
import com.temenos.t24.api.records.customer.CustomerRecord;

/**
 * TODO: Document me!
 *
 * @author sr115630
 *
 */
public class FfCusFunderId extends RecordLifecycle {

    private static final FusionFileLogger FfCusFunderIdLog = FusionFileLogger.getLogger(FfCusFunderId.class);
    DataAccess da = new DataAccess(this);
    private static final String EB_CUSTOMER_FUNDER = "EB-CUSTOMER.FUNDER";
    @Override
    public String checkId(String currentRecordId, TransactionContext transactionContext) {

        String yCurrFunc = transactionContext.getCurrentFunction();

        String yCustID = currentRecordId;

        if (yCurrFunc.equals("INPUT")) {
            CustomerRecord yCustRec = null;
            try {
                yCustRec = new CustomerRecord(da.getRecord("CUSTOMER", yCustID));
                String yMne = yCustRec.getMnemonic().getValue();
                FfCusFunderIdLog.info("Record present " + yMne);
            } catch (Exception e) {
                FfCusFunderIdLog.info("No Record Present");
            }

            if (yCustRec != null && !yCustRec.toString().trim().isEmpty()) {
                currentRecordId = yCustID;
                FfCusFunderIdLog.info("New Rec Open");   
            } else {
                FfCusFunderIdLog.info("Throw error");
                throw new T24CoreException("", EB_CUSTOMER_FUNDER);
                
            }
        }

        return currentRecordId;
    }

}
