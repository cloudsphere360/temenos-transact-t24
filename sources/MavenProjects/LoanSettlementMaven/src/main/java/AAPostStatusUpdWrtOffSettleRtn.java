package com.temenos.fusion;

import com.temenos.api.LocalRefGroup;
import com.temenos.api.TDate;
import com.temenos.api.TStructure;
import com.temenos.t24.api.arrangement.accounting.Contract;
import com.temenos.t24.api.complex.aa.activityhook.ArrangementContext;
import com.temenos.t24.api.complex.aa.activityhook.FieldPair;
import com.temenos.t24.api.complex.aa.activityhook.Property;
import com.temenos.t24.api.complex.aa.activityhook.SecondaryActivity;
import com.temenos.t24.api.hook.arrangement.ActivityLifecycle;
import com.temenos.t24.api.records.aaaccountdetails.AaAccountDetailsRecord;
import com.temenos.t24.api.records.aaaccountdetails.BillIdClass;
import com.temenos.t24.api.records.aaaccountdetails.BillPayDateClass;
import com.temenos.t24.api.records.aaactivityhistory.AaActivityHistoryRecord;
import com.temenos.t24.api.records.aaactivityhistory.ActivityRefClass;
import com.temenos.t24.api.records.aaactivityhistory.EffectiveDateClass;
import com.temenos.t24.api.records.aaarrangement.AaArrangementRecord;
import com.temenos.t24.api.records.aaarrangementactivity.AaArrangementActivityRecord;
import com.temenos.t24.api.records.aabilldetails.AaBillDetailsRecord;
import com.temenos.t24.api.records.aaprddesaccount.AaPrdDesAccountRecord;
import com.temenos.t24.api.records.aaproductcatalog.AaProductCatalogRecord;
import com.temenos.t24.api.records.dates.DatesRecord;
import com.temenos.t24.api.system.DataAccess;
import com.temenos.t24.api.system.Session;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class AAPostStatusUpdWrtOffSettleRtn extends ActivityLifecycle {
   private static final FusionFileLogger verLogger = FusionFileLogger.getLogger(AAPostStatusUpdWrtOffSettleRtn.class);


   @Override
   public void generateSecondaryActivity(AaAccountDetailsRecord accountDetailRecord, AaArrangementActivityRecord arrangementActivityRecord, ArrangementContext arrangementContext, AaArrangementRecord arrangementRecord, AaArrangementActivityRecord masterActivityRecord, TStructure productPropertyRecord, AaProductCatalogRecord productRecord, TStructure record, SecondaryActivity secondaryActivity) {
     

      try {
         Contract contractObj = new Contract(this);
         Session ssObj = new Session(this);
         DataAccess da = new DataAccess(this);
        
         
         if (arrangementContext.getActivityStatus().equals("UNAUTH")) {
             
           
           
            updateActivityStatus(arrangementContext, secondaryActivity,da,ssObj,contractObj,accountDetailRecord,arrangementActivityRecord);
         }
      } catch (Exception e) {
         verLogger.info(e.getMessage());
      }

   }

   /**
 * @param arrangementContext
 * @param secondaryActivity
 * @param da
 * @param ssObj
 * @param contractObj 
 * @param accountDetailRecord 
 * @param arrangementActivityRecord 
 */
private void updateActivityStatus(ArrangementContext arrangementContext, SecondaryActivity secondaryActivity,
        DataAccess da, Session ssObj, Contract contractObj, AaAccountDetailsRecord accountDetailRecord, AaArrangementActivityRecord arrangementActivityRecord) {
    try {
        String arrId = arrangementActivityRecord.getArrangement().getValue();
        contractObj.setContractId(arrId);
        DatesRecord dateObj = new DatesRecord(da.getRecord("DATES", ssObj.getCompanyId()));
        String today = dateObj.getToday().getValue();
        String activity=arrangementContext.getCurrentActivity();
        String compMne=ssObj.getCompanyRecord().getFinancialMne().getValue();
        AaPrdDesAccountRecord aaAccObj = new AaPrdDesAccountRecord(contractObj.getConditionForProperty("ACCOUNT"));
        String ffLoanSts = aaAccObj.getLocalRefField("FF.LOAN.STATUS").getValue();
        AaActivityHistoryRecord aaActivityHistoryRecord = new AaActivityHistoryRecord(
                da.getRecord("AA.ACTIVITY.HISTORY", arrId));
        if (activity.equals("LENDING-APPLYPAYMENT-INSURANCE.SETTLEMENT")&& (Boolean.TRUE.equals(checkConfirmStatus(aaAccObj)) || Boolean.TRUE.equals(checkConfirmGuarStatus(aaAccObj)))) {
            verLogger.info("log for settle instructions ");
               updateLoanStatus(arrangementContext, secondaryActivity, arrId, "DECEASED");
         }
        if (activity.equals("LENDING-SETTLE-FORECLOSURE")&&!checkSettleForeClosureActvty(aaActivityHistoryRecord,
                "LENDING-CHARGEOFF-ARRANGEMENT")) {
            String matDate = accountDetailRecord.getMaturityDate().getValue();
            verLogger.info(matDate + " today " + today);
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
            LocalDate d1 = LocalDate.parse(today, formatter);
            LocalDate d2 = LocalDate.parse(matDate, formatter);
            verLogger.info("d1 " + d1 + " d2 " + d2);
            if (d1.isAfter(d2)) {
               updateLoanStatus(arrangementContext, secondaryActivity, arrId, "Closure");
            } else if (d1.isBefore(d2)) {
               updateLoanStatus(arrangementContext, secondaryActivity, arrId, "Foreclosure");
            }
         }
        if (activity.equals("LENDING-APPLYPAYMENT-PR.COLLECTION")&&Boolean.TRUE.equals(checkLoanStatus(da,compMne,accountDetailRecord, arrangementActivityRecord))) {
            
               updateLoanStatus(arrangementContext, secondaryActivity, arrId, "Closure");
            
         }

        if (ffLoanSts.equals("WRITE OFF")&&activity.equals("LENDING-APPLYPAYMENT-WRITEOFF.SETTLEMENT")&&Boolean.TRUE.equals(checkLoanAmount(da,compMne,accountDetailRecord, arrangementActivityRecord))) {
           
              updateLoanStatus(arrangementContext, secondaryActivity, arrId, "POST WRITE OFF CLOSED");
           
        }

    }catch (Exception e) {
        verLogger.info(e.getMessage());
    }
    
    
}
private boolean checkSettleForeClosureActvty(AaActivityHistoryRecord aaActivityHistoryRecord, String activity) {
    try {
        for (EffectiveDateClass effectiveDateClass : aaActivityHistoryRecord.getEffectiveDate()) {
            for (ActivityRefClass activityRefClass : effectiveDateClass.getActivityRef()) {
                 if (activityRefClass.getActivity().getValue().equals(activity)) {
                    
                    return true;
                }

            }
        }
    }catch (Exception e) {
        verLogger.info(e.getMessage());
    }
    return false;
}
private Boolean checkConfirmStatus(AaPrdDesAccountRecord aaAccObj) {
      Boolean checkConfSts = false;

      try {
         verLogger.info("inside confirm status method ");

         for(LocalRefGroup curFields : aaAccObj.getLocalRefGroups("FF.CUR.DOD.STS")) {
            verLogger.info("cur field value " + curFields.getLocalRefField("FF.CUR.DOD.STS").getValue());
            if (curFields.getLocalRefField("FF.CUR.DOD.STS").getValue().equals("CONFIRMED")) {
               checkConfSts = true;
               break;
            }
         }
      } catch (Exception e) {
         verLogger.info(e.getMessage());
      }

      return checkConfSts;
   }

private Boolean checkConfirmGuarStatus(AaPrdDesAccountRecord aaAccObj) {
    Boolean checkConfSts = false;

    try {
       verLogger.info("inside guar confirm status method ");

       for(LocalRefGroup curFields : aaAccObj.getLocalRefGroups("FF.GUAR.DOD.STS")) {
          verLogger.info("cur field value " + curFields.getLocalRefField("FF.GUAR.DOD.STS").getValue());
          if (curFields.getLocalRefField("FF.GUAR.DOD.STS").getValue().equals("CONFIRMED")) {
             checkConfSts = true;
             break;
          }
       }
    } catch (Exception e) {
       verLogger.info(e.getMessage());
    }

    return checkConfSts;
 }

   private Boolean checkLoanAmount(DataAccess da, String compMne, AaAccountDetailsRecord accountDetailRecord, AaArrangementActivityRecord arrangementActivityRecord) {
      verLogger.info("inside checkloan amt method");
      Boolean checkAmt = false;
      verLogger.info("inside checkloan amt method1");

      try {
         String origAmt = arrangementActivityRecord.getOrigTxnAmt().getValue();
         BigDecimal origAmount = new BigDecimal(origAmt);
         verLogger.info("origAmt " + origAmt);
         List<BillPayDateClass> billList = accountDetailRecord.getBillPayDate();
         verLogger.info("billList " + billList);

         for(BillPayDateClass billNos : billList) {
            for(BillIdClass billIds : billNos.getBillId()) {
               if (billIds.getPayMethod().getValue().equals("INFO")) {
                  String billId = billIds.getBillId().getValue().replace("/", "");
                  verLogger.info("billId " + billId);
                  AaBillDetailsRecord billObj = new AaBillDetailsRecord(da.getRecord(compMne, "AA.BILL.DETAILS", "", billId));
                  String osAmt = billObj.getOsTotalAmount().getValue();
                  BigDecimal osAmount = new BigDecimal(osAmt);
                  verLogger.info("osAmt " + osAmt);
                  if (origAmount.compareTo(osAmount) == 0) {
                     checkAmt = true;
                     break;
                  }
               }
            }
         }
      } catch (Exception e) {
         verLogger.info(e.getMessage());
      }

      return checkAmt;
   }

   private Boolean checkLoanStatus(DataAccess da, String compMne, AaAccountDetailsRecord accountDetailRecord, AaArrangementActivityRecord arrangementActivityRecord) {
      verLogger.info("inside checkloan status method");
      String arrId = arrangementActivityRecord.getArrangement().getValue();
      Boolean checkSts = false;
      verLogger.info("inside checkloan status method1");

      try {
         String payEndDate = accountDetailRecord.getPaymentEndDate().getValue();
         List<String> billList = da.selectRecords(compMne, "AA.BILL.DETAILS", "", "WITH ARRANGEMENT.ID EQ " + arrId + " AND ACTUAL.PAY.DATE EQ " + payEndDate);
         verLogger.info("BillList " + billList + " payEndDate " + payEndDate + " arrId " + arrId);

         for(String billId : billList) {
            AaBillDetailsRecord aaBillObj = new AaBillDetailsRecord(da.getRecord(compMne, "AA.BILL.DETAILS", "", billId));
            verLogger.info("OsAmt " + aaBillObj.getOsTotalAmount().getValue());
            BigDecimal osAmount = new BigDecimal(aaBillObj.getOsTotalAmount().getValue());
            if (osAmount.compareTo(BigDecimal.ZERO) == 0) {
               checkSts = true;
            }
         }
      } catch (Exception e) {
         verLogger.info(e.getMessage());
      }

      return checkSts;
   }

   private void updateLoanStatus(ArrangementContext arrangementContext, SecondaryActivity secondaryActivity, String arrId, String status) {
      try {
         Property accProp = new Property();
         accProp.setPropertyName("ACCOUNT");
         FieldPair fieldPair = new FieldPair();
         fieldPair.setFieldName("FF.LOAN.STATUS");
         fieldPair.setFieldValue(status);
         accProp.setFieldPairs(fieldPair, 0);
         verLogger.info(" accProp " + accProp.toString());
         String aaArrangementActivityId = arrangementContext.getArrangementActivityId();
         String effDate = arrangementContext.getActivityEffectiveDate();
         TDate yEffDate = new TDate(effDate);
         secondaryActivity.setArrangementEffectivedate(yEffDate);
         secondaryActivity.setArrangementId(arrId);
         secondaryActivity.setArrangementActivityId(aaArrangementActivityId);
         String secondaryActivityName = "LENDING-UPDATE-ACCOUNT";
         secondaryActivity.setNewActivity(secondaryActivityName);
         secondaryActivity.setProperties(accProp, 0);
         verLogger.info("secondaryActivity " + secondaryActivity);
      } catch (Exception e) {
         verLogger.info(e.getMessage());
      }

   }
}