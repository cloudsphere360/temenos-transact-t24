package com.temenos.fusion;

/*-----------------------------------------------------------------------------
 * @author Vinothini P
 * Date Created:
 * Attached as : service Routine
 * EB.API : NA
 * Attached to :NA
 * Description: this routine is used calculate customer DPD.
 *------------------------------------------------------------------------------ 
 * Modification History :
 *----------------------------------------------------------------------------- 
 *22-Aug-2025   Development      Initial Version
 *-----------------------------------------------------------------------------
 */
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
import com.temenos.t24.api.records.aaaccountdetails.AaAccountDetailsRecord;
import com.temenos.t24.api.records.aaaccountdetails.BillIdClass;
import com.temenos.t24.api.records.aaaccountdetails.BillPayDateClass;
import com.temenos.t24.api.records.aaarrangement.AaArrangementRecord;
import com.temenos.t24.api.records.aaarrangementactivity.AaArrangementActivityRecord;
import com.temenos.t24.api.records.aaarrangementactivity.FieldNameClass;
import com.temenos.t24.api.records.aaarrangementactivity.PropertyClass;
import com.temenos.t24.api.records.company.CompanyRecord;
import com.temenos.t24.api.records.customer.CustomerRecord;
import com.temenos.t24.api.records.ebffcustdpd.EbFfCustDpdRecord;
import com.temenos.t24.api.records.ebffdpdparam.EbFfDpdParamRecord;
import com.temenos.t24.api.records.ebffdpdparam.ExcludeProductClass;
import com.temenos.t24.api.records.ebfffreezedpd.EbFfFreezeDpdRecord;
import com.temenos.t24.api.records.ebfffreezedpd.FreezeDpdClass;
import com.temenos.t24.api.records.ebffloandpd.DateClass;
import com.temenos.t24.api.records.ebffloandpd.EbFfLoanDpdRecord;
import com.temenos.t24.api.system.DataAccess;
import com.temenos.t24.api.system.Session;
import com.temenos.t24.api.tables.ebffcustdpd.EbFfCustDpdTable;

public class FfSerCustomerDpd extends ServiceLifecycle {
    private static final FusionFileLogger FfSerCustomerDpdLog = FusionFileLogger.getLogger(FfSerCustomerDpd.class);
    static final String EBFFLOANDPD = "EB.FF.LOAN.DPD";
    static final String YYYYMMDD = "yyyyMMdd";
    // Load once – do not reload inside loops
    private EbFfDpdParamRecord dpdParamObj;
    private List<ExcludeProductClass> excludeList;

    private final Map<String, AaArrangementRecord> arrangementCache = new HashMap<>();
    private final Map<String, AaAccountDetailsRecord> accountDetailsCache = new HashMap<>();

    private final DataAccess dataAccess = new DataAccess(this);
    private String finMnemonic;
    private String cusMnemonic;

    @Override
    public List<String> getIds(ServiceData serviceData, List<String> controlList) {

        FfSerCustomerDpdLog.info("FfSerCustDpd:getIds Method triggered");
        initialiseCompanyInfo(serviceData);

        // Load DPD PARAM only once

        return dataAccess.selectRecords(cusMnemonic, "EB.FF.CUST.DPD.CONCAT", "", "");
    }

    private void initialiseCompanyInfo(ServiceData serviceData) {
        try {
            String companyId = serviceData.getCompanyId();
            CompanyRecord companyObj = new CompanyRecord(dataAccess.getRecord("COMPANY", companyId));
            finMnemonic = companyObj.getFinancialMne().getValue();
            cusMnemonic = companyObj.getCustomerMnemonic().getValue();
        } catch (Exception e) {
            FfSerCustomerDpdLog.error("initialiseCompanyInfo : ", e);
        }

    }

    @Override
    public void postUpdateRequest(String id, ServiceData serviceData, String controlItem,
            List<TransactionData> transactionData, List<TStructure> records) {
        try {
            FfSerCustomerDpdLog
                    .info("***************UPDATE CUSTOMER TABLE METHOD TRIGGERED************************" + id);
            Session sessionObj = new Session(this);
            String today = sessionObj.getCurrentVariable("!TODAY");

            loadDpdParameters();

            // Prepare customer DPD record

            DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern(YYYYMMDD);
            LocalDate date = LocalDate.parse(today, inputFormatter);
            String formatted = date.getMonth().toString().substring(0, 3) + date.getYear();
            String custId = "CUS" + id + "-" + formatted;

            // --- Check FreezeDpd before processing arrangements ---
            EbFfCustDpdRecord custDpdRecord = loadCustDpdRecord(id);
            EbFfFreezeDpdRecord freezeDpdRecord = loadFreezeRecord(id);

            if (freezeDpdRecord != null && checkFreezeCheck(freezeDpdRecord, id, date, inputFormatter)) {
                return;
            }

            processArrangements(id, today, transactionData, records, custId, custDpdRecord);

        } catch (Exception e) {
            FfSerCustomerDpdLog.error("unable to process the record" + e);
        }

    }

    private void loadDpdParameters() {
        try {
            dpdParamObj = new EbFfDpdParamRecord(dataAccess.getRecord("", "EB.FF.DPD.PARAM", "", "SYSTEM"));
            excludeList = dpdParamObj.getExcludeProduct();
        } catch (Exception e) {
            FfSerCustomerDpdLog.error("Unable to load DPD PARAM", e);
        }
    }

    private void processArrangements(String id, String today, List<TransactionData> transactionData,
            List<TStructure> records, String custId, EbFfCustDpdRecord custDpdRecord) {

        List<String> arrangementList = dataAccess.selectRecords(finMnemonic, EBFFLOANDPD, "", "WITH CUSTOMER EQ " + id);

        List<String> npaLoans = new ArrayList<>();
        boolean anyLoanNpa = false;

        DpdMetrics metrics = new DpdMetrics();

        for (String arrangementId : arrangementList) {

            LoanProcessResult result = processSingleArrangement(arrangementId, today, id, transactionData, records,
                    metrics, npaLoans);

            if (result == null) {
                continue;
            }

            if (result.isNpa()) {
                anyLoanNpa = true;
            }
        }

        if (anyLoanNpa) {
            markAllLoansAsNpa(arrangementList, npaLoans);
        }

        CustomerUpdateContext context = new CustomerUpdateContext();
        context.id = id;
        context.custId = custId;
        context.today = today;
        context.npaLoans = npaLoans;
        context.transactionData = transactionData;
        context.records = records;
        context.custDpdRecord = custDpdRecord;
        context.anyLoanNpa = anyLoanNpa;
        context.metrics = metrics;

        finalizeCustomerUpdate(context);
    }

    private LoanProcessResult processSingleArrangement(String arrangementId, String today, String customerId,
            List<TransactionData> transactionData, List<TStructure> records, DpdMetrics metrics,
            List<String> npaLoans) {

        String baseArrangementId = arrangementId.split("-")[0];

        EbFfLoanDpdRecord loanDpdRecord = new EbFfLoanDpdRecord(
                dataAccess.getRecord(finMnemonic, EBFFLOANDPD, "", arrangementId));

        List<DateClass> dates = loanDpdRecord.getDate();
        if (dates == null || dates.isEmpty()) {
            return null;
        }

        DateClass latest = dates.get(dates.size() - 1);

        int curDpd = parseIntSafe(latest.getCurDpd());
        int peakDpd = parseIntSafe(latest.getPeakDpd());
        double avgDpd = parseDoubleSafe(latest.getAvgDpd());

        String dpdStatus = latest.getDpdStatus() != null ? latest.getDpdStatus().getValue() : "";

        if (checkParamCondition(arrangementId, today)) {
            return null;
        }

        updateMaxMetrics(arrangementId, curDpd, peakDpd, avgDpd, dpdStatus, metrics);

        if (isLoanNpa(dpdStatus)) {

            addUniqueLoan(baseArrangementId, npaLoans);

            metrics.isCustomerNpaToday = checkDpdParam(dpdStatus, customerId, transactionData, records,
                    metrics.isCustomerNpaToday);

            return new LoanProcessResult(true);
        }

        return new LoanProcessResult(false);
    }

    private void updateMaxMetrics(String arrangementId, int curDpd, int peakDpd, double avgDpd, String dpdStatus,
            DpdMetrics metrics) {

        if (curDpd > metrics.maxCurDpd) {
            metrics.maxCurDpd = curDpd;
            metrics.maxCurDpdArrangementId = arrangementId;
            metrics.maxDpdStatus = dpdStatus;
        }
        // After processing all loans
        if (metrics.maxCurDpd == 0) {
            // All loans have curDpd = 0 → DPD status is CUR
            metrics.maxDpdStatus = "CUR";
        }

        if (peakDpd > metrics.maxPeakDpd) {
            metrics.maxPeakDpd = peakDpd;
            metrics.maxPeakDpdArrangementId = arrangementId;
        }

        if (avgDpd > metrics.maxAvgDpd) {
            metrics.maxAvgDpd = avgDpd;
            metrics.maxAvgDpdArrangementId = arrangementId;
        }

    }

    private boolean isLoanNpa(String dpdStatus) {
        return "NPA".equalsIgnoreCase(dpdStatus);
    }

    private void addUniqueLoan(String loanId, List<String> npaLoans) {
        if (!npaLoans.contains(loanId)) {
            npaLoans.add(loanId);
        }
    }

    private void markAllLoansAsNpa(List<String> arrangementList, List<String> npaLoans) {
        npaLoans.clear();
        for (String arrangementId : arrangementList) {

            String baseArrangementId = arrangementId.split("-")[0];

            npaLoans.add(baseArrangementId);

        }
    }

    private static class DpdMetrics {

        int maxCurDpd = 0;
        int maxPeakDpd = 0;
        double maxAvgDpd = 0;

        String maxCurDpdArrangementId = null;
        String maxPeakDpdArrangementId = null;
        String maxAvgDpdArrangementId = null;

        String maxDpdStatus = null;
        boolean isCustomerNpaToday = false;
    }

    private static class LoanProcessResult {

        private final boolean npa;

        public LoanProcessResult(boolean npa) {
            this.npa = npa;
        }

        public boolean isNpa() {
            return npa;
        }
    }

    private void finalizeCustomerUpdate(CustomerUpdateContext context) {

        if (context.anyLoanNpa) {
            FfSerCustomerDpdLog.info("Final NPA loans for customer " + context.id + ": " + context.npaLoans);
            updateDpdStatus(context.npaLoans, "NPA", context.transactionData, context.records);
        }

        updateFfCustDpdTable(context.custId, context.custDpdRecord, context.today, context.metrics);
    }

    private static class CustomerUpdateContext {

        String id;
        String custId;
        String today;

        List<String> npaLoans;
        List<TransactionData> transactionData;
        List<TStructure> records;

        EbFfCustDpdRecord custDpdRecord;

        boolean anyLoanNpa;

        DpdMetrics metrics;
    }

    private EbFfFreezeDpdRecord loadFreezeRecord(String id) {
        EbFfFreezeDpdRecord freezeDpdRecord = null;
        try {
            freezeDpdRecord = new EbFfFreezeDpdRecord(dataAccess.getRecord(cusMnemonic, "EB.FF.FREEZE.DPD", "", id));
        } catch (Exception e) {
            FfSerCustomerDpdLog.info("NO existing freeze record " + e);
        }
        return freezeDpdRecord;
    }

    private EbFfCustDpdRecord loadCustDpdRecord(String id) {
        EbFfCustDpdRecord custDpdRecord = null;
        try {
            custDpdRecord = new EbFfCustDpdRecord(
                    dataAccess.getRecord(cusMnemonic, "EB.FF.CUST.DPD", "", "CUS" + id + "-" + getFormattedDate()));
        } catch (Exception e) {
            FfSerCustomerDpdLog.info("NO existing cust dpd record " + e);
        }
        return custDpdRecord;
    }

    private void updateFfCustDpdTable(String custId, EbFfCustDpdRecord custDpdRecord, String today,
            DpdMetrics metrics) {

        try {

            FfSerCustomerDpdLog.info("Update cust table method triggered");

            EbFfCustDpdTable custDpdTable = new EbFfCustDpdTable(this);

            custDpdRecord = loadOrCreateCustDpdRecord(custId, custDpdRecord);

            List<com.temenos.t24.api.records.ebffcustdpd.DateClass> existingDates = custDpdRecord.getDate();
            boolean dateExists = false;
            com.temenos.t24.api.records.ebffcustdpd.DateClass datecls = null;

            for (com.temenos.t24.api.records.ebffcustdpd.DateClass existing : existingDates) {

                if (today.equals(existing.getDate().getValue())) {

                    dateExists = true;
                    datecls = existing;
                    break;
                }
            }

            if (!dateExists) {

                datecls = new com.temenos.t24.api.records.ebffcustdpd.DateClass();

                datecls.setDate(today);
                datecls.setCurDpd(String.valueOf(metrics.maxCurDpd));
                datecls.setCurDpdAcRef(metrics.maxCurDpdArrangementId);

                datecls.setPeakDpd(String.valueOf(metrics.maxPeakDpd));
                datecls.setPeakDpdAcRef(metrics.maxPeakDpdArrangementId);

                datecls.setAvgDpd(String.valueOf(metrics.maxAvgDpd));
                datecls.setAvgDpdRef(metrics.maxAvgDpdArrangementId);

                datecls.setDpdStatus(metrics.maxDpdStatus);

                custDpdRecord.addDate(datecls);

            } else {

                if (metrics.maxCurDpd > parseIntSafe(datecls.getCurDpd())) {
                    datecls.setCurDpd(String.valueOf(metrics.maxCurDpd));
                    datecls.setCurDpdAcRef(metrics.maxCurDpdArrangementId);
                }

                if (metrics.maxPeakDpd > parseIntSafe(datecls.getPeakDpd())) {
                    datecls.setPeakDpd(String.valueOf(metrics.maxPeakDpd));
                    datecls.setPeakDpdAcRef(metrics.maxPeakDpdArrangementId);
                }

                if (metrics.maxAvgDpd > parseIntSafe(datecls.getAvgDpd())) {
                    datecls.setAvgDpd(String.valueOf(metrics.maxAvgDpd));
                    datecls.setAvgDpdRef(metrics.maxAvgDpdArrangementId);
                }

                datecls.setDate(today);
                datecls.setDpdStatus(metrics.maxDpdStatus);
            }

            if (metrics.isCustomerNpaToday) {
                datecls.setDtOfFlagset(today);
            }

            custDpdTable.write(custId, custDpdRecord);

        } catch (Exception e) {
            FfSerCustomerDpdLog.error("Unable to update the custdpd for " + custId + ": " + e);
        }
    }

    private EbFfCustDpdRecord loadOrCreateCustDpdRecord(String custId, EbFfCustDpdRecord custDpdRecord) {

        if (custDpdRecord == null) {
            try {
                custDpdRecord = new EbFfCustDpdRecord(dataAccess.getRecord(cusMnemonic, "EB.FF.CUST.DPD", "", custId));
                FfSerCustomerDpdLog.info("Existing customer DPD record found for " + custId);
            } catch (Exception e) {
                custDpdRecord = new EbFfCustDpdRecord(this); // Create new if doesn't exist
                FfSerCustomerDpdLog.info("No existing record found — creating new record for " + custId);
            }
        }

        return custDpdRecord;
    }

    private boolean checkFreezeCheck(EbFfFreezeDpdRecord freezeDpdRecord, String id, LocalDate date,
            DateTimeFormatter inputFormatter) {

        FfSerCustomerDpdLog.info("FfSerCusFreezeCheck:checkFreezeCheck triggered");
        try {

            List<FreezeDpdClass> freezeDpdlist = freezeDpdRecord.getFreezeDpd();
            FreezeDpdClass latestFreeze = freezeDpdlist.get(freezeDpdlist.size() - 1);
            FfSerCustomerDpdLog.info("FfSerCusFreezeCheck:checkFreezeCheck triggered " + freezeDpdlist.toString());

            String freezeDpdVal = latestFreeze.getFreezeDpd().getValue();
            String freezeStartStr = latestFreeze.getDateOfFreeze() != null ? latestFreeze.getDateOfFreeze().getValue()
                    : null;
            String freezeEndStr = latestFreeze.getFreezeEndDate() != null ? latestFreeze.getFreezeEndDate().getValue()
                    : null;
            // CASE 1:FreezeDPD = NO → always continue
            if (!"YES".equalsIgnoreCase(freezeDpdVal)) {
                FfSerCustomerDpdLog.info("FreezeDPD = NO → continue DPD");
                return false; // CONTINUE ROUTINE
            }

            if ("YES".equalsIgnoreCase(freezeDpdVal)) {

                // CASE 2: YES but no dates → always freeze
                if (freezeStartStr == null || freezeStartStr.isEmpty() || freezeEndStr == null
                        || freezeEndStr.isEmpty()) {

                    FfSerCustomerDpdLog.info("DPD skipped for customer " + id + " (FreezeDPD = YES, no dates)");
                    return true; // STOP ROUTINE
                }

                LocalDate freezeStart = LocalDate.parse(latestFreeze.getDateOfFreeze().getValue(), inputFormatter);
                LocalDate freezeEnd = LocalDate.parse(latestFreeze.getFreezeEndDate().getValue(), inputFormatter);
                FfSerCustomerDpdLog.info(
                        "FfSerCusFreezeCheck:checkFreezeCheck freezeStart " + freezeStart + "freezeEnd " + freezeEnd);
                boolean inFreezeRange = (date.isEqual(freezeStart) || date.isAfter(freezeStart))
                        && (date.isEqual(freezeEnd) || date.isBefore(freezeEnd));

                // CASE 3: Still inside freeze period → skip all DPD calculation
                if (inFreezeRange) {
                    FfSerCustomerDpdLog.info("DPD calculation skipped for customer " + id + " because FreezeDPD ("
                            + freezeDpdVal + ") is active until " + freezeEnd);

                    return true; // SKIP ALL FURTHER PROCESSING
                }
            }

        } catch (Exception e) {
            FfSerCustomerDpdLog.info("Freeze is not available " + e);
        }
        return false;
    }

    private boolean checkParamCondition(String arrangementId, String today) {

        if (dpdParamObj == null || excludeList == null)
            return false;

        return evaluateExclusion(arrangementId, today, dpdParamObj, excludeList);
    }

    private boolean evaluateExclusion(String arrangementId, String today, EbFfDpdParamRecord dpdParamObj,
            List<ExcludeProductClass> excludeList) {
        String baseArrangementId = arrangementId.split("-")[0];

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern(YYYYMMDD);
        LocalDate todayDate = LocalDate.parse(today, fmt);
        // 2. Load arrangement details
        AaArrangementRecord arrRecord = arrangementCache.get(baseArrangementId);

        if (arrRecord == null) {
            arrRecord = new AaArrangementRecord(
                    dataAccess.getRecord(finMnemonic, "AA.ARRANGEMENT", "", baseArrangementId));
            arrangementCache.put(baseArrangementId, arrRecord);
        }
        // Loan has a single product and single branch
        String loanProduct = arrRecord.getProduct().get(0).getProduct().getValue();
        String loanBranch = arrRecord.getCoCodeRec().getValue();

        // 3. Load Account Bill Details
        AaAccountDetailsRecord accountDetailsRec = accountDetailsCache.get(baseArrangementId);

        if (accountDetailsRec == null) {
            accountDetailsRec = new AaAccountDetailsRecord(
                    dataAccess.getRecord(finMnemonic, "AA.ACCOUNT.DETAILS", "", baseArrangementId));
            accountDetailsCache.put(baseArrangementId, accountDetailsRec);
        }
        List<BillPayDateClass> billPayDateList = accountDetailsRec.getBillPayDate();

        boolean billMatch = isBillMatch(billPayDateList, dpdParamObj);
        // Loop all exclusion rules
        for (ExcludeProductClass exc : excludeList) {

            String excProduct = exc.getExcludeProduct().getValue();
            String excBranch = exc.getExcludeBranch().getValue();

            LocalDate effDate = LocalDate.parse(exc.getEffectiveDate().getValue(), fmt);
            LocalDate endDate = LocalDate.parse(exc.getEndDate().getValue(), fmt);

            // 1. Product match
            boolean productMatch = loanProduct.equals(excProduct);

            // 2. Branch match
            boolean branchMatch = loanBranch.equals(excBranch);

            // 3. Date match → within range (1 to 10 includes both)
            boolean dateMatch = (todayDate.isEqual(effDate) || todayDate.isAfter(effDate))
                    && (todayDate.isEqual(endDate) || todayDate.isBefore(endDate));

            // Final condition: ALL 3 must match
            if ((productMatch && branchMatch && dateMatch) || billMatch) {

                FfSerCustomerDpdLog.info("Arrangement " + arrangementId + " excluded because: " + "productMatch="
                        + productMatch + ", branchMatch=" + branchMatch + ", dateMatch=" + dateMatch + ", billMatch="
                        + billMatch);

                return true; // loan excluded
            }
        }

        return false;
    }

    private boolean isBillMatch(List<BillPayDateClass> billPayDateList, EbFfDpdParamRecord dpdParamObj) {

        // Extract all bill types present in the arrangement
        Set<String> arrangementBillTypes = new HashSet<>();

        for (BillPayDateClass billTypeVal : billPayDateList) {
            for (BillIdClass billIdVal : billTypeVal.getBillId()) {
                if (billIdVal.getBillType() != null) {
                    arrangementBillTypes.add(billIdVal.getBillType().getValue());
                }
            }
        }

        // Excluded bill types
        Set<String> excludedBillTypeSet = new HashSet<>();
        for (TField t : dpdParamObj.getExcludeBillType()) {
            excludedBillTypeSet.add(t.getValue());
        }

        for (String arrBillType : arrangementBillTypes) {
            if (excludedBillTypeSet.contains(arrBillType)) {
                return true;
            }
        }

        return false;
    }

    private boolean checkDpdParam(String dpdStatus, String id, List<TransactionData> transactionData,
            List<TStructure> records, boolean isCustomerNpaToday) {

        try {
            FfSerCustomerDpdLog.info("checkDpdParam Method triggered for the status " + dpdStatus);

            if (dpdParamObj == null)
                return false;

            String paramDpdStatus = dpdParamObj.getCustDpdFlag().toString();
            FfSerCustomerDpdLog.info("paramDpdStatus " + paramDpdStatus);
            if (paramDpdStatus.equals(dpdStatus)) {
                FfSerCustomerDpdLog.info("dpdstatus " + dpdStatus + " matched with paramDpdStatus " + paramDpdStatus);
                boolean updated = updateCustomerDpdStatus(id, transactionData, records);
                if (updated) {
                    isCustomerNpaToday = true;
                }
            }

        } catch (Exception e) {
            FfSerCustomerDpdLog.error("No NPA record " + e);
        }
        return isCustomerNpaToday;

    }

    private boolean updateCustomerDpdStatus(String id, List<TransactionData> transactionData,
            List<TStructure> records) {
        try {
            FfSerCustomerDpdLog.info("Update customer DPD status method triggered");
            CustomerRecord cusRecord = new CustomerRecord(dataAccess.getRecord("CUSTOMER", id));
            String custNpa = cusRecord.getLocalRefField("FF.NPA.CUSTOMER").getValue();
            if (custNpa.isEmpty() || custNpa.equals("")) {
                cusRecord.getLocalRefField("FF.NPA.CUSTOMER").set("YES");
                TransactionData transData = new TransactionData();
                transData.setFunction("INPUT");
                transData.setSourceId("FF.OFS.UPD");
                transData.setNumberOfAuthoriser("0");
                transData.setTransactionId(id);
                transData.setVersionId("CUSTOMER,FF.UPDATE");
                transactionData.add(transData);
                records.add(cusRecord.toStructure());
                FfSerCustomerDpdLog.info("Updating customer NPA flag to yes " + transactionData.toString());
                return true;
            }
        } catch (Exception e) {
            FfSerCustomerDpdLog.error("Error updating customer NPA date: ", e);
        }
        return false;
    }

    private String getFormattedDate() {
        Session sessionObj = new Session(this);
        String today = sessionObj.getCurrentVariable("!TODAY"); // yyyyMMdd

        DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern(YYYYMMDD);
        LocalDate date = LocalDate.parse(today, inputFormatter);
        return date.toString();
    }

    private void updateDpdStatus(List<String> npaLoans, String dpdStatus, List<TransactionData> transactionData,
            List<TStructure> records) {
        try {
            FfSerCustomerDpdLog.info("updateDpdStatus Method triggered " + npaLoans.toString());
            boolean hasNpaVal = false;
            if ("NPA".equalsIgnoreCase(dpdStatus)) {
                hasNpaVal = true; // At least one loan is NPA

            }
            if (hasNpaVal) {
                for (String arrangementId : npaLoans) {
                    FfSerCustomerDpdLog.info(" UpdateDpdStatus arrangementid " + arrangementId);
                    AaArrangementActivityRecord aaaRec = new AaArrangementActivityRecord(this);
                    PropertyClass propertyRec = new PropertyClass();
                    FieldNameClass fieldNameRec = new FieldNameClass();
                    AaArrangementRecord arrRecord = arrangementCache.get(arrangementId);

                    if (arrRecord == null) {
                        arrRecord = new AaArrangementRecord(
                                dataAccess.getRecord(finMnemonic, "AA.ARRANGEMENT", "", arrangementId));
                        arrangementCache.put(arrangementId, arrRecord);
                    }
                    TField curCmpyId = arrRecord.getCoCodeRec();
                    FfSerCustomerDpdLog.info("curCmpyId " + curCmpyId.toString());

                    AaAccountDetailsRecord accountDetailsRec = accountDetailsCache.get(arrangementId);

                    if (accountDetailsRec == null) {
                        accountDetailsRec = new AaAccountDetailsRecord(
                                dataAccess.getRecord(finMnemonic, "AA.ACCOUNT.DETAILS", "", arrangementId));
                        accountDetailsCache.put(arrangementId, accountDetailsRec);
                    }
                    String setDpdStatus = accountDetailsRec.getArrAgeStatus().toString();
                    FfSerCustomerDpdLog.info("setDpdStatus" + setDpdStatus);

                    aaaRec.setArrangement(arrangementId);
                    propertyRec.addFieldName(fieldNameRec);
                    aaaRec.setActivity("LENDING-SET-DPD.STAGES*NPA");
                    propertyRec.setProperty("DPD.STAGES");
                    aaaRec.addProperty(propertyRec);

                    TransactionData npaTransData = new TransactionData();
                    npaTransData.setFunction("INPUT");
                    npaTransData.setSourceId("FF.OFS.UPD");
                    npaTransData.setNumberOfAuthoriser("0");
                    npaTransData.setCompanyId(curCmpyId.toString());
                    npaTransData.setTransactionId("");
                    npaTransData.setVersionId("AA.ARRANGEMENT.ACTIVITY,FF.PRINCIPLE.UPDATE");
                    transactionData.add(npaTransData);
                    records.add(aaaRec.toStructure());
                    FfSerCustomerDpdLog.info("transactionData for set NPA " + transactionData.toString());

                }
            }

        } catch (Exception e) {
            FfSerCustomerDpdLog.error("unable to trigger the NPA Activity" + e);
        }

    }

    private static int parseIntSafe(Object value) {
        try {
            if (value == null)
                return 0;
            String strVal = "";
            if (value instanceof String)
                strVal = (String) value;
            if (value instanceof TField)
                strVal = ((TField) value).getValue();

            if (strVal.contains(".")) {
                return (int) Math.round(Double.parseDouble(strVal)); // handles "0.0" or "190.0"
            }
            return Integer.parseInt(strVal);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static double parseDoubleSafe(Object value) {
        try {
            if (value == null)
                return 0;

            String strVal = "";

            if (value instanceof String)
                strVal = (String) value;

            if (value instanceof TField)
                strVal = ((TField) value).getValue();

            return Double.parseDouble(strVal);
        } catch (Exception e) {
            return 0;
        }
    }

}
