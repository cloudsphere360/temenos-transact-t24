package com.temenos.fusion;

import java.io.File;
import java.io.FileWriter;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.temenos.api.TField;
import com.temenos.t24.api.arrangement.accounting.Contract;
import com.temenos.t24.api.complex.aa.contractapi.BalanceMovement;
import com.temenos.t24.api.complex.eb.enquiryhook.EnquiryContext;
import com.temenos.t24.api.complex.eb.enquiryhook.FilterCriteria;
import com.temenos.t24.api.hook.system.Enquiry;
import com.temenos.t24.api.records.aaaccountdetails.AaAccountDetailsRecord;
import com.temenos.t24.api.records.aaaccountdetails.BillIdClass;
import com.temenos.t24.api.records.aaaccountdetails.BillPayDateClass;
import com.temenos.t24.api.records.aaactivityhistory.AaActivityHistoryRecord;
import com.temenos.t24.api.records.aaactivityhistory.ActivityRefClass;
import com.temenos.t24.api.records.aaactivityhistory.EffectiveDateClass;
import com.temenos.t24.api.records.aaarrangement.AaArrangementRecord;
import com.temenos.t24.api.records.aabilldetails.AaBillDetailsRecord;
import com.temenos.t24.api.records.aacustomerarrangement.AaCustomerArrangementRecord;
import com.temenos.t24.api.records.aacustomerarrangement.ArrangementClass;
import com.temenos.t24.api.records.aacustomerarrangement.ProductLineClass;
import com.temenos.t24.api.records.aaprddesaccount.AaPrdDesAccountRecord;
import com.temenos.t24.api.records.company.CompanyRecord;
import com.temenos.t24.api.records.companyconsol.CompanyConsolRecord;
import com.temenos.t24.api.records.ebffcentredetail.EbFfCentreDetailRecord;
import com.temenos.t24.api.records.ebffloanpaymenthis.DemandDateClass;
import com.temenos.t24.api.records.ebffloanpaymenthis.EbFfLoanPaymentHisRecord;
import com.temenos.t24.api.records.ebffparameter.EbFfParameterRecord;
import com.temenos.t24.api.records.ebffparameter.ParamDescClass;
import com.temenos.t24.api.records.ebffvillage.EbFfVillageRecord;
import com.temenos.t24.api.system.DataAccess;
import com.temenos.t24.api.system.Session;

public class FfNofileGlanceRptSummary extends Enquiry {
    public static final String FILE_NAME = "GlanceRep_Sum";
    public static final String TRADE = "TRADE";
    public static final String AA_ARRANGEMENT = "AA.ARRANGEMENT";
    public static final String PENDING_CLOSURE = "PENDING.CLOSURE";
    public static final String CLOSE = "CLOSE";

    public static final String LENDING_APPLYPAYMENT_WRITEOFF_SETTLEMENT = "LENDING-APPLYPAYMENT-WRITEOFF.SETTLEMENT";
    public static final String LENDING_APPLYPAYMENT_INSURANCE_SETTLEMENT = "LENDING-APPLYPAYMENT-INSURANCE.SETTLEMENT";
    public static final String LENDING_APPLYPAYMENT_PR_OUTSTANDING_PAYOFF = "LENDING-APPLYPAYMENT-PR.OUTSTANDING.PAYOFF";
    public static final String LENDING_APPLYPAYMENT_PR_CURR_BALANCE = "LENDING-APPLYPAYMENT-PR.CURR.BALANCE";
    public static final String LENDING_APPLYPAYMENT_PR_INSURANCE_BALANCES = "LENDING-APPLYPAYMENT-PR.INSURANCE.BALANCES";
    public static final String LENDING_WRITE_OFF_BAL_MAINTAIN = "LENDING-WRITE.OFF-BAL.MAINTAIN";
    public static final String LENDING_SETTLE_FORECLOSURE = "LENDING-SETTLE-FORECLOSURE";
    public static final String LENDING_APPLYPAYMENT_PR_COLLECTION = "LENDING-APPLYPAYMENT-PR.COLLECTION";
    public static final String LENDING_MATURE_ARRANGEMENT = "LENDING-MATURE-ARRANGEMENT";

    List<String> retvalues = new ArrayList<>();
    List<String> outvalues = new ArrayList<>();
    List<String> currentArrList = new ArrayList<>();
    List<String> allArrList = new ArrayList<>();
    List<String> customerList = new ArrayList<>();
    DataAccess da = new DataAccess(this);

    Set<String> totActiveAcc = new HashSet<>();
    Set<String> customerSet = new HashSet<>();
    Set<String> centreSet = new HashSet<>();
    Set<String> roSet = new HashSet<>();
    Set<String> villageSet = new HashSet<>();
    Set<String> totVillageSet = new HashSet<>();
    Set<String> groupSet = new HashSet<>();
    Set<String> actLnCustomerSet = new HashSet<>();
    Set<String> loanSetCycle1 = new HashSet<>();
    Set<String> loanSetCycle2 = new HashSet<>();
    Set<String> loanSetCycle3 = new HashSet<>();
    Set<String> loanSetCycle4 = new HashSet<>();
    Set<String> loanSetCycle5 = new HashSet<>();
    Set<String> loanSetCycleAbove5 = new HashSet<>();
    Set<String> dropoutCustSet = new HashSet<>();

    String selUser = "";
    String startDate = "";
    String endDate = "";
    String selDate = "";
    String selDateOp = "";
    String selBranch = "";
    String asOnDate = "";
    String branchName = "";
    String activeGroup = "";
    String totalAccountActive = "";
    String customer = "";
    String activeCustomer = "";
    String dormantClient = "";
    String dropOutCustomer = "";
    String activeCenter = "";
    String activeBlock = "";
    String activeVillage = "";
    String totalVillage = "";
    String fieldStaffHandleClientAvg = "";
    String activeFieldStaff = "";
    String disbursedLoanMonth = "";
    String disbursedAmountMonth = "";
    String disbursedLoanFy = "";
    String disbursedAmountFy = "";
    String principalOutstanding = "";
    String interestOutstanding = "";
    String cycle1 = "";
    String cycle2 = "";
    String cycle3 = "";
    String cycle4 = "";
    String cycle5 = "";
    String cycle5Above = "";
    String par30Loan = "";
    String par30Outstanding = "";
    String avgOutstandingPerLoan = "";
    String avgOutstandingPerFieldStaff = "";
    String ats = "";

    boolean writeOffTriggered = false;
    boolean writeOffSettTriggered = false;
    boolean insSettTriggered = false;
    boolean settleClosureTriggered = false;
    boolean repaymentTriggered = false;
    boolean maturityTriggered = false;

    boolean activityFound = false;

    String writeOffClosureDt = "";
    String writeOffSettClosureDt = "";
    String insSettClosureDt = "";
    String settleClosureDt = "";
    String repaymentClosureDt = "";
    String maturityClosureDt = "";

    String closedDate = "";

    String todayDate = "";
    String finMnemonic = "";
    String mnemonic = "";
    String companyIds = "";
    String arrStDt = "";
    double sumOfPrincipalOutstanding = 0.0;
    double sumOfInterestOutstanding = 0.0;
    double sumOfParPerc30OstBal = 0.0;
    double disbAmtForMonth = 0.0;
    double disbAmtForYear = 0.0;
    int disbLoanCountForMonth = 0;
    int disbLoanCountForFy = 0;
    int parPerc30 = 0;
    boolean disbInCurrMonth = false;
    boolean disbInCurrFY = false;
    boolean isMigratedContract = false;
    boolean isParPerc30 = false;

    int todayYear = 0;
    int todayMonth = 0;

    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
    DateTimeFormatter outDateFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
    DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HHmmss");

    @Override
    public List<String> setIds(List<FilterCriteria> filterCriteria, EnquiryContext enquiryContext) {
        processFilterCriteria(filterCriteria);
        try {
            Session session = new Session(this);
            todayDate = session.getCurrentVariable("!TODAY");
            todayYear = Integer.parseInt(todayDate.substring(0, 4));
            todayMonth = Integer.parseInt(todayDate.substring(4, 6));

            asOnDate = todayDate;
            Contract contract = new Contract(this);

            currentArrList = da.selectRecords(finMnemonic, AA_ARRANGEMENT, "", "WITH CO.CODE EQ " + companyIds);
            processFinalArrList(contract, currentArrList);

            customer = String.valueOf(customerSet.size());
            activeGroup = String.valueOf(groupSet.size());
            totalAccountActive = String.valueOf(totActiveAcc.size());
            activeCustomer = String.valueOf(actLnCustomerSet.size());
            activeCenter = String.valueOf(centreSet.size());
            activeVillage = String.valueOf(villageSet.size());
            totalVillage = String.valueOf(totVillageSet.size());
            activeFieldStaff = String.valueOf(roSet.size());
            disbursedLoanMonth = String.valueOf(disbLoanCountForMonth);
            disbursedAmountMonth = String.format("%.2f", disbAmtForMonth);
            disbursedLoanFy = String.valueOf(disbLoanCountForFy);
            disbursedAmountFy = String.format("%.2f", disbAmtForYear);
            principalOutstanding = String.format("%.2f", sumOfPrincipalOutstanding);
            interestOutstanding = String.format("%.2f", sumOfInterestOutstanding);
            cycle1 = String.valueOf(loanSetCycle1.size());
            cycle2 = String.valueOf(loanSetCycle2.size());
            cycle3 = String.valueOf(loanSetCycle3.size());
            cycle4 = String.valueOf(loanSetCycle4.size());
            cycle5 = String.valueOf(loanSetCycle5.size());
            cycle5Above = String.valueOf(loanSetCycleAbove5.size());
            par30Loan = String.valueOf(parPerc30);
            par30Outstanding = String.format("%.2f", sumOfParPerc30OstBal);
            dropOutCustomer = String.valueOf(dropoutCustSet.size());

            dormantClient = String.valueOf(customerSet.size() - actLnCustomerSet.size());

            double activeCustomerVal = actLnCustomerSet.size();
            int activeFieldStaffIntVal = roSet.size();
            fieldStaffHandleClientAvg = String.format("%.2f",
                    processAvgCalculation(activeCustomerVal, activeFieldStaffIntVal));

            double totOutstandingAmt = sumOfPrincipalOutstanding + sumOfInterestOutstanding;
            avgOutstandingPerLoan = String.format("%.2f",
                    processAvgCalculation(totOutstandingAmt, totActiveAcc.size()));

            avgOutstandingPerFieldStaff = String.format("%.2f",
                    processAvgCalculation(totOutstandingAmt, activeFieldStaffIntVal));

            ats = String.format("%.2f", processAvgCalculation(disbAmtForMonth, disbLoanCountForMonth));

            List<String> row = new ArrayList<>();
            row.add(convertDate(asOnDate));
            row.add(branchName);
            row.add(activeGroup);
            row.add(totalAccountActive);
            row.add(customer);
            row.add(activeCustomer);
            row.add(dormantClient);
            row.add(dropOutCustomer);
            row.add(activeCenter);
            row.add(activeBlock);
            row.add(activeVillage);
            row.add(totalVillage);
            row.add(fieldStaffHandleClientAvg);
            row.add(activeFieldStaff);
            row.add(disbursedLoanMonth);
            row.add(disbursedAmountMonth);
            row.add(disbursedLoanFy);
            row.add(disbursedAmountFy);
            row.add(principalOutstanding);
            row.add(interestOutstanding);
            row.add(cycle1);
            row.add(cycle2);
            row.add(cycle3);
            row.add(cycle4);
            row.add(cycle5);
            row.add(cycle5Above);
            row.add(par30Loan);
            row.add(par30Outstanding);
            row.add(avgOutstandingPerLoan);
            row.add(avgOutstandingPerFieldStaff);
            row.add(ats);
            retvalues.add(String.join("*", row));
            if (!row.isEmpty()) {
                String filePath = "";
                String paramId = "FF.BM.REPORT.EXTRACT";
                EbFfParameterRecord paramRec = new EbFfParameterRecord(da.getRecord("EB.FF.PARAMETER", paramId));
                for (ParamDescClass paramDesc : paramRec.getParamDesc()) {
                    if (paramDesc.getParamName().getValue().equals("Path")) {
                        filePath = paramDesc.getParamValue().getValue();
                    }
                }

                LocalDateTime currDtTime = LocalDateTime.now();
                String currDate = currDtTime.format(outDateFormatter);
                String currTime = currDtTime.format(timeFormatter);
                String outputPath = filePath + FILE_NAME + "_" + "DATE-WISE" + "_" + branchName + "_" + selUser + "_"
                        + currDate + "_" + currTime + ".csv";

                writeToFile(row, outputPath);
            }

        } catch (Exception e) {
            e.getMessage();
        }
        return retvalues;

    }
    
    public String convertDate(String inDate) {
        String outDate = "";
        try {
            LocalDate date = LocalDate.parse(inDate, formatter);
            outDate = date.format(outDateFormatter);
            return outDate;
        } catch (Exception e) {
            return inDate;
        }
    }

    public void processFilterCriteria(List<FilterCriteria> filterCriteria) {
        for (FilterCriteria filter : filterCriteria) {
            switch (filter.getFieldname()) {
            case "DATE.FROM":
                startDate = filter.getValue();
                break;
            case "DATE.TO":
                endDate = filter.getValue();
                break;
            case "USER":
                selUser = filter.getValue();
                break;
            case "BRANCH":
                selBranch = filter.getValue();
                initialiseCompanyInfo(selBranch);
                getLinkedCompIds(selBranch);
                break;
            default:
            }
        }
    }

    public void initialiseCompanyInfo(String companyId) {
        try {
            CompanyRecord companyObj = new CompanyRecord(da.getRecord("COMPANY", companyId));
            finMnemonic = companyObj.getFinancialMne().getValue();
            mnemonic = companyObj.getCustomerMnemonic().getValue();
            branchName = companyObj.getCompanyName().get(0).getValue();
            String[] brnNamePart = branchName.split("-");
            branchName = brnNamePart[0];
        } catch (Exception e) {
            e.getMessage();
        }

    }

    public void getLinkedCompIds(String selBranch) {
        StringBuilder company = new StringBuilder();
        try {
            List<String> comConsolRecList = da.selectRecords("", "COMPANY.CONSOL", "",
                    "WITH COM.CONSOL.TO EQ " + selBranch);
            if (!comConsolRecList.isEmpty()) {
                company.append(selBranch).append(" ");
                for (String comConsol : comConsolRecList) {
                    CompanyConsolRecord comConsolRec = new CompanyConsolRecord(
                            da.getRecord("COMPANY.CONSOL", comConsol));
                    List<TField> comConsolFromList = comConsolRec.getComConsolFrom();
                    if (comConsolFromList != null && !comConsolFromList.isEmpty()) {
                        for (TField comConsolFrom : comConsolFromList) {
                            company.append(comConsolFrom.getValue()).append(" ");
                        }
                    }
                }
                companyIds = company.toString().trim();
            } else {
                companyIds = selBranch;
            }
        } catch (Exception e) {
            e.getMessage();
        }
    }

    public void processFinalArrList(Contract contract, List<String> currentArrList) {
        try {
            for (String contractId : currentArrList) {
                isMigratedContract = false;
                isParPerc30 = false;
                closedDate = "";

                AaArrangementRecord aaArrRec = new AaArrangementRecord(
                        da.getRecord(finMnemonic, AA_ARRANGEMENT, "", contractId));
                String orgContDate = aaArrRec.getOrigContractDate().getValue();
                closedDate = aaArrRec.getClosedDate().getValue();
                if (checkValue(orgContDate)) {
                    arrStDt = orgContDate;
                    isMigratedContract = true;
                } else {
                    arrStDt = aaArrRec.getStartDate().getValue();
                }
                String arrStatus = aaArrRec.getArrStatus().getValue();
                String customerId = aaArrRec.getCustomer().get(0).getCustomer().getValue();
                customerSet.add(customerId);
                contract.setContractId(contractId);

                getAaArrAccountDetails(contract, contractId, arrStatus);
                processBasedOnArrStatus(contract, contractId, customerId, arrStatus);
            }
        } catch (Exception e) {
            e.getMessage();
        }
    }

    public void processBasedOnArrStatus(Contract contract, String contractId, String customerId, String arrStatus) {
        try {
            boolean isPendingOrClosed = arrStatus.equals(PENDING_CLOSURE) || arrStatus.equals(CLOSE);
            boolean dropCustCheckDone = dropoutCustSet.contains(customerId);
            if (!isPendingOrClosed) {
                actLnCustomerSet.add(customerId);
                totActiveAcc.add(contractId);
                processForCurrentArrangements(contract, contractId);
            } else if ((closedDate == null || closedDate.isEmpty()) && !dropCustCheckDone) {
                getAaActivityHistoryDets(contractId);
            }
            
            if (checkValue(closedDate) && !dropCustCheckDone) {
                boolean isCustomerRetained = getCustomersNewLoanCheck(customerId);
                if (!isCustomerRetained) {
                    dropoutCustSet.add(customerId);
                }
            }
        } catch (Exception e) {
            e.getMessage();
        }
    }

    public void processForCurrentArrangements(Contract contract, String contractId) {
        double disbAmount = 0.0;
        try {
            getEcbDetails(contract);

            if (isMigratedContract) {
                disbAmount = getEbFfLoanPaymentHisDets(contractId);
            } else {
                disbAmount = getAaAccountDetails(contractId);
            }

            if (disbInCurrMonth) {
                disbAmtForMonth += disbAmount;
                disbLoanCountForMonth++;
            }
            if (disbInCurrFY) {
                disbAmtForYear += disbAmount;
                disbLoanCountForFy++;
            }
        } catch (Exception e) {
            e.getMessage();
        }

    }

    public double processAvgCalculation(double value1, int value2) {
        if (value2 != 0) {
            return value1 / value2;
        }
        return 0.0;
    }

    public double getAaAccountDetails(String contractId) {
        String billIdRef = "";
        disbInCurrMonth = false;
        disbInCurrFY = false;
        try {
            AaAccountDetailsRecord aaAccDets = new AaAccountDetailsRecord(
                    da.getRecord(finMnemonic, "AA.ACCOUNT.DETAILS", "", contractId));
            for (BillPayDateClass billPayDt : aaAccDets.getBillPayDate()) {
                for (BillIdClass billId : billPayDt.getBillId()) {
                    if (billId.getBillType().getValue().equals("DISBURSEMENT")
                            && billId.getBillStatus().getValue().equals("SETTLED")) {
                        processDisbAmountCheck(arrStDt);
                        billIdRef = billId.getBillId().getValue();
                        AaBillDetailsRecord aaBillDetsRec = new AaBillDetailsRecord(
                                da.getRecord(finMnemonic, "AA.BILL.DETAILS", "", billIdRef));
                        return Math.abs(Double.parseDouble(aaBillDetsRec.getPaymentType().get(0).getPaymentAmount().getValue()));
                    }
                }
            }
        } catch (NumberFormatException e) {
            e.getMessage();
        }
        return 0.0;
    }

    public double getEbFfLoanPaymentHisDets(String contractId) {
        disbInCurrMonth = false;
        disbInCurrFY = false;
        try {
            EbFfLoanPaymentHisRecord ffLoanPaymentHisRec = new EbFfLoanPaymentHisRecord(
                    da.getRecord("", "EB.FF.LOAN.PAYMENT.HIS", "", contractId));
            for (DemandDateClass demandDtCls : ffLoanPaymentHisRec.getDemandDate()) {
                if (demandDtCls.getTransType().getValue().equalsIgnoreCase("DISBURSEMENT")) {
                    processDisbAmountCheck(arrStDt);
                    return Math.abs(Double.parseDouble(demandDtCls.getPymtAmt().getValue()));
                }
            }
        } catch (NumberFormatException e) {
            e.getMessage();
        }
        return 0.0;
    }

    public void processDisbAmountCheck(String arrStDt) {
        try {
            int arrYear = Integer.parseInt(arrStDt.substring(0, 4));
            int arrMonth = Integer.parseInt(arrStDt.substring(4, 6));
            disbInCurrMonth = (arrYear == todayYear && arrMonth == todayMonth);

            int fyStartYear = (todayMonth >= 4) ? todayYear : todayYear - 1;
            int fyEndYear = fyStartYear + 1;
            disbInCurrFY = (arrYear == fyStartYear && arrMonth >= 4) || (arrYear == fyEndYear && arrMonth <= 3);
        } catch (NumberFormatException e) {
            e.getMessage();
        }
    }

    public void getAaArrAccountDetails(Contract contract, String aaId, String arrStatus) {
        try {
            AaPrdDesAccountRecord aaArrAccRec = new AaPrdDesAccountRecord(contract.getConditionForProperty("ACCOUNT"));
            String village = aaArrAccRec.getLocalRefField("FF.VILLAGE").getValue();
            if (checkValue(village)) {
                totVillageSet.add(village);
            }
            if (!arrStatus.equals(PENDING_CLOSURE) && !arrStatus.equals(CLOSE)) {
                if (checkValue(village)) {
                    isParPerc30 = getEbFfVillageDets(village);
                    villageSet.add(village);
                }
                String group = aaArrAccRec.getLocalRefField("FF.GROUP").getValue();
                if (checkValue(group)) {
                    groupSet.add(group);
                }
                String centre = aaArrAccRec.getLocalRefField("FF.CENTRE").getValue();
                if (checkValue(centre)) {
                    centreSet.add(centre);
                    getEbFfCentreDetails(centre);
                }
                String cycle = aaArrAccRec.getLocalRefField("FF.LOAN.CYCLE").getValue();
                if (checkValue(cycle)) {
                    getCycleCountForLoans(cycle, aaId);
                }
            }

        } catch (Exception e) {
            e.getMessage();
        }
    }

    public boolean checkValue(String value) {
        return value != null && !value.isEmpty();
    }

    public void getEbFfCentreDetails(String centre) {
        try {
            EbFfCentreDetailRecord centreRec = new EbFfCentreDetailRecord(
                    da.getRecord("", "EB.FF.CENTRE.DETAIL", "", centre));
            String ro = centreRec.getCurrentRo().getValue();
            if (ro != null && !ro.isEmpty()) {
                roSet.add(ro);
            }
        } catch (Exception e) {
            e.getMessage();
        }
    }

    public boolean getEbFfVillageDets(String village) {
        try {
            EbFfVillageRecord ffVillageRec = new EbFfVillageRecord(da.getRecord("", "EB.FF.VILLAGE", "", village));
            String parPerc = ffVillageRec.getParPercentage().getValue();
            if (checkValue(parPerc)) {
                double parPercVal = Double.parseDouble(parPerc);
                if (parPercVal >= 30) {
                    return true;
                }
            }
        } catch (NumberFormatException e) {
            e.getMessage();
        }
        return false;
    }

    public void getCycleCountForLoans(String cycle, String aaId) {
        try {
            switch (cycle) {
            case "1":
                loanSetCycle1.add(aaId);
                break;
            case "2":
                loanSetCycle2.add(aaId);
                break;
            case "3":
                loanSetCycle3.add(aaId);
                break;
            case "4":
                loanSetCycle4.add(aaId);
                break;
            case "5":
                loanSetCycle5.add(aaId);
                break;
            default:
                if (Integer.parseInt(cycle) > 5) {
                    loanSetCycleAbove5.add(aaId);
                }
            }
        } catch (NumberFormatException e) {
            e.getMessage();
        }
    }

    public void getEcbDetails(Contract contract) {
        try {
            if (isParPerc30) {
                parPerc30++;
                double parPerc30OstBal = Double.parseDouble(getBalance(contract, "FFALLOSTBAL", TRADE));
                sumOfParPerc30OstBal += Math.abs(parPerc30OstBal);
            }
            double principalAmt = Double.parseDouble(getBalance(contract, "FFPRINODFUTAMT", TRADE));
            double interestAmt = Double.parseDouble(getBalance(contract, "FFINTODFUTAMT", TRADE));

            sumOfPrincipalOutstanding += Math.abs(principalAmt);
            sumOfInterestOutstanding += Math.abs(interestAmt);
        } catch (NumberFormatException e) {
            e.getMessage();
        }
    }

    public String getBalance(Contract contract, String accountType, String bookingType) {
        List<BalanceMovement> movements = contract.getContractBalanceMovements(accountType, bookingType);
        return (movements != null && !movements.isEmpty()) ? movements.get(0).getBalance().toString() : "0";
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
            if (writeOffTriggered) {
                closedDate = writeOffClosureDt;
            } else if (writeOffSettTriggered) {
                closedDate = writeOffSettClosureDt;
            } else if (insSettTriggered) {
                closedDate = insSettClosureDt;
            } else if (settleClosureTriggered) {
                closedDate = settleClosureDt;
            } else if ((maturityTriggered && repaymentTriggered) && (maturityClosureDt.equals(repaymentClosureDt))) {
                closedDate = repaymentClosureDt;
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

        String effectiveDt = effectiveDate.getEffectiveDate().getValue();

        switch (activity) {
        case LENDING_WRITE_OFF_BAL_MAINTAIN:
            writeOffTriggered = true;
            writeOffClosureDt = effectiveDt;
            activityFound = true;
            break;

        case LENDING_APPLYPAYMENT_WRITEOFF_SETTLEMENT:
            writeOffSettTriggered = true;
            writeOffSettClosureDt = effectiveDt;
            activityFound = true;
            break;

        case LENDING_APPLYPAYMENT_INSURANCE_SETTLEMENT:
            insSettTriggered = true;
            insSettClosureDt = effectiveDt;
            activityFound = true;
            break;

        case LENDING_SETTLE_FORECLOSURE:
            settleClosureTriggered = true;
            settleClosureDt = effectiveDt;
            activityFound = true;
            break;

        case LENDING_APPLYPAYMENT_PR_COLLECTION:
            repaymentTriggered = true;
            repaymentClosureDt = effectiveDt;
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

    public boolean getCustomersNewLoanCheck(String cusId) {
        try {
            AaCustomerArrangementRecord aaCurArrRec = new AaCustomerArrangementRecord(
                    da.getRecord(finMnemonic, "AA.CUSTOMER.ARRANGEMENT", "", cusId));
            for (ProductLineClass prdLineList : aaCurArrRec.getProductLine()) {
                if ("LENDING".equalsIgnoreCase(prdLineList.getProductLine().getValue())) {
                    for (ArrangementClass arrIdList : prdLineList.getArrangement()) {
                        String currArrId = arrIdList.getArrangement().getValue();
                        AaArrangementRecord arrangementRec = new AaArrangementRecord(
                                da.getRecord(finMnemonic, AA_ARRANGEMENT, "", currArrId));
                        LocalDate clsDt = LocalDate.parse(closedDate, formatter);
                        LocalDate stDt = LocalDate.parse(arrangementRec.getStartDate().getValue(), formatter);
                        if (stDt.equals(clsDt) || stDt.isAfter(clsDt)) {
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

    public void writeToFile(List<String> data, String filePath) {
        try {
            File file = new File(filePath);
            file.getParentFile().mkdirs();

            String[] header = { "AsonDate", "SummaryBranchName", "ActiveGroup", "TotalAccountActive", "Customer",
                    "AtiveCustomer", "DormantClient", "DropoutCustomer", "ActiveCenter", "ActiveBlock", "ActiveVillage",
                    "TotalVillage", "FieldStaffHandleClient(average)", "ActiveFieldStaff", "DisbursedLoanForTheMonth",
                    "DisbursedAmountForTheMonth", "DisbursedLoanFY", "DisbursedAmountFY", "PrincipalOutstanding",
                    "InterestOutstanding", "Cycle1", "Cycle2", "Cycle3", "Cycle4", "Cycle5", "Cycle5Above",
                    "Par30%Loan", "Par30%Outstanding", "AvgOutstandingPerLoan", "AvgOutstandingPerFieldStaff", "ATS" };

            try (FileWriter writer = new FileWriter(file)) {
                for (int i = 0; i < header.length; i++) {
                    String value = (i < data.size()) ? data.get(i) : "";
                    writer.write(header[i] + "," + value + System.lineSeparator());
                }
            }
        } catch (Exception e) {
            e.getMessage();
        }
    }

}
