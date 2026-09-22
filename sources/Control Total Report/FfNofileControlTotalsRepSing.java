package com.temenos.fusion;

import java.io.File;
import java.io.FileWriter;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.temenos.t24.api.arrangement.accounting.Contract;
import com.temenos.t24.api.complex.aa.contractapi.BalanceMovement;
import com.temenos.t24.api.complex.eb.enquiryhook.EnquiryContext;
import com.temenos.t24.api.complex.eb.enquiryhook.FilterCriteria;
import com.temenos.t24.api.hook.system.Enquiry;
import com.temenos.t24.api.records.aaactivityhistory.AaActivityHistoryRecord;
import com.temenos.t24.api.records.aaactivityhistory.ActivityRefClass;
import com.temenos.t24.api.records.aaactivityhistory.EffectiveDateClass;
import com.temenos.t24.api.records.aaarrangement.AaArrangementRecord;
import com.temenos.t24.api.records.aaprddesaccount.AaPrdDesAccountRecord;
import com.temenos.t24.api.records.company.CompanyRecord;
import com.temenos.t24.api.records.ebffcentredetail.EbFfCentreDetailRecord;
import com.temenos.t24.api.records.ebffloandpd.DateClass;
import com.temenos.t24.api.records.ebffloandpd.EbFfLoanDpdRecord;
import com.temenos.t24.api.system.DataAccess;
import com.temenos.t24.api.system.Session;

/**
 *
 * @author GokarajuHemalatha
 *
 */

public class FfNofileControlTotalsRepSing extends Enquiry {

    List<String> retvalues = new ArrayList<>();
    List<String> finalArrList = new ArrayList<>();
    List<String> data = new ArrayList<>();
   List<String> testp = new ArrayList<>();
    List<String> currentArrList = new ArrayList<>();
    List<String> writeArrList = new ArrayList<>();
    List<String> extraList = new ArrayList<>();
    DataAccess da = new DataAccess(this);

    Set<String> centreSet = new HashSet<>();
    Set<String> roSet = new HashSet<>();
    Set<String> bmSet = new HashSet<>();
    Set<String> villageSet = new HashSet<>();

    List<String> noOfadvcoll = new ArrayList<>();
    List<String> nodpdcnt = new ArrayList<>();
    List<String> dpdcnt0to30 = new ArrayList<>();
    List<String> dpdcnt31to60 = new ArrayList<>();
    List<String> dpdcnt61to90 = new ArrayList<>();
    List<String> dpdcnt91nAbove = new ArrayList<>();
    List<String> nofWriteOffLns = new ArrayList<>();

    Set<String> actLnCustomerSet = new HashSet<>();
    Set<String> loanSetCycle1 = new HashSet<>();
    Set<String> loanSetCycle2 = new HashSet<>();
    Set<String> loanSetCycle3 = new HashSet<>();
    Set<String> loanSetCycle4 = new HashSet<>();
    Set<String> loanSetCycleAbove5 = new HashSet<>();

    String startDate = "";
    String endDate = "";
    String selBranch = "";
    String selBranchOp = "";
    String branch = "";
    String branchName = "";
    String todayDate = "";
    String finMnemonic = "";
    String mnemonic = "";
    String filePath = "/shares/tafjud/Fusion/Reports/";
    String outputPath = "";
    double principalAmt = 0.0;

    double loanOverdueCnt0to30 = 0.0;
    double loanOverdueCnt31to60 = 0.0;
    double loanOverdueCnt61to90 = 0.0;
    double loanOverdueCnt91nAbove = 0.0;
    double prplAmtOvdueCnt0to30 = 0.0;
    double prplAmtOvdueCnt31to60 = 0.0;
    double prplAmtOverdue61to90 = 0.0;
    double prplAmtOverdue91nAbove = 0.0;
    double pOS0to30DPDscnt = 0.0;
    double pOS31to60DPDscnt = 0.0;
    double pOS61to90DPDscnt = 0.0;
    double pOS91nAbove = 0.0;
    double bpI = 0.0;
    double noofLoansAdvanceCollected = 0.0;
    double advCoLectAMOUNT = 0.0;
    double totwriteoffAmt = 0.0;
    String curDpd = "";
    String arrAgeStatus = "";
    double overdueAmt = 0;
    double pripldueAmt = 0;
    double outstndgBal = 0;
    String trade = "TRADE";

    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");

    @Override
    public List<String> setIds(List<FilterCriteria> filterCriteria, EnquiryContext enquiryContext) {

        Session session = new Session(this);
        todayDate = session.getCurrentVariable("!TODAY");
        Contract contract = new Contract(this);

        for (FilterCriteria filter : filterCriteria) {
            if (filter.getFieldname().equals("BRANCH")) {
                selBranch = filter.getValue();
                selBranchOp = filter.getOperand();
            }
        }

        if (!selBranch.equals("")) {
            extraList.add(selBranch);
        
        }
     
        try {

            for (String companyIds : extraList) {
                branch = companyIds;
                branchName = "";
                initialiseCompanyInfo(companyIds);

                centreSet.clear();
                roSet.clear();
                bmSet.clear();
                villageSet.clear();
                actLnCustomerSet.clear();

                currentArrList = da.selectRecords(finMnemonic, "AA.ARRANGEMENT", "",
                        "WITH ARR.STATUS EQ CURRENT OR ARR.STATUS EQ EXPIRED OR ARR.STATUS EQ AUTH AND CO.CODE EQ "
                                + companyIds);

                for (String contractId : currentArrList) {

                    AaArrangementRecord aaArrRec = new AaArrangementRecord(da.getRecord("AA.ARRANGEMENT", contractId));

                    String stat = aaArrRec.getArrStatus().getValue();
                    if (stat.equals("CURRENT") || stat.equals("EXPIRED")) {
                        finalArrList.add(contractId);
                    }
                    if (stat.equals("AUTH")) {

                        getAaActivityHistoryDets(contract, contractId);

                    }

                }

                for (String currAaId : finalArrList) {
                 

                    contract.setContractId(currAaId);
                    getEcbBalanceprinical(contract);

                    getEcbBalanceBasedOnDPDcnt(contract, currAaId);
                   
                    getArrangementDetails(contract);

                    getAaArrAccountDetails(contract, currAaId);

                }

                List<String> row = new ArrayList<>();

                row.add(branch);
                row.add(branchName);
                row.add(String.valueOf(bmSet.size()));
                row.add(String.valueOf(roSet.size()));
                row.add(String.valueOf(villageSet.size()));
                row.add(String.valueOf(centreSet.size()));
                row.add(String.valueOf(actLnCustomerSet.size()));
                row.add(String.valueOf(finalArrList.size()));
                row.add(String.format("%.2f", principalAmt));
                row.add(String.valueOf(loanSetCycle1.size()));
                row.add(String.valueOf(loanSetCycle2.size()));
                row.add(String.valueOf(loanSetCycle3.size()));
                row.add(String.valueOf(loanSetCycle4.size()));
                row.add(String.valueOf(loanSetCycleAbove5.size()));
                row.add(String.valueOf(nodpdcnt.size()));
                row.add(String.valueOf(dpdcnt0to30.size()));
                row.add(String.format("%.2f", prplAmtOvdueCnt0to30));
                row.add(String.format("%.2f", loanOverdueCnt0to30));
                row.add(String.format("%.2f", pOS0to30DPDscnt));
                row.add(String.valueOf(dpdcnt31to60.size()));
                row.add(String.format("%.2f", prplAmtOvdueCnt31to60));
                row.add(String.format("%.2f", loanOverdueCnt31to60));
                row.add(String.format("%.2f", pOS31to60DPDscnt));
                row.add(String.valueOf(dpdcnt61to90.size()));
                row.add(String.format("%.2f", prplAmtOverdue61to90));
                row.add(String.format("%.2f", loanOverdueCnt61to90));
                row.add(String.format("%.2f", pOS61to90DPDscnt));
                row.add(String.valueOf(dpdcnt91nAbove.size()));
                row.add(String.format("%.2f", prplAmtOverdue91nAbove));
                row.add(String.format("%.2f", loanOverdueCnt91nAbove));
                row.add(String.format("%.2f", pOS91nAbove));
                row.add(String.valueOf(nofWriteOffLns.size()));
                row.add(String.format("%.2f", totwriteoffAmt));
                row.add(String.format("%.2f", bpI));
                row.add(String.valueOf(noOfadvcoll.size()));
                row.add(String.format("%.2f", advCoLectAMOUNT));

                retvalues.add(String.join("*", row));

                data.add(row.toString());
                row.clear();

                finalArrList.clear();
                noOfadvcoll.clear();
                nodpdcnt.clear();
                loanSetCycle1.clear();
                loanSetCycle2.clear();
                loanSetCycle3.clear();
                loanSetCycle4.clear();
                loanSetCycleAbove5.clear();
                dpdcnt0to30.clear();
                dpdcnt31to60.clear();
                dpdcnt61to90.clear();
                dpdcnt91nAbove.clear();
                nofWriteOffLns.clear();

                loanOverdueCnt0to30 = 0.0;
                loanOverdueCnt31to60 = 0.0;
                loanOverdueCnt61to90 = 0.0;
                loanOverdueCnt91nAbove = 0.0;
                prplAmtOvdueCnt0to30 = 0.0;
                prplAmtOvdueCnt31to60 = 0.0;
                prplAmtOverdue61to90 = 0.0;
                prplAmtOverdue91nAbove = 0.0;
                pOS0to30DPDscnt = 0.0;
                pOS31to60DPDscnt = 0.0;
                pOS61to90DPDscnt = 0.0;
                pOS91nAbove = 0.0;
                advCoLectAMOUNT = 0.0;
                totwriteoffAmt = 0.0;
                principalAmt = 0.0;
                bpI = 0.0;

                outputPath = filePath + "ControtTotalssample" + ".csv";
                writeToFile(data, outputPath);
                data.clear();
            }

        } catch (Exception e) {
            e.getMessage();
        }

        return retvalues;
    }

    public void initialiseCompanyInfo(String companyIds) {

        try {
            CompanyRecord companyObj = new CompanyRecord(da.getRecord("COMPANY", companyIds));
            branch = companyIds;
            finMnemonic = companyObj.getFinancialMne().getValue();
            mnemonic = companyObj.getCustomerMnemonic().getValue();
            branchName = companyObj.getCompanyName().get(0).getValue();

        } catch (Exception e1) {
            e1.getMessage();
        }

    }

    public void getArrangementDetails(Contract contract) {

        try {

            AaArrangementRecord arrRec = contract.getContract();

            String cust = arrRec.getCustomer().get(0).getCustomer().getValue();
        
            actLnCustomerSet.add(cust);

        } catch (Exception e2) {
            e2.getMessage();
        }
    }

    public void getAaArrAccountDetails(Contract contract, String aaId) {

        try {
            AaPrdDesAccountRecord aaArrAccRec = new AaPrdDesAccountRecord(contract.getConditionForProperty("ACCOUNT"));

            String centre = aaArrAccRec.getLocalRefField("FF.CENTRE").getValue();
            if (centre != null && !centre.isEmpty()) {               
                centreSet.add(centre);
                getEbFfCentreDetails(centre);
            }
            String village = aaArrAccRec.getLocalRefField("FF.VILLAGE").getValue();

            if (village != null && !village.isEmpty()) {             
                villageSet.add(village);
            }
            String cycle = aaArrAccRec.getLocalRefField("FF.LOAN.CYCLE").getValue();

            if (cycle != null && !cycle.isEmpty()) {           
                getCycleCountForLoans(cycle, aaId);
            }else {
                loanSetCycle1.add(aaId);
            }
        } catch (Exception e) {
            e.getMessage();
        }
    }

    public void getEbFfCentreDetails(String centre) {
        try {
            EbFfCentreDetailRecord centreRec = new EbFfCentreDetailRecord(
                    da.getRecord("", "EB.FF.CENTRE.DETAIL", "", centre));
            String ro = centreRec.getCurrentRo().getValue();
            if (ro != null && !ro.isEmpty()) {
                roSet.add(ro);
            }
            String bm = centreRec.getBranchManager().getValue();
            if (bm != null && !bm.isEmpty()) {
                bmSet.add(bm);
            }

        } catch (Exception e) {
            e.getMessage();
        }
    }

    public void getCycleCountForLoans(String cycle, String aaId) {
        switch (cycle) {
        case "0":
            loanSetCycle1.add(aaId);
            break;
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
        default:
            loanSetCycleAbove5.add(aaId);
        }
    }

    public void getAaActivityHistoryDets(Contract contract, String contractId) {

        contract.setContractId(contractId);
        try {
            AaActivityHistoryRecord aaActHisRec = new AaActivityHistoryRecord(
                    da.getRecord(finMnemonic, "AA.ACTIVITY.HISTORY", "", contractId));
            for (EffectiveDateClass effectiveDate : aaActHisRec.getEffectiveDate()) {
                for (ActivityRefClass activeRef : effectiveDate.getActivityRef()) {
                    String activity = activeRef.getActivity().getValue();
                    if (activity.equals("LENDING-CHARGEOFF-ARRANGEMENT")) {
                        nofWriteOffLns.add(activity);
                        String curaAccountCo = getBalance(contract, "CURACCOUNTCO", "BOOKING");
                        totwriteoffAmt += Double.parseDouble(curaAccountCo);

                    }
                }
            }
        } catch (NumberFormatException e) {
            e.getMessage();
        }

    }

    private void getEcbBalanceBasedOnDPDcnt(Contract contract, String selectionArrId) {

        String tableId = null;

        DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern("yyyyMMdd");
        LocalDate date = LocalDate.parse(todayDate, inputFormatter);
        String formatted = date.getMonth().toString().substring(0, 3) + date.getYear();
        tableId = selectionArrId + "-" + formatted;
        EbFfLoanDpdRecord loandpdRec = null;

        List<DateClass> dateList;
        try {
            loandpdRec = new EbFfLoanDpdRecord(da.getRecord(finMnemonic, "EB.FF.LOAN.DPD", "", tableId));
            dateList = loandpdRec.getDate();
            DateClass last = dateList.get(dateList.size() - 1);
            curDpd = last.getCurDpd().getValue();

        } catch (Exception e4) {
            e4.getMessage();
        }

        if ((Double.parseDouble(curDpd)) == 0) {
            nodpdcnt.add(curDpd);

        } else if ((Double.parseDouble(curDpd)) > 0 && (Double.parseDouble(curDpd)) <= 30) {

            dpdcnt0to30.add(curDpd);
            getEcbBalance(contract);
            loanOverdueCnt0to30 += overdueAmt;
            prplAmtOvdueCnt0to30 += pripldueAmt;
            pOS0to30DPDscnt += outstndgBal;

        } else if ((Double.parseDouble(curDpd)) > 30 && (Double.parseDouble(curDpd)) <= 60) {

            dpdcnt31to60.add(curDpd);
            getEcbBalance(contract);
            loanOverdueCnt31to60 += overdueAmt;
            prplAmtOvdueCnt31to60 += pripldueAmt;
            pOS31to60DPDscnt += outstndgBal;

        } else if ((Double.parseDouble(curDpd)) > 60 && (Double.parseDouble(curDpd)) <= 90) {

            dpdcnt61to90.add(curDpd);
            getEcbBalance(contract);
            loanOverdueCnt61to90 += overdueAmt;
            prplAmtOverdue61to90 += pripldueAmt;
            pOS61to90DPDscnt += outstndgBal;

        } else if ((Double.parseDouble(curDpd)) > 90) {

            dpdcnt91nAbove.add(curDpd);
            getEcbBalance(contract);
            loanOverdueCnt91nAbove += overdueAmt;
            prplAmtOverdue91nAbove += pripldueAmt;
            pOS91nAbove += outstndgBal;

        }

    }

    private void getEcbBalanceprinical(Contract contract) {

        double outstndgBa2 = 0.0;

        String npaAccount = getBalance(contract, "FFCURODAMT", trade);
            outstndgBa2 = Double.parseDouble(npaAccount);

        principalAmt += outstndgBa2;

        String uncAccount = getBalance(contract, "UNCACCOUNT", trade);
           if ((Double.parseDouble(uncAccount)) > 0) {
            noOfadvcoll.add(uncAccount);
        }
        advCoLectAMOUNT += Double.parseDouble(uncAccount);

        String accPrincipalInt = getBalance(contract, "ACCPRINTEREST", trade);
        bpI += Double.parseDouble(accPrincipalInt);

    }

    private void getEcbBalance(Contract contract) {

        overdueAmt = 0.0;
        pripldueAmt = 0.0;
        outstndgBal = 0.0;

        try {
            String currAccount = getBalance(contract, "FFCURODAMT", trade);
            String dueAccount = getBalance(contract, "FFPRINDEFAMT", trade);
            String duePrincipalInt = getBalance(contract, "FFINTDEFAMT", trade);

            if ((Double.parseDouble(curDpd)) > 0) {

                overdueAmt = Double.parseDouble(duePrincipalInt);

                pripldueAmt = Double.parseDouble(dueAccount);

                outstndgBal = Double.parseDouble(currAccount);

            }

        } catch (Exception e) {
            e.getMessage();
        }

    }

    public String getBalance(Contract contract, String accountType, String bookingType) {

        List<BalanceMovement> movements = contract.getContractBalanceMovements(accountType, bookingType);
        return (movements != null && !movements.isEmpty()) ? movements.get(0).getBalance().toString() : "0";
    }

    public void writeToFile(List<String> data, String filePath) {
        try {
            File file = new File(filePath);
            file.getParentFile().mkdirs();
            boolean fileExists = file.exists();

            try (FileWriter writer = new FileWriter(file, true)) {

                if (!fileExists) {
                    String header = String.join(",", "control total br", "test doc @@@@");
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
