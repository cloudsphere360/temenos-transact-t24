package com.temenos.fusion;

import java.io.File;
import java.io.FileWriter;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import com.temenos.api.TField;
import com.temenos.api.exceptions.T24CoreException;
import com.temenos.t24.api.arrangement.accounting.Contract;
import com.temenos.t24.api.complex.eb.enquiryhook.EnquiryContext;
import com.temenos.t24.api.complex.eb.enquiryhook.FilterCriteria;
import com.temenos.t24.api.hook.system.Enquiry;
import com.temenos.t24.api.records.aaactivitybalances.AaActivityBalancesRecord;
import com.temenos.t24.api.records.aaactivityhistory.AaActivityHistoryRecord;
import com.temenos.t24.api.records.aaactivityhistory.ActivityRefClass;
import com.temenos.t24.api.records.aaactivityhistory.EffectiveDateClass;
import com.temenos.t24.api.records.aaarrangement.AaArrangementRecord;
import com.temenos.t24.api.records.aaarrbalancemaintenance.AaArrBalanceMaintenanceRecord;
import com.temenos.t24.api.records.aaarrbalancemaintenance.AdjBalTypeClass;
import com.temenos.t24.api.records.aaarrbalancemaintenance.AdjustPropClass;
import com.temenos.t24.api.records.aaproduct.AaProductRecord;
import com.temenos.t24.api.records.account.AccountRecord;
import com.temenos.t24.api.records.company.CompanyRecord;
import com.temenos.t24.api.records.companyconsol.CompanyConsolRecord;
import com.temenos.t24.api.records.ebffcentredetail.EbFfCentreDetailRecord;
import com.temenos.t24.api.records.ebffparameter.EbFfParameterRecord;
import com.temenos.t24.api.records.ebffparameter.ParamDescClass;
import com.temenos.t24.api.records.ebffrouser.EbFfRoUserRecord;
import com.temenos.t24.api.records.ebffvillage.EbFfVillageRecord;
import com.temenos.t24.api.system.DataAccess;
import com.temenos.t24.api.system.Session;

public class FfNofileClosureRptSummary extends Enquiry {
    private static final String SECONDARY = "SECONDARY";
    private static final String EB_FF_PARAMETER = "EB.FF.PARAMETER";

    public static final String FILE_NAME = "ClosureRep_Sum";
    public static final String ACCOUNT = "ACCOUNT";
    public static final String PRINTEREST = "PRINTEREST";

    public static final String LENDING_APPLYPAYMENT_WRITEOFF_SETTLEMENT = "LENDING-APPLYPAYMENT-WRITEOFF.SETTLEMENT";
    public static final String LENDING_APPLYPAYMENT_INSURANCE_SETTLEMENT = "LENDING-APPLYPAYMENT-INSURANCE.SETTLEMENT";
    public static final String LENDING_APPLYPAYMENT_PR_OUTSTANDING_PAYOFF = "LENDING-APPLYPAYMENT-PR.OUTSTANDING.PAYOFF";
    public static final String LENDING_APPLYPAYMENT_PR_CURR_BALANCE = "LENDING-APPLYPAYMENT-PR.CURR.BALANCE";
    public static final String LENDING_APPLYPAYMENT_PR_INSURANCE_BALANCES = "LENDING-APPLYPAYMENT-PR.INSURANCE.BALANCES";
    public static final String LENDING_WRITE_OFF_BAL_MAINTAIN = "LENDING-WRITE.OFF-BAL.MAINTAIN";
    public static final String LENDING_SETTLE_FORECLOSURE = "LENDING-SETTLE-FORECLOSURE";
    public static final String LENDING_APPLYPAYMENT_PR_COLLECTION = "LENDING-APPLYPAYMENT-PR.COLLECTION";
    public static final String LENDING_MATURE_ARRANGEMENT = "LENDING-MATURE-ARRANGEMENT";

    public static final String NO_REC_ERR = "EB-NO.REC.SELECT";

    Set<String> activityList = new HashSet<>(
            Set.of(LENDING_APPLYPAYMENT_WRITEOFF_SETTLEMENT, LENDING_APPLYPAYMENT_PR_CURR_BALANCE,
                    LENDING_APPLYPAYMENT_INSURANCE_SETTLEMENT, LENDING_APPLYPAYMENT_PR_INSURANCE_BALANCES,
                    LENDING_SETTLE_FORECLOSURE, LENDING_APPLYPAYMENT_PR_OUTSTANDING_PAYOFF));

    Set<String> activityProps = new HashSet<>(Set.of("PRINTEREST.SM0PRINTEREST", "PRINTEREST.SM1PRINTEREST",
            "PRINTEREST.SM2PRINTEREST", "PRINTEREST.NPAPRINTEREST", "PRINTEREST.DUEPRINTEREST",
            "PRINTEREST.ACCPRINTEREST", "PRINTEREST.SM0PRINTERESTCUST", "PRINTEREST.SM1PRINTERESTCUST",
            "PRINTEREST.SM2PRINTERESTCUST", "PRINTEREST.NPAPRINTERESTCUST", "PRINTEREST.DUEPRINTERESTCUST",
            "PRINTEREST.ACCPRINTERESTCUST", "ACCOUNT.SM0ACCOUNT", "ACCOUNT.SM1ACCOUNT", "ACCOUNT.SM2ACCOUNT",
            "ACCOUNT.NPAACCOUNT", "ACCOUNT.DUEACCOUNT", "ACCOUNT.CURACCOUNT", "ACCOUNT.SM0ACCOUNTCUST",
            "ACCOUNT.SM1ACCOUNTCUST", "ACCOUNT.SM2ACCOUNTCUST", "ACCOUNT.NPAACCOUNTCUST", "ACCOUNT.DUEACCOUNTCUST",
            "ACCOUNT.CURACCOUNTCUST"));

    List<String> retvalues = new ArrayList<>();
    List<String> outvalues = new ArrayList<>();
    List<String> finalArrList = new ArrayList<>();

    DataAccess da = new DataAccess(this);

    String selGroup = "";
    String seluser = "";
    String coCode = "";

    String selMonth = "";
    String selRo = "";
    String selProduct = "";
    String selCenterName = "";
    String selVillage = "";
    String selBranch = "";
    String startDate = "";
    String endDate = "";
    String todayDate = "";
    String finMnemonic = "";
    String branchName = "";
    String branchCode = "";
    String zoneName = "";
    String regionName = "";
    String divisionName = "";
    String clusterName = "";
    String companyIds = "";
    String enquiryName = "";
    String closureDate = "";
    String ro = "";
    String product = "";
    String centre = "";
    String village = "";
    String selectionFilter = "";
    String selFilteredField = "";
    String settleClsContId = "";
    String settleClsActRefId = "";
    String writeOffClosureDt = "";
    String writeOffActRefId = "";
    String matureActRefId = "";
    String writeOffSettContractId = "";
    String insSettContractId = "";
    String repaymentContractId = "";
    String writeOffSettActRefId = "";
    String insSettActRefId = "";
    String repaymentActRefId = "";
    String insSettClosureDt = "";
    String settleClosureDt = "";
    String repaymentClosureDt = "";
    String maturityClosureDt = "";
    String writeOffSettClosureDt = "";
    String productName = "";
    String villageName = "";
    String centreName = "";
    String roName = "";
    String accNum = "";
    String settleClosureActRefId = "";
    String settleClosureContractId = "";

    // NB CHANGE
    String pastMonth = "";
    String datedefaultRange = "";
    int defaultDate;
    int daterange;

    double prinPaidAtClosure = 0.0;

    boolean settleClosureTriggered = false;

    boolean writeOffTriggered = false;
    boolean maturedTriggered = false;
    boolean writeOffSettAct = false;
    boolean insSettTriggered = false;
    boolean repaymentTriggered = false;
    boolean maturityTriggered = false;
    boolean activityFound = false;
    boolean writeOffSettTriggered = false;

    boolean dateGrp = false;

    Map<String, Integer> accountCount = new HashMap<>();

    Map<String, Double> principalPaidAtClosure = new HashMap<>();

    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
    DateTimeFormatter outDateFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
    DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HHmmss");

    @Override
    public List<String> setIds(List<FilterCriteria> filterCriteria, EnquiryContext enquiryContext) {
        try {

            Session session = new Session(this);
            todayDate = session.getCurrentVariable("!TODAY");
            Contract contract = new Contract(this);

            getFilterCriteriaDets(filterCriteria);
            getDefaultDatRange();
            getMaxdateRangedet();
            validateDateRange(daterange, defaultDate);

            Set<String> closedArrSet = new LinkedHashSet<>(da.selectRecords(finMnemonic, "AA.ARRANGEMENT", "",
                    "WITH ARR.STATUS EQ CLOSE PENDING.CLOSURE AND CO.CODE EQ " + companyIds));

            processFinalArrList(closedArrSet, contract);

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
            String outputPath = filePath + FILE_NAME + "_" + selGroup + "-WISE" + "_" + branchName + "_" + seluser + "_"
                    + currDate + "_" + currTime + ".csv";
            writeToFile(outvalues, outputPath);

        } catch (T24CoreException e) {

            throw e;
        } catch (Exception e1) {

            throw new T24CoreException("Technical Error: " + e1.getMessage(), "EB.ERROR");
        }

        return retvalues;

    }

    public void processFinalArrList(Set<String> closedArrSet, Contract contract) {

        try {

            LocalDate today = LocalDate.parse(todayDate, formatter);
            LocalDate start = startDate.isEmpty() ? today.minusDays(defaultDate)
                    : LocalDate.parse(startDate, formatter);
            LocalDate end = endDate.isEmpty() ? today : LocalDate.parse(endDate, formatter);

            for (String contractId : closedArrSet) {

                closureDate = "";
                prinPaidAtClosure = 0.0;

                getAaActivityHistoryDets(contractId);

                if (closureDate != null && !closureDate.isEmpty()) {

                    LocalDate closedDt = LocalDate.parse(closureDate, formatter);

                    if (dateConditionCheck(closedDt, start, end)) {

                        processToAddvaluesList(contractId, contract);

                    }
                }
            }

            List<String> sortedKeys = new ArrayList<>(accountCount.keySet());

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

                row.add(String.valueOf(accountCount.get(currentGrp)));

                row.add(String.format("%.2f", principalPaidAtClosure.get(currentGrp)));

                retvalues.add(String.join("*", row));
                outvalues.add(String.join(",", row));
            }

        } catch (Exception e) {
            e.getMessage();
        }
    }

    public boolean dateConditionCheck(LocalDate closedDt, LocalDate start, LocalDate end) {
        return (closedDt.isEqual(start) || closedDt.isAfter(start))
                && (closedDt.isEqual(end) || closedDt.isBefore(end));
    }

    public void processToAddvaluesList(String contractId, Contract contract) {

        try {

            ro = "";
            product = "";
            centre = "";
            village = "";
            roName = "";
            productName = "";
            centreName = "";
            villageName = "";

            contract.setContractId(contractId);
            getAaArrangementDets(contractId);
            String groupValue = getGroupBasedValue(accNum);

            if (chkSelectionBased()) {
                return;
            }

            if (groupValue != null && !groupValue.isEmpty()) {

                accountCount.put(groupValue, accountCount.getOrDefault(groupValue, 0) + 1);

                principalPaidAtClosure.put(groupValue,
                        principalPaidAtClosure.getOrDefault(groupValue, 0.0) + Math.abs(prinPaidAtClosure));

            }
        } catch (Exception e) {
            e.getMessage();
        }
    }

    public void getAaArrangementDets(String arrId) {

        try {
            AaArrangementRecord arrRec = new AaArrangementRecord(
                    da.getRecord(finMnemonic, "AA.ARRANGEMENT", "", arrId));

            coCode = arrRec.getCoCodeRec().getValue();

            product = arrRec.getProduct().get(0).getProduct().getValue();

            accNum = arrRec.getLinkedAppl().get(0).getLinkedApplId().getValue();
            getAaProductDetails(product);

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

    public void getAccountDets(String accNum) {
        try {
            AccountRecord accRec = new AccountRecord(da.getRecord(finMnemonic, ACCOUNT, "", accNum));

            centre = accRec.getLocalRefField("FF.CENTRE").getValue();

            village = accRec.getLocalRefField("FF.VILLAGE").getValue();

            if (village != null && !village.isEmpty()) {
                getEbFfVillageDetails(village);

            }
            if (centre != null && !centre.isEmpty()) {
                getEbFfCentreDetails(centre);

            }
        } catch (Exception e) {
            e.getMessage();
        }
    }

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

    public boolean chkSelectionBased() {
        return (selRo != null && !selRo.isEmpty() && !selRo.equals(ro))
                || (selProduct != null && !selProduct.isEmpty() && !selProduct.equals(product))
                || (selCenterName != null && !selCenterName.isEmpty() && !selCenterName.equals(centre))
                || (selVillage != null && !selVillage.isEmpty() && !selVillage.equals(village));
    }

    public String getGroupBasedValue(String accNum) {

        String groupKey = "";

        switch (selGroup) {

        case "BRANCH":
            selFilteredField = "Branch";
            groupKey = getCompanyDescription(coCode);
            break;

        case "DATE":
            selFilteredField = "Date";

            if (closureDate != null && !closureDate.isEmpty()) {
                LocalDate date = LocalDate.parse(closureDate, formatter);
                groupKey = date.format(outDateFormatter);
            }

            dateGrp = true;
            break;
        case "RO":
            selFilteredField = "RO";
            getAccountDets(accNum);
            groupKey = roName;
            break;
        case "PRODUCT":
            selFilteredField = "Product";
            groupKey = productName;
            break;
        case "CENTER":
            selFilteredField = "CenterName";
            getAccountDets(accNum);
            groupKey = centreName;
            break;
        case "VILLAGE":
            selFilteredField = "Village";
            getAccountDets(accNum);
            groupKey = villageName;
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
                    initialiseCompanyInfo(selBranch);
                    getLinkedCompIds(selBranch);
                    break;

                case "GROUP.BY":
                    selGroup = value;
                    break;
                case "USER":
                    seluser = value;
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
        } catch (Exception e) {
            e.getMessage();
        }
    }

    private void validateDateRange(int maxHistory, int maxRange) {
        LocalDate today = LocalDate.parse(todayDate, formatter);
        LocalDate maxBackDate = today.minusDays(maxHistory);

        if (startDate.isEmpty() && endDate.isEmpty()) {
            this.startDate = today.minusDays(maxRange).format(formatter);
            this.endDate = today.format(formatter);

            return;
        }

        LocalDate stDt = startDate.isEmpty() ? today : LocalDate.parse(startDate, formatter);
        LocalDate endDt = endDate.isEmpty() ? today : LocalDate.parse(endDate, formatter);

        if (stDt.isAfter(endDt)) {

            LocalDate tmp = stDt;
            stDt = endDt;
            endDt = tmp;
        }

        long daysBetween = ChronoUnit.DAYS.between(stDt, endDt);

        if (stDt.isBefore(maxBackDate)) {

            throw new T24CoreException(covertParamValue(pastMonth), "EB-FF.BM.PAST.MAX.DT.RANGE");
        }

        if (daysBetween > maxRange) {

            throw new T24CoreException(covertParamValue(datedefaultRange), "EB-FF.BM.DATE.FILTER.RANGE");
        }

        this.startDate = stDt.format(formatter);
        this.endDate = endDt.format(formatter);
    }

    private String covertParamValue(String value) {

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

    private void getMaxdateRangedet() {
        String paramId = "FF.BM.REPORT.DATE.RANGE";

        EbFfParameterRecord paramRec = new EbFfParameterRecord(da.getRecord(EB_FF_PARAMETER, paramId));

        for (ParamDescClass paramDesc : paramRec.getParamDesc()) {

            if (paramDesc.getParamName().getValue().equals("CLOSURE")) {

                pastMonth = paramDesc.getParamValue().getValue();

                if (pastMonth.contains("3M")) {

                    daterange = 90;

                }

            }

        }

    }

    private void getDefaultDatRange() {
        String paramId = "FF.BM.REPORT.DATE.DEFAULT";

        EbFfParameterRecord paramRec = new EbFfParameterRecord(da.getRecord(EB_FF_PARAMETER, paramId));

        for (ParamDescClass paramDesc : paramRec.getParamDesc()) {

            if (paramDesc.getParamName().getValue().equals("CLOSURE")) {

                datedefaultRange = paramDesc.getParamValue().getValue();

                if (datedefaultRange.contains("1M")) {

                    defaultDate = 31;

                }

            }

        }

    }

    public void initialiseCompanyInfo(String companyId) {
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

        String compName = "";
        try {
            if (companyCode != null && !companyCode.isEmpty()) {
                CompanyRecord companyRec = new CompanyRecord(da.getRecord("COMPANY", companyCode));
                compName = companyRec.getCompanyName().get(0).getValue();
                String[] compNamePart = compName.split("-");
                compName = compNamePart[0];
                return compName;
            }
        } catch (Exception e) {

            e.getMessage();
        }
        return compName;
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

    public void getAaActivityHistoryDets(String arrId) {
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

            getAaActivityBalDetails(arrId, activityList, activityProps, null);

            if (writeOffTriggered) {
                closureDate = writeOffClosureDt;

            } else if (writeOffSettTriggered) {
                closureDate = writeOffSettClosureDt;

            } else if (insSettTriggered) {
                closureDate = insSettClosureDt;

            } else if (settleClosureTriggered) {
                closureDate = settleClosureDt;

            } else if ((maturityTriggered && repaymentTriggered) && (maturityClosureDt.equals(repaymentClosureDt))
                    && (prinPaidAtClosure == 0.0)) {
                closureDate = repaymentClosureDt;

                activityList.clear();
                activityList.add(LENDING_APPLYPAYMENT_PR_COLLECTION);
                getAaActivityBalDetails(arrId, activityList, activityProps, maturityClosureDt);
            }

            prinPaidAtClosure = Math.abs(prinPaidAtClosure);

        } catch (Exception e) {
            e.getMessage();
        }
    }

    public void processBasedOnActivity(EffectiveDateClass effectiveDate, ActivityRefClass activeRef) {
        String activity = activeRef.getActivity().getValue();
        String actStatus = activeRef.getActStatus().getValue();
        String initiation = activeRef.getInitiation().getValue();

        if (initiation.equalsIgnoreCase(SECONDARY)) {
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

                }
            }
        } catch (NumberFormatException e) {
            e.getMessage();
        }
    }

    public void getAaActivityBalDetails(String arrId, Set<String> activityList, Set<String> activityProps,
            String matDate) {

        try {
            AaActivityBalancesRecord aaActbalRec = new AaActivityBalancesRecord(
                    da.getRecord(finMnemonic, "AA.ACTIVITY.BALANCES", "", arrId));
            for (com.temenos.t24.api.records.aaactivitybalances.ActivityRefClass accRefList : aaActbalRec
                    .getActivityRef()) {
                if (matDate != null && !matDate.isEmpty() && !matDate.equals(accRefList.getActivityDate().getValue())) {
                    continue;
                }

                String activity = accRefList.getActivity().getValue();
                if (activityList.contains(activity)) {
                    sumPropertyAmounts(accRefList, activityProps);
                }
            }
        } catch (Exception e) {
            e.getMessage();
        }

    }

    public void sumPropertyAmounts(com.temenos.t24.api.records.aaactivitybalances.ActivityRefClass accRefList,
            Set<String> propertyNames) {

        try {
            for (com.temenos.t24.api.records.aaactivitybalances.PropertyClass prop : accRefList.getProperty()) {
                String propName = prop.getProperty().getValue();
                double propAmt = Double.parseDouble(prop.getPropertyAmt().getValue());
                if (propertyNames.contains(propName)) {

                    if (propName.startsWith(ACCOUNT)) {
                        prinPaidAtClosure += propAmt;
                    }
                }
            }

        } catch (NumberFormatException e) {
            e.getMessage();
        } catch (Exception e1) {
            e1.getMessage();
        }
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
                        String header = String.join(",", "Zone Name", "Region Name", "Division Name", "Cluster Name",
                                "Branch Code", "Branch Name", selFilteredField, "Account Count (closed)",
                                "Principal paid at closure");
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