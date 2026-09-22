package com.temenos.fusion;

import com.temenos.api.exceptions.T24CoreException;
import com.temenos.logging.facade.Logger;
import com.temenos.logging.facade.LoggerFactory;
import com.temenos.t24.api.complex.eb.templatehook.TransactionContext;
import com.temenos.t24.api.hook.system.RecordLifecycle;
import com.temenos.t24.api.system.Session;

/*-----------------------------------------------------------------------------
* @author Deepakumar S
* Date Created: 27-01-2026
* Attached as : ID ROUTINE
* EB.API>FF.IDCREDIT.BUREAU.REPORT
* VERSION>EB.FF.CREDIT.BUREAU.REPORT,LOAN.CORRECTION
* VERSION>EB.FF.CREDIT.BUREAU.REPORT,INTERIM
* Description: ID Routine for the version
*-----------------------------------------------------------------------------*/
public class FfIdCreditBureauReport extends RecordLifecycle {

    private static final String L3API = "L3API";
    private static final Logger LOGGER = LoggerFactory.getLogger(L3API);

    @Override
    public String checkId(String currentRecordId, TransactionContext transactionContext)
            throws T24CoreException {

        Session ses = new Session(this);
        String inpFun = transactionContext.getCurrentFunction();

        if ("INPUT".equals(inpFun)) {

            String currVersion = transactionContext.getCurrentVersionId().toString();
            LOGGER.info("currVersion :" + currVersion);

            String todayDate = ses.getCurrentVariable("!TODAY");
            todayDate = todayDate.replace("-", "").replace("/", "");
            LOGGER.info("todayDate :" + todayDate);

            if (currVersion.equals(",INTERIM")) {

                currentRecordId = todayDate + "-INTERIM";
                LOGGER.info("Generated ID :" + currentRecordId);

            } else if (currVersion.equals(",LOAN.CORRECTION")) {

                currentRecordId = todayDate + "-LOANS";
                LOGGER.info("Generated ID :" + currentRecordId);
            }
        }

        LOGGER.info("currentRecordId before return :" + currentRecordId);
        return currentRecordId;
    }
}
