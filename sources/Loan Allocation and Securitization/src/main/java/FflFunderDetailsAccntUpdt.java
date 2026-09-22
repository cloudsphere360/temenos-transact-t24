package com.temenos.fusion;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import com.temenos.api.TStructure;
import com.temenos.api.TValidationResponse;
import com.temenos.t24.api.arrangement.accounting.Contract;
import com.temenos.t24.api.complex.aa.activityhook.ArrangementContext;
import com.temenos.t24.api.hook.arrangement.ActivityLifecycle;
import com.temenos.t24.api.records.aaaccountdetails.AaAccountDetailsRecord;
import com.temenos.t24.api.records.aaarrangement.AaArrangementRecord;
import com.temenos.t24.api.records.aaarrangementactivity.AaArrangementActivityRecord;
import com.temenos.t24.api.records.aaprddesaccount.AaPrdDesAccountRecord;
import com.temenos.t24.api.records.aaproductcatalog.AaProductCatalogRecord;
import com.temenos.t24.api.records.ebfffunderdetails.EbFfFunderDetailsRecord;
import com.temenos.t24.api.system.DataAccess;
import com.temenos.t24.api.system.Session;

public class FflFunderDetailsAccntUpdt extends ActivityLifecycle {

    private DataAccess da = new DataAccess(this);

    private static final FusionFileLogger FflFunderDetailsAccntUpdtLog = FusionFileLogger
            .getLogger(FflFunderDetailsAccntUpdt.class);

    private static final Set<String> VALID_ALLOC_MODES = new HashSet<>(Arrays.asList("DIR", "OWN", "PTC"));

    private String ffFunderTranche = "";
    private boolean errorFlag = false;
    private String ffAllocMode = "";
    private String oldEffectiveDate = "";
    private String newEffectiveDate = "";
    private String oldFunderName = "";
    private String ffFunderName = "";
    String date = "";
    String toDayDate = "";
    String toDayDate1 = "";
    String idComp = "";
    String idCompStr="";

    @Override
    public TValidationResponse validateRecord(AaAccountDetailsRecord accountDetailRecord,
            AaArrangementActivityRecord arrangementActivityRecord, ArrangementContext arrangementContext,
            AaArrangementRecord arrangementRecord, AaArrangementActivityRecord masterActivityRecord,
            TStructure productPropertyRecord, AaProductCatalogRecord productRecord, TStructure record) {
        Session ses = new Session(this);
        toDayDate1 = ses.getCurrentVariable("!TODAY");
        FflFunderDetailsAccntUpdtLog.info("toDayDate1" + toDayDate1);// 20260413
        String localdDate = toDayDate1.substring(2);
        FflFunderDetailsAccntUpdtLog.info("localdDate = " + localdDate);//260413
        //
        LocalDate today = LocalDate.now();
        FflFunderDetailsAccntUpdtLog.info("today" + today);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyMMdd");
        FflFunderDetailsAccntUpdtLog.info("formatter" + formatter);
        toDayDate = today.format(formatter);
        FflFunderDetailsAccntUpdtLog.info("toDayDate" + toDayDate);
        Contract contract = new Contract(this);
        contract.setContractId(arrangementActivityRecord.getArrangement().getValue());

        AaPrdDesAccountRecord aaPrdDesAccount = new AaPrdDesAccountRecord(record);
        FflFunderDetailsAccntUpdtLog.info("entering7");
        FflFunderDetailsAccntUpdtLog.info("aaPrdDesAccount = " + aaPrdDesAccount);

        errorFlag = false;
        oldFunderName = "";
        oldEffectiveDate = "";

        ffFunderName = getValue(aaPrdDesAccount.getLocalRefField("FF.FUNDER.NAME").getValue());

        ffFunderTranche = getValue(aaPrdDesAccount.getLocalRefField("FF.FN.TRANCHE").getValue());

        ffAllocMode = getValue(aaPrdDesAccount.getLocalRefField("FF.ALLOC.MODE").getValue());

        try {
            newEffectiveDate = getValue(aaPrdDesAccount.getLocalRefField("FF.EFFECTIVE.DATE.ALLOC").getValue());
        } catch (Exception e) {
            e.getMessage();
        }
        try {
            String effectiveDateAllocMode = getValue(
                    aaPrdDesAccount.getLocalRefField("FF.EFFECTIVE.DATE.ALLOC").getValue());
            FflFunderDetailsAccntUpdtLog.info("NEW FF.EFFECTIVE.DATE.ALLOC = [" + effectiveDateAllocMode + "]");
        } catch (Exception e) {
            e.getMessage();
        }

        FflFunderDetailsAccntUpdtLog.info("NEW FF.FUNDER.NAME = [" + ffFunderName + "]");

        FflFunderDetailsAccntUpdtLog.info("NEW FF.FN.TRANCHE = [" + ffFunderTranche + "]");

        FflFunderDetailsAccntUpdtLog.info("NEW FF.ALLOC.MODE = [" + ffAllocMode + "]");
        FflFunderDetailsAccntUpdtLog.info("NEW FF.EFFECTIVE.DATE.ALLOC = [" + newEffectiveDate + "]");

        getExistingAccountDetails(contract, aaPrdDesAccount);
        validateDuplicateFunderName(aaPrdDesAccount);
        validateDuplicateEffectiveDate(aaPrdDesAccount);
        if (!ffFunderName.isEmpty()) {
            getEbFfFunderDetails(ffFunderName, aaPrdDesAccount);
        }

        return aaPrdDesAccount.getValidationResponse();
    }

    private void getExistingAccountDetails(Contract contract, AaPrdDesAccountRecord aaPrdDesAccount) {

        FflFunderDetailsAccntUpdtLog.info("Entering getExistingAccountDetails method");

        try {

            FflFunderDetailsAccntUpdtLog.info("Contract = " + contract);

            TStructure existingAccountStructure = contract.getConditionForProperty("ACCOUNT");

            if (existingAccountStructure == null) {

                FflFunderDetailsAccntUpdtLog.info("Existing ACCOUNT structure is null");

                return;
            }

            AaPrdDesAccountRecord existingAccount = new AaPrdDesAccountRecord(existingAccountStructure);

            FflFunderDetailsAccntUpdtLog.info("Existing AaPrdDesAccountRecord = " + existingAccount);

            oldFunderName = getValue(existingAccount.getLocalRefField("FF.FUNDER.NAME").getValue());

            FflFunderDetailsAccntUpdtLog.info("EXISTING FF.FUNDER.NAME = [" + oldFunderName + "]");
            //
            idComp = existingAccount.getIdComp3().getValue();
            FflFunderDetailsAccntUpdtLog.info("idComp" + idComp);
            idCompStr = idComp.toString().split("\\.")[0]; // "20260821"
            FflFunderDetailsAccntUpdtLog.info("idCompStr" + idCompStr);//20260413

            //
            String value = existingAccount.getDateTime(0);
            FflFunderDetailsAccntUpdtLog.info("value" + value);
            date = value.substring(0, 6);
            FflFunderDetailsAccntUpdtLog.info("date" + date);

            oldEffectiveDate = getValue(existingAccount.getLocalRefField("FF.EFFECTIVE.DATE.ALLOC").getValue());

            FflFunderDetailsAccntUpdtLog.info("EXISTING FF.EFFECTIVE.DA = [" + oldEffectiveDate + "]");

        } catch (Exception e) {

            FflFunderDetailsAccntUpdtLog.error(
                    "ERROR reading existing ACCOUNT details: " + e.getClass().getName() + " - " + e.getMessage());
        }
    }

    private void validateDuplicateFunderName(AaPrdDesAccountRecord aaPrdDesAccount) {

        try {
            FflFunderDetailsAccntUpdtLog.info("Checking duplicate Funder Name");

            FflFunderDetailsAccntUpdtLog.info("Existing Funder Name = [" + oldFunderName + "]");

            FflFunderDetailsAccntUpdtLog.info("New Funder Name = [" + ffFunderName + "]");
            if (!oldFunderName.isEmpty() && !ffFunderName.isEmpty() && oldFunderName.equalsIgnoreCase(ffFunderName)
                    && date.equals(toDayDate)) {

                aaPrdDesAccount.getLocalRefField("FF.FUNDER.NAME")
                        .setError("Funder Name already exists for the account");

                FflFunderDetailsAccntUpdtLog.error("DUPLICATE FUNDER NAME FOUND FOR TODAY. " + "Existing=["
                        + oldFunderName + "]" + ", New=[" + ffFunderName + "]" + ", AuditDate=[" + date + "]"
                        + ", TodayDate=[" + toDayDate + "]");
                //idCompStr.equals(toDayDate1) comparing cob date with id comp3
            } else {

                FflFunderDetailsAccntUpdtLog.info("Funder Name validation passed. " + "Existing=[" + oldFunderName
                        + "], New=[" + ffFunderName + "]");
            }
        } catch (Exception e) {
            e.getMessage();
        }
    }

    private void validateDuplicateEffectiveDate(AaPrdDesAccountRecord aaPrdDesAccount) {
        try {
            FflFunderDetailsAccntUpdtLog.info("Checking duplicate Effective Date Name");
            if (!oldEffectiveDate.isEmpty() && !newEffectiveDate.isEmpty()
                    && oldEffectiveDate.equalsIgnoreCase(newEffectiveDate) && date.equals(toDayDate)) {

                aaPrdDesAccount.getLocalRefField("FF.EFFECTIVE.DATE.ALLOC")
                        .setError("Effevtive Date already exists for the account");

                FflFunderDetailsAccntUpdtLog.error("DUPLICATE Effective Date FOUND FOR TODAY. ");
              //idCompStr.equals(toDayDate1) comparing cob date with id comp3
            } else {

                FflFunderDetailsAccntUpdtLog.info("Effective Date validation passed. " + "Existing=[" + oldEffectiveDate
                        + "], New=[" + newEffectiveDate + "]");
            }
        } catch (Exception e) {
            e.getMessage();
        }

    }

    private void getEbFfFunderDetails(String ffFunderName, AaPrdDesAccountRecord aaPrdDesAccount) {

        try {

            EbFfFunderDetailsRecord ebfflCustomer = new EbFfFunderDetailsRecord(
                    da.getRecord("EB.FF.FUNDER.DETAILS", ffFunderName));

            FflFunderDetailsAccntUpdtLog.info("EB.FF.FUNDER.DETAILS = " + ebfflCustomer);

            errorFlag = false;

            for (int i = 0; i < ebfflCustomer.getFfFunderNameTranche().size(); i++) {

                String trancheValue = ebfflCustomer.getFfFunderNameTranche().get(i).getFfFunderNameTranche().getValue();

                FflFunderDetailsAccntUpdtLog.info("Tranche Value = [" + trancheValue + "]");

                if (trancheValue != null && !trancheValue.trim().isEmpty() && ffFunderTranche != null
                        && !ffFunderTranche.trim().isEmpty()
                        && trancheValue.trim().equalsIgnoreCase(ffFunderTranche.trim())) {

                    errorFlag = true;

                    FflFunderDetailsAccntUpdtLog.info("Funder tranche matched");

                    break;
                }
            }

        } catch (Exception e) {

            aaPrdDesAccount.getLocalRefField("FF.FUNDER.NAME").setError("Funder Name does not exist in Funder Master");

            FflFunderDetailsAccntUpdtLog.error(
                    "Funder Name does not exist in Funder Master. " + e.getClass().getName() + " - " + e.getMessage());

            return;
        }

        if (!ffAllocMode.isEmpty()) {

            ffAllocMode = ffAllocMode.trim().toUpperCase();

            FflFunderDetailsAccntUpdtLog.info("Final Allocation Mode = [" + ffAllocMode + "]");

            if (!VALID_ALLOC_MODES.contains(ffAllocMode)) {

                aaPrdDesAccount.getLocalRefField("FF.ALLOC.MODE").setError("Alloc mode does not exist");

                FflFunderDetailsAccntUpdtLog.error("Invalid Allocation Mode = [" + ffAllocMode + "]");
            }
        }

        if (ffFunderTranche != null && !ffFunderTranche.isEmpty() && !errorFlag) {

            aaPrdDesAccount.getLocalRefField("FF.FN.TRANCHE")
                    .setError("Funder Name Tranche does not belong to the funder");

            FflFunderDetailsAccntUpdtLog.error("Funder Tranche does not belong to Funder Name. " + "Funder=["
                    + ffFunderName + "], Tranche=[" + ffFunderTranche + "]");
        }
    }

    private String getValue(String value) {

        if (value == null) {
            return "";
        }

        return value.trim();
    }
}