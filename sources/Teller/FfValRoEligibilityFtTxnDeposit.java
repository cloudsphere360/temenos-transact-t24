package com.temenos.fusion;

import com.temenos.api.TStructure;
import com.temenos.api.TValidationResponse;
import com.temenos.t24.api.complex.eb.templatehook.TransactionContext;
import com.temenos.t24.api.hook.system.RecordLifecycle;
import com.temenos.t24.api.records.ebffrocollconcat.EbFfRoCollConcatRecord;
import com.temenos.t24.api.records.ebffrouser.EbFfRoUserRecord;
import com.temenos.t24.api.records.fundstransfer.FundsTransferRecord;
import com.temenos.t24.api.system.DataAccess;
import com.temenos.t24.api.system.Session;

/**
 * -----------------------------------------------------------------------------
 * Modification History :
 * -----------------------------------------------------------------------------
 * Description : This Routine used to Validate RO Eligibility for BC/OBS
 * deposits
 *
 * Developed By : Harshini Sakthivel
 *
 * Development Reference : Teller Report
 *
 * Attached To : VERSION - FUNDS.TRANSFER,CASH.OBS.COLLECTION
 * >FF.FT.EMPLOYEE.VAL VERSION - FUNDS.TRANSFER,CASH.BCP.COLLECTION
 * >FF.FT.EMPLOYEE.VAL
 * 
 * Attached As : Input Routine
 * 
 * -----------------------------------------------------------------------------
 */
public class FfValRoEligibilityFtTxnDeposit extends RecordLifecycle {

    private static final FusionFileLogger roEligibilityFtTxnDeposit = FusionFileLogger
            .getLogger(FfValRoEligibilityFtTxnDeposit.class);

    DataAccess da = new DataAccess(this);
    Session ss = new Session(this);
    FundsTransferRecord ftRec = null;

    @Override
    public TValidationResponse validateRecord(String application, String currentRecordId, TStructure currentRecord,
            TStructure unauthorisedRecord, TStructure liveRecord, TransactionContext transactionContext) {
        roEligibilityFtTxnDeposit.info("FfValRoEligibilityFtTxnDeposit is triggered Successfully");

        EbFfRoCollConcatRecord roCollContactRec = null;
        ftRec = new FundsTransferRecord(currentRecord);
        String employeeId = ftRec.getLocalRefField("FF.POSTED.BY").getValue();
        String creditAmt = ftRec.getCreditAmount().getValue();
        String roCollectConcatId = employeeId + "-" + ss.getCompanyId() + "-" + ss.getCurrentVariable("!TODAY");
        roEligibilityFtTxnDeposit.info("roCollectConcatId" + roCollectConcatId);

        try {
            roCollContactRec = new EbFfRoCollConcatRecord(da.getRecord("EB.FF.RO.COLL.CONCAT", roCollectConcatId));
            roEligibilityFtTxnDeposit.info("roCollContactRec" + roCollContactRec.toString());
            String cashcollected = roCollContactRec.getCashCollected().getValue();
            if (cashcollected.isEmpty()) {
                ftRec.getLocalRefField("FF.POSTED.BY").setError("EB-RO.ELIGIBLE.VAL.ERR");
            } else if (Double.parseDouble(cashcollected) > 0) {
                double ftCreditAmount = Double.parseDouble(creditAmt);
                double rocashcollected = Double.parseDouble(cashcollected);
                if (ftCreditAmount > rocashcollected) {
                    ftRec.getCreditAmount().setError("EB-BC.DEPOSIT.AMT.VAL.ERR");
                }
            } else if (Double.parseDouble(cashcollected) == 0) {
                ftRec.getCreditAmount()
                        .setError("EB-RO.ELIGIBLE.VAL.ERR");
            }
            checkThevalidationForEmpId(employeeId);
        } catch (Exception e) {
            ftRec.getLocalRefField("FF.POSTED.BY").setError("EB-RO.ELIGIBLE.VAL.ERR");
        }
        currentRecord.set(ftRec.toStructure());

        return ftRec.getValidationResponse();
    }

    /**
     * @param employeeId
     */
    private void checkThevalidationForEmpId(String employeeId) {
        roEligibilityFtTxnDeposit.info("checkThevalidationForEmpId method is triggered" + employeeId);
        EbFfRoUserRecord roUserRec = null;
        if (!employeeId.isEmpty()) {
            roEligibilityFtTxnDeposit.info("Entering if" + employeeId);
            try {
                roUserRec = new EbFfRoUserRecord(da.getRecord("EB.FF.RO.USER", employeeId));
                String roName = roUserRec.getRoName().getValue();
                String branchId = roUserRec.getBranchId().getValue();
                if ((!branchId.isEmpty()) && (branchId.equals(ss.getCompanyId()))) {
                    roEligibilityFtTxnDeposit.info("roName" + roName);
                    ftRec.getLocalRefField("FF.POST.LGLNAME").setValue(roName);
                } else {
                    ftRec.getLocalRefField("FF.POSTED.BY")
                            .setError("this RO is not allocated for this Branch" + ss.getCompanyId());
                }
            } catch (Exception e) {
                roEligibilityFtTxnDeposit.error("roUserRec doesn't exit" + e);
                ftRec.getLocalRefField("FF.POSTED.BY").setError("EB-EMPLOYEE.VAL.ERR");
            }
        }

    }

}
