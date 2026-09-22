package com.temenos.fusion;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import com.temenos.api.TStructure;
import com.temenos.t24.api.complex.eb.servicehook.ServiceData;
import com.temenos.t24.api.complex.eb.servicehook.TransactionData;
import com.temenos.t24.api.hook.system.ServiceLifecycle;
import com.temenos.t24.api.records.aaaccountdetails.AaAccountDetailsRecord;
import com.temenos.t24.api.records.aaaccountdetails.BillIdClass;
import com.temenos.t24.api.records.aaaccountdetails.BillPayDateClass;
import com.temenos.t24.api.records.aaarrangement.AaArrangementRecord;
import com.temenos.t24.api.records.aabilldetails.AaBillDetailsRecord;
import com.temenos.t24.api.records.company.CompanyRecord;
import com.temenos.t24.api.records.ebffcustdpdconcat.EbFfCustDpdConcatRecord;
import com.temenos.t24.api.records.ebffloandpd.DateClass;
import com.temenos.t24.api.records.ebffloandpd.EbFfLoanDpdRecord;
import com.temenos.t24.api.system.DataAccess;
import com.temenos.t24.api.system.Session;
import com.temenos.t24.api.tables.ebffcustdpdconcat.EbFfCustDpdConcatTable;
import com.temenos.t24.api.tables.ebffloandpd.EbFfLoanDpdTable;

/*-------------------------------------------------------------------------------
 * Fully Optimized High-Volume Loan DPD Service
 * Preserves original logic, batch caching, cumulative & average fixes
 *-------------------------------------------------------------------------------*/
public class FfSerLoanDpd extends ServiceLifecycle {

    static final String YYYYMMDD = "yyyyMMdd";
    static final String PENDINGCLOSURE = "PENDING.CLOSURE";
    static final String CLOSED = "CLOSED";
    private static final FusionFileLogger log = FusionFileLogger.getLogger(FfSerLoanDpd.class);

    private DataAccess dataAccess = new DataAccess(this);
    private String finMnemonic;
    private Session session;
    private DateTimeFormatter dateFormatter;

    private Map<String, AaBillDetailsRecord> billCache = new HashMap<>();
 
    @Override
    public List<String> getIds(ServiceData serviceData, List<String> controlList) {
        List<String> arrangementList = new ArrayList<>();
        try {
            initialiseCompanyInfo(serviceData);
            List<String> overdueList = dataAccess.selectRecords(finMnemonic, "AA.OVERDUE.STATS", "", "");
            arrangementList = overdueList.stream().map(v -> v.split("-")[0]).collect(Collectors.toList());
            log.info("Fetched " + arrangementList.size() + " arrangements from overdue stats");
        } catch (Exception e) {
            log.error("Error fetching arrangement IDs: " + e.getMessage());
        }
        return arrangementList;
    }

    @Override
    public void postUpdateRequest(String id, ServiceData serviceData, String controlItem,
            List<TransactionData> transactionData, List<TStructure> records) {
        try {

            initialiseSessionObjects();

            String today = session.getCurrentVariable("!TODAY");
            LocalDate todayDate = LocalDate.parse(today, dateFormatter);

            // 1️ Fetch account & arrangement
            AaAccountDetailsRecord account = getAccount(id);
            AaArrangementRecord arrangement = getArrangement(id);

            String arrangementStatus = arrangement != null ? arrangement.getArrStatus().getValue() : null;

            // 2️ Determine DPD status

            String dpdStatus = determineDpdStatus(arrangementStatus, account);

            // 3️ Prepare table ID
            String tableId = generateTableId(id, todayDate);

            // 4️ Fetch existing DPD record
            EbFfLoanDpdRecord loanDpdRecord = getLoanDpdRecord(tableId);

            // 5️ CUR/CLOSED → reuse previous values
            if (isCurrentOrClosed(dpdStatus)) {
                checkExistingDPD(tableId, arrangement, loanDpdRecord);
                return;
            }

            // 6️ Delinquent → compute CUR/PEAK/AVG/CUM
            LoanProcessContext ctx = new LoanProcessContext();

            ctx.id = id;
            ctx.today = today;
            ctx.todayDate = todayDate;
            ctx.account = account;
            ctx.arrangement = arrangement;
            ctx.tableId = tableId;
            ctx.loanDpdRecord = loanDpdRecord;
            ctx.dpdStatus = dpdStatus;

            processDelinquentLoan(ctx);
        } catch (Exception e) {
            log.error("Error processing arrangement " + id + ": " + e.getMessage());
        }
    }

    // ------------------- Helper Methods -------------------

    private void initialiseSessionObjects() {
        if (session == null)
            session = new Session(this);

        if (dateFormatter == null)
            dateFormatter = DateTimeFormatter.ofPattern(YYYYMMDD);
    }

    private AaAccountDetailsRecord getAccount(String id) {
        try {
            return new AaAccountDetailsRecord(dataAccess.getRecord(finMnemonic, "AA.ACCOUNT.DETAILS", "", id));
        } catch (Exception e) {
            log.info("Unable to fetch account details " + id);
            return null;
        }
    }

    private AaArrangementRecord getArrangement(String id) {
        try {
            return new AaArrangementRecord(dataAccess.getRecord(finMnemonic, "AA.ARRANGEMENT", "", id));
        } catch (Exception e) {
            log.info("Unable to fetch arrangement " + id);
            return null;
        }
    }

    private String determineDpdStatus(String arrangementStatus, AaAccountDetailsRecord account) {

        if (PENDINGCLOSURE.equalsIgnoreCase(arrangementStatus))
            return CLOSED;

        if (account.getArrAgeStatus() != null)
            return account.getArrAgeStatus().toString();

        return "CUR";
    }

    private String generateTableId(String id, LocalDate todayDate) {

        String monthStr = todayDate.getMonth().toString().substring(0, 3) + todayDate.getYear();
        return id + "-" + monthStr;
    }

    private EbFfLoanDpdRecord getLoanDpdRecord(String tableId) {

        try {
            return new EbFfLoanDpdRecord(dataAccess.getRecord(finMnemonic, "EB.FF.LOAN.DPD", "", tableId));
        } catch (Exception e) {
            log.info("No existing DPD record for " + tableId);
            return null;
        }
    }

    private boolean isCurrentOrClosed(String dpdStatus) {

        return "".equals(dpdStatus) || "CUR".equalsIgnoreCase(dpdStatus) || CLOSED.equalsIgnoreCase(dpdStatus);
    }

    private void processDelinquentLoan(LoanProcessContext ctx) {
        Map<String, String> oldestBill = getTheOldestBill(ctx.account);

        String curDpdRef = oldestBill.get("BILL_ID");
        String billDateVal = oldestBill.get("BILL_DATE");

        String customer = getCustomerId(ctx.arrangement);

        String curDpd = computeCurDpd(billDateVal, ctx.todayDate);

        String[] peakValues = computePeakDpd(curDpd, curDpdRef, ctx.loanDpdRecord);

        String peakDpd = peakValues[0];
        String peakDpdRef = peakValues[1];

        String peakDpdCollDate = getBillCollectionDate(curDpdRef);

        EbFfLoanDpdRecord prevMonthRecord = getPreviousMonthRecord(ctx.id, ctx.todayDate);

        String cumDpd = computeCumulativeDpd(ctx.todayDate, curDpd, ctx.loanDpdRecord, prevMonthRecord);

        String paymentDate = getPaymentDate(curDpdRef);

        String avgDpd = computeAvgDpd(cumDpd, paymentDate, ctx.todayDate);

        LoanDpdMetrics metrics = new LoanDpdMetrics();

        metrics.curDpd = curDpd;
        metrics.dpdStatus = ctx.dpdStatus;
        metrics.curDpdRef = curDpdRef;
        metrics.peakDpd = peakDpd;
        metrics.peakDpdRef = peakDpdRef;
        metrics.peakDpdCollDate = peakDpdCollDate;
        metrics.avgDpd = avgDpd;
        metrics.cumDpd = cumDpd;
        metrics.customer = customer;

        updateLoanDpdTable(ctx.tableId, ctx.today, ctx.loanDpdRecord, metrics);
    }

    private static class LoanProcessContext {

        String id;
        String today;
        LocalDate todayDate;
        AaAccountDetailsRecord account;
        AaArrangementRecord arrangement;
        String tableId;
        EbFfLoanDpdRecord loanDpdRecord;
        String dpdStatus;

    }

    private EbFfLoanDpdRecord getPreviousMonthRecord(String id, LocalDate todayDate) {

        if (todayDate.getDayOfMonth() != 1)
            return null;

        try {

            LocalDate prev = todayDate.minusMonths(1);
            String prevMonthStr = prev.getMonth().toString().substring(0, 3) + prev.getYear();
            String prevId = id + "-" + prevMonthStr;

            return new EbFfLoanDpdRecord(dataAccess.getRecord(finMnemonic, "EB.FF.LOAN.DPD", "", prevId));

        } catch (Exception e) {
            return null;
        }
    }

    private String getPaymentDate(String billId) {

        try {

            if (billId == null || billId.isEmpty())
                return "";

            AaBillDetailsRecord bill = fetchBillFromCache(billId);

            if (bill != null && bill.getPaymentDate() != null)
                return bill.getPaymentDate().getValue();

        } catch (Exception e) {
            log.error("unable to fetch payment date");
        }

        return "";
    }

    private void checkExistingDPD(String tableId, AaArrangementRecord arrangement, EbFfLoanDpdRecord loanDpdRecord) {
        try {
            log.info("checkExistingDPD triggered for " + tableId);

            String customer = getCustomerId(arrangement);
            DpdValues dpdValues = extractPreviousValues(loanDpdRecord);
            String dpdStatus = determineClosureStatus(arrangement);
            if (!shouldSkipUpdate(arrangement, dpdValues.prevStatus)) {

                String today = session.getCurrentVariable("!TODAY");

                LoanDpdMetrics metrics = new LoanDpdMetrics();

                metrics.curDpd = dpdValues.curDpd;
                metrics.dpdStatus = dpdStatus;
                metrics.curDpdRef = dpdValues.curDpdRef;
                metrics.peakDpd = dpdValues.peakDpd;
                metrics.peakDpdRef = dpdValues.peakDpdRef;
                metrics.peakDpdCollDate = dpdValues.peakDpdCollDate;
                metrics.avgDpd = dpdValues.avgDpd;
                metrics.cumDpd = dpdValues.cumDpd;
                metrics.customer = customer;

                updateLoanDpdTable(tableId, today, loanDpdRecord, metrics);
            }

        } catch (Exception e) {
            log.error("Error in checkExistingDPDOptimized: " + e.getMessage());
        }
    }

    private static class DpdValues {

        String curDpd = "0";
        String curDpdRef = "";
        String peakDpd = "0";
        String peakDpdRef = "";
        String peakDpdCollDate = "";
        String avgDpd = "0";
        String cumDpd = "0";
        String prevStatus = "";
    }

    private String determineClosureStatus(AaArrangementRecord arrangement) {

        if (arrangement != null && PENDINGCLOSURE.equalsIgnoreCase(arrangement.getArrStatus().getValue()))
            return CLOSED;

        return "CUR";
    }

    private boolean shouldSkipUpdate(AaArrangementRecord arrangement, String prevStatus) {

        if (arrangement == null)
            return false;

        if (PENDINGCLOSURE.equalsIgnoreCase(arrangement.getArrStatus().getValue())
                && CLOSED.equalsIgnoreCase(prevStatus)) {

            log.info("Skipping DPD update as status already CLOSED");
            return true;
        }

        return false;
    }

    private DpdValues extractPreviousValues(EbFfLoanDpdRecord loanDpdRecord) {

        DpdValues values = new DpdValues();

        try {

            if (loanDpdRecord == null || loanDpdRecord.getDate() == null || loanDpdRecord.getDate().isEmpty())
                return values;

            DateClass last = loanDpdRecord.getDate().get(loanDpdRecord.getDate().size() - 1);

            values.peakDpd = last.getPeakDpd() != null ? last.getPeakDpd().getValue() : "0";
            values.peakDpdRef = last.getPeakDpdRef() != null ? last.getPeakDpdRef().getValue() : "";
            values.avgDpd = last.getAvgDpd() != null ? last.getAvgDpd().getValue() : "0";
            values.cumDpd = last.getCumDpd() != null ? last.getCumDpd().getValue() : "0";

            values.prevStatus = last.getDpdStatus() != null ? last.getDpdStatus().getValue() : "";

            String lastRef = last.getCurDpdRef() != null ? last.getCurDpdRef().getValue() : "";

            if (lastRef != null && !lastRef.isEmpty())
                values.peakDpdCollDate = getBillCollectionDate(lastRef);

        } catch (Exception e) {

            log.error("Error extracting previous DPD values: " + e.getMessage());
        }

        return values;
    }

    private void initialiseCompanyInfo(ServiceData serviceData) {
        CompanyRecord comp = new CompanyRecord(dataAccess.getRecord("COMPANY", serviceData.getCompanyId()));
        finMnemonic = comp.getFinancialMne().getValue();
    }

    private String getCustomerId(AaArrangementRecord arrangement) {
        try {
            if (arrangement != null && !arrangement.getCustomer().isEmpty())
                return arrangement.getCustomer().get(0).getCustomer().getValue();
        } catch (Exception e) {
            log.error("Error fetching customer: " + e.getMessage());
        }
        return null;
    }

    private Map<String, String> getTheOldestBill(AaAccountDetailsRecord account) {
        Map<String, String> billMap = new HashMap<>();
        try {
            String oldestDate = null;
            String oldestBillId = null;
            for (BillPayDateClass bpd : account.getBillPayDate()) {
                String payDate = bpd.getBillPayDate().getValue();
                for (BillIdClass bid : bpd.getBillId()) {
                    if ("UNPAID".equalsIgnoreCase(bid.getSetStatus().getValue())
                            && "AGING".equalsIgnoreCase(bid.getBillStatus().getValue())) {
                        String bId = bid.getBillId().getValue();
                        if (oldestDate == null || payDate.compareTo(oldestDate) < 0) {
                            oldestDate = payDate;
                            oldestBillId = bId;
                        }
                    }
                }
            }
            if (oldestDate != null && oldestBillId != null) {
                billMap.put("BILL_DATE", oldestDate);
                billMap.put("BILL_ID", oldestBillId);
            }
        } catch (Exception e) {
            log.error("Error fetching oldest bill: " + e.getMessage());
        }
        return billMap;
    }

    private String computeCurDpd(String billDateVal, LocalDate today) {
        try {
            if (billDateVal == null || billDateVal.isEmpty())
                return "0";
            LocalDate billDate = LocalDate.parse(billDateVal, dateFormatter);
            return String.valueOf(Math.max(ChronoUnit.DAYS.between(billDate, today), 0));
        } catch (Exception e) {
            return "0";
        }
    }

    private String[] computePeakDpd(String curDpd, String curDpdRef, EbFfLoanDpdRecord loanDpd) {
        String peakDpd = curDpd;
        String peakRef = curDpdRef;
        try {
            if (loanDpd != null && loanDpd.getDate() != null && !loanDpd.getDate().isEmpty()) {
                DateClass last = loanDpd.getDate().get(loanDpd.getDate().size() - 1);
                int existingPeak = last.getPeakDpd() != null ? Integer.parseInt(last.getPeakDpd().getValue()) : 0;
                if (Integer.parseInt(curDpd) < existingPeak) {
                    peakDpd = String.valueOf(existingPeak);
                    peakRef = last.getPeakDpdRef() != null ? last.getPeakDpdRef().getValue() : "";
                }
            }
        } catch (Exception e) {
            log.error("Error computing peak DPD: " + e.getMessage());
        }
        return new String[] { peakDpd, peakRef };
    }

    private String getBillCollectionDate(String billId) {
        if (billId == null || billId.isEmpty())
            return "";
        try {
            AaBillDetailsRecord bill = fetchBillFromCache(billId);
            if (bill != null && bill.getBillStatus() != null) {
                return bill.getBillStatus().stream()
                        .filter(bs -> "SETTLED".equalsIgnoreCase(bs.getBillStatus().getValue()))
                        .map(bs -> bs.getBillStChgDt().getValue()).filter(Objects::nonNull).findFirst().orElse("");
            }
        } catch (Exception e) {
            log.error("Error fetching bill collection date: " + e.getMessage());
        }
        return "";
    }

    private AaBillDetailsRecord fetchBillFromCache(String billId) {

        return billCache.computeIfAbsent(billId,
                id -> new AaBillDetailsRecord(dataAccess.getRecord(finMnemonic, "AA.BILL.DETAILS", "", id)));
    }

    private String computeCumulativeDpd(LocalDate todayDate, String curDpd, EbFfLoanDpdRecord currentMonthRecord,
            EbFfLoanDpdRecord prevMonthRecord) {
        String cumDpdValue = "0";
        String lastCumDpd = null;

        try {
            int curDpdVal = Integer.parseInt(curDpd);

                // If first day of month → use previous month's cumulative DPD if exists
            if (todayDate.getDayOfMonth() == 1) {
                if (prevMonthRecord != null && prevMonthRecord.getDate() != null
                        && !prevMonthRecord.getDate().isEmpty()) {
                    List<DateClass> prevDates = prevMonthRecord.getDate();
                    lastCumDpd = prevDates.get(prevDates.size() - 1).getCumDpd().getValue();
                    cumDpdValue = String.valueOf(curDpdVal * (curDpdVal + 1) / 2 + Integer.parseInt(lastCumDpd));
                } else {
                    // fallback if no previous month record exists
                    cumDpdValue = String.valueOf(curDpdVal * (curDpdVal + 1) / 2);
                }
            } else {
                // Other days → use last cumulative of current month if exists
                if (currentMonthRecord != null && currentMonthRecord.getDate() != null
                        && !currentMonthRecord.getDate().isEmpty()) {
                    List<DateClass> currentDates = currentMonthRecord.getDate();
                    lastCumDpd = currentDates.get(currentDates.size() - 1).getCumDpd().getValue();
                    cumDpdValue = String.valueOf(curDpdVal + Integer.parseInt(lastCumDpd));
                } else {
                    // fallback if no current month record exists
                    cumDpdValue = String.valueOf(curDpdVal * (curDpdVal + 1) / 2);
                }
            }

        } catch (Exception e) {
            log.error("Error computing cumulative DPD for today " + todayDate, e);
        }

        return cumDpdValue;
    }

    private String computeAvgDpd(String cumDpd, String paymentDate, LocalDate todayDate) {
        try {
            if (paymentDate == null || paymentDate.isEmpty())
                return "0";

            // Parse payment date only once
            LocalDate startDate = LocalDate.parse(paymentDate, dateFormatter).withDayOfMonth(1);

            long days = ChronoUnit.DAYS.between(startDate, todayDate) + 1; // include today
            if (days <= 0)
                return "0";

            double avg = Double.parseDouble(cumDpd) / days;

            // Round to 2 decimal places without creating new objects repeatedly
            return String.format("%.2f", avg);

        } catch (Exception e) {
            log.error("Error computing average DPD for payment date: " + paymentDate, e);
            return "0";
        }
    }

    private static class LoanDpdMetrics {

        String curDpd;
        String dpdStatus;
        String curDpdRef;
        String peakDpd;
        String peakDpdRef;
        String peakDpdCollDate;
        String avgDpd;
        String cumDpd;
        String customer;

    }

    private void updateLoanDpdTable(String tableId, String today, EbFfLoanDpdRecord loanDpdRecord,
            LoanDpdMetrics metrics) {

        try {

            EbFfLoanDpdTable tableObj = new EbFfLoanDpdTable(this);

            EbFfLoanDpdRecord recordObj = loanDpdRecord != null ? loanDpdRecord : new EbFfLoanDpdRecord(this);

            if (loanDpdRecord == null)
                recordObj.setCustomer(metrics.customer);

            boolean exists = false;
            for (DateClass d : recordObj.getDate()) {
                if (today.equals(d.getDate().getValue())) {
                    exists = true;
                    break;
                }
            }
            if (!exists) {

                DateClass dateCls = new DateClass();

                dateCls.setCurDpd(metrics.curDpd);
                dateCls.setDpdStatus(metrics.dpdStatus);
                dateCls.setCurDpdRef(metrics.curDpdRef);
                dateCls.setPeakDpd(metrics.peakDpd);
                dateCls.setPeakDpdRef(metrics.peakDpdRef);
                dateCls.setPeakDpdCollDate(metrics.peakDpdCollDate);
                dateCls.setAvgDpd(metrics.avgDpd);
                dateCls.setCumDpd(metrics.cumDpd);
                dateCls.setDate(today);

                recordObj.addDate(dateCls);

                tableObj.write(tableId, recordObj);

                EbFfCustDpdConcatTable custTable = new EbFfCustDpdConcatTable(this);
                EbFfCustDpdConcatRecord custRecord = new EbFfCustDpdConcatRecord(this);

                custRecord.setCustomerId(metrics.customer);

                custTable.write(metrics.customer, custRecord);
            }

        } catch (Exception e) {

            log.error("Error updating loan DPD table: " + e.getMessage());
        }
    }
}