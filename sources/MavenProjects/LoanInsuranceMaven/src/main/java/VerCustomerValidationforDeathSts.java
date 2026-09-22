package com.temenos.fusion;

import java.util.ArrayList;
import java.util.List;

import com.temenos.api.LocalRefList;
import com.temenos.api.TStructure;
import com.temenos.api.TValidationResponse;
import com.temenos.logging.facade.Logger;
import com.temenos.logging.facade.LoggerFactory;
import com.temenos.t24.api.complex.eb.templatehook.TransactionContext;
import com.temenos.t24.api.hook.system.RecordLifecycle;
import com.temenos.t24.api.records.account.AccountRecord;
import com.temenos.t24.api.records.customer.CustomerRecord;
import com.temenos.t24.api.records.ebffloandetails.EbFfLoanDetailsRecord;
import com.temenos.t24.api.records.ebffloandetails.FmEntityNumberClass;
import com.temenos.t24.api.system.DataAccess;
import com.temenos.t24.api.system.Session;

public class VerCustomerValidationforDeathSts extends RecordLifecycle {
    private static final String L3API = "L3API";
    private static final Logger verLogger = LoggerFactory.getLogger(L3API);
    String currStatus = "FF.CUR.DOD.STS";
    public static final String CONFIRMED_STS = "CONFIRMED";
    public static final String REJECTED_STS = "REJECTED";
    public static final String GUAR_DOD_STS = "FF.GUAR.DOD.STS";
    public static final String LOAN_NUM = "FF.LOAN.NUMBER";
    VerGuarantorDeathDateUpd verGuarantorUpdObj = new VerGuarantorDeathDateUpd();
    VerDeathDateUpdate verDeathDateUpdateObj = new VerDeathDateUpdate();

    @Override
    public void defaultFieldValues(String application, String currentRecordId, TStructure currentRecord,
            TStructure unauthorisedRecord, TStructure liveRecord, TransactionContext transactionContext) {
        try {

            verLogger.info("default routine triggered");
            CustomerRecord cusRec = new CustomerRecord(currentRecord);
            String dodStatus = cusRec.getLocalRefField(currStatus).getValue();
            verLogger.info("dodStatus " + dodStatus);

            if (dodStatus.equals(CONFIRMED_STS)) {
                verLogger.info("inside if");
                cusRec.getLocalRefField("FF.IS.ALIVE").setValue("NO");

            } else if (dodStatus.equals(REJECTED_STS)) {
                verLogger.info("inside else");
                cusRec.getLocalRefField("FF.IS.ALIVE").setValue("YES");
            }
            verLogger.info("cusRec " + cusRec);
            currentRecord.set(cusRec.toStructure());

        } catch (Exception e) {
            verLogger.info(e.getMessage());
        }
    }

    @Override
    public TValidationResponse validateRecord(String application, String currentRecordId, TStructure currentRecord,
            TStructure unauthorisedRecord, TStructure liveRecord, TransactionContext transactionContext) {
        CustomerRecord cusRec = new CustomerRecord(currentRecord);

        try {

            DataAccess daObj = new DataAccess(this);
            Session ssObj = new Session(this);
            String compMne = ssObj.getCompanyRecord().getFinancialMne().getValue();
            CustomerRecord liveCusRec = new CustomerRecord(liveRecord);

            String dodStatus = cusRec.getLocalRefField(currStatus).getValue();
            String previousDodSts = liveCusRec.getLocalRefField(currStatus).getValue();
            cusRec = cusReccheckdeathTagStatus(cusRec, dodStatus);

            List<String> accountRecList = verDeathDateUpdateObj.getAccountList(daObj, currentRecordId);
            List<String> loanIds = getLoanList(accountRecList, daObj, compMne);

            LocalRefList loanNumbers = cusRec.getLocalRefGroups(LOAN_NUM);
            cusRec = checkLoanNumber(loanNumbers, cusRec, loanIds);

            if (previousDodSts.equals(CONFIRMED_STS) && dodStatus.equals(REJECTED_STS)) {
                cusRec.getLocalRefField(currStatus)
                        .setError("Confirmed status already updated so REJECTED is not allowed");

            } else if (previousDodSts.equals(REJECTED_STS) && dodStatus.equals(CONFIRMED_STS)) {
                cusRec.getLocalRefField(currStatus)
                        .setError("REJECTED status already updated so CONFIRMED is not allowed");

            }
            cusRec = cusReccheckLegalName(compMne, daObj, cusRec);

        } catch (Exception e) {
            verLogger.info(e.getMessage());
        }
        return cusRec.getValidationResponse();
    }

    /**
     * @param cusRec
     * @param loanNumbers
     * @param loanIds
     * @return
     * 
     */
    public CustomerRecord checkLoanNumber(LocalRefList loanNumbers, CustomerRecord cusRec, List<String> loanIds) {
        try {

            if (!loanNumbers.isEmpty()) {
                for (int i = 0; i < loanNumbers.size(); i++) {

                    String arrId = loanNumbers.get(i).getLocalRefField(LOAN_NUM).getValue();
                    verLogger.info("arrId " + arrId);
                    verLogger.info("loanIds.contains(arrId) " + loanIds.contains(arrId));
                    if (!loanIds.contains(arrId)) {
                        verLogger.info("inside if deathDate");
                        cusRec.getDeathDate()
                                .setError("The Provided loan number does not belong to the underlying customer");
                        verLogger.info("cusrec " + cusRec);
                    }
                }
            }
        } catch (Exception e) {
            verLogger.info(e.getMessage());
        }
        return cusRec;
    }

    /**
     * @param accountRecList
     * @param compMne
     * @param daObj
     * @return
     */
    private List<String> getLoanList(List<String> accountRecList, DataAccess daObj, String compMne) {
        List<String> loanIds = new ArrayList<>();
        try {
            for (String accId : accountRecList) {
                AccountRecord accRec = new AccountRecord(daObj.getRecord(compMne, "ACCOUNT", "", accId));
                String arrId = accRec.getArrangementId().getValue();
                loanIds.add(arrId);
            }
            verLogger.info("loanIds" + loanIds);
        } catch (Exception e) {
            verLogger.info(e.getMessage());
        }
        return loanIds;
    }

    /**
     * @param cusRec
     * @param dodStatus
     * @return
     */
    private CustomerRecord cusReccheckdeathTagStatus(CustomerRecord cusRec, String dodStatus) {
        String deathTagFld = "FF.DEATH.TAG";
        try {
            String deathTag = cusRec.getLocalRefField(deathTagFld).getValue();
            LocalRefList guarSts = cusRec.getLocalRefGroups(GUAR_DOD_STS);
            if (!deathTag.isEmpty()) {
                if (deathTag.equals("CUSTOMER") && !guarSts.isEmpty()) {
                    cusRec.getLocalRefField(deathTagFld).setError("Only CUSTOMER status is allowed");
                } else if (deathTag.equals("GUARANTOR") && !dodStatus.isEmpty()) {
                    cusRec.getLocalRefField(deathTagFld).setError("Only GUARANTOR status is allowed");
                }
            }
        } catch (Exception e) {
            verLogger.info(e.getMessage());
        }
        return cusRec;
    }

    /**
     * @param compMne
     * @param daObj
     * @param cusRec
     * @return
     */
    public CustomerRecord cusReccheckLegalName(String compMne, DataAccess daObj, CustomerRecord cusRec) {
        try {
            LocalRefList loanNum = cusRec.getLocalRefGroups(LOAN_NUM);
            int loanSize = loanNum.size();
            for (int i = 0; i < loanSize; i++) {
                String arrId = loanNum.get(i).getLocalRefField(LOAN_NUM).getValue();
                String guarName = loanNum.get(i).getLocalRefField("FF.GUAR.NAME").getValue();
                if (!arrId.isEmpty()) {
                    EbFfLoanDetailsRecord ebFfLoanDetailsObj = verGuarantorUpdObj.readEbFfRecord(compMne, daObj, arrId);
                    verLogger.info("ebFfLoanDetailsObj" + ebFfLoanDetailsObj);
                    List<FmEntityNumberClass> fmEntityList = ebFfLoanDetailsObj.getFmEntityNumber();

                    for (FmEntityNumberClass fmEntity : fmEntityList) {

                        String isGuar = fmEntity.getIsGuarantor().getValue();
                        if (isGuar.equals("YES")) {
                            String legalName = fmEntity.getLegalName().getValue();
                            if (!legalName.equals(guarName)) {
                                cusRec.getLocalRefGroups(LOAN_NUM).get(i).getLocalRefField("FF.GUAR.NAME")
                                        .setError("Gurantor name and legal name in FF.LOAN.DETAILS is not same");
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            verLogger.info(e.getMessage());
        }
        return cusRec;
    }

}
