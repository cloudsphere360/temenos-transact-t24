package com.tem.msg.fusion;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.temenos.api.TStructure;
import com.temenos.fusion.FusionFileLogger;
import com.temenos.t24.api.arrangement.accounting.Contract;
import com.temenos.t24.api.complex.eb.servicehook.ServiceData;
import com.temenos.t24.api.complex.eb.servicehook.SynchronousTransactionData;
import com.temenos.t24.api.complex.eb.servicehook.TransactionControl;
import com.temenos.t24.api.hook.system.ServiceLifecycle;
import com.temenos.t24.api.records.aaarraccount.AaArrAccountRecord;
import com.temenos.t24.api.records.aaarrangement.AaArrangementRecord;
import com.temenos.t24.api.records.aaprddesaccount.AaPrdDesAccountRecord;
import com.temenos.t24.api.records.customer.CustomerRecord;
import com.temenos.t24.api.records.ebffcentredetail.EbFfCentreDetailRecord;
import com.temenos.t24.api.records.ebffgenericparamsms.EbFfGenericParamSmsRecord;
import com.temenos.t24.api.records.ebffgenericparamsms.KeyNameClass;
import com.temenos.t24.api.records.ebffrochange.EbFfRoChangeRecord;
import com.temenos.t24.api.records.ebffrouser.EbFfRoUserRecord;
import com.temenos.t24.api.records.ebfusionlogsmsupdate.EbFusionLogSmsUpdateRecord;
import com.temenos.t24.api.system.DataAccess;
import com.temenos.t24.api.system.Session;
import com.temenos.t24.api.tables.ebffrochange.EbFfRoChangeTable;
import com.temenos.t24.api.tables.ebfusionlogsmsupdate.EbFusionLogSmsUpdateTable;

/**
 * TODO: Document me!
 *
 * @author lm116299
 *
 */
public class RoProcess extends ServiceLifecycle {
    private static final FusionFileLogger logger = FusionFileLogger.getLogger(RoProcess.class);
    Session sn = new Session(this);
    DataAccess da = new DataAccess(this);
    Contract api = new Contract(this);
    String Mne = sn.getCompanyRecord().getFinancialMne().toString();
    EbFusionLogSmsUpdateRecord logRec = new EbFusionLogSmsUpdateRecord(this);
    EbFfGenericParamSmsRecord genParam = null;
    String apiStatus = "";
    String description = "";
    EbFusionLogSmsUpdateTable logTab = new EbFusionLogSmsUpdateTable(this);
    EbFfRoChangeTable roTab = new EbFfRoChangeTable(this);
    EbFfRoChangeRecord roRec = new EbFfRoChangeRecord(this);

    @Override
    public List<String> getIds(ServiceData serviceData, List<String> controlList) {
        List<String> etdList = new ArrayList<>();
        List<String> ArrList = new ArrayList<>();
        List<String> TempAccList = new ArrayList<>();
        List<String> orgArrList = new ArrayList<>();
        List<String> updList = new ArrayList<>();
        try {
            etdList = da.selectRecords("", "EB.FF.RO.CHANGE", "", "WITH STATUS EQ ROCHANGE AND MNE EQ " + Mne);
            logger.info("etdList" + etdList);

            for (String etd : etdList) {

                ArrList = da.selectRecords(Mne, "AA.ARR.ACCOUNT", "", "WITH FF.CENTRE EQ " + etd);
                logger.info("ArrList" + ArrList);
                if (ArrList.isEmpty()) {
                    roRec.getStatus().setValue("ROCHANGE_PROCESSED");
                    roRec.setMne(Mne);
                    roTab.write(etd, roRec);
                }
                logger.info("ArrList" + ArrList);
                for (int i = 0; i < ArrList.size(); i++) {
                    String id = ArrList.get(i).toString();
                    logger.info("id" + id);
                    AaArrAccountRecord accrec = new AaArrAccountRecord(da.getRecord(Mne, "AA.ARR.ACCOUNT", "", id));
                    String idComp = accrec.getIdComp1().getValue();
                    logger.info("idComp" + idComp);
                    TempAccList.add(idComp);
                    logger.info("etd" + etd);
                }
                orgArrList = TempAccList.stream().distinct().collect(Collectors.toList());
                TempAccList.clear();
                for (int i = 0; i < orgArrList.size(); i++) {                
                    logger.info("orgArrList"+orgArrList.get(i).toString());
                    
                    api.setContractId(orgArrList.get(i));
                    
                    AaPrdDesAccountRecord aaree = new AaPrdDesAccountRecord(api.getConditionForProperty("ACCOUNT"));
                   if( aaree.getLocalRefField("FF.CENTRE").getValue().equalsIgnoreCase(etd)){
                       updList.add(orgArrList.get(i)+ "*" + etd);
                    }   
                }
                break;
            }

        } catch (Exception e210) {
            logger.info("e210" + e210);
        }

        return updList;

    }

    @Override
    public void updateRecord(String id, ServiceData serviceData, String controlItem,
            TransactionControl transactionControl, List<SynchronousTransactionData> transactionData,
            List<TStructure> records) {
        try {
            String[] part = id.split("\\*");
            String RoId = part[1];
            String AaId = part[0];
            AaArrangementRecord arr = new AaArrangementRecord(da.getRecord(Mne, "AA.ARRANGEMENT", "", AaId));
            String LoanAcct = arr.getLinkedAppl().get(0).getLinkedApplId().getValue();
            String customerId = arr.getCustomer().get(0).getCustomer().getValue();
            logger.info("customerId" + customerId);
            CustomerRecord cus = new CustomerRecord(da.getRecord("CUSTOMER", customerId));
            String cusSname = cus.getShortName().get(0).getValue();
            String cusName = cus.getName1().get(0).getValue();
            String fullName = cusSname.concat(" ").concat(cusName);
            logger.info("fullName" + fullName);
            String mobileNo = "";
            EbFfCentreDetailRecord roName = new EbFfCentreDetailRecord(da.getRecord("EB.FF.CENTRE.DETAIL", RoId));
            String currentRoName = roName.getCurrentRo().getValue();
            EbFfRoUserRecord roUser = new EbFfRoUserRecord(da.getRecord("EB.FF.RO.USER", currentRoName));
            String curPhoneNum = roUser.getRoMobileNumber().getValue();
            String tempType = "RO_CHANGE";
            String smsReqCreate = "";
            String userId = "USERID";
            String userName = "USERNAME";

            int cusId = 0;
            try {
                cusId = Integer.parseInt(customerId);
            } catch (Exception e1) {
                logger.info("e1" + e1);
            }

            try {
                genParam = new EbFfGenericParamSmsRecord(da.getRecord("EB.FF.GENERIC.PARAM.SMS", "FF.SMS.URL"));

                userId = processParam(userId, genParam);
                userName = processParam(userName, genParam);
            } catch (Exception e200) {
                logger.info("e200" + e200);
            }

            try {
                mobileNo = cus.getPhone1().get(0).getSms1().getValue();
                logger.info("mobileNo" + mobileNo);

            } catch (Exception e12) {
                logger.info("e12" + e12);
            }

            FfMessageRequestApi requestApi = new FfMessageRequestApi();
            Params params = new Params();
            requestApi.setTemplateType(tempType);
            requestApi.setMobileNumber(mobileNo);
            requestApi.setLoanAccountNumber(LoanAcct);
            requestApi.setCustomerId(cusId);
            requestApi.setUserId(userId);
            requestApi.setUserName(userName);

            params.setArg1(RoId);
            params.setArg2(currentRoName);
            params.setArg3(curPhoneNum);

            requestApi.setParams(params);

            try {
                ObjectWriter ow = (new ObjectMapper()).writer().withDefaultPrettyPrinter();
                smsReqCreate = ow.writeValueAsString(requestApi);
                logger.info("smsReqCreate" + smsReqCreate);
            } catch (Exception e4) {
                logger.info("e4" + e4);
            }

            try {
                String apiUrl = genParam.getUrl().getValue();
                logger.info("apiUrl" + apiUrl);
                SendSmsUtil.SendSms(smsReqCreate, apiUrl, logRec, description, apiStatus);
            } catch (Exception e122) {
                logger.info("e122" + e122);
            }

            try {
                logger.info("logRec" + logRec.toString());
                logTab.write(AaId + "-" + "Ro", logRec);
                roRec.getStatus().setValue("ROCHANGE_PROCESSED");
                roRec.setMne(Mne);
                roTab.write(RoId, roRec);
            } catch (Exception e3) {
                logger.info("e3" + e3);
            }

        } catch (Exception e39) {
            logger.info("e39" + e39);
            e39.printStackTrace();
            logger.info("Root cause: " + e39.getCause());

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

        return tempType;
    }

}
