package com.temenos.fusion;

import com.temenos.api.TStructure;
import com.temenos.t24.api.complex.eb.templatehook.TransactionContext;
import com.temenos.t24.api.hook.system.RecordLifecycle;
import com.temenos.t24.api.records.company.CompanyRecord;
import com.temenos.t24.api.records.ebffparameter.EbFfParameterRecord;
import com.temenos.t24.api.records.fundstransfer.FundsTransferRecord;
import com.temenos.t24.api.system.DataAccess;
import com.temenos.t24.api.system.Session;

/**
 *
 * @author ar116388
 *
 */
public class FfBankVaultCredAcc extends RecordLifecycle {

    private static final FusionFileLogger FfBankVaultCred = FusionFileLogger.getLogger(FfBankVaultCredAcc.class);

    private final Session ses = new Session();
    private final DataAccess da = new DataAccess(this);

    String subDivCode = "";

    @Override
    public void defaultFieldValues(String application, String currentRecordId, TStructure currentRecord,
            TStructure unauthorisedRecord, TStructure liveRecord, TransactionContext transactionContext) {

        FfBankVaultCred.info("FfVFtDefCreditAccNum is triggered Successfully");
        try {
            FundsTransferRecord ftRec = new FundsTransferRecord(currentRecord);
            String compCode = ses.getCompanyId();
            CompanyRecord compRec = null;
            compRec = new CompanyRecord(da.getRecord("COMPANY", compCode));
            subDivCode = compRec.getSubDivisionCode().getValue();
            
            String baseAcctNum = getVaultCreditAccount();
            String creditAcNum = baseAcctNum + subDivCode;
            FfBankVaultCred.info("creditAcNum" + creditAcNum);
            ftRec.setCreditAcctNo(creditAcNum);
            currentRecord.set(ftRec.toStructure());
        } catch (Exception e) {
            FfBankVaultCred.info("FfVFtDefCreditAccNum Code Error " + e);
        }

    }
    
    private String getVaultCreditAccount() {
        try {
            String parameterId = "FF.TELLER.ACCT.NUM";
            
            EbFfParameterRecord paramRec = new EbFfParameterRecord(
                    da.getRecord("EB.FF.PARAMETER", parameterId));
            
            for (int i = 0; i < paramRec.getParamDesc().size(); i++) {
                
                String paramDesc = paramRec.getParamDesc(i).getParamDesc().getValue();
                
                if ("BC.VAULT.CREDIT.ACCT".equalsIgnoreCase(paramDesc)) {

                    String acctNum = paramRec.getParamDesc(i).getParamValue().getValue();

                    FfBankVaultCred.info("Vault Credit Account : " + acctNum);

                    return acctNum;
                }
            }
        }catch (Exception e) {
            FfBankVaultCred.info("Error reading EB.FF.PARAMETER : " + e);
        }
        
        return "";
    }

}
