package com.temenos.fusion;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import com.temenos.api.TField;
import com.temenos.api.TStructure;
import com.temenos.logging.facade.Logger;
import com.temenos.logging.facade.LoggerFactory;
import com.temenos.t24.api.complex.eb.servicehook.TransactionData;
import com.temenos.t24.api.complex.eb.templatehook.TransactionContext;
import com.temenos.t24.api.hook.system.RecordLifecycle;
import com.temenos.t24.api.records.ebffeodrodets.EbFfEodRoDetsRecord;
import com.temenos.t24.api.records.ebffrocollconcat.EbFfRoCollConcatRecord;
import com.temenos.t24.api.records.fundstransfer.FundsTransferRecord;
import com.temenos.t24.api.system.DataAccess;
import com.temenos.t24.api.system.Session;
import com.temenos.t24.api.tables.ebffeodrodets.EbFfEodRoDetsTable;
import com.temenos.t24.api.tables.ebffrocollconcat.EbFfRoCollConcatTable;

/**
 *
 * @author mp116300
 */
public class FfUpdateConcatForCollectionFt extends RecordLifecycle {

    private final Session ses = new Session();
    private final DataAccess da = new DataAccess();
    private static final String L3API = "L3API";
    private static final Logger LOGGER = LoggerFactory.getLogger(L3API);
    
    

    @Override
    public void postUpdateRequest(String application, String currentRecordId, TStructure currentRecord,
            List<TransactionData> transactionData, List<TStructure> currentRecords,
            TransactionContext transactionContext) {
        try {
            LOGGER.info("Routine triggered for Funds Transfer concat update.");
            String tableName = "EB.FF.RO.COLL.CONCAT";
            String compCode = safeString(ses.getCompanyId());
            String ymnemonic = safeString(ses.getCompanyRecord().getFinancialMne());
            String todayDate = ses.getCurrentVariable("!TODAY");
            LOGGER.info("Company Code: {}", compCode);
            LOGGER.info("Mnemonic: {}", ymnemonic);
            LOGGER.info("Table Name: {}", tableName);

            FundsTransferRecord fundsTransferObj = new FundsTransferRecord(currentRecord);

            String processDate = safeString(fundsTransferObj.getProcessingDate());
            String postBy = safeString(fundsTransferObj.getLocalRefField("FF.POSTED.BY"));
            String postName = safeString(fundsTransferObj.getLocalRefField("FF.POST.LGLNAME"));
            String credAmount = safeString(fundsTransferObj.getCreditAmount());
            String payMode = safeString(fundsTransferObj.getLocalRefField("FF.PYMT.MODE"));

            LOGGER.info("processDate: {}", processDate);
            LOGGER.info("postBy: {}", postBy);
            LOGGER.info("postName: {}", postName);
            LOGGER.info("credAmount: {}", credAmount);
            LOGGER.info("payMode: {}", payMode);

            String idConcat = postBy + "-" + compCode + "-" + processDate;
            LOGGER.info("idConcat: {}", idConcat);

            EbFfRoCollConcatTable collTab = new EbFfRoCollConcatTable(this);

            // Fetch existing record
            TStructure fetchedRecord = null;
            try {
                fetchedRecord = da.getRecord(ymnemonic, tableName, "", idConcat);
            } catch (Exception e) {
                LOGGER.error("Error fetching record for idConcat {}: {}", idConcat, e.getMessage(), e);
            }

            if (fetchedRecord != null && !fetchedRecord.toString().trim().isEmpty()) {
                EbFfRoCollConcatRecord collRec1 = new EbFfRoCollConcatRecord(fetchedRecord);
                LOGGER.info("Existing record found for idConcat: {}", idConcat);
                postUpdateConcat(collRec1, credAmount, payMode, idConcat);
            } else {
                LOGGER.info("No existing record found. Creating new record for idConcat: {}", idConcat);
                EbFfRoCollConcatRecord collRec = new EbFfRoCollConcatRecord(this);

                collRec.setRoEmpId(postBy);
                collRec.setRoEmpName(postName);

                if ("CASH".equalsIgnoreCase(payMode)) {
                    collRec.setCashCollected(credAmount);
                }
                else if("UPI".equalsIgnoreCase(payMode)||("BBPS".equalsIgnoreCase(payMode))){
                    collRec.setDigitalCollected(credAmount);
                }
                BigDecimal cashAmt = safeBigDecimal(collRec.getCashCollected());
                BigDecimal digitalAmt = safeBigDecimal(collRec.getDigitalCollected());
               
                BigDecimal totCollected = cashAmt.add(digitalAmt);

                collRec.setTotalCollected(totCollected.toString());
                collRec.setDate(processDate);
                collRec.setBranch(compCode);

                String roTabId = compCode + "-" + todayDate;
                String updEodConcat = collRec.getUpdateEodConcat().toString();
                if (updEodConcat.isEmpty()) {
                    LOGGER.info("updEodConcat is empty. Proceeding to append idConcat: {}", idConcat);

                    EbFfEodRoDetsTable eodTab = new EbFfEodRoDetsTable(this);
                    EbFfEodRoDetsRecord eodRec;

                    // Read existing record if present
                    try {
                        eodRec = eodTab.read(roTabId);
                        LOGGER.info("Existing EB.FF.EOD.RO.DETS record found for roTabId: {}", roTabId);
                    } catch (Exception e) {
                        LOGGER.warn("No existing EB.FF.EOD.RO.DETS record found for roTabId: {}. Creating new record.",
                                roTabId);
                        eodRec = new EbFfEodRoDetsRecord(this);
                    }

                    // Prepare modifiable list
                    List<TField> roList = new ArrayList<>();
                    List<TField> existingList = eodRec.getRoCollectionConcat();

                    if (existingList != null) {
                        LOGGER.debug("Existing RoCollectionConcat list found with size: {}", existingList.size());
                        for (int i = 0; i < existingList.size(); i++) {
                            LOGGER.trace("Existing value at index {}: {}", i, existingList.get(i));
                        }
                        roList.addAll(existingList); // copy existing values
                    } else {
                        LOGGER.debug("No existing RoCollectionConcat list found. Initializing new list.");
                    }

                    // Append new value
                    LOGGER.info("Appending new idConcat value: {}", idConcat);
                    roList.add(new TField(idConcat));
                    LOGGER.debug("New list size after adding idConcat: {}", roList.size());

                    // Set back to record
                    for (int i = 0; i < roList.size(); i++) {
                        LOGGER.trace("Setting RoCollectionConcat at index {} with value: {}", i, roList.get(i));
                        eodRec.setRoCollectionConcat(roList.get(i), i);
                    }

                    LOGGER.info("RoCollectionConcat field updated successfully with {} values.", roList.size());

                    // Write updated record back
                    try {
                        LOGGER.info("Writing record to table with roTabId: {}", roTabId);
                        eodTab.write(roTabId, eodRec);
                        LOGGER.info("Record write completed successfully.");
                    } catch (Exception e) {
                        LOGGER.error("Error writing record to EB.FF.EOD.RO.DETS for roTabId {}: {}", roTabId,
                                e.getMessage(), e);
                    }

                    // Update UpdateEodConcat flag in first table
                    collRec.setUpdateEodConcat("YES");
                }

                try {
                    collTab.write(idConcat, collRec);
                    LOGGER.info("New record written successfully for idConcat: {}", idConcat);
                } catch (Exception e) {
                    LOGGER.error("Error writing new record for idConcat {}: {}", idConcat, e.getMessage(), e);
                }
            }
        } catch (Exception e) {
            LOGGER.error("Unexpected error in postUpdateRequest: {}", e.getMessage(), e);
        }
    }

    private void postUpdateConcat(EbFfRoCollConcatRecord collRec1, String credAmount, String payMode, String idConcat) {
        EbFfRoCollConcatTable collTab1 = new EbFfRoCollConcatTable(this);

        try {
            BigDecimal credAmtBd = safeBigDecimal(credAmount);

            if ("CASH".equalsIgnoreCase(payMode)) {
                LOGGER.info("Updating CASH amount for idConcat: {}", idConcat);
                BigDecimal cashCollectOld = safeBigDecimal(collRec1.getCashCollected());
                collRec1.setCashCollected(cashCollectOld.add(credAmtBd).toString());
            } else if("UPI".equalsIgnoreCase(payMode)||("BBPS".equalsIgnoreCase(payMode))){
                LOGGER.info("Updating DIGITAL amount for idConcat: {}", idConcat);
                BigDecimal digiCollectOld = safeBigDecimal(collRec1.getDigitalCollected());
                collRec1.setDigitalCollected(digiCollectOld.add(credAmtBd).toString());
            }
            
            BigDecimal cashAmt = safeBigDecimal(collRec1.getCashCollected());
            BigDecimal digiAmt = safeBigDecimal(collRec1.getDigitalCollected());
            BigDecimal totAmtFinal = cashAmt.add(digiAmt);

            collRec1.setTotalCollected(totAmtFinal.toString());

            collTab1.write(idConcat, collRec1);
            LOGGER.info("Record updated successfully for idConcat: {}", idConcat);
        } catch (Exception e) {
            LOGGER.error("Error updating record for idConcat {}: {}", idConcat, e.getMessage(), e);
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