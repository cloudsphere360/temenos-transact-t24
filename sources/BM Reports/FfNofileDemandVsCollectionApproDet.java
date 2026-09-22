package com.temenos.fusion;

import java.io.File;
import java.io.FileWriter;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import com.temenos.api.TField;
import com.temenos.api.exceptions.T24CoreException;
import com.temenos.t24.api.arrangement.accounting.Contract;
import com.temenos.t24.api.complex.aa.contractapi.BalanceMovement;
import com.temenos.t24.api.complex.eb.enquiryhook.EnquiryContext;
import com.temenos.t24.api.complex.eb.enquiryhook.FilterCriteria;
import com.temenos.t24.api.hook.system.Enquiry;
import com.temenos.t24.api.records.aaaccountdetails.AaAccountDetailsRecord;
import com.temenos.t24.api.records.aaaccountdetails.BillIdClass;
import com.temenos.t24.api.records.aaaccountdetails.BillPayDateClass;
import com.temenos.t24.api.records.aaactivitybalances.AaActivityBalancesRecord;
import com.temenos.t24.api.records.aaactivitybalances.PropertyClass;
import com.temenos.t24.api.records.aaactivityhistory.AaActivityHistoryRecord;
import com.temenos.t24.api.records.aaactivityhistory.ActivityRefClass;
import com.temenos.t24.api.records.aaactivityhistory.EffectiveDateClass;
import com.temenos.t24.api.records.aaarrangement.AaArrangementRecord;
import com.temenos.t24.api.records.aabilldetails.AaBillDetailsRecord;
import com.temenos.t24.api.records.aacustomerarrangement.AaCustomerArrangementRecord;
import com.temenos.t24.api.records.aacustomerarrangement.ArrangementClass;
import com.temenos.t24.api.records.aacustomerarrangement.ProductLineClass;
import com.temenos.t24.api.records.aaprddestermamount.AaPrdDesTermAmountRecord;
import com.temenos.t24.api.records.account.AccountRecord;
import com.temenos.t24.api.records.account.AltAcctTypeClass;
import com.temenos.t24.api.records.company.CompanyRecord;
import com.temenos.t24.api.records.companyconsol.CompanyConsolRecord;
import com.temenos.t24.api.records.customer.CustomerRecord;
import com.temenos.t24.api.records.ebffcentredetail.EbFfCentreDetailRecord;
import com.temenos.t24.api.records.ebffcollectiondets.EbFfCollectionDetsRecord;
import com.temenos.t24.api.records.ebffcollectiondetshistory.EbFfCollectionDetsHistoryRecord;
import com.temenos.t24.api.records.ebffgroups.EbFfGroupsRecord;
import com.temenos.t24.api.records.ebffloandetails.EbFfLoanDetailsRecord;
import com.temenos.t24.api.records.ebffloandpd.DateClass;
import com.temenos.t24.api.records.ebffloandpd.EbFfLoanDpdRecord;
import com.temenos.t24.api.records.ebffloanpaymenthis.DemandDateClass;
import com.temenos.t24.api.records.ebffloanpaymenthis.EbFfLoanPaymentHisRecord;
import com.temenos.t24.api.records.ebffparameter.EbFfParameterRecord;
import com.temenos.t24.api.records.ebffparameter.ParamDescClass;
import com.temenos.t24.api.records.ebffrouser.EbFfRoUserRecord;
import com.temenos.t24.api.records.fundstransfer.FundsTransferRecord;
import com.temenos.t24.api.records.user.UserRecord;
import com.temenos.t24.api.system.DataAccess;
import com.temenos.t24.api.system.Date;
import com.temenos.t24.api.system.Session;



public class FfNofileDemandVsCollectionApproDet extends Enquiry {
 //   private static final FusionFileLogger loggerFile = FusionFileLogger.getLogger(FfNofileDemandVsCollectionApproDet.class);
 //  private static final Logger loggerFile = Logger.getLogger("FfNofileDemandVsCollectionApproDet");
 
    
    DataAccess da = new DataAccess(this);
    Session ss = new Session(this);
    List<String> returnValueList = new ArrayList<>();
    private Map<String, AaAccountDetailsRecord> accDetailsId = new HashMap<>();
    public static final String FILE_NAME = "DemVsColAppropriateRep_Det";
    public static final String AA_BILL_DETAILS = "AA.BILL.DETAILS";
    public static final String TRADE = "TRADE";
    public static final String AA_ARRANGEMENT = "AA.ARRANGEMENT";
    public static final String AA_ARR_ACCOUNT = "AA.ARR.ACCOUNT";
    public static final String APPLY_PR_COLLECTION = "LENDING-APPLYPAYMENT-PR.COLLECTION";
    public static final String SETTLE_PR_COLLECTION = "LENDING-SETTLE-PR.COLLECTION";
    
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
    DateTimeFormatter outDateFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
    DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HHmmss");
    private Map<String, EbFfLoanDpdRecord> loanDpd = new HashMap<>();
    List<String> custOverDueList = new ArrayList<>();
    List<String> curDpdList = new ArrayList<>();
    private Map<String, AaArrangementRecord> arrangements = new HashMap<>();
    int clientsOverdue = 0;

    public static final String FFALLOVRDUEDMND = "FFALLOVRDUEDMND";
    public static final String UNCACCOUNT = "UNCACCOUNT";
    String arrNpaPrInterest ="";
    String arrDuePrInterest="";
    String arrS0PrInterest="";
    String arrS1PrInterest="";
    String arrS2PrInterest="";
    List<String> outvalues = new ArrayList<>();

    double overdueportfolioOustandingAmtDbl =0.0;
    String selFromDate = "";
    String selToDate = "";
    String startDate = "";
    String endDate = "";
    double totalCollectiondb=0.0;
    double advanceAmtDbl=0.0;
    List<String> finalArrList = new ArrayList<>();
    String selCenterName = "";
    String selProduct = "";
    String selVillage = "";
    String selDistrict = "";
    String selCycle = "";
    String selPurpose = "";
    String selCaste = "";
    String selReligion = "";
    Set<String> centreSet = new HashSet<>();
    Set<String> filterValSet = new HashSet<>();
    String branchName = "";
    String branchCode = "";
    String zoneName = "";
    String regionName = "";
    String divisionName = "";
    String clusterName = "";
    String arrUncAccBal = "";
    String arrDueAccBal = "";
    String arrS0AccBal = "";
    String arrS1AccBal = "";
    String arrS2AccBal = "";
    String arrNpaAccBal = "";
    String arrAccprinBal = "";
    String arrDuePrinBal = "";
    String arrS0PrinBal = "";
    String arrS1PrinBal = "";
    String arrS2PrinBal = "";
    String arrNpaPrinBal = "";
    String arrCurrAccBal = "";
    Double overdueDemanddbl = 0.0;
    String advanceAmt = "";
    Double parkedPaymentAmtdbl = 0.0;
    String overdueDemand = "";
    double demand = 0.0;
    int overdueCount = 0;
    String demandCashCarry = "";
    String totalCollection = "";
    Date t24date = new Date(this);
    String date = "";
    String todayDate = "";
    String finMnemonic = "";
    String selDateOp = "";
    String billVal = "";
    String foreclosure = "";
    double collectionAgainstOverdueDemand = 0.0;
    double collectionAgainstCurrentDemand = 0.0;
    boolean onlyDateFilter = false;
    boolean dateErrFlag = false;
    boolean noRecErrFlag = false;
    double loanAccountDbl = 0.0;
    double prInterestDbl = 0.0;
    double uncAccountDbl = 0.0;
    double totalCollectiondbl = 0.0;
    List<String> loanDpdList = new ArrayList<>();
    String curDpd = "";
    List<String> billList = new ArrayList<>();
    String loanAccount = "";
    String prInterest = "";
    String loanNumber = "";
    String customerNumber = "";
    String productName = "";
    String branchDistrict = "";
    String groupName = "";
    String centerName = "";
    String centerCode = "";
    String cycleNumber = "";
    String purposeOfLoan = "";
    String relofficerName = "";
    String groupCode = "";
    String givesName = "";
    String familyName = "";
    String customerName = "";
    String loanDate = "";
    String loanAmount = "";
    String branchState = "";
    String relMobileNumber = "";
    String branchManagerName = "";
    String branchManagerMobileNumber = "";
    boolean settlePayoff = false;
    String foreCloBillId = "";
    String dueDate = "";
    String companyIds = "";
    String selBranch = "";
    String branch="";
    String leagacyAccountNumber="";
    String mnemonic="";
    String origContractDate="";
    double totalDemand=0.0;
    double demandAmount=0.0;
    String pendingCollection = "";
    double prCollectionAmt =0.0;
    double foreclosureAmt=0.0;
    String legAcctNo="";
    boolean legacy=false;

    public static final String DATE_RANGE_ERR = "EB-FF.BM.FUTURE.MAX.DT.RANGE";
    public static final String DATE_FILTER_ERR = "EB-FF.BM.DATE.FILTER.RANGE";
    public static final String NO_REC_ERR = "EB-NO.REC.SELECT";
    String dateRangeVal = "";
    String daetFilterVal = "";
    boolean dateRangeErrFlag = false;
    boolean dateFilterErrFlag = false;
    String fldName = "";
    public static final String CENTRE = "FF.CENTRE";
    public static final String VILLAGE = "FF.VILLAGE";
    public static final String DISTRICT_NAME = "DISTRICT.NAME";
    public static final String LOAN_CYCLE = "FF.LOAN.CYCLE";
    public static final String LOAN_PURP = "FF.LOAN.PURP";
    public static final String RELIG_GROUP = "FF.RELIG.GROUP";
    public static final String CASTE = "FF.CASTE";
    String selUser="";
    double pending = 0.0;
    double overdueCollectionAmt=0.0;
    double collectionAmt=0.0;
    String stDt ="";
    private boolean billRecordFound = false;
    
    public static final List<String> FORECLOSURE_ACTIVITY_LIST = Arrays.asList("LENDING-SETTLE-FORECLOSURE","LENDING-APPLYPAYMENT-PR.OUTSTANDING.PAYOFF","LENDING-APPLYPAYMENT-PR.CURR.BALANCE",
            "LENDING-APPLYPAYMENT-INSURANCE.SETTLEMENT","LENDING-APPLYPAYMENT-PR.INSURANCE.BALANCES","LENDING-APPLYPAYMENT-WRITEOFF.SETTLEMENT");
     
    private static final List<String> TRANSACTION_TYPE_LIST = Arrays.asList(
            "ACDB",
            "ACRP",
            "ACP2",
            "ACPD",
            "ACDF"
    );
  
    @Override
    public List<String> setIds(List<FilterCriteria> filterCriteria, EnquiryContext enquiryContext) {
        try {
            todayDate = ss.getCurrentVariable("!TODAY");
            Contract contract = new Contract(this);         

            for (FilterCriteria filter : filterCriteria) {
                if (filter.getFieldname().equals("BRANCH")) {
                    selBranch = filter.getValue();
                    initialiseCompanyInfo(selBranch);
                    getLinkedCompIds(selBranch);
                    break;
                }
            }

            Set<String> selectionSet = getFilterCriteriaDets(filterCriteria);

            processDateValidation(startDate, endDate);
            if (dateFilterErrFlag) {
                throw new T24CoreException(covertParamValue(daetFilterVal), DATE_FILTER_ERR);
            }
            if (dateRangeErrFlag) {
                throw new T24CoreException(covertParamValue(dateRangeVal), DATE_RANGE_ERR);
            }
         
               Set<String> arrList = new LinkedHashSet<>(da.selectRecords("", AA_ARRANGEMENT, "","WITH CO.CODE EQ " + companyIds));
               
         //     Set<String> arrList = new LinkedHashSet<>(da.selectRecords("", AA_ARRANGEMENT, "","WITH @ID EQ AA25305GG7HW AND CO.CODE EQ " + companyIds));
         //  Set<String> arrList = new LinkedHashSet<>(da.selectRecords("", AA_ARRANGEMENT, "","WITH @ID EQ AA25305M1NR8 OR @ID EQ AA25305GG7HW AND CO.CODE EQ " + companyIds));   // DMT
               
            Set<String> preFinalSet = getPreFinalset(arrList, selectionSet);
            
            preFinalSet.retainAll(arrList);
            Map<String, String> finalArrMap =getFinalArrList(preFinalSet);
            for (String contractId : finalArrMap.keySet()) {
 
                AaArrangementRecord aaArrRec =new AaArrangementRecord(da.getRecord(finMnemonic,AA_ARRANGEMENT,"",contractId));            
                branch = aaArrRec.getCoCodeRec().getValue();             
                finalArrList.add(contractId);
            }

            for (String arrId : finalArrList) {
                resetVariables();

                contract.setContractId(arrId);  
                getArrangementDetails(contract,arrId);
                getCustomerDetails(customerNumber,arrId);
                getAaArrAccountDetails(legAcctNo);
                getAaArrTermAmountDetails(contract);
                getOverdueStat(arrId);               
                getEbFfLoanDetails(arrId);  

                getCollections( arrId) ;
                collectionAgainstOverdueDemand =getCollectionAgainstOverdueDemand(arrId);              

                collectionAgainstCurrentDemand =getCollectionAgainstCurrentDemand(arrId); 
                
                foreclosureAmt = getForeclosureWriteOffAmount(arrId);
                demand = getCurrentDemand(arrId);

                totalDemand =  getTotalDemand(arrId);
                
                totalCollectiondbl = foreclosureAmt+ advanceAmtDbl+ collectionAgainstCurrentDemand+ collectionAgainstOverdueDemand;               
                totalCollection = String.format("%.2f", totalCollectiondbl);

                pendingCollection = String.format("%.2f",Math.abs(totalDemand - totalCollectiondbl));

                List<String> row = new ArrayList<>();
                row.add(zoneName);
                row.add(regionName);
                row.add(divisionName);
                row.add(clusterName);
                row.add(branchName);
                row.add(branchDistrict); 
                row.add(branchState);
                row.add(branchCode);
                row.add(centerName); 
                row.add(centerCode); 
                row.add(groupName);
                row.add(groupCode);
                row.add(customerName);
                row.add(customerNumber);
                row.add(loanNumber);
                row.add(leagacyAccountNumber);
                row.add(convertDate(loanDate)); 
                row.add(loanAmount);
                row.add(productName);
                row.add(cycleNumber); 
                row.add(relofficerName); 
                row.add(relMobileNumber);
                row.add(branchManagerName);
                row.add(branchManagerMobileNumber);
                row.add(overdueDemand);
                row.add(String.format("%.2f", demand));
                row.add(demandCashCarry);
                row.add(String.format("%.2f", totalDemand));
                row.add(String.valueOf(collectionAgainstOverdueDemand)); 
                row.add(String.valueOf(collectionAgainstCurrentDemand));
                row.add(advanceAmt);
                row.add(String.valueOf(foreclosureAmt));
                row.add(totalCollection);
                row.add(pendingCollection);

                returnValueList.add(String.join("*", row));  
                outvalues.add(String.join(",", row));
                

            }  

            String fileParamId = "FF.BM.REPORT.EXTRACT";
            String fileParamName = "Path";
            String filePath = getEbFfParamRecDets(fileParamId, fileParamName);

            LocalDateTime currDtTime = LocalDateTime.now();
            String currDate = currDtTime.format(outDateFormatter);
            String currTime = currDtTime.format(timeFormatter);
            String outputPath = filePath + FILE_NAME + "_" + selBranch + "_" + selUser + "_"
                    + currDate + "_" + currTime + ".csv";

            //   DemVsColAppropriateRep_Det_BranchName_USERID_Today'sdate_Time.csv

            writeToFile(outvalues, outputPath);


        } catch (T24CoreException e) {
            throw e;
        } catch (Exception e) {
            throw new T24CoreException(e.getMessage());
        }
        if (noRecErrFlag || returnValueList.isEmpty()) {
            throw new T24CoreException("", NO_REC_ERR);
        } else {
            return returnValueList;
        }
    }

     
    public Map<String, String> getFinalArrList(Set<String> preArrSet) {
     
        Map<String, String> arrMap = new LinkedHashMap<>();
     
        try {
     
            String fromDate = (startDate == null || startDate.isEmpty())? todayDate : startDate;
     
            String toDate = (endDate == null || endDate.isEmpty())? todayDate : endDate;
     
            for (String arrId : preArrSet) {
     
                boolean validArrangement = false;
                String finalEffDate = "";
     
                AaActivityHistoryRecord aaActHisRec =new AaActivityHistoryRecord(da.getRecord(finMnemonic,"AA.ACTIVITY.HISTORY","",arrId));   
                List<EffectiveDateClass> effDateList =aaActHisRec.getEffectiveDate();   
                for (EffectiveDateClass effDateCls : effDateList) {    
                    finalEffDate =effDateCls.getEffectiveDate().getValue();    
                    if (finalEffDate != null && !finalEffDate.isEmpty() && finalEffDate.compareTo(fromDate) >= 0
                            && finalEffDate.compareTo(toDate) <= 0) {    
                        List<ActivityRefClass> activityRefList = effDateCls.getActivityRef();    
                        for (ActivityRefClass actRef : activityRefList) {    
                            String contractId =actRef.getContractId().getValue();
     
                            if (contractId != null && contractId.startsWith("FT")) {   
                                String ftId = contractId.contains("\\")? contractId.split("\\\\")[0]: contractId;    
                                FundsTransferRecord ftRec = null;
     
                                try {
                                    ftRec = new FundsTransferRecord(da.getRecord("FUNDS.TRANSFER",ftId));
                                } catch (Exception e) {
                                    try {
                                        ftRec = new FundsTransferRecord(da.getHistoryRecord("FUNDS.TRANSFER",ftId));
                                    } catch (Exception exception) {
                                        ftRec = null;
                                    }
                                }
     
                                if (ftRec != null) {  
                                    String transactionType =ftRec.getTransactionType().getValue();
                                    if (transactionType != null&& TRANSACTION_TYPE_LIST.contains(transactionType)) {  
                                        validArrangement = true;
                                        break;
                                    }
                                }
                            }
                        }
     
                        if (validArrangement) {
                            break;
                        }
                    }
                }
     
                if (validArrangement) {
                    arrMap.put(arrId, finalEffDate);
                }
            }
     
        } catch (Exception e) {
            e.printStackTrace();
        }
     
        return arrMap;
    }
     

    public void processDateValidation(String startDate, String endDate) {

        try {
            dateRangeErrFlag = false;
            dateFilterErrFlag = false;

            String dateRangeValLocal = getEbFfParamRecDets("FF.BM.REPORT.DATE.RANGE", "DEM.COLL.APP"); 
            String dateFilterValLocal = getEbFfParamRecDets("FF.BM.REPORT.DATE.DEFAULT", "DEM.COLL.APP");

            this.dateRangeVal = dateRangeValLocal;
            this.daetFilterVal = dateFilterValLocal;


            LocalDate today = LocalDate.parse(todayDate, formatter);
            LocalDate start = (startDate == null || startDate.isEmpty())? today: LocalDate.parse(startDate, formatter);
            LocalDate end = (endDate == null || endDate.isEmpty())? today: LocalDate.parse(endDate, formatter);

            if (start.isAfter(end)) {
                dateRangeErrFlag = true;
                return;
            }

            if (start.isAfter(today) || end.isAfter(today)) {
                dateRangeErrFlag = true;
                return;
            }

            int maxDays = Integer.parseInt(dateRangeValLocal.replace("D", ""));
            LocalDate minAllowedDate = today.minusDays(maxDays);

            if (start.isBefore(minAllowedDate) || end.isBefore(minAllowedDate)) {
                dateRangeErrFlag = true;
                return;
            }

            int maxInputDays = Integer.parseInt(dateFilterValLocal.replace("D", ""));
            long diff = ChronoUnit.DAYS.between(start, end);

            if (diff > maxInputDays) {
                dateFilterErrFlag = true;
            }

        } catch (Exception e) {
            e.getMessage();
            throw new RuntimeException(e);

        }
    }

    public String getEbFfParamRecDets(String paramId, String paramName) {

        String paramVal = "";
        try {
            EbFfParameterRecord paramRec = new EbFfParameterRecord(da.getRecord("EB.FF.PARAMETER", paramId));
            for (ParamDescClass paramDesc : paramRec.getParamDesc()) {
                if (paramDesc.getParamName().getValue().equals(paramName)) {

                    paramVal = paramDesc.getParamValue().getValue();
                    return paramVal;

                }
            }
        } catch (Exception e) {
            e.getMessage();
        }
        return paramVal;
    }

    public String covertParamValue(String value) {

        if (value == null || value.isEmpty()) {
            return "";
        }
        String numberPart = value.replaceAll("\\D", "");
        String unitPart = value.replaceAll("\\d", "");

        if (numberPart.isEmpty() || unitPart.isEmpty()) {
            return value;
        }

        int num = Integer.parseInt(numberPart);
        char unit = Character.toUpperCase(unitPart.charAt(0));

        switch (unit) {
        case 'D':
            return num + (num == 1 ? " Day" : " Days");
        case 'M':
            return num + (num == 1 ? " Month" : " Months");
        default:
            return value;
        }
    }


    public String convertDate(String inDate) {
        String outDate = "";
        try {
            LocalDate date = LocalDate.parse(inDate, formatter);
            outDate = date.format(outDateFormatter);
            return outDate;
        } catch (Exception e) {
            e.getMessage();
        }
        return outDate;
    }

    private void resetVariables() {
        overdueportfolioOustandingAmtDbl = 0.0;
        totalCollectiondb = 0.0;
        totalCollectiondbl = 0.0;
        advanceAmtDbl = 0.0;
        loanAccountDbl = 0.0;
        prInterestDbl = 0.0;
        uncAccountDbl = 0.0;

        overdueDemand = "";
        demand = 0.0;
        demandCashCarry = "";
        collectionAgainstOverdueDemand = 0.0;
        collectionAgainstCurrentDemand = 0.0;
        advanceAmt = "";
        foreclosure = "";
        totalDemand = 0.0;
        totalCollection = "";
        arrUncAccBal = "";
        arrDueAccBal = "";
        arrS0AccBal = "";
        arrS1AccBal = "";
        arrS2AccBal = "";
        arrNpaAccBal = "";
        arrDuePrInterest = "";
        arrS0PrInterest = "";
        arrS1PrInterest = "";
        arrS2PrInterest = "";
        arrNpaPrInterest = "";
        loanAccount = "";
        prInterest = "";
        loanNumber = "";
        customerNumber = "";
        productName = "";
        leagacyAccountNumber = "";
        customerName = "";
        groupName = "";
        groupCode = "";
        centerName = "";
        centerCode = "";
        cycleNumber = "";
        purposeOfLoan = "";
        relofficerName = "";
        relMobileNumber = "";
        branchManagerName = "";
        branchManagerMobileNumber = "";
        loanDate = "";
        loanAmount = "";
        branchDistrict = "";

        foreclosure = "";
        curDpd = "";
        settlePayoff = false;
        pendingCollection = "";
        prCollectionAmt =0.0;
        foreclosureAmt=0.0;
        legacy= false;
        collectionAmt = 0.0;

    }

    public Set<String> getPreFinalset(Set<String> arrList, Set<String> selectionSet) {

        Set<String> preFinalSet = new LinkedHashSet<>();
        if (selectionSet.isEmpty()) {

            if (!filterValSet.isEmpty()) {             
                noRecErrFlag = true;            
            } else {

                preFinalSet = arrList;             
            }
        } else {

            preFinalSet = new LinkedHashSet<>(selectionSet);

        }
        return preFinalSet == null ? new LinkedHashSet<>() : preFinalSet;
    }


    public Set<String> getFilterCriteriaDets(List<FilterCriteria> filterCriteria) {
        Set<String> selectionSet = null;
        Set<String> currentFilterSet = new LinkedHashSet<>();

        try {
            for (FilterCriteria filter : filterCriteria) {
                String value = filter.getValue();
                if (value == null || value.isEmpty()) {
                    continue;
                }
                switch (filter.getFieldname()) {
                case "BRANCH":
                    selBranch = value;
                    if ("CURRENT.ID".equalsIgnoreCase(selBranch)) {
                        Session session = new Session(this);
                        selBranch = session.getCompanyId();

                    }
                    initialiseCompanyInfo(selBranch);
                    getLinkedCompIds(selBranch);
                    break;


                case "USER":
                    selUser = value;
                    break;                      

                case "DATE.FROM":
                    startDate = value;
                    break;

                case "DATE.TO":
                    endDate = value;
                    break;

                case "PRODUCT":
                    filterValSet.add(value);
                    selProduct = value;
                    currentFilterSet.addAll(da.selectRecords(finMnemonic, AA_ARRANGEMENT, "",
                            "WITH ARR.STATUS EQ CURRENT AND PRODUCT EQ " + selProduct));
                    break;

                case "CENTER":
                    filterValSet.add(value);
                    selCenterName = value;
                    fldName = CENTRE;
                    currentFilterSet.addAll(getArrAccountList(fldName, selCenterName));
                    break;

                case "VILLAGE":
                    filterValSet.add(value);
                    selVillage = value;
                    fldName = VILLAGE;
                    currentFilterSet.addAll(getArrAccountList(fldName, selVillage));
                    break;

                case "DISTRICT":
                    filterValSet.add(value);
                    selDistrict = value;
                    fldName = DISTRICT_NAME;
                    currentFilterSet.addAll(getCustomerArrList(fldName, selDistrict));
                    break;

                case "CYCLE":
                    filterValSet.add(value);
                    selCycle = value;
                    fldName = LOAN_CYCLE;
                    currentFilterSet.addAll(getArrAccountList(fldName, selCycle));
                    break;

                case "PURPOSE":
                    filterValSet.add(value);
                    selPurpose = value;
                    fldName = LOAN_PURP;
                    currentFilterSet.addAll(getArrAccountList(fldName, selPurpose));
                    break;

                case "RELIGION":
                    filterValSet.add(value);
                    selReligion = value;
                    fldName = RELIG_GROUP;
                    currentFilterSet.addAll(getCustomerArrList(fldName, selReligion));
                    break;

                case "CASTE":
                    filterValSet.add(value);
                    selCaste = value;
                    fldName = CASTE;
                    currentFilterSet.addAll(getCustomerArrList(fldName, selCaste));
                    break;

                default:
                }
                selectionSet = addToFinalSelectionSet(selectionSet, currentFilterSet);
            }

        } catch (Exception e) {
            e.getMessage();
        }

        return selectionSet == null ? new LinkedHashSet<>() : selectionSet;
    }

    public Set<String> addToFinalSelectionSet(Set<String> selectionSet, Set<String> currentFilterSet) {
        if (selectionSet == null) {
            if (!currentFilterSet.isEmpty()) {
                selectionSet = currentFilterSet;
            }
        } else {
            selectionSet.retainAll(currentFilterSet);
        }
        return selectionSet;
    }

    public List<String> getArrAccountList(String fieldName, String fieldValue) {
        List<String> arrAccList = da.selectRecords(finMnemonic, "AA.ARR.ACCOUNT", "",
                "WITH " + fieldName + " EQ " + fieldValue);
        return getArrListFromSelection(arrAccList);
    }
    public List<String> getArrListFromSelection(List<String> aaArrAccList) {
        List<String> arrIdList = new ArrayList<>();
        try {
            for (String arrId : aaArrAccList) {
                String[] parts = arrId.split("-");
                String aaId = parts[0];
                if (!arrIdList.contains(aaId)) {
                    arrIdList.add(aaId);
                }
            }
        } catch (Exception e) {
            e.getMessage();
        }
        return arrIdList;
    } 

    public List<String> getCustomerArrList(String fieldName, String fieldValue) {

        Set<String> customerSet = new LinkedHashSet<>(
                da.selectRecords(mnemonic, "CUSTOMER", "", "WITH " + fieldName + " EQ " + fieldValue));
        Set<String> arrCustomerSet = new LinkedHashSet<>(
                da.selectRecords(finMnemonic, "AA.CUSTOMER.ARRANGEMENT", "", "WITH CUSTOMER.ID NE NULL"));
        customerSet.retainAll(arrCustomerSet);
        return getArrListFromCusSelection(customerSet);
    }

    public List<String> getArrListFromCusSelection(Set<String> custList) {
        List<String> aaIdList = new ArrayList<>();
        try {
            for (String custId : custList) {
                AaCustomerArrangementRecord custArrRec = new AaCustomerArrangementRecord(
                        da.getRecord(finMnemonic, "AA.CUSTOMER.ARRANGEMENT", "", custId));
                for (ProductLineClass prdLine : custArrRec.getProductLine()) {
                    if (prdLine.getProductLine().getValue().equals("LENDING")) {
                        for (ArrangementClass arrIds : prdLine.getArrangement()) {
                            aaIdList.add(arrIds.getArrangement().getValue());
                        }
                        break;
                    }
                }
            }
        } catch (Exception e) {
            e.getMessage();
        }
        return aaIdList;
    }


    public AaArrangementRecord getArrangement(String arrId) {
        AaArrangementRecord arrRec = arrangements.get(arrId);
        if (arrRec == null) {
            arrRec = new AaArrangementRecord(da.getRecord(AA_ARRANGEMENT, arrId));
            arrangements.put(arrId, arrRec);
        }
        return arrRec;
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
            //   FfNofileDemandVsCollectionApproDet.info("Error getLinkedCompIds: " + e.getMessage(), e);

        }
    }

    private String getCompanyDescription(String companyCode) {

        String companyName = "";
        try {
            if (companyCode != null && !companyCode.isEmpty()) {
                CompanyRecord companyRec = new CompanyRecord(da.getRecord("COMPANY", companyCode));
                companyName = companyRec.getCompanyName().get(0).getValue();

                String[] compNamePart = companyName.split("-");
                companyName = compNamePart[0];

                return companyName;
            }
        } catch (Exception e) {
            e.getMessage();
        }
        return companyName;
    }

    private void initialiseCompanyInfo(String  branch) {

        try {
            CompanyRecord companyObj = new CompanyRecord(da.getRecord("COMPANY", branch));
            finMnemonic = companyObj.getFinancialMne().getValue();
            branchState = companyObj.getLocalRefField("FF.STATE").getValue();
            branchCode = branch;
            branchName = getCompanyDescription(branch);
            zoneName = getCompanyDescription(companyObj.getLocalRefField("FF.ZONE").getValue());
            regionName = getCompanyDescription(companyObj.getLocalRefField("FF.REGION").getValue());
            divisionName = getCompanyDescription(companyObj.getLocalRefField("FF.DIVISION").getValue());
            clusterName = getCompanyDescription(companyObj.getLocalRefField("FF.CLUSTER").getValue());

        } catch (Exception e) {
            e.getMessage();
        }

    }

    public void getAaArrAccountDetails(String legAcctNo) {

        try {
            AccountRecord accRec = new AccountRecord(da.getRecord(finMnemonic, "ACCOUNT", "", legAcctNo));        
            groupCode = accRec.getLocalRefField("FF.GROUP").getValue();          
            centerCode = accRec.getLocalRefField("FF.CENTRE").getValue();
            cycleNumber = accRec.getLocalRefField("FF.LOAN.CYCLE").getValue();
            purposeOfLoan = accRec.getLocalRefField("FF.LOAN.PURP").getValue();
            getCentreName(centerCode);
            getGroupName(groupCode);


        } catch (Exception e) {
            e.getMessage();
        }
    }   

    private void getCentreName(String centerCode) {
        try { 

            EbFfCentreDetailRecord ebCentreDetail = new EbFfCentreDetailRecord(da.getRecord("EB.FF.CENTRE.DETAIL", centerCode));           
            centerName = ebCentreDetail.getCenterName().getValue();
            String currentRo = ebCentreDetail.getCurrentRo().getValue();
            if (currentRo != null && !currentRo.isEmpty()) {
                getEbFfRoUserDets(currentRo);
            }
            String branchManager = ebCentreDetail.getBranchManager().getValue();
            if (branchManager != null && !branchManager.isEmpty()) {
                getUserDets(branchManager);
            }


        } catch (Exception e) { 
            e.getMessage(); 
        }

    }
    public void getEbFfRoUserDets(String ro) {
        try {
            EbFfRoUserRecord roUserRec = new EbFfRoUserRecord(da.getRecord("", "EB.FF.RO.USER", "", ro));
            relofficerName = roUserRec.getRoName().getValue();
            relMobileNumber = roUserRec.getRoMobileNumber().getValue();
        } catch (Exception e) {
            e.getMessage();
        }
    }

    private void getGroupName(String groupCode) { 
        try { 
            EbFfGroupsRecord ebFfGroups = new EbFfGroupsRecord(da.getRecord("EB.FF.GROUPS", groupCode));          
            groupName = ebFfGroups.getGroupName().getValue();

        } catch (Exception e) {
            e.getMessage(); 
        }

    }

    public void getArrangementDetails(Contract contract, String arrId) {
        try {
            AaArrangementRecord arrRec = contract.getContract();

            loanNumber = arrId;
            customerNumber = arrRec.getCustomer().get(0).getCustomer().getValue();
            productName = arrRec.getProduct().get(0).getProduct().getValue();

            stDt = arrRec.getStartDate().getValue();
            String origDate = arrRec.getOrigContractDate().getValue();

            if (origDate != null && !origDate.isEmpty()) {
                loanDate = origDate; 
            } else {
                loanDate = stDt;
            }

        } catch (Exception e) {
            e.getMessage();
        }
    }

    public void getCustomerDetails(String customerNumber,String arrId) {
        try {

            CustomerRecord cusRec = new CustomerRecord(da.getRecord("CUSTOMER", customerNumber));
            String first = cusRec.getName1().stream().map(TField::getValue).findFirst().orElse("");

            String second = cusRec.getName2().stream().map(TField::getValue).findFirst().orElse("");

            String family = cusRec.getFamilyName().getValue();

            // customerName = String.join(" ", first, second, family).trim();
            customerName = getGivenName(first,second, family);

            AaArrangementRecord aaArrRec = getArrangement(arrId); 
            customerNumber = cusRec.getMnemonic().getValue();               
            legAcctNo = aaArrRec.getLinkedAppl().get(0).getLinkedApplId().getValue();
            if (aaArrRec.getOrigContractDate().getValue() != null && !aaArrRec.getOrigContractDate().getValue().isEmpty()) {
                legacy = true;
            }   
            if(legacy) {
                AccountRecord accRec = new AccountRecord(da.getRecord(finMnemonic, "ACCOUNT", "", legAcctNo));
                for (AltAcctTypeClass altType : accRec.getAltAcctType()) {
                    if (altType.getAltAcctType().getValue().equals("LEGACY")) {
                        leagacyAccountNumber = altType.getAltAcctId().getValue();
                    }
                }
            }


        } catch (Exception e) {
            e.getMessage();

        }
    }


    private String getGivenName(String first, String second, String family) {

        StringBuilder sb = new StringBuilder();
        sb.append(first);
        if (!second.equals("")) {
            sb.append(" ");
            sb.append(second);
        }
        if (!family.equals("")) {
            sb.append(" ");
            sb.append(family);
        }

        return sb.toString();
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

    public void getAaArrTermAmountDetails(Contract contract) {
        try {
            AaPrdDesTermAmountRecord aaArrTermAmtRec = new AaPrdDesTermAmountRecord(
                    contract.getConditionForProperty("COMMITMENT"));
            if (!aaArrTermAmtRec.toString().isEmpty()) {
                loanAmount = aaArrTermAmtRec.getAmount().getValue();

            }
        } catch (Exception e) {
            e.getMessage();

        }
    }



    public void getUserDets(String userId) {
        try {
            UserRecord userRec = new UserRecord(da.getRecord("USER", userId));
            branchManagerName = userRec.getUserName().getValue();
            branchManagerMobileNumber = userRec.getLocalRefField("FF.MOBILE.NO").getValue();
        } catch (Exception e) {
            e.getMessage();
        }
    }

    // ***************************************************************************************************

    public String formDpId(String arrId, String date) {
        LocalDate parsedDate = LocalDate.parse(date, DateTimeFormatter.ofPattern("yyyyMMdd"));
        String monthText = parsedDate.format(DateTimeFormatter.ofPattern("MMM")).toUpperCase();    
        String year = String.valueOf(parsedDate.getYear());

        return arrId + "-" + monthText + year;
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
                curDpd = loanDpdDate.getCurDpd().getValue();   
                if (curDpd != null && !curDpd.isEmpty()) {
                    int dpd = Integer.parseInt(curDpd);    
                    if (dpd > 0) {
                        isOverdue = true;
                        break;
                    }
                }
            }

            if (isOverdue) {
                if(!curDpdList.contains(arrId)) {
                    curDpdList.add(arrId);   
                    Contract overdueContract = new Contract(this);
                    overdueContract.setContractId(arrId);
                    getOverDueBalanceDetails(overdueContract);
                }
            }  

            overdueDemand = String.format("%.2f", overdueportfolioOustandingAmtDbl);
        } catch (Exception e) {
            e.getMessage();
        }
    }

    public void getOverDueBalanceDetails(Contract overdueContract) {

        try {    
            arrDueAccBal = getOverDueBalance(overdueContract, FFALLOVRDUEDMND , TRADE);
            double overdueportfolioOustandingDbl = Math.abs(Double.parseDouble(arrDueAccBal));
            overdueportfolioOustandingAmtDbl += overdueportfolioOustandingDbl;

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

    //   ************************************************************************************************************************  

    private void getEbFfLoanDetails(String arrId) {

        try {

            EbFfLoanDetailsRecord ebFfloanRec =new EbFfLoanDetailsRecord(da.getRecord("EB.FF.LOAN.DETAILS", arrId));
            double totalCashCarry = 0.0;
            LocalDate today = LocalDate.parse(todayDate, formatter);

            for (int i = 0; i < ebFfloanRec.getCcProduct().size(); i++) {
                String ccAmt =ebFfloanRec.getCcProduct(i).getCcAmount().getValue();    
                String ccDate =ebFfloanRec.getCcProduct(i).getCcDate().getValue();

                if (ccAmt == null || ccAmt.isEmpty()
                        || ccDate == null || ccDate.isEmpty()) {
                    continue;
                }

                LocalDate ccDt = LocalDate.parse(ccDate, formatter);

                if (!ccDt.isAfter(today)) {
                    totalCashCarry += Double.parseDouble(ccAmt);

                }
            }

            demandCashCarry = String.format("%.2f", totalCashCarry);
        } catch (Exception e) {

        }
    }

    //***********************************************************************************************************************************

    private void getCollections(String arrId) {

        try {

            AaActivityHistoryRecord aaActHisRec = new AaActivityHistoryRecord(da.getRecord(finMnemonic, "AA.ACTIVITY.HISTORY", "", arrId));

            List<ActivityRefClass> activityRefs = getActivityRefs(aaActHisRec);

            advanceAmtDbl = calculateAdvanceAmount(activityRefs);       
            advanceAmt = String.format("%.2f",advanceAmtDbl);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private List<ActivityRefClass> getActivityRefs(AaActivityHistoryRecord aaActHisRec) {

        List<ActivityRefClass> activityRefs = new ArrayList<>();

        try {

            String fromDate = (startDate != null && !startDate.isEmpty()) ? startDate : todayDate;
            String toDate = (endDate != null && !endDate.isEmpty()) ? endDate : todayDate;

            LocalDate fromDt = LocalDate.parse(fromDate, formatter);
            LocalDate toDt = LocalDate.parse(toDate, formatter);

            for (EffectiveDateClass effDateCls : aaActHisRec.getEffectiveDate()) {

                for (ActivityRefClass actRefCls : effDateCls.getActivityRef()) {

                    String systemDate = actRefCls.getSystemDate().getValue();

                    if (systemDate == null || systemDate.isEmpty()) {
                        continue;
                    }

                    LocalDate sysDt = LocalDate.parse(systemDate, formatter);

                    if (!sysDt.isBefore(fromDt) && !sysDt.isAfter(toDt)) {
                        activityRefs.add(actRefCls);
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return activityRefs;
    }

    private double calculateAdvanceAmount(List<ActivityRefClass> activityRefs) {   
        double advanceAmt = 0.0;

        try {
            for (ActivityRefClass actRefCls : activityRefs) {

                if ("LENDING-CREDIT-ARRANGEMENT".equals(actRefCls.getActivity().getValue())) {
                    String activityAmt = actRefCls.getActivityAmt().getValue();
                    if (activityAmt != null && !activityAmt.isEmpty()) {
                        advanceAmt += Double.parseDouble(activityAmt);
                    }
                }
            }
        } catch (NumberFormatException e) {

        }

        return advanceAmt;
    }
    //************************************************************************************************************************************
    
    private double getForeclosureWriteOffAmount(String arrId) {
        
        double foreclosureWriteOffAmt = 0.0;
     
        try {
     
     
            Set<String> foreclosureProperty = new HashSet<>();
            foreclosureProperty.addAll(overdueProps);
            foreclosureProperty.addAll(currentProps);
            foreclosureProperty.addAll(foreclosureProps);
     
            String[] dateRange = getDateRange();
            String fromDate = dateRange[0];
            String toDate = dateRange[1];
     
            AaActivityBalancesRecord actBalRec =new AaActivityBalancesRecord(da.getRecord("AA.ACTIVITY.BALANCES", arrId));
     
            for (com.temenos.t24.api.records.aaactivitybalances.ActivityRefClass activityRef: actBalRec.getActivityRef()) {
     
                String activityName = activityRef.getActivity().getValue();
                String activityDate = activityRef.getActivityDate().getValue();
     
                if (FORECLOSURE_ACTIVITY_LIST.contains(activityName) && activityDate.compareTo(fromDate) >= 0 && activityDate.compareTo(toDate) <= 0) {
     
                    for (PropertyClass property : activityRef.getProperty()) {
     
                        String propertyName =
                                property.getProperty().getValue();
     
                        if (propertyName != null && !propertyName.isEmpty()) {
     
                            propertyName = propertyName.toUpperCase();
     
                            if (propertyName.contains(".")) {
                                propertyName = propertyName.substring(
                                        propertyName.indexOf('.') + 1);
                            }
     
                            if (foreclosureProperty.contains(propertyName)) {     
                                String propertyAmt =property.getPropertyAmt().getValue();    
                                if (propertyAmt != null && !propertyAmt.isEmpty()) {
                                    foreclosureWriteOffAmt +=Math.abs(Double.parseDouble(propertyAmt));
                                }
                            }
                        }
                    }
                }
            }
     
        } catch (Exception e) {
            e.printStackTrace();
        }
     
        return foreclosureWriteOffAmt;
    }
    
    //*************************************************************************************************************************************

    private double getActivityBalanceAmount(String arrId, Set<String> propertyList) {
        double activityAmt = 0.0;

        try {


            String reportDate = (endDate != null && !endDate.isEmpty())? endDate: todayDate;

            AaActivityBalancesRecord actBalRec =new AaActivityBalancesRecord(da.getRecord("AA.ACTIVITY.BALANCES", arrId));

            for (com.temenos.t24.api.records.aaactivitybalances.ActivityRefClass activityRef : actBalRec.getActivityRef()) {
                String activityName = activityRef.getActivity().getValue();
                String activityDate = activityRef.getActivityDate().getValue();

                if (activityName.equals(APPLY_PR_COLLECTION) || activityName.equals(SETTLE_PR_COLLECTION) && reportDate.equals(activityDate)) {
                    for (PropertyClass property : activityRef.getProperty()) {
                        String propertyName =property.getProperty().getValue().toUpperCase();

                        if (propertyName.contains(".")) {
                            propertyName = propertyName.substring(propertyName.indexOf('.') + 1);

                        }

                        if (propertyList.contains(propertyName)) {

                            String amt =property.getPropertyAmt().getValue();

                            if (amt != null && !amt.isEmpty()) {
                                activityAmt += Math.abs(Double.parseDouble(amt));
                            }
                        }
                    }
                }        
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return activityAmt;
    }

    private double getOverduePaymentAmount(String arrId) {
        double paymentAmt = 0.0;

        try {

            LocalDate fromDt = LocalDate.parse((startDate != null && !startDate.isEmpty()) ? startDate : todayDate,formatter);
            LocalDate toDt = LocalDate.parse((endDate != null && !endDate.isEmpty()) ? endDate : todayDate,formatter);

            EbFfLoanPaymentHisRecord paymentRec =new EbFfLoanPaymentHisRecord(da.getRecord("EB.FF.LOAN.PAYMENT.HIS", arrId));
            for (DemandDateClass demand : paymentRec.getDemandDate()) {
                String transType = demand.getTransType().getValue();
                if ((transType.equalsIgnoreCase("PRINCIPAL")|| transType.equalsIgnoreCase("INTEREST")|| transType.equalsIgnoreCase("OVERDUE INTEREST"))) {
                    LocalDate demandDate =LocalDate.parse(demand.getDemandDate().getValue(), formatter);
                    LocalDate paymentDate =LocalDate.parse(demand.getTransDate().getValue(), formatter);

                    if (!paymentDate.isBefore(fromDt) && !paymentDate.isAfter(toDt)) {
                        if (demandDate.isBefore(paymentDate)) {
                            String amt = demand.getPymtAmt().getValue();
                            if (amt != null && !amt.isEmpty()) {
                                paymentAmt += Math.abs(Double.parseDouble(amt));
                            }
                        }
                    }}
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return paymentAmt;
    }

    private double getCurrentPaymentAmount(String arrId) {

        double paymentAmt = 0.0;

        try {

            LocalDate fromDt = LocalDate.parse((startDate != null && !startDate.isEmpty()) ? startDate : todayDate,formatter);
            LocalDate toDt = LocalDate.parse((endDate != null && !endDate.isEmpty()) ? endDate : todayDate,formatter);

            EbFfLoanPaymentHisRecord paymentRec =new EbFfLoanPaymentHisRecord(da.getRecord("EB.FF.LOAN.PAYMENT.HIS", arrId));

            for (DemandDateClass demand : paymentRec.getDemandDate()) {

                String transType = demand.getTransType().getValue();

                if(transType.equalsIgnoreCase("PRINCIPAL")|| transType.equalsIgnoreCase("INTEREST")) {
                    LocalDate demandDate =LocalDate.parse(demand.getDemandDate().getValue(), formatter);
                    LocalDate paymentDate =LocalDate.parse(demand.getTransDate().getValue(), formatter);

                    if (!paymentDate.isBefore(fromDt) && !paymentDate.isAfter(toDt)) {
                        if (demandDate.equals(paymentDate)) {
                            String amt = demand.getPymtAmt().getValue();

                            if (amt != null && !amt.isEmpty()) {
                                paymentAmt += Math.abs(Double.parseDouble(amt));
                            }
                        }
                    }}
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return paymentAmt;
    }
    
    
    private Set<String> overdueProps = new HashSet<>(Arrays.asList(
            "SM0ACCOUNT",
            "SM1ACCOUNT",
            "SM2ACCOUNT",
            "NPAACCOUNT",
            "SM0ACCOUNTCUST",
            "SM1ACCOUNTCUST",
            "SM2ACCOUNTCUST",
            "NPAACCOUNTCUST",
            "SM0PRINTEREST",
            "SM1PRINTEREST",
            "SM2PRINTEREST",
            "NPAPRINTEREST",
            "SM0PRINTERESTCUST",
            "SM1PRINTERESTCUST",
            "SM2PRINTERESTCUST",
            "NPAPRINTERESTCUST"
    ));
     
    private Set<String> currentProps = new HashSet<>(Arrays.asList(
            "DUEACCOUNT",
            "DUEACCOUNTCUST",
            "DUEPRINTEREST",
            "DUEPRINTERESTCUST"
    ));
     
    private Set<String> foreclosureProps = new HashSet<>(Arrays.asList(
            "CURACCOUNT",
            "CURACCOUNTCUST",
            "ACCPRINTEREST",
            "ACCPRINTERESTCUST"
    ));
     


    private double getCollectionAgainstOverdueDemand(String arrId) {

        return getActivityBalanceAmount(arrId, overdueProps) + getOverduePaymentAmount(arrId);

        //  return getActivityBalanceAmount(arrId, overdueProps);
    }

    private double getCollectionAgainstCurrentDemand(String arrId) {

       return getActivityBalanceAmount(arrId, currentProps ) + getCurrentPaymentAmount(arrId);

     //     return getActivityBalanceAmount(arrId, currentProps );
        
    }   
        
   

    //   *****************************************************************************************************************************************  

    private double getTotalDemand(String arrId) {

        if (legacy) {
            return getHistoryTotalDemand(arrId);
        } else {
            return getCollectionTotalDemand(arrId);
        }
    }

    
    private double getHistoryTotalDemand(String arrId) {       
        double amount = 0.0;

        try {

            String historyId = arrId + "-" + stDt + ".01";

            EbFfCollectionDetsHistoryRecord hist =new EbFfCollectionDetsHistoryRecord(da.getRecord("EB.FF.COLLECTION.DETS.HISTORY", historyId));

            List<TField> due = hist.getDueDate();
            List<TField> prin = hist.getPrincipalAmt();
            List<TField> intr = hist.getInterestAmt();

            for (int i = 0; i < due.size(); i++) {
                
                if (due.get(i).getValue().compareTo(todayDate) <= 0) {

                    double principal = Double.parseDouble(prin.get(i).getValue());
                    double interest = Double.parseDouble(intr.get(i).getValue());

                    amount += principal + interest;
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return amount;
    }

   
    private double getCollectionTotalDemand(String arrId) {

        double amount = 0.0;

        try {

            EbFfCollectionDetsRecord collRec =new EbFfCollectionDetsRecord(da.getRecord("EB.FF.COLLECTION.DETS", arrId));

            List<TField> dueDate = collRec.getDueDate();
            List<TField> principal = collRec.getPrincipalAmt();
            List<TField> interest = collRec.getInterestAmt();
                  
            String dueDate1 = dueDate.isEmpty() ? "" : dueDate.get(0).getValue();    
            for (int i = 0; i < dueDate.size(); i++) {

                String due = dueDate.get(i).getValue();
             //   if (due.compareTo(todayDate) <= 0 && !due.equals(dueDate1)) {
                    if (due.compareTo(todayDate) <= 0 && !due.equals(dueDate1)) {

                    double prinAmt = principal.get(i).getValue().isEmpty()? 0.0: Double.parseDouble(principal.get(i).getValue());
                    double intAmt = interest.get(i).getValue().isEmpty()? 0.0: Double.parseDouble(interest.get(i).getValue());

                    amount += (prinAmt + intAmt);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return amount;
    }

  
    //  *****************************************************************************************************************************

    private double getCurrentDemand(String arrId) {
        billRecordFound = false;
     
        double demand = getBillAmount(arrId);
     
        if (!billRecordFound) {
     
            if (legacy) {
                demand = getHistoryAmount(arrId);
            } else {
                demand = getCollectionAmount(arrId);
            }
        }
     
        return demand;
    }

    private double getBillAmount(String arrId) {

        double amount = 0.0;

        try {
            String[] dateRange = getDateRange();
            String fromDate = dateRange[0];
            String toDate = dateRange[1];
           
            AaAccountDetailsRecord accRec =new AaAccountDetailsRecord(da.getRecord("AA.ACCOUNT.DETAILS", arrId));

            for (BillPayDateClass billPay : accRec.getBillPayDate()) {

                for (BillIdClass bill : billPay.getBillId()) {

                    if ("INSTALLMENT".equalsIgnoreCase(bill.getBillType().getValue())&& "DUE".equalsIgnoreCase(bill.getPayMethod().getValue())) {

                        String billDate = bill.getBillDate().getValue();

                        if (billDate.compareTo(fromDate) >= 0 && billDate.compareTo(toDate) <= 0) {

                            AaBillDetailsRecord billRec =new AaBillDetailsRecord(da.getRecord("AA.BILL.DETAILS",bill.getBillId().getValue()));
                            boolean validProperty = false;
                            for (com.temenos.t24.api.records.aabilldetails.PropertyClass prop : billRec.getProperty()) {

                                String property = prop.getProperty().getValue();

                                if ("ACCOUNT".equalsIgnoreCase(property) || "PRINTEREST".equalsIgnoreCase(property)) {
                                    validProperty = true;
                                }
                            }
                        
                            if(validProperty) {
                                billRecordFound = true;
                                String amt = billRec.getOrTotalAmount().getValue();

                                if (amt != null && !amt.isEmpty()) {
                                    amount += Double.parseDouble(amt);

                                }
                            }
                        }
                    }

                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return amount;
    }


    private double getHistoryAmount(String arrId) {

        double amount = 0.0;

        try {
            String[] dateRange = getDateRange();
            String fromDate = dateRange[0];
            String toDate = dateRange[1];

            String historyId = arrId + "-" + stDt + ".01";

            EbFfCollectionDetsHistoryRecord hist =new EbFfCollectionDetsHistoryRecord(da.getRecord("EB.FF.COLLECTION.DETS.HISTORY", historyId));

            List<TField> due = hist.getDueDate();
            List<TField> principal = hist.getPrincipalAmt();
            List<TField> interest = hist.getInterestAmt();

            for (int i = 0; i < due.size(); i++) {

                String dueDate = due.get(i).getValue();

                if (dueDate.compareTo(fromDate) >= 0
                        && dueDate.compareTo(toDate) <= 0) {

                    double prinAmt = principal.get(i).getValue().isEmpty()? 0.0: Double.parseDouble(principal.get(i).getValue());
                    double intAmt = interest.get(i).getValue().isEmpty()? 0.0: Double.parseDouble(interest.get(i).getValue());

                    amount += (prinAmt + intAmt);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return amount;
    }
      
    private double getCollectionAmount(String arrId) {        
        double amount = 0.0;
     
        try {
     
            String[] dateRange = getDateRange();
            String fromDate = dateRange[0];
            String toDate = dateRange[1];
     
            EbFfCollectionDetsRecord collRec =new EbFfCollectionDetsRecord(da.getRecord("EB.FF.COLLECTION.DETS", arrId));
     
            List<TField> dueDate = collRec.getDueDate();
            List<TField> principal = collRec.getPrincipalAmt();
            List<TField> interest = collRec.getInterestAmt();
            
            String dueDate1 = dueDate.get(0).getValue();
     
            for (int i = 0; i < dueDate.size(); i++) {
     
                String due = dueDate.get(i).getValue();
                
                if (due.compareTo(fromDate) >= 0 && due.compareTo(toDate) <= 0) {

                    if (!(i == 0 && fromDate.equals(dueDate1) && toDate.equals(dueDate1))) {
     
                    double prinAmt = principal.get(i).getValue().isEmpty()? 0.0: Double.parseDouble(principal.get(i).getValue());    
                    double intAmt = interest.get(i).getValue().isEmpty()? 0.0: Double.parseDouble(interest.get(i).getValue());    
                    amount += prinAmt + intAmt;
                }
            }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
     
        return amount;
    } 
      
    private String[] getDateRange() {
        
        String fromDate = (startDate == null || startDate.isEmpty())? todayDate: startDate;   
        String toDate = (endDate == null || endDate.isEmpty())? todayDate: endDate;
     
        return new String[] { fromDate, toDate };
    }
    
    //********************************************************************************************************************************************************

    public void writeToFile(List<String> data, String filePath) {
        try {

            File file = new File(filePath);
            file.getParentFile().mkdirs();
            boolean fileExists = file.exists();

            try (FileWriter writer = new FileWriter(file, true)) {
                if (data == null || data.isEmpty()) {
                    writer.write("No records matched the selection criteria" + System.lineSeparator());
                } else {
                    if (!fileExists) {
                        String header = String.join("," ,  "ZoneName " , " RegionName " , " DivisionName " , " ClusterName " , 
                                "BranchName" , "BranchDistrict" , "BranchState" , "BranchCode" , 
                                "CenterName" , "CenterCode" , "GroupName" , "GroupCode" ,  "CustomerName" ,
                                "CustomerNumber" , "LoanAccountNumber" , "LeagacyAccountNumber" , "LoanDate" , "LoanAmount" , "ProductName"
                                , "cycle" , "RelationshipOfficerName" , "RelationshipOfficerMobileNumber" , "BranchManagerName" ,
                                "BranchManagerMobileNumber" , "OverdueDemand(Principal&Interest)" , "Demand(Principal&Interest)" , "DemandCashCarry" , "TotalDemand"
                                , "CollectionAgainstOverdueDemand" , "CollectionAgainstCurrentDemand" , "AdvanceAmt " , "Foreclosure/Write-offCollection"
                                ,  "TotalCollection" , "PendingCollection");

                        writer.write(header + System.lineSeparator());
                    }
                }
                for (String line : data) {
                    writer.write(line + System.lineSeparator());
                }
            }
        } catch (Exception e) {
        }
    }

}

