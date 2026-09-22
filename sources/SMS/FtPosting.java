package com.tem.msg.fusion;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.temenos.api.TStructure;
import com.temenos.fusion.FusionFileLogger;
import com.temenos.t24.api.arrangement.accounting.Contract;
import com.temenos.t24.api.complex.eb.servicehook.TransactionData;
import com.temenos.t24.api.complex.eb.templatehook.TransactionContext;
import com.temenos.t24.api.hook.system.RecordLifecycle;
import com.temenos.t24.api.records.aaarrangement.AaArrangementRecord;
import com.temenos.t24.api.records.account.AccountRecord;
import com.temenos.t24.api.records.customer.CustomerRecord;
import com.temenos.t24.api.records.ebffgenericparamsms.EbFfGenericParamSmsRecord;
import com.temenos.t24.api.records.ebffgenericparamsms.KeyNameClass;
import com.temenos.t24.api.records.ebfusionlogsmsupdate.EbFusionLogSmsUpdateRecord;
import com.temenos.t24.api.records.fundstransfer.FundsTransferRecord;
import com.temenos.t24.api.system.DataAccess;
import com.temenos.t24.api.system.Session;
import com.temenos.t24.api.tables.ebfusionlogsmsupdate.EbFusionLogSmsUpdateTable;

/**
 * TODO: Document me!
 *
 * @author lm116299
 *
 */
public class FtPosting extends RecordLifecycle {
    
    String processingDateOrg = "";
    String debitAmount = "";
    String creditAmount = "";
    String loanaccountId = "";
    String status = "";
    Session sn = new Session(this);
    DataAccess da = new DataAccess(this);
    Contract api = new Contract(this);
    EbFfGenericParamSmsRecord genParam = null;
    String description = "";
    String processingDate = "";
    String apiStatus = "";
    String tempStatus = "";
    EbFusionLogSmsUpdateRecord logRec = new EbFusionLogSmsUpdateRecord(this);
    EbFusionLogSmsUpdateTable logTab = new EbFusionLogSmsUpdateTable(this);
    private static final FusionFileLogger LOGGER = FusionFileLogger.getLogger(FtPosting.class);
    
    @Override
    public void postUpdateRequest(String application, String currentRecordId, TStructure currentRecord,
            List<TransactionData> transactionData, List<TStructure> currentRecords,
            TransactionContext transactionContext) {
        try {
        try {
            EbFusionLogSmsUpdateRecord dup = new EbFusionLogSmsUpdateRecord(da.getRecord("EB.FUSION.LOG.SMS.UPDATE",currentRecordId ));
            tempStatus = dup.getStatus().getValue();
            }catch(Exception e204){
                LOGGER.info("e204" + e204);
            }
        LOGGER.info("tempStatus" + tempStatus);
        FundsTransferRecord ftrec = new FundsTransferRecord(currentRecord);
        processingDateOrg = ftrec.getProcessingDate().getValue();
        LOGGER.info("processingDateOrg" + processingDateOrg);
        LOGGER.info("transactionContext" + transactionContext.toString());
        debitAmount = ftrec.getDebitAmount().getValue();
        LOGGER.info("debitAmount" + debitAmount);
        if(debitAmount.equals(null)||debitAmount.isEmpty()) {
            creditAmount = ftrec.getCreditAmount().getValue();
            debitAmount = creditAmount;
        }
        loanaccountId = ftrec.getCreditAcctNo().getValue();
        LOGGER.info("loanaccountId" + loanaccountId);
        status = transactionContext.getCurrentFunction();
        LOGGER.info("status" + status);
        try {
        DateTimeFormatter dformatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
        DateTimeFormatter yformatter = DateTimeFormatter.ofPattern("yyyyMMdd");
        LocalDate tempDate = LocalDate.parse(processingDateOrg, yformatter);
        processingDate = tempDate.format(dformatter);
        }catch(Exception e) {
            LOGGER.info("e" + e); 
        }
        if(status.equalsIgnoreCase("REVERSE") ) {
            
            String Mne = sn.getCompanyRecord().getFinancialMne().toString();
            LOGGER.info("Mne" + Mne);
            AccountRecord ar = new AccountRecord(da.getRecord(Mne, "ACCOUNT", "",loanaccountId));
            String arrId = ar.getArrangementId().getValue();
            LOGGER.info("arrId" + arrId);
            AaArrangementRecord arr = new AaArrangementRecord(da.getRecord(Mne, "AA.ARRANGEMENT", "" , arrId));
            String customerId = arr.getCustomer().get(0).getCustomer().getValue();
            LOGGER.info("customerId" + customerId);
            CustomerRecord cus = new CustomerRecord(da.getRecord("CUSTOMER", customerId));
            String mobileNo = cus.getPhone1().get(0).getSms1().getValue();
            LOGGER.info("mobileNo" + mobileNo);
            String tempType = "RECEIPT_CANCEL";
            String smsReqCreate = "";
            
            int cusId = 0;
            try {
                cusId = Integer.parseInt(customerId);
            } catch (Exception e1) {

            }
            genParam = new EbFfGenericParamSmsRecord(da.getRecord("EB.FF.GENERIC.PARAM.SMS", "FF.SMS.URL"));
            
            String userId = "USERID";
            String userName = "USERNAME";
            
            userId = processParam(userId, genParam);
            userName = processParam(userName, genParam);
            
            FfMessageRequestApi requestApi = new FfMessageRequestApi();
            requestApi.setTemplateType(tempType);
            requestApi.setMobileNumber(mobileNo);
            requestApi.setLoanAccountNumber(loanaccountId);
            requestApi.setCustomerId(cusId);
            requestApi.setUserId(userId);
            requestApi.setUserName(userName);

            Params params = new Params();
            params.setArg1(debitAmount);
            params.setArg2(arrId);
            params.setArg3(processingDate);

            requestApi.setParams(params);

            ObjectWriter ow = (new ObjectMapper()).writer().withDefaultPrettyPrinter();
            try {
                smsReqCreate = ow.writeValueAsString(requestApi);
            } catch (Exception e) {
                
            }
            
            //String apiUrl = genParam.getUrl().getValue();
            
            if(tempStatus.equals("") || tempStatus.isEmpty()) {
            //SendSmsUtil.SendSms(smsReqCreate, apiUrl, logRec, description, apiStatus);
            //SendSms(smsReqCreate, apiUrl, logRec, description, apiStatus);
                LocalDate todayDate = LocalDate.now();
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
                String todayLocalDate =todayDate.format(formatter);
                logRec.getStatus().setValue("REVERSE");
                logRec.getRequest().setValue(smsReqCreate);
                logRec.getResponse().setValue(description);
                logRec.getReserved3().setValue(Mne);
                logRec.getDateTriggered().setValue(todayLocalDate);
                logRec.getRetryCount().setValue("0");
            }

            try {
                logTab.write(currentRecordId, logRec);
            } catch (Exception e) {
               
            }
        }
        
    }catch(Exception e) {
        
    }
    }
    
    private static String processParam(String payMethod, EbFfGenericParamSmsRecord genParam) {
        String tempType = "";
        for (KeyNameClass temp : genParam.getKeyName()) {
            if (temp.getKeyName().getValue().equals(payMethod)) {
                tempType = temp.getKeyValue().getValue();
                logger.info("tempType" + tempType);
                break;
            }
            
        }
        
        return  tempType;
    }
}


