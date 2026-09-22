package com.temenos.fusion;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import com.temenos.api.TField;
import com.temenos.api.TStructure;
import com.temenos.t24.api.complex.eb.servicehook.TransactionData;
import com.temenos.t24.api.complex.eb.templatehook.TransactionContext;
import com.temenos.t24.api.hook.system.RecordLifecycle;
import com.temenos.t24.api.records.ebffeodrodets.EbFfEodRoDetsRecord;
import com.temenos.t24.api.records.ebffftcollrevconcat.EbFfFtCollRevConcatRecord;
import com.temenos.t24.api.records.ebffrocollconcat.EbFfRoCollConcatRecord;
import com.temenos.t24.api.records.fundstransfer.FundsTransferRecord;
import com.temenos.t24.api.system.DataAccess;
import com.temenos.t24.api.system.Session;
import com.temenos.t24.api.tables.ebffeodrodets.EbFfEodRoDetsTable;
import com.temenos.t24.api.tables.ebffftcollrevconcat.EbFfFtCollRevConcatTable;
import com.temenos.t24.api.tables.ebffrocollconcat.EbFfRoCollConcatTable;

/**
 * -----------------------------------------------------------------------------
 * Modification History :
 * -----------------------------------------------------------------------------
 * Description : This Routine used to update EB.FF.FT.COLL.REV.CONCAT table
 * based on the payment mode and Reversal Status to get the data from Funds
 * Transfer application during authorisation.
 *
 * Developed By : Preethi Selvam
 *
 * Development Reference : Teller Report
 *
 * Attached To : VERSION>FUNDS.TRANSFER,AUTH.COLLECTION >FF.FT.REV.UPDATE
 * 
 * Attached As : Auth Routine
 * 
 * -----------------------------------------------------------------------------
 */
public class FfFtUpdConcatForRevCollect extends RecordLifecycle {

    private final Session ses = new Session();
    private final DataAccess da = new DataAccess(this);
    EbFfEodRoDetsTable eodTab = new EbFfEodRoDetsTable(this);
    EbFfFtCollRevConcatTable collTab = new EbFfFtCollRevConcatTable(this);
    String credAmount = "";
    String payMode = "";
    String processDate = "";
    String postBy = "";
    String postName = "";
    String tableName = "";
    String compCode = "";
    String ymnemonic = "";
    String todayDate = "";
    String idConcat = "";

    private static final FusionFileLogger ftUpdConcatForRevCollect = FusionFileLogger
            .getLogger(FfFtUpdConcatForRevCollect.class);

    @Override
    public void postUpdateRequest(String application, String currentRecordId, TStructure currentRecord,
            List<TransactionData> transactionData, List<TStructure> currentRecords,
            TransactionContext transactionContext) {
        try {
            ftUpdConcatForRevCollect.info("FfFtUpdConcatForRevCollect: Routine triggered" + currentRecordId);

            tableName = "EB.FF.FT.COLL.REV.CONCAT";
            compCode = safeString(ses.getCompanyId());
            ymnemonic = safeString(ses.getCompanyRecord().getFinancialMne());
            todayDate = ses.getCurrentVariable("!TODAY");
            ftUpdConcatForRevCollect.info("Company Details " + compCode + "**" + ymnemonic + "**" + todayDate);

            FundsTransferRecord fundsTransferObj = new FundsTransferRecord(currentRecord);

            processDate = safeString(fundsTransferObj.getProcessingDate());
            postBy = safeString(fundsTransferObj.getLocalRefField("FF.POSTED.BY"));
            postName = safeString(fundsTransferObj.getLocalRefField("FF.POST.LGLNAME"));
            credAmount = safeString(fundsTransferObj.getCreditAmount());
            payMode = safeString(fundsTransferObj.getLocalRefField("FF.PYMT.MODE"));

            ftUpdConcatForRevCollect.info(
                    "Ft Details" + processDate + "**" + postBy + "**" + postName + "**" + credAmount + "**" + payMode);

            idConcat = postBy + "-" + compCode + "-" + processDate;
            ftUpdConcatForRevCollect.info("FfFtUpdConcatForRevCollect:- idConcat: {}" + idConcat);

            // Fetch existing record
            EbFfFtCollRevConcatRecord fetchedRecord = getFfFtCollRevConcatRecord(ymnemonic, tableName, idConcat);

            if (fetchedRecord != null && !fetchedRecord.toString().trim().isEmpty()) {
                postUpdateConcat(fetchedRecord, credAmount, payMode, idConcat);
            } else {
                ftUpdConcatForRevCollect.info(
                        "FfFtUpdConcatForRevCollect:- No existing record found. Creating new record for idConcat: {}"
                                + idConcat);
                updateTheConcatTables(payMode, postBy, postName, processDate, compCode);

            }
            updateTheEbFfRoCollConcat(idConcat, credAmount, ymnemonic, payMode);

        } catch (Exception e) {
            ftUpdConcatForRevCollect.error("Unexpected error in postUpdateRequest: {}" + e.getMessage(), e);
        }
    }

    /**
     * @param credAmount
     * @param ymnemonic
     * @param payMode
     * @param idConcat2
     */
    private void updateTheEbFfRoCollConcat(String idConcat, String credAmount, String ymnemonic, String payMode) {

        ftUpdConcatForRevCollect.info("updateTheEbFfRoCollConcat is triggered" + idConcat);
        try {
            EbFfRoCollConcatRecord ffRoCollConcatRec = new EbFfRoCollConcatRecord(
                    da.getRecord(ymnemonic, "EB.FF.RO.COLL.CONCAT", "", idConcat));

            String bcPointColl = ffRoCollConcatRec.getBcPointCollected().getValue();
            String otherBankColl = ffRoCollConcatRec.getOtherBankCollected().getValue();
            ftUpdConcatForRevCollect.info("bcPointColl" + bcPointColl + "***" + otherBankColl);

            String revBcPointColl = ffRoCollConcatRec.getReverseBcPointCollected().getValue();
            String revOtherBankColl = ffRoCollConcatRec.getReverseOtherBankCollected().getValue();
            ftUpdConcatForRevCollect.info("revBcPointColl" + revBcPointColl + "***" + revOtherBankColl);

            if (payMode.equalsIgnoreCase("BCP")) {
                ftUpdConcatForRevCollect.info("payMode" + payMode);
                if (!bcPointColl.isEmpty()) {
                    double subBcRevAmt = Double.parseDouble(bcPointColl) - Double.parseDouble(credAmount);
                    ftUpdConcatForRevCollect.info("bcPointColl" + bcPointColl);
                    ffRoCollConcatRec.getBcPointCollected().setValue(String.valueOf(subBcRevAmt));
                }
                if (!revBcPointColl.isEmpty()) {
                    double revBcAmt = Double.parseDouble(revBcPointColl) + Double.parseDouble(credAmount);
                    ffRoCollConcatRec.getReverseBcPointCollected().setValue(String.valueOf(revBcAmt));
                } else {
                    ftUpdConcatForRevCollect.info("newly Updated BCP" + ffRoCollConcatRec);
                    ffRoCollConcatRec.getReverseBcPointCollected().setValue(String.valueOf(credAmount));
                }
            } else if (payMode.equalsIgnoreCase("OBS")) {
                ftUpdConcatForRevCollect.info("payMode" + payMode);
                if (!otherBankColl.isEmpty()) {
                    double subOtherBankRevAmt = Double.parseDouble(otherBankColl) - Double.parseDouble(credAmount);
                    ftUpdConcatForRevCollect.info("subOtherBankRevAmt" + subOtherBankRevAmt);
                    ffRoCollConcatRec.getOtherBankCollected().setValue(String.valueOf(subOtherBankRevAmt));
                }
                if (!revOtherBankColl.isEmpty()) {
                    double revObsAmt = Double.parseDouble(revOtherBankColl) + Double.parseDouble(credAmount);
                    ftUpdConcatForRevCollect.info("revObsAmt" + revObsAmt);
                    ffRoCollConcatRec.getReverseOtherBankCollected().setValue(String.valueOf(revObsAmt));
                } else {
                    ffRoCollConcatRec.getReverseOtherBankCollected().setValue(String.valueOf(credAmount));
                    ftUpdConcatForRevCollect.info("newly Updated OBS" + ffRoCollConcatRec);
                }
            }
            writeTheEbFfRoCollConcatTable(idConcat, ffRoCollConcatRec);
        } catch (Exception e) {
            ftUpdConcatForRevCollect.error("updateTheEbFfRoCollConcat" + e);
        }
    }

    /**
     * @param idConcat2
     * @param ffRoCollConcatRec
     */
    private void writeTheEbFfRoCollConcatTable(String idConcat, EbFfRoCollConcatRecord ffRoCollConcatRec) {

        EbFfRoCollConcatTable ffRoCollConcatTable = new EbFfRoCollConcatTable(this);
        try {
            ffRoCollConcatTable.write(idConcat, ffRoCollConcatRec);
        } catch (Exception e) {
            ftUpdConcatForRevCollect.error("writeTheEbFfRoCollConcatTable" + e);
        }
    }

    private void updateTheConcatTables(String payMode, String postBy, String postName, String processDate,
            String compCode) {
        try {
            ftUpdConcatForRevCollect.info("updateTheConcatTables is triggered successfully : {}" + idConcat);
            EbFfFtCollRevConcatRecord collRec = new EbFfFtCollRevConcatRecord(this);

            if ("BCP".equalsIgnoreCase(payMode) || ("OBS".equalsIgnoreCase(payMode))) {
                collRec.setBcPointRevCollected(credAmount);
            }
            collRec.setRoEmpId(postBy);
            collRec.setRoEmpName(postName);
            collRec.setDate(processDate);
            collRec.setBranch(compCode);

            String roTabId = compCode + "-" + todayDate;
            String updEodConcat = collRec.getUpdateEodRevConcat().toString();
            ftUpdConcatForRevCollect.info("updEodConcat" + updEodConcat);
            if (updEodConcat.isEmpty()) {
                ftUpdConcatForRevCollect.info("updEodConcat is empty. Proceeding to append idConcat: {}" + idConcat);

                EbFfEodRoDetsRecord eodRec = getFfEodRoDetsRecord(roTabId, eodTab);

                // Prepare modifiable list EB.FF.EOD.RO.DETS
                List<TField> roList = new ArrayList<>();
                List<TField> existingList = eodRec.getRoCollectionConcat();

                if (existingList != null) {
                    ftUpdConcatForRevCollect
                            .info("Existing RoCollectionConcat list found with size: {}" + existingList.size());
                    roList.addAll(existingList); // copy existing values
                }

                // Append new value
                ftUpdConcatForRevCollect.info("Appending new idConcat value: {}" + idConcat);
                roList.add(new TField(idConcat));
                ftUpdConcatForRevCollect.debug("New list size after adding idConcat: {}" + roList);

                // Set back to record
                for (int i = 0; i < roList.size(); i++) {
                    ftUpdConcatForRevCollect
                            .info("Setting RoCollectionConcat at index {} with value: {}" + i + roList.get(i));
                    eodRec.setRoCollectionConcat(roList.get(i), i);
                }

                ftUpdConcatForRevCollect
                        .info("RoRevCollectionConcat field updated successfully with {} values." + roList.size());

                // Write updated record back
                getIntoTheFfEodRoDetsRecordWritePrs(roTabId, eodRec);

                // Update UpdateEodConcat flag in first table
                collRec.setUpdateEodRevConcat("YES");

                getIntoTheFfFtCollRevConcatRecordWritePrs(idConcat, collRec);

            }
        } catch (Exception e) {
            ftUpdConcatForRevCollect.error("updateTheConcatTables" + e);
        }

    }

    /**
     * @param idConcat
     * @param collRec
     */
    private void getIntoTheFfFtCollRevConcatRecordWritePrs(String idConcat, EbFfFtCollRevConcatRecord collRec) {

        try {
            collTab.write(idConcat, collRec);
            ftUpdConcatForRevCollect.info("New record written successfully for idConcat: {}" + idConcat);
        } catch (Exception e) {
            ftUpdConcatForRevCollect
                    .error("Error writing new record for idConcat {}: {}" + idConcat + e.getMessage() + e);
        }

    }

    /**
     * @param roTabId
     * @param eodRec
     */
    private void getIntoTheFfEodRoDetsRecordWritePrs(String roTabId, EbFfEodRoDetsRecord eodRec) {

        try {
            ftUpdConcatForRevCollect.info("Writing record to table with roTabId: {}" + roTabId);
            eodTab.write(roTabId, eodRec);
            ftUpdConcatForRevCollect.info("Record write completed successfully.");
        } catch (Exception e) {
            ftUpdConcatForRevCollect.error("Error writing record to EB.FF.FT.EOD.RO.REV.DETS for roTabId {}: {}"
                    + roTabId + e.getMessage() + e);
        }

    }

    /**
     * @param roTabId
     * @param eodTab
     * @return
     */
    private EbFfEodRoDetsRecord getFfEodRoDetsRecord(String roTabId, EbFfEodRoDetsTable eodTab) {

        EbFfEodRoDetsRecord eodRec = null;
        // Read existing record if present
        try {
            eodRec = eodTab.read(roTabId);
            ftUpdConcatForRevCollect.info("Existing EB.FF.FT.EOD.RO.REV.DETS record found for roTabId: {}" + roTabId);
        } catch (Exception e) {
            ftUpdConcatForRevCollect
                    .error("No existing EB.FF.FT.EOD.RO.REV.DETS record found for roTabId: {}. Creating new record."
                            + roTabId);
            eodRec = new EbFfEodRoDetsRecord(this);
        }
        return eodRec;
    }

    /**
     * @param ymnemonic
     * @param tableName
     * @param idConcat
     * @return
     */
    private EbFfFtCollRevConcatRecord getFfFtCollRevConcatRecord(String ymnemonic, String tableName, String idConcat) {

        EbFfFtCollRevConcatRecord fetchedRecord = null;
        try {
            fetchedRecord = new EbFfFtCollRevConcatRecord(da.getRecord(ymnemonic, tableName, "", idConcat));
        } catch (Exception e) {
            ftUpdConcatForRevCollect
                    .error("FfFtUpdConcatForRevCollect:- Error fetching record for idConcat {}: {}" + idConcat + e);
        }
        return fetchedRecord;
    }

    private void postUpdateConcat(EbFfFtCollRevConcatRecord collRec1, String credAmount, String payMode,
            String idConcat) {
        EbFfFtCollRevConcatTable collTab1 = new EbFfFtCollRevConcatTable(this);

        try {
            BigDecimal credAmtBd = safeBigDecimal(credAmount);

            if ("BCP".equalsIgnoreCase(payMode)) {
                ftUpdConcatForRevCollect
                        .info("FfFtUpdConcatForRevCollect:- Updating BCP amount for idConcat: {}" + idConcat);
                BigDecimal bcpointOld = safeBigDecimal(collRec1.getBcPointRevCollected());
                collRec1.setBcPointRevCollected(bcpointOld.add(credAmtBd).toString());
            } else if ("OBS".equalsIgnoreCase(payMode)) {
                ftUpdConcatForRevCollect
                        .info("FfFtUpdConcatForRevCollect:- Updating OBS amount for idConcat: {}" + idConcat);
                BigDecimal otherBankOld = safeBigDecimal(collRec1.getBcPointRevCollected());
                collRec1.setBcPointRevCollected(otherBankOld.add(credAmtBd).toString());
            }

            collTab1.write(idConcat, collRec1);
            ftUpdConcatForRevCollect
                    .info("FfFtUpdConcatForRevCollect:- Record updated successfully for idConcat: {}" + idConcat);
        } catch (Exception e) {
            ftUpdConcatForRevCollect.error("FfFtUpdConcatForRevCollect:- Error updating record for idConcat {}: {}"
                    + idConcat + e.getMessage() + e);
        }
    }

    private String safeString(Object value) {
        return value == null ? "" : value.toString().trim();
    }

    private BigDecimal safeBigDecimal(Object value) {
        if (value == null)
            return BigDecimal.ZERO;
        String strValue = value.toString().trim();
        return strValue.isEmpty() ? BigDecimal.ZERO : new BigDecimal(strValue);
    }
}
