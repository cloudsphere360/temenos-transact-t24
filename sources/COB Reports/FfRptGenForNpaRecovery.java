package com.temenos.fusion;

import java.io.File;
import java.io.FileWriter;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.temenos.api.TDate;
import com.temenos.api.TField;
import com.temenos.t24.api.arrangement.accounting.Contract;
import com.temenos.t24.api.complex.aa.contractapi.BalanceMovement;
import com.temenos.t24.api.complex.eb.servicehook.ServiceData;
import com.temenos.t24.api.hook.system.ServiceLifecycle;
import com.temenos.t24.api.records.aaaccountdetails.AaAccountDetailsRecord;
import com.temenos.t24.api.records.aaaccountdetails.AgeAllBillTypeClass;
import com.temenos.t24.api.records.aaaccountdetails.AgeAllDateClass;
import com.temenos.t24.api.records.aaactivityhistory.AaActivityHistoryRecord;
import com.temenos.t24.api.records.aaactivityhistory.ActivityRefClass;
import com.temenos.t24.api.records.aaactivityhistory.EffectiveDateClass;
import com.temenos.t24.api.records.aaarrangement.AaArrangementRecord;
import com.temenos.t24.api.records.aaarrangementactivity.AaArrangementActivityRecord;
import com.temenos.t24.api.records.aaprddesaccount.AaPrdDesAccountRecord;
import com.temenos.t24.api.records.aaprddesinterest.AaPrdDesInterestRecord;
import com.temenos.t24.api.records.aaprddestermamount.AaPrdDesTermAmountRecord;
import com.temenos.t24.api.records.account.AccountRecord;
import com.temenos.t24.api.records.account.AltAcctTypeClass;
import com.temenos.t24.api.records.company.CompanyRecord;
import com.temenos.t24.api.records.customer.CustomerRecord;
import com.temenos.t24.api.records.ebffloanpaymenthis.DemandDateClass;
import com.temenos.t24.api.records.ebffloanpaymenthis.EbFfLoanPaymentHisRecord;
import com.temenos.t24.api.records.ebffnpawriteoffmig.EbFfNpaWriteoffMigRecord;
import com.temenos.t24.api.records.ebffnpawriteoffmig.RecoveryDateClass;
import com.temenos.t24.api.records.ebffparameter.EbFfParameterRecord;
import com.temenos.t24.api.records.ebffparameter.ParamDescClass;
import com.temenos.t24.api.records.fundstransfer.FundsTransferRecord;
import com.temenos.t24.api.system.DataAccess;
import com.temenos.t24.api.system.Session;

public class FfRptGenForNpaRecovery extends ServiceLifecycle {
    public static final String FUNDS_TRANSFER = "FUNDS.TRANSFER";
    public static final String TRADE = "TRADE";
    public static final String ACCOUNT = "ACCOUNT";
    public static final String PENDING_CLOSURE = "PENDING.CLOSURE";
    public static final String CLOSE = "CLOSE";
    public static final String AA_ACCOUNT_DETAILS = "AA.ACCOUNT.DETAILS";
    public static final String AA_ACTIVITY_HISTORY = "AA.ACTIVITY.HISTORY";
    public static final String LENDING_APPLYPAYMENT_WRITEOFF_SETTLEMENT = "LENDING-APPLYPAYMENT-WRITEOFF.SETTLEMENT";
    public static final String LENDING_APPLYPAYMENT_INSURANCE_SETTLEMENT = "LENDING-APPLYPAYMENT-INSURANCE.SETTLEMENT";
    public static final String LENDING_APPLYPAYMENT_PR_OUTSTANDING_PAYOFF = "LENDING-APPLYPAYMENT-PR.OUTSTANDING.PAYOFF";
    public static final String LENDING_APPLYPAYMENT_PR_CURR_BALANCE = "LENDING-APPLYPAYMENT-PR.CURR.BALANCE";
    public static final String LENDING_APPLYPAYMENT_PR_INSURANCE_BALANCES = "LENDING-APPLYPAYMENT-PR.INSURANCE.BALANCES";
    public static final String LENDING_WRITE_OFF_BAL_MAINTAIN = "LENDING-WRITE.OFF-BAL.MAINTAIN";
    public static final String LENDING_SETTLE_FORECLOSURE = "LENDING-SETTLE-FORECLOSURE";
    public static final String LENDING_APPLYPAYMENT_PR_COLLECTION = "LENDING-APPLYPAYMENT-PR.COLLECTION";
    public static final String LENDING_MATURE_ARRANGEMENT = "LENDING-MATURE-ARRANGEMENT";
    public static final String LENDING_CHARGEOFF_ARRANGEMENT = "LENDING-CHARGEOFF-ARRANGEMENT";

    DataAccess da = new DataAccess(this);
    List<String> arrList = new ArrayList<>();
    List<String> finalArrList = new ArrayList<>();
    List<List<String>> recoveryDetails = new ArrayList<>();
    String companyId = "";
    String arrId = "";
    String finMnemonic = "";
    String mnemonic = "";
    String todayDate = "";
    String accId = "";
    String arrStatus = "";

    String branchName = "";
    String accountNumber = "";
    String legacyAcctNo = "";
    String customerNumber = "";
    String customerName = "";
    String loanStartDate = "";
    String loanMaturityDate = "";
    String interestRateAsOnDateOfNpa = "";
    String npaDate = "";
    String accountStatus = "";
    String accountCloseFlag = "";
    String closingDate = "";
    String closureType = "";
    String outstandingAmountAsOnNpaDate = "";

    boolean migratedContractFlg = false;
    boolean activityFound = false;

    boolean writeOffTriggered = false;
    boolean writeOffSettTriggered = false;
    boolean insSettTriggered = false;
    boolean settleClosureTriggered = false;
    boolean repaymentTriggered = false;
    boolean maturityTriggered = false;

    String writeOffClosureDt = "";
    String writeOffSettClosureDt = "";
    String insSettClosureDt = "";
    String settleClosureDt = "";
    String repaymentClosureDt = "";
    String maturityClosureDt = "";

    String writeOffSettContractId = "";
    String insSettContractId = "";
    String settleClosureContractId = "";
    String repaymentContractId = "";

    String writeOffActRefId = "";

    String origWriteOffDt = "";
    String chgOffDate = "";
    boolean chgOffTriggered = false;

    String filePath = "";

    public static final String FILE_NAME = "NPARecoveryReport";
    private boolean filePathFlag = false;

    public static final DateTimeFormatter T24_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    public static final DateTimeFormatter OUT_FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    @Override
    public List<String> getIds(ServiceData serviceData, List<String> controlList) {
        try {
            initialiseCompanyInfo(serviceData, companyId);
            arrList = da.selectRecords(finMnemonic, "AA.ARRANGEMENT", "", "");
            for (String contractId : arrList) {
                boolean isContractNPA = checkNpaContract(contractId);

                if (isContractNPA) {
                    finalArrList.add(contractId);
                }
            }
        } catch (Exception e) {
            e.getMessage();
        }
        return finalArrList;
    }

    public boolean checkNpaContract(String contractId) {
        try {
            AaArrangementRecord aaRec = new AaArrangementRecord(
                    da.getRecord(finMnemonic, "AA.ARRANGEMENT", "", contractId));
            String orgiContDate = aaRec.getOrigContractDate().getValue();
            if (orgiContDate != null && !orgiContDate.isEmpty() && checkMigrationNPASts(contractId)) {
                return true;
            }
            AaAccountDetailsRecord aaAccountDets = new AaAccountDetailsRecord(
                    da.getRecord(finMnemonic, AA_ACCOUNT_DETAILS, "", contractId));

            if (aaAccountDets.getArrAgeStatus().getValue().equals("NPA")) {
                return true;
            } else {
                for (AgeAllDateClass ageAllDateCls : aaAccountDets.getAgeAllDate()) {
                    for (AgeAllBillTypeClass ageBillTypeCls : ageAllDateCls.getAgeAllBillType()) {
                        if (ageBillTypeCls.getAgeAllStatus().getValue().equals("NPA")) {
                            return true;
                        }
                    }
                }
            }

        } catch (Exception e) {
            e.getMessage();
        }
        return false;
    }

    public boolean checkMigrationNPASts(String contractId) {
        try {
            EbFfNpaWriteoffMigRecord npaMigRec = new EbFfNpaWriteoffMigRecord(
                    da.getRecord(mnemonic, "EB.FF.NPA.WRITEOFF.MIG", "", contractId));
            String migNpaDate = npaMigRec.getFirstNpaDate().getValue();
            if (migNpaDate != null && !migNpaDate.isEmpty()) {
                return true;
            }

        } catch (Exception e) {
            return false;
        }
        return false;
    }

    @Override
    public void process(String id, ServiceData serviceData, String controlItem) {
        try {
            List<String> outvalues = new ArrayList<>();
            Contract contract = new Contract(this);
            Session session = new Session(this);
            todayDate = session.getCurrentVariable("!TODAY");

            arrId = id;
            contract.setContractId(arrId);
            getArrangementDetails(contract);
            initialiseCompanyInfo(serviceData, companyId);
            getCustomerDetails(customerNumber);
            getAccountDetails(accId);
            getAaArrInterestDetails(contract);
            getAaAccountDetails(contract);
            getRecoveryDetails(arrId, npaDate);
            getAaArrTermAmountDetails(contract);
            getClosingDateValue(arrId);
            getAaArrAccountDetails(contract);

            List<String> row = new ArrayList<>();
            row.add(branchName);
            row.add(accountNumber);
            row.add(legacyAcctNo);
            row.add(customerNumber);
            row.add(customerName);
            row.add(convertDate(loanStartDate));
            row.add(convertDate(loanMaturityDate));
            row.add(interestRateAsOnDateOfNpa);
            row.add(convertDate(npaDate));
            row.add(accountStatus);
            row.add(accountCloseFlag);
            row.add(convertDate(closingDate));
            row.add(closureType);
            row.add(outstandingAmountAsOnNpaDate);

            if (!recoveryDetails.isEmpty()) {
                for (List<String> recovery : recoveryDetails) {
                    row.add(recovery.get(0));
                    row.add(convertDate(recovery.get(1)));
                    row.add(recovery.get(2));
                }
            }

            outvalues.add(String.join(",", row));

            if (!filePathFlag) {
                String paramId = "FF.COB.REPORT.EXTRACT";
                EbFfParameterRecord paramRec = new EbFfParameterRecord(da.getRecord("EB.FF.PARAMETER", paramId));
                for (ParamDescClass paramDesc : paramRec.getParamDesc()) {
                    if (paramDesc.getParamName().getValue().equals("Reports Temp Path")) {
                        filePath = paramDesc.getParamValue().getValue();
                    }
                }
                filePathFlag = true;
            }
            String outputPath = filePath + FILE_NAME + "_" + finMnemonic + "_" + todayDate + "_" + "temp" + "_"
                    + session.getSessionNumber() + ".csv";

            writeToFile(outvalues, outputPath);
        } catch (Exception e) {
            e.getMessage();
        }

    }

    public String convertDate(String inDate) {
        String outDate = "";
        try {
            LocalDate date = LocalDate.parse(inDate, T24_FORMATTER);
            outDate = date.format(OUT_FORMATTER);
            return outDate;
        } catch (Exception e) {
            e.getMessage();
        }
        return outDate;
    }

    public void getArrangementDetails(Contract contract) {
        try {
            AaArrangementRecord arrRec = contract.getContract();
            companyId = arrRec.getCoCodeRec().getValue();
            accountNumber = arrId;
            accId = arrRec.getLinkedAppl().get(0).getLinkedApplId().getValue();
            customerNumber = arrRec.getCustomer().get(0).getCustomer().getValue();
            arrStatus = arrRec.getArrStatus().getValue();
            if (arrRec.getOrigContractDate().getValue() != null && !arrRec.getOrigContractDate().getValue().isEmpty()) {
                migratedContractFlg = true;
                loanStartDate = arrRec.getOrigContractDate().getValue();
            } else {
                loanStartDate = arrRec.getStartDate().getValue();
            }
        } catch (

        Exception e) {
            e.getMessage();
        }

    }

    public void getAccountDetails(String accId) {
        try {
            AccountRecord accRec = new AccountRecord(da.getRecord(finMnemonic, ACCOUNT, "", accId));
            if (migratedContractFlg) {
                for (AltAcctTypeClass altType : accRec.getAltAcctType()) {
                    if (altType.getAltAcctType().getValue().equals("LEGACY")) {
                        legacyAcctNo = altType.getAltAcctId().getValue();
                    }
                }
            }
        } catch (Exception e) {
            e.getMessage();
        }
    }

    public void getCustomerDetails(String custNumber) {
        try {
            StringBuilder custNameBuild = new StringBuilder();
            CustomerRecord custRec = new CustomerRecord(da.getRecord(mnemonic, "CUSTOMER", "", custNumber));
            if (migratedContractFlg) {
                customerNumber = custRec.getMnemonic().getValue();
            }

            TField name1Field = (custRec.getName1() != null && !custRec.getName1().isEmpty())
                    ? custRec.getName1().get(0)
                    : null;

            TField name2Field = (custRec.getName2() != null && !custRec.getName2().isEmpty())
                    ? custRec.getName2().get(0)
                    : null;

            List<String> cusNameVal = Arrays.asList(checkFiled(name1Field), checkFiled(name2Field),
                    checkFiled(custRec.getFamilyName()));
            for (String cusNameValList : cusNameVal) {
                appendIfNotEmpty(custNameBuild, cusNameValList);
            }
            customerName = custNameBuild.toString();

        } catch (Exception e) {
            e.getMessage();
        }

    }

    public String checkFiled(TField field) {
        try {
            return (field != null) ? field.getValue() : null;
        } catch (Exception e) {
            return null;
        }
    }

    public void appendIfNotEmpty(StringBuilder customerName, String value) {
        if (value != null && !value.isEmpty()) {
            if (customerName.length() > 0) {
                customerName.append(" ");
            }
            customerName.append(value);
        }
    }

    public void getAaArrAccountDetails(Contract contract) {
        try {
            AaPrdDesAccountRecord aaArrAccRec = new AaPrdDesAccountRecord(contract.getConditionForProperty(ACCOUNT));
            String ffAccountStatus = checkFiled(aaArrAccRec.getLocalRefField("FF.LOAN.STATUS"));
            if (ffAccountStatus != null && !ffAccountStatus.isEmpty()) {
                accountStatus = ffAccountStatus;
            }
            String ffClosuretype = checkFiled(aaArrAccRec.getLocalRefField("FF.LOAN.STATUS"));
            if (ffClosuretype != null && !ffClosuretype.isEmpty()) {
                closureType = ffClosuretype;
            }
            getAccountCloseFlag(accountStatus);
        } catch (Exception e) {
            e.getMessage();
        }
    }

    public void getAccountCloseFlag(String accountStatus) {
        if (!arrStatus.equals(CLOSE) && !arrStatus.equals(PENDING_CLOSURE)) {
            accountCloseFlag = "0";
        }
        if (accountStatus.equalsIgnoreCase("WRITE OFF") || arrStatus.equals(CLOSE) || arrStatus.equals(PENDING_CLOSURE)
                || arrStatus.equals("MATURED")) {
            accountCloseFlag = "1";
        }
    }

    public void getAaArrTermAmountDetails(Contract contract) {
        try {
            AaPrdDesTermAmountRecord aaArrTermAmtRec = new AaPrdDesTermAmountRecord(
                    contract.getConditionForProperty("COMMITMENT"));
            if (!aaArrTermAmtRec.toString().isEmpty()) {
                loanMaturityDate = aaArrTermAmtRec.getMaturityDate().getValue();
            }
        } catch (Exception e) {
            e.getMessage();
        }
    }

    public void getAaArrInterestDetails(Contract contract) {
        try {
            AaPrdDesInterestRecord aaArrIntRec = new AaPrdDesInterestRecord(
                    contract.getConditionForProperty("PRINTEREST"));
            interestRateAsOnDateOfNpa = aaArrIntRec.getFixedRate(0).getFixedRate().getValue();

        } catch (Exception e) {
            e.getMessage();
        }
    }

    public void getAaAccountDetails(Contract contract) {
        try {
            String arrAgeStatus = "";
            AaAccountDetailsRecord aaAcctDets = new AaAccountDetailsRecord(
                    da.getRecord(finMnemonic, AA_ACCOUNT_DETAILS, "", arrId));
            arrAgeStatus = aaAcctDets.getArrAgeStatus().getValue();
            getAccountStatus(arrAgeStatus);

            if (migratedContractFlg) {
                getFfNPAWriteOffMigDets(arrId);
                getMigRecoveryDetails(arrId, npaDate);
            }
            if (npaDate == null || npaDate.isEmpty()) {
                getAccountDetsNPADate(aaAcctDets, contract);
            }
        } catch (Exception e) {
            e.getMessage();
        }
    }

    public void getFfNPAWriteOffMigDets(String arrId) {
        try {
            EbFfNpaWriteoffMigRecord npaWriteOffMigRec = new EbFfNpaWriteoffMigRecord(
                    da.getRecord(mnemonic, "EB.FF.NPA.WRITEOFF.MIG", "", arrId));
            origWriteOffDt = npaWriteOffMigRec.getOrigWriteoffDt().getValue();
            npaDate = npaWriteOffMigRec.getFirstNpaDate().getValue();
            if (npaDate == null || npaDate.trim().isEmpty()) {
                return;
            }
            LocalDate npaDateVal = LocalDate.parse(npaDate, T24_FORMATTER);

            outstandingAmountAsOnNpaDate = npaWriteOffMigRec.getFirstNpaoutAmt().getValue();

            List<RecoveryDateClass> recovDtList = npaWriteOffMigRec.getRecoveryDate();
            for (RecoveryDateClass recovDt : recovDtList) {
                String migRecovDt = recovDt.getRecoveryDate().getValue();
                String migRecovAmt = recovDt.getRecoveryAmount().getValue();
                LocalDate curRecovDate = LocalDate.parse(migRecovDt, T24_FORMATTER);
                if (!curRecovDate.isBefore(npaDateVal)) {
                    processRecoveryDetails(migRecovDt, migRecovAmt);
                }
            }

        } catch (Exception e) {
            e.getMessage();
        }
    }

    public void getAccountDetsNPADate(AaAccountDetailsRecord aaAcctDets, Contract contract) {
        try {
            boolean npaFound = false;
            List<AgeAllDateClass> ageDateGroups = aaAcctDets.getAgeAllDate();
            for (int ageAllDtCnt = ageDateGroups.size() - 1; ageAllDtCnt >= 0; ageAllDtCnt--) {
                AgeAllDateClass currentAgeGroup = ageDateGroups.get(ageAllDtCnt);
                for (AgeAllBillTypeClass ageAllBillType : currentAgeGroup.getAgeAllBillType()) {
                    String currentAgeStatus = ageAllBillType.getAgeAllStatus().getValue();
                    if ("NPA".equals(currentAgeStatus)) {
                        npaDate = currentAgeGroup.getAgeAllDate().getValue();
                        if (npaDate != null && !npaDate.trim().isEmpty()) {
                            getEcbDetails(contract);
                            npaFound = true;
                            break;
                        }
                    }
                }
                if (npaFound) {
                    break;
                }
            }
        } catch (Exception e) {
            e.getMessage();
        }
    }

    public void getAccountStatus(String arrAgeStatus) {
        if (!arrStatus.equals(CLOSE) && !arrStatus.equals(PENDING_CLOSURE)) {
            switch (arrAgeStatus) {
            case "SM0":
                accountStatus = "ACTIVE-SMA0";
                break;
            case "SM1":
                accountStatus = "ACTIVE-SMA1";
                break;
            case "SM2":
                accountStatus = "ACTIVE-SMA2";
                break;
            case "NPA":
                accountStatus = "ACTIVE-NPA";
                break;
            default:
                accountStatus = "ACTIVE";
            }
        }
    }

    public void getMigRecoveryDetails(String arrId, String npaDate) {
        try {
            if (npaDate == null || npaDate.trim().isEmpty()) {
                return;
            }
            boolean hasPendingInterest = false;
            String prevDemandDate = "";
            String prevIntPymtAmt = "0.00";
            String paymentAmount = "";
            String currentDemandDate = "";
            String currentTxnType = "";
            String pymtDate = "";
            LocalDate npaDateVal = LocalDate.parse(npaDate, T24_FORMATTER);

            EbFfLoanPaymentHisRecord ffLoanPayHisRec = new EbFfLoanPaymentHisRecord(
                    da.getRecord(mnemonic, "EB.FF.LOAN.PAYMENT.HIS", "", arrId));

            for (DemandDateClass demandDtCls : ffLoanPayHisRec.getDemandDate()) {
                currentDemandDate = demandDtCls.getDemandDate().getValue();
                currentTxnType = demandDtCls.getTransType().getValue();
                pymtDate = demandDtCls.getPymtDate().getValue();
                LocalDate curPymtDate = LocalDate.parse(pymtDate, T24_FORMATTER);
                if (!curPymtDate.isBefore(npaDateVal)) {
                    if (currentTxnType.equalsIgnoreCase("Interest")
                            || currentTxnType.equalsIgnoreCase("Overdue Interest")) {
                        prevDemandDate = currentDemandDate;
                        prevIntPymtAmt = demandDtCls.getPymtAmt().getValue();
                        hasPendingInterest = true;
                    } else if (currentTxnType.equalsIgnoreCase("Principal") && hasPendingInterest
                            && currentDemandDate.equals(prevDemandDate)) {

                        String prinPymtAmt = demandDtCls.getPymtAmt().getValue();
                        double totalPymt = Double.parseDouble(prevIntPymtAmt) + Double.parseDouble(prinPymtAmt);
                        paymentAmount = String.format("%.2f", totalPymt);
                        processRecoveryDetails(pymtDate, paymentAmount);

                        hasPendingInterest = false;
                    }
                }
            }
        } catch (NumberFormatException e) {
            e.getMessage();
        }
    }

    public void getRecoveryDetails(String arrId, String npaDate) {
        try {
            String ftId = "";
            String recovDate = "";
            String recovAmt = "";
            LocalDate npaDateVal = LocalDate.parse(npaDate, T24_FORMATTER);

            AaActivityHistoryRecord aaActHistRec = new AaActivityHistoryRecord(
                    da.getRecord(finMnemonic, AA_ACTIVITY_HISTORY, "", arrId));
            for (EffectiveDateClass effectiveDt : aaActHistRec.getEffectiveDate()) {
                LocalDate effDate = LocalDate.parse(effectiveDt.getEffectiveDate().getValue(), T24_FORMATTER);
                if (!effDate.isBefore(npaDateVal)) {
                    for (ActivityRefClass activityRef : effectiveDt.getActivityRef()) {
                        if (activityRef.getActStatus().getValue().equals("AUTH")
                                && activityRef.getContractId().getValue().startsWith("FT")) {
                            ftId = activityRef.getContractId().getValue();
                            String[] latestFtId = ftId.split("\\\\");
                            ftId = latestFtId[0];

                            String ftResult = getFtTxnDetails(ftId);
                            String[] ftValue = ftResult.split("\\*");
                            recovDate = ftValue[0];
                            recovAmt = ftValue[1];
                            processRecoveryDetails(recovDate, recovAmt);
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.getMessage();
        }
    }

    public void processRecoveryDetails(String recovDate, String recovAmt) {
        String recovNpv = "";
        try {
            if (recovAmt != null && !recovAmt.isEmpty()) {
                double npvDblVal = getNpvValue(recovAmt, interestRateAsOnDateOfNpa, npaDate, recovDate);
                recovNpv = String.format("%.2f", npvDblVal);

                recoveryDetails.add(Arrays.asList(recovAmt, recovDate, recovNpv));
            }
        } catch (Exception e) {
            e.getMessage();
        }
    }

    public double getNpvValue(String recovAmt, String interestRateAsOnDateOfNpa, String npaDate, String recoveredDate) {
        double npv = 0.0;
        try {
            double recoveryAmount = Double.parseDouble(recovAmt);
            double intRate = Double.parseDouble(interestRateAsOnDateOfNpa);
            LocalDate npaDateLdval = LocalDate.parse(npaDate, T24_FORMATTER);
            LocalDate recoveredDateLdval = LocalDate.parse(recoveredDate, T24_FORMATTER);
            long diffInDays = ChronoUnit.DAYS.between(npaDateLdval, recoveredDateLdval);

            npv = recoveryAmount / Math.pow((1 + intRate / 100), (double) diffInDays / 365);
        } catch (NumberFormatException e) {
            e.getMessage();
        }
        return npv;
    }

    public String getFtTxnDetails(String ftId) {
        String creditAmount = "";
        String creditDate = "";
        FundsTransferRecord ftRec = null;
        try {
            ftRec = new FundsTransferRecord(da.getRecord(finMnemonic, FUNDS_TRANSFER, "", ftId));
        } catch (Exception e) {
            try {
                ftRec = new FundsTransferRecord(da.getHistoryRecord(FUNDS_TRANSFER, ftId));
            } catch (Exception e1) {
                e.getMessage();
            }
        }

        if (ftRec != null && ftRec.getTransactionType().getValue().equalsIgnoreCase("ACRP")) {
            creditDate = ftRec.getCreditValueDate().getValue();
            creditAmount = ftRec.getCreditAmount().getValue();
        }
        return creditDate + "*" + creditAmount;
    }

    public void getClosingDateValue(String arrId) {
        if (arrStatus.equals(CLOSE) || arrStatus.equals(PENDING_CLOSURE)) {
            getAaActivityHistoryDets(arrId);
            return;
        }

        if (origWriteOffDt != null && !origWriteOffDt.trim().isEmpty()) {
            closingDate = origWriteOffDt;
        } else {
            checkChargeOffActTriggered(arrId);
            if (chgOffTriggered) {
                closingDate = chgOffDate;
            }
        }
    }

    public void checkChargeOffActTriggered(String arrId) {
        chgOffTriggered = false;
        try {
            AaActivityHistoryRecord aaActHisRec = new AaActivityHistoryRecord(
                    da.getRecord(finMnemonic, AA_ACTIVITY_HISTORY, "", arrId));

            for (EffectiveDateClass effectiveDate : aaActHisRec.getEffectiveDate()) {
                for (ActivityRefClass activeRef : effectiveDate.getActivityRef()) {
                    if (activeRef.getActivity().getValue().equals(LENDING_CHARGEOFF_ARRANGEMENT)
                            && !activeRef.getInitiation().getValue().equalsIgnoreCase("SECONDARY")
                            && activeRef.getActStatus().getValue().equalsIgnoreCase("AUTH")) {
                        chgOffDate = effectiveDate.getEffectiveDate().getValue();
                        chgOffTriggered = true;
                    }
                    if (chgOffTriggered) {
                        break;
                    }
                }
                if (chgOffTriggered) {
                    break;
                }
            }
        } catch (Exception e) {
            e.getMessage();
        }
    }

    public void getAaActivityHistoryDets(String arrId) {
        activityFound = false;
        writeOffTriggered = false;
        writeOffSettTriggered = false;
        insSettTriggered = false;
        settleClosureTriggered = false;
        repaymentTriggered = false;
        maturityTriggered = false;

        writeOffClosureDt = "";
        writeOffSettClosureDt = "";
        insSettClosureDt = "";
        settleClosureDt = "";
        repaymentClosureDt = "";
        maturityClosureDt = "";

        writeOffSettContractId = "";
        insSettContractId = "";
        settleClosureContractId = "";
        repaymentContractId = "";

        writeOffActRefId = "";

        try {
            AaActivityHistoryRecord aaActHisRec = new AaActivityHistoryRecord(
                    da.getRecord(finMnemonic, AA_ACTIVITY_HISTORY, "", arrId));

            for (EffectiveDateClass effectiveDate : aaActHisRec.getEffectiveDate()) {
                for (ActivityRefClass activeRef : effectiveDate.getActivityRef()) {
                    processBasedOnActivity(effectiveDate, activeRef);
                    if (activityFound) {
                        break;
                    }
                }
                if (activityFound) {
                    break;
                }
            }
            if (writeOffTriggered) {
                closingDate = writeOffClosureDt;
                closureType = "WRITE.OFF CLOSURE";
                getAccountStatusFromAAA(writeOffActRefId);
            } else if (writeOffSettTriggered) {
                closingDate = writeOffSettClosureDt;
                String ftId = writeOffSettContractId.split("\\\\")[0];
                getFundsTransferDetails(ftId);
            } else if (insSettTriggered) {
                closingDate = insSettClosureDt;
                closureType = "DEATH CLOSURE";
                String ftId = insSettContractId.split("\\\\")[0];
                getFundsTransferDetails(ftId);
            } else if (settleClosureTriggered) {
                closingDate = settleClosureDt;
                closureType = "FORECLOSURE";
                String ftId = settleClosureContractId.split("\\\\")[0];
                getFundsTransferDetails(ftId);
            } else if ((maturityTriggered && repaymentTriggered) && (maturityClosureDt.equals(repaymentClosureDt))) {
                closingDate = repaymentClosureDt;
                closureType = "MATURITY CLOSURE";
                String ftId = repaymentContractId.split("\\\\")[0];
                getFundsTransferDetails(ftId);
            }
        } catch (Exception e) {
            e.getMessage();
        }
    }

    public void processBasedOnActivity(EffectiveDateClass effectiveDate, ActivityRefClass activeRef) {
        String activity = activeRef.getActivity().getValue();
        String actStatus = activeRef.getActStatus().getValue();
        String initiation = activeRef.getInitiation().getValue();

        if (initiation.equalsIgnoreCase("SECONDARY")) {
            return;
        }
        if (!actStatus.equalsIgnoreCase("AUTH")) {
            return;
        }

        String contractId = activeRef.getContractId().getValue();
        String activityRefId = activeRef.getActivityRef().getValue();
        String effectiveDt = effectiveDate.getEffectiveDate().getValue();

        switch (activity) {
        case LENDING_WRITE_OFF_BAL_MAINTAIN:
            writeOffTriggered = true;
            writeOffClosureDt = effectiveDt;
            writeOffActRefId = activityRefId;
            activityFound = true;
            break;

        case LENDING_APPLYPAYMENT_WRITEOFF_SETTLEMENT:
            writeOffSettTriggered = true;
            writeOffSettClosureDt = effectiveDt;
            writeOffSettContractId = contractId;
            activityFound = true;
            break;

        case LENDING_APPLYPAYMENT_INSURANCE_SETTLEMENT:
            insSettTriggered = true;
            insSettClosureDt = effectiveDt;
            insSettContractId = contractId;
            activityFound = true;
            break;

        case LENDING_SETTLE_FORECLOSURE:
            settleClosureTriggered = true;
            settleClosureDt = effectiveDt;
            settleClosureContractId = contractId;
            activityFound = true;
            break;

        case LENDING_APPLYPAYMENT_PR_COLLECTION:
            repaymentTriggered = true;
            repaymentClosureDt = effectiveDt;
            repaymentContractId = contractId;
            break;

        case LENDING_MATURE_ARRANGEMENT:
            maturityTriggered = true;
            maturityClosureDt = effectiveDt;
            if (repaymentTriggered) {
                activityFound = true;
            }
            break;

        default:
            break;
        }
    }

    public void getFundsTransferDetails(String ftId) {
        FundsTransferRecord ftRec = null;
        try {
            ftRec = new FundsTransferRecord(da.getRecord(finMnemonic, FUNDS_TRANSFER, "", ftId));
        } catch (Exception e) {
            try {
                ftRec = new FundsTransferRecord(da.getHistoryRecord(FUNDS_TRANSFER, ftId));
            } catch (Exception e1) {
                e.getMessage();
            }
        }
        if (ftRec != null) {
            accountStatus = ftRec.getLocalRefField("FF.COLL.TYPE").getValue();
        }
    }

    public void getAccountStatusFromAAA(String activityRefId) {
        try {
            AaArrangementActivityRecord aaArrAct = new AaArrangementActivityRecord(
                    da.getRecord(finMnemonic, "AA.ARRANGEMENT.ACTIVITY", "", activityRefId));
            accountStatus = aaArrAct.getNarrative().get(0).getValue();
        } catch (Exception e) {
            e.getMessage();
        }
    }

    public void getEcbDetails(Contract contract) {
        try {
            double principalOst = Double.parseDouble(getBalance(contract, "FFPRIOSTDUEAMT", TRADE));
            outstandingAmountAsOnNpaDate = String.format("%.2f", Math.abs(principalOst));
        } catch (NumberFormatException e) {
            e.getMessage();
        }
    }

    public String getBalance(Contract contract, String accountType, String bookingType) {
        List<BalanceMovement> movements = contract.getContractBalanceMovementsForPeriod(accountType, bookingType,
                new TDate(npaDate), new TDate(npaDate));
        if (movements == null || movements.isEmpty()) {
            return "0";
        }

        BalanceMovement lastMovement = movements.get(movements.size() - 1);
        return (lastMovement != null && lastMovement.getBalance() != null) ? lastMovement.getBalance().toString() : "0";
    }

    public void initialiseCompanyInfo(ServiceData serviceData, String companyId) {
        try {
            if (companyId == null || companyId.isEmpty()) {
                companyId = serviceData.getCompanyId();
            }
            CompanyRecord companyObj = new CompanyRecord(da.getRecord("COMPANY", companyId));
            finMnemonic = companyObj.getFinancialMne().getValue();
            mnemonic = companyObj.getCustomerMnemonic().getValue();
            branchName = companyObj.getCompanyName().get(0).getValue();
            String[] branchNamePart = branchName.split("-");
            branchName = branchNamePart[0];

        } catch (Exception e) {
            e.getMessage();
        }

    }

    public void writeToFile(List<String> data, String filePath) {
        try {
            File file = new File(filePath);
            file.getParentFile().mkdirs();
            boolean fileExists = file.exists();

            try (FileWriter writer = new FileWriter(file, true)) {
                if (!fileExists) {
                    String header = String.join(",", "BranchName", "AccountNumber", "LegacyLoanNumber",
                            "CustomerNumber", "CustomerName", "LoanStartDate", "LoanMaturityDate",
                            "InterestRateAsOnDateOfNPA", "NPADate", "AccountStatus", "AccountCloseFlag/WriteOffFlag",
                            "ClosingDate/WriteOffDate", "ClosureType", "OutstandingAmountAsOnNPADate");

                    int recoveryCount = 86;
                    StringBuilder recoveryHeaders = new StringBuilder();
                    for (int k = 1; k <= recoveryCount; k++) {
                        recoveryHeaders.append(",RecoveredAmount-").append(k);
                        recoveryHeaders.append(",RecoveredDate-").append(k);
                        recoveryHeaders.append(",NPV-").append(k);
                    }

                    writer.write(header + recoveryHeaders + System.lineSeparator());
                }

                for (String line : data) {
                    writer.write(line + System.lineSeparator());
                }
            }

        } catch (Exception e) {
            e.getMessage();
        }
    }

}
