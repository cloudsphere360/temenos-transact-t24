package com.temenos.fusion;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.temenos.api.TField;
import com.temenos.api.TStructure;
import com.temenos.t24.api.complex.eb.servicehook.ServiceData;
import com.temenos.t24.api.complex.eb.servicehook.TransactionData;
import com.temenos.t24.api.hook.system.ServiceLifecycle;
import com.temenos.t24.api.records.aaarrangement.AaArrangementRecord;
import com.temenos.t24.api.records.aaarrangementactivity.AaArrangementActivityRecord;
import com.temenos.t24.api.records.aaarrangementactivity.PropertyClass;
import com.temenos.t24.api.records.company.CompanyRecord;
import com.temenos.t24.api.records.customer.CustomerRecord;
import com.temenos.t24.api.records.ebffcustdpd.EbFfCustDpdRecord;
import com.temenos.t24.api.records.ebffloandpd.DateClass;
import com.temenos.t24.api.records.ebffloandpd.EbFfLoanDpdRecord;
import com.temenos.t24.api.system.DataAccess;
import com.temenos.t24.api.system.Session;
import com.temenos.t24.api.tables.ebffcustdpd.EbFfCustDpdTable;

public class FfSerResetDpdCheck extends ServiceLifecycle {
    private static final FusionFileLogger ffSerResetDpdChecklog = FusionFileLogger.getLogger(FfSerResetDpdCheck.class);
    private final Map<String, AaArrangementRecord> arrangementCache = new HashMap<>();
    private final DataAccess dataAccess = new DataAccess(this);
    private String finMnemonic;
    private String cusMnemonic;

    @Override
    public List<String> getIds(ServiceData serviceData, List<String> controlList) {

        try {
            initialiseCompanyInfo(serviceData);
            return dataAccess.selectRecords(cusMnemonic, "EB.FF.CUST.DPD.CONCAT", "", "");
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private void initialiseCompanyInfo(ServiceData serviceData) {

        String companyId = serviceData.getCompanyId();
        CompanyRecord companyObj = new CompanyRecord(dataAccess.getRecord("COMPANY", companyId));

        finMnemonic = companyObj.getFinancialMne().getValue();
        cusMnemonic = companyObj.getCustomerMnemonic().getValue();
    }

    @Override
    public void postUpdateRequest(String id, ServiceData serviceData, String controlItem,
            List<TransactionData> transactionData, List<TStructure> records) {
        Session session = new Session(this);
        String today = session.getCurrentVariable("!TODAY");

        try {

            List<String> loanRecords = dataAccess.selectRecords(finMnemonic, "EB.FF.LOAN.DPD", "",
                    "WITH CUSTOMER EQ " + id);

            if (loanRecords == null || loanRecords.isEmpty()) {
                return; // No loans
            }

            boolean allLoansZeroCurDpd = true;
            Set<String> uniqueArrangements = new HashSet<>();

            for (String recordId : loanRecords) {

                EbFfLoanDpdRecord loanRec = new EbFfLoanDpdRecord(
                        dataAccess.getRecord(finMnemonic, "EB.FF.LOAN.DPD", "", recordId));

                List<DateClass> dates = loanRec.getDate();

                if (dates != null && !dates.isEmpty()) {

                    DateClass latest = dates.get(dates.size() - 1);
                    int curDpd = parseIntSafe(latest.getCurDpd().getValue());

                    if (curDpd > 0) {
                        allLoansZeroCurDpd = false;
                        break;
                    }
                    String arrangementId = recordId.split("-")[0];
                    uniqueArrangements.add(arrangementId);
                }
            }
            if (!allLoansZeroCurDpd) {
                return;
            }

            // Update customer only once
            boolean updated = updateCustFlag(id, transactionData, records);

            if (updated) {

                updateCustomerReleaseDate(id, today); // call method to update DtOfFlagRelease
            }

            // Trigger reset activity per unique arrangement
            for (String arrangementId : uniqueArrangements) {
                triggerResetOverdueActivity(arrangementId, transactionData, records);
            }

        } catch (Exception e) {
            // Minimal logging to avoid COB failure
            ffSerResetDpdChecklog.error("Reset DPD Check Error for Customer: " + id);
        }
    }

    private void updateCustomerReleaseDate(String customerId, String today) {
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
            LocalDate todayDate = LocalDate.parse(today, formatter);
            String formatted = todayDate.getMonth().toString().substring(0, 3) + todayDate.getYear();
            String custId = "CUS" + customerId + "-" + formatted;

            EbFfCustDpdRecord custDpdRecord = getCustDpdRecord(custId);
            if (custDpdRecord == null)
                return; // no record available

            List<com.temenos.t24.api.records.ebffcustdpd.DateClass> dateList = custDpdRecord.getDate();

            boolean updated = false;
            for (com.temenos.t24.api.records.ebffcustdpd.DateClass dateCls : dateList) {
                LocalDate recordDate = LocalDate.parse(dateCls.getDate().getValue(), formatter);

                if (recordDate.equals(todayDate)) {
                    // Set release date
                    dateCls.setDtOfFlagRelease(today);
                    updated = true;
                    break; // stop after updating today’s record
                }
            }

            if (updated) {
                // Write updated record once
                EbFfCustDpdTable table = new EbFfCustDpdTable(this);
                table.write(custId, custDpdRecord);
                ffSerResetDpdChecklog.info("DtOfFlagRelease updated for customer " + customerId);
            }

        } catch (Exception e) {
            ffSerResetDpdChecklog.error("Failed to update release date for customer " + customerId);
        }
    }

    private EbFfCustDpdRecord getCustDpdRecord(String custId) {
        try {
            return new EbFfCustDpdRecord(dataAccess.getRecord(cusMnemonic, "EB.FF.CUST.DPD", "", custId));
        } catch (Exception e) {
            // Log if needed, or just return null
            return null;
        }
    }

    private void triggerResetOverdueActivity(String arrangementId, List<TransactionData> transactionData,
            List<TStructure> records) {

        try {
            AaArrangementRecord arrRecord = arrangementCache.get(arrangementId);
            if (arrRecord == null) {
                arrRecord = new AaArrangementRecord(
                        dataAccess.getRecord(finMnemonic, "AA.ARRANGEMENT", "", arrangementId));
                arrangementCache.put(arrangementId, arrRecord);
            }
            TField curCmpyId = arrRecord.getCoCodeRec();
            AaArrangementActivityRecord aaaRec = new AaArrangementActivityRecord(this);

            PropertyClass propertyRec = new PropertyClass();

            aaaRec.setArrangement(arrangementId);
            aaaRec.setActivity("LENDING-MANUAL.RESET-DPD.STAGES");
            propertyRec.setProperty("DPD.STAGES");
            aaaRec.addProperty(propertyRec);

            TransactionData transData = new TransactionData();
            transData.setFunction("INPUT");
            transData.setSourceId("FF.OFS.UPD");
            transData.setNumberOfAuthoriser("0");
            transData.setCompanyId(curCmpyId.toString());
            transData.setVersionId("AA.ARRANGEMENT.ACTIVITY,FF.PRINCIPLE.UPDATE");

            transactionData.add(transData);
            records.add(aaaRec.toStructure());

        } catch (Exception e) {
            ffSerResetDpdChecklog.error("Activity Trigger Failed: " + arrangementId);
        }
    }

    private boolean updateCustFlag(String id, List<TransactionData> transactionData, List<TStructure> records) {

        try {

            CustomerRecord cusRecord = new CustomerRecord(dataAccess.getRecord("CUSTOMER", id));

            cusRecord.getLocalRefField("FF.NPA.CUSTOMER").setValue("NULL");

            TransactionData transData = new TransactionData();
            transData.setFunction("INPUT");
            transData.setSourceId("FF.OFS.UPD");
            transData.setNumberOfAuthoriser("0");
            transData.setTransactionId(id);
            transData.setVersionId("CUSTOMER,FF.UPDATE");

            transactionData.add(transData);
            records.add(cusRecord.toStructure());
            return true;
        } catch (Exception e) {
            ffSerResetDpdChecklog.error("Customer Update Failed: " + id);
        }
        return false;
    }

    private static int parseIntSafe(Object value) {
        try {
            if (value == null)
                return 0;
            return (int) Double.parseDouble(value.toString());
        } catch (Exception e) {
            return 0;
        }
    }
}