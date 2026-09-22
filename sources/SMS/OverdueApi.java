package com.tem.msg.fusion;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.temenos.api.TStructure;
import com.temenos.fusion.FusionFileLogger;
import com.temenos.t24.api.arrangement.accounting.Contract;
import com.temenos.t24.api.complex.eb.servicehook.ServiceData;
import com.temenos.t24.api.complex.eb.servicehook.SynchronousTransactionData;
import com.temenos.t24.api.complex.eb.servicehook.TransactionControl;
import com.temenos.t24.api.hook.system.ServiceLifecycle;
import com.temenos.t24.api.records.aaaccountdetails.AaAccountDetailsRecord;
import com.temenos.t24.api.records.aaaccountdetails.BillIdClass;
import com.temenos.t24.api.records.aaaccountdetails.BillPayDateClass;
import com.temenos.t24.api.records.aabilldetails.AaBillDetailsRecord;
import com.temenos.t24.api.records.account.AccountRecord;
import com.temenos.t24.api.records.ebffgenericparamsms.EbFfGenericParamSmsRecord;
import com.temenos.t24.api.records.ebffgenericparamsms.KeyNameClass;
import com.temenos.t24.api.records.ebfusionlogsmsupdate.EbFusionLogSmsUpdateRecord;
import com.temenos.t24.api.system.DataAccess;
import com.temenos.t24.api.system.Session;
import com.temenos.t24.api.tables.ebfusionlogsmsupdate.EbFusionLogSmsUpdateTable;


/**
 * TODO: Document me!
 *
 * @author lm116299
 *
 */
public class OverdueApi extends ServiceLifecycle{
    
    private static final FusionFileLogger LOGGER = FusionFileLogger.getLogger(OverdueApi.class);
    DataAccess da = new DataAccess(this);
    Session sn = new Session(this);
    EbFfGenericParamSmsRecord genParam = null;
    Contract api = new Contract(this);
    String Mne = sn.getCompanyRecord().getFinancialMne().toString();
    EbFusionLogSmsUpdateTable logTab = new EbFusionLogSmsUpdateTable(this);
    String apiUrl = "";
    String description = "";
    String apiStatus = "";
    
    @Override
    public List<String> getIds(ServiceData serviceData, List<String> controlList) {
        // TODO Auto-generated method stub
        int tempType = 0;
        List<String> updSelectionList = new ArrayList<>();
        try {
        LocalDate todayDate = LocalDate.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
        String todayLocalDate =todayDate.format(formatter);
        LOGGER.info("todayLocalDate: " + todayLocalDate);
        List<String> selectionList = da.selectRecords("", "EB.FUSION.LOG.SMS.UPDATE", "", "WITH STATUS EQ PENDING OR STATUS EQ FAILED OR STATUS EQ REVERSE AND RESERVED.3 EQ "+Mne);
        LOGGER.info("selectionList: " + selectionList.toString());
        for(int i = 0; i<selectionList.size(); i++) {
            String iD = selectionList.get(i) ;
            EbFusionLogSmsUpdateRecord loopoff = new EbFusionLogSmsUpdateRecord(da.getRecord("EB.FUSION.LOG.SMS.UPDATE", iD));
            String retryCount= loopoff.getRetryCount().getValue();
            String dateTriggeredStr = loopoff.getDateTriggered().getValue();
            LocalDate triggeredDate = LocalDate.parse(dateTriggeredStr, formatter);
            Integer count = Integer.valueOf(retryCount);
            genParam = new EbFfGenericParamSmsRecord(da.getRecord("EB.FF.GENERIC.PARAM.SMS", "FF.SMS.URL"));
            for (KeyNameClass temp : genParam.getKeyName()) {
                if (temp.getKeyName().getValue().equals("RETRY_COUNT")) {
                    tempType = Integer.valueOf(temp.getKeyValue().getValue());
                    LOGGER.info("tempType: " + tempType);
                    break;
                }
            }
            
            
            
            if( count<=tempType && !triggeredDate.isAfter(todayDate)) {
                updSelectionList.add(iD);
            }
        }
        LOGGER.info("updSelectionList: " + updSelectionList.toString());
                  
    }catch(Exception e) {
        LOGGER.info("exception85: " + e);
    }
        return updSelectionList;
    }

    @Override
    public void updateRecord(String id, ServiceData serviceData, String controlItem,
            TransactionControl transactionControl, List<SynchronousTransactionData> transactionData,
            List<TStructure> records) {
        
        LOGGER.info("id: " + id);
        String templateType = "";
        String loanAccountNumber = "";
        
        EbFusionLogSmsUpdateRecord logRec = new EbFusionLogSmsUpdateRecord(da.getRecord("EB.FUSION.LOG.SMS.UPDATE", id));
       String smsReqCreate = logRec.getRequest().getValue();
       String status = logRec.getStatus().getValue();
       genParam = new EbFfGenericParamSmsRecord(da.getRecord("EB.FF.GENERIC.PARAM.SMS", "FF.SMS.URL"));
       String apiUrl = genParam.getUrl().getValue();
       
       if(status.equals("PENDING")) {
           ObjectMapper mapper = new ObjectMapper();
           
        try {
            JsonNode root;
            root = mapper.readTree(smsReqCreate);
            templateType      = root.path("templateType").asText(null);
            loanAccountNumber = root.path("loanAccountNumber").asText(null);
            String mobileNumber      = root.path("mobileNumber").asText(null);
            String customerId        = root.path("customerId").asText(null);

            JsonNode params = root.path("params");
            String arg1 = params.path("arg1").asText(null);
            String arg2 = params.path("arg2").asText(null); 
            String date = params.path("arg3").asText(null); 
            DateTimeFormatter yformatter = DateTimeFormatter.ofPattern("yyyyMMdd");
            DateTimeFormatter dformatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
            String dueDate = null;
            if (date != null && !date.isBlank()) {
                LocalDate d = LocalDate.parse(date.trim(),dformatter );
                dueDate = d.format(yformatter);
            }

            LOGGER.info("templateType: " + templateType);
            LOGGER.info("loanAccountNumber: " + loanAccountNumber);
            LOGGER.info("mobileNumber: " + mobileNumber);
            LOGGER.info("customerId: " + customerId);
            LOGGER.info("arg1: " + arg1);
            LOGGER.info("arg2: " + arg2);
            LOGGER.info("arg3 (original): " + date);
            LOGGER.info("arg3 (yyyyMMdd): " + dueDate);
            
            //AccountRecord ar = new AccountRecord(da.getRecord(Mne, "ACCOUNT", "", loanAccountNumber));
            LOGGER.info("MNE " + Mne);
            AccountRecord ar = new AccountRecord(da.getRecord(Mne, "ACCOUNT", "",loanAccountNumber));
            String arrId = ar.getArrangementId().getValue();
            List<String> billIds = new ArrayList<>();
            try {
                LOGGER.info("arrId "+loanAccountNumber);
                AaAccountDetailsRecord aad = new AaAccountDetailsRecord(da.getRecord(Mne, "AA.ACCOUNT.DETAILS", "", arrId));
                for(BillPayDateClass aabill : aad.getBillPayDate()) {
                 for(BillIdClass aabills : aabill.getBillId()) {
                     String bills = aabills.getBillId().getValue().replace("/", "");
                     billIds.add(bills);
                 }
                }
                
                LOGGER.info("billIds " + billIds.toString());
                String BillDate = "";
                String  Status = "";
                for(String billId : billIds) 
                {
                    AaBillDetailsRecord aab = new AaBillDetailsRecord(da.getRecord(Mne, "AA.BILL.DETAILS", "", billId));
                    BillDate = aab.getPaymentDate().getValue();
                    if(dueDate.equals(BillDate)){
                        Status = aab.getBillStatus().get(0).getBillStatus().getValue();
                        break;
                    }else{
                      continue;
                     }
                 }
                 if(Status.equals("SETTLED")) {
                 logRec.getStatus().setValue("SUSPEND");
                 logRec.getResponse().setValue("Bill is Already Settled");
                }else {
                  SendSms(smsReqCreate, apiUrl, logRec, description, apiStatus);
                 }
            } catch (Exception e12) {
                LOGGER.info("E12 " + e12);
            }
            
        } catch (Exception e204) {
            LOGGER.info("Exception"+ e204);
        } 

       }else {
           
           SendSms(smsReqCreate, apiUrl, logRec, description, apiStatus);
           
       }
       
       try {
           logTab.write(id, logRec);
       } catch (Exception e) {
           LOGGER.info("exception75: " + e);
       }
       
    }
    
    public static void SendSms(String smsReqCreate, String apiUrl, EbFusionLogSmsUpdateRecord logRec,
            String description, String apiStatus) {
        ObjectMapper mapper = new ObjectMapper();
        StringBuilder sb = new StringBuilder();
        String responseValue = "";
        LOGGER.info("apiUrl" + apiUrl);
        try {
            URL url = new URL(apiUrl);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("POST");
            con.setRequestProperty("content-type", "application/json");
            con.setRequestProperty("User-Agent", "Mozilla/5.0");
            con.setRequestProperty("Accept-Language", "UTF-8");
            con.setDoOutput(true);
            OutputStream os = con.getOutputStream();

            os.write(smsReqCreate.getBytes());

            os.flush();
            os.close();

            int responseCode = con.getResponseCode();
            LOGGER.info("responseCode" + responseCode);

            BufferedReader bfReader;
            if (responseCode != 200) {
                bfReader = new BufferedReader(new InputStreamReader(con.getErrorStream()));
            } else {
                bfReader = new BufferedReader(new InputStreamReader(con.getInputStream()));
            }

            String inputLine;
            while ((inputLine = bfReader.readLine()) != null) {
                sb.append(inputLine);
            }

            bfReader.close();
            responseValue = sb.toString();
            LOGGER.info("responseValue" + responseValue);

            try {
                JsonNode rootNode = mapper.readTree(responseValue);
                String status = rootNode.path("status").asText();

                if (!status.equals("")) {
                    FfMessageFailResponseApi apiFailResponse = new FfMessageFailResponseApi();
                    apiFailResponse = mapper.readValue(responseValue, FfMessageFailResponseApi.class);
                    description = apiFailResponse.getResponseMessage();
                    apiStatus = "FAILED";
                } else {

                    description = responseValue;
                    apiStatus = "SUCCESS";
                }
            } catch (Exception e) {
                description = responseValue;
                apiStatus = "SUCCESS";
            }
            
            String retry = logRec.getRetryCount().getValue();
            Integer value = Integer.valueOf(retry);
            value = value+1;
                    
            logRec.getStatus().setValue(apiStatus);
            logRec.getRequest().setValue(smsReqCreate);
            logRec.getResponse().setValue(description);
            logRec.getRetryCount().setValue(String.valueOf(value));

        } catch (Exception e) {
            LOGGER.info("e80: " + e);
        }

    }
    
}
