package com.temenos.fusion;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.temenos.api.TDate;
import com.temenos.api.TStructure;
import com.temenos.t24.api.arrangement.accounting.Contract;
import com.temenos.t24.api.complex.aa.activityhook.ArrangementContext;
import com.temenos.t24.api.complex.aa.contractapi.RepaymentDetails;
import com.temenos.t24.api.hook.arrangement.ActivityLifecycle;
import com.temenos.t24.api.records.aaaccountdetails.AaAccountDetailsRecord;
import com.temenos.t24.api.records.aaarrangement.AaArrangementRecord;
import com.temenos.t24.api.records.aaarrangementactivity.AaArrangementActivityRecord;
import com.temenos.t24.api.records.aaprddespaymentschedule.AaPrdDesPaymentScheduleRecord;
import com.temenos.t24.api.records.aaprddespaymentschedule.PaymentTypeClass;
import com.temenos.t24.api.records.aaprddespaymentschedule.PercentageClass;
import com.temenos.t24.api.records.aaprddespaymentschedule.PropertyClass;
import com.temenos.t24.api.records.aaproductcatalog.AaProductCatalogRecord;

/*-----------------------------------------------------------------------------
 * @author VINOTHINI P
 * Date Created: 
 * Attached as : ACTIVITY.API
 * EB.API : EB.FF.PYMT.SCH.UPD
 * Attached to :PRE VALIDATION ROUTINE
 * Description: This routine is used to change the schedule either full or single.
 *------------------------------------------------------------------------------ 
 * Modification History :
 *----------------------------------------------------------------------------- 
 *22-Aug-2023   Development      Initial Version
 *-----------------------------------------------------------------------------
 */
public class FfAaScheduleUpdate extends ActivityLifecycle {
    private static final FusionFileLogger FfAaScheduleUpdatelog = FusionFileLogger.getLogger(FfAaScheduleUpdate.class);
  
    static final String CONSTANT = "CONSTANT";
    static final String INTEREST = "INTEREST";

    @Override
    public void defaultFieldValues(AaAccountDetailsRecord accountDetailRecord,
            AaArrangementActivityRecord arrangementActivityRecord, ArrangementContext arrangementContext,
            AaArrangementRecord arrangementRecord, AaArrangementActivityRecord masterActivityRecord,
            TStructure productPropertyRecord, AaProductCatalogRecord productRecord, TStructure currRecord) {
        try {
            FfAaScheduleUpdatelog.info("FfAaScheduleUpdate:defaultFieldValues triggered");
            Contract contractObj = new Contract(this);
            contractObj.setContractId(arrangementActivityRecord.getArrangement().toString());

            AaPrdDesPaymentScheduleRecord paymentScheduleObj = new AaPrdDesPaymentScheduleRecord(currRecord);
            List<PaymentTypeClass> paymentTypeList = paymentScheduleObj.getPaymentType();

            String activity = arrangementActivityRecord.getActivity().getValue();
            String chgPayDateUpdDate = arrangementActivityRecord.getRemarks().getValue();
            FfAaScheduleUpdatelog.info("FfAaScheduleUpdate:defaultFieldValues chgPayDateUpdDate " + chgPayDateUpdDate);
            String dueDate = chgPayDateUpdDate.split("-")[0];
            String chgPayDate = chgPayDateUpdDate.split("-")[1];

            String simRefId = arrangementActivityRecord.getSimRunRef().getValue();
            TDate effectiveDate = new TDate(arrangementActivityRecord.getEffectiveDate().toString());
            TDate maturityDate = new TDate(accountDetailRecord.getMaturityDate().toString());

            String nextDate = getNextPaymentDate(simRefId, contractObj, effectiveDate, maturityDate, dueDate);
            FfAaScheduleUpdatelog.info("FfAaScheduleUpdate:defaultFieldValues " + nextDate);
            String actualAmt = getActualAmt(contractObj);
            if ("LENDING-CHANGE-SCHD.FULL".equals(activity)) {
                updateStartDateforfullSchedule(paymentScheduleObj, chgPayDate, actualAmt);
            }
            if ("LENDING-CHANGE-PAYMENT.SCHEDULE".equals(activity)) {

                addMultivalueConstantInterest(paymentTypeList, paymentScheduleObj, actualAmt);
                updateStartDateforSingleSchedule(paymentScheduleObj, chgPayDate, nextDate);
                FfAaScheduleUpdatelog
                        .info(" FfAaScheduleUpdate:defaultFieldValues paymentScheduleObj before final update: "
                                + paymentScheduleObj.toString());
                PaymentTypeClass interest = paymentScheduleObj.getPaymentType(2);
                PaymentTypeClass constant = paymentScheduleObj.getPaymentType(3);
                paymentScheduleObj.setPaymentType(constant, 2);
                paymentScheduleObj.setPaymentType(interest, 3);

                FfAaScheduleUpdatelog
                        .info("FfAaScheduleUpdate:defaultFieldValues Updated payment schedule successfully for: "
                                + arrangementActivityRecord.getArrangement());
            }
            FfAaScheduleUpdatelog.info("FfAaScheduleUpdate:defaultFieldValues paymentScheduleObj final update: "
                    + paymentScheduleObj.toString());
            currRecord.set(paymentScheduleObj.toStructure());
        } catch (

        Exception e) {
            FfAaScheduleUpdatelog
                    .error("FfAaScheduleUpdate:defaultFieldValues Error in FfAaScheduleUpdate.updateContractFields: "
                            + e.getMessage());
        }
    }

    private String getActualAmt(Contract contractObj) {
        String actualAmt = null;
        try {
            FfAaScheduleUpdatelog.info("FfAaScheduleUpdate:getActualAmt triggered");
            AaPrdDesPaymentScheduleRecord aaPrdDesPaymentScheduleObj = new AaPrdDesPaymentScheduleRecord(
                    contractObj.getSimulationConditionForProperty("PAYMENT.SCHEDULE"));
            FfAaScheduleUpdatelog.info("FfAaScheduleUpdate:getActualAmt aaPrdDesPaymentScheduleObj "
                    + aaPrdDesPaymentScheduleObj.toString());
            List<PaymentTypeClass> paymentTypeList = aaPrdDesPaymentScheduleObj.getPaymentType();
            FfAaScheduleUpdatelog.info("FfAaScheduleUpdate:getActualAmt paymentTypeList " + paymentTypeList.toString());
            for (PaymentTypeClass paymentType : paymentTypeList) {
                if ("DUE".equalsIgnoreCase(paymentType.getPaymentMethod().getValue())
                        && (CONSTANT.equalsIgnoreCase(paymentType.getPaymentType().getValue()))) {
                    FfAaScheduleUpdatelog.info("FfAaScheduleUpdate:getActualAmt paymentmethod "
                            + paymentType.getPaymentMethod().getValue());
                    List<PercentageClass> percentageList = paymentType.getPercentage();
                    for (PercentageClass percentageVal : percentageList) {
                        actualAmt = percentageVal.getActualAmt().getValue();

                    }

                }
            }
        } catch (Exception e) {
            FfAaScheduleUpdatelog.info("FfAaScheduleUpdate:getActualAmt Error enable to get actual amt " + e);
        }
        return actualAmt;
    }

    private void addMultivalueConstantInterest(List<PaymentTypeClass> paymentTypeList,
            AaPrdDesPaymentScheduleRecord paymentScheduleObj, String actualAmt) {
        FfAaScheduleUpdatelog.info("FfAaScheduleUpdate:addMultivalueConstantInterest triggered");
        try {

            int constantCount = 0;
            int interestCount = 0;

            for (PaymentTypeClass pt : paymentTypeList) {
                String type = pt.getPaymentType().getValue();
                if (CONSTANT.equals(type))
                    constantCount++;
                FfAaScheduleUpdatelog
                        .info("FfAaScheduleUpdate:addMultivalueConstantInterest constantCount loop CONSTANT "
                                + constantCount);

                if (INTEREST.equals(type))
                    interestCount++;
                FfAaScheduleUpdatelog
                        .info("FfAaScheduleUpdate:addMultivalueConstantInterest interestCount loop INTEREST "
                                + interestCount);
            }

            for (PaymentTypeClass paymentType : paymentTypeList) {
                String type = paymentType.getPaymentType().getValue();
                FfAaScheduleUpdatelog
                        .info(" FfAaScheduleUpdate:addMultivalueConstantInterest paymentScheduleObj before: "
                                + paymentScheduleObj.toString());

                // Only duplicate CONSTANT
                duplicateConstant(paymentScheduleObj, type, paymentType, constantCount, actualAmt);

                // Only duplicate INTEREST
                duplicateInterest(paymentScheduleObj, type, paymentType, interestCount);

            }

            FfAaScheduleUpdatelog.info("FfAaScheduleUpdate:addMultivalueConstantInterest paymentScheduleObj After: "
                    + paymentScheduleObj.toString());

        } catch (Exception e) {

            FfAaScheduleUpdatelog.error(
                    "FfAaScheduleUpdate:addMultivalueConstantInterest Error in add Multivalue Constant Interest: "
                            + e.getMessage());
        }
    }

    private void duplicateInterest(AaPrdDesPaymentScheduleRecord paymentScheduleObj, String type,
            PaymentTypeClass paymentType, int interestCount) {

        FfAaScheduleUpdatelog.info("FfAaScheduleUpdate:duplicateInterest method trigerred");
        try {
            FfAaScheduleUpdatelog.info("FfAaScheduleUpdate:duplicateInterest interestCount INTEREST " + interestCount);
            if (INTEREST.equals(type) && interestCount < 2) {
                FfAaScheduleUpdatelog
                        .info("FfAaScheduleUpdate:duplicateInterest Duplicate loop triggered for INTEREST " + type);
                PaymentTypeClass copyPaymentIntType = new PaymentTypeClass();

                copyPaymentIntType.setPaymentType(type);
                copyPaymentIntType.setPaymentFreq(paymentType.getPaymentFreq().getValue());
                copyPaymentIntType.setPaymentMethod(paymentType.getPaymentMethod().getValue());
                copyPaymentIntType.setBillType(paymentType.getBillType().getValue());
                copyPaymentIntType.setBillProduced(paymentType.getBillProduced().getValue());
                if (paymentType.getProperty() != null) {
                    for (PropertyClass propertyVal : paymentType.getProperty()) {
                        PropertyClass propertyCopy = new PropertyClass();
                        propertyCopy.setProperty(propertyVal.getProperty());
                        propertyCopy.setDueFreq(propertyVal.getDueFreq());
                        copyPaymentIntType.addProperty(propertyCopy);
                    }

                    PercentageClass percentageCopy = new PercentageClass();
                    copyPaymentIntType.addPercentage(percentageCopy);
                    FfAaScheduleUpdatelog.info(
                            " FfAaScheduleUpdate:duplicateInterest copyPaymentType " + copyPaymentIntType.toString());

                }
                FfAaScheduleUpdatelog.info(
                        " FfAaScheduleUpdate:duplicateInterest copyPaymentIntType " + copyPaymentIntType.toString());
                paymentScheduleObj.addPaymentType(copyPaymentIntType);
                FfAaScheduleUpdatelog
                        .info("FfAaScheduleUpdate:duplicateInterest Duplicated PaymentType inline: " + type);
            }
        } catch (Exception e) {
            FfAaScheduleUpdatelog.error("FfAaScheduleUpdate:duplicateInterest unable to duplicate interest" + e);
        }

    }

    private void duplicateConstant(AaPrdDesPaymentScheduleRecord paymentScheduleObj, String type,
            PaymentTypeClass paymentType, int constantCount, String actualAmt) {
        FfAaScheduleUpdatelog.info("FfAaScheduleUpdate:duplicateConstant method trigerred");
        try {
            FfAaScheduleUpdatelog.info("FfAaScheduleUpdate:duplicateConstant constantCount CONSTANT " + constantCount);
            if (CONSTANT.equals(type) && constantCount < 2) {
                FfAaScheduleUpdatelog
                        .info("FfAaScheduleUpdate:duplicateConstant Duplicate loop triggered for CONSTANT " + type);
                PaymentTypeClass copyPaymentType = new PaymentTypeClass();

                copyPaymentType.setPaymentType(type);
                copyPaymentType.setPaymentFreq(paymentType.getPaymentFreq().getValue());
                copyPaymentType.setPaymentMethod(paymentType.getPaymentMethod().getValue());
                copyPaymentType.setBillType(paymentType.getBillType().getValue());
                copyPaymentType.setBillProduced(paymentType.getBillProduced().getValue());
                // Copy Property list
                if (paymentType.getProperty() != null) {
                    for (PropertyClass propertyVal : paymentType.getProperty()) {
                        PropertyClass propertyCopy = new PropertyClass();
                        propertyCopy.setProperty(propertyVal.getProperty());
                        propertyCopy.setDueFreq(propertyVal.getDueFreq());
                        copyPaymentType.addProperty(propertyCopy);
                    }
                }
                // Copy Percentage list

                for (PercentageClass percentageVal : paymentType.getPercentage()) {
                    PercentageClass percentageCopy = new PercentageClass();
                    percentageCopy.setCalcAmount(percentageVal.getCalcAmount());
                    percentageCopy.setActualAmt(actualAmt);
                    copyPaymentType.addPercentage(percentageCopy);
                    FfAaScheduleUpdatelog
                            .info("FfAaScheduleUpdate:duplicateConstant copyPaymentType " + copyPaymentType.toString());
                }
                paymentScheduleObj.addPaymentType(copyPaymentType);
            }
        } catch (Exception e) {
            FfAaScheduleUpdatelog.error("FfAaScheduleUpdate:duplicateConstant unable to duplicate interest" + e);
        }

    }

    private String getNextPaymentDate(String simRefId, Contract contractObj, TDate effectiveDate, TDate maturityDate,
            String dueDate) {
        String nextDate = null;
        FfAaScheduleUpdatelog.info("FfAaScheduleUpdate:getNextPaymentDate method trigerred");
        try {

            List<RepaymentDetails> repaymentSchedule = contractObj.getPaymentSchedule(simRefId, effectiveDate,
                    maturityDate, null, null);

            for (RepaymentDetails schedule : repaymentSchedule) {
                String orginaldueDate = schedule.getDueDate().toString();

                if (orginaldueDate.compareTo(dueDate) > 0) { // find next greater date
                    nextDate = orginaldueDate;
                    break;

                }
            }

        } catch (Exception e) {
            FfAaScheduleUpdatelog.error(
                    "FfAaScheduleUpdate:getNextPaymentDate Error in getting the next due date: " + e.getMessage());
        }

        return nextDate;

    }

    private void updateStartDateforSingleSchedule(AaPrdDesPaymentScheduleRecord paymentScheduleObj, String chgPayDate,
            String nextDate) {
        FfAaScheduleUpdatelog.info("FfAaScheduleUpdate:updateStartDateforSingleSchedule method trigerred");
        try {
            int intCount = 0;
            int constCount = 0;
            for (PaymentTypeClass paymentType : paymentScheduleObj.getPaymentType()) {

                String type = paymentType.getPaymentType().getValue();
                if (CONSTANT.equals(type)) {
                    constCount++;
                    updateStartEndDateforConstant(type, paymentType, chgPayDate, nextDate, paymentScheduleObj,
                            constCount);
                }
                if (INTEREST.equals(type)) {
                    intCount++;
                    updateStartEndDateforInterest(type, paymentType, chgPayDate, intCount, nextDate);
                }

            }
        } catch (Exception e) {
            FfAaScheduleUpdatelog.info(
                    "FfAaScheduleUpdate:updateStartDateforSingleSchedule Error while update StartDate for single schedule: "
                            + e.getMessage());

        }
    }

    private void updateStartEndDateforInterest(String type, PaymentTypeClass paymentType, String chgPayDate,
            int intCount, String nextDate) {
        FfAaScheduleUpdatelog.info("FfAaScheduleUpdate:updateStartEndDateforInterest method trigerred");
        try {
            FfAaScheduleUpdatelog.info("FfAaScheduleUpdate:updateStartEndDateforInterest count value for type " + type
                    + "value " + intCount);
            if (intCount == 1) {
                updateStartDateforInterest(paymentType, chgPayDate, type, intCount);

            } else if (intCount == 2) {
                List<PercentageClass> percentageList = paymentType.getPercentage();
                if (percentageList == null || percentageList.isEmpty()) {
                    FfAaScheduleUpdatelog
                            .info("FfAaScheduleUpdate:updateStartEndDateforInterest No existing Percentage found for "
                                    + type + ", creating one...");
                    PercentageClass newPercentage = new PercentageClass();
                    newPercentage.setStartDate(nextDate);
                    newPercentage.setEndDate("R_MATURITY");

                    paymentType.addPercentage(newPercentage);

                } else {

                    for (PercentageClass percentage : percentageList) {
                        FfAaScheduleUpdatelog
                                .info("FfAaScheduleUpdate:updateStartEndDateforInterest Updating StartDate for " + type
                                        + " (inside Percentage)");
                        percentage.setStartDate(nextDate);
                        percentage.setEndDate("R_MATURITY");
                    }
                }

            }
        } catch (Exception e) {
            FfAaScheduleUpdatelog.info(
                    "FfAaScheduleUpdate:updateStartEndDateforInterest Error while update StartDate for single schedule: "
                            + e);
        }

    }

    private void updateStartDateforInterest(PaymentTypeClass paymentType, String chgPayDate, String type,
            int intCount) {
        FfAaScheduleUpdatelog.info("FfAaScheduleUpdate:updateStartDateforInterest method trigerred");
        try {
            List<PercentageClass> percentageList = paymentType.getPercentage();

            if (percentageList == null || percentageList.isEmpty()) {
                FfAaScheduleUpdatelog
                        .info("FfAaScheduleUpdate:updateStartDateforInterestNo existing Percentage found for interest "
                                + intCount + type + "creating one...");
                PercentageClass newPercentage = new PercentageClass();
                newPercentage.setStartDate(chgPayDate);
                newPercentage.setEndDate(chgPayDate);
                paymentType.addPercentage(newPercentage);

            } else {

                for (PercentageClass percentage : percentageList) {
                    FfAaScheduleUpdatelog
                            .info("FfAaScheduleUpdate:updateStartDateforInterestUpdating StartDate for interest "
                                    + intCount + type + "inside Percentage");
                    percentage.setStartDate(chgPayDate);
                    percentage.setEndDate(chgPayDate);
                }
            }
        } catch (Exception e) {
            FfAaScheduleUpdatelog.info(
                    "FfAaScheduleUpdate:updateStartDateforInterest Error while update StartDate for interest: " + e);
        }

    }

    private void updateStartEndDateforConstant(String type, PaymentTypeClass paymentType, String chgPayDate,
            String nextDate, AaPrdDesPaymentScheduleRecord paymentScheduleObj, int constCount) {
        FfAaScheduleUpdatelog.info("FfAaScheduleUpdate:updateStartEndDateforConstant method trigerred");
        try {
            FfAaScheduleUpdatelog.info("FfAaScheduleUpdate:updateStartEndDateforConstant count value for type " + type
                    + "value " + constCount + "before date update " + paymentScheduleObj.getPaymentType().toString());
            if (constCount == 1) {
                updateStartDateforConstantCount(paymentType, chgPayDate, constCount, type);

                FfAaScheduleUpdatelog.info("FfAaScheduleUpdate:updateStartEndDateforConstant for the count "
                        + constCount + "type " + type + paymentScheduleObj.getPaymentType().toString());
            } else if (constCount == 2) {
                List<PercentageClass> percentageList = paymentType.getPercentage();
                if (percentageList == null || percentageList.isEmpty()) {
                    FfAaScheduleUpdatelog.info(
                            "FfAaScheduleUpdate:updateStartEndDateforConstant No existing Percentage found for count "
                                    + constCount + type + "creating one");
                    PercentageClass newPercentage = new PercentageClass();
                    newPercentage.setStartDate(nextDate);
                    paymentType.addPercentage(newPercentage);

                } else {

                    for (PercentageClass percentage : percentageList) {
                        FfAaScheduleUpdatelog
                                .info("FfAaScheduleUpdate:updateStartEndDateforConstant Updating StartDate for count "
                                        + constCount + type + " inside Percentage");
                        percentage.setStartDate(nextDate);
                    }
                }

            }
            FfAaScheduleUpdatelog.info("FfAaScheduleUpdate:updateStartEndDateforConstant for the count " + constCount
                    + "type " + type + paymentScheduleObj.getPaymentType().toString());
        } catch (Exception e) {
            FfAaScheduleUpdatelog.error(
                    "FfAaScheduleUpdate:updateStartEndDateforConstant Error while update StartDate for constant: " + e);
        }

    }

    private void updateStartDateforConstantCount(PaymentTypeClass paymentType, String chgPayDate, int constCount,
            String type) {
        FfAaScheduleUpdatelog.info("FfAaScheduleUpdate:updateStartDateforConstantCount method trigerred");
        try {
            List<PercentageClass> percentageList = paymentType.getPercentage();

            if (percentageList == null || percentageList.isEmpty()) {
                FfAaScheduleUpdatelog.info(
                        "FfAaScheduleUpdate:updateStartDateforConstantCount No existing Percentage found for count "
                                + constCount + " type " + type + " creating one...");
                PercentageClass newPercentage = new PercentageClass();
                newPercentage.setStartDate(chgPayDate);
                newPercentage.setEndDate(chgPayDate);
                paymentType.addPercentage(newPercentage);

            } else {

                for (PercentageClass percentage : percentageList) {
                    FfAaScheduleUpdatelog.info(
                            "FfAaScheduleUpdate:updateStartDateforConstantCount Updating StartDate for constCount "
                                    + constCount + type + " inside Percentage");
                    percentage.setStartDate(chgPayDate);
                    percentage.setEndDate(chgPayDate);
                }
            }
        } catch (Exception e) {
            FfAaScheduleUpdatelog.error(
                    "FfAaScheduleUpdate:updateStartDateforConstantCount Error while update StartDate for constant: "
                            + e);
        }

    }

    private void updateStartDateforfullSchedule(AaPrdDesPaymentScheduleRecord paymentScheduleObj, String chgPayDate,
            String actualAmt) {
        FfAaScheduleUpdatelog.info("FfAaScheduleUpdate:updateStartDateforfullSchedule method trigerred");
        try {

            List<PaymentTypeClass> paymentTypeList = paymentScheduleObj.getPaymentType();

            Map<String, Integer> countMap = countPaymentTypes(paymentTypeList);

            if (countMap.get(CONSTANT) > 1 || countMap.get(INTEREST) > 1) {
                cleanupDuplicatePaymentTypes(paymentScheduleObj);
                normalizeInterestProperties(paymentScheduleObj);
            }

            List<PaymentTypeClass> cleanedList = paymentScheduleObj.getPaymentType();

            for (PaymentTypeClass paymentType : cleanedList) {
                String typeValue = paymentType.getPaymentType().getValue();

                if (CONSTANT.equals(typeValue) || INTEREST.equals(typeValue)) {
                    updateStartDate(paymentType, typeValue, chgPayDate, actualAmt);

                }
            }

        } catch (Exception e) {
            FfAaScheduleUpdatelog.error(
                    "FfAaScheduleUpdate:updateStartDateforfullSchedule Error while update StartDate for full schedule: "
                            + e.getMessage());

        }

    }

    private void normalizeInterestProperties(AaPrdDesPaymentScheduleRecord paymentScheduleObj) {

        for (PaymentTypeClass pt : paymentScheduleObj.getPaymentType()) {

            if (INTEREST.equals(pt.getPaymentType().getValue())) {
                removeDuplicateAdvPayRefund(pt);
            }
        }
    }

    private void removeDuplicateAdvPayRefund(PaymentTypeClass pt) {

        List<PropertyClass> props = pt.getProperty();
        if (props == null || props.size() <= 1) {
            return;
        }

        boolean advKept = false;

        // Traverse backward to safely remove
        for (int i = props.size() - 1; i >= 0; i--) {

            PropertyClass prop = props.get(i);

            if ("ADVPAYREFUND".equals(prop.getProperty().getValue())) {
                if (!advKept) {
                    advKept = true; // keep the last one
                } else {
                    pt.removeProperty(i);
                    FfAaScheduleUpdatelog.info("Removed duplicate ADVPAYREFUND at index " + i + " for PaymentType "
                            + pt.getPaymentType().getValue());
                }
            }
        }
    }

    private void cleanupDuplicatePaymentTypes(AaPrdDesPaymentScheduleRecord paymentScheduleObj) {

        FfAaScheduleUpdatelog.info("FfAaScheduleUpdate:cleanupDuplicatePaymentTypes triggered");

        try {
            List<PaymentTypeClass> list = paymentScheduleObj.getPaymentType();

            boolean constantKept = false;
            boolean interestKept = false;

            for (int i = list.size() - 1; i >= 0; i--) {
                String type = list.get(i).getPaymentType().getValue();

                if (CONSTANT.equals(type)) {
                    if (!constantKept) {
                        constantKept = true; // keep last CONSTANT
                    } else {
                        paymentScheduleObj.removePaymentType(i);
                        FfAaScheduleUpdatelog.info("Removed duplicate CONSTANT at index " + i);
                    }
                } else if (INTEREST.equals(type)) {
                    if (!interestKept) {
                        interestKept = true; // keep last INTEREST
                    } else {
                        paymentScheduleObj.removePaymentType(i);
                        FfAaScheduleUpdatelog.info("Removed duplicate INTEREST at index " + i);
                    }
                }
            }

            FfAaScheduleUpdatelog
                    .info("Cleanup done CONSTANT kept=" + constantKept + ", INTEREST kept=" + interestKept);

        } catch (Exception e) {
            FfAaScheduleUpdatelog.error("cleanupDuplicatePaymentTypes error: " + e);
        }
    }

    private Map<String, Integer> countPaymentTypes(List<PaymentTypeClass> paymentTypeList) {
        FfAaScheduleUpdatelog.info("FfAaScheduleUpdate:countPaymentTypes method trigerred");
        Map<String, Integer> countMap = new HashMap<>();
        try {
            int constantCount = 0;
            int interestCount = 0;

            for (PaymentTypeClass pt : paymentTypeList) {
                String type = pt.getPaymentType().getValue();

                if (CONSTANT.equals(type)) {
                    constantCount++;
                } else if (INTEREST.equals(type)) {
                    interestCount++;
                }
            }

            FfAaScheduleUpdatelog.info("Before cleanup -> CONSTANT=" + constantCount + ", INTEREST=" + interestCount);

            countMap.put(CONSTANT, constantCount);
            countMap.put(INTEREST, interestCount);

        } catch (Exception e) {
            FfAaScheduleUpdatelog.error(" FfAaScheduleUpdate:countPaymentTypes error in count the type" + e);
        }

        return countMap;
    }

    private void updateStartDate(PaymentTypeClass paymentType, String typeValue, String chgPayDate, String actualAmt) {
        FfAaScheduleUpdatelog.info("FfAaScheduleUpdate:updateStartDateforfullSchedule method trigerred");
        try {
            List<PercentageClass> percentageList = paymentType.getPercentage();

            if (percentageList == null || percentageList.isEmpty()) {

                FfAaScheduleUpdatelog
                        .info("FfAaScheduleUpdate:updateStartDateforfullSchedule No existing Percentage found for "
                                + typeValue + ", creating one...");
                PercentageClass newPercentage = new PercentageClass();
                newPercentage.setStartDate(chgPayDate);
                if (CONSTANT.equals(typeValue)) {
                    newPercentage.setActualAmt(actualAmt);
                }
                paymentType.addPercentage(newPercentage);

            } else {

                for (PercentageClass percentage : percentageList) {
                    FfAaScheduleUpdatelog
                            .info(" FfAaScheduleUpdate:updateStartDateforfullSchedule Updating StartDate for "
                                    + typeValue + " (inside Percentage)");
                    if (CONSTANT.equals(typeValue)) {
                        percentage.setActualAmt(actualAmt);
                    }
                    percentage.setStartDate(chgPayDate);
                }
            }

        } catch (Exception e) {
            FfAaScheduleUpdatelog.error(
                    "FfAaScheduleUpdate:updateStartDateforfullSchedule Error while update StartDate for full schedule: "
                            + e);
        }
    }
}
