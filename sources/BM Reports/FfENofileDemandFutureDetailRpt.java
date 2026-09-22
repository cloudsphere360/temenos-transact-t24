package com.temenos.fusion;

import java.io.File;
import java.io.FileWriter;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.ibm.icu.math.BigDecimal;
import com.temenos.api.TField;
import com.temenos.api.exceptions.T24CoreException;
import com.temenos.t24.api.arrangement.accounting.Contract;
import com.temenos.t24.api.complex.eb.enquiryhook.EnquiryContext;
import com.temenos.t24.api.complex.eb.enquiryhook.FilterCriteria;
import com.temenos.t24.api.hook.system.Enquiry;
import com.temenos.t24.api.records.aaarrangement.AaArrangementRecord;
import com.temenos.t24.api.records.aaarrangement.LinkedApplClass;
import com.temenos.t24.api.records.aaarrtermamount.AaArrTermAmountRecord;
import com.temenos.t24.api.records.aacustomerarrangement.AaCustomerArrangementRecord;
import com.temenos.t24.api.records.aacustomerarrangement.ArrangementClass;
import com.temenos.t24.api.records.aacustomerarrangement.ProductLineClass;
import com.temenos.t24.api.records.account.AccountRecord;
import com.temenos.t24.api.records.account.AltAcctTypeClass;
import com.temenos.t24.api.records.company.CompanyRecord;
import com.temenos.t24.api.records.companyconsol.CompanyConsolRecord;
import com.temenos.t24.api.records.customer.CustomerRecord;
import com.temenos.t24.api.records.ebffcentredetail.EbFfCentreDetailRecord;
import com.temenos.t24.api.records.ebffcollectiondets.EbFfCollectionDetsRecord;
import com.temenos.t24.api.records.ebffgroups.EbFfGroupsRecord;
import com.temenos.t24.api.records.ebffloandetails.AddressTypeClass;
import com.temenos.t24.api.records.ebffloandetails.CcProductClass;
import com.temenos.t24.api.records.ebffloandetails.EbFfLoanDetailsRecord;
import com.temenos.t24.api.records.ebffparameter.EbFfParameterRecord;
import com.temenos.t24.api.records.ebffparameter.ParamDescClass;
import com.temenos.t24.api.records.ebffrouser.EbFfRoUserRecord;
import com.temenos.t24.api.records.user.UserRecord;
import com.temenos.t24.api.system.DataAccess;
import com.temenos.t24.api.system.Session;

/**
 * @author Kavin Prabha M Date Created: 23.12.2025 Attached
 *         as:NofileEnquiryRoutine EB.API :FF.E.NOFILE.DE.FUTURE.DETAIL.RPT
 *         ENQUIRY >NOFILE.FF.DMD.FUT.DETAIL.RPT ------------OLD
 *         ENQUIRY>FF.DMD.FUT.RPT.DETAIL----NEW*06MAY2026 STANDARD.SELECTION
 *         >NOFILE.FF.DE.FUTURE.DETAIL.RPT Description: BM Online Report
 *         generation ->Demand Future Detailed Report
 */
public class FfENofileDemandFutureDetailRpt extends Enquiry {
    public static final String DATE_RANGE_ERR = "EB-FF.BM.FUTURE.MAX.DT.RANGE";
    public static final String DATE_FILTER_ERR = "EB-FF.BM.DATE.FILTER.RANGE";
    public static final String NO_REC_ERR = "EB-NO.REC.SELECT";
    private static final String AA_ARRANGEMENT_RECORD = "AA.ARRANGEMENT";
    public static final String SEL_APP_CUS = "CUSTOMER";
    public static final String SEL_APP_COMP = "COMPANY";
    public static final String FILE_NAME = "DemandFutRep_Det";

    DataAccess da = new DataAccess(this);
    List<String> outArrList = new ArrayList<>();
    List<String> returnvalues = new ArrayList<>();
    List<LinkedApplClass> linkedAppList = null;
    List<ProductLineClass> productLineList = null;

    Set<String> filterValSet = new HashSet<>();
    Set<String> productSet = new HashSet<>();
    Set<String> centreSet = new HashSet<>();
    Set<String> villageSet = new HashSet<>();
    Set<String> filteredArrList = new LinkedHashSet<>();
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
    List<String> outvalues = new ArrayList<>();
    DateTimeFormatter outDateFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
    DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HHmmss");

    boolean noRecErrFlag = false;
    boolean dateFilterErrFlag = false;
    boolean dateRangeErrFlag = false;

    String todayDate = "";
    String finMnemonic = "";
    String startDate = "";
    String endDate = "";
    String selDate = "";
    String selDateOp = "";
    String selProduct = "";
    String selCentreName = "";
    String selVillage = "";
    String date = "";
    String selDistrict = "";
    String selLoanCycle = "";
    String selLoanPurp = "";
    String selReligGrp = "";
    String selCaste = "";
    String dateOfBirth = "";
    String companyId = "";

    String zoneName = "";
    String regionName = "";
    String divisionName = "";
    String clusterName = "";
    String branchName = "";
    String branchCode = "";
    String branchDistrict = "";
    String branchState = "";
    String centerName = "";
    String centerId = "";
    String ebgroupName = "";
    String groupName = "";
    String groupCode = "";
    String customerName = "";
    String customerNumber = "";
    int customerAge = 0;
    String caste = "";
    String occupation = "";
    String purpose = "";
    String loanacctNum = "";
    String legacyAcctnum = "";
    String disbursementDate = "";
    String loanAmount = "";
    String relationshipOfficerName = "";
    String relationshipOfficerMobileNumber = "";
    String branchManagerName = "";
    String branchManagerMobileNumber = "";
    double principalDemandBal = 0.0;
    double interestDemandBal = 0.0;
    double feeDemandBal = 0.0;
    double totalDemandBal = 0.0;
    String cycle = "";
    String centre = "";
    String village = "";
    String cusmnemonic = "";
    String fstName = "";
    String scdName = "";
    String familyName = "";
    String customerid = "";
    String religGrp = "";
    String companyName = "";
    String officerCode = "";
    String productDet = "";
    String branch = "";
    String displayDate = "";
    String companyIds = "";
    String mnemonic = "";
    String origContractDate = "";
    boolean legacy = false;
    String ccDate = "";
    String ccAmount = "";
    Double cashCarryTotal = 0.0;
    Session session = new Session(this);
    String dateRangeVal = "";
    String dateFilterVal = "";
    String acctNo = "";
    String selUser = "";

    /**
     * METHOD: setIds DESCRIPTION:The setIds method retrieves and filters current
     * lending arrangements based on input filter criteria and a date range, gathers
     * customer, product, branch, and demand-related details, and returns the result
     * as a list of formatted output records.
     */

    @Override
    public List<String> setIds(List<FilterCriteria> filterCriteria, EnquiryContext enquiryContext) {

        try {
            todayDate = session.getCurrentVariable("!TODAY");
            companyId = session.getCompanyId();
            Contract contract = new Contract(this);

            for (FilterCriteria filter : filterCriteria) {
                if (filter.getFieldname().equals("BRANCH")) {
                    branch = filter.getValue();
                    initialiseCompanyInfo(branch);
                    break;
                }
            }
            branch = getBranchId(filterCriteria);
            getLinkedCompIds(branch);

            if (!branch.equals("")) {
                initialiseCompanyInfo(branch);
            } else {
                initialiseCompanyInfo(companyId);
            }
            Set<String> selectionSet = getFilterCriteriaDets(filterCriteria);
            processDateValidation(startDate, endDate);
            if (dateFilterErrFlag) {
                throw new T24CoreException(covertParamValue(dateFilterVal), DATE_FILTER_ERR);
            }
            if (dateRangeErrFlag) {
                throw new T24CoreException(covertParamValue(dateRangeVal), DATE_RANGE_ERR);
            }

            Set<String> demandFutureArrSetObj = new LinkedHashSet<>(da.selectRecords(finMnemonic, AA_ARRANGEMENT_RECORD,
                    "",
                    "WITH ARR.STATUS NE CLOSE AND ARR.STATUS NE PENDING.CLOSURE AND ARR.STATUS NE EXPIRED AND CO.CODE EQ "
                            + companyIds));
            Set<String> outFinalSet = getoutFinalset(demandFutureArrSetObj, selectionSet);
            outFinalSet.retainAll(demandFutureArrSetObj);
            processFinalArrList(contract, outFinalSet);

            String fileParamId = "FF.BM.REPORT.EXTRACT";
            String fileParamName = "Path";
            String filePath = getEbFfParamRecDets(fileParamId, fileParamName);

            LocalDateTime currDtTime = LocalDateTime.now();
            String currDate = currDtTime.format(outDateFormatter);
            String currTime = currDtTime.format(timeFormatter);
            String outputPath = filePath + FILE_NAME + "_" + branchName + "_" + selUser + "_" + currDate + "_"
                    + currTime + ".csv";
            writeToFile(outvalues, outputPath);

        } catch (Exception e1) {
            e1.getMessage();
            throw e1;
        }
        if (noRecErrFlag || returnvalues.isEmpty()) {
            throw new T24CoreException("", NO_REC_ERR);
        } else {
            return returnvalues;
        }
    }

    /**
     * @param daetFilterVal2
     * @return
     */
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

    /**
     * @param contract
     * @param outFinalSet
     */
    private void processFinalArrList(Contract contract, Set<String> outFinalSet) {

        try {
            LocalDate today = LocalDate.parse(todayDate, formatter);
            LocalDate start = startDate.isEmpty() ? today : LocalDate.parse(startDate, formatter);
            LocalDate end = endDate.isEmpty() ? getEndDateBasedOnParamRec(dateFilterVal, today)
                    : LocalDate.parse(endDate, formatter);

            for (String selectionArrId : outFinalSet) {
                EbFfCollectionDetsRecord colldetsRec = new EbFfCollectionDetsRecord(
                        da.getRecord(finMnemonic, "EB.FF.COLLECTION.DETS", "", selectionArrId));
                if (isDueDateInRange(colldetsRec, start, end)) {
                    processSingleArrangement(contract, start, end, selectionArrId);
                }
            }
        } catch (Exception e3) {
            e3.getMessage();
        }
    }

    /**
     * METHOD:processSingleArrangement DESCRIPTION:Processes a single arrangement by
     * retrieving its account details and validating its start date within the given
     * range. If valid, it collects related data and builds the final output entry
     * for the arrangement.
     */

    public void processSingleArrangement(Contract contract, LocalDate start, LocalDate end, String selectionArrId) {
        try {
            outArrList.add(selectionArrId);
            contract.setContractId(selectionArrId);
            getArrangementFieldMappingDet(selectionArrId);
            getEbFfLoanDetails(selectionArrId);
            getAccountDetails(acctNo);
            getaaArrTermAmount(contract);
            getEbFfCollectionDets(selectionArrId, start, end);
            buildAndAddRow();

        } catch (Exception e4) {
            e4.getMessage();
        } finally {

            cashCarryTotal = 0.0;
            customerAge = 0;
            legacyAcctnum = "";
            occupation = "";
            purpose = "";
            loanacctNum = "";
            disbursementDate = "";
            loanAmount = "";
            cycle = "";
            centre = "";
            village = "";
            customerName = "";
            customerNumber = "";
            customerid = "";
            mnemonic = "";
            caste = "";
            religGrp = "";
            centerName = "";
            centerId = "";
            groupName = "";
            ebgroupName = "";
            relationshipOfficerName = "";
            relationshipOfficerMobileNumber = "";
            branchManagerName = "";
            branchManagerMobileNumber = "";
            productDet = "";
            origContractDate = "";
            acctNo = "";
            legacy = false;

        }
    }

    /**
     * @param colldetsRec
     * @param start
     * @param end
     * @return
     */
    private boolean isDueDateInRange(EbFfCollectionDetsRecord colldetsRec, LocalDate start, LocalDate end) {
        try {
            List<TField> duedateList = colldetsRec.getDueDate();
            if (duedateList == null)
                return false;

            for (TField duedateField : duedateList) {
                String duedateStr = duedateField.getValue();
                if (duedateStr == null || duedateStr.isEmpty())
                    continue;

                LocalDate duedate = LocalDate.parse(duedateStr, formatter);
                if (!duedate.isBefore(start) && !duedate.isAfter(end)) {
                    return true;
                }
            }

        } catch (Exception e) {
            e.getMessage();
        }
        return false;
    }

    /**
     * @param startDate2
     * @param endDate2
     */
    private void processDateValidation(String startDate, String endDate) {
        try {

            String dateParamId = "FF.BM.REPORT.DATE.RANGE";/// 1M
            String dateParamName = "DEMAND.FUTURE";
            String defDtParamId = "FF.BM.REPORT.DATE.DEFAULT";// 7DAYS

            dateRangeVal = getEbFfParamRecDets(dateParamId, dateParamName);
            dateFilterVal = getEbFfParamRecDets(defDtParamId, dateParamName);

            if ((startDate != null && !startDate.isEmpty()) && (endDate != null && !endDate.isEmpty())) {
                LocalDate todayDt = LocalDate.parse(todayDate, formatter);
                LocalDate stDt = LocalDate.parse(startDate, formatter);
                LocalDate endDt = LocalDate.parse(endDate, formatter);

                LocalDate expectedEndDt = getEndDateBasedOnParamRec(dateRangeVal, todayDt);
                boolean isRangeValid = !stDt.isBefore(todayDt) && !endDt.isAfter(expectedEndDt);
                if (!isRangeValid) {
                    dateRangeErrFlag = true;
                }

                LocalDate defExpEnddate = getEndDateBasedOnParamRec(dateFilterVal, stDt);

                if (endDt.isAfter(defExpEnddate)) {
                    dateFilterErrFlag = true;
                }
            }

        } catch (Exception e2) {
            e2.getMessage();
        }
    }

    /**
     * @param dateRangeVal2
     * @param todayDt
     * @return
     */
    private LocalDate getEndDateBasedOnParamRec(String paramValue, LocalDate stDt) {

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
        } catch (NumberFormatException e6) {
            e6.getMessage();
        }
        return expectedEndDt;
    }

    /**
     * @param fileParamId
     * @param fileParamName
     * @return
     */
    private String getEbFfParamRecDets(String paramId, String paramName) {

        String paramVal = "";
        try {
            EbFfParameterRecord paramRec = new EbFfParameterRecord(da.getRecord("EB.FF.PARAMETER", paramId));
            for (ParamDescClass paramDesc : paramRec.getParamDesc()) {
                if (paramDesc.getParamName().getValue().equals(paramName)) {
                    paramVal = paramDesc.getParamValue().getValue();
                    return paramVal;
                }
            }
        } catch (Exception e7) {
            e7.getMessage();
        }
        return paramVal;
    }

    /**
     * METHOD:buildAndAddRow Description:Builds a data row from arrangement, branch,
     * customer, and loan details, then adds it to output lists in both '*' and ','
     * delimited formats.
     */
    private void buildAndAddRow() {
        try {
            List<String> row = new ArrayList<>();
            row.add(zoneName);
            row.add(regionName);
            row.add(divisionName);
            row.add(clusterName);
            row.add(branchName);
            row.add(branchDistrict);
            row.add(branchState);
            row.add(branchCode);
            row.add(centerName);// centerName
            row.add(centerId);// centerCode
            row.add(groupName);
            row.add(ebgroupName);// groupCode
            row.add(customerName);
            row.add(customerNumber);
            row.add(String.valueOf(customerAge));
            row.add(caste);
            row.add(occupation);
            row.add(purpose);
            row.add(loanacctNum);
            row.add(legacyAcctnum);// *
            row.add(convertDate(disbursementDate));
            row.add(loanAmount);
            row.add(relationshipOfficerName);
            row.add(relationshipOfficerMobileNumber);
            row.add(branchManagerName);
            row.add(branchManagerMobileNumber);
            row.add(String.valueOf(principalDemandBal));
            row.add(String.valueOf(interestDemandBal));
            row.add(String.valueOf(feeDemandBal));
            row.add(String.valueOf(cashCarryTotal));
            row.add(String.format("%.2f", totalDemandBal));
            returnvalues.add(String.join("*", row));
            outvalues.add(String.join(",", row));
        } catch (Exception e) {
            e.getMessage();
        }
    }

    public String convertDate(String inDate) {
        String outDate = "";
        try {
            LocalDate dates = LocalDate.parse(inDate, formatter);
            outDate = dates.format(outDateFormatter);
            return outDate;
        } catch (Exception e) {
            e.getMessage();
        }
        return outDate;
    }

    /**
     * METHOD:writeToFile Description: his method creates the report output file in
     * the specified file path and writes the report data into it.
     */
    private void writeToFile(List<String> data, String filePath) {
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
                                "BranchName", "BranchDistrict", "BranchState", "BranchCode", "CenterName", "CenterCode",
                                "GroupName", "GroupCode", "CustomerName", "CustomerNumber", "CustomreAge", "Caste",
                                "Occupation", "Purpose", "LoanAccountNumber", "LegacyAcctountNumber",
                                "DisbursementDate", "LoanAmount", "RelationshipOfficerName",
                                "RelationshipOfficerMobileNumber", "BranchManagerName", "BranchMangerMobileNumber",
                                "PrincipalDemand", "InterestDemand", "FeeDemand", "CashCarryDemand", "TotalDemand");

                        writer.write(header + System.lineSeparator());
                    }
                    for (String line : data) {
                        writer.write(line + System.lineSeparator());
                    }
                }
            }
        } catch (Exception e8) {
            e8.getMessage();
        }

    }

    /**
     * METHOD:getLinkedCompIds DESCRIPTION: This method is used to get the values.
     */
    private void getLinkedCompIds(String branch) {
        StringBuilder company = new StringBuilder();
        try {
            List<String> comConsolRecList = da.selectRecords("", "COMPANY.CONSOL", "",
                    "WITH COM.CONSOL.TO EQ " + branch);
            if (!comConsolRecList.isEmpty()) {
                company.append(branch).append(" ");
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
                companyIds = branch;
            }

        } catch (Exception e9) {
            e9.getMessage();
        }
    }

    /**
     * METHOD:getBranchId DESCRIPTION: This method is used to get the branch value.
     */
    private String getBranchId(List<FilterCriteria> filterCriteria) {
        try {
            for (FilterCriteria filter : filterCriteria) {
                String value = filter.getValue();
                String field = filter.getFieldname();
                if ("BRANCH".equalsIgnoreCase(field)) {
                    branch = value;
                }
            }
        } catch (Exception e10) {
            e10.getMessage();
        }
        return branch;

    }

    /**
     * METHOD:initialiseCompanyInfo DESCRIPTION: This method is used to get the
     * values.
     */
    private void initialiseCompanyInfo(String companyId) {
        try {
            CompanyRecord companyObj = new CompanyRecord(da.getRecord(SEL_APP_COMP, companyId));
            finMnemonic = companyObj.getFinancialMne().getValue();
            cusmnemonic = companyObj.getCustomerMnemonic().getValue();
            branchCode = companyId;

            branchState = companyObj.getLocalRefField("FF.STATE").getValue();
            branchName = getCompanyDescription(companyId);
            zoneName = getCompanyDescription(companyObj.getLocalRefField("FF.ZONE").getValue());
            regionName = getCompanyDescription(companyObj.getLocalRefField("FF.REGION").getValue());
            divisionName = getCompanyDescription(companyObj.getLocalRefField("FF.DIVISION").getValue());
            clusterName = getCompanyDescription(companyObj.getLocalRefField("FF.CLUSTER").getValue());
        } catch (Exception e11) {
            e11.getMessage();
        }

    }

    /**
     ** METHOD:getCompanyDescription DESCRIPTION: his method is used to get the
     * values and split using.
     */
    private String getCompanyDescription(String companyCode) {
        try {
            if (companyCode != null && !companyCode.isEmpty()) {
                CompanyRecord companyRec = new CompanyRecord(da.getRecord(SEL_APP_COMP, companyCode));
                companyName = companyRec.getCompanyName().get(0).getValue();
                String[] compNamePart = companyName.split("-");
                companyName = compNamePart[0];
                return companyName;
            }
        } catch (Exception e12) {
            e12.getMessage();
        }
        return companyName;
    }

    /**
     * METHOD:getArrangementFieldMappingDet DESCRIPTION:This method etrieves
     * arrangement details for the given arrangement ID and populates company,
     * customer, and linked account information.
     * 
     */
    private void getArrangementFieldMappingDet(String selectionArrId) {

        try {
            AaArrangementRecord aaArrRec = new AaArrangementRecord(
                    da.getRecord(finMnemonic, AA_ARRANGEMENT_RECORD, "", selectionArrId));
            loanacctNum = selectionArrId;
            acctNo = aaArrRec.getLinkedAppl().get(0).getLinkedApplId().getValue();
            if (!branch.equals("")) {
                branchCode = branch;
            } else {
                branchCode = companyId;
            }
            String coCode = aaArrRec.getCoCodeRec().getValue();
            if (coCode != null && coCode.equals(companyId)) {
                filteredArrList.add(selectionArrId);
            }
            customerid = aaArrRec.getCustomer().get(0).getCustomer().getValue();
            CompanyRecord companyRec = new CompanyRecord(da.getRecord(SEL_APP_COMP, coCode));
            companyName = companyRec.getCompanyName().get(0).getValue();
            origContractDate = aaArrRec.getOrigContractDate().getValue();
            startDate = aaArrRec.getStartDate().getValue();
            if (!origContractDate.isEmpty()) {
                disbursementDate = origContractDate;
                legacy = true;
            } else {
                disbursementDate = startDate;
            }
            getCustomerDetails(customerid);

            String productGrp = aaArrRec.getProductGroup().getValue();
            String product = aaArrRec.getProduct().get(0).getProduct().getValue();
            productDet = productGrp + "," + product;

        } catch (Exception e13) {
            e13.getMessage();
        }

    }

    /**
     * METHOD:getoutFinalset DESCRIPTION:This method Determines and returns the
     * final set of arrangement IDs based on date-only filtering, selection
     * criteria, and available demand data.
     */
    private Set<String> getoutFinalset(Set<String> demandFutureArrSetObj, Set<String> selectionSet) {
        Set<String> outFinalSet = null;
        try {
            if (selectionSet.isEmpty()) {
                if (!filterValSet.isEmpty()) {
                    noRecErrFlag = true;
                } else {
                    outFinalSet = demandFutureArrSetObj;
                }
            } else {
                outFinalSet = new LinkedHashSet<>(selectionSet);
            }
        } catch (Exception e) {
            e.getMessage();
        }
        return outFinalSet == null ? new LinkedHashSet<>() : outFinalSet;

    }

    /**
     * METHOD:getFilterCriteriaDets DESCRIPTION:This method Processes the provided
     * filter criteria and returns a set of arrangement IDs matching the selected
     * filters such as date, product, centre, village, district, cycle, purpose,
     * religion, and caste.
     */
    private Set<String> getFilterCriteriaDets(List<FilterCriteria> filterCriteria) {
        Set<String> selectionSet = null;
        try {
            for (FilterCriteria filter : filterCriteria) {
                Set<String> currentFilterSet = new LinkedHashSet<>();
                String value = filter.getValue();
                if (value == null || value.isEmpty())
                    continue;

                switch (filter.getFieldname()) {
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
                    selProduct = value;
                    break;

                case "CENTER":
                    selCentreName = value;
                    break;

                case "VILLAGE":
                    selVillage = value;
                    break;

                case "DISTRICT":
                    selDistrict = value;
                    break;

                case "CYCLE":
                    selLoanCycle = value;
                    break;

                case "PURPOSE":
                    selLoanPurp = value;
                    break;

                case "RELIGION":
                    selReligGrp = value;
                    break;

                case "CASTE":
                    selCaste = value;
                    break;
                default:
                }
                selectionSet = addToFinalSelectionSet(selectionSet, currentFilterSet);
            }

        } catch (Exception e14) {
            e14.getMessage();
        }
        return selectionSet == null ? new LinkedHashSet<>() : selectionSet;

    }

    /**
     * @param selectionSet
     * @param currentFilterSet
     * @return
     */
    private Set<String> addToFinalSelectionSet(Set<String> selectionSet, Set<String> currentFilterSet) {
        try {
            if (selectionSet == null) {
                if (!currentFilterSet.isEmpty()) {
                    selectionSet = currentFilterSet;
                }
            } else {
                selectionSet.retainAll(currentFilterSet);
            }
        } catch (Exception e) {
            e.getMessage();
        }
        return selectionSet;
    }

    /**
     * METHOD:ggetProductDet DESCRIPTION:Extracts arrangement IDs from the "LENDING"
     * product line of a customer arrangement and adds them to the provided list.
     */
    public void getProductDet(List<String> arrIdList, AaCustomerArrangementRecord aaCusRec) {
        try {
            productLineList = aaCusRec.getProductLine();
            for (ProductLineClass productLine : productLineList) {
                if (productLine.getProductLine().getValue().equals("LENDING")) {
                    for (ArrangementClass arrIdFrmAAcus : productLine.getArrangement()) {
                        arrIdList.add(arrIdFrmAAcus.getArrangement().getValue());

                    }
                }
            }
        } catch (Exception e17) {
            e17.getMessage();
        }
    }

    /**
     * METHOD:getCustomerDetails DESCRIPTION:This method Retrieves customer
     * personal, demographic, and group details using the customer number and
     * populates relevant customer information.
     * 
     */
    private void getCustomerDetails(String customerid) {
        CustomerRecord cusRec = null;
        try {
            cusRec = new CustomerRecord(da.getRecord(cusmnemonic, SEL_APP_CUS, "", customerid));
            for (TField name1 : cusRec.getName1()) {
                fstName = name1.getValue();
            }

            for (TField name2 : cusRec.getName2()) {
                scdName = name2.getValue();
            }

            familyName = cusRec.getFamilyName().getValue();
            customerName = String.join(" ", fstName, scdName, familyName).trim().replaceAll("\\s+", " ");

            if (cusRec.getMnemonic() != null) {
                mnemonic = cusRec.getMnemonic().getValue();
            }

            if ((origContractDate != null)) {
                customerNumber = mnemonic;
            } else {
                customerNumber = customerid;
            }

            dateOfBirth = cusRec.getDateOfBirth().getValue();
            customerAge = customerAgeCalculation(dateOfBirth);
            caste = cusRec.getLocalRefField("FF.CASTE").getValue();
            religGrp = cusRec.getLocalRefField("FF.RELIG.GROUP").getValue();

        } catch (Exception e18) {
            e18.getMessage();
        }

    }

    /**
     * METHOD:customerAgeCalculation DESCRIPTION:This method get the value for
     * birthDate,localDate today,customerAge for details.
     */
    private int customerAgeCalculation(String dateOfBirth) {

        try {
            if (!dateOfBirth.isEmpty()) {
                LocalDate birthDate;
                birthDate = LocalDate.parse(dateOfBirth, formatter);

                LocalDate today = LocalDate.now();
                customerAge = Period.between(birthDate, today).getYears();

            }
        } catch (Exception e19) {
            e19.getMessage();
        }
        return customerAge;

    }

    /**
     * METHOD:getEbFfLoanDetails DESCRIPTION:This method getEbFfLoanDetails
     * retrieves the branchState with a give ArrangementId object.
     */
    private void getEbFfLoanDetails(String selectionArrId) {
        try {
            todayDate = session.getCurrentVariable("!TODAY");
            LocalDate today = LocalDate.parse(todayDate, formatter);
            EbFfLoanDetailsRecord ffLoanDetsRec = new EbFfLoanDetailsRecord(
                    da.getRecord(finMnemonic, "EB.FF.LOAN.DETAILS", "", selectionArrId));
            if (!ffLoanDetsRec.toString().isEmpty()) {
                List<AddressTypeClass> addTypeList = ffLoanDetsRec.getAddressType();
                for (AddressTypeClass addType : addTypeList) {
                    branchDistrict = addType.getDistrictName().getValue();
                }
                for (int entiNo = 0; entiNo < ffLoanDetsRec.getFmEntityNumber().size(); entiNo++) {
                    String relation = ffLoanDetsRec.getFmEntityNumber().get(entiNo).getRelation().getValue();
                    if (relation.equalsIgnoreCase("SELF")) {
                        occupation = ffLoanDetsRec.getFmEntityNumber().get(entiNo).getOccupation().getValue();
                    }
                }

                for (CcProductClass productobj : ffLoanDetsRec.getCcProduct())
                    getCashCarryDet(today, productobj);
            }
        } catch (Exception e20) {
            e20.getMessage();
        }

    }

    /**
     * @param today
     * @param productobj
     */
    public void getCashCarryDet(LocalDate today, CcProductClass productobj) {
        try {
            ccDate = productobj.getCcDate().getValue();
            if (ccDate != null && !ccDate.isEmpty()) {
                LocalDate ccDatestr = LocalDate.parse(ccDate, formatter);
                if (!ccDatestr.isAfter(today)) {
                    ccAmount = productobj.getCcAmount().getValue();
                    if (ccAmount != null && !ccAmount.trim().isEmpty()) {
                        cashCarryTotal += Double.parseDouble(ccAmount);
                    }

                }
            }
        } catch (NumberFormatException e) {
            e.getMessage();
        }
    }

    /**
     * @param loanacctNum2
     */
    private void getAccountDetails(String acctNo) {

        try {
            AccountRecord accRec = new AccountRecord(da.getRecord(finMnemonic, "ACCOUNT", "", acctNo));
            if (legacy) {
                for (AltAcctTypeClass altType : accRec.getAltAcctType()) {
                    if (altType.getAltAcctType().getValue().equals("LEGACY")) {
                        legacyAcctnum = altType.getAltAcctId().getValue();
                    }
                }
            }
            cycle = accRec.getLocalRefField("FF.LOAN.CYCLE").getValue();
            village = accRec.getLocalRefField("FF.VILLAGE").getValue();
            purpose = accRec.getLocalRefField("FF.LOAN.PURP").getValue();
            ebgroupName = accRec.getLocalRefField("FF.GROUP").getValue();
            if (ebgroupName != null && !ebgroupName.isEmpty()) {
                getffgroupName(ebgroupName);
            }

            centerId = accRec.getLocalRefField("FF.CENTRE").getValue();
            if (centerId != null && !centerId.isEmpty()) {
                getEbFfCentreDetails(centerId);
            }

        } catch (Exception e5) {
            e5.getMessage();
        }
    }

    /**
     * @param ebgroupName
     */
    private void getffgroupName(String ebgroupName) {
        try {
            EbFfGroupsRecord ffGroupRec = new EbFfGroupsRecord(da.getRecord("", "EB.FF.GROUPS", "", ebgroupName));
            groupName = ffGroupRec.getGroupName().getValue();
        } catch (Exception e22) {
            e22.getMessage();
        }

    }

    /**
     * * METHOD:getofficerName DESCRIPTION:The getofficerName methord retrieves the
     * relationshipOfficerName with a give primaryOfficer,
     */
    private void getEbFfCentreDetails(String centerId) {
        try {
            EbFfCentreDetailRecord centerRec = new EbFfCentreDetailRecord(
                    da.getRecord("", "EB.FF.CENTRE.DETAIL", "", centerId));
            centerName = centerRec.getCenterName().getValue();
            String ro = centerRec.getCurrentRo().getValue();
            if (ro != null && !ro.isEmpty()) {
                getEbFfRoUserDets(ro);
            }
            String branchManagerId = centerRec.getBranchManager().getValue();
            if (branchManagerId != null && !branchManagerId.isEmpty()) {
                getUserDets(branchManagerId);
            }
        } catch (Exception e22) {
            e22.getMessage();
        }

    }

    /**
     * @param ebbranchManagerName
     */
    private void getUserDets(String branchManagerId) {
        try {
            UserRecord userRec = new UserRecord(da.getRecord("", "USER", "", branchManagerId));
            branchManagerName = userRec.getUserName().getValue();
            branchManagerMobileNumber = userRec.getLocalRefField("FF.MOBILE.NO").getValue();

        } catch (Exception e23) {
            e23.getMessage();
        }
    }

    /**
     * @param ebrelationshipOffName
     */
    private void getEbFfRoUserDets(String ro) {
        try {
            EbFfRoUserRecord roUserRec = new EbFfRoUserRecord(da.getRecord("", "EB.FF.RO.USER", "", ro));
            relationshipOfficerName = roUserRec.getRoName().getValue();
            relationshipOfficerMobileNumber = roUserRec.getRoMobileNumber().getValue();
        } catch (Exception e24) {
            e24.getMessage();
        }
    }

    /**
     * METHOD:getaaArrTermAmount DESCRIPTION:The getaaArrTermAmount method retrieves
     * the loan amount (or term amount) associated with a given Contract object.
     */
    private void getaaArrTermAmount(Contract contract) {
        try {
            AaArrTermAmountRecord aaArrTermAmt = new AaArrTermAmountRecord(
                    contract.getConditionForProperty("COMMITMENT"));
            loanAmount = aaArrTermAmt.getAmount().getValue();

        } catch (Exception e25) {
            e25.getMessage();
        }
    }

    /**
     * METHOD:getEbFfCollectionDets DESCRIPTION: The getEbFfCollectionDets method
     * retrieves and processes collection details
     * 
     * @param end
     * @param start
     */
    private void getEbFfCollectionDets(String selectionArrId, LocalDate start, LocalDate end) {
        principalDemandBal = 0.0;
        interestDemandBal = 0.0;
        feeDemandBal = 0.0;
        totalDemandBal = 0.0;

        BigDecimal duePrincipal = BigDecimal.ZERO;
        BigDecimal dueInterest = BigDecimal.ZERO;
        BigDecimal dueFee = BigDecimal.ZERO;

        try {
            EbFfCollectionDetsRecord ffCollDetsRec = new EbFfCollectionDetsRecord(
                    da.getRecord(finMnemonic, "EB.FF.COLLECTION.DETS", "", selectionArrId));
            for (int i = 0; i < ffCollDetsRec.getDueDate().size(); i++) {

                String dueDate = ffCollDetsRec.getDueDate().get(i).getValue();
                if (dueDate != null && !dueDate.isEmpty()) {
                    LocalDate dueDateVal = LocalDate.parse(dueDate, formatter);

                    if (!dueDateVal.isBefore(start) && !dueDateVal.isAfter(end)) {
                        String principalAmt = ffCollDetsRec.getPrincipalAmt(i).getValue();
                        String interestAmt = ffCollDetsRec.getInterestAmt().get(i).getValue();
                        String feeAmt = ffCollDetsRec.getChargeAmt().get(i).getValue();

                        principalAmt = (principalAmt == null || principalAmt.trim().isEmpty()) ? "0" : principalAmt;
                        interestAmt = (interestAmt == null || interestAmt.trim().isEmpty()) ? "0" : interestAmt;
                        feeAmt = (feeAmt == null || feeAmt.trim().isEmpty()) ? "0" : feeAmt;

                        duePrincipal = duePrincipal.add(new BigDecimal(principalAmt));
                        dueInterest = dueInterest.add(new BigDecimal(interestAmt));
                        dueFee = dueFee.add(new BigDecimal(feeAmt));
                    }
                }
            }
            principalDemandBal = duePrincipal.doubleValue();
            interestDemandBal = dueInterest.doubleValue();
            feeDemandBal = dueFee.doubleValue();
            totalDemandBal = principalDemandBal + interestDemandBal;

        } catch (Exception e26) {
            e26.getMessage();
        }

    }

}
