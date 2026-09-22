package com.temenos.fusion;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import com.temenos.api.TField;
import com.temenos.api.TStructure;
import com.temenos.t24.api.complex.eb.templatehook.TransactionContext;
import com.temenos.t24.api.hook.system.RecordLifecycle;
import com.temenos.t24.api.records.dates.DatesRecord;
import com.temenos.t24.api.records.ebffcollpostingscreen.EbFfCollPostingScreenRecord;
import com.temenos.t24.api.records.ebffcollpostingscreen.EmployeeIdClass;
import com.temenos.t24.api.records.ebffeodrodets.EbFfEodRoDetsRecord;
import com.temenos.t24.api.records.ebffrocollconcat.EbFfRoCollConcatRecord;
import com.temenos.t24.api.records.ebsnatcfraudcollscrn.EbSnatcFraudCollScrnRecord;
import com.temenos.t24.api.records.ebsnatcfraudcollscrn.TxnDateClass;
import com.temenos.t24.api.system.DataAccess;
import com.temenos.t24.api.system.Session;

public class FfTellerDefaultRout extends RecordLifecycle {

    private static final FusionFileLogger FfTellerDefaultRout = FusionFileLogger.getLogger(FfTellerDefaultRout.class);

    private final DataAccess da = new DataAccess(this);
    private final Session ses = new Session(this);

    @Override
    public void defaultFieldValues(String application, String currentRecordId, TStructure currentRecord,
            TStructure unauthorisedRecord, TStructure liveRecord, TransactionContext transactionContext) {

        try {
            BigDecimal totCash = BigDecimal.ZERO;
            BigDecimal totDigital = BigDecimal.ZERO;
            BigDecimal totPendingCash = BigDecimal.ZERO;
            BigDecimal totCollected = BigDecimal.ZERO;
            BigDecimal totBcPoint = BigDecimal.ZERO;
            BigDecimal totCashAmend = BigDecimal.ZERO;
            BigDecimal totPrevPending = BigDecimal.ZERO;
            EbFfCollPostingScreenRecord collposting = null;
            FfTellerDefaultRout.info("Current Rec Id " + currentRecordId);
            FfTellerDefaultRout.info("Current Record TStructure " + currentRecord.toString().toString());
            try {
                collposting = new EbFfCollPostingScreenRecord(currentRecord);
            } catch (Exception e) {
                e.getMessage();
            }
            String companyCode = ses.getCompanyId();
            // String todayDate = ses.getCurrentVariable("!TODAY");
            String todayDate = "";
            String yestDate = "";
            TStructure datesRec = da.getRecord("DATES", companyCode);
            DatesRecord datesRecord = new DatesRecord(datesRec);
            String coBatchStatus = datesRecord.getCoBatchStatus().getValue();

            FfTellerDefaultRout.info("datesRec is : " + datesRec.toString());
            FfTellerDefaultRout.info("datesRecord is : " + datesRecord.toString());
            FfTellerDefaultRout.info("coBatchStatus is : " + coBatchStatus.toString());

            // -------------------------------------------------
            // IF BATCH STATUS = O
            // USE CURRENT COMPANY DATES RECORD
            // -------------------------------------------------

            if ("O".equalsIgnoreCase(coBatchStatus)) {

                todayDate = datesRecord.getToday().getValue();
                yestDate = datesRecord.getLastWorkingDay().getValue();

                FfTellerDefaultRout.info("Using ONLINE dates record");

            } else {
                // -------------------------------------------------
                // ELSE READ COB COMPANY DATES RECORD
                // Example: IN-001-0001-COB
                // -------------------------------------------------
                String cobCompany = companyCode + "-COB";

                FfTellerDefaultRout.info("Reading COB dates record : " + cobCompany);

                TStructure cobDatesRec = da.getRecord("DATES", cobCompany);

                DatesRecord cobDatesRecord = new DatesRecord(cobDatesRec);

                todayDate = cobDatesRecord.getToday().getValue();
                yestDate = cobDatesRecord.getLastWorkingDay().getValue();

                FfTellerDefaultRout.info("Using COB dates record");
            }
            String fMnemonic = ses.getCompanyRecord().getFinancialMne().toString();
            String id = companyCode + "-" + todayDate;
            FfTellerDefaultRout.info("TodayId " + id);
            // String yestDate = ses.getCurrentVariable("!LAST.WORKING.DAY");
            String yestId = companyCode + "-" + yestDate;
            FfTellerDefaultRout.info("yestId " + yestId);
            EbFfEodRoDetsRecord rodets = null;
            EbFfRoCollConcatRecord conCat = null;

            try {
                EbFfCollPostingScreenRecord yestRec = new EbFfCollPostingScreenRecord(
                        da.getRecord("EB.FF.COLL.POSTING.SCREEN", yestId));
                FfTellerDefaultRout.info("yest Collection Record " + yestRec.toString());

                List<EmployeeIdClass> yestRecList = yestRec.getEmployeeId();
                int size = yestRecList.size();

                for (int i = 0; i < size; i++) {
                    EmployeeIdClass yestRecObj = yestRecList.get(i);
                    String empIdYest = yestRecObj.getEmployeeId().getValue();
                    FfTellerDefaultRout.info("yest Collection Employee Id " + empIdYest);
                    String prevPendingYest = yestRecObj.getRoPendingCollection().toString();
                    FfTellerDefaultRout.info("yest Prev Pending Collection " + prevPendingYest);

                    updateTodaysRec(collposting, empIdYest, prevPendingYest, id);

                }
            } catch (Exception e) {
                e.getMessage();
            }
            FfTellerDefaultRout.info("collposting Record after Prev pending update " + collposting.toString());
            try {
                rodets = new EbFfEodRoDetsRecord(da.getRecord(fMnemonic, "EB.FF.EOD.RO.DETS", "", id));
                List<TField> roCollList = rodets.getRoCollectionConcat();
                if (rodets != null) {
                    for (TField roField : roCollList) {
                        String roStr = roField.getValue();
                        try {
                            conCat = new EbFfRoCollConcatRecord(
                                    da.getRecord(fMnemonic, "EB.FF.RO.COLL.CONCAT", "", roStr));
                        } catch (Exception e) {
                            e.getMessage();
                        }
                        FfTellerDefaultRout.info("conCat " + conCat.toString());
                        String empId = conCat.getRoEmpId().toString();
                        FfTellerDefaultRout.info("empId " + empId);
                        String empName = conCat.getRoEmpName().toString();
                        FfTellerDefaultRout.info("empName " + empName);
                        String cashCollect = normalizeNumber(conCat.getCashCollected().toString());
                        FfTellerDefaultRout.info("cashCollect " + cashCollect);
                        String digital = normalizeNumber(conCat.getDigitalCollected().toString());
                        FfTellerDefaultRout.info("digital " + digital);
                        String totalCollect = normalizeNumber(conCat.getTotalCollected().toString());
                        FfTellerDefaultRout.info("totalCollect " + totalCollect);
                        String bcPoint = normalizeNumber(conCat.getBcPointCollected().toString());
                        FfTellerDefaultRout.info("bcPoint " + bcPoint);
                        String otherBank = normalizeNumber(conCat.getOtherBankCollected().toString());
                        FfTellerDefaultRout.info("otherBank " + otherBank);

                        updatePostingTable(collposting, empId, empName, cashCollect, digital, totalCollect, bcPoint,
                                otherBank, id);
                    }
                }
            } catch (Exception e) {
                FfTellerDefaultRout.info("Error occurred: " + e.getMessage());
                List<EmployeeIdClass> collList = collposting.getEmployeeId();
                for (int j = 0; j < collList.size(); j++) {
                    EmployeeIdClass existingRec = collList.get(j);
                    FfTellerDefaultRout.info("existing inside catch block " + existingRec.toString());
                    String empName = existingRec.getEmployeeId().getValue();
                    BigDecimal prevPend = safeBigDecimal(
                            normalizeNumber(existingRec.getPrevPendingCollection().getValue()));
                    BigDecimal bcp = safeBigDecimal(normalizeNumber(existingRec.getDepositAtBcpoint().getValue()));
                    BigDecimal cashAmt = safeBigDecimal(normalizeNumber(existingRec.getRoCashAmend().getValue()));
                    BigDecimal cashBranch = safeBigDecimal(
                            normalizeNumber(existingRec.getRoCashCollected().getValue()));

                    BigDecimal prevTotal = prevPend.add(cashAmt);
                    BigDecimal bcpTotal = bcp.add(cashBranch);
                    BigDecimal snatchBD = getSnatchAmount(empName, id);
                    BigDecimal pendingVal = prevTotal.subtract(bcpTotal).subtract(snatchBD);
                    existingRec.setRoPendingCollection(pendingVal.toString());
                    FfTellerDefaultRout.info("existing inside catch block after setting " + existingRec.toString());
                    collposting.setEmployeeId(existingRec, j);

                }

            }

            FfTellerDefaultRout.info("collposting Record after Update posting table " + collposting.toString());
            List<EmployeeIdClass> finalEmpIdList = collposting.getEmployeeId();
            if (finalEmpIdList != null) {
                for (EmployeeIdClass finalObject : finalEmpIdList) {

                    if (finalObject.getRoCashCollected().getValue() == null
                            || finalObject.getRoCashCollected().getValue().trim().isEmpty()) {
                        finalObject.setRoCashCollected("0.00");
                        totCash = totCash.add(safeBigDecimal(finalObject.getRoCashCollected().getValue()));
                        FfTellerDefaultRout.info("final Object cash by BM value if block: "
                                + finalObject.getRoCashCollected().getValue());
                    } else {
                        totCash = totCash
                                .add(safeBigDecimal(normalizeNumber(finalObject.getRoCashCollected().getValue())));
                        FfTellerDefaultRout.info("final Object cash by BM value else block: "
                                + finalObject.getRoCashCollected().getValue());
                    }

                    totDigital = totDigital
                            .add(safeBigDecimal(normalizeNumber(finalObject.getDigitalCollection().getValue())));
                    FfTellerDefaultRout
                            .info("final Object digital value: " + finalObject.getDigitalCollection().getValue());
                    totCashAmend = totCashAmend
                            .add(safeBigDecimal(normalizeNumber(finalObject.getRoCashAmend().getValue())));
                    FfTellerDefaultRout.info("final Object cash value: " + finalObject.getRoCashAmend().getValue());

                    totBcPoint = totBcPoint
                            .add(safeBigDecimal(normalizeNumber(finalObject.getDepositAtBcpoint().getValue())));
                    FfTellerDefaultRout
                            .info("final Object bcpoint value: " + finalObject.getDepositAtBcpoint().getValue());
                    totPrevPending = totPrevPending
                            .add(safeBigDecimal(normalizeNumber(finalObject.getPrevPendingCollection().getValue())));
                    FfTellerDefaultRout.info(
                            "final Object prev pending value: " + finalObject.getPrevPendingCollection().getValue());

                    totPendingCash = totPendingCash
                            .add(safeBigDecimal(normalizeNumber(finalObject.getRoPendingCollection().getValue())));
                    FfTellerDefaultRout
                            .info("final Object pending value: " + finalObject.getRoPendingCollection().getValue());

                    totCollected = totCollected
                            .add(safeBigDecimal(normalizeNumber(finalObject.getCollectionInLms().getValue())));
                    FfTellerDefaultRout.info(
                            "final Object total collection value: " + finalObject.getCollectionInLms().getValue());

                }
            }
            FfTellerDefaultRout.info("final Object totCollected value: " + totCollected.toString());
            collposting.setTotalCashAmt(totCash.toString());
            collposting.setTotalDigitalAmt(totDigital.toString());
            collposting.setTotRoCashAmend(totCashAmend.toString());
            collposting.setTotalDepositBcpoint(totBcPoint.toString());
            collposting.setTotPrevPendingCollection(totPrevPending.toString());

            collposting.setTotalCollectedAmt(totCollected.toString());
            collposting.setTotalPendingCash(totPendingCash.toString());

            currentRecord.set(collposting.toStructure());
        } catch (Exception e) {
            e.getMessage();
        }
    }

    private void updateTodaysRec(EbFfCollPostingScreenRecord collposting, String empIdYest, String prevPendingYest,
            String id) {

        FfTellerDefaultRout.info("===== START updateTodaysRec =====");
        FfTellerDefaultRout.info("Employee Id : " + empIdYest);
        FfTellerDefaultRout.info("Prev Pending : " + prevPendingYest);

        // Validation
        if (empIdYest == null || prevPendingYest == null) {
            FfTellerDefaultRout.info("Employee Id or Pending value is null");
            return;
        }

        BigDecimal pendingValue;

        try {
            pendingValue = new BigDecimal(prevPendingYest.trim());
        } catch (Exception e) {
            FfTellerDefaultRout.info("Invalid Pending Value : " + prevPendingYest);
            return;
        }

        // Skip employees with zero pending
        if (pendingValue.compareTo(BigDecimal.ZERO) <= 0) {
            FfTellerDefaultRout.info("Skipping employee with zero pending : " + empIdYest);
            return;
        }

        List<EmployeeIdClass> collList = collposting.getEmployeeId();

        if (collList == null) {
            collList = new ArrayList<>();
        }

        FfTellerDefaultRout.info("Today's collection list size : " + collList.size());

        String yestIdOnly = empIdYest.contains("/") ? empIdYest.split("/", 2)[0].trim() : empIdYest.trim();

        FfTellerDefaultRout.info("Yesterday employee id only : " + yestIdOnly);

        boolean matchFound = false;

        for (int i = 0; i < collList.size(); i++) {

            try {

                EmployeeIdClass existing = collList.get(i);

                if (existing == null) {

                    FfTellerDefaultRout.info("Null record found at index : " + i);

                    continue;
                }

                if (existing.getEmployeeId() == null) {

                    FfTellerDefaultRout.info("EmployeeId field is null at index : " + i);

                    continue;
                }

                String existingFull = existing.getEmployeeId().getValue();

                if (existingFull == null) {

                    FfTellerDefaultRout.info("EmployeeId value is null at index : " + i);

                    continue;
                }

                existingFull = existingFull.trim();

                // Skip invalid rows like "/"
                if (existingFull.isEmpty() || "/".equals(existingFull)) {

                    FfTellerDefaultRout.info("Skipping blank employee row at index " + i + " value=" + existingFull);

                    continue;
                }

                String[] empParts = existingFull.split("/", 2);

                if (empParts.length == 0 || empParts[0].trim().isEmpty()) {

                    FfTellerDefaultRout.info("Skipping invalid employee row at index " + i + " value=" + existingFull);

                    continue;
                }

                String existingId = empParts[0].trim();

                FfTellerDefaultRout
                        .info("Checking Index " + i + " ExistingId=[" + existingId + "] YestId=[" + yestIdOnly + "]");

                if (yestIdOnly.equals(existingId)) {

                    FfTellerDefaultRout.info("MATCH FOUND AT INDEX : " + i);

                    BigDecimal prevPend = safeBigDecimal(
                            normalizeNumber(existing.getPrevPendingCollection().getValue()));

                    BigDecimal bcp = safeBigDecimal(normalizeNumber(existing.getDepositAtBcpoint().getValue()));

                    BigDecimal cashAmt = safeBigDecimal(normalizeNumber(existing.getRoCashAmend().getValue()));

                    BigDecimal cashBranch = safeBigDecimal(normalizeNumber(existing.getRoCashCollected().getValue()));

                    BigDecimal prevTotal = prevPend.add(cashAmt);

                    BigDecimal bcpTotal = bcp.add(cashBranch);

                    BigDecimal snatchBD = getSnatchAmount(empIdYest, id);

                    BigDecimal pendingVal = prevTotal.subtract(bcpTotal).subtract(snatchBD);

                    existing.setPrevPendingCollection(prevPendingYest);

                    existing.setRoPendingCollection(pendingVal.toString());

                    collposting.setEmployeeId(existing, i);

                    FfTellerDefaultRout.info("Updated employee at index " + i + " Pending = " + pendingVal);

                    matchFound = true;

                    break;
                }

            } catch (Exception ex) {

                FfTellerDefaultRout.info("Exception while checking index " + i + " : " + ex.toString());
            }
        }

        // Add employee if match not found
        if (!matchFound) {

            FfTellerDefaultRout.info("No match found. Adding new employee : " + empIdYest);

            BigDecimal snatchBD = getSnatchAmount(empIdYest, id);

            BigDecimal prevPendBD = safeBigDecimal(prevPendingYest);

            BigDecimal pendingAmt = prevPendBD.subtract(snatchBD);

            EmployeeIdClass newEmp = new EmployeeIdClass();

            newEmp.setEmployeeId(empIdYest);
            newEmp.setPrevPendingCollection(prevPendingYest);
            newEmp.setRoCashCollected("0");
            newEmp.setDigitalCollection("0");
            newEmp.setDepositAtBcpoint("0");
            newEmp.setRoCashAmend("0");
            newEmp.setCollectionInLms("0");
            newEmp.setRoPendingCollection(pendingAmt.toString());

            collposting.setEmployeeId(newEmp, collList.size());

            FfTellerDefaultRout.info("Added employee : " + empIdYest);
        }

        FfTellerDefaultRout.info("===== END updateTodaysRec =====");
    }

    private void updatePostingTable(EbFfCollPostingScreenRecord collposting, String empId, String empName,
            String cashCollect, String digital, String totalCollect, String bcPoint, String otherBank, String id) {
        List<EmployeeIdClass> collList = collposting.getEmployeeId();
        FfTellerDefaultRout.info("collList inside update method " + collList);
        if (collList == null) {
            collList = new ArrayList<>();
        }

        boolean updated = false;
        for (int i = 0; i < collList.size(); i++) {
            EmployeeIdClass existing = collList.get(i);
            FfTellerDefaultRout.info("existing inside update method " + existing.toString());
            String empConcat = empId + "/" + empName;
            if (existing.getEmployeeId() != null && existing.getEmployeeId().toString().equals(empConcat)) {

                String cashByBm = existing.getRoCashCollected().getValue();
                String prevPend = existing.getPrevPendingCollection().getValue();
                EmployeeIdClass empIdObj = setEmployeeMultiValue(empId, cashCollect, cashByBm, empName, digital,
                        totalCollect, bcPoint, otherBank, prevPend, id);
                collposting.setEmployeeId(empIdObj, i);
                updated = true;
                break;
            }
        }
        if (!updated) {
            EmployeeIdClass empIdObj = setEmployeeMultiValueforFirst(empId, cashCollect, "", empName, digital,
                    totalCollect, bcPoint, otherBank, id);
            collposting.setEmployeeId(empIdObj, collList.size());
        }

    }

    private EmployeeIdClass setEmployeeMultiValueforFirst(String empId, String cashCollect, String existingCash,
            String empName, String digital, String totalCollect, String bcPointAmt, String bankAmt, String id) {

        EmployeeIdClass empIdObj = new EmployeeIdClass();
        BigDecimal adjustedTotal = BigDecimal.ZERO;
        BigDecimal pendingVal = BigDecimal.ZERO;
        String concatName = empId + "/" + empName;
        empIdObj.setEmployeeId(concatName);

        // Set raw inputs
        empIdObj.setRoCashCollected(existingCash);
        empIdObj.setDigitalCollection(digital);

        // Parse values
        BigDecimal lmsTotal = safeBigDecimal(normalizeNumber(totalCollect)); // LMS total (cash + digital)
        BigDecimal cashBD = safeBigDecimal(normalizeNumber(existingCash));
        BigDecimal digitalBD = safeBigDecimal(normalizeNumber(digital));
        BigDecimal bcPointBD = safeBigDecimal(normalizeNumber(bcPointAmt));
        BigDecimal oldCashBD = safeBigDecimal(normalizeNumber(cashCollect));
        BigDecimal bankBD = safeBigDecimal(normalizeNumber(bankAmt));
        BigDecimal bcpTotal = bcPointBD.add(bankBD);
        FfTellerDefaultRout.info("bcpTotal inside setEmployeeMultiValueforFirst: " + bcpTotal.toString());
        FfTellerDefaultRout.info("Total inside setEmployeeMultiValueforFirst: " + lmsTotal.toString());
        adjustedTotal = cashBD.add(digitalBD).add(bcPointBD).add(bankBD);
        FfTellerDefaultRout.info("adjustedTotal inside setEmployeeMultiValueforFirst: " + adjustedTotal.toString());
        BigDecimal snatchBD = getSnatchAmount(concatName, id);
        pendingVal = oldCashBD.subtract(bcpTotal.add(cashBD)).subtract(snatchBD);
        FfTellerDefaultRout.info("pendingVal inside setEmployeeMultiValueforFirst : " + pendingVal.toString());

        // Keep LMS Total from source (do NOT replace with component sums)
        empIdObj.setDepositAtBcpoint(bcpTotal.toString());
        empIdObj.setRoCashAmend(cashCollect);
        empIdObj.setCollectionInLms(lmsTotal.toString());
        empIdObj.setRoPendingCollection(pendingVal.toString());

        return empIdObj;
    }

    private EmployeeIdClass setEmployeeMultiValue(String empId, String cashCollect, String cashByBm, String empName,
            String digital, String totalCollect, String bcPoint, String otherBank, String prevPend, String id) {

        EmployeeIdClass empIdObj = new EmployeeIdClass();
        BigDecimal pendingVal = BigDecimal.ZERO;
        BigDecimal adjustedTotal = BigDecimal.ZERO;
        String concatName = empId + "/" + empName;
        empIdObj.setEmployeeId(concatName);

        BigDecimal newcashBD = safeBigDecimal(normalizeNumber(cashByBm));
        BigDecimal oldCashBD = safeBigDecimal(normalizeNumber(cashCollect));
        BigDecimal digitalBD = safeBigDecimal(normalizeNumber(digital));
        BigDecimal bcPointBD = safeBigDecimal(normalizeNumber(bcPoint));
        BigDecimal bankBD = safeBigDecimal(normalizeNumber(otherBank));
        BigDecimal prevPendBD = safeBigDecimal(normalizeNumber(prevPend));
        BigDecimal lmsTotal = safeBigDecimal(normalizeNumber(totalCollect));

        BigDecimal bcpTotal = bcPointBD.add(bankBD);
        BigDecimal prevTotal = prevPendBD.add(oldCashBD);
        FfTellerDefaultRout.info("bcpTotal inside setEmployeeMultiValue: " + bcpTotal.toString());
        FfTellerDefaultRout.info("oldTotal inside setEmployeeMultiValue: " + lmsTotal.toString());

        adjustedTotal = newcashBD.add(digitalBD).add(bcPointBD).add(bankBD);
        FfTellerDefaultRout.info("adjustedTotal inside setEmployeeMultiValue: " + adjustedTotal.toString());
        BigDecimal snatchBD = getSnatchAmount(concatName, id);
        pendingVal = prevTotal.subtract(bcpTotal.add(newcashBD)).subtract(snatchBD);
        FfTellerDefaultRout.info("pendingVal inside setEmployeeMultiValue if : " + pendingVal.toString());
        empIdObj.setRoCashCollected(newcashBD.toString());
        empIdObj.setDigitalCollection(digital);
        empIdObj.setDepositAtBcpoint(bcpTotal.toString());
        empIdObj.setRoCashAmend(cashCollect);
        empIdObj.setPrevPendingCollection(prevPend);
        empIdObj.setCollectionInLms(lmsTotal.toString());
        empIdObj.setRoPendingCollection(pendingVal.toString());

        return empIdObj;
    }

    private BigDecimal getSnatchAmount(String empName, String id) {
        EbSnatcFraudCollScrnRecord snatchRec = null;
        BigDecimal snatchBD = BigDecimal.ZERO;
        String recId = id + "-" + empName;
        FfTellerDefaultRout.info("Record Id of Snatch Table " + recId);
        try {
            snatchRec = new EbSnatcFraudCollScrnRecord(da.getRecord("EB.SNATC.FRAUD.COLL.SCRN", recId));
            FfTellerDefaultRout.info("Record of Snatch Table full rec " + snatchRec.toString());
            /*
             * String snatchRecStr =
             * snatchRec.getTxnDate().get(0).getFfSnaFrdAmt().getValue();
             * FfTellerDefaultRout.info("amount of Snatch Table " + snatchRecStr); snatchBD
             * = new BigDecimal((snatchRecStr == null || snatchRecStr.isEmpty()) ? "0" :
             * snatchRecStr);
             */
            List<TxnDateClass> snatchList = snatchRec.getTxnDate();
            for (int k = 0; k < snatchList.size(); k++) {
                TxnDateClass snatchObj = snatchList.get(k);
                String snatchRecStr = snatchObj.getFfSnaFrdAmt().getValue();
                snatchBD = snatchBD
                        .add(new BigDecimal((snatchRecStr == null || snatchRecStr.isEmpty()) ? "0" : snatchRecStr));
                FfTellerDefaultRout.info("Snatch BigDecimal inside for loop " + snatchBD.toString());
            }

            return snatchBD;
        } catch (Exception e) {
            FfTellerDefaultRout.info("Catch initiated for snatch table method " + e.getMessage());

            snatchBD = BigDecimal.ZERO;
            FfTellerDefaultRout.info("Snatch Amount in catch " + snatchBD.toString());
            return snatchBD;
        }
    }

    private BigDecimal safeBigDecimal(String value) {

        try {
            if (value == null) {
                return BigDecimal.ZERO;
            }

            value = value.trim();

            if (value.length() == 0) {
                return BigDecimal.ZERO;
            }

            if (value.isEmpty() || "null".equalsIgnoreCase(value)) {
                return BigDecimal.ZERO;
            }

            value = value.replace(",", "");

            return new BigDecimal(value);

        } catch (Exception e) {

            FfTellerDefaultRout.info("Invalid number format: [" + value + "]");
            return BigDecimal.ZERO;
        }
    }

    private String normalizeNumber(String value) {

        if (value == null) {
            return "0";
        }

        value = value.trim();

        if (value.length() == 0) {
            return "0";
        }

        if (value.isEmpty() || "null".equalsIgnoreCase(value)) {
            return "0";
        }

        return value.replace(",", "");
    }

}