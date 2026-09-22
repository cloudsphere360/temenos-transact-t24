package com.temenos.fusion;

import com.temenos.api.exceptions.T24CoreException;
import com.temenos.t24.api.complex.eb.templatehook.TransactionContext;
import com.temenos.t24.api.hook.system.RecordLifecycle;
import com.temenos.t24.api.records.ebffcustomerfunder.EbFfCustomerFunderRecord;
import com.temenos.t24.api.records.ebfffunderdetails.EbFfFunderDetailsRecord;
import com.temenos.t24.api.system.DataAccess;

/**
 * 
 * @author sr115630
 *
 */
public class FfFundrCusIdCheck extends RecordLifecycle {
    String applicationName = "";
    DataAccess da = new DataAccess();
    private static final String EB_FUNDER_ID_CHECK = "EB-FUNDER.CUST.ID.CHECK";
    private static final FusionFileLogger FF_FUNDER_CUS_ID_CHECK = FusionFileLogger.getLogger(FfFundrCusIdCheck.class);

    @Override
    public String checkId(String currentRecordId, TransactionContext transactionContext) {
        String ofsSource = transactionContext.getOfsSourceID();
        FF_FUNDER_CUS_ID_CHECK.info("ofsSource" + ofsSource);
        if (!ofsSource.equals("FF.FUNDER.PROCESS")) {
            applicationName = transactionContext.getApplicationName();
            if (applicationName.equals("EB.FF.CUSTOMER.FUNDER")) {

                try {
                    EbFfCustomerFunderRecord ebFfCustomerFun = new EbFfCustomerFunderRecord(
                            da.getRecord("EB.FF.CUSTOMER.FUNDER", currentRecordId));
                    FF_FUNDER_CUS_ID_CHECK.info("ebFfCustomerFun" + ebFfCustomerFun);
                } catch (Exception e) {
                    throw new T24CoreException("", EB_FUNDER_ID_CHECK);
                }
            } else if (applicationName.equals("EB.FF.FUNDER.DETAILS")) {
                try {
                    EbFfFunderDetailsRecord ebFfunderDel = new EbFfFunderDetailsRecord(
                            da.getRecord("EB.FF.FUNDER.DETAILS", currentRecordId));
                    FF_FUNDER_CUS_ID_CHECK.info("ebFfunderDel" + ebFfunderDel);

                } catch (Exception e) {
                    throw new T24CoreException("", EB_FUNDER_ID_CHECK);
                }

            }
        }
        return currentRecordId;
    }

}
