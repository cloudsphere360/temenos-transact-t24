package com.temenos.fusion;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.temenos.api.TStructure;
import com.temenos.logging.facade.Logger;
import com.temenos.logging.facade.LoggerFactory;
import com.temenos.t24.api.complex.eb.servicehook.TransactionData;
import com.temenos.t24.api.complex.eb.templatehook.TransactionContext;
import com.temenos.t24.api.hook.system.RecordLifecycle;
import com.temenos.t24.api.records.aaactivityhistory.AaActivityHistoryRecord;
import com.temenos.t24.api.records.aaactivityhistory.ActivityRefClass;
import com.temenos.t24.api.records.aaactivityhistory.EffectiveDateClass;
import com.temenos.t24.api.records.ebffloanactivity.EbFfLoanActivityRecord;
import com.temenos.t24.api.records.fundstransfer.FundsTransferRecord;
import com.temenos.t24.api.system.DataAccess;
import com.temenos.t24.api.system.Session;

/**
 * TODO: Document me!
 *
 * @author mp116300
 *
 */
public class FfAaNetOffReversalRout extends RecordLifecycle {
    DataAccess da = new DataAccess(this);
    Session session = new Session(this);
    String arrId = "";
    private static final String L3API = "L3API";
    private static final Logger Logger = LoggerFactory.getLogger(L3API);

    @Override
    public void postUpdateRequest(String application, String currentRecordId, TStructure currentRecord,
            List<TransactionData> transactionData, List<TStructure> currentRecords,
            TransactionContext transactionContext) {

        EbFfLoanActivityRecord loanActRec = new EbFfLoanActivityRecord(currentRecord);

        List<String> contrList = new ArrayList<>();
        List<String> contrList1 = new ArrayList<>();
        arrId = loanActRec.getArrangementId().getValue();
        Logger.info("arrId " + arrId);
        if (!arrId.equals("")) {
            try {
                AaActivityHistoryRecord aaActHistRec = new AaActivityHistoryRecord(
                        da.getRecord("AA.ACTIVITY.HISTORY", arrId));
                Logger.info("aaActHistRec after data access: " + aaActHistRec);
                contrList = getContractIds(aaActHistRec);
                contrList1 = getContractIdsUser(aaActHistRec);
                contrList = removeDuplicates(contrList);
                contrList1 = removeDuplicates(contrList1);
                Logger.info("List of contract Ids: " + contrList.toString());
                Logger.info("List of contract Ids of type User: " + contrList1.toString());
                if (!contrList.isEmpty()) {
                    for (String id : contrList) {
                        if (id.startsWith("FT")) {
                            String secondId = "";
                            FundsTransferRecord ftRecForRev = new FundsTransferRecord(da.getRecord(
                                    session.getCompanyRecord().getFinancialMne().toString(), "FUNDS.TRANSFER", "", id));
                            String fClose = ftRecForRev.getLocalRefField("FF.NETOFF.CLOSE").toString();
                            String finalFClose = (fClose != null && !fClose.trim().isEmpty()) ? fClose : "";
                            Logger.info("finalFClose FT to be reversed: " + finalFClose);
                            String fRepay = ftRecForRev.getLocalRefField("FF.NETOFF.REPAY").toString();
                            String finalFrepay = (fRepay != null && !fRepay.trim().isEmpty()) ? fRepay : "";
                            Logger.info("finalFrepay FT to be reversed: " + finalFrepay);
                            if (!finalFClose.isEmpty()) {
                                secondId = getDataActHistory(finalFClose);
                            } else if (!finalFrepay.isEmpty()) {
                                secondId = getDataActHistory(finalFrepay);
                            }
                            Logger.info("Second Id FT to be reversed: " + secondId);
                            postFtReverse(id, transactionData);
                            postFtReverse(secondId, transactionData);
                            currentRecords.add(loanActRec.toStructure());
                        } else if (id.startsWith("AAA")) {
                            postAaaReverse(id, transactionData);
                            currentRecords.add(loanActRec.toStructure());
                        }

                    }
                }

                if (!contrList1.isEmpty()) {
                    for (String id1 : contrList1) {
                        if (id1.startsWith("AAA")) {
                            postAaaReverse(id1, transactionData);
                            currentRecords.add(loanActRec.toStructure());
                        }
                    }
                }
            } catch (Exception e) {

            }

        }

    }

    /**
     * @param id
     * @return
     */
    private String getDataActHistory(String arrIdFt) {

        /*
         * EbFfLoanNetoffFtRecord netOffRec = new
         * EbFfLoanNetoffFtRecord(da.getRecord("EB.FF.LOAN.NETOFF.FT", id));
         * Logger.info("Table Record of Netoff: " + netOffRec); String ftToRev =
         * netOffRec.getTransReference(0).toString();
         * Logger.info("The FT to be reversed: " + ftToRev);
         */
        String contrId = "";
        try {

            AaActivityHistoryRecord aaActHistRecFt = new AaActivityHistoryRecord(
                    da.getRecord("AA.ACTIVITY.HISTORY", arrIdFt));
            for (EffectiveDateClass effectiveDate : aaActHistRecFt.getEffectiveDate()) {
                for (ActivityRefClass actRefCls : effectiveDate.getActivityRef()) {
                    String initType = actRefCls.getInitiation().toString();
                    String transInitType = actRefCls.getTransactionInitiation().toString();
                    Logger.info("initType " + initType);
                    if (initType.equals("TRANSACTION") && transInitType.equals("CUSTOMER")) {
                        contrId = getcontractId(actRefCls);
                        Logger.info("contrId " + contrId);
                    }
                }
            }

        } catch (Exception e) {

        }
        Logger.info("contrId BEFORE RETURN" + contrId);
        return contrId;
    }

    private void postAaaReverse(String id, List<TransactionData> transactionData) {
        Logger.info("AAA id: " + id);
        TransactionData txnData1 = new TransactionData();
        txnData1.setFunction("REVERSE");
        txnData1.setNumberOfAuthoriser("0");
        txnData1.setSourceId("NETOFF.OFS");
        txnData1.setCompanyId(session.getCompanyId());
        txnData1.setVersionId("AA.ARRANGEMENT.ACTIVITY,CANCEL");
        txnData1.setTransactionId(id);
        Logger.info("txnData " + txnData1);
        transactionData.add(txnData1);

    }

    private void postFtReverse(String id, List<TransactionData> transactionData) {
        Logger.info("FT id: " + id);
        TransactionData txnData1 = new TransactionData();
        txnData1.setFunction("REVERSE");
        txnData1.setNumberOfAuthoriser("0");
        txnData1.setSourceId("NETOFF.OFS");
        txnData1.setCompanyId(session.getCompanyId());
        txnData1.setVersionId("FUNDS.TRANSFER,NETOFF.REV");
        txnData1.setTransactionId(id);
        Logger.info("txnData " + txnData1);
        transactionData.add(txnData1);
    }

    private List<String> getContractIds(AaActivityHistoryRecord aaActHistRec) {
        List<String> contractList = new ArrayList<>();
        Logger.info("aaActHistRec inside getContractIds: " + aaActHistRec);
        try {
            String contrId = "";
            for (EffectiveDateClass effectiveDate : aaActHistRec.getEffectiveDate()) {
                for (ActivityRefClass actRefCls : effectiveDate.getActivityRef()) {
                    String initType = actRefCls.getInitiation().toString();
                    Logger.info("initType " + initType);
                    if (initType.equals("TRANSACTION") || initType.equals("CUSTOMER")) {
                        contrId = getcontractId(actRefCls);
                        Logger.info("contrId " + contrId);
                        contractList.add(contrId);

                    }
                }
            }

        } catch (Exception e) {

        }

        return contractList;
    }

    private List<String> getContractIdsUser(AaActivityHistoryRecord aaActHistRec) {
        List<String> contractList = new ArrayList<>();
        Logger.info("aaActHistRec inside getContractIds: " + aaActHistRec);
        try {
            String contrId = "";
            for (EffectiveDateClass effectiveDate : aaActHistRec.getEffectiveDate()) {
                for (ActivityRefClass actRefCls : effectiveDate.getActivityRef()) {
                    String initType = actRefCls.getInitiation().toString();
                    Logger.info("initType " + initType);
                    if (initType.equals("USER")) {
                        contrId = getcontractId(actRefCls);
                        Logger.info("contrId " + contrId);
                        contractList.add(contrId);

                    }
                }
            }

        } catch (Exception e) {

        }

        return contractList;
    }

    public String getcontractId(ActivityRefClass actRefCls) {
        String finalid = "";
        try {
            if (!actRefCls.getContractId().toString().isEmpty()) {
                String contid = actRefCls.getContractId().toString();
                String[] splitSpecPos = contid.split("\\\\");
                finalid = splitSpecPos[0];

            } else {
                finalid = actRefCls.getActivityRef().toString();
            }

        } catch (Exception e) {

        }

        return finalid;
    }

    public List<String> removeDuplicates(List<String> inputList) {
        if (inputList == null) {
            return null; // or Collections.emptyList()
        }

        // Removes duplicates and preserves original order
        return inputList.stream().distinct().collect(Collectors.toList());
    }

}
