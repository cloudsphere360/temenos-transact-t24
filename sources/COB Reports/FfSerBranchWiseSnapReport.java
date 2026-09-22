package com.temenos.fusion;

import java.io.File;
import java.io.FileWriter;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import com.temenos.t24.api.arrangement.Bill;
import com.temenos.t24.api.arrangement.accounting.Contract;
import com.temenos.t24.api.complex.aa.contractapi.BalanceMovement;
import com.temenos.t24.api.complex.eb.servicehook.ServiceData;
import com.temenos.t24.api.hook.system.ServiceLifecycle;
import com.temenos.t24.api.records.aaaccountdetails.AaAccountDetailsRecord;
import com.temenos.t24.api.records.aaaccountdetails.BillIdClass;
import com.temenos.t24.api.records.aaaccountdetails.BillPayDateClass;
import com.temenos.t24.api.records.aaactivityhistory.AaActivityHistoryRecord;
import com.temenos.t24.api.records.aaactivityhistory.ActivityRefClass;
import com.temenos.t24.api.records.aaactivityhistory.EffectiveDateClass;
import com.temenos.t24.api.records.aaarrangement.AaArrangementRecord;
import com.temenos.t24.api.records.aabilldetails.AaBillDetailsRecord;
import com.temenos.t24.api.records.aabilldetails.PayPropertyClass;
import com.temenos.t24.api.records.aabilldetails.PaymentTypeClass;
import com.temenos.t24.api.records.aaprddestermamount.AaPrdDesTermAmountRecord;
import com.temenos.t24.api.records.account.AccountRecord;
import com.temenos.t24.api.records.company.CompanyRecord;
import com.temenos.t24.api.records.ebffloandpd.DateClass;
import com.temenos.t24.api.records.ebffloandpd.EbFfLoanDpdRecord;
import com.temenos.t24.api.records.ebffparameter.EbFfParameterRecord;
import com.temenos.t24.api.records.ebffparameter.ParamDescClass;
import com.temenos.t24.api.system.DataAccess;
import com.temenos.t24.api.system.Session;

/*-----------------------------------------------------------------------------
 * @author Kavya N
 * Date Created:
 * Attached as : Service Routine
 * EB.API>FF.BRN.SNAPSHOT.REPORT
 *PGM.FILE>BNK/FF.BRN.SNAPSHOT.REPORT
 * Attached to : BATCH > BNK/FF.DAILY.REPORT.EXTRACT  & MFI/FF.DAILY.REPORT.EXTRACT
 * Description: COB Report generation -> BranchWise snapshot Report
 * 
 *  MFI/FF.B.SNAPSHOT.COB.REPORT
 *------------------------------------------------------------------------------ 
 * Modification History : NA
 *----------------------------------------------------------------------------- 
 */
 
public class FfSerBranchWiseSnapReport extends ServiceLifecycle {

    //  private static final FusionFileLogger loggerfile = FusionFileLogger.getLogger(FfSerBranchWiseSnapReport.class);
  //  private static final Logger loggerfile = Logger.getLogger("BranchwiseSnapshot");
    public static final String AA_BILL_DETAILS = "AA.BILL.DETAILS";
    public static final String UNCACCOUNT = "UNCACCOUNT";
    public static final String TRADE = "TRADE";
    public static final String AA_ARRANGEMENT = "AA.ARRANGEMENT";
    public static final String AA_ARR_ACCOUNT = "AA.ARR.ACCOUNT";
    public static final String COMPANY = "COMPANY";
    public static final String FFCURODAMT = "FFCURODAMT";
    public static final String FFPRINDEFAMT = "FFPRINDEFAMT";

    public static final DateTimeFormatter DATE_FORMAT_YYYYMMDD = DateTimeFormatter.ofPattern("yyyyMMdd");
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
    DataAccess da = new DataAccess();
    Session ss = new Session(this);
    Bill bill = new Bill(this);
    private Map<String, AaArrangementRecord> arrangements = new HashMap<>();
    private Map<String, AaAccountDetailsRecord> accDetailsId = new HashMap<>();
    List<String> overdueList = new ArrayList<>();
    Map<String, CompanyRecord> companyRecCache = new HashMap<>();
    List<String> arrList = new ArrayList<>();
    List<String> todayArrList = new ArrayList<>();
    List<String> actArrList = new ArrayList<>();
    List<String> todayCustarr = new ArrayList<>();
    Set<String> custSet = new HashSet<>();
    List<String> custarr = new ArrayList<>();
    List<String> activeCust = new ArrayList<>();
    List<String> comLists = new ArrayList<>();
    Set<String> uniqueVillages = new HashSet<>();
    Set<String> groupList = new HashSet<>();
    Set<String> centerList = new HashSet<>();
    Set<String> ecbCustCount = new HashSet<>();
    Set<String> branchCodeCount = new HashSet<>();
    int activeCustomers = 0;
    String todayDate = ss.getCurrentVariable("!TODAY");
    String finMnemonic = "";
    String customerNumber = "";
    String arrCurAccBal = "";
    String arrDueAccBal = "";
    String arrS0AccBal = "";
    String arrS1AccBal = "";
    String arrS2AccBal = "";
    String arrNpaAccBal = "";    
    String arrDisbureAmt = "";    
    double portfolioOustandingDbl = 0.0;  
    double overduePrincipalAmtdbl = 0.0;
    double overduePrincipalAmountDbl = 0.0;  
    Double arrTotOustanddbl = 0.0;
    double amountDisburedInYearDbl = 0.0;    
    EbFfParameterRecord paramRec = null;
    List<ParamDescClass> paramDescList = null;
    String paramPath = "";
    String paraDesc = "";
    String paraDescName = "";
    String blocksMunicipalityCovered = "";
    String groupId = "";    
    String centerId = "";
    int centers = 0;  
    double amountDisburedInPeriodDbl = 0.0;   
    String lendingarrId = "";  
    int groups = 0;
    String ecbCustNo = "";
    int ecbCustNoCount = 0;
    List<String> arrIdList = new ArrayList<>();
    List<String> contractIdList = new ArrayList<>();
    List<String> loanDisburedInYearList = new ArrayList<>();
    List<String> loanDisburedInYearList2 = new ArrayList<>();
    List<String> loanDisburedInPeriodList = new ArrayList<>();
    List<String> custOverDueList = new ArrayList<>();
    List<String> curDpdList = new ArrayList<>();
    private Map<String, EbFfLoanDpdRecord> loanDpd = new HashMap<>();

    List<String> totalDisbursementAmount = new ArrayList<>();
    List<String> finalReport = new ArrayList<>();
    String arrId = "";
    double overdueportfolioOustandingDbl = 0.0;
    String arrUncAccBal = "";
    String arrAccprinBal="";
    String arrDuePrinBal="";
    String arrS0PrinBal="";
    String arrS1PrinBal="";
    String arrS2PrinBal="";
    String arrNpaPrinBal="";
    String arrCurrAccBal="";
    double overdueportfolioOustandingAmtDbl=0.0;
    String mnemonic="";
    String branchCodeVals="";
    List<String> cusLendArrIds = new ArrayList<>();
    String clusterOfficeName = "";
    String clusterOfficeCode= "";
    String divisionalOfficeName = "";
    String divisionalOfficeCode = "";
    String zoneOfficeName = "";
    String zoneOfficeCode = "";
    String branchName = "";
    String branchCode = "";
    String state = "";
    int openLoanCLient = 0;
    int activeGroups = 0;
    int activeCenters = 0;
    int openLoanAccount;
    String portfolioOustanding="";
    int clientsOverdue = 0;
    String   overduePrincipalAmount="";
    String overdueportfolioOustanding ;
    int villageCovered;
    int loanDisburedInYear;
    String amountDisburedInYear ="";
    int loanDisburedInPeriod;
    String amountDisburedInPeriod="";
    String paymentBill="";
    String origContractDate="";
    LocalDate today = LocalDate.parse(todayDate, formatter);
    LocalDate oneYearAgo = today.minusYears(1);
    LocalDate oneMonthAgo = today.minusMonths(1);
    LocalDate fyStart;
    LocalDate fyEnd;
    LocalDate monthStart;
    List<String> branchCodeList = new ArrayList<String>();
    public static final String CHARGEACC = "LENDING-CHARGEOFF-ACCOUNT";
    public static final String CHARGEARR = "LENDING-CHARGEOFF-ARRANGEMENT";
    List<String> arrtActivityList = new ArrayList<>();
    @Override
  
    public List<String> getIds(ServiceData serviceData, List<String> controlList) {

        try {
            initialiseCompanyInfo(serviceData);        
            comLists = da.selectRecords("", COMPANY , "","");

            /*
             * for (String arrIds : comLists) { AaArrangementRecord aaArrRec =
             * getArrangement(arrIds); branchSet.add(aaArrRec.getCoCodeRec().getValue()); }
             */     

        } catch (Exception e) {
            e.getMessage();
        }
        return new ArrayList<>(comLists);
    }


    @Override
    public void process(String id, ServiceData serviceData, String controlItem) {

        try {
            branchCodeVals = id;
            initialiseCompanyInfo(serviceData);           
            getVariables();                       
            arrList = da.selectRecords(finMnemonic, AA_ARRANGEMENT, "","WITH CO.CODE EQ " + branchCodeVals);  
            // arrList = da.selectRecords("", AA_ARRANGEMENT, "","WITH @ID EQ AA25304CZ9LC AND CO.CODE EQ " + branchCodeVals);
 
            int count = arrList.size();
            for(int j=0;j<count;j++){
                arrId = arrList.get(j); 

                if (!isChargeOffArrangement(arrId)) {
                    Contract contract = new Contract(this);
                    contract.setContractId(arrId); 
                    getCocode(branchCodeVals);                 
                    getAaArrAccountDetails(arrId);                  
                    processDisbursementForFY(arrId,formatter);
                    processMonthToDateDisbursement(arrId,today);
                    getBalanceDetails(contract); 
                    getCustomerRecords(arrId);
                    getOverdueStat(arrId);
                }       
                if(j==count-1) {
              
                    finalReport.add(clusterOfficeName + "," + clusterOfficeCode + "," + divisionalOfficeName + "," + divisionalOfficeCode + "," + zoneOfficeName
                            + "," + zoneOfficeCode + "," + branchName + "," + branchCodeVals + "," + state + "," + openLoanCLient + ","
                            + activeGroups + "," + activeCenters + "," + openLoanAccount + "," + portfolioOustanding + ","
                            + clientsOverdue + "," + overduePrincipalAmount + "," + overdueportfolioOustanding + ","
                            + villageCovered + "," + loanDisburedInYear + ","
                            + amountDisburedInYear + "," + loanDisburedInPeriod + "," + amountDisburedInPeriod);

                }
            }

        } catch (Exception e) {
            e.getMessage();
        }
        try {
            if (!finalReport.isEmpty()) {
                paramRec = new EbFfParameterRecord(da.getRecord("EB.FF.PARAMETER", "FF.COB.REPORT.EXTRACT"));
                if (paramRec != null && !paramRec.getParamDesc().isEmpty()) {
                    paramDescList = paramRec.getParamDesc();
                    for (ParamDescClass paramDesc : paramDescList) {
                        paraDesc = paramDesc.getParamDesc().getValue();
                        if (paraDesc.equals("Custom Path for COB Reports")) {

                            paramPath = paramDesc.getParamValue().getValue();
                        }
                    }
                }

            }

            String outputPath = paramPath + "BranchwiseSnapshot" + "_" + finMnemonic + "_" + todayDate + "_temp_"
                    + ss.getSessionNumber() + ".csv";
            writeToFile(finalReport, outputPath);   


        } catch (Exception e) {
            e.getMessage();
        }
    }
  
    public void getVariables()  {
        portfolioOustandingDbl=0.0;
        overduePrincipalAmountDbl =0.0;
        overdueportfolioOustandingAmtDbl =0.0;
        amountDisburedInYearDbl =0.0;
        amountDisburedInPeriodDbl =0.0;
        loanDisburedInYearList.clear();
        loanDisburedInPeriodList.clear();
        ecbCustCount.clear(); 
        groupList.clear();
        centerList.clear();
        uniqueVillages.clear();
        actArrList.clear();
        custOverDueList.clear();
        cusLendArrIds.clear();
        curDpdList.clear();
        custSet.clear();
        arrtActivityList.clear();

        portfolioOustanding ="";
        overduePrincipalAmount="";
        overdueportfolioOustanding="0";
        amountDisburedInYear ="";
        amountDisburedInPeriod ="";
        loanDisburedInYear=0;
        loanDisburedInPeriod=0;
        clientsOverdue=0;
        activeCustomers=0;
        groups=0;
        activeGroups=0;
        centers=0;
        activeCenters=0;
        villageCovered=0;
        openLoanAccount=0;
    }

    private boolean isChargeOffArrangement(String arrId) {
  
        try {
            AaActivityHistoryRecord actRec =new AaActivityHistoryRecord(da.getRecord(finMnemonic, "AA.ACTIVITY.HISTORY", "", arrId)); 

            for (EffectiveDateClass eff : actRec.getEffectiveDate()) {
                for (ActivityRefClass act : eff.getActivityRef()) {   
                    String activity = act.getActivity().getValue();
                    if (CHARGEACC.equals(activity) || CHARGEARR.equals(activity)) {
                        return true;
                    }
                }
            }
        } catch (Exception e) {

        }
        return false;
    }

    public AaArrangementRecord getArrangement(String arrId) {
        try {
            AaArrangementRecord arrRec = arrangements.get(arrId);
            if (arrRec == null) {
                arrRec = new AaArrangementRecord(da.getRecord(AA_ARRANGEMENT, arrId));
                arrangements.put(arrId, arrRec);
            }
            return arrRec;
        } catch (Exception e) {
            return null;
        }
    }

    public AaAccountDetailsRecord getAccountDetails(String arrId) {
     
        try {
            AaAccountDetailsRecord accRec = accDetailsId.get(arrId);
            if (accRec == null) {
                accRec = new AaAccountDetailsRecord(
                        da.getRecord("AA.ACCOUNT.DETAILS", arrId));
                accDetailsId.put(arrId, accRec);
            }
            return accRec;
        } catch (Exception e) {
            return null;
        }
    }

    public CompanyRecord getCompanyRecord(String code) {

        try {
            CompanyRecord compRec = companyRecCache.get(code);
            if (compRec == null) {
                compRec = new CompanyRecord(da.getRecord(COMPANY, code));
                companyRecCache.put(code, compRec);
            }
            return compRec;
        } catch (Exception e) {
            return null;
        }
    }

    public void initialiseCompanyInfo(ServiceData serviceData) {

        try {
            String companyId = serviceData.getCompanyId();
            CompanyRecord companyRec = new CompanyRecord(da.getRecord(COMPANY, companyId));
            finMnemonic = companyRec.getFinancialMne().getValue();
        } catch (Exception e) {
            e.getMessage();
        }

    }

    public void getCocode(String branchCodeVals) {

        try { 

            CompanyRecord companyRecs = getCompanyRecord(branchCodeVals);
            mnemonic = companyRecs.getMnemonic().getValue();    
            String companyName = companyRecs.getCompanyName().get(0).getValue();
            String[] splitCompanyName = companyName.split("-");
            branchName = splitCompanyName[0];

            zoneOfficeCode = companyRecs.getLocalRefField("FF.ZONE").getValue();       
            CompanyRecord zoneRec = getCompanyRecord(zoneOfficeCode);
            String zoneOfficeNameVal = zoneRec.getCompanyName().get(0).getValue();
            String[] splitZoneName = zoneOfficeNameVal.split("-");
            zoneOfficeName = splitZoneName[0];

            divisionalOfficeCode = companyRecs.getLocalRefField("FF.DIVISION").getValue();      
            CompanyRecord divisionRec = getCompanyRecord(divisionalOfficeCode);
            String divisionalOfficeNameVal = divisionRec.getCompanyName().get(0).getValue();
            String[] splitDivisionName = divisionalOfficeNameVal.split("-");
            divisionalOfficeName = splitDivisionName[0];

            clusterOfficeCode = companyRecs.getLocalRefField("FF.CLUSTER").getValue();          
            CompanyRecord clusterRec = getCompanyRecord(clusterOfficeCode);
            String clusterOfficeNameVal = clusterRec.getCompanyName().get(0).getValue();
            String[] splitClusterName = clusterOfficeNameVal.split("-");
            clusterOfficeName = splitClusterName[0];

            state = companyRecs.getLocalRefField("FF.STATE").getValue();


        } catch (Exception e) {
            e.getMessage();
        } }


    public void getCustomerRecords(String arrId) {

        try {    
            AaArrangementRecord aaArrRec =getArrangement(arrId);
            String arrStatus = aaArrRec.getArrStatus() != null ? aaArrRec.getArrStatus().getValue() : "";
            if(!arrStatus.equals("PENDING.CLOSURE") && (!arrStatus.equals("CLOSE")) ) {  

                cusLendArrIds.add(arrId);
                String custId = aaArrRec.getCustomer(0).getCustomer().getValue();
                if( custId != null && !custId.isEmpty()) {
                    custSet.add(custId);
                }}
            openLoanAccount = cusLendArrIds.size();
            openLoanCLient = custSet.size();


        } catch (Exception e) {
            e.getMessage();
        }
    }

    public void getAaArrAccountDetails(String arrId) {

        try {
            AaArrangementRecord aaArrRec = getArrangement(arrId);               
            String legAcctNo = aaArrRec.getLinkedAppl().get(0).getLinkedApplId().getValue();

            if(!legAcctNo.isEmpty()) {             
                AccountRecord accRec = new AccountRecord(da.getRecord(finMnemonic, "ACCOUNT", "", legAcctNo));
                groupId = accRec.getLocalRefField("FF.GROUP").getValue();
                if (!groupId.equals("")) {
                    groupList.add(groupId);
                    activeGroups = groupList.size();

                }

                centerId = accRec.getLocalRefField("FF.CENTRE").getValue();
                if (!centerId.equals("")) {
                    centerList.add(centerId);
                    activeCenters = centerList.size();

                }

                String uniqueVillage = accRec.getLocalRefField("FF.VILLAGE").getValue();
                if (!uniqueVillage.equals("")) {
                    uniqueVillages.add(uniqueVillage);
                    villageCovered = uniqueVillages.size();

                }
            }
        } catch (Exception e) {
            e.getMessage();
        }
    }


    public String formDpId(String arrId, String date) {

        try {
            LocalDate parsedDate = LocalDate.parse(date, DateTimeFormatter.ofPattern("yyyyMMdd"));
            String monthText = parsedDate.format(DateTimeFormatter.ofPattern("MMM")).toUpperCase();    
            String year = String.valueOf(parsedDate.getYear());

            return arrId + "-" + monthText + year;
        } catch (Exception e) {
            return null;
        }
    }


    public void getOverdueStat(String arrId) {

        try {
            String dpdId = formDpId(arrId, todayDate);
            EbFfLoanDpdRecord loanDpdRec = loanDpd.get(dpdId);    
            if (loanDpdRec == null) {
                loanDpdRec = new EbFfLoanDpdRecord(da.getRecord("EB.FF.LOAN.DPD", dpdId));
                loanDpd.put(dpdId, loanDpdRec);
            }

            boolean isOverdue = false;   
            for (DateClass loanDpdDate : loanDpdRec.getDate()) {   
                String curDpd = loanDpdDate.getCurDpd().getValue();   
                if (curDpd != null && !curDpd.isEmpty()) {
                    int dpd = Integer.parseInt(curDpd);    
                    if (dpd > 0) {
                        isOverdue = true;
                        break;
                    }
                }
            }

            if (isOverdue) {
                if (!curDpdList.contains(arrId)) {
                    curDpdList.add(arrId);   
                    AaArrangementRecord aaArrRec = getArrangement(arrId);
                    String custId = aaArrRec.getCustomer(0).getCustomer().getValue();  
                    if (!custOverDueList.contains(custId)) {
                        custOverDueList.add(custId);
                    }
                    Contract overdueContract = new Contract(this);
                    overdueContract.setContractId(arrId);
                    getOverDueBalanceDetails(overdueContract);
                }
            }  
            clientsOverdue = custOverDueList.size();
            overdueportfolioOustanding =
                    String.format("%.2f", overdueportfolioOustandingAmtDbl);

        } catch (Exception e) {
            e.getMessage();
        }
    }

    public void getOverDueBalanceDetails(Contract overdueContract) {

        try {
            arrCurrAccBal = getOverDueBalance(overdueContract, FFCURODAMT, TRADE);      

            overdueportfolioOustandingDbl = Math.abs(Double.parseDouble(arrCurrAccBal));

            overdueportfolioOustandingAmtDbl += overdueportfolioOustandingDbl;
            overdueportfolioOustanding = String.format("%.2f", overdueportfolioOustandingAmtDbl);


        } catch (NumberFormatException e) {
            e.getMessage();
        }
    }

    public String getOverDueBalance(Contract overdueContract, String accountType, String bookingType) {
        List<BalanceMovement> movements=null;
        try {
            movements = overdueContract.getContractBalanceMovements(accountType, bookingType);
        } catch (Exception e) {

        }
        return (movements != null && !movements.isEmpty()) ? movements.get(0).getBalance().toString() : "0";
    }

    public void getBalanceDetails(Contract contract) {

        try {
            arrCurrAccBal = getBalance(contract, FFCURODAMT, TRADE);  //CUR,DUE,S0,S1,S2,NPA ACCOUNT
            arrDueAccBal = getBalance(contract, FFPRINDEFAMT, TRADE); //S0,S1,S2,NPA-ACCOUNT


            overduePrincipalAmtdbl = Math.abs(Double.parseDouble(arrDueAccBal));
            arrTotOustanddbl = Math.abs(Double.parseDouble(arrCurrAccBal) );

            portfolioOustandingDbl += arrTotOustanddbl;
            overduePrincipalAmountDbl += overduePrincipalAmtdbl;

            portfolioOustanding=String.format("%.2f", portfolioOustandingDbl);
            overduePrincipalAmount=String.format("%.2f", overduePrincipalAmountDbl);


        } catch (NumberFormatException e) {
            e.getMessage();
        }
    }

    public String getBalance(Contract contract, String accountType, String bookingType) {
        List<BalanceMovement> movements=null;
        try {
            movements = contract.getContractBalanceMovements(accountType, bookingType);
        } catch (Exception e) {

        }
        return (movements != null && !movements.isEmpty()) ? movements.get(0).getBalance().toString() : "0";
    }

    public LocalDate[] getFinancialYearRange(LocalDate today) {   
    
        if (today.getMonthValue() >= 4) {
            fyStart = LocalDate.of(today.getYear(), 4, 1);
            fyEnd = LocalDate.of(today.getYear() + 1, 3, 31);
        } else {
            fyStart = LocalDate.of(today.getYear() - 1, 4, 1);
            fyEnd = LocalDate.of(today.getYear(), 3, 31);
        }

        return new LocalDate[]{fyStart, fyEnd};

    }


    public void processDisbursementForFY(String arrId,DateTimeFormatter formatter) {  
        try {

            LocalDate[] fyRange = getFinancialYearRange(today);   
            fyStart = fyRange[0];
            fyEnd = fyRange[1];  
            boolean addedFromArrangement =checkArrangementDateForYear(arrId, fyStart, fyEnd, formatter);   
            if (!addedFromArrangement) {
                checkDisbursementBillsForYear(arrId, fyStart, fyEnd, formatter);
            }   
            amountDisburedInYear = String.format("%.2f", amountDisburedInYearDbl);    

        } catch (Exception e) {
            e.getMessage();
        }
    }
    public boolean checkArrangementDateForYear(String arrId,LocalDate fyStart,LocalDate fyEnd,DateTimeFormatter formatter) {

        try {
            AaArrangementRecord aaArrRec = getArrangement(arrId);   
            String origContDate = aaArrRec.getOrigContractDate().getValue();   
            if (origContDate != null && !origContDate.isEmpty()) {   
                LocalDate orgDate = LocalDate.parse(origContDate, formatter);   
                if (!orgDate.isBefore(fyStart) && !orgDate.isAfter(fyEnd)) {   
                    loanDisburedInYear++;
                    loanDisburedInYearList.add(arrId);   
                    addArrangementAmountForYear(arrId);
                    return true;
                }
            }

        } catch (Exception e) {
            e.getMessage();
        }

        return false;
    }

    public void addArrangementAmountForYear(String arrId) {

        try {
            Contract termContract = new Contract(this);
            termContract.setContractId(arrId);

            AaPrdDesTermAmountRecord aaArrTermAmtRec =new AaPrdDesTermAmountRecord(termContract.getConditionForProperty("COMMITMENT"));   
            if (aaArrTermAmtRec != null && aaArrTermAmtRec.getAmount() != null) {   
                String amt = aaArrTermAmtRec.getAmount().getValue();   
                if (amt != null && !amt.isEmpty()) {
                    amountDisburedInYearDbl += Double.parseDouble(amt);
                }
            }
        } catch (NumberFormatException e) {
            e.getMessage();
        }
    }
    public void checkDisbursementBillsForYear(String arrId,LocalDate fyStart,LocalDate fyEnd,DateTimeFormatter formatter) {

        try {
            AaAccountDetailsRecord aaAccDetRec = getAccountDetails(arrId); 
            for (BillPayDateClass billPayDt : aaAccDetRec.getBillPayDate()) {  
                for (BillIdClass billId : billPayDt.getBillId()) {   
                    if (billId.getBillType().getValue().equals("DISBURSEMENT")) {   
                        String billDateStr = billId.getBillDate().getValue();  
                        if (billDateStr != null &&!billDateStr.isEmpty()) {   
                            LocalDate billDate =LocalDate.parse(billDateStr, formatter);  
                            if (!billDate.isBefore(fyStart) &&!billDate.isAfter(fyEnd)) {  
                                loanDisburedInYear++;
                                addBillAmountForYear(billId.getBillId().getValue());
                            }
                        }
                    }
                }
            }

        } catch (Exception e) {
            e.getMessage();
        }  

    }


    public void addBillAmountForYear(String billId) {

        try {
            AaBillDetailsRecord aaBillDetsRec =new AaBillDetailsRecord(da.getRecord(AA_BILL_DETAILS, billId));   
            for (PaymentTypeClass paymentType :aaBillDetsRec.getPaymentType()) {   
                for (PayPropertyClass payProp :paymentType.getPayProperty()) { 
                    String amt =payProp.getOrPrAmt().getValue(); 
                    if (amt != null && !amt.isEmpty()) {
                        amountDisburedInYearDbl +=Double.parseDouble(amt);
                    }
                }
            }
        } catch (NumberFormatException e) {
            e.getMessage();
        }
    }

    public void processMonthToDateDisbursement(String arrId, LocalDate today) {

        try {         
            monthStart = today.withDayOfMonth(1); 
            boolean addedFromArrangement =checkArrangementDateForPeriod(arrId, monthStart, today, formatter);
            if (!addedFromArrangement) {
                checkBillsForPeriod(arrId, monthStart, today, formatter);
            }
            amountDisburedInPeriod =String.format("%.2f",amountDisburedInPeriodDbl);

        } catch (Exception e) {
            e.getMessage();
        }
    }

    public boolean checkArrangementDateForPeriod(String arrId,LocalDate monthStart, LocalDate today,DateTimeFormatter formatter) {
        try {
            AaArrangementRecord aaArrRec = getArrangement(arrId);
            String origContDate =aaArrRec.getOrigContractDate().getValue();
            if (origContDate != null &&!origContDate.isEmpty()) {
                LocalDate orgDate =LocalDate.parse(origContDate, formatter);
                if (!orgDate.isBefore(monthStart) &&!orgDate.isAfter(today)) {
                    loanDisburedInPeriod++;
                    addArrangementAmountForPeriod(arrId);
                    return true;
                }
            }


        } catch (Exception e) {
            e.getMessage();
        }
        return false;
    }

    public void addArrangementAmountForPeriod(String arrId) {

        try {
            Contract termContract = new Contract(this);
            termContract.setContractId(arrId);
            AaPrdDesTermAmountRecord aaArrTermAmtRec =new AaPrdDesTermAmountRecord(termContract.getConditionForProperty("COMMITMENT"));
            if (aaArrTermAmtRec != null &&aaArrTermAmtRec.getAmount() != null) {
                String amt = aaArrTermAmtRec.getAmount().getValue();
                if (amt != null && !amt.isEmpty()) {
                    amountDisburedInPeriodDbl += Double.parseDouble(amt);
                }
            }
        } catch (NumberFormatException e) {
            e.getMessage();
        }
    }

    public void checkBillsForPeriod(String arrId,LocalDate monthStart, LocalDate today, DateTimeFormatter formatter) {

        try {
            AaAccountDetailsRecord aaAccDetRec = getAccountDetails(arrId);
            for (BillPayDateClass billPayDt :aaAccDetRec.getBillPayDate()) {
                for (BillIdClass billId :billPayDt.getBillId()) {
                    if (billId.getBillType().getValue().equals("DISBURSEMENT")) {  
                        String billDateStr =billId.getBillDate().getValue();
                        if (billDateStr != null &&!billDateStr.isEmpty()) {
                            LocalDate billDate = LocalDate.parse(billDateStr, formatter);
                            if (!billDate.isBefore(monthStart) &&!billDate.isAfter(today)) {
                                loanDisburedInPeriod++;
                                addBillAmountForPeriod(
                                        billId.getBillId().getValue());
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.getMessage();
        }
    }

    public void addBillAmountForPeriod(String billId) {

        try {
            AaBillDetailsRecord aaBillDetsRec =new AaBillDetailsRecord(da.getRecord(AA_BILL_DETAILS, billId));
            for (PaymentTypeClass paymentType : aaBillDetsRec.getPaymentType()) {
                for (PayPropertyClass payProp : paymentType.getPayProperty()) {
                    String amt =payProp.getOrPrAmt().getValue();
                    if (amt != null && !amt.isEmpty()) {
                        amountDisburedInPeriodDbl +=Double.parseDouble(amt);
                    }
                }
            }
        } catch (NumberFormatException e) {
            e.getMessage();
        }
    }  

    public void writeToFile(List<String> data, String outputPath) {
        try {
            File file = new File(outputPath);
            file.getParentFile().mkdirs();
            boolean fileExists = file.exists();

            try (FileWriter writer = new FileWriter(file, true)) {
                if (!fileExists) {
                    String header = String.join(",", "ClusterOfficeName", "ClusterOfficeCode", "DivisionalOfficeName", "DivisionalOfficeCode",
                            "ZoneOfficeName", "ZoneOfficeCode", "BranchName", "BranchCode", "State", "OpenLoanCLient",
                            "ActiveGroups", "ActiveCenters", "TotalNoOfOpenLoanAccounts", "PortfolioOustanding",
                            "ClientsOverdue", "OverduePrincipalAmount", "OverduePortfolioOustanding", "VillagesCovered",
                            "LoansDisbursedInFinancialYear", "AmountDisbursedInFinancialYear", "LoanDisburedInPeriod", "AmountDisburedInPeriod");
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
