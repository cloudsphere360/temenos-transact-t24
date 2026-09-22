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
import com.temenos.t24.api.records.ebffrocollconcat.EbFfRoCollConcatRecord;
import com.temenos.t24.api.records.fundstransfer.FundsTransferRecord;
import com.temenos.t24.api.records.imdocumentimage.ImDocumentImageRecord;
import com.temenos.t24.api.system.DataAccess;
import com.temenos.t24.api.system.Session;
import com.temenos.t24.api.tables.ebffeodrodets.EbFfEodRoDetsTable;
import com.temenos.t24.api.tables.ebffrocollconcat.EbFfRoCollConcatTable;

/**
 * -----------------------------------------------------------------------------
 * Modification History :
 * -----------------------------------------------------------------------------
 * Description : This Routine used to update EB.FF.FT.COLLECTION.CONCAT table
 * based on the payment mode and get the data from Funds Transfer application
 * during authorisation.
 *
 * Developed By : Preethi Selvam
 *
 * Development Reference : Teller Report
 *
 * Attached To : VERSION>IM.DOCUMENT.IMAGE,CAPTURE.IMG >FF.AUTH.FT.COLLECTION
 * 
 * Attached As : Auth Routine
 * 
 * -----------------------------------------------------------------------------
 */
public class FfFtUpdConcatForCollect extends RecordLifecycle {

    private final Session ses = new Session();
    private final DataAccess da = new DataAccess(this);
    String todayDate = ses.getCurrentVariable("!TODAY");
    List<TField> roList = new ArrayList<>();

    FundsTransferRecord fundsTransferObj = null;

    private static final FusionFileLogger ftUpdConcatForCollect = FusionFileLogger
            .getLogger(FfFtUpdConcatForCollect.class);

    @Override
    public void postUpdateRequest(String application, String currentRecordId, TStructure currentRecord,
            List<TransactionData> transactionData, List<TStructure> currentRecords,
            TransactionContext transactionContext) {
        try {
            ftUpdConcatForCollect.info("FfFtUpdConcatForCollect: Routine triggered for Funds Transfer concat update.");
            ftUpdConcatForCollect.info("currentRecordId: {}" + currentRecordId);
            ftUpdConcatForCollect.info("currentRecord: {}" + currentRecord);

            ImDocumentImageRecord imgRecord = new ImDocumentImageRecord(currentRecord);
            String imgReferance = safeString(imgRecord.getImageReference());

            ftUpdConcatForCollect.info("Image Referance: {}" + imgReferance);

            String tableName = "EB.FF.RO.COLL.CONCAT";
            String compCode = safeString(ses.getCompanyId());
            String ymnemonic = safeString(ses.getCompanyRecord().getFinancialMne());

            ftUpdConcatForCollect.info("Company Code: {}" + compCode);
            ftUpdConcatForCollect.info("Mnemonic: {}" + ymnemonic);
            ftUpdConcatForCollect.info("Table Name: {}" + tableName);

            fundsTransferObj = new FundsTransferRecord(da.getRecord(ymnemonic, "FUNDS.TRANSFER", "", imgReferance));

            String processDate = safeString(fundsTransferObj.getProcessingDate());
            String postBy = safeString(fundsTransferObj.getLocalRefField("FF.POSTED.BY"));
            String postName = safeString(fundsTransferObj.getLocalRefField("FF.POST.LGLNAME"));
            String credAmount = safeString(fundsTransferObj.getCreditAmount());
            String payMode = safeString(fundsTransferObj.getLocalRefField("FF.PYMT.MODE"));

            ftUpdConcatForCollect.info("processDate: {}" + processDate);
            ftUpdConcatForCollect.info("postBy: {}" + postBy);
            ftUpdConcatForCollect.info("postName: {}" + postName);
            ftUpdConcatForCollect.info("credAmount: {}" + credAmount);
            ftUpdConcatForCollect.info("payMode: {}" + payMode);

            String idConcat = postBy + "-" + compCode + "-" + processDate;
            ftUpdConcatForCollect.info("FfFtUpdConcatForCollect:- idConcat: {}" + idConcat);

            // Fetch existing record
            TStructure fetchedRecord = getFfRoCollConcatRecord(ymnemonic, tableName, idConcat);

            if (fetchedRecord != null && !fetchedRecord.toString().trim().isEmpty()) {
                EbFfRoCollConcatRecord collRec1 = new EbFfRoCollConcatRecord(fetchedRecord);

                ftUpdConcatForCollect
                        .info("FfFtUpdConcatForCollect:- Existing record found for idConcat: {}" + idConcat);
                postUpdateConcat(collRec1, credAmount, payMode, idConcat);
            } else {
                ftUpdConcatForCollect
                        .info("FfFtUpdConcatForCollect:- No existing record found. Creating new record for idConcat: {}"
                                + idConcat);
                updateTheConcatTables(payMode, postBy, postName, processDate, compCode, credAmount, idConcat);

            }
        } catch (Exception e) {
            ftUpdConcatForCollect
                    .error("FfFtUpdConcatForCollect:- Unexpected error in postUpdateRequest: {}" + e.getMessage() + e);
        }
    }

    /**
     * @param payMode
     * @param postBy
     * @param postName
     * @param processDate
     * @param compCode
     * @param fundsTransferObj
     * @param credAmount
     * @param idConcat
     */
    private void updateTheConcatTables(String payMode, String postBy, String postName, String processDate,
            String compCode, String credAmount, String idConcat) {

        EbFfRoCollConcatRecord collRec = new EbFfRoCollConcatRecord(this);
        EbFfRoCollConcatTable collTab = new EbFfRoCollConcatTable(this);
        EbFfEodRoDetsTable eodTab = new EbFfEodRoDetsTable(this);

        collRec.setRoEmpId(postBy);
        collRec.setRoEmpName(postName);

        if ("BCP".equalsIgnoreCase(payMode)) {
            collRec.setBcPointCollected(credAmount);
        } else if ("OBS".equalsIgnoreCase(payMode)) {
            collRec.setOtherBankCollected(credAmount);
        }

        collRec.setDate(processDate);
        collRec.setBranch(compCode);

        String roTabId = compCode + "-" + todayDate;
        String updEodConcat = collRec.getUpdateEodConcat().toString();
        if (updEodConcat.isEmpty()) {
            ftUpdConcatForCollect.info(
                    "FfFtUpdConcatForCollect:- updEodConcat is empty. Proceeding to append idConcat: {}" + idConcat);

            EbFfEodRoDetsRecord eodRec = getEbFfEodRoDetsRecord(roTabId, eodTab);

            // Prepare modifiable list

            List<TField> existingList = eodRec.getRoCollectionConcat();

            if (existingList != null) {
                ftUpdConcatForCollect
                        .debug("FfFtUpdConcatForCollect:- Existing RoCollectionConcat list found with size: {}"
                                + existingList.size());
                roList.addAll(existingList); // copy existing values
            }

            roList.add(new TField(idConcat));
            ftUpdConcatForCollect
                    .debug("FfFtUpdConcatForCollect:- New list size after adding idConcat: {}" + roList.size());

            // Set back to record
            for (int i = 0; i < roList.size(); i++) {
                ftUpdConcatForCollect
                        .info("FfFtUpdConcatForCollect:- Setting RoCollectionConcat at index {} with value: {}" + i
                                + roList.get(i));
                eodRec.setRoCollectionConcat(roList.get(i), i);
            }

            ftUpdConcatForCollect
                    .info("FfFtUpdConcatForCollect:- RoCollectionConcat field updated successfully with {} values."
                            + roList.size());

            // Write updated record back
            getIntoTheFfEodRoDetsRecordWritePrs(roTabId, eodRec, eodTab);

            // Update UpdateEodConcat flag in first table
            collRec.setUpdateEodConcat("YES");
        }
        getIntoTheFfCollRoConcatRecordWritePrs(collTab, idConcat, collRec);
    }

    /**
     * @param collTab
     * @param idConcat
     * @param collRec
     */
    private void getIntoTheFfCollRoConcatRecordWritePrs(EbFfRoCollConcatTable collTab, String idConcat,
            EbFfRoCollConcatRecord collRec) {
        try {
            collTab.write(idConcat, collRec);
            ftUpdConcatForCollect
                    .info("FfFtUpdConcatForCollect:- New record written successfully for idConcat: {}" + idConcat);
        } catch (Exception e) {
            ftUpdConcatForCollect.error("FfFtUpdConcatForCollect:- Error writing new record for idConcat {}: {}"
                    + idConcat + e.getMessage(), e);
        }

    }

    /**
     * @param roTabId
     * @param eodRec
     * @param eodTab
     */
    private void getIntoTheFfEodRoDetsRecordWritePrs(String roTabId, EbFfEodRoDetsRecord eodRec,
            EbFfEodRoDetsTable eodTab) {
        try {
            ftUpdConcatForCollect.info("FfFtUpdConcatForCollect:- Writing record to table with roTabId: {}" + roTabId);
            eodTab.write(roTabId, eodRec);

        } catch (Exception e) {
            ftUpdConcatForCollect
                    .error("FfFtUpdConcatForCollect:- Error writing record to EB.FF.FT.EOD.RO.DETS for roTabId {}: {}"
                            + roTabId + e.getMessage() + e);
        }

    }

    /**
     * @param roTabId
     * @param eodTab
     * @return
     */
    private EbFfEodRoDetsRecord getEbFfEodRoDetsRecord(String roTabId, EbFfEodRoDetsTable eodTab) {
        EbFfEodRoDetsRecord eodRec = null;
        // Read existing record if present
        try {
            eodRec = eodTab.read(roTabId);
            ftUpdConcatForCollect.info(
                    "FfFtUpdConcatForCollect:- Existing EB.FF.EOD.RO.DETS record found for roTabId: {}" + roTabId);
        } catch (Exception e) {
            ftUpdConcatForCollect.error(
                    "FfFtUpdConcatForCollect:- No existing EB.FF.EOD.RO.DETS record found for roTabId: {}. Creating new record."
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
    private TStructure getFfRoCollConcatRecord(String ymnemonic, String tableName, String idConcat) {
        TStructure fetchedRecord = null;
        try {
            fetchedRecord = da.getRecord(ymnemonic, tableName, "", idConcat);
        } catch (Exception e) {
            ftUpdConcatForCollect.error("FfFtUpdConcatForCollect:- Error fetching record for idConcat {}: {}" + idConcat
                    + e.getMessage() + e);
        }
        return fetchedRecord;
    }

    private void postUpdateConcat(EbFfRoCollConcatRecord collRec1, String credAmount, String payMode, String idConcat) {
        EbFfRoCollConcatTable collTab1 = new EbFfRoCollConcatTable(this);

        try {
            BigDecimal credAmtBd = safeBigDecimal(credAmount);
            BigDecimal bcpointOld = safeBigDecimal(collRec1.getBcPointCollected());
            BigDecimal otherBankOld = safeBigDecimal(collRec1.getOtherBankCollected());

            if ("BCP".equalsIgnoreCase(payMode)) {
                ftUpdConcatForCollect.info("FfFtUpdConcatForCollect:- Updating BCP amount for idConcat: {}" + idConcat);
                collRec1.setBcPointCollected(bcpointOld.add(credAmtBd).toString());
            } else if ("OBS".equalsIgnoreCase(payMode)) {
                ftUpdConcatForCollect.info("FfFtUpdConcatForCollect:- Updating OBS amount for idConcat: {}" + idConcat);
                collRec1.setOtherBankCollected(otherBankOld.add(credAmtBd).toString());
            }
 
            collTab1.write(idConcat, collRec1);
            ftUpdConcatForCollect
                    .info("FfFtUpdConcatForCollect:- Record updated successfully for idConcat: {}" + idConcat);
        } catch (Exception e) {
            ftUpdConcatForCollect.error("FfFtUpdConcatForCollect:- Error updating record for idConcat {}: {}" + idConcat
                    + e.getMessage() + e);
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
