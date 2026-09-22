package com.temenos.fusion;

import java.io.File;
import java.io.FileWriter;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import com.temenos.api.TField;
import com.temenos.api.exceptions.T24CoreException;
import com.temenos.t24.api.arrangement.accounting.Contract;
import com.temenos.t24.api.complex.aa.contractapi.BalanceMovement;
import com.temenos.t24.api.complex.eb.enquiryhook.EnquiryContext;
import com.temenos.t24.api.complex.eb.enquiryhook.FilterCriteria;
import com.temenos.t24.api.hook.system.Enquiry;
import com.temenos.t24.api.records.aaaccountdetails.AaAccountDetailsRecord;
import com.temenos.t24.api.records.aaarrangement.AaArrangementRecord;
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

public class FfNofileFutureMatLoanRptSummary extends Enquiry {
    public static final String DATE_RANGE_ERR = "EB-FF.BM.FUTURE.MAX.DT.RANGE";
    public static final String DATE_FILTER_ERR = "EB-FF.BM.DATE.FILTER.RANGE";
    public static final String NO_REC_ERR = "EB-NO.REC.SELECT";
    public static final String TRADE = "TRADE";
    public static final String FILE_NAME = "FutureMaturityLoanRep_Sum";

    List<String> retvalues = new ArrayList<>();
    List<String> outvalues = new ArrayList<>();
    DataAccess da = new DataAccess(this);

    String selUser = "";
    String selGroup = "";
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

    String maturityDate = "";
    String coCode = "";
    String ro = "";
    String product = "";
    String centre = "";
    String village = "";
    String roName = "";
    String productName = "";
    String centreName = "";
    String villageName = "";
    String accNum = "";
    String selFilterField = "";
    String dateRangeVal = "";
    String daetFilterVal = "";

    String outputPath = "";

    boolean dateRangeErrFlag = false;
    boolean dateFilterErrFlag = false;
    boolean dateGrp = false;
    boolean compDescFlg = false;

    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
    DateTimeFormatter outDateFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
    DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HHmmss");

    @Override
    public List<String> setIds(List<FilterCriteria> filterCriteria, EnquiryContext enquiryContext) {

        Session session = new Session(this);
        todayDate = session.getCurrentVariable("!TODAY");
        Contract contract = new Contract(this);

        getFilterCriteriaDets(filterCriteria);

        try {
            Set<String> futureArrSet = new LinkedHashSet<>(da.selectRecords(finMnemonic, "AA.ARRANGEMENT", "",
                    "WITH ARR.STATUS NE PENDING.CLOSURE CLOSE AND CO.CODE EQ " + companyIds));
            processFinalArrList(futureArrSet, contract);

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
            outputPath = filePath + FILE_NAME + "_" + selGroup + "-WISE" + "_" + branchName + "_" + selUser + "_"
                    + currDate + "_" + currTime + ".csv";

            writeToFile(outvalues, outputPath);

        } catch (Exception e) {
            e.getMessage();
        }

        if (retvalues.isEmpty()) {
            throw new T24CoreException("", NO_REC_ERR);
        } else {
            return retvalues;
        }
    }

    public void processFinalArrList(Set<String> futureArrSet, Contract contract) {
        Map<String, Integer> accountCount = new HashMap<>();
        Map<String, Double> outstandingBal = new HashMap<>();
        String dateParamName = "FUTURE.MATURITY.LOAN";
        String defDtParamId = "FF.BM.REPORT.DATE.DEFAULT";
        try {
            daetFilterVal = getEbFfParamRecDets(defDtParamId, dateParamName);
            LocalDate today = LocalDate.parse(todayDate, formatter);
            LocalDate start = startDate.isEmpty() ? today : LocalDate.parse(startDate, formatter);
            LocalDate end = endDate.isEmpty() ? getEndDateBasedOnParamRec(daetFilterVal, today)
                    : LocalDate.parse(endDate, formatter);
            for (String arrId : futureArrSet) {
                LocalDate matDt = getMatDateFromAccountDets(arrId);
                if (validateDateRange(matDt, start, end)) {

                    ro = "";
                    product = "";
                    centre = "";
                    village = "";
                    roName = "";
                    productName = "";
                    centreName = "";
                    villageName = "";
                    contract.setContractId(arrId);
                    getAaArrangementDets(arrId);

                    String groupValue = getGroupBasedValue(accNum);

                    boolean selectionCheck = chkSelectionBased();
                    if (selectionCheck) {
                        continue;
                    }

                    double outstanding = Double.parseDouble(getBalance(contract, "FFPRINODFUTAMT", TRADE));

                    if (groupValue != null && !groupValue.isEmpty()) {
                        accountCount.put(groupValue, accountCount.getOrDefault(groupValue, 0) + 1);
                        outstandingBal.put(groupValue,
                                outstandingBal.getOrDefault(groupValue, 0.0) + Math.abs(outstanding));
                    }
                }

            }
            List<String> sortedKeys = processSortingForDateGrp(accountCount);
            processBuildRowValues(sortedKeys, accountCount, outstandingBal);

        } catch (Exception e) {
            e.getMessage();
        }
    }

    public void processBuildRowValues(List<String> sortedKeys, Map<String, Integer> accountCount,
            Map<String, Double> outstandingBal) {
        for (String currentGrp : sortedKeys) {
            List<String> row = new ArrayList<>();
            row.add(zoneName);
            row.add(regionName);
            row.add(divisionName);
            row.add(clusterName);
            row.add(branchCode);
            row.add(branchName);
            if (dateGrp) {
                row.add(convertDate(currentGrp));
            } else {
                row.add(currentGrp);
            }
            row.add(String.valueOf(accountCount.getOrDefault(currentGrp, 0)));
            row.add(String.format("%.2f", outstandingBal.getOrDefault(currentGrp, 0.0)));
            retvalues.add(String.join("*", row));
            outvalues.add(String.join(",", row));
        }
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

    public boolean validateDateRange(LocalDate matDt, LocalDate start, LocalDate end) {
        if (matDt == null || start == null || end == null) {
            return false;
        }
        return (matDt.isEqual(start) || matDt.isAfter(start)) && (matDt.isEqual(end) || matDt.isBefore(end));
    }

    public void getAaArrangementDets(String arrId) {
        try {
            AaArrangementRecord arrRec = new AaArrangementRecord(
                    da.getRecord(finMnemonic, "AA.ARRANGEMENT", "", arrId));
            product = arrRec.getProduct().get(0).getProduct().getValue();
            accNum = arrRec.getLinkedAppl().get(0).getLinkedApplId().getValue();
            coCode = arrRec.getCoCodeRec().getValue();

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
            AccountRecord accRec = new AccountRecord(da.getRecord(finMnemonic, "ACCOUNT", "", accNum));
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

    public LocalDate getMatDateFromAccountDets(String arrId) {
        try {
            AaAccountDetailsRecord aaAccountDets = new AaAccountDetailsRecord(
                    da.getRecord(finMnemonic, "AA.ACCOUNT.DETAILS", "", arrId));
            maturityDate = aaAccountDets.getMaturityDate().getValue();
            if (maturityDate != null && !maturityDate.isEmpty()) {
                return LocalDate.parse(maturityDate, formatter);
            }
        } catch (Exception e) {
            e.getMessage();
        }
        return null;
    }

    public boolean chkSelectionBased() {
        return (selRo != null && !selRo.isEmpty() && !selRo.equals(ro))
                || (selProduct != null && !selProduct.isEmpty() && !selProduct.equals(product))
                || (selCenterName != null && !selCenterName.isEmpty() && !selCenterName.equals(centre))
                || (selVillage != null && !selVillage.isEmpty() && !selVillage.equals(village));
    }

    public List<String> processSortingForDateGrp(Map<String, Integer> accountCount) {
        List<String> sortedKeys = new ArrayList<>(accountCount.keySet());
        if (dateGrp) {
            Collections.sort(sortedKeys);
        }
        return sortedKeys;
    }

    public String getGroupBasedValue(String accNum) {
        String groupKey = "";

        switch (selGroup) {
        case "BRANCH":
            selFilterField = "Branch";
            groupKey = getCompanyDescription(coCode);
            break;
        case "DATE":
            selFilterField = "Date";
            groupKey = maturityDate;
            dateGrp = true;
            break;
        case "RO":
            selFilterField = "RO";
            getAccountDets(accNum);
            groupKey = roName;
            break;
        case "PRODUCT":
            selFilterField = "Product";
            groupKey = productName;
            break;
        case "CENTER":
            selFilterField = "CenterName";
            getAccountDets(accNum);
            groupKey = centreName;
            break;
        case "VILLAGE":
            selFilterField = "Village";
            getAccountDets(accNum);
            groupKey = villageName;
            break;
        default:
        }
        return groupKey;
    }

    public String getBalance(Contract contract, String accountType, String bookingType) {
        try {
            List<BalanceMovement> movements = contract.getContractBalanceMovements(accountType, bookingType);
            if (movements != null && !movements.isEmpty() && movements.get(0).getBalance() != null) {
                return movements.get(0).getBalance().toString();
            }
        } catch (Exception e) {
            e.getMessage();
        }
        return "0";
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
        } catch (Exception e) {
            e.getMessage();
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

    public LocalDate getEndDateBasedOnParamRec(String paramValue, LocalDate stDt) {
        LocalDate expectedEndDt = null;
        try {
            if (paramValue.endsWith("D")) {
                int allowedDays = Integer.parseInt(paramValue.replace("D", ""));
                expectedEndDt = stDt.plusDays(allowedDays);
                return expectedEndDt;
            } else if (paramValue.endsWith("M")) {
                int allowedMonths = Integer.parseInt(paramValue.replace("M", ""));
                expectedEndDt = stDt.plusMonths(allowedMonths);
                return expectedEndDt;
            }
        } catch (NumberFormatException e) {
            e.getMessage();
        }
        return expectedEndDt;
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
                                "BranchCode", "BranchName", selFilterField, "AccountCount", "Outstanding");
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