package com.temenos.fusion;

import com.temenos.api.TStructure;
import com.temenos.api.TValidationResponse;
import com.temenos.api.exceptions.T24CoreException;
import com.temenos.t24.api.complex.eb.templatehook.TransactionContext;
import com.temenos.t24.api.hook.system.RecordLifecycle;
import com.temenos.t24.api.records.ebffrouser.EbFfRoUserRecord;
import com.temenos.t24.api.records.fundstransfer.FundsTransferRecord;
import com.temenos.t24.api.system.DataAccess;

/**
 *
 * @author hs115664
 *
 */
public class FfTellerValEmployeeId extends RecordLifecycle {

    private static final FusionFileLogger tellerValEmployeeId = FusionFileLogger.getLogger(FfTellerValEmployeeId.class);

    DataAccess da = new DataAccess(this);
    EbFfRoUserRecord roUserRec = null;

    @Override
    public TValidationResponse validateField(String application, String recordId, String fieldData, TStructure currentrecord) {
        tellerValEmployeeId.info("FfTellerValEmployeeId is triggered");
        FundsTransferRecord ftRec = new FundsTransferRecord(currentrecord);

        String postedBy = ftRec.getLocalRefField("FF.POSTED.BY").getValue();
        tellerValEmployeeId.info("postedBy" + postedBy);
        if (!postedBy.isEmpty()) {
            tellerValEmployeeId.info("Entering if" + postedBy);
            try {
                roUserRec = new EbFfRoUserRecord(da.getRecord("EB.FF.RO.USER", postedBy));
                String roName = roUserRec.getRoName().getValue();
                tellerValEmployeeId.info("roName" + roName);
                ftRec.getLocalRefField("FF.POST.LGLNAME").setValue(roName);
            } catch (Exception e) {
                tellerValEmployeeId.error("roUserRec doesn't exit" + e);
                ftRec.getLocalRefField("FF.POSTED.BY").setError("EB-EMPLOYEE.VAL.ERR");
            }
        }
        currentrecord.set(ftRec.toStructure());
        tellerValEmployeeId.info("currentRecord" + currentrecord);
        return ftRec.getValidationResponse();
    }

}
