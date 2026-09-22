package com.temenos.fusion;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import com.temenos.api.TStructure;
import com.temenos.t24.api.complex.eb.templatehook.TransactionContext;
import com.temenos.t24.api.hook.system.RecordLifecycle;
import com.temenos.t24.api.records.ebffcollpostingscreen.EbFfCollPostingScreenRecord;
import com.temenos.t24.api.records.ebffcollpostingscreen.EmployeeIdClass;
import com.temenos.t24.api.system.DataAccess;
import com.temenos.t24.api.system.Session;

/**
 * TODO: Document me!
 *
 * @author mp116300
 *
 */
public class FfPrevPendCollDefRout extends RecordLifecycle {

    private static final FusionFileLogger FfPrevPendCollDefRout = FusionFileLogger
            .getLogger(FfPrevPendCollDefRout.class);

    private final DataAccess da = new DataAccess(this);
    private final Session ses = new Session(this);

    @Override
    public void defaultFieldValues(String application, String currentRecordId, TStructure currentRecord,
            TStructure unauthorisedRecord, TStructure liveRecord, TransactionContext transactionContext) {

        try {

            EbFfCollPostingScreenRecord ebCollRec = null;
            BigDecimal totCash = BigDecimal.ZERO;
            BigDecimal totDigital = BigDecimal.ZERO;
            BigDecimal totBcPoint = BigDecimal.ZERO;
            BigDecimal totPendingCash = BigDecimal.ZERO;
            BigDecimal totCollected = BigDecimal.ZERO;
            BigDecimal totPrevPending = BigDecimal.ZERO;
            try {
                ebCollRec = new EbFfCollPostingScreenRecord(currentRecord);
            } catch (Exception e) {
                e.getMessage();
            }

            String companyCode = ses.getCompanyId();

            String yestDate = ses.getCurrentVariable("!LAST.WORKING.DAY");
            String yestId = companyCode + "-" + yestDate;
            FfPrevPendCollDefRout.info("yestId " + yestId);
            try {
                EbFfCollPostingScreenRecord yestRec = new EbFfCollPostingScreenRecord(
                        da.getRecord("EB.FF.COLL.POSTING.SCREEN", yestId));
                FfPrevPendCollDefRout.info("yest Collection Record " + yestRec.toString());

                List<EmployeeIdClass> yestRecList = yestRec.getEmployeeId();
                int size = yestRecList.size();

                for (int i = 0; i < size; i++) {
                    EmployeeIdClass yestRecObj = yestRecList.get(i);
                    String empIdYest = yestRecObj.getEmployeeId().getValue();
                    FfPrevPendCollDefRout.info("yest Collection Employee Id " + empIdYest);
                    String prevPendingYest = yestRecObj.getRoPendingCollection().toString();
                    FfPrevPendCollDefRout.info("yest Prev Pending Collection " + prevPendingYest);

                    updateTodaysRec(ebCollRec, empIdYest, prevPendingYest);

                }

                List<EmployeeIdClass> empIdList = ebCollRec.getEmployeeId();
                if (!empIdList.equals(null)) {
                    for (int i = 0; i < empIdList.size(); i++) {
                        EmployeeIdClass exist = empIdList.get(i);
                        FfPrevPendCollDefRout.info("exist " + exist.toString());
                        BigDecimal prevpend = safeBigDecimal(normalizeNumber(exist.getPrevPendingCollection().getValue()));
                        FfPrevPendCollDefRout.info("prevpend " + prevpend.toString());
                        BigDecimal cashAmt = safeBigDecimal(normalizeNumber(exist.getRoCashAmend().getValue()));
                        FfPrevPendCollDefRout.info("cashAmt " + cashAmt.toString());
                        BigDecimal bcpBank = safeBigDecimal(normalizeNumber(exist.getDepositAtBcpoint().getValue()));
                        FfPrevPendCollDefRout.info("bcpBank " + bcpBank.toString());
                        BigDecimal cashByBm = safeBigDecimal(normalizeNumber(exist.getRoCashCollected().getValue()));
                        FfPrevPendCollDefRout.info("cashByBm " + cashByBm.toString());

                        BigDecimal pending = prevpend.add(cashAmt).subtract(bcpBank.add(cashByBm));
                        FfPrevPendCollDefRout.info("pending " + pending.toString());
                        exist.setRoPendingCollection(pending.toString());
                        ebCollRec.setEmployeeId(exist, i);

                    }
                }

                List<EmployeeIdClass> finalEmpIdList = ebCollRec.getEmployeeId();
                if (!finalEmpIdList.equals(null)) {
                    for (EmployeeIdClass finalObject : finalEmpIdList) {

                        totCash = totCash.add(safeBigDecimal(normalizeNumber(finalObject.getRoCashCollected().getValue())));
                        FfPrevPendCollDefRout
                                .info("final Object cash by BM value: " + finalObject.getRoCashCollected().getValue());

                        totDigital = totDigital.add(safeBigDecimal(normalizeNumber(finalObject.getDigitalCollection().getValue())));
                        FfPrevPendCollDefRout
                                .info("final Object digital value: " + finalObject.getDigitalCollection().getValue());

                        totBcPoint = totBcPoint.add(safeBigDecimal(normalizeNumber(finalObject.getDepositAtBcpoint().getValue())));
                        FfPrevPendCollDefRout
                                .info("final Object bcpoint value: " + finalObject.getDepositAtBcpoint().getValue());
                        totPrevPending = totPrevPending
                                .add(safeBigDecimal(normalizeNumber(finalObject.getPrevPendingCollection().getValue())));
                        FfPrevPendCollDefRout.info("final Object prev pending value: "
                                + finalObject.getPrevPendingCollection().getValue());

                        totPendingCash = totPendingCash
                                .add(safeBigDecimal(normalizeNumber(finalObject.getRoPendingCollection().getValue())));
                        FfPrevPendCollDefRout.info(
                                "final Object totcollection value: " + finalObject.getRoPendingCollection().getValue());

                    }
                }
                totCollected = totCash.add(totDigital).add(totBcPoint).add(totPendingCash);
                FfPrevPendCollDefRout.info("totCollected FfPrevPendCollDefRout " + totCollected.toString());
                ebCollRec.setTotalPendingCash(totPendingCash.toString());
                ebCollRec.setTotalCollectedAmt(totCollected.toString());
                ebCollRec.setTotPrevPendingCollection(totPrevPending.toString());

            } catch (Exception e) {
                e.getMessage();
            }

            currentRecord.set(ebCollRec.toStructure());

        } catch (Exception e) {
            e.getMessage();
        }

    }

    /**
     * @param ebCollRec
     * @param empIdYest
     * @param prevPendingYest
     */
    private void updateTodaysRec(EbFfCollPostingScreenRecord ebCollRec, String empIdYest, String prevPendingYest) {

        List<EmployeeIdClass> collList = ebCollRec.getEmployeeId();
        FfPrevPendCollDefRout.info("collList inside update method " + collList);

        // Ensure list is initialized properly
        if (collList == null) {
            collList = new ArrayList<>();
        }

        boolean updated = false;

        for (int i = 0; i < collList.size(); i++) {

            EmployeeIdClass existing = collList.get(i);

            if (existing == null || existing.getEmployeeId() == null) {
                continue;
            }

            String existEmployee = existing.getEmployeeId().getValue();

            FfPrevPendCollDefRout.info("yesterday employee id " + empIdYest);
            FfPrevPendCollDefRout.info("existing employee id " + existEmployee);

            // Safe comparison
            if (existEmployee != null && existEmployee.equals(empIdYest)) {

                FfPrevPendCollDefRout.info("Matched employee. Updating Prev Pending");
                FfPrevPendCollDefRout.info("Prev Pending Yesterday " + prevPendingYest);

                // IMPORTANT: Update existing object (DO NOT replace it)
                existing.setPrevPendingCollection(prevPendingYest);

                // Put back updated object
                ebCollRec.setEmployeeId(existing, i);

                updated = true;
                break;
            }
        }

        // Add new entry ONLY if not found
        if (!updated) {

            FfPrevPendCollDefRout.info("Employee not found. Adding new MV entry");

            EmployeeIdClass empIdObj = new EmployeeIdClass();

            empIdObj.setEmployeeId(empIdYest);
            empIdObj.setPrevPendingCollection(prevPendingYest);

            ebCollRec.setEmployeeId(empIdObj, collList.size());
        }
    }

    private BigDecimal safeBigDecimal(String value) {
        try {
            if (value == null) {
                return BigDecimal.ZERO;
            }

            value = value.trim();

            if (value.isEmpty()) {
                return BigDecimal.ZERO;
            }

            if ("null".equalsIgnoreCase(value)) {
                return BigDecimal.ZERO;
            }

            // Step 5: Remove formatting commas (CRITICAL FIX)
            // Example: "2,000.00" → "2000.00"
            value = value.replace(",", "");

            return new BigDecimal(value);

        } catch (Exception e) {
            FfPrevPendCollDefRout.info("Invalid number format for value: [" + value + "]");
            return BigDecimal.ZERO;
        }
    }

    private String normalizeNumber(String value) {
        if (value == null || value.trim().isEmpty() || "null".equalsIgnoreCase(value.trim())) {
            return "0";
        }
        return value.replace(",", "").trim();
    }
}
