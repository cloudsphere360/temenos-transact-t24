package com.temenos.fusion;

import java.util.List;

import com.temenos.api.TStructure;
import com.temenos.logging.facade.Logger;
import com.temenos.logging.facade.LoggerFactory;
import com.temenos.t24.api.complex.eb.servicehook.TransactionData;
import com.temenos.t24.api.complex.eb.templatehook.TransactionContext;
import com.temenos.t24.api.hook.system.RecordLifecycle;
import com.temenos.t24.api.records.company.CompanyRecord;
import com.temenos.t24.api.records.tsaservice.TsaServiceRecord;
import com.temenos.t24.api.system.DataAccess;
import com.temenos.t24.api.system.Session;

/*-----------------------------------------------------------------------------
* @author Harshini Sakthivel
* Date Created: 27-01-2026
* Attached as : AUTH ROUTINE
* EB.API-->FF.V.CREDIT.BUREAU.AUTH
* VERSION>EB.FF.CREDIT.BUREAU.REPORT,LOAN.CORRECTION
* VERSION>EB.FF.CREDIT.BUREAU.REPORT,INTERIM
* Description: Start the TSA.SERVICE automatically while authorise the Record
*-----------------------------------------------------------------------------*/
public class FfVEbFfCreditBureauAuthProcess extends RecordLifecycle {

    private static final String L3API = "L3API";
    private static final Logger LOGGER = LoggerFactory.getLogger(L3API);
    DataAccess da = new DataAccess(this);
    Session session = new Session(this);
    String companyId = "";
    String finMnemonic = "";
    String tsaServiceId = "";

    @Override
    public void postUpdateRequest(String application, String currentRecordId, TStructure currentRecord,
            List<TransactionData> transactionData, List<TStructure> currentRecords,
            TransactionContext transactionContext) {
        try {
            LOGGER.info("FfVEbFfCreditBureauAuthProcess is triggered successfully  "
                    + transactionContext.getCurrentFunction());
            companyId = session.getCompanyId();
            initialiseCompanyInfo(companyId);

            LOGGER.info("Enter into the Auth condition check");
            if (currentRecordId.contains("LOANS")) {
                tsaServiceId = finMnemonic + "^FF.B.CBR.LOAN.CORRECTION.REPORT";
            } else if (currentRecordId.contains("INTERIM")) {
                tsaServiceId = finMnemonic + "^FF.B.CBR.INTERIM.REPORT";
            }
            TsaServiceRecord tsa = new TsaServiceRecord(this);
            tsa.setServiceControl("START");
            tsa.setUser("INPUTTER");

            TransactionData txnData = new TransactionData();
            txnData.setVersionId("TSA.SERVICE,CBR.INPUT");
            txnData.setTransactionId(tsaServiceId);
            txnData.setSourceId("OFS.CB.REPORT");
            txnData.setFunction("INPUT");
            transactionData.add(txnData);
            LOGGER.info("txnData  " + txnData);
            currentRecords.add(tsa.toStructure());

        } catch (Exception e) {
            LOGGER.info("error in auth rtn  " + e);
        }
    }

    private void initialiseCompanyInfo(String companyId) {
        try {
            CompanyRecord companyObj = new CompanyRecord(da.getRecord("COMPANY", companyId));
            finMnemonic = companyObj.getFinancialMne().getValue();
            LOGGER.info("initialiseCompanyInfo" + companyId + "**********" + finMnemonic);
        } catch (Exception e) {
            LOGGER.error("Error initialiseCompanyInfo: " + e.getMessage(), e);
        }
    }
}
