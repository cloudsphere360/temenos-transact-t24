package com.temenos.fusion;

import java.io.File;
import java.io.FileWriter;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.temenos.api.TField;
import com.temenos.api.exceptions.T24CoreException;
import com.temenos.t24.api.arrangement.accounting.Contract;
import com.temenos.t24.api.complex.aa.contractapi.BalanceMovement;
import com.temenos.t24.api.complex.eb.enquiryhook.EnquiryContext;
import com.temenos.t24.api.complex.eb.enquiryhook.FilterCriteria;
import com.temenos.t24.api.hook.system.Enquiry;
import com.temenos.t24.api.records.aaaccountdetails.AaAccountDetailsRecord;
import com.temenos.t24.api.records.aaarrangement.AaArrangementRecord;
import com.temenos.t24.api.records.aacustomerarrangement.AaCustomerArrangementRecord;
import com.temenos.t24.api.records.aacustomerarrangement.ArrangementClass;
import com.temenos.t24.api.records.aacustomerarrangement.ProductLineClass;
import com.temenos.t24.api.records.aaprddestermamount.AaPrdDesTermAmountRecord;
import com.temenos.t24.api.records.aaproduct.AaProductRecord;
import com.temenos.t24.api.records.account.AccountRecord;
import com.temenos.t24.api.records.account.AltAcctTypeClass;
import com.temenos.t24.api.records.company.CompanyRecord;
import com.temenos.t24.api.records.companyconsol.CompanyConsolRecord;
import com.temenos.t24.api.records.customer.CustomerRecord;
import com.temenos.t24.api.records.ebffcentredetail.EbFfCentreDetailRecord;
import com.temenos.t24.api.records.ebffloandetails.AddressTypeClass;
import com.temenos.t24.api.records.ebffloandetails.EbFfLoanDetailsRecord;
import com.temenos.t24.api.records.ebffparameter.EbFfParameterRecord;
import com.temenos.t24.api.records.ebffparameter.ParamDescClass;
import com.temenos.t24.api.records.ebffrouser.EbFfRoUserRecord;
import com.temenos.t24.api.records.user.UserRecord;
import com.temenos.t24.api.system.DataAccess;
import com.temenos.t24.api.system.Session;

public class FfNofileFutureMatLoanDetailRpt extends Enquiry {
    public static final String FILE_NAME = "FutureMaturityLoanRep_Det";
    public static final String DATE_RANGE_ERR = "EB-FF.BM.FUTURE.MAX.DT.RANGE";
    public static final String DATE_FILTER_ERR = "EB-FF.BM.DATE.FILTER.RANGE";
    public static final String NO_REC_ERR = "EB-NO.REC.SELECT";
    public static final String CENTRE = "FF.CENTRE";
    public static final String VILLAGE = "FF.VILLAGE";
    public static final String DISTRICT_NAME = "DISTRICT.NAME";
    public static final String LOAN_CYCLE = "FF.LOAN.CYCLE";
    public static final String LOAN_PURP = "FF.LOAN.PURP";
    public static final String RELIG_GROUP = "FF.RELIG.GROUP";
    public static final String CASTE = "FF.CASTE";
    public static final String TRADE = "TRADE";
    public static final String AA_ARRANGEMENT = "AA.ARRANGEMENT";

    List<String> retvalues = new ArrayList<>();
    List<String> outvalues = new ArrayList<>();
    DataAccess da = new DataAccess(this);

    Set<String> filterValSet = new HashSet<>();

    String seluser = "";
    String selDate = "";
    String selDateOp = "";
    String selProduct = "";
    String selCenterName = "";
    String selVillage = "";
    String selDistrict = "";
    String selCycle = "";
    String selPurpose = "";
    String selReligion = "";
    String selCaste = "";
    String startDate = "";
    String endDate = "";
    String zoneName = "";
    String regionName = "";
    String divisionName = "";
    String clusterName = "";
    String branchName = "";
    String branchDistrict = "";
    String branchState = "";
    String branchCode = "";
    String centerName = "";
    String centerCode = "";
    String customerName = "";
    String customerNumber = "";
    String customerAge = "";
    String religion = "";
    String occupation = "";
    String purpose = "";
    String loanAccountNumber = "";
    String legacyAcctNo = "";
    String loanDate = "";
    String loanAmount = "";
    String productName = "";
    String cycle = "";
    String roOfficerName = "";
    String roMobileNumber = "";
    String branchManagerName = "";
    String branchManagerMobileNumber = "";
    String disbursementDate = "";
    String maturityDate = "";
    String outstanding = "";
    String finMnemonic = "";
    String mnemonic = "";
    String todayDate = "";
    String fldName = "";

    boolean onlyDateFilter = false;
    boolean dateRangeErrFlag = false;
    boolean dateFilterErrFlag = false;
    boolean noRecErrFlag = false;
    boolean migratedContractFlg = false;

    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
    DateTimeFormatter outDateFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
    DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HHmmss");

    String branch = "";
    String displayDate = "";
    String companyId = "";
    String selBranch = "";
    String selBranchName = "";
    String companyIds = "";
    String dateRangeVal = "";
    String daetFilterVal = "";
    String acctNo = "";

    @Override
    public List<String> setIds(List<FilterCriteria> filterCriteria, EnquiryContext enquiryContext) {

        Session session = new Session(this);
        todayDate = session.getCurrentVariable("!TODAY");
        Contract contract = new Contract(this);

        for (FilterCriteria filter : filterCriteria) {
            if (filter.getFieldname().equals("BRANCH")) {
                selBranch = filter.getValue();
                selBranchName = getCompanyDescription(selBranch);
                initialiseCompanyInfo(selBranch);
                getLinkedCompIds(selBranch);
                break;
            }
        }

        try {
            Set<String> selectionSet = getFilterCriteriaDets(filterCriteria);
            Set<String> futureArrSet = new LinkedHashSet<>(da.selectRecords("", AA_ARRANGEMENT, "",
                    "WITH ARR.STATUS NE PENDING.CLOSURE CLOSE AND CO.CODE EQ " + companyIds));

            Set<String> preFinalSet = getPreFinalset(futureArrSet, selectionSet);
            preFinalSet.retainAll(futureArrSet);

            processFinalArrList(contract, preFinalSet);

            String fileParamId = "FF.BM.REPORT.EXTRACT";
            String fileParamName = "Path";
            String filePath = getEbFfParamRecDets(fileParamId, fileParamName);

            LocalDateTime currDtTime = LocalDateTime.now();
            String currDate = currDtTime.format(outDateFormatter);
            String currTime = currDtTime.format(timeFormatter);
            String outputPath = filePath + FILE_NAME + "_" + selBranchName + "_" + seluser + "_" + currDate + "_"
                    + currTime + ".csv";
            writeToFile(outvalues, outputPath);

        } catch (Exception e) {
            e.getMessage();
        }
        if (noRecErrFlag || retvalues.isEmpty()) {
            throw new T24CoreException("", NO_REC_ERR);
        } else {
            return retvalues;
        }

    }

    public void processFinalArrList(Contract contract, Set<String> preFinalSet) {
        String dateParamName = "FUTURE.MATURITY.LOAN";
        String defDtParamId = "FF.BM.REPORT.DATE.DEFAULT";
        daetFilterVal = getEbFfParamRecDets(defDtParamId, dateParamName);
        LocalDate today = LocalDate.parse(todayDate, formatter);
        LocalDate start = startDate.isEmpty() ? today : LocalDate.parse(startDate, formatter);
        LocalDate end = endDate.isEmpty() ? getEndDateBasedOnParamRec(daetFilterVal, today)
                : LocalDate.parse(endDate, formatter);
        for (String arrId : preFinalSet) {
            LocalDate matDt = getMatDateFromAccountDets(arrId);
            if (validateDateRange(matDt, start, end)) {
                branchDistrict = "";
                branchState = "";
                centerName = "";
                centerCode = "";
                customerName = "";
                customerNumber = "";
                customerAge = "";
                religion = "";
                occupation = "";
                purpose = "";
                loanAccountNumber = "";
                legacyAcctNo = "";
                loanDate = "";
                loanAmount = "";
                productName = "";
                cycle = "";
                roOfficerName = "";
                roMobileNumber = "";
                branchManagerName = "";
                branchManagerMobileNumber = "";
                disbursementDate = "";
                maturityDate = "";
                outstanding = "";

                contract.setContractId(arrId);
                getArrangementDetails(arrId);
                getCustomerDetails(customerNumber);
                getAccountDetails(acctNo);
                getAaArrTermAmountDetails(contract);
                getEbFfLoanDetails(arrId);
                getEcbDetails(contract);

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
                row.add(customerName);
                row.add(customerNumber);
                row.add(customerAge);
                row.add(religion);
                row.add(occupation);
                row.add(purpose);
                row.add(loanAccountNumber);
                row.add(legacyAcctNo);
                row.add(convertDate(loanDate));
                row.add(loanAmount);
                row.add(productName);
                row.add(cycle);
                row.add(roOfficerName);
                row.add(roMobileNumber);
                row.add(branchManagerName);
                row.add(branchManagerMobileNumber);
                row.add(convertDate(disbursementDate));
                row.add(convertDate(maturityDate));
                row.add(outstanding);

                retvalues.add(String.join("*", row));
                outvalues.add(String.join(",", row));
                
                migratedContractFlg = false;
            }
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

    public boolean validateDateRange(LocalDate matDt, LocalDate start, LocalDate end) {
        if (matDt == null || start == null || end == null) {
            return false;
        }
        return (matDt.isEqual(start) || matDt.isAfter(start)) && (matDt.isEqual(end) || matDt.isBefore(end));
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

    public void initialiseCompanyInfo(String companyId) {
        try {
            CompanyRecord companyObj = new CompanyRecord(da.getRecord("COMPANY", companyId));
            finMnemonic = companyObj.getFinancialMne().getValue();
            mnemonic = companyObj.getCustomerMnemonic().getValue();
            branchCode = companyId;
            branchState = companyObj.getLocalRefField("FF.STATE").getValue();

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

    public Set<String> getPreFinalset(Set<String> futureArrSet, Set<String> selectionSet) {
        Set<String> preFinalSet = null;
        if (selectionSet.isEmpty()) {
            if (!filterValSet.isEmpty()) {
                noRecErrFlag = true;
            } else {
                preFinalSet = futureArrSet;
            }
        } else {
            preFinalSet = new LinkedHashSet<>(selectionSet);
        }
        return preFinalSet == null ? new LinkedHashSet<>() : preFinalSet;
    }

    public Set<String> getFilterCriteriaDets(List<FilterCriteria> filterCriteria) {
        Set<String> selectionSet = null;
        try {
            for (FilterCriteria filter : filterCriteria) {
                if (!filter.getFieldname().equals("BRANCH")) {
                    Set<String> currentFilterSet = new LinkedHashSet<>();
                    String value = filter.getValue();
                    if (value == null || value.isEmpty())
                        continue;

                    switch (filter.getFieldname()) {

                    case "USER":
                        seluser = value;
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
                                "WITH ARR.STATUS NE PENDING.CLOSURE CLOSE AND PRODUCT EQ " + selProduct));
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
                da.selectRecords(finMnemonic, "AA.CUSTOMER.ARRANGEMENT", "", ""));
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

    public void getArrangementDetails(String arrId) {
        try {
            AaArrangementRecord arrRec = new AaArrangementRecord(da.getRecord("", AA_ARRANGEMENT, "", arrId));
            loanAccountNumber = arrId;
            acctNo = arrRec.getLinkedAppl().get(0).getLinkedApplId().getValue();
            customerNumber = arrRec.getCustomer().get(0).getCustomer().getValue();
            loanDate = arrRec.getStartDate().getValue();

            if (arrRec.getOrigContractDate().getValue() != null && !arrRec.getOrigContractDate().getValue().isEmpty()) {
                migratedContractFlg = true;
                disbursementDate = arrRec.getOrigContractDate().getValue();
                loanDate = arrRec.getOrigContractDate().getValue();
            } else {
                disbursementDate = arrRec.getStartDate().getValue();
                loanDate = arrRec.getStartDate().getValue();
            }

            getAaProductDetails(arrRec.getProduct().get(0).getProduct().getValue());
            initialiseCompanyInfo(arrRec.getCoCodeRec().getValue());
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

    public void getCustomerDetails(String custNumber) {
        try {
            StringBuilder custNameBuild = new StringBuilder();
            CustomerRecord cusRec = new CustomerRecord(da.getRecord(mnemonic, "CUSTOMER", "", custNumber));
            religion = cusRec.getLocalRefField(RELIG_GROUP).getValue();

            String dob = cusRec.getDateOfBirth().getValue();
            LocalDate todayDt = LocalDate.parse(todayDate, formatter);
            LocalDate dateOfBirth = LocalDate.parse(dob, formatter);
            customerAge = String.valueOf(Period.between(dateOfBirth, todayDt).getYears());

            if (migratedContractFlg) {
                customerNumber = cusRec.getMnemonic().getValue();
            }

            TField name1Field = (cusRec.getName1() != null && !cusRec.getName1().isEmpty()) ? cusRec.getName1().get(0)
                    : null;

            TField name2Field = (cusRec.getName2() != null && !cusRec.getName2().isEmpty()) ? cusRec.getName2().get(0)
                    : null;

            List<String> cusNameVal = Arrays.asList(checkFiled(name1Field), checkFiled(name2Field),
                    checkFiled(cusRec.getFamilyName()));
            for (String cusNameValList : cusNameVal) {
                appendIfNotEmpty(custNameBuild, cusNameValList);
            }
            customerName = custNameBuild.toString();

        } catch (Exception e) {
            e.getMessage();
        }

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
                maturityDate = aaArrTermAmtRec.getMaturityDate().getValue();
                loanAmount = aaArrTermAmtRec.getAmount().getValue();
            }
        } catch (Exception e) {
            e.getMessage();
        }
    }

    public void getAccountDetails(String acctNo) {
        try {
            AccountRecord accRec = new AccountRecord(da.getRecord(finMnemonic, "ACCOUNT", "", acctNo));
            if (migratedContractFlg) {
                for (AltAcctTypeClass altType : accRec.getAltAcctType()) {
                    if (altType.getAltAcctType().getValue().equals("LEGACY")) {
                        legacyAcctNo = altType.getAltAcctId().getValue();
                    }
                }
            }
            centerCode = accRec.getLocalRefField(CENTRE).getValue();
            cycle = accRec.getLocalRefField(LOAN_CYCLE).getValue();
            purpose = accRec.getLocalRefField(LOAN_PURP).getValue();
            if (centerCode != null && !centerCode.isEmpty()) {
                getEbFfCentreDetails(centerCode);
            }
        } catch (Exception e) {
            e.getMessage();
        }
    }

    public void getEbFfCentreDetails(String centre) {
        try {
            EbFfCentreDetailRecord centreRec = new EbFfCentreDetailRecord(
                    da.getRecord("", "EB.FF.CENTRE.DETAIL", "", centre));
            centerName = centreRec.getCenterName().getValue();
            String branchManager = centreRec.getBranchManager().getValue();
            if (branchManager != null && !branchManager.isEmpty()) {
                getUserDets(branchManager);
            }

            String ro = centreRec.getCurrentRo().getValue();
            if (ro != null && !ro.isEmpty()) {
                getEbFfRoUserDets(ro);
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

    public void getEbFfRoUserDets(String ro) {
        try {
            EbFfRoUserRecord roUserRec = new EbFfRoUserRecord(da.getRecord("", "EB.FF.RO.USER", "", ro));
            roOfficerName = roUserRec.getRoName().getValue();
            roMobileNumber = roUserRec.getRoMobileNumber().getValue();
        } catch (Exception e) {
            e.getMessage();
        }
    }

    public void getEbFfLoanDetails(String arrId) {
        try {
            EbFfLoanDetailsRecord ffLoanDetsRec = new EbFfLoanDetailsRecord(
                    da.getRecord(finMnemonic, "EB.FF.LOAN.DETAILS", "", arrId));
            if (!ffLoanDetsRec.toString().isEmpty()) {
                for (AddressTypeClass addressType : ffLoanDetsRec.getAddressType()) {
                    branchDistrict = addressType.getDistrictName().getValue();
                }

                for (int entiNo = 0; entiNo < ffLoanDetsRec.getFmEntityNumber().size(); entiNo++) {
                    String relation = ffLoanDetsRec.getFmEntityNumber().get(entiNo).getRelation().getValue();
                    if (relation.equalsIgnoreCase("SELF")) {
                        occupation = ffLoanDetsRec.getFmEntityNumber().get(entiNo).getOccupation().getValue();
                    }
                }
            }
        } catch (Exception e) {
            e.getMessage();
        }

    }

    public void getEcbDetails(Contract contract) {
        try {
            double totOutstanding = Double.parseDouble(getBalance(contract, "FFPRINODFUTAMT", TRADE));
            outstanding = String.format("%.2f", Math.abs(totOutstanding));

        } catch (NumberFormatException e) {
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
                if (data == null || data.isEmpty()) {
                    writer.write("No records matched the selection criteria" + System.lineSeparator());
                } else {
                    if (!fileExists) {
                        String header = String.join(",", "ZoneName", "RegionName", "DivisionName", "ClusterName",
                                "BranchName", "BranchDistrict", "BranchState", "BranchCode", "CenterName", "CenterCode",
                                "CustomerName", "CustomerNumber", "CustomerAge", "Religion", "Occupation", "Purpose",
                                "LoanAccountNumber", "LegacyAccountNumber", "LoanDate", "LoanAmount", "ProductName",
                                "Cycle", "RelationshipOfficerName", "RelationshipOfficerMobileNumber",
                                "BranchManagerName", "BranchManagerMobileNumber", "DisbursementDate", "MaturityDate",
                                "Outstanding");
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
