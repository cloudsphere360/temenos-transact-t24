package com.temenos.fusion;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
/*-----------------------------------------------------------------------------
 * @author Vinothini P
 * Date Created:
 * Attached as : Post Routine
 * EB.API : NA
 * Attached to :NA
 * Description: 
 *------------------------------------------------------------------------------ 
 * Modification History :
 *----------------------------------------------------------------------------- 
 *22-Aug-2025   Development      Initial Version
 *
 *-----------------------------------------------------------------------------
 */
import java.util.List;

import com.temenos.api.TDate;
import com.temenos.api.TStructure;
import com.temenos.api.exceptions.T24IOException;

import com.temenos.t24.api.arrangement.accounting.Contract;
import com.temenos.t24.api.complex.aa.activityhook.ArrangementContext;
import com.temenos.t24.api.complex.aa.activityhook.TransactionData;
import com.temenos.t24.api.complex.aa.contractapi.RepaymentDetails;
import com.temenos.t24.api.complex.aa.contractapi.RepaymentDueType;
import com.temenos.t24.api.complex.aa.contractapi.RepaymentMethod;
import com.temenos.t24.api.hook.arrangement.ActivityLifecycle;
import com.temenos.t24.api.records.aaaccountdetails.AaAccountDetailsRecord;
import com.temenos.t24.api.records.aaarrangement.AaArrangementRecord;
import com.temenos.t24.api.records.aaarrangementactivity.AaArrangementActivityRecord;
import com.temenos.t24.api.records.aaprddesaccount.AaPrdDesAccountRecord;
import com.temenos.t24.api.records.aaprddestermamount.AaPrdDesTermAmountRecord;
import com.temenos.t24.api.records.aaproductcatalog.AaProductCatalogRecord;
import com.temenos.t24.api.records.company.CompanyRecord;
import com.temenos.t24.api.records.ebffcollectiondets.EbFfCollectionDetsRecord;
import com.temenos.t24.api.records.ebffcollectiondetshistory.EbFfCollectionDetsHistoryRecord;
import com.temenos.t24.api.records.ebffloandpd.DateClass;
import com.temenos.t24.api.records.ebffloandpd.EbFfLoanDpdRecord;
import com.temenos.t24.api.records.ebffloansluc.EbFfLoansLucRecord;
import com.temenos.t24.api.system.DataAccess;
import com.temenos.t24.api.system.Session;
import com.temenos.t24.api.tables.ebffcollectiondets.EbFfCollectionDetsTable;
import com.temenos.t24.api.tables.ebffloandpd.EbFfLoanDpdTable;

public class FfPostScheduleUpdate extends ActivityLifecycle {
    private static final FusionFileLogger FfPostScheduleUpdatelog = FusionFileLogger
            .getLogger(FfPostScheduleUpdate.class);

    String centre;
    String group;
    String zone;
    String village;
    String totalDue ;
    String totalCap;
    String principalAmt;
    String interestAmt;
    String totalPymt;
    String chargeAmt;
    String taxAmt;

    @Override
    public void postCoreTableUpdate(AaAccountDetailsRecord accountDetailRecord,
            AaArrangementActivityRecord arrangementActivityRecord, ArrangementContext arrangementContext,
            AaArrangementRecord arrangementRecord, AaArrangementActivityRecord masterActivityRecord,
            TStructure productPropertyRecord, AaProductCatalogRecord productRecord, TStructure currRecord,
            List<TransactionData> transactionData, List<TStructure> transactionRecord) {
        try {
            FfPostScheduleUpdatelog.info("postCoreTableUpdate:FfPostScheduleUpdate routine triggered");
            Session sessionObj = new Session(this);
            Contract contractObj = new Contract(this);
            DataAccess dataAccess = new DataAccess(this);
            FfPostScheduleUpdatelog.info(" arrangementContext " + arrangementContext.toString());
            if (!"AUTH".equalsIgnoreCase(arrangementContext.getActivityStatus())) {
                return;
            }
            String cmpyId = arrangementRecord.getCoCodeRec().getValue();

            String finMnemonic = getCompanyMnemonic(dataAccess, cmpyId);

            String arrangementId = arrangementActivityRecord.getArrangement().getValue();
            String simRefId = arrangementActivityRecord.getSimRunRef().getValue() != null 
                    ? arrangementActivityRecord.getSimRunRef().getValue() 
                    : "";

            String activity = arrangementActivityRecord.getActivity().getValue();

            TDate startDate = new TDate(arrangementRecord.getStartDate().toString());
            TDate effectiveDate = new TDate(arrangementActivityRecord.getEffectiveDate().getValue());
            String customerId = arrangementActivityRecord.getCustomer(0).getCustomer().toString();
            String product = arrangementActivityRecord.getProduct().toString();
            TDate maturityDate = new TDate(accountDetailRecord.getMaturityDate().toString());
            contractObj.setContractId(arrangementId);
            EbFfCollectionDetsRecord ebFfCollectionDetsObj = new EbFfCollectionDetsRecord(this);

            EbFfCollectionDetsTable ebFfCollectionTableObj = new EbFfCollectionDetsTable(this);
            getAccountLocalTableValue(contractObj, activity, effectiveDate, currRecord);

            FfPostScheduleUpdatelog
                    .info("values " + "centre" + centre + "group" + group + "zone" + zone + "village" + village);
            FfPostScheduleUpdatelog.info("simRefId " + simRefId);
            FfPostScheduleUpdatelog.info("startDate " + startDate);
            FfPostScheduleUpdatelog.info("maturityDate " + maturityDate);
            List<RepaymentDetails> repaymentSchedule = contractObj.getPaymentSchedule(simRefId, startDate, maturityDate,
                    null, null);

            FfPostScheduleUpdatelog
                    .info("postCoreTableUpdate:FfPostScheduleUpdate repaymentSchedule " + repaymentSchedule.toString());

            for (RepaymentDetails schedule : repaymentSchedule) {
                totalPymt = "0.00";
                chargeAmt = "0.00";
                principalAmt = "0.00";
                interestAmt = "0.00";
                taxAmt = "0.00";
                totalDue = "0.00";
                totalCap = "0.00";

                String outstandingAmt = getSafe(schedule.getDueOutstandingBalance());
                FfPostScheduleUpdatelog.info("outstandingAmt " + outstandingAmt);

                String dueDate = schedule.getDueDate().toString();

                ebFfCollectionDetsObj.addDueDate(dueDate);
                ebFfCollectionDetsObj.addOutstandingAmt(outstandingAmt);

                processRepaymentDetails(schedule);

               
                ebFfCollectionDetsObj.setCentre(centre);
                ebFfCollectionDetsObj.setGroup(group);
                ebFfCollectionDetsObj.setZone(zone);
                ebFfCollectionDetsObj.setVillage(village);
                ebFfCollectionDetsObj.setLosProdDetails(product);
                ebFfCollectionDetsObj.setCustomer(customerId);
                ebFfCollectionDetsObj.addTotalDue(totalDue);
                ebFfCollectionDetsObj.addTotalCap(totalCap);
                ebFfCollectionDetsObj.addPrincipalAmt(principalAmt);
                ebFfCollectionDetsObj.addInterestAmt(interestAmt);
                ebFfCollectionDetsObj.addChargeAmt(chargeAmt);
                ebFfCollectionDetsObj.addTaxAmt(taxAmt);
                ebFfCollectionDetsObj.addTotalPymt(totalPymt);
            }

            // Step 1 – Capture OLD EB.FF.COLLECTION.DETS before delete
            TStructure oldCollectionSnapshot = getOldRecord(arrangementId, ebFfCollectionTableObj);

            // Step 2 – Delete old record
            deleteArrangement(ebFfCollectionTableObj, arrangementId);

            // Step 3 – Write new record
            ebFfCollectionTableObj.write(arrangementId, ebFfCollectionDetsObj);
            FfPostScheduleUpdatelog.info(" EB.FF.COLLECTION.DETS Table updated for: " + arrangementId
                    + "ebFfCollectionDetsObj " + ebFfCollectionDetsObj.toString());

            // Step 4 – Move OLD record to HISTORY method to update the
            // EB.FF.COLLECTION.DETS.HISTORY table
            updateCollectionHistory(arrangementId, sessionObj, transactionData, transactionRecord, finMnemonic,
                    oldCollectionSnapshot);

            // method to update the EB.FF.LOANS.LUC table
            updateFfLoanLucTable(startDate, arrangementId, customerId, contractObj, transactionData, transactionRecord);

            // method to update the EB.FF.LOAN.DPD table
            updateFfLoanDpdTable(arrangementId, customerId, accountDetailRecord);

        } catch (

        Exception e) {
            FfPostScheduleUpdatelog.error("postCoreTableUpdate:FfPostScheduleUpdate" + e);
        }
    }

    private void processRepaymentDetails(RepaymentDetails schedule) {
        try {
            for (RepaymentDueType dueType : schedule.getRepaymentDueType()) {
                String dueTypeCode = dueType.getDueType();
                String dueTypeAmount = getSafe(dueType.getDueTypeAmount());

                if (dueTypeCode.equalsIgnoreCase("CHARGE")) {
                    chargeAmt = dueTypeAmount;
                }
                if (dueTypeCode.equalsIgnoreCase("CONSTANT")) {
                    totalDue = dueTypeAmount;
                }
                if (dueTypeCode.equalsIgnoreCase("DISBURSEMENT.%")
                        || dueTypeCode.equalsIgnoreCase("DISBURSEMENT.AMT")) {
                    totalPymt = dueTypeAmount;
                }
                if (dueTypeCode.equalsIgnoreCase("CAPITALISE")) {
                    totalCap = addAmounts(totalCap, dueTypeAmount);
                }

                for (RepaymentMethod method : dueType.getRepaymentMethod()) {

                    processRepaymentMethod(method);

                }
            }
        } catch (Exception e) {
            FfPostScheduleUpdatelog.info("No existing record to delete for: " + e);
        }
    }

    private void processRepaymentMethod(RepaymentMethod method) {
        try {
            if ("CAPITALISE".equalsIgnoreCase(method.getDueMethod())) {
                totalCap = addAmounts(totalCap, method.getDuePropertyAmount());
            }

            if ("ACCOUNT".equalsIgnoreCase(method.getDueProperty())) {
                principalAmt = getSafe(method.getDuePropertyAmount());
            }

            if (!"DUE".equalsIgnoreCase(method.getDueMethod()))
                return;

            switch (method.getDueProperty()) {
            case "PRINTEREST":
                interestAmt = getSafe(method.getDuePropertyAmount());
                break;
            case "CHARGE":
                totalDue = getSafe(method.getDuePropertyAmount());
                break;
            case "TAX":
                taxAmt = getSafe(method.getDuePropertyAmount());
                break;
            default:
                break;
            }

            if (method.getDueTaxPropertyAmount() != null) {
                taxAmt = getSafe(method.getDueTaxPropertyAmount());
            }

        } catch (Exception e) {
            FfPostScheduleUpdatelog.error("unable to process " + e);

        }

    }

    private void deleteArrangement(EbFfCollectionDetsTable ebFfCollectionTableObj, String arrangementId) {
        try {
            ebFfCollectionTableObj.delete(arrangementId);
            FfPostScheduleUpdatelog.info("Deleted existing EB.FF.COLLECTION.DETS for: " + arrangementId);
        } catch (Exception e) {
            FfPostScheduleUpdatelog.info("No existing record to delete for: " + arrangementId);
        }

    }

    private TStructure getOldRecord(String arrangementId, EbFfCollectionDetsTable ebFfCollectionTableObj) {
        TStructure oldCollectionSnapshot = null;
        try {
            EbFfCollectionDetsRecord oldRec = ebFfCollectionTableObj.read(arrangementId);
            oldCollectionSnapshot = oldRec.toStructure();
            FfPostScheduleUpdatelog.info("Old EB.FF.COLLECTION.DETS snapshot captured for " + arrangementId);
        } catch (Exception e) {
            FfPostScheduleUpdatelog
                    .info("No existing EB.FF.COLLECTION.DETS record (first creation) for " + arrangementId);
        }
        return oldCollectionSnapshot;
    }

    private void getAccountLocalTableValue(Contract contractObj, String activity, TDate effectiveDate,
            TStructure currRecord) {
        try {
            AaPrdDesAccountRecord prdDesAccountObj = contractObj.getAccountConditionForEffectiveDate("ACCOUNT",
                    effectiveDate);

            FfPostScheduleUpdatelog.info("prdDesAccountObj " + prdDesAccountObj.toString());

            if (activity.equals("LENDING-UPDATE-ACCOUNT")) {
                AaPrdDesAccountRecord desActObj = new AaPrdDesAccountRecord(currRecord);
                centre = desActObj.getLocalRefField("FF.CENTRE").getValue();
                group = desActObj.getLocalRefField("FF.GROUP").getValue();
                zone = desActObj.getLocalRefField("FF.ZONE").getValue();
                village = desActObj.getLocalRefField("FF.VILLAGE").getValue();

            } else {
                centre = prdDesAccountObj.getLocalRefField("FF.CENTRE").getValue();
                group = prdDesAccountObj.getLocalRefField("FF.GROUP").getValue();
                zone = prdDesAccountObj.getLocalRefField("FF.ZONE").getValue();
                village = prdDesAccountObj.getLocalRefField("FF.VILLAGE").getValue();
            }

        } catch (Exception e) {

            FfPostScheduleUpdatelog.error("unable to fetch account details" + e);
        }

    }

    private String getCompanyMnemonic(DataAccess dataAccess, String cmpyId) {
        String finMnemonic = null;
        try {
            CompanyRecord companyObj = new CompanyRecord(dataAccess.getRecord("COMPANY", cmpyId));
            finMnemonic = companyObj.getFinancialMne().getValue();

        } catch (Exception e) {
            FfPostScheduleUpdatelog.error("unable to get mnemonic" + e);
        }
        return finMnemonic;
    }

    private void updateFfLoanDpdTable(String arrangementId, String customerId,
            AaAccountDetailsRecord accountDetailRecord) {

        try {
            if (accountDetailRecord.getArrAgeStatus().getValue().isEmpty()
                    || accountDetailRecord.getArrAgeStatus().getValue().equals("")) {
                Session sessionObj = new Session(this);
                String today = sessionObj.getCurrentVariable("!TODAY");
                FfPostScheduleUpdatelog.info("updateFfLoanDpdTable method triggered");
                EbFfLoanDpdRecord loanDpd = new EbFfLoanDpdRecord(this);
                FfPostScheduleUpdatelog.info("loanDpd before " + loanDpd.toString());
                EbFfLoanDpdTable loanDpdTable = new EbFfLoanDpdTable(this);
                loanDpd.setCustomer(customerId);
                DateClass dateList = new DateClass();
                dateList.setDate(today);
                dateList.setCurDpd("0");
                dateList.setAvgDpd("0.0");
                dateList.setCumDpd("0");
                dateList.setPeakDpd("0");
                loanDpd.addDate(dateList);
                DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern("yyyyMMdd");
                LocalDate date = LocalDate.parse(today, inputFormatter);
                String formatted = date.getMonth().toString().substring(0, 3) + date.getYear();
                FfPostScheduleUpdatelog.info("loanDpd after " + loanDpd.toString());
                String tableId = arrangementId + "-" + formatted;
                loanDpdTable.write(tableId, loanDpd);
            }
        } catch (Exception e) {
            FfPostScheduleUpdatelog.error("Unable to write the ff loan dpd table " + e);
        }

    }

    private void updateCollectionHistory(String arrangementId, Session sessionObj,
            List<TransactionData> transactionData, List<TStructure> transactionRecord, String finMnemonic,
            TStructure oldCollectionSnapshot) throws T24IOException {
        DataAccess dataAccess = new DataAccess(this);

        String todayDate = sessionObj.getCurrentVariable("!TODAY");
        EbFfCollectionDetsHistoryRecord collHistoryRecord;
        if (oldCollectionSnapshot == null) {
            // First time → copy LIVE to HISTORY
            try {
                EbFfCollectionDetsTable collectionTable = new EbFfCollectionDetsTable(this);
                EbFfCollectionDetsRecord liveRecord = collectionTable.read(arrangementId);

                collHistoryRecord = new EbFfCollectionDetsHistoryRecord(liveRecord.toStructure());
                FfPostScheduleUpdatelog.info("First time HISTORY creation from LIVE for " + arrangementId);
            } catch (Exception e) {
                FfPostScheduleUpdatelog.error("Unable to read LIVE record for HISTORY " + e);
                return;
            }
        } else {
            // Normal case → use old snapshot
            collHistoryRecord = new EbFfCollectionDetsHistoryRecord(oldCollectionSnapshot);
            FfPostScheduleUpdatelog.info("Moving OLD snapshot to HISTORY for " + arrangementId);
        }

        String historyBaseId = arrangementId + "-" + todayDate;
        String milli = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HHmmssSSS"));

        String tempBase = historyBaseId + "." + milli;
        FfPostScheduleUpdatelog.info(" historyBaseId " + historyBaseId);
        int seq = 1;

        String nextId = "";
        while (seq <= 999) {
            nextId = tempBase + "." + String.format("%02d", seq);
            FfPostScheduleUpdatelog.info("nextId Before " + nextId);
            try {
                String readId = historyBaseId + "." + String.format("%02d", seq);
                FfPostScheduleUpdatelog.info(" readId " + readId);
                EbFfCollectionDetsHistoryRecord collHistoryRecordSeq = new EbFfCollectionDetsHistoryRecord(
                        dataAccess.getRecord(finMnemonic, "EB.FF.COLLECTION.DETS.HISTORY", "", readId));

                if (!collHistoryRecordSeq.toString().isEmpty()) {
                    seq++;
                }
            } catch (Exception e) {
                // Record does NOT exist → this ID is free
                break;
            }
        }
        String finalHistoryId = historyBaseId + "." + String.format("%02d", seq);
        FfPostScheduleUpdatelog.info(" finalHistoryId " + finalHistoryId);

        FfPostScheduleUpdatelog.info(" nextId " + nextId);
        TransactionData transData = new TransactionData();
        transData.setFunction("INPUT");
        transData.setSourceId("FF.OFS.UPD");
        transData.setNumberOfAuthoriser("0");
        transData.setTransactionId(finalHistoryId);
        transData.setVersionId("EB.FF.COLLECTION.DETS.HISTORY,FF.UPDATE");
        transactionData.add(transData);
        transactionRecord.add(collHistoryRecord.toStructure());

        FfPostScheduleUpdatelog.info(
                "Moved existing EB.FF.COLLECTION.DETS record to HISTORY via OFS for arrangement: " + arrangementId);

    }

    private void updateFfLoanLucTable(TDate startDate, String arrangementId, String customerId, Contract contractObj,
            List<TransactionData> transactionData, List<TStructure> transactionRecord) {

        String termAmount = null;
        try {
            AaPrdDesTermAmountRecord aaPrdDesTermAmountObj = new AaPrdDesTermAmountRecord(
                    contractObj.getSimulationConditionForProperty("COMMITMENT"));
            termAmount = aaPrdDesTermAmountObj.getAmount().getValue();
            FfPostScheduleUpdatelog.info("Term Amount :" + termAmount);
        } catch (Exception e) {
            FfPostScheduleUpdatelog.error("Failed to get term amount: " + e.getMessage());
        }
        EbFfLoansLucRecord lucRecord = new EbFfLoansLucRecord(this);
        TransactionData transData = new TransactionData();
        lucRecord.setRoLucCom("NO");
        lucRecord.setRoLnDisbAmt(termAmount);
        lucRecord.setBmLnDisAmt(termAmount);
        lucRecord.setCustomer(customerId);
        lucRecord.setLoanCreationDate(startDate.toString());

        transData.setFunction("INPUT");
        transData.setSourceId("FF.OFS.UPD");
        transData.setNumberOfAuthoriser("0");
        transData.setTransactionId(arrangementId);
        transData.setVersionId("EB.FF.LOANS.LUC,FF.UPDATE");
        transactionData.add(transData);
        transactionRecord.add(lucRecord.toStructure());
        FfPostScheduleUpdatelog.info(" EB.FF.LOANS.LUC Table updated for: " + arrangementId);

    }

    private String addAmounts(String totalCap, String duePropertyAmount) {
        double amount1 = Double.parseDouble(getSafe(totalCap));
        double amount2 = Double.parseDouble(getSafe(duePropertyAmount));
        return String.format("%.2f", amount1 + amount2);
    }

    private String getSafe(Object val) {
        if (val == null || val.toString().trim().isEmpty()) {
            return "0.00";
        }
        try {
            return String.format("%.2f", Double.parseDouble(val.toString()));
        } catch (NumberFormatException e) {
            return "0.00";
        }
    }

}
