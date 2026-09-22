package com.temenos.fusion;

import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.temenos.api.TField;
import com.temenos.t24.api.arrangement.accounting.Contract;
import com.temenos.t24.api.complex.eb.enquiryhook.EnquiryContext;
import com.temenos.t24.api.complex.eb.enquiryhook.FilterCriteria;
import com.temenos.t24.api.hook.system.Enquiry;
import com.temenos.t24.api.records.aaaccountdetails.AaAccountDetailsRecord;
import com.temenos.t24.api.records.aaaccountdetails.BillIdClass;
import com.temenos.t24.api.records.aaaccountdetails.BillPayDateClass;
import com.temenos.t24.api.records.aaactivity.AaActivityRecord;
import com.temenos.t24.api.records.aaactivitybalances.AaActivityBalancesRecord;
import com.temenos.t24.api.records.aaactivityhistory.AaActivityHistoryRecord;
import com.temenos.t24.api.records.aaactivityhistory.ActivityRefClass;
import com.temenos.t24.api.records.aaactivityhistory.EffectiveDateClass;
import com.temenos.t24.api.records.aaarrangement.AaArrangementRecord;
import com.temenos.t24.api.records.aaarrangementactivity.AaArrangementActivityRecord;
import com.temenos.t24.api.records.aaarrbalancemaintenance.AaArrBalanceMaintenanceRecord;
import com.temenos.t24.api.records.aaarrbalancemaintenance.BillRefClass;
import com.temenos.t24.api.records.aaarrbalancemaintenance.PropertyClass;
import com.temenos.t24.api.records.aabilldetails.AaBillDetailsRecord;
import com.temenos.t24.api.records.aabilldetails.RepayRefClass;
import com.temenos.t24.api.records.aabilldetails.WriteoffRefClass;
import com.temenos.t24.api.records.aaproperty.AaPropertyRecord;
import com.temenos.t24.api.records.company.CompanyRecord;
import com.temenos.t24.api.records.ebffloanpaymenthis.DemandDateClass;
import com.temenos.t24.api.records.ebffloanpaymenthis.EbFfLoanPaymentHisRecord;
import com.temenos.t24.api.records.fundstransfer.FundsTransferRecord;
import com.temenos.t24.api.system.DataAccess;
import com.temenos.t24.api.system.Session;

public class FfNofileSoaLoanDetails extends Enquiry {
    public static final String ACCOUNT = "ACCOUNT";
    public static final String PRINTEREST = "PRINTEREST";
    public static final String LENDING_CHARGEOFF_ARRANGEMENT = "LENDING-CHARGEOFF-ARRANGEMENT";
    public static final String LENDING_WRITE_OFF_BAL_MAINTAIN = "LENDING-WRITE.OFF-BAL.MAINTAIN";
    public static final String LENDING_APPLYPAYMENT_WRITEOFF_SETTLEMENT = "LENDING-APPLYPAYMENT-WRITEOFF.SETTLEMENT";
    public static final String LENDING_APPLYPAYMENT_INSURANCE_SETTLEMENT = "LENDING-APPLYPAYMENT-INSURANCE.SETTLEMENT";
    public static final String LENDING_SETTLE_FORECLOSURE = "LENDING-SETTLE-FORECLOSURE";
    public static final String LENDING_APPLYPAYMENT_PR_COLLECTION = "LENDING-APPLYPAYMENT-PR.COLLECTION";
    public static final String LENDING_MATURE_ARRANGEMENT = "LENDING-MATURE-ARRANGEMENT";
    public static final String LENDING_CREDIT_ARRANGEMENT = "LENDING-CREDIT-ARRANGEMENT";
    public static final String LENDING_APPLYPAYMENT_PR_CURR_BALANCE = "LENDING-APPLYPAYMENT-PR.CURR.BALANCE";
    public static final String LENDING_APPLYPAYMENT_PR_INSURANCE_BALANCES = "LENDING-APPLYPAYMENT-PR.INSURANCE.BALANCES";

    public static final Set<String> LENDING_ACTIVITIES = Set.of(LENDING_CHARGEOFF_ARRANGEMENT,
            LENDING_WRITE_OFF_BAL_MAINTAIN, LENDING_APPLYPAYMENT_WRITEOFF_SETTLEMENT,
            LENDING_APPLYPAYMENT_INSURANCE_SETTLEMENT, LENDING_SETTLE_FORECLOSURE, LENDING_APPLYPAYMENT_PR_COLLECTION,
            LENDING_MATURE_ARRANGEMENT, LENDING_CREDIT_ARRANGEMENT);

    public static class SoaRowData {
        LocalDate sortingDate;
        String demandDate = "";
        String transactionType = "";
        String demandAmount = "0.00";
        String principalDemandAmount = "0.00";
        String interestDemandAmount = "0.00";
        String principalDueAmount = "0.00";
        String interestDueAmount = "0.00";
        String paymentDate = "";
        String paymentMode = "";
        String paymentAmount = "0.00";
        String transactionDate = "";
        String transactionAmount = "0.00";
        String principalCollected = "0.00";
        String interestCollected = "0.00";
        String principalOutstanding = "0.00";
    }

    Set<String> feesTxnTypeSet = new HashSet<>();
    Set<String> disbTxnTypeSet = new HashSet<>();
    Set<String> instTxnTypeSet = new HashSet<>();
    Set<String> collectionTxnTypeSet = new HashSet<>();

    List<String> retvalues = new ArrayList<>();
    List<SoaRowData> rowRecordsPipeline = new ArrayList<>();
    DataAccess da = new DataAccess(this);

    String arrangementId = "";
    String selDate = "";
    String selDateOp = "";
    String todayDate = "";
    String finMnemonic = "";
    String mnemonic = "";
    String arrStDt = "";

    String demandDate = "";
    String transactionType = "";
    String demandAmount = "";
    String dueAmount = "";
    String paymentDate = "";
    String paymentMode = "";
    String paymentAmount = "";
    String transactionDate = "";
    String transactionAmount = "";
    String principalOutstanding = "";
    String otherFinancialCharges = "";
    String narration = "";

    String startDate = "";
    String endDate = "";
    String lastPrinOstMvmt = "";
    String lastTxnAmt = "";
    String orgContractDt = "";

    double chgPrinOst = 0.0;
    double prinOstMvmt = 0.0;

    boolean noDateFilter = false;
    boolean isInstallment = false;
    boolean isChargeOffTrigg = false;

    String principalDemandAmount = "";
    String interestDemandAmount = "";
    String principalDueAmount = "";
    String interestDueAmount = "";
    String principalCollected = "";
    String interestCollected = "";

    double prinPropertyTotal = 0.0;
    double intPropertyTotal = 0.0;

    Contract contract = new Contract(this);
    Session session = new Session(this);

    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");

    @Override
    public List<String> setIds(List<FilterCriteria> filterCriteria, EnquiryContext enquiryContext) {
        try {
            todayDate = session.getCurrentVariable("!TODAY");
            String companyId = session.getCompanyId();

            getFilterCriteriaDets(filterCriteria);
            initialiseCompanyInfo(companyId);

            contract.setContractId(arrangementId);
            getArrangementDetails(contract);
            getDates(selDate, selDateOp, todayDate, arrStDt);

            if (orgContractDt != null && !orgContractDt.isEmpty()) {
                EbFfLoanPaymentHisRecord loanPaymentHistRec = new EbFfLoanPaymentHisRecord(
                        da.getRecord("", "EB.FF.LOAN.PAYMENT.HIS", "", arrangementId));

                processLoanPaymentHistory(loanPaymentHistRec);
            }

            AaAccountDetailsRecord aaAccDetsRec = new AaAccountDetailsRecord(
                    da.getRecord(finMnemonic, "AA.ACCOUNT.DETAILS", "", arrangementId));
            for (BillPayDateClass billPayDateList : aaAccDetsRec.getBillPayDate()) {
                demandDate = billPayDateList.getBillPayDate().getValue();
                for (BillIdClass billIdList : billPayDateList.getBillId()) {
                    getBillAmount(billIdList.getBillId().getValue());
                }
            }

            getAaActivityHistoryDets(arrangementId);

            if (!rowRecordsPipeline.isEmpty()) {
                rowRecordsPipeline.sort((row1, row2) -> {
                    int dateCompare = row1.sortingDate.compareTo(row2.sortingDate);
                    if (dateCompare != 0) {
                        return dateCompare;
                    }
                    int priority1 = getTxnTypePriority(row1.transactionType);
                    int priority2 = getTxnTypePriority(row2.transactionType);

                    return Integer.compare(priority1, priority2);
                });
                processToBuildReturnList();
            }

        } catch (Exception e) {
            e.getMessage();
        }

        if (retvalues.isEmpty()) {
            retvalues.add("No Entries Available to Display");
            return retvalues;
        }

        return retvalues;

    }

    private int getTxnTypePriority(String transactionType) {
        if (transactionType == null) {
            return 5;
        }
        if (feesTxnTypeSet.contains(transactionType)) {
            return 1;
        }
        if (disbTxnTypeSet.contains(transactionType)) {
            return 2;
        }
        if (instTxnTypeSet.contains(transactionType)) {
            return 3;
        }
        if (collectionTxnTypeSet.contains(transactionType)) {
            return 4;
        }

        return 5;
    }

    public void processLoanPaymentHistory(EbFfLoanPaymentHisRecord loanPaymentHistRec) {
        try {
            boolean hasPendingInterest = false;
            String prevDemandDate = "";
            String prevIntDemand = "0.00";
            String prevIntDue = "0.00";
            String prevIntPymtAmt = "0.00";
            String prevIntTransAmt = "0.00";

            for (DemandDateClass demandDtCls : loanPaymentHistRec.getDemandDate()) {
                String currentDemandDate = demandDtCls.getDemandDate().getValue();
                String currentTxnType = demandDtCls.getTransType().getValue();

                if (currentTxnType.equalsIgnoreCase("Interest")
                        || currentTxnType.equalsIgnoreCase("Overdue Interest")) {
                    prevDemandDate = currentDemandDate;
                    prevIntDemand = demandDtCls.getDemandAmt().getValue();
                    prevIntDue = demandDtCls.getDueAmt().getValue();
                    prevIntPymtAmt = demandDtCls.getPymtAmt().getValue();
                    prevIntTransAmt = demandDtCls.getTransAmt().getValue();
                    hasPendingInterest = true;
                } else if (currentTxnType.equalsIgnoreCase("Principal") && hasPendingInterest
                        && currentDemandDate.equals(prevDemandDate)) {
                    String prinDemandAmt = demandDtCls.getDemandAmt().getValue();
                    String prinDueAmt = demandDtCls.getDueAmt().getValue();
                    String prinPymtAmt = demandDtCls.getPymtAmt().getValue();
                    String prinTransAmt = demandDtCls.getTransAmt().getValue();

                    double intDmd = Double.parseDouble(prevIntDemand);
                    double intDue = Double.parseDouble(prevIntDue);
                    double prinDmd = Double.parseDouble(prinDemandAmt);
                    double prinDue = Double.parseDouble(prinDueAmt);

                    demandDate = currentDemandDate;
                    transactionType = "Installment";
                    instTxnTypeSet.add(transactionType);

                    demandAmount = String.format("%.2f", intDmd + prinDmd);

                    principalDemandAmount = String.format("%.2f", prinDmd);
                    interestDemandAmount = String.format("%.2f", intDmd);

                    principalDueAmount = String.format("%.2f", prinDue);
                    interestDueAmount = String.format("%.2f", intDue);

                    principalCollected = String.format("%.2f", Math.max(0.0, prinDmd - prinDue));
                    interestCollected = String.format("%.2f", Math.max(0.0, intDmd - intDue));

                    paymentDate = demandDtCls.getPymtDate().getValue();
                    paymentMode = demandDtCls.getPymtMode().getValue();

                    double totalPymt = Double.parseDouble(prevIntPymtAmt) + Double.parseDouble(prinPymtAmt);
                    paymentAmount = String.format("%.2f", totalPymt);

                    transactionDate = demandDtCls.getTransDate().getValue();

                    double totalTrans = Double.parseDouble(prevIntTransAmt) + Double.parseDouble(prinTransAmt);
                    transactionAmount = String.format("%.2f", totalTrans);

                    principalOutstanding = demandDtCls.getPrincipalOutstanding().getValue();

                    addTxnDetsToPipeline();
                    hasPendingInterest = false;

                } else {
                    demandDate = currentDemandDate;
                    transactionType = currentTxnType;
                    demandAmount = convertAmountAsPositive(demandDtCls.getDemandAmt().getValue());
                    paymentDate = demandDtCls.getPymtDate().getValue();
                    paymentMode = demandDtCls.getPymtMode().getValue();
                    paymentAmount = convertAmountAsPositive(demandDtCls.getPymtAmt().getValue());
                    transactionDate = demandDtCls.getTransDate().getValue();
                    transactionAmount = convertAmountAsPositive(demandDtCls.getTransAmt().getValue());
                    principalOutstanding = convertAmountAsPositive(demandDtCls.getPrincipalOutstanding().getValue());

                    if (transactionType.toUpperCase().contains("DISBURSEMENT")) {
                        disbTxnTypeSet.add(transactionType);
                    } else {
                        feesTxnTypeSet.add(transactionType);
                    }

                    clearInstallmentFields();

                    addTxnDetsToPipeline();
                }
            }
        } catch (NumberFormatException e) {
            e.getMessage();
        }
    }

    private String convertAmountAsPositive(String amountStr) {
        if (amountStr == null || amountStr.isBlank()) {
            return "0.00";
        }
        try {
            double value = Double.parseDouble(amountStr);
            return String.format("%.2f", Math.abs(value));
        } catch (NumberFormatException e) {
            return "0.00";
        }
    }

    public void getAaActivityHistoryDets(String arrId) {
        try {
            AaActivityHistoryRecord aaActHisRec = new AaActivityHistoryRecord(
                    da.getRecord(finMnemonic, "AA.ACTIVITY.HISTORY", "", arrId));

            List<EffectiveDateClass> effectiveDates = aaActHisRec.getEffectiveDate();
            for (int i = effectiveDates.size() - 1; i >= 0; i--) {
                EffectiveDateClass effectiveDate = effectiveDates.get(i);

                List<ActivityRefClass> activityRefs = effectiveDate.getActivityRef();
                for (int j = activityRefs.size() - 1; j >= 0; j--) {
                    ActivityRefClass activeRef = activityRefs.get(j);
                    processBasedOnActivity(effectiveDate, activeRef);
                }
            }
        } catch (Exception e) {
            e.getMessage();
        }
    }

    public void processBasedOnActivity(EffectiveDateClass effectiveDate, ActivityRefClass activeRef) {
        boolean secondaryInit = false;
        prinPropertyTotal = 0.0;
        intPropertyTotal = 0.0;
        demandDate = "";
        transactionType = "";
        demandAmount = "";
        principalDueAmount = "";
        interestDueAmount = "";
        transactionAmount = "";
        principalCollected = "";
        interestCollected = "";
        dueAmount = "";
        paymentDate = "";
        paymentMode = "";
        paymentAmount = "";
        transactionDate = "";
        transactionAmount = "";

        String secActivity = "";

        String activity = activeRef.getActivity().getValue();

        if (!LENDING_ACTIVITIES.contains(activity)) {
            return;
        }

        String actStatus = activeRef.getActStatus().getValue();
        String initiation = activeRef.getInitiation().getValue();
        secondaryInit = initiation.equalsIgnoreCase("SECONDARY");

        if (!actStatus.equalsIgnoreCase("AUTH")) {
            return;
        }

        String activityRef = activeRef.getActivityRef().getValue();
        String effectiveDt = effectiveDate.getEffectiveDate().getValue();
        demandDate = effectiveDt;

        AaActivityRecord actRec = new AaActivityRecord(da.getRecord("", "AA.ACTIVITY", "", activity));
        transactionType = actRec.getDescription(0).getValue();
        collectionTxnTypeSet.add(transactionType);

        String contractId = activeRef.getContractId().getValue();

        Set<String> balanceProperties = Set.of("ACCOUNT.SM0ACCOUNT", "ACCOUNT.SM1ACCOUNT", "ACCOUNT.SM2ACCOUNT",
                "ACCOUNT.NPAACCOUNT", "ACCOUNT.DUEACCOUNT", "ACCOUNT.CURACCOUNT", "PRINTEREST.SM0PRINTEREST",
                "PRINTEREST.SM1PRINTEREST", "PRINTEREST.SM2PRINTEREST", "PRINTEREST.NPAPRINTEREST",
                "PRINTEREST.DUEPRINTEREST", "PRINTEREST.ACCPRINTEREST");

        if (secondaryInit) {
            if (activity.equals(LENDING_CREDIT_ARRANGEMENT)) {
                String txnContId = getTransactionRefFromAAA(activityRef);
                handleCreditArrangement(activeRef, txnContId);
            } else if (activity.equals(LENDING_WRITE_OFF_BAL_MAINTAIN)) {
                getAaArrBalMaintDets(contract, "COLL");
                addTxnDetsToPipeline();
            }
            return;
        }

        switch (activity) {
        case LENDING_APPLYPAYMENT_PR_COLLECTION:
            balanceProperties = setChgOffBalancePropType(isChargeOffTrigg, balanceProperties);

            boolean hasMaturityDayCollection = checkForMaturityCollection(effectiveDate);

            if (hasMaturityDayCollection) {
                calculateSameDayCollectionBalances(effectiveDate, balanceProperties, secActivity);
                transactionType = "Maturity Repayment Collection";
                addTxnDetsToPipeline();
            }
            break;
        case LENDING_CHARGEOFF_ARRANGEMENT:
            isChargeOffTrigg = true;
            balanceProperties = setChgOffBalancePropType(isChargeOffTrigg, balanceProperties);
            demandAmount = String.format("%.2f",
                    getAaActivityBalDetails(arrangementId, activityRef, activity, balanceProperties, secActivity));
            principalCollected = "0.00";
            interestCollected = "0.00";
            getPaymentDetails(contractId);

            setupCommonPipelineFields();
            addTxnDetsToPipeline();
            break;
        case LENDING_WRITE_OFF_BAL_MAINTAIN:
        case LENDING_APPLYPAYMENT_WRITEOFF_SETTLEMENT:
        case LENDING_APPLYPAYMENT_INSURANCE_SETTLEMENT:
        case LENDING_SETTLE_FORECLOSURE:

            balanceProperties = setChgOffBalancePropType(isChargeOffTrigg, balanceProperties);

            boolean isChildActivityTrigg = checkChildActivity(activityRef);
            if (isChildActivityTrigg) {
                secActivity = getSecActivity(activity);
            }

            demandAmount = String.format("%.2f",
                    getAaActivityBalDetails(arrangementId, activityRef, activity, balanceProperties, secActivity));
            if ("0.00".equals(demandAmount)) {
                demandAmount = activeRef.getActivityAmt().getValue();
            }

            getPaymentDetails(contractId);

            setupCommonPipelineFields();
            addTxnDetsToPipeline();
            break;
        case LENDING_CREDIT_ARRANGEMENT:
            handleCreditArrangement(activeRef, contractId);
            break;

        default:
            break;
        }
    }

    public void handleCreditArrangement(ActivityRefClass activeRef, String contractId) {
        getPaymentDetails(contractId);
        demandAmount = activeRef.getActivityAmt().getValue();
        paymentAmount = demandAmount;
        setupCommonPipelineFields();
        addTxnDetsToPipeline();
    }

    public Set<String> setChgOffBalancePropType(boolean isChargeOffTrigg, Set<String> balanceProperties) {
        if (isChargeOffTrigg) {
            return Set.of("ACCOUNT.SM0ACCOUNTCUST", "ACCOUNT.SM1ACCOUNTCUST", "ACCOUNT.SM2ACCOUNTCUST",
                    "ACCOUNT.NPAACCOUNTCUST", "ACCOUNT.DUEACCOUNTCUST", "ACCOUNT.CURACCOUNTCUST",
                    "PRINTEREST.SM0PRINTERESTCUST", "PRINTEREST.SM1PRINTERESTCUST", "PRINTEREST.SM2PRINTERESTCUST",
                    "PRINTEREST.NPAPRINTERESTCUST", "PRINTEREST.DUEPRINTERESTCUST", "PRINTEREST.ACCPRINTERESTCUST");
        }
        return balanceProperties;
    }

    public void getPaymentDetails(String contractId) {
        if (contractId.startsWith("FT")) {
            contractId = contractId.split("\\\\")[0];
            getFundsTransferDets(contractId);
            demandAmount = paymentAmount;
        } else {
            paymentDate = demandDate;
            paymentAmount = demandAmount;
            transactionDate = demandDate;
        }
    }

    public String getSecActivity(String activity) {
        if (activity.equals(LENDING_APPLYPAYMENT_WRITEOFF_SETTLEMENT) || activity.equals(LENDING_SETTLE_FORECLOSURE)) {
            return LENDING_APPLYPAYMENT_PR_CURR_BALANCE;
        } else if (activity.equals(LENDING_APPLYPAYMENT_INSURANCE_SETTLEMENT)) {
            return LENDING_APPLYPAYMENT_PR_INSURANCE_BALANCES;
        } else {
            return "";
        }
    }

    public boolean checkChildActivity(String activityRefId) {
        try {
            AaArrangementActivityRecord aaArrAct = new AaArrangementActivityRecord(
                    da.getRecord(finMnemonic, "AA.ARRANGEMENT.ACTIVITY", "", activityRefId));
            if (aaArrAct.getChildActivity() != null && !aaArrAct.getChildActivity().isEmpty()) {
                return true;
            }
        } catch (Exception e) {
            e.getMessage();
        }
        return false;
    }

    private boolean checkForMaturityCollection(EffectiveDateClass effectiveDate) {
        for (ActivityRefClass ref : effectiveDate.getActivityRef()) {
            if (ref.getActivity().getValue().equals(LENDING_MATURE_ARRANGEMENT)
                    && ref.getActStatus().getValue().equalsIgnoreCase("AUTH")) {
                return true;
            }
        }
        return false;
    }

    private void calculateSameDayCollectionBalances(EffectiveDateClass effectiveDate, Set<String> propertyNames,
            String secActivity) {
        double totalDemandSum = 0.0;

        for (ActivityRefClass ref : effectiveDate.getActivityRef()) {
            String actId = ref.getActivity().getValue();
            String status = ref.getActStatus().getValue();
            String init = ref.getInitiation().getValue();
            String contId = ref.getContractId().getValue();
            String actRef = ref.getActivityRef().getValue();

            if ("SECONDARY".equalsIgnoreCase(init) || !status.equalsIgnoreCase("AUTH")) {
                continue;
            }

            if (actId.equals(LENDING_APPLYPAYMENT_PR_COLLECTION)) {
                if (contId.startsWith("FT")) {
                    contId = contId.split("\\\\")[0];
                    getFundsTransferDets(contId);
                }
                totalDemandSum += getAaActivityBalDetails(arrangementId, actRef, actId, propertyNames, secActivity);
            }
        }

        demandAmount = String.format("%.2f", totalDemandSum);
        paymentAmount = demandAmount;

        setupCommonPipelineFields();
    }

    private void setupCommonPipelineFields() {
        principalDemandAmount = "0.00";
        interestDemandAmount = "0.00";
        principalDueAmount = "0.00";
        interestDueAmount = "0.00";
        transactionAmount = demandAmount;
    }

    public double getAaActivityBalDetails(String arrId, String currActivityRef, String currActivity,
            Set<String> propertyNames, String secActivity) {
        double totalPropAmt = 0.0;
        try {
            AaActivityBalancesRecord aaActbalRec = new AaActivityBalancesRecord(
                    da.getRecord(finMnemonic, "AA.ACTIVITY.BALANCES", "", arrId));

            com.temenos.t24.api.records.aaactivitybalances.ActivityRefClass parentMatch = null;
            com.temenos.t24.api.records.aaactivitybalances.ActivityRefClass secondaryMatch = null;

            for (com.temenos.t24.api.records.aaactivitybalances.ActivityRefClass accRefList : aaActbalRec
                    .getActivityRef()) {
                String activity = accRefList.getActivity().getValue();
                String actRef = accRefList.getActivityRef().getValue();
                if (activity.equals(currActivity) && actRef.equals(currActivityRef)) {
                    parentMatch = accRefList;
                }

                if (secActivity != null && !secActivity.isEmpty() && activity.equals(secActivity)) {
                    secondaryMatch = accRefList;
                }
            }

            if (secondaryMatch != null) {
                sumPropertyAmounts(secondaryMatch, propertyNames);
            } else if (parentMatch != null) {
                sumPropertyAmounts(parentMatch, propertyNames);
            }
            totalPropAmt = prinPropertyTotal + intPropertyTotal;
        } catch (NumberFormatException e) {
            e.getMessage();
        }

        return totalPropAmt;
    }

    public void sumPropertyAmounts(com.temenos.t24.api.records.aaactivitybalances.ActivityRefClass accRefList,
            Set<String> propertyNames) {
        prinPropertyTotal = 0.0;
        intPropertyTotal = 0.0;
        try {
            for (com.temenos.t24.api.records.aaactivitybalances.PropertyClass prop : accRefList.getProperty()) {
                String propName = prop.getProperty().getValue();
                if (propName.contains("-")) {
                    propName = propName.split("-")[0];
                }
                if (propName.toUpperCase().contains("UNCACCOUNT") || !propertyNames.contains(propName)) {
                    continue;
                }

                double propAmt = safeParseAmount(prop.getPropertyAmt().getValue());

                if (propName.startsWith(ACCOUNT)) {
                    prinPropertyTotal += propAmt;
                } else if (propName.startsWith(PRINTEREST)) {
                    intPropertyTotal += propAmt;
                }

                if (propName.startsWith("ACCOUNT.CURACCOUNT")) {
                    principalCollected = String.format("%.2f", propAmt);
                }
                if (propName.startsWith("PRINTEREST.ACCPRINTEREST")) {
                    interestCollected = String.format("%.2f", propAmt);
                }

            }
        } catch (NumberFormatException e) {
            e.getMessage();
        }
    }

    private double safeParseAmount(String value) {
        try {
            return Math.abs(Double.parseDouble(value));
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    public void getFilterCriteriaDets(List<FilterCriteria> filterCriteria) {
        try {
            for (FilterCriteria filter : filterCriteria) {
                String value = filter.getValue();
                if (value == null || value.isEmpty())
                    continue;

                switch (filter.getFieldname()) {
                case "ARRANGEMENT.ID":
                    arrangementId = value;
                    break;
                case "DATE":
                    selDate = value;
                    selDateOp = filter.getOperand();
                    break;
                default:
                    break;
                }
            }
        } catch (Exception e) {
            e.getMessage();
        }

    }

    public void getDates(String date, String dateOp, String todayDate, String arrStDt) {
        try {
            switch (dateOp) {
            case "1":
                if (date == null || date.isEmpty()) {
                    startDate = arrStDt;
                    endDate = todayDate;
                    noDateFilter = true;
                } else {
                    startDate = date;
                    endDate = date;
                }
                break;
            case "2":
                String[] dateRange = date.split(" ");
                startDate = dateRange[0];
                endDate = dateRange[1];
                break;
            case "8":
                startDate = arrStDt;
                endDate = date;
                break;
            case "3":
                LocalDate givenDateForLT = LocalDate.parse(date, formatter);
                startDate = arrStDt;
                endDate = givenDateForLT.minusDays(1).format(formatter);
                break;
            case "9":
                startDate = date;
                endDate = todayDate;
                break;
            case "4":
                LocalDate givenDateForGT = LocalDate.parse(date, formatter);
                startDate = givenDateForGT.plusDays(1).format(formatter);
                endDate = todayDate;
                break;
            default:
                startDate = arrStDt;
                endDate = todayDate;
                noDateFilter = true;
                break;
            }
        } catch (Exception e) {
            e.getMessage();
        }
    }

    public void initialiseCompanyInfo(String companyId) {
        try {
            CompanyRecord companyObj = new CompanyRecord(da.getRecord("COMPANY", companyId));
            finMnemonic = companyObj.getFinancialMne().getValue();
            mnemonic = companyObj.getCustomerMnemonic().getValue();
        } catch (Exception e) {
            e.getMessage();
        }

    }

    public void getArrangementDetails(Contract contract) {
        try {
            AaArrangementRecord arrRec = contract.getContract();
            arrStDt = arrRec.getStartDate().getValue();
            orgContractDt = arrRec.getOrigContractDate().getValue();
        } catch (Exception e) {
            e.getMessage();
        }

    }

    public void getBillAmount(String billId) {
        String billPayMethod = "";
        principalDemandAmount = "0.00";
        principalDueAmount = "0.00";
        interestDemandAmount = "0.00";
        interestDueAmount = "0.00";
        try {
            AaBillDetailsRecord aaBillRec = new AaBillDetailsRecord(
                    da.getRecord(finMnemonic, "AA.BILL.DETAILS", "", billId));

            transactionType = aaBillRec.getPaymentType().get(0).getBillType().getValue();
            demandAmount = aaBillRec.getOrTotalAmount().getValue();
            billPayMethod = aaBillRec.getPaymentType().get(0).getPaymentMethod().getValue();

            switch (transactionType) {
            case "ACT.CHARGE":
                dueAmount = aaBillRec.getProperty().get(0).getOsPropAmount().getValue();
                transactionAmount = aaBillRec.getProperty().get(0).getOrPropAmount().getValue();
                getTxnDetsForChgAndDisb(aaBillRec);

                String propName = aaBillRec.getProperty().get(0).getProperty().getValue();
                AaPropertyRecord propRec = new AaPropertyRecord(da.getRecord("AA.PROPERTY", propName));
                transactionType = propRec.getDescription().get(0).getValue();
                feesTxnTypeSet.add(transactionType);

                chgPrinOst += Double.parseDouble(paymentAmount);
                principalOutstanding = String.format("%.2f", chgPrinOst * -1);

                clearInstallmentFields();
                addTxnDetsToPipeline();
                break;
            case "DISBURSEMENT":
                transactionType = "Disbursement";
                disbTxnTypeSet.add(transactionType);

                String settleSts = aaBillRec.getSettleStatus().get(0).getSettleStatus().getValue();
                transactionAmount = aaBillRec.getProperty().get(0).getOrPropAmount().getValue();
                if (settleSts.equals("REPAID")) {
                    getTxnDetsForChgAndDisb(aaBillRec);
                } else {
                    transactionAmount = "";
                }
                clearInstallmentFields();
                addTxnDetsToPipeline();
                break;
            case "INSTALLMENT":
                if (billPayMethod.equalsIgnoreCase("CAPITALISE")) {
                    getCapitaliseBillInfo(aaBillRec);
                    return;
                }
                if (!billPayMethod.equalsIgnoreCase("DUE")
                        || Double.parseDouble(aaBillRec.getOrTotalAmount().getValue()) < 1) {
                    return;
                }
                getTxnDetsForInstallments(aaBillRec);
                break;
            default:
                break;
            }

        } catch (NumberFormatException e) {
            e.getMessage();
        }

    }

    private void clearInstallmentFields() {
        principalDemandAmount = "0.00";
        interestDemandAmount = "0.00";
        principalDueAmount = "0.00";
        interestDueAmount = "0.00";
        principalCollected = "0.00";
        interestCollected = "0.00";
    }

    public double parsePrincipalFromCharge(String lastPrinOstMvmt) {
        if (lastPrinOstMvmt != null && !lastPrinOstMvmt.isEmpty()) {
            try {
                return Double.parseDouble(lastPrinOstMvmt);
            } catch (NumberFormatException e) {
                return 0.0;
            }
        }
        return 0.0;
    }

    public void getCapitaliseBillInfo(AaBillDetailsRecord aaBillRec) {
        try {
            dueAmount = aaBillRec.getProperty().get(0).getOsPropAmount().getValue();
            transactionAmount = aaBillRec.getProperty().get(0).getOrPropAmount().getValue();
            paymentMode = "";
            paymentDate = aaBillRec.getBillStatus().get(0).getBillStChgDt().getValue();
            paymentAmount = aaBillRec.getOrTotalAmount().getValue();
            transactionDate = aaBillRec.getBillStatus().get(0).getBillStChgDt().getValue();

            String propName = aaBillRec.getProperty().get(0).getProperty().getValue();
            AaPropertyRecord propRec = new AaPropertyRecord(da.getRecord("AA.PROPERTY", propName));
            transactionType = propRec.getDescription().get(0).getValue();
            instTxnTypeSet.add(transactionType);

            principalCollected = transactionAmount;

            principalDemandAmount = "0.00";
            interestDemandAmount = "0.00";
            principalDueAmount = "0.00";
            interestDueAmount = "0.00";
            interestCollected = "0.00";
            addTxnDetsToPipeline();
        } catch (Exception e) {
            e.getMessage();
        }

    }

    public void getAaActivityHistoryDetsForInstalment(String actRepayRef) {
        String transactionRef = "";
        String txnRef = "";
        try {
            transactionRef = getTransactionRefFromAAA(actRepayRef);
            txnRef = transactionRef.split("\\\\")[0];
            if (txnRef != null && !txnRef.isEmpty()) {
                getFundsTransferDets(txnRef);
            } else {
                getAaArrBalMaintDets(contract, "INST");
            }
        } catch (Exception e) {
            e.getMessage();
        }

    }

    public String getTransactionRefFromAAA(String actRepayRef) {
        String currentRef = actRepayRef;
        try {
            while (true) {
                AaArrangementActivityRecord aaaRec = new AaArrangementActivityRecord(
                        da.getRecord(finMnemonic, "AA.ARRANGEMENT.ACTIVITY", "", currentRef));

                for (TField initationList : aaaRec.getInitiationType()) {
                    if (initationList.getValue().equalsIgnoreCase("TRANSACTION")) {
                        return aaaRec.getTxnContractId().getValue();
                    }
                }
                String masterAaaRef = aaaRec.getMasterAaa().getValue();
                if (masterAaaRef.equals(currentRef)) {
                    return aaaRec.getTxnContractId().getValue();
                }
                String linkedActRef = aaaRec.getLinkedActivity().getValue();
                if (linkedActRef != null && !linkedActRef.isEmpty()) {
                    currentRef = linkedActRef;
                } else {
                    currentRef = masterAaaRef;
                }
            }
        } catch (Exception e) {
            return "";
        }
    }

    public void getFundsTransferDets(String txnRef) {
        if (txnRef != null && txnRef.startsWith("FT")) {
            FundsTransferRecord ftRec = null;
            try {
                ftRec = new FundsTransferRecord(da.getRecord(finMnemonic, "FUNDS.TRANSFER", "", txnRef));
            } catch (Exception e) {
                try {
                    ftRec = new FundsTransferRecord(da.getHistoryRecord("FUNDS.TRANSFER", txnRef));
                } catch (Exception e1) {
                    e.getMessage();
                }
            }

            if (ftRec != null) {
                getFTfieldValues(ftRec);
            }
        }
    }

    public void getFTfieldValues(FundsTransferRecord ftRec) {
        try {
            paymentMode = ftRec.getLocalRefField("FF.PYMT.MODE").getValue();
            paymentAmount = ftRec.getCreditAmount().getValue();
            if (paymentAmount == null || paymentAmount.isEmpty()) {
                paymentAmount = ftRec.getDebitAmount().getValue();
            }
            paymentDate = ftRec.getCreditValueDate().getValue();
            transactionDate = ftRec.getProcessingDate().getValue();
        } catch (Exception e) {
            e.getMessage();
        }
    }

    public void getAaArrBalMaintDets(Contract contract, String curTxnType) {
        try {
            AaArrBalanceMaintenanceRecord arrBalMainRec = new AaArrBalanceMaintenanceRecord(
                    contract.getConditionForProperty("BAL.MAINTAIN"));
            String effectDt = arrBalMainRec.getIdComp3().getValue().split("\\.")[0];
            paymentDate = effectDt;
            transactionDate = effectDt;

            if (curTxnType.equals("INST")) {
                paymentAmount = arrBalMainRec.getNetAdjustAmt().getValue();
            } else if (curTxnType.equals("COLL")) {
                getBalMainDetsForColl(arrBalMainRec);
            }

        } catch (Exception e) {
            e.getMessage();
        }
    }

    public void getBalMainDetsForColl(AaArrBalanceMaintenanceRecord arrBalMainRec) {
        try {
            for (BillRefClass billRefList : arrBalMainRec.getBillRef()) {
                paymentAmount = billRefList.getPaymentAmount().getValue();
                demandAmount = paymentAmount;
                transactionAmount = paymentAmount;
                for (PropertyClass billPropList : billRefList.getProperty()) {
                    if (billPropList.getWofProperty().getValue().equalsIgnoreCase("YES")) {
                        String billOrAmt = String.format("%.2f",
                                Math.abs(Double.parseDouble(billPropList.getOrPropAmt().getValue())));
                        if (billPropList.getProperty().getValue().equalsIgnoreCase(ACCOUNT)) {
                            principalDemandAmount = billOrAmt;
                            principalCollected = billOrAmt;
                            principalDueAmount = "0.00";
                        }
                        if (billPropList.getProperty().getValue().equalsIgnoreCase(PRINTEREST)) {
                            interestDemandAmount = billOrAmt;
                            interestCollected = billOrAmt;
                            interestDueAmount = "0.00";
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.getMessage();
        }
    }

    public void getTxnDetsForChgAndDisb(AaBillDetailsRecord aaBillRec) {
        try {
            paymentMode = "NEFT";
            paymentDate = aaBillRec.getBillStatus().get(0).getBillStChgDt().getValue();
            paymentAmount = aaBillRec.getOrTotalAmount().getValue();
            transactionDate = aaBillRec.getBillStatus().get(0).getBillStChgDt().getValue();
        } catch (Exception e) {
            e.getMessage();
        }
    }

    public void getTxnDetsForInstallments(AaBillDetailsRecord aaBillRec) {
        try {
            transactionType = "Installment";
            instTxnTypeSet.add(transactionType);
            double totalPrinDemand = 0.0;
            double totalIntDemand = 0.0;
            double totalPrinDue = 0.0;
            double totalIntDue = 0.0;

            List<String> interestRepayRefs = new ArrayList<>();
            List<String> principalRepayRefs = new ArrayList<>();

            Map<String, Double> principalPaidMap = new HashMap<>();
            Map<String, Double> interestPaidMap = new HashMap<>();

            for (int i = 0; i < aaBillRec.getProperty().size(); i++) {
                String property = aaBillRec.getProperty().get(i).getProperty().getValue();
                double orAmt = Double.parseDouble(
                        checkAmount(aaBillRec.getProperty().get(i).getOrPropAmount().getValue()).replace(",", ""));
                double osAmt = Double.parseDouble(
                        checkAmount(aaBillRec.getProperty().get(i).getOsPropAmount().getValue()).replace(",", ""));

                aaBillRec.getSettleStatus().get(0).getSettleStatus().getValue();

                List<RepayRefClass> repayList = aaBillRec.getProperty().get(i).getRepayRef();
                List<WriteoffRefClass> writeOffRefList = aaBillRec.getProperty().get(i).getWriteoffRef();

                if (property.equalsIgnoreCase(PRINTEREST)) {
                    totalIntDemand += orAmt;
                    totalIntDue += osAmt;

                    populateRepayReferenceDetails(repayList, interestRepayRefs, interestPaidMap);
                    populateWriteOffReferenceDetails(writeOffRefList, interestRepayRefs, interestPaidMap);

                } else if (property.equalsIgnoreCase(ACCOUNT)) {
                    totalPrinDemand += orAmt;
                    totalPrinDue += osAmt;

                    populateRepayReferenceDetails(repayList, principalRepayRefs, principalPaidMap);
                    populateWriteOffReferenceDetails(writeOffRefList, principalRepayRefs, principalPaidMap);

                }
            }

            List<String> combinedRepayRefs = combineRepayReferences(interestRepayRefs, principalRepayRefs);

            if (!combinedRepayRefs.isEmpty()) {
                double currentPrinDemand = totalPrinDemand;
                double currentIntDemand = totalIntDemand;

                for (String repayRef : combinedRepayRefs) {
                    double pCollected = principalPaidMap.getOrDefault(repayRef, 0.0);
                    double iCollected = interestPaidMap.getOrDefault(repayRef, 0.0);

                    double totalDemand = currentPrinDemand + currentIntDemand;

                    demandAmount = String.format("%.2f", totalDemand);
                    principalDemandAmount = String.format("%.2f", currentPrinDemand);
                    interestDemandAmount = String.format("%.2f", currentIntDemand);

                    principalDueAmount = String.format("%.2f", totalPrinDue);
                    interestDueAmount = String.format("%.2f", totalIntDue);

                    principalCollected = String.format("%.2f", pCollected);
                    interestCollected = String.format("%.2f", iCollected);
                    transactionAmount = String.format("%.2f", pCollected + iCollected);

                    String[] tempRepayRef = repayRef.split("-");
                    String actRepayRef = tempRepayRef[0];

                    getAaActivityHistoryDetsForInstalment(actRepayRef);
                    addTxnDetsToPipeline();

                    currentPrinDemand = Math.max(0.0, currentPrinDemand - pCollected);
                    currentIntDemand = Math.max(0.0, currentIntDemand - iCollected);
                }
            } else {
                principalDemandAmount = String.format("%.2f", totalPrinDemand);
                interestDemandAmount = String.format("%.2f", totalIntDemand);
                principalDueAmount = String.format("%.2f", totalPrinDue);
                interestDueAmount = String.format("%.2f", totalIntDue);
                principalCollected = "0.00";
                interestCollected = "0.00";
                transactionAmount = "0.00";
                addTxnDetsToPipeline();
            }

        } catch (Exception e) {
            e.getMessage();
        }
    }

    private void populateRepayReferenceDetails(List<RepayRefClass> repayList, List<String> repayRefListByProp,
            Map<String, Double> repayAmtMap) {
        if (repayList == null)
            return;

        for (int j = repayList.size() - 1; j >= 0; j--) {
            RepayRefClass repayValueList = repayList.get(j);
            String refValue = repayValueList.getRepayRef().getValue();
            String amtValue = repayValueList.getRepayAmount().getValue();

            processValues(refValue, amtValue, repayRefListByProp, repayAmtMap);
        }
    }

    private void populateWriteOffReferenceDetails(List<WriteoffRefClass> writeOffList, List<String> writeRefListByProp,
            Map<String, Double> writeOffAmtMap) {
        if (writeOffList == null)
            return;

        for (int j = writeOffList.size() - 1; j >= 0; j--) {
            WriteoffRefClass writeOffValueList = writeOffList.get(j);
            String refValue = writeOffValueList.getWriteoffRef().getValue();
            String amtValue = writeOffValueList.getWriteoffAmt().getValue();

            processValues(refValue, amtValue, writeRefListByProp, writeOffAmtMap);
        }
    }

    private void processValues(String refValue, String amtValue, List<String> referenceTracker,
            Map<String, Double> trackingAmtMap) {
        if (refValue != null && !refValue.contains("SUSPEND") && !refValue.contains("CANCEL")) {
            if (!referenceTracker.contains(refValue)) {
                referenceTracker.add(refValue);
            }
            double amt = Double.parseDouble(checkAmount(amtValue).replace(",", ""));
            trackingAmtMap.put(refValue, trackingAmtMap.getOrDefault(refValue, 0.0) + amt);
        }
    }

    private List<String> combineRepayReferences(List<String> interestRefs, List<String> principalRefs) {
        List<String> combined = new ArrayList<>();
        for (String ref : interestRefs) {
            if (!combined.contains(ref)) {
                combined.add(ref);
            }
        }
        for (String ref : principalRefs) {
            if (!combined.contains(ref)) {
                combined.add(ref);
            }
        }
        return combined;
    }

    private void addTxnDetsToPipeline() {
        SoaRowData row = new SoaRowData();
        row.sortingDate = LocalDate.parse(demandDate, formatter);
        row.demandDate = demandDate;
        row.transactionType = transactionType;
        row.demandAmount = demandAmount.isEmpty() ? "0.00" : demandAmount;
        row.principalDemandAmount = principalDemandAmount.isEmpty() ? "0.00" : principalDemandAmount;
        row.interestDemandAmount = interestDemandAmount.isEmpty() ? "0.00" : interestDemandAmount;
        row.principalDueAmount = principalDueAmount.isEmpty() ? "0.00" : principalDueAmount;
        row.interestDueAmount = interestDueAmount.isEmpty() ? "0.00" : interestDueAmount;
        row.paymentDate = paymentDate;
        row.paymentMode = paymentMode;
        row.paymentAmount = paymentAmount.isEmpty() ? "0.00" : paymentAmount;
        row.transactionDate = transactionDate;
        row.transactionAmount = transactionAmount.isEmpty() ? "0.00" : transactionAmount;
        row.principalCollected = principalCollected.isEmpty() ? "0.00" : principalCollected;
        row.interestCollected = interestCollected.isEmpty() ? "0.00" : interestCollected;
        rowRecordsPipeline.add(row);

        paymentDate = "";
        paymentMode = "";
        paymentAmount = "";
        transactionDate = "";
        transactionAmount = "";
    }

    private void processToBuildReturnList() {
        double runningOutstandingPrincipal = 0.0;

        LocalDate stDt = LocalDate.parse(startDate, formatter);
        LocalDate enDt = LocalDate.parse(endDate, formatter);

        for (SoaRowData row : rowRecordsPipeline) {
            boolean insideDateRange = isRowInsideDateRange(row, stDt, enDt);

            double currentTxnAmt = Double.parseDouble(row.transactionAmount);
            double currentPrinColl = Double.parseDouble(row.principalCollected);

            if (instTxnTypeSet.contains(row.transactionType) || collectionTxnTypeSet.contains(row.transactionType)) {
                runningOutstandingPrincipal -= currentPrinColl;
            } else if (disbTxnTypeSet.contains(row.transactionType)) {
                runningOutstandingPrincipal += currentTxnAmt;
            } else if (feesTxnTypeSet.contains(row.transactionType)) {
                runningOutstandingPrincipal += currentTxnAmt;
            }

            if (!insideDateRange) {
                continue;
            }

            double finalBal = (runningOutstandingPrincipal == 0.0) ? runningOutstandingPrincipal
                    : runningOutstandingPrincipal * -1;
            String pOutstandingFormatted = String.format("%.2f", finalBal);
            retvalues.add(row.demandDate + "*" + row.transactionType + "*" + checkAmount(row.demandAmount) + "*"
                    + checkAmount(row.principalDemandAmount) + "*" + checkAmount(row.interestDemandAmount) + "*"
                    + checkAmount(row.principalDueAmount) + "*" + checkAmount(row.interestDueAmount) + "*"
                    + row.paymentDate + "*" + row.paymentMode + "*" + checkAmount(row.paymentAmount) + "*"
                    + row.transactionDate + "*" + checkAmount(row.transactionAmount) + "*"
                    + checkAmount(row.principalCollected) + "*" + checkAmount(row.interestCollected) + "*"
                    + checkAmount(pOutstandingFormatted));
        }
    }

    private boolean isRowInsideDateRange(SoaRowData row, LocalDate stDt, LocalDate enDt) {
        if (noDateFilter) {
            return true;
        }
        return (row.sortingDate.isEqual(stDt) || row.sortingDate.isAfter(stDt))
                && (row.sortingDate.isEqual(enDt) || row.sortingDate.isBefore(enDt));
    }

    public static String checkAmount(String amount) {
        if (amount == null || amount.trim().isEmpty()) {
            return "0.00";
        }
        try {
            double val = Double.parseDouble(amount.trim());
            DecimalFormat formatter = new DecimalFormat("#,##0.00");
            return formatter.format(val);
        } catch (NumberFormatException e) {
            return "0.00";
        }
    }

}
