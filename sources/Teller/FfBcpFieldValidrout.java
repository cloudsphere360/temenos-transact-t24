package com.temenos.fusion;

import java.math.BigDecimal;
import java.util.List;

import com.temenos.api.TStructure;
import com.temenos.api.TValidationResponse;
import com.temenos.t24.api.complex.eb.templatehook.TransactionContext;
import com.temenos.t24.api.hook.system.RecordLifecycle;
import com.temenos.t24.api.records.ebffcollpostingscreen.EbFfCollPostingScreenRecord;
import com.temenos.t24.api.records.ebffcollpostingscreen.EmployeeIdClass;
import com.temenos.t24.api.records.ebffrocollconcat.EbFfRoCollConcatRecord;
import com.temenos.t24.api.records.ebffrouser.EbFfRoUserRecord;
import com.temenos.t24.api.records.fundstransfer.FundsTransferRecord;
import com.temenos.t24.api.system.DataAccess;
import com.temenos.t24.api.system.Session;

/**
 * TODO: Document me!
 *
 * @author mp116300
 *
 */
public class FfBcpFieldValidrout extends RecordLifecycle {
    private static final FusionFileLogger FfBcpFieldValidrout = FusionFileLogger.getLogger(FfBcpFieldValidrout.class);
    DataAccess dataAccess = new DataAccess(this);
    Session session = new Session(this);

    EbFfCollPostingScreenRecord collectionRec = null;
    EbFfCollPostingScreenRecord prevcollectionRec = null;
    EbFfRoCollConcatRecord collConcatRec = null;

    String pendColl = "";
    BigDecimal pendCollBd = BigDecimal.ZERO;
    BigDecimal credAmtBd = BigDecimal.ZERO;
    String today = "";
    String companyId = "";
    String id = "";
    String ftCredAmount = "";
    String yid = "";
    String lastWorkingDay = "";
    String finMnemonic = "";
    String employeeId = "";
    String empName = "";
    String roNameComplete = "";
    String concatId = "";
    String credAmt = "";

    @Override
    public TValidationResponse validateRecord(String application, String currentRecordId, TStructure currentRecord,
            TStructure unauthorisedRecord, TStructure liveRecord, TransactionContext transactionContext) {
        FundsTransferRecord ftRec = new FundsTransferRecord(currentRecord);
        FfBcpFieldValidrout.info("ftRec : " + ftRec.toString());
        try {
            employeeId = ftRec.getLocalRefField("FF.POSTED.BY").getValue();
            empName = ftRec.getLocalRefField("FF.POST.LGLNAME").getValue();
            credAmt = ftRec.getCreditAmount().getValue();
            credAmtBd = new BigDecimal(credAmt);
            roNameComplete = employeeId + "/" + empName;
            today = session.getCurrentVariable("!TODAY");
            companyId = session.getCompanyId();

            lastWorkingDay = session.getCurrentVariable("!LAST.WORKING.DAY");

            finMnemonic = session.getCompanyRecord().getFinancialMne().getValue();

            FfBcpFieldValidrout.info("Today : " + today);
            FfBcpFieldValidrout.info("Company Id : " + companyId);
            FfBcpFieldValidrout.info("finMnemonic : " + finMnemonic);
            concatId = employeeId + "-" + companyId + "-" + today;
            id = companyId + "-" + today;
            yid = companyId + "-" + lastWorkingDay;

            FfBcpFieldValidrout.info("Constructed Record Id : " + id);
            FfBcpFieldValidrout.info("Constructed Record YID : " + yid);

            checkThevalidationForEmpId(employeeId, ftRec);

            try {
                collectionRec = new EbFfCollPostingScreenRecord(dataAccess.getRecord("EB.FF.COLL.POSTING.SCREEN", id));
                if (collectionRec != null) {
                    List<EmployeeIdClass> empIdListColl = collectionRec.getEmployeeId();
                    FfBcpFieldValidrout.info("empIdListColl " + empIdListColl.toString());
                    if (empIdListColl != null && !empIdListColl.isEmpty()) {
                        boolean matchFound = false;

                        for (EmployeeIdClass empIdObj : empIdListColl) {
                            if (empIdObj == null)
                                continue;

                            String empName = empIdObj.getEmployeeId().getValue();

                            if (roNameComplete.equals(empName)) {
                                matchFound = true;

                                BigDecimal cashReceivedBd = safeBigDecimal(empIdObj.getRoCashCollected().getValue());

                                BigDecimal cashAmt = getCashAmount();
                                BigDecimal bcpAmt = getBcpAmount();
                                BigDecimal prevPendAmt = getPrevPendingAmount();

                                BigDecimal pend = cashAmt.add(prevPendAmt).subtract(bcpAmt.add(cashReceivedBd));

                                if (credAmtBd.compareTo(pend) > 0) {
                                    ftRec.getCreditAmount().setError("EB-CREDAMT.VALID.ERROR");
                                }
                                break;
                            }
                        }

                        if (!matchFound) {
                            BigDecimal cashAmt = getCashAmount();
                            BigDecimal bcpAmt = getBcpAmount();
                            BigDecimal prevPendAmt = getPrevPendingAmount();

                            BigDecimal pend = cashAmt.add(prevPendAmt).subtract(bcpAmt);

                            if (credAmtBd.compareTo(pend) > 0 && pend.compareTo(BigDecimal.ZERO) != 0) {
                                ftRec.getCreditAmount().setError("EB-CREDAMT.VALID.ERROR");
                            }

                            if (pend.compareTo(BigDecimal.ZERO) == 0) {
                                ftRec.getLocalRefField("FF.POSTED.BY").setError("EB-RO.ELIG.VALID.ERROR");
                            }
                        }
                    }
                }

            } catch (Exception e) {
                FfBcpFieldValidrout.error("Exception Occured 125" + e.getMessage());
                BigDecimal cashReceivedBd = BigDecimal.ZERO;
                BigDecimal cashAmt = getCashAmount();
                BigDecimal bcpAmt = getBcpAmount();
                BigDecimal prevPendAmt = getPrevPendingAmount();
                BigDecimal firstSum = cashAmt.add(prevPendAmt);
                BigDecimal secSum = bcpAmt.add(cashReceivedBd);
                BigDecimal pend = firstSum.subtract(secSum);
                if (credAmtBd.compareTo(pend) > 0) {
                    ftRec.getCreditAmount().setError("EB-CREDAMT.VALID.ERROR");
                }
                if (pend.compareTo(BigDecimal.ZERO) == 0) {
                    ftRec.getLocalRefField("FF.POSTED.BY").setError("EB-RO.ELIG.VALID.ERROR");
                }
            }
        } catch (Exception e) {
            FfBcpFieldValidrout.error("Exception Occured 141 " + e.getMessage());
        }
        currentRecord.set(ftRec.toStructure());
        return ftRec.getValidationResponse();
    }

    /**
     * @return
     */
    private BigDecimal getPrevPendingAmount() {

        String pendColl = "";
        BigDecimal pendCollBd = BigDecimal.ZERO;

        try {
            FfBcpFieldValidrout.info("Financial Mnemonic getPrevPendingAmount " + finMnemonic);
            prevcollectionRec = new EbFfCollPostingScreenRecord(dataAccess.getRecord("EB.FF.COLL.POSTING.SCREEN", yid));
            FfBcpFieldValidrout.info("prev coll Id " + yid);
            FfBcpFieldValidrout.info("prev collRec " + prevcollectionRec.toString());
            List<EmployeeIdClass> empIdList = prevcollectionRec.getEmployeeId();
            FfBcpFieldValidrout.info("Employee Id List " + empIdList.toString());
            if (empIdList != null && !empIdList.isEmpty()) {
                for (int i = 0; i < empIdList.size(); i++) {
                    EmployeeIdClass empIdObj = empIdList.get(i);
                    FfBcpFieldValidrout.info("empIdObj " + empIdObj);
                    if (empIdObj == null)
                        continue;
                    String empName = empIdObj.getEmployeeId().getValue();
                    if (roNameComplete.equals(empName)) {

                        pendColl = empIdObj.getRoPendingCollection().getValue();
                        FfBcpFieldValidrout.info("pendColl " + pendColl);
                    }

                }
            }
            pendCollBd = safeBigDecimal(pendColl);
            FfBcpFieldValidrout.info("pendCollBd " + pendCollBd.toString());
            return pendCollBd;
        } catch (Exception e) {
            FfBcpFieldValidrout.error("Exception Occured 181 " + e.getMessage());
            pendCollBd = BigDecimal.ZERO;
            return pendCollBd;
        }

    }

    /**
     * @return
     */
    private BigDecimal getBcpAmount() {

        BigDecimal bcpAmtBd = BigDecimal.ZERO;
        try {
            FfBcpFieldValidrout.info("Financial Mnemonic getBcpAmount " + finMnemonic);
            collConcatRec = new EbFfRoCollConcatRecord(
                    dataAccess.getRecord(finMnemonic, "EB.FF.RO.COLL.CONCAT", "", concatId));
            FfBcpFieldValidrout.info("concat Id " + concatId);
            FfBcpFieldValidrout.info("collConcatRec " + collConcatRec.toString());
            String bcpAmt = collConcatRec.getBcPointCollected().getValue();
            FfBcpFieldValidrout.info("Bcp Amount retrieved " + bcpAmt);
            bcpAmtBd = safeBigDecimal(bcpAmt);
            return bcpAmtBd;

        } catch (Exception e) {
            FfBcpFieldValidrout.error("Exception Occured 206 " + e.getMessage());
            bcpAmtBd = BigDecimal.ZERO;
            return bcpAmtBd;
        }

    }

    /**
     * @return
     */
    private BigDecimal getCashAmount() {

        BigDecimal cashAmtBd = BigDecimal.ZERO;
        try {
            FfBcpFieldValidrout.info("Financial Mnemonic getCashAmount " + finMnemonic);
            collConcatRec = new EbFfRoCollConcatRecord(
                    dataAccess.getRecord(finMnemonic, "EB.FF.RO.COLL.CONCAT", "", concatId));
            FfBcpFieldValidrout.info("concat Id " + concatId);
            FfBcpFieldValidrout.info("collConcatRec " + collConcatRec.toString());
            String cashAmt = collConcatRec.getCashCollected().getValue();
            FfBcpFieldValidrout.info("Cash Amount retrieved " + cashAmt);
            cashAmtBd = safeBigDecimal(cashAmt);
            return cashAmtBd;

        } catch (Exception e) {
            FfBcpFieldValidrout.error("Exception Occured 231" + e.getMessage());
            cashAmtBd = BigDecimal.ZERO;
            return cashAmtBd;
        }

    }

    /**
     * @param employeeId
     * @param ftRec
     */
    private void checkThevalidationForEmpId(String employeeId, FundsTransferRecord ftRec) {
        FfBcpFieldValidrout.info("checkThevalidationForEmpId method is triggered" + employeeId);
        EbFfRoUserRecord roUserRec = null;
        if (!employeeId.isEmpty()) {
            FfBcpFieldValidrout.info("Entering if" + employeeId);
            try {
                roUserRec = new EbFfRoUserRecord(dataAccess.getRecord("EB.FF.RO.USER", employeeId));
                String roName = roUserRec.getRoName().getValue();
                String branchId = roUserRec.getBranchId().getValue();
                if ((!branchId.isEmpty()) && (branchId.equals(session.getCompanyId()))) {
                    FfBcpFieldValidrout.info("roName" + roName);
                    ftRec.getLocalRefField("FF.POST.LGLNAME").setValue(roName);
                } else {
                    ftRec.getLocalRefField("FF.POSTED.BY")
                            .setError("This RO is not allocated for this Branch" + session.getCompanyId());
                }
            } catch (Exception e) {
                FfBcpFieldValidrout.error("roUserRec doesn't exit" + e);
                ftRec.getLocalRefField("FF.POSTED.BY").setError("EB-EMPLOYEE.VAL.ERR");
            }
        }

    }

    private BigDecimal safeBigDecimal(String value) {
        if (value == null || value.trim().isEmpty()) {
            return BigDecimal.ZERO;
        }
        return new BigDecimal(value);
    }

}
