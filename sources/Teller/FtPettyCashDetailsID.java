package com.temenos.fusion;

import com.temenos.api.exceptions.T24CoreException;
import com.temenos.t24.api.complex.eb.templatehook.TransactionContext;
import com.temenos.t24.api.hook.system.RecordLifecycle;
import com.temenos.t24.api.records.dates.DatesRecord;
import com.temenos.t24.api.system.DataAccess;
import com.temenos.t24.api.system.Date;
import com.temenos.t24.api.system.Session;

/**
 * @author jh116454
 * @Attached To: VERSION > EB.FF.RO.PETTY.CASH.UPD,PETTY.CASH
 * @Attached As: ID Routine > EB.API > FF.PETTY.CASH.DETAILS.ID
 * @Description: Updating all the bills in the table
 *               EB.PETTY.CASH.TOTAL.UPD.ETD,INPUT
 * 
 */

public class FtPettyCashDetailsID extends RecordLifecycle {

    private static final FusionFileLogger yPettyCashDetailsIDLog = FusionFileLogger
            .getLogger(FtPettyCashDetailsID.class);
    DataAccess da = new DataAccess(this);

    @Override
    public String checkId(String currentRecordId, TransactionContext transactionContext) {

        Session ySession = new Session(this);
        String yCurrFunc = transactionContext.getCurrentFunction();

        String companycode = ySession.getCompanyId();

        Date yDate = new Date(this);
        DatesRecord yDateRec = yDate.getDates();
        String yTodDt = yDateRec.getToday().getValue();
        String id = companycode + "-" + yTodDt;
  
        if (yCurrFunc.equals("INPUT")) {

            if (currentRecordId.equals(id)) {
                throw new T24CoreException("", "Record ID already Exists");
            } else {
                currentRecordId = id;
                yPettyCashDetailsIDLog.info("Diff Rec ID ");
            }
        }
        return currentRecordId;
    }

}
