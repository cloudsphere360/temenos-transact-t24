package com.temenos.fusion;

import com.temenos.api.exceptions.T24CoreException;
import com.temenos.t24.api.complex.eb.templatehook.TransactionContext;
import com.temenos.t24.api.hook.system.RecordLifecycle;
import com.temenos.t24.api.system.Session;

/**
 * TODO: Document me!
 *
 * @author mv115891
 *
 */
public class FfTellerCheckIdRout extends RecordLifecycle {

    private static final FusionFileLogger FfTellerCheckIdRout = FusionFileLogger.getLogger(FfTellerCheckIdRout.class);

    @Override
    public String checkId(String currentRecordId, TransactionContext transactionContext) {

        Session ses = new Session(this);
        String currFunc = transactionContext.getCurrentFunction();
        FfTellerCheckIdRout.info("currFunc :" + currFunc);

        FfTellerCheckIdRout.info("currentRecordId :" + currentRecordId);
        String companycode = ses.getCompanyId();
        FfTellerCheckIdRout.info("companycode :" + companycode);

        String todayDate = ses.getCurrentVariable("!TODAY");
        FfTellerCheckIdRout.info("todayDate :" + todayDate);

        String id = companycode + "-" + todayDate;
        FfTellerCheckIdRout.info("id :" + id);

        if (currFunc.equals("INPUT")) {
            currentRecordId = id;
            if (!currentRecordId.equals(id)) {

                FfTellerCheckIdRout.info("entering id rtn if block");
                throw new T24CoreException("", "EB-ID");

            }

        }
        FfTellerCheckIdRout.info("currentRecordId before return :" + currentRecordId);
        return currentRecordId;

    }

}
