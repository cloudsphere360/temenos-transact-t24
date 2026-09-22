
package com.temenos.fusion;

import java.io.File;
import java.io.FileWriter;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import com.temenos.api.TField;
import com.temenos.t24.api.arrangement.accounting.Contract;
import com.temenos.t24.api.complex.eb.enquiryhook.EnquiryContext;
import com.temenos.t24.api.complex.eb.enquiryhook.FilterCriteria;
import com.temenos.t24.api.hook.system.Enquiry;
import com.temenos.t24.api.records.aaarrangement.AaArrangementRecord;
import com.temenos.t24.api.records.aaprddescharge.AaPrdDesChargeRecord;
import com.temenos.t24.api.records.aaprddestermamount.AaPrdDesTermAmountRecord;
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
import java.util.Map;

/**
 * @author Kavin Prabha M Date Created: 12.12.2025 Attached as
 *         :NofileEnquiryRoutine EB.API : FF.E.NOFILE.DE.DISB.INSUR.RPT
 *         STANDARD.SELECTION >NOFILE.FF.DE.DISB.INSUR.RPT Description: BM
 *         Online Report generation ->Disbursement-Fee-Insurance Summary Report
 *         Modification History : Initial Draft
 *
 *
 *         12-DEC-2025 Development Initial Version
 *         -----------------------------------------------------------------------------
 *         12-Feb-2026 Grouping(Client Count, Count Loan Sathish Kumar N B
 *         Amount, Fee, Insurance) 26-Mar-2026 Updated to handle DATE.FROM
 *         Sathish Kumar N B and DATE.TO separate selection fields
 */

public class FfENofDisbursementFeeInsuSummaryRpt extends Enquiry {

    public static final String DATE_RANGE_ERR = "EB-FF.DATE.RANGE.GREATER";
    public static final String FILE_NAME = "DisFeeInsuranceRep_Sum";
    private static final String AA_ARRANGEMENT = "AA.ARRANGEMENT";
    public static final String EB_FF_PARAMETER = "EB.FF.PARAMETER";

    List<String> retvalues = new ArrayList<>();
    List<String> outvalues = new ArrayList<>();
    List<String> finalArrList = new ArrayList<>();
    DataAccess da = new DataAccess(this);

    String selDate = "";
    String selDateOp = "";
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

    String arrStDt = "";
    String ro = "";
    String product = "";
    String centre = "";
    String village = "";
    String selectionFilter = "";
    String selFilterField = "";
    int customerCount = 0;

    String customer = "";

    double processingFees = 0.0;
    double insuranceFees = 0.0;

    boolean dateErrFlag = false;
    boolean dateGrp = false;
    boolean compDescFlg = false;
    DateTimeFormatter outDateFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
    DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HHmmss");

    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
    int defaultDate;
    int daterange;
    String pastMonth;
    String customerNumber = "";
    String datedefaultRange;
    String accNum = "";
    String coCode = "";
    String productName = "";
    String villageName = "";
    String centreName = "";
    String roName = "";
    String selUser = "";
    String selGroup = "";
    boolean branchSelection = false;
    @Override

    public List<String> setIds(List<FilterCriteria> filterCriteria, EnquiryContext enquiryContext) {

        try {

            getFilterCriteriaDets(filterCriteria);

            runBranchSummary();

        } catch (Exception e1) {
            e1.getMessage();

        }

        return retvalues;

    }

    private void runBranchSummary() {

        try {
            Session session = new Session(this);
            todayDate = session.getCurrentVariable("!TODAY");
            Contract contract = new Contract(this);
            Set<String> arrSet = new LinkedHashSet<>(da.selectRecords(finMnemonic, AA_ARRANGEMENT, "",
                    "WITH ARR.STATUS NE EXPIRED AND ARR.STATUS NE MATURED AND CO.CODE EQ " + companyIds));

            finalArrList = getFinalArrList(arrSet);

            Map<String, Integer> clientCount = new HashMap<>();
            Map<String, Integer> loanCount = new HashMap<>();
            Map<String, Double> loanAmountBal = new HashMap<>();
            Map<String, Double> processingFeeBal = new HashMap<>();
            Map<String, Double> insuranceFeeBal = new HashMap<>();
            Map<String, Set<String>> groupCustomers = new HashMap<>();

            getArrList(contract, clientCount, loanCount, loanAmountBal, processingFeeBal, insuranceFeeBal,
                    groupCustomers);

            List<String> sortedKeys = new ArrayList<>(loanCount.keySet());
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
                row.add(String.valueOf(clientCount.getOrDefault(currentGrp, 0)));
                row.add(String.valueOf(loanCount.get(currentGrp)));
                row.add(String.format("%.2f", loanAmountBal.get(currentGrp)));
                row.add(String.format("%.2f", processingFeeBal.get(currentGrp)));
                row.add(String.format("%.2f", insuranceFeeBal.get(currentGrp)));
                retvalues.add(String.join("*", row));
               
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
                String outputPath = filePath + FILE_NAME + "_" + selGroup + "-WISE" + "_" + branchName + "_" + selUser
                        + "_" + "_" + currDate + "_" + currTime + ".csv";
               
                writeToFile(outvalues, outputPath);
            
        } catch (Exception e2) {
             e2.getMessage();
        }
    }

    /**
     * Processes each arrangement from the filtered arrangement list. Retrieves
     * arrangement details, loan amount, and charge information. Determines grouping
     * key based on enquiry selection. Applies filter conditions and updates
     * aggregated values like: client count, loan count, loan amount, processing
     * fee, and insurance fee.
     */
    private void getArrList(Contract contract, Map<String, Integer> clientCount, Map<String, Integer> loanCount,
            Map<String, Double> loanAmountBal, Map<String, Double> processingFeeBal,
            Map<String, Double> insuranceFeeBal, Map<String, Set<String>> groupCustomers) {
        for (String arrId : finalArrList) {

            arrStDt = "";
            ro = "";
            product = "";
            centre = "";
            village = "";
            processingFees = 0.0;
            insuranceFees = 0.0;

            contract.setContractId(arrId);
          
            getAaArrangementDets(arrId);
            double loanAmount = getAaArrTermAmountDetails(contract);
           
            getAaArrChargeDetails(contract);

            String groupValue = getGroupBasedValue(arrId);
            

            boolean selectionCheck = chkSelectionBased();
            if (selectionCheck) {
                continue;
            }

            if (groupValue != null && !groupValue.isEmpty()) {
                
                groupCustomers.putIfAbsent(groupValue, new HashSet<>());
                Set<String> customersInGroup = groupCustomers.get(groupValue);

                if (!customersInGroup.contains(customer)) {
                    clientCount.put(groupValue, clientCount.getOrDefault(groupValue, 0) + 1);

                    customersInGroup.add(customer);
                }

                loanCount.put(groupValue, loanCount.getOrDefault(groupValue, 0) + 1);
                loanAmountBal.put(groupValue, loanAmountBal.getOrDefault(groupValue, 0.0) + Math.abs(loanAmount));
                processingFeeBal.put(groupValue,
                        processingFeeBal.getOrDefault(groupValue, 0.0) + Math.abs(processingFees));
                insuranceFeeBal.put(groupValue,
                        insuranceFeeBal.getOrDefault(groupValue, 0.0) + Math.abs(insuranceFees));

            }
        }
    }

    public List<String> getFinalArrList(Set<String> arrSet) {
       
        List<String> arrIdList = new ArrayList<>();
        String dateParamName = "DISB.FEE.INSURANCE";
        String defDtParamId = "FF.BM.REPORT.DATE.DEFAULT";
        try {
            String dateFilterVal = getEbFfParamRecDets(defDtParamId, dateParamName);
            
            LocalDate today = LocalDate.parse(todayDate, formatter);
           
            LocalDate start = startDate.isEmpty() ? getEndDateBasedOnParamRec(dateFilterVal, today)
                    : LocalDate.parse(startDate, formatter);
           
            LocalDate end = endDate.isEmpty() ? today : LocalDate.parse(endDate, formatter);
            
            for (String contractId : arrSet) {
                String arrStartDt = getStartDate(contractId);
                if (arrStartDt != null && !arrStartDt.isEmpty()) {
                    LocalDate stDate = LocalDate.parse(arrStartDt, formatter);
                    if (!stDate.isBefore(start) && !stDate.isAfter(end)) {
                        arrIdList.add(contractId);
                       

                    }
                }

            }
        } catch (Exception e3) {
            e3.getMessage();
            
        }

        return arrIdList;
    }

    public LocalDate getEndDateBasedOnParamRec(String paramValue, LocalDate baseDate) {
        
        LocalDate expectedDt = null;
        try {
            if (paramValue.endsWith("D")) {
                int allowedDays = Integer.parseInt(paramValue.replace("D", ""));
                expectedDt = baseDate.minusDays(allowedDays);
            } else if (paramValue.endsWith("M")) {
                int allowedMonths = Integer.parseInt(paramValue.replace("M", ""));
               
                expectedDt = baseDate.minusMonths(allowedMonths);
               
            }
        } catch (NumberFormatException e) {
            e.getMessage();
            
        }
        return expectedDt;
    }

    public String getEbFfParamRecDets(String paramId, String paramName) {
        
        String paramVal = "";
        try {
            EbFfParameterRecord paramRec = new EbFfParameterRecord(da.getRecord(EB_FF_PARAMETER, paramId));
            
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

    public void getAaArrangementDets(String arrId) {
       
        try {
            AaArrangementRecord arrRec = new AaArrangementRecord(da.getRecord(finMnemonic, AA_ARRANGEMENT, "", arrId));
           
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
    private void getAaProductDetails(String product) {
      
        try {
            AaProductRecord aaProRec = new AaProductRecord(da.getRecord("AA.PRODUCT", product));
           
            productName = aaProRec.getDescription(0).getValue();
            
        } catch (Exception e) {
            e.getMessage();
            
        }
    }

    /**
     * Retrieves account related local reference fields. Extracts centre and village
     * from ACCOUNT property. Also determines RO by reading EB.FF.CENTRE.DETAIL
     * record.
     */
    public void getAaArrAccountDets(String accNum) {
       
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

    /**
     * @param village2
     */
    private void getEbFfVillageDetails(String village) {
        
        try {
            EbFfVillageRecord villageRec = new EbFfVillageRecord(da.getRecord("", "EB.FF.VILLAGE", "", village));
            
            villageName = villageRec.getVillageName().getValue();
            

        } catch (Exception e) {
            e.getMessage();
            
        }

    }

    /**
     * Reads EB.FF.CENTRE.DETAIL record. Returns the current RO mapped to the given
     * centre.
     */
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

    /**
     * Fetches start date of the arrangement from AA.ARRANGEMENT. Used for date
     * filtering and DATE based grouping.
     */
    public String getStartDate(String arrId) {
        
        String dateToUse = "";
        try {
            AaArrangementRecord aaRec = new AaArrangementRecord(da.getRecord(finMnemonic, AA_ARRANGEMENT, "", arrId));
            

            // Priority 1: Check ORIG.CONTRACT.DATE
            String origDate = aaRec.getOrigContractDate().getValue();
            

            if (origDate != null && !origDate.isEmpty()) {
                dateToUse = origDate;
                
            } else {
                // Priority 2: Fallback to START.DATE
                dateToUse = aaRec.getStartDate().getValue();
               
            }
        } catch (Exception e7) {
            e7.getMessage();
            
        }
        return dateToUse;
    }

    /**
     * Validates whether the arrangement satisfies user selection filters. Checks
     * RO, product, centre and village conditions. Returns true if the arrangement
     * should be skipped.
     */
    public boolean chkSelectionBased() {
        return (selRo != null && !selRo.isEmpty() && !selRo.equals(ro))
                || (selProduct != null && !selProduct.isEmpty() && !selProduct.equals(product))
                || (selCenterName != null && !selCenterName.isEmpty() && !selCenterName.equals(centre))
                || (selVillage != null && !selVillage.isEmpty() && !selVillage.equals(village));
    }

    /**
     * Determines grouping key based on enquiry type. Possible grouping values:
     * DATE, RO, PRODUCT, CENTERNAME, VILLAGE. Returns the value used as grouping
     * key for aggregation.
     */
    public String getGroupBasedValue(String arrId) {
        
        String groupKey = "";

        switch (selGroup) {
        case "BRANCH":
            selFilterField = "Branch";
            branchSelection = true;
            groupKey = getCompanyDescription(coCode);
            break;
        case "DATE":
            selFilterField = "Date";
            arrStDt = getStartDate(arrId);
            groupKey = arrStDt;
            dateGrp = true;
            break;
        case "RO":

            selFilterField = "RO";

            getAaArrAccountDets(accNum);

            groupKey = roName;

            break;

        case "PRODUCT":

            selFilterField = "Product";

            groupKey = productName;

            break;

        case "CENTER":

            selFilterField = "CenterName";

            getAaArrAccountDets(accNum);

            groupKey = centreName;

            break;

        case "VILLAGE":

            selFilterField = "Village";

            getAaArrAccountDets(accNum);

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
        } catch (Exception e8) {
            e8.getMessage();
            
        }
    }

    /**
     * Retrieves company hierarchy information. Fetches branch name, zone, region,
     * division and cluster descriptions. Also sets financial mnemonic used for
     * record retrieval.
     */
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

        } catch (Exception e9) {
            e9.getMessage();
        }
    }

    /**
     * Retrieves company name for a given company code. Extracts readable
     * description from COMPANY record.
     */
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
        } catch (Exception e10) {
            e10.getMessage();
           
        }
        return companyName;
    }

    /**
     * Retrieves all linked company IDs from COMPANY.CONSOL. Builds list of
     * companies consolidated under selected branch. Used for retrieving
     * arrangements across linked companies.
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

        } catch (Exception e11) {
            e11.getMessage();
            
        }
    }

    public double getAaArrTermAmountDetails(Contract contract) {
        double loanAmount = 0.0;

        try {
            AaPrdDesTermAmountRecord aaArrTermAmtRec = new AaPrdDesTermAmountRecord(
                    contract.getConditionForProperty("COMMITMENT"));
            if (!aaArrTermAmtRec.toString().isEmpty()) {
                loanAmount = Double.parseDouble(aaArrTermAmtRec.getAmount().getValue());

                return loanAmount;
            }
        } catch (Exception e13) {
            e13.getMessage();
            
        }
        return loanAmount;
    }

    /**
     * Reads charge properties associated with the arrangement. Extracts
     * PROCESSINGFEE and INSURANCEFEE values.
     */
    public void getAaArrChargeDetails(Contract contract) {

        try {
            List<String> chgPropList = contract.getPropertyIdsForPropertyClass("CHARGE");
            for (String chgProperty : chgPropList) {
                AaPrdDesChargeRecord aaArrChgRec = new AaPrdDesChargeRecord(
                        contract.getConditionForProperty(chgProperty));
                String idComp2 = aaArrChgRec.getIdComp2().getValue();
                if (idComp2.equals("PROCESSINGFEE")) {
                    processingFees = Double.parseDouble(aaArrChgRec.getFixedAmount().getValue());
                }
                if (idComp2.equals("INSURANCEFEE")) {
                    insuranceFees = Double.parseDouble(aaArrChgRec.getFixedAmount().getValue());
                }
            }

        } catch (Exception e14) {
            e14.getMessage();
            
        }
    }

    /**
     * Writes aggregated report data into CSV file. Creates file if it does not
     * exist and writes header row. Appends report rows to the output file path.
     */
    public void writeToFile(List<String> data, String filePath) {
        try {

            File file = new File(filePath);
            file.getParentFile().mkdirs();
            boolean fileExists = file.exists();

            try (FileWriter writer = new FileWriter(file, true)) {
                if(data == null || data.isEmpty()) {
                    writer.write("No records matched the selection criteria"+ System.lineSeparator());
                }
                else {
                    if (!fileExists) {
                
                    String header = String.join(",", "ZoneName", "RegionName", "DivisionName", "ClusterName",
                            "BranchCode", "BranchName", selFilterField, "Count Client", "Count Loan", "Loan Amount",
                            "Fee", "Insurance");
                    writer.write(header + System.lineSeparator());
                }
                

                for (String line : data) {
                    writer.write(line + System.lineSeparator());
                }
            }
            }
        } catch (Exception e15) {
            e15.getMessage();
           
        }
    }
}
