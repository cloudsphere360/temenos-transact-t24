package com.temenos.fusion;

import java.io.File;
import java.io.FileWriter;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.temenos.api.TField;
import com.temenos.api.exceptions.T24CoreException;
import com.temenos.t24.api.arrangement.accounting.Contract;
import com.temenos.t24.api.complex.aa.contractapi.BalanceMovement;
import com.temenos.t24.api.complex.eb.enquiryhook.EnquiryContext;
import com.temenos.t24.api.complex.eb.enquiryhook.FilterCriteria;
import com.temenos.t24.api.hook.system.Enquiry;
import com.temenos.t24.api.records.aaaccountdetails.AaAccountDetailsRecord;
import com.temenos.t24.api.records.aaactivityhistory.AaActivityHistoryRecord;
import com.temenos.t24.api.records.aaactivityhistory.ActivityRefClass;
import com.temenos.t24.api.records.aaactivityhistory.EffectiveDateClass;
import com.temenos.t24.api.records.aaarrangement.AaArrangementRecord;
import com.temenos.t24.api.records.aaproduct.AaProductRecord;
import com.temenos.t24.api.records.account.AccountRecord;
import com.temenos.t24.api.records.company.CompanyRecord;
import com.temenos.t24.api.records.companyconsol.CompanyConsolRecord;
import com.temenos.t24.api.records.customer.CustomerRecord;
import com.temenos.t24.api.records.ebffcentredetail.EbFfCentreDetailRecord;
import com.temenos.t24.api.records.ebffcustdpd.EbFfCustDpdRecord;
import com.temenos.t24.api.records.ebffloandetails.EbFfLoanDetailsRecord;
import com.temenos.t24.api.records.ebffloandetails.FfCurDodStsClass;
import com.temenos.t24.api.records.ebffloandetails.FmEntityNumberClass;
import com.temenos.t24.api.records.ebffloandpd.EbFfLoanDpdRecord;
import com.temenos.t24.api.records.ebffparameter.EbFfParameterRecord;
import com.temenos.t24.api.records.ebffparameter.ParamDescClass;
import com.temenos.t24.api.records.ebffrouser.EbFfRoUserRecord;
import com.temenos.t24.api.records.ebffvillage.EbFfVillageRecord;
import com.temenos.t24.api.system.DataAccess;
import com.temenos.t24.api.system.Session;
import com.temenos.t24.api.records.ebffloandpd.DateClass;

/*-----------------------------------------------------------------------------
 * @author Harshini Sakthivel
 * Date Created: 12-Dec-2025 
 * Attached as : Nofile Enquiry Routine
 * EB.API : FF.E.BM.OVERDUE.SM.RPT
 * Attached to : STANDARD.SELECTION > NOFILE.FF.BM.OVERDUE.SM.RPT 
 * Description: Branch Online Report generation -> OverDue Summary Report
 *------------------------------------------------------------------------------ 
 * Modification History : 
 * @author 
 * Date Created: 12-Dec-2025 
 *----------------------------------------------------------------------------- 
*12-Dec-2025   Development      Harshini Sakthivel
*-----------------------------------------------------------------------------
*11-Mar-2026   Remapping and Grouping Logic  Renuka M
*-----------------------------------------------------------------------------
*/
public class FfNofileBranchMangOverDueRptSum extends Enquiry {

    public static final String DATE_RANGE_ERR = "EB-FF.DATE.RANGE.GREATER";
    public static final String NO_REC_ERR = "EB-NO.REC.SELECT";
    DataAccess da = new DataAccess(this);
    Set<String> centreSet = new HashSet<>();
    Set<String> villageSet = new HashSet<>();
    Set<String> roSet = new HashSet<>();
    Set<String> productSet = new HashSet<>();
    List<String> finalArrIdList = new ArrayList<>();
    List<String> returnVal = new ArrayList<>();

    String finMnemonic = "";
    String mnemonic = "";
    String branchName = "";
    String branchCode = "";
    String zoneName = "";
    String regionName = "";
    String divisionName = "";
    String clusterName = "";
    String selDate = "";
    String selBranch = "";
    String selDateOp = "";
    String selMonth = "";
    String selRo = "";
    String selProduct = "";
    String selCenterName = "";
    String selVillage = "";
    String startDate = "";
    String endDate = "";
    String arrAgeStatus = "";
    long daysBtToday = 0;

    List<String> ffLoanDpdList = null;
    int numOfLoanOverDueCnt = 0;
    List<String> ffCusDpdList = null;
    int numOfCustOverDueCnt = 0;
    String aaArrDpdId = "";
    double principalDefault = 0.0;
    double interestdefault = 0.0;
    double totalDefault = 0.0;
    double outstanding = 0.0;
    String startDateArrAcc = "";
    List<String> dpdCntOf1to30list = null;
    List<String> aaArrIdListBsCus = null;
    List<String> dateBaseArrId = new ArrayList<>();
    String finalCntValue = "";
    List<String> dpdCntOf31to60list = null;
    List<String> dpdCntOf61to90list = null;
    List<String> dpdCntOf91to120list = null;
    List<String> dpdCntOf121to180list = null;
    List<String> dpdCntOf181to365list = null;
    List<String> dpdCntOf366list = null;
    List<String> aaAccDetIdList = null;
   

    private int loanDpd1 = 0;
    private int loanDpd2 = 0;
    private int loanDpd3 = 0;
    private int loanDpd4 = 0;
    private int loanDpd5 = 0;
    private int loanDpd6 = 0;
    private int loanDpd7 = 0;
    Set<String> finalCusList = new HashSet<>();
    Set<String> filterValSet = new HashSet<>();
    boolean onlyDateFilter = false;
    boolean dateErrFlag = false;
    boolean noRecErrFlag = false;
    String writeOff = "";
    String startDateAccDet = "";
    EbFfLoanDpdRecord loanDpdRec = null;
    int dateFrmLnDpdlist = 0;
    List<String> ffLoanDpdIdList = null;
    String enquiryName = "";
    String selectionFilter = "";
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
    String ro = "";
    String strtDate = "";
    String product = "";
    String centre = "";
    String village = "";
    public static final String FILE_NAME = "OverdueRep_Sum";
    List<String> outvalues = new ArrayList<>();
    String selFilterField = "";
    boolean dateGrp = false;

    int numOfCustDpd = 0;
    int loanDpdcnt = 0;
    DateTimeFormatter outDateFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
    DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HHmmss");
    String companyIds = "";
    AaActivityHistoryRecord aaActHisRec = null;
    List<EffectiveDateClass> effectiveDateList;
    List<ActivityRefClass> activityRefList;
    String activity = "";
    List<String> arrtActivityList = new ArrayList<>();
    private static final String REQ_TYPE_BOOKING = "TRADE";
    private static final String AA_ARRANGEMENT_RECORD = "AA.ARRANGEMENT";
    Map<String, Integer> defaultClientMap = new HashMap<>();
    Map<String, Integer> defaultLoanAcctMap = new HashMap<>();
    Map<String, Double> principalDefaultMap = new HashMap<>();
    Map<String, Double> interestdefaultMap = new HashMap<>();
    Map<String, Double> totalDefaultMap = new HashMap<>();
    Map<String, Double> outstandingMap = new HashMap<>();
    Map<String, Integer> loancnt1to30Map = new HashMap<>();
    Map<String, Integer> loanDpd31to60Map = new HashMap<>();
    Map<String, Integer> loanDpd61to90Map = new HashMap<>();
    Map<String, Integer> loanDpd91to120Map = new HashMap<>();
    Map<String, Integer> loanDpd121to180Map = new HashMap<>();
    Map<String, Integer> loanDpd181to365Map = new HashMap<>();
    Map<String, Integer> loanDpd366Map = new HashMap<>();

    Map<String, Integer> deathFlaggedMap = new HashMap<>();
    Map<String, Integer> writeOffMap = new HashMap<>();
    Set<String> aaArrIdInLoanDpd = new HashSet<>();
    Set<String> aaArrIdInCusDpd = new HashSet<>();
    Map<String, Integer> defaultClient = new HashMap<>();
    Map<String, Integer> defaultLoanAcct = new HashMap<>();
    String customer = "";
    Set<String> customerIds = new HashSet<>();
    boolean flag = false;
    private static final String COMPANY = "COMPANY";
    List<String> ffLoanDpdIds = new ArrayList<>();
    public static final DateTimeFormatter T24_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    public static final DateTimeFormatter MONTH_YEAR_FORMAT = DateTimeFormatter.ofPattern("MMMuuuu");

    int overallSelectSize = 0;

    String companyName = "";

    String coCode = "";
    String zonalName = "";
    String regionalName = "";
    String divisionalCode = "";
    String clusterCode = "";

   
    private static final String EB_FF_PARAMETER = "EB.FF.PARAMETER";
    int daterange = 0;
    int defaultDate = 0;
    boolean dateRangeFlag = false;
    Session session = new Session(this);
    String todayDate = session.getCurrentVariable("!TODAY");
    LocalDate today = LocalDate.parse(todayDate, formatter);
    String maxMonth = "";
    String defaultMonth = "";
    public static final String MAX_DATE_RANGE_ERR = "EB-FF.BM.PAST.MAX.DT.RANGE";
    String roName = "";
    String productName = "";
    String centreName = "";
    String villageName = "";
    String accNum = "";
    String selGroup = "";
    String selUser = "";
    int writeOffCount = 0;
    public static final String CHARGEACC = "LENDING-CHARGEOFF-ACCOUNT";
    public static final String CHARGEARR = "LENDING-CHARGEOFF-ARRANGEMENT";
    public static final String BAL = "LENDING-WRITE.OFF-BAL.MAINTAIN";
    boolean isWriteOffLoan = false;
    boolean dpd1count = false;
    boolean dpd2count = false;
    boolean dpd3count = false;
    boolean dpd4count = false;
    boolean dpd5count = false;
    boolean dpd6count = false;
    boolean dpd7count = false;
    String loanstatus = "";
    CustomerRecord cusRec = null;
    String dateOfDeath = "";
    String deathFlaged = "";
    boolean branchSelection = false;
    Map<String, Set<String>> groupCustomers = new HashMap<>();
    Map<String, Integer> clientCount = new HashMap<>();
    Map<String, Integer> loanCount = new HashMap<>();
    int count = 0;

    @Override
    /*
     * method: setIds Description: This method is used to fecth the enquiry details.
     */
    public List<String> setIds(List<FilterCriteria> filterCriteria, EnquiryContext enquiryContext) {

        try {

            getFilterCriteriaDets(filterCriteria);
            runBranchSummary();
        } catch (Exception e1) {
            e1.getMessage();
        }
        if (noRecErrFlag) {
            throw new T24CoreException("", NO_REC_ERR);
        } else {
            return returnVal;
        }

    }

    /*
     * method: runBranchSummary Description: This method is used to fecth the
     * grouping value detailA.
     */
    private void runBranchSummary() {

        try {
            Contract contract = new Contract(this);
            Set<String> overAllArrAccDetIdList = new LinkedHashSet<>(
                    da.selectRecords(finMnemonic, AA_ARRANGEMENT_RECORD, "",
                            "WITH ARR.STATUS NE CLOSE PENDING.CLOSURE AND CO.CODE EQ " + companyIds));

            overallSelectSize = overAllArrAccDetIdList.size();

            finalArrIdList = getFinalArrList(overAllArrAccDetIdList);

            for (String finalArrId : finalArrIdList) {
                isWriteOffLoan = false;
                dpd1count = false;
                dpd2count = false;
                dpd3count = false;
                dpd4count = false;
                dpd5count = false;
                dpd6count = false;
                dpd7count = false;
                startDate = "";
                ro = "";
                product = "";
                centre = "";
                roName = "";
                productName = "";
                centreName = "";
                villageName = "";
                loanDpd1 = 0;
                loanDpd2 = 0;
                loanDpd3 = 0;
                loanDpd4 = 0;
                loanDpd5 = 0;
                loanDpd6 = 0;
                loanDpd7 = 0;

                deathFlaged = "NO";
                writeOff = "NO";
                count++;

                contract.setContractId(finalArrId);
                getAaArrangementDets(finalArrId);
                getCustomerDpd(customer);
                getLoanDpd(finalArrId, contract);

                checkLoanDpd(finalArrId);

                calculatingDeathCnt(customer);

                String groupValue = getGroupBasedValue(finalArrId);

                boolean selectionCheck = chkSelectionBased();
                if (selectionCheck) {
                    continue;
                }
                if (groupValue != null && !groupValue.isEmpty()) {
                    calculation(groupValue, finalArrId);
                }
            }

            List<String> sortedKeys = new ArrayList<>(defaultLoanAcct.keySet());

            if (dateGrp) {
                Collections.sort(sortedKeys);
            }
            for (String currentGrp : sortedKeys) {

                List<String> row = new ArrayList<>();

                row.add(zoneName);
                row.add(regionName);
                row.add(divisionName);
                row.add(clusterName);
                row.add(branchCode);
                row.add(branchName);
                row.add(currentGrp);
                row.add(String.valueOf(defaultClient.get(currentGrp)));
                row.add(String.valueOf(defaultLoanAcct.get(currentGrp)));
                row.add(String.format("%.2f", principalDefaultMap.get(currentGrp)));
                row.add(String.format("%.2f", interestdefaultMap.get(currentGrp)));
                row.add(String.format("%.2f", totalDefaultMap.get(currentGrp)));
                row.add(String.format("%.2f", outstandingMap.get(currentGrp)));
                row.add(String.valueOf(loancnt1to30Map.get(currentGrp)));
                row.add(String.valueOf(loanDpd31to60Map.get(currentGrp)));
                row.add(String.valueOf(loanDpd61to90Map.get(currentGrp)));
                row.add(String.valueOf(loanDpd91to120Map.get(currentGrp)));
                row.add(String.valueOf(loanDpd121to180Map.get(currentGrp)));
                row.add(String.valueOf(loanDpd181to365Map.get(currentGrp)));
                row.add(String.valueOf(loanDpd366Map.get(currentGrp)));
                row.add(String.valueOf(deathFlaggedMap.get(currentGrp)));
                row.add(String.valueOf(writeOffMap.get(currentGrp)));

                returnVal.add(String.join("*", row));

                outvalues.add(String.join(",", row));

            }

            String filePath = "";
            String paramId = "FF.BM.REPORT.EXTRACT";
            EbFfParameterRecord paramRec = new EbFfParameterRecord(da.getRecord(EB_FF_PARAMETER, paramId));
            for (ParamDescClass paramDesc : paramRec.getParamDesc()) {
                if (paramDesc.getParamName().getValue().equals("Path")) {
                    filePath = paramDesc.getParamValue().getValue();
                }
            }
            LocalDateTime currDtTime = LocalDateTime.now();
            String currDate = currDtTime.format(outDateFormatter);
            String currTime = currDtTime.format(timeFormatter);

            String outputPath = filePath + FILE_NAME + "_" + selGroup + "-WISE" + "_" + branchName + "_" + selUser + "_"
                    + currDate + "_" + currTime + ".csv";

            writeToFile(outvalues, outputPath);

        } catch (Exception e2) {
            e2.getMessage();
        }
    }

    /*
     * method: checkLoanDpd Description: This method is used to check the outstaning
     * values.
     */
    private void checkLoanDpd(String finalArrId) {

        Contract defaultContract = new Contract(this);
        try {
            if (aaArrIdInLoanDpd.contains(finalArrId)) {

                flag = true;
                defaultContract.setContractId(finalArrId);

            }

        } catch (Exception e3) {
            e3.getMessage();
        }

    }

    /*
     * method: calculation Description: This method is used to check the grouoing
     * values.
     */
    private void calculation(String groupValue, String finalArrId) {

        try {

            groupCustomers.putIfAbsent(groupValue, new HashSet<>());

            Set<String> customersInGroup = groupCustomers.get(groupValue);

            defaultClient.putIfAbsent(groupValue, 0);

            defaultLoanAcct.putIfAbsent(groupValue, 0);

            if (!customersInGroup.contains(customer)) {

                if (aaArrIdInCusDpd.contains(customer)) {

                    int defClient = defaultClient.getOrDefault(groupValue, 0);

                    defaultClient.put(groupValue, defClient + 1);

                }

                customersInGroup.add(customer);

            }

            if (aaArrIdInLoanDpd.contains(finalArrId)) {

                int defLoan = defaultLoanAcct.getOrDefault(groupValue, 0);

                defaultLoanAcct.put(groupValue, defLoan + 1);

            }

            if (dpd1count) {
                loancnt1to30Map.put(groupValue, loancnt1to30Map.getOrDefault(groupValue, 0) + loanDpd1);
            } else {
                loancnt1to30Map.putIfAbsent(groupValue, 0);
            }
            if (dpd2count) {
                loanDpd31to60Map.put(groupValue, loanDpd31to60Map.getOrDefault(groupValue, 0) + loanDpd2);
            } else {
                loanDpd31to60Map.putIfAbsent(groupValue, 0);
            }
            if (dpd3count) {
                loanDpd61to90Map.put(groupValue, loanDpd61to90Map.getOrDefault(groupValue, 0) + loanDpd3);
            } else {
                loanDpd61to90Map.putIfAbsent(groupValue, 0);
            }
            if (dpd4count) {
                loanDpd91to120Map.put(groupValue, loanDpd91to120Map.getOrDefault(groupValue, 0) + loanDpd4);
            } else {
                loanDpd91to120Map.putIfAbsent(groupValue, 0);
            }

            if (dpd5count) {
                loanDpd121to180Map.put(groupValue, loanDpd121to180Map.getOrDefault(groupValue, 0) + loanDpd5);
            } else {
                loanDpd121to180Map.putIfAbsent(groupValue, 0);
            }
            if (dpd6count) {
                loanDpd181to365Map.put(groupValue, loanDpd181to365Map.getOrDefault(groupValue, 0) + loanDpd6);
            } else {
                loanDpd181to365Map.putIfAbsent(groupValue, 0);
            }
            if (dpd7count) {
                loanDpd366Map.put(groupValue, loanDpd366Map.getOrDefault(groupValue, 0) + loanDpd7);
            } else {
                loanDpd366Map.putIfAbsent(groupValue, 0);
            }

            principalDefaultMap.put(groupValue,
                    principalDefaultMap.getOrDefault(groupValue, 0.0) + Math.abs(principalDefault));
            interestdefaultMap.put(groupValue,
                    interestdefaultMap.getOrDefault(groupValue, 0.0) + Math.abs(interestdefault));
            totalDefaultMap.put(groupValue, totalDefaultMap.getOrDefault(groupValue, 0.0) + Math.abs(totalDefault));
            outstandingMap.put(groupValue, outstandingMap.getOrDefault(groupValue, 0.0) + Math.abs(outstanding));

            if ("YES".equals(deathFlaged)) {
                deathFlaggedMap.put(groupValue, deathFlaggedMap.getOrDefault(groupValue, 0) + 1);
            } else {
                deathFlaggedMap.putIfAbsent(groupValue, 0);
            }

            if ("YES".equals(writeOff)) {
                writeOffMap.put(groupValue, writeOffMap.getOrDefault(groupValue, 0) + 1);
            } else {
                writeOffMap.putIfAbsent(groupValue, 0);
            }

        } catch (Exception e4) {
            e4.getStackTrace();
        }
    }

    /*
     * method: getLoanCustDpd Description: This method is used to check the CUR DPD
     * value greaterthan zero.
     */
    private void getLoanDpd(String finalArrId, Contract contract) {

        try {
            LocalDate currDate = LocalDate.parse(todayDate, T24_FORMATTER);
            String currMonthYear = currDate.format(MONTH_YEAR_FORMAT).toUpperCase();
            String loanDpdRecId = finalArrId + "-" + currMonthYear;
            EbFfLoanDpdRecord loanDpdRecord = new EbFfLoanDpdRecord(
                    da.getRecord(finMnemonic, "EB.FF.LOAN.DPD", "", loanDpdRecId));

            int dateListSize = loanDpdRecord.getDate().size();
            String loanDpd = loanDpdRecord.getDate().get(dateListSize - 1).getCurDpd().getValue();
            int curDpdInt = Integer.parseInt(loanDpd);

            if (curDpdInt > 0) {
                aaArrIdInLoanDpd.add(finalArrId);
                getDPdValues(contract, loanDpdRecId, curDpdInt);

            }

        } catch (Exception e5) {
            e5.getStackTrace();

        }
    }

    /**
     * @param contract
     * @param loanDpdRecId
     * @param curDpdInt
     */
    public void getDPdValues(Contract contract, String loanDpdRecId, int curDpdInt) {

        /*
         * String[] arrIdstr = loanDpdRecId.split("-"); String arrId = arrIdstr[0];
         * aaArrIdInLoanDpd.add(arrId);
         */
        sumofPrincipalDefault(contract);
        if (curDpdInt >= 1 && curDpdInt <= 30) {
            dpd1count = true;
            loanDpd1++;

        } else if (curDpdInt >= 31 && curDpdInt <= 60) {
            getDpd31to60(contract, loanDpdRecId, curDpdInt);
        } else if (curDpdInt >= 61 && curDpdInt <= 90) {
            getDpd61to90(contract, loanDpdRecId, curDpdInt);
        } else if (curDpdInt >= 91 && curDpdInt <= 120) {
            getDpd91to120(contract, loanDpdRecId, curDpdInt);
        } else if (curDpdInt >= 121 && curDpdInt <= 180) {
            getDpd121to190(contract, loanDpdRecId, curDpdInt);
        } else if (curDpdInt >= 180 && curDpdInt <= 365) {
            getDpd365Detailes(contract, loanDpdRecId, curDpdInt);
        } else if (curDpdInt >= 366) {
            dpd7count = true;
            loanDpd7++;

            // sumofPrincipalDefault(contract);
        }
    }

    /**
     * @param contract
     * @param loanDpdRecId
     */
    public void getDpd365Detailes(Contract contract, String loanDpdRecId, int curDpdInt) {

        try {
            dpd6count = true;
            loanDpd6++;

        } catch (Exception e25) {
            e25.getMessage();
        }
    }

    /**
     * @param contract
     * @param loanDpdRecId
     */
    public void getDpd121to190(Contract contract, String loanDpdRecId, int curDpdInt) {

        try {
            dpd5count = true;
            loanDpd5++;

        } catch (Exception e26) {
            e26.getMessage();
        }
    }

    /**
     * @param contract
     * @param loanDpdRecId
     */
    public void getDpd91to120(Contract contract, String loanDpdRecId, int curDpdInt) {

        try {
            dpd4count = true;
            loanDpd4++;

        } catch (Exception e27) {
            e27.getMessage();
        }
    }

    /**
     * @param contract
     * @param loanDpdRecId
     */
    public void getDpd61to90(Contract contract, String loanDpdRecId, int curDpdInt) {

        try {
            dpd3count = true;
            loanDpd3++;

        } catch (Exception e28) {
            e28.getMessage();
        }
    }

    /**
     * @param contract
     * @param loanDpdRecId
     */
    public void getDpd31to60(Contract contract, String loanDpdRecId, int curDpdInt) {

        try {
            dpd2count = true;
            loanDpd2++;

        } catch (Exception e29) {
            e29.getMessage();
        }
    }

    private void getCustomerDpd(String customer) {
        try {
            LocalDate date = LocalDate.parse(todayDate, T24_FORMATTER);
            String formatted = date.getMonth().toString().substring(0, 3) + date.getYear();
            String customerDpdId = "CUS" + customer + "-" + formatted;

            EbFfCustDpdRecord ebFfCustDpdRecord = new EbFfCustDpdRecord(
                    da.getRecord(mnemonic, "EB.FF.CUST.DPD", "", customerDpdId));

            List<com.temenos.t24.api.records.ebffcustdpd.DateClass> dateClass = ebFfCustDpdRecord.getDate();
            String customerDpd = dateClass.get(dateClass.size() - 1).getCurDpd().getValue();

            int curDpdInt = Integer.parseInt(customerDpd);
            if (curDpdInt > 0) {
                String cusId = customerDpdId.split("-")[0].replaceAll("[a-zA-Z]", "");
                aaArrIdInCusDpd.add(cusId);
            }
        } catch (Exception e6) {
            e6.getMessage();
        }
    }

    /*
     * method: getFinalArrList Description: This method is used to fecth the date
     * values.
     */
    private List<String> getFinalArrList(Set<String> preFinalSet) {

        List<String> arrIdList = new ArrayList<>();
        try {

            /*
             * LocalDate start = startDate.isEmpty() ? today.minusDays(defaultDate) :
             * LocalDate.parse(startDate, formatter); LocalDate end = endDate.isEmpty() ?
             * today : LocalDate.parse(endDate, formatter);
             */

            for (String contractId : preFinalSet) {

                String ageStatus = getStartDateFromAccountDets(contractId);

                if (ageStatus != null && !ageStatus.isEmpty()) {
                    // LocalDate stDate = LocalDate.parse(arrStartDt, formatter);

                    if (ageStatus.equals("SM0") || ageStatus.equals("SM1") || ageStatus.equals("SM2")
                            || ageStatus.equals("NPA")) {

                        arrIdList.add(contractId);
                    }
                }
            }

        } catch (Exception e7) {
            e7.getMessage();
        }

        return arrIdList;
    }

    /*
     * method: getAaArrangementDets Description: This method is used to fecth the
     * product values from AA_ARRANGEMENT_RECORD.
     */
    public void getAaArrangementDets(String finalArrId) {

        try {
            AaArrangementRecord arrRec = new AaArrangementRecord(
                    da.getRecord(finMnemonic, AA_ARRANGEMENT_RECORD, "", finalArrId));

            product = arrRec.getProduct().get(0).getProduct().getValue();
            accNum = arrRec.getLinkedAppl().get(0).getLinkedApplId().getValue();

            customer = arrRec.getCustomer().get(0).getCustomer().getValue();

            coCode = arrRec.getCoCodeRec().getValue();

            getAaProductDetails(product);

        } catch (Exception e8) {
            e8.getMessage();
        }

    }

    /**
     * @param product2
     */
    private void getAaProductDetails(String productId) {

        try {
            AaProductRecord aaProRec = new AaProductRecord(da.getRecord("AA.PRODUCT", productId));
            productName = aaProRec.getDescription(0).getValue();

        } catch (Exception e) {
            e.getMessage();
        }

    }

    /*
     * method: getAaArrAccountDet Description: This method is used to fecth the
     * centre and village values from AA.PRD.DES.ACCOUNT RECORD Application.
     */
    private void getAaArrAccountDet(String accNum) {

        try {
            AccountRecord accRec = new AccountRecord(da.getRecord(finMnemonic, "ACCOUNT", "", accNum));
            centre = accRec.getLocalRefField("FF.CENTRE").getValue();
            village = accRec.getLocalRefField("FF.VILLAGE").getValue();

            if (village != null && !village.isEmpty()) {
                getEbFfVillageDetails(village);
            }
            if (centre != null && !centre.isEmpty()) {
                getEbFfCentreDetails(centre);
            }
            loanstatus = accRec.getLocalRefField("FF.LOAN.STATUS").getValue();
            writeOff = "NO";
            if (loanstatus.equalsIgnoreCase("WRITE OFF")) {
                writeOff = "YES";
            }
        } catch (Exception e) {
            e.getMessage();
        }
    }
    /*
     * method: getEbFfCentreDetails Description: This method is used to get the
     * roName values from EB.FF.CENTRE.DETAIL Application.
     */

    public void getEbFfVillageDetails(String village) {
        try {
            EbFfVillageRecord villageRec = new EbFfVillageRecord(da.getRecord("", "EB.FF.VILLAGE", "", village));
            villageName = villageRec.getVillageName().getValue();

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
    /*
     * method: getStartDateFromAccountDets Description: This method is used to get
     * the startDate values from AA.ACCOUNT.DETAILS Application.
     */

    private String getStartDateFromAccountDets(String contractId) {

        try {
            AaAccountDetailsRecord aaAccountDet = new AaAccountDetailsRecord(
                    da.getRecord(finMnemonic, "AA.ACCOUNT.DETAILS", "", contractId));
            startDateArrAcc = aaAccountDet.getStartDate().getValue();
            arrAgeStatus = aaAccountDet.getArrAgeStatus().getValue();

        } catch (Exception e11) {
            e11.getMessage();
        }

        return arrAgeStatus;
    }
    /*
     * method: chkSelectionBased Description: This method is used to check the
     * Ro,Product,CenterName and Village values.
     */

    public boolean chkSelectionBased() {

        return (selRo != null && !selRo.isEmpty() && !selRo.equals(ro))
                || (selProduct != null && !selProduct.isEmpty() && !selProduct.equals(product))
                || (selCenterName != null && !selCenterName.isEmpty() && !selCenterName.equals(centre))
                || (selVillage != null && !selVillage.isEmpty() && !selVillage.equals(village));
    }

    /*
     * method: getGroupBasedValue Description: This method is used to check the
     * field values.
     */
    public String getGroupBasedValue(String finalArrId) {

        String groupKey = "";

        switch (selGroup) {
        case "BRANCH":
            selFilterField = "Branch";
            branchSelection = true;
            groupKey = getCompanyDescription(coCode);

            break;

        case "RO":
            selFilterField = "RO";
            getAaArrAccountDet(accNum);
            groupKey = roName;
            break;
        case "PRODUCT":
            selFilterField = "Product";
            groupKey = productName;
            break;
        case "CENTER":
            selFilterField = "CenterName";
            getAaArrAccountDet(accNum);
            groupKey = centreName;
            break;
        case "VILLAGE":
            selFilterField = "Village";
            getAaArrAccountDet(accNum);
            groupKey = villageName;
            break;
        default:
        }

        return groupKey;
    }

    /*
     * method: getFilterCriteriaDets Description: This method is used to check the
     * selection field values.
     */
    private void getFilterCriteriaDets(List<FilterCriteria> filterCriteria) {

        try {
            for (FilterCriteria filter : filterCriteria) {
                String value = filter.getValue();
                if (value == null || value.isEmpty()) {
                    continue;
                }
                switch (filter.getFieldname()) {
                case "BRANCH":
                    selBranch = value;
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
                case "PRODUCT":
                    selProduct = value;

                    break;
                case "CENTER":
                    selCenterName = value;

                    break;
                case "VILLAGE":
                    selVillage = value;

                    break;
                default:
                }
            }
        } catch (Exception e12) {
            e12.getMessage();
        }
    }

    /*
     * method: initialiseCompanyInfo Description: This method is used to get the
     * Company wise dispalyed the field values.
     */

    public void initialiseCompanyInfo(String companyId) {
        try {

            CompanyRecord companyObj = new CompanyRecord(da.getRecord(COMPANY, companyId));
            finMnemonic = companyObj.getFinancialMne().getValue();
            mnemonic = companyObj.getCustomerMnemonic().getValue();
            branchCode = companyId;
            branchName = getCompanyDescription(companyId);
            zoneName = getCompanyDescription(companyObj.getLocalRefField("FF.ZONE").getValue());
            regionName = getCompanyDescription(companyObj.getLocalRefField("FF.REGION").getValue());
            divisionName = getCompanyDescription(companyObj.getLocalRefField("FF.DIVISION").getValue());
            clusterName = getCompanyDescription(companyObj.getLocalRefField("FF.CLUSTER").getValue());

        } catch (Exception e14) {
            e14.getMessage();
        }
    }

    /*
     * method: getCompanyDescription Description: This method is used to get the
     * Company descriptions.
     */

    public String getCompanyDescription(String companyCode) {
        try {
            if (companyCode != null && !companyCode.isEmpty()) {
                CompanyRecord companyRec = new CompanyRecord(da.getRecord("COMPANY", companyCode));
                companyName = companyRec.getCompanyName().get(0).getValue();
                String[] compNamePart = companyName.split("-");
                companyName = compNamePart[0];
                return companyName;
            }
        } catch (Exception e9) {
            e9.getMessage();
        }
        return companyName;

    }

    /*
     * method: getLinkedCompIds Description: This method is used to checked the
     * company wise.
     */

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
        } catch (Exception e17) {
            e17.getMessage();
        }
    }
    /*
     * method: calculatingDeathCnt Description: This method is used to check and
     * count the death status list from EB.FF.LOAN.DETAILS application
     */

    private void calculatingDeathCnt(String customer) {
        try {
            deathFlaged = "NO";
            cusRec = new CustomerRecord(da.getRecord(mnemonic, "CUSTOMER", "", customer));
            dateOfDeath = cusRec.getNotificationOfDeath().getValue();
            if (dateOfDeath != null && !dateOfDeath.isEmpty()) {
                deathFlaged = "YES";

            }

        } catch (Exception e) {
            e.getMessage();
        }

    }

    /*
     * method: getActivityValues Description: This method is used to checking the
     * activity's from AA.ACTIVITY.HISTORY application.
     * 
     * 
     * public void getActivityValues(String finalArrId) {
     * 
     * try { int loanOffCount = 0; // for (String id : finalArrIdList) {
     * 
     * aaActHisRec = new AaActivityHistoryRecord(da.getRecord(finMnemonic,
     * "AA.ACTIVITY.HISTORY", "", finalArrId)); boolean found = false;
     * effectiveDateList = aaActHisRec.getEffectiveDate(); for (EffectiveDateClass
     * effectiveDate : effectiveDateList) { activityRefList =
     * effectiveDate.getActivityRef(); for (ActivityRefClass activityRef :
     * activityRefList) { activity = activityRef.getActivity().getValue(); if
     * (activity.equals(CHARGEACC) || activity.equals(CHARGEARR) ||
     * activity.equals(BAL)) { found = true; break; } } if (found) { break; } }
     * 
     * if (found) { isWriteOffLoan = true; } // }
     * 
     * writeOff = loanOffCount;
     * 
     * } catch (Exception e20) { e20.getMessage(); }
     * 
     * }
     */
    /*
     * method: sumofPrincipalDefault Description: This method is used to check the
     * outstanding values from EB.CONTRACT.BALANCES application.
     */
    private void sumofPrincipalDefault(Contract defaultContract) {

        try {

            String principalDefaultValue = getBalance(defaultContract, "FFPRINODAMT", REQ_TYPE_BOOKING);

            String interestdefaultValue = getBalance(defaultContract, "FFINTODAMT", REQ_TYPE_BOOKING);

            String totalDefaultValue = getBalance(defaultContract, "FFALLOVRDUE", REQ_TYPE_BOOKING);

            String outstandingValue = getBalance(defaultContract, "FFALLOSTBAL", REQ_TYPE_BOOKING);

            principalDefault = Double.parseDouble(principalDefaultValue);

            interestdefault = Double.parseDouble(interestdefaultValue);

            totalDefault = Double.parseDouble(totalDefaultValue);

            outstanding = Double.parseDouble(outstandingValue);

        } catch (Exception e21) {
            e21.getMessage();
        }
    }

    private String getBalance(Contract contract1, String accountType, String bookingType) {

        List<BalanceMovement> movements = null;
        try {
            movements = contract1.getContractBalanceMovements(accountType, bookingType);
        } catch (Exception e22) {
            e22.getMessage();

        }
        return (movements != null && !movements.isEmpty()) ? movements.get(0).getBalance().toString() : "0";
    }

    /*
     * method: writeToFile Description: This method is used to write the field
     * values in .csv file.
     */
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
                        String header = String.join(",", "ZoneName", "RegionName", "DivisionName", "ClusterName",
                                "BranchCode", "BranchName", selFilterField, "Defaulter Client",
                                "Defaulter Loan Account", "Principal Default", "Interest default", "Total default",
                                "Outstanding", "loancnt1to30", "loanDpd31to60", "loanDpd61to90", "loanDpd91to120",
                                "loanDpd121to180", "loanDpd181to365", "loanDpd366", "Death Flagged", "Write off");
                        writer.write(header + System.lineSeparator());
                    }

                    for (String line : data) {
                        writer.write(line + System.lineSeparator());

                    }
                }
            }
        } catch (Exception e23) {
            e23.getMessage();
        }

    }

}
