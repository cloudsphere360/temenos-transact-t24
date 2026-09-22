package com.temenos.fusion;

import java.io.File;
import java.io.FileWriter;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
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

import com.temenos.t24.api.records.aaarrangement.AaArrangementRecord;
import com.temenos.t24.api.records.aaarrangement.LinkedApplClass;
import com.temenos.t24.api.records.aaproduct.AaProductRecord;
import com.temenos.t24.api.records.account.AccountRecord;
import com.temenos.t24.api.records.company.CompanyRecord;
import com.temenos.t24.api.records.companyconsol.CompanyConsolRecord;
import com.temenos.t24.api.records.ebffcentredetail.EbFfCentreDetailRecord;
import com.temenos.t24.api.records.ebffcustdpd.EbFfCustDpdRecord;
import com.temenos.t24.api.records.ebffloandpd.EbFfLoanDpdRecord;
import com.temenos.t24.api.records.ebffparameter.EbFfParameterRecord;
import com.temenos.t24.api.records.ebffparameter.ParamDescClass;
import com.temenos.t24.api.records.ebffrouser.EbFfRoUserRecord;
import com.temenos.t24.api.records.ebffvillage.EbFfVillageRecord;
import com.temenos.t24.api.system.DataAccess;
import com.temenos.t24.api.system.Session;

/**
 * ------------------------------------------------------------------------------
 * 
 * @author Deepakumar S Date Created: 11-12-2025 Attached as : EB.API
 *         :FF.E.NOFILE.OUTSTAND.BLANCE.SUMMARY Attached to : STANDARD.SELECTION
 *         > NOFILE.FF.BM.OUTSTAND.BLANCE.SUMMARY Enquiry Name:
 *         FF.NOFILE.BM.OUTSTAND.BLANCE.SUMMARY Description: Branch Online
 *         Report generation -> OUTSTANDING.BLANCE.SUMMARY
 *         --------------------------------------------------------------------------------
 *         Modification History : NA
 *         --------------------------------------------------------------------------------
 *         11-12-2025 Development Initial Version
 * 
 *         11-03-2026 Remapping Jerome
 * 
 *         ----------------------------------------------------------------------------------
 * 
 * 
 * 
 */
public class FfNofileOutstandingBalance extends Enquiry {

    private static final String COMPANY = "COMPANY";
    private static final String AA_ARRANGEMENT = "AA.ARRANGEMENT";
    public static final String NO_REC_ERR = "EB-NO.REC.SELECT";
    private static final String TRADE = "TRADE";
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    public static final DateTimeFormatter MONTH_YEAR_FORMAT = DateTimeFormatter.ofPattern("MMMuuuu");
    public static final String FILE_NAME = "OutStandingBalRep_Sum";

    DataAccess da = new DataAccess(this);
    Session ss = new Session(this);
    Contract contract = new Contract(this);

    List<String> outvalues = new ArrayList<>();
    Set<String> productSet = new HashSet<>();
    List<String> finalArrIdList = new ArrayList<>();
    List<String> returnVal = new ArrayList<>();

    Map<String, Set<String>> groupCustomers = new HashMap<>();
    Map<String, Integer> loanCount = new HashMap<>();
    Map<String, Integer> clientCount = new HashMap<>();
    Map<String, Integer> defaultClient = new HashMap<>();
    Map<String, Integer> defaultLoanAcct = new HashMap<>();
    Map<String, Double> principalOutstand = new HashMap<>();
    Map<String, Double> interestOutstand = new HashMap<>();
    Map<String, Double> principalDef = new HashMap<>();
    Map<String, Double> interestDef = new HashMap<>();
    Map<String, Double> totalDef = new HashMap<>();
    Map<String, Double> outstandingDef = new HashMap<>();

    Set<String> customerIds = new HashSet<>();

    String todayDate = "";
    String finMnemonic = "";
    String mnemonic = "";
    String branchName = "";
    String branchCode = "";
    String zoneName = "";
    String regionName = "";
    String divisionName = "";
    String selFilterField = "";
    String clusterName = "";
    String selDate = "";
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
    AaArrangementRecord arrRec = null;
    String coCode = "";
    CompanyRecord compyRec = null;
    String companyName = "";
    String roCount = "";
    String centreCount = "";
    String villageCount = "";
    String productCount = "";
    String missedInstallmentAmount = "";
    String loansCount = "";
    String missedInstallment = "";
    String osPropAmount = "";
    double sumOfMissedinstallment = 0.0;

    String startDateArrAcc = "";
    List<String> dateBaseArrId = new ArrayList<>();

    double currentOd = 0.0;
    double principalOverdue = 0.0;
    double totalInterestCollected = 0.0;
    double interestOverdue = 0.0;
    double principalOutstanding = 0.0;
    double interestOutstanding = 0.0;
    double interestDefault = 0.0;
    double outstandingDefault = 0.0;
    double principalDefault = 0.0;

    double totalDefault = 0.0;
    int numOfCustDpd = 0;

    List<String> ffLoanDpdList = null;
    int numOfLoanOverDueCnt = 0;
    List<String> ffCusDpdList = null;
    int numOfCustOverDueCnt = 0;

    String outstanding = "";
    Set<String> aaArrIdInLoanDpd = new HashSet<>();
    Set<String> aaArrIdInCusDpd = new HashSet<>();

    Set<String> finalCusList = new HashSet<>();
    String parkedAmount = "";
    String overdueIntrestDenand = "";
    String overdueInstallment = "";
    String currentInstallmentDemandDate = "";
    String principalPrepayment = "";
    String intrestPrepayment = "";
    String currentPrincipalInstallment = "";
    String currentInterestInstallment = "";

    String overAllPrincipalOutstanding = "";
    String overallAllinterestOutstanding = "";
    String overAllPrincipalDefault = "";
    String overAllInterestDefault = "";
    String overAllTotalDefault = "";
    String oversumofAllOutstandingDefault = "";
    double sumofAllPrincipalOutstanding = 0.0;
    double sumofAllinterestOutstanding = 0.0;
    double sumofAllPrincipalDefault = 0.0;
    double sumofAllInterestDefault = 0.0;
    double sumofAllTotalDefault = 0.0;
    double sumofAllOutstandingDefault = 0.0;
    String roName = "";
    String ro = "";
    String strtDate = "";
    String product = "";
    String centre = "";
    String village = "";
    String selBranch = "";
    String enquiryName = "";
    String selectionFilter = "";
    String companyIds = "";
    String maturityDate = "";
    boolean dateErrFlag = false;
    boolean branchSelection = false;
    boolean dateGrp = false;
    boolean flag = false;
    Set<String> cusLendArrIds = new HashSet<>();
    int openLoanAccount = 0;
    List<String> custarr = new ArrayList<>();
    int openLoanCLient = 0;
    String customer = "";
    int itergatingCount = 0;
    String accNum = "";
    String roOfficerName = "";
    String centreName = "";
    String productDesc = "";
    String villageName = "";
    String selGroup = "";
    String selUser = "";

    @Override

    public List<String> setIds(List<FilterCriteria> filterCriteria, EnquiryContext enquiryContext) {

        try {

            Session session = new Session(this);
            todayDate = session.getCurrentVariable("!TODAY");

            getFilterCriteriaDets(filterCriteria);

            Set<String> overAllArrAccDetIdList = new LinkedHashSet<>(da.selectRecords(finMnemonic, AA_ARRANGEMENT, "",
                    "WITH ARR.STATUS NE CLOSE AND ARR.STATUS NE PENDING.CLOSURE AND CO.CODE EQ " + companyIds));

            for (String finalArrId : overAllArrAccDetIdList) {

                itergatingCount++;

                startDate = "";
                ro = "";
                product = "";
                centre = "";
                village = "";
                roOfficerName = "";
                centreName = "";
                productDesc = "";
                villageName = "";
                principalOutstanding = 0.0;
                interestOutstanding = 0.0;
                interestDefault = 0.0;
                outstandingDefault = 0.0;
                principalDefault = 0.0;
                totalDefault = 0.0;

                contract.setContractId(finalArrId);

                getAaArrDet(finalArrId);
                getOutstandingDetails(contract);
                getLoanDpd(finalArrId);
                getCustomerDpd(customer);
                checkLoanDpd(finalArrId);

                String groupValue = getGroupBasedValue(finalArrId, accNum);

                boolean selectionCheck = chkSelectionBased();
                if (selectionCheck) {
                    continue;
                }

                if (groupValue != null && !groupValue.isEmpty()) {

                    calculation(groupValue, finalArrId);

                }

            }

            List<String> sortedKeys = sortMethod(loanCount);

            for (String currentGrp : sortedKeys) {
                List<String> row = new ArrayList<>();
                row.add(zoneName);
                row.add(regionName);
                row.add(divisionName);
                row.add(clusterName);
                row.add(branchCode);
                row.add(branchName);
                row.add(currentGrp);
                row.add(String.valueOf(clientCount.get(currentGrp)));
                row.add(String.valueOf(loanCount.get(currentGrp)));
                row.add(String.format("%.2f", principalOutstand.get(currentGrp)));
                row.add(String.format("%.2f", interestOutstand.get(currentGrp)));
                row.add(String.valueOf(defaultClient.get(currentGrp)));
                row.add(String.valueOf(defaultLoanAcct.get(currentGrp)));
                row.add(String.format("%.2f", principalDef.get(currentGrp)));
                row.add(String.format("%.2f", interestDef.get(currentGrp)));
                row.add(String.format("%.2f", totalDef.get(currentGrp)));
                row.add(String.format("%.2f", outstandingDef.get(currentGrp)));
                returnVal.add(String.join("*", row));
                outvalues.add(String.join(",", row));
            }

            fileWrite();
        } catch (Exception e) {

            e.getMessage();
        }
        if (returnVal.isEmpty()) {
            throw new T24CoreException("", NO_REC_ERR);
        } else {
            return returnVal;
        }
    }

    private void calculation(String groupValue, String finalArrId) {

        try {

            groupCustomers.putIfAbsent(groupValue, new HashSet<>());

            Set<String> customersInGroup = groupCustomers.get(groupValue);

            loanCount.putIfAbsent(groupValue, 0);
            defaultClient.putIfAbsent(groupValue, 0);
            defaultLoanAcct.putIfAbsent(groupValue, 0);

            if (!customersInGroup.contains(customer)) {
                clientCount.put(groupValue, clientCount.getOrDefault(groupValue, 0) + 1);

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

            loanCount.put(groupValue, loanCount.getOrDefault(groupValue, 0) + 1);

            principalOutstand.put(groupValue,
                    principalOutstand.getOrDefault(groupValue, 0.0) + Math.abs(principalOutstanding));

            interestOutstand.put(groupValue,
                    interestOutstand.getOrDefault(groupValue, 0.0) + Math.abs(interestOutstanding));
            principalDef.put(groupValue, principalDef.getOrDefault(groupValue, 0.0) + Math.abs(principalDefault));
            interestDef.put(groupValue, interestDef.getOrDefault(groupValue, 0.0) + Math.abs(interestDefault));
            totalDef.put(groupValue, totalDef.getOrDefault(groupValue, 0.0) + Math.abs(totalDefault));
            outstandingDef.put(groupValue, outstandingDef.getOrDefault(groupValue, 0.0) + Math.abs(outstandingDefault));

        } catch (Exception e) {
            e.getMessage();
        }
    }

    // Sorts the group keys based on grouping type (used for date-based grouping).
    private List<String> sortMethod(Map<String, Integer> loanCount) {
        List<String> sortedKeys = new ArrayList<>(loanCount.keySet());
        if (dateGrp) {
            Collections.sort(sortedKeys);
        }
        return sortedKeys;
    }

    private void fileWrite() {
        try {

            DateTimeFormatter outDateFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
            DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HHmmss");

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
            String outputPath = filePath + FILE_NAME + "_" + selGroup + "-WISE" + "_" + branchName + "_" + selUser + "_"
                    + currDate + "_" + currTime + ".csv";

            writeToFile(outvalues, outputPath);

        } catch (Exception e) {
            e.getMessage();

        }
    }

    private void checkLoanDpd(String finalArrId) {
        Contract defaultContract = new Contract(this);
        try {

            if (aaArrIdInLoanDpd.contains(finalArrId)) {

                flag = true;
                defaultContract.setContractId(finalArrId);
                getEcbDetails(defaultContract);
            }

        } catch (Exception e) {
            e.getMessage();

        }

    }

    public boolean chkSelectionBased() {
        return (selRo != null && !selRo.isEmpty() && !selRo.equals(roName))
                || (selProduct != null && !selProduct.isEmpty() && !selProduct.equals(product))
                || (selCenterName != null && !selCenterName.isEmpty() && !selCenterName.equals(centre))
                || (selVillage != null && !selVillage.isEmpty() && !selVillage.equals(village));
    }

    private String getStartDateFromArrangement(String arrId) {

        String aaStDate = "";
        try {

            AaArrangementRecord aaArrRec = new AaArrangementRecord(
                    da.getRecord(finMnemonic, AA_ARRANGEMENT, "", arrId));
            aaStDate = aaArrRec.getStartDate().getValue();

        } catch (Exception e) {
            e.getMessage();

        }
        return aaStDate;
    }

    private void getAccountDetails(String acctId) {

        try {

            AccountRecord accRec = new AccountRecord(da.getRecord(finMnemonic, "ACCOUNT", "", acctId));
            centre = accRec.getLocalRefField("FF.CENTRE").getValue();
            village = accRec.getLocalRefField("FF.VILLAGE").getValue();

            if (centre != null && !centre.isEmpty()) {
                getEbFfCentreDetails(centre);

            }
            if (village != null && !village.isEmpty()) {
                getVillageDetails(village);
            }

        } catch (Exception e) {
            e.getMessage();

        }

    }

    private void getVillageDetails(String village) {
        try {

            EbFfVillageRecord ffVillage = new EbFfVillageRecord(da.getRecord("", "EB.FF.VILLAGE", "", village));
            villageName = ffVillage.getVillageName().getValue();

        } catch (Exception e) {
            e.getMessage();

        }

    }

    public String getGroupBasedValue(String arrId, String accNum2) {

        String groupKey = "";

        switch (selGroup) {
        case "BRANCH":
            selFilterField = "Branch";
            branchSelection = true;
            groupKey = getCompanyDescription(coCode);
            break;

        case "MONTH":
            selFilterField = "Month";
            strtDate = getStartDateFromArrangement(arrId);
            groupKey = strtDate;
            dateGrp = true;
            break;

        case "RO":
            selFilterField = "RO";
            getAccountDetails(accNum2);
            groupKey = roOfficerName;

            break;

        case "PRODUCT":
            selFilterField = "Product";
            groupKey = productDesc;

            break;

        case "CENTER":
            selFilterField = "Center";
            getAccountDetails(accNum2);
            groupKey = centreName;
            break;

        case "VILLAGE":
            selFilterField = "Village";
            getAccountDetails(accNum2);
            groupKey = villageName;

            break;
        default:
        }
        return groupKey;
    }

    private void getEbFfCentreDetails(String centre) {

        try {

            EbFfCentreDetailRecord centreRec = new EbFfCentreDetailRecord(
                    da.getRecord("", "EB.FF.CENTRE.DETAIL", "", centre));
            roName = centreRec.getCurrentRo().getValue();
            centreName = centreRec.getCenterName().getValue();
            if (roName != null && !roName.isEmpty()) {
                getEbFfRoUserDets(roName);
            }

        } catch (Exception e) {

            e.getMessage();
        }

    }

    public void getEbFfRoUserDets(String ro) {
        try {

            EbFfRoUserRecord roUserRec = new EbFfRoUserRecord(da.getRecord("", "EB.FF.RO.USER", "", ro));
            roOfficerName = roUserRec.getRoName().getValue();

        } catch (Exception e) {
            e.getMessage();

        }
    }

    private void getAaArrDet(String finalArrId) {

        try {

            arrRec = new AaArrangementRecord(da.getRecord(finMnemonic, AA_ARRANGEMENT, "", finalArrId));
            coCode = arrRec.getCoCodeRec().getValue();
            List<LinkedApplClass> linkedAppList = arrRec.getLinkedAppl();
            for (LinkedApplClass linkedApp : linkedAppList) {
                if (linkedApp.getLinkedAppl().getValue().equals("ACCOUNT")) {
                    accNum = linkedApp.getLinkedApplId().getValue();

                }
            }
            product = arrRec.getProduct().get(0).getProduct().getValue();
            getProductName(product);
            customer = arrRec.getCustomer().get(0).getCustomer().getValue();

        } catch (Exception e) {
            e.getMessage();

        }

    }

    private void getProductName(String product) {
        try {

            AaProductRecord prodRec = new AaProductRecord(da.getRecord("AA.PRODUCT", product));
            productDesc = (prodRec.getDescription(0).getValue() != null) ? prodRec.getDescription(0).getValue() : "";

        } catch (Exception e) {

            e.getMessage();
        }
    }

    public static long daysBetween(String startDateStr, String endDateStr) {
        LocalDate start = parseDate(startDateStr);
        LocalDate end = parseDate(endDateStr);
        return ChronoUnit.DAYS.between(start, end); // exclusive difference
    }

    private static LocalDate parseDate(String dateStr) {
        try {
            return LocalDate.parse(dateStr, FORMATTER);
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException(
                    "Invalid date: \"" + dateStr + "\". Expected format: yyyyMMdd (e.g., 20251031).", ex);
        }
    }

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

                case "DATE":
                    selDate = value;
                    selDateOp = filter.getOperand();
                    getDates(selDate, selDateOp);
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

        } catch (Exception e) {
            e.getMessage();

        }

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

    private void getDates(String date, String dateOp) {

        try {
            if (dateOp.equals("RG")) {
                String[] dateRange = date.split(" ");
                startDate = dateRange[0];
                endDate = dateRange[1];
            }

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

    private void initialiseCompanyInfo(String companyId) {

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

        } catch (Exception e) {
            e.getMessage();

        }

    }

    public String getCompanyDescription(String companyCode) {

        String compName = "";
        try {

            if (companyCode != null && !companyCode.isEmpty()) {
                CompanyRecord companyRec = new CompanyRecord(da.getRecord(COMPANY, companyCode));
                compName = companyRec.getCompanyName().get(0).getValue();
                String[] compNamePart = compName.split("-");
                compName = compNamePart[0];

            }

        } catch (Exception e) {

            e.getMessage();

        }
        return compName;
    }

    private void getOutstandingDetails(Contract overdueContract) {
        try {

            String currAccount = getOverDueBalance(overdueContract, "FFPRINOUTAMT", TRADE);
            String accPrinterest = getOverDueBalance(overdueContract, "FFINTODFUTAMT", TRADE);
            principalOutstanding = Double.parseDouble(currAccount);
            interestOutstanding = Double.parseDouble(accPrinterest);

        } catch (Exception e) {

            e.getMessage();
        }

    }

    public String getOverDueBalance(Contract overdueContract, String accountType, String bookingType) {

        List<BalanceMovement> movements = null;
        try {
            movements = overdueContract.getContractBalanceMovements(accountType, bookingType);

        } catch (Exception e) {
            e.getMessage();

        }
        return (movements != null && !movements.isEmpty()) ? movements.get(0).getBalance().toString() : "0";
    }

    private void getLoanDpd(String finalArrId) {

        try {
            LocalDate currDate = LocalDate.parse(todayDate, FORMATTER);
            String formatted = currDate.getMonth().toString().substring(0, 3) + currDate.getYear();
            String loanDpdRecId = finalArrId + "-" + formatted;
            EbFfLoanDpdRecord loanDpdRecord = new EbFfLoanDpdRecord(
                    da.getRecord(finMnemonic, "EB.FF.LOAN.DPD", "", loanDpdRecId));
            int dateListSize = loanDpdRecord.getDate().size();
            String loanDpd = loanDpdRecord.getDate().get(dateListSize - 1).getCurDpd().getValue();
            int curDpdInt = Integer.parseInt(loanDpd);
            if (curDpdInt > 0) {
                aaArrIdInLoanDpd.add(finalArrId);
            }

        } catch (Exception e5) {
            e5.getMessage();

        }
    }

    private void getCustomerDpd(String customer) {

        try {
            LocalDate date = LocalDate.parse(todayDate, FORMATTER);
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

    private void getEcbDetails(Contract defaultContract) {

        try {

            String outstandingDefe = "";
            String totalDefe = "";
            String principalDefe = "";
            String interestDefe = "";

            principalDefe = getBalance(defaultContract, "FFPRINODAMT", TRADE);
            principalDefault = Double.valueOf(principalDefe);

            interestDefe = getBalance(defaultContract, "FFINTODAMT", TRADE);
            interestDefault = Double.valueOf(interestDefe);

            totalDefe = getBalance(defaultContract, "FFALLOVRDUE", TRADE);
            totalDefault = Double.valueOf(totalDefe);

            outstandingDefe = getBalance(defaultContract, "FFALLOSTBAL", TRADE);
            outstandingDefault = Double.valueOf(outstandingDefe);
        } catch (NumberFormatException e) {

            e.getMessage();

        }
    }

    public String getBalance(Contract contract, String accountType, String bookingType) {
        List<BalanceMovement> movements = null;
        try {
            movements = contract.getContractBalanceMovements(accountType, bookingType);

        } catch (Exception e) {
            e.getMessage();

        }
        return (movements != null && !movements.isEmpty()) ? movements.get(0).getBalance().toString() : "0";
    }

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
                                "BranchCode", "BranchName", selFilterField, "ClientCount", "LoanCount",
                                "PrincipalOutstanding", "InterestOutstanding", "DefaulterClient", "DefaultLoanAccount",
                                "PrincipalDefault", "InterestDefault", "TotalDefault", "Outstanding");
                        writer.write(header + System.lineSeparator());

                    }

                    for (String line : data) {
                        writer.write(line + System.lineSeparator());
                    }
                }
            }
        } catch (Exception e) {
            e.getMessage();
        }
    }

}
