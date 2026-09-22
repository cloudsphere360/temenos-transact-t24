package com.temenos.fusion;

import java.io.File;
import java.io.FileWriter;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.temenos.api.TField;
import com.temenos.t24.api.arrangement.accounting.Contract;
import com.temenos.t24.api.complex.eb.servicehook.ServiceData;
import com.temenos.t24.api.hook.system.ServiceLifecycle;
import com.temenos.t24.api.records.aaaccountdetails.AaAccountDetailsRecord;
import com.temenos.t24.api.records.aaaccountdetails.BillIdClass;
import com.temenos.t24.api.records.aaaccountdetails.BillPayDateClass;
import com.temenos.t24.api.records.aaactivitybalances.AaActivityBalancesRecord;
import com.temenos.t24.api.records.aaactivityhistory.AaActivityHistoryRecord;
import com.temenos.t24.api.records.aaactivityhistory.ActivityRefClass;
import com.temenos.t24.api.records.aaactivityhistory.EffectiveDateClass;
import com.temenos.t24.api.records.aaarrangement.AaArrangementRecord;
import com.temenos.t24.api.records.aaarrangementactivity.AaArrangementActivityRecord;
import com.temenos.t24.api.records.aaarrbalancemaintenance.AaArrBalanceMaintenanceRecord;
import com.temenos.t24.api.records.aaarrbalancemaintenance.AdjBalTypeClass;
import com.temenos.t24.api.records.aaarrbalancemaintenance.AdjustPropClass;
import com.temenos.t24.api.records.aabilldetails.AaBillDetailsRecord;
import com.temenos.t24.api.records.aabilldetails.PropertyClass;
import com.temenos.t24.api.records.aaoverduestats.AaOverdueStatsRecord;
import com.temenos.t24.api.records.aaoverduestats.MvmtDateClass;
import com.temenos.t24.api.records.aaoverduestats.OdStatusClass;
import com.temenos.t24.api.records.aaprddesaccount.AaPrdDesAccountRecord;
import com.temenos.t24.api.records.aaprddestermamount.AaPrdDesTermAmountRecord;
import com.temenos.t24.api.records.aaproduct.AaProductRecord;
import com.temenos.t24.api.records.account.AccountRecord;
import com.temenos.t24.api.records.account.AltAcctTypeClass;
import com.temenos.t24.api.records.company.CompanyRecord;
import com.temenos.t24.api.records.customer.CustomerRecord;
import com.temenos.t24.api.records.ebffparameter.EbFfParameterRecord;
import com.temenos.t24.api.records.ebffparameter.ParamDescClass;
import com.temenos.t24.api.records.fundstransfer.FundsTransferRecord;
import com.temenos.t24.api.records.user.UserRecord;
import com.temenos.t24.api.system.DataAccess;
import com.temenos.t24.api.system.Session;
import com.temenos.t24.api.records.ebffloandpd.EbFfLoanDpdRecord;
import com.temenos.t24.api.records.ebffloanpaymenthis.DemandDateClass;
import com.temenos.t24.api.records.ebffloanpaymenthis.EbFfLoanPaymentHisRecord;

public class FfBDropoutClosureReport extends ServiceLifecycle {
    public static final String BOOKING = "BOOKING";
    public static final String ACCOUNT = "ACCOUNT";
    public static final String PRINTEREST = "PRINTEREST";
    public static final String FILE_NAME = "ClosureReport";
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

    Set<String> dueInterestProps = new HashSet<>(Set.of("PRINTEREST.SM0PRINTEREST", "PRINTEREST.SM1PRINTEREST",
            "PRINTEREST.SM2PRINTEREST", "PRINTEREST.NPAPRINTEREST", "PRINTEREST.DUEPRINTEREST"));
    Set<String> accInterestProps = new HashSet<>(Set.of("PRINTEREST.ACCPRINTEREST"));

    Set<String> dueAccountProps = new HashSet<>(Set.of("ACCOUNT.SM0ACCOUNT", "ACCOUNT.SM1ACCOUNT", "ACCOUNT.SM2ACCOUNT",
            "ACCOUNT.NPAACCOUNT", "ACCOUNT.DUEACCOUNT"));
    Set<String> accAccountProps = new HashSet<>(Set.of("ACCOUNT.CURACCOUNT"));

    Set<String> dueCustInterestProps = Set.of("PRINTEREST.SM0PRINTERESTCUST", "PRINTEREST.SM1PRINTERESTCUST",
            "PRINTEREST.SM2PRINTERESTCUST", "PRINTEREST.NPAPRINTERESTCUST", "PRINTEREST.DUEPRINTERESTCUST");
    Set<String> accCustInterestProps = Set.of("PRINTEREST.ACCPRINTERESTCUST");

    Set<String> dueCustAccountProps = Set.of("ACCOUNT.SM0ACCOUNTCUST", "ACCOUNT.SM1ACCOUNTCUST",
            "ACCOUNT.SM2ACCOUNTCUST", "ACCOUNT.NPAACCOUNTCUST", "ACCOUNT.DUEACCOUNTCUST");
    Set<String> accCustAccountProps = Set.of("ACCOUNT.CURACCOUNTCUST");

    List<String> arrList = new ArrayList<>();

    DataAccess da = new DataAccess(this);
    Session session = new Session(this);

    String branchName = "";
    String branchCode = "";
    String center = "";
    String customerNumber = "";
    String customerName = "";
    String accountNumber = "";
    String legacyAcctNo = "";
    String productName = "";
    String loanAmount = "";
    String cycleNumber = "";
    String disbursementDate = "";
    String maturityDate = "";
    String closureDate = "";
    String closureReason = "";
    String closureType = "";
    String interestCollected = "";
    String totalInterestCollected = "";
    String totalPrincipalCollected = "";
    String closingPrincipal = "";
    String penaltyCollected = "0";
    String funder = "";
    String fundingSource = "";
    String user = "";
    String employeeName = "";
    String employeeNumber = "";
    String remarks = "";
    String dpd = "";
    String overdueAmt = "";

    String finMnemonic = "";
    String mnemonic = "";
    String companyId = "";
    String arrId = "";
    String filePath = "";
    String outputPath = "";
    String todayDate = "";
    String accId = "";

    double prinPaidAtClosure = 0.0;
    double intPaidAtClosure = 0.0;
    double totIntCollAmt = 0.0;
    double totPrinCollAmt = 0.0;

    boolean chgOffTriggered = false;
    boolean writeOffTriggered = false;
    boolean writeOffSettTriggered = false;
    boolean insSettTriggered = false;
    boolean settleClosureTriggered = false;
    boolean repaymentTriggered = false;
    boolean maturityTriggered = false;

    String writeOffSettContractId = "";
    String insSettContractId = "";
    String settleClosureContractId = "";
    String repaymentContractId = "";

    String writeOffActRefId = "";
    String writeOffSettActRefId = "";
    String insSettActRefId = "";
    String settleClosureActRefId = "";
    String repaymentActRefId = "";

    String writeOffClosureDt = "";
    String writeOffSettClosureDt = "";
    String insSettClosureDt = "";
    String settleClosureDt = "";
    String repaymentClosureDt = "";
    String maturityClosureDt = "";

    boolean migratedContractFlg = false;
    boolean activityFound = false;
    private boolean filePathFlag = false;

    public static final DateTimeFormatter T24_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    public static final DateTimeFormatter OUT_FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy");
    public static final DateTimeFormatter MONTH_YEAR_FORMAT = DateTimeFormatter.ofPattern("MMMuuuu");

    @Override
    public List<String> getIds(ServiceData serviceData, List<String> controlList) {
        try {
            initialiseCompanyInfo(serviceData, companyId);
            arrList = da.selectRecords(finMnemonic, "AA.ARRANGEMENT", "", "WITH ARR.STATUS EQ CLOSE PENDING.CLOSURE");
        } catch (Exception e) {
            e.getMessage();
        }
        return arrList;
    }

    @Override
    public void process(String id, ServiceData serviceData, String controlItem) {
        List<String> outvalues = new ArrayList<>();
        try {
            todayDate = session.getCurrentVariable("!TODAY");
            arrId = id;
            Contract contract = new Contract(this);
            contract.setContractId(arrId);
            getArrangementDetails(contract);
            initialiseCompanyInfo(serviceData, companyId);
            getCustomerDetails(customerNumber);
            getAccountDetails(accId);
            getAaArrTermAmountDetails(contract);
            checkChargeOffActTriggered(arrId);
            getAaActivityHistoryDets(arrId, contract);
            getPaymentHistoryDets(contract);
            getDpdBalanceDetails(arrId);
            getAaOverdueStatusDetails(arrId);
            getAaArrAccountDetails(contract);
            List<String> row = new ArrayList<>();

            row.add(branchName);
            row.add(branchCode);
            row.add(center);
            row.add(customerNumber);
            row.add(customerName);
            row.add(accountNumber);
            row.add(legacyAcctNo);
            row.add(productName);
            row.add(loanAmount);
            row.add(cycleNumber);
            row.add(convertDate(disbursementDate));
            row.add(convertDate(maturityDate));
            row.add(convertDate(closureDate));
            row.add(closureReason);
            row.add(closureType);
            row.add(interestCollected);
            row.add(totalInterestCollected);
            row.add(closingPrincipal);
            row.add(penaltyCollected);
            row.add(funder);
            row.add(fundingSource);
            row.add(user);
            row.add(employeeName);
            row.add(employeeNumber);
            row.add(remarks);
            row.add(dpd);
            row.add(overdueAmt);

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

            outputPath = filePath + FILE_NAME + "_" + finMnemonic + "_" + todayDate + "_" + "temp" + "_"
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

    public void initialiseCompanyInfo(ServiceData serviceData, String companyId) {
        try {
            if (companyId == null || companyId.isEmpty()) {
                companyId = serviceData.getCompanyId();
            }
            CompanyRecord companyObj = new CompanyRecord(da.getRecord("COMPANY", companyId));
            finMnemonic = companyObj.getFinancialMne().getValue();
            mnemonic = companyObj.getCustomerMnemonic().getValue();
            branchCode = companyId;
            branchName = companyObj.getCompanyName().get(0).getValue();
            String[] branchNamePart = branchName.split("-");
            branchName = branchNamePart[0];
        } catch (Exception e) {
            e.getMessage();
        }

    }

    public void getArrangementDetails(Contract contract) {
        try {
            AaArrangementRecord arrRec = contract.getContract();
            String product = arrRec.getProduct().get(0).getProduct().getValue();
            getAaProductDetails(product);
            companyId = arrRec.getCoCodeRec().getValue();
            accountNumber = arrId;
            accId = arrRec.getLinkedAppl().get(0).getLinkedApplId().getValue();
            customerNumber = arrRec.getCustomer().get(0).getCustomer().getValue();
            if (arrRec.getOrigContractDate().getValue() != null && !arrRec.getOrigContractDate().getValue().isEmpty()) {
                migratedContractFlg = true;
                disbursementDate = arrRec.getOrigContractDate().getValue();
            } else {
                disbursementDate = arrRec.getStartDate().getValue();
            }
        } catch (Exception e) {
            e.getMessage();
        }

    }

    public void getAaProductDetails(String productId) {
        try {
            AaProductRecord aaProRec = new AaProductRecord(da.getRecord("AA.PRODUCT", productId));
            productName = aaProRec.getDescription(0).getValue();
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

    public void getAccountDetails(String accId) {
        try {
            AccountRecord accRec = new AccountRecord(da.getRecord(finMnemonic, ACCOUNT, "", accId));
            funder = accRec.getLocalRefField("FF.FUND.SOURCE").getValue();
            fundingSource = accRec.getLocalRefField("FF.FUND.SOURCE").getValue();
            center = accRec.getLocalRefField("FF.CENTRE").getValue();
            cycleNumber = accRec.getLocalRefField("FF.LOAN.CYCLE").getValue();
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

    public void getAaArrAccountDetails(Contract contract) {
        try {
            AaPrdDesAccountRecord aaArrAccRec = new AaPrdDesAccountRecord(contract.getConditionForProperty(ACCOUNT));
            String ffClosuretype = checkFiled(aaArrAccRec.getLocalRefField("FF.LOAN.STATUS"));
            if (ffClosuretype != null && !ffClosuretype.isEmpty()) {
                closureType = ffClosuretype;
            }
        } catch (Exception e) {
            e.getMessage();
        }
    }

    public void getAaArrTermAmountDetails(Contract contract) {
        try {
            AaPrdDesTermAmountRecord aaArrTermAmtRec = new AaPrdDesTermAmountRecord(
                    contract.getConditionForProperty("COMMITMENT"));
            if (!aaArrTermAmtRec.toString().isEmpty()) {
                loanAmount = aaArrTermAmtRec.getAmount().getValue();
                maturityDate = aaArrTermAmtRec.getMaturityDate().getValue();
            }
        } catch (Exception e) {
            e.getMessage();
        }
    }

    public void checkChargeOffActTriggered(String arrId) {
        chgOffTriggered = false;
        try {
            AaActivityHistoryRecord aaActHisRec = new AaActivityHistoryRecord(
                    da.getRecord(finMnemonic, "AA.ACTIVITY.HISTORY", "", arrId));

            for (EffectiveDateClass effectiveDate : aaActHisRec.getEffectiveDate()) {
                for (ActivityRefClass activeRef : effectiveDate.getActivityRef()) {
                    if (activeRef.getActivity().getValue().equals(LENDING_CHARGEOFF_ARRANGEMENT)
                            && !activeRef.getInitiation().getValue().equalsIgnoreCase("SECONDARY")
                            && activeRef.getActStatus().getValue().equalsIgnoreCase("AUTH")) {
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

    public void getAaActivityHistoryDets(String arrId, Contract contract) {
        activityFound = false;
        writeOffTriggered = false;
        writeOffSettTriggered = false;
        insSettTriggered = false;
        settleClosureTriggered = false;
        repaymentTriggered = false;
        maturityTriggered = false;

        writeOffSettContractId = "";
        insSettContractId = "";
        settleClosureContractId = "";
        repaymentContractId = "";

        writeOffActRefId = "";
        writeOffSettActRefId = "";
        insSettActRefId = "";
        settleClosureActRefId = "";
        repaymentActRefId = "";

        writeOffClosureDt = "";
        writeOffSettClosureDt = "";
        insSettClosureDt = "";
        settleClosureDt = "";
        repaymentClosureDt = "";
        maturityClosureDt = "";

        intPaidAtClosure = 0.0;
        prinPaidAtClosure = 0.0;

        try {
            AaActivityHistoryRecord aaActHisRec = new AaActivityHistoryRecord(
                    da.getRecord(finMnemonic, "AA.ACTIVITY.HISTORY", "", arrId));

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

            processConvertChgOffTriggBalProp();

            if (writeOffTriggered) {
                closureDate = writeOffClosureDt;
                closureType = "WRITE.OFF CLOSURE";
                getAaArrBalMaintDets(contract);

                getInputterNameFromAAA(writeOffActRefId);
            } else if (writeOffSettTriggered) {
                closureDate = writeOffSettClosureDt;
                String ftId = writeOffSettContractId.split("\\\\")[0];
                getFundsTransferDetails(ftId);

                intPaidAtClosure = getAaActivityBalDetails(arrId, LENDING_APPLYPAYMENT_WRITEOFF_SETTLEMENT,
                        dueCustInterestProps, LENDING_APPLYPAYMENT_PR_CURR_BALANCE, accCustInterestProps);
                prinPaidAtClosure = getAaActivityBalDetails(arrId, LENDING_APPLYPAYMENT_WRITEOFF_SETTLEMENT,
                        dueCustAccountProps, LENDING_APPLYPAYMENT_PR_CURR_BALANCE, accCustAccountProps);

                getInputterNameFromAAA(writeOffSettActRefId);
            } else if (insSettTriggered) {
                closureDate = insSettClosureDt;
                closureType = "DEATH CLOSURE";
                String ftId = insSettContractId.split("\\\\")[0];
                getFundsTransferDetails(ftId);

                intPaidAtClosure = getAaActivityBalDetails(arrId, LENDING_APPLYPAYMENT_INSURANCE_SETTLEMENT,
                        dueInterestProps, LENDING_APPLYPAYMENT_PR_INSURANCE_BALANCES, accInterestProps);
                prinPaidAtClosure = getAaActivityBalDetails(arrId, LENDING_APPLYPAYMENT_INSURANCE_SETTLEMENT,
                        dueAccountProps, LENDING_APPLYPAYMENT_PR_INSURANCE_BALANCES, accAccountProps);

                getInputterNameFromAAA(insSettActRefId);
            } else if (settleClosureTriggered) {
                closureDate = settleClosureDt;
                closureType = "FORECLOSURE";
                String ftId = settleClosureContractId.split("\\\\")[0];
                getFundsTransferDetails(ftId);

                intPaidAtClosure = getAaActivityBalDetails(arrId, LENDING_APPLYPAYMENT_PR_OUTSTANDING_PAYOFF,
                        dueInterestProps, LENDING_APPLYPAYMENT_PR_CURR_BALANCE, accInterestProps);
                prinPaidAtClosure = getAaActivityBalDetails(arrId, LENDING_APPLYPAYMENT_PR_OUTSTANDING_PAYOFF,
                        dueAccountProps, LENDING_APPLYPAYMENT_PR_CURR_BALANCE, accAccountProps);
                getInputterNameFromAAA(settleClosureActRefId);
            } else if ((maturityTriggered && repaymentTriggered) && (maturityClosureDt.equals(repaymentClosureDt))) {
                closureDate = repaymentClosureDt;
                closureType = "MATURITY CLOSURE";
                String ftId = repaymentContractId.split("\\\\")[0];
                getFundsTransferDetails(ftId);

                intPaidAtClosure = getAaActivityBalDetails(arrId, LENDING_APPLYPAYMENT_PR_COLLECTION, dueInterestProps,
                        null, null);
                prinPaidAtClosure = getAaActivityBalDetails(arrId, LENDING_APPLYPAYMENT_PR_COLLECTION, dueAccountProps,
                        null, null);

                getInputterNameFromAAA(repaymentActRefId);
            }

            interestCollected = String.format("%.2f", Math.abs(intPaidAtClosure));
            closingPrincipal = String.format("%.2f", Math.abs(prinPaidAtClosure));
        } catch (Exception e) {
            e.getMessage();
        }
    }

    public void processConvertChgOffTriggBalProp() {
        try {
            if (chgOffTriggered) {
                dueInterestProps.clear();
                accInterestProps.clear();
                dueAccountProps.clear();
                accAccountProps.clear();

                dueInterestProps.addAll(dueCustInterestProps);
                accInterestProps.addAll(accCustInterestProps);
                dueAccountProps.addAll(dueCustAccountProps);
                accAccountProps.addAll(accCustAccountProps);
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
            writeOffSettActRefId = activityRefId;
            writeOffSettContractId = contractId;
            activityFound = true;
            break;

        case LENDING_APPLYPAYMENT_INSURANCE_SETTLEMENT:
            insSettTriggered = true;
            insSettClosureDt = effectiveDt;
            insSettActRefId = activityRefId;
            insSettContractId = contractId;
            activityFound = true;
            break;

        case LENDING_SETTLE_FORECLOSURE:
            settleClosureTriggered = true;
            settleClosureDt = effectiveDt;
            settleClosureActRefId = activityRefId;
            settleClosureContractId = contractId;
            activityFound = true;
            break;

        case LENDING_APPLYPAYMENT_PR_COLLECTION:
            repaymentTriggered = true;
            repaymentClosureDt = effectiveDt;
            repaymentActRefId = activityRefId;
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
            ftRec = new FundsTransferRecord(da.getRecord(finMnemonic, "FUNDS.TRANSFER", "", ftId));
        } catch (Exception e) {
            try {
                ftRec = new FundsTransferRecord(da.getHistoryRecord("FUNDS.TRANSFER", ftId));
            } catch (Exception e1) {
                e.getMessage();
            }
        }
        if (ftRec != null) {
            remarks = ftRec.getLocalRefField("FF.NARRATION").getValue();
            closureReason = ftRec.getLocalRefField("FF.COLL.TYPE").getValue();
        }
    }

    public void getAaArrBalMaintDets(Contract contract) {
        try {
            AaArrBalanceMaintenanceRecord arrBalMainRec = new AaArrBalanceMaintenanceRecord(
                    contract.getConditionForProperty("BAL.MAINTAIN"));
            for (AdjustPropClass adjProp : arrBalMainRec.getAdjustProp()) {
                proceesToGetAdjBalDets(adjProp);
            }
        } catch (NumberFormatException e) {
            e.getMessage();
        } catch (Exception e1) {
            e1.getMessage();
        }
    }

    public void proceesToGetAdjBalDets(AdjustPropClass adjProp) {
        try {
            if (adjProp.getAdjustProp().getValue().equals(ACCOUNT)
                    || adjProp.getAdjustProp().getValue().equals(PRINTEREST)) {
                for (AdjBalTypeClass adjBal : adjProp.getAdjBalType()) {
                    if (adjBal.getAdjBalType().getValue().equals("CURACCOUNT")) {
                        prinPaidAtClosure = Double.parseDouble(adjBal.getOrigBalAmt().getValue())
                                - Double.parseDouble(adjBal.getNewBalAmt().getValue());
                    }
                    if (adjBal.getAdjBalType().getValue().equals("ACCPRINTEREST")) {
                        intPaidAtClosure = Double.parseDouble(adjBal.getOrigBalAmt().getValue())
                                - Double.parseDouble(adjBal.getNewBalAmt().getValue());
                    }
                }
            }
        } catch (NumberFormatException e) {
            e.getMessage();
        }
    }

    public double getAaActivityBalDetails(String arrId, String secActivity1, Set<String> secActivity1Props,
            String secActivity2, Set<String> secActivity2Props) {
        double totalPropAmt = 0.0;
        double secActivity1Total = 0.0;
        double secActivity2Total = 0.0;
        try {
            AaActivityBalancesRecord aaActbalRec = new AaActivityBalancesRecord(
                    da.getRecord(finMnemonic, "AA.ACTIVITY.BALANCES", "", arrId));
            for (com.temenos.t24.api.records.aaactivitybalances.ActivityRefClass accRefList : aaActbalRec
                    .getActivityRef()) {
                String activity = accRefList.getActivity().getValue();
                if (secActivity1 != null && secActivity1Props != null && activity.equals(secActivity1)) {
                    secActivity1Total += sumPropertyAmounts(accRefList, secActivity1Props);
                }

                if (secActivity2 != null && secActivity2Props != null && activity.equals(secActivity2)) {
                    secActivity2Total += sumPropertyAmounts(accRefList, secActivity2Props);
                }
            }

            totalPropAmt = secActivity1Total + secActivity2Total;
        } catch (NumberFormatException e) {
            e.getMessage();
        } catch (Exception e1) {
            e1.getMessage();
        }

        return totalPropAmt;
    }

    public double sumPropertyAmounts(com.temenos.t24.api.records.aaactivitybalances.ActivityRefClass accRefList,
            Set<String> propertyNames) {
        double propertyTotal = 0.0;
        try {
            for (com.temenos.t24.api.records.aaactivitybalances.PropertyClass prop : accRefList.getProperty()) {
                String propName = prop.getProperty().getValue();
                if (propertyNames.contains(propName)) {
                    propertyTotal += Double.parseDouble(prop.getPropertyAmt().getValue());
                }
            }
        } catch (NumberFormatException e) {
            e.getMessage();
        }
        return propertyTotal;
    }

    public void getInputterNameFromAAA(String activityRefId) {
        String approverName = "";
        try {
            AaArrangementActivityRecord aaArrAct = new AaArrangementActivityRecord(
                    da.getRecord(finMnemonic, "AA.ARRANGEMENT.ACTIVITY", "", activityRefId));
            approverName = aaArrAct.getAuthoriser();
            String[] approverNameSplit = approverName.split("_");
            user = approverNameSplit[1];

            UserRecord userRec = new UserRecord(da.getRecord("", "USER", "", user));
            employeeName = userRec.getUserName().getValue();
            employeeNumber = user;

            if (remarks == null || remarks.isEmpty()) {
                remarks = aaArrAct.getNarrative().get(0).getValue();
            }

            if (closureReason == null || closureReason.isEmpty()) {
                closureReason = aaArrAct.getNarrative().get(0).getValue();
            }
        } catch (Exception e) {
            e.getMessage();
        }
    }

    public void getPaymentHistoryDets(Contract contract) {
        try {
            totIntCollAmt = 0.0;
            totPrinCollAmt = 0.0;
            AaAccountDetailsRecord aaAcctDets = contract.getAccountDetailsRecord();

            if (migratedContractFlg) {
                EbFfLoanPaymentHisRecord ffLoanPayHistRec = new EbFfLoanPaymentHisRecord(
                        da.getRecord("", "EB.FF.LOAN.PAYMENT.HIS", "", arrId));

                LocalDate currDate = LocalDate.parse(todayDate, T24_FORMATTER);

                for (DemandDateClass demandDtList : ffLoanPayHistRec.getDemandDate()) {
                    LocalDate demandDate = LocalDate.parse(demandDtList.getDemandDate().getValue(), T24_FORMATTER);
                    double dueAmt = Double.parseDouble(demandDtList.getDueAmt().getValue());

                    if (!demandDate.isAfter(currDate) && dueAmt == 0.0) {
                        processToGetCollectionAmt(demandDtList);
                    }
                }
            }

            getPaymentDetsFromAccountDets(aaAcctDets);

            totalInterestCollected = String.format("%.2f", Math.abs(totIntCollAmt));
            totalPrincipalCollected = String.format("%.2f", Math.abs(totPrinCollAmt));
        } catch (NumberFormatException e) {
            e.getMessage();
        } catch (Exception e1) {
            e1.getMessage();
        }

    }

    public void processToGetCollectionAmt(DemandDateClass demandDtList) {
        try {
            double demandAmt = Double.parseDouble(demandDtList.getDemandAmt().getValue());
            switch (demandDtList.getTransType().getValue().toUpperCase()) {
            case "INTEREST":
                totIntCollAmt += demandAmt;
                break;
            case "PRINCIPAL":
                totPrinCollAmt += demandAmt;
                break;
            default:
                break;
            }
        } catch (NumberFormatException e) {
            e.getMessage();
        }
    }

    public void getPaymentDetsFromAccountDets(AaAccountDetailsRecord aaAcctDetails) {
        try {
            for (BillPayDateClass billPayDate : aaAcctDetails.getBillPayDate()) {
                for (BillIdClass billId : billPayDate.getBillId()) {
                    if (billId.getBillType().getValue().equals("INSTALLMENT")
                            && (billId.getSetStatus().getValue().equals("SETTLED")
                                    || billId.getSetStatus().getValue().equals("REPAID"))) {
                        getAaBillDetails(billId.getBillId().getValue());
                    }
                }
            }
        } catch (Exception e) {
            e.getMessage();
        }
    }

    public void getAaBillDetails(String billId) {
        try {
            AaBillDetailsRecord billDetailRecord = new AaBillDetailsRecord(
                    da.getRecord(finMnemonic, "AA.BILL.DETAILS", "", billId));

            for (PropertyClass prop : billDetailRecord.getProperty()) {
                double orPropAmount = Double.parseDouble(prop.getOrPropAmount().getValue());
                switch (prop.getProperty().getValue().toUpperCase()) {
                case PRINTEREST:
                    totIntCollAmt += orPropAmount;
                    break;
                case ACCOUNT:
                    totPrinCollAmt += orPropAmount;
                    break;
                default:
                    break;
                }
            }
        } catch (NumberFormatException e) {
            e.getMessage();
        } catch (Exception e1) {
            e1.getMessage();
        }
    }

    public void getDpdBalanceDetails(String arrId) {
        try {
            LocalDate currDate = LocalDate.parse(todayDate, T24_FORMATTER);
            String currMonthYear = currDate.format(MONTH_YEAR_FORMAT).toUpperCase();
            String dpdRecId = arrId + "-" + currMonthYear;

            int curDpd = getLatestNonZeroDpdValue(dpdRecId);

            if (curDpd <= 0) {
                LocalDate prevMonthDate = currDate.minusMonths(1);
                String prevMonthYear = prevMonthDate.format(MONTH_YEAR_FORMAT).toUpperCase();
                String prevDpdRecId = arrId + "-" + prevMonthYear;

                int prevDpd = getLatestNonZeroDpdValue(prevDpdRecId);

                if (curDpd == -1 && prevDpd == -1) {
                    dpd = "";
                    return;
                } else if (prevDpd != -1) {
                    curDpd = prevDpd;
                } else if (prevDpd == -1) {
                    dpd = String.valueOf(curDpd);
                    return;
                }
            }
            dpd = String.valueOf(curDpd);
        } catch (Exception e) {
            e.getMessage();
        }
    }

    public int getLatestNonZeroDpdValue(String dpdRecId) {
        try {
            EbFfLoanDpdRecord loanDpdRec = new EbFfLoanDpdRecord(
                    da.getRecord(finMnemonic, "EB.FF.LOAN.DPD", "", dpdRecId));
            for (int i = loanDpdRec.getDate().size() - 1; i >= 0; i--) {
                int currentDpdval = Integer.parseInt(loanDpdRec.getDate().get(i).getCurDpd().getValue());
                if (currentDpdval != 0) {
                    return currentDpdval;
                }
            }
        } catch (Exception e) {
            return -1;
        }
        return 0;
    }

    public void getAaOverdueStatusDetails(String arrId) {
        try {
            AaOverdueStatsRecord aaOverDueStsRec = new AaOverdueStatsRecord(
                    da.getRecord(finMnemonic, "AA.OVERDUE.STATS", "", arrId + "-INSTALLMENT-DPD.STAGES"));
            int oldStatusCnt = aaOverDueStsRec.getOdStatus().size() - 1;
            for (int oldStsPos = oldStatusCnt; oldStsPos >= 0; oldStsPos--) {
                OdStatusClass oldStausCls = aaOverDueStsRec.getOdStatus().get(oldStsPos);
                for (MvmtDateClass mvmtDateList : oldStausCls.getMvmtDate()) {
                    getOverdueAmt(mvmtDateList);
                }
            }

        } catch (NumberFormatException e) {
            e.getMessage();
        } catch (Exception e1) {
            if (dpd == null || dpd.isEmpty()) {
                dpd = "0";
            }
        }
    }

    public void getOverdueAmt(MvmtDateClass mvmtDateList) {
        if (mvmtDateList.getMvmtDate().getValue().equals(closureDate)) {
            String overdue = mvmtDateList.getMvmtCredit().getValue();
            if (overdue != null && !overdue.isEmpty()) {
                overdueAmt = String.format("%.2f", Double.parseDouble(overdue));
            } else {
                overdueAmt = "0.00";
            }
        }
    }

    private void writeToFile(List<String> data, String filePath) {
        try {
            File file = new File(filePath);
            file.getParentFile().mkdirs();
            boolean fileExists = file.exists();

            try (FileWriter writer = new FileWriter(file, true)) {
                if (!fileExists) {
                    String header = String.join(",", "BranchName", "BranchCode", "Center", "CustomerNumber",
                            "CustomerName", "AccountNumber", "LegacyLoanNumber", "ProductName", "LoanAmount",
                            "CycleNumber", "DisbursementDate", "MaturityDate", "ClosedDate", "ClosureReason",
                            "ClosureType", "InterestCollected", "TotalInterestCollected", "ClosingPrincipal",
                            "PenaltyCollected", "Funder", "FundingSource", "User", "EmployeeName", "EmployeeNumber",
                            "Remarks", "DPD", "AmountOverdue");

                    writer.write(header + System.lineSeparator());
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
