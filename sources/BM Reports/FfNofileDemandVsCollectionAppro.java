package com.temenos.fusion;


import java.io.File;
import java.io.FileWriter;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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
import com.temenos.t24.api.records.account.AccountRecord;
import com.temenos.t24.api.records.company.CompanyRecord;
import com.temenos.t24.api.records.companyconsol.CompanyConsolRecord;
import com.temenos.t24.api.records.ebffcentredetail.EbFfCentreDetailRecord;
import com.temenos.t24.api.records.ebffcollectiondets.EbFfCollectionDetsRecord;
import com.temenos.t24.api.records.ebffcollectiondetshistory.EbFfCollectionDetsHistoryRecord;
import com.temenos.t24.api.records.ebffloandetails.EbFfLoanDetailsRecord;
import com.temenos.t24.api.records.ebffloandpd.DateClass;
import com.temenos.t24.api.records.ebffloandpd.EbFfLoanDpdRecord;
import com.temenos.t24.api.records.ebffloanpaymenthis.DemandDateClass;
import com.temenos.t24.api.records.ebffloanpaymenthis.EbFfLoanPaymentHisRecord;
import com.temenos.t24.api.records.ebffparameter.EbFfParameterRecord;
import com.temenos.t24.api.records.ebffparameter.ParamDescClass;
import com.temenos.t24.api.records.ebffrouser.EbFfRoUserRecord;
import com.temenos.t24.api.records.fundstransfer.FundsTransferRecord;
import com.temenos.t24.api.system.DataAccess;
import com.temenos.t24.api.system.Date;
import com.temenos.t24.api.system.Session;
/*-----------------------------------------------------------------------------
 * @author Kavya N
 * Date Created:22 Dec 2025
 * 
 * EB.API : E.FF.NOFILE.DEM.COLL.APPRO.SUM
 * SS : NOFILE.DEM.COLL.APPRO.SUM
 * ENQUIRY:
 * FF.NOFILE.DEM.COLL.APPRO.SUMMARY.DATE
 * FF.NOFILE.DEM.COLL.APPRO.SUMMARY.RO  
 * FF.NOFILE.DEM.COLL.APPRO.SUMMARY.CENTRENAME 
 * FF.NOFILE.DEM.COLL.APPRO.SUMMARY.LOANWISE
 * 
 * Description: Get Demand Vs Collection Approximate Summary BM Enquiry Report
 *------------------------------------------------------------------------------ 
 * Modification History :
 *-----------------------------------------------------------------------------
 * 09-Mar-2026   Comapany Console---Kavya N
 *-----------------------------------------------------------------------------
 */

public class FfNofileDemandVsCollectionAppro extends Enquiry {
 //    private static final FusionFileLogger loggerFile = FusionFileLogger.getLogger(FfNofileDemandVsCollectionAppro.class);
   //  private static final Logger loggerFile = Logger.getLogger("FfNofileDemandVsCollectionAppro");
    DataAccess da = new DataAccess(this);
    Session ss = new Session(this);
    Set<String> arrList = new LinkedHashSet<>();
    List<String> returnValueList = new ArrayList<>();
    String paramValue = getParamValue("DEM.COLL.APP");
    public static final String FILE_NAME = "DemVsColAppropriateRep_Sum";
    public static final String AA_BILL_DETAILS = "AA.BILL.DETAILS";
    public static final String TRADE = "TRADE";
    public static final String FFTOTDEFAMT = "FFTOTDEFAMT";
    public static final String UNCACCOUNT = "UNCACCOUNT";
    public static final String FFALLOVRDUEDMND = "FFALLOVRDUEDMND";
    public static final String APPLY_PR_COLLECTION = "LENDING-APPLYPAYMENT-PR.COLLECTION";
    public static final String SETTLE_PR_COLLECTION = "LENDING-SETTLE-PR.COLLECTION";
    
    String arrNpaPrInterest ="";
    String arrDuePrInterest="";
    String arrS0PrInterest="";
    String arrS1PrInterest="";
    String arrS2PrInterest="";
    List<String> curDpdList = new ArrayList<>();
    DateTimeFormatter outDateFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
    private Map<String, EbFfLoanDpdRecord> loanDpd = new HashMap<>();    
    public static final String AA_ARRANGEMENT = "AA.ARRANGEMENT";
    private Map<String, AaArrangementRecord> arrangements = new HashMap<>();
    String selDate = "";
    String startDate = "";
    String endDate = "";
    List<String> finalArrList = new ArrayList<>();
    List<String> outvalues = new ArrayList<>();
    int roCount;
    int centreCount;
    String selRo = "";
    String selCentreName = "";
    Set<String> centreSet = new HashSet<>();
    Set<String> roSet = new HashSet<>();
    Set<String> filterValSet = new HashSet<>();
    String selBranch = "";
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
    Double overdueDemanddbl = 0.0;
    Double advanceAmtDbl = 0.0;
    int overdueCount = 0;
    String demandCashCarry = "";
    Date t24date = new Date(this);
    String date = "";
    String todayDate = "";
    String finMnemonic = "";
    String selDateOp = "";
    String roSelFld = "";
    String loanWiseSelFld = "";
    String selLoan = "";
    String billVal = "";
    String foreClosure = "";
    int loanCount = 0;
    Double totalDemandDbl = 0.0;
    String selCenterName = "";
    boolean onlyDateFilter = false;
    boolean dateErrFlag = false;
    boolean noRecErrFlag = false;
    double loanAccountDbl = 0.0;
    double prInterestDbl = 0.0;
    double uncAccountDbl = 0.0;
    double totalCollectiondbl = 0.0;
    double totalCollectiondb = 0.0;
    List<String> loanDpdList = new ArrayList<>();
    String curDpd = "";
    List<String> billList = new ArrayList<>();
    String loanAccount = "";
    String prInterest = "";
    String enquiryName = "";
    String selectionFilter = "";
    String startDateArrAcc = "";
    String arrAgeStatus = "";
    String ro = "";
    String centre = "";
    String companyIds = "";
    String selLoanCount = "";
    Double demandDbl = 0.0;
    String collectionAgainstOverdue = "";
    String selFilterField = "";
    boolean dateGrp = false;
    String aaStartDate = "";
    String loanwise = "";
    String centreName="";
    String roName="";
    String accNum="";
    Double pendingCollectionDbl=0.0;
    double collectionAgainstCurrentDemand=0.0;
    String foreclosure = "";
    double collectionAgainstOverdueDemand = 0.0;
    double prCollectionAmt =0.0;
    double foreclosureAmt=0.0;
    String totalCollection="";
    String totalDemand="";
    DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HHmmss");
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
    public static final String DATE_RANGE_ERR = "EB-FF.BM.FUTURE.MAX.DT.RANGE";
    public static final String DATE_FILTER_ERR = "EB-FF.BM.DATE.FILTER.RANGE";
    public static final String NO_REC_ERR = "EB-NO.REC.SELECT";
    String dateRangeVal = "";
    String daetFilterVal = "";
    boolean dateRangeErrFlag = false;
    boolean dateFilterErrFlag = false;
    double pendingCollection = 0.0;
    String selGroup="";
    String selUser="";
    String effDate="";
    double overdueCollectionAmt=0.0;
    double overdueportfolioOustandingDbl= 0.0;
    boolean legacy = false;
    double pending=0.0;
    String stDt="";
    boolean branchSelection = false;
    String coCode = "";
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
            Session session = new Session(this);
            todayDate = session.getCurrentVariable("!TODAY");
            Contract contract = new Contract(this);           
            getFilterCriteriaDets(filterCriteria);           
            processDateValidation(startDate, endDate);

            if (dateFilterErrFlag) {
                throw new T24CoreException(covertParamValue(daetFilterVal), DATE_FILTER_ERR);
            }
            if (dateRangeErrFlag) {
                throw new T24CoreException(covertParamValue(dateRangeVal), DATE_RANGE_ERR);
            }

            Set<String> preFinalSet = new LinkedHashSet<>(da.selectRecords(finMnemonic, AA_ARRANGEMENT, "","WITH CO.CODE EQ " + companyIds));
          //  Set<String> preFinalSet = new LinkedHashSet<>(da.selectRecords("", AA_ARRANGEMENT, "","WITH @ID EQ AA25305GG7HW AND CO.CODE EQ " + companyIds));
            //  Set<String> preFinalSet = new LinkedHashSet<>(da.selectRecords(finMnemonic, AA_ARRANGEMENT, "","WITH @ID EQ AA253039X7TM AND CO.CODE EQ " + companyIds));          

            Map<String, String> finalArrMap =getFinalArrList(preFinalSet);
            Map<String, Double> totalDemand = new HashMap<>();
            Map<String, Double> advanceAmt = new HashMap<>();
            Map<String, Double> foreClosure  = new HashMap<>();
            Map<String, Double> demand = new HashMap<>();
            Map<String, Double> overdueDemand = new HashMap<>();
            Map<String, Integer> accountCount = new HashMap<>();
            Map<String, Double> collectionAgainstOverdue = new HashMap<>();
            Map<String, Double> collectionAgainstCurrent = new HashMap<>();
            Map<String, Double> totalCollection = new HashMap<>();

            for (Map.Entry<String, String> entry: finalArrMap.entrySet()) {
 

                String arrId = entry.getKey();
                effDate = entry.getValue();

                ro = "";
                centre = "";
                loanwise = "";
                resetVariables();
                contract.setContractId(arrId);
                getAaArrangementDets(arrId);
                loanwise = arrId;    

                getOverdueStat(arrId);
                getEbFfLoanDetails(arrId);  
                getCollections( arrId) ;
                
                collectionAgainstOverdueDemand =getCollectionAgainstOverdueDemand(arrId);              
                collectionAgainstCurrentDemand =getCollectionAgainstCurrentDemand(arrId); 
                foreclosureAmt = getForeclosureWriteOffAmount(arrId);
              
                demandDbl = getCurrentDemand(arrId);
                totalDemandDbl =  getTotalDemand(arrId);
                             
                totalCollectiondbl = foreclosureAmt+ advanceAmtDbl+ collectionAgainstCurrentDemand+ collectionAgainstOverdueDemand;               
                  
                String groupValue = getGroupBasedValue(arrId, contract,effDate);

                boolean selectionCheck = chkSelectionBased();

                
                if (selectionCheck) {                     
                    continue;
                }

                if (groupValue != null && !groupValue.isEmpty()) {
                    overdueDemand.put(groupValue,overdueDemand.getOrDefault(groupValue, 0.0) + overdueDemanddbl);
                    demand.put(groupValue, demand.getOrDefault(groupValue, 0.0) + demandDbl);
                    totalDemand.put(groupValue, totalDemand.getOrDefault(groupValue, 0.0) + totalDemandDbl);
                    advanceAmt.put(groupValue,advanceAmt.getOrDefault(groupValue, 0.0) + advanceAmtDbl);
                    foreClosure.put(groupValue,foreClosure.getOrDefault(groupValue, 0.0) + foreclosureAmt);
                    collectionAgainstOverdue.put(groupValue,collectionAgainstOverdue.getOrDefault(groupValue, 0.0) + collectionAgainstOverdueDemand);
                    collectionAgainstCurrent.put(groupValue,collectionAgainstCurrent.getOrDefault(groupValue, 0.0) + collectionAgainstCurrentDemand);
                    accountCount.put(groupValue, accountCount.getOrDefault(groupValue, 0) + 1);               
                    totalCollection.put(groupValue,totalCollection.getOrDefault(groupValue, 0.0) + totalCollectiondbl);
                
                }
            }
            List<String> sortedKeys = new ArrayList<>(accountCount.keySet());
            if (dateGrp) {

                Collections.sort(sortedKeys);
            }

            for (String currentGrp : sortedKeys) {
                List<String> row = new ArrayList<>();
                
                double totalDemandVal =totalDemand.getOrDefault(currentGrp, 0.0); 
                double totalCollectionVal =totalCollection.getOrDefault(currentGrp, 0.0);  
            
                
                double pendingCollectionVal = Math.abs(totalDemandVal - totalCollectionVal);
         
                row.add(zoneName);
                row.add(regionName);
                row.add(divisionName);
                row.add(clusterName);
                row.add(branchCode);
                row.add(branchName);
                row.add(currentGrp);
                row.add(String.format("%.2f", overdueDemand.get(currentGrp)));
                row.add(String.format("%.2f", demand.get(currentGrp)));
                row.add(demandCashCarry);
                row.add(String.format("%.2f", totalDemandVal));
                row.add(String.format("%.2f",collectionAgainstOverdue.get(currentGrp))); 
                row.add(String.format("%.2f",collectionAgainstCurrent.get(currentGrp)));               
                row.add(String.format("%.2f", advanceAmt.get(currentGrp)));
                row.add(String.format("%.2f",foreClosure.get(currentGrp)));
                row.add(String.format("%.2f", totalCollectionVal));
                row.add(String.format("%.2f", pendingCollectionVal));
                returnValueList.add(String.join("*", row));
                outvalues.add(String.join(",", row));

            }

            String fileParamId = "FF.BM.REPORT.EXTRACT";
            String fileParamName = "Path";
            String filePath = getEbFfParamRecDets(fileParamId, fileParamName);

            LocalDateTime currDtTime = LocalDateTime.now();
            String currDate = currDtTime.format(outDateFormatter);
            String currTime = currDtTime.format(timeFormatter);

            String outputPath = filePath + FILE_NAME + "_" + selGroup + "-WISE" + "_" + selBranch + selUser + "_"
                    + currDate + "_" + currTime + ".csv";

            if(returnValueList == null || returnValueList.isEmpty()) {             
                writeToFile(null, outputPath);
                noRecErrFlag = true;
            }else {

                writeToFile(outvalues, outputPath);
            }

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

    
    public boolean chkSelectionBased() {
        return (selRo != null && !selRo.isEmpty() && !selRo.equals(ro))
                || (selLoan != null && !selLoan.isEmpty() && !selLoan.equals(loanwise))
                || (selCenterName != null && !selCenterName.isEmpty() && !selCenterName.equals(centre));

    }

    public String getParamValue(String reportName) {
        String value = "";

        try {
            EbFfParameterRecord paramRec =
                    new EbFfParameterRecord(da.getRecord("EB.FF.PARAMETER", "FF.BM.REPORT.DATE.RANGE"));

            for (ParamDescClass param : paramRec.getParamDesc()) {
                if (param.getParamName().getValue().equals(reportName)) {
                    value = param.getParamValue().getValue();
                    break;
                }
            }
        } catch (Exception e) {
            e.getMessage();
        }
        return value;
    }

    public void getAaArrangementDets(String arrId) {
        legacy = false;
        try {
            AaArrangementRecord arrRec = new AaArrangementRecord(
                    da.getRecord(finMnemonic, AA_ARRANGEMENT, "", arrId));
            coCode = arrRec.getCoCodeRec().getValue();
            accNum = arrRec.getLinkedAppl().get(0).getLinkedApplId().getValue();
            stDt = arrRec.getStartDate().getValue();           
            if (arrRec.getOrigContractDate().getValue() != null && !arrRec.getOrigContractDate().getValue().isEmpty()) {
                legacy = true;
            }     
        } catch (Exception e) {
            e.getMessage();
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


    public void getAccountDets(String accNum) {

        try {
            AccountRecord accRec = new AccountRecord(da.getRecord(finMnemonic, "ACCOUNT", "", accNum));
            centre = accRec.getLocalRefField("FF.CENTRE").getValue();

            if (centre != null && !centre.isEmpty()) {
                getEbFfCentreDetails(centre);
            }
        } catch (Exception e) {
            e.getMessage();
        }
    }

    public void getEbFfCentreDetails(String centre) {

        try {
            EbFfCentreDetailRecord centreRec = new EbFfCentreDetailRecord(
                    da.getRecord("", "EB.FF.CENTRE.DETAIL", "", centre));
            centreName = centreRec.getCenterName().getValue();
            ro = centreRec.getCurrentRo().getValue();
            if (ro != null && !ro.isEmpty()) {
                getEbFfRoUserDets(ro);
            }
        } catch (Exception e) {
            e.getMessage();
        }
    }

    public void getEbFfRoUserDets(String ro) {
        try {
            EbFfRoUserRecord roUserRec = new EbFfRoUserRecord(da.getRecord("", "EB.FF.RO.USER", "", ro));
            roName = roUserRec.getRoName().getValue();
        } catch (Exception e) {
            e.getMessage();
        }
    }

    public String getGroupBasedValue(String arrId, Contract contract,String effDate) {
        String groupKey = "";
        switch (selGroup) {
        
        case "BRANCH":
            selFilterField = "Branch";
            branchSelection = true;
            groupKey = getCompanyDescription(coCode);
            break;
        case "DATE":
            selFilterField = "Date";
            groupKey = effDate;
            dateGrp = true;
            break;
        case "RO":
            selFilterField = "RO";
            getAccountDets(accNum);
            groupKey = roName;
            break;
        case "LOAN.WISE":          
            getAccountDets(accNum);
            groupKey = loanwise;
            break;
        case "CENTER":
            selFilterField = "CenterName";
            getAccountDets(accNum);
            groupKey = centre;
            break;

        default:
        }
        return groupKey;
    }

    public void getFilterCriteriaDets(List<FilterCriteria> filterCriteria) {
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

                case "GROUP.BY":
                    selGroup = value;
                    break;

                case "DATE.FROM":
                    startDate = value;
                    break;

                case "DATE.TO":
                    endDate = value;
                    break;

                case "RO":
                    selRo = value;
                    break;

                case "LOAN.WISE":
                    selLoan = value;                 
                    break;

                case "CENTER":
                    selCenterName = value;
                    break;

                default:
                }
            }
        } catch (Exception e) {

            e.getMessage();
        }
    }


    private void initialiseCompanyInfo(String companyId) {
        try {

            CompanyRecord companyObj = new CompanyRecord(da.getRecord("COMPANY", companyId));
            finMnemonic = companyObj.getFinancialMne().getValue();
            branchCode = companyId;
            branchName = getCompanyDescription(companyId);
            zoneName = getCompanyDescription(companyObj.getLocalRefField("FF.ZONE").getValue());
            regionName = getCompanyDescription(companyObj.getLocalRefField("FF.REGION").getValue());
            divisionName = getCompanyDescription(companyObj.getLocalRefField("FF.DIVISION").getValue());
            clusterName = getCompanyDescription(companyObj.getLocalRefField("FF.CLUSTER").getValue());

        } catch (Exception e) {
            e.getMessage();
        }

    }

    
    public String getCompanyDescription(String companyCode) {
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
            overdueDemanddbl += overdueportfolioOustandingDbl;
         //   overdueDemand = String.format("%.2f", overdueportfolioOustandingAmtDbl);
      
        } catch (Exception e) {
            e.getMessage();
        }
    }

    public void getOverDueBalanceDetails(Contract overdueContract) {

        try {    
            arrDueAccBal = getOverDueBalance(overdueContract, FFALLOVRDUEDMND , TRADE);
            overdueportfolioOustandingDbl = Math.abs(Double.parseDouble(arrDueAccBal));
          //  overdueportfolioOustandingAmtDbl += overdueportfolioOustandingDbl;

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
         //  foreclosureAmt = calculateForeclosureAmount(activityRefs);
     
           /* advanceAmt = String.format("%.2f",advanceAmtDbl);
            foreclosure = String.format("%.2f",foreclosureAmt);
            loggerFile.info("advanceAmt" + advanceAmt);
            loggerFile.info("foreclosure" + foreclosure);*/
     
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
    //**********************************************************************************************************************************************
    
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


            String reportDate = (endDate != null && !endDate.isEmpty())
                    ? endDate
                            : todayDate;

            AaActivityBalancesRecord actBalRec =new AaActivityBalancesRecord(da.getRecord("AA.ACTIVITY.BALANCES", arrId));

            for (com.temenos.t24.api.records.aaactivitybalances.ActivityRefClass activityRef : actBalRec.getActivityRef()) {
                String activityName = activityRef.getActivity().getValue();
                String activityDate = activityRef.getActivityDate().getValue();

               // if ("LENDING-APPLYPAYMENT-PR.COLLECTION".equals(activityName) && reportDate.equals(activityDate)) {
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

            LocalDate fromDt = LocalDate.parse((startDate != null && !startDate.isEmpty()) ? startDate : todayDate,
                            formatter);

            LocalDate toDt = LocalDate.parse(
                    (endDate != null && !endDate.isEmpty()) ? endDate : todayDate,
                            formatter);

            EbFfLoanPaymentHisRecord paymentRec =
                    new EbFfLoanPaymentHisRecord(
                            da.getRecord("EB.FF.LOAN.PAYMENT.HIS", arrId));

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

      //    return getActivityBalanceAmount(arrId, overdueProps);
    }

    private double getCollectionAgainstCurrentDemand(String arrId) {

        return getActivityBalanceAmount(arrId, currentProps ) + getCurrentPaymentAmount(arrId);

      //    return getActivityBalanceAmount(arrId, currentProps );
        
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
    
    //*************************************************************************************************************************************************** 

    private void resetVariables() {

        totalCollectiondb = 0.0;
        totalCollectiondbl = 0.0;     
        advanceAmtDbl = 0.0;
        overdueDemanddbl = 0.0;
        totalDemandDbl = 0.0;
        pendingCollectionDbl = 0.0;
        overdueCollectionAmt = 0.0;
        overdueportfolioOustandingDbl = 0.0;     
        collectionAgainstOverdueDemand = 0.0;
        prCollectionAmt = 0.0;
        foreclosureAmt = 0.0;
        demandDbl = 0.0;
        loanAccountDbl = 0.0;
        prInterestDbl = 0.0;
        uncAccountDbl = 0.0;

        demandCashCarry = "0.00";
        collectionAgainstCurrentDemand = 0.0;
        foreclosure = "0.00";
        totalCollection = ""; 
        totalDemand = "";
        loanAccount = "";
        prInterest = "";
        curDpd = "";   
        legacy = false;
    }
    
    public void writeToFile(List<String> data, String filePath) {

        try {

            File file = new File(filePath);
            if(file.getParentFile()!=null) {
                file.getParentFile().mkdirs();
            }
            boolean fileExists = file.exists();

            try (FileWriter writer = new FileWriter(file, true)) {
                if (data == null || data.isEmpty()) {
                    writer.write("No records matched the selection criteria" + System.lineSeparator());

                } else {
                    if (!fileExists) {
                        String header = String.join(",", "ZoneName", "RegionName", "DivisionName", "ClusterName",
                                "BranchCode", "BranchName", selFilterField, "OverdueDemand", "Demand", "DemandCashCarry",
                                "TotalDemand", "CollectionAgainstOverdueDemand", "CollectionAgainstCurrentDemand", "AdvanceAmt", "Foreclosure/Write-offCollection",
                                "TotalCollection" , "PendingCollection");



                        writer.write(header + System.lineSeparator());
                    }
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



