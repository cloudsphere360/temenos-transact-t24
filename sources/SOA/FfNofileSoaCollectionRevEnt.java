package com.temenos.fusion;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.temenos.api.exceptions.T24CoreException;
import com.temenos.t24.api.arrangement.accounting.Contract;
import com.temenos.t24.api.complex.eb.enquiryhook.EnquiryContext;
import com.temenos.t24.api.complex.eb.enquiryhook.FilterCriteria;
import com.temenos.t24.api.hook.system.Enquiry;
import com.temenos.t24.api.records.aaactivityhistory.AaActivityHistoryRecord;
import com.temenos.t24.api.records.aaactivityhistory.ActivityRefClass;
import com.temenos.t24.api.records.aaactivityhistory.EffectiveDateClass;
import com.temenos.t24.api.records.company.CompanyRecord;
import com.temenos.t24.api.records.fttxntypecondition.FtTxnTypeConditionRecord;
import com.temenos.t24.api.records.fundstransfer.FundsTransferRecord;
import com.temenos.t24.api.system.DataAccess;
import com.temenos.t24.api.system.Session;

public class FfNofileSoaCollectionRevEnt extends Enquiry {

    public static final String NO_REC_ERR = "EB-NO.REC.SELECT";
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");

    String txnType = "";
    String debitAccNo = "";
    String creditAccNo = "";
    String processDate = "";
    String recStatus = "";
    String creditAmount = "";
    String debitAmount = "";
    String creditDate = "";
    String txnAmount = "";

    String arrangementId = "";
    String selDate = "";
    String selDateOp = "";

    List<String> retvalues = new ArrayList<>();
    List<String> reversalDateListArr = new ArrayList<>();
    Set<String> ftIdList = new HashSet<>();
    Map<String, String> ftMap = new HashMap<>();
    DataAccess da = new DataAccess(this);
    String todayDate = "";
    String finMnemonic = "";
    String mnemonic = "";
    String startDate = "";
    String endDate = "";
    boolean noDateFilter = false;
    String ftId = "";
    String fundTxnId = "";
    String effectDate = "";
    String latestFtId = "";
    String reversalDate = "";
    FtTxnTypeConditionRecord ftTxnCondRec = null;
    String txnDes = "";

    @Override
    public List<String> setIds(List<FilterCriteria> filterCriteria, EnquiryContext enquiryContext) {

        try {
            Contract contract = new Contract(this);
            Session session = new Session(this);
            todayDate = session.getCurrentVariable("!TODAY");
            String companyId = session.getCompanyId();

            getSelectionFilterCriteriaValue(filterCriteria);
            initialiseCompanyInfo(companyId);
            contract.setContractId(arrangementId);

            AaActivityHistoryRecord aaActHisRec = new AaActivityHistoryRecord(
                    da.getRecord(finMnemonic, "AA.ACTIVITY.HISTORY", "", arrangementId));
            List<EffectiveDateClass> effectiveDateList = aaActHisRec.getEffectiveDate();

            for (int i = effectiveDateList.size() - 1; i >= 0; i--) {

                List<ActivityRefClass> activityRefList = effectiveDateList.get(i).getActivityRef();

                for (int j = activityRefList.size() - 1; j >= 0; j--) {
                    latestFtId = "";
                    String activitySts = activityRefList.get(j).getActStatus().getValue();
                    String contractId = activityRefList.get(j).getContractId().getValue();
                    if (contractId.startsWith("FT")) {
                        if ((activitySts.equalsIgnoreCase("AUTH-REV"))) {
                            effectDate = effectiveDateList.get(i).getEffectiveDate().getValue();
                            latestFtId = contractId.split("\\\\")[0];
                            ftMap.put(latestFtId, effectDate);
                        } else if (activitySts.equalsIgnoreCase("AUTH")) {
                            effectDate = effectiveDateList.get(i).getEffectiveDate().getValue();
                            latestFtId = contractId.split("\\\\")[0];
                            ftMap.remove(latestFtId, effectDate);
                        }
                    }
                }
            }
            getTheFtDetails(ftMap);
        } catch (Exception e) {
            e.getMessage();
        }
        if (retvalues.isEmpty()) {
            throw new T24CoreException("", NO_REC_ERR);
        } else {
            return retvalues;
        }
    }

    private void returnTheFinalValue() {
        try {
            boolean addToFinalList = false;

            if (noDateFilter) {
                addToFinalList = true;
            } else {
                LocalDate creditDt = LocalDate.parse(creditDate, formatter);
                LocalDate stDt = LocalDate.parse(startDate, formatter);
                LocalDate enDt = LocalDate.parse(endDate, formatter);

                if ((creditDt.isEqual(stDt) || creditDt.isAfter(stDt))
                        && (creditDt.isEqual(enDt) || creditDt.isBefore(enDt))) {
                    addToFinalList = true;
                }
            }
            if (addToFinalList) {
                retvalues.add(fundTxnId + "*" + txnDes + "*" + debitAccNo + "*" + creditAccNo + "*" + txnAmount + "*"
                        + processDate + "*" + reversalDate + "*" + recStatus);
            }
        } catch (Exception e) {
            e.getMessage();
        }

    }

    private void getTheFtDetails(Map<String, String> ftMap) {

        try {
            FundsTransferRecord ftRec = null;
            if (!ftMap.isEmpty()) {
                for (String txnId : ftMap.keySet()) {
                    fundTxnId = txnId;
                    ftRec = getFtRecord();

                    if (ftRec != null) {
                        reversalDate = ftMap.get(fundTxnId);
                        txnType = ftRec.getTransactionType().getValue();
                        ftTxnCondRec = getFtTxnCondition();

                        txnDes = ftTxnCondRec.getDescription(0).getValue();
                        debitAccNo = ftRec.getDebitAcctNo().getValue();
                        creditAccNo = ftRec.getCreditAcctNo().getValue();
                        creditAmount = ftRec.getCreditAmount().getValue();
                        debitAmount = ftRec.getDebitAmount().getValue();
                        if(creditAmount != null && !creditAmount.isEmpty()) {
                            txnAmount = creditAmount;
                        }else if(debitAmount != null && !debitAmount.isEmpty()) {
                            txnAmount = debitAmount;
                        }
                        
                        processDate = ftRec.getProcessingDate().getValue();
                        recStatus = ftRec.getRecordStatus();
                        creditDate = ftRec.getCreditValueDate().getValue();

                        getDates(selDate, selDateOp, todayDate, creditDate);
                        returnTheFinalValue();

                    }
                }
            }
        } catch (Exception e) {
            e.getMessage();
        }
    }

    private FtTxnTypeConditionRecord getFtTxnCondition() {

        try {
            ftTxnCondRec = new FtTxnTypeConditionRecord(da.getRecord("", "FT.TXN.TYPE.CONDITION", "", txnType));
        } catch (Exception e) {
            e.getMessage();
        }
        return ftTxnCondRec;
    }

    private FundsTransferRecord getFtRecord() {
        FundsTransferRecord ftRec = null;
        try {
            ftRec = new FundsTransferRecord(da.getRecord(finMnemonic, "FUNDS.TRANSFER", "", fundTxnId));
        } catch (Exception e) {
            try {
                ftRec = new FundsTransferRecord(da.getHistoryRecord("FUNDS.TRANSFER", fundTxnId));
            } catch (Exception e1) {
                e1.getMessage();
            }
        }
        return ftRec;

    }

    private void getDates(String selDate, String selDateOp, String todayDate, String creditDate) {
        try {
            switch (selDateOp) {
            case "1":
                if (selDate == null || selDate.isEmpty()) {
                    startDate = creditDate;
                    endDate = todayDate;
                    noDateFilter = true;
                } else {
                    startDate = selDate;
                    endDate = selDate;
                }
                break;
            case "2":
                String[] dateRange = selDate.split(" ");
                startDate = dateRange[0];
                endDate = dateRange[1];
                break;
            case "8":
                startDate = creditDate;
                endDate = selDate;
                break;
            case "3":
                LocalDate givenDateForLT = LocalDate.parse(selDate, formatter);
                startDate = creditDate;
                endDate = givenDateForLT.minusDays(1).format(formatter);
                break;
            case "9":
                startDate = selDate;
                endDate = todayDate;
                break;
            case "4":
                LocalDate givenDateForGT = LocalDate.parse(selDate, formatter);
                startDate = givenDateForGT.plusDays(1).format(formatter);
                endDate = todayDate;
                break;
            default:
                startDate = creditDate;
                endDate = todayDate;
                noDateFilter = true;
                break;
            }
        } catch (Exception e) {
            e.getMessage();
        }
    }

    private void initialiseCompanyInfo(String companyId) {
        try {
            CompanyRecord companyObj = new CompanyRecord(da.getRecord("COMPANY", companyId));
            finMnemonic = companyObj.getFinancialMne().getValue();
            mnemonic = companyObj.getCustomerMnemonic().getValue();
        } catch (Exception e) {
            e.getMessage();
        }

    }

    private void getSelectionFilterCriteriaValue(List<FilterCriteria> filterCriteria) {
        try {
            for (FilterCriteria filter : filterCriteria) {
                String value = filter.getValue();
                if (value == null || value.isEmpty())
                    continue;

                switch (filter.getFieldname()) {
                case "ARRANGEMENT.ID":
                    arrangementId = value;
                    break;
                case "DATE":
                    selDate = value;
                    selDateOp = filter.getOperand();
                    break;
                default:
                    break;
                }
            }
        } catch (Exception e) {
            e.getMessage();
        }

    }
}
