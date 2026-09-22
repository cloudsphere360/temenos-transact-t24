package com.temenos.fusion;

import com.temenos.t24.api.hook.system.RecordLifecycle;
import com.temenos.t24.api.complex.eb.templatehook.TransactionContext;
import com.temenos.api.exceptions.T24CoreException;
import com.temenos.t24.api.system.DataAccess;
import com.temenos.t24.api.system.Session;

/**
 * @author jh116454
 * @Attached To: VERSION > EB.PETTY.CASH.LIMIT,PETTY.CASH
 * @Attached As: ID routine > EB.API > FF.PETTY.LIMIT.CHECK.ID
 * @Description: To default @ID (only company code) in the version
 * 
 */

public class PettyLimitCheckID extends RecordLifecycle {

    private static final FusionFileLogger yPettyLimitCheckIDLog = FusionFileLogger.getLogger(PettyLimitCheckID.class);
    DataAccess da = new DataAccess(this);

    @Override
    public String checkId(String currentRecordId, TransactionContext transactionContext) {

        Session ySession = new Session(this);
        String yCurrFunc = transactionContext.getCurrentFunction();

        String companycode = ySession.getCompanyId();

        String id = companycode;

        if ((yCurrFunc.equals("INPUT") && (!currentRecordId.equals(id)))) {
            yPettyLimitCheckIDLog.info("entering id rtn if block");
            throw new T24CoreException("", "EB-ID");
        }

        currentRecordId = id;
        return currentRecordId;
    }

}
