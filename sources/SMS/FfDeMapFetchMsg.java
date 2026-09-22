package com.tem.msg.fusion;


import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.temenos.api.TString;
import com.temenos.fusion.FusionFileLogger;
import com.temenos.t24.api.complex.de.deliveryhook.DeliveryDetail;
import com.temenos.t24.api.hook.system.Delivery;
import com.temenos.t24.api.records.ebffgenericparamsms.EbFfGenericParamSmsRecord;
import com.temenos.t24.api.records.ebffgenericparamsms.KeyNameClass;
import com.temenos.t24.api.records.ebfusionlogsmsupdate.EbFusionLogSmsUpdateRecord;
import com.temenos.t24.api.system.DataAccess;
import com.temenos.t24.api.system.Session;
import com.temenos.t24.api.tables.ebfusionlogsmsupdate.EbFusionLogSmsUpdateTable;


public class FfDeMapFetchMsg extends Delivery {

    private static final FusionFileLogger logger = FusionFileLogger.getLogger(FfDeMapFetchMsg.class);

    EbFusionLogSmsUpdateRecord logRec = new EbFusionLogSmsUpdateRecord(this);
    EbFusionLogSmsUpdateTable logTab = new EbFusionLogSmsUpdateTable(this);
    String smsId = "";
    String apiUrl = "";
    String description = "";
    String apiStatus = "";
    DataAccess da = new DataAccess(this);
    EbFfGenericParamSmsRecord genParam = null;
    

    @Override
    public void processOutwardMessage(String uniqueId, DeliveryDetail deliveryData, TString carrierMessage,
            TString errorResponse) {
        Session sn = new Session(this);
        logger.info("DE INTERFACE MSG RTN TRIGG");
        logger.info("carrierMessage" + carrierMessage);

        String carrMessage = carrierMessage.get();
        logger.info("carrMessage: " + carrMessage);
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setIgnoringElementContentWhitespace(true);

            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(new ByteArrayInputStream(carrMessage.getBytes("UTF-8")));
            
            String actName = "";

            String message = document.getElementsByTagName("message").item(0).getTextContent();
            String repayAmount = extractValue(message, "Repay Amount");
            String loanAccount = extractValue(message, "Loan Account");
            String txnId = extractValue(message, "Txn Id");
            String payMethod = extractValue(message, "Pay Method");
            String customerNo = extractValue(message, "Customer No");
            String arrangementId = extractValue(message, "Arrangement Id");
            String customerName = extractValue(message, "Customer Name");
            String receiptNo = extractValue(message, "Receipt No");
            String effDate = extractValue(message, "Effective Date");
            String dueDate = extractValue(message, "DueDate");
            String dueAmt = extractValue(message, "DueAmt");
             actName = extractValue(message, "Activity Name");
            String status = extractValue(message, "Status");
            String urlLink = extractValue(message, "urlLink");
            String inputter = extractValue(message, "inputter");
            String company = extractValue(message, "company");
            logger.info("repayAmount" + repayAmount);
            logger.info("loanAccount" + loanAccount);
            logger.info("txnId" + txnId);
            logger.info("payMethod" + payMethod);
            logger.info("customerNo" + customerNo);
            logger.info("arrangementId" + arrangementId);
            logger.info("customerName" + customerName);
            logger.info("receiptNo" + receiptNo);
            logger.info("dueDate" + dueDate);
            logger.info("effDate" + effDate);
            logger.info("dueAmt" + dueAmt);
            logger.info("actName" + actName);
            logger.info("Status" + status);
            logger.info("UrlLink" + urlLink);
            logger.info("inputter" + inputter);
            logger.info("company" + company);
                
            NodeList receiverList = document.getElementsByTagName("receiver");

            Element smsElement = (Element) document.getElementsByTagName("sms").item(0);
            smsId = smsElement.getAttribute("id");
            logger.info("smsId" + smsId);
            if (receiverList != null && receiverList.getLength() > 0) {
                String receiver = receiverList.item(0).getTextContent();
                logger.info("Receiver Number : " + receiver);

                try {
                    genParam = new EbFfGenericParamSmsRecord(da.getRecord("EB.FF.GENERIC.PARAM.SMS", "FF.SMS.URL"));
                    apiUrl = genParam.getUrl().getValue();
                } catch (Exception e) {
                    logRec.getStatus().setValue("FAILED");
                    logRec.getResponse().setValue("SMS URL Missing or incorrect");
                }

                SendSmsRequest(receiver, payMethod, apiUrl, loanAccount, customerNo, arrangementId, genParam, logRec,
                        description, apiStatus, repayAmount, receiptNo, effDate, dueDate, dueAmt ,status ,actName,urlLink,sn, inputter, company);

            } else {
                logRec.getStatus().setValue("FAILED");
                logRec.getResponse().setValue("Receiver Tag missing in carrierMessage");
            }
        } catch (Exception e) {
            logger.info("exception56: " + e);
        }

        try {
            logTab.write(smsId, logRec);
        } catch (Exception e) {
            logger.info("exception75: " + e);
        }
    }

    private static String extractValue(String message, String key) {
        if (message == null || key == null) {
            return "";
        }
        Matcher m = Pattern.compile(key + ":\\s*([^,]+)").matcher(message);
        return m.find() ? m.group(1).trim() : null;
    }

    private static void SendSmsRequest(String receiver, String payMethod, String apiUrl, String loanAccount,
            String customerNo, String arrangementId, EbFfGenericParamSmsRecord genParam, EbFusionLogSmsUpdateRecord logRec,
            String description, String apiStatus, String repayAmount, String receiptNo, String effDate, String dueDate, String dueAmt ,String status,String actName,String urlLink, Session sn ,String inputter, String company) {
        String mne = sn.getCompanyRecord().getFinancialMne().toString();
        String smsReqCreate = "";
        String tempType = "";
        String susType = "SUSPEND";
        
        String userId = "USERID";
        String userName = "USERNAME";
        
        
        
        if(payMethod == null || payMethod.isBlank()) {
            payMethod = actName;
        }
        
        
        tempType =  processParam(payMethod, genParam);
        userId = processParam(userId, genParam);
        userName = processParam(userName, genParam);

        int cusId = 0;
        try {
            cusId = Integer.parseInt(customerNo);
        } catch (Exception e1) {
            logger.info("e1" + e1);
        }
        logger.info("cusId" + cusId);

        FfMessageRequestApi requestApi = new FfMessageRequestApi();
        requestApi.setTemplateType(tempType);
        requestApi.setMobileNumber(receiver);
        requestApi.setLoanAccountNumber(loanAccount);
        requestApi.setCustomerId(cusId);
        requestApi.setUserId(userId);
        requestApi.setUserName(userName);

        Params params = new Params();

        if (tempType.equalsIgnoreCase("COLLECTION_POSTING")) {
            params.setArg1(repayAmount);
            params.setArg2(arrangementId);
            params.setArg3(effDate);
            params.setArg4(receiptNo);
            params.setArg5(urlLink);
        }else if (tempType.equalsIgnoreCase("EMI_NOT_RECEIVED")) {
            params.setArg1(arrangementId);
            params.setArg2(dueAmt);
            params.setArg3(dueDate);
        } else if (tempType.equalsIgnoreCase("LOAN_CLOSED")) {
            params.setArg1(arrangementId);       
        }else if (tempType.equalsIgnoreCase("LOAN_FORECLOSURE")) {
            params.setArg1(arrangementId);       
        }else if (tempType.equalsIgnoreCase("LOAN_MARKED_NPA")) {
            params.setArg1(arrangementId);
            params.setArg2(effDate);
            params.setArg3(company);
        }

        requestApi.setParams(params);

        ObjectWriter ow = (new ObjectMapper()).writer().withDefaultPrettyPrinter();
        try {
            smsReqCreate = ow.writeValueAsString(requestApi);
        } catch (Exception e) {
            logger.info("e" + e);
        }
        logger.info("smsReqCreate: " + smsReqCreate);
            
        if(tempType.equalsIgnoreCase("EMI_NOT_RECEIVED")) {
            
            try {
            logRec.getStatus().setValue("PENDING");
            logRec.getRequest().setValue(smsReqCreate);
            DateTimeFormatter dformatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
            DateTimeFormatter yformatter = DateTimeFormatter.ofPattern("yyyyMMdd");
            LocalDate today = LocalDate.parse(dueDate, dformatter);
            LocalDate afterOneDay = today.plusDays(1);
            logRec.getDateTriggered().setValue(afterOneDay.format(yformatter));
            logRec.getRetryCount().setValue("0");    
            logRec.getReserved3().setValue(mne);
            }catch(Exception e) {
                logger.info("e90: " + e);
            }
            
        }
            
          else if (tempType.equalsIgnoreCase("COLLECTION_POSTING")) {
                try {
                    boolean receiptMissing = (receiptNo == null || receiptNo.isBlank());
                    if (receiptMissing) {
                        logRec.getStatus().setValue(susType);
                        logRec.getRetryCount().setValue("0");
                        logRec.getReserved3().setValue(mne);
                        logRec.getRequest().setValue(smsReqCreate);
                    }else {
                        SendSmsUtil.SendSms(smsReqCreate, apiUrl, logRec, description, apiStatus);
                    }
                } catch (Exception e) {
                    logger.info("e900: " + e);
                }
                
          }
                
                else if(tempType.equalsIgnoreCase("LOAN_CLOSED")) {
                    
                    try {
                    if(!status.equals("PENDING.CLOSURE") || inputter.contains("NETOFF")) {  
                        logRec.getStatus().setValue(susType);
                        logRec.getRetryCount().setValue("0");
                        logRec.getReserved3().setValue(mne);
                        logRec.getRequest().setValue(smsReqCreate);
                    }else {
                        SendSmsUtil.SendSms(smsReqCreate, apiUrl, logRec, description, apiStatus);
                    }
                    }
                    catch(Exception e) {
                        logger.info("e9: " + e);
                    }
                    
                }
        
                    else if(tempType.equalsIgnoreCase("LOAN_FORECLOSURE")) {
                                        
                                        try {
                                        if(inputter.contains("NETOFF")) {  
                                            logRec.getStatus().setValue(susType);
                                            logRec.getRetryCount().setValue("0");
                                            logRec.getReserved3().setValue(mne);
                                            logRec.getRequest().setValue(smsReqCreate);
                                        }else {
                                            SendSmsUtil.SendSms(smsReqCreate, apiUrl, logRec, description, apiStatus);
                                        }
                                        }
                                        catch(Exception e) {
                                            logger.info("e99: " + e);
                                        }
                                        
                                    }
            
         else {
            SendSmsUtil.SendSms(smsReqCreate, apiUrl, logRec, description, apiStatus);
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
