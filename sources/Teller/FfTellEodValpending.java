package com.temenos.fusion;

import java.math.BigDecimal;
import java.util.List;

import com.temenos.api.TStructure;
import com.temenos.api.TValidationResponse;
import com.temenos.t24.api.complex.eb.templatehook.TransactionContext;
import com.temenos.t24.api.hook.system.RecordLifecycle;
import com.temenos.t24.api.records.ebffcollpostingscreen.EbFfCollPostingScreenRecord;
import com.temenos.t24.api.records.ebffcollpostingscreen.EmployeeIdClass;
import com.temenos.t24.api.records.ebffeodscreen.EbFfEodScreenRecord;
import com.temenos.t24.api.system.DataAccess;
import com.temenos.t24.api.system.Session;

/**
 * TODO: Document me!
 *
 * @author mp116300
 *
 */
public class FfTellEodValpending extends RecordLifecycle {

    private static final FusionFileLogger FfTellEodValpending = FusionFileLogger.getLogger(FfTellEodValpending.class);
    private final DataAccess da = new DataAccess(this);
    private final Session ses = new Session(this);

    @Override
    public TValidationResponse validateRecord(String application, String currentRecordId, TStructure currentRecord,
            TStructure unauthorisedRecord, TStructure liveRecord, TransactionContext transactionContext) {

        EbFfEodScreenRecord eodScreenCurrRec = new EbFfEodScreenRecord(currentRecord);
        EbFfCollPostingScreenRecord ebCollScreenRec = null;

        try {
            FfTellEodValpending.info("FfTellEodValpending routine started <3");

            String companyCode = ses.getCompanyId();
            String todayDate = ses.getCurrentVariable("!TODAY");
            String id = companyCode + "-" + todayDate;
            FfTellEodValpending.info("Id for the Collection Screen " + id);

            try {
                ebCollScreenRec = new EbFfCollPostingScreenRecord(da.getRecord("EB.FF.COLL.POSTING.SCREEN", id));
                FfTellEodValpending.info("ebCollScreenRec " + ebCollScreenRec.toString());
            } catch (Exception e) {
                FfTellEodValpending.error("Error loading EB.FF.COLL.POSTING.SCREEN for id " + id, e);
            }

            StringBuilder allErrors = new StringBuilder();

            if (ebCollScreenRec != null) {
                List<EmployeeIdClass> empIdListColl = ebCollScreenRec.getEmployeeId();
                FfTellEodValpending.info("empIdListColl " + empIdListColl.toString());
                if (empIdListColl != null && !empIdListColl.isEmpty()) {
                    for (int i = 0; i < empIdListColl.size(); i++) {
                        EmployeeIdClass empIdObj = empIdListColl.get(i);
                        FfTellEodValpending.info("empIdObj " + empIdObj);
                        if (empIdObj == null)
                            continue;
                        String pendColl = safe(empIdObj.getRoPendingCollection().toString());
                        FfTellEodValpending.info("pendColl " + pendColl);
                        String empId = safe(empIdObj.getEmployeeId().toString());
                        FfTellEodValpending.info("empId " + empId);
                        String empName = safe(empIdObj.getRoName().toString());
                        FfTellEodValpending.info("empName " + empName);
                        BigDecimal pendCollBd = safeBigDecimal(pendColl);

                        if (pendCollBd.compareTo(BigDecimal.ZERO) > 0) {
                            // Accumulate every error
                            if (allErrors.length() > 0)
                                allErrors.append("\n");
                            allErrors.append("Pending Collection ").append(pendColl).append(" exists for ")
                                    .append(empId).append(" - ").append(empName);
                            FfTellEodValpending.info("allErrors " + allErrors.toString());
                        }
                    }
                }
            }

            String pendCash = eodScreenCurrRec.getTotalPendingCash().getValue();
            FfTellEodValpending.info("Pending Amount EOD " + pendCash);
            String controlAmount = eodScreenCurrRec.getSuspenseAmount().getValue();
            FfTellEodValpending.info("Control Amount EOD " + controlAmount);
            // Set one aggregated error so ALL violations are visible
            if (allErrors.length() > 0 && (!pendCash.equals(controlAmount))) {
                FfTellEodValpending.info("Entering overall if " + allErrors.toString());
                eodScreenCurrRec.getPendingCollections().setError(allErrors.toString());
            }

        } catch (Exception e) {
            FfTellEodValpending.error("Unexpected error in validateRecord", e);
            eodScreenCurrRec.getPendingCollections().setError("Validation failed due to a technical error.");
        }

        currentRecord.set(eodScreenCurrRec.toStructure());
        return eodScreenCurrRec.getValidationResponse();
    }

    // helpers
    private static String safe(String s) {
        return (s == null) ? "" : s.trim();
    }

    private static BigDecimal safeBigDecimal(String s) {
        try {
            return (s == null || s.trim().isEmpty()) ? BigDecimal.ZERO : new BigDecimal(s.trim());
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

}
