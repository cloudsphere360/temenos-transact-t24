package com.temenos.fusion;

import com.temenos.api.TDate;
import com.temenos.api.TStructure;
import com.temenos.t24.api.arrangement.accounting.Contract;
import com.temenos.t24.api.complex.aa.activityhook.ArrangementContext;
import com.temenos.t24.api.complex.aa.activityhook.FieldPair;
import com.temenos.t24.api.complex.aa.activityhook.Property;
import com.temenos.t24.api.complex.aa.activityhook.SecondaryActivity;
import com.temenos.t24.api.hook.arrangement.ActivityLifecycle;
import com.temenos.t24.api.records.aaaccountdetails.AaAccountDetailsRecord;
import com.temenos.t24.api.records.aaactivityhistory.AaActivityHistoryRecord;
import com.temenos.t24.api.records.aaactivityhistory.ActivityRefClass;
import com.temenos.t24.api.records.aaactivityhistory.EffectiveDateClass;
import com.temenos.t24.api.records.aaarrangement.AaArrangementRecord;
import com.temenos.t24.api.records.aaarrangementactivity.AaArrangementActivityRecord;
import com.temenos.t24.api.records.aaarrtermamount.AaArrTermAmountRecord;
import com.temenos.t24.api.records.aaprddesaccount.AaPrdDesAccountRecord;
import com.temenos.t24.api.records.aaproductcatalog.AaProductCatalogRecord;
import com.temenos.t24.api.records.dates.DatesRecord;
import com.temenos.t24.api.records.ebffgenericparameter.EbFfGenericParameterRecord;
import com.temenos.t24.api.records.ebffgenericparameter.KeyNameClass;
import com.temenos.t24.api.system.DataAccess;
import com.temenos.t24.api.system.Session;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class VerAaWrtOffSecUpdateStatus extends ActivityLifecycle {
   private static final FusionFileLogger verLogger = FusionFileLogger.getLogger(VerAaWrtOffSecUpdateStatus.class);
   VerAaPosWrtOffUpdate verAaPosWrtOffUpdateObj = new VerAaPosWrtOffUpdate();
   String compMne = "";
   String ffLoanSts="";

   public void generateSecondaryActivity(AaAccountDetailsRecord accountDetailRecord, AaArrangementActivityRecord arrangementActivityRecord, ArrangementContext arrangementContext, AaArrangementRecord arrangementRecord, AaArrangementActivityRecord masterActivityRecord, TStructure productPropertyRecord, AaProductCatalogRecord productRecord, TStructure aaRecord, SecondaryActivity secondaryActivity) {
      String activtyStatus = arrangementContext.getActivityStatus();
      if (activtyStatus.equals("UNAUTH")) {
         DataAccess da = new DataAccess(this);
         Session ssObj = new Session(this);
         this.compMne = ssObj.getCompanyRecord().getFinancialMne().getValue();
         Contract contractObj = new Contract(this);
         String aaArrangementId = arrangementContext.getArrangementId();
         
         String status = this.getStatus(da, ssObj, accountDetailRecord, arrangementActivityRecord.getActivity().getValue(), aaArrangementId, contractObj);
         verLogger.info("status " + status+" ffLoanSts "+ffLoanSts);
         if(!ffLoanSts.equals(status)) {
         if (!status.equals("")) {
            Contract contract = new Contract(this);
            Property accProp = new Property();
            accProp.setPropertyName("ACCOUNT");
            FieldPair fieldPair = new FieldPair();
            fieldPair.setFieldName("FF.LOAN.STATUS");
            if (status.equals("BLANK")) {
               fieldPair.setFieldValue("NULL");
            } else {
               fieldPair.setFieldValue(status);
            }

            accProp.setFieldPairs(fieldPair, 0);
            String contractId = arrangementActivityRecord.getArrangement().toString();
            String aaArrangementActivityId = arrangementContext.getArrangementActivityId();
            String effDate = arrangementContext.getActivityEffectiveDate();
            TDate yEffDate = new TDate(effDate);
            contract.setContractId(contractId);
            secondaryActivity.setArrangementEffectivedate(yEffDate);
            secondaryActivity.setArrangementId(aaArrangementId);
            secondaryActivity.setArrangementActivityId(aaArrangementActivityId);
            String secondaryActivityName = "LENDING-UPDATE-ACCOUNT";
            secondaryActivity.setNewActivity(secondaryActivityName);
            secondaryActivity.setProperties(accProp, 0);
            verLogger.info("secondaryActivity " + secondaryActivity);
         }
      }
      }

   }

   public String getStatus(DataAccess da, Session ssObj, AaAccountDetailsRecord accountDetailRecord, String activity, String arrangement, Contract contractObj) {
      AaActivityHistoryRecord aaActivityHistoryRecord = new AaActivityHistoryRecord(da.getRecord("AA.ACTIVITY.HISTORY", arrangement));
      verLogger.info(activity);
      verLogger.info("aaActivityHistoryRecord " + aaActivityHistoryRecord);
      contractObj.setContractId(arrangement);
      DatesRecord dateObj = new DatesRecord(da.getRecord("DATES", ssObj.getCompanyId()));
      AaPrdDesAccountRecord aaAccObj = new AaPrdDesAccountRecord(contractObj.getConditionForProperty("ACCOUNT"));
       ffLoanSts = aaAccObj.getLocalRefField("FF.LOAN.STATUS").getValue();
      verLogger.info("ffLoanSts " + ffLoanSts);
      String today = dateObj.getToday().getValue();
      if (activity.equals("LENDING-WRITE.OFF-BAL.MAINTAIN") && this.checkSettleForeClosureActvty(aaActivityHistoryRecord, "LENDING-SETTLE-FORECLOSURE") && !this.checkSettleForeClosureActvty(aaActivityHistoryRecord, "LENDING-CHARGEOFF-ARRANGEMENT")) {
         verLogger.info(" today " + today);
         DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
         LocalDate d1 = LocalDate.parse(today, formatter);
         verLogger.info("d1 " + d1);
         activity = this.getForeClosureCheck(arrangement, d1, this.compMne, da, activity);
      }

      if (activity.equals("LENDING-SETTLE-FORECLOSURE") && this.getChargeOFf(aaActivityHistoryRecord)) {
         activity = "WRITE.CLOSE";
      }

      if (activity.equals("LENDING-WRITE.OFF-BAL.MAINTAIN") && (ffLoanSts.equals("AIC") || ffLoanSts.equals("DECEASED"))) {
         verLogger.info("true deceased");
         activity = "WRITE.DECEASED";
      }

      activity = this.verAaPosWrtOffUpdateObj.fetchStatus(da, activity, arrangement, this.compMne);
      verLogger.info("activity " + activity);
      if (activity.equals("BLANK")) {
         return "BLANK";
      } else {
         EbFfGenericParameterRecord ebFfGenericParameterRecord = new EbFfGenericParameterRecord(da.getRecord("EB.FF.GENERIC.PARAMETER", "FF.LOAN.STATUS"));

         for(KeyNameClass keyNameClass : ebFfGenericParameterRecord.getKeyName()) {
            if (keyNameClass.getKeyName().getValue().equals(activity)) {
               return keyNameClass.getKeyValue().getValue();
            }
         }

         return "";
      }
   }

   private String getForeClosureCheck(String arrangement, LocalDate d1, String compMne, DataAccess da, String activity) {
      try {
         List<String> termRecList = da.selectRecords(compMne, "AA.ARR.TERM.AMOUNT", "", " WITH @ID LIKE " + arrangement + "... AND ACTIVITY EQ LENDING-NEW-ARRANGEMENT");
         if (termRecList.isEmpty()) {
            termRecList = da.selectRecords(compMne, "AA.ARR.TERM.AMOUNT", "", " WITH @ID LIKE " + arrangement + "... AND ACTIVITY EQ LENDING-TAKEOVER-ARRANGEMENT");
         }

         verLogger.info("termRecList " + termRecList);
         if (!termRecList.isEmpty()) {
            AaArrTermAmountRecord arrTermAmountObj = new AaArrTermAmountRecord(da.getRecord(compMne, "AA.ARR.TERM.AMOUNT", "", (String)termRecList.get(0)));
            String matDate = arrTermAmountObj.getMaturityDate().getValue();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
            LocalDate d2 = LocalDate.parse(matDate, formatter);
            verLogger.info("d1 and d2 " + d1 + " " + d2);
            if (d1.isBefore(d2)) {
               activity = "FORECLOSURE";
            } else if (d1.isAfter(d2) || d1.equals(d2)) {
               activity = "CLOSURE";
            }
         }
      } catch (Exception e) {
         verLogger.info(e.getMessage());
      }

      return activity;
   }

   public Boolean checkSettleForeClosureActvty(AaActivityHistoryRecord aaActivityHistoryRecord, String activity) {
      try {
         verLogger.info("Inside methid of chkforclosure");

         for(EffectiveDateClass effectiveDateClass : aaActivityHistoryRecord.getEffectiveDate()) {
            for(ActivityRefClass activityRefClass : effectiveDateClass.getActivityRef()) {
               verLogger.info("activityRefClass.getActivity().getValue() " + activityRefClass.getActivity().getValue());
               if (activityRefClass.getActivity().getValue().equals(activity)) {
                  verLogger.info("true");
                  return true;
               }
            }
         }
      } catch (Exception e) {
         verLogger.info(e.getMessage());
      }

      return false;
   }

   private boolean getChargeOFf(AaActivityHistoryRecord aaActivityHistoryRecord) {
      for(EffectiveDateClass effectiveDateClass : aaActivityHistoryRecord.getEffectiveDate()) {
         for(ActivityRefClass activityRefClass : effectiveDateClass.getActivityRef()) {
            if (activityRefClass.getActivity().getValue().equals("LENDING-CHARGEOFF-ARRANGEMENT")) {
               return activityRefClass.getActStatus().getValue().equalsIgnoreCase("AUTH");
            }
         }
      }

      return false;
   }
}