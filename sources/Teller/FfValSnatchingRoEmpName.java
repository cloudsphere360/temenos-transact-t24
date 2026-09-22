package com.temenos.fusion;

import java.util.List;

import com.temenos.api.TStructure;
import com.temenos.api.TValidationResponse;
import com.temenos.t24.api.complex.eb.templatehook.TransactionContext;
import com.temenos.t24.api.hook.system.RecordLifecycle;
import com.temenos.t24.api.records.dates.DatesRecord;
import com.temenos.t24.api.records.ebffcollpostingscreen.EbFfCollPostingScreenRecord;
import com.temenos.t24.api.records.ebffcollpostingscreen.EmployeeIdClass;
import com.temenos.t24.api.records.ebffrocollconcat.EbFfRoCollConcatRecord;
import com.temenos.t24.api.records.ebffrouser.EbFfRoUserRecord;
import com.temenos.t24.api.records.fundstransfer.FundsTransferRecord;
import com.temenos.t24.api.system.DataAccess;
import com.temenos.t24.api.system.Date;
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
 * >FF.FT.EMPLOYEE.VAL VERSION > FUNDS.TRANSFER,FF.SNATCHING.FRAUD.COLL
 * 
 * Attached As : Input Routine
 * 
 * -----------------------------------------------------------------------------
 */
public class FfValSnatchingRoEmpName extends RecordLifecycle {

    private static final FusionFileLogger snatchingRoEmpName = FusionFileLogger
            .getLogger(FfValSnatchingRoEmpName.class);

    DataAccess da = new DataAccess(this);
    Session ss = new Session(this);
    FundsTransferRecord ftRec = null;
    String yVersionID = "";
    
    @Override
    public TValidationResponse validateRecord(String application, String currentRecordId, TStructure currentRecord,
            TStructure unauthorisedRecord, TStructure liveRecord, TransactionContext transactionContext) {
        snatchingRoEmpName.info("FfValRoEligibilityFtTxnDeposit is triggered Successfully");

        ftRec = new FundsTransferRecord(currentRecord);
        yVersionID = transactionContext.getCurrentVersionId();


        EbFfRoCollConcatRecord roCollContactRec = null;

        String employeeId = ftRec.getLocalRefField("FF.POSTED.BY").getValue();
        String creditAmt = ftRec.getDebitAmount().getValue();

        String roCollectConcatId = employeeId + "-" + ss.getCompanyId() + "-" + ss.getCurrentVariable("!TODAY");
        snatchingRoEmpName.info("roCollectConcatId" + roCollectConcatId);

        try {
            roCollContactRec = new EbFfRoCollConcatRecord(da.getRecord("EB.FF.RO.COLL.CONCAT", roCollectConcatId));
            snatchingRoEmpName.info("roCollContactRec" + roCollContactRec.toString());
            String cashcollected = roCollContactRec.getCashCollected().getValue();
            if (cashcollected.isEmpty()) {
                ftRec.getLocalRefField("FF.POSTED.BY").setError("EB-RO.VAL.ERR");
            } else if (Double.parseDouble(cashcollected) > 0) {
                double ftCreditAmount = Double.parseDouble(creditAmt);
                double rocashcollected = Double.parseDouble(cashcollected);
                if (ftCreditAmount > rocashcollected) {
                    ftRec.getCreditAmount().setError("EB-BC.DEPOSIT.AMT.VAL.ERR");
                }
            } else if (Double.parseDouble(cashcollected) == 0) {
                ftRec.getCreditAmount().setError("EB-RO.VAL.ERR");
            }
            checkThevalidationForEmpId(employeeId);
        } catch (Exception e) {
            ftRec.getLocalRefField("FF.POSTED.BY").setError("EB-RO.VAL.ERR");
        }
        
        
        currentRecord.set(ftRec.toStructure());

        return ftRec.getValidationResponse();
    }

    /**
     * @param employeeId
     */
    private void checkThevalidationForEmpId(String employeeId) {
        snatchingRoEmpName.info("checkThevalidationForEmpId method is triggered" + employeeId);
        EbFfRoUserRecord roUserRec = null;
        if (!employeeId.isEmpty()) {
            snatchingRoEmpName.info("Entering if" + employeeId);
            try {
                roUserRec = new EbFfRoUserRecord(da.getRecord("EB.FF.RO.USER", employeeId));
                String roName = roUserRec.getRoName().getValue();
                snatchingRoEmpName.info("roName" + roName);
                String branchId = roUserRec.getBranchId().getValue();
                if ((!branchId.isEmpty()) && (branchId.equals(ss.getCompanyId()))) {
                    snatchingRoEmpName.info("roName" + roName);
                    ftRec.getLocalRefField("FF.POST.LGLNAME").setValue(roName);

                    if (yVersionID.equals(",FF.SNATCHING.FRAUD.COLL")) {
                        snatchingRoEmpName.error("inside Snatching Version");
                        getSnatcAmtVal();
                    }
                } else {
                    ftRec.getLocalRefField("FF.POSTED.BY")
                            .setError("this RO is not allocated for this Branch" + ss.getCompanyId());
                }
            } catch (Exception e) {
                snatchingRoEmpName.error("roUserRec doesn't exit" + e);
                ftRec.getLocalRefField("FF.POSTED.BY").setError("EB-EMPLOYEE.VAL.ERR");
            }
        }

    }

    private void getSnatcAmtVal() {
        try {
            Date date = new Date(this);
            DatesRecord dateRec = date.getDates();
            String yStrTodayVal = dateRec.getToday().getValue();

            String yCoCode = ss.getCompanyId();
            String yCollPostID = yCoCode + "-" + yStrTodayVal;
            snatchingRoEmpName.info(" yCollPostID -> " + yCollPostID);
            String yEmpVerID = ftRec.getLocalRefField("FF.POSTED.BY").getValue();
            snatchingRoEmpName.info(" yEmpVerID -> " + yEmpVerID);
            String yEmpVerName = ftRec.getLocalRefField("FF.POST.LGLNAME").getValue();
            snatchingRoEmpName.info(" yEmpVerName -> " + yEmpVerName);
            String yEmpIDNameVer = yEmpVerID +"/"+ yEmpVerName;
            snatchingRoEmpName.info(" yEmpIDName Version -> " + yEmpIDNameVer);
            
            EbFfCollPostingScreenRecord yEbCollPostRec = new EbFfCollPostingScreenRecord(
                    da.getRecord("EB.FF.COLL.POSTING.SCREEN", yCollPostID));

            List<EmployeeIdClass> yEmpIDList = yEbCollPostRec.getEmployeeId();

            for (int i = 0; i < yEmpIDList.size(); i++) {
                String yEmpIDname = yEmpIDList.get(i).getEmployeeId().getValue();
                snatchingRoEmpName.info(" yEmpID in Screen-> " + yEmpIDname);
                
                if (yEmpIDNameVer.equalsIgnoreCase(yEmpIDname)) {
                    String yRoPendCash = yEmpIDList.get(i).getRoPendingCollection().getValue();
                    snatchingRoEmpName.info(" yRoPendCash -> " + yRoPendCash);
                    if (yRoPendCash.equals("") || yRoPendCash.isEmpty() || yRoPendCash.equals("0.00")) {
                        snatchingRoEmpName.info("No Amt "); 
                        ftRec.getDebitAmount().setError("EB-SNAT.NOAMT.ERR");
                    } else {
                        snatchingRoEmpName.info("inside amt");
                        double yTotPendCashD = Double.parseDouble(yRoPendCash);
                        snatchingRoEmpName.info(" yTotPendCashD -> " + yTotPendCashD);
                        double yDebAmt = Double.parseDouble(ftRec.getDebitAmount().getValue());
                        snatchingRoEmpName.info(" yDebAmtB -> " + yDebAmt);
                        if (yDebAmt > yTotPendCashD) {
                            snatchingRoEmpName.info("yDebAmtB > yTotPendCashB");
                            ftRec.getDebitAmount().setError("EB-SNAT.COL.AMT.ERR");
                        }
                    }
                }
            }
        } catch (Exception e) {
            snatchingRoEmpName.error("Rec Error  -> " + e);
            ftRec.getDebitAmount().setError("EB-SNAT.COLL.ERR");
        }

    }

}
