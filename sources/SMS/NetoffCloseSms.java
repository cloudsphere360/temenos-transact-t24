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
public class NetoffCloseSms extends RecordLifecycle {

    private static final FusionFileLogger LOGGER = FusionFileLogger.getLogger(NetoffCloseSms.class);
    Session sn = new Session(this);
    DataAccess da = new DataAccess(this);
    Contract api = new Contract(this);
    String Mne = sn.getCompanyRecord().getFinancialMne().toString();
    EbFusionLogSmsUpdateRecord logRec = new EbFusionLogSmsUpdateRecord(this);
    EbFfGenericParamSmsRecord genParam = null;
    String apiStatus = "";
    String description = "";
    String loanNetOffId = "";
    String Status = "";
    Integer cusId;
    String MobileNo = "";
    String LoanAcct = "";
    String customerId = "";
    String ProcessingDate = "";
    String processedDate  = "";
    LocalDate processDate ;
    String newAccId = "";
    String aaId = "";
    String closeAmount = "";
    String newAccAcct = "";
    EbFusionLogSmsUpdateTable logTab = new EbFusionLogSmsUpdateTable(this);
            
        @Override
    public void postUpdateRequest(String application, String currentRecordId, TStructure currentRecord,
            List<TransactionData> transactionData, List<TStructure> currentRecords,
            TransactionContext transactionContext) {
            
            FundsTransferRecord ftRec = new FundsTransferRecord(currentRecord);
            LOGGER.info("transactionContext" + transactionContext);
            
            
            if(ftRec.getTransactionType().getValue().equals("ACP2") && transactionContext.getCurrentFunction().equalsIgnoreCase("INPUT")) {
            
            try {
                
                try {
                    
                    ProcessingDate = ftRec.getProcessingDate().getValue();
                    LOGGER.info("ProcessingDate" + ProcessingDate);
                    processDate = LocalDate.parse(ProcessingDate, DateTimeFormatter.ofPattern("yyyyMMdd"));
                    processedDate = processDate.format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));
                    newAccId = ftRec.getLocalRefField("FF.NETOFF.LOAN").getValue();
                    LOGGER.info("newAccId" + newAccId);
                    closeAmount =ftRec.getCreditAmount().getValue();
                    LOGGER.info("closeAmount" + closeAmount);
                    loanNetOffId = ftRec.getLocalRefField("FF.NETOFF.CLOSE").getValue();
                    LOGGER.info("loanNetOffId" + loanNetOffId);
                    AaArrangementRecord newarrRec = new AaArrangementRecord(da.getRecord(Mne, "AA.ARRANGEMENT", "", newAccId));
                    newAccAcct = newarrRec.getLinkedAppl().get(0).getLinkedApplId().getValue();
                    LOGGER.info("newAccAcct" + newAccAcct);

                } catch (Exception e123) {
                    LOGGER.info("e123" + e123);
                }
               
                if (!loanNetOffId.equals(null) || !loanNetOffId.isEmpty()) {
                    AaArrangementRecord arrRec = new AaArrangementRecord(da.getRecord(Mne, "AA.ARRANGEMENT", "", loanNetOffId));
                    Status = arrRec.getArrStatus().getValue();
                    LOGGER.info("Status" + Status);
                     
                        LoanAcct = arrRec.getLinkedAppl().get(0).getLinkedApplId().getValue();
                        customerId = arrRec.getCustomer().get(0).getCustomer().getValue();
                        CustomerRecord cus = new CustomerRecord(da.getRecord("CUSTOMER", customerId));
                        try {
                            MobileNo = cus.getPhone1().get(0).getSms1().getValue();
                            LOGGER.info("mobileNo" + MobileNo);
                        } catch (Exception e12) {
                            LOGGER.info("e12" + e12);
                        }
                        String tempType = "NET_OFF";
                        String smsReqCreate = "";

                        int cusId = 0;
                        try {
                            cusId = Integer.parseInt(customerId);
                        } catch (Exception e1) {
                            LOGGER.info("e1" + e1);
                        }
                        
                        genParam = new EbFfGenericParamSmsRecord(da.getRecord("EB.FF.GENERIC.PARAM.SMS", "FF.SMS.URL"));

                        String userId = "USERID";
                        String userName = "USERNAME";
                        
                        userId = processParam(userId, genParam);
                        userName = processParam(userName, genParam);

                        
                        FfMessageRequestApi requestApi = new FfMessageRequestApi();
                        requestApi.setTemplateType(tempType);
                        requestApi.setMobileNumber(MobileNo);
                        requestApi.setLoanAccountNumber(LoanAcct);
                        requestApi.setCustomerId(cusId);
                        requestApi.setUserId(userId);
                        requestApi.setUserName(userName);

                        Params params = new Params();
                        params.setArg1(loanNetOffId);
                        params.setArg2(closeAmount);
                        params.setArg3(newAccId);
                        params.setArg4(processedDate);

                        requestApi.setParams(params);

                        ObjectWriter ow = (new ObjectMapper()).writer().withDefaultPrettyPrinter();
                        try {
                            smsReqCreate = ow.writeValueAsString(requestApi);
                        } catch (Exception e4) {
                            LOGGER.info("e4" + e4);
                        }
                       
                        String apiUrl = genParam.getUrl().getValue();

                        SendSmsUtil.SendSms(smsReqCreate, apiUrl, logRec, description, apiStatus);
                        // SendSms(smsReqCreate, apiUrl, logRec, description, apiStatus);

                        try {
                            logTab.write(LoanAcct + "-" + "NETOFF", logRec);
                        } catch (Exception e3) {
                            LOGGER.info("e3" + e3);
                        }
                    }
                    
                }catch(Exception e104) {
                    LOGGER.info("e104" + e104);
                }
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
