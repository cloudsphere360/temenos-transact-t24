package com.temenos.fusion;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import com.temenos.api.TStructure;
import com.temenos.api.TValidationResponse;
//import com.temenos.api.exceptions.T24CoreException;
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
    DataAccess da = new DataAccess(this);
    Session ses = new Session(this);
    private static final FusionFileLogger FflFunderDetailsAccntUpdtLog = FusionFileLogger
            .getLogger(FflFunderDetailsAccntUpdt.class);
   // private static final String EB_FF_I_CHECK_CUST = "EB-FF.ID.CHECK.CUST";
   // private static final String EB_FF_FUNDER_NAME_TRANCHEC = "EB-FF.FUNDER.NAME.TRANCHE";
  //  private static final String EB_FF_ALLOC_MODE= "EB-FF.ALLOC.MODE";
    private static final Set<String> VALID_ALLOC_MODES = new HashSet<>(Arrays.asList("DIR", "OWN", "PTC"));

    String ffFunderTranche = "";
    boolean errorFlag = false;
    String ffAllocMode = "";

    @Override
    public TValidationResponse validateRecord(AaAccountDetailsRecord accountDetailRecord,
            AaArrangementActivityRecord arrangementActivityRecord, ArrangementContext arrangementContext,
            AaArrangementRecord arrangementRecord, AaArrangementActivityRecord masterActivityRecord,
            TStructure productPropertyRecord, AaProductCatalogRecord productRecord, TStructure record) {      
        AaPrdDesAccountRecord aaPrdDesAccount = new AaPrdDesAccountRecord(record);
        Session session = new Session(this);
        String ofsSource = session.getSourceId();
        //if (ofsSource.equals("FF.BULK.OFS")) {
        FflFunderDetailsAccntUpdtLog.info("ofsSource" + ofsSource);       
            FflFunderDetailsAccntUpdtLog.info("aaPrdDesAccount" + aaPrdDesAccount);
            String ffFunderName = aaPrdDesAccount.getLocalRefField("FF.FUNDER.NAME").getValue();
            FflFunderDetailsAccntUpdtLog.info("ffFunderName" + ffFunderName);
            //
            ffFunderTranche = aaPrdDesAccount.getLocalRefField("FF.FN.TRANCHE").getValue();
            FflFunderDetailsAccntUpdtLog.info("ffFunderTranche"+ffFunderTranche);
            ffAllocMode = aaPrdDesAccount.getLocalRefField("FF.ALLOC.MODE").getValue();
            FflFunderDetailsAccntUpdtLog.info("ffAllocMode"+ffAllocMode);
            if(ffFunderName!=null && !ffFunderName.isEmpty()) {
                getEbFfFunderDetails(ffFunderName,  aaPrdDesAccount);
                FflFunderDetailsAccntUpdtLog.info("entering to method");
            } 
        //}
        return aaPrdDesAccount.getValidationResponse();
    }
    private void getEbFfFunderDetails(String ffFunderName, AaPrdDesAccountRecord aaPrdDesAccount) {
        try {
            
            EbFfFunderDetailsRecord ebfflCustomer = new EbFfFunderDetailsRecord(
                    da.getRecord("EB.FF.FUNDER.DETAILS", ffFunderName));
            FflFunderDetailsAccntUpdtLog.info("ebfflCustomer" + ebfflCustomer);
            //
            for (int i = 0; i < ebfflCustomer.getFfFunderNameTranche().size(); i++) {
                String trancheValue = ebfflCustomer.getFfFunderNameTranche().get(i).getFfFunderNameTranche()
                        .getValue();
                FflFunderDetailsAccntUpdtLog.info("trancheValue"+trancheValue);
                if (trancheValue != null && !trancheValue.isEmpty() && trancheValue.trim().equalsIgnoreCase(ffFunderTranche.trim())) {
                    FflFunderDetailsAccntUpdtLog.info("checking funder tranche");
                    errorFlag = true;
                    break;
                }

            }
        } catch (Exception e) {          
            aaPrdDesAccount.getLocalRefField("FF.FUNDER.NAME").setError("Funder Name does not exist in Funder Master");
            FflFunderDetailsAccntUpdtLog.error("Funder Customer is not working");
           // throw new T24CoreException("", EB_FF_I_CHECK_CUST);            
        }
        if (ffAllocMode != null && !ffAllocMode.isEmpty()) {
            ffAllocMode = ffAllocMode.trim().toUpperCase();
            FflFunderDetailsAccntUpdtLog.error("ffAllocMode final");
            if (!VALID_ALLOC_MODES.contains(ffAllocMode)) {
                aaPrdDesAccount.getLocalRefField("FF.ALLOC.MODE").setError("Alloc mode does not exist");
                FflFunderDetailsAccntUpdtLog.error("Alloc mode is not working");
                //throw new T24CoreException("", EB_FF_ALLOC_MODE);
            }
        }

        if (ffFunderTranche != null && !ffFunderTranche.isEmpty() && !errorFlag) {
            aaPrdDesAccount.getLocalRefField("FF.FN.TRANCHE").setError("Funder Name Tranche does not belong to the funder");
            FflFunderDetailsAccntUpdtLog.error("funder tranche is not working");
            //throw new T24CoreException("", EB_FF_FUNDER_NAME_TRANCHEC);
        }
    }
}
