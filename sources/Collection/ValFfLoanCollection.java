package com.temenos.fusion;

import java.util.List;
import java.math.BigDecimal;
import java.math.RoundingMode;
import com.temenos.api.TStructure;
// import com.temenos.api.TValidationResponse;
import com.temenos.logging.facade.Logger;
import com.temenos.logging.facade.LoggerFactory;
import com.temenos.t24.api.arrangement.accounting.Contract;
import com.temenos.t24.api.complex.aa.contractapi.BalanceMovement;
import com.temenos.t24.api.complex.eb.templatehook.TransactionContext;
import com.temenos.t24.api.hook.system.RecordLifecycle;
import com.temenos.t24.api.records.account.AccountRecord;
import com.temenos.t24.api.records.fundstransfer.FundsTransferRecord;
import com.temenos.t24.api.system.DataAccess;
import com.temenos.t24.api.system.Session;

public class ValFfLoanCollection extends RecordLifecycle {
     static final String BOOKING = "BOOKING";

    private static final String L3API = "L3API";
    private static final Logger LOGGER = LoggerFactory.getLogger(L3API);

    @Override
    public void defaultFieldValues(String application, String currentRecordId, TStructure currentRecord,
            TStructure unauthorisedRecord, TStructure liveRecord, TransactionContext transactionContext) {
        EnqFfCollectionResponse enqcoll = new EnqFfCollectionResponse();

        FundsTransferRecord ftRec = new FundsTransferRecord(this);

        try {
            LOGGER.info("fundtra triggered ");
            ftRec = new FundsTransferRecord(currentRecord);
            DataAccess da = new DataAccess(this);
            Session ssObj = new Session(this);
            String comMne = ssObj.getCompanyRecord().getFinancialMne().getValue();
            String creditAccount = ftRec.getCreditAcctNo().getValue();
            AccountRecord accRec = new AccountRecord(da.getRecord(comMne, "ACCOUNT", "", creditAccount));

            LOGGER.info("account REcord " + accRec);
            String arrId = accRec.getArrangementId().getValue();

            Contract contract = new Contract(this);
            contract.setContractId(arrId);

            List<BalanceMovement> listBalMovement = contract.getContractBalanceMovements("DUEACCOUNT", BOOKING);
            String dueprinciple = listBalMovement.get(0).getBalance().toString();

            listBalMovement = contract.getContractBalanceMovements("DUEPRINTEREST", BOOKING);
            String dueinterest = listBalMovement.get(0).getBalance().toString();

            BigDecimal dueprin = new BigDecimal(dueprinciple).setScale(2, RoundingMode.HALF_UP);
            BigDecimal dueint = new BigDecimal(dueinterest).setScale(2, RoundingMode.HALF_UP);
            BigDecimal netDueOut = dueprin.add(dueint);

            BigDecimal overdueprinciple = enqcoll.getFinalInterestAmt(contract);
            BigDecimal overdueinterest = enqcoll.getFinalInterestAmt(contract);

            listBalMovement = contract.getContractBalanceMovements("UNCACCOUNT", BOOKING);
            String parkAmt = listBalMovement.get(0).getBalance().toString();

            BigDecimal nabPrin = overdueprinciple.setScale(2, RoundingMode.HALF_UP);
            BigDecimal nabint = overdueinterest.setScale(2, RoundingMode.HALF_UP);
            BigDecimal overdue = nabPrin.add(nabint);
            BigDecimal totOut = overdue.add(netDueOut);

            BigDecimal uncAmount = new BigDecimal(parkAmt).setScale(2, RoundingMode.HALF_UP);

            String collectiontype = ftRec.getLocalRefField("FF.COLL.TYPE").getValue();
            if (collectiontype.equals("FORECLOSURE")) {
                ftRec.setTransactionType("ACP2");
            } else {

                ftRec.setTransactionType("ACRP");
            }

            ftRec.getLocalRefField("FF.BEF.TOT.DUE").setValue(totOut.toString());
            ftRec.getLocalRefField("FF.BEF.DUE.PRIN").setValue(dueprin.toString());
            ftRec.getLocalRefField("FF.BEF.DUE.INT").setValue(dueint.toString());
            ftRec.getLocalRefField("FF.BEF.OVER.DUE").setValue(overdue.toString());
            ftRec.getLocalRefField("FF.BEF.PARK.AMT").setValue(uncAmount.toString());

            LOGGER.info("fundtransfer.REC ");

        } catch (Exception e) {
            e.printStackTrace();
            LOGGER.info("error " + e.getMessage());
        }
        currentRecord.set(ftRec.toStructure());
      
    }

}
